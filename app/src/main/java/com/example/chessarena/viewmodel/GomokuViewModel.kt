package com.example.chessarena.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chessarena.data.GamePreferences
import com.example.chessarena.data.GameRecord
import com.example.chessarena.data.DataRepository
import com.example.chessarena.engine.ChessEngine
import com.example.chessarena.engine.Difficulty
import com.example.chessarena.engine.GomokuEngine
import com.example.chessarena.game.gomoku.GomokuRules
import com.example.chessarena.game.gomoku.GomokuState
import com.example.chessarena.game.gomoku.GomokuStatus
import com.example.chessarena.game.gomoku.Stone
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * 五子棋 UI 状态
 */
data class GomokuUiState(
    val gameState: GomokuState? = null,
    val isAiThinking: Boolean = false,
    val evaluation: Int = 0,
    val difficulty: Difficulty = Difficulty.GOMOKU_SENIOR,
    val showDifficultyDialog: Boolean = true,
    val gameOverMessage: String? = null,
    val moveHistory: List<String> = emptyList(),
    val forbiddenMoves: List<Pair<Int, Int>> = emptyList(),
    val winningLine: List<Pair<Int, Int>> = emptyList(),
    val playerSide: Stone = Stone.BLACK,
    val engineHealthError: String? = null,
    val isGameActive: Boolean = false
)

class GomokuViewModel(
    private val engine: ChessEngine = GomokuEngine(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val initialDifficulty =
        GamePreferences.loadGomokuDifficulty() ?: Difficulty.GOMOKU_SENIOR
    private val _uiState = MutableStateFlow(GomokuUiState(difficulty = initialDifficulty))
    val uiState: StateFlow<GomokuUiState> = _uiState.asStateFlow()

    private var aiJob: Job? = null
    private var aiGeneration = 0
    private val stateHistory = mutableListOf<GomokuState>()
    private var recordSaved = false

    init {
        viewModelScope.launch {
            engine.initialize()
        }
    }

    /**
     * 选择难度开始五子棋对局
     */
    fun onDifficultySelected(difficulty: Difficulty, playerSide: Stone = Stone.BLACK) {
        viewModelScope.launch {
            cancelAiJob()
            ensureEngineReady()
            engine.setDifficulty(difficulty)
            GamePreferences.saveGomokuDifficulty(difficulty)

            val initialState = GomokuState.initial(useRenju = true)
            stateHistory.clear()
            stateHistory.add(initialState)
            recordSaved = false

            val forbidden = withContext(dispatcher) {
                GomokuRules.getForbiddenMoves(initialState)
            }

            _uiState.update {
                GomokuUiState(
                    gameState = initialState,
                    difficulty = difficulty,
                    showDifficultyDialog = false,
                    forbiddenMoves = forbidden,
                    playerSide = playerSide,
                    isGameActive = true
                )
            }

            if (playerSide == Stone.WHITE) {
                triggerAiMove(initialState)
            }
        }
    }

    /**
     * 点击棋盘落子
     */
    fun onPositionClick(row: Int, col: Int) {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        if (currentState.isAiThinking || currentState.gameOverMessage != null) return
        if (gameState.currentTurn != currentState.playerSide) return

        if (GomokuRules.isValidMove(gameState, col, row)) {
            executePlayerMove(col, row, gameState)
        }
    }

    private fun executePlayerMove(col: Int, row: Int, gameState: GomokuState) {
        stateHistory.add(gameState)

        // 玩家落子
        val newState = GomokuRules.makeMove(gameState, col, row)
        val stoneName = if (gameState.currentTurn == Stone.BLACK) "黑" else "白"
        val notation = "$stoneName ${'A' + col}${row + 1}"

        val eval = evaluateGomokuState(newState)
        _uiState.update {
            it.copy(
                gameState = newState,
                moveHistory = it.moveHistory + notation,
                evaluation = eval,
                forbiddenMoves = emptyList() // 落子后切换，只有黑方有禁手
            )
        }

        if (checkGameOver(newState)) return

        // 触发 AI 执白落子
        triggerAiMove(newState)
    }

    private fun triggerAiMove(gameState: GomokuState) {
        cancelAiJob()
        val generation = aiGeneration
        aiJob = viewModelScope.launch {
            _uiState.update { it.copy(isAiThinking = true) }

            try {
                ensureEngineReady()
                // 将棋盘状态转换为命令序列发送给 GomokuEngine
                val boardString = boardToEngineFormat(gameState)
                val result = withContext(dispatcher) {
                    engine.getBestMove(boardString, emptyList())
                }

                if (generation != aiGeneration) return@launch

                val engineMove = GomokuEngine.parseMove(result.bestMove)
                val aiMove = engineMove
                    ?.takeIf { (col, row) -> GomokuRules.isValidMove(gameState, col, row) }
                    ?: findFallbackAiMove(gameState)

                if (aiMove != null) {
                    if (generation != aiGeneration) return@launch
                    val (col, row) = aiMove
                    stateHistory.add(gameState)

                    val newState = GomokuRules.makeMove(gameState, col, row)
                    val stoneName = if (gameState.currentTurn == Stone.BLACK) "黑" else "白"
                    val notation = "$stoneName ${'A' + col}${row + 1}"

                    val forbidden = withContext(dispatcher) {
                        GomokuRules.getForbiddenMoves(newState)
                    }

                    val eval = evaluateGomokuState(newState)
                    _uiState.update {
                        it.copy(
                            gameState = newState,
                            isAiThinking = false,
                            evaluation = eval,
                            moveHistory = it.moveHistory + notation,
                            forbiddenMoves = forbidden
                        )
                    }

                    checkGameOver(newState)
                } else {
                    _uiState.update {
                        it.copy(
                            isAiThinking = false,
                            gameOverMessage = "棋盘已满，和棋！",
                            isGameActive = false
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation != aiGeneration) return@launch
                _uiState.update {
                    it.copy(
                        isAiThinking = false,
                        gameOverMessage = "五子棋引擎错误: ${e.message}",
                        isGameActive = false
                    )
                }
            }
        }
    }

    private fun findFallbackAiMove(state: GomokuState): Pair<Int, Int>? {
        val playerSide = _uiState.value.playerSide
        if (state.currentTurn == playerSide || state.isGameOver) return null

        val occupied = state.moveHistory.map { it.col to it.row }
        return (0 until GomokuState.SIZE).flatMap { row ->
            (0 until GomokuState.SIZE).map { col -> col to row }
        }
            .filter { (col, row) -> GomokuRules.isValidMove(state, col, row) }
            .minWithOrNull(
                compareBy<Pair<Int, Int>>(
                    { (col, row) ->
                        occupied.minOfOrNull { (occupiedCol, occupiedRow) ->
                            abs(col - occupiedCol) + abs(row - occupiedRow)
                        } ?: 0
                    },
                    { (col, row) ->
                        abs(col - GomokuState.SIZE / 2) + abs(row - GomokuState.SIZE / 2)
                    },
                    { (_, row) -> row },
                    { (col, _) -> col }
                )
            )
    }

    private fun checkGameOver(state: GomokuState): Boolean {
        val playerSide = _uiState.value.playerSide
        return when (state.status) {
            GomokuStatus.BLACK_WIN -> {
                val winningLine = findWinningLine(state, Stone.BLACK)
                _uiState.update {
                    it.copy(
                        gameOverMessage = "五连！黑方获胜！",
                        winningLine = winningLine,
                        isGameActive = false
                    )
                }
                saveGameRecord(if (playerSide == Stone.BLACK) "WIN" else "LOSE")
                true
            }
            GomokuStatus.WHITE_WIN -> {
                val lastMove = state.lastMove
                // 检查这是否是因为黑棋落子在禁手点而判定败北
                val isForbiddenLose = lastMove != null && lastMove.stone == Stone.BLACK && state.useRenju &&
                        stateHistory.lastOrNull()?.let { prevState -> GomokuRules.isForbiddenMove(prevState, lastMove.col, lastMove.row) } ?: false

                if (isForbiddenLose) {
                    _uiState.update {
                        it.copy(
                            gameOverMessage = "禁手判负！黑方落子在禁手点（X 标记处），白方获胜！",
                            winningLine = emptyList(),
                            isGameActive = false
                        )
                    }
                } else {
                    val winningLine = findWinningLine(state, Stone.WHITE)
                    _uiState.update {
                        it.copy(
                            gameOverMessage = "五连！白方获胜！",
                            winningLine = winningLine,
                            isGameActive = false
                        )
                    }
                }
                saveGameRecord(if (playerSide == Stone.WHITE) "WIN" else "LOSE")
                true
            }
            GomokuStatus.DRAW -> {
                _uiState.update {
                    it.copy(
                        gameOverMessage = "棋盘已满，和棋！",
                        isGameActive = false
                    )
                }
                saveGameRecord("DRAW")
                true
            }
            else -> false
        }
    }

    /**
     * 辅助寻找最后成五的子，用于渲染高亮线
     */
    private fun findWinningLine(state: GomokuState, stone: Stone): List<Pair<Int, Int>> {
        val last = state.lastMove ?: return emptyList()
        val dirs = listOf(Pair(1, 0), Pair(0, 1), Pair(1, 1), Pair(1, -1))
        for ((dx, dy) in dirs) {
            val line = mutableListOf<Pair<Int, Int>>()
            line.add(Pair(last.row, last.col))

            // 正向
            var c = last.col + dx
            var r = last.row + dy
            while (c in 0..14 && r in 0..14 && state.stoneAt(c, r) == stone) {
                line.add(Pair(r, c))
                c += dx
                r += dy
            }

            // 反向
            c = last.col - dx
            r = last.row - dy
            while (c in 0..14 && r in 0..14 && state.stoneAt(c, r) == stone) {
                line.add(Pair(r, c))
                c -= dx
                r -= dy
            }

            if (line.size >= 5) {
                return line.sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
            }
        }
        return emptyList()
    }

    fun onNewGame() {
        cancelAiJob()
        _uiState.update {
            it.copy(
                showDifficultyDialog = true
            )
        }
    }

    fun onUndo() {
        val currentState = _uiState.value
        if (currentState.isAiThinking) return

        if (stateHistory.size < 2) return

        stateHistory.removeLastOrNull()
        val previousState = stateHistory.removeLastOrNull() ?: return

        val newHistory = currentState.moveHistory.let { history ->
            if (history.size >= 2) history.dropLast(2) else emptyList()
        }

        // 悔棋后允许重新保存对局记录
        recordSaved = false

        viewModelScope.launch {
            val forbidden = withContext(dispatcher) {
                GomokuRules.getForbiddenMoves(previousState)
            }
            val eval = evaluateGomokuState(previousState)
            _uiState.update {
                it.copy(
                    gameState = previousState,
                    moveHistory = newHistory,
                    evaluation = eval,
                    forbiddenMoves = forbidden,
                    winningLine = emptyList(),
                    gameOverMessage = null,
                    isGameActive = true
                )
            }
        }
    }

    fun onResign() {
        val currentState = _uiState.value
        if (currentState.gameOverMessage != null) return

        cancelAiJob()

        _uiState.update {
            it.copy(
                isAiThinking = false,
                gameOverMessage = "黑方认输，白方获胜！",
                isGameActive = false
            )
        }

        saveGameRecord("LOSE")
    }

    /**
     * 自动保存五子棋对局记录
     */
    private fun saveGameRecord(result: String) {
        if (recordSaved) return
        recordSaved = true

        val state = _uiState.value
        val history = state.moveHistory
        if (history.isEmpty()) return

        val record = GameRecord(
            id = java.util.UUID.randomUUID().toString(),
            gameType = "gomoku",
            difficulty = state.difficulty.displayName,
            playerSide = if (state.playerSide == Stone.BLACK) "执黑" else "执白",
            result = result,
            moves = history,
            timestamp = System.currentTimeMillis(),
            movesCount = history.size,
            durationSeconds = history.size * 5L // 估算平均每步5秒
        )

        viewModelScope.launch(dispatcher) {
            try {
                DataRepository.saveGameRecord(record)
            } catch (t: Throwable) {
                Log.e("GomokuViewModel", "Failed to save game record", t)
            }
        }
    }

    fun onDismissDifficultyDialog() {
        if (_uiState.value.gameState != null) {
            _uiState.update { it.copy(showDifficultyDialog = false) }
        }
    }

    fun onDismissGameOver() {
        _uiState.update { it.copy(gameOverMessage = null) }
    }

    fun onDismissEngineError() {
        _uiState.update { it.copy(engineHealthError = null) }
    }

    fun onRetryEngine() {
        _uiState.update { it.copy(engineHealthError = null) }
        viewModelScope.launch { engine.initialize() }
    }

    /**
     * 辅助转换棋盘到引擎所需的参数形式
     */
    private fun boardToEngineFormat(state: GomokuState): String {
        // Gomocup 需要用 0 表示空，1 表示自己，2 表示对方
        // 在我们的设定中，黑是玩家 (1)，白是 AI (2)
        val sb = StringBuilder()
        for (r in 0 until GomokuState.SIZE) {
            for (c in 0 until GomokuState.SIZE) {
                val stone = state.stoneAt(c, r)
                val cell = when (stone) {
                    Stone.BLACK -> "1"
                    Stone.WHITE -> "2"
                    null -> "0"
                }
                sb.append(cell)
                if (c < GomokuState.SIZE - 1) sb.append(",")
            }
            if (r < GomokuState.SIZE - 1) sb.append(";")
        }
        return sb.toString()
    }

    override fun onCleared() {
        super.onCleared()
        cancelAiJob()
        engine.destroy()
    }

    private fun cancelAiJob() {
        aiJob?.cancel()
        engine.stop()
        aiGeneration += 1
    }

    /**
     * 局面 5 子滑动窗口局势评估算法
     */
    private fun evaluateGomokuState(state: GomokuState): Int {
        var blackScore = 0
        var whiteScore = 0
        val board = state.board
        val size = GomokuState.SIZE
        val windowSize = 5

        fun evaluateWindow(b: Int, w: Int) {
            if (b > 0 && w > 0) return
            if (b > 0) {
                when (b) {
                    5 -> blackScore += 80000
                    4 -> blackScore += 4500
                    3 -> blackScore += 320
                    2 -> blackScore += 25
                    1 -> blackScore += 1
                }
            } else if (w > 0) {
                when (w) {
                    5 -> whiteScore += 80000
                    4 -> whiteScore += 4500
                    3 -> whiteScore += 320
                    2 -> whiteScore += 25
                    1 -> whiteScore += 1
                }
            }
        }

        // 1. 横向
        for (r in 0 until size) {
            for (c in 0..size - windowSize) {
                var b = 0
                var w = 0
                for (i in 0 until windowSize) {
                    val stone = board[r * size + (c + i)]
                    if (stone == Stone.BLACK) b++ else if (stone == Stone.WHITE) w++
                }
                evaluateWindow(b, w)
            }
        }

        // 2. 纵向
        for (c in 0 until size) {
            for (r in 0..size - windowSize) {
                var b = 0
                var w = 0
                for (i in 0 until windowSize) {
                    val stone = board[(r + i) * size + c]
                    if (stone == Stone.BLACK) b++ else if (stone == Stone.WHITE) w++
                }
                evaluateWindow(b, w)
            }
        }

        // 3. 主对角线 (左上至右下)
        for (r in 0..size - windowSize) {
            for (c in 0..size - windowSize) {
                var b = 0
                var w = 0
                for (i in 0 until windowSize) {
                    val stone = board[(r + i) * size + (c + i)]
                    if (stone == Stone.BLACK) b++ else if (stone == Stone.WHITE) w++
                }
                evaluateWindow(b, w)
            }
        }

        // 4. 副对角线 (右上至左下)
        for (r in 0..size - windowSize) {
            for (c in windowSize - 1 until size) {
                var b = 0
                var w = 0
                for (i in 0 until windowSize) {
                    val stone = board[(r + i) * size + (c - i)]
                    if (stone == Stone.BLACK) b++ else if (stone == Stone.WHITE) w++
                }
                evaluateWindow(b, w)
            }
        }

        val rawDiff = blackScore - whiteScore
        val scaled = (rawDiff * 0.15f).toInt()
        return maxOf(-1000, minOf(1000, scaled))
    }

    private suspend fun ensureEngineReady() {
        engine.initialize()
    }
}

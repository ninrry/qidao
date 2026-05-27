package com.example.chessarena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chessarena.data.GamePreferences
import com.example.chessarena.engine.ChessEngine
import com.example.chessarena.engine.Difficulty
import com.example.chessarena.engine.XiangqiEngine
import com.example.chessarena.game.xiangqi.FenParser
import com.example.chessarena.game.xiangqi.Move
import com.example.chessarena.game.xiangqi.Position
import com.example.chessarena.game.xiangqi.Side
import com.example.chessarena.game.xiangqi.XiangqiRules
import com.example.chessarena.game.xiangqi.XiangqiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 象棋游戏 UI 状态
 *
 * @param gameState 当前棋盘状态（来自游戏逻辑层）
 * @param isAiThinking AI 是否正在思考
 * @param evaluation 当前局面评估分数（厘兵值，正数表示红方优势）
 * @param difficulty 当前难度等级
 * @param showDifficultyDialog 是否显示难度选择对话框
 * @param gameOverMessage 游戏结束信息（null 表示游戏进行中）
 * @param selectedPosition 当前选中的棋子位置
 * @param validMoves 当前选中棋子的合法走法目标位置列表
 * @param lastMove 最后一步走法（用于高亮显示）
 * @param moveHistory 走法历史记录（中文棋谱格式）
 * @param playerSide 玩家执棋方（始终为红方）
 * @param capturedByPlayer 玩家吃掉的棋子列表
 * @param capturedByAi AI 吃掉的棋子列表
 */
data class XiangqiUiState(
    val gameState: XiangqiState? = null,
    val isAiThinking: Boolean = false,
    val evaluation: Int = 0,
    val difficulty: Difficulty = Difficulty.XIANGQI_SENIOR,
    val showDifficultyDialog: Boolean = true,
    val gameOverMessage: String? = null,
    val selectedPosition: Position? = null,
    val validMoves: List<Position> = emptyList(),
    val lastMove: Move? = null,
    val moveHistory: List<String> = emptyList(),
    val playerSide: Side = Side.RED,
    val capturedByPlayer: List<com.example.chessarena.game.xiangqi.Piece> = emptyList(),
    val capturedByAi: List<com.example.chessarena.game.xiangqi.Piece> = emptyList(),
    val isInCheck: Boolean = false,
    val engineHealthError: String? = null
)

/**
 * 象棋游戏 ViewModel
 *
 * 管理象棋游戏的完整生命周期：
 * - 玩家始终执红方（底部），AI 执黑方（顶部）
 * - 处理棋子选择、走法验证、走子动画
 * - 管理 AI 思考流程
 * - 支持悔棋（撤销玩家+AI的两步走法）
 * - 支持认输
 * - 检测将军、将杀、困毙等结束状态
 */
class XiangqiViewModel(
    private val engine: ChessEngine = XiangqiEngine(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val initialDifficulty =
        GamePreferences.loadXiangqiDifficulty() ?: Difficulty.XIANGQI_SENIOR
    private val _uiState = MutableStateFlow(XiangqiUiState(difficulty = initialDifficulty))
    val uiState: StateFlow<XiangqiUiState> = _uiState.asStateFlow()

    /** AI 思考的协程 Job，用于取消 */
    private var aiJob: Job? = null
    private var aiGeneration = 0

    /** 走法历史栈，用于悔棋。每个元素是一个完整的游戏状态快照 */
    private val stateHistory = mutableListOf<XiangqiState>()

    /** 被吃棋子记录 */
    private val capturedByPlayer = mutableListOf<com.example.chessarena.game.xiangqi.Piece>()
    private val capturedByAi = mutableListOf<com.example.chessarena.game.xiangqi.Piece>()

    init {
        // 初始化引擎
        viewModelScope.launch {
            engine.initialize()
        }
    }

    /**
     * 选择难度并开始新游戏
     */
    fun onDifficultySelected(difficulty: Difficulty, playerSide: Side = Side.RED) {
        viewModelScope.launch {
            cancelAiJob()
            ensureEngineReady()
            engine.setDifficulty(difficulty)
            GamePreferences.saveXiangqiDifficulty(difficulty)

            val initialState = XiangqiState.initial()
            stateHistory.clear()
            stateHistory.add(initialState)
            capturedByPlayer.clear()
            capturedByAi.clear()

            _uiState.update {
                XiangqiUiState(
                    gameState = initialState,
                    difficulty = difficulty,
                    showDifficultyDialog = false,
                    playerSide = playerSide,
                    isInCheck = false
                )
            }

            if (playerSide == Side.BLACK) {
                triggerAiMove(initialState)
            }
        }
    }

    /**
     * 处理棋盘位置点击事件
     *
     * 交互逻辑：
     * 1. AI 思考中 → 忽略点击
     * 2. 游戏已结束 → 忽略点击
     * 3. 未选中棋子 + 点击己方棋子 → 选中该棋子，显示合法走法
     * 4. 已选中棋子 + 点击合法目标 → 执行走法，触发 AI
     * 5. 已选中棋子 + 点击其他己方棋子 → 切换选中
     * 6. 已选中棋子 + 点击非法位置 → 取消选中
     */
    fun onPositionClick(position: Position) {
        val currentState = _uiState.value
        val gameState = currentState.gameState ?: return

        // AI 思考中或游戏结束时忽略点击
        if (currentState.isAiThinking || currentState.gameOverMessage != null) return

        // 只有轮到玩家（红方）才能操作
        if (gameState.currentTurn != currentState.playerSide) return

        val clickedPiece = gameState.pieceAt(position)
        val selectedPos = currentState.selectedPosition

        when {
            // 情况1：未选中棋子，点击了己方棋子 → 选中
            selectedPos == null && clickedPiece != null && clickedPiece.side == currentState.playerSide -> {
                selectPiece(position, gameState)
            }

            // 情况2：已选中棋子，点击了合法走法目标 → 执行走法
            selectedPos != null && position in currentState.validMoves -> {
                executePlayerMove(selectedPos, position, gameState)
            }

            // 情况3：已选中棋子，点击了其他己方棋子 → 切换选中
            selectedPos != null && clickedPiece != null && clickedPiece.side == currentState.playerSide -> {
                selectPiece(position, gameState)
            }

            // 情况4：其他情况 → 取消选中
            else -> {
                deselectPiece()
            }
        }
    }

    /**
     * 选中棋子并计算合法走法
     */
    private fun selectPiece(position: Position, gameState: XiangqiState) {
        val legalMoves = XiangqiRules.getLegalMoves(gameState, position)
        _uiState.update {
            it.copy(
                selectedPosition = position,
                validMoves = legalMoves
            )
        }
    }

    /**
     * 取消选中
     */
    private fun deselectPiece() {
        _uiState.update {
            it.copy(
                selectedPosition = null,
                validMoves = emptyList()
            )
        }
    }

    /**
     * 执行玩家走法
     */
    private fun executePlayerMove(from: Position, to: Position, gameState: XiangqiState) {
        // 保存当前状态用于悔棋
        stateHistory.add(gameState)

        // 记录被吃的棋子
        val capturedPiece = gameState.pieceAt(to)
        if (capturedPiece != null) {
            capturedByPlayer.add(capturedPiece)
        }

        // 执行走法
        val newState = XiangqiRules.makeMove(gameState, from, to) ?: return

        // 生成中文走法记录
        val moveNotation = generateMoveNotation(gameState, from, to)

        val movingPiece = gameState.pieceAt(from) ?: return
        // 创建 Move 对象
        val lastMove = Move(from, to, movingPiece, capturedPiece)

        val isInCheck = newState.status == com.example.chessarena.game.xiangqi.GameStatus.CHECK || XiangqiRules.isInCheck(newState, newState.currentTurn)
        _uiState.update {
            it.copy(
                gameState = newState,
                selectedPosition = null,
                validMoves = emptyList(),
                lastMove = lastMove,
                moveHistory = it.moveHistory + moveNotation,
                capturedByPlayer = capturedByPlayer.toList(),
                isInCheck = isInCheck
            )
        }

        // 检查游戏是否结束
        if (checkGameOver(newState)) return

        // 触发 AI 走法
        triggerAiMove(newState)
    }

    /**
     * 触发 AI 走法
     */
    private fun triggerAiMove(gameState: XiangqiState) {
        cancelAiJob()
        val generation = aiGeneration
        aiJob = viewModelScope.launch {
            _uiState.update { it.copy(isAiThinking = true) }

            try {
                ensureEngineReady()
                // 获取当前局面的 FEN 字符串
                val fen = FenParser.generate(gameState)

                // 调用引擎获取最佳走法
                val result = withContext(dispatcher) {
                    engine.getBestMove(fen, emptyList())
                }

                if (generation != aiGeneration) return@launch

                var from: Position? = null
                var to: Position? = null

                // 解析引擎返回的走法
                val positions = XiangqiEngine.uciToPositions(result.bestMove)
                if (positions != null) {
                    from = positions.first
                    to = positions.second
                }

                // ── 黄金决策与着步双重兜底机制 ────────────────────────────────
                var newState: XiangqiState? = null
                if (from != null && to != null) {
                    newState = XiangqiRules.makeMove(gameState, from, to)
                }

                if (newState == null) {
                    // 若引擎推荐的走法意外失效（或无效），从当前所有绝对合规的着步中随机挑选一步作为兜底
                    val fallback = selectFallbackMove(gameState)
                    if (fallback != null) {
                        from = fallback.first
                        to = fallback.second
                        newState = XiangqiRules.makeMove(gameState, from, to)
                    }
                }

                if (newState != null && from != null && to != null) {
                    if (generation != aiGeneration) return@launch
                    // 保存状态用于悔棋
                    stateHistory.add(gameState)

                    // 记录被吃的棋子
                    val capturedPiece = gameState.pieceAt(to)
                    if (capturedPiece != null) {
                        capturedByAi.add(capturedPiece)
                    }

                    // 生成走法记录
                    val moveNotation = generateMoveNotation(gameState, from, to)

                    val movingPiece = gameState.pieceAt(from)
                    if (movingPiece != null) {
                        val lastMove = Move(from, to, movingPiece, capturedPiece)

                        val isInCheck = newState.status == com.example.chessarena.game.xiangqi.GameStatus.CHECK || XiangqiRules.isInCheck(newState, newState.currentTurn)
                        _uiState.update {
                            it.copy(
                                gameState = newState,
                                isAiThinking = false,
                                evaluation = result.evaluation,
                                lastMove = lastMove,
                                moveHistory = it.moveHistory + moveNotation,
                                capturedByAi = capturedByAi.toList(),
                                isInCheck = isInCheck
                            )
                        }

                        // 检查 AI 走后是否游戏结束
                        checkGameOver(newState)
                    } else {
                        // 降级防线：如果找不到移动的棋子，撤销历史记录并重置思考状态
                        _uiState.update { it.copy(isAiThinking = false) }
                    }
                } else {
                    // AI 确实已经全盘无路可走（被将死或困毙）
                    _uiState.update {
                        it.copy(
                            isAiThinking = false,
                            gameOverMessage = "黑方无路可走，红方获胜！"
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (generation != aiGeneration) return@launch
                // 全量 Throwable 捕捉防线，保障任何 Runtime 异常或 Error 下状态机都不会死锁
                _uiState.update {
                    it.copy(
                        isAiThinking = false,
                        gameOverMessage = "对局调度异常: ${e.message ?: "未知错误"}"
                    )
                }
            }
        }
    }

    /**
     * 获取局面唯一特征签名（忽略半步和总步数）
     */
    private fun getBoardSignature(state: XiangqiState): String {
        val fen = FenParser.generate(state)
        val parts = fen.split(" ")
        return if (parts.size >= 2) {
            // 保留棋盘分布和轮到谁走等核心局势信息，抛弃无吃子步数和总步数
            parts.dropLast(2).joinToString(" ")
        } else {
            fen
        }
    }

    /**
     * 检查游戏是否结束
     * @return true 如果游戏已结束
     */
    private fun checkGameOver(state: XiangqiState): Boolean {
        val isCheckmate = XiangqiRules.isCheckmate(state, state.currentTurn)
        val isStalemate = XiangqiRules.isStalemate(state, state.currentTurn)
        val isDraw = state.status == com.example.chessarena.game.xiangqi.GameStatus.DRAW

        // 1. 三次重复局面检测
        val currentSig = getBoardSignature(state)
        val historySigs = stateHistory.map { getBoardSignature(it) }
        val occurrences = historySigs.count { it == currentSig } + 1 // 历史中出现过的次数加上当前的这一次

        if (occurrences >= 3) {
            // 发生三次重复局面！启动长将检测判定
            val recentStates = stateHistory.takeLast(minOf(stateHistory.size, 8)) + state
            
            // 过滤出在这些状态中，红方/黑方落子后的棋局（因为 currentTurn 指向将要下棋的一方）
            // 当 state.currentTurn == Side.BLACK，说明上一步是红方落子
            // 当 state.currentTurn == Side.RED，说明上一步是黑方落子
            val redMoveStates = recentStates.filter { it.currentTurn == Side.BLACK }
            val blackMoveStates = recentStates.filter { it.currentTurn == Side.RED }

            // 检查在各自所有动作之后，对方是否处于被将军状态
            val redChecks = redMoveStates.map { XiangqiRules.isInCheck(it, Side.BLACK) }
            val blackChecks = blackMoveStates.map { XiangqiRules.isInCheck(it, Side.RED) }

            // 长将判定：如果在此重复循环周期中，某一方的每一步棋都在将军且至少将军了 2 次
            val redIsLongChecking = redChecks.isNotEmpty() && redChecks.size >= 2 && redChecks.all { it }
            val blackIsLongChecking = blackChecks.isNotEmpty() && blackChecks.size >= 2 && blackChecks.all { it }

            return when {
                redIsLongChecking && !blackIsLongChecking -> {
                    _uiState.update {
                        it.copy(gameOverMessage = "长将判负！红方连续长将被判为违规，黑方获胜！")
                    }
                    true
                }
                blackIsLongChecking && !redIsLongChecking -> {
                    _uiState.update {
                        it.copy(gameOverMessage = "长将判负！黑方（AI）连续长将被判为违规，红方获胜！")
                    }
                    true
                }
                else -> {
                    _uiState.update {
                        it.copy(gameOverMessage = "对局和棋：双方触发三次重复局面，不变作和。")
                    }
                    true
                }
            }
        }

        return when {
            isCheckmate -> {
                // 被将杀的是 currentTurn 方
                val winner = if (state.currentTurn == Side.RED) "黑方" else "红方"
                val loser = if (state.currentTurn == Side.RED) "红方" else "黑方"
                _uiState.update {
                    it.copy(gameOverMessage = "将杀！${winner}获胜！${loser}被将死。")
                }
                true
            }
            isStalemate -> {
                val stalemateSide = if (state.currentTurn == Side.RED) "红方" else "黑方"
                _uiState.update {
                    it.copy(gameOverMessage = "困毙！${stalemateSide}无子可动，对方获胜。")
                }
                true
            }
            isDraw -> {
                _uiState.update {
                    it.copy(gameOverMessage = "对局和棋：双方连续 60 回合未吃子。")
                }
                true
            }
            else -> {
                false
            }
        }
    }

    /**
     * 开始新游戏 - 显示难度选择对话框
     */
    fun onNewGame() {
        cancelAiJob()
        _uiState.update {
            XiangqiUiState(
                showDifficultyDialog = true,
                difficulty = it.difficulty  // 保留上次选择的难度
            )
        }
    }

    /**
     * 悔棋 - 撤销最近两步（玩家 + AI 各一步）
     *
     * 只有在以下条件满足时才能悔棋：
     * 1. AI 没有在思考
     * 2. 游戏没有结束
     * 3. 历史记录中至少有2步（玩家+AI各一步）
     */
    fun onUndo() {
        val currentState = _uiState.value
        if (currentState.isAiThinking || currentState.gameOverMessage != null) return

        // 至少需要2个历史状态（AI走后的和AI走前的）才能回退两步
        if (stateHistory.size < 2) return

        // 移除最后两个状态（AI的走法 + 玩家的走法）
        stateHistory.removeLastOrNull()
        val previousState = stateHistory.removeLastOrNull() ?: return

        // 同步更新吃子记录
        if (capturedByAi.isNotEmpty()) capturedByAi.removeLastOrNull()
        if (capturedByPlayer.isNotEmpty()) capturedByPlayer.removeLastOrNull()

        // 移除最后两步走法记录
        val newHistory = _uiState.value.moveHistory.let { history ->
            if (history.size >= 2) history.dropLast(2) else emptyList()
        }

        val isInCheck = previousState.status == com.example.chessarena.game.xiangqi.GameStatus.CHECK || XiangqiRules.isInCheck(previousState, previousState.currentTurn)
        _uiState.update {
            it.copy(
                gameState = previousState,
                selectedPosition = null,
                validMoves = emptyList(),
                lastMove = null,
                moveHistory = newHistory,
                capturedByPlayer = capturedByPlayer.toList(),
                capturedByAi = capturedByAi.toList(),
                gameOverMessage = null,
                isInCheck = isInCheck
            )
        }
    }

    /**
     * 认输
     */
    fun onResign() {
        val currentState = _uiState.value
        if (currentState.gameOverMessage != null) return

        cancelAiJob()

        _uiState.update {
            it.copy(
                isAiThinking = false,
                gameOverMessage = "红方认输，黑方获胜！"
            )
        }
    }

    /**
     * 关闭难度选择对话框
     */
    fun onDismissDifficultyDialog() {
        // 只有在已有游戏状态的情况下才允许关闭
        if (_uiState.value.gameState != null) {
            _uiState.update { it.copy(showDifficultyDialog = false) }
        }
    }

    /**
     * 关闭游戏结束提示
     */
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

    // ==================== 中文棋谱生成 ====================

    /** 中文数字 */
    private val chineseNumbers = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
    private val chineseNumbersBlack = arrayOf("０", "１", "２", "３", "４", "５", "６", "７", "８", "９")

    /**
     * 生成中文棋谱走法记录
     *
     * 格式：[棋子名][起始列][动作][目标列或步数]
     * 例如：炮二平五、马八进七、车一进一
     *
     * 红方使用中文数字，从右到左为一到九
     * 黑方使用阿拉伯数字，从右到左为1到9
     */
    private fun generateMoveNotation(state: XiangqiState, from: Position, to: Position): String {
        val piece = state.pieceAt(from) ?: return "??"
        val isRed = piece.side == Side.RED

        // 棋子名称
        val pieceName = piece.displayName.toString()

        // 红方列号从右到左为一到九（col: 8→一, 7→二, ..., 0→九）
        // 黑方列号从右到左为1到9（col: 0→1, 1→2, ..., 8→9）
        val fromCol = if (isRed) {
            chineseNumbers[9 - from.col]
        } else {
            chineseNumbersBlack[from.col + 1]
        }

        // 判断走法方向
        val action: String
        val target: String

        when {
            // 平移（行号不变）
            from.row == to.row -> {
                action = "平"
                target = if (isRed) {
                    chineseNumbers[9 - to.col]
                } else {
                    chineseNumbersBlack[to.col + 1]
                }
            }
            // 红方：行号变小为进，变大为退
            // 黑方：行号变大为进，变小为退
            else -> {
                val isForward = if (isRed) to.row < from.row else to.row > from.row
                action = if (isForward) "进" else "退"

                // 对于直线走子（车、兵、将），目标是目标行列号
                // 对于斜线走子（马、象、士），目标是目标列号
                val isDiagonal = from.col != to.col && from.row != to.row

                // 简化处理：直线走子用步数，斜线走子用列号
                target = if (from.col == to.col) {
                    // 直线前进/后退，用步数
                    val steps = kotlin.math.abs(to.row - from.row)
                    if (isRed) chineseNumbers[steps] else chineseNumbersBlack[steps]
                } else {
                    // 斜线走子（马、象、士），用目标列号
                    if (isRed) {
                        chineseNumbers[9 - to.col]
                    } else {
                        chineseNumbersBlack[to.col + 1]
                    }
                }
            }
        }

        return "$pieceName$fromCol$action$target"
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

    private suspend fun ensureEngineReady() {
        engine.initialize()
    }

    private fun selectFallbackMove(state: XiangqiState): Pair<Position, Position>? {
        val legalMoves = XiangqiRules.getAllLegalMoves(state)
        if (legalMoves.isEmpty()) return null
        return legalMoves.maxByOrNull { (from, to) ->
            scoreFallbackMove(state, from, to)
        }
    }

    private fun scoreFallbackMove(state: XiangqiState, from: Position, to: Position): Int {
        val movingPiece = state.pieceAt(from)
        val captured = state.pieceAt(to)
        val captureScore = (captured?.value ?: 0) * 12
        val forwardSteps = if (movingPiece?.side == Side.RED) from.row - to.row else to.row - from.row
        val advanceScore = forwardSteps.coerceAtLeast(0) * 8
        val centerScore = 24 - (kotlin.math.abs(to.col - 4) + kotlin.math.abs(to.row - 4))

        val nextState = XiangqiRules.makeMove(state, from, to) ?: return Int.MIN_VALUE
        val givesCheck = XiangqiRules.isInCheck(nextState, nextState.currentTurn)
        val checkScore = if (givesCheck) 900 else 0

        return captureScore + checkScore + advanceScore + centerScore
    }
}

package com.example.chessarena.engine

import com.example.chessarena.game.gomoku.GomokuRules
import com.example.chessarena.game.gomoku.GomokuState
import com.example.chessarena.game.gomoku.Stone
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

class GomokuEngine(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ChessEngine {

    private var isInitialized = false
    val isNativeHealthy: Boolean get() = isInitialized && useNative
    private var currentDifficulty = Difficulty.GOMOKU_SENIOR
    @Volatile private var shouldStop = false

    // JNI Native SlowRenju 状态
    private var useNative = false
    @Volatile private var nativeFailureCount = 0
    @Volatile var nativeLibraryLoaded = false
    @Volatile var isNativeEverRan = false

    val isNative: Boolean get() = useNative

    companion object {
        private const val MAX_NATIVE_FAILURES = 3
        private const val MATE = 1000000
        private const val INF = MATE * 2
        private const val SIZE = GomokuState.SIZE
        private const val BOARD_SIZE = GomokuState.BOARD_SIZE
        private val DIRECTIONS = listOf(Pair(1, 0), Pair(0, 1), Pair(1, 1), Pair(1, -1))

        fun parseMove(moveStr: String): Pair<Int, Int>? {
            val parts = moveStr.split(",")
            if (parts.size != 2) return null
            val x = parts[0].trim().toIntOrNull() ?: return null
            val y = parts[1].trim().toIntOrNull() ?: return null
            return Pair(x, y)
        }
    }

    // ==================== ChessEngine interface ====================

    override suspend fun initialize() {
        if (isInitialized) return

        if (!nativeLibraryLoaded) {
            try {
                System.loadLibrary("slowrenju")
                nativeLibraryLoaded = true
                isNativeEverRan = true
                useNative = true
                logD("SlowRenjuJni", "SlowRenju 原生库加载成功！")
            } catch (e: UnsatisfiedLinkError) {
                logE("SlowRenjuJni", "无法加载 SlowRenju 原生库", e)
                useNative = false
            } catch (e: Exception) {
                logE("SlowRenjuJni", "引擎初始化异常", e)
                useNative = false
            }
        }

        isInitialized = true
    }

    override suspend fun setDifficulty(difficulty: Difficulty) {
        require(difficulty in Difficulty.gomokuDifficulties()) {
            "必须使用五子棋难度等级，当前传入: ${difficulty.name}"
        }
        currentDifficulty = difficulty
    }

    override suspend fun getBestMove(fen: String, moves: List<String>): EngineResult = withContext(dispatcher) {
        check(isInitialized) { "引擎尚未初始化" }
        shouldStop = false

        val startTime = System.currentTimeMillis()
        val state = parseBoardToState(fen) ?: buildStateFromMoves(moves)

        // 原生引擎带重试搜索
        if (useNative) {
            val result = searchNativeWithRetry(state)
            if (result != null) {
                return@withContext EngineResult(
                    bestMove = "$result",
                    evaluation = 0,
                    depth = currentDifficulty.engineParam,
                    thinkingTime = System.currentTimeMillis() - startTime
                )
            }
        }

        throw Exception("SlowRenju 原生引擎未能产出着法。引擎状态异常，请检查原生库文件。")
    }

    override suspend fun getEvaluation(fen: String): Int = withContext(dispatcher) {
        check(isInitialized)
        0
    }

    override fun stop() {
        shouldStop = true
        if (useNative && nativeLibraryLoaded) {
            try {
                com.wind23.slowrenju.GlobalValue.stopThink()
            } catch (e: Exception) {
                logE("SlowRenjuJni", "停止引擎思考异常", e)
            }
        }
    }

    override fun destroy() {
        stop()
        isInitialized = false
    }

    // ==================== 原生引擎加固核心 ====================

    /**
     * 带重试的原生搜索：失败 → 重试，最多 MAX_NATIVE_FAILURES 次
     */
    private fun searchNativeWithRetry(state: GomokuState): String? {
        val slowRenjuBoard = buildSlowRenjuBoard(state)
        val fflag = if (state.useRenju) 1 else 0
        val level = currentDifficulty.engineParam

        for (attempt in 0 until MAX_NATIVE_FAILURES) {
            try {
                val ret = com.wind23.slowrenju.GlobalValue.getPos(slowRenjuBoard, fflag, level)
                val bestCol = ret % SIZE
                val bestRow = ret / SIZE

                // 验证返回值合法性
                if (bestCol in 0 until SIZE && bestRow in 0 until SIZE) {
                    nativeFailureCount = 0
                    return "$bestCol,$bestRow"
                }

                logE("SlowRenjuJni", "原生引擎返回越界坐标: col=$bestCol, row=$bestRow (attempt=$attempt)")
            } catch (e: Exception) {
                logE("SlowRenjuJni", "原生引擎搜索异常 (attempt=$attempt)", e)
            }

            nativeFailureCount++

            // 尝试重新加载原生库
            if (attempt < MAX_NATIVE_FAILURES - 1) {
                logD("SlowRenjuJni", "正在重新加载原生库...")
                try {
                    System.loadLibrary("slowrenju")
                    nativeLibraryLoaded = true
                } catch (e: Exception) {
                    logE("SlowRenjuJni", "重新加载原生库失败", e)
                    break
                }
            }
        }

        if (nativeFailureCount >= MAX_NATIVE_FAILURES) {
            logE("SlowRenjuJni", "原生引擎连续失败 $nativeFailureCount 次，禁用原生引擎")
            useNative = false
        }
        return null
    }

    private fun buildSlowRenjuBoard(state: GomokuState): IntArray {
        val board = IntArray(225)
        for (row in 0 until SIZE) {
            for (col in 0 until SIZE) {
                val stone = state.stoneAt(col, row)
                board[col * SIZE + row] = when (stone) {
                    Stone.BLACK -> 1
                    Stone.WHITE -> -1
                    null -> 0
                }
            }
        }
        return board
    }

    private fun logD(tag: String, msg: String) {
        try { android.util.Log.d(tag, msg) } catch (_: Throwable) { println("[$tag] $msg") }
    }

    private fun logE(tag: String, msg: String, tr: Throwable? = null) {
        try { android.util.Log.e(tag, msg, tr) } catch (_: Throwable) { System.err.println("[$tag] $msg"); tr?.printStackTrace() }
    }

    private fun getCandidateMoves(state: GomokuState): List<Pair<Int, Int>> {
        val occupied = mutableListOf<Pair<Int, Int>>()
        for (i in state.board.indices) {
            if (state.board[i] != null) occupied.add(Pair(i % SIZE, i / SIZE))
        }
        if (occupied.isEmpty()) return listOf(Pair(SIZE / 2, SIZE / 2))

        val candidateSet = mutableSetOf<Pair<Int, Int>>()
        for ((col, row) in occupied) {
            for (dc in -3..3) {
                for (dr in -3..3) {
                    val nc = col + dc; val nr = row + dr
                    if (nc in 0 until SIZE && nr in 0 until SIZE && state.stoneAt(nc, nr) == null) {
                        candidateSet.add(Pair(nc, nr))
                    }
                }
            }
        }
        return if (candidateSet.isEmpty()) listOf(Pair(SIZE / 2, SIZE / 2)) else candidateSet.toList()
    }

    // ==================== Kotlin Fallback Search Engine ====================

    private var ktNodeCount = 0L
    private var ktSearchStart = 0L
    private var ktTimeLimit = 0L

    private fun ktTimeout(): Boolean {
        if (shouldStop) return true
        if (ktNodeCount and 255L == 0L) return System.currentTimeMillis() - ktSearchStart > ktTimeLimit
        return false
    }

    private fun kotlinSearch(state: GomokuState, candidates: List<Pair<Int, Int>>): Quadruple<Int, Int, Int, Int> {
        ktNodeCount = 0; ktSearchStart = System.currentTimeMillis()
        ktTimeLimit = currentDifficulty.maxThinkTime
        val maxDepth = currentDifficulty.engineParam.coerceIn(2, 12)
        var bestCol = candidates[0].first; var bestRow = candidates[0].second
        var bestScore = 0; var depthReached = 1
        for (depth in 1..maxDepth) {
            if (ktTimeout()) break
            val result = ktRootSearch(state, candidates, depth)
            if (!ktTimeout()) { bestCol = result.first.first; bestRow = result.first.second; bestScore = result.second; depthReached = depth } else break
        }
        return Quadruple(bestCol, bestRow, bestScore, depthReached)
    }

    private fun ktRootSearch(state: GomokuState, candidates: List<Pair<Int, Int>>, depth: Int): Pair<Pair<Int, Int>, Int> {
        val ordered = candidates.sortedByDescending { (c, r) -> ktMoveScore(state, c, r) }
        var bestMove = ordered.first(); var alpha = -INF
        for ((col, row) in ordered) {
            if (ktTimeout()) break
            if (!GomokuRules.isValidMove(state, col, row)) continue
            val ns = GomokuRules.makeMove(state, col, row)
            if (ns.isGameOver && ns.winner == state.currentTurn) return Pair(col, row) to MATE
            val score = -ktAlphaBeta(ns, depth - 1, -INF, -alpha)
            if (score > alpha) { alpha = score; bestMove = Pair(col, row) }
        }
        return bestMove to alpha
    }

    private fun ktAlphaBeta(state: GomokuState, depth: Int, alpha: Int, beta: Int): Int {
        ktNodeCount++
        if (state.isGameOver) return when (state.winner) { null -> 0; else -> if (state.winner == state.currentTurn) MATE else -MATE }
        if (depth == 0) return ktEvaluate(state)
        val candidates = getCandidateMoves(state)
        if (candidates.isEmpty()) return 0
        var a = alpha; var best = -INF
        for ((col, row) in candidates.sortedByDescending { (c, r) -> ktMoveScore(state, c, r) }) {
            if (!GomokuRules.isValidMove(state, col, row)) continue
            val ns = GomokuRules.makeMove(state, col, row)
            if (ns.isGameOver && ns.winner != null) return MATE
            val score = -ktAlphaBeta(ns, depth - 1, -beta, -a)
            if (score > best) { best = score; if (score > a) a = score; if (a >= beta) return best }
        }
        return best
    }

    private fun ktEvaluate(state: GomokuState): Int {
        if (state.isGameOver) return when (state.winner) { null -> 0; else -> if (state.winner == state.currentTurn) MATE else -MATE }
        var score = 0
        val visited = mutableSetOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
        for (row in 0 until SIZE) {
            for (col in 0 until SIZE) {
                val stone = state.stoneAt(col, row) ?: continue
                for ((dx, dy) in DIRECTIONS) {
                    val key = Pair(Pair(col, row), Pair(dx, dy))
                    if (key in visited) continue
                    val pc = col - dx; val pr = row - dy
                    if (pc in 0 until SIZE && pr in 0 until SIZE && state.stoneAt(pc, pr) == stone) continue
                    var count = 1; var c = col + dx; var r = row + dy
                    while (c in 0 until SIZE && r in 0 until SIZE && state.stoneAt(c, r) == stone) { visited.add(Pair(Pair(c, r), Pair(dx, dy))); count++; c += dx; r += dy }
                    val bo = pc in 0 until SIZE && pr in 0 until SIZE && state.stoneAt(pc, pr) == null
                    val ao = c in 0 until SIZE && r in 0 until SIZE && state.stoneAt(c, r) == null
                    val pv = ktPatternValue(count, bo, ao)
                    if (stone == state.currentTurn) score += pv else score -= pv
                }
            }
        }
        if (state.useRenju && state.currentTurn == Stone.BLACK) score -= GomokuRules.getForbiddenMoves(state).size * 3000
        return score
    }

    private fun ktPatternValue(count: Int, openBefore: Boolean, openAfter: Boolean): Int {
        val oe = (if (openBefore) 1 else 0) + (if (openAfter) 1 else 0)
        return when (count) {
            in 5..15 -> 1000000
            4 -> when (oe) { 2 -> 500000; 1 -> 50000; else -> 0 }
            3 -> when (oe) { 2 -> 10000; 1 -> 1000; else -> 0 }
            2 -> when (oe) { 2 -> 500; 1 -> 100; else -> 0 }
            1 -> oe * 10
            else -> 0
        }
    }

    private fun ktMoveScore(state: GomokuState, col: Int, row: Int): Int {
        val cur = state.currentTurn; val opp = cur.opponent
        var off = 0; var def = 0
        for ((dx, dy) in DIRECTIONS) {
            var count = 1; var ob = false; var oa = false
            var c = col + dx; var r = row + dy
            while (c in 0 until SIZE && r in 0 until SIZE && state.stoneAt(c, r) == cur) { count++; c += dx; r += dy }
            oa = c in 0 until SIZE && r in 0 until SIZE && state.stoneAt(c, r) == null
            c = col - dx; r = row - dy
            while (c in 0 until SIZE && r in 0 until SIZE && state.stoneAt(c, r) == cur) { count++; c -= dx; r -= dy }
            ob = c in 0 until SIZE && r in 0 until SIZE && state.stoneAt(c, r) == null
            off += ktPatternValue(count, ob, oa)
            count = 1
            c = col + dx; r = row + dy
            while (c in 0 until SIZE && r in 0 until SIZE && state.stoneAt(c, r) == opp) { count++; c += dx; r += dy }
            oa = c in 0 until SIZE && r in 0 until SIZE && state.stoneAt(c, r) == null
            c = col - dx; r = row - dy
            while (c in 0 until SIZE && r in 0 until SIZE && state.stoneAt(c, r) == opp) { count++; c -= dx; r -= dy }
            ob = c in 0 until SIZE && r in 0 until SIZE && state.stoneAt(c, r) == null
            def += ktPatternValue(count, ob, oa)
        }
        return off * 3 + def
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    // ==================== Board parsing ====================

    private fun parseBoardToState(boardDescription: String): GomokuState? {
        if (boardDescription.isBlank()) return null
        val rows = boardDescription.split(";")
        if (rows.size != SIZE) return null
        val board = MutableList<Stone?>(BOARD_SIZE) { null }
        var blackCount = 0; var whiteCount = 0
        for ((row, rowText) in rows.withIndex()) {
            val cells = rowText.split(",")
            if (cells.size != SIZE) return null
            for ((col, cellText) in cells.withIndex()) {
                val stone = when (cellText.trim()) {
                    "1" -> { blackCount++; Stone.BLACK }
                    "2" -> { whiteCount++; Stone.WHITE }
                    "0" -> null
                    else -> return null
                }
                board[GomokuState.posToIndex(col, row)] = stone
            }
        }
        val currentTurn = if (blackCount == whiteCount) Stone.BLACK else Stone.WHITE
        return GomokuState(board = board, currentTurn = currentTurn)
    }

    private fun buildStateFromMoves(moves: List<String>): GomokuState {
        val board = MutableList<Stone?>(BOARD_SIZE) { null }
        var currentTurn = Stone.BLACK
        for (move in moves) {
            val parsed = parseMove(move)
            if (parsed != null) {
                board[GomokuState.posToIndex(parsed.first, parsed.second)] = currentTurn
                currentTurn = currentTurn.opponent
            }
        }
        return GomokuState(board = board, currentTurn = currentTurn)
    }
}

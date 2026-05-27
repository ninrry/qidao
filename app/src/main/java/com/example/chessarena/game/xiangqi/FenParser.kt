package com.example.chessarena.game.xiangqi

/**
 * 象棋 FEN 字符串解析器
 *
 * FEN (Forsyth-Edwards Notation) 是一种标准的棋盘局面表示法，
 * 被 UCI 引擎（如 Pikafish）广泛使用。
 *
 * 标准 FEN 格式：
 * [棋盘] [走棋方] [保留] [保留] [半步计数] [回合数]
 *
 * 棋盘字符说明（红方大写，黑方小写）：
 * - K/k: 帅/将 (King/General)
 * - A/a: 仕/士 (Advisor)
 * - B/b: 相/象 (Bishop/Elephant)
 * - N/n: 馬 (kNight/Horse)
 * - R/r: 車 (Rook/Chariot)
 * - C/c: 炮/砲 (Cannon)
 * - P/p: 兵/卒 (Pawn/Soldier)
 * - 数字: 连续空格数
 * - /: 行分隔符
 */
object FenParser {

    /** 象棋初始局面 FEN 字符串 */
    const val INITIAL_FEN = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1"

    /**
     * 解析 FEN 字符串为游戏状态
     *
     * @param fen FEN 字符串
     * @return 对应的 XiangqiState
     * @throws IllegalArgumentException 当 FEN 格式不正确时
     */
    fun parse(fen: String): XiangqiState {
        val parts = fen.trim().split(" ")
        require(parts.isNotEmpty()) { "FEN 字符串不能为空" }

        // 解析棋盘部分
        val boardStr = parts[0]
        val board = parseBoard(boardStr)

        // 解析走棋方（默认红方）
        val currentTurn = if (parts.size > 1) {
            when (parts[1].lowercase()) {
                "w", "r" -> Side.RED   // w=white/red 先手
                "b" -> Side.BLACK
                else -> Side.RED
            }
        } else Side.RED

        // 解析半步计数（默认0）
        val halfMoveClock = if (parts.size > 4) {
            parts[4].toIntOrNull() ?: 0
        } else 0

        // 解析回合数（默认1）
        val fullMoveNumber = if (parts.size > 5) {
            parts[5].toIntOrNull() ?: 1
        } else 1

        return XiangqiState(
            board = board,
            currentTurn = currentTurn,
            halfMoveClock = halfMoveClock,
            fullMoveNumber = fullMoveNumber
        )
    }

    /**
     * 解析 FEN 的棋盘部分
     *
     * @param boardStr 棋盘字符串（如 "rnbakabnr/9/1c5c1/..."）
     * @return 棋盘数组（90格）
     */
    private fun parseBoard(boardStr: String): List<Piece?> {
        val board = MutableList<Piece?>(XiangqiState.BOARD_SIZE) { null }
        val rows = boardStr.split("/")
        require(rows.size == XiangqiState.ROWS) {
            "FEN 棋盘必须有 ${XiangqiState.ROWS} 行，实际有 ${rows.size} 行"
        }

        for (row in rows.indices) {
            var col = 0
            for (ch in rows[row]) {
                if (ch.isDigit()) {
                    // 数字表示连续的空格
                    col += ch.digitToInt()
                } else {
                    require(col < XiangqiState.COLS) {
                        "FEN 第 $row 行列数超出范围：col=$col"
                    }
                    val piece = Piece.fromFenChar(ch)
                    require(piece != null) {
                        "无法识别的 FEN 字符: '$ch' (行=$row, 列=$col)"
                    }
                    board[XiangqiState.posToIndex(col, row)] = piece
                    col++
                }
            }
            require(col == XiangqiState.COLS) {
                "FEN 第 $row 行列数不正确：期望 ${XiangqiState.COLS}，实际 $col"
            }
        }
        return board
    }

    /**
     * 将游戏状态生成 FEN 字符串
     *
     * @param state 当前游戏状态
     * @return 标准 FEN 字符串
     */
    fun generate(state: XiangqiState): String {
        val sb = StringBuilder()

        // 棋盘部分
        for (row in 0 until XiangqiState.ROWS) {
            if (row > 0) sb.append('/')
            var emptyCount = 0
            for (col in 0 until XiangqiState.COLS) {
                val piece = state.pieceAt(col, row)
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(piece.fenChar)
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
        }

        // 走棋方
        sb.append(' ')
        sb.append(if (state.currentTurn == Side.RED) 'w' else 'b')

        // 保留字段
        sb.append(" - -")

        // 半步计数和回合数
        sb.append(' ')
        sb.append(state.halfMoveClock)
        sb.append(' ')
        sb.append(state.fullMoveNumber)

        return sb.toString()
    }

    /**
     * 验证 FEN 字符串是否合法
     *
     * @param fen FEN 字符串
     * @return 是否合法
     */
    fun isValid(fen: String): Boolean {
        return try {
            val state = parse(fen)
            // 验证双方各有且仅有一个将/帅
            val redGenerals = state.getPiecesOf(Side.RED).count { it.second.type == PieceType.GENERAL }
            val blackGenerals = state.getPiecesOf(Side.BLACK).count { it.second.type == PieceType.GENERAL }
            redGenerals == 1 && blackGenerals == 1
        } catch (_: Exception) {
            false
        }
    }
}

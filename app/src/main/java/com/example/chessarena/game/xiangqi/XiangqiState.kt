package com.example.chessarena.game.xiangqi

/**
 * 棋盘坐标位置
 *
 * 象棋棋盘为 9列(col: 0-8) × 10行(row: 0-9) 的格局。
 * 红方在下方（row 7-9），黑方在上方（row 0-2）。
 *
 * @property col 列号 (0-8)，从左到右
 * @property row 行号 (0-9)，从上到下（黑方视角：0在最上面）
 */
data class Position(val col: Int, val row: Int) {
    /** 检查坐标是否在棋盘范围内 */
    val isValid: Boolean get() = col in 0..8 && row in 0..9

    /** 是否在红方半场（河这边，row 5-9） */
    val isRedSide: Boolean get() = row in 5..9

    /** 是否在黑方半场（河那边，row 0-4） */
    val isBlackSide: Boolean get() = row in 0..4

    /** 是否在红方九宫格内 */
    val isInRedPalace: Boolean get() = col in 3..5 && row in 7..9

    /** 是否在黑方九宫格内 */
    val isInBlackPalace: Boolean get() = col in 3..5 && row in 0..2

    /** 检查位置是否在指定方的半场 */
    fun isOnSide(side: Side): Boolean = when (side) {
        Side.RED -> isRedSide
        Side.BLACK -> isBlackSide
    }

    /** 检查位置是否在指定方的九宫格内 */
    fun isInPalace(side: Side): Boolean = when (side) {
        Side.RED -> isInRedPalace
        Side.BLACK -> isInBlackPalace
    }

    /** 向指定方向偏移 */
    fun offset(dc: Int, dr: Int): Position = Position(col + dc, row + dr)

    override fun toString(): String = "($col,$row)"
}

/**
 * 棋步记录
 *
 * 记录一步棋的完整信息，用于悔棋和棋谱记录。
 *
 * @property from 起始位置
 * @property to 目标位置
 * @property piece 移动的棋子
 * @property captured 被吃的棋子（如果有）
 * @property notation 中文着法记录（如"炮二平五"）
 */
data class Move(
    val from: Position,
    val to: Position,
    val piece: Piece,
    val captured: Piece? = null,
    val notation: String = ""
) {
    override fun toString(): String =
        if (notation.isNotEmpty()) notation
        else "${piece.displayName}: $from -> $to${if (captured != null) " 吃${captured.displayName}" else ""}"
}

/**
 * 游戏状态枚举
 */
enum class GameStatus {
    /** 正常对弈中 */
    PLAYING,
    /** 一方被将军 */
    CHECK,
    /** 将杀 - 游戏结束 */
    CHECKMATE,
    /** 困毙 - 无子可走，游戏结束 */
    STALEMATE,
    /** 和棋 */
    DRAW
}

/**
 * 象棋游戏状态（不可变）
 *
 * 使用不可变数据类表示完整的游戏状态。每次操作都返回新的状态实例。
 * 棋盘使用一维数组表示 9×10 的棋盘，索引 = row * 9 + col。
 *
 * @property board 棋盘数组（9×10 = 90格），索引 = row * 9 + col
 * @property currentTurn 当前走棋方
 * @property moveHistory 历史走法列表
 * @property status 当前游戏状态
 * @property selectedPosition 当前选中的棋子位置（UI用）
 * @property validMoves 当前选中棋子的合法走法（UI用）
 * @property lastMove 最后一步棋
 * @property capturedByRed 红方吃掉的棋子列表
 * @property capturedByBlack 黑方吃掉的棋子列表
 * @property halfMoveClock 无吃子步数（用于和棋判定）
 * @property fullMoveNumber 完整回合数
 */
data class XiangqiState(
    val board: List<Piece?>,
    val currentTurn: Side = Side.RED,
    val moveHistory: List<Move> = emptyList(),
    val status: GameStatus = GameStatus.PLAYING,
    val selectedPosition: Position? = null,
    val validMoves: List<Position> = emptyList(),
    val lastMove: Move? = null,
    val capturedByRed: List<Piece> = emptyList(),
    val capturedByBlack: List<Piece> = emptyList(),
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1
) {
    companion object {
        const val COLS = 9
        const val ROWS = 10
        const val BOARD_SIZE = COLS * ROWS // 90

        /** 将 (col, row) 转换为一维索引 */
        fun posToIndex(col: Int, row: Int): Int = row * COLS + col

        /** 将 Position 转换为一维索引 */
        fun posToIndex(pos: Position): Int = pos.row * COLS + pos.col

        /** 将一维索引转换为 Position */
        fun indexToPos(index: Int): Position = Position(index % COLS, index / COLS)

        /** 创建初始对局状态 */
        fun initial(): XiangqiState = FenParser.parse(FenParser.INITIAL_FEN)
    }

    // ============================================================
    // 棋盘访问方法
    // ============================================================

    /** 获取指定位置的棋子 */
    fun pieceAt(pos: Position): Piece? {
        if (!pos.isValid) return null
        return board[posToIndex(pos)]
    }

    /** 获取指定坐标的棋子 */
    fun pieceAt(col: Int, row: Int): Piece? {
        if (col !in 0 until COLS || row !in 0 until ROWS) return null
        return board[posToIndex(col, row)]
    }

    /** 检查指定位置是否有棋子 */
    fun hasPieceAt(pos: Position): Boolean = pieceAt(pos) != null

    /** 检查指定位置是否有己方棋子 */
    fun hasFriendlyPieceAt(pos: Position, side: Side): Boolean =
        pieceAt(pos)?.side == side

    /** 检查指定位置是否有对方棋子 */
    fun hasEnemyPieceAt(pos: Position, side: Side): Boolean {
        val piece = pieceAt(pos) ?: return false
        return piece.side != side
    }

    /** 查找指定方的将/帅位置 */
    fun findGeneral(side: Side): Position? {
        for (i in board.indices) {
            val piece = board[i]
            if (piece != null && piece.type == PieceType.GENERAL && piece.side == side) {
                return indexToPos(i)
            }
        }
        return null
    }

    /** 获取指定方的所有棋子及其位置 */
    fun getPiecesOf(side: Side): List<Pair<Position, Piece>> {
        val result = mutableListOf<Pair<Position, Piece>>()
        for (i in board.indices) {
            val piece = board[i]
            if (piece != null && piece.side == side) {
                result.add(indexToPos(i) to piece)
            }
        }
        return result
    }

    // ============================================================
    // 状态变更方法（返回新实例）
    // ============================================================

    /**
     * 在棋盘上放置/移除棋子，返回新的棋盘列表
     */
    fun withPieceAt(pos: Position, piece: Piece?): List<Piece?> {
        val newBoard = board.toMutableList()
        newBoard[posToIndex(pos)] = piece
        return newBoard
    }

    /**
     * 执行移动并返回新状态（不包含合法性检查）
     *
     * 此方法仅更新棋盘和相关计数器，不检查走法是否合法。
     * 上层应使用 XiangqiRules.makeMove() 来执行合法移动。
     */
    fun applyMove(move: Move): XiangqiState {
        val newBoard = board.toMutableList()
        newBoard[posToIndex(move.from)] = null
        newBoard[posToIndex(move.to)] = move.piece

        val newCapturedByRed = if (move.captured != null && move.piece.side == Side.RED)
            capturedByRed + move.captured else capturedByRed
        val newCapturedByBlack = if (move.captured != null && move.piece.side == Side.BLACK)
            capturedByBlack + move.captured else capturedByBlack

        val newHalfMoveClock = if (move.captured != null) 0 else halfMoveClock + 1
        val newFullMoveNumber = if (currentTurn == Side.BLACK) fullMoveNumber + 1 else fullMoveNumber

        return copy(
            board = newBoard,
            currentTurn = currentTurn.opponent,
            moveHistory = moveHistory + move,
            lastMove = move,
            capturedByRed = newCapturedByRed,
            capturedByBlack = newCapturedByBlack,
            selectedPosition = null,
            validMoves = emptyList(),
            halfMoveClock = newHalfMoveClock,
            fullMoveNumber = newFullMoveNumber
        )
    }

    /** 选中一枚棋子，并计算其合法走法（配合UI使用） */
    fun selectPiece(pos: Position, moves: List<Position>): XiangqiState = copy(
        selectedPosition = pos,
        validMoves = moves
    )

    /** 取消选中 */
    fun deselect(): XiangqiState = copy(
        selectedPosition = null,
        validMoves = emptyList()
    )

    /** 更新游戏状态 */
    fun withStatus(newStatus: GameStatus): XiangqiState = copy(status = newStatus)

    /** 判断游戏是否结束 */
    val isGameOver: Boolean
        get() = status == GameStatus.CHECKMATE || status == GameStatus.STALEMATE || status == GameStatus.DRAW

    /** 获取获胜方（如果有） */
    val winner: Side?
        get() = when (status) {
            GameStatus.CHECKMATE -> currentTurn.opponent // 被将杀的一方是当前走棋方，赢的是对手
            else -> null
        }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.appendLine("  0 1 2 3 4 5 6 7 8")
        for (row in 0 until ROWS) {
            sb.append("$row ")
            for (col in 0 until COLS) {
                val piece = pieceAt(col, row)
                sb.append(piece?.displayName ?: '·')
                if (col < COLS - 1) sb.append(' ')
            }
            sb.appendLine()
            if (row == 4) {
                sb.appendLine("  ＝＝＝＝楚河 汉界＝＝＝＝")
            }
        }
        sb.appendLine("当前走棋方: ${if (currentTurn == Side.RED) "红方" else "黑方"}")
        sb.appendLine("游戏状态: $status")
        return sb.toString()
    }
}

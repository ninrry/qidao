package com.example.chessarena.game.gomoku

/**
 * 五子棋游戏状态枚举
 */
enum class GomokuStatus {
    /** 对弈进行中 */
    PLAYING,
    /** 黑方获胜 */
    BLACK_WIN,
    /** 白方获胜 */
    WHITE_WIN,
    /** 和棋（棋盘下满） */
    DRAW
}

/**
 * 五子棋落子记录
 *
 * @property col 列号 (0-14)
 * @property row 行号 (0-14)
 * @property stone 棋子颜色
 * @property moveNumber 手数（第几步）
 */
data class GomokuMove(
    val col: Int,
    val row: Int,
    val stone: Stone,
    val moveNumber: Int
) {
    override fun toString(): String = "${stone.displayName}($col,$row) #$moveNumber"
}

/**
 * 五子棋游戏状态（不可变）
 *
 * 使用不可变数据类表示完整的游戏状态。标准 15×15 棋盘。
 * 棋盘使用一维数组表示，索引 = row * 15 + col。
 *
 * @property board 棋盘数组（15×15 = 225格）
 * @property currentTurn 当前走棋方
 * @property moveHistory 历史走法列表
 * @property status 游戏状态
 * @property lastMove 最后一步棋的位置
 * @property useRenju 是否启用连珠（Renju）规则
 */
data class GomokuState(
    val board: List<Stone?>,
    val currentTurn: Stone = Stone.BLACK,
    val moveHistory: List<GomokuMove> = emptyList(),
    val status: GomokuStatus = GomokuStatus.PLAYING,
    val lastMove: GomokuMove? = null,
    val useRenju: Boolean = true
) {
    companion object {
        const val SIZE = 15
        const val BOARD_SIZE = SIZE * SIZE // 225

        /** 将 (col, row) 转换为一维索引 */
        fun posToIndex(col: Int, row: Int): Int = row * SIZE + col

        /** 将一维索引转换为 (col, row) */
        fun indexToPos(index: Int): Pair<Int, Int> = (index % SIZE) to (index / SIZE)

        /** 创建空棋盘的初始状态 */
        fun initial(useRenju: Boolean = true): GomokuState = GomokuState(
            board = List(BOARD_SIZE) { null },
            useRenju = useRenju
        )
    }

    // ============================================================
    // 棋盘访问方法
    // ============================================================

    /** 获取指定位置的棋子 */
    fun stoneAt(col: Int, row: Int): Stone? {
        if (col !in 0 until SIZE || row !in 0 until SIZE) return null
        return board[posToIndex(col, row)]
    }

    /** 检查指定位置是否为空 */
    fun isEmpty(col: Int, row: Int): Boolean {
        if (col !in 0 until SIZE || row !in 0 until SIZE) return false
        return board[posToIndex(col, row)] == null
    }

    /** 检查棋盘是否已满 */
    val isBoardFull: Boolean get() = board.none { it == null }

    /** 当前手数 */
    val moveCount: Int get() = moveHistory.size

    /** 判断游戏是否结束 */
    val isGameOver: Boolean
        get() = status != GomokuStatus.PLAYING

    /** 获取获胜方（如果有） */
    val winner: Stone?
        get() = when (status) {
            GomokuStatus.BLACK_WIN -> Stone.BLACK
            GomokuStatus.WHITE_WIN -> Stone.WHITE
            else -> null
        }

    // ============================================================
    // 状态变更方法（返回新实例）
    // ============================================================

    /**
     * 在指定位置落子并返回新状态
     *
     * 此方法仅更新棋盘，不检查走法是否合法。
     * 上层应使用 GomokuRules.makeMove() 来执行合法移动。
     */
    fun applyMove(col: Int, row: Int): GomokuState {
        val newBoard = board.toMutableList()
        newBoard[posToIndex(col, row)] = currentTurn
        val move = GomokuMove(col, row, currentTurn, moveCount + 1)
        return copy(
            board = newBoard,
            currentTurn = currentTurn.opponent,
            moveHistory = moveHistory + move,
            lastMove = move
        )
    }

    /** 更新游戏状态 */
    fun withStatus(newStatus: GomokuStatus): GomokuState = copy(status = newStatus)

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("   ")
        for (col in 0 until SIZE) {
            sb.append("%2c".format('A' + col))
        }
        sb.appendLine()
        for (row in 0 until SIZE) {
            sb.append("%2d ".format(row + 1))
            for (col in 0 until SIZE) {
                val stone = stoneAt(col, row)
                sb.append(
                    when (stone) {
                        Stone.BLACK -> "●"
                        Stone.WHITE -> "○"
                        null -> "·"
                    }
                )
                if (col < SIZE - 1) sb.append(' ')
            }
            sb.appendLine()
        }
        sb.appendLine("当前走棋方: ${currentTurn.displayName}")
        sb.appendLine("手数: $moveCount")
        sb.appendLine("状态: $status")
        return sb.toString()
    }
}

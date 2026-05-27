package com.example.chessarena.game.gomoku

object GomokuRules {

    private val DIRECTIONS = listOf(
        Pair(1, 0),   // 水平
        Pair(0, 1),   // 垂直
        Pair(1, 1),   // 主对角线
        Pair(1, -1)   // 副对角线
    )

    /**
     * 判断落子是否合法
     */
    fun isValidMove(state: GomokuState, col: Int, row: Int): Boolean {
        if (state.isGameOver) return false
        if (col !in 0 until GomokuState.SIZE || row !in 0 until GomokuState.SIZE) return false
        if (!state.isEmpty(col, row)) return false

        // 为了实现黑棋禁手“落子即负”的成熟专业判定，此处不再直接拦截。
        // 改在 makeMove 执行后进行统一的禁手判负结算。
        return true
    }

    /**
     * 执行落子，并自动检查胜负或和棋，返回更新后的状态
     */
    fun makeMove(state: GomokuState, col: Int, row: Int): GomokuState {
        if (!isValidMove(state, col, row)) return state

        // 应用落子
        var nextState = state.applyMove(col, row)

        // 检查胜负
        val lastStone = state.currentTurn
        if (lastStone == Stone.BLACK && state.useRenju && isForbiddenMove(state, col, row)) {
            // 专业连珠规则：黑棋落子在禁手点，直接判白方获胜！
            nextState = nextState.withStatus(GomokuStatus.WHITE_WIN)
        } else if (checkWin(nextState, col, row, lastStone)) {
            val winStatus = if (lastStone == Stone.BLACK) GomokuStatus.BLACK_WIN else GomokuStatus.WHITE_WIN
            nextState = nextState.withStatus(winStatus)
        } else if (nextState.isBoardFull) {
            nextState = nextState.withStatus(GomokuStatus.DRAW)
        }

        return nextState
    }

    /**
     * 检查刚刚落子的一方是否获胜
     */
    fun checkWin(state: GomokuState, col: Int, row: Int, stone: Stone): Boolean {
        for ((dx, dy) in DIRECTIONS) {
            val count = 1 + countInDirection(state, col, row, dx, dy, stone) + countInDirection(state, col, row, -dx, -dy, stone)
            
            if (stone == Stone.WHITE) {
                // 白棋：五连或长连都算赢
                if (count >= 5) return true
            } else {
                // 黑棋：必须是精确的五连，长连是禁手
                if (state.useRenju) {
                    if (count == 5) return true
                } else {
                    if (count >= 5) return true
                }
            }
        }
        return false
    }

    /**
     * 判断黑棋落子是否为禁手
     */
    fun isForbiddenMove(state: GomokuState, col: Int, row: Int): Boolean {
        // 禁手仅针对黑棋
        val stone = Stone.BLACK

        // 1. 长连禁手：六子及以上连线
        if (checkOverline(state, col, row, stone)) return true

        // 2. 四四禁手：同时形成两个及以上的四（包括活四和冲四）
        if (checkDoubleFour(state, col, row, stone)) return true

        // 3. 三三禁手：同时形成两个及以上的活三
        if (checkDoubleThree(state, col, row, stone)) return true

        return false
    }

    /**
     * 统计指定方向上相同颜色棋子的个数（不包括起点自己）
     */
    private fun countInDirection(state: GomokuState, col: Int, row: Int, dx: Int, dy: Int, stone: Stone): Int {
        var count = 0
        var c = col + dx
        var r = row + dy
        while (c in 0 until GomokuState.SIZE && r in 0 until GomokuState.SIZE && state.stoneAt(c, r) == stone) {
            count++
            c += dx
            r += dy
        }
        return count
    }

    /**
     * 检查长连禁手
     */
    private fun checkOverline(state: GomokuState, col: Int, row: Int, stone: Stone): Boolean {
        for ((dx, dy) in DIRECTIONS) {
            val count = 1 + countInDirection(state, col, row, dx, dy, stone) + countInDirection(state, col, row, -dx, -dy, stone)
            if (count > 5) return true
        }
        return false
    }

    /**
     * 检查四四禁手（两个或更多冲四或活四）
     */
    private fun checkDoubleFour(state: GomokuState, col: Int, row: Int, stone: Stone): Boolean {
        var fourCount = 0
        // 在该点虚拟落子
        val tempBoard = state.board.toMutableList()
        tempBoard[GomokuState.posToIndex(col, row)] = stone
        val tempState = state.copy(board = tempBoard)

        for ((dx, dy) in DIRECTIONS) {
            if (hasFourInLine(tempState, col, row, dx, dy, stone)) {
                fourCount++
                if (fourCount >= 2) return true
            }
        }
        return false
    }

    /**
     * 检查在该方向上是否存在一个"四"（即如果落一个子，能形成五连）
     */
    private fun hasFourInLine(state: GomokuState, col: Int, row: Int, dx: Int, dy: Int, stone: Stone): Boolean {
        // 在当前线上寻找所有可能的5子连续窗口，如果窗口内有4个己方棋子和1个空位，则说明有四
        for (i in -4..0) {
            var stoneCount = 0
            var emptyCount = 0
            var emptyCol = -1
            var emptyRow = -1
            var outOfBounds = false

            for (j in 0..4) {
                val c = col + (i + j) * dx
                val r = row + (i + j) * dy
                if (c !in 0 until GomokuState.SIZE || r !in 0 until GomokuState.SIZE) {
                    outOfBounds = true
                    break
                }
                val s = state.stoneAt(c, r)
                if (s == stone) {
                    stoneCount++
                } else if (s == null) {
                    emptyCount++
                    emptyCol = c
                    emptyRow = r
                } else {
                    outOfBounds = true
                    break
                }
            }

            if (!outOfBounds && stoneCount == 4 && emptyCount == 1) {
                // 如果空位填上可以形成五连（黑棋非禁手五连，或白棋任意五连），这算是一个合法的四
                val testBoard = state.board.toMutableList()
                testBoard[GomokuState.posToIndex(emptyCol, emptyRow)] = stone
                val testState = state.copy(board = testBoard)
                if (checkWin(testState, emptyCol, emptyRow, stone)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 检查三三禁手（两个或更多活三）
     */
    private fun checkDoubleThree(state: GomokuState, col: Int, row: Int, stone: Stone): Boolean {
        var threeCount = 0
        // 在该点虚拟落子
        val tempBoard = state.board.toMutableList()
        tempBoard[GomokuState.posToIndex(col, row)] = stone
        val tempState = state.copy(board = tempBoard)

        for ((dx, dy) in DIRECTIONS) {
            if (isLiveThree(tempState, col, row, dx, dy, stone)) {
                threeCount++
                if (threeCount >= 2) return true
            }
        }
        return false
    }

    /**
     * 检查指定线上当前落子是否形成了"活三"
     * 活三：可以通过添加一个子变成"活四"的三。
     * 活四：两端都是空的且都可以形成五连的四。
     */
    private fun isLiveThree(state: GomokuState, col: Int, row: Int, dx: Int, dy: Int, stone: Stone): Boolean {
        // 在当前线上寻找所有可能的6个格子的窗口。
        // 如果我们发现一个空位，在此空位落子后，可以使得该方向上成为一个"活四"，则原状态是"活三"。
        for (i in -4..0) {
            // 检查5个格子的范围
            var stoneCount = 0
            var emptyPositions = mutableListOf<Pair<Int, Int>>()
            var outOfBounds = false

            for (j in 0..4) {
                val c = col + (i + j) * dx
                val r = row + (i + j) * dy
                if (c !in 0 until GomokuState.SIZE || r !in 0 until GomokuState.SIZE) {
                    outOfBounds = true
                    break
                }
                val s = state.stoneAt(c, r)
                if (s == stone) {
                    stoneCount++
                } else if (s == null) {
                    emptyPositions.add(Pair(c, r))
                } else {
                    outOfBounds = true
                    break
                }
            }

            if (!outOfBounds && stoneCount == 3 && emptyPositions.size == 2) {
                // 如果恰有3个棋子和2个空位。
                // 必须在其中一个空位落子可以形成一个"活四"
                for ((ec, er) in emptyPositions) {
                    val testBoard = state.board.toMutableList()
                    testBoard[GomokuState.posToIndex(ec, er)] = stone
                    val testState = state.copy(board = testBoard)
                    
                    // 检查 ec, er 处落子后，是否是活四
                    if (isLiveFour(testState, ec, er, dx, dy, stone)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * 检查在 (col, row) 位置落子后在 (dx, dy) 方向是否形成"活四"
     * 活四：四个子连续，两端为空，且两端落子都可以获胜（即两端均不是禁手）。
     */
    private fun isLiveFour(state: GomokuState, col: Int, row: Int, dx: Int, dy: Int, stone: Stone): Boolean {
        // 寻找包含这格的5个格子的区间，使得这5格里有4个己方，1个空，且这个空是在边缘。
        // 或者说，存在一个4个连续的己方棋子，两端都是空的。
        for (i in -3..0) {
            var allStones = true
            // 四个连续的己方棋子
            for (j in 0..3) {
                val c = col + (i + j) * dx
                val r = row + (i + j) * dy
                if (c !in 0 until GomokuState.SIZE || r !in 0 until GomokuState.SIZE || state.stoneAt(c, r) != stone) {
                    allStones = false
                    break
                }
            }

            if (allStones) {
                // 四子连续了！检查两端是否为空
                val leftCol = col + (i - 1) * dx
                val leftRow = row + (i - 1) * dy
                val rightCol = col + (i + 4) * dx
                val rightRow = row + (i + 4) * dy

                if (leftCol in 0 until GomokuState.SIZE && leftRow in 0 until GomokuState.SIZE && state.stoneAt(leftCol, leftRow) == null &&
                    rightCol in 0 until GomokuState.SIZE && rightRow in 0 until GomokuState.SIZE && state.stoneAt(rightCol, rightRow) == null) {
                    
                    // 还要确保两端落子不属于禁手（即两端落子都可以成五连）
                    val testBoardL = state.board.toMutableList()
                    testBoardL[GomokuState.posToIndex(leftCol, leftRow)] = stone
                    val testStateL = state.copy(board = testBoardL)
                    val winL = checkWin(testStateL, leftCol, leftRow, stone)

                    val testBoardR = state.board.toMutableList()
                    testBoardR[GomokuState.posToIndex(rightCol, rightRow)] = stone
                    val testStateR = state.copy(board = testBoardR)
                    val winR = checkWin(testStateR, rightCol, rightRow, stone)

                    if (winL && winR) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * 辅助方法：获取给定状态下黑棋的所有禁手点
     */
    fun getForbiddenMoves(state: GomokuState): List<Pair<Int, Int>> {
        if (state.currentTurn != Stone.BLACK || !state.useRenju || state.isGameOver) return emptyList()

        // 收集盘面上所有已有棋子，如果完全没有棋子，不可能存在禁手
        val occupied = mutableListOf<Pair<Int, Int>>()
        for (i in state.board.indices) {
            if (state.board[i] != null) {
                occupied.add(GomokuState.indexToPos(i))
            }
        }
        if (occupied.isEmpty()) return emptyList()

        val forbidden = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until GomokuState.SIZE) {
            for (c in 0 until GomokuState.SIZE) {
                if (state.isEmpty(c, r)) {
                    // 局部优化：只有与任何已有落子的切比雪夫距离（Chebyshev distance）在 2 格以内的空格才有可能构成禁手
                    // 如果空点离所有棋子都很远，说明它处于绝对空旷的地区，绝无可能形成三三、四四或长连，直接跳过计算！
                    val hasNeighbor = occupied.any { (oc, or) ->
                        kotlin.math.abs(oc - c) <= 2 && kotlin.math.abs(or - r) <= 2
                    }
                    if (!hasNeighbor) continue

                    if (isForbiddenMove(state, c, r)) {
                        forbidden.add(Pair(r, c))
                    }
                }
            }
        }
        return forbidden
    }
}

package com.example.chessarena.game.xiangqi

/**
 * 象棋规则引擎
 *
 * 实现完整的中国象棋规则，包括：
 * - 七种棋子的走法生成（帅/将、仕/士、相/象、馬、車、砲、兵/卒）
 * - 合法走法验证（不能送将）
 * - 将军检测
 * - 将杀检测（无子可走且被将军）
 * - 困毙检测（无子可走但未被将军）
 * - 飞将规则（双将不能在同列且中间无子时对面）
 *
 * 坐标系说明：
 * - col: 0-8（从左到右）
 * - row: 0-9（从上到下，0为黑方底线，9为红方底线）
 * - 红方在下方 (row 5-9)，黑方在上方 (row 0-4)
 * - 红方前进方向为 row 减小，黑方前进方向为 row 增大
 */
object XiangqiRules {

    // ============================================================
    // 主要公开接口
    // ============================================================

    /**
     * 获取指定位置棋子的所有合法走法
     *
     * 过滤掉会导致己方被将军的走法（包含飞将检测）。
     *
     * @param state 当前游戏状态
     * @param pos 棋子位置
     * @return 合法的目标位置列表
     */
    fun getLegalMoves(state: XiangqiState, pos: Position): List<Position> {
        val piece = state.pieceAt(pos) ?: return emptyList()
        if (piece.side != state.currentTurn) return emptyList()

        val pseudoMoves = getPseudoLegalMoves(state, pos, piece)
        return pseudoMoves.filter { to ->
            !wouldBeInCheck(state, pos, to, piece.side)
        }
    }

    /**
     * 执行一步棋
     *
     * @param state 当前游戏状态
     * @param from 起始位置
     * @param to 目标位置
     * @return 新的游戏状态，如果走法不合法则返回 null
     */
    fun makeMove(state: XiangqiState, from: Position, to: Position): XiangqiState? {
        val piece = state.pieceAt(from) ?: return null
        if (piece.side != state.currentTurn) return null

        val legalMoves = getLegalMoves(state, from)
        if (to !in legalMoves) return null

        val captured = state.pieceAt(to)
        val move = Move(
            from = from,
            to = to,
            piece = piece,
            captured = captured,
            notation = generateNotation(state, from, to, piece)
        )

        var newState = state.applyMove(move)

        // 检测游戏状态
        newState = updateGameStatus(newState)

        return newState
    }

    /**
     * 检测指定方是否被将军
     *
     * @param state 当前棋盘状态
     * @param side 被检测的一方
     * @return 是否被将军
     */
    fun isInCheck(state: XiangqiState, side: Side): Boolean {
        val generalPos = state.findGeneral(side) ?: return true // 找不到将/帅视为被将杀

        // 检查是否有对方棋子能攻击到将/帅的位置
        return isPositionAttacked(state, generalPos, side.opponent)
    }

    /**
     * 检测指定方是否被将杀（无合法走法且被将军）
     */
    fun isCheckmate(state: XiangqiState, side: Side): Boolean {
        if (!isInCheck(state, side)) return false
        return !hasAnyLegalMove(state, side)
    }

    /**
     * 检测指定方是否被困毙（无合法走法但未被将军）
     */
    fun isStalemate(state: XiangqiState, side: Side): Boolean {
        if (isInCheck(state, side)) return false
        return !hasAnyLegalMove(state, side)
    }

    /**
     * 检测飞将（两将在同列且中间无子）
     *
     * @param state 当前棋盘状态
     * @return 是否存在飞将
     */
    fun isFlyingGenerals(state: XiangqiState): Boolean {
        val redGeneral = state.findGeneral(Side.RED) ?: return false
        val blackGeneral = state.findGeneral(Side.BLACK) ?: return false

        // 两将必须在同一列
        if (redGeneral.col != blackGeneral.col) return false

        // 检查两将之间是否有其他棋子
        val minRow = minOf(redGeneral.row, blackGeneral.row) + 1
        val maxRow = maxOf(redGeneral.row, blackGeneral.row)
        for (row in minRow until maxRow) {
            if (state.pieceAt(redGeneral.col, row) != null) return false
        }

        // 同列且中间无子 → 飞将
        return true
    }

    // ============================================================
    // 伪合法走法生成（不考虑送将）
    // ============================================================

    /**
     * 获取棋子的伪合法走法（不考虑是否会导致己方被将军）
     */
    private fun getPseudoLegalMoves(state: XiangqiState, pos: Position, piece: Piece): List<Position> {
        return when (piece.type) {
            PieceType.GENERAL -> getGeneralMoves(state, pos, piece.side)
            PieceType.ADVISOR -> getAdvisorMoves(state, pos, piece.side)
            PieceType.ELEPHANT -> getElephantMoves(state, pos, piece.side)
            PieceType.HORSE -> getHorseMoves(state, pos, piece.side)
            PieceType.CHARIOT -> getChariotMoves(state, pos, piece.side)
            PieceType.CANNON -> getCannonMoves(state, pos, piece.side)
            PieceType.SOLDIER -> getSoldierMoves(state, pos, piece.side)
        }
    }

    /**
     * 帅/将的走法
     *
     * 规则：
     * 1. 只能在九宫格内移动
     * 2. 每步只能走一格（横或竖）
     * 3. 不能走到有己方棋子的位置
     */
    private fun getGeneralMoves(state: XiangqiState, pos: Position, side: Side): List<Position> {
        val moves = mutableListOf<Position>()
        val directions = listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0) // 上、下、左、右

        for ((dc, dr) in directions) {
            val target = pos.offset(dc, dr)
            if (target.isValid && target.isInPalace(side) && !state.hasFriendlyPieceAt(target, side)) {
                moves.add(target)
            }
        }

        return moves
    }

    /**
     * 仕/士的走法
     *
     * 规则：
     * 1. 只能在九宫格内移动
     * 2. 每步沿对角线走一格
     * 3. 不能走到有己方棋子的位置
     */
    private fun getAdvisorMoves(state: XiangqiState, pos: Position, side: Side): List<Position> {
        val moves = mutableListOf<Position>()
        val diagonals = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)

        for ((dc, dr) in diagonals) {
            val target = pos.offset(dc, dr)
            if (target.isValid && target.isInPalace(side) && !state.hasFriendlyPieceAt(target, side)) {
                moves.add(target)
            }
        }

        return moves
    }

    /**
     * 相/象的走法
     *
     * 规则：
     * 1. 走"田"字对角线（斜走两格）
     * 2. 不能过河
     * 3. 象眼（田字中心）不能有棋子阻挡（塞象眼）
     * 4. 不能走到有己方棋子的位置
     */
    private fun getElephantMoves(state: XiangqiState, pos: Position, side: Side): List<Position> {
        val moves = mutableListOf<Position>()
        // 四个方向的田字走法：(目标偏移, 象眼偏移)
        val elephantMoves = listOf(
            Pair(2, 2) to Pair(1, 1),
            Pair(2, -2) to Pair(1, -1),
            Pair(-2, 2) to Pair(-1, 1),
            Pair(-2, -2) to Pair(-1, -1)
        )

        for ((targetOffset, eyeOffset) in elephantMoves) {
            val target = pos.offset(targetOffset.first, targetOffset.second)
            val eye = pos.offset(eyeOffset.first, eyeOffset.second)

            if (target.isValid
                && target.isOnSide(side)       // 不能过河
                && !state.hasPieceAt(eye)       // 象眼不能被堵
                && !state.hasFriendlyPieceAt(target, side)  // 目标不能有己方棋子
            ) {
                moves.add(target)
            }
        }

        return moves
    }

    /**
     * 馬的走法
     *
     * 规则：
     * 1. 走"日"字（先直走一格再斜走一格）
     * 2. 蹩马腿：直走一格的位置不能有棋子阻挡
     * 3. 不能走到有己方棋子的位置
     *
     * 马走日字的8个方向及对应的蹩腿位置：
     * 例如向上走（先直上一格到蹩腿点），再斜走到左上或右上
     */
    private fun getHorseMoves(state: XiangqiState, pos: Position, side: Side): List<Position> {
        val moves = mutableListOf<Position>()
        // (目标col偏移, 目标row偏移, 蹩腿col偏移, 蹩腿row偏移)
        val horseMoves = listOf(
            intArrayOf(-1, -2, 0, -1),  // 上左
            intArrayOf(1, -2, 0, -1),   // 上右
            intArrayOf(-1, 2, 0, 1),    // 下左
            intArrayOf(1, 2, 0, 1),     // 下右
            intArrayOf(-2, -1, -1, 0),  // 左上
            intArrayOf(-2, 1, -1, 0),   // 左下
            intArrayOf(2, -1, 1, 0),    // 右上
            intArrayOf(2, 1, 1, 0)      // 右下
        )

        for (m in horseMoves) {
            val target = pos.offset(m[0], m[1])
            val leg = pos.offset(m[2], m[3])

            if (target.isValid
                && !state.hasPieceAt(leg)       // 蹩马腿检查
                && !state.hasFriendlyPieceAt(target, side)  // 目标不能有己方棋子
            ) {
                moves.add(target)
            }
        }

        return moves
    }

    /**
     * 車的走法
     *
     * 规则：
     * 1. 沿直线（横或竖）移动，不限格数
     * 2. 不能越过其他棋子
     * 3. 可以吃掉路径上遇到的第一个对方棋子
     */
    private fun getChariotMoves(state: XiangqiState, pos: Position, side: Side): List<Position> {
        val moves = mutableListOf<Position>()
        val directions = listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0) // 上、下、左、右

        for ((dc, dr) in directions) {
            var current = pos.offset(dc, dr)
            while (current.isValid) {
                val targetPiece = state.pieceAt(current)
                if (targetPiece == null) {
                    // 空位，可以走
                    moves.add(current)
                } else if (targetPiece.side != side) {
                    // 对方棋子，可以吃，然后停止
                    moves.add(current)
                    break
                } else {
                    // 己方棋子，阻挡，停止
                    break
                }
                current = current.offset(dc, dr)
            }
        }

        return moves
    }

    /**
     * 砲/炮的走法
     *
     * 规则：
     * 1. 不吃子时沿直线移动（和车一样）
     * 2. 吃子时必须隔一个棋子（炮架/炮台），跳过炮架后吃掉对方棋子
     * 3. 不能连续越过两个或以上的棋子
     */
    private fun getCannonMoves(state: XiangqiState, pos: Position, side: Side): List<Position> {
        val moves = mutableListOf<Position>()
        val directions = listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0)

        for ((dc, dr) in directions) {
            var current = pos.offset(dc, dr)
            var foundPlatform = false // 是否已找到炮架

            while (current.isValid) {
                val targetPiece = state.pieceAt(current)
                if (!foundPlatform) {
                    // 还没找到炮架
                    if (targetPiece == null) {
                        // 空位，可以移动到（不吃子）
                        moves.add(current)
                    } else {
                        // 找到炮架
                        foundPlatform = true
                    }
                } else {
                    // 已经有炮架了，寻找可以吃的对方棋子
                    if (targetPiece != null) {
                        if (targetPiece.side != side) {
                            // 对方棋子，可以吃
                            moves.add(current)
                        }
                        // 无论吃还是不吃，遇到棋子就停止
                        break
                    }
                }
                current = current.offset(dc, dr)
            }
        }

        return moves
    }

    /**
     * 兵/卒的走法
     *
     * 规则：
     * 1. 未过河前：只能向前走一步
     * 2. 过河后：可以向前、向左、向右各走一步（不能后退）
     *
     * 红方前进方向为 row 减小（向上），黑方前进方向为 row 增大（向下）
     */
    private fun getSoldierMoves(state: XiangqiState, pos: Position, side: Side): List<Position> {
        val moves = mutableListOf<Position>()
        val forwardDr = if (side == Side.RED) -1 else 1
        val hasCrossedRiver = !pos.isOnSide(side)

        // 向前走一步（始终可以）
        val forward = pos.offset(0, forwardDr)
        if (forward.isValid && !state.hasFriendlyPieceAt(forward, side)) {
            moves.add(forward)
        }

        // 过河后可以左右走
        if (hasCrossedRiver) {
            val left = pos.offset(-1, 0)
            if (left.isValid && !state.hasFriendlyPieceAt(left, side)) {
                moves.add(left)
            }
            val right = pos.offset(1, 0)
            if (right.isValid && !state.hasFriendlyPieceAt(right, side)) {
                moves.add(right)
            }
        }

        return moves
    }

    // ============================================================
    // 将军与合法性检测
    // ============================================================

    /**
     * 检测某个位置是否被指定方的棋子攻击
     *
     * @param state 当前棋盘状态
     * @param target 被检测的位置
     * @param attacker 攻击方
     * @return 是否被攻击
     */
    private fun isPositionAttacked(state: XiangqiState, target: Position, attacker: Side): Boolean {
        // 遍历攻击方的所有棋子，检查是否有棋子能走到 target
        for (i in state.board.indices) {
            val piece = state.board[i] ?: continue
            if (piece.side != attacker) continue

            val pos = XiangqiState.indexToPos(i)
            val pseudoMoves = getPseudoLegalMoves(state, pos, piece)
            if (target in pseudoMoves) return true
        }
        return false
    }

    /**
     * 模拟走一步棋后检测是否会导致己方被将军
     *
     * 也检测走后是否违反飞将规则（对走棋方不利的飞将）。
     *
     * @param state 当前状态
     * @param from 起始位置
     * @param to 目标位置
     * @param side 走棋方
     * @return 走棋后是否被将军（true=不合法）
     */
    private fun wouldBeInCheck(state: XiangqiState, from: Position, to: Position, side: Side): Boolean {
        // 模拟走棋
        val newBoard = state.board.toMutableList()
        val piece = newBoard[XiangqiState.posToIndex(from)]
        newBoard[XiangqiState.posToIndex(from)] = null
        newBoard[XiangqiState.posToIndex(to)] = piece

        val tempState = state.copy(board = newBoard)

        // 检查走棋后己方将/帅是否被将军
        if (isInCheck(tempState, side)) return true

        // 检查飞将规则
        if (isFlyingGenerals(tempState)) return true

        return false
    }

    /**
     * 检查指定方是否还有合法走法
     */
    private fun hasAnyLegalMove(state: XiangqiState, side: Side): Boolean {
        val tempState = state.copy(currentTurn = side)
        for ((pos, _) in state.getPiecesOf(side)) {
            if (getLegalMovesInternal(tempState, pos).isNotEmpty()) {
                return true
            }
        }
        return false
    }

    /**
     * 内部版本的合法走法计算（不检查 currentTurn）
     */
    private fun getLegalMovesInternal(state: XiangqiState, pos: Position): List<Position> {
        val piece = state.pieceAt(pos) ?: return emptyList()
        val pseudoMoves = getPseudoLegalMoves(state, pos, piece)
        return pseudoMoves.filter { to ->
            !wouldBeInCheck(state, pos, to, piece.side)
        }
    }

    // ============================================================
    // 游戏状态更新
    // ============================================================

    /**
     * 根据当前局面更新游戏状态
     */
    private fun updateGameStatus(state: XiangqiState): XiangqiState {
        val currentSide = state.currentTurn

        val inCheck = isInCheck(state, currentSide)
        val hasLegalMoves = hasAnyLegalMove(state, currentSide)

        return when {
            // 被将军且无合法走法 → 将杀
            inCheck && !hasLegalMoves -> state.withStatus(GameStatus.CHECKMATE)
            // 未被将军但无合法走法 → 困毙
            !inCheck && !hasLegalMoves -> state.withStatus(GameStatus.STALEMATE)
            // 被将军但还有走法 → 将军状态
            inCheck -> state.withStatus(GameStatus.CHECK)
            // 和棋判定（60回合无吃子，即120半步）
            state.halfMoveClock >= 120 -> state.withStatus(GameStatus.DRAW)
            // 正常对弈
            else -> state.withStatus(GameStatus.PLAYING)
        }
    }

    // ============================================================
    // 中文着法记谱
    // ============================================================

    /** 中文数字（红方用中文，黑方用阿拉伯数字） */
    private val chineseNumbers = charArrayOf('零', '一', '二', '三', '四', '五', '六', '七', '八', '九')

    /**
     * 生成中文着法字符串
     *
     * 格式：[棋子名][列号][动作][目标]
     * - 列号：红方从右到左为一~九，黑方从右到左为1~9
     * - 动作：进（前进）、退（后退）、平（平移）
     * - 目标：进退时为步数，平移时为目标列号
     *
     * @param state 走棋前的状态
     * @param from 起始位置
     * @param to 目标位置
     * @param piece 移动的棋子
     * @return 中文着法字符串
     */
    private fun generateNotation(state: XiangqiState, from: Position, to: Position, piece: Piece): String {
        val sb = StringBuilder()
        val side = piece.side

        // 检查同列是否有相同的棋子（前/后标注）
        val sameTypeSameCol = findSameTypeSameCol(state, from, piece)
        if (sameTypeSameCol != null) {
            // 需要用"前"/"后"来区分
            val isFirstInForward = if (side == Side.RED) from.row < sameTypeSameCol.row
            else from.row > sameTypeSameCol.row
            sb.append(if (isFirstInForward) '前' else '后')
            sb.append(piece.displayName)
        } else {
            sb.append(piece.displayName)
            sb.append(colToNotation(from.col, side))
        }

        // 动作和目标
        val rowDiff = to.row - from.row
        val isForward = if (side == Side.RED) rowDiff < 0 else rowDiff > 0

        if (from.col == to.col) {
            // 直线前进或后退
            sb.append(if (isForward) '进' else '退')
            sb.append(stepsToNotation(kotlin.math.abs(rowDiff), side))
        } else if (from.row == to.row) {
            // 平移
            sb.append('平')
            sb.append(colToNotation(to.col, side))
        } else {
            // 斜线走法（马、象、士）
            sb.append(if (isForward) '进' else '退')
            sb.append(colToNotation(to.col, side))
        }

        return sb.toString()
    }

    /**
     * 列号转记谱表示
     * 红方：从右到左为一~九（col 8 对应"一"，col 0 对应"九"）
     * 黑方：从右到左为1~9（col 8 对应"1"，col 0 对应"9"）
     */
    private fun colToNotation(col: Int, side: Side): Char {
        val num = if (side == Side.RED) 9 - col else col + 1
        return if (side == Side.RED) chineseNumbers[num] else ('0' + num)
    }

    /**
     * 步数转记谱表示
     */
    private fun stepsToNotation(steps: Int, side: Side): Char {
        return if (side == Side.RED) chineseNumbers[steps] else ('0' + steps)
    }

    /**
     * 查找同列同类型的另一个棋子（用于"前/后"标注）
     */
    private fun findSameTypeSameCol(state: XiangqiState, pos: Position, piece: Piece): Position? {
        for (row in 0 until XiangqiState.ROWS) {
            if (row == pos.row) continue
            val other = state.pieceAt(pos.col, row)
            if (other != null && other.type == piece.type && other.side == piece.side) {
                return Position(pos.col, row)
            }
        }
        return null
    }

    // ============================================================
    // 工具方法
    // ============================================================

    /**
     * 获取所有合法走法（包含所有棋子）
     *
     * @param state 当前游戏状态
     * @return 所有合法走法的列表 (from, to)
     */
    fun getAllLegalMoves(state: XiangqiState): List<Pair<Position, Position>> {
        val allMoves = mutableListOf<Pair<Position, Position>>()
        for ((pos, _) in state.getPiecesOf(state.currentTurn)) {
            val moves = getLegalMoves(state, pos)
            for (to in moves) {
                allMoves.add(pos to to)
            }
        }
        return allMoves
    }

    /**
     * 验证一步走法是否合法
     */
    fun isLegalMove(state: XiangqiState, from: Position, to: Position): Boolean {
        val piece = state.pieceAt(from) ?: return false
        if (piece.side != state.currentTurn) return false
        return to in getLegalMoves(state, from)
    }

    /**
     * 统计指定方被将军的攻击来源
     *
     * @return 正在将军的棋子位置列表
     */
    fun getCheckingPieces(state: XiangqiState, side: Side): List<Position> {
        val generalPos = state.findGeneral(side) ?: return emptyList()
        val checkers = mutableListOf<Position>()

        for (i in state.board.indices) {
            val piece = state.board[i] ?: continue
            if (piece.side == side) continue

            val pos = XiangqiState.indexToPos(i)
            val pseudoMoves = getPseudoLegalMoves(state, pos, piece)
            if (generalPos in pseudoMoves) {
                checkers.add(pos)
            }
        }

        return checkers
    }

    /**
     * 评估局面材料分
     *
     * 正值表示红方优势，负值表示黑方优势
     */
    fun evaluateMaterial(state: XiangqiState): Int {
        var score = 0
        for (i in state.board.indices) {
            val piece = state.board[i] ?: continue
            score += if (piece.side == Side.RED) piece.value else -piece.value
        }
        return score
    }
}

package com.example.chessarena.game.xiangqi

/**
 * 象棋棋子类型枚举
 *
 * 定义了象棋中所有7种棋子类型，每种棋子红黑双方各有不同的中文名称。
 *
 * @property redName 红方棋子名称
 * @property blackName 黑方棋子名称
 * @property value 棋子价值（用于AI评估）
 */
enum class PieceType(
    val redName: Char,
    val blackName: Char,
    val value: Int
) {
    /** 帅/将 - 主帅，不能离开九宫格，每步只能走一格（横或竖） */
    GENERAL('帅', '将', 10000),

    /** 仕/士 - 护卫，只能在九宫格内沿对角线走一格 */
    ADVISOR('仕', '士', 200),

    /** 相/象 - 走"田"字对角线，不能过河，有塞象眼限制 */
    ELEPHANT('相', '象', 250),

    /** 馬 - 走"日"字，有蹩马腿限制 */
    HORSE('馬', '馬', 450),

    /** 車 - 直线行走，不限格数（横或竖），不能越子 */
    CHARIOT('車', '車', 1000),

    /** 砲/炮 - 直线移动，吃子时必须隔一个棋子（炮架） */
    CANNON('炮', '砲', 500),

    /** 兵/卒 - 过河前只能前进，过河后可左右或前进，每步一格 */
    SOLDIER('兵', '卒', 100);

    /**
     * 根据所属方获取对应的中文显示名称
     */
    fun displayName(side: Side): Char = when (side) {
        Side.RED -> redName
        Side.BLACK -> blackName
    }
}

/**
 * 象棋双方枚举
 *
 * 红方（RED）通常在下方，先手；黑方（BLACK）在上方，后手。
 */
enum class Side {
    RED, BLACK;

    /** 获取对手方 */
    val opponent: Side
        get() = when (this) {
            RED -> BLACK
            BLACK -> RED
        }

    /** 是否为红方 */
    val isRed: Boolean get() = this == RED

    /** 是否为黑方 */
    val isBlack: Boolean get() = this == BLACK
}

/**
 * 象棋棋子数据类
 *
 * 不可变数据类，表示棋盘上的一枚棋子。
 *
 * @property type 棋子类型
 * @property side 所属方（红/黑）
 */
data class Piece(
    val type: PieceType,
    val side: Side
) {
    /** 棋子的中文显示名称 */
    val displayName: Char get() = type.displayName(side)

    /** 棋子的价值（用于AI评估） */
    val value: Int get() = type.value

    /** FEN 字符表示（红方大写，黑方小写） */
    val fenChar: Char
        get() {
            val c = when (type) {
                PieceType.GENERAL -> 'K'
                PieceType.ADVISOR -> 'A'
                PieceType.ELEPHANT -> 'B'
                PieceType.HORSE -> 'N'
                PieceType.CHARIOT -> 'R'
                PieceType.CANNON -> 'C'
                PieceType.SOLDIER -> 'P'
            }
            return if (side == Side.RED) c else c.lowercaseChar()
        }

    override fun toString(): String = "${side.name}[$displayName]"

    companion object {
        /** 根据 FEN 字符创建棋子 */
        fun fromFenChar(c: Char): Piece? {
            val side = if (c.isUpperCase()) Side.RED else Side.BLACK
            val type = when (c.uppercaseChar()) {
                'K' -> PieceType.GENERAL
                'A' -> PieceType.ADVISOR
                'B', 'E' -> PieceType.ELEPHANT
                'N', 'H' -> PieceType.HORSE
                'R' -> PieceType.CHARIOT
                'C' -> PieceType.CANNON
                'P' -> PieceType.SOLDIER
                else -> return null
            }
            return Piece(type, side)
        }
    }
}

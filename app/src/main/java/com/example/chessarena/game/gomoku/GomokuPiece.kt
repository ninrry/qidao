package com.example.chessarena.game.gomoku

/**
 * 五子棋棋子枚举
 *
 * 五子棋只有黑白两色棋子。在连珠（Renju）规则下，黑方先手但有禁手限制。
 */
enum class Stone {
    /** 黑子 - 先手，在连珠规则下有禁手限制（三三、四四、长连） */
    BLACK,

    /** 白子 - 后手，无任何限制（长连也算赢） */
    WHITE;

    /** 获取对手方 */
    val opponent: Stone
        get() = when (this) {
            BLACK -> WHITE
            WHITE -> BLACK
        }

    /** 是否为黑子 */
    val isBlack: Boolean get() = this == BLACK

    /** 是否为白子 */
    val isWhite: Boolean get() = this == WHITE

    /** 中文显示名称 */
    val displayName: String
        get() = when (this) {
            BLACK -> "黑"
            WHITE -> "白"
        }
}

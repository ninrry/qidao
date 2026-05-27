package com.wind23.slowrenju

object GlobalValue {
    init {
        System.loadLibrary("slowrenju")
    }

    /**
     * 调用 SlowRenju 原生搜索算法计算最佳落子。
     * @param board 225 长度的一维棋盘数组（1代表黑棋，-1代表白棋，0代表空白）
     * @param fflag 禁手规则（1为有禁手规则，0为无禁手）
     * @param level 难度系数限制节点总数（1-5分别对应限制，0或-1为解限）
     * @return 返回计算的最佳落子一维坐标索引（y * 15 + x），可直接除模得出行列
     */
    @JvmStatic
    external fun getPos(board: IntArray, fflag: Int, level: Int): Int

    /**
     * 判断当前落子之后是否分出胜负或触发禁手
     * @return 1为黑胜，-1为白胜（或黑棋禁手判负），0为进行中，2为和棋
     */
    @JvmStatic
    external fun testWin(board: IntArray, fflag: Int, lastMoveIndex: Int): Int

    /**
     * 停止当前的计算
     */
    @JvmStatic
    external fun stopThink(): Int

    /**
     * 获取当前计算的状态信息字符串
     */
    @JvmStatic
    external fun thinkingStatus(): String
}

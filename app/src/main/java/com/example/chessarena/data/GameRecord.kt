package com.example.chessarena.data

import kotlinx.serialization.Serializable

/**
 * 对局记录数据模型
 */
@Serializable
data class GameRecord(
    val id: String,
    val gameType: String,       // "xiangqi" (象棋) 或 "gomoku" (五子棋)
    val difficulty: String,     // AI 难度名称
    val playerSide: String,     // 玩家执子名称 ("执红", "执黑", "执白" 等)
    val result: String,         // "WIN" (胜利), "LOSE" (战败), "DRAW" (和棋)
    val moves: List<String>,    // 走子历史/棋谱
    val timestamp: Long,        // 对局结束时间戳
    val movesCount: Int,        // 总步数
    val durationSeconds: Long = 0L // 对局估算用时
)

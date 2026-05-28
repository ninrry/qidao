package com.example.chessarena.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.chessarena.engine.AppContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 棋道数据管理器 - 提供本地对局历史的持久化存储与管理
 */
object DataRepository {
    private const val PREFS_NAME = "chess_arena_data_prefs"
    private const val KEY_GAME_RECORDS = "game_records"

    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    private fun prefs(): SharedPreferences? {
        val context = AppContext.context ?: return null
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 加载全部对局历史记录 (默认按时间倒序排列)
     */
    fun loadGameRecords(): List<GameRecord> {
        val p = prefs() ?: return emptyList()
        val jsonStr = p.getString(KEY_GAME_RECORDS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<GameRecord>>(jsonStr).sortedByDescending { it.timestamp }
        } catch (t: Throwable) {
            Log.e("DataRepository", "Failed to decode game records", t)
            emptyList()
        }
    }

    /**
     * 保存单条对局历史记录
     */
    fun saveGameRecord(record: GameRecord) {
        val current = loadGameRecords().toMutableList()
        current.add(0, record) // 新纪录加入首位
        saveList(current)
    }

    /**
     * 删除单条对局记录
     */
    fun deleteGameRecord(id: String) {
        val current = loadGameRecords().filter { it.id != id }
        saveList(current)
    }

    /**
     * 清空全部对局历史记录
     */
    fun clearAllRecords() {
        prefs()?.edit()?.remove(KEY_GAME_RECORDS)?.apply()
    }

    private fun saveList(list: List<GameRecord>) {
        try {
            val jsonStr = json.encodeToString(list)
            prefs()?.edit()?.putString(KEY_GAME_RECORDS, jsonStr)?.apply()
        } catch (t: Throwable) {
            Log.e("DataRepository", "Failed to encode and save list", t)
        }
    }
}

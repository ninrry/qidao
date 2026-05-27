package com.example.chessarena.data

import android.content.Context
import android.content.SharedPreferences
import com.example.chessarena.engine.AppContext
import com.example.chessarena.engine.Difficulty

object GamePreferences {
    private const val PREFS_NAME = "chess_arena_game_prefs"
    private const val KEY_GOMOKU_DIFFICULTY = "gomoku_difficulty"
    private const val KEY_XIANGQI_DIFFICULTY = "xiangqi_difficulty"

    private fun prefs(): SharedPreferences? {
        val context = AppContext.context ?: return null
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun loadGomokuDifficulty(): Difficulty? {
        val name = prefs()?.getString(KEY_GOMOKU_DIFFICULTY, null) ?: return null
        return Difficulty.entries.firstOrNull { it.name == name && it in Difficulty.gomokuDifficulties() }
    }

    fun saveGomokuDifficulty(difficulty: Difficulty) {
        prefs()?.edit()?.putString(KEY_GOMOKU_DIFFICULTY, difficulty.name)?.apply()
    }

    fun loadXiangqiDifficulty(): Difficulty? {
        val name = prefs()?.getString(KEY_XIANGQI_DIFFICULTY, null) ?: return null
        return Difficulty.entries.firstOrNull { it.name == name && it in Difficulty.xiangqiDifficulties() }
    }

    fun saveXiangqiDifficulty(difficulty: Difficulty) {
        prefs()?.edit()?.putString(KEY_XIANGQI_DIFFICULTY, difficulty.name)?.apply()
    }
}

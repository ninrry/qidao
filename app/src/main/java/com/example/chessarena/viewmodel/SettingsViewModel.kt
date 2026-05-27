package com.example.chessarena.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 棋盘主题
 */
enum class BoardTheme(val displayName: String, val description: String) {
    WOOD("木质", "经典木质棋盘"),
    DARK("暗色", "深色主题棋盘"),
    MARBLE("大理石", "大理石纹理棋盘")
}

/**
 * 动画速度
 */
enum class AnimationSpeed(val displayName: String, val durationMs: Long) {
    SLOW("慢速", 500L),
    NORMAL("正常", 300L),
    FAST("快速", 150L),
    INSTANT("无动画", 0L)
}

/**
 * 设置 UI 状态
 *
 * @param boardTheme 棋盘主题
 * @param soundEnabled 是否启用音效
 * @param hapticEnabled 是否启用触觉反馈
 * @param showEvalBar 是否显示局面评估条
 * @param animationSpeed 动画速度
 * @param showMoveHistory 是否显示走法历史
 * @param showCoordinates 是否显示坐标标注
 * @param autoSave 是否自动保存对局
 */
data class SettingsUiState(
    val boardTheme: BoardTheme = BoardTheme.WOOD,
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val showEvalBar: Boolean = true,
    val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    val showMoveHistory: Boolean = true,
    val showCoordinates: Boolean = true
)

/**
 * 设置管理 ViewModel
 *
 * 使用 SharedPreferences 进行简化的持久化存储。
 * 管理棋盘主题、音效、触觉反馈、评估条显示、动画速度等设置项。
 *
 * TODO: 后续可迁移到 DataStore 以获得更好的异步读写性能
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(loadSettings())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * 从 SharedPreferences 加载设置
     */
    private fun loadSettings(): SettingsUiState {
        return SettingsUiState(
            boardTheme = prefs.getString(KEY_BOARD_THEME, BoardTheme.WOOD.name)
                ?.let { name -> BoardTheme.entries.find { it.name == name } }
                ?: BoardTheme.WOOD,
            soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true),
            hapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, true),
            showEvalBar = prefs.getBoolean(KEY_SHOW_EVAL_BAR, true),
            animationSpeed = prefs.getString(KEY_ANIMATION_SPEED, AnimationSpeed.NORMAL.name)
                ?.let { name -> AnimationSpeed.entries.find { it.name == name } }
                ?: AnimationSpeed.NORMAL,
            showMoveHistory = prefs.getBoolean(KEY_SHOW_MOVE_HISTORY, true),
            showCoordinates = prefs.getBoolean(KEY_SHOW_COORDINATES, true)
        )
    }

    /**
     * 保存设置到 SharedPreferences
     */
    private fun saveSettings(state: SettingsUiState) {
        prefs.edit()
            .putString(KEY_BOARD_THEME, state.boardTheme.name)
            .putBoolean(KEY_SOUND_ENABLED, state.soundEnabled)
            .putBoolean(KEY_HAPTIC_ENABLED, state.hapticEnabled)
            .putBoolean(KEY_SHOW_EVAL_BAR, state.showEvalBar)
            .putString(KEY_ANIMATION_SPEED, state.animationSpeed.name)
            .putBoolean(KEY_SHOW_MOVE_HISTORY, state.showMoveHistory)
            .putBoolean(KEY_SHOW_COORDINATES, state.showCoordinates)
            .apply()
    }

    /**
     * 更新设置并自动持久化
     */
    private fun updateAndSave(transform: (SettingsUiState) -> SettingsUiState) {
        _uiState.update { current ->
            val newState = transform(current)
            saveSettings(newState)
            newState
        }
    }

    // ==================== 各项设置的更新方法 ====================

    /** 设置棋盘主题 */
    fun setBoardTheme(theme: BoardTheme) {
        updateAndSave { it.copy(boardTheme = theme) }
    }

    /** 设置音效开关 */
    fun setSoundEnabled(enabled: Boolean) {
        updateAndSave { it.copy(soundEnabled = enabled) }
    }

    /** 设置触觉反馈开关 */
    fun setHapticEnabled(enabled: Boolean) {
        updateAndSave { it.copy(hapticEnabled = enabled) }
    }

    /** 设置评估条显示 */
    fun setShowEvalBar(show: Boolean) {
        updateAndSave { it.copy(showEvalBar = show) }
    }

    /** 设置动画速度 */
    fun setAnimationSpeed(speed: AnimationSpeed) {
        updateAndSave { it.copy(animationSpeed = speed) }
    }

    /** 设置走法历史显示 */
    fun setShowMoveHistory(show: Boolean) {
        updateAndSave { it.copy(showMoveHistory = show) }
    }

    /** 设置坐标标注显示 */
    fun setShowCoordinates(show: Boolean) {
        updateAndSave { it.copy(showCoordinates = show) }
    }

    /** 重置所有设置为默认值 */
    fun resetToDefaults() {
        val defaultState = SettingsUiState()
        _uiState.value = defaultState
        saveSettings(defaultState)
    }

    companion object {
        private const val PREFS_NAME = "chess_arena_settings"
        private const val KEY_BOARD_THEME = "board_theme"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_SHOW_EVAL_BAR = "show_eval_bar"
        private const val KEY_ANIMATION_SPEED = "animation_speed"
        private const val KEY_SHOW_MOVE_HISTORY = "show_move_history"
        private const val KEY_SHOW_COORDINATES = "show_coordinates"
    }
}

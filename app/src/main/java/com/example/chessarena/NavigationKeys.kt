package com.example.chessarena

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey
@Serializable data object XiangqiGame : NavKey
@Serializable data object GomokuGame : NavKey
@Serializable data object Settings : NavKey
@Serializable data object History : NavKey

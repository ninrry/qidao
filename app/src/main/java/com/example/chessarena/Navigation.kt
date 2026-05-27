package com.example.chessarena

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.chessarena.ui.screens.GomokuScreen
import com.example.chessarena.ui.screens.HomeScreen
import com.example.chessarena.ui.screens.SettingsScreen
import com.example.chessarena.ui.screens.XiangqiScreen
import android.app.Activity

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Home)
  val activity = LocalContext.current as? Activity

  NavDisplay(
    backStack = backStack,
    onBack = {
      if (backStack.size > 1) {
        backStack.removeLastOrNull()
      } else {
        activity?.finish()
      }
    },
    entryProvider =
      entryProvider {
        entry<Home> {
          HomeScreen(
            onNavigateTo = { navKey -> backStack.add(navKey) },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<XiangqiGame> {
          XiangqiScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<GomokuGame> {
          GomokuScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
        entry<Settings> {
          SettingsScreen(
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}

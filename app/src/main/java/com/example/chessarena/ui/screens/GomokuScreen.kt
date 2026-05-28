package com.example.chessarena.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chessarena.R
import com.example.chessarena.audio.SoundManager
import com.example.chessarena.engine.Difficulty
import com.example.chessarena.game.gomoku.Stone
import com.example.chessarena.theme.*
import com.example.chessarena.ui.components.*
import com.example.chessarena.ui.components.dialogs.DifficultyDialog
import com.example.chessarena.ui.components.dialogs.EngineErrorDialog
import com.example.chessarena.ui.components.dialogs.GameOverDialog
import com.example.chessarena.ui.components.dialogs.ExitConfirmDialog
import androidx.activity.compose.BackHandler
import com.example.chessarena.viewmodel.GomokuViewModel
import com.example.chessarena.viewmodel.SettingsViewModel

@Composable
fun GomokuScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GomokuViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    var showHistory by remember { mutableStateOf(settingsState.showMoveHistory) }
    var showEval by remember { mutableStateOf(settingsState.showEvalBar) }
    var showExitConfirm by remember { mutableStateOf(false) }

    // 拦截物理返回键
    val isGameInProgress = uiState.isGameActive
    BackHandler(enabled = isGameInProgress) {
        showExitConfirm = true
    }

    LaunchedEffect(uiState.moveHistory.size) {
        if (uiState.moveHistory.isNotEmpty()) {
            try {
                if (settingsState.hapticEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            } catch (t: Throwable) {
                Log.w("GomokuScreen", "Unable to perform haptic feedback", t)
            }
            try {
                if (settingsState.soundEnabled) {
                    SoundManager.play(SoundManager.SoundType.MOVE)
                }
            } catch (t: Throwable) {
                Log.w("GomokuScreen", "Unable to play move feedback", t)
            }
        }
    }

    LaunchedEffect(uiState.gameOverMessage) {
        val message = uiState.gameOverMessage
        if (message != null && settingsState.soundEnabled) {
            try {
                when {
                    message.contains("黑方获胜") -> SoundManager.play(SoundManager.SoundType.WIN)
                    message.contains("白方获胜") || message.contains("AI 走步异常") -> SoundManager.play(SoundManager.SoundType.LOSE)
                }
            } catch (t: Throwable) {
                Log.w("GomokuScreen", "Unable to play game-over feedback", t)
            }
        }
    }

    val gameState = uiState.gameState
    val boardState = if (gameState == null) {
        GomokuBoardState()
    } else {
        val stones = mutableListOf<GomokuStone>()
        for (i in gameState.board.indices) {
            val stone = gameState.board[i]
            if (stone != null) {
                stones.add(GomokuStone(row = i / 15, col = i % 15, color = if (stone == Stone.BLACK) StoneColor.BLACK else StoneColor.WHITE))
            }
        }
        GomokuBoardState(
            stones = stones,
            lastMove = gameState.lastMove?.let { GomokuStone(row = it.row, col = it.col, color = if (it.stone == Stone.BLACK) StoneColor.BLACK else StoneColor.WHITE) },
            forbiddenMoves = uiState.forbiddenMoves,
            winningLine = uiState.winningLine
        )
    }

    Box(
        modifier = modifier.fillMaxSize().background(if (isDark) PaperBgDark else PaperBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GameTopBarGomoku(
                title = "连珠五子棋 · 棋道",
                onBack = { if (isGameInProgress) showExitConfirm = true else onBack() },
                onUndo = { viewModel.onUndo() },
                onResign = { viewModel.onResign() },
                canUndo = uiState.moveHistory.size >= 2 && !uiState.isAiThinking
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                PlayerInfoBar(
                    avatarChar = if (uiState.playerSide == Stone.BLACK) "白" else "黑",
                    avatarColor = AiAvatarBlue,
                    name = uiState.difficulty.displayName,
                    level = "Lvl ${uiState.difficulty.engineParam}",
                    info = uiState.difficulty.formatThinkTime(),
                    status = when {
                        uiState.isAiThinking -> "思考中"
                        else -> if (gameState?.currentTurn != uiState.playerSide) "回合中" else "等待中"
                    },
                    statusColor = if (uiState.isAiThinking) Warning else Success,
                    isThinking = uiState.isAiThinking
                )

                Spacer(modifier = Modifier.height(8.dp))

                val historyVisible = showHistory && uiState.moveHistory.isNotEmpty()
                AnimatedVisibility(visible = historyVisible) {
                    Column {
                        MoveHistory(
                            moves = uiState.moveHistory,
                            currentMoveIndex = uiState.moveHistory.size - 1,
                            onMoveClick = {},
                            firstMoveLabel = "黑",
                            secondMoveLabel = "白",
                            modifier = Modifier.semantics { contentDescription = "走棋历史，共${uiState.moveHistory.size}步" }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 五子棋实时胜率/分差局势评估条
                        AnimatedVisibility(visible = showEval) {
                            EvalBar(
                                score = uiState.evaluation,
                                isThinking = uiState.isAiThinking,
                                modifier = Modifier.fillMaxHeight(0.9f).width(20.dp)
                            )
                        }

                        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            val boardSide = minOf(maxWidth, maxHeight)
                            GomokuBoard(
                                state = boardState,
                                modifier = Modifier.size(boardSide),
                                onPositionClick = viewModel::onPositionClick,
                                showCoordinates = settingsState.showCoordinates,
                                animationSpeed = settingsState.animationSpeed,
                                boardTheme = settingsState.boardTheme
                            )
                        }
                    }

                    val isPlayerTurn = uiState.gameState?.currentTurn == uiState.playerSide
                    val showBanner = isPlayerTurn && uiState.playerSide == Stone.BLACK && uiState.forbiddenMoves.isNotEmpty()
                    if (showBanner) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp).clip(RoundedCornerShape(8.dp)),
                            color = Error.copy(alpha = 0.92f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Warning.copy(alpha = 0.8f))
                        ) {
                            Text(
                                text = "⚠️ 警示：盘中已存黑方禁手，落子即负！",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                PlayerInfoBar(
                    avatarChar = if (uiState.playerSide == Stone.BLACK) "黑" else "白",
                    avatarColor = PlayerAvatarGreen,
                    name = "玩家",
                    level = "",
                    info = uiState.difficulty.formatThinkTime(),
                    status = when {
                        uiState.isAiThinking -> "等待中"
                        else -> if (gameState?.currentTurn == uiState.playerSide) "回合中" else "等待中"
                    },
                    statusColor = Success,
                    isThinking = false
                )

                Spacer(modifier = Modifier.height(6.dp))

                GomokuToolBar(
                    onNewGame = { viewModel.onNewGame() },
                    onToggleHistory = { showHistory = !showHistory },
                    onToggleEval = { showEval = !showEval },
                    showHistory = historyVisible,
                    showEval = showEval
                )
            }
        }

        if (uiState.showDifficultyDialog) {
            DifficultyDialog(
                title = "选择五子棋难度",
                difficulties = Difficulty.gomokuDifficulties(),
                selectedDifficulty = uiState.difficulty,
                firstMoveLabel1 = "我先手",
                firstMoveLabel2 = "AI先手",
                onStartGame = { difficulty, sideIndex ->
                    val side = if (sideIndex == 0) Stone.BLACK else Stone.WHITE
                    viewModel.onDifficultySelected(difficulty, side)
                },
                onDismiss = {
                    if (uiState.gameState == null) onBack()
                    else viewModel.onDismissDifficultyDialog()
                }
            )
        }

        uiState.gameOverMessage?.let { message ->
            GameOverDialog(
                message = message,
                gameType = "gomoku",
                onDismiss = { viewModel.onDismissGameOver() },
                onPlayAgain = {
                    viewModel.onDismissGameOver()
                    viewModel.onNewGame()
                }
            )
        }

        uiState.engineHealthError?.let { error ->
            EngineErrorDialog(
                message = error,
                onDismiss = { viewModel.onDismissEngineError() },
                onRetry = { viewModel.onRetryEngine() }
            )
        }

        if (showExitConfirm) {
            ExitConfirmDialog(
                onConfirm = {
                    showExitConfirm = false
                    viewModel.onResign()
                    onBack()
                },
                onDismiss = { showExitConfirm = false }
            )
        }
    }
}

@Composable
private fun GameTopBarGomoku(
    title: String, onBack: () -> Unit, onUndo: () -> Unit, onResign: () -> Unit, canUndo: Boolean
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Surface(color = if (isDark) SurfaceCardDark else SurfaceCardLight, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = if (isDark) SilkWhite else DeepInk)
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) SilkWhite else DeepInk
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onUndo, enabled = canUndo) {
                    Text("悔棋", color = if (canUndo) GomokuCardBlue else if (isDark) OnSurfaceDarkSecondary.copy(alpha = 0.5f) else StoneGray.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = onResign) {
                    Text("认输", color = Error, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun GomokuToolBar(
    onNewGame: () -> Unit,
    onToggleHistory: () -> Unit,
    onToggleEval: () -> Unit,
    showHistory: Boolean,
    showEval: Boolean
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val activeColor = GomokuCardBlue
    val idleColor = if (isDark) OnSurfaceDarkSecondary else StoneGray

    data class Tool(val label: String, val icon: Int, val active: Boolean, val onClick: () -> Unit)
    val tools = listOf(
        Tool("新局", R.drawable.ic_reset_chaos, false, onNewGame),
        Tool("棋谱", R.drawable.ic_undo_reincarnation, showHistory, onToggleHistory),
        Tool("分析", R.drawable.ic_brain_pikafish, showEval, onToggleEval)
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        tools.forEach { tool ->
            val tint = if (tool.active) activeColor else idleColor
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { tool.onClick() }.padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier.size(42.dp).background(if (tool.active) activeColor.copy(alpha = 0.15f) else if (isDark) SurfaceDarkVariant else SurfaceLightVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(id = tool.icon), contentDescription = tool.label, modifier = Modifier.size(20.dp), tint = tint)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = tool.label, style = MaterialTheme.typography.labelSmall, color = tint)
            }
        }
    }
}

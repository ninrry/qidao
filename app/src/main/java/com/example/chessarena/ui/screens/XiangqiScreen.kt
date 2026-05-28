package com.example.chessarena.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.chessarena.game.xiangqi.Side
import com.example.chessarena.theme.*
import com.example.chessarena.ui.components.*
import com.example.chessarena.ui.components.dialogs.DifficultyDialog
import com.example.chessarena.ui.components.dialogs.EngineErrorDialog
import com.example.chessarena.ui.components.dialogs.GameOverDialog
import com.example.chessarena.ui.components.dialogs.ExitConfirmDialog
import androidx.activity.compose.BackHandler
import com.example.chessarena.viewmodel.SettingsViewModel
import com.example.chessarena.viewmodel.XiangqiViewModel

@Composable
fun XiangqiScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: XiangqiViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    // 面板切换状态
    var showHistory by remember { mutableStateOf(settingsState.showMoveHistory) }
    var showEval by remember { mutableStateOf(settingsState.showEvalBar) }
    var showExitConfirm by remember { mutableStateOf(false) }

    // 拦截物理返回键
    val isGameInProgress = uiState.isGameActive
    BackHandler(enabled = isGameInProgress) {
        showExitConfirm = true
    }

    // 将军抖动动画
    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(uiState.isInCheck) {
        if (uiState.isInCheck) {
            shakeAnim.snapTo(0f)
            shakeAnim.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 300
                    14f at 30 using FastOutLinearInEasing
                    -11f at 60 using LinearEasing
                    8f at 100 using LinearEasing
                    -5f at 150 using LinearEasing
                    2f at 210 using LinearEasing
                    -0.5f at 260 using LinearEasing
                    0f at 300 using LinearEasing
                }
            )
        } else {
            shakeAnim.snapTo(0f)
        }
    }

    // 音效和震动反馈
    LaunchedEffect(uiState.moveHistory.size) {
        if (uiState.moveHistory.isNotEmpty()) {
            if (settingsState.soundEnabled) {
                try {
                    when {
                        uiState.isInCheck -> SoundManager.play(SoundManager.SoundType.CHECK)
                        uiState.lastMove?.captured != null -> SoundManager.play(SoundManager.SoundType.CAPTURE)
                        else -> SoundManager.play(SoundManager.SoundType.MOVE)
                    }
                } catch (t: Throwable) {
                    Log.w("XiangqiScreen", "Unable to play move feedback", t)
                }
            }
            if (settingsState.hapticEnabled) {
                try {
                    when {
                        uiState.isInCheck -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        uiState.lastMove?.captured != null -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        else -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                } catch (t: Throwable) {
                    Log.w("XiangqiScreen", "Unable to perform haptic feedback", t)
                }
            }
        }
    }

    // 胜负音效
    LaunchedEffect(uiState.gameOverMessage) {
        val message = uiState.gameOverMessage
        if (message != null && settingsState.soundEnabled) {
            try {
                if (message.contains("红方获胜") || message.contains("胜利") || message.contains("赢")) {
                    SoundManager.play(SoundManager.SoundType.WIN)
                } else {
                    SoundManager.play(SoundManager.SoundType.LOSE)
                }
            } catch (t: Throwable) {
                Log.w("XiangqiScreen", "Unable to play game-over feedback", t)
            }
        }
    }

    // 棋盘状态
    val gameState = uiState.gameState
    val boardState = if (gameState == null) XiangqiBoardState() else {
        val pieces = gameState.board.mapIndexedNotNull { i, p ->
            p?.let { XiangqiPieceData(mapPieceType(it.type), mapSide(it.side), BoardPosition(i / 9, i % 9)) }
        }
        XiangqiBoardState(
            pieces = pieces,
            selectedPosition = uiState.selectedPosition?.let { BoardPosition(it.row, it.col) },
            validMoves = uiState.validMoves.map { BoardPosition(it.row, it.col) },
            lastMove = uiState.lastMove?.let { XiangqiMoveData(BoardPosition(it.from.row, it.from.col), BoardPosition(it.to.row, it.to.col), it.notation) },
            isRedTurn = gameState.currentTurn == Side.RED,
            isInCheck = uiState.isInCheck
        )
    }

    val capturedCount = uiState.capturedByPlayer.size + uiState.capturedByAi.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) PaperBgDark else PaperBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            GameTopBar(
                title = "中国象棋 · 棋道",
                onBack = { if (isGameInProgress) showExitConfirm = true else onBack() },
                onUndo = { viewModel.onUndo() },
                onResign = { viewModel.onResign() },
                canUndo = uiState.moveHistory.size >= 2 && !uiState.isAiThinking
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // AI 信息栏
                val aiSide = uiState.playerSide.opponent
                val aiSideName = if (aiSide == Side.RED) "红" else "黑"
                PlayerInfoBar(
                    avatarChar = getAvatarChar(uiState.difficulty),
                    avatarColor = AiAvatarRed,
                    name = uiState.difficulty.displayName,
                    level = "Lvl ${uiState.difficulty.engineParam}",
                    info = "执${aiSideName} · ${uiState.difficulty.formatThinkTime()}",
                    status = when {
                        uiState.isAiThinking -> "思考中"
                        uiState.isInCheck -> "被将军"
                        else -> if (boardState.isRedTurn && aiSide == Side.RED) "回合中" else "等待中"
                    },
                    statusColor = when {
                        uiState.isAiThinking -> Warning
                        uiState.isInCheck -> Error
                        else -> Success
                    },
                    isThinking = uiState.isAiThinking
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 棋谱面板
                val historyVisible = showHistory && uiState.moveHistory.isNotEmpty()
                AnimatedVisibility(visible = historyVisible) {
                    Column {
                        MoveHistory(
                            moves = uiState.moveHistory,
                            currentMoveIndex = uiState.moveHistory.size - 1,
                            onMoveClick = {},
                            modifier = Modifier.semantics { contentDescription = "走棋历史，共${uiState.moveHistory.size}步" }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // 棋盘 + 评估条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer { translationX = shakeAnim.value },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 评估条
                        AnimatedVisibility(visible = showEval) {
                            EvalBar(
                                score = uiState.evaluation,
                                isThinking = uiState.isAiThinking,
                                modifier = Modifier.fillMaxHeight(0.85f).width(20.dp)
                            )
                        }

                        BoxWithConstraints(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            val boardSize = minOf(maxWidth, maxHeight * 0.92f)
                            XiangqiBoard(
                                state = boardState,
                                onPositionClick = { viewModel.onPositionClick(mapPositionBack(it)) },
                                modifier = Modifier.width(boardSize),
                                isFlipped = uiState.playerSide == Side.BLACK,
                                showCoordinates = settingsState.showCoordinates,
                                animationSpeed = settingsState.animationSpeed,
                                boardTheme = settingsState.boardTheme
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 玩家信息栏
                val playerSideName = if (uiState.playerSide == Side.RED) "执红" else "执黑"
                PlayerInfoBar(
                    avatarChar = "我",
                    avatarColor = PlayerAvatarGreen,
                    name = "玩家",
                    level = playerSideName,
                    info = if (capturedCount > 0) {
                        uiState.capturedByPlayer.joinToString(" ") { mapPieceChar(it.type) }
                    } else "",
                    status = when {
                        uiState.isAiThinking -> "等待中"
                        uiState.isInCheck -> "被将军"
                        else -> if (!boardState.isRedTurn && uiState.playerSide == Side.BLACK || boardState.isRedTurn && uiState.playerSide == Side.RED) "回合中" else "等待中"
                    },
                    statusColor = when {
                        uiState.isInCheck -> Error
                        else -> Success
                    },
                    isThinking = false
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 底部工具栏
                GameToolBar(
                    onNewGame = { viewModel.onNewGame() },
                    onToggleHistory = { showHistory = !showHistory },
                    onToggleEval = { showEval = !showEval },
                    showHistory = historyVisible,
                    showEval = showEval
                )
            }
        }

        XiangqiCheckEffects(isInCheck = uiState.isInCheck)

        if (uiState.showDifficultyDialog) {
            DifficultyDialog(
                title = "选择象棋难度",
                difficulties = Difficulty.xiangqiDifficulties(),
                selectedDifficulty = uiState.difficulty,
                firstMoveLabel1 = "我先手",
                firstMoveLabel2 = "AI先手",
                onStartGame = { difficulty, sideIndex ->
                    val side = if (sideIndex == 0) Side.RED else Side.BLACK
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
                gameType = "xiangqi",
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
private fun GameTopBar(
    title: String,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onResign: () -> Unit,
    canUndo: Boolean
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Surface(
        color = if (isDark) SurfaceCardDark else SurfaceCardLight,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = if (isDark) SilkWhite else DeepInk
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) SilkWhite else DeepInk
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onUndo, enabled = canUndo) {
                    Text(
                        text = "悔棋",
                        color = if (canUndo) XiangqiCardGreen else
                            if (isDark) OnSurfaceDarkSecondary.copy(alpha = 0.5f) else StoneGray.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                TextButton(onClick = onResign) {
                    Text("认输", color = Error, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun GameToolBar(
    onNewGame: () -> Unit,
    onToggleHistory: () -> Unit,
    onToggleEval: () -> Unit,
    showHistory: Boolean,
    showEval: Boolean
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val activeColor = XiangqiCardGreen
    val idleColor = if (isDark) OnSurfaceDarkSecondary else StoneGray

    data class Tool(val label: String, val icon: Int, val active: Boolean, val onClick: () -> Unit)

    val tools = listOf(
        Tool("新局", R.drawable.ic_reset_chaos, false, onNewGame),
        Tool("棋谱", R.drawable.ic_undo_reincarnation, showHistory, onToggleHistory),
        Tool("分析", R.drawable.ic_brain_pikafish, showEval, onToggleEval)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tools.forEach { tool ->
            val tint = if (tool.active) activeColor else idleColor
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { tool.onClick() }
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (tool.active) activeColor.copy(alpha = 0.15f)
                            else if (isDark) SurfaceDarkVariant else SurfaceLightVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = tool.icon),
                        contentDescription = tool.label,
                        modifier = Modifier.size(20.dp),
                        tint = tint
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = tool.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint
                )
            }
        }
    }
}

private fun mapPieceChar(type: com.example.chessarena.game.xiangqi.PieceType): String = when (type) {
    com.example.chessarena.game.xiangqi.PieceType.GENERAL -> "帥"
    com.example.chessarena.game.xiangqi.PieceType.ADVISOR -> "仕"
    com.example.chessarena.game.xiangqi.PieceType.ELEPHANT -> "相"
    com.example.chessarena.game.xiangqi.PieceType.HORSE -> "馬"
    com.example.chessarena.game.xiangqi.PieceType.CHARIOT -> "車"
    com.example.chessarena.game.xiangqi.PieceType.CANNON -> "砲"
    com.example.chessarena.game.xiangqi.PieceType.SOLDIER -> "兵"
}

private fun getAvatarChar(difficulty: Difficulty): String = when (difficulty) {
    Difficulty.XIANGQI_SENIOR -> "李"
    Difficulty.XIANGQI_REGIONAL -> "省"
    Difficulty.XIANGQI_PROFESSIONAL -> "师"
    Difficulty.XIANGQI_MASTER -> "孟"
    Difficulty.XIANGQI_GRANDMASTER -> "许"
    Difficulty.XIANGQI_CHAMPION -> "王"
    else -> "棋"
}

private fun mapPieceType(type: com.example.chessarena.game.xiangqi.PieceType) = when (type) {
    com.example.chessarena.game.xiangqi.PieceType.GENERAL -> XiangqiPieceType.JIANG
    com.example.chessarena.game.xiangqi.PieceType.ADVISOR -> XiangqiPieceType.SHI
    com.example.chessarena.game.xiangqi.PieceType.ELEPHANT -> XiangqiPieceType.XIANG
    com.example.chessarena.game.xiangqi.PieceType.HORSE -> XiangqiPieceType.MA
    com.example.chessarena.game.xiangqi.PieceType.CHARIOT -> XiangqiPieceType.JU
    com.example.chessarena.game.xiangqi.PieceType.CANNON -> XiangqiPieceType.PAO
    com.example.chessarena.game.xiangqi.PieceType.SOLDIER -> XiangqiPieceType.BING
}

private fun mapSide(side: Side) = when (side) {
    Side.RED -> PieceSide.RED
    Side.BLACK -> PieceSide.BLACK
}

private fun mapPositionBack(pos: BoardPosition) = com.example.chessarena.game.xiangqi.Position(col = pos.col, row = pos.row)

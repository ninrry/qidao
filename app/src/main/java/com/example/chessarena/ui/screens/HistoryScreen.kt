package com.example.chessarena.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessarena.R
import com.example.chessarena.data.DataRepository
import com.example.chessarena.data.GameRecord
import com.example.chessarena.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 棋道对局印记界面 (对局历史记录)
 * 素雅新国风水墨与金箔宣纸背景，伴有印砂书法捷败盖印
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    var records by remember { mutableStateOf<List<GameRecord>>(emptyList()) }
    var selectedRecord by remember { mutableStateOf<GameRecord?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // 加载本地历史记录
    LaunchedEffect(Unit) {
        records = DataRepository.loadGameRecords()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .xuanPaperBackground(isDark),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "对局印记",
                        fontWeight = FontWeight.Bold,
                        color = SilkWhite,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back_leaf),
                            contentDescription = "返回",
                            tint = SilkWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    if (records.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirm = true }) {
                            Text(
                                "清空印记",
                                color = AccentGold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = SilkWhite
                ),
                modifier = Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E1E24),
                            Color(0xFF121215)
                        )
                    )
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (records.isEmpty()) {
                // 空置水墨美学状态
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_logo_zen),
                        contentDescription = null,
                        tint = if (isDark) OnSurfaceDarkSecondary.copy(alpha = 0.2f) else StoneGray.copy(alpha = 0.2f),
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "落子无声，尚未留下对局印记",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDark) OnSurfaceDarkSecondary else StoneGray,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        HistoryCard(
                            record = record,
                            isDark = isDark,
                            onClick = { selectedRecord = record },
                            onDelete = {
                                DataRepository.deleteGameRecord(record.id)
                                records = DataRepository.loadGameRecords()
                            }
                        )
                    }
                }
            }
        }
    }

    // 棋谱弹窗详情
    selectedRecord?.let { record ->
        RecordDetailDialog(
            record = record,
            isDark = isDark,
            onDismiss = { selectedRecord = null }
        )
    }

    // 清空确认弹窗
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空印记", fontWeight = FontWeight.Bold, color = if (isDark) SilkWhite else DeepInk) },
            text = { Text("确定要抹除所有生平积累的对局印记吗？此举将不可逆转。", color = if (isDark) OnSurfaceDarkSecondary else StoneGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        DataRepository.clearAllRecords()
                        records = emptyList()
                        showClearConfirm = false
                    }
                ) {
                    Text("断然清空", color = Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("且慢", color = if (isDark) SilkWhite else DeepInk)
                }
            },
            containerColor = if (isDark) Color(0xFF1C1E2A) else Color(0xFFFCFAF2),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun HistoryCard(
    record: GameRecord,
    isDark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(record.timestamp))
    
    val isWin = record.result == "WIN"
    val isLose = record.result == "LOSE"
    
    // 国风朱砂泥印章颜色
    val stampColor = when {
        isWin -> Color(0xFFB33E3E)      // 朱砂红
        isLose -> Color(0xFF6B7280)     // 黛黑色
        else -> Color(0xFFC49A45)       // 碎金色
    }
    val stampText = when {
        isWin -> "捷"
        isLose -> "负"
        else -> "和"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xEE1C1E2A) else Color(0xFFF5F2EA)
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0x1EFFFFFF) else Color(0x11000000))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val gameName = if (record.gameType == "xiangqi") "中国象棋" else "连珠五子棋"
                    val gameColor = if (record.gameType == "xiangqi") XiangqiCardGreen else GomokuCardBlue
                    Surface(
                        color = gameColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = gameName,
                            fontSize = 10.sp,
                            color = gameColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.difficulty,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) SilkWhite else DeepInk
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${record.playerSide} · 共 ${record.movesCount} 步 · 用时约 ${record.durationSeconds} 秒",
                    fontSize = 12.sp,
                    color = if (isDark) OnSurfaceDarkSecondary else StoneGray
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = (if (isDark) OnSurfaceDarkSecondary else StoneGray).copy(alpha = 0.6f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // 国风印章
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .border(BorderStroke(2.dp, stampColor.copy(alpha = 0.6f)), RoundedCornerShape(6.dp))
                        .background(stampColor.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stampText,
                        color = stampColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 垃圾桶删除
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_reset_chaos),
                        contentDescription = "删除记录",
                        tint = Error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordDetailDialog(
    record: GameRecord,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (record.gameType == "xiangqi") "中国象棋 · 棋谱" else "连珠五子棋 · 局势轨迹",
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) SilkWhite else DeepInk
                )
                Text(
                    text = "对手: ${record.difficulty} (${record.playerSide})",
                    fontSize = 12.sp,
                    color = if (isDark) OnSurfaceDarkSecondary else StoneGray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        },
        text = {
            Surface(
                color = if (isDark) Color(0x33000000) else Color(0x0A000000),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0x1EFFFFFF) else Color(0x0A000000)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val stepPairs = record.moves.chunked(2)
                    items(stepPairs.size) { index ->
                        val pair = stepPairs[index]
                        val stepNum = index + 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "第 $stepNum 回合",
                                fontSize = 13.sp,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(72.dp)
                            )
                            
                            val move1 = pair.getOrNull(0) ?: ""
                            Text(
                                text = if (record.gameType == "xiangqi") move1 else "黑: $move1",
                                fontSize = 13.sp,
                                color = if (isDark) SilkWhite else DeepInk,
                                modifier = Modifier.weight(1f)
                            )
                            
                            val move2 = pair.getOrNull(1) ?: ""
                            if (move2.isNotEmpty()) {
                                Text(
                                    text = if (record.gameType == "xiangqi") move2 else "白: $move2",
                                    fontSize = 13.sp,
                                    color = if (isDark) OnSurfaceDarkSecondary else StoneGray,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("合谱", color = AccentGold, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = if (isDark) Color(0xFF1C1E2A) else Color(0xFFFCFAF2),
        shape = RoundedCornerShape(20.dp)
    )
}

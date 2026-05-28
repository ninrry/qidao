package com.example.chessarena.ui.components.dialogs

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chessarena.R
import com.example.chessarena.engine.Difficulty
import com.example.chessarena.theme.*

@Composable
fun DifficultyDialog(
    title: String,
    difficulties: List<Difficulty>,
    selectedDifficulty: Difficulty,
    firstMoveLabel1: String,
    firstMoveLabel2: String,
    onStartGame: (Difficulty, sideIndex: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var tempDifficulty by remember { mutableStateOf(selectedDifficulty) }
    var tempSide by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = if (androidx.compose.foundation.isSystemInDarkTheme()) PaperBgDark else PaperBg,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back_leaf),
                            contentDescription = "返回",
                            tint = if (androidx.compose.foundation.isSystemInDarkTheme()) SilkWhite else DeepInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (androidx.compose.foundation.isSystemInDarkTheme()) SilkWhite else DeepInk,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = if (androidx.compose.foundation.isSystemInDarkTheme()) SilkWhite else DeepInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 先手选择
                FirstMoveSelector(
                    option1 = firstMoveLabel1,
                    option2 = firstMoveLabel2,
                    selectedSide = tempSide,
                    onSideSelected = { tempSide = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    difficulties.forEachIndexed { index, difficulty ->
                        DifficultyItem(
                            difficulty = difficulty,
                            isSelected = difficulty == tempDifficulty,
                            index = index,
                            onClick = { tempDifficulty = difficulty }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { onStartGame(tempDifficulty, tempSide) },
                    colors = ButtonDefaults.buttonColors(containerColor = XiangqiCardGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(text = "开始对局", fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@Composable
private fun FirstMoveSelector(
    option1: String,
    option2: String,
    selectedSide: Int,
    onSideSelected: (Int) -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Surface(
        color = if (isDark) SurfaceDarkVariant else SurfaceLightVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            listOf(option1 to 0, option2 to 1).forEach { (label, sideIndex) ->
                val selected = sideIndex == selectedSide
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) XiangqiCardGreen else Color.Transparent)
                        .clickable { onSideSelected(sideIndex) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) Color.White else
                            if (isDark) OnSurfaceDarkSecondary else StoneGray
                    )
                }
            }
        }
    }
}

@Composable
private fun DifficultyItem(
    difficulty: Difficulty,
    isSelected: Boolean,
    index: Int,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 50L); visible = true }

    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(250), label = "alpha")

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val bgColor = when {
        isSelected -> if (isDark) Color(0xFF2A3D33) else Color(0xFFE8F5EC)
        else -> if (isDark) SurfaceCardDark else SurfaceCardLight
    }
    val borderColor = if (isSelected) XiangqiCardGreen else if (isDark) DividerDark else DividerLight

    Card(
        modifier = Modifier.fillMaxWidth().alpha(alpha).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.5.dp, borderColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 1.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val avatarColors = listOf(Color(0xFF5A9A6E), Color(0xFF5B8DB8), Color(0xFFC04851), Color(0xFFD4B106), Color(0xFF8B5A8C), Color(0xFF4C8DAE))
            val avatarColor = avatarColors[index % avatarColors.size]

            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(avatarColor.copy(alpha = 0.15f)).border(BorderStroke(1.5.dp, avatarColor), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = difficulty.displayName.take(1), color = avatarColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = difficulty.displayName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp), color = if (isDark) SilkWhite else DeepInk)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(color = if (isSelected) XiangqiCardGreen.copy(alpha = 0.2f) else if (isDark) Color(0x33FFFFFF) else Color(0xFFF0F0F0), shape = RoundedCornerShape(4.dp)) {
                        Text(text = "Lv${difficulty.engineParam}", modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp), color = if (isSelected) XiangqiCardGreen else if (isDark) OnSurfaceDarkSecondary else StoneGray, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = difficulty.formatThinkTime(), color = if (isSelected) XiangqiCardGreen else if (isDark) OnSurfaceDarkSecondary else StoneGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
                Text(text = difficulty.description, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 14.sp), color = if (isDark) OnSurfaceDarkSecondary else StoneGray, maxLines = 1)
            }

            Spacer(modifier = Modifier.width(6.dp))

            AnimatedVisibility(visible = isSelected) {
                Box(modifier = Modifier.size(20.dp).background(XiangqiCardGreen, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "已选择", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

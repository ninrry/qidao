# 棋道 (Qidao) - Chess Arena

一款融合传统棋道美学与现代AI技术的双棋类对弈应用，支持中国象棋与连珠五子棋，搭载成熟开源引擎提供专业级对弈体验。

## ✨ 特性

### 🎨 国风美学设计
- 太极阴阳主题图标与界面
- 水墨风格视觉元素
- 流畅的动画过渡效果
- 深色/浅色主题自适应

### ♟️ 中国象棋
- **引擎**: Pikafish (基于 Fairy-Stockfish)
- **难度**: 6级，从市级名手到全国冠军
- **功能**:
  - 实时引擎评估 (EvalBar)
  - 走棋历史记录 (竹简风格)
  - 吃子统计与展示
  - 将军检测与绝杀判定
  - 悔棋与认输
  - 先手选择 (玩家/AI)

### ⚫ 连珠五子棋
- **引擎**: SlowRenju (C++ 原生)
- **难度**: 5级，从业余一段到世界冠军
- **功能**:
  - 专业连珠禁手检测
  - 走棋历史记录
  - 悔棋与认输
  - 先手选择 (玩家/AI)

### 🎮 对弈体验
- 原生 C++ 引擎 (ARM64 + x86_64 双架构)
- 实时AI思考状态反馈
- 引擎健康监控与异常反馈
- 音效与触觉反馈
- 自适应棋盘主题 (木质/深色/大理石)
- 动画速度调节

## 🏗️ 技术架构

### 引擎层
- **Pikafish**: 基于 Fairy-Stockfish 的中国象棋引擎，支持 NNUE 神经网络评估
- **SlowRenju**: 专业五子棋引擎，支持禁手规则
- 双架构支持: `arm64-v8a` (真机) + `x86_64` (模拟器)
- CMake 构建系统，按架构条件编译

### 应用层
- **语言**: Kotlin
- **UI框架**: Jetpack Compose + Material 3
- **架构**: MVVM (ViewModel + StateFlow)
- **导航**: Navigation 3
- **异步**: Kotlin Coroutines
- **持久化**: SharedPreferences

### 核心模块
```
app/src/main/java/com/example/chessarena/
├── engine/          # 引擎接口与实现
│   ├── EngineInterface.kt      # 引擎抽象接口
│   ├── XiangqiEngine.kt        # 象棋引擎 (Pikafish)
│   └── GomokuEngine.kt         # 五子棋引擎 (SlowRenju)
├── game/            # 游戏规则与状态
│   ├── xiangqi/               # 象棋规则
│   └── gomoku/                # 五子棋规则
├── viewmodel/       # ViewModel 层
│   ├── XiangqiViewModel.kt    # 象棋游戏逻辑
│   ├── GomokuViewModel.kt     # 五子棋游戏逻辑
│   └── SettingsViewModel.kt   # 设置管理
├── ui/              # UI 层
│   ├── screens/               # 页面组件
│   └── components/            # 可复用组件
└── theme/           # 主题与样式
```

## 🚀 构建指南

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17+
- Android SDK API 34
- NDK 25.1.8937393 或更高版本
- CMake 3.22.1

### 构建步骤

1. **克隆仓库**
```bash
git clone https://github.com/luzzr-123/qidao.git
cd qidao
```

2. **配置签名** (可选，用于发布)
创建 `signing.properties`:
```properties
storeFile=/path/to/your/keystore.jks
storePassword=your_password
keyAlias=your_alias
keyPassword=your_key_password
```

3. **构建 Debug 版本**
```bash
./gradlew assembleDebug
```

4. **构建 Release 版本**
```bash
./gradlew assembleRelease
```

APK 输出位置: `app/build/outputs/apk/release/app-release.apk`

### 安装到设备

```bash
# 真机 (ARM64)
adb install app/build/outputs/apk/release/app-release.apk

# MuMu 模拟器 (x86_64)
adb connect 127.0.0.1:7555
adb -s 127.0.0.1:7555 install app/build/outputs/apk/release/app-release.apk
```

## 📱 系统要求

- **最低版本**: Android 8.0 (API 26)
- **目标版本**: Android 14 (API 34)
- **支持架构**: arm64-v8a, x86_64
- **存储空间**: ~60MB (含双架构原生引擎)

## 🎯 难度等级

### 中国象棋
| 等级 | 名称 | 描述 |
|------|------|------|
| 1 | 市级名手 | 街头业余水平，适合初学者 |
| 2 | 省级高手 | 业余比赛常客，有一定战术意识 |
| 3 | 省级大师 | 专业训练背景，布局严谨 |
| 4 | 国家大师 | 职业棋手水平，计算深度强 |
| 5 | 特级大师 | 顶尖职业水平，接近人类极限 |
| 6 | 全国冠军 | 最强AI，超越人类水平 |

### 连珠五子棋
| 等级 | 名称 | 描述 |
|------|------|------|
| 1 | 业余初段 | 基础水平，适合入门 |
| 2 | 业余三段 | 有一定战术意识 |
| 3 | 业余五段 | 熟练掌握各种定式 |
| 4 | 专业初段 | 职业入门水平 |
| 5 | 专业三段 | 顶尖职业水平 |

## 🔧 开发说明

### 引擎配置
- 象棋引擎使用 UCI 协议通信
- 五子棋引擎使用 Gomocup 协议
- 引擎健康状态实时监控
- 异常时提供用户反馈而非静默回退

### 性能优化
- 原生 C++ 引擎确保计算性能
- Compose 重组优化
- 状态管理使用 StateFlow
- 异步操作使用 Coroutines

### 已知限制
- 不支持网络对弈 (仅本地AI)
- 不支持棋谱导入/导出
- 不支持多语言 (仅中文)

## 📄 开源协议

本项目基于 MIT 协议开源。

### 第三方组件
- **Pikafish**: GPL-3.0 (基于 Fairy-Stockfish)
- **SlowRenju**: 自定义协议
- **Jetpack Compose**: Apache 2.0
- **Material 3**: Apache 2.0

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📧 联系方式

如有问题或建议，请通过 GitHub Issues 反馈。

---

**棋道** - 以棋会友，以道修身

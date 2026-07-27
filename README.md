# MengSama Net Music

[**中文**](#中文) | [**English**](#english)

---

## 中文

### 简介

MengSama Net Music 是一个功能全面的 Minecraft 音乐播放器模组，为游戏带来了完整的音乐播放体验。模组集成了网易云音乐和 QQ 音乐两大平台，支持本地音乐播放，并提供了精美的 3D 模型方块和便携式播放器。

### 核心功能

- **唱片机方块** — 拥有精美 3D 模型的音乐播放器方块，可放置在世界中播放音乐
- **随身播放器** — 便携式音乐播放器，可手持右键使用，也可放置在地上作为方块使用
- **播放列表管理** — 支持多种播放模式（顺序播放、随机播放、单曲循环）
- **网易云音乐集成** — 支持搜索歌曲、VIP 歌曲播放
- **QQ 音乐集成** — 支持 QQ 音乐歌曲搜索和播放
- **实时歌词显示** — HUD 界面显示当前播放歌曲的歌词、封面和进度
- **音乐缓存系统** — 本地缓存已播放的歌曲，减少重复下载
- **模组兼容** — 支持精妙背包（Sophisticated Backpacks）和车万女仆（Touhou Little Maid）联动

### 合成配方

**唱片机：**
```
   金
 铁 音符盒
铜 铜 铜
```
| 符号 | 材料 |
|------|------|
| 金 | 金锭 |
| 铁 | 铁锭 |
| 音符盒 | 音符盒 |
| 铜 | 铜锭 |

**随身播放器：**
```
   铜
 铁 铁
   唱片机
```
| 符号 | 材料 |
|------|------|
| 铜 | 铜锭 |
| 铁 | 铁锭 |
| 唱片机 | 唱片机方块 |

### 使用教程

1. 合成音乐播放器方块并放置在世界中，或手持随身播放器右键打开界面
2. 在播放器界面中使用网易云音乐或 QQ 音乐搜索歌曲
3. 将搜索结果加入播放器内的播放列表，并按需调整顺序或播放模式
4. 从播放列表选择歌曲开始播放；方块播放器和随身播放器均直接使用该列表，不需要制作或插入 CD
5. 随身播放器也可以放置在地上作为方块使用

### 运行环境

- Minecraft 1.20.1
- Forge 47.4.22（模组元数据兼容 Forge 46+）
- Java 17
- GeckoLib 4.2+（必需）
- Cloth Config、Patchouli、Jade、JEI、Sophisticated Backpacks 与 Touhou Little Maid 为构建或兼容性依赖，具体版本见 `build.gradle`

### 从源码构建

仓库保留了 Gradle Wrapper 与 `libs/` 中构建所需的本地音频依赖。不要使用系统安装的 Gradle；在仓库根目录运行 Wrapper。首次构建需要联网下载 Forge、映射和 Maven 依赖。

Windows PowerShell 或命令提示符：

```powershell
.\gradlew.bat clean test build --no-daemon
```

Linux/macOS：

```sh
./gradlew clean test build --no-daemon
```

构建需要可用的 Java 17 JDK。单元测试报告位于 `build/reports/tests/test/`，构建产物位于 `build/libs/`；当前构建脚本还会在项目父目录复制一份名为 `MengSamaNetMusic-1.20.1-forge-fixed.jar` 的发布 JAR。联网服务集成测试默认跳过；仅在明确需要并了解其会访问第三方服务时，使用 `-PmengsamaLiveTests=true` 启用。

### 制作团队

| 职责 | 贡献者 |
|------|--------|
| 程序开发 | **MengSama0502** & **niumadadi520** |
| UI 美工 | **YuZiJiang** |
| 模型制作 | **niumadadi520** |

### 更新日志

#### v1.2.0
- 修复随身播放器播放列表无法使用的问题
- 修复上一首/下一首按钮切换时未停止当前歌曲的问题
- 优化网络数据包分发，仅向目标玩家发送
- 唱片机与随身播放器采用不同合成配方
- 播放列表槽位扩展至 54 个

#### v1.1.0
- 移除 shift+右键从唱片机取唱片功能
- 代码注释清理与规范化
- 修复 GeckoLib 模型路径问题
- 修复随身播放器纹理 UV 映射

### 许可证与第三方声明

本项目不是统一采用 MIT：

- NetMusic 衍生及本项目 Java 代码：**BSD-3-Clause**
- `src/main/resources/assets` 与 `src/main/resources/data` 中的项目资源：**CC BY-NC-SA 4.0**（署名、非商业、相同方式共享）
- 来自 `netMusicListForge` 的部分：**MIT**，Copyright (c) 2025 gly091020
- `NetMusicCanNeedQQ`：**All Rights Reserved**，仅作为兼容性比较资料；其源码未作为第三方组件纳入本项目，QQ 互操作实现已独立重写/整理

完整文本和第三方组件说明见 JAR/源码根资源中的 `LICENSE-CODE-BSD-3-CLAUSE.txt`、`LICENSE-ASSETS-CC-BY-NC-SA-4.0.txt`、`LICENSE-NETMUSICLISTFORGE-MIT.txt` 与 `THIRD-PARTY-NOTICES.txt`。

---

## English

### Overview

MengSama Net Music is a comprehensive music player mod for Minecraft, bringing a full music playback experience to the game. The mod integrates NetEase Cloud Music and QQ Music platforms, supports local music playback, and features beautifully crafted 3D block models and a portable music player.

### Key Features

- **Music Player Block** — A beautifully modeled 3D music player block that can be placed in the world to play music
- **Portable Music Player** — A handheld music player that can be used by right-clicking or placed on the ground as a block
- **Playlist Management** — Multiple play modes (Sequential, Random, Single Loop)
- **NetEase Cloud Music Integration** — Song search, VIP song playback
- **QQ Music Integration** — QQ Music song search and playback
- **Real-time Lyrics Display** — HUD overlay showing lyrics, album cover, and playback progress
- **Music Caching System** — Local caching of played songs to reduce repeated downloads
- **Mod Compatibility** — Sophisticated Backpacks and Touhou Little Maid integration

### Crafting Recipes

**Music Player Block:**
```
     G
   I N
C C C
```
| Symbol | Material |
|--------|----------|
| G | Gold Ingot |
| I | Iron Ingot |
| N | Note Block |
| C | Copper Ingot |

**Portable Music Player:**
```
     C
   I I
     P
```
| Symbol | Material |
|--------|----------|
| C | Copper Ingot |
| I | Iron Ingot |
| P | Music Player Block |

### Usage Guide

1. Craft and place a Music Player Block, or right-click the Portable Music Player in hand to open its screen
2. Search NetEase Cloud Music or QQ Music from the player screen
3. Add search results to the player's playlist, then reorder them or select a play mode as needed
4. Select a song from the playlist to play it; both player types use this list directly, with no CD item to craft or insert
5. The Portable Music Player can also be placed on the ground as a block

### Runtime Requirements

- Minecraft 1.20.1
- Forge 47.4.22 (the mod metadata accepts Forge 46+)
- Java 17
- GeckoLib 4.2+ (required)
- Cloth Config, Patchouli, Jade, JEI, Sophisticated Backpacks, and Touhou Little Maid are build-time or compatibility dependencies; see `build.gradle` for exact versions

### Building from Source

The repository includes the Gradle Wrapper and the local audio dependencies required from `libs/`. Do not use a system Gradle installation; run the Wrapper from the repository root. The first build requires network access to download Forge, mappings, and Maven dependencies.

Windows PowerShell or Command Prompt:

```powershell
.\gradlew.bat clean test build --no-daemon
```

Linux/macOS:

```sh
./gradlew clean test build --no-daemon
```

A Java 17 JDK must be available. Test reports are written to `build/reports/tests/test/`, and build artifacts to `build/libs/`. The current build script also copies a release JAR named `MengSamaNetMusic-1.20.1-forge-fixed.jar` to the project parent directory. Live provider integration tests are skipped by default; enable them only when explicitly required and when third-party network access is acceptable, using `-PmengsamaLiveTests=true`.

### Team

| Role | Contributor |
|------|-------------|
| Programming | **MengSama0502** & **niumadadi520** |
| UI Art | **YuZiJiang** |
| 3D Models | **niumadadi520** |

### Changelog

#### v1.2.0
- Fixed handheld player playlist not working
- Fixed previous/next buttons not stopping current song before switching
- Optimized network packet distribution to target only specific players
- Differentiated crafting recipes for Music Player Block and Portable Music Player
- Expanded playlist slots to 54

#### v1.1.0
- Removed shift+right-click CD extraction from music player blocks
- Code comment cleanup and standardization
- Fixed GeckoLib model path issue
- Fixed portable player texture UV mapping

### License and Third-Party Notices

This project is not uniformly MIT-licensed:

- NetMusic-derived and project Java code: **BSD-3-Clause**
- Project resources under `src/main/resources/assets` and `src/main/resources/data`: **CC BY-NC-SA 4.0** (Attribution, NonCommercial, ShareAlike)
- Portions derived from `netMusicListForge`: **MIT**, Copyright (c) 2025 gly091020
- `NetMusicCanNeedQQ`: **All Rights Reserved**. It was used only as a compatibility comparison reference; its source is not included, and the QQ interoperability implementation was independently reworked.

The complete texts and bundled-component notices are available at the root of the source distribution and final JAR: `LICENSE-CODE-BSD-3-CLAUSE.txt`, `LICENSE-ASSETS-CC-BY-NC-SA-4.0.txt`, `LICENSE-NETMUSICLISTFORGE-MIT.txt`, and `THIRD-PARTY-NOTICES.txt`.
# MengSama Net Music

[**中文**](#中文) | [**English**](#english)

---

## 中文

### 简介

MengSama Net Music 是一个功能全面的 Minecraft 音乐播放器模组，集成了网易云音乐和 QQ 音乐两大平台，支持本地音乐播放，并提供了精美的 3D 模型唱片机方块和便携式随身播放器。

### 版本分支

本项目通过 Git 分支管理不同 Minecraft 版本和模组加载器的构建。**main 分支仅包含此 README 文件**，请根据你的需求切换到对应的分支：

| 分支 | Minecraft | 模组加载器 | Java | 构建工具 |
|------|-----------|-----------|------|---------|
| [`1.20.1-forge`](https://github.com/MengSama0502/MengSamaNetMusic/tree/1.20.1-forge) | 1.20.1 | Forge 46+ | 17 | ForgeGradle 6.0 |
| [`1.20.1-neoforge`](https://github.com/MengSama0502/MengSamaNetMusic/tree/1.20.1-neoforge) | 1.20.1 | NeoForge 1+ | 17 | NeoGradle 6.0 |
| [`1.20.1-fabric`](https://github.com/MengSama0502/MengSamaNetMusic/tree/1.20.1-fabric) | 1.20.1 | Fabric | 17 | Fabric Loom 1.6 |
| [`1.21.1-forge`](https://github.com/MengSama0502/MengSamaNetMusic/tree/1.21.1-forge) | 1.21.1 | Forge | 21 | NeoGradle 7.0 |
| [`1.21.1-neoforge`](https://github.com/MengSama0502/MengSamaNetMusic/tree/1.21.1-neoforge) | 1.21.1 | NeoForge | 21 | NeoGradle 7.0 |
| [`1.21.1-fabric`](https://github.com/MengSama0502/MengSamaNetMusic/tree/1.21.1-fabric) | 1.21.1 | Fabric | 21 | Fabric Loom 1.7 |

### 核心功能

- **唱片机方块** — 拥有精美 3D 模型的音乐播放器方块，可放置在世界中播放音乐
- **随身播放器** — 便携式音乐播放器，可手持右键使用，也可放置在地上作为方块使用
- **播放列表管理** — 支持多种播放模式（顺序播放、随机播放、单曲循环），最多 54 首歌曲
- **网易云音乐集成** — 支持搜索歌曲、歌单导入、VIP 歌曲播放（需登录）
- **QQ 音乐集成** — 支持 QQ 音乐歌曲搜索和播放，扫码登录
- **实时歌词显示** — HUD 界面显示当前播放歌曲的歌词、封面和播放进度
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

1. 合成唱片机并放置在世界中
2. 使用网易云音乐或 QQ 音乐搜索歌曲
3. 制作音乐唱片或播放列表
4. 将唱片放入唱片机或随身播放器即可播放
5. 随身播放器可以手持右键打开 GUI，也可以放置在地上使用

### 构建指南

```bash
# 克隆仓库并切换到对应分支
git clone https://github.com/MengSama0502/MengSamaNetMusic.git
cd MengSamaNetMusic
git checkout 1.20.1-forge   # 或选择其他分支

# 使用 Gradle Wrapper 构建
./gradlew build
```

构建产物位于 `build/libs/` 目录下。

### 依赖项

| 依赖 | 1.20.1 版本 | 1.21.1 版本 |
|------|------------|------------|
| GeckoLib | 4.2+ | 4.5+ |
| Cloth Config（可选） | 11.1.0+ | 15.0.0+ |
| 车万女仆（可选） | 1.5.2+ | - |
| Patchouli（可选） | 1.20.1-84+ | - |

### 制作团队

| 职责 | 贡献者 |
|------|--------|
| 程序开发 | **MengSama0502** & **niumadadi520** |
| UI 美工 | **YuZiJiang** |
| 模型制作 | **niumadadi520** |

### 开源协议

MIT License

---

## English

### Overview

MengSama Net Music is a comprehensive music player mod for Minecraft, integrating NetEase Cloud Music and QQ Music platforms, supporting local music playback, and featuring beautifully crafted 3D music player blocks and a portable handheld music player.

### Version Branches

This project uses Git branches to manage builds for different Minecraft versions and mod loaders. **The main branch contains only this README file**. Please switch to the appropriate branch for your needs:

| Branch | Minecraft | Mod Loader | Java | Build Tool |
|--------|-----------|-----------|------|------------|
| [`1.20.1-forge`](https://github.com/MengSama0502/MengSamaNetMusic/tree/1.20.1-forge) | 1.20.1 | Forge 46+ | 17 | ForgeGradle 6.0 |
| [`1.20.1-neoforge`](https://github.com/MengSama0502/MengSamaNetMusic/tree/1.20.1-neoforge) | 1.20.1 | NeoForge 1+ | 17 | NeoGradle 6.0 |
| [`1.20.1-fabric`](https://github.com/MengSama0502/MengSamaNetMusic/tree/1.20.1-fabric) | 1.20.1 | Fabric | 17 | Fabric Loom 1.6 |
| [`1.21.1-forge`](https://github.com/MengSama0502/MengSamaNetMusic/tree/1.21.1-forge) | 1.21.1 | Forge | 21 | NeoGradle 7.0 |
| [`1.21.1-neoforge`](https://github.com/MengSama0502/MengSamaNetMusic/tree/1.21.1-neoforge) | 1.21.1 | NeoForge | 21 | NeoGradle 7.0 |
| [`1.21.1-fabric`](https://github.com/MengSama0502/MengSamaNetMusic/tree/1.21.1-fabric) | 1.21.1 | Fabric | 21 | Fabric Loom 1.7 |

### Key Features

- **Music Player Block** — A beautifully modeled 3D music player block that can be placed in the world to play music
- **Portable Music Player** — A handheld music player that can be used by right-clicking or placed on the ground as a block
- **Playlist Management** — Multiple play modes (Sequential, Random, Single Loop), up to 54 songs
- **NetEase Cloud Music Integration** — Song search, playlist import, VIP song playback (login required)
- **QQ Music Integration** — QQ Music song search and playback, QR code login
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

1. Craft a Music Player Block and place it in the world
2. Search for songs using NetEase Cloud Music or QQ Music
3. Create music CDs or playlists
4. Insert CDs into the Music Player or Portable Music Player to start playback
5. The Portable Music Player can be right-clicked in hand to open the GUI, or placed on the ground

### Build Guide

```bash
# Clone the repository and switch to the desired branch
git clone https://github.com/MengSama0502/MengSamaNetMusic.git
cd MengSamaNetMusic
git checkout 1.20.1-forge   # or choose another branch

# Build with Gradle Wrapper
./gradlew build
```

The build artifact will be in the `build/libs/` directory.

### Dependencies

| Dependency | 1.20.1 Version | 1.21.1 Version |
|------------|---------------|---------------|
| GeckoLib | 4.2+ | 4.5+ |
| Cloth Config (Optional) | 11.1.0+ | 15.0.0+ |
| Touhou Little Maid (Optional) | 1.5.2+ | - |
| Patchouli (Optional) | 1.20.1-84+ | - |

### Team

| Role | Contributor |
|------|-------------|
| Programming | **MengSama0502** & **niumadadi520** |
| UI Art | **YuZiJiang** |
| 3D Models | **niumadadi520** |

### License

MIT License
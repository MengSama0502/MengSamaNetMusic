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

1. 合成唱片机并放置在世界中
2. 使用网易云音乐或 QQ 音乐搜索歌曲
3. 制作音乐唱片或播放列表
4. 将唱片放入唱片机或随身播放器即可播放
5. 随身播放器可以手持右键打开 GUI，也可以放置在地上使用

### 客户端需求

- 需要安装 GeckoLib 4.2+

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

### 开源协议

MIT License

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

1. Craft a Music Player Block and place it in the world
2. Search for songs using NetEase Cloud Music or QQ Music
3. Create music CDs or playlists
4. Insert CDs into the Music Player or Portable Music Player to start playback
5. The Portable Music Player can be right-clicked in hand to open the GUI, or placed on the ground

### Client Requirements

- GeckoLib 4.2+ is required

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

### License

MIT License
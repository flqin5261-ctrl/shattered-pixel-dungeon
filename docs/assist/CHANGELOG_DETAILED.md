# Assist MOD 逐版本详细变更史

本文件记录 Shattered Pixel Dungeon · Assist 从基础分支到当前版本的完整演进。

目的不是做面向玩家的简短 changelog，而是给后续 ChatGPT / Codex / 维护者提供“为什么会变成现在这样”的技术历史。

如果需要了解当前架构，请配合阅读：

- `ASSIST_HANDOFF.md`
- `ARCHITECTURE_AND_INVARIANTS.md`
- `BUG_HISTORY_AND_FIXES.md`
- `ROADMAP_AND_TEST_CHECKLIST.md`

---

# 0. 分支/版本总表

| 阶段 | 版本 | versionCode | 主要分支 | 代表 SHA |
|---|---:|---:|---|---|
| Base | 4.0.0-assist-base1 | 913 | assist-base1 | 8b957872fb2ac304746d17b78687cc82614f337f |
| Assist 初版 | 0.1 | 915 | assist-0.1-stable | c190b323d8cf780789de7dd0c342cb9626460021 |
| Extra Challenge | 0.2 | 917 | assist-0.2-stable | b2758416ab674cb1834e1249a6849e44953355dd |
| Extra Challenge 修复 | 0.2.1 | 918 | assist-0.2.1-stable | a99ab69e289398184609f8b9f8fb06940c663d00 |
| Infinite World 原型 | 0.3 | 919 | assist-0.3-stable | 2af0eb8eccb797d52fb1026e0b6891c876ec9fbf |
| Worldgen v2 | 0.3.1 | 920 | assist-0.3.1-stable | 9de8f8d68e0949f303c882a5769daaca54758a88 |
| 世界坐标/重开 | 0.3.2 | 921 | assist-0.3.2-stable | 6fcb9a57749506e3ec18bd6d9b182539c464cfec |
| 路径/重开/Mapping 修复 | 0.3.3 | 922 | assist-0.3.3-stable | 379945bc28e55ee443fcf58538834a592eea1556 |
| Streaming 稳定化 | 0.3.4 | 923 | assist-0.3.4-stable | df86c0635ff14061589e4d2b7db82692f2ae0f81 |
| 视觉主题稳定化 | 0.3.5 | 924 | assist-0.3.5-stable | eee190fadeb8dabb0a8ddc56f97e983da8080a78 |
| Render/VBO 修复 | 0.3.6 | 925→928 | assist-0.3.6-renderfix | bd89137057edacf6a8d9d12b8250ddf14c4d8078 |
| 世界状态/缩放优化 | 0.3.7 | 929 | assist-0.3.7-stable | b1f53c7e52c94e863b9ad2c8aaae512cfc5d8c3a |
| 互动/混贴图/传送 | 0.3.8 | 930 | assist-0.3.8-stable | 4875c78ba2c0ebcf54d77a2c08b2ddb7b18a8199 |
| 高密度内容 | 0.3.9 | 940 | assist-0.3.9-stable | 45db5abb4f8efbb8c15e4b67d35221406291f515 |
| 世界物资/钥匙/环境 | 0.4.0 | 941 | assist-0.4.0-stable | aa757bf4973e737a3cdf1f843cd19eca65537359 |
| 真主题房间 | 0.4.1 | 942 | assist-0.4.1-stable | b33d6c7402245d72da50cce292469c0db7f953db |
| 秘密房/房间级主题 | 0.4.2 | 943 | assist-0.4.2-stable | 21eb31d97ba0bf46b920a6f622a587ea97c560f0 |
| 无限骨架/阈限异常区 | 0.4.3 | 944 | assist-0.4.3-stable | a513fc1a099d272c356c3127f874c3dd861e2799 |
| 多层无限支线网络 | 0.4.4 | 945 | assist-0.4.4-stable | 02689bb5b1ed0d91424de21779d6f94cc96c8c83 |
| 玩家中心怪物生态 | 0.5.0 | 946 | assist-0.5.0-stable | c01cabebd7e9c453c11b3dc0f886a1579d9543fd |
| 稀有商人据点 | 0.5.1 | 947 | assist-0.5.1-stable | e14bac5c266d6b0cad3189ace9dc7492967ac74b |

特别说明：

- `assist-0.3.6-stable` 仍停在早期 build 925。
- 真正完成 0.3.6 render 修复的开发分支是 `assist-0.3.6-renderfix`，最终 build 928。
- 0.3.7 之后已经包含 0.3.6 后续 render 修复，不应回退到 0.3.6-stable 作为新开发基线。

---

# 1. assist-base1 — 项目基础版

版本：

- `4.0.0-assist-base1`
- versionCode 913
- branch：`assist-base1`

核心工作：

1. 从 Shattered Pixel Dungeon v4.0.0 建立 Assist MOD 基础。
2. 单独 applicationId：
   `com.shatteredpixel.shatteredpixeldungeon.assist`
3. 单独 App 名：
   `Shattered Pixel Dungeon · Assist`
4. 建立独立 GitHub Actions release APK 构建链。
5. 固定使用同一 AOSP test key 签名。
6. 目标是后续所有 Assist 版本都能原位覆盖安装。

这一阶段最重要的意义不是功能，而是建立“稳定包名 + 固定签名 + 可重复构建”。

后续任何版本都必须保留这三个基础条件。

---

# 2. 0.1 — Assist 基础功能

版本：

- 0.1
- versionCode 915
- stable：`assist-0.1-stable`

历史提交包括：

- Remove duplicate Assist entry from game menu
- Center Assist gear icon
- Simplify Assist UI interactions for version 0.1
- Set Assist version 0.1
- Fix Assist upgrades to execute real item upgrade logic
- Pixel-center the Assist gear icon
- Bump 0.1 hotfix version code

主要功能：

## 2.1 Assist 菜单

加入专门 Assist UI 与 HUD 齿轮入口。

修过的问题：

- 菜单入口重复
- 图标对不齐
- UI 交互过于复杂

最后保留顶部独立 Assist gear。

## 2.2 基础作弊/辅助项

已经形成后来一直保留的 Assist 能力：

- 无敌
- 物品/金币只增加
- 装备升级
- 自定义升级量
- 移动速度倍率
- 神器相关辅助
- 指定楼层传送

## 2.3 装备升级修复

早期直接改 level 数字会导致部分装备没有执行原版升级副作用。

修复为：

```java
item.upgrade(amount)
```

Mage Staff 额外：

```java
updateWand(false)
```

这个修复必须一直保留。

---

# 3. 0.1 movement hotfix → 0.2 前置

在进入 0.2 前，做过一次速度表现修复：

- Sync hero movement animation duration with Assist speed
- Sync hero run animation with Assist speed

解决：

> 实际移动速度已经变快，但 Hero 动画/tween 仍是原速，看起来滑动、脱节。

最终逻辑速度和视觉移动速度同步。

---

# 4. 0.2 — Extra Challenge 框架 + 镜庭

版本：

- 0.2
- versionCode 917
- stable：`assist-0.2-stable`

主要提交：

- Add isolated save slot for Extra Challenge
- Add standalone Extra Challenge dungeon mode
- Add handcrafted one-floor Mirror Court extra challenge
- Add isolated hero-select flow for Extra Challenge
- Add Mirror Court entry
- Keep floor teleport out of standalone Extra Challenge

新增了“额外挑战”体系。

它和原版 Challenge 不同，是独立模式入口。

## 4.1 镜庭

设计：

- 35×35
- 手工固定地图
- 单层
- 无怪
- 正常门/钥匙/箱子/水/草
- 独立存档
- 独立 Hero Select 流程

这一步为后来的 Infinite World 提供了：

- 独立模式
- 独立存档槽
- 不影响原版正常模式

的框架。

---

# 5. 0.2.1 — 镜庭楼梯修复

版本：

- 0.2.1
- versionCode 918
- stable：`assist-0.2.1-stable`

提交：

- Prevent leaving Extra Challenge through entrance stairs

问题：

镜庭虽然设计为单层，但 `REGULAR_ENTRANCE` 仍可能被当成可使用的层间入口。

修复后：

- Entrance 只作为出生/定位锚点
- 不能离开单层模式

---

# 6. 0.3 — Infinite World 第一版

版本：

- 0.3
- versionCode 919
- stable：`assist-0.3-stable`

主要提交：

- Add persistent state for infinite world prototype
- Add streaming 3x3 chunk infinite world prototype
- Integrate infinite world mode and persistent state into Dungeon
- Add isolated save slot for infinite world
- Add hero-select flow for infinite world mode
- Add Infinite Dungeon choices under Extra Challenge
- Shift infinite-world active window when hero crosses chunk boundary
- Disable floor teleport inside infinite world

这是整个项目最大转折点。

## 6.1 核心概念

Infinite World 不是无限 floor。

目标改成：

> 同一平面水平无限向四方向移动。

第一版使用较小 streaming 窗口，建立了：

- InfiniteWorldLevel
- InfiniteWorldState
- 独立存档
- Chunk 坐标
- 世界切窗

## 6.2 初期问题

第一版地图：

- 规律感很强
- 容易重复
- 视觉上像规则格子拼接
- streaming 仍很原始

但架构可行性得到验证。

---

# 7. 0.3.1 — Generator v2 + 7×7 Streaming

版本：

- 0.3.1
- versionCode 920
- stable：`assist-0.3.1-stable`

主要提交：

- Persist generated chunks and bump infinite world generator to v2
- Redesign infinite world with persistent 7x7 streaming and pseudo-random generator v2
- Refresh streamed infinite world in place without GameScene rebuild
- Track explored infinite-world chunks separately from generated chunks
- Keep explored infinite-world chunks fully remembered
- Keep explored infinite-world terrain free of black fog
- Preserve generator v1 formulas for existing 0.3 worlds
- Use new save slot for infinite world v2

## 7.1 活动窗口扩大

变成：

- Chunk 24×24
- 7×7 Chunk
- 168×168 cells

目标：

- 边缘有安全缓冲
- 避免频繁切换
- 为未来传送/怪物留下空间

## 7.2 Generated 与 Explored 分离

早期容易把：

- “Chunk 已生成”
- “玩家已经探索”

混成一个概念。

0.3.1 分开追踪。

这非常重要。

生成某 Chunk 不代表玩家应该知道它的地图。

## 7.3 旧世界兼容

引入 generatorVersion。

旧 v1 世界继续使用旧公式，避免升级后已走过地形全部变化。

这个原则一直沿用到后来的 V8。

---

# 8. 0.3.2 — 绝对世界坐标 + 真正重开新 Seed

版本：

- 0.3.2
- versionCode 921
- stable：`assist-0.3.2-stable`

提交：

- Persist absolute hero world coordinates in infinite world
- Add fresh random seed initializer for infinite world
- Always use a new seed when restarting infinite world
- Use absolute world coordinates for infinite-world streaming
- Add seamless hero/camera rebase for infinite world
- Rebase streamed world without camera snapping to local spawn coordinates
- Fix streamed shift values for render callback

新增核心状态：

- `heroWorldX`
- `heroWorldY`

原因：

168×168 只是当前 local window。

真正无限世界必须知道 Hero 在世界中的绝对位置。

## 8.1 Restart

重新开始 Infinite World 时：

- 删除完整 Infinite World save
- 清 state
- 初始化新 seed
- 重新 Hero Select
- 新世界不是旧 seed 重开

---

# 9. 0.3.3 — 路径目标、彻底重开、Mapping 限制

版本：

- 0.3.3
- versionCode 922
- stable：`assist-0.3.3-stable`

提交：

- Fix stale path targets and separate generated from explored terrain
- Stop stale movement tween before infinite-world rebase
- Add complete hidden-slot deletion for infinite-world restart
- Guarantee a fresh seed for every new infinite world
- Hard-reset Infinite World data and seed before hero selection
- Limit magic mapping to nearby infinite-world region
- Fully clear stale hero motion during world rebase

## 9.1 Stale Target

长距离点击移动时，HeroAction 目标是 local cell。

Streaming 后 local grid 换了，但旧 dst 还存在。

结果可能：

- 自动走错
- 卡住
- 角色原地动画
- 目标落在另一块地形

修复：

- shift 前转换为绝对坐标
- shift 后 rebase
- 超出窗口则取消

## 9.2 Magic Mapping

原版 Mapping 是“整层”。

Infinite World 根本没有有限整层。

早期先限制为附近区域，而不是全 168×168 或无限世界。

后来 0.3.8 进一步直接禁用 Mapping 类消耗品。

---

# 10. 0.3.4 — Streaming 稳定版

版本：

- 0.3.4
- versionCode 923
- stable：`assist-0.3.4-stable`

提交：

- Move infinite-world streaming to completed movement and add hysteresis
- Stream infinite world only after hero movement finishes
- Make infinite-world sprite rebase safe for actor waiters

这是一个关键稳定节点。

## 10.1 修复“走一段后卡在原地移动”

原始问题：

- Hero tween 尚未完成
- 地图先 streaming shift
- Actor/sprite waiter 状态被打乱
- 左上角 loading/turn indicator 持续转

最终原则：

> Streaming 只在 Hero motion complete 后发生。

## 10.2 修复地图 A/B 主题闪烁

旧阈值切换后容易立即触发反方向 shift。

最终使用：

```java
SHIFT_LOW = CHUNK_SIZE;              // 24
SHIFT_HIGH = MAP_SIZE - CHUNK_SIZE; // 144
SHIFT_STEP = 3;
```

这套 hysteresis 是已知稳定方案。

后续不要随便改回“每跨一个 Chunk 就 shift”。

---

# 11. 0.3.5 — 稳定视觉主题

版本：

- 0.3.5
- versionCode 924
- stable：`assist-0.3.5-stable`

提交：

- Bump infinite world generator version for stable visual themes
- Stabilize infinite-world tiles and randomize per-world visual theme
- Make infinite-world tile variants stable by absolute world coordinate
- Refresh all infinite-world visual layers before showing water background

## 11.1 每世界主题

从原版五种环境中选择：

- Sewers
- Prison
- Caves
- City
- Halls

当时是整个世界固定一种主题。

## 11.2 Tile Variance 稳定

原版 tile alt 可能按 local pos 变化。

Infinite World streaming 后同一世界坐标会落到不同 local pos。

所以如果不改：

> 走开再回来，同一块地砖纹理会变。

修复：

`stableTileVariance(pos)`

使用：

- absolute world X/Y
- world seed

决定变体。

---

# 12. 0.3.6 — VBO / 水 / 黑屏渲染大战

这是历史上最重要的 Bug 修复阶段之一。

开发分支：

- `assist-0.3.6-renderfix`

版本号：

- 0.3.6
- build 从 925 一路到 928

### 重要分支差异

`assist-0.3.6-stable` 只停在：

- build 925
- SHA `15ed16dac20b5054a8e4b4a7d14c1359cfe2c40e`

真正后续修复在：

- `assist-0.3.6-renderfix`
- build 928
- SHA `bd89137057edacf6a8d9d12b8250ddf14c4d8078`

历史提交顺序：

1. Add synchronous Tilemap buffer flush for streamed worlds
2. Render infinite-world water cells explicitly instead of exposing backdrop
3. Synchronize terrain and remove water backdrop
4. Set version 0.3.6
5. Build branch
6. Actually upload streamed tile VBOs during synchronous flush
7. Restore normal water backdrop after true synchronous tile upload
8. Use original water rendering path in infinite world
9. Bump build 926
10. Keep real water backdrop visible during streamed refresh
11. Bump build 927
12. Support VBO vertex offsets for large quad batches
13. Batch large tilemaps below unsigned-short index limit
14. Bump build 928

## 12.1 最初症状：巨大水区域

走远后：

- 新地图区域先变成巨大蓝/青色水
- 走一阵/等一会地形才出现

一开始误以为是 water layer 遮住地形。

## 12.2 第一次错误修复

尝试：

- 隐藏完整 water backdrop
- 让 Terrain tilemap 自己画 WATER

结果：

> 水没了，但大片地面变成黑色。

原因：

原版水本来就由 full-level `SkinnedBlock water` 提供。

Terrain Tilemap 对真正 WATER cell 是故意 skip 的。

所以永久隐藏 water backdrop 本身就是错的。

## 12.3 真正根因

`Tilemap.updateMap()` 只更新 CPU-side 数据。

`Vertexbuffer.updateVertices()` 只是标 dirty。

真正 GPU 上传需要：

`Vertexbuffer.updateGLData()`

普通游戏是在 draw path 才触发。

Infinite World 同一帧进行：

- map 重建
- Hero rebase
- camera 重定位

如果 VBO 尚未上传，新 frame 可能看到旧 buffer / 空 buffer。

## 12.4 最终修复

`Tilemap.flushMapUpdate()` 中真正执行：

```java
buffer.updateGLData();
```

Streaming refresh 时同步 flush。

然后恢复原版 water backdrop。

## 12.5 大 Tilemap index 问题

168×168 比原版普通楼层大很多。

后续还处理了：

- VBO vertex offset
- large quad batch
- unsigned-short index limit

否则大地图一次 draw 可能超过 index 范围。

这也是为什么不能随便把 7×7 window 再暴力扩大。

---

# 13. 0.3.7 — 探索状态与性能

版本：

- 0.3.7
- versionCode 929
- stable：`assist-0.3.7-stable`

提交：

- Store infinite-world exploration as compact chunk bitsets
- Cache streamed chunks and reuse current base terrain
- Flush infinite-world fog synchronously after streaming
- Clamp infinite-world zoom and flush streamed fog
- Limit infinite-world zoom-out to 2x
- Persist explored cells incrementally while walking
- Persist infinite-world visibility incrementally
- Persist mapped infinite-world cells immediately

## 13.1 探索数据压缩

visited / mapped 不再粗暴保存超大 local window。

使用 Chunk 级 bitset 保存探索历史。

目标：

- 跑得很远仍可保存
- 不随世界面积线性爆炸成一个巨大全局数组

## 13.2 Chunk Cache

加入最近 Chunk 缓存。

当前核心：

- LRU
- limit 128

用于减少来回走动时重复生成。

## 13.3 Fog 同步

Streaming 后 Fog 也必须同步 refresh。

否则地形已经出来，Fog 还落后一帧或出现旧黑雾。

## 13.4 Zoom 限制

用户反馈缩太远以后容易看到活动窗口边界/黑雾异常。

最初限制到 2x。

---

# 14. 0.3.8 — 互动世界成型

版本：

- 0.3.8
- versionCode 930
- stable：`assist-0.3.8-stable`
- Generator V4

主要提交：

- Allow 1.8x minimum zoom in infinite world
- Disable magic mapping in infinite world
- Disable foresight scroll in infinite world
- Disable clairvoyance stone in infinite world
- Preserve infinite-world exploration through warping traps
- Bump generator to v4
- Add v4 strange districts, mixed accents, loot and teleport policy
- Add mixed-region floor accent tilemap
- Refresh mixed chunk overlays during streaming
- Enable pickaxe mining on infinite-world ore
- Let bombs carve infinite-world walls
- Keep chains synchronized with infinite-world coordinates
- Give infinite-world teleports safe explored and frontier policies
- Refresh wall discovery after explosions

## 14.1 Zoom 1.8x

用户觉得 2x 限制过大。

改为最低 1.8x。

同时修复 pinch zoom 松手后取整数导致又弹回 2x：

Infinite World 使用 0.1 精度 round。

## 14.2 Mapping 系彻底禁用

禁用：

- ScrollOfMagicMapping
- ScrollOfForesight
- StoneOfClairvoyance

原因：

Infinite World 不存在“整层地图”，大范围探图很容易破坏 explored/mapped 逻辑。

## 14.3 WarpingTrap

原版可能清掉 visited/mapped。

Infinite World 下禁止它清空永久探索历史。

## 14.4 Generator V4

加入：

- strange landmarks
- district
- 更怪异结构
- 更多 loot
- region style

## 14.5 Mixed floor tiles

新增：

`InfiniteWorldAccentTilemap`

最初只在安全 floor 类型上混其他区域 tileset。

## 14.6 炸墙

Bomb 在 Infinite World 可以破坏：

- WALL
- WALL_DECO
- MINE_CRYSTAL
- MINE_BOULDER

外围窗口边界保留不可破坏。

## 14.7 Pickaxe

Infinite World 加入：

- WALL_DECO
- MINE_CRYSTAL
- MINE_BOULDER

挖掘。

## 14.8 Chains

Ethereal Chains 改变 Hero pos 后同步绝对 world coord。

## 14.9 Teleport

建立“安全传送策略”：

- 普通随机传送偏向已探索
- Teleport scroll 可偏向未探索边疆
- 限制在安全 active window 内
- 传送后同步 absolute coords

## 14.10 音乐

这一阶段开始不再要求整个 Infinite World 永久循环单一主题音乐，后来整理为多区域曲目队列。

---

# 15. 0.3.9 — 高密度世界

版本：

- 0.3.9
- versionCode 940
- stable：`assist-0.3.9-stable`
- Generator V5

提交：

- Bump infinite world generator to v5
- Make v5 chunks denser and fill them with deterministic interactions

用户反馈：

> 虽然地图能跑了，但空、大、没意思。

所以这一版重点不是 streaming，而是密度。

## 15.1 空间 Profile

加入多种空间尺度：

- Tiny cell / closet
- Compact
- Normal
- Broad hall
- Stretched/asymmetric

不同 Chunk 可能：

- 很多小房间
- 一两个大厅
- 长走廊
- 混合

## 15.2 更窄走廊

紧凑区域大量使用 1-cell 路线。

Broad 区域才会主动扩宽。

## 15.3 更多 Terrain Props

随机撒：

- SECRET_TRAP
- ALCHEMY
- BARRICADE
- BOOKSHELF
- HIGH_GRASS
- EMBERS
- MINE_CRYSTAL
- STATUE
- EMPTY_DECO

## 15.4 真 Trap 对象

不仅放 Terrain.TRAP。

还恢复真实 Trap object：

- TeleportationTrap
- BurningTrap
- ChillingTrap
- PoisonDartTrap
- OozeTrap
- ConfusionTrap
- GrippingTrap

## 15.5 容器

每 Chunk 额外生成容器：

- Chest
- Tomb
- Skeleton

并加入 deterministic/persistent state。

---

# 16. 0.4.0 — 世界物资、植物、钥匙、水晶箱、多环境

版本：

- 0.4.0
- versionCode 941
- stable：`assist-0.4.0-stable`
- Generator V6

提交：

- Make infinite-world keys global across the world
- Allow global keys to unlock infinite-world locks
- Add persistent object state for world loot and plants
- Support full mixed-environment chunk overlays
- Refresh mixed wall environments during streaming
- Add v6 plants, global keys, crystal chests and broad world loot
- Make v6 environment districts visibly change tilesets
- Render complete alternate environment wall layers safely
- Fix v6 plant level reference

用户反馈当时缺：

- 植物
- 食物
- 钥匙
- 水晶宝箱
- 装备
- 武器
- 卷轴
- 环境变化

0.4.0 逐项补齐。

## 16.1 objectStates

InfiniteWorldState 新增 generic object state。

用于：

- 植物是否已触发
- loose loot 是否已拿
- deterministic object 是否仍存在

## 16.2 Key 全球化

Infinite World 下同类型 Key 不再按 depth 分隔。

用户要求“银钥匙开地图上所有锁门”。

实现入口：

`Key.isSimilar()`

Infinite World 下：

- 类型一样即可视为可用
- 忽略旧 depth 绑定

Hero 中原版 branch 限制也做了 Infinite World 例外。

## 16.3 Plants

真实植物随机生成。

## 16.4 Loose Loot

加入：

- Food
- Weapon
- Armor
- Wand
- Ring
- Scroll
- Potion
- Seed
- Bomb
- Blink stone
- Torch
- Key

## 16.5 Crystal Chest

加入 `Heap.Type.CRYSTAL_CHEST`。

## 16.6 Environment District

从“少量 floor patch”推进到：

- Chunk 大面积 floor
- wall
- 多区域 tileset

并在 streaming 时刷新 customWalls。

---

# 17. 0.4.1 — 真正封闭主题房间

版本：

- 0.4.1
- versionCode 942
- stable：`assist-0.4.1-stable`
- Generator V7

提交：

- Bump generator to v7 themed rooms
- Generate enclosed themed rooms with real doors and locks
- Fill themed rooms with plants, loot, keys and crystal vaults
- Bias v7 toward compact room-and-corridor layouts

用户反馈 0.4.0：

> 东西是多了，但还是“开拓地带里撒东西”，不像真正地牢。

所以 0.4.1 建立：

- 真墙
- 真房间
- 真门
- 走廊连接

## 17.1 房间数量

每 Chunk 通常：

- 2 个
- 部分 3 个

## 17.2 房间主题

V7：

- 植物房
- 卷轴房
- 药剂房
- 食物房
- 钥匙房
- 武器库
- 水晶宝库
- 工坊/陷阱房

## 17.3 门

V7 锁门比例偏高。

这是后来用户明确指出的问题。

当时逻辑：

- 许多主题房都约 58% 概率 LOCKED_DOOR
- 某些基础房例外

结果用户体感：

> “门有了，但好像全是锁门。”

这是 0.4.2 需要修的来源。

## 17.4 钥匙保障

锁门外生成对应 IronKey。

Crystal Vault 外生成 CrystalKey。

避免 soft lock。

---

# 18. 0.4.2 — 秘密房、普通门、金币园、房间级主题

版本：

- 0.4.2
- versionCode 943
- stable：`assist-0.4.2-stable`
- Generator V8

提交：

- Bump infinite world generator to v8 secret rooms
- Mix unlocked, locked and hidden room entrances in v8
- Add dense plant rooms, gold gardens and rich secret caches
- Persist v8 gold and secret-room rewards
- Reserve themed-room entrances for guaranteed key access
- Give each v8 themed room its own environment tileset
- Keep mutable doors out of static mixed-theme overlays
- Keep secret rooms visually concealed until discovered

这是当前最新版。

---

## 18.1 修复“全是锁门”的体感

V8 门策略：

- index 0 房间保证普通 DOOR
- 后续房间：
  - 一部分 LOCKED_DOOR
  - 一部分 SECRET_DOOR
  - 其余普通 DOOR

Key Room 永远普通门。

Crystal Vault 不再强制铁钥匙门。

---

## 18.2 Secret Room

直接使用原版：

`Terrain.SECRET_DOOR`

而不是自定义伪装。

因此继承原版：

- 未发现像墙
- 自动搜索
- 主动搜索
- Talisman/search 逻辑
- discover → DOOR

秘密房会附加额外高价值奖励。

---

## 18.3 Gold Garden

新增 Gold Garden。

内部：

- 小型原版风格房间
- 多堆 Gold
- 每堆数量确定性随机

目标不是大矿区，而是“小房间里密密麻麻金币”的奖励感。

---

## 18.4 Plant Garden 加密

V8 植物房：

- 从约 7 株
- 增加到约 12 株

并继续使用真实 Plant。

---

## 18.5 房间尺寸

V7 常见外尺寸约 6～8。

V8 调成更接近原版：

- 5～8 tile
- 少量较大特殊房

设计参考原版：

- Room
- PlantsRoom
- GardenRoom
- SecretGardenRoom
- SecretHoardRoom
- SecretLarderRoom
- SecretLaboratoryRoom
- TreasuryRoom
- CrystalVaultRoom

---

## 18.6 原版 Room 系统研究结果

原版确实大量使用随机生成。

主要机制：

1. Room 有 min/max width/height。
2. Room 有 neighbour / connected。
3. Door 有类型：
   - REGULAR
   - HIDDEN
   - LOCKED
   - CRYSTAL
   - 等
4. Room.paint() 自己决定：
   - 墙
   - 地板
   - 草
   - 水
   - 物品
   - 植物
   - 陷阱
5. 整层先建立 Room Graph，再 painter 一次性完成。

Infinite World 没直接搬整套。

原因：

- 原版有有限层边界
- 有入口出口
- 全 Room 一次性已知
- Infinite World 只知道当前流式窗口
- Chunk 需要独立确定性生成

所以目前采用：

> 原版 Room 设计思想 + Infinite World deterministic chunk 房间模板。

---

## 18.7 房间级 tileset

V8 每个主题房间可以独立选择：

- Sewers
- Prison
- Caves
- City
- Halls

并覆盖：

- floor
- wall

所以一个 Chunk 里可以同时出现多种视觉环境。

---

## 18.8 修复锁门贴图残留

0.4.1 实机发现：

> 用钥匙解锁后，逻辑门已经变化，但锁门旧贴图仍在。

根因：

`InfiniteWorldAccentTilemap` 是 static custom overlay。

创建时如果门是 LOCKED_DOOR，它把锁门视觉写进自己的 mesh。

后来逻辑 Terrain 改为 DOOR，但 overlay 没跟着重建。

0.4.2 处理：

不允许静态 mixed-theme overlay 绘制动态门。

排除：

- DOOR
- OPEN_DOOR
- LOCKED_DOOR
- HERO_LKD_DR
- CRYSTAL_DOOR
- SECRET_DOOR
- LOCKED_EXIT
- UNLOCKED_EXIT

同时墙上方的 door overhang 也避免由静态 overlay 固化。

---

## 18.9 Secret Room 不提前暴露

如果 hidden room 还没被发现：

- 房间独立 tileset overlay 暂时不添加

避免：

> 外面明明看起来是城市墙，突然一块监狱墙区域，玩家直接知道这里有密室。

---

# 19. 当前状态

截至本文档写入：

- 最新 stable：0.4.2
- code：943
- generator：V8
- streaming：稳定
- world persistence：已建立
- objects：已持久化
- room system：已建立
- secret room：已建立
- room theme mixing：已建立
- monsters：尚未正式引入
- bosses：尚未正式引入

下一步重点已经从“无限地图能不能跑”变成：

> **地图是否足够像真正丰富的地牢世界。**

再补一批房间/环境内容后，进入怪物阶段。


# 20. 0.4.3 — 无限骨架、资源稀疏化、异常空间与异境记录

版本：

- 0.4.3
- versionCode 944
- stable：`assist-0.4.3-stable`
- dev：`assist-0.4.3-liminal`
- Generator V9
- release code SHA：`a513fc1a099d272c356c3127f874c3dd861e2799`

这一版来自 0.4.2 的一组实机反馈：

1. 某些世界/路线可能让玩家产生“地图是不是其实有限”的感觉，旧版甚至出现过所有可见分支都走到头。
2. 用户不要求所有岔路都有出口，但要求整个模式在结构上真的存在无限延展路径。
3. 0.4.2 用户没有实际找到秘密房。
4. 隐藏陷阱过多。
5. 房间/野外高价值物资过密，新世界跑不远就能收集大量物资。
6. 希望加入少量非常大、空旷、重复、类似阈限空间/后室气质的异常区域。
7. 希望异常区域以后能发现纸条，并在指南里单独收录。

## 20.1 Generator V9

`WORLD_GEN_VERSION`：

```
8 -> 9
```

因此 V9 地形结构只作用于新建 Infinite World。

旧 V8 世界继续按 V8 公式恢复，不会因为升级而重排已探索地图。

## 20.2 世界级 Infinite Backbone

V9 在正常 Chunk 生成全部完成后增加一层稀疏的永久连通骨架。

规则：

- `floorMod(cx, 6) == 0`：
  - Chunk 中心 x=11..12 刻出南北贯通双格走廊。
- `floorMod(cy, 6) == 0`：
  - Chunk 中心 y=11..12 刻出东西贯通双格走廊。
- (0,0) 同时属于两条主干。

意义：

- 普通支路可以自然死路。
- 主题房间可以封闭。
- 怪异局部结构仍然可以“不合理”。
- 但出生区域所在的主连通分量中一定存在可以无限向远处延展的路径。

这一层在普通房、装饰、障碍生成结束后最后刻出，因此后来房间 shell 不会再把它堵死。

## 20.3 秘密房更容易真实遇到

V8 只有随机 Secret Door，理论存在不代表玩家短期能碰见。

V9：

- 每个 3×3 Chunk macro 通过 hash 确定一个秘密房候选 Chunk；
- 候选 Chunk 的 room index 1 强制 Secret Door；
- 其余房间仍保留随机秘密门；
- 锁门比例进一步下降；
- Infinite World 下靠近 SECRET_DOOR 后被动搜索概率提高到 55%；
- 主动 Search 继续走原版发现机制；
- SECRET_TRAP 的普通发现率不跟着提高。

目标不是让隐藏房变成明房，而是让认真贴墙探索的玩家确实有机会遇到。

## 20.4 隐藏陷阱降密

V5/V8 环境 prop 曾经大量抽到 SECRET_TRAP。

V9：

- Ambient prop 总投放次数降低；
- SECRET_TRAP 从多个高权重 case 降到约 1/18 的 prop roll；
- Trap Workshop 内部 secret trap 概率从约 22% 降到约 10%；
- 异常大区域不撒普通环境陷阱。

## 20.5 普通物资大幅降密

V9 的核心平衡原则：

> 地图可以丰富，高价值战利品必须稀疏。

调整：

- 普通 deterministic chest：V9 约 12% Chunk（旧约 42%）。
- V5 container：V9 约 28% Chunk 仅 1 个，其他没有。
- V6 loose loot：V9 约 30% Chunk 仅 1 件。
- 野外 IronKey：约 8%。
- 野外 CrystalKey：约 5%。
- 野外真实 Plant：约 45% Chunk 1 株。
- 锁门旁仍保证 IronKey。
- Crystal Vault 仍保证 CrystalKey。

因此减少的是泛滥资源，不删除防 soft-lock 的钥匙保障。

## 20.6 主题房奖励重新平衡

V9：

- 植物房：约 7～9 株真实植物。
- Scroll Room：1～2 张卷轴。
- Potion Lab：1～2 瓶药。
- Pantry：2～3 份食物。
- Key Room：1 IronKey + 1 CrystalKey。
- Armory：1～2 件装备。
- Crystal Vault：1 个 Crystal Chest。
- Workshop：Bomb + StoneOfBlink。
- Gold Garden：4～7 堆较小金币。
- Treasury：2～4 件混合奖励。
- Secret Bonus：额外 2～3 件高价值物资。

秘密房仍值得找，但不会一次把玩家背包装满。

## 20.7 三种 5×5 Chunk 阈限异常区

V9 在远离出生点的区域增加 5×5 Chunk 宏区。

触发：

- 出生周边 `abs(cx)<=4 && abs(cy)<=4` 不产生异常宏区；
- 之后约 8% 的 5×5 macro 被选为 anomaly；
- 一个 anomaly macro 内 25 个 Chunk 共享同一种异常空间。

### Type 1 — Repeating Offices

- 大片 `EMPTY_SP`
- 规则墙网格
- 重复间隔
- 每组墙有少量确定性缺口
- 强调“走了很久但景物似乎没变化”的感觉

### Type 2 — Pool Halls

- 大面积 WATER
- 重复浅水池
- 中央十字干路
- 使用原版稳定 water backdrop，不另做新的 water renderer

### Type 3 — Endless Hall

- 大片 `EMPTY_SP`
- 稀疏规则 STATUE/柱体
- 大量空旷面积
- 奖励极少

三类异常区仍保留 shared-edge gateway，并叠加 Infinite Backbone。

## 20.8 异常区环境主题

V9 anomaly 不再运行普通主题房内容。

视觉上：

- Repeating Offices：偏 City material
- Pool Halls：偏 Sewers material
- Endless Hall：偏 Halls material

floor + wall 100% chunk overlay，使 5×5 macro 的视觉区别明显。

WATER 仍交给原版水层。

## 20.9 Infinite World Field Notes

新增：

- `Document.INFINITE_WORLD_NOTES`
- `InfiniteWorldNote`
- 指南页中的独立“无界异境记录”栏目

当前页面：

- Liminal_Offices
- Pool_Halls
- Endless_Hall

每个 5×5 anomaly macro 只有中心 Anchor Chunk 放一张对应 note。

Note：

- 使用 deterministic cell；
- 使用 objectStates 防刷新；
- 拾取后解锁 Journal 页面；
- 走远回来不重复出现。

文本是 Assist 自己编写的异常空间观察记录，不复制外部作品的具体 Lore。

## 20.10 Build

0.4.3 release workflow：

- run：36595730471
- head SHA：`a513fc1a099d272c356c3127f874c3dd861e2799`
- result：success
- 固定 applicationId 未变
- 固定 signing 流程未变



# 21. 0.4.4 — 多层无限支线网络

版本：

- 0.4.4
- versionCode 945
- dev：`assist-0.4.4-branch-network`
- stable：`assist-0.4.4-stable`
- Generator V10
- release code SHA：`02689bb5b1ed0d91424de21779d6f94cc96c8c83`

用户指出 V9 虽然用主干网保证了“世界整体无限”，但如果只有主干无限，普通旁路经常走到头，玩家会产生“离开高速公路就必须折返”的感觉。

V10 保留 V9 主干网，同时新增真正无限延伸的次级支线。

## 21.1 主干继续保留

V9 主干仍然存在：

- 每 6 Chunk 的纵向双格通路；
- 每 6 Chunk 的横向双格通路；
- 作为最低层级的全局连通保障。

## 21.2 次级无限横向支线

世界按 18 个 Chunk 高度分成水平 band。

每个 band 通过 seed/hash 确定一个非主干 Chunk 行。

这一整条 Chunk 行会形成一条真正无限的东西向路线。

每个 Chunk 中：

- 西边界入口高度由共享边界 hash 决定；
- 东边界入口高度由下一条共享边界 hash 决定；
- 中间使用 wander path 连接；
- 相邻 Chunk 对同一边界计算出完全一致的入口位置。

因此该支线不会在 Chunk seam 处断掉，而且不是笔直机械走廊，会在每个 Chunk 内上下游走。

## 21.3 次级无限纵向支线

同样地：

- 世界按 18 个 Chunk 宽度分 column band；
- 每个 band 选一个非主干 Chunk 列；
- 整列形成无限南北路线；
- 南北边界使用共享 edge hash；
- Chunk 内左右游走。

## 21.4 为什么这些次级支线与主干一定连通

东西向次级路线横穿整个 Chunk。

在 `cx % 6 == 0` 的主干 Chunk 内，它必然穿过主干纵向 x=11..12 通路。

南北向次级路线同理，会在 `cy % 6 == 0` 的 Chunk 穿过主干横向通路。

因此次级无限路线不是孤立平行线，而是会周期性重新接入主干网，也会与其他次级路线相交。

## 21.5 仍然保留死路

V10 没有把每一条普通局部岔路强行改成无限。

仍然允许：

- 小房间死路；
- 短走廊；
- 隐藏房；
- 普通支路尽头；
- 局部异常结构。

目标变成：

> 世界里不只有一套无限主干，而是同时存在多条可长期离开主干继续探索的无限支线。

这样既保留迷宫和死胡同，也避免“所有旁路最终都只能返回高速公路”的单调体验。


# 22. 0.5.0 — 玩家中心稀疏怪物生态

版本：

- 0.5.0
- versionCode 946
- dev：`assist-0.5.0-mobs`
- stable：`assist-0.5.0-stable`
- Generator 仍为 V10
- release code SHA：`c01cabebd7e9c453c11b3dc0f886a1579d9543fd`

这是 Infinite World 正式进入怪物阶段的第一版。

用户明确要求：

- 怪物数量不能太多；
- 仿照 Minecraft，离玩家太远的怪直接消失；
- 距离消失不产生掉落；
- 玩家附近怪物刷新不要频繁；
- 探索是主要玩法，战斗只占一部分；
- 玩家周围怪物有硬上限；
- 暂时不加 Boss；
- 普通小怪为主，精英怪极少。

## 22.1 不使用原版“整层预生成怪群”

Infinite World 的 `createMobs()` 仍不在世界构建时一次性塞怪。

改为独立 `InfiniteWorldMobEcology` Actor：

- Hero 进入世界后延迟约 20～32 回合才开始生态刷新；
- 每次生态检查后等待约 32～50 回合；
- 一次最多只刷 1 只；
- 数量越接近上限，成功刷怪概率越低；
- 异常宏区刷新概率再乘 0.45，使阈限空间维持更空旷的气氛。

## 22.2 数量上限

- 正常刷怪目标上限：5
- 绝对硬上限：6
- 精英怪同时最多：1

普通情况下预期玩家身边常见约 1～3 只，而不是一直顶满上限。

## 22.3 出现与消失范围

普通生态怪只会尝试生成在：

- 距玩家路径距离至少 14 格；
- 最远约 28 格；
- 不在 Hero 当前 FOV；
- 可通行；
- 非实体墙/坑/秘密 terrain；
- 没有角色、箱子、Trap 或 Plant 占位的位置。

距离玩家超过约 40 格后：

- 直接从 Actor 和 Level mob 集合移除；
- Sprite 直接销毁；
- 不走 `die()`；
- 不走 `Mob.destroy()`；
- 不掉 Loot；
- 不给 EXP；
- 不计入击杀统计。

为了实现这一点，在 `Mob` 中加入专门的 `despawnFromInfiniteWorld()`，不能用正常死亡入口代替。

## 22.4 Streaming 中的怪物处理

Infinite World 每次 Window shift 会移动 3 Chunk，即 72 cell。

若不处理，Mob.pos 仍是旧 local cell，会产生严重坐标错位。

0.5.0 在 shift 后：

- 按 shiftedCellsX/Y 把仍在有效范围内的 Mob local pos 重算；
- 清空旧 path/target/enemy；
- 重设为 WANDERING；
- Sprite 重新 place 到新 local cell；
- 超出新 window、距离太远或落入无效 terrain 的怪直接无奖励 despawn。

新增 `Mob.rebaseForInfiniteWorld()` 专门清理旧 local navigation state。

## 22.5 怪物池

近距离/初期：

- Rat
- Snake
- Gnoll
- Crab
- Slime

玩家等级提高或探索到更远 world chunk 后，逐步混入：

- Skeleton
- DM100
- Bat
- Brute
- Spinner

很远或角色等级较高时，少量混入：

- Warlock
- Monk

暂时没有：

- Boss
- Miniboss
- Thief（避免偷走玩家物品后距离 despawn 导致物品永久消失）
- 大量召唤型怪物

## 22.6 精英怪

精英使用原版 `ChampionEnemy` buff，而不是新建第二套怪物类。

- 单只怪成为精英的基础概率约 6%；
- 同时最多 1 只；
- 新角色出生附近不会立刻刷精英；
- 当前只使用：
  - Projecting
  - AntiMagic
  - Blessed
  - Growing

刻意不使用：

- Blazing：Buff detach 时会制造火焰，不符合“距离 despawn 无副作用”；
- Giant：改变 LARGE/placement 需求，第一版暂不引入。

## 22.7 存档策略

0.5.0 的生态设计不是“每个世界 Chunk 永久保存一只固定怪”。

它更接近 Minecraft 式临时生态：

- 当前仍在玩家附近的 Mob 会继续由原版 Level MOBS 序列化，正常 save/load；
- 一旦玩家走远超过 despawn radius，该怪物就永久离开当前生态，不保存到远处 Chunk；
- 之后附近空缺由生态控制器按低频率重新生成新怪。

这符合用户明确要求的“走远后直接消失”，也避免为无限世界维护无限增长的 mob world-state 数据。


## 0.5.0 Build

0.5.0 release workflow：

- run：36601486694
- artifact：11049646020
- artifact name：ShatteredPD-Assist-Base
- build head：`c01cabebd7e9c453c11b3dc0f886a1579d9543fd`
- result：success
- artifact ZIP SHA-256：`441b1baca7fc84867ce66f58612e9f6c42a57d9bd02edc98d3544175495a3f90`
- APK SHA-256：`6f350229df052ccc38ff66d8c1ac2d2b3eaaf10cd40f4b24d528fc8c679dc683`


# 23. 0.5.1 — 稀有流浪商人据点

版本：
- 0.5.1
- versionCode 947
- dev：`assist-0.5.1-merchants`
- stable：`assist-0.5.1-stable`
- Generator V11
- release code SHA：`e14bac5c266d6b0cad3189ace9dc7492967ac74b`

## 商人生成
Infinite World 新增固定世界坐标的流浪商人据点，不把商人当作普通随机怪生成。

- 商人据点使用 7-Chunk 间距的 seed 驱动格点。
- 7×7 活动窗口的坐标跨度只有 6 Chunk，因此同时最多一个商人。
- 出生附近 4 Chunk 不生成商店。
- Liminal anomaly 不生成商店。
- Primary/Secondary infinite road 所在 Chunk 不生成商店，避免 V10 最后刻路时切穿商店墙体。
- 商店占用该 Chunk 的第一个主题房，入口固定为普通门。

## 商店库存
每家店最多 6 个出售堆：
1. 治疗药剂
2. 小份口粮
3. 随机安全药剂
4. 随机安全卷轴（不包含探地图卷轴）
5. 随世界距离提高档位的随机装备
6. 炸弹/火把/闪现符石/稿子/祛邪卷轴之一

库存使用 `Heap.Type.FOR_SALE` 和原版 `WndTradeItem`，购买/出售继续走原版交易逻辑。

## 持久化
- 商品位置和内容由 seed/世界坐标确定。
- 已购买商品写入 InfiniteWorldState.objectStates。
- 走远、Streaming、Save/Load 后不会补货。
- 商人被攻击或偷窃失败逃跑时，记录“该据点永久关闭”，回来不会重新生成商人和库存。
- 商人正常离开活动窗口只是 unload，不视为逃跑。

## Streaming
新增 `InfiniteWorldShopkeeper` 保存据点 Chunk 坐标。
商人如果在 Window shift 后仍位于新活动窗口，会同步 rebase local pos；离开窗口则无死亡逻辑地卸载。重新进入对应 Chunk 时按固定世界据点恢复。

## 怪物关系
- 商人是 NEUTRAL NPC，不参与 0.5.0 的 40 格敌人 despawn。
- 普通生态刷怪不会在商人 8 格范围内直接生成。
- 暂不做 Boss 商店、特殊货币或多商人同时存在。


## 0.5.1 Build

- GitHub Actions run：36652519842
- artifact ID：11071242855
- artifact name：ShatteredPD-Assist-Base
- build head：`e14bac5c266d6b0cad3189ace9dc7492967ac74b`
- result：success
- ZIP SHA-256：`ab8a78facc9447d287b5b3627173d6b459dc7e05f1deb13d19ac7a816bb2b003`
- APK SHA-256：`4baab2a0ce442d9e52cddf0fa4f0750cdc47e926fa8cf5c36cdd5cfa2d1fe177`


# 23. 0.5.1 — 流浪商人与商店房

版本：

- 0.5.1
- versionCode 947
- dev：`assist-0.5.1-merchants`
- stable：`assist-0.5.1-stable`
- Generator V11
- release code SHA：`d9e0d06829d61674d4bb96a52f649cfea45fe90c`

本版本加入 Infinite World 第一版商人系统。

核心设计：

- 复用原版 `Shopkeeper + Heap.Type.FOR_SALE + WndTradeItem` 交易机制。
- 商人不随机站在路边，而是生成在独立的小型商店房里。
- 商店按稀疏 9-Chunk 网格确定，7×7 active window 内最多一个商人，避免多个 Shopkeeper 同时存在导致出售/回购选择错误。
- 出生附近排除商店。
- 商店不生成在 V10 Primary/Secondary 无限路线本体上，避免道路最终 carve 穿过店墙。
- 商店附近 8 格内不生成普通生态怪。
- 商人不属于 ENEMY，因此不会进入 0.5.0 的 40 格敌怪 despawn 逻辑。
- Streaming 时商人会跟随 local window 正确 rebase；离开 active window 后不作为远程 NPC 常驻内存，重新进入其固定 world chunk 时恢复。
- 商人被打跑会写入 InfiniteWorldState，回到该商店不会重新刷新。
- 默认库存 6 件：
  - 治疗药剂
  - 小份口粮
  - 随机药剂
  - 非 Mapping 安全卷轴
  - 一件随世界距离提升 tier 的装备/法杖/戒指
  - Bomb/Torch/Blink Stone/Pickaxe/Remove Curse 之一
- FOR_SALE 库存按 objectStates 保存；买走后切区块、退出重进都不会补货。
- 商店仍允许使用原版卖物、回购和金币交易机制。

由于新增固定商店房属于世界生成内容，Generator 从 V10 升到 V11。

因此：
- 旧 V10 世界不会自动长出商店。
- 要测试 0.5.1 商人，需要在“无界地牢”里重新开始一次 V11 世界。

Build：

- workflow run：36652803838
- artifact：11071337926
- artifact digest：`sha256:3636d91475c1fd5cc27e4d37fa277d6dad8a4fd9b246cf5a13358e1a53a4f202`
- APK SHA-256：`8508db9fce64d1368315c80946414bad1646df05d555df0af05e116359cd6329`
- result：success


# 23. 0.5.1 — 稀有商人据点

版本：

- 0.5.1
- versionCode 947
- dev：`assist-0.5.1-merchants`
- stable：`assist-0.5.1-stable`
- Generator V11
- release code SHA：`d9e0d06829d61674d4bb96a52f649cfea45fe90c`

本版首次向 Infinite World 加入商人。

## 23.1 商人不是普通随机 NPC

商人被设计为固定世界坐标的稀有据点，而不是随机站在路边。

- 使用 9-Chunk 稀疏 lattice 选定候选商店 Chunk；
- 出生周边不会直接生成商店；
- anomaly 宏区不生成商店；
- primary spine / secondary infinite route 上不放商店，避免 V10 路网最后 carve 时切穿店铺；
- 由于 9 Chunk 间距大于 7×7 active window，同一窗口最多只会有一个商人。

## 23.2 商店房

Generator V11 新增 merchant room theme=10：

- 房门固定普通门；
- 房内为安全的小型商店空间；
- 普通 themed-room loot 不再叠加到商店房；
- 普通怪生态不会在商人 8 格范围内自然刷怪。

## 23.3 商人

新增 `InfiniteWorldShopkeeper extends Shopkeeper`。

直接复用原版：

- Shopkeeper 交互；
- `Heap.Type.FOR_SALE`；
- `WndTradeItem`；
- 原版买卖/价格逻辑。

商人不会被普通敌人的 40 格 ecology despawn 逻辑清掉。

Streaming 时商人和普通近距离 Mob 一样重算 local position。

## 23.4 库存

每个商店固定 6 个货位：

- PotionOfHealing
- SmallRation
- 随机药剂
- 安全卷轴
- 1 件按世界距离决定 tier 的装备
- 1 件工具/实用品

库存使用 deterministic world position + objectStates。

买掉商品后：

- 对应货位标记 consumed；
- streaming 后不刷新；
- save/load 后不刷新。

## 23.5 商人离开

如果玩家攻击商人导致原版 `flee()`：

- 商店 Chunk 的 merchant-gone 状态写入 objectStates；
- 该商人不会因为 streaming 重建而重新出现。

## 23.6 Build

- workflow run：36652803838
- artifact：11071337926
- result：success
- artifact ZIP SHA-256：`3636d91475c1fd5cc27e4d37fa277d6dad8a4fd9b246cf5a13358e1a53a4f202`
- APK SHA-256：`286294c86515604e706223005bbd0d0d62a5f954cb5a34bb3594099e4f452764`

# 0.5.2 / Generator V12 — 可发现性调优与唯一神器箱

版本：
- versionName: 0.5.2
- versionCode: 948
- dev: assist-0.5.2-discovery
- stable: assist-0.5.2-stable
- WORLD_GEN_VERSION: 12

本版来自 0.5.1 实机反馈：商人过难遇到、Liminal 异境过稀且可能经过整个异境仍看不到便笺，以及水晶箱缺少真正稀有的长期探索奖励。

## 商人密度
- V12 商人格点间距由 7 Chunk 缩短到 5 Chunk。
- 出生排除区由 4 Chunk 缩短到 3 Chunk。
- 继续禁止商店生成在 Liminal anomaly、Primary spine、Secondary infinite route 上。
- V11 老世界继续使用旧 7-Chunk 规则，不改变旧世界确定性生成。
- 因 5-Chunk 间距小于 7×7 active window 的坐标跨度，V12 不再维持“窗口内绝对最多 1 个商人”的旧假设；理论上可同时加载最多 4 个格点，实际还会受道路/异境排除影响。
- 库存、购买持久化、逃跑永久关闭、怪物 8 格刷新安全区均保持原逻辑。

## Liminal 异境与便笺
- V12 的 5×5 Chunk anomaly macro 命中率由约 8% 提高到约 15%。
- V9～V11 老世界仍保留 8%，避免旧世界地形改变。
- 原来的 deterministic anchor note 继续保留。
- V12 新增“首次未收录异境近身便笺保证”：Hero 进入某种尚未收录的异常空间时，会在附近可达位置生成并直接标记可见的 InfiniteWorldNote。
- 对应 Document 页面一旦收录，该类型不再依靠近身保证重复刷纸条。
- 目的不是增加普通 loot，而是保证玩家真的能完成“遇到异境 → 看到纸条 → 收录讲解”的探索闭环。

## 全局唯一神器水晶箱
- V12 新增持久化 heroActionValue，只累计 Hero 的正向 spend/spendConstant 行动时间。
- 阈值为 400 action-value。
- 达到阈值且本局尚未生成保证箱时，在 Hero 附近 2～5 格寻找可达位置，必要时放宽到 1～7 格。
- 生成 Heap.Type.CRYSTAL_CHEST，并强制 seen，同时写入 visited/mapped，保证玩家能发现。
- 生成时同时给予 1 把 CrystalKey，避免唯一奖励因钥匙供应造成 soft-lock。
- 箱内固定为 1 件 Artifact；优先从 Generator 当前剩余神器牌组中确定性选择，并调用 removeArtifact 消耗唯一性。
- 全局保存：artifactChestWorldX/Y、artifactChestArtifactIndex、artifactChestState。
- 生命周期：0 未生成 / 1 未开水晶箱 / 2 已开但神器仍在 / 3 神器已取走。
- Streaming 和 Save/Load 均按绝对世界坐标恢复，不允许生成第二个保证神器箱。
- 若箱已打开但神器未取，回来应恢复普通 Heap；神器取走后永久结束。

## 架构保护
- 未改变 Hero.onMotionComplete() 后才允许 Streaming 的硬约束。
- 未改变 Tilemap VBO / Water / Fog 修复。
- 未改变 Mob.despawnFromInfiniteWorld() 的无奖励距离清除规则。

# 0.5.3 / Generator V13 — 商人真正可发现 + 六类异境

版本：
- versionName: 0.5.3
- versionCode: 949
- dev: assist-0.5.3-merchants-liminal
- stable: assist-0.5.3-stable
- WORLD_GEN_VERSION: 13

用户对 0.5.2 的实机反馈确认：
- 唯一神器箱规则正常，整局只出现一个；
- 商人格点从 7 缩到 5 后，玩家仍然长期“见不到商人”；
- Liminal / 类后室异常空间仍显得太少，希望增加出现频率与层级种类。

## 商人可发现性
V12 的问题不只是密度。Merchant Outpost 是二维 lattice 上的固定小房，即使据点已经进入 7×7 active window，只要玩家沿当前路线持续前进、没有偏向该房间所在 Chunk，就可能完全不进入 FOV，因此体感仍然像“没有商人”。

V13 同时做三件事：
- lattice 间距 5 -> 4 Chunk；
- 出生排除范围约 3 -> 2 Chunk；
- 只要商人据点进入 active window，其房间周边直接写入 mapped，出售货堆设为 seen；
- 未提示过的商店进入 Hero 2 Chunk 范围时，输出一次本地化“附近有商人据点”提示；
- 每个据点的提示状态使用 objectStates 记录，不反复刷屏。

商店仍然：
- 不生成在 anomaly；
- 不生成在 Primary / Secondary 无限道路 Chunk；
- 不改变 6 槽库存和交易逻辑；
- 已购商品不刷新；
- flee 后永久关闭；
- 8 格普通怪自然生成安全区继续有效。

## Liminal / 类后室区域
- V13 5×5 macro anomaly 概率：约 28%。
- V12 保持 15%，V9-V11 保持 8%，旧世界生成结果不被改写。
- anomaly 类型从 3 种扩到 6 种。

保留：
1. Repeating Offices / 重复回廊
2. Pool Halls / 静水廊厅
3. Endless Hall / 无尽大厅

新增：
4. Yellow Maze / 黄墙迷廊
   - 高频重复隔墙；
   - 开口位置轻微错位；
   - EMPTY_SP 主导，强调“熟悉但对不上记忆”的重复空间。
5. Service Tunnels / 检修管廊
   - 多条规则窄通道形成维护网格；
   - EMPTY_DECO 与少量 EMBERS；
   - 强调隐藏在巨大建筑背后的设备层。
6. Dark Storage / 暗仓网格
   - 大量重复实体储藏块；
   - 狭长过道；
   - 少量 STATUE，强调压迫、重复与方向感丧失。

所有类型仍会在末尾重新连接 shared-edge gateways，并继续叠加 V10 Primary/Secondary infinite network，不能把世界封死。

## Field Notes
新增三篇：
- Yellow_Maze
- Service_Tunnels
- Dark_Storage

六种 anomaly 都继续使用：
- deterministic anchor note；
- 未收录页面进入异境后的近身可见便笺保证；
- Document 持久化；
- 收录后不再重复触发近身保证。

## 未改动
- 400 action-value 全局唯一神器水晶箱保持 V12 逻辑；
- Streaming 仍只能 Hero.onMotionComplete() 后发生；
- SHIFT 24/144/3 不改；
- Water/VBO/Fog 修复不改；
- Mob.despawnFromInfiniteWorld() 无奖励距离清除不改。

# 0.5.4 — 指定物品 / 指定数量作弊

版本：
- versionName: 0.5.4
- versionCode: 950
- dev: assist-0.5.4-item-grant
- stable: assist-0.5.4-stable
- WORLD_GEN_VERSION: 13（不变）

新增 Assist 菜单按钮“获取指定物品”。

流程：
1. 选择“装备 / 神器 / 饰物”或“消耗品 / 材料 / 钥匙”。
2. 分类直接读取原版 Catalog。
3. 选择具体物品；长列表按每页 8 个分页。
4. 输入数量 1～999。
5. 可堆叠物品直接生成一个对应数量的 stack；不可堆叠物品逐件生成。
6. 正常背包放不下时掉在 Hero 当前格，不静默丢失。
7. Key 类型不进入背包，而是以 Dungeon.depth 为当前层写入 Notes/KeyRecord，并刷新钥匙 UI。
8. Gold / EnergyCrystal 直接增加 Dungeon.gold / Dungeon.energy，不生成假的背包货币对象。
9. Dewdrop 直接加入 Waterskin，最多加到水袋容量；没有水袋时给出提示。
10. 生成物品自动 identify，方便作弊使用。

物品来源使用 Catalog 而不是另写一套硬编码清单，因此会覆盖原版图鉴中的武器、护甲、投掷武器、法杖、戒指、神器、饰物、袋子、药水、卷轴、种子、符石、食物、炸弹、飞镖、炼金酿剂/药剂、法术、钥匙以及大部分任务/杂项物品。

本版只改 Assist UI/物品生成，不改 Infinite World 世界生成公式，所以 Generator V13 保持不变，0.5.3 V13 存档可直接继续。

# 0.5.5 / Generator V14 — 商店强制可达 + 动态墙贴图同步

版本：
- versionName: 0.5.5
- versionCode: 951
- dev: assist-0.5.5-merchant-connectivity
- stable: assist-0.5.5-stable
- WORLD_GEN_VERSION: 14

本版来自真实设备反馈，不是单纯调商人概率。

## 修复：商店房可能存在但无法正常到达
V14 在每个 merchant chunk 的所有主题房和无限道路完成后执行最终 connectivity validation：
- 商店门外必须能沿 PASSABLE terrain 到至少一个 shared-edge gateway；
- 已连通则完全不改；
- 不连通才自动补一条 deterministic corridor；
- 补路优先绕开其他主题房矩形；
- 不额外打穿非法 chunk 边界；
- 极端布局使用 fallback route，保证商店不再成为孤岛。

V11-V13 旧存档也有 runtime repair：
- active merchant chunk 加载时即时检查；
- 仅修断路；
- 修复结果通过 terrainOverrides 持久化；
- 不需要为了这一个 Bug 强制抛弃当前 V13 存档。

## 修复：可走地板仍显示旧墙
InfiniteWorldAccentTilemap 不再只有 Streaming rebuild 才更新。
现在 GameScene.updateMap(cell) 会局部刷新受影响的 accent floor/wall mesh。
因此：
- Bomb 炸掉主题墙后视觉立即同步；
- 不会再出现逻辑能走、画面还是墙、角色被旧墙覆盖的情况。

## Secret Room
SECRET_DOOR 从墙变为普通门时会立即重建当前 Infinite World accent definitions，并重新挂载 custom overlays。
Secret Room 的独立材质不再必须等下次窗口 Streaming 才出现。

## Merchant map reveal
商店提示除了标记房间，还会标记商店到共享 Chunk 出口的真实可走路径。
不会把 SECRET_DOOR 当成普通通路提前暴露。

## 兼容
- 新世界使用 Generator V14，基础地形本身包含 merchant access guarantee。
- 旧 V11-V13 世界不改 generatorVersion，但会运行 merchant access repair。
- 唯一神器箱、六类 Liminal、怪物生态、Streaming 触发时机、VBO/Water 修复均保持。

# 0.5.6 — 无界旁观测试模式

版本：
- versionName: 0.5.6
- versionCode: 952
- dev: assist-0.5.6-spectator-test
- stable: assist-0.5.6-stable
- WORLD_GEN_VERSION: 14（不变）

Assist 菜单在 Infinite World 中新增“无界旁观测试模式”，用于高效率地形、Streaming 与可达性 QA。

## 移动
- 只在 Infinite World 生效。
- Hero 可穿过 WALL、WALL_DECO、门壳、坑等活动窗口内部地形。
- 不修改 Level.map / passable 等真实地形数据，只为 Hero 的 QA 寻路构造临时 passable map。
- 不能穿越 168×168 活动窗口最外层 streaming frame。
- 仍避开实际角色占据的格子，避免与 Mob/NPC 叠格。
- 旁观模式最低移动倍率 ×4；普通 Assist 移速若高于 ×4，则使用更高倍率。
- 忽略 Root / Paralysis / Vertigo 对测试移动的阻断。
- 经过坑、陷阱、植物、门、地表 Blob 时不触发物理踩踏效果。
- Hero 在旁观模式下免疫伤害。

## Streaming 与探索
- Streaming 触发点完全不变，仍只在 Hero.onMotionComplete() 后执行。
- 每步仍通过 Hero.move -> InfiniteWorldLevel.recordHeroMove 记录绝对世界坐标。
- Dungeon.observe/FOV/visited/mapped/InfiniteWorldState 探索同步继续正常工作。
- Hero 站在墙格内部时，只将当前所在格临时视为不阻挡视线；周围真实墙仍正常挡视野，因此不是全图 X-Ray。
- 经过的区域会像普通探索一样加载、驱散迷雾并永久记录探索状态。

## 怪物冻结
- Actor.process 在调度 Mob 时直接冻结其行动，只推进调度时间，不调用 Mob.act。
- 因此已有怪物不会检测、追击、移动、攻击或因看见 Hero 改 AI 状态。
- InfiniteWorldMobEcology 在模式开启时暂停自然刷新。
- 可见怪物仍保留在地图上供测试观察，但不会触发 Hero 的发现新敌人中断移动或附近有敌人不能自动捡物/交易等限制。
- 商人等 NPC 的直接 Hero 交互仍可使用。

## 正常交互
- 宝箱仍能正常打开；
- 地面物品仍能正常拾取；
- FOR_SALE 仍能购买；
- NPC/商人交互仍走原有逻辑；
- 钥匙/门等手动交互仍可执行。
- 点击墙本身时不会再被 Infinite World Pickaxe 的挖掘动作抢占；点击墙后方即可穿墙移动。

## 显示与退出保护
- 旁观时 Hero Sprite 被临时放到 raised terrain / wall 之后的 effects 层，穿墙途中不会被墙贴图完全遮住。
- 关闭模式时，如果 Hero 正位于墙/坑等非法站立格，会自动落到当前 active window 内最近的正常可站立格。
- 正常格关闭时不移动 Hero。

本版没有改变世界生成公式，所以 WORLD_GEN_VERSION 保持 V14，0.5.5 V14 存档可直接继续。

# 0.5.7 / Generator V15 — 14 类 Backrooms 环境 + 自动档案 + 成长型商人

版本：
- versionName: 0.5.7
- versionCode: 953
- dev: assist-0.5.7-backrooms-merchants
- stable: assist-0.5.7-stable
- WORLD_GEN_VERSION: 15

## V15 Backrooms 环境成为正常世界的一部分
V15 暂不实现特殊 noclip 入口、独立层级切换或专用转层道具。14 类 Backrooms-inspired district 直接作为 Infinite World 的普通地形宏区，通过正常探索即可遇到。
- macro 从旧 5×5 调整为 V15 4×4 Chunk。
- V15 特殊环境宏区概率约 42%。
- 出生缓冲缩到 2 Chunk；开局附近仍保留普通地牢学习区。
- 所有 V15 特殊区最终仍刻四边 shared gateway，并继续叠加 V10 infinite network。
- 不复制网络图片；视觉使用原版 sewers/prison/caves/city/halls 五套材质，通过不同结构与材质映射重组。

首批 14 类：
1. Level 0 — Threshold：黄墙式重复迷宫。
2. Level 1 — Habitable Zone：混凝土/仓储/服务厅。
3. Level 2 — Abandoned Utility Halls：狭窄检修/管线走廊。
4. Level 3 — Electrical Station：密集设备与维护网格。
5. Level 4 — Abandoned Office：宽办公区与重复隔断。
6. Level 5 — Terror Hotel：旅馆主廊与重复客房。
7. Level 6 — Lights Out：狭窄、低可读性的暗廊结构。
8. Level 7 — Thalassophobia：大面积水域与少量干燥岛。
9. Level 8 — Cave Systems：不规则岩洞与曲折连接。
10. Level 9 — The Suburbs：街道与小型住宅块。
11. Level 10 — Bumper Crop：高草田野与农路。
12. Level 11 — The City That Never Sleeps：城市街网与建筑块。
13. Level 37 — Sublimity / Poolrooms：池厅、水面与宽通道。
14. Level 94 — Motion：草地、小镇、房屋与道路。

## 不再生成层级纸条
- V15 `generateV9AnomalyNotes()` / snapshot 对新世界停用。
- Hero 第一次进入某种 V15 环境时直接 `Document.INFINITE_WORLD_NOTES.findPage()`。
- 首次解锁输出本地化提示并闪烁 Journal。
- 老 V9-V14 save 仍保留旧六类 Field Note 行为和旧 deterministic 生成公式。

## V15 商人出现频率成长
- 基础 merchant lattice 从 V14 4 Chunk 提高到 3 Chunk。
- 出生排除从 2 Chunk 缩到 1 Chunk。
- 进度 tier 由 `max(Hero 等级 tier, heroActionValue tier)` 决定。
- Level thresholds: 4 / 7 / 11 / 15。
- action thresholds: 300 / 800 / 1600 / 2800。
- Tier 1-4 会为之后首次生成的 Chunk 逐步开放额外稀疏商人格点。
- 每个 Chunk 第一次生成时把 merchant / no-merchant 结果锁进 objectStates，旧区域不会因为升级突然变成商店或失去商店。

## V15 商人库存成长
- 商人第一次进入 Hero 2 Chunk 范围时锁定自身库存 tier。
- Tier 0 只允许基础武器/护甲和基础生存/工具品，装备槽不出 Wand/Ring。
- Tier 上升后逐渐允许 Wand、Ring、Transmutation 等更稀有货物。
- Tier 3/4 才有 25% / 45% 概率出现 +1 可升级装备。
- 商人不出售 Artifact。

## 必备扩展背包保证
当前原版四种扩展包：VelvetPouch、ScrollHolder、PotionBandolier、MagicalHolster。
- Hero 未全部拥有前，每一个新遇到的 V15 商人 slot 0 必须出售一个当前缺失的 Bag。
- 如果商店预加载后 Hero 从别处获得了原计划出售的 Bag，真正接近该商人时会重新挑选仍缺失的 Bag。
- 已购买/消耗的 slot 不恢复，不会用同一个商人无限刷 Bag。
- Infinite World Bag 售价改为 item value ×2（最低 20），而普通商品继续沿用原版 ×5 贪婪定价。

## 保持不变
- V14 Merchant connectivity guarantee / legacy repair。
- 0.5.6 无界旁观测试模式。
- Streaming 只能 Hero.onMotionComplete 后发生。
- VBO/Water/Fog 修复。
- 唯一神器水晶箱。
- 怪物生态和无奖励超距 despawn。

# 0.5.8 — Spectator Flight QA + CC0 Environment Props

- versionName: 0.5.8
- versionCode: 954
- WORLD_GEN_VERSION remains 15.
- dev: assist-0.5.8-spectator-props
- stable: assist-0.5.8-stable

## Spectator QA movement
- Infinite World spectator minimum movement multiplier increased from x4 to x8.
- Hero logical speed and CharSprite PosTweener speed use the same minimum x8 multiplier.
- Spectator movement suppresses the run animation: Hero keeps the idle pose while gliding cell-to-cell.
- Native CharSprite.State.LEVITATING supplies the floating effect.
- Disabling spectator mode removes LEVITATING, restores idle state and still runs settleHeroAfterSpectator().
- Streaming timing is unchanged: shifts still happen only after Hero.onMotionComplete().

## Spectator QA vision
- While spectator mode is active, Hero FOV uses ShadowCaster.MAX_DISTANCE = 20.
- Normal Infinite World and normal dungeon vision remain unchanged.
- Existing wall-source FOV exception for a Hero currently inside a wall remains intact.

## V15 CC0 environment decoration
New sparse non-gameplay props:
- bush
- mushrooms
- weathered sign
- discarded barrel
- empty crate

Art sources:
- Kenney Tiny Town 1.1, CC0 1.0.
- Kenney Tiny Dungeon 1.0, CC0 1.0.
- Both are native 16x16 pixel packs, matching DungeonTilemap.SIZE.
- Build downloads are pinned to upstream commit e22e06e317be6c933b779ad7b055b6a6aeafa5e8 and verified against exact Git blob IDs before Gradle runs.

Integration:
- Props are CustomTilemap overlays and never alter map[], passability, doors or water.
- Placement occurs after heaps/traps/plants/themed-room content so occupied cells are skipped.
- V15 Backrooms districts receive two props per active chunk when valid cells exist.
- Ordinary V15 terrain receives one prop in roughly 62% of chunks.
- Common center crossing lanes are kept clear.
- WndInfoCell/magnifier uses each prop's actual sprite plus localized name/description.
- Props are saved sparsely through Bundle state; empty atlas cells are not serialized.
- External asset provenance is recorded in docs/assist/THIRD_PARTY_ASSETS.md.

## Not changed
- Monster strength/stats/ecology are untouched in 0.5.8.
- V15 Backrooms generation and merchant progression remain unchanged.
- Water/VBO/Fog/Streaming invariants remain unchanged.

# 0.5.9 / Generator V16 — Rich Environment Scenery

Version:
- versionName: 0.5.9
- versionCode: 955
- dev: assist-0.5.9-environment-rich
- stable: assist-0.5.9-stable
- WORLD_GEN_VERSION: 16

## Why V16
0.5.8 proved the CC0 overlay pipeline but only exposed five sparse sprites and all were pass-through.
0.5.9 turns scenery into a real environment system. Bulky scenery now participates in ordinary movement/pathfinding, so new worlds use Generator V16.

## 30 inspectable scenery kinds
Town/natural/road props include round/green/amber trees, fern, mushrooms, fence pieces, wooden posts, signs, warning markers, rocks, logs, barrels, sealed crates, tubs and troughs.
Dungeon/interior props include old caskets, torches, rubble, bookshelves, stone crosses, gravestones, tables, stools, stone basins, cabinets, iron railings, cold firepits and weapon displays.
All remain non-interactive. Magnifier inspection still shows each prop's own sprite, localized name and description.

## Density / authored grouping
- ordinary chunk: deterministic 2-4 props
- Level 0: 4-6
- Levels 1/2/3: 6-9
- Levels 4/5: 6-9
- Level 6: 4-6
- Level 7: 3-5
- Level 8: 7-10
- Level 9: 8-11
- Level 10: 9-13
- Level 11: 8-11
- Level 37: 4-6
- Level 94: 9-13
Compatible families form small deterministic clusters instead of appearing only as isolated single sprites.

## Regional prop pools
- industrial/service: barrels, crates, cabinets, rails, warnings, torches, rubble
- office/hotel: tables, stools, shelves, cabinets, signs
- Lights Out: rubble, caskets, gravestones, crosses
- ocean: logs, barrels, rocks, signs
- caves: mushrooms, rocks, rubble, firepits
- suburbs: trees, fences, signs, barrels, logs, crates
- fields: trees, ferns, mushrooms, fences, logs
- city: signs, warnings, barrels, crates, iron rails, tables
- Poolrooms: stone basins, tubs, troughs, signs, rocks
- Motion: green/amber trees, fences, ferns, mushrooms, logs, signs

## Physical scenery
Generator V16 uses Terrain.CUSTOM_DECO for bulky physical scenery.
Bulky props block normal Hero movement and Mob pathing, remain non-interactive, and remain phaseable in Infinite spectator mode.
Pass-through ground clutter is limited to mushrooms, ferns, torches and rubble.

## Connectivity protection
Physical placement rejects origin-plaza blockers, merchant-chunk blockers, center crossing lanes, room-access cells, chest cells/vicinity, doors/transitions/wells/alchemy/pedestals, and local chokepoints.
The chokepoint test removes the candidate virtually and verifies its immediately adjacent walkable cells can still reconnect within a 7x7 neighborhood while respecting already selected hard props.

## Compatibility
- Existing V15 saves get the expanded catalog, higher visual density and regional grouping, but no new solid blockers.
- New V16 worlds gain physical blockers.
- 0.5.8 spectator flight still phases through all scenery.
- Monster strength/stats remain untouched in 0.5.9.


# 0.5.10 / Generator V17 — Solid Scenery + Artifact Chest Pacing

Version:
- versionName: 0.5.10
- versionCode: 956
- dev: assist-0.5.10-solid-scenery
- target stable: assist-0.5.10-stable
- WORLD_GEN_VERSION: 17

## Guaranteed artifact chest
- Trigger raised from 400 to 1000 positive Infinite World Hero action-value.
- Spawn is constrained to the Hero-centered 3x3 cell neighbourhood (one of the eight adjacent cells).
- If no adjacent legal cell exists, the spawn is deferred and retried on a later positive Hero action; it is never pushed outside the requested 3x3 area.
- The chest remains a single persistent CRYSTAL_CHEST containing one artifact.
- One CrystalKey is granted when the chest is created.

## All scenery is physical in V17
- All 30 InfiniteWorldDecorationLayer kinds are solid in newly generated V17 worlds.
- Every visible prop uses Terrain.CUSTOM_DECO for ordinary collision/pathfinding.
- Hero and ordinary mobs must route around every prop.
- Props remain non-interactive and spectator mode still phases through them.
- Existing V16 saves preserve the old V16 pass-through exceptions (mushrooms, fern, decorative torch and rubble).

## Safety
- Center crossing lanes, room access, chest vicinity and critical terrain remain protected.
- Local connectivity/chokepoint checks apply to every V17 prop and account for already planned blockers in the same chunk.
- V17 removes the old pass-through fallback for origin/merchant areas; any decoration that is actually placed must satisfy real blocker safety.

## Not changed
- Monster strength/stats/ecology are unchanged.
- Streaming timing remains Hero.onMotionComplete() -> InfiniteWorldLevel.afterHeroMotionComplete().
- applicationId and stable signing key remain unchanged.


# 0.5.11 — Infinite World Long-Run Progression

Version:
- versionName: 0.5.11
- versionCode: 957
- dev: assist-0.5.11-progression
- target stable: assist-0.5.11-stable
- WORLD_GEN_VERSION: 17 (unchanged)

## Start / narrative
- Fresh Infinite World gold: 300.
- Replaced the normal sewer/dungeon story card with a dedicated Infinite World introduction.

## Dynamic enemies
- Every newly spawned Infinite World mob snapshots Hero level, strongest owned weapon/armor/wand, two strongest rings, artifact ownership and artifact visible level.
- Dynamic threat level 1-60 affects enemy family selection.
- HP scales strongly against offensive upgrades; damage scales strongly against defensive upgrades; accuracy/defense scale more moderately.
- Spawned mobs keep their snapshot until despawn/death.
- Dynamic loot probability scales up to about 2.25x base and EXP gains receive a threat-level floor.
- No bosses were added.

## Level-30 breakthrough
- Hero cannot advance past level 30 until the breakthrough succeeds.
- Level 30 automatically grants a BreakthroughToken.
- Token enters a dedicated 49x49 branch-99 arena.
- Ten waves, ten enemies per wave.
- Wave pools and stat multipliers escalate from low mobs to mixed late-game threats.
- 30 action-value gap between cleared waves with countdown messages.
- Arena contains scattered temporary recovery/utility supplies.
- Trial defeat is non-lethal and never consumes Ankhs or submits a normal game-over.
- Success and failure both restore the exact pre-trial Hero/inventory/gold/energy snapshot; failure reissues the token.
- Success unlocks Hero level 60, standard equipment +120, artifacts +30.

## Upgrade caps
- Before breakthrough: Weapon/Armor/Ring/Wand +50; Artifact +10.
- After breakthrough: Weapon/Armor/Ring/Wand +120; Artifact +30.
- Artifact post-cap progression is stored separately from native artifact levels so original artifact state remains compatible; equipped capped artifacts continue long-run growth from Hero XP.

## QA
- Infinite World Assist window includes both +1 Hero level and one-click current-stage-cap testing buttons.
- The fast button reaches level 30 before breakthrough and level 60 after success; neither button can bypass the level-30 trial.
- BreakthroughToken is guaranteed to stay in the backpack even when normal capacity is full, using one temporary over-cap slot instead of dropping into the streaming world.

## CI note
- One intermediate 0.5.11 Action failed while downloading the Gradle distribution with `Connection reset by peer`. This was a transient runner network failure, not a compiler failure; earlier/later runs are the authority for code validity.


# 0.5.12 — Shorter Breakthrough Trial

Version:
- versionName: 0.5.12
- versionCode: 958
- dev: assist-0.5.12-short-trial
- target stable: assist-0.5.12-stable
- WORLD_GEN_VERSION: 17 (unchanged)

Changes:
- Breakthrough trial reduced from 10 waves to 5.
- Enemies per wave reduced from 10 to 5.
- Inter-wave delay remains 30 action-value with the existing countdown messages.
- Difficulty span is compressed rather than truncated: wave 5 reaches approximately the old wave-10 finale multipliers.
- Slime is no longer part of normal wave pools. It can appear only as wave-1 slot 5 with 25% probability, so a trial can contain at most one Slime and usually none.
- Wave 5 retains a mixed late-game finale without filling the arena with five simultaneous ranged threats.


# 0.5.13 — Breakthrough Certificate

Version: 0.5.13 / versionCode 959 / WORLD_GEN_VERSION 17 unchanged.

- Fixed stale quickslot references after breakthrough rollback by snapshotting the pre-trial layout as placeholders and rebinding it to restored belongings.
- Lowered wave-5 stat spike while retaining a meaningful finale.
- Added an independent BreakthroughCertificate equipment slot that coexists with artifacts.
- Certificate tiers: lv30/lv40/lv50/lv60, with escalating HP, STR, damage, regen, hunger reduction, gold bonus, movement, vision, shop discount, chest bonus and rechargeable revival.
- Certificate bonuses are queried from the equipped item at calculation time, so unequipping removes them immediately.
- Normal and locked chests may roll one extra supply item; crystal artifact chests are excluded.
- Artifact post-+10 levels now explicitly affect effective level and gain +2.5% recharge efficiency per overlevel, up to +50% at visible +30.

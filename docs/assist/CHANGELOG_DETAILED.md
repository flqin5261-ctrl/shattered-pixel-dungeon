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


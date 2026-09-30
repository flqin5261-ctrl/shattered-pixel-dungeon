# Assist Infinite World 架构与不可破坏约束

本文档面向后续维护者，描述当前 Infinite World 的技术结构、数据流和必须保持的行为。

---

# 1. 总体模型

Infinite World 使用“固定大小活动窗口 + 无限绝对世界坐标”的方式实现。

它不是动态扩容一个无限二维数组。

核心参数：

```java
CHUNK_SIZE = 24
WINDOW_CHUNKS = 7
HALF_WINDOW = 3
MAP_SIZE = 168
SHIFT_STEP = 3
SHIFT_LOW = 24
SHIFT_HIGH = 144
CHUNK_CACHE_LIMIT = 128
```

因此内存里真正存在的 Level.map 始终约为：

```
168 × 168
```

玩家看到的“无限”由窗口不断重新映射到世界坐标实现。

---

# 2. Local Cell 与 World Coordinate

## 2.1 Local

原版 Shattered Pixel Dungeon 的绝大多数系统都认为位置是：

```java
cell = x + y * width
```

Infinite World 保留这个假设。

所以：

- Hero.pos
- Heap.pos
- Trap.pos
- Plant.pos
- PathFinder
- FOV

在当前活动窗口内仍用 local cell。

## 2.2 World

真正的永久位置使用：

- worldX
- worldY

Hero 保存：

- `heroWorldX`
- `heroWorldY`

转换公式：

```
worldX = (centerChunkX - HALF_WINDOW) * CHUNK_SIZE + localX
worldY = (centerChunkY - HALF_WINDOW) * CHUNK_SIZE + localY
```

反向：

```
localX = worldX - (centerChunkX - HALF_WINDOW) * CHUNK_SIZE
localY = worldY - (centerChunkY - HALF_WINDOW) * CHUNK_SIZE
```

任何新增跨位置机制都必须理解这两个坐标系。

---

# 3. Hero Movement / Streaming 生命周期

正常移动顺序：

1. Hero 开始动作。
2. sprite/tween 播放。
3. Hero local pos 改变。
4. `recordHeroMove()` 更新 absolute world coords。
5. tween 完成。
6. `Hero.onMotionComplete()`
7. `InfiniteWorldLevel.afterHeroMotionComplete()`
8. 检查是否进入 streaming band。
9. 若需要：
   - snapshot 当前世界状态
   - 修改 centerChunk
   - rebuildWindow
   - 根据 world pos 算新的 local pos
   - rebase HeroAction target
   - GameScene.refreshInfiniteWorldWindow

**禁止在 motion complete 之前 shift。**

这是已经验证过的硬约束。

---

# 4. 为什么 SHIFT_STEP = 3

活动窗口是 7 Chunk。

如果角色靠近边缘时只 shift 1 Chunk，会频繁 streaming。

曾经更严重的问题是：

- threshold 和 shift 落点组合不合理
- shift 后 Hero 直接落在相反方向触发区域
- 下一次立刻反向 shift
- 地图 A/B 来回闪
- actor 可能卡死

当前一次移动 3 Chunk：

- streaming 次数明显降低
- Hero 重定位到窗口中间安全带
- 两侧留足缓冲
- teleport 也有安全区域

后续若想改窗口尺寸或 shift 参数，必须完整重新验证 hysteresis。

---

# 5. Action Rebase

Streaming 时需要处理：

- Move
- PickUp
- OpenChest
- Buy
- Unlock
- LvlTransition
- Mine
- Alchemy

如果 action 包含 `dst`：

1. shift 前将旧 local dst 转 world coordinate。
2. rebuild。
3. world coordinate 转新 local dst。
4. 若超出新 window：
   - action 取消/null。

否则会发生 stale target。

未来增加新 HeroAction 类型时，如果它持有 cell target，也应加入 rebase。

---

# 6. InfiniteWorldState

核心持久层：

`InfiniteWorldState.java`

当前重要字段/概念：

## 6.1 generatorVersion

用于保护旧世界。

规则：

> 任何会改变同 seed / same chunk 基础地形的生成器大改，都必须提升 WORLD_GEN_VERSION。

当前：

```
WORLD_GEN_VERSION = 11
```

旧存档保存自己的 generatorVersion。

加载旧世界时不能自动强制升级成新公式。

否则：

- 玩家站着的地面可能变墙
- 已探索房间结构突变
- 箱子和墙位置错位
- terrain override 应用到错误基础地形

---

## 6.2 centerChunkX/Y

当前 7×7 window 的中心 Chunk。

不是 Hero 当前 Chunk。

---

## 6.3 heroWorldX/Y

Hero 永久绝对坐标。

所有 teleport / chains / 未来 knockback teleport / Boss portal 都要同步。

---

## 6.4 generatedChunks

表示：

> 生成器曾经构造过该 Chunk。

它不代表玩家看过。

---

## 6.5 exploredChunks

表示：

> 玩家真正探索进入过的 Chunk。

不要和 generated 混用。

---

## 6.6 visited / mapped

Infinite World 不是保存整个无限 boolean[]。

使用 world key / chunk bitset 风格存储。

窗口 rebuild 时再还原到当前 local arrays。

当前会在行走/observe 中增量同步，避免只等到下一次 shift 才存。

---

## 6.7 terrainOverrides

基础地形：

```
baseWindow = deterministic generator output
```

玩家修改：

- 开门
- 炸墙
- 挖矿
- 草变化
- 其他 Terrain change

如果：

```
map[cell] != baseWindow[cell]
```

则保存：

```
worldKey -> terrain
```

回来时：

1. generate base
2. apply overrides

所以用户修改的世界可以持久化。

注意：

如果以后加入更复杂“地形对象状态”，不要只依赖 map terrain；有些对象还要独立 state。

---

## 6.8 chestStates

箱子状态需要区分：

- 原封未开
- 已开但内容仍在
- 已拿空

不能仅通过 terrain 判断。

---

## 6.9 objectStates

V6 起加入。

用于 deterministic 世界对象：

- loose loot
- 植物
- 钥匙
- 主题房奖励

核心理念：

> 生成公式决定“本来应该有什么”；objectStates 决定“玩家已经改变了什么”。

---

# 7. Base Window 与 Chunk Cache

`generateBaseWindow(centerChunkX, centerChunkY)`：

1. 创建 168×168 WALL buffer。
2. 遍历 7×7 Chunk。
3. 检查 chunkCache。
4. 有缓存：
   - pasteChunk
5. 没缓存：
   - generator 生成
6. 全窗口完成后重新 extract active chunks 进 LRU。

Cache：

```
LinkedHashMap<Long, int[]>
limit = 128
accessOrder = true
```

它只是性能优化。

**世界真实性不能依赖 cache。**

即使某 Chunk 被 LRU 淘汰，再次生成也必须 deterministic。

---

# 8. Shared Edge

Chunk 不能完全独立随便开门。

相邻 Chunk 边缘出口必须一致。

使用 shared edge hash：

- north/south
- east/west

相同边界共享同一确定性函数。

未来新增 generator structure 时：

> 不允许让某个 Chunk 单独决定“右边有出口”，而邻居独立决定“左边没有出口”。

否则出现死墙 seam。

---

# 9. Generator 分层

当前代码仍保留历史版本兼容。

大体：

- V1：原型
- V2：随机 hub + shared edges
- V3：视觉稳定
- V4：怪异 district / landmark
- V5：density profile + interactions
- V6：world loot / plants / mixed environment
- V7：enclosed themed rooms
- V8：secret rooms / room-level themes

`generateChunkV2()` 名字虽然仍叫 V2，但内部根据 generatorVersion 分叉增加后续逻辑。

不要被方法名误导。

---

# 10. V5 Space Profile

当前空间 Profile：

- 0：many tiny rooms / closets
- 1：compact
- 2：normal
- 3：broad chambers
- 4：stretched / asymmetric

V7 起概率偏向：

- compact/tiny 为主
- broad 少量保留

因为用户不喜欢“大面积开拓地”。

---

# 11. V7/V8 Theme Room

每 Chunk：

- 通常 2 房
- 一部分 3 房

房间用确定性 `v7RoomSpec()` 得到：

- left
- top
- right
- bottom
- doorX
- doorY
- outsideX
- outsideY
- theme
- doorTerrain
- V8 secret flag

房间 shell：

- 外圈 WALL
- 内圈 EMPTY/主题 terrain
- 一个入口 door
- corridor 向 Chunk core 连接

---

# 12. V8 Door Strategy

目标：

普通门要成为主流可见结构，而不是全地图锁门。

规则：

- 每 Chunk 的第一个 themed room：
  - 普通 DOOR
- 其他：
  - 一部分 LOCKED_DOOR
  - 一部分 SECRET_DOOR
  - 其余 DOOR

Key Room：

- 强制 DOOR

Crystal Vault：

- 不允许被 IronKey 再锁一层
- chest 自己使用 CrystalKey

---

# 13. Secret Door

使用原版：

```
Terrain.SECRET_DOOR
```

原版 flags：

- WALL
- SECRET
- LOS_BLOCKING
- SOLID

discover：

```
SECRET_DOOR -> DOOR
```

因此 Hero 原版 Search 系统天然支持。

不要自己造第二套 secret boolean。

---

# 14. Room 内容确定性

主题房物品不能用“每次 rebuild 重新 Random 一次”。

否则：

- 走远回来奖励会换
- 可刷物资

所以内容选择必须基于：

- cx
- cy
- roomIndex
- slot
- salt
- Dungeon.seed

需要调用原版 `Generator.random...` 时：

使用：

```java
Random.pushGenerator(hash(...))
try {
    ...
} finally {
    Random.popGenerator()
}
```

确保不污染全局 RNG。

---

# 15. Plant Persistence

Plant 是对象，不只是 Terrain。

生成时：

- 根据 deterministic slot 创建 Seed
- seed.couch(cell, level)
- plants.put

snapshot 时：

- 如果本应存在
- 但 plants.get(cell) 已不存在

则 objectStates 标记 consumed。

否则走远回来会重新长出来。

---

# 16. Trap Persistence

V5 terrain generator 先放：

- SECRET_TRAP
- TRAP

然后 runtime 根据 absolute coord hash 重建具体 Trap 类。

触发后：

- Terrain 变 inactive
- map override 保存

下次 reload 不会按原始 SECRET_TRAP 重建新 trap。

未来新增 Trap class 应保持这个逻辑。

---

# 17. Heap / Chest Persistence

确定性容器不能只通过 `heaps.contains(cell)` 判断。

因为：

- unopened chest
- opened heap
- empty removed heap

是不同状态。

当前有：

- chestStates
- objectStates

Crystal Chest 也使用 chest state。

未来新增：

- locked chest
- mimic chest
- altar reward

应设计明确状态枚举，不要只依赖 sprite 是否存在。

---

# 18. Global Key System

原版 Key 通常包含：

- depth
- type

Infinite World 是一个连续世界。

因此 `Key.isSimilar()` 在 `Dungeon.infiniteWorld` 时忽略 depth。

注意：

这意味着：

- IronKey 全球通用
- CrystalKey 也按类型全球通用

Hero 原版针对 branch/subfloor 的“不允许钥匙”条件也加了 Infinite World 例外。

未来如果想做：

- BossKey
- AncientKey
- region-specific key

不要直接继承同一全局行为而不加新类型/规则，否则会被普通旧钥匙误开。

---

# 19. Teleport 约束

任何 teleport 必须满足：

1. target 在当前 active map 中。
2. 目标 passable。
3. 尽量不让角色落到 streaming trigger 外。
4. 完成后更新 Hero absolute world position。
5. 更新 FOV/Fog。
6. 必要时 interrupt current action。

当前 ScrollOfTeleportation 在 Infinite World 有特殊 policy。

不要直接调用原版“随机全层 cell”逻辑。

---

# 20. Ethereal Chains

Chains 直接改变：

```
hero.pos
```

因此曾经会漏掉 `heroWorldX/Y`。

当前 Chains 完成后调用 InfiniteWorldLevel 同步。

未来任何 item：

- Blink
- Dash
- Jump
- Portal
- teleport trap
- Boss pull/push

都要检查是否绕过普通 Hero.move。

---

# 21. Assist 与 Infinite World 限制

WndAssist 的楼层传送：

```
if (Dungeon.extraChallenge || Dungeon.infiniteWorld)
```

禁止。

原因：

Infinite World 没有普通 depth/floor 跳转语义。

不要重新开放，除非专门做“世界坐标传送”。

---

# 22. GameScene Streaming Refresh

Infinite World 不重建整个 GameScene。

当前走：

`GameScene.refreshInfiniteWorldWindow(shiftedCellsX, shiftedCellsY)`

主要步骤：

1. clear/re-add heaps
2. clear/rebuild customTiles
3. clear/rebuild customWalls
4. resetMap
5. flush main tilemaps
6. HeroSprite rebase
7. Dungeon.observe
8. Fog refresh + flush

为什么不重建 GameScene：

- 太重
- UI/actor 状态容易丢
- Tween/animation 容易错
- 音乐/窗口可能闪

---

# 23. Tilemap.flushMapUpdate

这是 Infinite World 稳定性的关键底层修改。

普通 Tilemap：

- updateMap → dirty
- draw → upload GL

Infinite World streaming：

必须：

- dirty
- **同步 upload**
- camera rebase

所以 `flushMapUpdate()` 必须最终触发：

```
buffer.updateGLData()
```

不要“优化掉”。

---

# 24. Large Tilemap Batch

168×168 Tilemap 远大于普通 SPD Level。

VBO/index 绘制曾经触及 unsigned-short index 限制。

因此 0.3.6 后有：

- vertex offset 支持
- large quad batch 分段

未来如果：

- WINDOW_CHUNKS 增大
- custom overlay 数量暴增

要重新检查：

- vertex count
- index count
- draw call
- memory
- Android GPU

不要假设 PC 能跑就代表手机没问题。

---

# 25. Water Layer

原版真实 water cell 大量依赖：

`SkinnedBlock water`

DungeonTerrainTilemap 并不是每个 water cell 都直接画完整水面。

所以：

**不要永久关闭 water backdrop。**

这条是历史重大事故总结。

---

# 26. Fog

Infinite World Fog 需要：

- visited
- mapped
- heroFOV
- wallBlocking

一起正确 refresh。

只更新 map texture 不够。

窗口 shift 后需要同步 Fog flush。

缩放限制 1.8x 也是为了避免玩家过度看到活动 window 边界/未及时更新区域。

---

# 27. Mixed Environment Architecture

当前混贴图是 visual-only。

逻辑 map 仍只有统一 Terrain。

使用：

`InfiniteWorldAccentTilemap`

模式包括：

- MODE_FLOOR
- MODE_WALLS

优点：

- 不改碰撞
- 不改 pathfinding
- 可以混原版 tileset

缺点：

- static custom overlay 遇到动态 Terrain 容易 stale

0.4.2 已专门排除所有动态门。

后续应实现：

> 局部 overlay invalidation / rebuild

而不是不断增加“这个 terrain 也排除”。

---

# 28. Secret Room Visual Rule

Secret Door 尚未 discover 时：

- 不创建独立 room environment overlay

这是为了防止不同墙材质暴露秘密房。

已知副作用：

- discover 后该 room 的专属主题可能要等下次 streaming rebuild 才出现。

未来优化方式：

- discover cell 时定位所属 room
- rebuild 该 room overlay
- 或重建 customTiles/customWalls 但不重建整个 map

---

# 29. Music

InfiniteWorldLevel 当前播放：

- 15 首普通区域曲目
- Sewers 3
- Prison 3
- Caves 3
- City 3
- Halls 3

通过 `Music.INSTANCE.playTracks(...)` 轮换。

不要退回整局单曲。

未来可以按 biome/room 做动态切歌，但需要避免频繁跨房切歌。

---

# 30. Save / Restart

Restart Infinite World：

必须删除完整 Infinite World slot/save data。

不能只：

- 重置 Hero
- 重置 level

还要重置：

- InfiniteWorldState
- seed
- explored
- overrides
- object states

然后生成新 seed。

---

# 31. 原版 Room System 如何参考

原版 `levels/rooms/` 是非常有价值的素材库。

可参考：

## Standard

- PlantsRoom
- HallwayRoom
- CellBlockRoom
- PillarsRoom
- LibraryHallRoom
- RingRoom
- RuinsRoom
- AquariumRoom
- MinefieldRoom
- StudyRoom
- SuspiciousChestRoom

## Special

- GardenRoom
- LaboratoryRoom
- LibraryRoom
- MagicWellRoom
- PoolRoom
- RunestoneRoom
- SacrificeRoom
- StatueRoom
- StorageRoom
- TrapsRoom
- TreasuryRoom
- CrystalVaultRoom

## Secret

- SecretGardenRoom
- SecretHoardRoom
- SecretLarderRoom
- SecretLaboratoryRoom
- SecretLibraryRoom
- SecretMazeRoom
- SecretRunestoneRoom
- SecretWellRoom

建议：

抽取：

- 尺寸范围
- 内容布局
- door type
- reward density
- painter pattern

不要直接把有限楼层 graph generator 当 Infinite World generator。

---

# 32. 未来 Mob Architecture 必须先设计

现在 `createMobs()` 仍返回不生成敌人。

正式加入怪物前必须决定：

## 32.1 World Identity

Mob 不能只用 local pos。

要有：

- world chunk
- world x/y
- unique deterministic/spawn id

## 32.2 Unload

远离 active window 的 mob：

- 不应继续 Actor tick
- 不应留 sprite
- 状态要 snapshot

## 32.3 Reload

回到 Chunk：

- 已死 mob 不复活
- 受伤 mob 保持 HP
- 掉落保持
- elite/buff 保持必要状态

## 32.4 Movement

Mob 追玩家到 window 边缘时：

不能让它引用已卸载 local cell。

## 32.5 Spawn

不能每次 Chunk reload 都重新刷满怪。

需要：

- deterministic initial spawn
- persistent dead state
- 可选 respawn 系统另做

---

# 33. Boss Architecture

Boss 更严格。

必须有：

- boss landmark world key
- once-only spawn state
- defeated state
- arena state
- reward state
- window unload handling

Boss 不能因为：

- 走出 arena
- streaming
- save/load

复制。

---

# 34. 性能原则

Infinite World 最危险的性能点：

1. 49 Chunk 同时存在
2. 168×168 Terrain
3. 多 custom Tilemap
4. 每 Chunk 多容器/植物/陷阱
5. 未来 mobs
6. Fog
7. save state

原则：

- deterministic 生成尽量 O(chunk area)
- 不要每步遍历整个 168×168 做重计算
- exploration 已有 bounded incremental sync
- object snapshot 可在 shift/save 时做
- mob AI 未来只 tick 活跃范围

---

# 35. 更新架构文档的触发条件

以下任何变化都必须更新本文件：

- CHUNK_SIZE 改
- WINDOW_CHUNKS 改
- SHIFT 参数改
- 新 state 类型
- 新 generator version
- 新 persistence 体系
- 新 rendering layer
- 新 teleport 系统
- 新 monster persistence
- Boss architecture
- applicationId/signing/build changes


# 36. Generator V9：世界级 Infinite Backbone

V9 增加一个与普通 Chunk 生成器分离的“世界级连通保障层”。

普通 generator 仍然负责：

- hub
- themed room
- shared edge
- door
- biome
- prop
- landmark

这些局部结构允许出现死胡同。

随后 V9 最后执行：

`carveV9InfiniteBackbone()`

规则：

```
cx mod 6 == 0 -> Chunk 中心双格南北贯通
cy mod 6 == 0 -> Chunk 中心双格东西贯通
```

原点同时在两类 spine 上。

### 为什么这比“每个 Chunk 四边都强制开口”更合适

用户明确接受局部 dead end。

若强制所有边界永远连通：

- 地图会重新变得规则；
- Secret/特殊房更难形成；
- 岔路缺乏真正终点；
- 异常空间也失去封闭/迷失感。

Infinite Backbone 只保证：

> 从原点所在连通分量中始终存在无限延展路线。

它不保证每一条路都继续。

### 硬约束

V9 及后续 generator 如果增加会重写大量地形的步骤：

- Infinite Backbone 必须在这些步骤之后刻出；
- 或者新步骤明确不得覆盖 backbone cell。

否则会再次出现“理论无限但实际全被堵死”的问题。

---

# 37. V9 Liminal Macro Region

异常空间不是单 Chunk 特殊房。

它使用 5×5 Chunk 宏区：

```
macroX = floorDiv(cx, 5)
macroY = floorDiv(cy, 5)
```

宏区 hash 决定：

- 是否 anomaly
- anomaly type

约 8% macro 为 anomaly。

出生附近 abs(cx/cy)<=4 排除，避免开局直接陷入巨大重复空间。

当前类型：

1. Repeating Offices
2. Pool Halls
3. Endless Hall

每个 anomaly chunk：

- 跳过正常 themed-room carve；
- 跳过普通 ambient container/loot/plant；
- 仍保留 shared edge gateway；
- 仍保留 Infinite Backbone；
- 使用 deterministic full-chunk alternate environment overlay。

### 为什么 macro size = 5

单 Chunk 24×24 太小，无法形成真正的“大而重复”体验。

5×5 = 120×120 cell，在正常 7×7 active window 中：

- 玩家可以连续跨很多 streaming-independent Chunk；
- 同一异常风格会持续很久；
- 仍然能与现有 7×7 window 共存，不需要扩大 GPU Tilemap。

因此这种“大地图体验”没有重新触碰 0.3.6 已解决的大 VBO/window 尺寸风险。

---

# 38. V9 Anomaly Field Notes

异常空间资料没有另造独立存档系统，而是复用原版 Journal Document。

新增：

```
Document.INFINITE_WORLD_NOTES
InfiniteWorldNote extends DocumentPage
```

Document 页面：

- Liminal_Offices
- Pool_Halls
- Endless_Hall

Guide UI 在 Adventurer's Guide 下面增加第二个 section。

### 世界生成

每个 anomaly 5×5 macro：

- 只在 local macro chunk (2,2) 放 1 张 Note；
- Note 放在 Chunk 中央稳定可通行 cell；
- anomaly chunk 本身不撒普通大量物资，因此该 cell 不与 ambient loot 争位置。

### 持久化

物理 Note 的世界存在状态：

- InfiniteWorldState.objectStates

指南已解锁状态：

- 原版 Journal/Document persistence

拾取 Note：

1. DocumentPage.findPage
2. 页面变 FOUND
3. Note 从 Heap 移除
4. objectStates 在下一 snapshot 标记 consumed
5. 回到该 macro 不会重刷

这种双层状态的意义：

- world item 不重复；
- journal entry 永久保留。

---

# 39. V9 Scarcity Principle

V5～V8 逐步叠加了多个“内容来源”：

- base chest
- v5 container
- v6 loose loot
- v6 key
- wild plant
- themed room rewards
- secret bonus

单独看每层概率都不夸张，但叠加后单位 Chunk 奖励过密。

V9 不删除系统，而是为新 generatorVersion 对每层做低概率 gating。

架构原则改为：

> **地形与可交互物可以高密度，高价值永久物资必须低密度。**

未来再增加：

- monsters
- boss reward
- NPC trade
- new special room

时，要把它们算入整体经济，而不是继续无限叠加奖励层。

---

# 40. V9 Secret Room Availability

V8 只有随机概率 SECRET_DOOR，导致“代码有功能但玩家测试不到”。

V9 加入宏区级 deterministic placement：

- 3×3 Chunk macro
- hash 决定一个 local chunk
- 该 Chunk room index 1 强制 Secret Door

这不是把 Secret Room 存在状态保存到 state；它仍然是 generator deterministic output。

Secret Door discover 后：

- Terrain override 保存普通 DOOR 状态
- 下次 reload 不重新隐藏。

Infinite World 下近距离被动 Search 对 SECRET_DOOR 使用 55% chance。

SECRET_TRAP 保持自己的低发现率。

---

# 41. 后续怪物系统与 V9 Backbone

未来 Mob AI 接入时，Infinite Backbone 不能直接被当成“怪物高速公路”。

第一版 Mob：

- 仍只在 active simulation radius 内寻路；
- 不允许用 backbone 做跨无限世界全局 PathFinder；
- Mob world persistence 使用 chunk/world coordinates；
- dead-end 支路依然是合法 AI 环境。

Boss Arena 也不应永久破坏 Infinite Backbone。

若 Boss 战要临时锁路：

- 用 runtime blocking/door state；
- 战斗结束恢复；
- 不把永久 WALL override 写进所有 backbone 通路。



# 42. Generator V10：多层级无限路线网络

V9 只从“世界整体连通性”角度保证无限：每 6 Chunk 一组横纵主干。

V10 进一步解决“只有主干无限、旁路经常死”的体验问题。

## 42.1 两级无限网络

### 一级：Primary Spine

继续使用 V9：

- `cx mod 6 == 0`：纵向双格主干
- `cy mod 6 == 0`：横向双格主干

### 二级：Secondary Infinite Routes

横向：

- 每 18 个 Chunk 高度为一个 band
- 每个 band seed/hash 决定一个非主干 Chunk row
- 这一整行 Chunk 都生成无限东西向支线

纵向：

- 每 18 个 Chunk 宽度为一个 band
- 每个 band seed/hash 决定一个非主干 Chunk column
- 这一整列 Chunk 都生成无限南北向支线

## 42.2 Shared Boundary Contract

次级路线不能简单在每个 Chunk 独立随机入口。

横向支线：

```
westY = hash(edgeX = cx, branchCy)
eastY = hash(edgeX = cx + 1, branchCy)
```

相邻 Chunk 对共同边界使用同一个 `edgeX`，因此得到相同 Y。

纵向同理：

```
northX = hash(branchCx, edgeY = cy)
southX = hash(branchCx, edgeY = cy + 1)
```

这个共享边界规则是 V10 支线不会在 seam 断裂的关键。

## 42.3 为什么支线不是孤岛

横向 secondary route 会在所有 `cx % 6 == 0` Chunk 中穿过纵向 primary spine。

纵向 secondary route 会在所有 `cy % 6 == 0` Chunk 中穿过横向 primary spine。

因此：

- 次级线周期性重新接入主干；
- 横纵次级线也会互相交叉；
- 玩家可以从主干进入次级线后无限走；
- 也可以从次级线再回到别的主干区域。

## 42.4 为什么仍允许普通 Dead End

并不是所有局部 corridor 都提升为 secondary route。

否则地图会变成高密度规则网格。

V10 保留：

- local hub tree dead end
- themed room dead end
- secret room
- short side corridor
- small cul-de-sac

设计目标：

> 保证“有很多条无限路线”，而不是“每条路线都无限”。

## 42.5 Generator 顺序约束

V10 network 必须在：

- themed room
- biome decoration
- props
- anomaly layout

之后 carve。

否则后生成墙体可能重新堵住 secondary route。

对于 anomaly chunk，生成 anomaly 后再 carve V10 network。

未来任何 V11+ 大地形步骤也必须遵守这一顺序，或者显式保护 V10 route cells。


# 43. 0.5.0 Player-Centered Ephemeral Mob Ecology

旧 Roadmap 曾设想“每个 Chunk 的怪永久保存”，但用户在真正进入怪物阶段时明确选择了更接近 Minecraft 的模型：

> 远离玩家一定范围的普通/精英怪直接 despawn，不保留远处世界实体。

因此 0.5.0 的正式架构是 **ephemeral ecology**，而不是 chunk-persistent population。

## 43.1 Ecology Actor

InfiniteWorldLevel 不使用原版 RegularLevel 的整层预生成敌人。

`createMobs()` 初始不创建敌人。

`addRespawner()` 创建：

`InfiniteWorldMobEcology extends Actor`

它：

1. 进入世界后延迟 20～32 turn。
2. prune 过远敌人。
3. 若当前敌人低于 target cap，按低概率尝试生成 1 只。
4. 再等待 32～50 turn。

不能改回原版 `MobSpawner` 的“spawn 失败后下一回合立刻重试”逻辑，否则会显著抬高 Infinite World 刷怪密度。

## 43.2 Cap

两级限制：

```
MOB_SPAWN_TARGET_CAP = 5
MOB_HARD_CAP = 6
ELITE_HARD_CAP = 1
```

Ecology 自己到 5 后停止常规刷怪。

如果 Trap/其他运行时机制额外造敌人，prune 阶段会在超过 6 时优先移除距离最远的普通敌人。

Boss/MINIBOSS 预留为未来例外，目前没有生成。

## 43.3 Spawn Annulus

新敌人必须：

- 路径距离 >=14
- 路径距离 <=28
- 不在 heroFOV
- passable
- 非 solid/pit/secret
- 无 Actor
- 无 Heap/Trap/Plant 占位
- LARGE 时需 openSpace

每次 spawn 才构建 bounded PathFinder distance map，不在每个 Hero step 做完整寻路。

## 43.4 Despawn

Hero 每次 recordHeroMove 后，只遍历当前很小的 mobs 集合，使用 local Chebyshev 距离判断。

```
distance > 40 -> despawnFromInfiniteWorld()
```

`Mob.despawnFromInfiniteWorld()`：

- `Actor.remove(this)`
- `Dungeon.level.mobs.remove(this)`
- interrupt sprite motion
- killAndErase sprite

**绝对不能调用 `die()` 或 `destroy()`。**

原因：

- `die()` 会 roll loot
- `Mob.destroy()` 会加击杀统计、EXP、Bestiary 等
- 这与“走远自然消失”语义完全不同

## 43.5 Streaming Rebase

Streaming 时中心移动 3 Chunk = 72 cell。

当前活跃 Mob 的绝对世界位置不变，local pos 改：

```
newX = oldX - shiftedCellsX
newY = oldY - shiftedCellsY
```

`Mob.rebaseForInfiniteWorld(newPos)` 同时：

- interrupt old sprite motion
- pos = newPos
- previousPos = -1
- path = null
- clearEnemy
- target = newPos
- 非 PASSIVE -> WANDERING
- sprite.place(newPos)

清 path/target 是硬要求，否则 Mob 会继续使用旧 window 的 path cell。

## 43.6 Save/Load

当前 active mobs 仍由 Level 原生：

```
bundle.put(MOBS, mobs)
```

保存。

所以：

- 玩家附近还未 despawn 的怪可以正常 save/load。
- 已经因距离 despawn 的怪不再保存。
- 不建立随探索面积无限增长的 mob-state map。

## 43.7 Enemy Pools

选择的是较少依赖楼层脚本的普通敌人。

明确排除：

- Boss/Miniboss
- Thief（偷物后 despawn 风险）
- 大量 summon-heavy 敌人

后续增加新怪前检查：

1. 是否偷/持有玩家永久物品；
2. 是否召唤大量子怪；
3. 是否依赖固定楼层/Room/Boss state；
4. despawn 时 Buff.detach 是否有环境副作用；
5. 是否会突破 hard cap。

## 43.8 Elite

精英复用 ChampionEnemy。

当前允许：

- Projecting
- AntiMagic
- Blessed
- Growing

禁止 Blazing 用于 ephemeral elite，因为其 detach 可释放 Fire。

未来如果加入新的 Elite buff，必须测试：

> reward-free despawn 时 detach 是否真正无副作用。


# 44. Generator V11：Merchant Outposts

V11 将商人定义为 deterministic world structure，而不是 ecology spawn。

## 44.1 为什么不用普通 Mob Ecology
商人必须：
- 有固定地点；
- 有固定库存；
- 可离开后回来继续交易；
- 不因 40 格敌人 despawn 消失；
- 不能随着低频刷怪随机重复出现。

因此使用 `InfiniteWorldShopkeeper extends Shopkeeper`，并保存 `shopChunkX/Y`。

## 44.2 空间约束
商人候选采用 seed 驱动 7-Chunk lattice。
7×7 active window 覆盖的 chunk coordinate span 为 6，因此任意窗口最多一个 lattice point，可避免同时存在多个 Shopkeeper。

候选还排除：
- 出生 4 Chunk 范围；
- V9 anomaly macro；
- V10 primary spine；
- V10 secondary infinite route。

最后一条很重要：V10 无限道路是在房间之后最终 carve，如果商店位于该路网上，可能被道路穿墙。V11 直接避免这种冲突。

## 44.3 商店房
商店复用 themed room index 0，但 theme 强制为 10：
- 普通门；
- 内部 EMPTY_SP / EMPTY_DECO；
- 不生成原本的植物/卷轴/藏宝室奖励；
- 不允许 legacy chest 在商人 Chunk 叠加。

## 44.4 库存
库存使用原版 `Heap.Type.FOR_SALE`，因此：
- 点击商品继续走 `WndTradeItem`；
- 金币结算继续使用原版 Shopkeeper；
- 玩家可向商人出售物品；
- 回购列表在商人仍 active 时保留。

每个 stock slot 使用 deterministic world cell + objectState key。
购买导致 FOR_SALE heap 消失，下一次 snapshot 将 slot 标记 consumed。

## 44.5 商人状态
商人被攻击/偷窃失败调用 flee 时：
`InfiniteWorldShopkeeper.flee()`
先调用 `markInfiniteWorldMerchantGone(cx,cy)`，再执行原版 flee。

普通 Streaming unload 不能调用 flee/destroy，只调用 reward-free actor removal，因此不会把据点错误标记为关闭。

## 44.6 Streaming
Window shift：
- Enemy 继续使用 0.5.0 距离 prune/rebase。
- InfiniteWorldShopkeeper 单独处理：
  - 仍在新 window -> local pos rebase；
  - 离开 window -> unload；
  - 新 merchant chunk 进入 -> `ensureV11Shopkeeper(true)` 补入 GameScene。

商人的 buyback 历史只在 NPC 连续 active 时保存；一旦商人完全离开活动窗口后重新生成，旧 buyback 历史不保证保留。正式库存状态不受影响。

# V12 — 可发现性保证与唯一神器箱不变量

## 1. Generator 兼容
V12 的商人密度和 anomaly 概率只对 generatorVersion >= 12 生效。
旧 V11 世界必须继续得到旧商人格点与 8% anomaly 结果，不能因为安装新 APK 改写已存在世界的基础布局。

## 2. 唯一神器箱必须使用绝对世界状态
保证神器箱不是普通随机房间奖励，也不能只依赖当前 local cell。
InfiniteWorldState 保存：
- heroActionValue
- artifactChestWorldX / artifactChestWorldY
- artifactChestArtifactIndex
- artifactChestState

状态 0/1/2/3 分别表示未生成、关闭水晶箱、已开但神器仍在、神器已取走。
任何 Window shift / Save / Load / rebuildWindow 都只能恢复同一个世界对象，绝不能再次执行一次新的“400 行动值奖励”。

## 3. 行动值定义
只累计 Hero 正向 spend / spendConstant 时间；负 spend 不计入。
阈值当前固定为 400。
如果阈值到达瞬间没有合法附近格子，本次不改变 state=0，之后正向行动会继续尝试，直到能安全生成。

## 4. 神器唯一性
首次确定神器类型时优先从 Generator.Category.ARTIFACT 当前剩余 probs 中选择，并用 Generator.removeArtifact() 消耗该类型，避免保证箱与后续随机神器重复。
恢复箱子时只能按已经保存的 artifactChestArtifactIndex 重建，不能重新抽取。

## 5. 可发现性
保证箱生成位置必须：
- 在当前 active window 内；
- Hero 可达；
- 避开 solid/pit/secret、角色、trap、plant 和非普通 Heap；
- 优先 2～5 cell，必要时 1～7 cell；
- Heap.seen=true；
- 同步 visited/mapped；
- 给一把 CrystalKey，避免唯一奖励 soft-lock。

## 6. 异境便笺
Document.INFINITE_WORLD_NOTES 的页面发现状态是“是否还需要近身保证”的权威状态。
V12 进入尚未收录的 anomaly 类型时，可在 Hero 附近补一张可见便笺；一旦页面收录，补偿逻辑必须停止。
原 deterministic anomaly anchor note 仍可存在，用于保持世界级固定内容设计。

## 7. 不得破坏的旧不变量
本版不授权修改：
- Streaming 只能在 Hero.onMotionComplete() 后执行；
- SHIFT_LOW=24、SHIFT_HIGH=144、SHIFT_STEP=3 的 hysteresis；
- animated water backdrop / synchronous VBO flush；
- 普通怪距离清除必须使用 despawnFromInfiniteWorld()；
- applicationId、固定签名和 versionCode 单调递增。

# V13 — 商人发现机制与六类 Liminal 不变量

## 商人据点
V13 仍然把 Merchant Outpost 视为 deterministic world structure，而不是运行时随机 NPC。

生成：
- V11 spacing=7；
- V12 spacing=5；
- V13 spacing=4；
- V13 出生排除约 2 Chunk。
旧 generatorVersion 必须继续使用各自旧公式。

道路安全约束不变：
- anomaly Chunk 不放普通商店；
- Primary spine Chunk 不放商店；
- Secondary infinite route Chunk 不放商店。
原因仍是主题房先于 V10 route carve，不能让道路后处理切穿商店壳体。

## 可发现性
“loaded”不等于“player discovered”。
V13 规定：
- active window 中存在的 merchant room 可以直接写入 mapped；
- FOR_SALE heaps 可设 seen；
- 当未提示过的 outpost 距 Hero <=2 Chunk 时，只提示一次；
- 提示状态必须使用该据点自己的 objectState key，不能使用全局一次性 boolean。

该机制只揭示商店，不修改 Hero FOV，不传送 Hero，也不改变 Streaming 触发时机。

## Anomaly V13
V13 anomaly chance=28%，type count=6。
V9-V11=8%/3 types，V12=15%/3 types，旧世界不可升级重算。

新类型必须遵守：
- 仍是 5×5 macro 共用一个 anomaly type；
- shared edge contract 保留；
- anomaly chunk 末尾仍接四边 gateway；
- Generator V10+ 继续 carve infinite network；
- 不能新增独立 Water renderer；
- 大面积 WATER 仍只能走原版 animated water backdrop。

## Field Notes
Document.INFINITE_WORLD_NOTES 现有 6 页。
每一个 anomaly type 必须映射唯一 page key。
近身补偿只以 Document 页面是否已发现为停止条件，不能因为 anchor note 生成失败而永久失去该页面。

# V14 — Landmark 连通性与动态 Accent 不变量

## Merchant landmark connectivity
Merchant Outpost 不能再依赖“生成时曾经刻过 corridor”作为可达证明。

硬约束：
1. 检查时机必须晚于 themed rooms、biome、props、V10 infinite network 等所有可能改写 terrain 的步骤。
2. Merchant entrance 必须有实际 PASSABLE path 到至少一个 shared-edge gateway。
3. 若已有 path，不得为统一外观而重复改地形。
4. 自动补路优先避开其他 themed-room rectangles。
5. Chunk 外边界只能通过已有四个 gateway 之一离开。
6. Legacy V11-V13 可以在 active window runtime repair，但必须让 terrainOverrides 持久化结果。
7. Runtime repair 不得把玩家已经打开/炸掉且仍 passable 的 merchant entrance 强制复原。

这一规则以后同样适用于 Boss landmark / quest landmark。

## Accent overlay runtime synchronization
InfiniteWorldAccentTilemap 是 visual-only，不得改变 collision。
因此必须始终满足：
> 如果 Level.map 已经变成可通行 terrain，custom overlay 不得继续显示旧 solid wall。

当前机制：
- GameScene.updateMap(cell) -> refresh affected InfiniteWorldAccentTilemap；
- wall stitch/overhang 依赖邻格，因此 affected 判定含 1-cell halo；
- refresh 只重建对应小 overlay，不重建整个 GameScene；
- SECRET_DOOR discovery 是特殊情况：隐藏房原先没有 room overlay，需要重建 accent definitions 并重新挂载 custom floor/wall groups。

禁止回退到：
- 只更新 base DungeonTerrainTilemap；
- 等下一次 Streaming 才修正 custom wall；
- 为每一种可变 Terrain 单独永久 hardcode exclude。

# Infinite World Spectator QA Mode 不变量

0.5.6 增加仅用于 Assist 调试的无界旁观测试模式。它的目标是加速实机 QA，而不是成为新的世界物理规则。

1. 不得修改真实 Terrain 来实现穿墙。
   - 不得把 WALL 批量改 EMPTY。
   - 不得永久修改 Level.passable / solid / pit。
   - 穿墙只能使用 Hero 专属临时 QA path map。

2. Streaming 生命周期保持原样。
   - 每一步仍是正常 Hero.move。
   - recordHeroMove() 仍更新绝对世界坐标。
   - window shift 仍只能由 Hero.onMotionComplete() -> afterHeroMotionComplete() 触发。
   - 禁止为了快速飞行而改回移动中 Streaming。

3. 探索必须是真实探索。
   - Dungeon.observe()、heroFOV、visited、mapped、syncExplorationArea() 不得绕开。
   - 经过的区域应和正常步行一样永久记录。
   - 旁观模式不是一次性 Magic Mapping，也不是全图 X-Ray。

4. 怪物 AI 必须冻结。
   - Actor.process() 在调用 Mob.act() 之前拦截。
   - 模式开启时普通 Mob 不得检测 Hero、更新 AI、移动或攻击。
   - InfiniteWorldMobEcology 不得自然生成新敌人。
   - 冻结的 Mob 可以继续显示，便于观察 spawn/location bug。

5. Hero 交互必须保留。
   - Chest / Heap / FOR_SALE / NPC interact / Unlock 等仍走正常 HeroAction。
   - 不应因为被冻结的 visibleEnemies 阻止拾取、购买或连续测试移动。

6. 旁观移动非物理。
   - Level.occupyCell(Hero) 在旁观模式中不触发 trap / plant / chasm / floor blob / door-enter 等踩踏逻辑。
   - Hero 在此模式免疫伤害，并忽略 Root / Paralysis / Vertigo 对 QA 移动的阻断。

7. 视觉必须允许看见 Hero。
   - 穿进 wall cell 时 Hero Sprite 临时渲染在 wall/raised terrain 之上、fog 之下。
   - 禁止通过删除墙视觉来解决人物被遮挡，因为那会污染地形 QA。

8. 关闭模式必须安全。
   - 如果 Hero 正在非法站立格，先落到最近正常可站立且未被角色占据的格。
   - 随后恢复普通 pathing / collision / Hero render layer。

# V15 — Backrooms District Archive 与成长型 Merchant 不变量

## 14 类特殊环境
V15 的 Level 0/1/2/3/4/5/6/7/8/9/10/11/37/94 目前只是 Infinite World 的 normal district。
硬约束：
- 不需要特殊入口。
- 不需要 noclip 事件。
- 不需要特殊物品切换层级。
- 不创建独立 Level 实例。
- 每个 district 仍必须满足 shared-edge gateway contract。
- V10 primary/secondary infinite routes 仍是最终全局连通骨架。
- Pool/Water 类型只能复用原版 animated water backdrop，不允许新建全屏水层。

## Archive acquisition
- Generator V15 不生成 InfiniteWorldNote。
- `recordHeroMove()` 根据 Hero 绝对世界 Chunk 判断当前 district。
- 对首次进入的类型调用 `Document.INFINITE_WORLD_NOTES.findPage(page)`。
- Document 本身负责 Journal persistence。
- 老 V9-V14 保存仍可继续使用旧六类 physical note keys，因此这些 legacy page keys 不能删除。

## Merchant site persistence
V15 merchant frequency 可以随进度提高，但禁止用 mutable progress 每次重算旧地图。
规则：
1. 基础候选为 3×3 deterministic lattice。
2. tier 1-4 依次加入额外 deterministic sparse lattices。
3. 一个 Chunk 第一次参与生成时把最终 merchant/no-merchant 决定写入 `objectStates`。
4. 后续无论 Hero 等级或 action value 如何变化，已锁定 Chunk 的 merchant status 不得变化。
5. anomaly、primary road、secondary route 排除仍优先于 merchant candidate。
6. V14 shop connectivity final-pass validation 对 V15 继续生效。

## Merchant progress tier
`tier = max(levelTier, actionTier)`。
- level: <4=0, 4+=1, 7+=2, 11+=3, 15+=4。
- action: <300=0, 300+=1, 800+=2, 1600+=3, 2800+=4。
商人第一次进入 Hero 2 Chunk 范围时把 tier 锁入 per-outpost objectState；库存此后不因 Hero 再升级而变化。

## Mandatory bag offer
四个 progression bags 为 VelvetPouch / ScrollHolder / PotionBandolier / MagicalHolster。
- 只要 Hero 仍缺至少一个，任一新遇到的 V15 商人 slot 0 必须是当前 missing set 中的一件。
- 实际接近商人时必须再次检查 Hero inventory；预生成选择若已经不再 missing，必须 retarget。
- 买走 slot 以后沿用既有 consumed state，不得刷新。
- 全部四包获得后 slot 0 回退到 PotionOfHealing。
- Infinite World Bag 购买价使用 value×2（min 20）；普通商品继续原版 Shopkeeper pricing。

## Merchant rarity progression
- tier0 equipment: basic weapon/armor only。
- Wand/Ring 等高价值类型只能随 tier 引入。
- +1 merchandise 仅 tier3/4 按低概率出现。
- Artifact 永远不进入普通 merchant stock。
- 不允许用世界距离替代 V15 的 Hero level/action progression；旧 V11-V14 仍保留自己的旧计算。

# 0.5.8 Spectator / Decoration invariants

## Spectator visual and timing invariants
- Infinite World spectator movement is a floating glide, not a run animation.
- Hero.speed() and CharSprite movement tween must both honor at least x8 while spectator mode is active; do not speed only one side.
- CharSprite.State.LEVITATING is spectator presentation only and must be removed when the mode is disabled.
- Spectator FOV may use ShadowCaster.MAX_DISTANCE (20); normal gameplay view distance must not be changed.
- The larger FOV does not change Streaming thresholds or timing.
- Streaming remains Hero.onMotionComplete() -> InfiniteWorldLevel.afterHeroMotionComplete() only.

## External decoration invariants
- Kenney Tiny Town/Tiny Dungeon props are CC0, 16x16 and fetched from a pinned upstream commit by scripts/fetch-assist-cc0-assets.sh.
- The build must fail if either downloaded PNG does not match its recorded Git blob SHA.
- InfiniteWorldDecorationLayer is visual-only. Never use these props to change map[], solid/passable arrays, water, doors, pathfinding or object-state gameplay.
- Decoration placement must run after generated heaps/traps/plants/themed-room objects so props do not cover gameplay objects.
- Props must remain inspectable: custom tile image(), name() and desc() must keep working through WndInfoCell.
- Decoration overlays are appended after broad InfiniteWorldAccentTilemap floor overlays so magnifier selection resolves to the prop, not the reskinned floor.
- Any path that clears/rebuilds customTiles (including secret-door accent refresh and Streaming rebuild) must restore the sparse decoration layer before GameScene refreshes custom overlays.
- Environment props do not justify a generator-version bump while they remain deterministic, non-collision visual overlays.


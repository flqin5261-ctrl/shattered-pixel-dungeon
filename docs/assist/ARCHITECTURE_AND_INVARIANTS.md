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
WORLD_GEN_VERSION = 8
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

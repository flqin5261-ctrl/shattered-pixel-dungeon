# Assist MOD Bug 历史、失败方案与修复记录

本文件专门记录“出过什么严重问题、为什么、哪些修法是错的、最后怎么稳定下来”。

新的维护者不要只看最终代码。很多看起来“可以简化”的逻辑，实际上是为了避开已经踩过的坑。

---

# 1. Assist 升级功能：只改 level 数值不等于真正升级

## 症状

用户点击自定义升级后：

- 数字可能变
- 但装备内部升级副作用没有执行
- Mage Staff 等特殊装备状态不一致

## 错误思路

直接：

```java
item.level(after)
```

## 正确修复

普通装备：

```java
item.upgrade(amount)
```

Mage Staff：

```java
updateWand(false)
```

## 教训

优先调用原版行为入口，不要直接篡改结果字段。

---

# 2. Assist 移速：逻辑速度变快但动画不变

## 症状

- 角色逻辑移动速度提升
- sprite tween 仍按原时间
- 视觉滑动/脱节

## 修复

Hero 的 `CharSprite.move()` duration 和 run/sprint animation 同时使用 Assist speed multiplier。

## 教训

影响时间尺度的 Assist 选项必须同时考虑：

- Actor 时间
- Sprite tween
- Animation duration

---

# 3. 镜庭入口楼梯导致离开单层模式

## 症状

镜庭本应只有一层，但出生入口仍可触发 transition。

## 原因

REGULAR_ENTRANCE 在原版语义里不是纯装饰。

## 修复

Extra Challenge 下入口只做 spawn anchor，activateTransition 返回 false。

---

# 4. Infinite World 早期“地图不是无限，只是在局部反复刷新”

## 症状

窗口能切，但没有真正持久绝对位置。

回头时：

- 世界坐标含糊
- 地形/探索不稳定

## 根因

把 local 168×168 坐标当世界坐标。

## 修复

加入：

- heroWorldX/Y
- centerChunkX/Y
- world/local 转换

---

# 5. Restart 后还是相似/同一世界

## 症状

用户点击重新开始，但地图 seed 没真正刷新。

## 原因

只重建了 Level/State，没有完整清理隐藏 Infinite World slot 或 seed 生命周期不对。

## 修复历史

陆续加入：

- complete hidden-slot deletion
- fresh seed initializer
- new seed before Hero Select
- hard-reset Infinite World data

## 教训

“重新开始”必须清：

- save slot
- world state
- seed
- explored
- overrides
- object state

---

# 6. Streaming 后长距离点击目标错乱

## 症状

玩家点击远处连续走。

中途发生 window shift 后：

- 角色走错位置
- 卡住
- action 指向不可达 cell

## 根因

`HeroAction.dst` 是 local cell。

window shift 后 local cell 的世界含义变了。

## 修复

shift 前：

```
local dst -> absolute world x/y
```

shift 后：

```
absolute world -> new local dst
```

若目标已不在 window：

- action = null

---

# 7. 最严重的移动 Bug：角色卡在原地走，转圈不结束

## 用户实机表现

- 往下/某方向走一段
- 角色突然不再推进
- 角色仍播放走路动作
- 左上角圆圈一直转
- 如果刚跨环境边界，画面可能在两套环境间闪

## 根因

window shift 发生得太早。

Hero sprite tween 尚未结束时：

- map 重建
- sprite 被 place/rebase
- Actor 还在等待原 tween completion
- waiter 状态被破坏

## 最终修复

**Streaming 只能在 movement tween 完成后触发。**

关键流程：

```
CharSprite.onComplete
  -> Hero.onMotionComplete
  -> InfiniteWorldLevel.afterHeroMotionComplete
```

## 禁止回退

不要在：

- Hero.move()
- getCloser()
- 每个 Actor tick

里直接同步 shift map。

---

# 8. Streaming A/B/A/B 来回闪

## 症状

刚切到新区域，马上又切回旧区域，然后继续来回。

## 根因

旧 threshold + shift distance 无 hysteresis。

shift 完成后 Hero 直接落在反方向 threshold。

## 最终值

```java
SHIFT_LOW = 24
SHIFT_HIGH = 144
SHIFT_STEP = 3
```

## 教训

窗口 streaming 必须设计“切换后落点安全带”。

---

# 9. 0.3.5/0.3.6：新区域先出现巨大水面

## 用户实机表现

- 每次新世界开始像图书馆/某主题
- 往下走后背景突然全是红色/蓝色液体
- 再走一会地形才出现
- 之前以为液体是设计内容，后来发现其实是贴图没加载

随后另一版：

- 不再全是水
- 但走过区域变黑
- Hero 仍能在黑色上走

这是项目最关键的一次渲染事故。

---

# 10. 错误修复：把 Water Backdrop 关掉

## 当时思路

认为：

> 水层先画出来，把 Terrain 遮住。

于是尝试：

- `water.visible = false`
- DungeonTerrainTilemap 自己渲染 WATER

## 结果

- 巨大水问题减轻
- 但出现大片纯黑背景

## 为什么错

原版真正的 water rendering 是：

- animated full-level SkinnedBlock water 作为底
- Terrain tilemap 对真实 WATER cell 故意 skip

也就是说：

关闭 water backdrop 后，WATER/背景位置本来就可能什么都没有。

## 结论

**不要永久关闭 water backdrop。**

---

# 11. 真正原因：VBO 还没上传 GPU

## 调查

`Tilemap.map/updateMap()`：

只是改变 CPU-side map / vertices dirty state。

`Vertexbuffer.updateVertices()`：

也只是：

- 替换数据引用
- 标记 update range

真正执行：

- glBufferData
- glBufferSubData

的是：

`Vertexbuffer.updateGLData()`

正常走：

`NoosaScript.drawQuadSet()`

时才 upload。

## Infinite World 特殊时序

同一帧：

1. rebuild map
2. mark VBO dirty
3. rebase Hero/camera
4. next visual frame

如果 GPU buffer 尚未上传：

- 可能画旧 Tilemap
- 可能画空 Tilemap
- 底下只剩 water backdrop
- 或 water backdrop 被错误关掉后只剩黑

---

# 12. 正确修复：同步 flush 真正上传

新增/调整：

`Tilemap.flushMapUpdate()`

必须执行：

```java
buffer.updateGLData();
```

不是只调用：

```java
buffer.updateVertices(...)
```

Infinite World refresh 时：

- tiles
- occlusion
- visualGrid
- terrainFeatures
- raisedTerrain
- walls
- wallBlocking

在 camera/Hero rebase 前同步 flush。

---

# 13. 0.3.6 大 Tilemap Index Limit

## 背景

168×168 map 的 quad 数量远大于普通 SPD Level。

一次性 vertex/index buffer 可能超过 unsigned short index 范围。

## 症状风险

- 某些设备缺图
- 几何错乱
- 黑块
- GPU draw 错

## 修复

- 支持 VBO vertex offset
- 将大型 quad set 分 batch 绘制

## 教训

如果未来把 WINDOW_CHUNKS 从 7 改 9/11：

必须重新审视 GPU index/vertex 限制。

---

# 14. 黑雾/探索状态在缩小地图时偶尔覆盖已探索区域

## 用户反馈

长距离探索后把镜头缩到很小：

- 已走过的地方偶尔出现一点黑 Fog

## 修复路线

0.3.7：

- compact chunk exploration bitsets
- incremental visited sync
- mapped immediate sync
- synchronous fog flush
- zoom clamp

最终用户接受再放宽到：

- 最低 1.8x

## 为什么不允许无限缩小

活动 window 只有 168×168。

无限世界并没有在 GPU 上一次绘制所有走过区域。

缩得太远：

- 更容易看到 window 之外
- 更容易暴露边界 refresh 时序

所以 zoom clamp 本身是架构保护。

---

# 15. 1.8x Zoom 松手又弹回 2x

## 症状

代码把 minimumZoom 改成 1.8，但 pinch 手势结束后最终变 2。

## 根因

原 UI 逻辑：

```
Math.round(camera.zoom)
```

直接整数取整。

## 修复

Infinite World 单独：

```
Math.round(value * 10f) / 10f
```

保留 0.1 步长。

---

# 16. Magic Mapping / Foresight / Clairvoyance 导致无限地图逻辑风险

## 原版假设

“当前 Level 是一个有限完整楼层”。

## Infinite World

只有当前 7×7 window。

如果直接使用整层探图：

- 把大量还没真正探索的 cells mapped
- window shift 后这些 local cells 指向新的 world coords
- 容易污染永久探索数据
- 还可能重新诱发 Fog bug

## 处理

最终在 Infinite World 禁用：

- Magic Mapping
- Foresight Scroll
- Clairvoyance Stone

神器保留。

---

# 17. Warping Trap 清探索状态

## 原版行为

Warping 类机制可能清 visited/mapped。

## Infinite World 问题

用户跑了很远积累的永久探索档案不应被一次 trap 全抹掉。

## 修复

Infinite World 下跳过该 clear 行为。

---

# 18. Bomb 炸墙后墙发现/渲染状态没更新完整

## 问题

仅：

`Level.set(cell, EMPTY)`

不足以保证所有 wall discovery/stitch 状态正确。

## 修复

Infinite World 爆破后：

- cleanWalls
- observe
- GameScene map refresh

并通过 terrainOverrides 保存结果。

---

# 19. Teleport 导致 absolute coordinate 脱节

## 风险

原版很多 teleport 最终只：

```
hero.pos = newLocalCell
```

但 Infinite World 还需要：

- heroWorldX
- heroWorldY

## 修复

InfiniteWorldLevel 提供 record/sync。

ScrollOfTeleportation 和 Chains 已补。

## 后续检查清单

任何新增：

- Dash
- Charge
- Portal
- Boss teleport
- scripted reposition

都要查这个问题。

---

# 20. 0.4.0 Plant 编译错误

## 情况

首次 0.4.0 GitHub Actions 构建失败。

Plant 生成代码在匿名 ChunkVisitor 内传错了 `this`。

`this` 指向：

- ChunkVisitor

而不是：

- InfiniteWorldLevel

## 修复

使用：

```java
InfiniteWorldLevel.this
```

作为 Seed.couch 的 Level 参数。

## 教训

Java 匿名内部类中 `this` 不是外部类。

---

# 21. Key 原版 depth 限制

## 用户反馈

希望“一把银钥匙能开无限世界里所有锁门”。

## 原版

Key.isSimilar 检查：

- key type
- depth

Hero 还有 branch/subfloor 限制。

## 修复

Infinite World 下：

- same Key subclass 即 similar
- 忽略 depth
- branch 条件允许 Infinite World

## 风险

未来如果加入新的特殊 key，不要全部继承这种全局通行语义。

---

# 22. 0.4.1：主题房间全是锁门的体感

## 用户反馈

“门有了，但都是带锁的，或者没带锁的我没碰到。”

## 根因

V7 对大多数非例外房间：

约 58% LOCKED_DOOR。

即使数学上存在普通门，短期样本体感仍非常偏锁门。

## V8 修复

每个 Chunk：

- room index 0 强制普通门
- 其余混：
  - unlocked
  - locked
  - secret

Key Room 强制普通门。

Crystal Vault 不再 Iron-lock。

---

# 23. 0.4.1：锁门已经解锁，但旧锁门贴图仍留着

## 用户反馈

使用钥匙后：

- 可以通过
- 逻辑正常
- 视觉仍然是锁门

## 根因

V6 以后加入的 `InfiniteWorldAccentTilemap` 是静态 visual overlay。

创建 overlay 时：

`LOCKED_DOOR`

对应视觉被写进 overlay mesh。

之后 Level map 更新为普通 DOOR，但：

- base DungeonTerrainTilemap 更新
- static custom overlay 没更新

最终旧图压在新门上面。

## V8 修复

静态 mixed-theme overlay 不再绘制任何会动态变化的门：

- DOOR
- OPEN_DOOR
- LOCKED_DOOR
- HERO_LKD_DR
- CRYSTAL_DOOR
- SECRET_DOOR
- LOCKED_EXIT
- UNLOCKED_EXIT

同时避免门 overhang 静态化。

## 通用教训

凡是 runtime 会频繁变 Terrain 的格子，不适合放在“只在 streaming 时创建”的 static custom overlay 里。

---

# 24. Secret Room 可能被异色墙提前暴露

## 问题

如果：

- 外面是 Caves
- Secret Room 自己是 Prison

即使 door 是 SECRET_DOOR，房间外圈墙材质不同，也可能让玩家一眼看出密室。

## 修复

房间仍隐藏时：

- 跳过 room-specific environment overlay

只有 SECRET_DOOR 不再存在后，下一次 overlay rebuild 才允许专属主题。

## 当前已知小缺点

刚 discover 的同一帧：

- 房间独立主题不一定立即出现

可能等下次 window refresh。

未来可做局部 overlay refresh。

---

# 25. Static Room Overlay 与炸墙的潜在同类问题

0.4.2 已解决动态 Door。

但如果以后玩家在带 room-specific custom wall overlay 的房间：

- 炸掉墙
- 烧掉某些被 overlay 绘制的结构

逻辑 Terrain 已变化，但 static overlay 可能仍存在直到 rebuild。

这是当前已知技术债。

## 推荐修复

给 InfiniteWorldAccentTilemap 增加：

- dirty room
- dirty chunk
- refreshRect

GameScene.updateMap(cell) 时，如果 Infinite World：

1. 找 cell 属于哪些 accent overlay
2. 只重建相关 overlay rect

不要每个地形类型继续硬编码 exclude。

---

# 26. Random Objects 占掉门口/钥匙刷新位置

## 风险

V6 随机 loose loot / plants / traps 可能先占主题房间 door/outside cell。

导致：

- 理论上生成了 key
- 实际无法放 Heap
- 用户看不到 key

## V8 修复

`v8LocalCellReservedForRoomAccess`

排除：

- door cell
- outside access cell

旧随机系统不会再占这些格。

---

# 27. Generator 改版导致旧世界变形

这是潜在最严重的数据问题之一。

## 禁止做法

发布 V9 后：

加载 V8 save 时直接使用 V9 公式。

## 为什么

terrainOverrides 只保存“相对基础地形的变化”。

如果基础地图换了：

- 一个旧的 EMPTY override 可能落到新宝库墙
- 箱子坐标和房间错位
- Hero 可能加载在墙内

## 正确方式

每次大改：

- WORLD_GEN_VERSION++
- save 保留原 generatorVersion
- generator 代码兼容旧版本

旧世界继续旧布局。

用户想体验新世界时：

- 点“重新开始”

---

# 28. GitHub Actions 构建失败时的处理原则

用户不想收到：

> “构建还没好，你等下再问我。”

项目约定：

1. Assistant 自己检查 workflow。
2. 如果失败：
   - 读 job logs
   - 修代码
   - commit
   - 等新 run
3. 成功：
   - fetch artifact
   - download ZIP
   - 解压 APK
   - 校验
   - 直接发 APK

不要把“再来问一次”作为正常流程。

---

# 29. APK 点击后“直接打开游戏”问题

用户曾反馈点击聊天中的 APK 附件不像安装更新，而是直接打开游戏。

处理策略：

- versionCode 必须递增
- 同时提供：
  - APK
  - ZIP
- ZIP 可让用户：
  - 保存
  - 文件管理器解压
  - 手动点击 APK

不能通过改 applicationId 解决，否则破坏覆盖升级。

---

# 30. 未来 Bug 记录格式

以后遇到严重问题，在本文件追加：

## [版本] 问题标题

### 用户表现

实际手机看到了什么。

### 复现条件

如何触发。

### 根因

真实代码原因。

### 错误尝试

哪些方案试过但不对。

### 最终修复

改哪些类/方法。

### 回归测试

以后每版必须测什么。

### 不可回退原则

以后维护者不能“优化”掉什么。


# 31. 0.4.2 后：局部地图看起来可能“有限”

## 用户表现

用户曾在某个较早世界中遇到：

- 一条路走到头；
- 换另一条支路也走到头；
- 最终所有可见分支都被墙封死；
- 因此怀疑 Infinite World 实际上只是一个有限随机迷宫。

0.4.2 最新测试没有再完整复现“所有路都死”的情况，但普通死胡同依然存在。

用户明确要求：

> 不需要每一条岔路都有出口，但必须从整体结构上保证这个世界真的可以无限走下去。

## 原因

V2～V8 的理论无限性建立在：

- 每个 Chunk 有 shared-edge gateway；
- gateway 与内部 hub 相连。

但后续 generator 层不断叠加：

- landmark
- terrain props
- themed room shell
- room corridors
- wall replacement

即使 shared-edge 本身存在，后生成结构仍可能重写内部连接。

而“每个 Chunk 都是确定性生成”只意味着坐标无限，不自动证明出生点所在 passable component 无限。

## 不采用的方案

### 强制所有 Chunk 四边都永远连到中心

这样当然容易证明连通，但会导致：

- 地图过度规则；
- 所有岔路都没有真正终点；
- 房间/异常区域很难形成压迫感；
- 重新回到早期“格子生成器”的观感。

## V9 最终方案

加入稀疏 world-scale Infinite Backbone：

- 每第 6 个 Chunk 列有南北贯通双格通路；
- 每第 6 个 Chunk 行有东西贯通双格通路；
- 原点位于交叉主干；
- Backbone 在普通 generator 所有墙/房间/装饰之后最后 carve。

因此：

- 普通支路依然允许 dead end；
- 玩家可能需要回头寻找主路；
- 但出生点所属主连通分量在数学结构上有无限东西/南北延伸。

## 回归测试

- 新 V9 世界向四方向长跑；
- 故意进入多个 dead-end 支路再返回；
- 连续跨越多个 6-Chunk spine intersection；
- 测试主题房和 anomaly 区是否覆盖 spine；
- save/load 后 spine 不应凭空变墙。

## 不可回退原则

以后新增房间/Boss Arena/大型 landmark：

不得在 backbone carve 之后永久覆盖其通路，除非另有等价的全局无限性证明。

---

# 32. 0.4.2：内容越加越多后经济密度失控

## 用户表现

用户反馈：

- 新开世界没跑多远就能拿到大量物品；
- 卷轴、药剂、装备、食物等很快大量收集；
- 探索后续区域缺少“还能发现什么”的动力；
- 隐藏陷阱也明显太多。

## 根因

不是单一房间奖励设置错误，而是历史内容层叠加：

1. 旧基础 chest
2. V5 container
3. V5 ambient prop/trap
4. V6 loose loot
5. V6 ambient keys
6. V6 wild plants
7. V7 themed room loot
8. V8 secret-room bonus

每一层单独看都合理，但同一个 Chunk 可以同时命中多层。

这属于典型“增量功能叠加导致总经济失衡”。

## V9 修复

不是删除内容类型，而是为 V9 给旧层增加稀疏 gate：

- chest 42% -> 12%
- V5 container -> 约 28% Chunk 1 个
- loose loot -> 约 30% Chunk 1 件
- ambient IronKey -> 约 8%
- ambient CrystalKey -> 约 5%
- wild Plant -> 约 45% Chunk 1 株
- ambient secret trap 权重大幅降低
- trap workshop trap chance 22% -> 10%
- themed room reward count 全面下降

同时保留：

- 锁门旁 guaranteed IronKey
- Crystal Vault guaranteed CrystalKey

所以不会因为降密导致 soft-lock。

## 回归测试

比较新 V9 世界前 10～20 Chunk：

- 总 loose loot 数
- 武器/装备数
- 卷轴药剂数
- key 数
- trap 数

目标不是“贫瘠”，而是：

> 场景很多、互动很多，但真正拿进背包的资源不再每几步一件。

---

# 33. 0.4.2：Secret Room 存在但实机很难碰到

## 用户表现

用户明确说：

> “隐藏房间我没找到。”

## 根因

V8 Secret Door 只是每个普通 themed room 的概率分支。

随机世界中：

- 某段路线可能完全没抽到；
- 抽到了也可能不在玩家实际经过的墙边；
- 原版被动 Secret Door 发现概率较低。

因此功能“代码上存在”，但不是可靠可测试内容。

## V9 修复

两层措施：

1. 每个 3×3 Chunk macro 确定一个强制 Secret Room 候选。
2. Infinite World 中近距离被动搜索 SECRET_DOOR chance 提升到 55%。

主动 Search 保留原版机制。

SECRET_TRAP 没有使用这个 55%，防止进一步增强陷阱干扰。

## 回归测试

在全新 V9：

- 不主动 Search，只贴墙走过多个 3×3 macro；
- 应在合理时间内发现至少一间 Secret Room；
- 主动 Search 可更稳定确认；
- 远处不能看到 Secret Room 独立 tileset；
- discover 后 reload 不得重新隐藏。

---

# 34. V9 Pool Hall 必须防止历史 Water Render 回归

V9 新增大型 Pool Hall anomaly。

它会产生远大于普通房间的 WATER 面积。

历史 0.3.6 曾经出现：

- 巨大假水背景
- 黑地
- VBO 延迟上传

V9 Pool Hall **没有**新建一套局部 Water renderer。

仍然：

- Terrain.WATER 使用原版 normal animated water backdrop；
- custom room/anomaly tileset 只负责兼容 floor/wall；
- streaming 继续使用 synchronous Tilemap VBO upload。

回归时如果看到：

- 整片水先出现、地形后 pop-in
- 黑色可走区域

不要先怀疑 Pool Hall 地形公式。

优先重新检查：

- Tilemap.flushMapUpdate
- Vertexbuffer.updateGLData
- water backdrop visibility
- large Tilemap batching


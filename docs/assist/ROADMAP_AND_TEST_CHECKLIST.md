# Assist Infinite World Roadmap 与回归测试清单

本文档回答两个问题：

1. **当前还要做什么？**
2. **每次修改后必须测试什么，避免把以前修好的东西弄坏？**

---

# A. 当前项目状态

当前最新稳定：

- 版本：0.5.0
- versionCode：946
- Generator：V10
- stable：`assist-0.5.0-stable`
- dev：`assist-0.5.0-mobs`

当前核心已经具备：

- 水平无限 Chunk streaming
- 绝对世界坐标
- Seed 确定性生成
- 7×7 active window
- 探索状态持久化
- terrain override
- 箱子/物品/植物状态持久化
- 多环境 tileset
- 房间级环境 tileset
- 普通门/锁门/隐藏门
- 植物房
- 金币园
- 食物房
- 卷轴房
- 药剂房
- 钥匙房
- 武器库
- 水晶宝库
- 陷阱工坊
- 综合藏宝室
- Bomb / Pickaxe / Chains / Teleport 等互动

当前已进入：
- 普通怪物生态
- 稀有精英怪

尚未正式进入：
- Boss
- 世界事件/NPC

---

# B. 0.4.2 当前实机优先验证

下一次用户测试反馈回来时，优先判断以下内容。

## B1. 动态门贴图

必须确认：

- [ ] LOCKED_DOOR 用 IronKey 解锁后，旧锁门贴图立即消失
- [ ] 新门贴图正确
- [ ] 普通门可正常打开
- [ ] OPEN_DOOR 再关门正常
- [ ] 不出现两个门叠在一起
- [ ] 房间级异材质不覆盖动态门

如果仍旧有残影：

优先查：

- InfiniteWorldAccentTilemap
- customWalls
- customTiles
- DungeonWallsTilemap
- GameScene.updateMap(cell)

不要先去改 Key 逻辑，因为当前已确认逻辑解锁本身可用。

---

## B2. 普通门比例

目标：

玩家短距离走动就应该看到：

- 普通门房
- 锁门房
- 少量秘密房

不能再出现“几十个门全是锁门”的体感。

如果仍偏锁：

- 降低 V8 doorRoll 中 LOCKED 门区间
- 保留每 Chunk index 0 强制普通门

---

## B3. Secret Door

检查：

- [ ] 未发现时像墙
- [ ] 从远处看不出房间
- [ ] 靠近有机会自动发现
- [ ] 主动搜索能发现
- [ ] 发现后变普通门
- [ ] 可以进入
- [ ] 奖励存在
- [ ] 离开再回来仍保持已发现
- [ ] 不会重新变隐藏墙

如果 Secret Door 重载后重新隐藏：

查：

- terrainOverrides
- snapshotForSave
- baseWindow
- Terrain.discover()

---

## B4. Secret Room 主题

已知可能存在：

> Secret Door 刚发现时，房间仍暂时是周围环境主题；下次 streaming 后才出现房间独立主题。

这是非阻塞问题。

如果用户在意，下一版修成：

- discover 时局部重建 room accent

---

## B5. Gold Garden

检查：

- [ ] 房间不是太大
- [ ] 多堆金币
- [ ] 拿走后不重刷
- [ ] streaming 后已拿金币不恢复

---

## B6. Plant Garden

检查：

- [ ] 植物数量明显高
- [ ] 真实可触发
- [ ] 触发后状态正确
- [ ] 走远再回来不会复活全部植物

---

# B7. 0.4.3 V9 专项测试

0.4.3 新增了世界级 Infinite Backbone、5×5 anomaly macro、异境便笺和大幅资源降密。

必须在“重新开始”的 V9 世界执行。

## B7.1 真无限性

- [ ] 从出生区域沿任意正常路线探索。
- [ ] 刻意走入多个 dead end。
- [ ] 能回到主干继续前进。
- [ ] 向东跑至少 12+ Chunk。
- [ ] 向西跑至少 12+ Chunk。
- [ ] 向南跑至少 12+ Chunk。
- [ ] 向北跑至少 12+ Chunk。
- [ ] 跨过多个 cx/6 或 cy/6 backbone 交叉点。
- [ ] 不出现整张世界所有可达分支同时永久封死。

注意：

允许单独支路死路；测试目标不是“每条路有出口”，而是“整体主连通分量无限”。

## B7.2 Secret Room

- [ ] 在多个 3×3 Chunk macro 内贴墙探索。
- [ ] 被动 Search 能在合理距离发现 Secret Door。
- [ ] 主动 Search 能更稳定发现。
- [ ] Secret Trap 数量明显少于 0.4.2。
- [ ] Secret Door 在远处仍不可见。
- [ ] 发现后不重新隐藏。
- [ ] 房内奖励约 2～3 件额外高价值物，而不是满地物资。

## B7.3 资源曲线

前 10～20 Chunk 统计体感：

- [ ] 普通野外 loose loot 不再每 Chunk 都有。
- [ ] 普通箱子明显变少。
- [ ] Scroll Room 约 1～2 张。
- [ ] Potion Room 约 1～2 瓶。
- [ ] Armory 约 1～2 件装备。
- [ ] Crystal Vault 约 1 个水晶箱。
- [ ] 锁门仍总能找到对应 IronKey。
- [ ] 水晶箱仍能找到 CrystalKey。
- [ ] 不应该“刚开图没走多远就基本把装备/卷轴/药剂收齐”。

## B7.4 Liminal Macro

寻找远离出生区域的 anomaly：

- [ ] Repeating Offices 至少连续跨多个 Chunk 保持同一重复空间气质。
- [ ] Pool Halls 水面和干路正常。
- [ ] Endless Hall 足够空旷，不被普通 loot/主题房塞满。
- [ ] anomaly 与普通 Chunk 接壤处可通。
- [ ] anomaly 内 Infinite Backbone 可通。
- [ ] streaming 多次后无黑图。
- [ ] Pool Halls 无“整片假水先加载”回归。

## B7.5 Field Notes / Guide

- [ ] 5×5 anomaly 中只出现一张对应便笺。
- [ ] 拾取便笺后打开 Guide。
- [ ] Guide 内出现独立“无界异境记录”栏目。
- [ ] 对应条目从 missing 变为可读。
- [ ] save/load 后条目保留。
- [ ] 回到原 anomaly 后便笺不重刷。

---

# C. 下一版地图内容扩充建议

用户当前方向非常明确：

> 地图要像原版那样有各种大小和功能房，不要只是开拓地带。

建议下一版先继续“房间库”，再上怪。

---

# C1. 原版 Magic Well 风格房

参考：

`levels/rooms/special/MagicWellRoom.java`

Infinite World 版本建议：

- 5×5 / 6×6
- 中央 WELL
- 普通门或隐藏门
- 少量草/水
- 不需要锁

持久化：

- WELL 使用后变 EMPTY_WELL
- terrainOverrides 可以保存

---

# C2. Runestone 房

参考：

- RunestoneRoom
- SecretRunestoneRoom

内容：

- Runestone/Stone 奖励
- 环形/十字布局
- 可隐藏

---

# C3. Library 房

参考：

- LibraryRoom
- LibraryHallRoom
- SecretLibraryRoom
- SegmentedLibraryRoom

内容：

- BOOKSHELF
- 卷轴
- 少量特殊物资
- 可烧毁
- 可以混 Prison/City tileset

注意：

BOOKSHELF 可破坏后，static room overlay 可能需要局部 refresh。

---

# C4. Pool 房

参考：

- PoolRoom
- AquariumRoom
- WaterBridgeRoom

内容：

- 小水池
- 桥
- 岛
- 宝物

注意：

局部 water tileset 混合必须谨慎。

不要重现 0.3.6 water backdrop Bug。

第一阶段可以：

- Terrain.WATER 仍走全局 water texture
- 房间墙/地板换主题
- 不做局部 waterTex 替换

---

# C5. Fire / Barricade 房

参考：

- MagicalFireRoom
- BurnedRoom
- StorageRoom

内容：

- BARRICADE
- EMBERS
- PotionOfLiquidFlame
- Bomb

交互：

- 烧
- 炸

适合测试动态 Terrain + room overlay。

---

# C6. Trap Corridor / Trap Room

参考：

- TrapsRoom
- MinefieldRoom
- SecretHoardRoom

布局：

- 长条窄房
- 4～10 个 Trap
- 中央奖励
- 普通/隐藏入口

---

# C7. 小矿洞

内容：

- WALL_DECO
- MINE_CRYSTAL
- MINE_BOULDER
- Pickaxe
- 金币
- Bomb

不要让矿洞堵住 Chunk 主连通树。

---

# C8. Statue / Ritual 房

参考：

- StatueRoom
- StatuesRoom
- RitualRoom
- SacrificeRoom

第一阶段可以只做：

- STATUE
- PEDESTAL
- EMBERS
- items

后面怪物系统上线再给 ritual 加敌人。

---

# C9. Chest Maze

参考 SecretMazeRoom 思路。

小规模：

- 7×7 左右
- 内部 1-cell maze
- 1～3 Chest
- 可秘密入口

不要做超过 Chunk 尺寸的大迷宫。

---

# C10. Broken Cell Block

参考：

- CellBlockRoom
- Prison tileset

布局：

- 连续小隔间
- 每间一个门
- 有些门普通
- 有些空
- 有些箱子
- 未来可放怪

这个非常适合解决用户“为什么全是开拓地带”的问题。

---

# D. 环境视觉下一阶段

## D1. Room overlay 动态刷新

优先级高。

当前：

- room accent 是 static custom tilemap
- door 已绕开

未来实现：

`refreshInfiniteWorldAccentAt(cell)`

建议：

1. 根据 world/local cell 找 chunk。
2. 算该 chunk themed rooms。
3. 判断 cell 是否在 room rect。
4. 删除并重建相关 room visual。
5. 只 flush 相关 group。

触发：

- Secret door discover
- Wall destroyed
- Bookshelf burned
- Barricade destroyed
- Crystal mined
- door state change（虽然门已排除，也可统一）

---

## D2. Room-specific decoration

当前主要是 tileset 替换。

以后可以增加：

- custom floor decal
- skulls
- pipe
- cracks
- books
- rubble
- torches
- region deco

优先复用原版素材。

---

## D3. 多环境边界

Chunk/room theme 相遇时：

- 不必追求现实合理
- 但不能出现巨大片图层空洞
- 不允许 black quad
- 不允许错误 water background

可以怪诞，但渲染要稳定。

---

# E. 怪物系统：正式接入前设计

用户已经明确：

> 地图差不多以后，就开始怪物和 Boss。

建议版本阶段：

- 0.5.0：普通怪物原型
- 0.5.1：怪物 persistence
- 0.5.2：生态/区域怪
- 0.6.0：Boss 原型

版本号只是建议，可按实际调整。

---

# E1. 普通怪物 V1

第一步不要一次加全怪物表。

建议只选几只简单原版怪：

- Rat
- Gnoll
- Crab
- Skeleton
- Thief
- Bat

目标测试：

- Actor tick
- combat
- drop
- streaming

不要先加：

- summon-heavy
- teleport-heavy
- floor-transition-dependent
- quest mobs

---

# E2. Mob World State

必须新增类似：

`InfiniteWorldMobState`

至少保存：

- deterministic spawn id
- mob class
- worldX
- worldY
- HP
- dead
- alignment/特殊状态
- loot state（必要时）

建议 key：

```
(worldChunkX, worldChunkY, spawnIndex)
```

不要用 local cell 当永久 ID。

---

# E3. Spawn 规则

每 Chunk deterministic：

```
spawnCount = hash(seed, cx, cy)
spawnClass = hash(...)
spawnPos = hash(...)
```

第一次进入生成。

之后：

- 死了 → dead state
- 不再 reload 刷新

未来想要 respawn，单独实现世界时间系统。

---

# E4. Mob Active Radius

49 个 Chunk 全部让怪物正常 AI tick，未来很可能浪费性能。

建议：

- Hero 周围 1～2 Chunk full AI
- 更远 freeze
- 即将 unload snapshot

或者：

- 只把实际 active simulation mobs 注册 Actor
- 远处只保 state

---

# E5. Mob Streaming

Window shift：

旧区域 mob：

1. world coords snapshot
2. remove sprite
3. remove Actor
4. state 保存

新区域：

1. 查 deterministic spawn
2. 查 persistent state
3. dead → 不建
4. alive → 创建 Mob
5. world pos 转 local pos
6. add sprite/Actor

注意避免：

- 同一 mob 重复 add
- Actor 列表残留
- sprite.wait 卡死

---

# E6. Mob Pathfinding 跨 Window

最保守第一版：

- Mob 只能在当前 active window 内追
- 走到 streaming outer band 附近 freeze

不建议一开始就让 NPC 跨无限世界寻路。

后面可以做：

- world chunk path
- local PathFinder 分段

---

# E7. 怪物与 Theme

后续可以按 biome/room：

Sewers：
- Rat
- Crab
- Slime

Prison：
- Skeleton
- Thief
- Guard 类

Caves：
- Bat
- Gnoll
- Shaman

City：
- Monk
- Warlock

Halls：
- Demon
- Succubus 等

混合地图可以允许跨生态混刷。

用户并不要求合理，可以偏怪诞。

---

# F. Boss 计划

Boss 必须晚于普通 mob persistence。

---

# F1. Boss Landmark

生成器低概率产生：

```
BossArenaChunk
```

World key：

```
boss:<cx>:<cy>:<bossType>
```

state：

- discovered
- spawned
- alive
- defeated
- rewardClaimed

---

# F2. Boss 不重复

任何 boss 的 deterministic landmark：

若 defeated：

- reload 不再生成 boss

若 alive：

- reload 恢复同一个 boss state

不能因为 window shift 复制。

---

# F3. Boss Arena

可以参考原版 Boss Room painter。

但 Infinite World：

- 没有 floor lock transition
- arena 应局部封锁

设计：

1. 进入 arena
2. doors temporarily locked
3. boss active
4. win
5. doors unlock
6. reward spawn

所有状态写 world state。

---

# F4. Boss 远离

第一版建议：

Boss 激活后：

- 如果 Hero 试图离开 arena，门封锁

这样避免 Boss 跨 streaming。

后续再做真正 open-world boss chase。

---

# G. 每个新版本必须执行的回归测试

---

# G1. 安装/升级

- [ ] applicationId 未变
- [ ] 固定签名未变
- [ ] versionCode > 上一版
- [ ] 可以覆盖安装上一版
- [ ] 原 App 存档仍可读取
- [ ] App 名仍是 Assist

---

# G2. 普通模式

虽然主要开发 Infinite World，也要快速测：

- [ ] 原版正常开局
- [ ] 原版移动
- [ ] 原版门
- [ ] 原版物品
- [ ] 原版存档

防止全局类修改误伤普通模式。

尤其：

- Key.java
- Hero.java
- GameScene
- Tilemap
- Vertexbuffer

是全局代码。

---

# G3. Assist 功能

- [ ] Assist gear 还在
- [ ] 无敌
- [ ] 加金币
- [ ] 加物品
- [ ] 装备升级
- [ ] Mage Staff
- [ ] 速度倍率
- [ ] 楼层传送普通模式可用
- [ ] Infinite World 禁用楼层传送

---

# G4. 镜庭

- [ ] 可进入
- [ ] 独立存档
- [ ] 不生成怪
- [ ] 入口不能离开
- [ ] Assist floor teleport 被禁

---

# G5. Infinite World 新世界

每次 generatorVersion 改：

必须“重新开始”测试。

- [ ] seed 新
- [ ] 出生正常
- [ ] 中央区域可走
- [ ] 没出生墙内
- [ ] shared edge 可通

---

# G6. Streaming 压力测试

至少：

1. 一直向下跑 10+ 个 shift
2. 一直向右跑
3. 对角跑
4. 回头
5. 在 shift 临界线来回

检查：

- [ ] 不原地走路
- [ ] loading circle 不死
- [ ] 不 A/B 闪
- [ ] Hero 不跳位置
- [ ] camera 不突然飞
- [ ] path 不错位

---

# G7. Render

- [ ] 无巨大全屏水
- [ ] 无大黑块
- [ ] 无等待后才 pop in
- [ ] wall 正常
- [ ] water 动画正常
- [ ] 同 world cell tile variant 稳定

---

# G8. Fog

- [ ] 走过区域保持 explored
- [ ] 走远回来不重黑
- [ ] 1.8x 缩放不露大片错误黑雾
- [ ] mapped/visited 稳定
- [ ] Warping Trap 不抹永久记录

---

# G9. Terrain Persistence

修改：

- [ ] 炸一面墙
- [ ] 挖矿
- [ ] 开门
- [ ] 烧书架/路障

走远 streaming 后回来：

- [ ] 墙仍没了
- [ ] 矿仍挖了
- [ ] 门状态正确
- [ ] 破坏物不复原

---

# G10. Heap/Object Persistence

拿：

- [ ] 普通 loot
- [ ] 箱子
- [ ] Tomb
- [ ] Crystal Chest
- [ ] Food
- [ ] Gold Garden
- [ ] Weapon
- [ ] Scroll
- [ ] Potion

回来：

- [ ] 不重复刷新

---

# G11. Plants/Traps

- [ ] 踩植物
- [ ] 踩 trap
- [ ] reveal secret trap

回来：

- [ ] 不复活
- [ ] 不恢复隐藏
- [ ] 不变成无效 sprite

---

# G12. Doors

- [ ] 普通门
- [ ] 普通门开关
- [ ] LOCKED_DOOR
- [ ] IronKey
- [ ] Secret Door
- [ ] Crystal Chest + CrystalKey

重点：

- [ ] 动态贴图不残留
- [ ] 门口不被 random prop 堵

---

# G13. Teleport

测试：

- [ ] Teleport Scroll
- [ ] Fadeleaf
- [ ] Teleport Trap
- [ ] Blink
- [ ] Ethereal Chains

检查：

- [ ] 不出 window
- [ ] world coords 同步
- [ ] 下一次 streaming 不瞬移回旧位置
- [ ] 不黑屏
- [ ] 不卡 Actor

---

# G14. Save/Load

在：

- 刚 shift 后
- 开箱后
- 炸墙后
- Secret Door discover 后
- Teleport 后

保存退出。

重开：

- [ ] Hero world pos 正确
- [ ] 当前 window 正确
- [ ] 修改保留
- [ ] exploration 保留

---

# H. 发版流程

每个版本最后：

1. 开 dev branch。
2. 修改。
3. versionCode++。
4. versionName 更新。
5. workflow 加 branch。
6. Push。
7. 等 GitHub Actions。
8. 如果失败：
   - 自己读 logs
   - 修
   - 重跑
9. 成功：
   - fetch artifact
   - 下载 ZIP
   - 解出 APK
   - 校验 ZIP
   - 校验 APK
   - SHA-256
10. 建 stable branch。
11. 更新：
   - ASSIST_HANDOFF.md
   - CHANGELOG_DETAILED.md
   - ARCHITECTURE_AND_INVARIANTS.md（若架构变）
   - BUG_HISTORY_AND_FIXES.md（若修严重 bug）
   - PROJECT_STATE.json
12. 给用户：
   - APK
   - ZIP 备用
   - 简短但完整更新说明

---

# I. 文档本身也是发版的一部分

如果只改代码不更新交接文档，未来换对话就会重新踩坑。

所以从 0.4.2 起：

> **文档必须视为项目状态的一部分。**

新 ChatGPT 接手时，先读文档，再改代码。


# J. 0.4.3 之后的优先级

当前 V9 已经开始从“不断加东西”转向“控制节奏和世界层次”。

下一阶段建议按以下顺序：

1. 先根据用户实机反馈调整：
   - backbone 是否太明显；
   - anomaly 出现频率；
   - loot 是否又过稀或仍过密；
   - Secret Room 实际发现率。
2. 再增加少量**低奖励、高环境辨识度**的房间，而不是继续堆高价值物资：
   - Magic Well
   - Runestone
   - Library
   - Fire/Barricade
   - Mine
   - Broken Cell Block
   - Ritual/Statue
3. 对每一种真正“异常空间”继续增加 Field Note 页面；Field Notes 系统已经完成，不需要重做 Journal 架构。
4. 完成 room/chunk accent overlay 局部刷新：
   - Secret Door discover 后立即更新房间主题；
   - 炸墙/烧书架后局部 overlay 不残影。
5. 地图与经济曲线稳定后，正式进入普通怪物 persistence。
6. 普通怪 streaming/persistence 稳定后才进入 Boss。

新增地图内容时必须遵守 V9 的经济原则：

> **可以增加大量视觉结构和交互，但不要让每个新房间都同时成为新的奖励来源。**



# B8. 0.4.4 V10 次级无限路线专项测试

必须重新开始创建 V10 世界。

- [ ] 找到 primary spine 后离开主干探索。
- [ ] 进入 secondary east-west route 后连续跨 20+ Chunk，不应自然终止。
- [ ] 进入 secondary north-south route 后连续跨 20+ Chunk，不应自然终止。
- [ ] secondary route 跨 Chunk seam 时入口不能错位或断开。
- [ ] secondary route 与 primary spine 周期性交汇。
- [ ] 横纵 secondary route 之间可以形成新的交叉网络。
- [ ] 普通局部支路仍然允许出现 dead end。
- [ ] anomaly 5×5 macro 内 V10 route 仍贯通。
- [ ] 不应因为额外路线重新出现“地图过于规则的方格网”体感。
- [ ] Streaming、Fog、Water、VBO 不因新增路线发生回归。

V10 的验收标准不是“所有路都无限”，而是：

> **从主干出去后，世界里存在大量第二层无限路线，玩家不必总是撞死路后折返唯一主干。**


# B9. 0.5.0 怪物生态专项回归

0.5.0 不要求重新开始地图；现有 Generator V10 世界可以直接测试。

## B9.1 刷怪节奏

- [ ] 进入世界后前 20 回合左右不应立刻被怪包围。
- [ ] 怪物不是连续每几步刷新。
- [ ] 通常同时只有约 1～3 只。
- [ ] 常规生态不会主动刷到超过 5 只。
- [ ] 即使运行时其他机制临时加怪，也应最终压回绝对上限 6 左右。
- [ ] Liminal anomaly 区明显比普通区域更安静。

## B9.2 生成位置

- [ ] 新刷怪不会直接出现在 Hero 视野内。
- [ ] 不贴脸出生。
- [ ] 不刷在墙/坑/秘密门 terrain。
- [ ] 不刷在箱子、Trap、Plant、已有角色上。
- [ ] 刷出的怪从其位置到 Hero 所在连通区域有可行路径。

## B9.3 距离 Despawn

把普通怪引出来后持续远离：

- [ ] 超过约 40 cell 后怪直接消失。
- [ ] 不播放正常死亡奖励逻辑。
- [ ] 不掉落怪物 Loot。
- [ ] 不获得 EXP。
- [ ] 不增加击杀统计。
- [ ] 不留下 Champion 环境副作用。
- [ ] 再回到原位置，该只怪不会作为永久实体在那里等待。

## B9.4 正常击杀

近距离正常杀死怪：

- [ ] 正常死亡动画。
- [ ] 正常 EXP。
- [ ] 正常掉落逻辑。
- [ ] 正常击杀统计。

必须区分“玩家击杀”和“远距离 despawn”。

## B9.5 Streaming

找到 1～3 只怪后，把 Hero 带到 Window shift 阈值：

- [ ] 仍在 40 cell 内的怪不会因为 shift 瞬间消失。
- [ ] 怪物视觉位置与地图一致。
- [ ] 怪不会突然跳到 72 cell 外的旧 local 坐标。
- [ ] 怪 AI 不沿着旧 window path 穿墙。
- [ ] Actor thread 不出现原地移动/等待卡死。
- [ ] shift 后仍然能够正常发现并追踪 Hero。

## B9.6 Teleport

使用随机传送/Fadeleaf/Teleport Trap/Chains 后：

- [ ] 过远旧怪最终被 prune。
- [ ] 新区域不会瞬间刷满怪。
- [ ] 不出现旧怪跨巨大距离瞬移追来。

## B9.7 Save/Load

附近有普通怪和精英怪时保存退出：

- [ ] 重新进入后附近 active mob 可以恢复。
- [ ] Ecology controller 继续运行。
- [ ] 不因为 save/load 重复加一整批怪。
- [ ] hard cap 仍然有效。

## B9.8 精英怪

长时间探索直到碰到精英：

- [ ] 出现频率明显低。
- [ ] 同时最多 1 个 Champion elite。
- [ ] 有原版 Champion 视觉/信息。
- [ ] 不出现 Blazing。
- [ ] 不出现 Giant。
- [ ] 精英超距 despawn 不掉奖励、不制造额外地形效果。

## B9.9 第一版怪物池

确认只出现当前白名单普通怪：

- Rat / Snake / Gnoll / Crab / Slime
- Skeleton / DM100 / Bat / Brute / Spinner
- 远处/高等级少量 Warlock / Monk

暂不应看到：

- Boss
- Miniboss
- Thief
- 大量召唤型怪

---

# K. 0.5.0 之后怪物阶段优先级

1. 先只根据实机体感调四个参数：
   - target cap
   - spawn interval
   - despawn radius
   - elite chance
2. 验证 Streaming / teleport / save-load 后再扩怪物池。
3. 第二阶段可以按环境主题做怪物权重，但不能让每个环境都提高总数量。
4. 如果普通怪过强，优先调整 pool/tier，不要用提高玩家奖励来补偿。
5. 若未来允许会召唤子怪的敌人，必须把召唤物计入 hard cap。
6. Boss 暂时不做，直到普通怪和精英生态连续多个版本稳定。

# Assist Infinite World Roadmap 与回归测试清单

本文档回答两个问题：

1. **当前还要做什么？**
2. **每次修改后必须测试什么，避免把以前修好的东西弄坏？**

---

# A. 当前项目状态

当前开发候选：

- 版本：0.6.9
- versionCode：965
- Generator：V17
- target stable：`assist-0.6.9-stable`
- dev：`assist-0.6.9-genesis-talents`

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


# B10. 0.5.1 商人据点专项回归

必须重新开始创建 Generator V11 世界，旧 V10 世界不会凭空长出商店房。

## B10.1 遇店频率
- [ ] 出生附近 4 Chunk 内没有商店。
- [ ] 长距离探索后能遇到商店，但不应连续密集出现。
- [ ] 7×7 active window 同时最多一个商人。
- [ ] Liminal anomaly 内不出现普通商店。
- [ ] 商店不被 V10 无限主/次干道直接切穿。

## B10.2 房间
- [ ] 商店是独立封闭小房。
- [ ] 入口为普通门，不需要钥匙。
- [ ] 房内没有主题房原本的大量奖励。
- [ ] 不叠加 legacy chest。
- [ ] 商人站位不和出售商品重叠。

## B10.3 交易
- [ ] FOR_SALE 商品可正常点击购买。
- [ ] 金币足够/不足时按钮状态正确。
- [ ] 可向商人出售普通物品。
- [ ] 当前 active 状态下回购列表可用。
- [ ] 不出售 Magic Mapping 等 Infinite World 禁用探图物品。

## B10.4 库存持久化
购买 1～3 件商品后：
- [ ] Window shift 离开再回来，已购商品不刷新。
- [ ] Save/Load 后已购商品不刷新。
- [ ] 未购买商品仍在。
- [ ] 远处另一家商店有自己独立库存。

## B10.5 商人 Streaming
让商店在 Window shift 前后都仍位于 active window：
- [ ] 商人 local pos 正确 rebase。
- [ ] 不瞬移到旧坐标。
- [ ] 不卡 Actor。
- [ ] 不因为普通敌人 40 格 despawn 消失。

当商店离开 active window：
- [ ] 商人只 unload，不触发 flee。
- [ ] 回来后商人重新出现。
- [ ] 正式库存状态不变。

## B10.6 逃跑
攻击商人或触发偷窃失败：
- [ ] 商人使用原版警告/逃跑。
- [ ] 商品被收走。
- [ ] 该据点永久关闭。
- [ ] Streaming/Save-Load 后商人不复活。

## B10.7 怪物关系
- [ ] 普通怪不会直接刷新在商人 8 格范围内。
- [ ] 已经存在的怪仍可能追进商店，当前版本不把商店做成绝对无敌安全区。
- [ ] 商人不会计入普通怪 target/hard cap。

---

# L. 商人后续方向

先实机确定当前普通商人的密度和库存是否合适，再考虑：
- 极少量特殊商人；
- 不同环境偏好的商品池；
- 特殊货币；
- 随机限量商品；
- 与异境便笺/任务系统联动。

暂时不要一次加入太多商人类型，避免重新造成资源泛滥。


# B10. 0.5.1 商人专项测试

- [ ] 新 V11 世界能在远离出生区后找到商店房。
- [ ] 同一 7×7 active window 不出现多个商人。
- [ ] 商店不会被 V10 primary/secondary infinite road 切穿。
- [ ] 商人可以正常交谈、出售、购买。
- [ ] 6 个 FOR_SALE 货位价格正常。
- [ ] 买掉商品后走远再回来，该货位不会刷新。
- [ ] save/load 后已买商品不恢复。
- [ ] 攻击商人后其正常 flee。
- [ ] 商人 flee 后走远回来不复活。
- [ ] 普通怪不在商人约 8 格内自然生成。
- [ ] 商人不会被普通怪 40 格距离清除逻辑误删。
- [ ] Window shift 后商人位置和 Sprite 正常。

# B11. 0.5.2 / Generator V12 专项回归

要验证新的地形概率，请“重新开始”创建 V12 世界。旧 V11 世界不会被强制重生成。

## B11.1 商人可发现性
- [ ] 出生约 3 Chunk 内不直接生成商店。
- [ ] 相比 V11，正常长距离探索明显更容易遇到 Merchant Outpost。
- [ ] 商店仍不出现在 Liminal anomaly。
- [ ] 商店仍不被 Primary / Secondary 无限路线切穿。
- [ ] 如果同一 active window 出现多个据点，各自商人、库存、购买状态互不串线。
- [ ] 商人 8 格普通怪自然刷新安全区仍有效。
- [ ] 离开/回来、Save/Load 后已购买货位不刷新。
- [ ] 商人 flee 后该据点永久关闭。

## B11.2 Liminal 异境
- [ ] V12 的 anomaly 体感明显比 V11 的 8% 更容易遇到，但仍不是连续铺满世界。
- [ ] Repeating Offices / Pool Halls / Endless Hall 三类仍可跨 Chunk 连续。
- [ ] Primary / Secondary 无限路线仍贯穿 anomaly，不出现永久封死。
- [ ] Pool Halls 不回归假水、黑图、延迟贴图。

## B11.3 异境便笺保证
对三种未收录 anomaly 分别测试：
- [ ] 第一次进入该类型后，在 Hero 附近几格内能看到 InfiniteWorldNote。
- [ ] 纸条位置可达，不刷在墙、坑、陷阱或被占用格。
- [ ] 拾取后 Guide 的“无界异境记录”立即出现对应页面。
- [ ] Save/Load 后页面仍保留。
- [ ] 页面已经收录后，不再因为重新进入同类型 anomaly 无限刷近身纸条。

## B11.4 全局唯一神器水晶箱
从新 V12 世界开始：
- [ ] 400 action-value 前不会触发这个“保证箱”逻辑。
- [ ] 达到 400 后，在 Hero 附近约 2～5 格出现 CRYSTAL_CHEST；若局部拥挤可放宽到 7 格。
- [ ] 箱子进入 seen/visited/mapped，肉眼可发现。
- [ ] 同时获得 1 把 CrystalKey。
- [ ] 箱内只有 1 件 Artifact。
- [ ] 打开箱但不捡神器，Window shift 后回来仍能看到同一件神器。
- [ ] 打开箱但不捡神器，Save/Load 后仍能看到同一件神器。
- [ ] 捡走神器后，Streaming / Save-Load / 长距离往返均不生成第二个保证神器箱。
- [ ] 即使继续累计到 800、1200+ action-value，也绝不能再次触发。
- [ ] 保证箱神器不会因为恢复过程重新抽取成别的神器。

## B11.5 旧系统回归
- [ ] Streaming 仍只在运动动画完成后触发，无原地走路卡死。
- [ ] Fog / Water / VBO 无回归。
- [ ] 普通怪 >40 cell 距离清除仍无掉落、EXP、击杀统计。

# B12. 0.5.3 / Generator V13 专项回归

要测试 V13 新分布必须重新开始创建 V13 世界。

## B12.1 商人实际可发现性
- [ ] 正常探索一段距离后能明显比 V12 更容易遇到商店。
- [ ] 只要 Merchant Outpost 进入 active window，地图上能看到被揭示的商店房区域。
- [ ] 商店进入 Hero 2 Chunk 左右时出现一次“附近有商人据点”提示。
- [ ] 同一个据点提示不会每走一步重复刷。
- [ ] 远离后回来仍不会重复提示同一个据点。
- [ ] 多个商店各自可以提示一次。
- [ ] 商店仍不生成在 anomaly / primary / secondary route Chunk。
- [ ] 购买、出售、回购、库存 consumed 状态仍正常。
- [ ] flee 后永久关闭。
- [ ] 8 格普通怪刷新安全区保持。

## B12.2 异境密度
- [ ] 新 V13 世界中 anomaly 明显比 V12 的 15% 常见。
- [ ] 仍有足够普通地牢区域，不应几乎整个世界连续 anomaly。
- [ ] 一个 5×5 macro 内主题连续一致。
- [ ] macro 边界与普通区域连接正常。

## B12.3 六类异境
分别找到并检查：
- [ ] Repeating Offices
- [ ] Pool Halls
- [ ] Endless Hall
- [ ] Yellow Maze
- [ ] Service Tunnels
- [ ] Dark Storage

新三类重点：
- [ ] Yellow Maze 隔墙密集但可通行。
- [ ] Service Tunnels 网格通道不产生大面积孤岛。
- [ ] Dark Storage 重复储藏块之间有连续过道。
- [ ] V10 Primary/Secondary 路线仍能贯穿所有 anomaly。
- [ ] Streaming 后结构不突然换型。

## B12.4 六篇 Field Notes
- [ ] 六种 anomaly 第一次遇到未收录类型时，附近出现对应便笺。
- [ ] Yellow_Maze 页面正常显示中英文标题/正文。
- [ ] Service_Tunnels 页面正常显示。
- [ ] Dark_Storage 页面正常显示。
- [ ] 收录后不在同类型 anomaly 重复近身刷便笺。
- [ ] Save/Load 后收录状态保留。

## B12.5 历史核心回归
- [ ] 唯一神器水晶箱仍整局只有一个。
- [ ] Hero movement / Streaming 无原地走路卡死。
- [ ] Pool Halls 无假水、黑图、贴图延迟。
- [ ] Fog / VBO 无回归。
- [ ] 普通怪超距 despawn 仍无奖励。

# B13. 0.5.4 指定物品作弊专项回归

- [ ] Assist 菜单出现“获取指定物品”。
- [ ] 装备类 / 消耗品类两级分类可正常进入。
- [ ] Catalog 分类名称和物品真实名称正确显示。
- [ ] 长分类分页前后切换正常，返回分类正常。
- [ ] 数量只接受 1～999。
- [ ] Potion/Scroll 等 stackable 物品按输入数量合并成一组。
- [ ] 武器/护甲/戒指/法杖等 non-stackable 物品按输入数量生成多份。
- [ ] 背包满时多余物品掉在 Hero 当前格，不消失。
- [ ] Iron/Golden/Crystal/Worn Key 写入当前 Dungeon.depth 的 Notes key record，钥匙数量显示刷新。
- [ ] 生成装备和消耗品处于已鉴定状态。
- [ ] 神器/特殊任务物品的作弊生成不会导致菜单崩溃；重复唯一物品属于玩家主动作弊行为，不修改正常世界掉落规则。
- [ ] 0.5.3 V13 Infinite World 存档可直接继续，不需要重新开始。
- [ ] Infinite World 商人、六类异境、唯一神器箱和 Streaming 无回归。

# B14. 0.5.5 / Generator V14 商店连通与 Overlay 专项回归

## B14.1 Merchant connectivity
- [ ] 新 V14 世界中每一个被提示/地图揭示的商店均可正常步行抵达。
- [ ] 不使用炸弹、Blink、穿墙类效果，也能从公共网络进入商店。
- [ ] 商店门外实际存在 PASSABLE route 到至少一个 shared-edge gateway。
- [ ] 自动补路不会无意义穿过其他普通/秘密主题房。
- [ ] 不产生额外非法 Chunk 边界洞口。
- [ ] Merchant room door 仍为普通可进入入口。
- [ ] 商店到 gateway 的真实路线会随商店提示一起出现在地图上。
- [ ] SECRET_DOOR 不会因为 merchant route reveal 被提前显示成普通路。

## B14.2 Legacy V13 repair
- [ ] 用现有 V13 存档进入曾经断路的 Merchant Chunk 时能自动修复通路。
- [ ] 修复后 Streaming 离开再回来，通路仍存在。
- [ ] Save/Load 后通路仍存在。
- [ ] 已经炸开/打开的 merchant entrance 不会被强制还原成关闭门。

## B14.3 Bomb / wall overlay
- [ ] 在带 mixed-theme overlay 的普通房炸墙。
- [ ] 爆炸后逻辑可走位置立即显示为真实地板/开口。
- [ ] Hero 经过该位置时不会被一张旧墙图完全遮住。
- [ ] 相邻 wall stitching / overhang 正常刷新。
- [ ] 连续炸多个格子不出现残墙、黑块或旧 tileset 残影。

## B14.4 Secret Room
- [ ] SECRET_DOOR 未发现前仍表现为普通墙，不泄露独立房间材质。
- [ ] 被动/主动 Search 发现后，门立即显示正确。
- [ ] Secret Room 独立 accent 同一窗口内立即出现，不等 Streaming。
- [ ] 炸开 Secret Room 墙后 custom overlay 同步更新。

## B14.5 核心回归
- [ ] Streaming 仍只在 Hero.onMotionComplete 后触发。
- [ ] 168×168 VBO batching/flush 不回归。
- [ ] Pool Halls 无假水/黑图。
- [ ] V13 六类 Liminal 仍正常。
- [ ] 400 action-value 唯一神器箱仍整局只有一个。
- [ ] 0.5.4 指定物品作弊仍正常。

# B15. 0.5.6 无界旁观测试模式回归

## B15.1 开关与范围
- [ ] 普通地牢 Assist 菜单不显示“无界旁观测试模式”。
- [ ] Infinite World Assist 菜单显示该开关。
- [ ] 0.5.5 V14 旧存档可直接开启，不要求重开。
- [ ] 开关状态可正常持久化。

## B15.2 穿墙与速度
- [ ] 点击墙后方区域可自动穿越多格 WALL。
- [ ] WALL_DECO / solid props / pit 区域可作为测试移动路径。
- [ ] 不改变被穿过 terrain 的真实类型。
- [ ] 不允许走出 active window 的最外层 frame。
- [ ] 不与 Mob/NPC 占据同一格。
- [ ] 未开普通速度作弊时，旁观模式仍至少约 ×4。
- [ ] 普通 Assist speed > ×4 时使用更高倍率。
- [ ] Root / Paralysis / Vertigo 不阻断测试移动。

## B15.3 Streaming / Fog / World coords
- [ ] 连续跨多个 SHIFT_LOW/HIGH 边界，Streaming 稳定。
- [ ] Streaming 仍发生在 Hero movement tween 完成之后。
- [ ] Hero 不原地踏步、不出现 spinner 卡死、不发生世界坐标跳变。
- [ ] 飞过的区域正常驱散迷雾。
- [ ] visited/mapped 在离开再回来后仍保持。
- [ ] 墙内移动时仍有正常局部 FOV，但不能透视整个封闭结构。
- [ ] Pool Hall / 大型 anomaly Streaming 不出现假水、黑块或旧贴图。

## B15.4 怪物冻结
- [ ] 开启模式前记录现有怪物位置。
- [ ] 连续飞行几十/上百步后这些怪物不移动、不攻击、不改变追击状态。
- [ ] 从怪物旁边穿过不会中断长路径移动。
- [ ] 怪物在屏幕上仍可见。
- [ ] 旁观期间 InfiniteWorldMobEcology 不生成新普通敌人。
- [ ] 开箱、捡物、购买等消耗时间的动作也不会让怪物趁机行动。
- [ ] 关闭模式后现有怪物恢复正常 Actor 行动。

## B15.5 正常交互
- [ ] 可正常靠近并打开普通箱、锁箱、水晶箱。
- [ ] 可正常拾取 HEAP。
- [ ] 可正常购买 FOR_SALE。
- [ ] 可正常与 InfiniteWorldShopkeeper 交互。
- [ ] 可正常用钥匙解锁。
- [ ] 点击普通墙不被 Pickaxe Mine 动作抢走旁观穿墙意图。
- [ ] 测试交互不破坏唯一神器箱生命周期。

## B15.6 非物理与安全
- [ ] 穿过 trap 不触发。
- [ ] 穿过 chasm 不坠落。
- [ ] 穿过 plant / web / damaging floor blob 不因 occupyCell 触发。
- [ ] 旁观模式下 Hero 不受伤。
- [ ] Hero 在墙格中仍清晰显示，不被 custom wall / raised wall 完全遮住。
- [ ] 在墙中关闭模式时自动落到最近正常地板。
- [ ] 在正常地板关闭模式时位置不变化。
- [ ] 关闭后普通碰撞、陷阱、怪物、速度、Hero 渲染层全部恢复。

## B15.7 核心回归
- [ ] V14 merchant connectivity repair 仍正常。
- [ ] Dynamic accent refresh 仍正常。
- [ ] 六类 Liminal/Field Notes 正常。
- [ ] 400 action-value 唯一神器箱仍整局仅一个。
- [ ] 0.5.4 指定物品作弊正常。
- [ ] fixed applicationId / stable signature 不变。

# B16. 0.5.7 / Generator V15 专项回归

V15 地形分布发生变化，要验证 14 类新环境必须重新开始创建 V15 世界。

## B16.1 自动层级档案
- [ ] V15 世界不再生成 InfiniteWorldNote 纸条。
- [ ] 第一次进入每一种特殊环境时立即提示并解锁对应档案。
- [ ] 同一种环境再次进入不重复刷解锁提示。
- [ ] Save/Load 后档案仍保持已获得。
- [ ] Journal 可读 Level 0/1/2/3/4/5/6/7/8/9/10/11/37/94 共 14 条。

## B16.2 14 类地形
- [ ] Level 0 黄墙重复迷宫。
- [ ] Level 1 混凝土服务/仓储厅。
- [ ] Level 2 狭窄检修走廊。
- [ ] Level 3 电气/机械网格。
- [ ] Level 4 废弃办公室。
- [ ] Level 5 旅馆走廊/客房。
- [ ] Level 6 Lights Out 狭窄暗廊。
- [ ] Level 7 大面积水域。
- [ ] Level 8 不规则洞穴。
- [ ] Level 9 郊区街道与住宅。
- [ ] Level 10 田野/农路。
- [ ] Level 11 城市街网。
- [ ] Level 37 Poolrooms。
- [ ] Level 94 草地小镇。
- [ ] 每一种都能通过普通 Infinite World 探索直接进入/离开。
- [ ] 每一种都保留四边 gateway 与 V10 无限路连通。
- [ ] 连续 Streaming 后主题不出现错贴、假水、黑块。

## B16.3 分布
- [ ] 4×4 macro 内主题保持一致。
- [ ] 特殊 district 体感约 42%，明显常见但仍保留足够普通地牢。
- [ ] 起点 2 Chunk 缓冲内不出现 V15 特殊 district。

## B16.4 商人出现频率成长
- [ ] Tier0 的 3×3 基础格点已经比 V14 更容易遇到商店。
- [ ] Hero Level 4/7/11/15 或 action 300/800/1600/2800 推进后，新生成远方 Chunk 的 merchant density 逐档提高。
- [ ] 已经生成过的 Chunk 在升级前后 merchant/no-merchant 状态完全不变。
- [ ] 商店仍不覆盖 anomaly/primary/secondary infinite route。
- [ ] 每个商店继续满足 V14 connectivity guarantee。

## B16.5 库存成长
- [ ] 新开局第一个商人不出售高级 Wand/Ring/+1 装备。
- [ ] Tier1 后开始出现少量 Wand/Transmutation。
- [ ] Tier2 后 Ring/Wand 明显增加。
- [ ] Tier3/4 才允许少量 +1 merchandise。
- [ ] 每个商人的 tier 在首次接近后锁定，离开再回来不变。
- [ ] 买走的货物不刷新。

## B16.6 四种扩展背包
- [ ] Hero 缺四包时，第一个遇到的商人至少出售其中一个。
- [ ] 买到一个 Bag 后，下一个新商人保证出售剩余三种之一。
- [ ] 商店预加载后若从作弊/其他来源先获得其原计划 Bag，真正接近时能改卖另一个仍缺 Bag。
- [ ] 收齐 VelvetPouch / ScrollHolder / PotionBandolier / MagicalHolster 后不再强制 Bag。
- [ ] Infinite World Bag 价格为 value×2（最低20），明显低于普通 ×5 商店定价。
- [ ] 消耗过的 Bag slot 不会 Streaming/Save-Load 后复活。

## B16.7 核心回归
- [ ] 0.5.6 无界旁观测试模式正常。
- [ ] V14 merchant route repair / dynamic accent refresh 正常。
- [ ] 唯一神器水晶箱仍整局只有一个。
- [ ] Mob ecology / no-reward despawn 正常。
- [ ] Streaming 仍只在 Hero.onMotionComplete 后发生。
- [ ] fixed package/signature 可覆盖安装旧 Assist APK。

# B17. 0.5.8 Spectator Flight / CC0 Props regression

## B17.1 Spectator movement presentation
- [ ] Enable Infinite World spectator mode and verify Hero uses a floating/idle glide rather than run animation.
- [ ] Levitation particles remain stable during long movement and are not recreated/flickered every cell.
- [ ] Movement is at least x8 even if the general Assist speed option is disabled.
- [ ] If general Assist speed is set above x8, spectator mode respects the higher multiplier.
- [ ] Visual tween keeps pace with logical Hero movement; no delayed sprite catching up to the real position.
- [ ] Repeated 72-cell Streaming shifts still occur only after onMotionComplete and do not produce spinner/walk-in-place regressions.
- [ ] Disabling spectator removes levitation and safely relocates Hero if currently inside invalid terrain.

## B17.2 Spectator vision
- [ ] Spectator FOV reaches approximately 20 cells in open terrain.
- [ ] Walls still occlude normal line-of-sight rather than globally revealing the whole 168x168 map.
- [ ] FOV/fog refresh stays correct immediately after Streaming.
- [ ] Disabling spectator restores normal view distance without restarting the run.

## B17.3 CC0 decoration rendering
- [ ] CI fetch step verifies both Kenney PNG Git blob hashes.
- [ ] Bush, mushrooms, sign, barrel and crate all appear in normal V15 exploration.
- [ ] Backrooms-inspired districts visibly receive more decoration than ordinary chunks.
- [ ] Props never replace doors, water, chests, traps, plants, merchants, mobs or the Hero.
- [ ] Props do not block movement and do not alter pathfinding.
- [ ] Props remain stable across Save/Load and 7x7 Streaming rebuilds.
- [ ] Secret-door discovery/accent refresh does not permanently remove or duplicate decorations.

## B17.4 Magnifier/examine
- [ ] Bush -> 野生灌木 / Wild bush.
- [ ] Mushrooms -> 蘑菇丛 / Mushroom cluster.
- [ ] Sign -> 风化路牌 / Weathered sign.
- [ ] Barrel -> 废弃木桶 / Discarded barrel.
- [ ] Crate -> 空木箱 / Empty crate.
- [ ] WndInfoCell shows the prop sprite and prop description rather than only the underlying floor.

## B17.5 Deferred
- [ ] Do not change monster stats/strength rules in 0.5.8.
- [ ] After the spectator/visual pass is validated on device, design the Infinite World monster strength progression as the next separate task.

# B18. 0.5.9 / Generator V16 scenery regression

To test physical scenery, create a new V16 Infinite World. Existing V15 saves are for visual-only backward-compatibility testing.

## B18.1 Catalog / magnifier
- [ ] Trees, fern, mushrooms, fence pieces, signs, rocks, logs, barrels, crate, tub/trough all render correctly.
- [ ] Casket, torch, rubble, bookshelf, stone cross, gravestone, table, stool, basin, cabinet, iron rail, firepit and weapon display render correctly.
- [ ] Magnifier shows matching localized name/description for every prop.

## B18.2 Density and grouping
- [ ] Ordinary chunks visibly contain about 2-4 decorations.
- [ ] Backrooms districts are noticeably richer than ordinary terrain.
- [ ] Level 10 and Level 94 can reach the highest density without becoming unreadable.
- [ ] Compatible items form small clusters rather than a uniform grid.
- [ ] Groups remain stable across Streaming and Save/Load.

## B18.3 Physical props
- [ ] Normal Hero cannot walk through bulky trees, barrels, crates, furniture, signs, rails, rocks, logs or tubs.
- [ ] Normal mobs path around solid scenery.
- [ ] Mushrooms, fern, rubble and decorative torches remain pass-through.
- [ ] Physical props have no pickup/open/use action.
- [ ] Infinite spectator mode phases through all physical scenery.

## B18.4 Connectivity
- [ ] No prop blocks merchant access.
- [ ] No prop blocks normal or secret doors.
- [ ] No prop blocks central crossing lanes.
- [ ] No prop replaces a chest, sale heap, trap, plant, NPC or Hero.
- [ ] One-cell corridors remain traversable.
- [ ] Clusters cannot combine into a complete local wall.
- [ ] V10 primary/secondary infinite network remains usable.

## B18.5 District identity
- [ ] Levels 1-3 read as industrial/service.
- [ ] Levels 4-5 use indoor furniture.
- [ ] Level 6 remains sparse/eerie.
- [ ] Level 7 keeps water/open-space dominance.
- [ ] Level 8 reads as caves.
- [ ] Level 9 reads as suburban.
- [ ] Level 10 reads as fields/nature.
- [ ] Level 11 reads as urban.
- [ ] Level 37 remains relatively clean/pool-like.
- [ ] Level 94 reads as a decorated toy-like town.

## B18.6 Compatibility/core regressions
- [ ] Existing V15 save gains richer visuals but no new physical blocking.
- [ ] New V16 save uses physical scenery.
- [ ] Spectator x8 glide and 20-cell FOV unchanged.
- [ ] Merchant progression/bag guarantee unchanged.
- [ ] Unique Artifact chest unchanged.
- [ ] Water/VBO/Fog/Streaming fixes unchanged.
- [ ] Monster strength remains unchanged in 0.5.9.


# B19. 0.5.10 / Generator V17 — 全装饰实体 + 神器箱节奏

必须使用新建 V17 世界测试装饰碰撞；旧 V16 存档用于兼容测试。

## B19.1 保证神器水晶箱
- [ ] action-value < 1000 时不会生成保证神器箱。
- [ ] 达到 1000 后，箱子只能出现在 Hero 为中心的 3×3 范围内，即周围 8 个格之一。
- [ ] Hero 周围没有合法格时，不允许把箱子放到更远位置；后续移动/行动后再重试。
- [ ] 箱子为 CRYSTAL_CHEST，且内部只有应有的保证神器。
- [ ] 箱子出现时立即获得一个当前深度 CrystalKey。
- [ ] Streaming、Save/Load、开箱未取走神器、取走神器后均不得复制第二个保证箱。

## B19.2 所有装饰必须实体
- [ ] 30 类装饰逐类抽查，普通 Hero 均不能直接走进贴图所在格。
- [ ] 蘑菇、蕨类、装饰火把、碎石在 V17 也必须阻挡普通移动。
- [ ] Mob 寻路同样绕开所有装饰，不得出现 Hero 被挡但 Mob 穿过去。
- [ ] 装饰仍无拾取/打开/使用交互。
- [ ] spectator QA 仍可穿过所有装饰。

## B19.3 防堵路
- [ ] 中央保证通行十字带无实体装饰。
- [ ] 普通门、隐藏门、transition、well、alchemy、pedestal 周围无堵塞。
- [ ] 箱子/销售堆附近不会被装饰封死。
- [ ] 商人房实际入口和对外通路可正常步行进入。
- [ ] 一格宽通路不会被装饰截断。
- [ ] 同一 Chunk 多个装饰不会组合成封闭墙。

## B19.4 兼容与核心回归
- [ ] 旧 V16 存档仍保持 mushrooms/fern/torch/rubble 可穿的 V16 行为，不被升级成 V17 实体。
- [ ] 新 V17 世界所有装饰实体化。
- [ ] Streaming 仍只在 Hero.onMotionComplete() 后执行。
- [ ] Water/VBO/Fog、absolute world coordinate、merchant persistence、spectator x8/20-cell FOV 均无回归。
- [ ] 本版没有修改怪物强度、血量、攻击、防御、生成池或精英概率。


# B20. 0.5.11 long-run progression regression

## B20.1 Fresh start / intro
- [ ] Fresh Infinite World starts with exactly 300 gold.
- [ ] Normal modes retain their original starting gold.
- [ ] First Infinite World entry shows the dedicated boundless-world introduction, not the sewer text.
- [ ] Normal dungeon still shows the original regional introductions.

## B20.2 Dynamic monster strength
Test at several snapshots: low gear, level 30 with +5/+10, artificial +50, and post-breakthrough +80/+120.
- [ ] Magnifier/info shows a dynamic threat level for new Infinite World mobs.
- [ ] Newly spawned mobs become noticeably tougher with Hero level and gear.
- [ ] High weapon/wand upgrades strongly increase enemy HP but do not by themselves maximally increase enemy damage.
- [ ] High armor/ring defensive growth strongly increases enemy damage response.
- [ ] Accuracy/defense rise without making ordinary attacks permanently miss.
- [ ] Later threat tiers introduce late-game enemy families but never Bosses.
- [ ] Existing visible mobs do not change stats when the Hero swaps equipment.
- [ ] Dynamic loot chance rises but never exceeds certainty.
- [ ] Dynamic EXP keeps level 30-60 progression practical.
- [ ] Distance despawn still gives no loot, XP or kill statistics.

## B20.3 Level and upgrade gates
- [ ] Hero can reach level 30 but cannot become 31 before breakthrough.
- [ ] Reaching level 30 automatically produces one BreakthroughToken.
- [ ] Repeated gate checks never duplicate tokens.
- [ ] Weapon/Armor/Ring/Wand upgrades stop at +50 pre-breakthrough.
- [ ] Artifact visible level stops at +10 pre-breakthrough.
- [ ] After success, Hero can progress to level 60 and stops there.
- [ ] After success, standard equipment stops at +120.
- [ ] After success, equipped capped artifacts continue long-run XP growth toward +30.
- [ ] Assist equipment/artifact boosts obey the same caps.

## B20.4 Breakthrough trial
- [ ] Using token at level 30 opens a fresh 49x49 arena.
- [ ] Arena contains scattered healing/food/utility consumables.
- [ ] Each wave creates exactly 10 enemies.
- [ ] Wave 1 is manageable and waves 5/8/10 are progressively harder.
- [ ] Clearing a wave starts exactly a 30 action-value intermission.
- [ ] Messages appear for next-wave countdown at 20, 10, 5, 4, 3, 2, 1.
- [ ] Wave 10 clear completes the trial and returns to the original Infinite World position.
- [ ] A second attempt after failure gets a clean arena, not the consumed prior arena.

## B20.5 Failure restoration
Before entering, note HP, inventory item counts, equipment, gold, energy, and position.
- [ ] Intentionally die in the arena.
- [ ] No ranking/game-over is submitted and no Ankh is consumed.
- [ ] Hero returns alive to the original position.
- [ ] HP/equipment/backpack/consumables match the pre-trial snapshot.
- [ ] Gold and energy match the pre-trial snapshot.
- [ ] A replacement BreakthroughToken exists.
- [ ] Repeat failure more than once without corrupting the run.

## B20.6 QA controls/core regression
- [ ] Infinite WndAssist contains the +1 Hero level test button.
- [ ] The button reaches 30 quickly but cannot bypass the breakthrough.
- [ ] After breakthrough, the button can test levels 31-60.
- [ ] V17 all-solid scenery still works.
- [ ] Guaranteed artifact chest remains 1000 action-value, Hero-centered 3x3, with CrystalKey.
- [ ] Spectator x8/20-cell FOV and Mob freeze still work.
- [ ] Water/VBO/Fog and post-motion-only Streaming remain unchanged.


# B21. 0.5.12 short breakthrough trial

- [ ] Trial has exactly 5 waves.
- [ ] Every wave spawns exactly 5 enemies.
- [ ] Clearing waves 1-4 starts the same 30 action-value countdown.
- [ ] Wave 5 clear completes the trial immediately.
- [ ] Wave 1 feels easier than wave 3; wave 5 remains meaningfully difficult.
- [ ] Across repeated trial starts, Slime never appears in waves 2-5.
- [ ] Any single trial contains at most one Slime.
- [ ] Wave-1 Slime appears only occasionally (~25% of runs), so most runs have none.
- [ ] Failure restoration, token reissue, clean retry arena and success unlocks are unchanged.


# B22. 0.5.13 breakthrough certificate regression

- [ ] Cheat-generate a wand inside the breakthrough arena, quickslot it, die, and confirm the shortcut icon is gone after rollback.
- [ ] Pre-existing quickslotted items still point to usable restored items after rollback.
- [ ] Artifact and certificate occupy separate slots simultaneously.
- [ ] Unequip certificate: HP/STR/damage/speed/vision/shop/chest/hunger/regen/revive benefits stop.
- [ ] Re-equip certificate: benefits return without duplicating the item or buff.
- [ ] Certificate becomes lv40/lv50/lv60 at Hero 40/50/60.
- [ ] Save/load preserves tier, revive charge and equipped state.
- [ ] Normal FOV radius visibly expands by +1/+2/+3/+4 tiles at certificate tiers.
- [ ] Charged totem revives once, grants brief invulnerability, consumes no Ankh, and resets charge.
- [ ] Normal/locked chest bonus works; guaranteed CRYSTAL_CHEST still contains only its intended artifact behavior.
- [ ] Artifact visible +11..+30 changes effective artifact level and recharge rate, not only the title text.
- [ ] Wave 5 is easier than 0.5.12 but still harder than wave 4.


# B23. 0.6.4 certificate HUD and formatting
- [ ] Certificate item info lists each bonus on a separate readable line.
- [ ] A distinct certificate/amulet buff icon appears directly below the top-left HP bar while equipped.
- [ ] Compact mobile HUD shows 30/40/50/60 over the certificate badge.
- [ ] Tapping the badge opens current bonuses and the complete next-tier preview.
- [ ] lv60 badge reports maximum tier instead of previewing another level.
- [ ] Accuracy bonus changes actual Hero attackSkill.
- [ ] Evasion bonus changes actual Hero defenseSkill.
- [ ] EXP bonus affects normal XP gains but does not inflate Assist QA +1-level actions.
- [ ] Unequipping the certificate immediately removes the three new bonuses and all existing certificate bonuses.
- [ ] Old 0.5.13 save loads under user-facing 0.6.4 without save migration loss.


# B24. 0.6.5 Boundary Seal expansion

- [ ] “测试：快速通关突破” from a fresh restarted run raises to the gate, completes breakthrough, grants exactly one Seal and never enters the arena.
- [ ] The same button inside the arena exits through the normal success restore path.
- [ ] STR is +2/+4/+6/+8 at LV30/40/50/60.
- [ ] Evasion is +10/+16/+24/+32%.
- [ ] XP is +15/+25/+40/+60%.
- [ ] Movement is +8/+12/+18/+25%.
- [ ] Damage reduction is 8/12/16/20% and does not double-reduce hunger damage.
- [ ] Supported negative status duration/effect is reduced 15/25/35/50%.
- [ ] Natural wand recharge improves 20/35/55/80%.
- [ ] Positive potion/scroll timed buffs last 20/35/55/80% longer.
- [ ] Passive trap/secret-door search radius and chance rise at each tier.
- [ ] Seal displays one random permanent immunity and it persists over save/load.
- [ ] ScrollOfUpgrade rerolls to a different immunity and does not raise Seal tier.
- [ ] Unequipping Seal immediately removes immunity, DR, resistance, sensing and all other Seal effects.
- [ ] Shop buy UI shows struck-through original price and green discounted price on Android portrait layout.


# B25. 0.6.6 Miracle / Genesis regression

- [ ] Hero 59->60 requires 3600 XP.
- [ ] At 60, item title is 奇迹·回响 and EXP bonus line is absent.
- [ ] 创世回响 appears under HP and persists after Miracle Echo is unequipped.
- [ ] Re-equipping Miracle Echo restores the copied stat layer.
- [ ] Genesis Echo detail lists the copied Miracle layer line by line.
- [ ] Instant-death test does not consume Ankh or Undying Totem.
- [ ] Trap/enemy forced teleport is blocked; Hero Teleportation Scroll still works.
- [ ] Paralysis/Roots/Vertigo/Charm/Terror/Amok/Drowsy/Sleep/Slow/Chill/Frost do not control the Hero.
- [ ] Shop price is 0 with Genesis Echo.
- [ ] Repeated free purchases sometimes grant extra identical copies; no fixed extra-copy cap exists.
- [ ] Each actual enemy kill gives exactly +1 max HP and +1 STR; distance despawn gives neither.
- [ ] Save/load preserves Genesis Echo unlock and permanent kill bonuses.
- [ ] Base backpack capacity is 25.
- [ ] Seal slots are +5/+8/+12/+16; linked lv60 adds another copied +16.
- [ ] Unequip an overfilled Miracle Echo and verify every overflow item appears at the Hero rather than vanishing.
- [ ] Dynamic mobs do not scale from Miracle Echo, Genesis Echo, copied layer, or permanent kill growth.


# B26. 0.6.7 Genesis authority regression

- [ ] LV60 item is shown everywhere as 奇迹·世界.
- [ ] No player-facing “奇迹·回响” remains.
- [ ] No player-facing “60级经验加成已移除” line remains.
- [ ] 创世回响 persists after 奇迹·世界 is unequipped.
- [ ] Blindness/Daze/Paralysis/Roots/Vertigo/Cripple/Charm/Terror/Amok/Drowsy/Sleep/Slow/Chill/Frost cannot attach with 创世回响.
- [ ] Forced teleport from trap/enemy fails; own Teleportation Scroll succeeds.
- [ ] Direct instant-death source is blocked while ordinary lethal damage can still kill if 奇迹·世界 is not equipped.
- [ ] Equip 奇迹·世界: ordinary damage, magic, trap, hunger and DoT damage all leave HP unchanged.
- [ ] Equip 奇迹·世界: Burning/Poison/Bleeding/Corrosion/Ooze/Weakness/Vulnerable/Degrade/Hex and other NEGATIVE buffs cannot attach.
- [ ] Unequip 奇迹·世界: damage immunity and curse immunity disappear immediately.
- [ ] Seal/Miracle World no longer changes backpack capacity.
- [ ] Genesis Echo Infinite Space always leaves at least five root-backpack slots free as item count increases.
- [ ] WndUpgrade shows current permanent immunity before each reroll.
- [ ] Existing 0.6.6 save upgrades cleanly without losing Genesis kill-growth data.

- [ ] Infinite Space pagination: fill the root backpack beyond one page, navigate all pages, use/select/quickslot items from later pages, and verify the final page keeps five usable empty slots.


# B27. 0.6.8 Infinite World talent progression regression

- [ ] Infinite World Hero at level 11 still has only the tiers appropriate for that level.
- [ ] Reaching level 12 exposes tier-3 base class talents even when no subclass has been selected yet.
- [ ] Reaching level 12 with no subclass grants exactly one Tengu's Mask; repeated world refresh/save-load does not duplicate it.
- [ ] Choosing a subclass with the mask adds the correct subclass tier-3 talents and preserves existing tier-3 points.
- [ ] Reaching level 20 with no armor ability grants exactly one King's Crown.
- [ ] Before the crown is used, the talent window tells the player to use the crown rather than incorrectly mentioning the second/fourth Boss.
- [ ] Choosing an armor ability with the crown initializes and displays the correct tier-4 talents.
- [ ] An old level-60 Infinite World save with missing subclass/armor ability receives the missing selector item(s) on world entry/refresh.
- [ ] Full backpack cannot cause Tengu's Mask or King's Crown to be dropped/lost.
- [ ] Normal non-Infinite dungeon keeps upstream Boss/item talent gating unchanged.
- [ ] Miracle World description no longer ends with the redundant Genesis-link explanation.
- [ ] Genesis Echo description no longer exposes the dynamic-monster-scaling implementation note.

# B28. 0.6.9 Genesis talent regression

- [ ] Seal/Miracle World descriptions contain no “永久免疫” line and Scroll of Upgrade cannot reroll a Seal immunity.
- [ ] Burning/Poison/Bleeding/Corrosion/Ooze/control/curse blocks show a short matching “××免疫” feedback above the Hero and/or log.
- [ ] Forced teleport shows “传送免疫”; direct instant death shows “即死免疫”; linked damage shows “伤害免疫”.
- [ ] Tier 5 appears in Infinite World at the intended level window and all four numerical effects match their descriptions.
- [ ] Tier 6 appears in Infinite World and all five numerical effects match their descriptions.
- [ ] Non-Infinite games still expose only original tiers 1-4.
- [ ] Equip Miracle World: every initialized tier-1-through-tier-6 talent immediately reports/evaluates at max rank without overwriting saved point allocation.
- [ ] Unequip Miracle World: tier-1-through-tier-6 talents immediately return to their stored point values.
- [ ] Valid normal weapon attacks while Miracle World is linked execute ordinary mobs and special-death/mechanism mobs.
- [ ] Tier 7 is hidden before Genesis Echo and visible after Genesis Echo awakening.
- [ ] Tier 7 has exactly one spendable point; after choosing one authority the other three cannot be selected.
- [ ] Tier-7 reset clears the current authority, removes its Buff, and permits a different choice.
- [ ] Selected tier-7 authority shows a dedicated “VII” Buff beneath HP and remains active after Miracle World is removed.
- [ ] Genesis Reach kills any hostile mob currently in Hero fieldOfView by clicking it and remotely collects/opens every revealed heap/chest without walking.
- [ ] Genesis Teleport: double-tap visited standable cells at near/far screen positions; no cooldown; black/unvisited cells reject teleport; a normal single tap still moves after the 0.28s detection window.
- [ ] Genesis Overcast: normal and cursed wand paths execute targeted hostile mobs; positive potion/scroll buffs are at least 999 action-value.
- [ ] Genesis Fortune: test normal, locked and crystal chests; output scatters across the 9x9 Hero-centred area, creates at least 10 extra items, stackables are >=10, and Weapon/Armor/Ring/Wand items are +120.
- [ ] Fortune burst with few nearby passable cells does not lose items; items may reuse valid cells but must remain obtainable.
- [ ] Save/load preserves tiers 5/6 allocations, selected tier-7 authority and its visible Buff.

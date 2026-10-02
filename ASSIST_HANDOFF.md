# Shattered Pixel Dungeon · Assist — 项目交接总说明

## 0. 接手效率规则（每次必读，优先级很高）

本项目采用“效率优先”的上下文读取策略。

### 普通任务默认流程

对于以下类型的小改动：

- 参数调整
- 单点 Bug
- UI 微调
- 文案/显示修正
- 小范围地图生成概率调整
- 单一房间/单一物品/单一交互修正
- 不涉及底层架构的局部功能增加

**不要每次从头通读全部历史交接文档。**

默认做法是：

1. 先看当前代码。
2. 必要时只查看最近版本或与问题直接相关的那一份/几份记录。
3. 直接完成修改。
4. 构建/验证。
5. 修改完成后，及时把本次真实变化同步写回 GitHub 交接记录。
6. 再向用户交付安装包或结果。

默认工作流应是：

> **直接改 → 构建/验证 → 更新必要记录 → 上传/稳定分支/安装包**

目标是把时间优先用于实际开发，而不是重复阅读已经熟悉的历史资料。

### 只有以下情况才重新完整核对历史资料

遇到下列情况之一，才需要重新完整阅读或系统回溯：

- Streaming 核心逻辑改动
- Infinite World 存档结构改动
- 绝对世界坐标体系改动
- WORLD_GEN_VERSION / Generator 大改
- Render / VBO / Water / Fog 等底层渲染问题
- 跨多个旧版本的兼容问题
- 同一个 Bug 已经连续修改多次仍反复出现
- 代码行为与已有记录明显冲突
- 需要追查某个历史失败方案为什么被放弃
- applicationId / signing / build workflow 等发布基础设施改动
- 怪物 persistence / Boss persistence 等新的核心架构阶段

### 重要补充

“少读历史”不等于“不更新历史”。

无论任务大小，只要实际修改了项目：

> **任务结束前仍然必须更新必要的 GitHub 记录。**

也就是说：

- 开始任务时：按需最小化读取。
- 结束任务时：保证记录实时同步。

这条规则与“强制实时记录协议”同时生效，并且属于所有新对话接手时必须优先看到的项目维护规则。

---

> **给任何新的 ChatGPT / Codex / 维护者：先读本文件，再读 `docs/assist/` 下其余文档。**
>
> 本文件的目标是避免因为聊天长度、换对话、换模型或隔了一段时间后，后续修改偏离当前项目方向。
> 这里记录的是“当前真实项目状态、不可破坏的约束、现在做到哪里、下一步做什么”。
>
> 如果本文档与代码冲突，以**最新稳定分支的实际代码**为准，并优先检查当前最新 stable 分支。
> 不要凭记忆猜实现细节。

---

## 1. 项目身份

- 仓库：`flqin5261-ctrl/shattered-pixel-dungeon`
- 上游：`00-Evan/shattered-pixel-dungeon`
- 上游基础版本：Shattered Pixel Dungeon **v4.0.0**
- 项目性质：用户私人使用的 Assist MOD
- 许可证：GPL-3.0
- 应用名：`Shattered Pixel Dungeon · Assist`
- 固定 applicationId：`com.shatteredpixel.shatteredpixeldungeon.assist`
- 当前最新稳定版本：**0.5.9**
- 当前 versionCode：**955**
- 当前最新稳定分支：`assist-0.5.9-stable`
- 当前发布代码 SHA：`4a987553a9705db2dd05b645c77681e9f0a6aaf8`
- 当前对应开发分支：`assist-0.5.9-environment-rich`
- 当前无限世界生成器版本：**WORLD_GEN_VERSION = 16**

用户的核心目标不是做一个“原版小改版”，而是逐步把一个额外模式做成：

> **真正水平无限、随机、多环境、多房间、多互动，最终加入怪物生态与 Boss 的 Shattered Pixel Dungeon 无限世界。**

---

## 2. 新对话接手时必须做的事

新的 ChatGPT 开始工作时，不要先问用户“项目在哪”“现在做到哪”。直接：

1. 打开仓库 `flqin5261-ctrl/shattered-pixel-dungeon`。
2. 先读取本文件顶部的“接手效率规则”和当前项目身份。
3. 普通小改只读取当前代码、近期版本和直接相关记录；只有遇到该规则列出的重大架构/反复故障情形时，才完整读取：
   - `docs/assist/CHANGELOG_DETAILED.md`
   - `docs/assist/ARCHITECTURE_AND_INVARIANTS.md`
   - `docs/assist/BUG_HISTORY_AND_FIXES.md`
   - `docs/assist/ROADMAP_AND_TEST_CHECKLIST.md`
4. 检查最新稳定分支 `assist-0.5.3-stable` 和最新开发分支。
5. 修改前优先从最新稳定分支创建新的开发分支，例如：
   - `assist-0.5.2-xxxxx`
6. **不要覆盖历史 stable 分支。**
7. 每次可安装版本都必须：
   - versionCode 递增
   - 保持固定 applicationId
   - 保持固定签名
   - GitHub Actions 构建成功
   - 最终把 APK 解压出来直接发用户
   - 最好同时给 ZIP 备用

用户明确希望：**中途无需频繁汇报，可以直接做；但做完要给清楚总结和 APK。**

---

## 3. 构建与签名：绝对不能随便改

工作流：`.github/workflows/build-assist.yml`

当前构建流程：

- Ubuntu runner
- JDK 21
- Android SDK 36
- Build Tools 36.0.0
- `./gradlew :android:assembleRelease --stacktrace`
- zipalign
- apksigner
- GitHub Actions artifact

固定签名使用 AOSP test key：

- AOSP commit：
  `045a3d6a3e359633a14853a5a5e1e4f2a11cbdae`
- 来源：
  `aosp-mirror/platform_build`
- 文件：
  - `target/product/security/testkey.pk8`
  - `target/product/security/testkey.x509.pem`

这套签名从早期 Assist 版本起就固定使用。

### 为什么不能改

用户需要 Android 直接“覆盖安装”旧版。

如果改了：

- applicationId
- 签名证书
- versionCode 递增规则

就可能导致：

- 无法覆盖安装
- 提示签名冲突
- 变成第二个独立 App
- 旧存档无法继续使用

因此这三项属于**项目硬约束**。

---

## 4. 现有核心功能

### 4.1 Assist 基础功能

已经稳定，后续不要误删：

- 无敌
- 物品/金币只增不减
- 装备升级
- 自定义升级量
- 移动速度倍率
- 神器相关辅助
- 指定楼层传送
- HUD 顶部独立 Assist 按钮

装备升级的历史修复：

- 普通非神器装备必须使用：
  `item.upgrade(amount)`
- 不要简单用：
  `item.level(after)`
- Mage Staff 升级后还要：
  `updateWand(false)`

移动速度已经同步了：

- 逻辑移动速度
- Hero 行走 tween
- sprint 动画

不要只改逻辑速度，否则动画和角色位置会不同步。

---

## 5. 额外挑战系统

主菜单存在独立的“额外挑战”。

和原版 Challenges 分离，不占普通存档槽逻辑。

目前至少包括：

### 镜庭

- 单层
- 35×35
- 无怪物
- 使用原版移动/门/钥匙/箱子/水/草等
- 独立存档
- 入口楼梯仅作出生锚点，不允许跳层

### 无界地牢 / Infinite World

这是目前项目的主要开发对象。

---

# 6. 无限世界的基本设计

Infinite World 不是“无限层”。

它是：

> **在同一个世界里向东、西、南、北无限行走。**

当前核心参数：

- Chunk：24×24
- 活动窗口：7×7 Chunk
- 活动地图尺寸：168×168 cell
- 绝对世界坐标：
  - `heroWorldX`
  - `heroWorldY`
- 当前窗口中心：
  - `centerChunkX`
  - `centerChunkY`
- 确定性生成：
  - 基于 `Dungeon.seed + chunkX + chunkY`
- 相同世界坐标重复生成时应得到相同基础地形

核心文件：

- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/InfiniteWorldLevel.java`
- `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/InfiniteWorldState.java`

状态主要包括：

- generatorVersion
- centerChunkX/Y
- heroWorldX/Y
- heroWorldInitialized
- terrainOverrides
- generatedChunks
- exploredChunks
- visited
- mapped
- chestStates
- objectStates

---

## 7. 无限世界流式加载：已经稳定的部分不要乱改

这是项目最容易出严重 Bug 的地方。

### 当前策略

- 角色接近活动窗口边缘时重建 7×7 Chunk 窗口。
- 不是每走一个 Chunk 就换窗口。
- 使用 hysteresis，避免边界反复来回跳。
- 当前关键参数：

```java
SHIFT_LOW = CHUNK_SIZE;              // 24
SHIFT_HIGH = MAP_SIZE - CHUNK_SIZE; // 144
SHIFT_STEP = 3;
```

窗口一次移动 3 个 Chunk。

### 这套机制解决过的严重问题

#### 问题 A：角色卡在原地走路

早期在角色 tween 完成前就切换窗口，导致 Actor 进入 `sprite.wait()` 后无法正常恢复。

修复原则：

> 只能在 `Hero.onMotionComplete()` 之后处理窗口切换。

路径大致是：

`Hero.onMotionComplete()`
→ `InfiniteWorldLevel.afterHeroMotionComplete()`

#### 问题 B：地图主题来回闪烁 / A-B-A-B 无限切换

旧窗口阈值和位移量不匹配，角色换窗口后直接落到另一侧触发区，于是下一帧又切回来。

最终使用：

- LOW 24
- HIGH 144
- STEP 3 Chunk

形成足够大的 hysteresis。

#### 问题 C：长距离点击移动后窗口切换，目标点失效

原来 `curAction.dst` / `lastAction.dst` 是本地坐标。

窗口切换后目标仍然指向旧 local cell。

修复方式：

- 切换前把目标转成绝对世界坐标
- 切换后重新 rebase
- 如果新目标不在窗口中则取消

这部分属于**已稳定逻辑**，后续增加怪物、传送、Boss 时不要破坏。

---

## 8. 无限世界渲染：历史上最难的一组 Bug

### 曾经的症状

1. 走远后整片地图变成巨大蓝色/青色“水”
2. 等一会地形才突然出现
3. 修一次后，走过的地图又变成大片黑色
4. 玩家还能在黑色区域正常行走，说明逻辑地形存在，只是 GPU/Tilemap 没画出来

### 真正根因

`Tilemap.map()` / `updateMap()` 只更新 CPU 数据。

`Vertexbuffer.updateVertices()` 也只是标记 dirty。

真正上传 GPU 是：

`Vertexbuffer.updateGLData()`

正常情况下它在绘制阶段通过：

`NoosaScript.drawQuadSet(...)`

才发生。

无限世界在同一帧里重建 map + rebase camera 时，就可能在 VBO 真正上传前画到旧数据/空数据。

### 最终关键修复

给 Tilemap 加同步刷新逻辑：

`Tilemap.flushMapUpdate()`

并在里面真实执行：

`buffer.updateGLData()`

Infinite World 窗口刷新时，对主要 Tilemap 进行同步 flush，再 rebase Hero/camera。

### 水背景的错误方案

曾经尝试：

- 直接隐藏 full-level water SkinnedBlock
- 然后让 Terrain Tilemap 自己画 WATER

结果黑地图。

原因：

原版 `DungeonTerrainTilemap.needsRender()` 本来就故意跳过真正的 WATER tile，底下的动画 water backdrop 才是主水层。

所以当前正确方案是：

- 保留原版 animated water backdrop
- 保留原版 WATER skip
- 修 VBO upload

不要重新走“永久隐藏 water backdrop”的老路。

---

# 9. Infinite World 当前内容系统

截至 0.4.2 / Generator V8：

## 9.1 地形尺度

地图不再只有大空地。

当前随机出现：

- 很小的房间
- 紧凑房间群
- 窄走廊
- 普通房间
- 长条房
- 大厅
- 广场
- 怪异十字结构
- 环形/破碎结构
- 开放区

设计目标不是现实合理，而是：

> 随机、怪诞、变化明显、可互动，同时不能轻易把流式地图生成搞坏。

---

## 9.2 环境主题

可混用原版五大环境 tileset：

- Sewers
- Prison
- Caves
- City
- Halls

历史演进：

- 早期：整个世界只用一个主题
- 后来：Chunk 级异材质 floor patch
- 再后来：Chunk 级 floor + wall 混合
- 0.4.2：进一步加入**房间级 tileset**

现在可以出现：

- 外面洞穴
- 某房间监狱
- 隔壁房间城市
- 再出去回到另一区域

但环境混贴目前仍是“视觉层”，逻辑 Terrain 类型仍使用统一地图。

---

## 9.3 当前主题房间

Generator V8 已经有真正封闭、有墙、有门的小型房间。

房间尺寸参考原版 Room 系统，主要约 5～8 格外尺寸。

当前房间内容包括：

- 植物温室
- 卷轴档案室
- 药剂实验室
- 食物储藏室
- 钥匙房
- 武器库
- 水晶宝库
- 陷阱工坊
- 金币园
- 综合藏宝室

房间入口包括：

- 普通门
- 银钥匙锁门
- 隐藏门 / Secret Door

当前 V8 规则：

- 每个 Chunk 第一个主题房间保证普通门
- 其他房间再随机普通/锁门/隐藏门
- 钥匙房不会被铁钥匙锁死
- 水晶宝库自身不需要铁钥匙，但水晶箱仍需水晶钥匙

---

## 9.4 秘密房

0.4.2 新增。

入口直接使用原版：

`Terrain.SECRET_DOOR`

所以不是自己伪造一个隐藏效果。

原版搜索机制仍生效：

- 未发现时表现为墙
- 角色靠近可能通过被动搜索发现
- 主动搜索成功率高/可保证发现
- 特殊探测效果也可使用原版发现逻辑

秘密房额外有高密度奖励。

重要设计要求：

> 秘密房在“还没发现”时不能因为独立 tileset 的墙色不同而暴露位置。

当前代码因此会在 Secret Door 仍隐藏时跳过独立房间贴图覆盖。

---

## 9.5 植物

现在 Infinite World 会生成真实 Plant，不只是草 Terrain。

包括：

- Firebloom
- Icecap
- Sungrass
- Earthroot
- Fadeleaf
- Sorrowmoss
- Swiftthistle
- Blindweed
- Stormvine
- Mageroyal
- Starflower

V8 植物房会尝试放约 12 株真实植物。

植物消耗/触发状态有持久化逻辑，不应在走远后回来无限复活。

---

## 9.6 食物

无限世界中已可生成：

- Food
- SmallRation
- Pasty
- MysteryMeat
- MeatPie
- SupplyRation

同时有食物储藏室主题房。

---

## 9.7 装备与物品

无限世界中已经加入：

- 武器
- 防具
- 法杖
- 戒指
- 药剂
- 卷轴
- 炸弹
- 火把
- 闪现符石
- 稿子
- 食物
- 植物种子
- 金币

部分物品使用原版 Generator。

探地图相关物品在 Infinite World 中受到限制，避免再次造成大规模 Fog / visited 状态异常。

---

# 10. 钥匙系统

用户特别要求：

> “银钥匙要能开无限地图上所有对应锁门，而不是像原版一样绑定某一层。”

代码里用户口中的“银钥匙”对应 `IronKey`。

### 当前实现

在 Infinite World 中，同类型 Key 比较不再要求 depth 一致。

修改核心在：

`items/keys/Key.java`

Infinite World 下：

- IronKey 全世界通用
- CrystalKey 同类逻辑也不再被旧 depth 绑定限制

Hero 的锁门/水晶箱判断也放开了原版“branch/subfloor 不可使用 key”的限制。

### 当前钥匙供应

V8：

- 钥匙房稳定有 IronKey + CrystalKey
- 锁门附近会生成 IronKey
- 水晶宝库附近会生成 CrystalKey
- 世界中还有额外概率随机钥匙

设计原则：

> 无限世界不允许出现“生成了锁但世界里永远没有对应钥匙”的死局。

---

# 11. 水晶宝箱

当前使用原版：

`Heap.Type.CRYSTAL_CHEST`

需要 CrystalKey。

水晶宝库通常放高价值装备。

箱子打开/拿取状态通过 InfiniteWorldState 保存，不能因为 Chunk 卸载重载而刷新奖励。

---

# 12. 互动环境

当前已经接入的互动包括：

### 炸弹炸墙

Infinite World 中炸弹可以破坏：

- WALL
- WALL_DECO
- MINE_CRYSTAL
- MINE_BOULDER

活动窗口最外围边框故意不可炸，避免炸坏 streaming 边界。

炸过的地形通过 terrainOverrides 保存。

### 稿子采矿

Infinite World 允许用 Pickaxe 处理：

- WALL_DECO 矿脉
- MINE_CRYSTAL
- MINE_BOULDER

没有开放“所有普通墙都能随便稿子挖掉”，以免世界拓扑太容易完全失控。

### 虚空锁链

Ethereal Chains 位移后会同步绝对世界坐标。

否则 Hero visual/local pos 已移动，但 `heroWorldX/Y` 留在原处会导致下一次 streaming 算错。

### 随机传送

Infinite World 重新设计了随机传送策略：

- 普通随机传送优先已探索安全区域
- Teleportation Scroll 可优先把玩家送到未探索边疆
- 目标限制在当前安全加载窗口中
- 不允许一次传送直接把角色扔出活动窗口导致 streaming 崩坏

### 探图道具

为了避免曾经的 Fog/地图状态 Bug：

- Scroll of Magic Mapping：Infinite World 禁用
- Scroll of Foresight：Infinite World 禁用
- Stone of Clairvoyance：Infinite World 禁用

神器不用全部禁。

---

# 13. 音乐

早期 Infinite World 只固定循环一个区域主题。

后来改成多个原版区域音乐播放队列。

当前设计是可以轮换 Sewers / Prison / Caves / City / Halls 的普通区域音乐，不要重新退化成整局一首歌。

---

# 14. 当前仍然没有的系统

### 目前**没有正式加入普通怪物生态**

Infinite World 主模式目前重点一直在：

- 地图
- streaming
- 存档
- Fog
- 物品
- 房间
- 环境互动

用户明确说：

> “等地图上东西加得差不多了，就该引入怪物和 Boss。”

所以怪物是下一大阶段，但不要贸然直接把原版整层 spawn 逻辑搬过来。

需要先设计：

- Chunk 级 mob persistence
- 远处 mob 冻结/卸载
- 回到旧 Chunk 后 mob 状态恢复
- 无限地图安全出生距离
- 怪物追逐跨窗口策略
- 不允许 mob 把窗口 streaming 卡死
- Boss 区域的唯一性/重复生成控制

详见 Roadmap 文档。

---

# 15. 当前已知/待验证问题

这些是 0.4.2 之后新对话应优先关注的测试项。

## 15.1 0.4.2 开锁贴图修复需要实机确认

0.4.1 的问题：

- 锁门逻辑已经解锁
- 但 static mixed-theme overlay 仍画着旧 LOCKED_DOOR
- 看起来门没变

0.4.2 修复：

> 所有可动态变化的门不再由 `InfiniteWorldAccentTilemap` 静态绘制。

包括：

- DOOR
- OPEN_DOOR
- LOCKED_DOOR
- HERO_LKD_DR
- CRYSTAL_DOOR
- SECRET_DOOR
- LOCKED_EXIT / UNLOCKED_EXIT

需要实机确认：

- 铁钥匙开门后贴图马上更新
- 门打开/关闭正常
- Secret Door 被发现后正常变门
- 不残留旧材质门

## 15.2 Secret Room 房间级环境主题可能延迟出现

为了不提前暴露秘密房：

- Secret Door 还隐藏时，该房间的独立 tileset overlay 不生成。

可能导致：

- 玩家刚发现门时，房间仍暂时保持外围主题
- 等下一次 streaming/rebuild 后，独立房间主题才出现

这是当前可接受但值得改进的点。

未来可以：

- Secret Door discovered 时单独重建该房间 custom overlay
- 而不是等整个 Infinite World window refresh

## 15.3 房间级静态 overlay 与炸墙

房间 floor/wall 混贴是静态 custom Tilemap。

如果玩家：

- 在房间里炸墙
- 或改变某些被 custom overlay 覆盖的 Terrain

逻辑 map 会更新，但静态 overlay 有可能直到下一次窗口 rebuild 才完全同步。

门已经专门绕过这个问题。

未来如果继续增加大量可破坏房间，最好实现：

> room/chunk scoped accent overlay refresh

而不是继续给每个可变地形单独打补丁。

## 15.4 房间连接仍不是完整原版 Graph Room 生成器

目前借鉴了原版：

- 房间尺寸
- Door 思路
- Secret Door
- Special/Secret room 内容哲学

但 Infinite World 不是直接调用原版完整 `Room + Painter + Graph` 楼层生成流程。

原因：

原版生成器假定：

- 有完整楼层边界
- 有 entrance/exit
- 一次性生成整层
- Room graph 全局可见

Infinite World 是：

- Chunk 无限生成
- 局部 7×7 streaming
- 必须确定性重建

未来可以继续抽取原版 Room 的“局部房间模板”，但不要直接替换 Infinite World 主生成器。

---

# 16. 最近一版 0.4.2 的重点

版本：

- 0.4.2
- versionCode 943
- Generator V8
- branch：
  - `assist-0.4.2-secrets`
  - `assist-0.4.2-stable`
- `assist-0.4.3-stable`
- `assist-0.4.4-stable`
- `assist-0.5.0-stable`
- `assist-0.5.1-stable`

主要变化：

1. 修复动态门被 static mixed-theme layer 覆盖的问题。
2. 每 Chunk 至少保证一个普通不带锁的门房。
3. 锁门比例降低。
4. 新增 Secret Door 房间。
5. 秘密房使用原版搜索机制。
6. 隐藏房奖励更丰富。
7. 新增金币园。
8. 植物园密度提高。
9. 房间尺寸进一步靠近原版。
10. 新增房间级环境 tileset。
11. 未发现秘密房不会用独立材质提前暴露。
12. 继续保留钥匙配套生成。

---

# 17. 下一阶段建议顺序

不要马上把所有怪物一起塞进来。

建议：

## 阶段 A：继续补房间内容

优先添加原版风格：

- Magic Well 房
- 祭坛/献祭房
- Runestone 房
- 小型 Library
- Pool 房
- 火焰房
- 矿洞房
- 宝箱迷宫
- 陷阱走廊
- 废墟小屋
- 破碎牢房
- Barricade 房
- 骨堆房
- 多水晶房
- 随机机关房

并继续增加：

- 普通门
- 隐藏门
- 少量锁门
- 房间级独立主题

## 阶段 B：环境层优化

解决：

- secret room overlay 发现后立即刷新
- 炸墙/烧书架后 room overlay 及时刷新
- 多 tileset 更自然
- 水环境也做局部差异，但必须非常谨慎，不能重现 0.3.6 的水/黑屏问题

## 阶段 C：怪物生态

先普通怪，再精英，再 Boss。

必须做 persistence 和 streaming-aware AI。

## 阶段 D：Boss

Boss 不应该每个 Chunk 随机刷。

建议使用：

- rare boss landmark chunk
- 世界坐标唯一 key
- boss defeated state
- 专属房间
- 局部封锁/解锁
- 远离时冻结，不能因为窗口卸载无限复制 Boss

---

# 18. 历史稳定分支

保留，不要覆盖：

- `assist-base1`
- `assist-0.1-stable`
- `assist-0.2-stable`
- `assist-0.2.1-stable`
- `assist-0.3-stable`
- `assist-0.3.1-stable`
- `assist-0.3.2-stable`
- `assist-0.3.3-stable`
- `assist-0.3.4-stable`
- `assist-0.3.5-stable`
- `assist-0.3.6-stable`
- `assist-0.3.7-stable`
- `assist-0.3.8-stable`
- `assist-0.3.9-stable`
- `assist-0.4.0-stable`
- `assist-0.4.1-stable`
- `assist-0.4.2-stable`

开发历史分支也全部保留。

特别注意：

`assist-0.3.6-stable` 是早期 0.3.6 build 925 快照，
而 `assist-0.3.6-renderfix` 后续继续迭代到了 0.3.6 build 928。

因此如果排查 0.3.6 渲染修复史，不要只看 `assist-0.3.6-stable`。

---

# 19. 最重要的项目原则

1. **稳定优先于花哨。**
2. 地图可以怪、不合理，但不能轻易把 streaming 搞崩。
3. 同一个世界坐标重新加载必须稳定。
4. 走远后回来，探索状态必须保留。
5. 玩家修改过的地形必须保留。
6. 打开的箱子不能刷新。
7. 触发过的植物/陷阱不能因为 Chunk reload 无限重生。
8. 动态门必须真的动态。
9. 不要为了视觉混贴再次破坏 water/Fog/VBO。
10. 原版 Room 系统可以参考、抽取，但不能不加适配就硬搬。
11. 任何跨位置移动：
    - Teleport
    - Chains
    - 未来 Boss 技能
    都要同步 Infinite World 绝对坐标。
12. 未来怪物必须考虑 Chunk unload/reload。
13. 每次新 generator 大改都提升 WORLD_GEN_VERSION，避免旧世界地形突然变形。
14. 旧存档能兼容就兼容，不能兼容就保持旧 generator，而不是偷偷重生成。
15. 每版做完必须给用户真正能安装的 APK。

---

# 20. 其他详细文档

继续阅读：

- `docs/assist/CHANGELOG_DETAILED.md`
  - 从 Base1 到 0.4.3 的逐版本历史
- `docs/assist/ARCHITECTURE_AND_INVARIANTS.md`
  - Infinite World 架构、坐标、存档、渲染、对象状态
- `docs/assist/BUG_HISTORY_AND_FIXES.md`
  - 卡死、水贴图、黑屏、Fog、门贴图等问题的原因和错误方案
- `docs/assist/ROADMAP_AND_TEST_CHECKLIST.md`
  - 当前待测、下一步功能、怪物/Boss接入前检查清单

---

# 21. 0.4.3 / Generator V9 — 无限性保障、资源稀疏化与异常空间

当前 0.4.3 是一次“世界结构质量”更新，直接来自用户长期实机探索反馈。

## 21.1 为什么加入全局无限骨架

用户曾在某个旧世界中遇到：

- 各个分支都走到尽头；
- 局部地图仍然正常，但从出生区域可达的道路最终全部封死；
- 最新版本不常复现，但普通支路仍会自然出现死路。

用户并不要求每条岔路都无限延伸，而是要求：

> **至少保证整个世界从出生区域所属的连通分量中，始终存在可以无限继续探索的路线。**

V9 因此增加稀疏 world-scale backbone：

- 每隔 6 个 Chunk 的整列保留一条南北双格主干；
- 每隔 6 个 Chunk 的整行保留一条东西双格主干；
- 原点 (0,0) 同时位于东西和南北主干；
- 主干在普通房间、装饰和主题地形生成完成后最后刻出，因此不会再被主题房墙体堵死；
- 普通支路、房间、岔道仍然允许成为死胡同。

这不是把所有 Chunk 强制做成十字路口，而是提供一个隐藏在整个世界结构中的“无限骨架”。

## 21.2 V9 资源与陷阱重新平衡

0.4.2 的问题不是没有内容，而是奖励密度过高：

- 每个 Chunk 都可能同时存在普通箱子、V5 容器、V6 loose loot、钥匙和主题房奖励；
- 玩家开新世界走不了多远就可能收齐大量武器、卷轴、药剂和食物；
- SECRET_TRAP 也被普通环境 prop 和陷阱房重复投放，体感过多。

V9 调整：

- 普通基础宝箱约从 42% Chunk 降到 12%；
- V5 环境容器改为约 28% Chunk 才有 1 个；
- V6 loose loot 改为约 30% Chunk 才有 1 个；
- 野外 IronKey / CrystalKey 概率大幅下降，但锁门和水晶宝库仍保留配套钥匙保障；
- 普通野外植物改为约 45% Chunk 1 株，而不是每 Chunk 1～4 株；
- 普通环境 SECRET_TRAP 占比大幅下降；
- 陷阱工坊的隐藏陷阱密度也降低；
- 卷轴房/药剂房变为约 1～2 件核心物资；
- 食物房约 2～3 件；
- 武器库约 1～2 件装备；
- 水晶宝库 V9 只放 1 个水晶箱；
- 综合藏宝室约 2～4 件；
- 秘密房额外奖励约 2～3 件，仍比普通房值钱但不再一次塞满背包；
- 金币园保留“很多金币堆”的特色，但堆数和单堆金额都下调。

原则：

> **世界内容可以密，真正的高价值物资不能密。**

## 21.3 秘密房可发现性修复

0.4.2 用户实机探索没有找到秘密房。

V9 调整为：

- 普通随机 SECRET_DOOR 仍存在；
- 每个 3×3 Chunk 宏区中确定性指定一个秘密房候选（极少数包含出生原点的特殊情况除外）；
- Infinite World 中角色靠近 SECRET_DOOR 后，被动搜索发现率提高到 55%；
- 主动搜索仍走原版必定/高概率发现逻辑；
- SECRET_TRAP 的发现率没有随秘密门一起提高，因此不会进一步加剧陷阱刷屏。

秘密房仍然不能从远处直接看到。

## 21.4 Liminal / 异常大区域

V9 新增少量 5×5 Chunk 的宏观异常区域。

它们避开出生附近，约 8% 的 5×5 宏区成为异常空间，面积约 120×120 cell，目的是制造与普通地牢完全不同的长距离体验。

当前有三类：

### Repeating Offices / 重复回廊

- 规则性很强的墙格和重复小隔间；
- 大片 `EMPTY_SP`；
- 故意让方向感变差；
- 灵感来自“重复办公空间/阈限空间”的感觉，但地图和文本均为本项目自有生成设计。

### Pool Halls / 静水廊厅

- 大片浅水池；
- 重复干燥步道；
- Chunk 之间连续形成大尺度水厅；
- 水仍使用原版稳定 water backdrop，不引入新的局部 water renderer。

### Endless Hall / 无尽大厅

- 很大的空旷空间；
- 稀疏而重复的柱/雕像；
- 重点是尺度和重复感，而不是奖励。

异常区域仍保留 shared-edge gateways，并叠加 V9 无限骨架，因此不会成为封闭孤岛。

## 21.5 异境便笺与指南专栏

V9 已为异常区域建立正式的可收藏记录系统：

- 新 Document：`INFINITE_WORLD_NOTES`
- 新物品：`InfiniteWorldNote`
- 指南页中单独增加“无界异境记录 / Infinite World Field Notes”栏目
- 当前三篇：
  - `Liminal_Offices`
  - `Pool_Halls`
  - `Endless_Hall`

每个异常 5×5 宏区只在其中一个确定性 Anchor Chunk 放一张对应便笺。

拾取后：

- 原物品从世界消失；
- 页面加入 Journal/Guide；
- objectStates 记录已取走状态；
- Chunk unload/reload 后不会重新刷；
- Journal 文档自身仍使用原版 Document 持久化。

异常区刻意不再生成普通大量 loot；纸条本身就是主要探索奖励之一。

## 21.6 V9 新世界测试重点

必须用“重新开始”创建 Generator V9 世界测试：

- 长距离四方向探索，确认总有主连通路线可以继续；
- 允许普通岔路存在死胡同；
- 找至少一个 Secret Door；
- 比较 0.4.2 与 0.4.3 的物资/陷阱密度；
- 进入 5×5 anomaly macro 后连续跨多个 Chunk，确认 streaming 稳定；
- Pool Halls 不得重新出现历史“整片假水/黑地图”Bug；
- 拾取异境便笺后检查指南独立栏目；
- 离开异常区域再回来，便笺不得重复刷新。

---

## 强制实时记录协议（所有后续对话必须遵守）

从本条加入后，Assist 项目的任何一次实际修改都必须执行“代码 + 记录”双提交原则。

### 什么情况下必须更新记录

只要本次工作发生以下任意一种情况，就必须在本次任务结束前把记录同步写回 GitHub：

- 新功能
- Bug 修复
- UI/交互调整
- 参数调整
- 地图生成算法调整
- 新房间/物品/环境/怪物/Boss
- Streaming / Fog / Render / Save 等底层改动
- Build / Signing / Workflow 改动
- 新的已知问题
- 失败方案、回滚方案或重要技术结论
- 用户实机测试得到的新反馈
- 下一阶段路线发生变化

### 一次任务的完成标准

以后不能把“代码已经改完”视为任务结束。

完整完成必须包括：

1. 完成代码修改。
2. 编译/构建成功（如果这次改动需要可安装版本）。
3. 修复构建错误直到成功。
4. 根据改动类型更新交接文档。
5. 更新 PROJECT_STATE.json 中的当前状态。
6. 如果发布新版本，更新版本号/versionCode/branch/generatorVersion。
7. 如果是 Bug，补充“症状、根因、错误尝试、最终修复、回归测试”。
8. 如果架构变化，更新 ARCHITECTURE_AND_INVARIANTS.md。
9. 如果路线变化，更新 ROADMAP_AND_TEST_CHECKLIST.md。
10. 最后才向用户汇报“已完成”。

### 记录粒度

不是每改一行代码都单独写日志，而是以“一次完整修改任务/一次版本迭代”为最小记录单位。

例如：

- “修复 Secret Door 显示错误 + 局部 overlay 刷新”可以作为一条完整记录。
- “新增 8 种房间 + 调整房间生成比例”可以作为一次版本记录。
- “怪物 persistence 第一版”应单独形成架构记录。

### 新对话规则

任何新的 ChatGPT / Codex 接手本项目时：

1. 必须先读取 ASSIST_HANDOFF.md 和 docs/assist。
2. 完成任何修改后，必须同步更新这些文档。
3. 不得以“用户没有提醒我要写日志”为理由跳过记录。
4. 如果代码与文档不一致，以代码为准，然后立即修正文档。
5. 稳定分支不得覆盖；新版本继续新建 dev/stable 分支。

这条规则属于项目维护硬约束，与 applicationId、签名稳定、versionCode 递增、Streaming 安全约束同等级。

## 文档维护规则

以后每次发布新版本时都要同时更新这些文档：

1. 更新本文件的“当前版本 / 当前状态”
2. 在 `CHANGELOG_DETAILED.md` 追加版本
3. 新架构决定写进 `ARCHITECTURE_AND_INVARIANTS.md`
4. 新踩坑/严重 Bug 写进 `BUG_HISTORY_AND_FIXES.md`
5. 已完成 Roadmap 项移出“待做”，加入“已完成”
6. 写清：
   - 版本号
   - versionCode
   - stable branch
   - dev branch
   - WORLD_GEN_VERSION
   - APK 是否构建成功
   - 需要重新开始世界还是可沿用旧存档

这样以后即使完全换一个聊天窗口，也能从 GitHub 恢复完整上下文。


# 22. 0.4.4 / Generator V10 — 多层级无限路线网络

V9 已经保证“世界整体一定无限”，但用户指出如果只有主干无限，普通旁路大量死路，体感仍然像“只能沿高速公路无限走”。

V10 保留 V9 主干，同时加入真正无限的次级支线。

- Primary：每 6 Chunk 的横纵主干继续存在。
- Secondary horizontal：每 18 Chunk 高度 band 选一条非主干 Chunk 行，整行无限东西延伸。
- Secondary vertical：每 18 Chunk 宽度 band 选一条非主干 Chunk 列，整列无限南北延伸。
- 次级路线在每个 Chunk 内使用 shared-edge hash 决定边界入口，因此会自然游走而不是笔直画线。
- 横向次级路线必然周期性穿过纵向主干；纵向次级路线同理，因此它们不是孤立的无限平行线。
- 普通短岔路、主题房和小走廊仍允许 dead end。

设计目标：

> **世界里同时存在多条真正可以无限探索的路线，而不是只有一套主干；但仍保留局部死胡同和迷宫感。**


# 23. 0.5.0 — 稀疏怪物生态

Infinite World 从本版本开始正式加入普通怪和稀有精英怪，但战斗仍是次要玩法。

## 23.1 用户明确规则

- 探索优先，杀怪只是部分玩法；
- 不允许短距离不断刷怪；
- 玩家周围同时存在的敌人必须有上限；
- 怪离玩家太远后像 Minecraft 一样直接消失；
- 距离 despawn 不掉落、不加经验、不计击杀；
- Boss 暂时完全不加；
- 精英怪可以有，但必须很少。

## 23.2 当前参数

- 常规刷怪目标上限：5
- 绝对硬上限：6
- 同时精英上限：1
- 新怪出现距离：路径距离 14～28 cell
- 强制 despawn：约 40 cell
- 初次生态检查：进入世界后约 20～32 回合
- 后续检查间隔：约 32～50 回合
- 单次最多刷 1 只
- 精英基础概率：约 6%
- Liminal anomaly 区刷怪率再乘 0.45

实际体感目标是常见约 1～3 只，不是长期顶着 5～6 只。

## 23.3 怪物类别

初期低危险池：

- Rat
- Snake
- Gnoll
- Crab
- Slime

玩家等级/世界距离提高后逐渐混入：

- Skeleton
- DM100
- Bat
- Brute
- Spinner
- Warlock
- Monk

暂时不加入 Thief，原因是它可能偷玩家物品；若它随后因距离 despawn，会造成被偷物品永久丢失。

暂时不加入大量召唤型怪，以免破坏硬上限和第一版生态判断。

## 23.4 精英怪

复用原版 ChampionEnemy：

- Projecting
- AntiMagic
- Blessed
- Growing

没有使用 Blazing，因为 Champion buff detach 会制造火焰；距离 despawn 时必须无奖励、无环境副作用。

没有使用 Giant，因为它改变 LARGE placement 需求，第一版先保持生成规则简单。

## 23.5 怪物不是无限世界永久实体

当前设计特意**不做远处 Mob 世界持久化**。

仍在玩家附近的怪：

- 原版 Level MOBS 正常保存；
- save/load 后继续存在。

超出约 40 cell：

- Actor 移除；
- Level.mobs 移除；
- Sprite 销毁；
- 不保存在远处 Chunk；
- 将来空缺由低频 ecology 重新产生新怪。

这是用户要求的 Minecraft 式临时生态，而不是每只怪永久占据某个世界坐标。

## 23.6 Streaming

Mob.pos 是 local 168×168 cell。

Window shift 3 Chunk 后必须同步重定位当前附近 Mob：

- newX = oldX - shiftedCellsX
- newY = oldY - shiftedCellsY
- 清 path
- 清 enemy/target
- 状态回到 WANDERING
- Sprite place 到新 local cell

超出新 window 或超过 despawn radius 的 Mob 直接无奖励消失。

因此后续改 Streaming 时必须同时回归测试怪物 rebase。

## 23.7 不需要重新开始地图

0.5.0 没改基础 terrain generator：

- WORLD_GEN_VERSION 仍为 10。

因此现有 V10 Infinite World 可以直接升级后出现怪物，不必为了怪物功能重开世界。


# 24. 0.5.1 — 流浪商人据点

Generator V11 新增稀有、固定世界坐标的商人据点。

核心规则：

- 商人不是普通随机 NPC，而是世界结构的一部分。
- 据点只出现在普通区域，避开出生区、Liminal anomaly 和 V10 无限道路主/次干线。
- 商店占一个真正的主题房，普通门进入。
- 7-Chunk 商人格点保证 7×7 活动窗口同时最多一个商人，避免原版 Shopkeeper 的全局 FOR_SALE 逻辑互相干扰。
- 每店最多 6 件商品，内容偏基础补给 + 1 件装备，不制造新的高密度奖励。
- 商品购买后永久缺货，不因 Streaming/Save-Load 刷新。
- 商人逃跑后据点永久关闭。
- 正常 Streaming 离开只 unload，不算逃跑。
- 商人是中立 NPC，不进入 0.5.0 普通敌人的 40 格 despawn。
- 普通敌人不会直接刷新在商人 8 格范围内。

0.5.1 改变了基础房间主题，因此 WORLD_GEN_VERSION 从 10 升到 11。要看到新商店房必须重新开始 Infinite World；旧 V10 世界继续使用旧地形公式。


# 24. 0.5.1 — 商人据点

- Generator V11
- 新增稀有固定商店房和 `InfiniteWorldShopkeeper`
- 商店使用原版 FOR_SALE / WndTradeItem 买卖系统
- 每个 active window 最多一个商人
- 商店库存固定 6 格，买走后不刷新
- 商人被攻击逃跑后不会 streaming 复活
- 商人不受普通怪物距离 despawn
- 普通怪不会在商人约 8 格内自然刷新
- 商店不会生成在 V10 primary/secondary infinite route 上

# 25. 0.5.2 / Generator V12 — 可发现性与唯一神器箱

本版依据 0.5.1 实机反馈调整“能不能真实遇到内容”，不是重做商人或异境系统。

## 25.1 商人
- V12 Merchant lattice：5 Chunk（V11 老世界仍为 7）。
- 出生排除范围：约 3 Chunk。
- 仍避开 Liminal anomaly、Primary spine、Secondary infinite route。
- 因密度提高，旧的“7×7 active window 绝对最多 1 个商人”不再是 V12 不变量；理论上同窗最多可覆盖 4 个 lattice 点，实际受排除规则限制。
- 交易、6 槽库存、购买持久化、逃跑永久关闭、8 格怪物刷新安全区均保留。

## 25.2 Liminal 与 Field Notes
- V12 anomaly macro 概率约 15%；V9～V11 继续 8%。
- 原固定 Anchor Note 不删除。
- 首次进入一种尚未收录的异常空间时，会在 Hero 附近放置可见 InfiniteWorldNote，保证玩家能发现并收录说明。
- 对应 Guide 页面收录后，近身补偿停止。

## 25.3 唯一神器箱
- Hero 正向 action-value 累计到 400 后触发。
- 整局只允许一个保证神器箱。
- 在 Hero 附近 2～5 格生成，必要时放宽到 7 格。
- 类型为 CRYSTAL_CHEST，强制 seen / visited / mapped，并给予 1 把 CrystalKey。
- 内容为 1 件 Artifact，优先从剩余神器牌组中选择并消耗唯一性。
- InfiniteWorldState 保存 action-value、世界坐标、神器 index 和 0/1/2/3 生命周期。
- Streaming / Save-Load 只能恢复同一个箱，不能重新抽奖或生成第二个箱。

## 25.4 测试要求
V12 地形概率发生变化，因此要完整体验商人/异境密度必须重新开始一个 V12 世界。
主要实机观察：
- 商店现在是否仍过少，还是已经过密；
- anomaly 15% 的体感；
- 三种异境纸条是否都能稳定看到；
- 400 action-value 保证神器箱是否唯一、可见、可开；
- 开箱未拾取与拾取后的 Streaming / Save-Load 状态。

# 26. 0.5.3 / Generator V13 — 商人可发现性 + 六类异境

用户实机确认 V12 唯一神器箱正确，但商人仍难遇到，Liminal 类后室区域仍偏少。

## 26.1 商人
- V13 lattice spacing=4 Chunk；V12=5，V11=7。
- 出生排除约 2 Chunk。
- 仍避开 anomaly、Primary、Secondary 无限路线。
- Outpost 一旦进入 active window，商店房附近会直接标记 mapped，出售货堆设 seen。
- 未提示过的据点进入 Hero 约 2 Chunk 内时，出现一次本地化商人提示。
- 提示状态使用 per-outpost objectStates，因此不会每步刷屏。
- 购买持久化、逃跑关闭、6 槽库存、8 格怪物刷新安全区均不改。

## 26.2 异境
V13 anomaly 5×5 macro 概率提高到约 28%，并扩展到六种：
1. Repeating Offices
2. Pool Halls
3. Endless Hall
4. Yellow Maze
5. Service Tunnels
6. Dark Storage

V12 老世界仍按 15% / 三种，V9-V11 仍按 8% / 三种。

## 26.3 新 Field Notes
新增：
- Yellow_Maze
- Service_Tunnels
- Dark_Storage

六类异境都继续有 deterministic anchor note + 首次未收录时 Hero 附近可见便笺保证。

## 26.4 保持不变
- V12 400 action-value 唯一神器箱；
- Streaming onMotionComplete 时机；
- 24/144/3 hysteresis；
- VBO/Water/Fog 修复；
- Mob 无奖励距离 despawn。

# 27. 0.5.4 — 指定物品 / 指定数量作弊

Assist 菜单新增“获取指定物品”。

- 物品来源直接使用原版 Catalog，不维护第二套名单。
- 分类分为装备/神器/饰物与消耗品/材料/钥匙两大组。
- 具体物品列表每页最多 8 个。
- 数量范围 1～999。
- stackable：一次生成指定数量。
- non-stackable：生成指定份数。
- 背包满则掉 Hero 脚下。
- Key 特殊处理：使用当前 Dungeon.depth，加入 Notes/KeyRecord 并刷新钥匙显示，而不是错误塞入背包。
- Gold / EnergyCrystal 直接增加 Dungeon.gold / Dungeon.energy。
- Dewdrop 直接加入 Waterskin，水袋满或不存在时给出明确提示。
- 生成物品自动 identify。

本版 WORLD_GEN_VERSION 继续为 13；没有改变 Infinite World 地形公式，0.5.3 V13 世界可以直接继续。

# 28. 0.5.5 / Generator V14 — Merchant Connectivity + Dynamic Accent Refresh

用户实机确认：某次 Merchant Outpost 虽被提示并显示在地图上，但正常探索路线全部是死路；通过炸弹/隐藏房才发现一段逻辑可通行但仍显示为墙的区域。

根因分成两类：
1. Merchant Room 固定为 themed room index 0，它先刻出的 corridor 可能被后生成 room index 1/2 的 wall shell 覆盖，所以“曾经连接过”不等于最终地图可达。
2. InfiniteWorldAccentTilemap 是静态 custom overlay；炸墙或秘密门发现后 Level.map 已更新，但旧 wall mesh 可能继续盖住新地板，产生视觉穿墙。

V14 修复：
- 所有地形 pass 完成后，对 merchant entrance -> shared chunk gateway 做真实 PASSABLE path validation；
- 断路时才补 deterministic corridor，优先避开其他 themed room rectangle；
- 旧 V11-V13 active merchant chunks 在 load/stream 时执行同样 repair，并通过 terrainOverrides 持久化；
- Merchant map reveal 同时标记一条真实可走的 shop -> gateway 路线；
- GameScene.updateMap(cell) 会刷新覆盖该 cell 的 InfiniteWorldAccentTilemap；
- Accent refresh 含 1-cell halo，保证 wall stitching/overhang 同步；
- SECRET_DOOR discovery 会重建当前 window 的 accent definitions，使 Secret Room 材质立即正确出现。

重要兼容：
- 新世界使用 V14。
- 旧 V13 存档不用为了此 Bug 强制重开；商店断路会运行时修补。
- Streaming 时机、VBO/Water 修复、唯一神器箱、V13 六类 Liminal、0.5.4 item grant 均不能破坏。

# 29. 0.5.6 — Infinite World Spectator QA Mode

Assist 菜单在 Infinite World 中增加“无界旁观测试模式”，专用于快速实机排查地图生成、连通性、Streaming、Fog 与贴图 Bug。

核心行为：
- 只在 Infinite World 显示/生效；
- Hero 通过临时 QA path map 穿 WALL / solid / pit，不修改真实 Terrain；
- 最低移动倍率 ×4，已有 Assist speed 更高时沿用更高倍率；
- 每一步仍是正常 Hero.move，Streaming 仍只能在 Hero.onMotionComplete 后触发；
- Dungeon.observe / FOV / visited / mapped / InfiniteWorldState 探索持久化继续正常；
- Actor.process 在 Mob.act 之前冻结所有 Mob actor，怪物不检测、不移动、不攻击；
- InfiniteWorldMobEcology 暂停自然刷怪；
- 冻结怪物仍显示，但不再打断 Hero 连续移动、拾取或交易；
- Hero 可正常开箱、捡物、买 FOR_SALE、与商人/NPC 交互和解锁；
- 穿过 trap / plant / chasm / floor blob 不触发 occupyCell 物理效果；
- Hero 在测试模式中免伤，并忽略 Root / Paralysis / Vertigo 对移动的阻断；
- Hero Sprite 临时放到 wall/raised terrain 上方，穿墙时人物仍可见；
- 关闭模式时若 Hero 位于非法格，自动落到最近正常可站立格。

兼容与硬约束：
- WORLD_GEN_VERSION 仍为 14；
- 0.5.5 V14 存档可直接继续，不需要重新开始；
- 不得借此模式改动 Streaming 时机、Water backdrop、VBO batching/flush、absolute world coordinate 规则；
- 该模式是 QA 工具，不得把其临时 passability 写入 terrainOverrides 或正常世界生成公式。

# 30. 0.5.7 / Generator V15 — 14 类 Backrooms District + 自动层级档案 + 成长型商人

用户要求 Backrooms-like terrain 暂时作为 Infinite World 普通探索内容，不实现特殊 noclip/层级跳转系统。

## 30.1 14 类正常探索环境
V15 4×4 macro 中约 42% 为特殊 district，出生缓冲 2 Chunk。
类型：Level 0、1、2、3、4、5、6、7、8、9、10、11、37、94。
所有类型都：
- 正常步行即可进入；
- 保留 shared edge gateways；
- 继续叠加 V10 infinite network；
- 不创建独立 Level；
- 不要求特殊道具；
- 视觉只重组原版 sewers/prison/caves/city/halls 材质，不复制外部图片。

## 30.2 层级资料自动获得
- V15 不再生成 InfiniteWorldNote。
- Hero 首次进入一种 V15 district 时直接 find 对应 `INFINITE_WORLD_NOTES` page。
- 输出一次 `backrooms_info_unlocked` 提示并闪烁 Journal。
- Document 自身负责 Save/Load persistence。
- 旧 V9-V14 保存继续保留老六类 physical note 行为，因此 legacy page keys 不删除。

## 30.3 Merchant frequency progression
- Tier = max(Hero Level tier, heroActionValue tier)。
- Level 阈值：4 / 7 / 11 / 15。
- Action 阈值：300 / 800 / 1600 / 2800。
- Tier0 已有 3×3 base merchant lattice，比 V14 的 4×4 更密。
- Tier1-4 对之后首次生成的 Chunk 逐渐开放额外稀疏 deterministic lattices。
- 每个 Chunk 第一次生成时把 merchant/no-merchant 锁进 objectStates，因此升级后旧地形不会突然出现或消失商店。
- anomaly / primary / secondary route 排除规则继续优先。
- V14 merchant connectivity final-pass guarantee 继续生效。

## 30.4 Merchant stock progression
- 商人第一次进入 Hero 2 Chunk 范围时锁定自己的 stock tier。
- Tier0：基础 weapon/armor、生存品、工具品；equipment slot 不出 Wand/Ring。
- 中期逐步允许 Wand / Ring / ScrollOfTransmutation。
- Tier3 / Tier4 才分别有约 25% / 45% 的 +1 merchandise 机会。
- 普通 merchant 不卖 Artifact。
- 买走库存继续用原来的 consumed state，永不免费刷新。

## 30.5 Mandatory expansion bags
原版四个扩展包：
- VelvetPouch
- ScrollHolder
- PotionBandolier
- MagicalHolster

只要 Hero 还缺任意一个，每一个新遇到的 V15 商人 slot0 都必须卖一个当前 missing bag。
如果商店先被 active window 预加载，Hero 在真正靠近前从别处获得了该 Bag，靠近时会重新挑 still-missing bag。
四包收齐后 slot0 回退 PotionOfHealing。
Infinite World Bag 定价 value×2（最低20）；普通货物保持原版 value×5 pricing。

## 30.6 兼容
- V15 地形分布要新建 V15 世界才能体验。
- V14 老存档继续保持 generatorVersion14，不会被重算成 V15。
- 0.5.6 spectator QA、V14 merchant connectivity、dynamic accent、Water/VBO、唯一神器箱、Mob ecology 均不能回归。

# 31. 0.5.8 — Spectator Flight QA + 可识别 CC0 环境装饰

## 31.1 无界旁观测试模式
0.5.8 把旁观模式从“高速穿墙走路”改成明确的 QA flight：
- Hero.speed() 最低 x8。
- CharSprite PosTweener 也最低 x8，画面位置与逻辑位置同步。
- spectator 移动时不播放 run；保持 idle pose 滑行。
- 使用原版 CharSprite.State.LEVITATING 作为悬浮表现。
- 关闭模式时移除 LEVITATING，恢复 idle，并继续 settleHeroAfterSpectator() 安全落地。
- FOV 只在 spectator 模式强制到 ShadowCaster.MAX_DISTANCE=20。
- 普通玩法视野不改。
- Streaming 时机完全不改：只能 Hero.onMotionComplete() 后 shift。

## 31.2 新环境小物件
第一批只加入 5 种 16x16 环境装饰：
- 野生灌木
- 蘑菇丛
- 风化路牌
- 废弃木桶
- 空木箱

来源：
- Kenney Tiny Town 1.1 / Tiny Dungeon 1.0。
- CC0 1.0。
- 固定上游 commit e22e06e317be6c933b779ad7b055b6a6aeafa5e8。
- CI 在 Gradle 前下载 PNG，并按 upstream Git blob SHA 验证；验证失败直接终止构建。
- 详情见 docs/assist/THIRD_PARTY_ASSETS.md。

## 31.3 装饰层技术规则
- InfiniteWorldDecorationLayer 是 CustomTilemap visual overlay。
- 不改 map[] / passable / solid / water / door / pathfinding，因此不需要 Generator V16。
- 放置在 heaps/traps/plants/themed-room contents 生成之后，跳过已经被实际游戏对象占据的 cell。
- Backrooms V15 district 每 Chunk 最多 2 个；普通 V15 Chunk 约 62% 概率 1 个。
- 避开 chunk 中央常用通行十字带。
- Secret-door accent refresh 与 Streaming rebuild 后都要重建装饰层。
- name()/desc()/image() 接入 WndInfoCell，放大镜必须识别成对应物件，不能只显示 floor。
- Bundle 只保存稀疏 prop cell/kind，不把整张 168x168 空数组写进存档。

## 31.4 当前未做
- 0.5.8 不修改怪物强度、等级、血量、攻击、防御、生成池或精英概率。
- 用户明确要求先完成 spectator + environment visual pass，再单独讨论 monster strength mechanics。

# 32. 0.5.9 / Generator V16 — 高密度环境装饰 + 实体场景物件

用户实机反馈 0.5.8 的五种装饰数量太少，而且全部只是可穿过贴图，环境仍显得空。
0.5.9 暂不做可交互家具/容器，先把“不可交互环境装饰”做完整。

## 32.1 装饰目录
InfiniteWorldDecorationLayer 从 5 类扩到 30 类。
自然/道路类包括多种绿树/秋色树、蕨类、蘑菇、栅栏、木柱、路牌/警示牌、岩石、原木、木桶、封死木箱、木盆、水槽。
室内/地牢类包括旧棺匣、火把、碎石、书架、石十字、墓碑、桌凳、石盆、柜子、铁栏、冷却火堆、武器陈列架。
全部支持放大镜自己的 sprite/name/desc；全部不可拾取、不可打开、不可使用。

## 32.2 密度与分区
普通 Chunk 约 2-4 个；Backrooms district 根据主题约 3-13 个。
兼容类型会形成约 3 个以内的小簇，而不是每个物件完全独立随机散落。
Level 1-3 偏工业，4-5 偏室内家具，8 偏洞穴，9 偏郊区，10 偏田野，11 偏城市，37 保持整洁池厅，94 偏小镇/自然。

## 32.3 V16 实体装饰
Generator V16 对大型装饰使用原版 Terrain.CUSTOM_DECO：
- 普通 Hero/Mob 不能穿过；
- 自动寻路会绕开；
- 仍由 CustomTilemap 绘制 Kenney sprite；
- 无交互行为；
- spectator QA 因为本来就忽略 solid terrain，所以仍可穿过。

可穿过的小型地表装饰当前为：蘑菇、蕨类、碎石、装饰火把。

## 32.4 防堵路
硬装饰不允许：
- 堵原点广场；
- 在 Merchant Chunk 形成实体阻挡；
- 放进中心保证通行十字带；
- 占用 room access；
- 靠近 chest；
- 靠近 door/secret door/transition/well/alchemy/pedestal；
- 成为局部 chokepoint。
局部安全检测会把候选格虚拟封闭，并确认周围可走邻格在 7x7 范围内仍互相连通，同时计入同 Chunk 已选中的其他硬装饰。

## 32.5 兼容
- 新建 V16 世界：高密度装饰 + 大型装饰真实碰撞。
- 旧 V15 世界：会看到扩展后的高密度视觉装饰，但不会被突然加入新的实体碰撞，避免旧路线被改写。
- WORLD_GEN_VERSION 变更只服务于物理场景兼容边界；V15 的 14 类 Backrooms 和成长商人规则继续沿用。
- 0.5.9 仍不修改怪物强度。怪物强度机制等用户确认环境版本后单独讨论。


# 33. 0.5.10 / Generator V17 — 全装饰实体化 + 神器箱节奏调整

用户在 0.5.9 环境版之后明确提出两项调整：
1. 唯一保证神器水晶箱不再在 400 action-value 触发，改为 **1000 action-value**。
2. 当前全部环境装饰都必须有实体碰撞，普通 Hero/Mob 不能直接穿过。

## 33.1 保证神器箱
- 阈值：1000 positive Hero action-value。
- 仍然整局最多一个，生命周期与 Streaming/Save-Load 持久化逻辑不变。
- 生成位置限制为 **以 Hero 为中心的 3×3 格范围**，实际使用周围 8 个相邻格，不占 Hero 当前格。
- 如果触发瞬间 8 个邻格都不合法，不扩大生成半径；后续正 action 再重试。
- 箱子仍为 CRYSTAL_CHEST，包含一个神器。
- 箱子生成时立即向 Notes/KeyRecord 加入一个当前深度 CrystalKey，避免软锁。

## 33.2 V17 全装饰实体
- WORLD_GEN_VERSION = 17。
- 30 类可见环境装饰在新 V17 世界中全部使用 Terrain.CUSTOM_DECO。
- 普通 Hero 不能穿过；Mob 寻路也视为实体障碍。
- 所有装饰仍不可拾取、不可打开、不可使用。
- Infinite spectator QA 仍能像穿墙一样穿过这些实体。
- V17 不再使用“蘑菇/蕨类/火把/碎石可穿过”的例外。

## 33.3 防堵与兼容
- V17 仍保留中心通道、room-access、箱子邻域、门/transition/well/alchemy/pedestal 排除和局部 chokepoint 检测。
- V17 不再通过“替换成可穿装饰”来处理原点/商人区；所有实际放出的装饰都必须作为真实 blocker 通过安全检查。
- 旧 V16 存档继续保持 V16 规则：蘑菇、蕨类、装饰火把、碎石仍可穿，避免旧路线被突然改写。
- 0.5.10 不修改怪物血量、攻击、防御、生成池、精英概率或生态节奏。


# 34. 0.5.11 — 长期成长、动态怪物与30级突破试炼

0.5.11 在 0.5.10 V17 实体环境基础上加入 Infinite World 专属长期成长。WORLD_GEN_VERSION 仍为 17；本版没有改变 Chunk 地形生成公式或 Streaming 时机。

## 34.1 开局与背景
- 新建 Infinite World 初始金币 = 300；普通模式仍使用原版初始金币。
- 首次进入无界模式不再显示原版下水道介绍。
- 新介绍明确：这里是无楼层终点的持久无限世界，普通地形、遗迹和 Backrooms-inspired district 被缝合在一起，商人/资源/危险分布其中，敌人会随角色成长。

## 34.2 角色成长与强化上限
- 突破前：Hero 最高 30 级；Weapon / Armor / Ring / Wand 最高 +50；Artifact 最高 +10。
- 到达 30 级时自动发放唯一 BreakthroughToken。未突破时经验不会把 Hero 推过 30。
- 突破后：Hero 最高 60 级；Weapon / Armor / Ring / Wand 最高 +120；Artifact 最高 +30。
- 普通非 Infinite World 仍使用原版 Hero.MAX_LEVEL=30 和原版装备逻辑。
- Artifact 在原生 +10 等效等级之后使用独立 overlevel 记录；突破后，已到原生上限且正在装备的神器会随 Hero XP 继续向 +30 成长，约每 1.5 个 Hero 等级的经验获得 +1 visible artifact level。Assist 强化也遵循 +10/+30 门槛。

## 34.3 动态怪物强度
每只 Infinite World 敌人在生成瞬间读取 Hero 当前：
- level；
- 最强 Weapon / Armor / Wand 强化；
- 两枚最强 Ring 强化；
- Artifact 数量和最高 visible level。

生成后强度锁定，换装备不会让眼前怪物瞬间重算。

动态内容：
- threat level 1..60，并显示在怪物信息里；
- HP 主要跟随 Hero 的 offensive power；
- damage 主要跟随 Hero 的 defensive power，避免只强化武器却导致怪物伤害爆炸；
- accuracy / defense 随总成长中速提高；
- late threat levels 会逐步引入 Warlock / Monk / Golem / Succubus / Scorpio 等，但仍不加入 Boss；
- EXP 设置与 threat level 相关的最低值，保证 30..60 长线升级不过慢；
- 原始掉落概率乘动态 reward scale，最高约 2.25x，最终概率仍不超过 100%。

高强化装备是远超原版的数值区间，因此 HP/damage 曲线刻意能覆盖 +50 和 +120，而不是只做 2x~3x 的浅放大。

## 34.4 30级突破试炼
达到 30 后可自由决定何时使用 BreakthroughToken。

使用后：
- 保存进入前 Hero 完整 Bundle、金币、能量、原 Infinite World depth/branch/pos；
- 进入独立 branch 99 的 49×49 空旷试炼场；
- 场内零散放置治疗药水、食物、隐形药水和实用卷轴；
- 共 10 波，每波固定 10 个敌人；
- 每波敌人池和倍率递增；末波混合 5 个高阶威胁 + 5 个中阶敌人，避免一次塞满十个远程高阶怪；
- 一波清空后等待 30 action-value，再生成下一波；
- 20/10/5/4/3/2/1 有倒计时消息。

失败：
- Hero.die() 在试炼场被提前拦截，不走 Ankh/真正死亡流程；
- 旧 Hero 临时保持 alive，防止 Char.attack() 在调用栈返回后继续 Dungeon.fail()；
- 恢复进入试炼前 Hero/背包/装备/消耗品/HP、金币、能量；
- 返回原 Infinite World 坐标；
- 重新发放 BreakthroughToken，直到成功。

成功：
- 同样恢复挑战前损耗并返回原位置；
- 不再发信物；
- unlock level60 / +120 equipment / +30 artifact。

## 34.5 QA
WndAssist 在 Infinite World 中新增两种等级 QA：
- “测试：等级+1”：每点一次提升 1 Hero level，便于逐级观察动态怪物曲线；
- “测试：直升上限”：突破前一键升到当前阶段上限30，突破后一键升到60；
- 两个按钮都不能作弊越过30级突破门槛。

BreakthroughToken 是 progression-critical 物品：即使普通背包已满，也会使用一个临时超容量槽强制放入背包，不再掉到 Infinite World 地面，避免 Streaming/重复检查造成信物复制。

0.5.11 不能破坏：V17 实体装饰、1000 action-value 3×3 唯一神器箱、spectator x8/20 FOV、Mob distance despawn、VBO/Water/Fog/Streaming invariants。


# 35. 0.5.12 — 突破试炼缩短为 5×5

用户反馈 0.5.11 的十波、每波十只过于拖沓，且史莱姆在试炼中体感偏难。

本版只调整突破试炼节奏，不改 Infinite World Streaming、地形、商人、神器箱、普通生态怪强度公式或 30/60 级成长门槛。

## 35.1 波次
- TOTAL_WAVES：10 -> 5。
- MOBS_PER_WAVE：10 -> 5。
- 每波清空后仍等待 30 action-value，并保留 20/10/5/4/3/2/1 倒计时提示。
- 总敌人数由最多 100 降到 25。

## 35.2 难度压缩
5 波不是把原十波的前五波直接截断，而是把原来的终局跨度压缩进五波：
- Wave 1：偏友好；
- Wave 2：进入中前期怪；
- Wave 3：开始出现 Brute / Spinner / Warlock 等；
- Wave 4：稳定混入高阶威胁；
- Wave 5：Warlock + Monk + Golem 为固定核心，再混入 Brute/Spinner 与 Succubus/Scorpio。

Wave 5 的 HP / damage / accuracy / defense 倍率仍接近原 0.5.11 Wave 10，因此试炼明显更短，但突破感保留。

## 35.3 Slime
- Slime 仅允许在 Wave 1 的第 5 个槽位出现。
- 该槽位只有 25% 概率选择 Slime，否则为 Rat。
- 所以一次完整试炼最多 1 只 Slime，多数试炼为 0 只。
- Wave 2~5 完全不生成 Slime。


# 36. 0.5.13 — 突破之证、快捷栏回档修复与神器真实超等级

- 修复突破试炼回档后的“幽灵快捷栏”：进入前把6个快捷栏转换成可重绑占位快照；离开试炼时先 reset，再按恢复后的 belongings 重绑；试炼临时物品无法重绑的占位会被清空。
- 第5波再次下调：HP 1.40、damage 1.28、accuracy/defense 1.13、threat bonus +7；仍高于第4波但不再有明显终局跳变。
- 突破成功自动获得并装备 BreakthroughCertificate；它拥有独立第六装备槽，和 Artifact 同时存在。
- 旧的已突破存档如果没有证书，会在 Infinite World 正常移动时补发一次；如果玩家只是手动取下，证书仍在背包，因此不会被强制重新装备。
- 证书随 Hero 40/50/60 自动变成 lv40/lv50/lv60。取下立即失去所有增益。
- lv30/40/50/60 分别提供逐级提高的 HP、STR、伤害、回血、减饥饿、金币、移动、正常FOV、商店折扣、普通/上锁宝箱额外补给概率。
- 证书自带可反复充能的不死图腾。致命伤时若已充满，则先于 Ankh 触发；恢复一定比例HP并短暂无敌，然后充能归零重新积累。
- Artifact +10以上不是只改显示：effective level 已进入 Artifact.level()/buffedLvl() 的真实效果计算；此外 +11..+30 每级额外提供2.5%充能效率，+30合计+50%。
- CRYSTAL_CHEST 不参与突破之证宝箱额外掉落，避免破坏唯一保证神器箱生命周期。


## 36.1 最终补充
- 突破之证“攻击伤害”不仅作用于 Hero.damageRoll，DamageWand 的伤害骰也使用同一倍率。
- BreakthroughCertificate tier 会以温和权重加入 Infinite World dynamic mob power profile；只影响之后新生成的怪，不重算眼前已有敌人。


# 37. 0.6.4 — 突破之证信息与版本号规范化

## 37.1 版本号规则
用户明确不希望出现 0.5.10、0.5.11、0.5.12 这种两位 patch。自本版起，Assist 的用户可见版本号采用“个位 patch 到 9 后向 minor 进位”：
- legacy 0.5.10 -> 0.6.0
- legacy 0.5.11 -> 0.6.1
- legacy 0.5.12 -> 0.6.2
- legacy 0.5.13 -> 0.6.3
- 本轮下一次迭代 -> 0.6.4 / versionCode 960

旧分支不删除、不改写；增加 assist-0.6.0-stable ~ assist-0.6.3-stable 作为规范别名。后续继续 0.6.5...0.6.9，然后 0.7.0。

## 37.2 突破之证排版
- Item desc 不再把全部属性挤在同一段。
- 当前祝福每一项单独一行：HP、STR、伤害、命中、闪避、EXP、饥饿、金币、移速、视野、商店、宝箱、回血、不死图腾。
- 不死图腾的充能需求、复活 HP、当前充能单独成组。

## 37.3 左上角状态标记
StatusPane 本身已经把 hero BuffIndicator 放在 HP 条下方，因此不再重复造第二套 HUD。
BreakthroughBlessing 改为：
- 使用更容易识别的 AMULET 图标；
- 图标叠加 30/40/50/60 证书等级；
- 小屏/手机紧凑 UI 也强制显示该等级文字；
- 点击图标继续使用 WndInfoBuff。

WndInfoBuff 现在按行显示当前全部实际加成，同时显示完整下一阶段预览；lv60 则显示“已达到最高阶段”。

## 37.4 新增三类实际加成
为使突破之证更像长期成长核心，而不是单纯堆 HP/伤害，本版新增：
- 命中：lv30/40/50/60 = +5/+8/+12/+16%
- 闪避：+4/+7/+10/+14%
- 经验获取：+5/+8/+12/+15%

三项都接入真实 Hero 计算路径；取下证书立即失效。
没有再加固定伤害减免或永久免疫，因为当前已有最大生命、被动回血和可充能复活，继续叠防御会明显压低后期风险。


# 38. 0.6.5 — 破界之印强化与快速突破

## 38.1 QA 快速通关
WndAssist 新增“测试：快速通关突破”。
- 未进入试炼时：若 Hero < 30，先安全提升到 30；清理挑战信物；直接设置 breakthroughCompleted；发放并装备破界之印；解锁 60 级成长。
- 已在 branch 99 试炼场中：调用正常 finishBreakthroughTrial(true)，所以 Hero/背包/金币/能量/快捷栏仍按正式成功流程回滚到入场前。
- 已突破：只检查破界之印是否存在，不重复创建。

## 38.2 破界之印数值
LV30/LV40/LV50/LV60：
- STR +2/+4/+6/+8。
- Evasion +10/+16/+24/+32%。
- EXP +15/+25/+40/+60%。
- Movement +8/+12/+18/+25%。
- All non-hunger damage reduction 8/12/16/20%。
- Common negative-effect resistance 15/25/35/50%。
- Natural wand recharge +20/+35/+55/+80%。
- Positive Potion/Scroll buff duration +20/+35/+55/+80%。
- Secret trap/door passive search radius +1/+2/+3/+4 tiles。
- Secret trap/door passive detection chance +20/+30/+45/+60 percentage points。
- Undying Totem full charge 200/160/120/80 action-value，revive HP 35/45/55/70%。

既有 HP、damage、accuracy、gold、hunger、vision、shop discount、chest bonus、regen 等继续保留。

## 38.3 永久免疫
破界之印第一次生成时从以下常见负面效果随机 1 项永久免疫：
Burning / Poison / Paralysis / Vertigo / Blindness / Cripple / Weakness / Vulnerability / Slow / Charm / Hex。

该选择保存在 Seal 本体 Bundle 中，只有装备 Seal 时通过 BreakthroughBlessing.immunities 生效。
将 ScrollOfUpgrade 用在 Seal 上会：
- 正常消耗升级卷轴；
- 随机切换到与当前不同的一项免疫；
- 不改变 Seal tier，不增加 Item level；
- 如果正在装备，立即重建 Blessing 使新免疫即时生效。

## 38.4 商店价格 UI
Shopkeeper 增加 baseSellPrice(item)，sellPrice(item) 只负责在 base price 上应用 Seal discount。
WndTradeItem 在 Seal 折扣生效时显示：
- 灰色 original price；
- 一条横线覆盖 original price；
- 绿色 Seal discounted price + 折扣百分比；
- Buy 按钮与实际扣款继续使用 discounted price。

## 38.5 药剂/卷轴与法杖
Potion.drink / Scroll.execute 在同步效果应用期间开启 Seal consumable context。
Buff.append/affect/prolong 对 Hero 的正面持续 buff 应用 Seal duration multiplier；常见负面效果不被延长。
Wand.WandCharger 自然恢复 charge 时再乘 Seal wandChargeMultiplier。


# 39. 0.6.6 — 奇迹·回响与创世回响

## 39.1 LV60 最终升格
- Hero 59 -> 60 在 Infinite World 中需要 3600 XP。
- LV30/40/50 仍保留 +15/+25/+40% EXP；到 LV60 后 EXP 加成完全移除。
- 破界之印在 Hero LV60 时不再显示“破界之印-LV60”，正式改名为“奇迹·回响”。
- Miracle Echo / Genesis Echo / Genesis copied layer / Genesis kill-growth stats 都不进入 dynamic mob power profile。

## 39.2 创世回响
奇迹·回响升格时永久获得可见 Buff“创世回响”。
- revivePersists；取下奇迹·回响不会移除创世回响。
- 免疫即死类直接死亡来源。
- 免疫普通外部/强制 teleport；Hero 自己使用 Teleportation Scroll 仍然允许。
- 免控：Paralysis / Roots / Vertigo / Charm / Terror / Amok / Drowsy / Sleep / Slow / Chill / Frost。
- 商店购买价 = 0。
- 每次购买后独立进行无固定上限的 geometric extra-copy roll。
- 每个真实敌人死亡永久 +1 max HP、+1 STR；Infinite World distance despawn 不触发。
- 本版明确不实现“每500行动值自选物品”，也不实现“天降横财”。

## 39.3 奇迹共鸣复制层
奇迹·回响处于装备状态时，创世回响额外复制一整套奇迹·回响被动属性。
- 百分比 multiplier 以两层实际叠加；
- flat bonus（STR、vision、search radius、backpack slots）复制一次；
- item 被取下后复制层即时消失，但创世回响永久能力继续存在。
- Genesis Echo 详情页逐行列出被复制的一整套属性。

## 39.4 背包
- base Backpack: 20 -> 25。
- Seal capacity bonus: LV30 +5 / LV40 +8 / LV50 +12 / Miracle Echo +16。
- Miracle Echo + Genesis linked 时，再复制 +16，所以 LV60 装备时共有 +32 Seal-derived slots。
- unequip 后重新计算容量；若超载，优先把普通非 unique、非 Bag 物品移到 Hero 所在格，再处理其他非 Seal 物品；绝不直接删除。


# 40. 0.6.7 — 奇迹·世界与创世回响权限重构

## 40.1 命名
- LV60 终局装备正式统一名称：`奇迹·世界`。
- 不再使用“奇迹·回响”作为玩家可见名称。
- 去掉“60级经验加成已移除”之类解释性废话。

## 40.2 创世回响常驻权限
创世回响只要已经觉醒，即使取下奇迹·世界也永久存在并真实生效：
- 即死免疫：Hero.die 区分普通伤害致死与直接 instant-death 调用，直接即死被拒绝。
- 被迫传送免疫：普通外部 teleport 被阻止；Hero 自己使用 ScrollOfTeleportation 仍允许。
- 控制免疫：Blindness、Daze、Paralysis、Roots、Vertigo、Cripple、Charm、Terror、Amok、Drowsy、Sleep、Slow、Chill、Frost 通过 Char.isImmune 实际拒绝附加。
- 已经存在的被阻挡控制在创世回响恢复/觉醒时主动清理。
- 商店免费、购买额外复制、击杀永久 +1 HP/+1 STR 保持 0.6.6 行为。

## 40.3 奇迹·世界佩戴联动
只有真正装备 LV60 奇迹·世界时，创世回响临时新增：
- 诅咒免疫：Burning / Poison / Bleeding / Corrosion / Ooze / Weakness / Vulnerable / Degrade / Hex / Doom 显式加入动态 immunity；Hero.add 同时拒绝任何其他 `Buff.buffType.NEGATIVE`，避免未来新负面 Buff 漏网。
- 伤害免疫：Hero.damage 在最前端直接 return；Hero.isInvulnerable 同时返回 true。因此近战、远程、法术、陷阱、饥饿等走标准 damage pipeline 的伤害全部为 0。
- 原有第二套奇迹·世界属性复制层继续存在。
- 取下奇迹·世界后：诅咒免疫、伤害免疫、复制层立刻消失；创世回响本体和其常驻权限不消失。

## 40.4 无限空间
彻底取消破界之印/奇迹·世界的固定背包扩容。
- Backpack 恢复原版 20 格基础容量计算。
- 创世回响觉醒后，根背包容量改为 `max(normalCapacity, items.size()+5)`。
- 因此不论塞入多少物品，逻辑上始终至少还有 5 个空位。
- 该能力来自创世回响本体，所以取下奇迹·世界后仍永久存在。

## 40.5 升级卷轴免疫刷取 UI
WndUpgrade 选择破界之印/奇迹·世界时，窗口底部直接显示：
`当前永久免疫：xxx`
连续使用升级卷轴重刷时不需要退出窗口检查。


## 40.6 无限空间背包 UI
“无限空间”不仅放宽 collect/capacity 逻辑，也扩展 WndBag：
- 创世回响激活且打开根 Backpack 时启用分页；
- 武器/护甲/神器/破界槽/杂项/戒指等装备栏每页固定保留；
- 普通物品与5个保底空位进入可翻页区域；
- 超过一页时窗口底部出现 `< 当前页/总页` 与 `当前页/总页 >`；
- 因此物品数量超过原版固定背包 UI 上限后仍可逐页访问，不会出现“能存进去但看不到”的隐藏物品。

# 41. 0.6.8 — Infinite World 天赋进度修复与说明精简

## 41.1 版本
- versionName：0.6.8
- versionCode：964
- dev：assist-0.6.8-talent-progression
- target stable：assist-0.6.8-stable
- WORLD_GEN_VERSION：17，不变

## 41.2 天赋进度修复
Infinite World 没有原版 Boss 流程，因此不能继续用“第二个/第四个 Boss 掉落物”作为角色天赋的实际解锁门槛。

当前规则：
- 第1、2层天赋继续按原等级规则开放。
- Hero 达到12级时，第3层角色本体天赋直接按等级开放，不再因为尚未选择职业专精而整层隐藏。
- 达到12级且尚未选择职业专精时，自动发放一次天狗面具；玩家使用后自行选择原角色对应的职业专精，并补全该专精的第3层天赋。
- Hero 达到20级且尚未获得护甲技能时，自动发放一次矮人国王的王冠；玩家使用后自行选择原角色对应的护甲技能，随后开放第4层天赋。
- 里程碑道具背包满时也必须进入背包，不得因容量问题遗失。
- 旧存档若已经高于12/20级但仍缺职业专精/护甲技能，会在进入或刷新 Infinite World 时补发缺失选择道具。
- 普通地牢模式保持原版 Boss / 掉落物解锁逻辑，不受本修改影响。
- 不自动替玩家选择职业专精或护甲技能，选择权仍由玩家自己决定。

## 41.3 玩家可见说明精简
- 删除奇迹·世界详情底部重复解释“装备后复制/取下后失效”的整段说明；核心效果已在创世回响详情中直接展示。
- 删除创世回响详情中的开发实现说明“这一整套复制层不会参与怪物动态强度计算”。
- 删除“商店价格层”后面“创世回响本体已使商店免费”的重复括注。
- 动态怪物强度仍然按代码保持原行为：奇迹·世界、创世回响、复制层、击杀永久成长不计入动态强度；只是这类实现细节不再展示给玩家。

# 42. 0.6.9 — 六层成长与第七层创世天赋

## 42.1 版本
- versionName：0.6.9
- versionCode：965
- dev：assist-0.6.9-genesis-talents
- target stable：assist-0.6.9-stable
- WORLD_GEN_VERSION：17，不变

## 42.2 破界随机“永久免疫”移除
破界之印/奇迹·世界不再拥有单独随机抽取的“永久免疫”：
- 物品详情、Buff详情、下一阶段预览均删除该条目；
- 升级卷轴不再用于刷新破界之印免疫；
- 破界祝福不再向 Char.immunities 注入随机状态类；
- 原有异常抗性百分比继续存在，但它只是持续时间/抗性层，不再表现为随机完全免疫。
原因是60级已有常驻创世回响，免疫职责统一由创世回响承担。

## 42.3 创世回响免疫反馈
创世回响真实拒绝状态时增加可见反馈：
- 灼烧/中毒/流血/腐蚀/黏液/麻痹/束缚/致盲/混乱/残废/魅惑/恐惧/狂乱/迟缓/寒冷/虚弱/易伤/降级/诅咒等按类型显示“××免疫”；
- 其他 NEGATIVE Buff 显示“负面状态免疫”；
- 被迫传送显示“传送免疫”；
- 即死显示“即死免疫”；
- 奇迹·世界伤害免疫显示“伤害免疫”。
反馈与真实拦截在同一路径触发，不是单独的装饰文本。

## 42.4 奇迹·世界佩戴新增机制秒杀与天赋全满
只要 LV60 奇迹·世界正在破界槽装备且创世回响存在：
- Hero 的有效普通武器攻击会在原攻击结算后对仍存活的敌对目标执行强制处决；
- 处决会调用目标死亡流程，若特殊死亡机制仍使目标存活，则执行最终 destroy，确保机制怪也能被终结；
- 第1至第6层已初始化天赋的 pointsInTalent 直接按各自 maxPoints 返回，因此所有真实读取天赋等级的效果都按满级数值计算；
- UI中的前六层按钮也显示为满级，取下奇迹·世界后立即恢复真实已分配点数；
- 第7层不受该“全满”联动影响。

## 42.5 第5层与第6层
Infinite World 新增两层通用成长天赋：
- 第5层（30级进入展示、31级开始获得点数，42级前可自然点满）
  - 超凡生命：每级 +10% 最大生命，3级；
  - 超凡力量：每级 +10% 普通攻击伤害，3级；
  - 超凡专注：每级 +15% 命中，3级；
  - 超凡反射：每级 +15% 闪避，3级。
- 第6层（42级进入展示、43级开始获得点数，59级前可自然点满）
  - 超越之力：每级 +2 力量，4级；
  - 超越疾行：每级 +15% 移速，4级；
  - 超越视界：每级 +2格正常探索视野，3级；
  - 超越守御：每级 -8% 受到伤害，3级；
  - 超越再生：每点正向行动值恢复约 0.25% 最大生命/级，3级。
第5层总最大点数12，第6层总最大点数17，与对应自然成长区间点数完全匹配。

## 42.6 第7层创世天赋
第7层只在60级且创世回响已觉醒后显示。四项均只有1级，且存档层面只能同时拥有其中一项：
1. 超距离攻击：视野内敌人可直接点击强制秒杀；已经显示在地图上的 Heap/宝箱可无距离拾取或开启。
2. 超距离传送：默认地图操作下，对已 visited 且可落脚的格子在0.28秒窗口内双击即可瞬移；单击在窗口结束后仍按普通移动处理；无冷却。
3. 超极限施法：法杖对当前屏幕可点选位置施法，敌对目标在法杖效果后被强制秒杀；正面药剂/卷轴持续效果至少999行动值。
4. 超极限好运：打开普通/上锁/水晶宝箱后产生大爆发；保留原箱内容并额外至少生成10件物品，额外件数和可堆叠数量均使用无固定上限的连续随机；可堆叠物品单组至少10；武器/护甲/戒指/法杖直接设为+120；物品在角色周围9×9可落脚区域散开。

第7层选择后附加独立可见 Buff“创世权能”，Buff名称/说明直接跟随当前第7层天赋。第7层面板使用重选按钮清空当前选择后可重新选择；取下奇迹·世界不会清除第7层选择或Buff，因为第7层归属于常驻创世回响。

# 44. 0.6.13 — 安全滚动与超极限施法双击AOE

## 44.1 版本
- versionName：0.6.13
- versionCode：969
- dev：assist-0.6.13-scroll-aoe
- target stable：assist-0.6.13-stable
- WORLD_GEN_VERSION：17，不变

## 44.2 0.6.10/0.6.11 闪退根因
前两次滚动改造把 ScrollPane.setRect() 放在 add(scrollPane) 之前调用。ScrollPane.layout() 会立即通过 camera() 获取父窗口相机；此时 ScrollPane 还没有 parent，camera() 返回 null，因此在打开背包或 Buff 详情窗口时直接触发运行时空指针。0.6.12 通过完整恢复 0.6.9 的 WndBag/WndInfoBuff 验证了问题确实来自这条 UI 构造路径。

0.6.13 重新加入滚动时必须遵守：
1. new ScrollPane(content)
2. add(scrollPane)
3. scrollPane.setRect(...)
禁止再次反转 2/3。

## 44.3 Buff 长说明
- WndInfoBuff 宽度在 120~160 UI 单位之间按手机屏幕自适应。
- 正文超过安全高度时只让正文区域上下滚动，标题固定。
- 创世回响佩戴奇迹·世界后的长说明可完整上下滑到末尾。

## 44.4 背包连续滚动
- 删除创世回响根背包分页逻辑。
- 全部装备槽、物品、以及无限空间最后至少5个空槽位于一张连续纵向网格中。
- 使用标准 ScrollPane；ScrollPane 先加入 WndBag 再 setRect，避免 0.6.10/0.6.11 的空相机崩溃。
- 物品格原生 PointerArea 在滚动网格中关闭，由 ScrollPane 的标准点击分派调用物品格 onClick，避免滚动控制器与物品按钮争抢手势。
- 手机端点击物品仍可打开 WndUseItem；选择器模式仍可选择物品。

## 44.5 超极限施法双击法杖
- 第7层选择“超极限施法”后，快捷栏中的法杖支持0.35秒双击。
- 第一次点击仍进入正常法杖选点模式，因此单击玩法保持原逻辑。
- 0.35秒内再次点击同一根快捷栏法杖：立即取消选点，并发动无需方向的当前屏幕全屏AOE。
- AOE目标定义：当前屏幕中实际可见、存活、非友军的 Mob；使用 sprite.isVisible() 随相机缩放变化，因此缩放越小，实际覆盖的地图区域越大。
- AOE对每个目标走 GenesisEcho.forceSlay()，继续遵守创世权能的机制秒杀原则。
- 整次AOE只调用一次 wandUsed()，因此只消耗这根法杖一次正常施法的充能与1次施法行动时间，不按怪物数量重复扣充能。
- 原有单目标法杖施法仍保留 onUltraWandZap 的命中后秒杀。
- 正面药剂/卷轴至少999行动值的规则不变。

# 45. 0.6.14 — 滚动窗口排版对齐修复

- versionName：0.6.14
- versionCode：970
- dev：assist-0.6.14-layout-fix
- target stable：assist-0.6.14-stable

0.6.13 已不再闪退，但 WndInfoBuff 与 WndBag 的滚动内容整体偏移到窗口右下方，形成大面积空白。截图可见：
- 背包标题正常位于左上，物品网格却从窗口中下部/右侧开始绘制；
- 创世回响标题正常，正文却从窗口中下部/右侧开始，且被右边界裁切。

根因：0.6.13 虽然保证了 ScrollPane 已先 add 到父窗口，但仍在 Window.resize() 之前调用 ScrollPane.setRect()。setRect() 会立刻布局并计算内容相机屏幕坐标，而后续 resize() 会重新居中整个 Window；ScrollPane 的内容相机仍保存旧窗口位置，于是可视内容和窗口外框产生整体错位。

0.6.14 统一改为：
1. new ScrollPane(content)
2. add(scrollPane)
3. resize(windowWidth, windowHeight) —— 确定窗口最终尺寸与居中位置
4. scrollPane.setRect(...) —— 在最终窗口坐标下布局内容相机

此顺序同时应用于 WndInfoBuff 和 WndBag。禁止再在 resize() 前对窗口内 ScrollPane 做最终 setRect()。

# 46. 0.6.15 — 奇迹秒杀权能与超极限施法继承关系修正

- versionName：0.6.15
- versionCode：971
- dev：assist-0.6.15-execution-authority
- target stable：assist-0.6.15-stable

## 语义修正
“机制秒杀”属于“奇迹·世界已佩戴 + 创世回响存在”的联动权能，不属于第7层“超极限施法”本身。

奇迹·世界佩戴期间：
- Hero 成功命中的近战、徒手、投掷/远程武器攻击会触发 GenesisEcho.tryMiracleExecute；
- 普通单目标法杖命中也会触发同一机制秒杀，不要求第7层选择超极限施法；
- 特殊死亡机制怪仍通过 forceSlay 的 die -> destroy 兜底终结。

超极限施法自身：
- 双击快捷栏法杖，对当前屏幕所有可见敌对 Mob 结算一次真实AOE伤害；
- DamageWand 使用当前法杖 min/max 形成AOE伤害；非直接伤害型法杖使用随法杖等级增长的创世AOE伤害包；
- 整次AOE只扣一次法杖正常充能与一次施法行动时间；
- 未佩戴奇迹·世界时，AOE只造成伤害，不自动秒杀；
- 已佩戴奇迹·世界时，每个AOE目标在伤害后额外继承创世回响的机制秒杀。

# 47. 0.6.16 — 超距离传送与移动卡顿优化

- versionName：0.6.16
- versionCode：972
- dev：assist-0.6.16-teleport-performance
- target stable：assist-0.6.16-stable

## 优化点
1. 删除超距离传送中的重复工作：
   - ScrollOfTeleportation.appear(hero, cell) 本身会调用 Hero.move(..., false)；
   - Hero.move 已经执行 occupyCell 和 InfiniteWorldLevel.recordHeroMove；
   - 旧实现随后又手动 occupyCell、recordHeroMove 一次，造成重复逻辑；
   - Dungeon.observe() 本身已经执行必要 fog 更新，旧实现又额外全局 GameScene.updateFog() 一次。
   0.6.16 全部去重，只保留一次位置更新、一次世界记录和一次 observe。

2. 传送后清除旧导航缓存：
   - path = null；
   - curAction / lastAction 清空；
   - walkingToVisibleTrapInFog 清空；
   - 避免传送前的长路径在新位置触发重新寻路或恢复旧移动目标。

3. 缩短创世传送视觉淡入：
   - 普通传送卷轴仍保持0.4秒；
   - 超距离传送使用0.12秒淡入，仅改变视觉响应，不改变规则与无冷却设定。

4. Infinite World 每步维护节流：
   - 后室信息与商人发现只在英雄真正进入新 chunk 时执行，不再每走1格重复扫描；
   - 怪物裁剪改为每移动约4个世界格或跨 chunk 时检查；
   - 进度门槛/奇迹装备确保逻辑仍保留，不改变玩法。

5. 传送后的流式地图宽限：
   - 如果传送落点位于普通流式触发边带（24格安全边带）内，下一步不立刻重建168×168窗口；
   - 临时使用8格硬安全边带，玩家向地图内部移动时不会触发重建；
   - 如果继续向真正地图边缘移动，达到8格边带时仍会正常重建窗口，保证不会走出加载区；
   - 一旦回到正常安全区或完成一次重建，宽限自动解除。

# 48. 0.6.17 — 八荒·亘古元敕与出生物资

## 48.1 版本
- versionName：0.6.17
- versionCode：973
- dev：assist-0.6.17-eternal-edict
- target stable：assist-0.6.17-stable
- WORLD_GEN_VERSION：17，不变
- 内部 Java 类名 GenesisEcho 保留，仅作为存档/代码兼容标识；玩家可见正式名称统一为“八荒·亘古元敕”。

## 48.2 亘古·本源权能
60级觉醒后永久存在，与奇迹·世界是否佩戴无关：
- 元墟·绝殛：所有伤害在 Hero.damage 入口直接拒绝；Hero.isInvulnerable 同步为真；直接即死被拒绝；HP 每次权能 tick 回满，受伤路径也立即回满。状态栏生命显示为“∞/∞”。
- 万法·不羁：Blindness、Daze、Paralysis、Roots、Vertigo、Cripple、Charm、Terror、Amok、Drowsy/Sleep、Slow、Chill、Frost 等控制类状态无法附加；被迫传送继续被拒绝。拒绝/伤害阻断时使用“××敕免”反馈。
- 无垠·藏墟：根背包继续维持“当前物品数量 + 至少5个空槽”的自适应容量。
- 衡世·契权：商店实际免费；原有购买时无固定上限的连续额外同类商品随机继续保留。
- 纵横八荒：每次真实敌对击杀永久最大生命+10、力量+1。旧存档按累计力量成长数自动把旧的+1生命/杀迁移为至少+10生命/杀。
- 通识古今：成功获得物品并进入 Hero 所属背包后，如果尚未鉴定，立即调用 Item.identify()；觉醒时也会补鉴定角色当前持有的未鉴定物品。

## 48.3 奇迹·世界联动权能
只有奇迹·世界实际佩戴时生效：
- 万厄·归寂：原有显式 Burning/Poison/Bleeding/Corrosion/Ooze/Weakness/Vulnerable/Degrade/Hex/Doom 免疫继续保留，同时 Hero.add 会拒绝其他 NEGATIVE Buff。
- 终末·裁决：成功的近战/徒手/投掷/远程武器攻击和法杖伤害都会触发机制处决；forceSlay 先走目标 die(hero)，特殊死亡机制仍存活时再 destroy 兜底。
- 无限·天资：第1~6层已初始化天赋的 pointsInTalent 按各自 maxPoints 返回；第7层读取真实选择，不被联动全满。
- 寰宇·映射：继续通过 BreakthroughCertificate.sealCopies/effective* 完整叠加奇迹·世界既有属性/能力数值层。

## 48.4 第7层正式名称
- GENESIS_TELEPORT → 无界遁诰：原超距离双击传送，规则不变。
- GENESIS_REACH → 寰墟指殛：原视野内点击处决、远距拾取/开箱，规则不变。
- GENESIS_SPELLCAST → 元级肇法：所有法杖伤害目标都会机制秒杀，即使奇迹·世界未佩戴；双击快捷栏同一法杖仍发动当前屏幕无方向全屏AOE，并对所有命中目标敕杀；正面药剂/卷轴效果至少999行动值。
- GENESIS_FORTUNE → 万运隆敕：原宝箱大爆发逻辑不变。
- 第7层仍四选一、可重置重选、独立于奇迹·世界。

## 48.5 出生点固定物资
Infinite World 世界出生点绝对坐标仍为 (12,12)。首次初始化时，在出生点附近保证布置：
1. 一个普通宝箱：5份随机食物 + 5张随机卷轴 + 5瓶随机药水。
2. 一把水晶钥匙。
3. 一个水晶宝箱：1枚随机戒指 + 1根随机魔杖。

实现约束：
- 生成使用独立确定性随机种子，不扰动普通 Generator 的长期掉落序列。
- 两个出生宝箱的内容初始都不会直接暴露具体身份：食物使用出生物资专用“未知食物”状态；卷轴/药水遵循原版未知类型；戒指/魔杖保持未鉴定等级/诅咒信息。食物在鉴定或食用后恢复真实名称与贴图。
- 宝箱、钥匙及宝箱剩余内容都有世界坐标/对象状态/内容持久化，不会因7×7流式窗口重建复制。
- 物资格优先寻找出生点9格半径内的天然可用地块；极端地图没有3个可用地块时会强制预留附近普通地面，保证物资绝不缺失。
- 八荒·亘古元敕尚未觉醒时，物品保持原有未鉴定状态；觉醒并实际获得物品后，通识古今立即完成鉴定。


## Assist 1.0.2 — Talent runtime audit

- Version: 1.0.2 / versionCode 998. WORLD_GEN_VERSION remains 22.
- Branch: assist-1.0.2-talent-audit.
- T5/T6 runtime audit confirms real hooks for Ascendant Force/Focus/Reflex and Transcendent Strength/Speed/Guard/Regen.
- Ascendant Vitality now calls Hero.updateHT(true) immediately when a point is spent, so both HT and the gained current HP delta update on the same tap.
- Transcendent Vision was already implemented but was applied twice: Dungeon.observe() wrote +2/point into Hero.viewDistance, then Level.updateFieldOfView() added another +2/point. Hero.viewDistance now remains the base value; Level is the single FOV authority, while Dungeon.observe() mirrors the effective radius only for fog refresh bounds.
- Dungeon.observe() fog bounds now also mirror Eye of Newt and Breakthrough Certificate vision bonuses, preventing outer visible cells from retaining stale fog.
- Genesis Echo awakening now recalculates HT and FOV after the buff is attached. This is required because a linked level-60 Miracle World instantly enables full T1-T6 mastery and the second certificate layer.
- T7 audit confirms live implementations for Genesis Reach, Teleport, Spellcast and Fortune. Fortune minimum 10 bonus items / 9x9 spread / stack quantity >=10 / weapon-armor-ring-wand +120 and Spellcast 0.35s wand double-tap match their descriptions.

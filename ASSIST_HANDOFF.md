# Shattered Pixel Dungeon · Assist — 项目交接总说明

> **给任何新的 ChatGPT / Codex / 维护者：先读本文件，再读 `docs/assist/` 下其余文档。**
>
> 本文件的目标是避免因为聊天长度、换对话、换模型或隔了一段时间后，后续修改偏离当前项目方向。
> 这里记录的是“当前真实项目状态、不可破坏的约束、现在做到哪里、下一步做什么”。
>
> 如果本文档与代码冲突，以**最新稳定分支的实际代码**为准，并优先检查 `assist-0.4.2-stable`。
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
- 当前最新稳定版本：**0.4.2**
- 当前 versionCode：**943**
- 当前最新稳定分支：`assist-0.4.2-stable`
- 当前稳定代码 SHA：`21eb31d97ba0bf46b920a6f622a587ea97c560f0`
- 当前对应开发分支：`assist-0.4.2-secrets`
- 当前无限世界生成器版本：**WORLD_GEN_VERSION = 8**

用户的核心目标不是做一个“原版小改版”，而是逐步把一个额外模式做成：

> **真正水平无限、随机、多环境、多房间、多互动，最终加入怪物生态与 Boss 的 Shattered Pixel Dungeon 无限世界。**

---

## 2. 新对话接手时必须做的事

新的 ChatGPT 开始工作时，不要先问用户“项目在哪”“现在做到哪”。直接：

1. 打开仓库 `flqin5261-ctrl/shattered-pixel-dungeon`。
2. 读取本文件 `ASSIST_HANDOFF.md`。
3. 读取：
   - `docs/assist/CHANGELOG_DETAILED.md`
   - `docs/assist/ARCHITECTURE_AND_INVARIANTS.md`
   - `docs/assist/BUG_HISTORY_AND_FIXES.md`
   - `docs/assist/ROADMAP_AND_TEST_CHECKLIST.md`
4. 检查最新稳定分支 `assist-0.4.2-stable` 和最新开发分支。
5. 修改前优先从最新稳定分支创建新的开发分支，例如：
   - `assist-0.4.3-xxxxx`
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
  - 从 Base1 到 0.4.2 的逐版本历史
- `docs/assist/ARCHITECTURE_AND_INVARIANTS.md`
  - Infinite World 架构、坐标、存档、渲染、对象状态
- `docs/assist/BUG_HISTORY_AND_FIXES.md`
  - 卡死、水贴图、黑屏、Fog、门贴图等问题的原因和错误方案
- `docs/assist/ROADMAP_AND_TEST_CHECKLIST.md`
  - 当前待测、下一步功能、怪物/Boss接入前检查清单

---

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

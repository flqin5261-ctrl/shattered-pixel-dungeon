# 新对话接手用提示词

如果用户因为聊天长度限制开启新对话，可以直接把下面这段发给新的 ChatGPT。

---

我正在继续开发一个私人使用的 Shattered Pixel Dungeon Assist MOD。

仓库：

`flqin5261-ctrl/shattered-pixel-dungeon`

请不要让我重新解释项目历史。你先通过 GitHub 读取以下文件：

1. `ASSIST_HANDOFF.md`
2. `docs/assist/PROJECT_STATE.json`
3. `docs/assist/CHANGELOG_DETAILED.md`
4. `docs/assist/ARCHITECTURE_AND_INVARIANTS.md`
5. `docs/assist/BUG_HISTORY_AND_FIXES.md`
6. `docs/assist/ROADMAP_AND_TEST_CHECKLIST.md`

然后检查最新 stable 分支和当前开发分支，再接手工作。

当前已知基线：

- 最新稳定版：0.4.3
- versionCode：944
- stable：`assist-0.4.3-stable`
- 对应代码 release SHA：
  `a513fc1a099d272c356c3127f874c3dd861e2799`
- Infinite World Generator：V9
- 包名：
  `com.shatteredpixel.shatteredpixeldungeon.assist`
- 固定签名不能改
- versionCode 每个可安装版本必须递增
- 历史 stable 分支不能覆盖

当前主要项目是“无界地牢 / Infinite World”。

目标：

- 真正水平无限
- 随机且怪诞
- 房间大小有明显差异
- 地图不要空
- 大量原版可互动内容
- 多环境混合
- 一房间一环境也允许
- 探索/修改/箱子/植物/陷阱状态永久保存
- 最终加入怪物生态和 Boss

已经稳定的底层包括：

- 24×24 Chunk
- 7×7 active window
- 168×168 local map
- 绝对 world coordinates
- streaming hysteresis
- Hero motion complete 后才能 shift
- terrainOverrides
- visited/mapped persistence
- object/chest states
- VBO synchronous flush
- 原版 water backdrop
- 1.8x minimum zoom
- safe teleport policy

请特别注意历史严重 Bug：

- 过早 streaming 会让 Hero 原地走路卡死
- 没 hysteresis 会 A/B 地图反复切
- 只 dirty VBO、不 updateGLData 会出现巨大水/黑地图
- 不能永久关 water backdrop
- static mixed-theme overlay 曾导致解锁后旧锁门贴图仍残留
- 动态门现在必须由原版动态 Tilemap 绘制

0.4.3 当前内容：

- 普通门房、银钥匙锁门、Secret Door 隐藏房
- 植物园、金币园、卷轴房、药剂房、食物房、钥匙房、武器库、水晶宝库、陷阱工坊、综合藏宝室
- Sewers/Prison/Caves/City/Halls 多环境混合与房间级 tileset
- V9 世界级 Infinite Backbone：允许普通支路死路，但保证原点主连通分量真正无限
- 三类 5×5 Chunk 阈限异常宏区：重复回廊、静水廊厅、无尽大厅
- 异常区可收藏 InfiniteWorldNote
- Guide 中独立“无界异境记录”栏目
- V9 高价值物资/普通箱子/野外钥匙显著降密
- V9 SECRET_TRAP 大幅降密
- 每 3×3 Chunk macro 有确定性 Secret Room 候选，靠近秘密门被动发现率提高
- Bomb 炸墙
- Pickaxe 挖矿
- Chains
- safe teleport
- 全球通用 IronKey/CrystalKey 逻辑
- 水晶宝箱
- 多区域音乐

当前下一步方向：

1. 先根据 0.4.3 实机测试调整 Infinite Backbone、Secret Room、异常宏区和资源曲线。
2. 需要继续加房间时，优先添加低奖励但高辨识度的原版风格房间：
   - Magic Well
   - Runestone
   - Library
   - Fire/Barricade
   - Mine
   - Cell Block
   - Ritual/Statue
3. 做 room/chunk visual overlay 局部刷新，解决 Secret Door 被发现后主题延迟、炸墙后静态 overlay 可能残留。
4. 新增异常空间时直接扩展现有 Infinite World Field Notes，不要重新造 Journal 系统。
5. 地图和经济曲线稳定后开始怪物：
   - deterministic spawn
   - mob world state
   - dead state persistence
   - chunk unload/reload
   - active radius
6. 普通怪稳定后再做 Boss landmark 和 Boss persistence。

用户偏好：

- 不需要每改一点就停下来汇报
- 可以直接做判断并继续
- 做完后总结
- 最终要给真正可安装 APK
- GitHub Actions 没完成时不要让我过一会再回来问；你应自己继续检查、失败则修、成功后下载 artifact、解压 APK，再发给我
- 最好 APK + ZIP 都给

现在请读取仓库里的交接文档并直接继续当前任务。


另外有一条强制维护规则：

**每次完成任何功能修改、Bug 修复、参数调整、生成器变化、架构变化或路线变化后，必须在本次任务结束前同步更新 GitHub 里的交接文档。不能只改代码不记记录，也不能等用户提醒。代码修改完成但文档未更新时，任务视为未完成。**


## 每次接手时的效率规则（必须遵守）

不要因为看到有完整交接文档，就在每一次普通小改前把所有历史从头读一遍。

对于参数调整、单点 Bug、UI 微调、文案修正、小范围概率调整、单一房间/物品/交互修改等普通任务：

**优先直接查看当前代码，只在必要时查看近期版本或与问题直接相关的记录。**

只有在 Streaming、存档、世界坐标、Generator 大改、Render/Fog/VBO、跨版本兼容、同一 Bug 反复修不掉、代码与记录冲突、发布基础设施变化、怪物/Boss persistence 等核心架构问题上，才需要完整回溯历史资料。

默认流程：

> **直接改 → 构建/验证 → 更新必要记录 → 上传/稳定分支/安装包**

但注意：减少前置阅读不代表可以省略后置记录。只要项目发生实际修改，任务结束前仍必须及时更新 GitHub 交接记录。

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

- 最新稳定版：0.4.2
- versionCode：943
- stable：`assist-0.4.2-stable`
- 对应代码 release SHA：
  `21eb31d97ba0bf46b920a6f622a587ea97c560f0`
- Infinite World Generator：V8
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

0.4.2 当前内容：

- 普通门房
- 银钥匙锁门
- Secret Door 隐藏房
- 植物园
- 金币园
- 卷轴房
- 药剂房
- 食物房
- 钥匙房
- 武器库
- 水晶宝库
- 陷阱工坊
- 综合藏宝室
- Sewers/Prison/Caves/City/Halls 多环境混合
- 房间级 tileset
- Bomb 炸墙
- Pickaxe 挖矿
- Chains
- safe teleport
- 全球通用 IronKey/CrystalKey 逻辑
- 水晶宝箱
- 多区域音乐

当前下一步方向：

1. 继续参考原版 Room 系统增加更多小/中型真正房间：
   - Magic Well
   - Runestone
   - Library
   - Pool
   - Fire/Barricade
   - Mine
   - Trap Corridor
   - Cell Block
   - Maze
   - Ritual/Statue
2. 做 room/chunk visual overlay 局部刷新，解决 Secret Door 被发现后主题延迟、炸墙后静态 overlay 可能残留。
3. 地图内容足够后开始怪物：
   - deterministic spawn
   - mob world state
   - dead state persistence
   - chunk unload/reload
   - active radius
4. 普通怪稳定后再做 Boss landmark 和 Boss persistence。

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

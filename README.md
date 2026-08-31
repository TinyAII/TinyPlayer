# TinyPlayer 整合插件

一 jar 打尽 11 大模块的 Minecraft 服务器整合插件。登录、TPA、回城、交易、回家、返回、传送点、私聊、经济、神权管理、领地保护。全中文界面，中英双语命令，零依赖。

## 功能模块

| 模块 | 功能 |
|---|---|
| 🔐 登录 | 注册/登录、未登录锁定、30秒超时踢出、基岩版免登录、登录后返回原位 |
| 🔄 TPA 传送 | /传送 /传这里 /同意 /拒绝，聊天可点击按钮，拒绝并屏蔽8分钟 |
| 🏰 回城 | 传送到主城，屏幕倒计时，移动打断 |
| 🤝 交易 | 双方共享面板，确认倒计时，物品直接进背包，关闭归还 |
| 🏠 回家 | 多家园、主家、公共家、邀请参观、GUI 家列表、吟唱传送 |
| 🔙 返回 | 回到上次传送前/死亡位置 |
| 📍 传送点 | 管理员创建公共传送点，GUI 传送 |
| 💬 私聊 | 私人消息 + 回复 |
| 💰 经济 | 金币余额、转账、排行、OP 管理，对接自家 Economy/Vault |
| 👑 神权管理 | /神权 玩家/经济/家/传送点/传送/拉/密码/登录/交易/公告，操作日志 |
| 🌍 领地 | 三种圈地、Flag 权限、全局面板、子领地、模板、成员、欢迎语 |

## 领地模块特色

- 三种圈地：木锄头选区（火焰粒子预览）/ 指令圈地（完整3D坐标）/ 半径圈地
- Flag 权限：12 环境 + 10 权限 flag，外人默认全关，领地级覆盖
- 纳秒级空间索引：四象限分区 + 方块级 LRU 缓存
- 全局面板：全领地权限成员 + 领地管理列表 + 领地详情页（领地设置/子领地/管理员设置/欢迎语+退出语/成员/传送）
- 边界粒子：火把火光，六面或 12 边线可选，进出领地 2.3 秒自动消失
- 进出提示：ActionBar 欢迎语/欢送语，可自定义
- 子领地/模板（家园/战争/和平）/扩展/成员管理/清除选区
- 经济联动：创建/扩展领地扣费（自家 Economy → Vault → 免费）

## 安装

1. 下载 jar 放入 `plugins/` 目录
2. 重启服务器（或面板 reload）
3. 启动日志显示 TinyAII 横幅 + 各模块开/关状态

> 需要 Java 17+，支持 Paper/Spigot 1.16 ~ 26.2。零依赖，无需任何前置。

## 配置

`plugins/TinyPlayer/config.yml` —— 每个模块独立 `enabled` 开关（默认全开）：

- `auth.enabled` / `tpa.enabled` / `spawn.enabled` / `trade.enabled`
- `home.enabled` / `back.enabled` / `warp.enabled` / `msg.enabled`
- `economy.enabled`（provider: auto=自家Economy→Vault→内置）
- `claim.enabled`（领地模块）
- `admin.enabled`（神权管理）

领地模块详细配置见 `claim` 段（圈地限制/边界样式/flag 默认/模板/经济）。

## 主要命令（中英双语）

- 登录：`/登录` `/login`、`/注册` `/register`
- TPA：`/传送` `/tpa`、`/同意` `/tpy`、`/拒绝` `/tpn`
- 回城：`/回城` `/spawn`
- 交易：`/交易` `/trade`
- 回家：`/家` `/home`、`/sethome`、`/delhome`
- 返回：`/返回` `/back`
- 传送点：`/传送点` `/warp`
- 私聊：`/私聊` `/msg`、`/回复` `/r`
- 经济：`/金币` `/money`、`/pay`
- 领地：`/领地` `/claim`（创建/圈地/半径/面板/信息/传送/成员/flag/子/模板/扩展/欢迎语/退出语/清除选区）
- 神权：`/神权` `/admin`

## 兼容性

- Paper / Spigot 1.16 ~ 26.2，Java 17+
- 零依赖，不装任何前置也能跑
- 经济联动：装了自家 Economy 或 Vault 自动使用，都没装则内置经济
- 基岩版玩家（Geyser/Floodgate）自动免登录

---

# TinyPlayer - All-in-One Plugin Suite

One jar with 11 modules: Auth, TPA, Spawn, Trade, Home, Back, Warp, Private Message, Economy, Admin (God), Claim. Full Chinese UI, bilingual commands, zero dependency.

## Modules

- **Auth**: register/login, lock unlogged players, 30s timeout kick, Bedrock auto-bypass, return to last location after login
- **TPA**: /tpa /tpahere /tpy /tpn, clickable chat buttons, deny+block 8min
- **Spawn**: teleport to spawn with countdown, cancel on move
- **Trade**: shared panel, confirm countdown, items to inventory, refund on close
- **Home**: multiple homes, main home, public home, invite visit, GUI list
- **Back**: return to last teleport/death location
- **Warp**: admin-created public teleport points, GUI menu
- **Private Message**: /msg /reply
- **Economy**: balance, transfer, top list, admin manage, own Economy/Vault integration
- **Admin (God)**: /admin player/economy/home/warp/tp/summon/password/kick/trade/broadcast, operation log
- **Claim**: 3 claim methods, flag permissions, global panel, sub-claims, templates, members, welcome messages

## Claim Module Features

- 3 claim methods: wooden hoe selection (flame particle preview) / command coords (full 3D) / radius
- Flag system: 12 env + 10 privilege flags, strangers blocked by default
- Nanosecond spatial index: quadrant sectors + block LRU cache
- Global panel: global admins + claim list + detail page (settings/sub-claims/admin/welcome+leave/members/teleport)
- Boundary particles: torch flame, six-face or 12-edge, auto-hide in 2.3s
- Enter/Leave ActionBar notifications
- Sub-claims/templates (home/war/peace)/expand/member management/clear selection
- Economy: claim/expand cost (own Economy → Vault → free)

## Install

1. Put jar into `plugins/`
2. Restart server (or panel reload)
3. Startup log shows TinyAII banner + module states

> Java 17+, Paper/Spigot 1.16 ~ 26.2. Zero dependency.

## License

MIT License - free, open source. TinyAII brand banner preserved.

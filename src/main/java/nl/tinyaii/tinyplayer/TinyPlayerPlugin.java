package nl.tinyaii.tinyplayer;

import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;

public class TinyPlayerPlugin extends JavaPlugin {

    private Messages messages;

    @Override
    public void onEnable() {
        // TinyAII 品牌横幅 —— 必须在所有初始化逻辑之前输出（与 AutoBackup 完全一致）
        getLogger().info(" _____ _                _    ___ ___");
        getLogger().info("|_   _(_)_ __  _   _   / \\  |_ _|_ _|");
        getLogger().info("  | | | | '_ \\| | | | / _ \\  | | | |");
        getLogger().info("  | | | | | | | |_| |/ ___ \\ | | | |");
        getLogger().info("  |_| |_|_| |_|\\__, /_/   \\_\\___|___|");
        getLogger().info("               |___/");
        getLogger().info("TinyPlayer 整合插件 v" + getDescription().getVersion() + " - TinyAII 出品");

        saveDefaultConfig();
        messages = new Messages(this);

        // ===== 登录模块（开关 auth.enabled）=====
        if (getConfig().getBoolean("auth.enabled", true)) {
            authManager = new nl.tinyaii.tinyplayer.auth.AuthManager(this);
            authManager.load();
            getServer().getPluginManager().registerEvents(new nl.tinyaii.tinyplayer.auth.AuthListener(this), this);
            getCommand("注册").setExecutor(new nl.tinyaii.tinyplayer.auth.AuthCommand(this));
            getCommand("登录").setExecutor(new nl.tinyaii.tinyplayer.auth.AuthCommand(this));
            getLogger().info("[模块] 登录：开启");
        } else {
            getCommand("注册").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&c登录模块已关闭。")); return true; });
            getCommand("登录").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&c登录模块已关闭。")); return true; });
            getLogger().info("[模块] 登录：关闭");
        }

        // ===== TPA 传送模块（开关 tpa.enabled）=====
        if (getConfig().getBoolean("tpa.enabled", true)) {
            tpaManager = new nl.tinyaii.tinyplayer.tpa.TpaManager(this);
            getServer().getPluginManager().registerEvents(new nl.tinyaii.tinyplayer.tpa.TpaListener(this), this);
            getCommand("传送").setExecutor(new nl.tinyaii.tinyplayer.tpa.TpaCommand(this));
            getCommand("传这里").setExecutor(new nl.tinyaii.tinyplayer.tpa.TpaCommand(this));
            getCommand("同意").setExecutor(new nl.tinyaii.tinyplayer.tpa.TpaCommand(this));
            getCommand("拒绝").setExecutor(new nl.tinyaii.tinyplayer.tpa.TpaCommand(this));
            getLogger().info("[模块] TPA传送：开启");
        } else {
            getCommand("传送").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&cTPA传送模块已关闭。")); return true; });
            getCommand("传这里").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&cTPA传送模块已关闭。")); return true; });
            getCommand("同意").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&cTPA传送模块已关闭。")); return true; });
            getCommand("拒绝").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&cTPA传送模块已关闭。")); return true; });
            getLogger().info("[模块] TPA传送：关闭");
        }

        // ===== 回城模块（开关 spawn.enabled）=====
        if (getConfig().getBoolean("spawn.enabled", true)) {
            spawnCommand = new nl.tinyaii.tinyplayer.spawn.SpawnCommand(this);
            getCommand("回城").setExecutor(spawnCommand);
            getServer().getPluginManager().registerEvents(new nl.tinyaii.tinyplayer.spawn.SpawnListener(this), this);
            getLogger().info("[模块] 回城：开启");
        } else {
            getCommand("回城").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&c回城模块已关闭。")); return true; });
            getLogger().info("[模块] 回城：关闭");
        }

        // ===== 交易模块（开关 trade.enabled）=====
        if (getConfig().getBoolean("trade.enabled", true)) {
            tradeManager = new nl.tinyaii.tinyplayer.trade.TradeManager(this);
            tradeHandler = new nl.tinyaii.tinyplayer.trade.TradeHandler(this);
            getServer().getPluginManager().registerEvents(new nl.tinyaii.tinyplayer.trade.TradeListener(this), this);
            getServer().getPluginManager().registerEvents(tradeHandler, this);
            getServer().getPluginManager().registerEvents(new nl.tinyaii.tinyplayer.trade.TradeRequestListener(this), this);
            getCommand("交易").setExecutor(new nl.tinyaii.tinyplayer.trade.TradeCommand(this));
            // 交易完成倒计时检查（每秒）
            getServer().getScheduler().runTaskTimer(this, () -> tradeHandler.tickCompletions(), 20L, 20L);
            getLogger().info("[模块] 交易：开启");
        } else {
            getCommand("交易").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&c交易模块已关闭。")); return true; });
            getLogger().info("[模块] 交易：关闭");
        }

        // ===== 回家系统模块（开关 home.enabled）=====
        if (getConfig().getBoolean("home.enabled", true)) {
            homeManager = new nl.tinyaii.tinyplayer.home.HomeManager(this);
            homeManager.load();
            homeTeleport = new nl.tinyaii.tinyplayer.home.TeleportTask(this);
            getServer().getPluginManager().registerEvents(new nl.tinyaii.tinyplayer.home.HomeListener(this), this);
            getServer().getPluginManager().registerEvents(new nl.tinyaii.tinyplayer.home.gui.HomeGuiListener(this), this);
            getCommand("家").setExecutor(new nl.tinyaii.tinyplayer.home.command.HomeCommand(this));
            getCommand("sethome").setExecutor(new nl.tinyaii.tinyplayer.home.command.HomeCommand(this));
            getCommand("delhome").setExecutor(new nl.tinyaii.tinyplayer.home.command.HomeCommand(this));
            getLogger().info("[模块] 回家：开启");
        } else {
            getCommand("家").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&c回家系统已关闭。")); return true; });
            getCommand("sethome").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&c回家系统已关闭。")); return true; });
            getCommand("delhome").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&c回家系统已关闭。")); return true; });
            getLogger().info("[模块] 回家：关闭");
        }

        // ===== 返回模块（开关 back.enabled）=====
        if (getConfig().getBoolean("back.enabled", true)) {
            backCommand = new nl.tinyaii.tinyplayer.back.BackCommand(this);
            getServer().getPluginManager().registerEvents(backCommand, this);
            getCommand("返回").setExecutor(backCommand);
            getLogger().info("[模块] 返回：开启");
        } else {
            getCommand("返回").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&c返回模块已关闭。")); return true; });
            getLogger().info("[模块] 返回：关闭");
        }

        // ===== 传送点模块（开关 warp.enabled）=====
        if (getConfig().getBoolean("warp.enabled", true)) {
            warpManager = new nl.tinyaii.tinyplayer.warp.WarpManager(this);
            warpManager.load();
            getServer().getPluginManager().registerEvents(new nl.tinyaii.tinyplayer.warp.WarpGuiListener(this), this);
            getCommand("传送点").setExecutor(new nl.tinyaii.tinyplayer.warp.WarpCommand(this));
            getLogger().info("[模块] 传送点：开启");
        } else {
            getCommand("传送点").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&c传送点模块已关闭。")); return true; });
            getLogger().info("[模块] 传送点：关闭");
        }

        // ===== 私聊模块（开关 msg.enabled）=====
        if (getConfig().getBoolean("msg.enabled", true)) {
            msgCommand = new nl.tinyaii.tinyplayer.msg.MsgCommand(this);
            getCommand("私聊").setExecutor(msgCommand);
            getCommand("回复").setExecutor(msgCommand);
            getLogger().info("[模块] 私聊：开启");
        } else {
            getCommand("私聊").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&c私聊模块已关闭。")); return true; });
            getCommand("回复").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&c私聊模块已关闭。")); return true; });
            getLogger().info("[模块] 私聊：关闭");
        }

        // ===== 经济模块（开关 economy.enabled）=====
        if (getConfig().getBoolean("economy.enabled", true)) {
            economyInternal = new nl.tinyaii.tinyplayer.economy.EconomyManager(this);
            economyInternal.load();
            ecoBridge = new nl.tinyaii.tinyplayer.economy.EcoBridge(this);
            getCommand("金币").setExecutor(new nl.tinyaii.tinyplayer.economy.EconomyCommand(this));
            getCommand("pay").setExecutor(new nl.tinyaii.tinyplayer.economy.EconomyCommand(this));
            // 新玩家首次进服发放初始金币（仅内置模式；外部经济由外部管理）
            double initial = getConfig().getDouble("economy.initial-balance", 0);
            if (initial > 0) {
                getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
                    @EventHandler
                    public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
                        java.util.UUID u = e.getPlayer().getUniqueId();
                        if (!ecoBridge.isExternal() && economyInternal.getBalance(u) == 0 && !economyInternal.has(u)) {
                            economyInternal.setBalance(u, initial);
                        }
                    }
                }, this);
            }
            getLogger().info("[模块] 经济：开启（后端=" + ecoBridge.getMode() + "）");
        } else {
            getCommand("金币").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&c经济模块已关闭。")); return true; });
            getCommand("pay").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&c经济模块已关闭。")); return true; });
            getLogger().info("[模块] 经济：关闭");
        }

        // ===== 管理员神权模块（开关 admin.enabled）=====
        if (getConfig().getBoolean("admin.enabled", true)) {
            adminLog = new nl.tinyaii.tinyplayer.admin.AdminLog(this);
            getCommand("神权").setExecutor(new nl.tinyaii.tinyplayer.admin.AdminCommand(this));
            getLogger().info("[模块] 神权管理：开启");
        } else {
            getCommand("神权").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&c神权模块已关闭。")); return true; });
            getLogger().info("[模块] 神权管理：关闭");
        }

        // ===== 领地模块（开关 claim.enabled，复刻 TinyClaim）=====
        if (getConfig().getBoolean("claim.enabled", true)) {
            claimModule = new nl.tinyaii.tinyplayer.claim.ClaimModule(this);
            claimModule.onEnable();
        } else {
            getCommand("领地").setExecutor((s, c, l, a) -> { s.sendMessage(Messages.color("&c领地模块已关闭。")); return true; });
            getLogger().info("[模块] 领地：关闭");
        }

        getLogger().info("整合插件已启用。");
    }

    @Override
    public void onDisable() {
        if (authManager != null) authManager.save();
        if (claimModule != null) claimModule.onDisable();
    }

    /** 模块开关查询（供各模块内部 / 事件用） */
    public boolean authEnabled() { return getConfig().getBoolean("auth.enabled", true); }
    public boolean tpaEnabled() { return getConfig().getBoolean("tpa.enabled", true); }
    public boolean spawnEnabled() { return getConfig().getBoolean("spawn.enabled", true); }
    public boolean tradeEnabled() { return getConfig().getBoolean("trade.enabled", true); }
    public boolean homeEnabled() { return getConfig().getBoolean("home.enabled", true); }
    public boolean backEnabled() { return getConfig().getBoolean("back.enabled", true); }
    public boolean warpEnabled() { return getConfig().getBoolean("warp.enabled", true); }
    public boolean msgEnabled() { return getConfig().getBoolean("msg.enabled", true); }
    public boolean economyEnabled() { return getConfig().getBoolean("economy.enabled", true); }

    public boolean adminEnabled() { return getConfig().getBoolean("admin.enabled", true); }
    public boolean claimEnabled() { return getConfig().getBoolean("claim.enabled", true); }

    private nl.tinyaii.tinyplayer.auth.AuthManager authManager;
    private nl.tinyaii.tinyplayer.tpa.TpaManager tpaManager;
    private nl.tinyaii.tinyplayer.trade.TradeManager tradeManager;
    private nl.tinyaii.tinyplayer.trade.TradeHandler tradeHandler;
    private nl.tinyaii.tinyplayer.spawn.SpawnCommand spawnCommand;
    private nl.tinyaii.tinyplayer.home.HomeManager homeManager;
    private nl.tinyaii.tinyplayer.home.TeleportTask homeTeleport;
    private nl.tinyaii.tinyplayer.back.BackCommand backCommand;
    private nl.tinyaii.tinyplayer.warp.WarpManager warpManager;
    private nl.tinyaii.tinyplayer.msg.MsgCommand msgCommand;
    private nl.tinyaii.tinyplayer.economy.EconomyManager economyInternal;
    private nl.tinyaii.tinyplayer.economy.EcoBridge ecoBridge;
    private nl.tinyaii.tinyplayer.admin.AdminLog adminLog;
    private nl.tinyaii.tinyplayer.claim.ClaimModule claimModule;

    public nl.tinyaii.tinyplayer.spawn.SpawnCommand getSpawnCommand() { return spawnCommand; }

    /** 主城/出生点坐标：config 配了 spawn.world 就用坐标，空则世界出生点（回城+登录等待区共用） */
    public Location getSpawnLocation() {
        String wName = getConfig().getString("spawn.world", "");
        World w = wName.isEmpty() ? Bukkit.getWorlds().get(0) : Bukkit.getWorld(wName);
        if (w == null) w = Bukkit.getWorlds().get(0);
        if (wName.isEmpty()) return w.getSpawnLocation();
        return new Location(w, getConfig().getDouble("spawn.x", 0),
                getConfig().getDouble("spawn.y", 100),
                getConfig().getDouble("spawn.z", 0));
    }
    public nl.tinyaii.tinyplayer.auth.AuthManager getAuthManager() { return authManager; }
    public nl.tinyaii.tinyplayer.home.HomeManager getHomeManager() { return homeManager; }
    public nl.tinyaii.tinyplayer.home.TeleportTask getHomeTeleport() { return homeTeleport; }
    public nl.tinyaii.tinyplayer.warp.WarpManager getWarpManager() { return warpManager; }
    public nl.tinyaii.tinyplayer.economy.EconomyManager getEconomyInternal() { return economyInternal; }
    public nl.tinyaii.tinyplayer.economy.EcoBridge getEcoBridge() { return ecoBridge; }
    public nl.tinyaii.tinyplayer.admin.AdminLog getAdminLog() { return adminLog; }
    public nl.tinyaii.tinyplayer.trade.TradeHandler getTradeHandler() { return tradeHandler; }
    public nl.tinyaii.tinyplayer.claim.ClaimModule getClaimModule() { return claimModule; }

    // ===== 领地模块转发方法 =====
    public nl.tinyaii.tinyplayer.claim.data.ClaimManager getClaimManager() {
        return claimModule == null ? null : claimModule.getClaimManager();
    }
    public nl.tinyaii.tinyplayer.claim.flags.FlagCheck getFlagCheck() {
        return claimModule == null ? null : claimModule.getFlagCheck();
    }
    public nl.tinyaii.tinyplayer.claim.command.SelectionManager getClaimSelectionManager() {
        return claimModule == null ? null : claimModule.getSelectionManager();
    }
    public nl.tinyaii.tinyplayer.claim.storage.Storage getClaimStorage() {
        return claimModule == null ? null : claimModule.getStorage();
    }
    public nl.tinyaii.tinyplayer.claim.util.EcoBridge getClaimEcoBridge() {
        return claimModule == null ? null : claimModule.getClaimEcoBridge();
    }

    /** 判断玩家是否已登录：登录模块关闭时一律视为已登录（免登录） */
    public boolean isLoggedIn(java.util.UUID uuid) {
        return authManager == null || authManager.isLoggedIn(uuid);
    }
    public nl.tinyaii.tinyplayer.tpa.TpaManager getTpaManager() { return tpaManager; }
    public nl.tinyaii.tinyplayer.trade.TradeManager getTradeManager() { return tradeManager; }
    public Messages getMessages() { return messages; }
}

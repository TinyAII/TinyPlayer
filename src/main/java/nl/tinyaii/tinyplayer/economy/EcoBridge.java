package nl.tinyaii.tinyplayer.economy;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 经济桥：三层对接（全反射/软依赖，不装也照常跑）。
 *   ① 自家 Economy（TinyAII/Economy）→ 优先，余额共享
 *   ② Vault（若装了且绑定了经济实现）→ 次之
 *   ③ 内置 economy-data.yml → 兜底
 */
public class EcoBridge {

    private final TinyPlayerPlugin plugin;
    private Mode mode = Mode.INTERNAL;

    // 自家 Economy API（反射）
    private Method mGetBalance, mHas, mWithdraw, mDeposit, mSetBalance;
    // Vault API（反射）
    private Object vaultEconomy;

    enum Mode { INTERNAL, OWN_ECONOMY, VAULT }

    public EcoBridge(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
        tryInit();
    }

    /** 启动时探测（reload 也可重新调用） */
    public void tryInit() {
        String provider = plugin.getConfig().getString("economy.provider", "auto").toLowerCase();
        if (provider.equals("own") || provider.equals("auto")) {
            if (initOwnEconomy()) {
                mode = Mode.OWN_ECONOMY;
                plugin.getLogger().info("[经济桥] 已对接自家 Economy 插件");
                return;
            }
        }
        if (provider.equals("vault") || provider.equals("auto")) {
            if (initVault()) {
                mode = Mode.VAULT;
                plugin.getLogger().info("[经济桥] 已对接 Vault");
                return;
            }
        }
        mode = Mode.INTERNAL;
        if (provider.equals("auto")) {
            plugin.getLogger().info("[经济桥] 未检测到外部经济，使用内置经济。");
        } else {
            plugin.getLogger().warning("[经济桥] 指定 provider=" + provider + " 不可用，回退内置经济。");
        }
    }

    private boolean initOwnEconomy() {
        Plugin eco = Bukkit.getPluginManager().getPlugin("Economy");
        if (eco == null || !eco.isEnabled()) return false;
        try {
            Class<?> api = Class.forName("nl.tinyaii.economy.api.EconomyAPI");
            mGetBalance = api.getMethod("getBalance", UUID.class);
            mHas = api.getMethod("has", UUID.class, double.class);
            mWithdraw = api.getMethod("withdraw", UUID.class, double.class);
            mDeposit = api.getMethod("deposit", UUID.class, double.class);
            mSetBalance = api.getMethod("setBalance", UUID.class, double.class);
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("[经济桥] 自家 Economy 反射失败: " + t.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean initVault() {
        try {
            Class<?> ecoClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(ecoClass);
            if (rsp == null) return false;
            vaultEconomy = rsp.getProvider();
            return vaultEconomy != null;
        } catch (Throwable t) {
            return false;
        }
    }

    // ===== 对外统一接口 =====

    public Mode getMode() { return mode; }
    public boolean isExternal() { return mode != Mode.INTERNAL; }

    public double getBalance(UUID uuid) {
        switch (mode) {
            case OWN_ECONOMY:
                try { Object r = mGetBalance.invoke(null, uuid); return r instanceof Number ? ((Number) r).doubleValue() : 0; }
                catch (Exception e) { return 0; }
            case VAULT:
                try { Object r = vaultEconomy.getClass().getMethod("getBalance", OfflinePlayer.class).invoke(vaultEconomy, Bukkit.getOfflinePlayer(uuid)); return r instanceof Number ? ((Number) r).doubleValue() : 0; }
                catch (Exception e) { return 0; }
            default:
                return plugin.getEconomyInternal().getBalance(uuid);
        }
    }

    public boolean has(UUID uuid, double amount) {
        switch (mode) {
            case OWN_ECONOMY:
                try { Object r = mHas.invoke(null, uuid, amount); return Boolean.TRUE.equals(r); }
                catch (Exception e) { return false; }
            case VAULT:
                try { Object r = vaultEconomy.getClass().getMethod("has", OfflinePlayer.class, double.class).invoke(vaultEconomy, Bukkit.getOfflinePlayer(uuid), amount); return Boolean.TRUE.equals(r); }
                catch (Exception e) { return false; }
            default:
                return plugin.getEconomyInternal().getBalance(uuid) >= amount;
        }
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0) return true;
        switch (mode) {
            case OWN_ECONOMY:
                try { Object r = mWithdraw.invoke(null, uuid, amount); return Boolean.TRUE.equals(r); }
                catch (Exception e) { return false; }
            case VAULT:
                try { Object r = vaultEconomy.getClass().getMethod("withdrawPlayer", OfflinePlayer.class, double.class).invoke(vaultEconomy, Bukkit.getOfflinePlayer(uuid), amount); return Boolean.TRUE.equals(r); }
                catch (Exception e) { return false; }
            default:
                if (getBalance(uuid) < amount) return false;
                plugin.getEconomyInternal().setBalance(uuid, getBalance(uuid) - amount);
                return true;
        }
    }

    public void deposit(UUID uuid, double amount) {
        if (amount <= 0) return;
        switch (mode) {
            case OWN_ECONOMY:
                try { mDeposit.invoke(null, uuid, amount); } catch (Exception ignored) {}
                break;
            case VAULT:
                try { vaultEconomy.getClass().getMethod("depositPlayer", OfflinePlayer.class, double.class).invoke(vaultEconomy, Bukkit.getOfflinePlayer(uuid), amount); } catch (Exception ignored) {}
                break;
            default:
                plugin.getEconomyInternal().setBalance(uuid, getBalance(uuid) + amount);
        }
    }

    public void setBalance(UUID uuid, double amount) {
        switch (mode) {
            case OWN_ECONOMY:
                try { mSetBalance.invoke(null, uuid, Math.max(0, amount)); } catch (Exception ignored) {}
                break;
            case VAULT:
                try {
                    Object p = vaultEconomy.getClass().getMethod("getBalance", OfflinePlayer.class).invoke(vaultEconomy, Bukkit.getOfflinePlayer(uuid));
                    double cur = p instanceof Number ? ((Number) p).doubleValue() : 0;
                    if (amount > cur) deposit(uuid, amount - cur);
                    else if (amount < cur) withdraw(uuid, cur - amount);
                } catch (Exception ignored) {}
                break;
            default:
                plugin.getEconomyInternal().setBalance(uuid, Math.max(0, amount));
        }
    }

    /** 转账：返回 null=成功，否则错误消息 */
    public String transfer(UUID from, UUID to, double amount) {
        if (amount <= 0) return "金额必须大于 0";
        if (!has(from, amount)) return "余额不足";
        withdraw(from, amount);
        deposit(to, amount);
        return null;
    }

    /** 当前生效货币名 */
    public String currencyName() {
        switch (mode) {
            case OWN_ECONOMY:
                try {
                    Plugin eco = Bukkit.getPluginManager().getPlugin("Economy");
                    java.io.File f = new java.io.File(eco.getDataFolder(), "config.yml");
                    if (f.exists()) {
                        return org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f)
                                .getString("settings.currency-name", "金币");
                    }
                } catch (Throwable ignored) {}
                return "金币";
            case VAULT:
                try { Object r = vaultEconomy.getClass().getMethod("currencyNamePlural").invoke(vaultEconomy); return String.valueOf(r); }
                catch (Exception e) { return "金币"; }
            default:
                return "金币";
        }
    }

    /** 排行：自家 Economy 支持；Vault 无排行 → 返回空（由调用方降级） */
    public java.util.Map<UUID, Double> top(int n) {
        java.util.Map<UUID, Double> out = new java.util.LinkedHashMap<>();
        if (mode == Mode.OWN_ECONOMY) {
            try {
                Class<?> api = Class.forName("nl.tinyaii.economy.api.EconomyAPI");
                Method m = api.getMethod("topBalances", int.class);
                Object r = m.invoke(null, n);
                if (r instanceof java.util.List) {
                    for (Object o : (java.util.List<?>) r) {
                        try {
                            java.lang.reflect.Field uf = o.getClass().getField("uuid");
                            Object u = uf.get(o);
                            Method gb = o.getClass().getMethod("getBalance");
                            out.put((UUID) u, ((Number) gb.invoke(o)).doubleValue());
                        } catch (Throwable ignored) {}
                    }
                }
                return out;
            } catch (Throwable ignored) {}
        }
        // Vault / 内置：用内置数据排行
        return plugin.getEconomyInternal().top(n);
    }
}
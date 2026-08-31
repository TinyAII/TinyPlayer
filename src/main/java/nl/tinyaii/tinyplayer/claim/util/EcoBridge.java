package nl.tinyaii.tinyplayer.claim.util;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;

/**
 * 经济桥（软依赖/反射）：自家 Economy 优先 → Vault 次之 → 未装则经济功能禁用。
 * 与全家桶 EcoBridge 同构。
 */
public class EcoBridge {

    private final TinyPlayerPlugin plugin;
    private Method mHas, mWithdraw;
    private Object vaultEconomy;
    private boolean own = false;
    private boolean vault = false;

    public EcoBridge(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
        tryInit();
    }

    public void tryInit() {
        own = false;
        vault = false;
        // 自家 Economy
        if (Bukkit.getPluginManager().getPlugin("Economy") != null) {
            try {
                Class<?> api = Class.forName("nl.tinyaii.economy.api.EconomyAPI");
                mHas = api.getMethod("has", java.util.UUID.class, double.class);
                mWithdraw = api.getMethod("withdraw", java.util.UUID.class, double.class);
                own = true;
                plugin.getLogger().info("[经济桥] 已对接自家 Economy");
                return;
            } catch (Throwable ignored) {}
        }
        // Vault
        try {
            Class<?> ecoClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(ecoClass);
            if (rsp != null) {
                vaultEconomy = rsp.getProvider();
                vault = true;
                plugin.getLogger().info("[经济桥] 已对接 Vault");
            }
        } catch (Throwable ignored) {}
    }

    public boolean isAvailable() { return own || vault; }

    /** 扣费：返回 null=成功，否则错误信息 */
    public String tryWithdraw(Player p, double amount) {
        if (amount <= 0) return null;
        if (own) {
            try {
                boolean has = (Boolean) mHas.invoke(null, p.getUniqueId(), amount);
                if (!has) return "余额不足，需要 " + amount + " 金币";
                mWithdraw.invoke(null, p.getUniqueId(), amount);
                return null;
            } catch (Exception e) { return "经济接口异常"; }
        }
        if (vault) {
            try {
                boolean ok = (Boolean) vaultEconomy.getClass().getMethod("has", OfflinePlayer.class, double.class)
                        .invoke(vaultEconomy, p, amount);
                if (!ok) return "余额不足，需要 " + amount + " 金币";
                vaultEconomy.getClass().getMethod("withdrawPlayer", OfflinePlayer.class, double.class)
                        .invoke(vaultEconomy, p, amount);
                return null;
            } catch (Exception e) { return "经济接口异常"; }
        }
        return null; // 未接经济 → 免费
    }
}

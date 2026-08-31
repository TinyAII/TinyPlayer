package nl.tinyaii.tinyplayer.claim.flags;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.data.Claim;
import nl.tinyaii.tinyplayer.claim.data.ClaimManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * Flag 检查核心：领地保护总开关逻辑（与 Dominion 思路同构但独立实现）。
 *   - 环境 Flag：查坐标所在领地 → flag false → cancel 事件
 *   - 权限 Flag：查领地 → 查玩家角色 → 查 flag → false 则 cancel
 */
public class FlagCheck {

    private final TinyPlayerPlugin plugin;
    private final ClaimManager manager;

    public FlagCheck(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getClaimManager();
    }

    /** 玩家是否可无视领地保护（claim.bypass 或 OP） */
    public boolean bypass(Player p) {
        return p.hasPermission("claim.bypass") || p.isOp();
    }

    /**
     * 环境 Flag 检查：所在领地 flag 为 false → cancel 事件并返回 false。
     * 无领地时返回 true（领地外不限制）。
     */
    public boolean checkEnv(Location loc, String flagName, Cancellable event) {
        Claim claim = manager.getClaimAt(loc.getWorld().getUID(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (claim == null) return true;
        return checkEnvInClaim(claim, flagName, event);
    }

    /** 指定领地内做环境 flag 检查（子领地覆盖父领地） */
    public boolean checkEnvInClaim(Claim claim, String flagName, Cancellable event) {
        Boolean value = resolveEnv(claim, flagName);
        if (!Boolean.TRUE.equals(value) && event != null) {
            event.setCancelled(true);
        }
        return Boolean.TRUE.equals(value);
    }

    /**
     * 权限 Flag 检查：玩家在领地内做某事是否被允许。
     * 返回 true=允许，false=禁止（且已 cancel 事件）。
     */
    public boolean checkPri(Location loc, String flagName, Player p, Cancellable event) {
        if (bypass(p)) return true;
        Claim claim = manager.getClaimAt(loc.getWorld().getUID(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (claim == null) return true;
        return checkPriInClaim(claim, flagName, p, event);
    }

    /** 指定领地内做权限 flag 检查 */
    public boolean checkPriInClaim(Claim claim, String flagName, Player p, Cancellable event) {
        // 领地主人 / 该主人的全局权限成员（所有领地）/ 单领地成员：默认放行
        if (claim.isOwner(p.getUniqueId())
                || claim.isAdmin(p.getUniqueId())
                || claim.isMember(p.getUniqueId())
                || manager.isGlobalAdmin(claim.getOwner(), p.getUniqueId())) {
            return true;
        }
        // 外人：按 flag 值决定（默认关 → 拦截）
        Boolean value = resolvePri(claim, flagName);
        if (!Boolean.TRUE.equals(value)) {
            if (event != null) event.setCancelled(true);
            return false;
        }
        return true;
    }

    /** 解析环境 flag：子领地覆盖 → 领地覆盖 → 全局默认（config） */
    private Boolean resolveEnv(Claim claim, String flagName) {
        Boolean overridden = claim.getFlagOverride(flagName);
        if (overridden != null) return overridden;
        // 从 config 读全局默认
        return plugin.getConfig().getBoolean("claim.flags.default-environment." + flagName,
                Flags.ENV_FLAGS.getOrDefault(flagName, true));
    }

    /** 解析权限 flag：子领地覆盖 → 领地覆盖 → 全局默认（config） */
    private Boolean resolvePri(Claim claim, String flagName) {
        Boolean overridden = claim.getFlagOverride(flagName);
        if (overridden != null) return overridden;
        return plugin.getConfig().getBoolean("claim.flags.default-privilege." + flagName,
                Flags.PRI_FLAGS.getOrDefault(flagName, true));
    }
}

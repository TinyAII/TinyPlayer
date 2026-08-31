package nl.tinyaii.tinyplayer.auth;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * 登录监听：未登录玩家锁定（不能移动/交互/打怪/开箱/用命令），超时踢出。
 */
public class AuthListener implements Listener {

    private final TinyPlayerPlugin plugin;
    private final AuthManager am;

    public AuthListener(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
        this.am = plugin.getAuthManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getConfig().getBoolean("auth.enabled", true)) return;
        // 基岩版玩家（Geyser/Floodgate）免登录自动放行
        if (plugin.getConfig().getBoolean("auth.bypass-bedrock", true)
                && AuthManager.isBedrockPlayer(p.getUniqueId())) {
            am.setLoggedIn(p.getUniqueId(), true);
            p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&e基岩版玩家免登录，欢迎回来！"));
            return;
        }
        am.recordJoin(p.getUniqueId());
        // 延迟 1 tick 记录真实退出位置（玩家位置恢复完成后再读，避免拿到出生点），随后传送登录等待区
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            // 若上次是"未登录被踢/退出"，已有持久化原位置 → 不覆盖，保持真正退出位置
            if (!am.hasPersistedLocation(p.getUniqueId())) {
                am.recordOriginalLocation(p.getUniqueId(), p.getLocation());
            }
            p.teleport(plugin.getSpawnLocation());
        }, 1L);
        if (am.hasAccount(p.getUniqueId())) {
            p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&e请登录：&a/登录 <密码>"));
        } else {
            p.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&e首次进服请注册：&a/注册 <密码> <确认密码>"));
        }
        // 超时踢出定时检查
        int timeout = plugin.getConfig().getInt("auth.kick-timeout-seconds", 30);
        if (timeout > 0 && am.hasAccount(p.getUniqueId())) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline() && !am.isLoggedIn(p.getUniqueId())) {
                    p.kickPlayer(plugin.getConfig().getString("auth.kick-message", "请重新登录"));
                }
            }, timeout * 20L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID u = e.getPlayer().getUniqueId();
        am.clearJoin(u);
        // 未登录退出（被踢/主动退出）：把原位置持久化，下次进服登录仍能回真正退出位置（而非出生点）
        if (!am.isLoggedIn(u)) {
            am.persistOriginalLocation(u);
        } else {
            am.clearOriginalLocation(u);
        }
        am.setLoggedIn(u, false);
    }

    /**
     * 锁定判定：只要未登录（无论是否注册过账号）即锁定。
     * 修复：旧逻辑 requires hasAccount —— 新玩家没账号被放跑。
     */
    private boolean locked(Player p) {
        if (!plugin.getConfig().getBoolean("auth.enabled", true)) return false;
        // 基岩版玩家免登录放行
        if (plugin.getConfig().getBoolean("auth.bypass-bedrock", true)
                && AuthManager.isBedrockPlayer(p.getUniqueId())) return false;
        return !am.isLoggedIn(p.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!locked(e.getPlayer())) return;
        if (e.getFrom().getBlockX() != e.getTo().getBlockX()
                || e.getFrom().getBlockY() != e.getTo().getBlockY()
                || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && locked((Player) e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player && locked((Player) e.getDamager())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player && locked((Player) e.getWhoClicked())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player && locked((Player) e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!locked(e.getPlayer())) return;
        String cmd = e.getMessage().toLowerCase();
        // 放行登录/注册命令
        if (cmd.startsWith("/登录") || cmd.startsWith("/login")
                || cmd.startsWith("/注册") || cmd.startsWith("/register")) return;
        e.setCancelled(true);
        e.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "请先登录：/登录 <密码>");
    }
}

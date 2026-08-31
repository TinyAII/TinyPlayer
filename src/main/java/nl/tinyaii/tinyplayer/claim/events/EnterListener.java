package nl.tinyaii.tinyplayer.claim.events;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.data.Claim;
import nl.tinyaii.tinyplayer.claim.util.BoundaryRenderer;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进入/退出领地提示：
 *   - 进入领地：显示一次边界粒子（display-duration-seconds 后自动消失）+ ActionBar 欢迎语
 *   - 退出领地：显示一次边界粒子 + ActionBar 欢送语（欢迎下次光临）
 * 粒子不常驻（显示一段时间自动消失），避免一直刷。
 */
public class EnterListener implements Listener {

    private final TinyPlayerPlugin plugin;
    private final BoundaryRenderer renderer;
    private final Map<UUID, Integer> currentClaim = new ConcurrentHashMap<>();  // player -> claimId（-1=无）
    private final Map<UUID, BukkitRunnable> tasks = new ConcurrentHashMap<>();

    public EnterListener(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
        this.renderer = new BoundaryRenderer(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;
        Player p = e.getPlayer();
        Claim claim = plugin.getClaimManager().getClaimAt(p.getWorld().getUID(),
                p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ());
        int claimId = claim == null ? -1 : claim.getId();
        Integer prev = currentClaim.put(p.getUniqueId(), claimId);
        if (prev != null && prev == claimId) return;   // 状态没变

        if (claimId == -1) {
            // 离开领地：欢送语 + 显示一次边界粒子（自动消失）
            Claim left = plugin.getClaimManager().getById(prev == null ? -1 : prev);
            if (left != null) {
                String leave = left.getLeaveMsg() == null
                        ? plugin.getConfig().getString("claim.settings.default-leave", "欢迎下次光临")
                        : left.getLeaveMsg();
                showActionBar(p, Messages.color("&e《" + leave + "》&a欢迎下次光临 &f" + left.getName() + " &e的领地！"));
                flashBoundary(p, left);
            }
        } else {
            // 进入领地：欢迎语 + 显示一次边界粒子（自动消失）
            showWelcome(p, claim);
            flashBoundary(p, claim);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        currentClaim.remove(e.getPlayer().getUniqueId());
        stopTask(e.getPlayer().getUniqueId());
    }

    /** ActionBar 欢迎语（屏幕下方物品栏上方） */
    private void showWelcome(Player p, Claim claim) {
        String welcome = claim.getWelcomeMsg() == null
                ? plugin.getConfig().getString("claim.settings.default-welcome", "欢迎来到")
                : claim.getWelcomeMsg();
        showActionBar(p, Messages.color("&e《" + welcome + "》&a您已进入 &e" + claim.getName() + " &a的领地！"));
    }

    private void showActionBar(Player p, String msg) {
        try {
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(msg));
        } catch (Throwable t) {
            p.sendMessage(msg);
        }
    }

    /** 显示一次边界粒子：每秒重画保持可见，enter-leave-duration-seconds 后自动消失 */
    private void flashBoundary(Player p, Claim claim) {
        stopTask(p.getUniqueId());
        long durationMs = (long) (plugin.getConfig().getDouble("claim.settings.enter-leave-duration-seconds", 2.3) * 1000L);
        final long start = System.currentTimeMillis();
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); tasks.remove(p.getUniqueId()); return; }
                if (System.currentTimeMillis() - start > durationMs) {
                    cancel();
                    tasks.remove(p.getUniqueId());
                    return;
                }
                renderer.drawBox(p, claim.getCuboid());
            }
        };
        task.runTaskTimer(plugin, 0L, 20L);
        tasks.put(p.getUniqueId(), task);
    }

    private void stopTask(UUID u) {
        BukkitRunnable t = tasks.remove(u);
        if (t != null) t.cancel();
    }
}

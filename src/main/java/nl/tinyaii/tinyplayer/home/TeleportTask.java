package nl.tinyaii.tinyplayer.home;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 回家吟唱传送：倒计时（消息栏）→ 传送到安全落点；移动/受击自动打断。
 */
public class TeleportTask {

    private final TinyPlayerPlugin plugin;
    private final Map<UUID, UUID> active = new ConcurrentHashMap<>();  // player -> cancel token

    public TeleportTask(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /** 是否在吟唱中（打断用） */
    public boolean isActive(UUID uuid) { return active.containsKey(uuid); }

    /** 开始吟唱；被 newToken 打断旧吟唱 */
    public void start(Player p, Location dest, String destName) {
        int seconds = plugin.getConfig().getInt("home.warmup-seconds", 5);
        UUID token = UUID.randomUUID();
        active.put(p.getUniqueId(), token);

        p.sendMessage(Messages.color("&a回家传送开始，请勿移动... &e(" + destName + ")"));
        sendCount(p, seconds);

        final int[] remain = {seconds};
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline() || !token.equals(active.get(p.getUniqueId()))) {
                    cancel();
                    return;
                }
                remain[0]--;
                if (remain[0] <= 0) {
                    cancel();
                    active.remove(p.getUniqueId());
                    p.teleport(safeLocation(dest));
                    p.sendMessage(Messages.color("&a已到达 " + destName + "！"));
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                } else {
                    sendCount(p, remain[0]);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /** 消息栏倒计时（与交易一致，消息栏不被界面遮挡） */
    private void sendCount(Player p, int sec) {
        p.sendMessage(Messages.color("&e[回家] &f" + sec + " &e秒后传送，请勿移动"));
    }

    /** 打断（移动/受击） */
    public void cancel(UUID uuid, String reason) {
        UUID token = active.remove(uuid);
        if (token != null) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                p.sendMessage(Messages.color("&c传送已取消：" + reason));
            }
        }
    }

    /** 安全落点：目标被埋/岩浆里向上扫 5 格，找不到则原坐标硬传 */
    private Location safeLocation(Location dest) {
        World w = dest.getWorld();
        if (w == null) return dest;
        for (int i = 0; i <= 5; i++) {
            Location probe = dest.clone().add(0, i, 0);
            Material foot = probe.getBlock().getType();
            Material head = probe.clone().add(0, 1, 0).getBlock().getType();
            if (isSafe(foot) && isSafe(head)) {
                probe.setYaw(dest.getYaw());
                probe.setPitch(dest.getPitch());
                return probe;
            }
        }
        return dest;
    }

    private boolean isSafe(Material m) {
        return !m.isSolid() && m != Material.LAVA && m != Material.WATER
                && m != Material.MAGMA_BLOCK && m != Material.CACTUS;
    }
}
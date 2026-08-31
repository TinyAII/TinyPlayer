package nl.tinyaii.tinyplayer.spawn;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 回城命令：/回城 /spawn —— 传送到主城（config 可设坐标）或世界出生点。
 * 吟唱期间屏幕中间（Title）显示倒计时，移动取消。
 */
public class SpawnCommand implements CommandExecutor {

    private final TinyPlayerPlugin plugin;
    private final Map<UUID, Long> warmup = new HashMap<>();

    public SpawnCommand(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
        Player p = (Player) sender;

        // 吟唱（屏幕中间倒计时）
        int seconds = plugin.getConfig().getInt("spawn.warmup-seconds", 5);
        if (seconds <= 0) {
            p.teleport(spawnLocation());
            p.sendMessage(Messages.color("&a已回到主城！"));
            return true;
        }
        if (warmup.containsKey(p.getUniqueId())) {
            p.sendMessage(Messages.color("&c已在传送吟唱中，请勿重复使用。"));
            return true;
        }
        p.sendMessage(Messages.color("&a回城传送开始，请勿移动..."));
        warmup.put(p.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
        p.sendTitle("", Messages.color("&e回城倒计时 &f" + seconds + "&e 秒"), 2, 18, 2);

        final int[] remain = {seconds};
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); warmup.remove(p.getUniqueId()); return; }
                if (!warmup.containsKey(p.getUniqueId())) { cancel(); p.sendTitle("", "", 0, 0, 0); return; }
                remain[0]--;
                if (remain[0] <= 0) {
                    cancel();
                    warmup.remove(p.getUniqueId());
                    p.sendTitle("", "", 0, 0, 0);
                    p.teleport(spawnLocation());
                    p.sendMessage(Messages.color("&a已回到主城！"));
                } else {
                    p.sendTitle("", Messages.color("&e回城倒计时 &f" + remain[0] + "&e 秒"), 2, 18, 2);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        return true;
    }

    /** 主城坐标：复用主类 getSpawnLocation() */
    private Location spawnLocation() {
        return plugin.getSpawnLocation();
    }

    /** 移动打断（由 SpawnListener 调用） */
    public void cancelIfMoved(Player p) {
        Long deadline = warmup.remove(p.getUniqueId());
        if (deadline != null) {
            p.sendTitle("", "", 0, 0, 0);
            p.sendMessage(Messages.color("&c移动了，回城取消。"));
        }
    }
}
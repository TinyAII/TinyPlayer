package nl.tinyaii.tinyplayer.spawn;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * 回城吟唱移动打断。
 */
public class SpawnListener implements Listener {

    private final TinyPlayerPlugin plugin;

    public SpawnListener(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;
        // 通过主类拿 SpawnCommand 打断（简化：这里用静态 map 或回调）
        plugin.getSpawnCommand().cancelIfMoved((Player) e.getPlayer());
    }
}

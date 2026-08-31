package nl.tinyaii.tinyplayer.tpa;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * TPA 清理：玩家退出时移除相关请求。
 */
public class TpaListener implements Listener {

    private final TinyPlayerPlugin plugin;

    public TpaListener(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        java.util.UUID u = e.getPlayer().getUniqueId();
        plugin.getTpaManager().removeAllFrom(u);
        plugin.getTpaManager().pendingTo(u).forEach(plugin.getTpaManager()::remove);
    }
}

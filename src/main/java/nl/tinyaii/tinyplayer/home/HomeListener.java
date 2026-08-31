package nl.tinyaii.tinyplayer.home;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * 回家吟唱打断：移动/受击自动取消传送。
 */
public class HomeListener implements Listener {

    private final TinyPlayerPlugin plugin;

    public HomeListener(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;
        plugin.getHomeTeleport().cancel(e.getPlayer().getUniqueId(), "移动了");
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            plugin.getHomeTeleport().cancel(e.getEntity().getUniqueId(), "受到了伤害");
        }
    }
}
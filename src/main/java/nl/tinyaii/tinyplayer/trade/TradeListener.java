package nl.tinyaii.tinyplayer.trade;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 右键玩家打开交易：潜行右键/普通右键玩家 → 给目标弹交易请求确认 GUI。
 */
public class TradeListener implements Listener {

    private final TinyPlayerPlugin plugin;

    public TradeListener(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onRightClick(PlayerInteractEntityEvent e) {
        // 主手/副手各触发一次事件，只处理主手（HAND），避免重复弹窗/重复消息
        if (e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (!(e.getRightClicked() instanceof Player)) return;
        Player me = e.getPlayer();
        Player target = (Player) e.getRightClicked();
        if (!me.isSneaking()) return;   // 潜行右键玩家 = 发起交易请求
        if (!plugin.isLoggedIn(me.getUniqueId())
                || !plugin.isLoggedIn(target.getUniqueId())) return;

        String err = plugin.getTradeManager().requestTrade(me.getUniqueId(), target.getUniqueId());
        if (err != null) {
            me.sendMessage(Messages.color("&c" + err));
            return;
        }
        e.setCancelled(true);
        me.sendMessage(Messages.color("&a已向 &e" + target.getName() + " &a发起交易请求，等待对方处理..."));
        new TradeRequestGui(plugin, me.getUniqueId(), target.getUniqueId()).open(target);
        target.sendMessage(Messages.color("&e" + me.getName() + " &a想与你交易，请在窗口中选择。"));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getTradeManager().end(e.getPlayer().getUniqueId());
    }
}
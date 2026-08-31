package nl.tinyaii.tinyplayer.trade;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 交易请求确认 GUI 交互：
 *   [✔ 同意交易] / [✘ 拒绝交易] / [🛡 拒绝并屏蔽 8 分钟]
 */
public class TradeRequestListener implements Listener {

    private final TinyPlayerPlugin plugin;
    private final TradeGui tradeGui;
    private final Set<UUID> handledClose = new HashSet<>();

    public TradeRequestListener(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
        this.tradeGui = new TradeGui(plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof TradeRequestHolder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        TradeRequestHolder holder = (TradeRequestHolder) e.getInventory().getHolder();
        int raw = e.getRawSlot();
        Player requester = Bukkit.getPlayer(holder.requester);

        if (raw == 22) {
            // 同意交易
            handledClose.add(p.getUniqueId());
            p.closeInventory();
            handledClose.remove(p.getUniqueId());
            if (requester == null || !requester.isOnline()) {
                p.sendMessage(Messages.color("&c对方已不在线，交易取消。"));
                return;
            }
            TradeManager.TradeSession s = plugin.getTradeManager().start(holder.requester, p.getUniqueId());
            if (s == null) {
                p.sendMessage(Messages.color("&c交易失败（对方正在交易中或你已在交易中）。"));
                return;
            }
            tradeGui.open(requester, p, s);
        } else if (raw == 31) {
            // 拒绝交易
            handledClose.add(p.getUniqueId());
            p.closeInventory();
            handledClose.remove(p.getUniqueId());
            p.sendMessage(Messages.color("&c已拒绝交易。"));
            if (requester != null && requester.isOnline()) {
                requester.sendMessage(Messages.color("&e" + p.getName() + " &c拒绝了你的交易请求。"));
            }
        } else if (raw == 40) {
            // 拒绝并屏蔽 8 分钟
            plugin.getTradeManager().block(holder.requester, p.getUniqueId(),
                    plugin.getTradeManager().blockMinutes());
            handledClose.add(p.getUniqueId());
            p.closeInventory();
            handledClose.remove(p.getUniqueId());
            p.sendMessage(Messages.color("&c已拒绝交易，并在 &e" + plugin.getTradeManager().blockMinutes()
                    + " 分钟 &c内屏蔽该玩家的交易请求。"));
            if (requester != null && requester.isOnline()) {
                requester.sendMessage(Messages.color("&e" + p.getName() + " &c拒绝了你的交易请求（对方已屏蔽你 "
                        + plugin.getTradeManager().blockMinutes() + " 分钟）。"));
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof TradeRequestHolder)) return;
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        if (handledClose.remove(p.getUniqueId())) return;  // 按钮操作触发的关闭，不打扰请求方
        TradeRequestHolder holder = (TradeRequestHolder) e.getInventory().getHolder();
        Player requester = Bukkit.getPlayer(holder.requester);
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(Messages.color("&e" + p.getName() + " &7关闭了交易请求窗口。"));
        }
    }
}
package nl.tinyaii.tinyplayer.warp;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 传送点 GUI 点击：点击绿色旗帜直传。
 */
public class WarpGuiListener implements Listener {

    private final TinyPlayerPlugin plugin;

    public WarpGuiListener(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() != null) return;   // 只处理 WarpCommand 用 null holder 打开的传送点菜单
        if (e.getView().getTitle() == null || !e.getView().getTitle().contains("传送点")) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasDisplayName()) return;
        String name = net.md_5.bungee.api.ChatColor.stripColor(it.getItemMeta().getDisplayName()).trim();

        WarpManager.WarpData w = plugin.getWarpManager().getWarp(name);
        if (w == null || w.toLocation() == null) {
            p.sendMessage(Messages.color("&c该传送点世界不可用。"));
            return;
        }
        p.closeInventory();
        p.teleport(w.toLocation());
        p.sendMessage(Messages.color("&a已传送到 &e" + name));
    }
}
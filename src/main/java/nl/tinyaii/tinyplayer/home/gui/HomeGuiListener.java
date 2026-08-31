package nl.tinyaii.tinyplayer.home.gui;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.home.HomeManager;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * 家列表 GUI 点击：点击床图标传送回家。
 */
public class HomeGuiListener implements Listener {

    private final TinyPlayerPlugin plugin;

    public HomeGuiListener(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof HomeHolder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasDisplayName()) return;
        String name = it.getItemMeta().getDisplayName();
        // 去掉 [公共] 前缀 / 颜色
        name = net.md_5.bungee.api.ChatColor.stripColor(name);
        name = name.replace("[公共] ", "").trim();

        HomeManager hm = plugin.getHomeManager();
        HomeManager.HomeData h = hm.getHome(p.getUniqueId(), name);
        UUID owner = p.getUniqueId();
        if (h == null) {
            // 公共家：从全服找
            for (UUID u : hm.allOwners()) {
                HomeManager.HomeData hh = hm.getHome(u, name);
                if (hh != null && hh.isPublic) { h = hh; owner = u; break; }
            }
        }
        if (h == null) { p.sendMessage(Messages.color("&c找不到该家。")); return; }
        p.closeInventory();
        Location loc = h.toLocation();
        if (loc == null || loc.getWorld() == null) { p.sendMessage(Messages.color("&c该家所在世界不可用。")); return; }
        plugin.getHomeTeleport().start(p, loc, h.name);
    }
}
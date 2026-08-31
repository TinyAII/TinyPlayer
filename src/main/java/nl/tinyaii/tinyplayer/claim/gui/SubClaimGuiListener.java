package nl.tinyaii.tinyplayer.claim.gui;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.data.Claim;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * 子领地列表点击：点某个子领地进其详情；返回回父领地详情。
 */
public class SubClaimGuiListener implements Listener {

    private final TinyPlayerPlugin plugin;

    public SubClaimGuiListener(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof SubClaimHolder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        SubClaimHolder holder = (SubClaimHolder) e.getInventory().getHolder();
        Claim parent = plugin.getClaimManager().getById(holder.parentId);
        if (parent == null) { p.closeInventory(); return; }
        int raw = e.getRawSlot();
        if (raw == 49) {
            new ClaimDetailGui(plugin, parent).open(p);
            return;
        }
        // 点击子领地 → 进入该子领地详情
        if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()
                || !e.getCurrentItem().getItemMeta().hasDisplayName()) return;
        String name = net.md_5.bungee.api.ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).trim();
        for (Claim c : plugin.getClaimManager().getChildrenOf(parent.getId())) {
            if (c.getName().equalsIgnoreCase(name)) {
                new ClaimDetailGui(plugin, c).open(p);
                return;
            }
        }
    }
}

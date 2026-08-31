package nl.tinyaii.tinyplayer.claim.gui;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.data.Claim;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 领地列表点击：
 *   - 点某个领地 → 领地详情页
 *   - 底部返回 → 全局面板
 */
public class ClaimListGuiListener implements Listener {

    private final TinyPlayerPlugin plugin;

    public ClaimListGuiListener(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof ClaimListHolder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ClaimListHolder holder = (ClaimListHolder) e.getInventory().getHolder();
        if (!holder.owner.equals(p.getUniqueId()) && !p.isOp()) {
            p.sendMessage(Messages.color("&c只能管理自己的领地。"));
            return;
        }
        int raw = e.getRawSlot();
        if (raw == 49) {
            new GlobalPanelGui(plugin, holder.owner).open(p);
            return;
        }
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasDisplayName()) return;
        String name = net.md_5.bungee.api.ChatColor.stripColor(it.getItemMeta().getDisplayName()).trim();
        for (Claim c : plugin.getClaimManager().getClaimsOf(holder.owner)) {
            if (c.getName().equalsIgnoreCase(name)) {
                new ClaimDetailGui(plugin, c).open(p);
                return;
            }
        }
    }
}

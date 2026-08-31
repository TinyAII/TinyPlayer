package nl.tinyaii.tinyplayer.claim.gui;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * 全局面板点击：
 *   - 💎 全领地权限成员（槽 22）→ 全局成员管理
 *   - 🌍 领地管理（槽 31）→ 领地列表
 */
public class GlobalPanelGuiListener implements Listener {

    private final TinyPlayerPlugin plugin;

    public GlobalPanelGuiListener(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof GlobalPanelHolder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        GlobalPanelHolder holder = (GlobalPanelHolder) e.getInventory().getHolder();
        if (!holder.owner.equals(p.getUniqueId()) && !p.isOp()) {
            p.sendMessage(Messages.color("&c只能管理自己的领地。"));
            return;
        }
        int raw = e.getRawSlot();
        if (raw == 22) {
            new MemberGui(plugin, null).openGlobalRoleSelect(p);
        } else if (raw == 31) {
            new ClaimListGui(plugin, holder.owner).open(p);
        }
    }
}

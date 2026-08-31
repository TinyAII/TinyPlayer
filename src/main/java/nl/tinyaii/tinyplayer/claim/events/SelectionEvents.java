package nl.tinyaii.tinyplayer.claim.events;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.command.SelectionManager;
import nl.tinyaii.tinyplayer.claim.data.Claim;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * 木锄头圈地：手持木锄头（WOODEN_HOE）右键方块选点。
 * 第一次右键=起点，第二次右键=终点（自动粒子预览边界）。
 * 空手/其他工具右键不干扰原版交互。
 */
public class SelectionEvents implements Listener {

    private final TinyPlayerPlugin plugin;
    private final SelectionManager selManager;

    public SelectionEvents(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
        this.selManager = plugin.getClaimSelectionManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        // 必须手持木锄头
        Material held = p.getInventory().getItemInMainHand().getType();
        if (held != Material.WOODEN_HOE && held != Material.STONE_HOE
                && held != Material.IRON_HOE && held != Material.GOLDEN_HOE
                && held != Material.DIAMOND_HOE && held != Material.NETHERITE_HOE) {
            return;
        }
        // 主手/副手各触发一次事件，只处理主手
        if (e.getHand() != EquipmentSlot.HAND) return;
        // 蹲下（潜行）+ 锄头右键 = 清除选区（不想要当前位置时快捷清空）
        if (p.isSneaking()) {
            selManager.clearSelection(p);
            p.sendMessage(Messages.color("&a已清除选区，可以重新右键选点。"));
            e.setCancelled(true);
            return;
        }
        // 在他人领地内不允许选点（防圈别人家）
        Block block = e.getClickedBlock();
        Claim claim = plugin.getClaimManager().getClaimAt(block.getWorld().getUID(),
                block.getX(), block.getY(), block.getZ());
        if (claim != null && !claim.isOwner(p.getUniqueId()) && !claim.isAdmin(p.getUniqueId())) {
            p.sendMessage(Messages.color("&c不能在领地 &e" + claim.getName() + " &c内选点。"));
            e.setCancelled(true);
            return;
        }
        e.setCancelled(true);   // 锄头右键不再锄地
        selManager.onRightClick(p, block.getLocation());
    }
}

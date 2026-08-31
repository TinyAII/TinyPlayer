package nl.tinyaii.tinyplayer.claim.gui;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.data.Claim;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * 领地详情页点击：
 *   - ⚙ 领地设置（槽 20）→ 打开 ClaimGui（flag 开关）
 *   - 🌱 子领地（槽 22）→ 子领地列表
 *   - 👑 领地管理员设置（槽 24）→ 单领地成员管理（MEMBER）
 *   - 💬 欢迎语设置（槽 30）→ 欢迎语设置
 *   - 👥 领地成员（槽 32）→ 只读信息
 *   - ⬅ 返回（槽 49）→ 领地列表
 */
public class ClaimDetailGuiListener implements Listener {

    private final TinyPlayerPlugin plugin;

    public ClaimDetailGuiListener(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof ClaimDetailHolder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ClaimDetailHolder holder = (ClaimDetailHolder) e.getInventory().getHolder();
        Claim c = plugin.getClaimManager().getById(holder.claimId);
        if (c == null) { p.closeInventory(); return; }
        // 权限：主人 / 全局权限成员 / 管理员
        if (!c.isOwner(p.getUniqueId()) && !c.isAdmin(p.getUniqueId()) && !p.isOp()
                && !plugin.getClaimManager().isGlobalAdmin(c.getOwner(), p.getUniqueId())) {
            p.sendMessage(Messages.color("&c只有主人/管理员能操作领地面板。"));
            return;
        }
        int raw = e.getRawSlot();
        switch (raw) {
            case 20: new ClaimGui(plugin, c).open(p); break;
            case 22: new SubClaimGui(plugin, c).open(p); break;
            case 24: new MemberGui(plugin, c).openSingleClaimRoleSelect(p); break;
            case 30: new WelcomeGui(plugin, c).open(p); break;
            case 32: p.sendMessage(Messages.color("&e" + c.getName() + " &a成员:")); 
                     p.sendMessage(Messages.color("&7主人: &e" + org.bukkit.Bukkit.getOfflinePlayer(c.getOwner()).getName()));
                     for (java.util.UUID u : c.getMembers().keySet()) {
                         if (u.equals(c.getOwner())) continue;
                         p.sendMessage(Messages.color("&7成员: &e" + org.bukkit.Bukkit.getOfflinePlayer(u).getName()));
                     }
                     break;
            case 34:
                // 传送至此领地
                org.bukkit.Location center = c.getCuboid().center(p.getWorld());
                p.teleport(center);
                p.sendMessage(Messages.color("&a已传送到领地 &e" + c.getName() + " &a中心。"));
                break;
            case 49: new ClaimListGui(plugin, c.getOwner()).open(p); break;
        }
    }
}

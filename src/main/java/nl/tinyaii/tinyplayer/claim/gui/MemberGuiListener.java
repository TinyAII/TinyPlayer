package nl.tinyaii.tinyplayer.claim.gui;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.data.Claim;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

/**
 * 成员管理 GUI 点击流转（全局 + 单领地）：
 *   角色选择 → 操作选择 → 添加(在线)/删除(已添加) → 二次确认 → 执行
 *   ADMIN=全局权限成员（owner 所有领地） / MEMBER=单领地成员
 * 每层返回按钮：槽 8 或最后一格 = 返回上一级。
 */
public class MemberGuiListener implements Listener {

    private final TinyPlayerPlugin plugin;

    public MemberGuiListener(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof MemberHolder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        MemberHolder holder = (MemberHolder) e.getInventory().getHolder();
        // 权限：只有 owner 本人能操作全局；单领地成员管理需 owner/全局
        if (!holder.owner.equals(p.getUniqueId()) && !p.isOp()) {
            p.sendMessage(Messages.color("&c只能管理自己的领地成员。"));
            return;
        }
        int raw = e.getRawSlot();
        int lastSlot = e.getInventory().getSize() - 1;
        MemberGui gui = new MemberGui(plugin, holder.claimId > 0 ? plugin.getClaimManager().getById(holder.claimId) : null);
        MemberGui.RoleSel role = holder.role;
        String clickedName = clickedHeadName(e.getCurrentItem());

        // 返回按钮（槽 8 或最后一格，且不是可操作槽）
        if (raw == 8 || raw == lastSlot) {
            handleBack(p, gui, holder);
            return;
        }

        switch (holder.type) {
            case ROLE_SELECT:
                if (raw == 2) gui.openOpSelect(p, MemberGui.RoleSel.ADMIN);
                else if (raw == 6) gui.openOpSelect(p, MemberGui.RoleSel.MEMBER);
                break;
            case OP_SELECT:
                if (raw == 2) gui.openAddSelect(p, role);
                else if (raw == 4) gui.openRemoveSelect(p, role);
                else if (raw == 6) gui.openList(p, role);
                break;
            case ADD_SELECT:
                if (clickedName == null) return;
                UUID target = uuidOf(clickedName);
                if (target == null) { p.sendMessage(Messages.color("&c无法识别玩家。")); return; }
                if (role == MemberGui.RoleSel.ADMIN && holder.owner.equals(target)) {
                    p.sendMessage(Messages.color("&c不能添加自己。")); return;
                }
                gui.openConfirmAdd(p, role, target);
                break;
            case CONFIRM_ADD:
                if (raw == 2) {
                    doAdd(p, holder, role);
                    p.closeInventory();
                } else if (raw == 6) {
                    gui.openAddSelect(p, role);
                }
                break;
            case REMOVE_SELECT:
                if (clickedName == null) return;
                UUID del = uuidOf(clickedName);
                if (del == null) { p.sendMessage(Messages.color("&c无法识别玩家。")); return; }
                if (holder.owner.equals(del)) { p.sendMessage(Messages.color("&c不能移除主人自己。")); return; }
                gui.openConfirmRemove(p, role, del);
                break;
            case CONFIRM_REMOVE:
                if (raw == 2) {
                    doRemove(p, holder, role);
                    p.closeInventory();
                } else if (raw == 6) {
                    gui.openRemoveSelect(p, role);
                }
                break;
            case LIST:
                gui.openOpSelect(p, role);
                break;
        }
    }

    /** 执行添加：全局 → ClaimManager.globalAdmins；单领地 → claim.members */
    private void doAdd(Player p, MemberHolder holder, MemberGui.RoleSel role) {
        String name = Bukkit.getOfflinePlayer(holder.pending).getName();
        if (role == MemberGui.RoleSel.ADMIN) {
            // 全局权限成员
            plugin.getClaimManager().addGlobalAdmin(holder.owner, holder.pending);
            plugin.getClaimStorage().saveGlobalAdmins(plugin.getClaimManager());
            p.sendMessage(Messages.color("&a已添加 &e" + name + " &a为全局权限成员（管理你的所有领地）。"));
            Player target = Bukkit.getPlayer(holder.pending);
            if (target != null) {
                target.sendMessage(Messages.color("&e" + p.getName() + " &a已把你添加为全局权限成员，可管理他的所有领地。"));
            }
        } else {
            // 单领地成员
            Claim c = plugin.getClaimManager().getById(holder.claimId);
            if (c == null) { p.sendMessage(Messages.color("&c领地不存在。")); return; }
            c.addMember(holder.pending, Claim.Role.MEMBER);
            plugin.getClaimStorage().saveClaim(c);
            p.sendMessage(Messages.color("&a已添加 &e" + name + " &a为领地 &e" + c.getName() + " &a的成员。"));
            Player target = Bukkit.getPlayer(holder.pending);
            if (target != null) {
                target.sendMessage(Messages.color("&e" + p.getName() + " &a已把你添加为领地 &e" + c.getName() + " &a的成员。"));
            }
        }
    }

    /** 执行删除：全局 → globalAdmins；单领地 → claim.members（无提示） */
    private void doRemove(Player p, MemberHolder holder, MemberGui.RoleSel role) {
        String name = Bukkit.getOfflinePlayer(holder.pending).getName();
        if (role == MemberGui.RoleSel.ADMIN) {
            plugin.getClaimManager().removeGlobalAdmin(holder.owner, holder.pending);
            plugin.getClaimStorage().saveGlobalAdmins(plugin.getClaimManager());
            p.sendMessage(Messages.color("&a已撤销 &e" + name + " &a的全局权限成员资格。"));
        } else {
            Claim c = plugin.getClaimManager().getById(holder.claimId);
            if (c == null) { p.sendMessage(Messages.color("&c领地不存在。")); return; }
            c.removeMember(holder.pending);
            plugin.getClaimStorage().saveClaim(c);
            p.sendMessage(Messages.color("&a已撤销 &e" + name + " &a的成员资格。"));
        }
    }

    /** 返回上一级 */
    private void handleBack(Player p, MemberGui gui, MemberHolder holder) {
        switch (holder.type) {
            case ROLE_SELECT:
                new GlobalPanelGui(plugin, holder.owner).open(p);
                break;
            case OP_SELECT:
                gui.openGlobalRoleSelect(p);
                break;
            case ADD_SELECT:
            case REMOVE_SELECT:
            case LIST:
                gui.openOpSelect(p, holder.role);
                break;
            case CONFIRM_ADD:
                gui.openAddSelect(p, holder.role);
                break;
            case CONFIRM_REMOVE:
                gui.openRemoveSelect(p, holder.role);
                break;
        }
    }

    private String clickedHeadName(ItemStack it) {
        if (it == null || !(it.getItemMeta() instanceof SkullMeta)) return null;
        SkullMeta meta = (SkullMeta) it.getItemMeta();
        if (meta.getOwningPlayer() != null) return meta.getOwningPlayer().getName();
        if (meta.hasDisplayName()) {
            return net.md_5.bungee.api.ChatColor.stripColor(meta.getDisplayName());
        }
        return null;
    }

    private UUID uuidOf(String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p != null) return p.getUniqueId();
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }
}

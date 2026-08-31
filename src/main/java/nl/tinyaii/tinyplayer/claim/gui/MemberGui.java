package nl.tinyaii.tinyplayer.claim.gui;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.data.Claim;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 成员管理 GUI（全局 + 单领地）：
 *   全局：owner 名下所有领地的全局权限成员（含以后新建）
 *   单领地：仅当前这个领地的成员
 * 每层带返回按钮。
 *
 * 类型流（全局）：ROLE_SELECT(全局/单领地) → OP_SELECT → ADD/REMOVE/LIST → CONFIRM
 * 类型流（单领地）：从 ClaimGui 点"单领地成员"进入 → OP_SELECT → ...
 */
public class MemberGui {

    public enum Type { ROLE_SELECT, OP_SELECT, ADD_SELECT, CONFIRM_ADD, REMOVE_SELECT, CONFIRM_REMOVE, LIST }
    public enum RoleSel { ADMIN, MEMBER }

    private final TinyPlayerPlugin plugin;
    private final Claim claim;   // null=全局模式

    public MemberGui(TinyPlayerPlugin plugin, Claim claim) {
        this.plugin = plugin;
        this.claim = claim;
    }

    public MemberGui(TinyPlayerPlugin plugin) {
        this(plugin, null);
    }

    /** 全局入口：类型选择（全局权限成员 / 单领地权限成员） */
    public void openGlobalRoleSelect(Player viewer) {
        UUID owner = viewer.getUniqueId();
        Inventory inv = Bukkit.createInventory(new MemberHolder(0, Type.ROLE_SELECT, null, null, owner), 9,
                ChatColor.DARK_GRAY + "成员管理 - 选择类型");
        inv.setItem(2, button(Material.DIAMOND, "&e全局权限成员", "&7管理你所有领地的成员（含以后新建）"));
        inv.setItem(6, button(Material.PLAYER_HEAD, "&e单领地权限成员", "&7针对某一个领地设置成员"));
        // 返回全局面板
        inv.setItem(8, backButton("&c返回全局面板"));
        viewer.openInventory(inv);
    }

    /** 单领地入口：直接从当前领地进入操作选择（MEMBER 角色，仅本领地生效） */
    public void openSingleClaimRoleSelect(Player viewer) {
        if (claim == null) { viewer.closeInventory(); return; }
        UUID owner = viewer.getUniqueId();
        Inventory inv = Bukkit.createInventory(new MemberHolder(claim.getId(), Type.OP_SELECT, RoleSel.MEMBER, null, owner), 9,
                ChatColor.DARK_GRAY + "单领地成员 - " + claim.getName());
        inv.setItem(2, button(Material.LIME_DYE, "&a添加成员", "&7选择在线玩家添加为" + claim.getName() + " 的成员"));
        inv.setItem(4, button(Material.RED_DYE, "&c删除成员", "&7选择已添加成员移除"));
        inv.setItem(6, button(Material.BOOK, "&e已添加成员列表", "&7查看当前成员"));
        inv.setItem(8, backButton("&c返回领地面板"));
        viewer.openInventory(inv);
    }

    /** 操作选择 */
    public void openOpSelect(Player viewer, RoleSel role) {
        UUID owner = viewer.getUniqueId();
        boolean global = role == RoleSel.ADMIN;
        Inventory inv = Bukkit.createInventory(new MemberHolder(0, Type.OP_SELECT, role, null, owner), 9,
                ChatColor.DARK_GRAY + (global ? "全局权限成员" : "单领地权限成员"));
        inv.setItem(2, button(Material.LIME_DYE, "&a添加成员", "&7选择在线玩家添加"));
        inv.setItem(4, button(Material.RED_DYE, "&c删除成员", "&7选择已添加成员移除"));
        inv.setItem(6, button(Material.BOOK, "&e已添加成员列表", "&7查看当前成员"));
        inv.setItem(8, backButton("&c返回类型选择"));
        viewer.openInventory(inv);
    }

    /** 添加：显示在线玩家 */
    public void openAddSelect(Player viewer, RoleSel role) {
        UUID owner = viewer.getUniqueId();
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        int size = Math.max(9, ((Math.max(online.size(), 1) + 8) / 9) * 9);
        size = Math.min(54, Math.max(9, size));
        Inventory inv = Bukkit.createInventory(new MemberHolder(0, Type.ADD_SELECT, role, null, owner), size,
                ChatColor.DARK_GRAY + "添加" + (role == RoleSel.ADMIN ? "全局权限成员" : "单领地成员"));
        int slot = 0;
        for (Player p : online) {
            if (slot + 1 >= size) break;   // 留最后一格放返回
            inv.setItem(slot++, headItem(p.getName(), "&a" + p.getName(),
                    "&7点击添加为" + (role == RoleSel.ADMIN ? "全局权限成员" : "单领地成员")));
        }
        inv.setItem(size - 1, backButton("&c返回"));
        viewer.openInventory(inv);
    }

    /** 删除：显示已添加成员 */
    public void openRemoveSelect(Player viewer, RoleSel role) {
        UUID owner = viewer.getUniqueId();
        List<UUID> members = role == RoleSel.ADMIN
                ? new ArrayList<>(plugin.getClaimManager().getGlobalAdmins(owner))
                : currentClaimMembers();
        int size = Math.max(9, ((Math.max(members.size(), 1) + 8) / 9) * 9);
        size = Math.min(54, Math.max(9, size));
        Inventory inv = Bukkit.createInventory(new MemberHolder(0, Type.REMOVE_SELECT, role, null, owner), size,
                ChatColor.DARK_GRAY + "删除" + (role == RoleSel.ADMIN ? "全局权限成员" : "成员"));
        int slot = 0;
        for (UUID u : members) {
            if (slot + 1 >= size) break;
            String name = Bukkit.getOfflinePlayer(u).getName();
            inv.setItem(slot++, headItem(name, "&c" + (name == null ? u.toString() : name), "&7点击撤销权限"));
        }
        inv.setItem(size - 1, backButton("&c返回"));
        viewer.openInventory(inv);
    }

    /** 已添加成员列表（只读） */
    public void openList(Player viewer, RoleSel role) {
        UUID owner = viewer.getUniqueId();
        List<UUID> members = role == RoleSel.ADMIN
                ? new ArrayList<>(plugin.getClaimManager().getGlobalAdmins(owner))
                : currentClaimMembers();
        int size = Math.max(9, ((Math.max(members.size(), 1) + 8) / 9) * 9);
        size = Math.min(54, Math.max(9, size));
        Inventory inv = Bukkit.createInventory(new MemberHolder(0, Type.LIST, role, null, owner), size,
                ChatColor.DARK_GRAY + "已添加" + (role == RoleSel.ADMIN ? "全局权限成员" : "成员"));
        int slot = 0;
        for (UUID u : members) {
            if (slot + 1 >= size) break;
            String name = Bukkit.getOfflinePlayer(u).getName();
            inv.setItem(slot++, headItem(name, "&e" + (name == null ? u.toString() : name), "&7已添加"));
        }
        inv.setItem(size - 1, backButton("&c返回"));
        viewer.openInventory(inv);
    }

    /** 确认添加 */
    public void openConfirmAdd(Player viewer, RoleSel role, UUID target) {
        UUID owner = viewer.getUniqueId();
        Inventory inv = Bukkit.createInventory(new MemberHolder(0, Type.CONFIRM_ADD, role, target, owner), 9,
                ChatColor.DARK_GRAY + "确认添加");
        String name = Bukkit.getOfflinePlayer(target).getName();
        inv.setItem(2, button(Material.LIME_DYE, "&a确认添加",
                "&7添加 " + (name == null ? target.toString() : name) + " 为" + (role == RoleSel.ADMIN ? "全局权限成员" : "单领地成员")));
        inv.setItem(4, headItem(name, "&e" + (name == null ? target.toString() : name),
                "&7确认是否真的要添加为" + (role == RoleSel.ADMIN ? "全局管理员" : "成员")));
        inv.setItem(6, backButton("&c取消返回"));
        viewer.openInventory(inv);
    }

    /** 确认删除 */
    public void openConfirmRemove(Player viewer, RoleSel role, UUID target) {
        UUID owner = viewer.getUniqueId();
        Inventory inv = Bukkit.createInventory(new MemberHolder(0, Type.CONFIRM_REMOVE, role, target, owner), 9,
                ChatColor.DARK_GRAY + "确认删除");
        String name = Bukkit.getOfflinePlayer(target).getName();
        inv.setItem(2, button(Material.LIME_DYE, "&a确认撤销",
                "&7撤销 " + (name == null ? target.toString() : name) + " 的权限"));
        inv.setItem(4, headItem(name, "&e" + (name == null ? target.toString() : name), "&7确认是否真的要撤销他的权限"));
        inv.setItem(6, backButton("&c取消返回"));
        viewer.openInventory(inv);
    }

    // ===== 单领地成员（claim != null 时的 MEMBER 列表）=====
    private List<UUID> currentClaimMembers() {
        List<UUID> out = new ArrayList<>();
        if (claim == null) return out;
        for (UUID u : claim.getMembers().keySet()) {
            if (u.equals(claim.getOwner())) continue;
            if (claim.getMembers().get(u) == Claim.Role.MEMBER) out.add(u);
        }
        return out;
    }

    // ===== 工具 =====
    private ItemStack button(Material mat, String name, String lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(name));
            List<String> l = new ArrayList<>();
            l.add(Messages.color(lore));
            meta.setLore(l);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack backButton(String name) {
        ItemStack it = new ItemStack(Material.ARROW);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(Messages.color(name)); it.setItemMeta(meta); }
        return it;
    }

    private ItemStack headItem(String ownerName, String displayName, String lore) {
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) it.getItemMeta();
        if (meta != null) {
            if (ownerName != null) meta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerName));
            meta.setDisplayName(Messages.color(displayName));
            List<String> l = new ArrayList<>();
            l.add(Messages.color(lore));
            meta.setLore(l);
            it.setItemMeta(meta);
        }
        return it;
    }
}

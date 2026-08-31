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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 领地详情页：显示主人 + 其他成员，五个功能按钮。
 *   [领地设置]（flag 开关：火焰蔓延/苦力怕爆炸等）
 *   [子领地]（点击显示所有子领地）
 *   [领地管理员设置]（给该领地管理员权限）
 *   [欢迎语设置]（设置欢迎语和退出语）
 *   [成员信息]（显示主人和其他成员）
 */
public class ClaimDetailGui {

    private static final int SETTING_SLOT = 20;
    private static final int CHILD_SLOT = 22;
    private static final int ADMIN_SLOT = 24;
    private static final int WELCOME_SLOT = 30;
    private static final int MEMBER_SLOT = 32;
    private static final int TP_SLOT = 34;
    private static final int BACK_SLOT = 49;

    private final TinyPlayerPlugin plugin;
    private final Claim claim;

    public ClaimDetailGui(TinyPlayerPlugin plugin, Claim claim) {
        this.plugin = plugin;
        this.claim = claim;
    }

    public void open(Player viewer) {
        Inventory inv = Bukkit.createInventory(new ClaimDetailHolder(claim.getId()), 54, ChatColor.DARK_GRAY + "领地: " + claim.getName());

        // 全屏灰玻璃
        for (int i = 0; i < 54; i++) inv.setItem(i, glass());

        // 顶部标题条
        inv.setItem(4, titleItem());

        // 第一行三个功能按钮（20/22/24）
        inv.setItem(SETTING_SLOT, settingItem());
        inv.setItem(CHILD_SLOT, childItem());
        inv.setItem(ADMIN_SLOT, adminItem());

        // 第二行两个按钮 + 成员信息（30/32）+ 传送（34）
        inv.setItem(WELCOME_SLOT, welcomeItem());
        inv.setItem(MEMBER_SLOT, memberItem());
        inv.setItem(TP_SLOT, tpItem());

        // 底部返回
        inv.setItem(BACK_SLOT, backItem());

        viewer.openInventory(inv);
    }

    private ItemStack titleItem() {
        ItemStack it = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&e" + claim.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7范围: &e" + claim.getCuboid()));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack settingItem() {
        ItemStack it = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&a⚙ 领地设置"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7开启/关闭领地功能"));
            lore.add(Messages.color("&7火焰蔓延/苦力怕爆炸等"));
            lore.add(Messages.color("&a点击进入"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack childItem() {
        ItemStack it = new ItemStack(Material.SPRUCE_SAPLING);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&a🌱 子领地"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7查看/管理子领地"));
            lore.add(Messages.color("&a点击进入"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack adminItem() {
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&b👑 领地管理员设置"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7给该领地添加管理员权限"));
            lore.add(Messages.color("&7（仅对本领地生效）"));
            lore.add(Messages.color("&a点击进入"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack welcomeItem() {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&e💬 欢迎语设置"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7设置进入欢迎语和退出语"));
            lore.add(Messages.color("&a点击进入"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack memberItem() {
        ItemStack it = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&e👥 领地成员"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7主人: &e" + Bukkit.getOfflinePlayer(claim.getOwner()).getName()));
            for (UUID u : claim.getMembers().keySet()) {
                if (u.equals(claim.getOwner())) continue;
                lore.add(Messages.color("&7成员: &e" + Bukkit.getOfflinePlayer(u).getName()));
            }
            if (claim.getMembers().size() <= 1) lore.add(Messages.color("&7（无其他成员）"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack tpItem() {
        ItemStack it = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&d⬛ 传送至此领地"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7传送到该领地中心"));
            lore.add(Messages.color("&a点击传送"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack backItem() {
        ItemStack it = new ItemStack(Material.ARROW);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(Messages.color("&c⬅ 返回领地列表")); it.setItemMeta(meta); }
        return it;
    }

    private ItemStack glass() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); it.setItemMeta(meta); }
        return it;
    }
}

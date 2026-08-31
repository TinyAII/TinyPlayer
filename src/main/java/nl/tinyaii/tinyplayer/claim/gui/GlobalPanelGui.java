package nl.tinyaii.tinyplayer.claim.gui;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
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
 * 全局面板（大 GUI）：两个按钮。
 *   [💎 全领地权限成员]（管理所有领地的成员，含以后新建）
 *   [🌍 领地管理]（列出名下所有领地，点击进入详情）
 * 灰玻璃铺底美化排版。
 */
public class GlobalPanelGui {

    private static final int GLOBAL_ADMIN_SLOT = 22;
    private static final int CLAIM_MANAGE_SLOT = 31;

    private final TinyPlayerPlugin plugin;
    private final UUID owner;

    public GlobalPanelGui(TinyPlayerPlugin plugin, UUID owner) {
        this.plugin = plugin;
        this.owner = owner;
    }

    public void open(Player viewer) {
        Inventory inv = Bukkit.createInventory(new GlobalPanelHolder(owner), 54, ChatColor.DARK_GRAY + "全局面板");

        // 全屏铺灰玻璃
        for (int i = 0; i < 54; i++) inv.setItem(i, glass());

        // 顶部标题条
        inv.setItem(4, titleItem());

        // 两个大按钮（居中，隔开）
        inv.setItem(GLOBAL_ADMIN_SLOT, globalAdminItem());
        inv.setItem(CLAIM_MANAGE_SLOT, claimManageItem());

        // 装饰：玻璃柱边框（第 5 行左右）
        inv.setItem(18, border());
        inv.setItem(26, border());
        inv.setItem(36, border());
        inv.setItem(44, border());

        viewer.openInventory(inv);
    }

    private ItemStack titleItem() {
        ItemStack it = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&e⚙ 全局面板"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7管理名下所有领地"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack globalAdminItem() {
        ItemStack it = new ItemStack(Material.DIAMOND);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&b💎 全领地权限成员"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7管理你所有领地的权限成员"));
            lore.add(Messages.color("&7（包括以后新建的领地）"));
            lore.add(Messages.color("&a点击进入"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack claimManageItem() {
        ItemStack it = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&a🌍 领地管理"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7列出名下所有领地"));
            lore.add(Messages.color("&7点击进入领地详情"));
            lore.add(Messages.color("&a点击进入"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack border() {
        ItemStack it = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); it.setItemMeta(meta); }
        return it;
    }

    private ItemStack glass() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); it.setItemMeta(meta); }
        return it;
    }
}

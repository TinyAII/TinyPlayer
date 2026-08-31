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
 * 领地管理：列出名下所有领地（大 GUI），点击某领地进入详情页。
 */
public class ClaimListGui {

    private final TinyPlayerPlugin plugin;
    private final UUID owner;

    public ClaimListGui(TinyPlayerPlugin plugin, UUID owner) {
        this.plugin = plugin;
        this.owner = owner;
    }

    public void open(Player viewer) {
        List<Claim> claims = plugin.getClaimManager().getClaimsOf(owner);
        int size = 54;   // 大 GUI
        Inventory inv = Bukkit.createInventory(new ClaimListHolder(owner), size, ChatColor.DARK_GRAY + "领地管理");

        // 全屏灰玻璃
        for (int i = 0; i < size; i++) inv.setItem(i, glass());

        // 顶部标题
        inv.setItem(4, titleItem());

        // 领地列表从第 2 行开始（9-44，36 格）
        int slot = 9;
        for (Claim c : claims) {
            if (slot >= 45) break;
            inv.setItem(slot++, claimItem(c));
        }
        if (claims.isEmpty()) {
            inv.setItem(22, emptyItem());
        }

        // 底部返回
        inv.setItem(49, backItem());

        viewer.openInventory(inv);
    }

    private ItemStack titleItem() {
        ItemStack it = new ItemStack(Material.MAP);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&e🌍 领地管理"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7点击某个领地进入详情"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack claimItem(Claim c) {
        ItemStack it = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&e" + c.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7范围: &e" + c.getCuboid()));
            lore.add(Messages.color("&7成员: &e" + c.getMembers().size()));
            lore.add(Messages.color("&a点击管理"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack emptyItem() {
        ItemStack it = new ItemStack(Material.BARRIER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&c还没有领地"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7用木锄头选点或 /领地 圈地 创建"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack backItem() {
        ItemStack it = new ItemStack(Material.ARROW);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(Messages.color("&c⬅ 返回全局面板")); it.setItemMeta(meta); }
        return it;
    }

    private ItemStack glass() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); it.setItemMeta(meta); }
        return it;
    }
}

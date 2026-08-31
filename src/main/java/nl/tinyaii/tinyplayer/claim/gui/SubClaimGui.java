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

/**
 * 子领地列表：显示该领地的所有子领地。
 */
public class SubClaimGui {

    private final TinyPlayerPlugin plugin;
    private final Claim parent;

    public SubClaimGui(TinyPlayerPlugin plugin, Claim parent) {
        this.plugin = plugin;
        this.parent = parent;
    }

    public void open(Player viewer) {
        List<Claim> children = plugin.getClaimManager().getChildrenOf(parent.getId());
        int size = 54;
        Inventory inv = Bukkit.createInventory(new SubClaimHolder(parent.getId()), size, ChatColor.DARK_GRAY + "子领地 - " + parent.getName());

        for (int i = 0; i < size; i++) inv.setItem(i, glass());

        inv.setItem(4, titleItem());

        int slot = 9;
        for (Claim c : children) {
            if (slot >= 45) break;
            inv.setItem(slot++, childItem(c));
        }
        if (children.isEmpty()) {
            inv.setItem(22, emptyItem());
        }

        inv.setItem(49, backItem());

        viewer.openInventory(inv);
    }

    private ItemStack titleItem() {
        ItemStack it = new ItemStack(Material.SPRUCE_SAPLING);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&a🌱 子领地"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7用 /领地 子 创建 <名> 添加"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack childItem(Claim c) {
        ItemStack it = new ItemStack(Material.OAK_SAPLING);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&e" + c.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7范围: &e" + c.getCuboid()));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack emptyItem() {
        ItemStack it = new ItemStack(Material.BARRIER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&c暂无子领地"));
            meta.setLore(List.of(Messages.color("&7用 /领地 子 创建 <名> 添加")));
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack backItem() {
        ItemStack it = new ItemStack(Material.ARROW);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(Messages.color("&c⬅ 返回领地详情")); it.setItemMeta(meta); }
        return it;
    }

    private ItemStack glass() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); it.setItemMeta(meta); }
        return it;
    }
}

package nl.tinyaii.tinyplayer.home.gui;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.home.HomeManager;
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
 * 家列表 GUI：床图标 + 世界/坐标 Lore，点击即传。
 * 绿床=公共家，红床=自己家。
 */
public class HomeGui {

    private final TinyPlayerPlugin plugin;
    private final UUID owner;

    public HomeGui(TinyPlayerPlugin plugin, UUID owner) {
        this.plugin = plugin;
        this.owner = owner;
    }

    public void open(Player viewer) {
        List<HomeManager.HomeData> list = plugin.getHomeManager().getHomes(owner);
        // 再加上全服公共家（他人公开的家）
        List<HomeManager.HomeData> publics = new ArrayList<>();
        for (UUID u : plugin.getHomeManager().allOwners()) {
            if (u.equals(owner)) continue;
            for (HomeManager.HomeData h : plugin.getHomeManager().getHomes(u)) {
                if (h.isPublic) publics.add(h);
            }
        }

        int total = list.size() + publics.size();
        int size = Math.max(9, ((Math.max(total, 1) + 8) / 9) * 9);
        size = Math.min(54, Math.max(9, size));
        Inventory inv = Bukkit.createInventory(new HomeHolder(owner), size, ChatColor.DARK_GRAY + "家列表");

        int slot = 0;
        for (HomeManager.HomeData h : list) {
            if (slot >= size) break;
            inv.setItem(slot++, bedItem(h, false));
        }
        for (HomeManager.HomeData h : publics) {
            if (slot >= size) break;
            inv.setItem(slot++, bedItem(h, true));
        }
        viewer.openInventory(inv);
    }

    private ItemStack bedItem(HomeManager.HomeData h, boolean isPublic) {
        ItemStack it = new ItemStack(isPublic ? Material.GREEN_BED : Material.RED_BED);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color((isPublic ? "&a[公共] " : "&c") + h.name));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7世界: &e" + h.world));
            lore.add(Messages.color("&7坐标: &e" + (int) h.x + ", " + (int) h.y + ", " + (int) h.z));
            lore.add(Messages.color("&7点击传送到家"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }
}
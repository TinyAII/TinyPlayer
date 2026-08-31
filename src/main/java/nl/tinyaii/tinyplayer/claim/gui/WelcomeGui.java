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
 * 欢迎语设置：设置进入欢迎语和退出语。
 * 输入用聊天命令（GUI 无法输入文本）：
 *   /领地 欢迎语 <消息>  /领地 退出语 <消息>
 */
public class WelcomeGui {

    private final TinyPlayerPlugin plugin;
    private final Claim claim;

    public WelcomeGui(TinyPlayerPlugin plugin, Claim claim) {
        this.plugin = plugin;
        this.claim = claim;
    }

    public void open(Player viewer) {
        Inventory inv = Bukkit.createInventory(new WelcomeHolder(claim.getId()), 27, ChatColor.DARK_GRAY + "欢迎语设置");

        // 全屏灰玻璃
        for (int i = 0; i < 27; i++) inv.setItem(i, glass());

        // 标题
        inv.setItem(4, titleItem());

        // 当前欢迎语 / 当前退出语
        inv.setItem(10, welcomeViewItem());
        inv.setItem(16, leaveViewItem());

        // 设置按钮（提示用命令输入）
        inv.setItem(12, setWelcomeItem());
        inv.setItem(14, setLeaveItem());

        // 返回
        inv.setItem(26, backItem());

        viewer.openInventory(inv);
    }

    private ItemStack titleItem() {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&e💬 欢迎语设置"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7设置进入欢迎语和退出语"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack welcomeViewItem() {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&a当前进入欢迎语"));
            List<String> lore = new ArrayList<>();
            String w = claim.getWelcomeMsg() == null
                    ? plugin.getConfig().getString("claim.settings.default-welcome", "欢迎来到")
                    : claim.getWelcomeMsg();
            lore.add(Messages.color("&f" + w));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack leaveViewItem() {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&a当前退出欢送语"));
            List<String> lore = new ArrayList<>();
            String l = claim.getLeaveMsg() == null
                    ? plugin.getConfig().getString("claim.settings.default-leave", "欢迎下次光临")
                    : claim.getLeaveMsg();
            lore.add(Messages.color("&f" + l));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack setWelcomeItem() {
        ItemStack it = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&a设置欢迎语"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7输入: &e/领地 欢迎语 <消息>"));
            lore.add(Messages.color("&7输入: &e/领地 欢迎语 - &7清除"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack setLeaveItem() {
        ItemStack it = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&a设置退出语"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7输入: &e/领地 退出语 <消息>"));
            lore.add(Messages.color("&7输入: &e/领地 退出语 - &7清除"));
            meta.setLore(lore);
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

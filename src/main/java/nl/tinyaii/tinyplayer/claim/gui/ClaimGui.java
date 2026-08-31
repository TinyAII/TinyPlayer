package nl.tinyaii.tinyplayer.claim.gui;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.data.Claim;
import nl.tinyaii.tinyplayer.claim.flags.Flags;
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
 * 领地面板 GUI：显示领地信息 + flag 开关（点击切换）+ 成员列表 + 子领地。
 */
public class ClaimGui {

    private final TinyPlayerPlugin plugin;
    private final Claim claim;

    public ClaimGui(TinyPlayerPlugin plugin, Claim claim) {
        this.plugin = plugin;
        this.claim = claim;
    }

    public void open(Player viewer) {
        Inventory inv = Bukkit.createInventory(new ClaimHolder(claim.getId()), 54, ChatColor.DARK_GRAY + "领地: " + claim.getName());

        // 信息栏（0-8 行）
        inv.setItem(0, infoItem());

        // Flag 开关（9-43，最多 22 个 flag）
        int slot = 9;
        for (String name : Flags.PRI_FLAGS.keySet()) {
            if (slot >= 44) break;
            inv.setItem(slot++, flagItem(name, true));
        }
        for (String name : Flags.ENV_FLAGS.keySet()) {
            if (slot >= 44) break;
            inv.setItem(slot++, flagItem(name, false));
        }

        // 底部：单领地成员（45-47）+ 子领地（48-50）+ 返回（52）+ 关闭（53）
        inv.setItem(45, memberItem());
        inv.setItem(48, childrenItem());
        inv.setItem(52, backItem());
        inv.setItem(53, closeItem());

        viewer.openInventory(inv);
    }

    private boolean resolve(String name) {
        Boolean overridden = claim.getFlagOverride(name);
        if (overridden != null) return overridden;
        boolean def = Flags.ENV_FLAGS.containsKey(name)
                ? plugin.getConfig().getBoolean("claim.flags.default-environment." + name, Flags.ENV_FLAGS.get(name))
                : plugin.getConfig().getBoolean("claim.flags.default-privilege." + name, Flags.PRI_FLAGS.get(name));
        return def;
    }

    private ItemStack infoItem() {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&e" + claim.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7主人: &e" + Bukkit.getOfflinePlayer(claim.getOwner()).getName()));
            lore.add(Messages.color("&7成员: &e" + claim.getMembers().size()));
            lore.add(Messages.color("&7范围: &e" + claim.getCuboid()));
            lore.add(Messages.color("&7欢迎语: &f" + (claim.getWelcomeMsg() == null ? "默认" : claim.getWelcomeMsg())));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack flagItem(String name, boolean isPri) {
        boolean val = resolve(name);
        ItemStack it = new ItemStack(val ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color((val ? "&a" : "&c") + Flags.displayName(name)));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7类型: " + (isPri ? "权限" : "环境")));
            lore.add(Messages.color("&7当前: " + (val ? "&a开" : "&c关")));
            lore.add(Messages.color("&8键名: " + name));
            lore.add(Messages.color("&7点击切换"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack memberItem() {
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&e单领地成员"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7仅对本领地生效的成员"));
            lore.add(Messages.color("&7点击进入"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack backItem() {
        ItemStack it = new ItemStack(Material.ARROW);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(Messages.color("&c返回全局面板")); it.setItemMeta(meta); }
        return it;
    }

    private ItemStack childrenItem() {
        ItemStack it = new ItemStack(Material.SPRUCE_SAPLING);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&e子领地"));
            List<String> lore = new ArrayList<>();
            for (Claim c : plugin.getClaimManager().getChildrenOf(claim.getId())) {
                lore.add(Messages.color("&7- &e" + c.getName()));
            }
            if (lore.isEmpty()) lore.add(Messages.color("&7无"));
            lore.add(Messages.color("&7用 /领地 子 创建 <名> 添加"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack closeItem() {
        ItemStack it = new ItemStack(Material.BARRIER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(Messages.color("&c关闭")); it.setItemMeta(meta); }
        return it;
    }
}

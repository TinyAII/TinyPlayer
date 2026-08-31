package nl.tinyaii.tinyplayer.trade;

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
 * 交易请求确认 GUI：当玩家潜行右键 / 点击交易时，
 * 如果对方还没进入交易面板，则给对方弹一个 3 按钮确认界面：
 *   [✔ 同意交易] / [✘ 拒绝交易] / [🛡 拒绝并屏蔽 8 分钟]
 */
public class TradeRequestGui {

    private static final int ACCEPT_SLOT = 22;
    private static final int REJECT_SLOT = 31;
    private static final int REJECT_BLOCK_SLOT = 40;

    private final TinyPlayerPlugin plugin;
    private final UUID requester, target;

    public TradeRequestGui(TinyPlayerPlugin plugin, UUID requester, UUID target) {
        this.plugin = plugin;
        this.requester = requester;
        this.target = target;
    }

    /** 给被请求方弹确认 GUI */
    public void open(Player viewer) {
        Inventory inv = Bukkit.createInventory(new TradeRequestHolder(requester, target), 54,
                ChatColor.DARK_GRAY + "交易请求");
        for (int i = 0; i < 54; i++) inv.setItem(i, glass());

        String rName = (Bukkit.getPlayer(requester) != null ? Bukkit.getPlayer(requester).getName() : "对方");

        inv.setItem(13, infoItem(rName));
        inv.setItem(ACCEPT_SLOT, button(Material.LIME_DYE, "&a✔ 同意交易", "&7点击开始交易"));
        inv.setItem(REJECT_SLOT, button(Material.RED_DYE, "&c✘ 拒绝交易", "&7仅拒绝本次"));
        inv.setItem(REJECT_BLOCK_SLOT, button(Material.BARRIER, "&c🛡 拒绝并屏蔽 8 分钟", "&7期间对方无法再向你发请求"));

        viewer.openInventory(inv);
    }

    private ItemStack infoItem(String rName) {
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&e" + rName + " &a想与你交易"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7潜行右键也可发起交易"));
            lore.add(Messages.color("&7请选择下方操作："));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

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

    private ItemStack glass() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); it.setItemMeta(meta); }
        return it;
    }

    static boolean isAcceptSlot(int raw) { return raw == ACCEPT_SLOT; }
    static boolean isRejectSlot(int raw) { return raw == REJECT_SLOT; }
    static boolean isRejectBlockSlot(int raw) { return raw == REJECT_BLOCK_SLOT; }
}
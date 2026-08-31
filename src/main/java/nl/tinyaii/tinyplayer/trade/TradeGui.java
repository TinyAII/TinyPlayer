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

/**
 * 交易 GUI：54 格，双方共享同一 Inventory（固定视角布局）。
 * 布局（6 行 × 9 列）：
 *   A 区 = 左 4 列（20 格）   B 区 = 右 4 列（20 格）
 *   中间第 5 列 = 灰玻璃分隔（5 格）
 *   底部行：45=A 确认 / 46-52=状态 / 53=B 确认
 * 按钮与固定槽位绑定：左下永远是 A(1号) 的，右下永远是 B(2号) 的。
 */
public class TradeGui {

    static final int[] A_SLOTS = {0,1,2,3, 9,10,11,12, 18,19,20,21, 27,28,29,30, 36,37,38,39};
    static final int[] B_SLOTS = {5,6,7,8, 14,15,16,17, 23,24,25,26, 32,33,34,35, 41,42,43,44};
    private static final int[] MID_COL = {4,13,22,31,40};
    private static final int A_CONFIRM = 45, B_CONFIRM = 53;
    private static final int STATUS_START = 46, STATUS_END = 52;

    private final TinyPlayerPlugin plugin;

    public TradeGui(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /** 双方共享同一面板打开交易 */
    public void open(Player pa, Player pb, TradeManager.TradeSession s) {
        s.inv = Bukkit.createInventory(new TradeHolder(s.a, s.b), 54, ChatColor.DARK_GRAY + "交易");
        refresh(s);
        pa.openInventory(s.inv);
        pb.openInventory(s.inv);
        pa.sendMessage(Messages.color("&a交易已开启：&e左半=你（" + pa.getName() + "）&a，&e右半=" + pb.getName() + "&a。左下是你的确认按钮。"));
        pb.sendMessage(Messages.color("&a交易已开启：&e左半=" + pa.getName() + "&a，&e右半=你（" + pb.getName() + "）&a。右下是你的确认按钮。"));
    }

    /** 刷新面板：只重建分隔列 + 确认按钮 + 状态行，绝不触碰交易区（防物品丢失） */
    public void refresh(TradeManager.TradeSession s) {
        if (s.inv == null) return;
        Inventory inv = s.inv;
        String nameA = (Bukkit.getPlayer(s.a) != null ? Bukkit.getPlayer(s.a).getName() : "1号");
        String nameB = (Bukkit.getPlayer(s.b) != null ? Bukkit.getPlayer(s.b).getName() : "2号");

        // 中间分隔列：灰玻璃
        for (int i : MID_COL) inv.setItem(i, glass());
        // 底部行铺底
        for (int i = 45; i < 54; i++) inv.setItem(i, glass());

        // 确认按钮：槽 45 = A(1号,左) 的，槽 53 = B(2号,右) 的 —— 按固定槽位显示各自状态
        inv.setItem(A_CONFIRM, confirmButton(nameA, s.aReady));
        inv.setItem(B_CONFIRM, confirmButton(nameB, s.bReady));

        // 全局状态
        String status;
        if (s.done) status = "&a✓ 交易完成";
        else if (s.aReady && s.bReady) {
            int left = confirmSeconds() - (int) ((System.currentTimeMillis() - s.confirmStart) / 1000L);
            status = "&e完成中... &f" + Math.max(0, left) + "&e 秒";
        } else if (s.aReady) status = "&e等待 " + nameB + " 确认...";
        else if (s.bReady) status = "&e等待 " + nameA + " 确认...";
        else status = "&8双方放入物品后点击确认";
        for (int i = STATUS_START; i <= STATUS_END; i++) inv.setItem(i, statusItem(status));
    }

    private int confirmSeconds() { return plugin.getTradeManager().confirmSeconds(); }

    /** 确认按钮：显示该槽位主人的状态（固定槽位视角，双方看到一致） */
    private ItemStack confirmButton(String owner, boolean ready) {
        ItemStack it = new ItemStack(ready ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(ready ? "&a✔ 已确认" : "&a点击确认"));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7这是 &e" + owner + " &7的确认按钮"));
            if (ready) lore.add(Messages.color("&7已确认，等待完成"));
            else lore.add(Messages.color("&7点击确认交易"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack statusItem(String status) {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(Messages.color(status)); it.setItemMeta(meta); }
        return it;
    }

    private ItemStack glass() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); it.setItemMeta(meta); }
        return it;
    }

    /** 判断槽位归属（供 Handler 使用） */
    static boolean inA(int raw) { for (int i : A_SLOTS) if (i == raw) return true; return false; }
    static boolean inB(int raw) { for (int i : B_SLOTS) if (i == raw) return true; return false; }
}
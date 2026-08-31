package nl.tinyaii.tinyplayer.trade;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 交易核心逻辑（54 格共享面板）。
 * 布局：A=左 20 格、B=右 20 格、中间第 5 列灰玻璃、底部确认/状态行。
 * 自己区域可自由放/取；确认后锁定（防改价）；双方确认 → 倒计时 → 完成互换。
 */
public class TradeHandler implements Listener {

    private final TinyPlayerPlugin plugin;
    private final TradeGui gui;

    private static final int A_CONFIRM = 45, B_CONFIRM = 53;

    public TradeHandler(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
        this.gui = new TradeGui(plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof TradeHolder)) return;
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        TradeHolder holder = (TradeHolder) e.getInventory().getHolder();
        TradeManager.TradeSession s = plugin.getTradeManager().session(p.getUniqueId());
        if (s == null || s.done) { e.setCancelled(true); return; }

        boolean isA = s.isA(p.getUniqueId());
        int raw = e.getRawSlot();
        boolean inTop = raw >= 0 && raw < 54;

        if (!inTop) {
            // 背包区：禁止 shift 进交易面板
            if (e.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_LEFT
                    || e.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) {
                e.setCancelled(true);
            }
            return;
        }

        // 确认按钮
        if (raw == (isA ? A_CONFIRM : B_CONFIRM)) {
            e.setCancelled(true);
            toggleReady(s, p);
            return;
        }
        // 中间分隔列 / 状态行 / 对方区域：禁止
        boolean myZone = isA ? TradeGui.inA(raw) : TradeGui.inB(raw);
        if (!myZone) {
            e.setCancelled(true);
            return;
        }
        // 自己的交易区
        if (e.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_LEFT
                || e.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) {
            e.setCancelled(true);
            return;
        }
        // 已确认则锁定（防改价）
        boolean meReady = isA ? s.aReady : s.bReady;
        if (meReady) {
            e.setCancelled(true);
            p.sendMessage(Messages.color("&c你已确认，取消确认后再改动物品。"));
        }
        // 未确认：允许自由放/取
    }

    private void toggleReady(TradeManager.TradeSession s, Player p) {
        boolean isA = s.isA(p.getUniqueId());
        boolean meReady = isA ? s.aReady : s.bReady;
        if (meReady) {
            // 取消确认
            if (isA) s.aReady = false; else s.bReady = false;
            s.confirmStart = 0;
            p.sendMessage(Messages.color("&c已取消确认，可以调整物品。"));
        } else {
            if (isA) s.aReady = true; else s.bReady = true;
            p.sendMessage(Messages.color("&a已确认！等待对方确认..."));
            Player other = Bukkit.getPlayer(s.other(p.getUniqueId()));
            if (other != null) other.sendMessage(Messages.color("&e对方已确认。"));
        }
        // 双方都确认 → 开始倒计时
        if (s.aReady && s.bReady && s.confirmStart == 0) {
            s.confirmStart = System.currentTimeMillis();
            Player a = Bukkit.getPlayer(s.a), b = Bukkit.getPlayer(s.b);
            String msg = Messages.color("&a双方已确认！&e" + plugin.getTradeManager().confirmSeconds() + " &a秒后完成，请保持界面打开。");
            if (a != null) a.sendMessage(msg);
            if (b != null) b.sendMessage(msg);
        }
        gui.refresh(s);
    }

    /** 交易完成倒计时检查（每秒）：更新双方屏幕倒计时，倒计时归零则完成 */
    public void tickCompletions() {
        for (java.util.UUID u : new java.util.ArrayList<>(plugin.getTradeManager().sessions().keySet())) {
            TradeManager.TradeSession s = plugin.getTradeManager().session(u);
            if (s == null || s.done) continue;
            if (!s.isA(u)) continue;   // sessions 里 a/b 两个 key 指向同一 session，只处理一次（以 a 为准）
            if (!s.aReady || !s.bReady) continue;
            if (s.confirmStart == 0) continue;

            long elapsed = System.currentTimeMillis() - s.confirmStart;
            int left = confirmSeconds() - (int) (elapsed / 1000L);
            if (left <= 0) {
                complete(s);
                continue;
            }
            // 每秒刷新面板状态 + 聊天栏倒计时（只有消息栏不被交易面板遮挡）
            gui.refresh(s);
            Player a = Bukkit.getPlayer(s.a), b = Bukkit.getPlayer(s.b);
            String bar = Messages.color("&e[交易] &f" + left + " &e秒后完成，请保持界面打开");
            if (a != null) a.sendMessage(bar);
            if (b != null) b.sendMessage(bar);
        }
    }

    private int confirmSeconds() { return plugin.getTradeManager().confirmSeconds(); }

    /** ActionBar 消息（跨版本兼容：1.16 用包，1.17+ 用原版 API） */
    private void sendActionBar(Player p, String msg) {
        try {
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(msg));
        } catch (Throwable t) {
            // 兜底：版本过旧则退化为聊天消息
            p.sendMessage(msg);
        }
    }

    private void complete(TradeManager.TradeSession s) {
        s.done = true;
        Player a = Bukkit.getPlayer(s.a), b = Bukkit.getPlayer(s.b);
        Inventory inv = s.inv;
        if (inv != null) {
            // 读取双方交易区物品
            List<ItemStack> fromA = new java.util.ArrayList<>();
            List<ItemStack> fromB = new java.util.ArrayList<>();
            for (int i = 0; i < 20; i++) {
                fromA.add(inv.getItem(TradeGui.A_SLOTS[i]));
                fromB.add(inv.getItem(TradeGui.B_SLOTS[i]));
                inv.setItem(TradeGui.A_SLOTS[i], null);
                inv.setItem(TradeGui.B_SLOTS[i], null);
            }
            // 直接发放到双方背包（满则丢地上），而不是只改面板
            if (a != null) for (ItemStack it : fromB) if (it != null) giveItem(a, it);
            if (b != null) for (ItemStack it : fromA) if (it != null) giveItem(b, it);
        }
        if (a != null) { a.sendMessage(Messages.color("&a交易完成！物品已放入背包。")); a.closeInventory(); }
        if (b != null) { b.sendMessage(Messages.color("&a交易完成！物品已放入背包。")); b.closeInventory(); }
        plugin.getTradeManager().end(s.a);
    }

    /** 发物品到玩家背包，背包满则丢在脚边（绝不吞） */
    private void giveItem(Player p, ItemStack it) {
        java.util.Map<Integer, ItemStack> left = p.getInventory().addItem(it);
        for (ItemStack drop : left.values()) {
            if (drop != null) p.getWorld().dropItemNaturally(p.getLocation(), drop);
        }
    }

    /** 管理员强制取消某玩家的交易（物品归还双方） */
    public boolean forceCancel(Player p) {
        TradeManager.TradeSession s = plugin.getTradeManager().session(p.getUniqueId());
        if (s == null || s.done) return false;
        refund(s);
        plugin.getTradeManager().end(p.getUniqueId());
        p.sendMessage(Messages.color("&c管理员取消了你的交易，物品已归还。"));
        Player other = Bukkit.getPlayer(s.other(p.getUniqueId()));
        if (other != null) {
            other.sendMessage(Messages.color("&c管理员取消了交易，物品已归还。"));
            other.closeInventory();
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof TradeHolder)) return;
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        TradeManager.TradeSession s = plugin.getTradeManager().session(p.getUniqueId());
        if (s == null || s.done) return;
        // 未完成就关 → 取消交易，先归还物品再结束会话（避免物品消失）
        refund(s);
        plugin.getTradeManager().end(p.getUniqueId());
        p.sendMessage(Messages.color("&c你结束了交易，物品已归还。"));
        Player other = Bukkit.getPlayer(s.other(p.getUniqueId()));
        if (other != null) {
            other.sendMessage(Messages.color("&c对方结束了交易，已取消，物品已归还。"));
            other.closeInventory();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        TradeManager.TradeSession s = plugin.getTradeManager().session(p.getUniqueId());
        if (s == null) return;
        // 退出 → 取消交易，先归还物品
        refund(s);
        plugin.getTradeManager().end(p.getUniqueId());
        Player other = Bukkit.getPlayer(s.other(p.getUniqueId()));
        if (other != null) {
            other.sendMessage(Messages.color("&c对方退出了游戏，交易已取消，物品已归还。"));
            other.closeInventory();
        }
    }

    /** 取消/退出时：把面板里的物品还给各自的主人（背包满则丢脚边，绝不吞） */
    private void refund(TradeManager.TradeSession s) {
        Inventory inv = s.inv;
        if (inv == null) return;
        Player a = Bukkit.getPlayer(s.a);
        Player b = Bukkit.getPlayer(s.b);
        for (int i = 0; i < 20; i++) {
            ItemStack mine = inv.getItem(TradeGui.A_SLOTS[i]);
            if (mine != null) {
                inv.setItem(TradeGui.A_SLOTS[i], null);
                if (a != null) giveItem(a, mine);
            }
            ItemStack theirs = inv.getItem(TradeGui.B_SLOTS[i]);
            if (theirs != null) {
                inv.setItem(TradeGui.B_SLOTS[i], null);
                if (b != null) giveItem(b, theirs);
            }
        }
    }
}
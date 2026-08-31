package nl.tinyaii.tinyplayer.tpa;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TPA 传送请求管理：请求存储/超时/冷却/拒绝黑名单。
 */
public class TpaManager {

    public static class Request {
        public final UUID from;
        public final UUID to;
        public final boolean here;   // true=传这里(对方传到你身边)
        public final long createdAt;

        public Request(UUID from, UUID to, boolean here) {
            this.from = from; this.to = to; this.here = here; this.createdAt = System.currentTimeMillis();
        }
    }

    private final TinyPlayerPlugin plugin;
    private final List<Request> pending = new ArrayList<>();
    private final Map<UUID, Long> lastSend = new HashMap<>();
    private final Map<String, Long> softDenied = new HashMap<>();
    private final Map<String, Long> hardDenied = new HashMap<>();

    public TpaManager(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public int timeoutSeconds() { return plugin.getConfig().getInt("tpa.timeout-seconds", 30); }
    public int cooldownSeconds() { return plugin.getConfig().getInt("tpa.cooldown-seconds", 10); }
    /** 普通拒绝冷却(秒)：拒绝后对方短时间内不能再发请求 */
    public int denyCooldownSeconds() { return plugin.getConfig().getInt("tpa.deny-cooldown-seconds", 600); }

    /** 硬拒绝屏蔽时长(分钟)：拒绝并屏蔽，防骚扰 */
    public int denyHardMinutes() { return plugin.getConfig().getInt("tpa.deny-hard-cooldown-minutes", 8); }
    public int maxPending() { return plugin.getConfig().getInt("tpa.max-pending", 3); }

    public boolean onCooldown(UUID from) {
        Long t = lastSend.get(from);
        return t != null && System.currentTimeMillis() - t < cooldownSeconds() * 1000L;
    }

    public boolean isDenied(UUID from, UUID to) {
        Long t = softDenied.get(from + "|" + to);
        return t != null && System.currentTimeMillis() - t < denyCooldownSeconds() * 1000L;
    }

    /** 硬拒绝屏蔽检查（防骚扰，默认 8 分钟） */
    public boolean isHardDenied(UUID from, UUID to) {
        Long t = hardDenied.get(from + "|" + to);
        return t != null && System.currentTimeMillis() - t < denyHardMinutes() * 60_000L;
    }

    /** 发送请求；返回错误信息（null=成功） */
    public String request(Player from, Player to, boolean here) {
        UUID f = from.getUniqueId(), t = to.getUniqueId();
        if (f.equals(t)) return "不能向自己发送传送请求。";
        if (onCooldown(f)) return "传送冷却中，请稍后再试。";
        if (isDenied(f, t)) return "对方最近拒绝了你的请求，稍后再试。";
        // 重复请求拦截
        for (Request r : pending) {
            if (r.from.equals(f) && r.to.equals(t)) return "已有一条待处理的请求。";
        }
        // 对方待处理上限
        int toCount = 0;
        for (Request r : pending) if (r.to.equals(t)) toCount++;
        if (toCount >= maxPending()) return "对方待处理请求已满。";

        pending.add(new Request(f, t, here));
        lastSend.put(f, System.currentTimeMillis());
        return null;
    }

    /** 取某玩家收到的最新请求（可指定发送者） */
    public Request getPending(UUID to, UUID fromFilter) {
        // 清理过期
        long now = System.currentTimeMillis();
        pending.removeIf(r -> now - r.createdAt > timeoutSeconds() * 1000L);
        for (int i = pending.size() - 1; i >= 0; i--) {
            Request r = pending.get(i);
            if (r.to.equals(to) && (fromFilter == null || r.from.equals(fromFilter))) return r;
        }
        return null;
    }

    public List<Request> pendingTo(UUID to) {
        List<Request> out = new ArrayList<>();
        for (Request r : pending) if (r.to.equals(to)) out.add(r);
        return out;
    }

    public void remove(Request r) { pending.remove(r); }

    /** 移除某玩家发出的全部请求（退出清理） */
    public void removeAllFrom(UUID from) {
        pending.removeIf(r -> r.from.equals(from));
    }

    public void markDenied(UUID from, UUID to) { softDenied.put(from + "|" + to, System.currentTimeMillis()); }

    /** 硬拒绝并屏蔽（分钟） */
    public void markHardDenied(UUID from, UUID to, int minutes) {
        hardDenied.put(from + "|" + to, System.currentTimeMillis() + minutes * 60_000L);
    }

    /** 传送执行 */
    public void execute(Player from, Player to, boolean here) {
        Player teleporter = here ? to : from;   // 传这里=对方传到你身边
        Player target = here ? from : to;
        if (!teleporter.isOnline() || !target.isOnline()) return;
        teleporter.teleport(target.getLocation());
        teleporter.sendMessage(org.bukkit.ChatColor.GREEN + "传送完成！");
        target.sendMessage(org.bukkit.ChatColor.GREEN + teleporter.getName() + " 已传送过来。");
    }
}

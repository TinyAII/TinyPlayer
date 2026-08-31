package nl.tinyaii.tinyplayer.trade;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 交易状态机：双方共享同一个 45 格面板（TradeSession.inv），
 * 各自可见同一界面，A 操作左区(0-17)、B 操作右区(18-35)，底部状态行确认。
 */
public class TradeManager {

    public static class TradeSession {
        public final UUID a, b;
        public boolean aReady = false, bReady = false;
        public long confirmStart = 0;
        public boolean done = false;
        public Inventory inv;

        public TradeSession(UUID a, UUID b) {
            this.a = a;
            this.b = b;
        }

        public UUID other(UUID me) { return me.equals(a) ? b : a; }
        public boolean isA(UUID u) { return u.equals(a); }
    }

    private final TinyPlayerPlugin plugin;
    private final Map<UUID, TradeSession> sessions = new HashMap<>();
    private final Map<String, Long> blocked = new HashMap<>();   // requester|target -> 屏蔽到期时间
    private final Map<String, Long> recentRequests = new HashMap<>();  // requester|target -> 最近请求时间（去重）

    public TradeManager(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /** 被屏蔽的请求方（默认 8 分钟） */
    public boolean isBlocked(UUID requester, UUID target) {
        Long until = blocked.get(requester + "|" + target);
        return until != null && System.currentTimeMillis() < until;
    }

    /** 屏蔽请求方（分钟） */
    public void block(UUID requester, UUID target, int minutes) {
        blocked.put(requester + "|" + target, System.currentTimeMillis() + minutes * 60_000L);
    }

    /** 屏蔽时长（分钟，config 可调） */
    public int blockMinutes() { return plugin.getConfig().getInt("trade.block-minutes", 8); }

    /**
     * 发起交易请求：先检查屏蔽状态与去重，成功则给目标弹确认 GUI。
     * 返回错误信息（null=已发送请求）。
     */
    public String requestTrade(UUID requester, UUID target) {
        if (requester.equals(target)) return "不能向自己发起交易。";
        if (inTrade(requester) || inTrade(target)) return "交易失败（双方需均不在交易中）。";
        if (isBlocked(requester, target)) return "对方已屏蔽你的交易请求，稍后再试。";
        if (recentRequests.containsKey(requester + "|" + target)
                && System.currentTimeMillis() - recentRequests.get(requester + "|" + target) < 3000L) {
            return "交易请求已发送，请等待对方处理。";
        }
        recentRequests.put(requester + "|" + target, System.currentTimeMillis());
        return null;
    }

    public boolean inTrade(UUID u) { return sessions.containsKey(u); }

    public TradeSession session(UUID u) { return sessions.get(u); }

    /** 全部活跃会话（倒计时检查用） */
    public java.util.Map<UUID, TradeSession> sessions() { return sessions; }

    /** 开始交易（双方都不在交易中才成功）；失败返回 null */
    public TradeSession start(UUID a, UUID b) {
        if (inTrade(a) || inTrade(b)) return null;
        TradeSession s = new TradeSession(a, b);
        sessions.put(a, s);
        sessions.put(b, s);
        return s;
    }

    /** 结束交易（清双方） */
    public void end(UUID a) {
        TradeSession s = sessions.remove(a);
        if (s != null) sessions.remove(s.other(a));
    }

    public int confirmSeconds() { return plugin.getConfig().getInt("trade.confirm-seconds", 5); }
}
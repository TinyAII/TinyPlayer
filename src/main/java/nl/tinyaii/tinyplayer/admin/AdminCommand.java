package nl.tinyaii.tinyplayer.admin;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.economy.EconomyManager;
import nl.tinyaii.tinyplayer.home.HomeManager;
import nl.tinyaii.tinyplayer.util.Messages;
import nl.tinyaii.tinyplayer.warp.WarpManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * 管理员神权命令（双语）：
 *  /神权 玩家 <玩家>
 *  /神权 经济 给|扣|设|重置|排行 <玩家> <金额> [确认]
 *  /神权 家 查看|删除|传送|清理 <玩家> [家名] [确认]
 *  /神权 传送点 创建|删除|列表 <名字>
 *  /神权 传送 <玩家>
 *  /神权 拉 <玩家>
 *  /神权 密码 重置 <玩家>
 *  /神权 登录 踢 <玩家>
 *  /神权 交易 取消 <玩家>
 *  /神权 公告 <消息>
 */
public class AdminCommand implements CommandExecutor {

    private final TinyPlayerPlugin plugin;

    public AdminCommand(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isAdmin(CommandSender s) {
        if (s.hasPermission("tinyplayer.admin") || s.isOp()) return true;
        s.sendMessage(Messages.color("&c你没有权限这么做。"));
        return false;
    }

    private void log(String sender, String target, String action, String detail) {
        plugin.getAdminLog().log(sender, target, action, detail);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!isAdmin(sender)) return true;
        if (args.length == 0) { sender.sendMessage(Messages.color("&e用法: /神权 玩家|经济|家|传送点|传送|拉|密码|登录|交易|公告")); return true; }

        String sub = args[0];
        switch (sub) {
            case "玩家": case "player": case "p":
                return playerInfo(sender, args);
            case "经济": case "eco": case "money":
                return economy(sender, args);
            case "家": case "home":
                return home(sender, args);
            case "传送点": case "warp":
                return warp(sender, args);
            case "传送": case "tp":
                return tp(sender, args);
            case "拉": case "summon": case "here":
                return summon(sender, args);
            case "密码": case "password": case "passwd":
                return password(sender, args);
            case "登录": case "login":
                return login(sender, args);
            case "领地": case "claim":
                return claim(sender, args);
            case "交易": case "trade":
                return trade(sender, args);
            case "公告": case "broadcast": case "bc":
                return broadcast(sender, args);
            default:
                sender.sendMessage(Messages.color("&c未知子命令。可用: 玩家|经济|家|传送点|传送|拉|密码|登录|交易|公告"));
                return true;
        }
    }

    // ===== 玩家信息 =====
    private boolean playerInfo(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Messages.color("&c用法: /神权 玩家 <玩家>")); return true; }
        OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
        UUID u = op.getUniqueId();
        Player online = op.getPlayer();

        sender.sendMessage(Messages.color("&e===== 玩家信息: " + op.getName() + " ====="));
        sender.sendMessage(Messages.color("&7状态: " + (online != null && online.isOnline() ? "&a在线" : "&8离线")));
        if (plugin.economyEnabled()) {
            sender.sendMessage(Messages.color("&7" + plugin.getEcoBridge().currencyName() + ": &e" + String.format("%.2f", plugin.getEcoBridge().getBalance(u))));
        } else {
            sender.sendMessage(Messages.color("&7金币: &8经济模块未开启"));
        }
        if (plugin.homeEnabled()) {
            sender.sendMessage(Messages.color("&7家数: &e" + plugin.getHomeManager().getHomes(u).size()));
        } else {
            sender.sendMessage(Messages.color("&7家数: &8回家模块未开启"));
        }
        if (online != null) {
            sender.sendMessage(Messages.color("&7位置: &e" + online.getWorld().getName() + " "
                    + (int) online.getLocation().getX() + ", " + (int) online.getLocation().getY() + ", " + (int) online.getLocation().getZ()));
            sender.sendMessage(Messages.color("&7登录: " + (plugin.isLoggedIn(u) ? "&a已登录" : "&c未登录")));
        }
        return true;
    }

    // ===== 经济 =====
    private boolean economy(CommandSender sender, String[] args) {
        if (!plugin.economyEnabled()) { sender.sendMessage(Messages.color("&c经济模块未开启。")); return true; }
        if (args.length < 2) { sender.sendMessage(Messages.color("&c用法: /神权 经济 给|扣|设|重置|排行")); return true; }
        String op = args[1];
        switch (op) {
            case "给": case "give": case "add":
                if (args.length < 4) { sender.sendMessage(Messages.color("&c用法: /神权 经济 给 <玩家> <金额>")); return true; }
                return ecoGive(sender, args[2], args[3]);
            case "扣": case "take": case "remove":
                if (args.length < 4) { sender.sendMessage(Messages.color("&c用法: /神权 经济 扣 <玩家> <金额>")); return true; }
                return ecoTake(sender, args[2], args[3]);
            case "设": case "set":
                if (args.length < 4) { sender.sendMessage(Messages.color("&c用法: /神权 经济 设 <玩家> <金额>")); return true; }
                return ecoSet(sender, args[2], args[3]);
            case "重置": case "reset":
                if (args.length < 3) { sender.sendMessage(Messages.color("&c用法: /神权 经济 重置 <玩家> [确认]")); return true; }
                if (args.length < 4 || !args[3].equals("确认")) { sender.sendMessage(Messages.color("&c危险操作！再输入 &e/神权 经济 重置 " + args[2] + " 确认 &c执行")); return true; }
                plugin.getEcoBridge().setBalance(Bukkit.getOfflinePlayer(args[2]).getUniqueId(), 0);
                sender.sendMessage(Messages.color("&a已清零 &e" + args[2] + " &a的金币。"));
                log(sender.getName(), args[2], "经济重置", "清零");
                return true;
            case "排行": case "top":
                sender.sendMessage(Messages.color("&e===== 金币排行 TOP10 ====="));
                int i = 1;
                for (Map.Entry<UUID, Double> e : plugin.getEcoBridge().top(10).entrySet()) {
                    OfflinePlayer off = Bukkit.getOfflinePlayer(e.getKey());
                    sender.sendMessage(Messages.color("&e" + i + ". &f" + off.getName() + " &7- &e" + String.format("%.2f", e.getValue())));
                    i++;
                }
                return true;
            default:
                sender.sendMessage(Messages.color("&c未知经济操作。可用: 给|扣|设|重置|排行"));
                return true;
        }
    }

    private boolean ecoGive(CommandSender sender, String target, String amountStr) {
        double amount;
        try { amount = Double.parseDouble(amountStr); } catch (NumberFormatException e) { sender.sendMessage(Messages.color("&c金额不合法。")); return true; }
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        plugin.getEcoBridge().deposit(op.getUniqueId(), amount);
        sender.sendMessage(Messages.color("&a已给 &e" + op.getName() + " &a加 &e" + String.format("%.2f", amount) + " &a金币。"));
        log(sender.getName(), op.getName(), "经济给予", String.format("%.2f", amount));
        return true;
    }

    private boolean ecoTake(CommandSender sender, String target, String amountStr) {
        double amount;
        try { amount = Double.parseDouble(amountStr); } catch (NumberFormatException e) { sender.sendMessage(Messages.color("&c金额不合法。")); return true; }
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        plugin.getEcoBridge().withdraw(op.getUniqueId(), amount);
        sender.sendMessage(Messages.color("&a已从 &e" + op.getName() + " &a扣 &e" + String.format("%.2f", amount) + " &a金币。"));
        log(sender.getName(), op.getName(), "经济扣除", String.format("%.2f", amount));
        return true;
    }

    private boolean ecoSet(CommandSender sender, String target, String amountStr) {
        double amount;
        try { amount = Double.parseDouble(amountStr); } catch (NumberFormatException e) { sender.sendMessage(Messages.color("&c金额不合法。")); return true; }
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        plugin.getEcoBridge().setBalance(op.getUniqueId(), amount);
        sender.sendMessage(Messages.color("&a已设置 &e" + op.getName() + " &a余额为 &e" + String.format("%.2f", amount)));
        log(sender.getName(), op.getName(), "经济设置", String.format("%.2f", amount));
        return true;
    }

    // ===== 领地（神权）=====
    private boolean claim(CommandSender sender, String[] args) {
        if (!plugin.claimEnabled()) { sender.sendMessage(Messages.color("&c领地模块未开启。")); return true; }
        if (args.length < 2) { sender.sendMessage(Messages.color("&c用法: /神权 领地 查看|删除|列表")); return true; }
        nl.tinyaii.tinyplayer.claim.data.ClaimManager mgr = plugin.getClaimManager();
        String op = args[1];
        switch (op) {
            case "查看": case "info":
                if (args.length < 3) { sender.sendMessage(Messages.color("&c用法: /神权 领地 查看 <玩家>")); return true; }
                sender.sendMessage(Messages.color("&e===== " + args[2] + " 的领地 ====="));
                for (nl.tinyaii.tinyplayer.claim.data.Claim c : mgr.getClaimsOf(Bukkit.getOfflinePlayer(args[2]).getUniqueId())) {
                    sender.sendMessage(Messages.color("&7- &e" + c.getName() + " &7(" + c.getCuboid() + ")"));
                }
                return true;
            case "删除": case "delete":
                if (args.length < 3) { sender.sendMessage(Messages.color("&c用法: /神权 领地 删除 <玩家>")); return true; }
                java.util.List<nl.tinyaii.tinyplayer.claim.data.Claim> claims =
                        mgr.getClaimsOf(Bukkit.getOfflinePlayer(args[2]).getUniqueId());
                if (claims.isEmpty()) { sender.sendMessage(Messages.color("&c该玩家没有领地。")); return true; }
                for (nl.tinyaii.tinyplayer.claim.data.Claim c : claims) {
                    mgr.removeClaim(c);
                    plugin.getClaimStorage().deleteClaim(c.getId());
                }
                sender.sendMessage(Messages.color("&a已删除 &e" + args[2] + " &a的全部 &e" + claims.size() + " &a个领地。"));
                log(sender.getName(), args[2], "删除领地", claims.size() + " 个");
                return true;
            case "列表": case "list":
                sender.sendMessage(Messages.color("&e===== 全部领地 ====="));
                int i = 0;
                for (nl.tinyaii.tinyplayer.claim.data.Claim c : mgr.getAll()) {
                    sender.sendMessage(Messages.color("&7- &e" + c.getName() + " &7(" + c.getCuboid() + ")"));
                    i++;
                }
                if (i == 0) sender.sendMessage(Messages.color("&7暂无领地。"));
                return true;
            default:
                sender.sendMessage(Messages.color("&c用法: /神权 领地 查看|删除|列表"));
                return true;
        }
    }

    // ===== 家 =====
    private boolean home(CommandSender sender, String[] args) {
        if (!plugin.homeEnabled()) { sender.sendMessage(Messages.color("&c回家模块未开启。")); return true; }
        if (args.length < 2) { sender.sendMessage(Messages.color("&c用法: /神权 家 查看|删除|传送|清理 <玩家> [家名] [确认]")); return true; }
        HomeManager hm = plugin.getHomeManager();
        String op = args[1];
        switch (op) {
            case "查看": case "list": case "v":
                if (args.length < 3) { sender.sendMessage(Messages.color("&c用法: /神权 家 查看 <玩家>")); return true; }
                sender.sendMessage(Messages.color("&e===== " + args[2] + " 的家 ====="));
                for (HomeManager.HomeData h : hm.getHomes(Bukkit.getOfflinePlayer(args[2]).getUniqueId())) {
                    sender.sendMessage(Messages.color("&7- &e" + h.name + " &7(" + h.world + " "
                            + (int) h.x + ", " + (int) h.y + ", " + (int) h.z + ")")
                            + (h.isPublic ? Messages.color(" &a[公共]") : ""));
                }
                return true;
            case "删除": case "del": case "d":
                if (args.length < 4) { sender.sendMessage(Messages.color("&c用法: /神权 家 删除 <玩家> <家名>")); return true; }
                if (!hm.removeHome(Bukkit.getOfflinePlayer(args[2]).getUniqueId(), args[3])) {
                    sender.sendMessage(Messages.color("&c找不到该家。")); return true;
                }
                sender.sendMessage(Messages.color("&a已删除 &e" + args[2] + " &a的家 &e" + args[3]));
                log(sender.getName(), args[2], "删除家", args[3]);
                return true;
            case "传送": case "tp":
                if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
                if (args.length < 4) { sender.sendMessage(Messages.color("&c用法: /神权 家 传送 <玩家> <家名>")); return true; }
                HomeManager.HomeData h = hm.getHome(Bukkit.getOfflinePlayer(args[2]).getUniqueId(), args[3]);
                if (h == null || h.toLocation() == null) { sender.sendMessage(Messages.color("&c找不到该家。")); return true; }
                ((Player) sender).teleport(h.toLocation());
                sender.sendMessage(Messages.color("&a已传送到 &e" + args[2] + " &a的家 &e" + args[3]));
                return true;
            case "清理": case "clear": case "c":
                if (args.length < 3) { sender.sendMessage(Messages.color("&c用法: /神权 家 清理 <玩家> [确认]")); return true; }
                if (args.length < 4 || !args[3].equals("确认")) { sender.sendMessage(Messages.color("&c危险操作！再输入 &e/神权 家 清理 " + args[2] + " 确认 &c执行")); return true; }
                hm.clearHomes(Bukkit.getOfflinePlayer(args[2]).getUniqueId());
                sender.sendMessage(Messages.color("&a已清空 &e" + args[2] + " &a的全部家。"));
                log(sender.getName(), args[2], "清空家", "全部");
                return true;
            default:
                sender.sendMessage(Messages.color("&c未知家操作。可用: 查看|删除|传送|清理"));
                return true;
        }
    }

    // ===== 传送点 =====
    private boolean warp(CommandSender sender, String[] args) {
        if (!plugin.warpEnabled()) { sender.sendMessage(Messages.color("&c传送点模块未开启。")); return true; }
        if (args.length < 2) { sender.sendMessage(Messages.color("&c用法: /神权 传送点 创建|删除|列表")); return true; }
        WarpManager wm = plugin.getWarpManager();
        String op = args[1];
        switch (op) {
            case "创建": case "create": case "set":
                if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
                if (args.length < 3) { sender.sendMessage(Messages.color("&c用法: /神权 传送点 创建 <名字>")); return true; }
                wm.setWarp(args[2], ((Player) sender).getLocation());
                sender.sendMessage(Messages.color("&a已创建传送点 &e" + args[2]));
                log(sender.getName(), "-", "创建传送点", args[2]);
                return true;
            case "删除": case "del": case "remove":
                if (args.length < 3) { sender.sendMessage(Messages.color("&c用法: /神权 传送点 删除 <名字>")); return true; }
                if (!wm.removeWarp(args[2])) { sender.sendMessage(Messages.color("&c找不到传送点。")); return true; }
                sender.sendMessage(Messages.color("&a已删除传送点 &e" + args[2]));
                log(sender.getName(), "-", "删除传送点", args[2]);
                return true;
            case "列表": case "list":
                if (wm.getWarps().isEmpty()) { sender.sendMessage(Messages.color("&7暂无传送点。")); return true; }
                sender.sendMessage(Messages.color("&e===== 传送点列表 ====="));
                for (WarpManager.WarpData w : wm.getWarps().values()) {
                    sender.sendMessage(Messages.color("&7- &e" + w.name + " &7(" + w.world + " " + (int) w.x + ", " + (int) w.y + ", " + (int) w.z + ")"));
                }
                return true;
            default:
                sender.sendMessage(Messages.color("&c未知传送点操作。可用: 创建|删除|列表"));
                return true;
        }
    }

    // ===== 传送 =====
    private boolean tp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
        if (args.length < 2) { sender.sendMessage(Messages.color("&c用法: /神权 传送 <玩家>")); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage(Messages.color("&c玩家不在线。")); return true; }
        ((Player) sender).teleport(target.getLocation());
        sender.sendMessage(Messages.color("&a已传送到 &e" + target.getName() + " &a身边。"));
        return true;
    }

    private boolean summon(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
        if (args.length < 2) { sender.sendMessage(Messages.color("&c用法: /神权 拉 <玩家>")); return true; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage(Messages.color("&c玩家不在线。")); return true; }
        target.teleport(((Player) sender).getLocation());
        sender.sendMessage(Messages.color("&a已将 &e" + target.getName() + " &a拉到身边。"));
        target.sendMessage(Messages.color("&e管理员把你传送到了身边。"));
        return true;
    }

    // ===== 密码 =====
    private boolean password(CommandSender sender, String[] args) {
        if (!plugin.authEnabled()) { sender.sendMessage(Messages.color("&c登录模块未开启。")); return true; }
        if (args.length < 3) { sender.sendMessage(Messages.color("&c用法: /神权 密码 重置 <玩家>")); return true; }
        OfflinePlayer op = Bukkit.getOfflinePlayer(args[2]);
        if (!plugin.getAuthManager().deleteAccount(op.getUniqueId())) {
            sender.sendMessage(Messages.color("&c该玩家没有注册账号。")); return true;
        }
        sender.sendMessage(Messages.color("&a已重置 &e" + op.getName() + " &a的密码，该玩家下次进服重新 /注册。"));
        Player online = op.getPlayer();
        if (online != null) {
            plugin.getAuthManager().setLoggedIn(online.getUniqueId(), false);
            online.sendMessage(Messages.color("&c管理员重置了你的密码，请重新 /注册。"));
        }
        log(sender.getName(), op.getName(), "重置密码", "-");
        return true;
    }

    // ===== 登录 =====
    private boolean login(CommandSender sender, String[] args) {
        if (!plugin.authEnabled()) { sender.sendMessage(Messages.color("&c登录模块未开启。")); return true; }
        if (args.length < 3) { sender.sendMessage(Messages.color("&c用法: /神权 登录 踢 <玩家>")); return true; }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) { sender.sendMessage(Messages.color("&c玩家不在线。")); return true; }
        target.kickPlayer(Messages.color("&c管理员强制下线。"));
        sender.sendMessage(Messages.color("&a已踢出 &e" + target.getName()));
        log(sender.getName(), target.getName(), "强制下线", "-");
        return true;
    }

    // ===== 交易 =====
    private boolean trade(CommandSender sender, String[] args) {
        if (!plugin.tradeEnabled()) { sender.sendMessage(Messages.color("&c交易模块未开启。")); return true; }
        if (args.length < 3) { sender.sendMessage(Messages.color("&c用法: /神权 交易 取消 <玩家>")); return true; }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) { sender.sendMessage(Messages.color("&c玩家不在线。")); return true; }
        if (!plugin.getTradeHandler().forceCancel(target)) {
            sender.sendMessage(Messages.color("&c该玩家当前没有交易。"));
            return true;
        }
        sender.sendMessage(Messages.color("&a已取消 &e" + target.getName() + " &a的交易。"));
        log(sender.getName(), target.getName(), "取消交易", "-");
        return true;
    }

    // ===== 公告 =====
    private boolean broadcast(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Messages.color("&c用法: /神权 公告 <消息>")); return true; }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) { if (i > 1) sb.append(" "); sb.append(args[i]); }
        Bukkit.broadcastMessage(Messages.color("&6[公告] &f" + sb));
        log(sender.getName(), "-", "公告", sb.toString());
        return true;
    }
}
package nl.tinyaii.tinyplayer.home.command;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.home.HomeManager;
import nl.tinyaii.tinyplayer.home.gui.HomeGui;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 回家命令（双语）：
 *  /家                  → GUI 家列表
 *  /家 设置 <名>         / home set <name>    / sethome
 *  /家 回 [名]           / home go [name]     / home
 *  /家 删除 <名>         / home del <name>    / delhome
 *  /家 主 [名]           / home main [name]
 *  /家 移动 <名>         / home move <name>
 *  /家 公共 <名>         / home public <name>   (OP)
 *  /家 邀请 <玩家> <名>   / home invite <p> <name>
 *  /家 接受              / home accept
 *  /家 重载              / home reload
 */
public class HomeCommand implements CommandExecutor {

    private final TinyPlayerPlugin plugin;

    public HomeCommand(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // 便捷别名 /sethome <名> /delhome <名> /home 无参=GUI
        String name = cmd.getName();
        if (name.equalsIgnoreCase("sethome")) {
            if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
            Player p = (Player) sender;
            String homeName = args.length > 0 ? args[0] : "家";
            return doSet(p, homeName);
        }
        if (name.equalsIgnoreCase("delhome")) {
            if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
            Player p = (Player) sender;
            String homeName = args.length > 0 ? args[0] : null;
            return doDel(p, homeName);
        }
        if (name.equalsIgnoreCase("home") && args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
            new HomeGui(plugin, ((Player) sender).getUniqueId()).open((Player) sender);
            return true;
        }

        if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
        Player p = (Player) sender;
        if (args.length == 0) {
            new HomeGui(plugin, p.getUniqueId()).open(p);
            return true;
        }
        String sub = args[0];
        switch (sub) {
            case "设置": case "set":
                if (args.length < 2) { p.sendMessage(Messages.color("&c用法: /家 设置 <名字>")); return true; }
                return doSet(p, args[1]);
            case "回": case "go": case "回家":
                if (args.length >= 2) return doGo(p, args[1]);
                return doGo(p, null);
            case "删除": case "del": case "remove":
                if (args.length < 2) { p.sendMessage(Messages.color("&c用法: /家 删除 <名字>")); return true; }
                return doDel(p, args[1]);
            case "主": case "main":
                if (args.length >= 2) return doMain(p, args[1]);
                return doMain(p, null);
            case "移动": case "move":
                if (args.length < 2) { p.sendMessage(Messages.color("&c用法: /家 移动 <名字>")); return true; }
                return doMove(p, args[1]);
            case "公共": case "public":
                if (args.length < 2) { p.sendMessage(Messages.color("&c用法: /家 公共 <名字>")); return true; }
                return doPublic(p, args[1]);
            case "邀请": case "invite":
                if (args.length < 3) { p.sendMessage(Messages.color("&c用法: /家 邀请 <玩家> <名字>")); return true; }
                return doInvite(p, args[1], args[2]);
            case "接受": case "accept":
                return doAccept(p);
            case "重载": case "reload":
                if (!p.hasPermission("home.admin") && !p.isOp()) { p.sendMessage(Messages.color("&c无权限。")); return true; }
                plugin.getHomeManager().load();
                p.sendMessage(Messages.color("&a回家数据已重载。"));
                return true;
            default:
                p.sendMessage(Messages.color("&c未知子命令。可用: 设置/回/删除/主/移动/公共/邀请/接受/重载"));
                return true;
        }
    }

    // ===== 子命令实现 =====
    private boolean doSet(Player p, String homeName) {
        HomeManager hm = plugin.getHomeManager();
        if (homeName.contains(".") || homeName.length() > 20) { p.sendMessage(Messages.color("&c家名不合法（<=20字符，不含点）。")); return true; }
        if (hm.getHome(p.getUniqueId(), homeName) == null) {
            int limit = hm.homeLimit(p);
            if (hm.getHomes(p.getUniqueId()).size() >= limit) {
                p.sendMessage(Messages.color("&c已达家的数量上限（" + limit + " 个）。"));
                return true;
            }
        }
        hm.setHome(p.getUniqueId(), homeName, p.getLocation());
        p.sendMessage(Messages.color("&a已设置家 &e" + homeName + " &a(" + locStr(p.getLocation()) + ")"));
        return true;
    }

    private boolean doGo(Player p, String homeName) {
        HomeManager hm = plugin.getHomeManager();
        if (homeName == null || homeName.isEmpty()) homeName = hm.mainHomeName(p.getUniqueId());
        if (homeName == null) { p.sendMessage(Messages.color("&c你还没有家，先 /家 设置 <名字>")); return true; }
        HomeManager.HomeData h = hm.getHome(p.getUniqueId(), homeName);
        if (h == null) { p.sendMessage(Messages.color("&c找不到家 &e" + homeName)); return true; }
        Location loc = h.toLocation();
        if (loc == null || loc.getWorld() == null) { p.sendMessage(Messages.color("&c该家所在世界不可用。")); return true; }
        plugin.getHomeTeleport().start(p, loc, h.name);
        return true;
    }

    private boolean doDel(Player p, String homeName) {
        HomeManager hm = plugin.getHomeManager();
        if (homeName == null || homeName.isEmpty()) homeName = hm.mainHomeName(p.getUniqueId());
        if (homeName == null) { p.sendMessage(Messages.color("&c你还没有家。")); return true; }
        if (!hm.removeHome(p.getUniqueId(), homeName)) { p.sendMessage(Messages.color("&c找不到家 &e" + homeName)); return true; }
        p.sendMessage(Messages.color("&a已删除家 &e" + homeName));
        return true;
    }

    private boolean doMain(Player p, String homeName) {
        HomeManager hm = plugin.getHomeManager();
        if (homeName == null || homeName.isEmpty()) {
            String m = hm.mainHomeName(p.getUniqueId());
            p.sendMessage(Messages.color("&e你的主家: &a" + (m == null ? "无" : m)));
            return true;
        }
        if (!hm.setMain(p.getUniqueId(), homeName)) { p.sendMessage(Messages.color("&c找不到家 &e" + homeName)); return true; }
        p.sendMessage(Messages.color("&a已将 &e" + homeName + " &a设为主家。"));
        return true;
    }

    private boolean doMove(Player p, String homeName) {
        HomeManager hm = plugin.getHomeManager();
        HomeManager.HomeData h = hm.getHome(p.getUniqueId(), homeName);
        if (h == null) { p.sendMessage(Messages.color("&c找不到家 &e" + homeName)); return true; }
        hm.setHome(p.getUniqueId(), homeName, p.getLocation());
        p.sendMessage(Messages.color("&a已将家 &e" + homeName + " &a移动到当前位置 (" + locStr(p.getLocation()) + ")"));
        return true;
    }

    private boolean doPublic(Player p, String homeName) {
        if (!p.hasPermission("home.admin") && !p.isOp()) { p.sendMessage(Messages.color("&c无权限。")); return true; }
        HomeManager hm = plugin.getHomeManager();
        if (!hm.setPublic(p.getUniqueId(), homeName, true)) { p.sendMessage(Messages.color("&c找不到家 &e" + homeName)); return true; }
        p.sendMessage(Messages.color("&a已将家 &e" + homeName + " &a设为公共（全服可传）。"));
        return true;
    }

    private boolean doInvite(Player p, String targetName, String homeName) {
        HomeManager hm = plugin.getHomeManager();
        if (hm.getHome(p.getUniqueId(), homeName) == null) { p.sendMessage(Messages.color("&c找不到家 &e" + homeName)); return true; }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) { p.sendMessage(Messages.color("&c玩家不在线。")); return true; }
        hm.invite(p.getUniqueId(), target.getUniqueId(), homeName);
        p.sendMessage(Messages.color("&a已邀请 &e" + target.getName() + " &a参观家 &e" + homeName + " &a（30秒内 /家 接受）"));
        target.sendMessage(Messages.color("&e" + p.getName() + " &a邀请你参观他的家 &e" + homeName + "&a，&2/家 接受"));
        return true;
    }

    private boolean doAccept(Player p) {
        HomeManager hm = plugin.getHomeManager();
        HomeManager.Invite inv = hm.getInvite(p.getUniqueId());
        if (inv == null) { p.sendMessage(Messages.color("&c没有待接受的邀请。")); return true; }
        HomeManager.HomeData h = hm.getHome(inv.owner, inv.homeName);
        if (h == null) { p.sendMessage(Messages.color("&c该家已被删除。")); hm.clearInvite(p.getUniqueId()); return true; }
        hm.clearInvite(p.getUniqueId());
        Location loc = h.toLocation();
        if (loc == null || loc.getWorld() == null) { p.sendMessage(Messages.color("&c该家所在世界不可用。")); return true; }
        plugin.getHomeTeleport().start(p, loc, h.name);
        return true;
    }

    private String locStr(Location loc) {
        return (int) loc.getX() + ", " + (int) loc.getY() + ", " + (int) loc.getZ();
    }
}
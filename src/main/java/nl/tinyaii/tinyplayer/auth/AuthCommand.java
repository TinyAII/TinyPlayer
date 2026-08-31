package nl.tinyaii.tinyplayer.auth;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 登录/注册命令（双语：/登录 /login /注册 /register）。
 */
public class AuthCommand implements CommandExecutor {

    private final TinyPlayerPlugin plugin;

    public AuthCommand(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
        Player p = (Player) sender;
        AuthManager am = plugin.getAuthManager();
        // 基岩版玩家免登录，命令直接提示
        if (plugin.getConfig().getBoolean("auth.bypass-bedrock", true)
                && AuthManager.isBedrockPlayer(p.getUniqueId())) {
            p.sendMessage(org.bukkit.ChatColor.GREEN + "基岩版玩家免登录，无需注册/登录。");
            return true;
        }
        String name = cmd.getName();

        // 注册
        if (name.equals("注册") || name.equalsIgnoreCase("register")) {
            if (args.length < 2) { p.sendMessage(org.bukkit.ChatColor.RED + "用法: /注册 <密码> <确认密码>"); return true; }
            if (!args[0].equals(args[1])) { p.sendMessage(org.bukkit.ChatColor.RED + "两次密码不一致。"); return true; }
            if (args[0].length() < 4) { p.sendMessage(org.bukkit.ChatColor.RED + "密码至少 4 位。"); return true; }
            if (!am.register(p.getUniqueId(), args[0])) { p.sendMessage(org.bukkit.ChatColor.RED + "该账号已注册，请直接登录。"); return true; }
            am.setLoggedIn(p.getUniqueId(), true);
            teleportBack(p);
            p.sendMessage(org.bukkit.ChatColor.GREEN + "注册成功！已自动登录。");
            return true;
        }

        // 登录
        if (name.equals("登录") || name.equalsIgnoreCase("login")) {
            if (args.length < 1) { p.sendMessage(org.bukkit.ChatColor.RED + "用法: /登录 <密码>"); return true; }
            if (!am.hasAccount(p.getUniqueId())) { p.sendMessage(org.bukkit.ChatColor.RED + "该账号未注册，请用 /注册 <密码> <确认密码> 注册。"); return true; }
            if (am.isLoggedIn(p.getUniqueId())) { p.sendMessage(org.bukkit.ChatColor.GREEN + "你已登录。"); return true; }
            if (am.checkPassword(p.getUniqueId(), args[0])) {
                am.setLoggedIn(p.getUniqueId(), true);
                teleportBack(p);
                p.sendMessage(org.bukkit.ChatColor.GREEN + "登录成功！欢迎回来。");
            } else {
                p.sendMessage(org.bukkit.ChatColor.RED + "密码错误！");
            }
            return true;
        }
        return true;
    }

    /** 登录/注册成功：传回进服时的原始位置，清除记录 */
    private void teleportBack(Player p) {
        Location loc = plugin.getAuthManager().getOriginalLocation(p.getUniqueId());
        if (loc != null && loc.getWorld() != null) {
            p.teleport(loc);
        }
        plugin.getAuthManager().clearOriginalLocation(p.getUniqueId());
    }
}

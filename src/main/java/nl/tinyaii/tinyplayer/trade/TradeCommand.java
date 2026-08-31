package nl.tinyaii.tinyplayer.trade;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 交易命令：/交易 <玩家>（潜行右键玩家也可发起）。
 */
public class TradeCommand implements CommandExecutor {

    private final TinyPlayerPlugin plugin;

    public TradeCommand(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
        Player p = (Player) sender;
        if (args.length < 1) { p.sendMessage(Messages.color("&c用法: /交易 <玩家>（或潜行右键玩家）")); return true; }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { p.sendMessage(Messages.color("&c玩家不在线。")); return true; }
        if (!plugin.isLoggedIn(p.getUniqueId())
                || !plugin.isLoggedIn(target.getUniqueId())) {
            p.sendMessage(Messages.color("&c双方需先登录才能交易。"));
            return true;
        }
        String err = plugin.getTradeManager().requestTrade(p.getUniqueId(), target.getUniqueId());
        if (err != null) { p.sendMessage(Messages.color("&c" + err)); return true; }
        p.sendMessage(Messages.color("&a已向 &e" + target.getName() + " &a发起交易请求，等待对方处理..."));
        new TradeRequestGui(plugin, p.getUniqueId(), target.getUniqueId()).open(target);
        return true;
    }
}
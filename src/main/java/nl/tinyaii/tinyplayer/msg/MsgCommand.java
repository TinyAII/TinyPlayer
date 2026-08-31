package nl.tinyaii.tinyplayer.msg;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 私聊模块：/私聊 <玩家> <消息>，/回复 <消息>。
 * 消息格式：[私聊] 我→对方 / 对方→我，并附带对方名字彩色标识。
 */
public class MsgCommand implements CommandExecutor {

    private final TinyPlayerPlugin plugin;
    private final Map<UUID, UUID> lastFrom = new HashMap<>();

    public MsgCommand(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
        Player p = (Player) sender;
        String name = cmd.getName();

        if (name.equals("回复") || name.equalsIgnoreCase("r")) {
            UUID from = lastFrom.get(p.getUniqueId());
            if (from == null) { p.sendMessage(Messages.color("&c没有可回复的对象。")); return true; }
            Player target = Bukkit.getPlayer(from);
            if (target == null || !target.isOnline()) { p.sendMessage(Messages.color("&c对方已不在线。")); return true; }
            if (args.length < 1) { p.sendMessage(Messages.color("&c用法: /回复 <消息>")); return true; }
            send(p, target, String.join(" ", args));
            return true;
        }

        if (args.length < 2) { p.sendMessage(Messages.color("&c用法: /私聊 <玩家> <消息>")); return true; }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { p.sendMessage(Messages.color("&c玩家不在线。")); return true; }
        if (target.equals(p)) { p.sendMessage(Messages.color("&c不能给自己发私聊。")); return true; }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) { if (i > 1) sb.append(" "); sb.append(args[i]); }
        send(p, target, sb.toString());
        return true;
    }

    private void send(Player from, Player to, String msg) {
        lastFrom.put(from.getUniqueId(), to.getUniqueId());
        lastFrom.put(to.getUniqueId(), from.getUniqueId());
        from.sendMessage(Messages.color("&7[私聊] &e我 &7→ &e" + to.getName() + "&7: &f" + msg));
        to.sendMessage(Messages.color("&7[私聊] &e" + from.getName() + " &7→ &e我&7: &f" + msg));
    }
}
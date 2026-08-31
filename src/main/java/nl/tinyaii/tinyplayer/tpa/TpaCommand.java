package nl.tinyaii.tinyplayer.tpa;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.util.Messages;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * TPA 命令：/传送 /tpa /传这里 /tpahere /同意 /tpy /拒绝 /tpn。
 * 收到请求的玩家，聊天框出现可点击的 [✔ 同意] [✘ 拒绝] 按钮。
 */
public class TpaCommand implements CommandExecutor {

    private final TinyPlayerPlugin plugin;

    public TpaCommand(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
        Player p = (Player) sender;
        TpaManager tm = plugin.getTpaManager();
        String name = cmd.getName();

        if (name.equals("传送") || name.equalsIgnoreCase("tpa")) {
            if (args.length < 1) { p.sendMessage(Messages.color("&c用法: /传送 <玩家>")); return true; }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) { p.sendMessage(Messages.color("&c玩家不在线。")); return true; }
            String err = tm.request(p, target, false);
            if (err != null) { p.sendMessage(Messages.color("&c" + err)); return true; }
            p.sendMessage(Messages.color("&a已向 &e" + target.getName() + " &a发送传送请求。"));
            sendButtons(target, p.getName(), "想传送到你身边");
            return true;
        }

        if (name.equals("传这里") || name.equalsIgnoreCase("tpahere")) {
            if (args.length < 1) { p.sendMessage(Messages.color("&c用法: /传这里 <玩家>")); return true; }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) { p.sendMessage(Messages.color("&c玩家不在线。")); return true; }
            String err = tm.request(p, target, true);
            if (err != null) { p.sendMessage(Messages.color("&c" + err)); return true; }
            p.sendMessage(Messages.color("&a已请求 &e" + target.getName() + " &a传送到你身边。"));
            sendButtons(target, p.getName(), "想让你传送到身边");
            return true;
        }

        if (name.equals("同意") || name.equalsIgnoreCase("tpy") || name.equalsIgnoreCase("accept")) {
            TpaManager.Request r = args.length >= 1
                    ? tm.getPending(p.getUniqueId(), Bukkit.getPlayerExact(args[0]) != null ? Bukkit.getPlayerExact(args[0]).getUniqueId() : null)
                    : tm.getPending(p.getUniqueId(), null);
            if (r == null) { p.sendMessage(Messages.color("&c没有待处理的传送请求。")); return true; }
            Player from = Bukkit.getPlayer(r.from);
            tm.remove(r);
            tm.execute(from, p, r.here);
            return true;
        }

        if (name.equals("拒绝") || name.equalsIgnoreCase("tpn") || name.equalsIgnoreCase("deny")) {
            TpaManager.Request r = tm.getPending(p.getUniqueId(), null);
            if (r == null) { p.sendMessage(Messages.color("&c没有待处理的传送请求。")); return true; }
            tm.remove(r);
            tm.markDenied(r.from, p.getUniqueId());
            p.sendMessage(Messages.color("&c已拒绝请求。"));
            Player from = Bukkit.getPlayer(r.from);
            if (from != null) from.sendMessage(Messages.color("&e" + p.getName() + " &c拒绝了你的传送请求。"));
            return true;
        }
        return true;
    }

    /** 给接收者发可点击的 [✔ 同意] [✘ 拒绝] 按钮 */
    private void sendButtons(Player target, String requester, String action) {
        TextComponent main = new TextComponent(Messages.color("&e" + requester + " &a" + action + "，请选择："));
        TextComponent accept = new TextComponent(Messages.color("&2[✔ 同意] "));
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/同意 " + requester));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.ComponentBuilder("点击同意传送请求").create()));
        TextComponent deny = new TextComponent(Messages.color("&c[✘ 拒绝]"));
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/拒绝 " + requester));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.ComponentBuilder("点击拒绝传送请求").create()));
        target.spigot().sendMessage(main, accept, deny);
    }
}

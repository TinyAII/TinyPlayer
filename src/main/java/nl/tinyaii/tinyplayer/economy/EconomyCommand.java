package nl.tinyaii.tinyplayer.economy;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * 经济命令（双语）：
 *  /金币                     → 查自己余额
 *  /金币 <玩家>               → 查别人余额
 *  /金币 转账 <玩家> <金额>     → 转账
 *  /金币 排行                 → 余额排行
 *  /金币 给予 <玩家> <金额>     → OP 加钱
 *  /金币 扣除 <玩家> <金额>     → OP 扣钱
 *  /金币 设置 <玩家> <金额>     → OP 设余额
 * 别名：/money /bal /balance /pay
 */
public class EconomyCommand implements CommandExecutor {

    private final TinyPlayerPlugin plugin;

    public EconomyCommand(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName();

        // /pay <玩家> <金额> 快捷转账
        if (name.equalsIgnoreCase("pay")) {
            if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
            if (args.length < 2) { sender.sendMessage(Messages.color("&c用法: /pay <玩家> <金额>")); return true; }
            return doTransfer((Player) sender, args[0], args[1]);
        }

        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
            double bal = plugin.getEcoBridge().getBalance(((Player) sender).getUniqueId());
            sender.sendMessage(Messages.color("&a你的" + plugin.getEcoBridge().currencyName() + ": &e" + fmt(bal)));
            return true;
        }

        switch (args[0]) {
            case "转账": case "pay":
                if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
                if (args.length < 3) { sender.sendMessage(Messages.color("&c用法: /金币 转账 <玩家> <金额>")); return true; }
                return doTransfer((Player) sender, args[1], args[2]);
            case "排行": case "top":
                showTop(sender);
                return true;
            case "给予": case "give":
                if (!sender.hasPermission("economy.admin") && !sender.isOp()) { sender.sendMessage(Messages.color("&c无权限。")); return true; }
                if (args.length < 3) { sender.sendMessage(Messages.color("&c用法: /金币 给予 <玩家> <金额>")); return true; }
                return doGive(sender, args[1], args[2]);
            case "扣除": case "take":
                if (!sender.hasPermission("economy.admin") && !sender.isOp()) { sender.sendMessage(Messages.color("&c无权限。")); return true; }
                if (args.length < 3) { sender.sendMessage(Messages.color("&c用法: /金币 扣除 <玩家> <金额>")); return true; }
                return doTake(sender, args[1], args[2]);
            case "设置": case "set":
                if (!sender.hasPermission("economy.admin") && !sender.isOp()) { sender.sendMessage(Messages.color("&c无权限。")); return true; }
                if (args.length < 3) { sender.sendMessage(Messages.color("&c用法: /金币 设置 <玩家> <金额>")); return true; }
                return doSet(sender, args[1], args[2]);
            default:
                // 查别人余额
                OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
                double bal = plugin.getEcoBridge().getBalance(op.getUniqueId());
                sender.sendMessage(Messages.color("&e" + op.getName() + " &a的" + plugin.getEcoBridge().currencyName() + ": &e" + fmt(bal)));
                return true;
        }
    }

    private boolean doTransfer(Player from, String targetName, String amountStr) {
        double amount;
        try { amount = Double.parseDouble(amountStr); } catch (NumberFormatException e) { from.sendMessage(Messages.color("&c金额不合法。")); return true; }
        if (amount <= 0) { from.sendMessage(Messages.color("&c金额必须大于 0。")); return true; }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) { from.sendMessage(Messages.color("&c玩家不在线。")); return true; }
        if (target.equals(from)) { from.sendMessage(Messages.color("&c不能转给自己。")); return true; }
        String err = plugin.getEcoBridge().transfer(from.getUniqueId(), target.getUniqueId(), amount);
        if (err != null) { from.sendMessage(Messages.color("&c" + err)); return true; }
        from.sendMessage(Messages.color("&a已转账 &e" + fmt(amount) + " &a给 &e" + target.getName()
                + " &a(余额: " + fmt(plugin.getEcoBridge().getBalance(from.getUniqueId())) + ")"));
        target.sendMessage(Messages.color("&e" + from.getName() + " &a转给你 &e" + fmt(amount)
                + " &a" + plugin.getEcoBridge().currencyName() + " (余额: " + fmt(plugin.getEcoBridge().getBalance(target.getUniqueId())) + ")"));
        return true;
    }

    private boolean doGive(CommandSender sender, String targetName, String amountStr) {
        double amount;
        try { amount = Double.parseDouble(amountStr); } catch (NumberFormatException e) { sender.sendMessage(Messages.color("&c金额不合法。")); return true; }
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        plugin.getEcoBridge().deposit(op.getUniqueId(), amount);
        sender.sendMessage(Messages.color("&a已给 &e" + op.getName() + " &a添加 &e" + fmt(amount) + " &a" + plugin.getEcoBridge().currencyName() + "。"));
        return true;
    }

    private boolean doTake(CommandSender sender, String targetName, String amountStr) {
        double amount;
        try { amount = Double.parseDouble(amountStr); } catch (NumberFormatException e) { sender.sendMessage(Messages.color("&c金额不合法。")); return true; }
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        plugin.getEcoBridge().withdraw(op.getUniqueId(), amount);
        sender.sendMessage(Messages.color("&a已从 &e" + op.getName() + " &a扣除 &e" + fmt(amount) + " &a" + plugin.getEcoBridge().currencyName() + "。"));
        return true;
    }

    private boolean doSet(CommandSender sender, String targetName, String amountStr) {
        double amount;
        try { amount = Double.parseDouble(amountStr); } catch (NumberFormatException e) { sender.sendMessage(Messages.color("&c金额不合法。")); return true; }
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        plugin.getEcoBridge().setBalance(op.getUniqueId(), amount);
        sender.sendMessage(Messages.color("&a已设置 &e" + op.getName() + " &a余额为 &e" + fmt(amount)));
        return true;
    }

    private void showTop(CommandSender sender) {
        java.util.Map<UUID, Double> top = plugin.getEcoBridge().top(10);
        if (top.isEmpty()) { sender.sendMessage(Messages.color("&7暂无排行数据。")); return; }
        sender.sendMessage(Messages.color("&e===== " + plugin.getEcoBridge().currencyName() + " 排行 TOP10 ====="));
        int i = 1;
        for (Map.Entry<UUID, Double> e : top.entrySet()) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
            sender.sendMessage(Messages.color("&e" + i + ". &f" + op.getName() + " &7- &e" + fmt(e.getValue())));
            i++;
        }
    }

    private String fmt(double v) { return String.format("%.2f", v); }
}
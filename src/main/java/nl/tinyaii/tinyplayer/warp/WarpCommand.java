package nl.tinyaii.tinyplayer.warp;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 传送点命令（双语）：
 *  /传送点 列表           → GUI 传送点菜单
 *  /传送点 <名字>         → 传送到指定传送点
 *  /传送点 创建 <名字>     → 管理员创建（当前站的位置）
 *  /传送点 删除 <名字>     → 管理员删除
 *  /传送点 重载           → 重载数据
 */
public class WarpCommand implements CommandExecutor {

    private final TinyPlayerPlugin plugin;

    public WarpCommand(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
        Player p = (Player) sender;
        WarpManager wm = plugin.getWarpManager();

        if (args.length == 0) {
            openGui(p);
            return true;
        }
        String sub = args[0];
        switch (sub) {
            case "列表": case "list": case "menu":
                openGui(p);
                return true;
            case "创建": case "create": case "set":
                if (!p.hasPermission("warp.admin") && !p.isOp()) { p.sendMessage(Messages.color("&c无权限。")); return true; }
                if (args.length < 2) { p.sendMessage(Messages.color("&c用法: /传送点 创建 <名字>")); return true; }
                wm.setWarp(args[1], p.getLocation());
                p.sendMessage(Messages.color("&a已创建传送点 &e" + args[1]));
                return true;
            case "删除": case "del": case "remove":
                if (!p.hasPermission("warp.admin") && !p.isOp()) { p.sendMessage(Messages.color("&c无权限。")); return true; }
                if (args.length < 2) { p.sendMessage(Messages.color("&c用法: /传送点 删除 <名字>")); return true; }
                if (!wm.removeWarp(args[1])) { p.sendMessage(Messages.color("&c找不到传送点 &e" + args[1])); return true; }
                p.sendMessage(Messages.color("&a已删除传送点 &e" + args[1]));
                return true;
            case "重载": case "reload":
                if (!p.hasPermission("warp.admin") && !p.isOp()) { p.sendMessage(Messages.color("&c无权限。")); return true; }
                wm.load();
                p.sendMessage(Messages.color("&a传送点已重载。"));
                return true;
            default:
                if (wm.hasWarp(sub)) {
                    teleport(p, sub);
                } else {
                    p.sendMessage(Messages.color("&c找不到传送点 &e" + sub + "&c。用 /传送点 列表 查看。"));
                }
                return true;
        }
    }

    /** 传送（无吟唱，直接传送；世界不可用提示） */
    private void teleport(Player p, String name) {
        WarpManager.WarpData w = plugin.getWarpManager().getWarp(name);
        if (w == null || w.toLocation() == null) { p.sendMessage(Messages.color("&c该传送点世界不可用。")); return; }
        p.teleport(w.toLocation());
        p.sendMessage(Messages.color("&a已传送到 &e" + name));
    }

    /** GUI 传送点菜单：绿色旗帜图标，点击直传 */
    private void openGui(Player p) {
        Map<String, WarpManager.WarpData> warps = plugin.getWarpManager().getWarps();
        int size = Math.max(9, ((Math.max(warps.size(), 1) + 8) / 9) * 9);
        size = Math.min(54, Math.max(9, size));
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.DARK_GRAY + "传送点");
        int slot = 0;
        for (WarpManager.WarpData w : warps.values()) {
            if (slot >= size) break;
            inv.setItem(slot++, warpItem(w));
        }
        p.openInventory(inv);
    }

    private ItemStack warpItem(WarpManager.WarpData w) {
        ItemStack it = new ItemStack(Material.GREEN_BANNER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&a" + w.name));
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&7世界: &e" + w.world));
            lore.add(Messages.color("&7坐标: &e" + (int) w.x + ", " + (int) w.y + ", " + (int) w.z));
            lore.add(Messages.color("&7点击传送"));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }
}
package nl.tinyaii.tinyplayer.back;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 返回模块：记住玩家上一次「传送前」和「死亡」的位置，/返回 回去。
 * 记录所有传送的 from（含插件传送），但排除 /返回 自身触发的传送（防循环）。
 */
public class BackCommand implements CommandExecutor, Listener {

    private final TinyPlayerPlugin plugin;
    private final Map<UUID, Long> lastTeleportTime = new HashMap<>();
    private final Map<UUID, Location> lastTeleport = new HashMap<>();
    private final Map<UUID, Long> lastDeathTime = new HashMap<>();
    private final Map<UUID, Location> lastDeath = new HashMap<>();
    private final Set<UUID> skipNext = new HashSet<>();

    public BackCommand(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        UUID u = e.getPlayer().getUniqueId();
        if (skipNext.remove(u)) return;   // 这是 /返回 自己的传送，不记录
        lastTeleportTime.put(u, System.currentTimeMillis());
        lastTeleport.put(u, e.getFrom());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        UUID u = e.getEntity().getUniqueId();
        lastDeathTime.put(u, System.currentTimeMillis());
        lastDeath.put(u, e.getEntity().getLocation());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
        Player p = (Player) sender;
        UUID u = p.getUniqueId();

        Location loc = null;
        String type = "上次的位置";
        if (args.length >= 1 && args[0].equalsIgnoreCase("传送")) {
            loc = lastTeleport.get(u);
            type = "上次传送前的位置";
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("死亡")) {
            loc = lastDeath.get(u);
            type = "上次死亡的位置";
        } else {
            // 默认：取最近发生的事件（死亡 or 传送）
            Long t = lastDeathTime.get(u), d = lastTeleportTime.get(u);
            if (t != null && (d == null || t >= d)) { loc = lastDeath.get(u); type = "上次死亡的位置"; }
            else { loc = lastTeleport.get(u); type = "上次传送前的位置"; }
        }

        if (loc == null || loc.getWorld() == null) {
            p.sendMessage(Messages.color("&c没有可返回的位置。"));
            return true;
        }
        skipNext.add(u);
        p.teleport(loc);
        p.sendMessage(Messages.color("&a已返回" + type + "！"));
        return true;
    }
}
package nl.tinyaii.tinyplayer.claim.gui;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.data.Claim;
import nl.tinyaii.tinyplayer.claim.flags.Flags;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * 领地面板点击：flag 开关切换（主人/管理员）。
 */
public class ClaimGuiListener implements Listener {

    private final TinyPlayerPlugin plugin;

    public ClaimGuiListener(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof ClaimHolder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ClaimHolder holder = (ClaimHolder) e.getInventory().getHolder();
        Claim c = plugin.getClaimManager().getById(holder.claimId);
        if (c == null) { p.closeInventory(); return; }
        if (!c.isOwner(p.getUniqueId()) && !c.isAdmin(p.getUniqueId()) && !p.isOp()) {
            p.sendMessage(Messages.color("&c只有主人/管理员能操作领地面板。"));
            return;
        }
        int raw = e.getRawSlot();
        // 左下角"单领地成员"按钮 → 打开该领地的成员管理（单领地模式）
        if (raw == 45) {
            new MemberGui(plugin, c).openSingleClaimRoleSelect(p);
            return;
        }
        // 返回全局面板
        if (raw == 52) {
            new GlobalPanelGui(plugin, c.getOwner()).open(p);
            return;
        }
        if (raw >= 9 && raw < 44) {
            // flag 区：按 slot 顺序反查 flag 名
            String name = flagAt(raw);
            if (name == null) return;
            Boolean cur = c.getFlagOverride(name);
            boolean next = cur == null ? !resolveDefault(name) : !cur;
            c.setFlagOverride(name, next);
            plugin.getClaimStorage().saveClaim(c);
            plugin.getClaimManager().invalidateCache();
            p.sendMessage(Messages.color("&a已切换 flag &e" + name + " &a为 " + (next ? "&a开" : "&c关")));
            new ClaimGui(plugin, c).open(p);
        }
    }

    private String flagAt(int raw) {
        int idx = raw - 9;
        int i = 0;
        for (String name : Flags.PRI_FLAGS.keySet()) { if (i++ == idx) return name; }
        for (String name : Flags.ENV_FLAGS.keySet()) { if (i++ == idx) return name; }
        return null;
    }

    private boolean resolveDefault(String name) {
        return Flags.ENV_FLAGS.containsKey(name)
                ? plugin.getConfig().getBoolean("claim.flags.default-environment." + name, Flags.ENV_FLAGS.get(name))
                : plugin.getConfig().getBoolean("claim.flags.default-privilege." + name, Flags.PRI_FLAGS.get(name));
    }
}

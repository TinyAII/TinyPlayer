package nl.tinyaii.tinyplayer.claim.events;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.flags.FlagCheck;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.block.BlockIgniteEvent;

/**
 * 环境事件拦截：爆炸/火焰/液体流动/活塞/怪物破坏/踩踏。
 * 每个事件检查目标方块所在领地 flag，false → cancel。
 */
public class EnvEvents implements Listener {

    private final TinyPlayerPlugin plugin;
    private final FlagCheck check;

    public EnvEvents(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
        this.check = plugin.getFlagCheck();
    }

    // ===== 爆炸 =====
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        String flag = e.getEntityType().name().equals("CREEPER") ? "creeper-explosion" : "tnt-explosion";
        // 爆炸源在领地内 → 拦截整片
        if (check.checkEnv(e.getLocation(), flag, e)) return;
        e.blockList().clear();
    }

    // ===== 火焰 =====
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (check.checkEnv(e.getBlock().getLocation(), "fire-spread", e)) return;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        if (check.checkEnv(e.getBlock().getLocation(), "fire-spread", e)) return;
    }

    // ===== 液体流动 =====
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        Material from = e.getBlock().getType();
        boolean lava = from == Material.LAVA;
        boolean water = from == Material.WATER;
        if (!lava && !water) return;
        // 流到的目标方块所在领地
        Location to = e.getToBlock().getLocation();
        if (lava) {
            if (check.checkEnv(to, "lava-flow", e)) return;
        } else {
            if (check.checkEnv(to, "water-flow", e)) return;
        }
    }

    // ===== 活塞 =====
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        String flag = "piston-push";
        // 检查活塞推到的目标方块所在领地
        for (Block b : e.getBlocks()) {
            Location pushed = b.getRelative(e.getDirection()).getLocation();
            if (!check.checkEnv(pushed, flag, null)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        String flag = "piston-push";
        for (Block b : e.getBlocks()) {
            if (!check.checkEnv(b.getLocation(), flag, null)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    // ===== 怪物破坏（末影人/凋灵等）=====
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (check.checkEnv(e.getBlock().getLocation(), "mob-grief", e)) return;
    }

    // ===== 踩踏耕地 =====
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTrample(EntityInteractEvent e) {
        if (e.getBlock().getType() != Material.FARMLAND) return;
        boolean hostile = isHostile(e.getEntity().getType().name());
        String flag = hostile ? "mob-trample" : "animal-trample";
        if (check.checkEnv(e.getBlock().getLocation(), flag, e)) return;
    }

    private boolean isHostile(String name) {
        switch (name) {
            case "ZOMBIE": case "SKELETON": case "CREEPER": case "SPIDER":
            case "ENDERMAN": case "WITCH": case "BLAZE": case "GHAST":
            case "SLIME": case "MAGMA_CUBE": case "WITHER": case "ENDER_DRAGON":
            case "PIGLIN": case "HOGLIN": case "SHULKER": case "PHANTOM":
            case "DROWNED": case "STRAY": case "HUSK": case "ZOMBIFIED_PIGLIN":
            case "VINDICATOR": case "PILLAGER": case "EVOKER": case "RAVAGER":
            case "WARDEN": case "BREEZE": case "VEX":
                return true;
            default:
                return false;
        }
    }
}

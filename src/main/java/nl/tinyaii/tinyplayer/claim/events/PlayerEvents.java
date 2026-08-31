package nl.tinyaii.tinyplayer.claim.events;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.flags.FlagCheck;
import nl.tinyaii.tinyplayer.claim.util.BlockNames;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * 玩家事件拦截：破坏/放置/交互/战斗/桶。
 * 被拦截时给出具体提示（"你没有权限使用该领地内的 xx"）。
 */
public class PlayerEvents implements Listener {

    private final TinyPlayerPlugin plugin;
    private final FlagCheck check;
    private final java.util.Map<java.util.UUID, Long> lastMsg = new java.util.HashMap<>();

    public PlayerEvents(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
        this.check = plugin.getFlagCheck();
    }

    /** 节流提示：同玩家 1 秒内最多一条，防刷屏 */
    private void tell(Player p, String msg) {
        long now = System.currentTimeMillis();
        Long last = lastMsg.get(p.getUniqueId());
        if (last != null && now - last < 1000L) return;
        lastMsg.put(p.getUniqueId(), now);
        p.sendMessage(msg);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent e) {
        if (check.checkPri(e.getBlock().getLocation(), "build", e.getPlayer(), e)) return;
        tell(e.getPlayer(), Messages.color("&c你没有权限在该领地内破坏方块。"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent e) {
        if (check.checkPri(e.getBlock().getLocation(), "build", e.getPlayer(), e)) return;
        tell(e.getPlayer(), Messages.color("&c你没有权限在该领地内放置方块。"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getAction() == Action.PHYSICAL) return;
        if (e.getClickedBlock() == null) return;
        Location loc = e.getClickedBlock().getLocation();
        Material type = e.getClickedBlock().getType();

        // 容器/门/按钮/工作台 → container flag（桶操作走 build）
        if (isContainer(type)) {
            if (check.checkPri(loc, "container", p, e)) return;
            tell(p, Messages.color("&c你没有权限使用该领地内的 &e" + BlockNames.name(type) + "&c。"));
            return;
        }
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (check.checkPri(loc, "interact", p, e)) return;
            tell(p, Messages.color("&c你没有权限使用该领地内的 &e" + BlockNames.name(type) + "&c。"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (check.checkPri(e.getBlockClicked().getLocation(), "build", e.getPlayer(), e)) return;
        tell(e.getPlayer(), Messages.color("&c你没有权限在该领地内放置液体。"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        // 右键实体（村民/动物/盔甲架/展示框/矿车等）→ interact flag
        if (check.checkPri(e.getRightClicked().getLocation(), "interact", e.getPlayer(), e)) return;
        tell(e.getPlayer(), Messages.color("&c你没有权限与该领地内的生物/实体交互。"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (check.checkPri(e.getBlockClicked().getLocation(), "build", e.getPlayer(), e)) return;
        tell(e.getPlayer(), Messages.color("&c你没有权限在该领地内收集液体。"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        Location loc = e.getEntity().getLocation();
        if (e.getEntity() instanceof Player) {
            if (check.checkPri(loc, "pvp", p, e)) return;
            tell(p, Messages.color("&c该领地禁止 PVP。"));
            return;
        }
        // 怪物 vs 动物
        if (isHostile(e.getEntity().getType().name())) {
            if (check.checkPri(loc, "attack-monster", p, e)) return;
        } else {
            if (check.checkPri(loc, "attack-animal", p, e)) return;
        }
        tell(p, Messages.color("&c你没有权限在该领地内攻击生物。"));
    }

    private boolean isContainer(Material m) {
        switch (m) {
            case CHEST: case TRAPPED_CHEST: case ENDER_CHEST: case BARREL:
            case FURNACE: case BLAST_FURNACE: case SMOKER: case HOPPER:
            case DISPENSER: case DROPPER: case BREWING_STAND:
            case OAK_DOOR: case SPRUCE_DOOR: case BIRCH_DOOR: case JUNGLE_DOOR:
            case ACACIA_DOOR: case DARK_OAK_DOOR: case CRIMSON_DOOR: case WARPED_DOOR:
            case IRON_DOOR:
            case STONE_BUTTON: case OAK_BUTTON: case SPRUCE_BUTTON: case BIRCH_BUTTON:
            case JUNGLE_BUTTON: case ACACIA_BUTTON: case DARK_OAK_BUTTON:
            case CRIMSON_BUTTON: case WARPED_BUTTON: case STONE_PRESSURE_PLATE:
            case OAK_PRESSURE_PLATE: case LEVER:
            case CRAFTING_TABLE: case ENCHANTING_TABLE: case ANVIL:
            case STONECUTTER: case SMITHING_TABLE: case GRINDSTONE: case LOOM:
            case CARTOGRAPHY_TABLE: case FLETCHING_TABLE: case BEACON:
            case SHULKER_BOX: case CONDUIT:
                return true;
            default:
                return false;
        }
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

package nl.tinyaii.tinyplayer.claim.command;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.data.Claim;
import nl.tinyaii.tinyplayer.claim.data.Cuboid;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 选区管理器：木锄头右键选点 + 粒子预览。
 * 第一次右键=起点，第二次右键=终点（自动生成长方形/正方形选区预览）。
 * 边界样式可配：faces=六面全包围（直观）/ edges=12 条边线（低负担）——config settings.boundary-style
 * 粒子=火把火光(FLAME)；选点期间同时显示周围领地边界（防圈到别人家）。
 */
public class SelectionManager {

    private final TinyPlayerPlugin plugin;
    private final Map<UUID, Location> sel1 = new HashMap<>();
    private final Map<UUID, Location> sel2 = new HashMap<>();
    private final Map<UUID, BukkitRunnable> previewTasks = new HashMap<>();

    public SelectionManager(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /** 木锄头右键：返回提示消息（null=无需提示） */
    public String onRightClick(Player p, Location loc) {
        UUID u = p.getUniqueId();
        Location a = sel1.get(u);
        if (a == null) {
            // 第一次：标记起点
            sel1.put(u, loc);
            sel2.remove(u);
            stopPreview(u);
            p.sendMessage(nl.tinyaii.tinyplayer.util.Messages.color(
                    "&a已标记起点 &e" + locStr(loc) + "&a，再去右键终点。"));
            return null;
        }
        // 第二次：标记终点，开始预览
        sel2.put(u, loc);
        p.sendMessage(nl.tinyaii.tinyplayer.util.Messages.color(
                "&a已标记终点 &e" + locStr(loc) + "&a，选区范围：&e" + sizeStr(a, loc)));
        p.sendMessage(nl.tinyaii.tinyplayer.util.Messages.color(
                "&a用 &e/领地 创建 <名字> &a创建领地；重新右键可重新选区。"));
        startPreview(p);
        return null;
    }

    public void clearSelection(Player p) {
        UUID u = p.getUniqueId();
        sel1.remove(u);
        sel2.remove(u);
        stopPreview(u);
    }

    public Location getSel1(UUID u) { return sel1.get(u); }
    public Location getSel2(UUID u) { return sel2.get(u); }
    public boolean hasSelection(UUID u) { return sel1.containsKey(u) && sel2.containsKey(u); }

    public Cuboid getCuboid(UUID u) {
        Location a = sel1.get(u), b = sel2.get(u);
        if (a == null || b == null) return null;
        return Cuboid.fromLocations(a, b);
    }

    // ===== 粒子预览 =====
    private void startPreview(Player p) {
        stopPreview(p.getUniqueId());
        long durationMs = plugin.getConfig().getInt("claim.settings.display-duration-seconds", 10) * 1000L;
        final long start = System.currentTimeMillis();
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) { cancel(); previewTasks.remove(p.getUniqueId()); return; }
                if (System.currentTimeMillis() - start > durationMs) {
                    // 时间到：停止显示（选区数据保留，随时可 /领地 创建）
                    cancel();
                    previewTasks.remove(p.getUniqueId());
                    return;
                }
                drawAll(p);
            }
        };
        task.runTaskTimer(plugin, 0L, 20L);  // 每秒重画保持可见，durationMs 后自动停止
        previewTasks.put(p.getUniqueId(), task);
    }

    private void stopPreview(UUID u) {
        BukkitRunnable t = previewTasks.remove(u);
        if (t != null) t.cancel();
    }

    /** 画自己的选区 + 周围领地 */
    private void drawAll(Player p) {
        drawBox(p, getCuboid(p.getUniqueId()));
        drawNearbyClaims(p);
    }

    /** 周围领地（其他玩家已圈的地）：同样火焰边框，让玩家知道哪里已有领地 */
    private void drawNearbyClaims(Player p) {
        Location loc = p.getLocation();
        int radius = plugin.getConfig().getInt("claim.settings.selection-preview-radius", 64);
        for (Claim c : plugin.getClaimManager().getAll()) {
            Cuboid cub = c.getCuboid();
            if (!cub.getWorldUid().equals(loc.getWorld().getUID())) continue;
            // 粗略判断：领地中心与玩家距离（xz 平面）< radius + 领地半宽
            int cx = (cub.getMinX() + cub.getMaxX()) / 2;
            int cz = (cub.getMinZ() + cub.getMaxZ()) / 2;
            int hw = (cub.getMaxX() - cub.getMinX()) / 2;
            int hd = (cub.getMaxZ() - cub.getMinZ()) / 2;
            double dist = Math.hypot(loc.getBlockX() - cx, loc.getBlockZ() - cz);
            if (dist <= radius + Math.hypot(hw, hd)) {
                drawBox(p, cub);
            }
        }
    }

    /** 按配置画边界：faces=六面 / edges=12 条边 */
    private void drawBox(Player p, Cuboid cub) {
        if (cub == null) return;
        String style = plugin.getConfig().getString("claim.settings.boundary-style", "faces");
        if ("edges".equalsIgnoreCase(style)) {
            drawBoxEdges(p, cub);
        } else {
            drawBoxFaces(p, cub);
        }
    }

    /**
     * 六面全包围：六个面铺火焰网格（直观，粒子量稍多）。
     */
    private void drawBoxFaces(Player p, Cuboid cub) {
        World w = p.getWorld();
        double x1 = cub.getMinX(), y1 = cub.getMinY(), z1 = cub.getMinZ();
        double x2 = cub.getMaxX() + 1, y2 = cub.getMaxY() + 1, z2 = cub.getMaxZ() + 1;

        double step = stepFor(cub);

        // 底面 (y1) 与顶面 (y2)：xz 网格
        for (double x = x1; x <= x2; x += step) {
            for (double z = z1; z <= z2; z += step) {
                spawn(p, w, x, y1, z);
                spawn(p, w, x, y2, z);
            }
        }
        // 前面 (z1) 与后面 (z2)：xy 网格
        for (double x = x1; x <= x2; x += step) {
            for (double y = y1 + step; y < y2; y += step) {
                spawn(p, w, x, y, z1);
                spawn(p, w, x, y, z2);
            }
        }
        // 左面 (x1) 与右面 (x2)：yz 网格
        for (double y = y1 + step; y < y2; y += step) {
            for (double z = z1; z <= z2; z += step) {
                spawn(p, w, x1, y, z);
                spawn(p, w, x2, y, z);
            }
        }
    }

    /**
     * 12 条边线：只画棱（低负担，粒子量约为六面的一个零头）。
     */
    private void drawBoxEdges(Player p, Cuboid cub) {
        World w = p.getWorld();
        double x1 = cub.getMinX(), y1 = cub.getMinY(), z1 = cub.getMinZ();
        double x2 = cub.getMaxX() + 1, y2 = cub.getMaxY() + 1, z2 = cub.getMaxZ() + 1;

        double step = Math.max(stepFor(cub), 0.8);

        // 底面 4 条
        drawLine(p, w, x1, y1, z1, x2, y1, z1, step);
        drawLine(p, w, x2, y1, z1, x2, y1, z2, step);
        drawLine(p, w, x2, y1, z2, x1, y1, z2, step);
        drawLine(p, w, x1, y1, z2, x1, y1, z1, step);
        // 顶面 4 条
        drawLine(p, w, x1, y2, z1, x2, y2, z1, step);
        drawLine(p, w, x2, y2, z1, x2, y2, z2, step);
        drawLine(p, w, x2, y2, z2, x1, y2, z2, step);
        drawLine(p, w, x1, y2, z2, x1, y2, z1, step);
        // 垂直 4 条
        drawLine(p, w, x1, y1, z1, x1, y2, z1, step);
        drawLine(p, w, x2, y1, z1, x2, y2, z1, step);
        drawLine(p, w, x2, y1, z2, x2, y2, z2, step);
        drawLine(p, w, x1, y1, z2, x1, y2, z2, step);
    }

    private void drawLine(Player p, World w, double x1, double y1, double z1,
                          double x2, double y2, double z2, double step) {
        double len = Math.max(1, Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1) + (z2-z1)*(z2-z1)));
        int n = (int) (len / step);
        for (int i = 0; i <= n; i++) {
            double t = n == 0 ? 0 : (double) i / n;
            double x = x1 + (x2 - x1) * t;
            double y = y1 + (y2 - y1) * t;
            double z = z1 + (z2 - z1) * t;
            spawn(p, w, x, y, z);
        }
    }

    /** 步长自适应：边长越大步长越大，控制粒子数量 */
    private double stepFor(Cuboid cub) {
        int maxLen = Math.max(cub.getMaxX() - cub.getMinX(),
                Math.max(cub.getMaxY() - cub.getMinY(), cub.getMaxZ() - cub.getMinZ()));
        if (maxLen <= 16) return 0.6;
        if (maxLen <= 32) return 0.8;
        if (maxLen <= 64) return 1.0;
        if (maxLen <= 128) return 1.5;
        return 2.0;
    }

    private void spawn(Player p, World w, double x, double y, double z) {
        p.spawnParticle(Particle.FLAME, x, y, z, 1, 0, 0, 0, 0);
    }

    private String locStr(Location l) {
        return l.getWorld().getName() + " " + (int) l.getX() + "," + (int) l.getY() + "," + (int) l.getZ();
    }

    private String sizeStr(Location a, Location b) {
        int sx = Math.abs(a.getBlockX() - b.getBlockX()) + 1;
        int sy = Math.abs(a.getBlockY() - b.getBlockY()) + 1;
        int sz = Math.abs(a.getBlockZ() - b.getBlockZ()) + 1;
        return sx + "x" + sy + "x" + sz;
    }
}

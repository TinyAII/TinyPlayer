package nl.tinyaii.tinyplayer.claim.util;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.data.Cuboid;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * 领地边界绘制：按 config settings.boundary-style 画 FLAME 火焰粒子。
 * faces=六面全包围 / edges=12 条边线；步长自适应防卡；只发给自己。
 */
public class BoundaryRenderer {

    private final TinyPlayerPlugin plugin;

    public BoundaryRenderer(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    /** 画一个领地的边界（按配置样式） */
    public void drawBox(Player p, Cuboid cub) {
        if (cub == null) return;
        String style = plugin.getConfig().getString("claim.settings.boundary-style", "faces");
        if ("edges".equalsIgnoreCase(style)) {
            drawEdges(p, cub);
        } else {
            drawFaces(p, cub);
        }
    }

    /** 六面全包围 */
    private void drawFaces(Player p, Cuboid cub) {
        World w = p.getWorld();
        double x1 = cub.getMinX(), y1 = cub.getMinY(), z1 = cub.getMinZ();
        double x2 = cub.getMaxX() + 1, y2 = cub.getMaxY() + 1, z2 = cub.getMaxZ() + 1;
        double step = stepFor(cub);

        for (double x = x1; x <= x2; x += step) {
            for (double z = z1; z <= z2; z += step) {
                spawn(p, w, x, y1, z);
                spawn(p, w, x, y2, z);
            }
        }
        for (double x = x1; x <= x2; x += step) {
            for (double y = y1 + step; y < y2; y += step) {
                spawn(p, w, x, y, z1);
                spawn(p, w, x, y, z2);
            }
        }
        for (double y = y1 + step; y < y2; y += step) {
            for (double z = z1; z <= z2; z += step) {
                spawn(p, w, x1, y, z);
                spawn(p, w, x2, y, z);
            }
        }
    }

    /** 12 条边线 */
    private void drawEdges(Player p, Cuboid cub) {
        World w = p.getWorld();
        double x1 = cub.getMinX(), y1 = cub.getMinY(), z1 = cub.getMinZ();
        double x2 = cub.getMaxX() + 1, y2 = cub.getMaxY() + 1, z2 = cub.getMaxZ() + 1;
        double step = Math.max(stepFor(cub), 0.8);

        drawLine(p, w, x1, y1, z1, x2, y1, z1, step);
        drawLine(p, w, x2, y1, z1, x2, y1, z2, step);
        drawLine(p, w, x2, y1, z2, x1, y1, z2, step);
        drawLine(p, w, x1, y1, z2, x1, y1, z1, step);
        drawLine(p, w, x1, y2, z1, x2, y2, z1, step);
        drawLine(p, w, x2, y2, z1, x2, y2, z2, step);
        drawLine(p, w, x2, y2, z2, x1, y2, z2, step);
        drawLine(p, w, x1, y2, z2, x1, y2, z1, step);
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
            spawn(p, w, x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, z1 + (z2 - z1) * t);
        }
    }

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
}

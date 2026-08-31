package nl.tinyaii.tinyplayer.claim.data;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * 立方体区域（领地范围）。
 */
public class Cuboid {

    private final UUID worldUid;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    public Cuboid(UUID worldUid, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.worldUid = worldUid;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public static Cuboid fromLocations(Location a, Location b) {
        return new Cuboid(a.getWorld().getUID(),
                a.getBlockX(), a.getBlockY(), a.getBlockZ(),
                b.getBlockX(), b.getBlockY(), b.getBlockZ());
    }

    public UUID getWorldUid() { return worldUid; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    /** 是否包含某方块坐标 */
    public boolean contain(UUID world, int x, int y, int z) {
        if (!worldUid.equals(world)) return false;
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    /** 是否与另一个区域重叠（同世界才可能重叠） */
    public boolean intersects(Cuboid other) {
        if (!worldUid.equals(other.worldUid)) return false;
        return minX <= other.maxX && maxX >= other.minX
                && minY <= other.maxY && maxY >= other.minY
                && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    /** 是否完全包含另一个区域 */
    public boolean contains(Cuboid other) {
        if (!worldUid.equals(other.worldUid)) return false;
        return minX <= other.minX && maxX >= other.maxX
                && minY <= other.minY && maxY >= other.maxY
                && minZ <= other.minZ && maxZ >= other.maxZ;
    }

    /** 体积（方块数） */
    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    /** 中心点（用于传送） */
    public Location center(World world) {
        return new Location(world,
                (minX + maxX) / 2.0 + 0.5,
                (minY + maxY) / 2.0 + 0.5,
                (minZ + maxZ) / 2.0 + 0.5);
    }

    @Override
    public String toString() {
        return "Cuboid{" + minX + "," + minY + "," + minZ + " -> " + maxX + "," + maxY + "," + maxZ + "}";
    }
}

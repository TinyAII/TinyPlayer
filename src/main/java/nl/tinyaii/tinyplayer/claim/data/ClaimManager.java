package nl.tinyaii.tinyplayer.claim.data;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 领地管理器：内存中维护全部领地 + 空间索引（四象限分区 + 方块级 LRU 缓存）。
 * 这是性能核心——同一坐标反复查询直接命中缓存，不遍历全部领地。
 */
public class ClaimManager {

    private final TinyPlayerPlugin plugin;
    private final Map<Integer, Claim> claims = new ConcurrentHashMap<>();
    private final Map<Integer, List<Integer>> childrenMap = new ConcurrentHashMap<>();
    /** 全局权限成员：owner -> 可管理该 owner 所有领地的玩家集合（含以后新建） */
    private final Map<UUID, java.util.Set<UUID>> globalAdmins = new ConcurrentHashMap<>();
    private int nextId = 1;

    // ===== 空间索引：四象限分区（以原点 0,0 分 A/B/C/D）=====
    private final Map<UUID, List<Claim>> sectorA = new ConcurrentHashMap<>(); // x>=0, z>=0
    private final Map<UUID, List<Claim>> sectorB = new ConcurrentHashMap<>(); // x<0,  z>=0
    private final Map<UUID, List<Claim>> sectorC = new ConcurrentHashMap<>(); // x>=0, z<0
    private final Map<UUID, List<Claim>> sectorD = new ConcurrentHashMap<>(); // x<0,  z<0

    // ===== 方块级 LRU 缓存：BlockKey -> claimId（-1 = 无领地）=====
    private static final int CACHE_MAX = 65536;
    private static final int NO_CLAIM = -1;
    private final Map<UUID, Map<BlockKey, Integer>> blockCache = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger cacheSize = new java.util.concurrent.atomic.AtomicInteger();

    private record BlockKey(int x, int y, int z) {}

    public ClaimManager(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    // ===== 加载 =====
    public void addClaim(Claim c) {
        claims.put(c.getId(), c);
        if (c.getParentId() != null) {
            childrenMap.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c.getId());
        }
        indexClaim(c);
        clearCache();
    }

    public void removeClaim(Claim c) {
        claims.remove(c.getId());
        if (c.getParentId() != null) {
            List<Integer> siblings = childrenMap.get(c.getParentId());
            if (siblings != null) siblings.remove((Integer) c.getId());
        }
        childrenMap.remove(c.getId());
        clearCache();
    }

    private void indexClaim(Claim c) {
        Cuboid cub = c.getCuboid();
        UUID w = cub.getWorldUid();
        // 分区：取领地中心点定扇区（足够精确）
        int cx = (cub.getMinX() + cub.getMaxX()) / 2;
        int cz = (cub.getMinZ() + cub.getMaxZ()) / 2;
        Map<UUID, List<Claim>> sector = sectorOf(cx, cz);
        sector.computeIfAbsent(w, k -> new ArrayList<>()).add(c);
    }

    private Map<UUID, List<Claim>> sectorOf(int x, int z) {
        if (x >= 0 && z >= 0) return sectorA;
        if (x < 0 && z >= 0) return sectorB;
        if (x >= 0) return sectorC;
        return sectorD;
    }

    private void clearCache() {
        blockCache.clear();
        cacheSize.set(0);
    }

    // ===== 查询 =====
    public Claim getById(int id) { return claims.get(id); }

    public List<Claim> getAll() { return new ArrayList<>(claims.values()); }

    public List<Claim> getChildrenOf(int parentId) {
        List<Integer> ids = childrenMap.get(parentId);
        List<Claim> out = new ArrayList<>();
        if (ids == null) return out;
        for (int id : ids) {
            Claim c = claims.get(id);
            if (c != null) out.add(c);
        }
        return out;
    }

    public List<Claim> getClaimsOf(UUID player) {
        List<Claim> out = new ArrayList<>();
        for (Claim c : claims.values()) {
            if (c.isOwner(player)) out.add(c);
        }
        return out;
    }

    /**
     * 查询坐标所在领地（含子领地递归，子领地优先）。
     * 命中方块缓存直接返回；未命中查扇区列表。
     */
    public Claim getClaimAt(UUID world, int x, int y, int z) {
        // 1. 查缓存
        Map<BlockKey, Integer> worldCache = blockCache.get(world);
        if (worldCache != null) {
            Integer cached = worldCache.get(new BlockKey(x, y, z));
            if (cached != null) {
                return cached == NO_CLAIM ? null : claims.get(cached);
            }
        }
        // 2. 查扇区
        Claim result = searchSector(sectorOf(x, z), world, x, y, z);
        // 3. 写缓存
        cache(world, x, y, z, result == null ? NO_CLAIM : result.getId());
        return result;
    }

    private Claim searchSector(Map<UUID, List<Claim>> sector, UUID world, int x, int y, int z) {
        List<Claim> list = sector.get(world);
        if (list == null) return null;
        for (Claim c : list) {
            if (!c.contains(world, x, y, z)) continue;
            // 有子领地：子领地优先（精确坐标落子领地则返回子领地）
            Claim child = searchChildren(c, world, x, y, z);
            return child != null ? child : c;
        }
        return null;
    }

    private Claim searchChildren(Claim parent, UUID world, int x, int y, int z) {
        for (Claim child : getChildrenOf(parent.getId())) {
            if (child.contains(world, x, y, z)) {
                Claim grand = searchChildren(child, world, x, y, z);
                return grand != null ? grand : child;
            }
        }
        return null;
    }

    private void cache(UUID world, int x, int y, int z, int claimId) {
        if (cacheSize.get() >= CACHE_MAX) {
            blockCache.clear();
            cacheSize.set(0);
        }
        Map<BlockKey, Integer> worldCache = blockCache.computeIfAbsent(world, k -> new ConcurrentHashMap<>());
        if (worldCache.putIfAbsent(new BlockKey(x, y, z), claimId) == null) {
            cacheSize.incrementAndGet();
        }
    }

    public void invalidateCache() { clearCache(); }

    // ===== 全局权限成员（owner 名下所有领地共享）=====

    /** 该玩家是否是某 owner 的全局权限成员 */
    public boolean isGlobalAdmin(UUID owner, UUID player) {
        java.util.Set<UUID> set = globalAdmins.get(owner);
        return set != null && set.contains(player);
    }

    /** 获取 owner 的全部全局权限成员 */
    public java.util.Set<UUID> getGlobalAdmins(UUID owner) {
        return new java.util.HashSet<>(globalAdmins.getOrDefault(owner, java.util.Collections.emptySet()));
    }

    public void addGlobalAdmin(UUID owner, UUID player) {
        globalAdmins.computeIfAbsent(owner, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(player);
    }

    public boolean removeGlobalAdmin(UUID owner, UUID player) {
        java.util.Set<UUID> set = globalAdmins.get(owner);
        if (set != null) return set.remove(player);
        return false;
    }

    /** 全量导出（存储用） */
    public Map<UUID, java.util.Set<UUID>> getAllGlobalAdmins() { return globalAdmins; }

    /** 存储加载时清空并填充 */
    public void clearGlobalAdmins() { globalAdmins.clear(); }
    public void putGlobalAdmins(UUID owner, java.util.Set<UUID> admins) {
        globalAdmins.put(owner, new java.util.HashSet<>(admins));
    }

    /** 分配下一个 id */
    public int allocateId() { return nextId++; }
    public void setNextId(int id) { if (id > nextId) nextId = id; }

    public TinyPlayerPlugin getPlugin() { return plugin; }
}

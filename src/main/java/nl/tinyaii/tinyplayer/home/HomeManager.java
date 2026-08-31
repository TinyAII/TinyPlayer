package nl.tinyaii.tinyplayer.home;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 回家系统数据管理：多家园 / 上限 / 主家 / 公共家 / 邀请 / 持久化 home-data.yml。
 */
public class HomeManager {

    /** 单个家数据 */
    public static class HomeData {
        public String name;
        public String world;
        public double x, y, z;
        public float yaw, pitch;
        public boolean isPublic = false;

        public HomeData(String name, Location loc) {
            this.name = name;
            this.world = loc.getWorld().getName();
            this.x = loc.getX();
            this.y = loc.getY();
            this.z = loc.getZ();
            this.yaw = loc.getYaw();
            this.pitch = loc.getPitch();
        }

        public Location toLocation() {
            World w = Bukkit.getWorld(world);
            if (w == null) return null;
            return new Location(w, x, y, z, yaw, pitch);
        }
    }

    /** 邀请数据 */
    public static class Invite {
        public final UUID owner;
        public final String homeName;
        public final long expireAt;
        public Invite(UUID owner, String homeName) {
            this.owner = owner;
            this.homeName = homeName;
            this.expireAt = System.currentTimeMillis() + 30_000L;
        }
    }

    private final TinyPlayerPlugin plugin;
    private final Map<UUID, LinkedHashMap<String, HomeData>> homes = new HashMap<>();
    private final Map<UUID, Invite> invites = new HashMap<>();
    private final Map<UUID, Long> lastTeleport = new HashMap<>();
    private File file;

    public HomeManager(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        homes.clear();
        file = new File(plugin.getDataFolder(), "home-data.yml");
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("homes");
        if (root == null) return;
        for (String uuidStr : root.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            LinkedHashMap<String, HomeData> list = new LinkedHashMap<>();
            ConfigurationSection playerSec = root.getConfigurationSection(uuidStr);
            if (playerSec == null) continue;
            for (String name : playerSec.getKeys(false)) {
                String world = playerSec.getString(name + ".world", "world");
                HomeData h = new HomeData(name, new Location(
                        Bukkit.getWorld(world) != null ? Bukkit.getWorld(world) : Bukkit.getWorlds().get(0),
                        playerSec.getDouble(name + ".x", 0),
                        playerSec.getDouble(name + ".y", 64),
                        playerSec.getDouble(name + ".z", 0)));
                h.world = world;
                h.yaw = (float) playerSec.getDouble(name + ".yaw", 0);
                h.pitch = (float) playerSec.getDouble(name + ".pitch", 0);
                h.isPublic = playerSec.getBoolean(name + ".public", false);
                list.put(name, h);
            }
            homes.put(uuid, list);
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, LinkedHashMap<String, HomeData>> e : homes.entrySet()) {
            for (HomeData h : e.getValue().values()) {
                String path = "homes." + e.getKey() + "." + h.name;
                yml.set(path + ".world", h.world);
                yml.set(path + ".x", h.x);
                yml.set(path + ".y", h.y);
                yml.set(path + ".z", h.z);
                yml.set(path + ".yaw", h.yaw);
                yml.set(path + ".pitch", h.pitch);
                yml.set(path + ".public", h.isPublic);
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("保存 home-data.yml 失败: " + ex.getMessage());
        }
    }

    // ===== 查询 =====
    private LinkedHashMap<String, HomeData> list(UUID uuid) {
        return homes.computeIfAbsent(uuid, k -> new LinkedHashMap<>());
    }

    public List<HomeData> getHomes(UUID uuid) { return new ArrayList<>(list(uuid).values()); }

    /** 所有拥有家的玩家 UUID（GUI 展示公共家用） */
    public List<UUID> allOwners() { return new ArrayList<>(homes.keySet()); }

    public HomeData getHome(UUID uuid, String name) { return list(uuid).get(name); }

    public String mainHomeName(UUID uuid) {
        LinkedHashMap<String, HomeData> l = list(uuid);
        if (l.isEmpty()) return null;
        return l.keySet().iterator().next();
    }

    // ===== 上限 =====
    public int homeLimit(Player p) {
        if (p.hasPermission("home.admin") || p.isOp()) return Integer.MAX_VALUE;
        int limit = plugin.getConfig().getInt("home.max-homes-default", 3);
        for (String perm : new String[]{"svip", "vip", "default"}) {
            if (p.hasPermission("home.limit." + perm)) {
                int v = plugin.getConfig().getInt("home.limit." + perm, limit);
                if (v > limit) limit = v;
            }
        }
        return limit;
    }

    // ===== 操作 =====
    public boolean setHome(UUID uuid, String name, Location loc) {
        list(uuid).put(name, new HomeData(name, loc));
        save();
        return true;
    }

    public boolean removeHome(UUID uuid, String name) {
        boolean ok = list(uuid).remove(name) != null;
        if (ok) save();
        return ok;
    }

    /** 清空玩家全部家（管理员清理用） */
    public boolean clearHomes(UUID uuid) {
        boolean had = !list(uuid).isEmpty();
        homes.remove(uuid);
        if (had) save();
        return had;
    }

    /** 设主家：移到 map 首位 */
    public boolean setMain(UUID uuid, String name) {
        LinkedHashMap<String, HomeData> l = list(uuid);
        HomeData h = l.remove(name);
        if (h == null) return false;
        LinkedHashMap<String, HomeData> reordered = new LinkedHashMap<>();
        reordered.put(name, h);
        reordered.putAll(l);
        homes.put(uuid, reordered);
        save();
        return true;
    }

    public boolean setPublic(UUID uuid, String name, boolean pub) {
        HomeData h = list(uuid).get(name);
        if (h == null) return false;
        h.isPublic = pub;
        save();
        return true;
    }

    // ===== 邀请 =====
    public void invite(UUID owner, UUID target, String homeName) {
        invites.put(target, new Invite(owner, homeName));
    }

    public Invite getInvite(UUID target) {
        Invite i = invites.get(target);
        if (i == null) return null;
        if (System.currentTimeMillis() > i.expireAt) { invites.remove(target); return null; }
        return i;
    }

    public void clearInvite(UUID target) { invites.remove(target); }

    // ===== 传送冷却 =====
    public boolean onCooldown(UUID uuid) {
        Long t = lastTeleport.get(uuid);
        return t != null && System.currentTimeMillis() - t < 2000L;
    }

    public void markTeleport(UUID uuid) { lastTeleport.put(uuid, System.currentTimeMillis()); }

    /** 主城坐标（复用主类 getSpawnLocation，与回城一致） */
    public Location getSpawnLocation() { return plugin.getSpawnLocation(); }
}
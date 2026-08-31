package nl.tinyaii.tinyplayer.auth;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 登录管理：账号密码存储（SHA-256+盐哈希，不存明文）+ 登录状态。
 */
public class AuthManager {

    private final TinyPlayerPlugin plugin;
    private final Map<UUID, String> passwords = new HashMap<>();
    private final Map<UUID, Boolean> loggedIn = new HashMap<>();
    private final Map<UUID, Long> joinTime = new HashMap<>();
    private final Map<UUID, Location> originalLocations = new HashMap<>();
    private final Map<UUID, Location> persistedLocations = new HashMap<>();
    private File file;

    public AuthManager(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        passwords.clear();
        persistedLocations.clear();
        file = new File(plugin.getDataFolder(), "auth.yml");
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("accounts");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    passwords.put(uuid, root.getString(key, ""));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        // 读取未登录退出时持久化的原位置
        ConfigurationSection pending = yml.getConfigurationSection("pending-locations");
        if (pending != null) {
            for (String key : pending.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    Location loc = parseLocation(pending.getString(key, ""));
                    if (loc != null) persistedLocations.put(uuid, loc);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, String> e : passwords.entrySet()) {
            yml.set("accounts." + e.getKey(), e.getValue());
        }
        for (Map.Entry<UUID, Location> e : persistedLocations.entrySet()) {
            yml.set("pending-locations." + e.getKey(), serializeLocation(e.getValue()));
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("保存 auth.yml 失败: " + ex.getMessage());
        }
    }

    public boolean hasAccount(UUID uuid) { return passwords.containsKey(uuid); }

    public boolean register(UUID uuid, String password) {
        if (hasAccount(uuid)) return false;
        passwords.put(uuid, hash(password));
        save();
        return true;
    }

    /** 删除账号（管理员重置密码用） */
    public boolean deleteAccount(UUID uuid) {
        boolean ok = passwords.remove(uuid) != null;
        loggedIn.remove(uuid);
        if (ok) save();
        return ok;
    }

    public boolean checkPassword(UUID uuid, String password) {
        String stored = passwords.get(uuid);
        if (stored == null) return false;
        return stored.equals(hash(password));
    }

    public boolean isLoggedIn(UUID uuid) {
        return loggedIn.getOrDefault(uuid, false);
    }

    public void setLoggedIn(UUID uuid, boolean v) {
        loggedIn.put(uuid, v);
    }

    public void recordJoin(UUID uuid) { joinTime.put(uuid, System.currentTimeMillis()); }

    public long joinMillis(UUID uuid) {
        Long t = joinTime.get(uuid);
        return t == null ? 0 : t;
    }

    public void clearJoin(UUID uuid) { joinTime.remove(uuid); }

    /** 记录玩家进服时的原始位置（未登录期间强制在出生点，登录后传回） */
    public void recordOriginalLocation(UUID uuid, Location loc) {
        originalLocations.put(uuid, loc);
    }

    /** 取原位置：优先本次会话记录，其次上次未登录退出时持久化的位置 */
    public Location getOriginalLocation(UUID uuid) {
        Location loc = originalLocations.get(uuid);
        if (loc != null) return loc;
        return persistedLocations.get(uuid);
    }

    /** 未登录退出时调用：把原位置持久化，下次进服仍能回到真正退出位置（而不是出生点） */
    public void persistOriginalLocation(UUID uuid) {
        Location loc = originalLocations.get(uuid);
        if (loc != null) {
            persistedLocations.put(uuid, loc);
            save();
        }
    }

    /** 是否有持久化的原位置（上次未登录退出的） */
    public boolean hasPersistedLocation(UUID uuid) {
        return persistedLocations.containsKey(uuid);
    }

    public void clearOriginalLocation(UUID uuid) {
        originalLocations.remove(uuid);
        persistedLocations.remove(uuid);
        save();
    }

    /** 序列化 Location 为字符串 world:x:y:z:yaw:pitch */
    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ()
                + ":" + loc.getYaw() + ":" + loc.getPitch();
    }

    private Location parseLocation(String s) {
        try {
            String[] parts = s.split(":");
            if (parts.length < 4) return null;
            org.bukkit.World w = plugin.getServer().getWorld(parts[0]);
            if (w == null) return null;
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0f;
            return new Location(w, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断是否为基岩版玩家（Geyser/Floodgate 进入）。
     * 优先 Floodgate API（软依赖反射），兜底 UUID 前缀（Floodgate 基岩玩家固定 00000000-0000-0000-0000- 开头）。
     */
    public static boolean isBedrockPlayer(UUID uuid) {
        String s = uuid.toString();
        if (s.startsWith("00000000-0000-0000-0000-")) return true;
        try {
            Class<?> clazz = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = clazz.getMethod("getInstance").invoke(null);
            return (Boolean) clazz.getMethod("isFloodgatePlayer", UUID.class).invoke(api, uuid);
        } catch (Exception ignored) {
            return false;
        }
    }

    /** SHA-256 + 盐 哈希 */
    private String hash(String password) {
        String salt = plugin.getConfig().getString("auth.salt", "TinyAII_2026");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "hash_error";
        }
    }
}

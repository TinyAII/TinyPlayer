package nl.tinyaii.tinyplayer.claim.storage;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.data.Claim;
import nl.tinyaii.tinyplayer.claim.data.ClaimManager;
import nl.tinyaii.tinyplayer.claim.data.Cuboid;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * YAML 存储：claims.yml（领地/成员/flag 覆盖）。
 * 与全家桶一致（零依赖，不打包 SQLite 驱动）。
 */
public class Storage {

    private final TinyPlayerPlugin plugin;
    private File file;

    public Storage(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public void open() {
        file = new File(plugin.getDataFolder(), "claims.yml");
        if (!file.exists()) {
            try {
                if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException ignored) {}
        }
    }

    public void close() { }

    private YamlConfiguration load() {
        return YamlConfiguration.loadConfiguration(file);
    }

    private void save(YamlConfiguration yml) {
        try { yml.save(file); } catch (IOException ex) {
            plugin.getLogger().severe("保存 claims.yml 失败: " + ex.getMessage());
        }
    }

    /** 保存单个领地 */
    public void saveClaim(Claim c) {
        YamlConfiguration yml = load();
        writeClaim(yml, c);
        save(yml);
    }

    private void writeClaim(YamlConfiguration yml, Claim c) {
        String path = "claims." + c.getId();
        Cuboid cub = c.getCuboid();
        yml.set(path + ".name", c.getName());
        yml.set(path + ".owner", c.getOwner().toString());
        yml.set(path + ".parent", c.getParentId());
        yml.set(path + ".world", cub.getWorldUid().toString());
        yml.set(path + ".minX", cub.getMinX());
        yml.set(path + ".minY", cub.getMinY());
        yml.set(path + ".minZ", cub.getMinZ());
        yml.set(path + ".maxX", cub.getMaxX());
        yml.set(path + ".maxY", cub.getMaxY());
        yml.set(path + ".maxZ", cub.getMaxZ());
        yml.set(path + ".createdAt", c.getCreatedAt());
        if (c.getWelcomeMsg() != null) yml.set(path + ".welcome", c.getWelcomeMsg());
        if (c.getLeaveMsg() != null) yml.set(path + ".leave", c.getLeaveMsg());
        // 成员
        for (var e : c.getMembers().entrySet()) {
            yml.set(path + ".members." + e.getKey(), e.getValue().name());
        }
        // flag 覆盖
        for (var e : c.getFlagOverrides().entrySet()) {
            yml.set(path + ".flags." + e.getKey(), e.getValue());
        }
    }

    /** 删除领地 */
    public void deleteClaim(int id) {
        YamlConfiguration yml = load();
        yml.set("claims." + id, null);
        save(yml);
    }

    /** 保存全局权限成员（owner -> [成员列表]） */
    public void saveGlobalAdmins(nl.tinyaii.tinyplayer.claim.data.ClaimManager manager) {
        YamlConfiguration yml = load();
        yml.set("global-admins", null);
        for (var e : manager.getAllGlobalAdmins().entrySet()) {
            yml.set("global-admins." + e.getKey().toString(), new java.util.ArrayList<>(e.getValue()));
        }
        save(yml);
    }

    /** 加载全局权限成员 */
    public void loadGlobalAdmins(nl.tinyaii.tinyplayer.claim.data.ClaimManager manager) {
        YamlConfiguration yml = load();
        manager.clearGlobalAdmins();
        org.bukkit.configuration.ConfigurationSection sec = yml.getConfigurationSection("global-admins");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                UUID owner = UUID.fromString(key);
                java.util.Set<UUID> admins = new java.util.HashSet<>();
                for (String s : sec.getStringList(key)) {
                    try { admins.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
                }
                manager.putGlobalAdmins(owner, admins);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    /** 全量加载进内存 */
    public void loadAll(ClaimManager manager) {
        YamlConfiguration yml = load();
        ConfigurationSection root = yml.getConfigurationSection("claims");
        if (root == null) return;
        for (String idStr : root.getKeys(false)) {
            try {
                int id = Integer.parseInt(idStr);
                ConfigurationSection sec = root.getConfigurationSection(idStr);
                if (sec == null) continue;
                Claim c = new Claim(
                        sec.getString("name", "领地"),
                        UUID.fromString(sec.getString("owner")),
                        new Cuboid(
                                UUID.fromString(sec.getString("world")),
                                sec.getInt("minX"), sec.getInt("minY"), sec.getInt("minZ"),
                                sec.getInt("maxX"), sec.getInt("maxY"), sec.getInt("maxZ")));
                c.setId(id);
                c.setCreatedAt(sec.getLong("createdAt", System.currentTimeMillis()));
                if (sec.contains("welcome")) c.setWelcomeMsg(sec.getString("welcome"));
                if (sec.contains("leave")) c.setLeaveMsg(sec.getString("leave"));
                if (sec.contains("parent")) {
                    Object parent = sec.get("parent");
                    if (parent instanceof Number) c.setParentId(((Number) parent).intValue());
                }
                ConfigurationSection memSec = sec.getConfigurationSection("members");
                if (memSec != null) {
                    for (String key : memSec.getKeys(false)) {
                        c.addMember(UUID.fromString(key), Claim.Role.valueOf(memSec.getString(key)));
                    }
                }
                ConfigurationSection flagSec = sec.getConfigurationSection("flags");
                if (flagSec != null) {
                    for (String key : flagSec.getKeys(false)) {
                        c.setFlagOverride(key, flagSec.getBoolean(key));
                    }
                }
                manager.addClaim(c);
                manager.setNextId(id + 1);
            } catch (Exception ex) {
                plugin.getLogger().severe("加载领地 " + idStr + " 失败: " + ex.getMessage());
            }
        }
    }
}

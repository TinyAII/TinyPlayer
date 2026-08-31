package nl.tinyaii.tinyplayer.warp;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 传送点数据：管理员创建的公共传送点，持久化 warps.yml。
 */
public class WarpManager {

    public static class WarpData {
        public String name;
        public String world;
        public double x, y, z;
        public float yaw, pitch;

        public WarpData(String name, Location loc) {
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

    private final TinyPlayerPlugin plugin;
    private final Map<String, WarpData> warps = new LinkedHashMap<>();
    private File file;

    public WarpManager(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        warps.clear();
        file = new File(plugin.getDataFolder(), "warps.yml");
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("warps");
        if (root == null) return;
        for (String name : root.getKeys(false)) {
            WarpData w = new WarpData(name, new Location(
                    Bukkit.getWorld(root.getString(name + ".world", "world")) != null
                            ? Bukkit.getWorld(root.getString(name + ".world", "world"))
                            : Bukkit.getWorlds().get(0),
                    root.getDouble(name + ".x", 0),
                    root.getDouble(name + ".y", 64),
                    root.getDouble(name + ".z", 0)));
            w.world = root.getString(name + ".world", "world");
            w.yaw = (float) root.getDouble(name + ".yaw", 0);
            w.pitch = (float) root.getDouble(name + ".pitch", 0);
            warps.put(name, w);
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (WarpData w : warps.values()) {
            String path = "warps." + w.name;
            yml.set(path + ".world", w.world);
            yml.set(path + ".x", w.x);
            yml.set(path + ".y", w.y);
            yml.set(path + ".z", w.z);
            yml.set(path + ".yaw", w.yaw);
            yml.set(path + ".pitch", w.pitch);
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("保存 warps.yml 失败: " + ex.getMessage());
        }
    }

    public Map<String, WarpData> getWarps() { return warps; }
    public WarpData getWarp(String name) { return warps.get(name); }
    public boolean hasWarp(String name) { return warps.containsKey(name); }

    public void setWarp(String name, Location loc) {
        warps.put(name, new WarpData(name, loc));
        save();
    }

    public boolean removeWarp(String name) {
        boolean ok = warps.remove(name) != null;
        if (ok) save();
        return ok;
    }
}
package nl.tinyaii.tinyplayer.economy;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 内置经济实现：玩家金币余额，持久化 economy-data.yml。
 * 仅当未对接外部经济（自家 Economy / Vault）时作为兜底使用。
 */
public class EconomyManager {

    private final TinyPlayerPlugin plugin;
    private final Map<UUID, Double> balances = new HashMap<>();
    private File file;

    public EconomyManager(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        balances.clear();
        file = new File(plugin.getDataFolder(), "economy-data.yml");
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("balances");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                balances.put(UUID.fromString(key), root.getDouble(key, 0));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, Double> e : balances.entrySet()) {
            yml.set("balances." + e.getKey(), Math.round(e.getValue() * 100) / 100.0);
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("保存 economy-data.yml 失败: " + ex.getMessage());
        }
    }

    public double getBalance(UUID uuid) { return balances.getOrDefault(uuid, 0.0); }

    public boolean has(UUID uuid) { return balances.containsKey(uuid); }

    public void setBalance(UUID uuid, double amount) {
        balances.put(uuid, Math.max(0, amount));
        save();
    }

    public void add(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) + amount);
    }

    /** 转账：返回 false=余额不足 */
    public boolean transfer(UUID from, UUID to, double amount) {
        if (amount <= 0) return false;
        if (getBalance(from) < amount) return false;
        setBalance(from, getBalance(from) - amount);
        setBalance(to, getBalance(to) + amount);
        return true;
    }

    /** 前 N 名排行（玩家名解析交给调用方） */
    public Map<UUID, Double> top(int n) {
        return balances.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(n)
                .collect(java.util.LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        Map::putAll);
    }
}
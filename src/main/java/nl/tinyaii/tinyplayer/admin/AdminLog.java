package nl.tinyaii.tinyplayer.admin;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 管理员操作日志：每次神权操作写入 admin-log.yml（可追溯防滥用）。
 */
public class AdminLog {

    private final TinyPlayerPlugin plugin;
    private final File file;

    public AdminLog(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "admin-log.yml");
    }

    /** 记录一条操作：时间 / 操作者 / 目标 / 动作 / 详情 */
    public void log(String sender, String target, String action, String detail) {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        String key = "log." + System.currentTimeMillis();
        yml.set(key + ".time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        yml.set(key + ".sender", sender);
        yml.set(key + ".target", target);
        yml.set(key + ".action", action);
        yml.set(key + ".detail", detail);
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("写入 admin-log.yml 失败: " + ex.getMessage());
        }
    }
}
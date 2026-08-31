package nl.tinyaii.tinyplayer.claim;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.command.ClaimCommand;
import nl.tinyaii.tinyplayer.claim.command.SelectionManager;
import nl.tinyaii.tinyplayer.claim.data.ClaimManager;
import nl.tinyaii.tinyplayer.claim.events.EnterListener;
import nl.tinyaii.tinyplayer.claim.events.EnvEvents;
import nl.tinyaii.tinyplayer.claim.events.PlayerEvents;
import nl.tinyaii.tinyplayer.claim.events.SelectionEvents;
import nl.tinyaii.tinyplayer.claim.flags.FlagCheck;
import nl.tinyaii.tinyplayer.claim.gui.ClaimDetailGuiListener;
import nl.tinyaii.tinyplayer.claim.gui.ClaimGuiListener;
import nl.tinyaii.tinyplayer.claim.gui.ClaimListGuiListener;
import nl.tinyaii.tinyplayer.claim.gui.GlobalPanelGuiListener;
import nl.tinyaii.tinyplayer.claim.gui.MemberGuiListener;
import nl.tinyaii.tinyplayer.claim.gui.SubClaimGuiListener;
import nl.tinyaii.tinyplayer.claim.gui.WelcomeGuiListener;
import nl.tinyaii.tinyplayer.claim.storage.Storage;
import nl.tinyaii.tinyplayer.claim.util.EcoBridge;

/**
 * 领地模块（TinyClaim v1.8.0 复刻进 TinyPlayer 的第 11 模块）。
 * 由 TinyPlayerPlugin 在 claim.enabled=true 时初始化；命令/事件注册到宿主插件。
 */
public class ClaimModule {

    private final TinyPlayerPlugin plugin;
    private Storage storage;
    private ClaimManager claimManager;
    private FlagCheck flagCheck;
    private EcoBridge ecoBridge;
    private SelectionManager selectionManager;

    public ClaimModule(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        migrateConfig();
        storage = new Storage(plugin);
        storage.open();

        claimManager = new ClaimManager(plugin);
        storage.loadAll(claimManager);
        storage.loadGlobalAdmins(claimManager);

        flagCheck = new FlagCheck(plugin);

        plugin.getServer().getPluginManager().registerEvents(new PlayerEvents(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new EnvEvents(plugin), plugin);
        selectionManager = new SelectionManager(plugin);
        plugin.getServer().getPluginManager().registerEvents(new SelectionEvents(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new EnterListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ClaimGuiListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new MemberGuiListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GlobalPanelGuiListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ClaimListGuiListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ClaimDetailGuiListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new WelcomeGuiListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SubClaimGuiListener(plugin), plugin);

        plugin.getCommand("领地").setExecutor(new ClaimCommand(plugin));

        ecoBridge = new EcoBridge(plugin);

        plugin.getLogger().info("[模块] 领地：开启（已加载 " + claimManager.getAll().size() + " 个领地）");
    }

    public void onDisable() {
        if (storage != null) storage.close();
    }

    /** 配置迁移：旧 config.yml 无 claim 段时，从插件默认配置合并缺失键（防回退内置默认导致权限显示错误） */
    private void migrateConfig() {
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        java.io.File cfgFile = new java.io.File(plugin.getDataFolder(), "config.yml");
        if (!cfgFile.exists() || cfg.contains("claim")) return;
        // 读内置默认配置的 claim 段
        java.io.InputStream defStream = plugin.getResource("config.yml");
        if (defStream == null) return;
        org.bukkit.configuration.file.YamlConfiguration def =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                        new java.io.InputStreamReader(defStream, java.nio.charset.StandardCharsets.UTF_8));
        org.bukkit.configuration.ConfigurationSection claimSec = def.getConfigurationSection("claim");
        if (claimSec == null) return;
        for (String key : claimSec.getKeys(true)) {
            String path = "claim." + key;
            if (!cfg.contains(path)) {
                cfg.set(path, claimSec.get(key));
            }
        }
        try { cfg.save(cfgFile); } catch (java.io.IOException ex) {
            plugin.getLogger().warning("[领地] 配置迁移失败: " + ex.getMessage());
        }
    }

    public Storage getStorage() { return storage; }
    public ClaimManager getClaimManager() { return claimManager; }
    public FlagCheck getFlagCheck() { return flagCheck; }
    public EcoBridge getClaimEcoBridge() { return ecoBridge; }
    public SelectionManager getSelectionManager() { return selectionManager; }
}

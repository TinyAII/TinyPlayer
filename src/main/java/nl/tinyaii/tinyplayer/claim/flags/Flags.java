package nl.tinyaii.tinyplayer.claim.flags;

import nl.tinyaii.tinyplayer.claim.data.Claim;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flag 定义：环境 Flag（影响世界/实体）与权限 Flag（决定玩家行为）。
 * 语义：flag = true 表示允许该行为；false 表示禁止。
 */
public final class Flags {

    private Flags() {}

    /** 环境 Flag 名称集合（默认值） */
    public static final Map<String, Boolean> ENV_FLAGS = new LinkedHashMap<>();
    /** 权限 Flag 名称集合（默认值） */
    public static final Map<String, Boolean> PRI_FLAGS = new LinkedHashMap<>();

    static {
        // 环境
        ENV_FLAGS.put("creeper-explosion", false);
        ENV_FLAGS.put("tnt-explosion", false);
        ENV_FLAGS.put("fire-spread", false);
        ENV_FLAGS.put("lava-flow", false);
        ENV_FLAGS.put("water-flow", false);
        ENV_FLAGS.put("piston-push", false);
        ENV_FLAGS.put("mob-grief", false);
        ENV_FLAGS.put("mob-trample", false);
        ENV_FLAGS.put("animal-trample", false);
        ENV_FLAGS.put("item-drop", true);
        ENV_FLAGS.put("mob-spawn", true);
        ENV_FLAGS.put("animal-spawn", true);

        // 权限
        PRI_FLAGS.put("build", false);
        PRI_FLAGS.put("interact", false);
        PRI_FLAGS.put("container", false);
        PRI_FLAGS.put("pvp", false);
        PRI_FLAGS.put("attack-monster", false);
        PRI_FLAGS.put("attack-animal", false);
        PRI_FLAGS.put("use-elytra", true);
        PRI_FLAGS.put("use-redstone", false);
        PRI_FLAGS.put("enter", true);
        PRI_FLAGS.put("chest-access", false);
    }

    public static boolean isEnvFlag(String name) { return ENV_FLAGS.containsKey(name); }
    public static boolean isPriFlag(String name) { return PRI_FLAGS.containsKey(name); }
    public static boolean isKnown(String name) { return ENV_FLAGS.containsKey(name) || PRI_FLAGS.containsKey(name); }

    /** Flag 中文显示名（面板/消息用；未收录回退英文原键） */
    public static final Map<String, String> DISPLAY_NAMES = new LinkedHashMap<>();
    static {
        // 环境
        DISPLAY_NAMES.put("creeper-explosion", "苦力怕爆炸");
        DISPLAY_NAMES.put("tnt-explosion", "TNT爆炸");
        DISPLAY_NAMES.put("fire-spread", "火焰蔓延");
        DISPLAY_NAMES.put("lava-flow", "岩浆流动");
        DISPLAY_NAMES.put("water-flow", "水流");
        DISPLAY_NAMES.put("piston-push", "活塞推出");
        DISPLAY_NAMES.put("mob-grief", "怪物破坏");
        DISPLAY_NAMES.put("mob-trample", "怪物踩踏");
        DISPLAY_NAMES.put("animal-trample", "动物踩踏");
        DISPLAY_NAMES.put("item-drop", "掉落物");
        DISPLAY_NAMES.put("mob-spawn", "怪物生成");
        DISPLAY_NAMES.put("animal-spawn", "动物生成");
        // 权限
        DISPLAY_NAMES.put("build", "建造破坏");
        DISPLAY_NAMES.put("interact", "交互使用");
        DISPLAY_NAMES.put("container", "容器");
        DISPLAY_NAMES.put("pvp", "PVP战斗");
        DISPLAY_NAMES.put("attack-monster", "攻击怪物");
        DISPLAY_NAMES.put("attack-animal", "攻击动物");
        DISPLAY_NAMES.put("use-elytra", "鞘翅飞行");
        DISPLAY_NAMES.put("use-redstone", "红石设备");
        DISPLAY_NAMES.put("enter", "进入");
        DISPLAY_NAMES.put("chest-access", "箱子权限");
    }

    /** 取中文显示名 */
    public static String displayName(String key) {
        return DISPLAY_NAMES.getOrDefault(key, key);
    }

    /** 从 config 读取环境 flag 默认值（允许覆盖） */
    public static boolean getEnvDefault(String name, boolean fallback) {
        return fallback;
    }
}

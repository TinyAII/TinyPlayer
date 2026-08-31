package nl.tinyaii.tinyplayer.claim.util;

import org.bukkit.Material;

/**
 * 功能方块中文名映射：被拦截提示用（"你没有权限使用该领地内的 xx"）。
 * 精简版：只收录常见容器/门/按钮/设备；未收录回退英文。
 */
public final class BlockNames {

    private BlockNames() {}

    private static final java.util.Map<String, String> ZH = new java.util.HashMap<>();

    private static void put(String k, String v) { ZH.put(k.toUpperCase(), v); }

    static {
        // 容器
        put("CHEST", "箱子"); put("TRAPPED_CHEST", "陷阱箱"); put("ENDER_CHEST", "末影箱");
        put("BARREL", "木桶"); put("FURNACE", "熔炉"); put("BLAST_FURNACE", "高炉");
        put("SMOKER", "烟熏炉"); put("HOPPER", "漏斗"); put("DISPENSER", "发射器");
        put("DROPPER", "投掷器"); put("BREWING_STAND", "酿造台");
        put("CRAFTING_TABLE", "工作台"); put("ENCHANTING_TABLE", "附魔台"); put("ANVIL", "铁砧");
        put("STONECUTTER", "切石机"); put("SMITHING_TABLE", "锻造台"); put("GRINDSTONE", "砂轮");
        put("LOOM", "织布机"); put("CARTOGRAPHY_TABLE", "制图台"); put("FLETCHING_TABLE", "制箭台");
        // 门
        put("OAK_DOOR", "橡木门"); put("SPRUCE_DOOR", "云杉木门"); put("BIRCH_DOOR", "白桦木门");
        put("JUNGLE_DOOR", "丛林木门"); put("ACACIA_DOOR", "金合欢木门"); put("DARK_OAK_DOOR", "深色橡木门");
        put("CRIMSON_DOOR", "绯红木门"); put("WARPED_DOOR", "诡异木门"); put("IRON_DOOR", "铁门");
        // 开关
        put("LEVER", "拉杆"); put("STONE_BUTTON", "石按钮"); put("OAK_BUTTON", "橡木按钮");
        put("STONE_PRESSURE_PLATE", "石压力板"); put("OAK_PRESSURE_PLATE", "橡木压力板");
        // 功能
        put("BEACON", "信标"); put("CONDUIT", "潮涌核心"); put("SHULKER_BOX", "潜影盒");
    }

    public static String name(Material m) {
        String zh = ZH.get(m.name());
        if (zh != null) return zh;
        // 回退：英文美化（小写转首字母大写，_ 转空格）
        String raw = m.name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}

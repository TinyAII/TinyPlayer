package nl.tinyaii.tinyplayer.claim.command;

import nl.tinyaii.tinyplayer.TinyPlayerPlugin;
import nl.tinyaii.tinyplayer.claim.data.Claim;
import nl.tinyaii.tinyplayer.claim.data.ClaimManager;
import nl.tinyaii.tinyplayer.claim.data.Cuboid;
import nl.tinyaii.tinyplayer.claim.flags.Flags;
import nl.tinyaii.tinyplayer.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 领地命令（双语）：
 *  /领地 创建 <名>            / claim create <name>
 *  /领地 删除 [确认]          / claim delete [confirm]
 *  /领地 信息                 / claim info
 *  /领地 列表                 / claim list
 *  /领地 选择 开始|结束        / claim select start|end
 *  /领地 成员 添加|移除 <玩家>  / claim member add|remove <p>
 *  /领地 flag 设置|查看 <名> [值]  / claim flag set|get <name> [value]
 *  /领地 传送                 / claim tp
 *  /领地 管理 删除 <玩家>      / claim admin delete <p>
 *  /领地 重载                 / claim reload
 */
public class ClaimCommand implements CommandExecutor {

    private final TinyPlayerPlugin plugin;

    public ClaimCommand(TinyPlayerPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isAdmin(CommandSender s) {
        return s.hasPermission("claim.admin") || s.isOp();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
        Player p = (Player) sender;
        if (args.length == 0) { help(p); return true; }
        String sub = args[0];
        switch (sub) {
            case "清除选区": case "clear": case "取消选区": case "取消":
                plugin.getClaimSelectionManager().clearSelection(p);
                p.sendMessage(Messages.color("&a已清除选区，可以重新右键选点。"));
                return true;
            case "创建": case "create":
                if (args.length < 2) { p.sendMessage(Messages.color("&c用法: /领地 创建 <名字>")); return true; }
                return create(p, args[1]);
            case "圈地": case "coords":
                if (args.length < 8) { p.sendMessage(Messages.color("&c用法: /领地 圈地 <名字> <x1> <y1> <z1> <x2> <y2> <z2>")); return true; }
                return coords(p, args);
            case "半径": case "radius":
                if (args.length < 3) { p.sendMessage(Messages.color("&c用法: /领地 半径 <名字> <半径> [y1] [y2]（不输 Y 默认 ±64）")); return true; }
                return radius(p, args);
            case "删除": case "delete":
                return delete(p, args.length > 1 && args[1].equals("确认"));
            case "信息": case "info":
                return info(p, args.length > 1 ? args[1] : null);
            case "列表": case "list":
                return list(p);
            case "成员": case "member":
                if (args.length < 3) { p.sendMessage(Messages.color("&c用法: /领地 成员 添加 <玩家> [admin] | /领地 成员 移除 <玩家>")); return true; }
                return member(p, args);
            case "flag": case "flags":
                return flag(p, args);
            case "传送": case "tp":
                return tp(p, args.length > 1 ? args[1] : null);
            case "管理": case "admin":
                if (!isAdmin(sender)) { p.sendMessage(Messages.color("&c无权限。")); return true; }
                return admin(p, args);
            case "退出语": case "leave":
                if (args.length < 2) { p.sendMessage(Messages.color("&c用法: /领地 退出语 <消息>（空=清除）")); return true; }
                return leave(p, args);
            case "欢迎语": case "welcome":
                if (args.length < 2) { p.sendMessage(Messages.color("&c用法: /领地 欢迎语 <消息>（空=清除）")); return true; }
                return welcome(p, args);
            case "面板": case "gui": case "panel":
                return panel(p);
            case "子": case "child":
                return child(p, args);
            case "模板": case "template":
                return template(p, args);
            case "扩展": case "expand":
                if (args.length < 3) { p.sendMessage(Messages.color("&c用法: /领地 扩展 <方向> <格数>（方向: 上/下/北/南/东/西）")); return true; }
                return expand(p, args);
            case "重载": case "reload":
                if (!isAdmin(sender)) { p.sendMessage(Messages.color("&c无权限。")); return true; }
                plugin.getClaimManager().invalidateCache();
                p.sendMessage(Messages.color("&a已重载索引缓存。"));
                return true;
            default:
                help(p);
                return true;
        }
    }

    // ===== 创建（木锄头选区版）=====
    private boolean create(Player p, String name) {
        nl.tinyaii.tinyplayer.claim.command.SelectionManager sel = plugin.getClaimSelectionManager();
        Location a = sel.getSel1(p.getUniqueId());
        Location b = sel.getSel2(p.getUniqueId());
        if (a == null || b == null) {
            p.sendMessage(Messages.color("&c请先用木锄头右键选两点（第一次=起点，第二次=终点）。"));
            return true;
        }
        if (!a.getWorld().getUID().equals(b.getWorld().getUID())) {
            p.sendMessage(Messages.color("&c两个点必须在同一世界。"));
            return true;
        }
        boolean ok = doCreate(p, name, Cuboid.fromLocations(a, b), sel);
        if (ok) sel.clearSelection(p);
        return true;
    }

    // ===== 指令圈地：/领地 圈地 <名字> <x1> <y1> <z1> <x2> <y2> <z2> =====
    // 两个完整坐标点，Y 即领地高低度（例：1号(4,5,6) 2号(1,2,3) → Y 范围 5-2）
    private boolean coords(Player p, String[] args) {
        try {
            int x1 = Integer.parseInt(args[2]);
            int y1 = Integer.parseInt(args[3]);
            int z1 = Integer.parseInt(args[4]);
            int x2 = Integer.parseInt(args[5]);
            int y2 = Integer.parseInt(args[6]);
            int z2 = Integer.parseInt(args[7]);
            Cuboid cub = new Cuboid(p.getWorld().getUID(), x1, y1, z1, x2, y2, z2);
            doCreate(p, args[1], cub, null);
            return true;
        } catch (NumberFormatException e) {
            p.sendMessage(Messages.color("&c坐标必须是整数。"));
            return true;
        }
    }

    // ===== 半径圈地：/领地 半径 <名字> <半径> [y1] [y2]（以玩家为中心的正方形，可选指定 Y 高低）=====
    private boolean radius(Player p, String[] args) {
        int r;
        try { r = Integer.parseInt(args[2]); }
        catch (NumberFormatException e) { p.sendMessage(Messages.color("&c半径必须是整数。")); return true; }
        if (r < 1 || r > 128) { p.sendMessage(Messages.color("&c半径范围 1-128。")); return true; }
        Location loc = p.getLocation();
        int cx = loc.getBlockX(), cz = loc.getBlockZ();
        int yLow, yHigh;
        if (args.length >= 5) {
            // 玩家指定 Y 高低（与指令圈地一致）
            try {
                yLow = Integer.parseInt(args[3]);
                yHigh = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) { p.sendMessage(Messages.color("&cY 坐标必须是整数。")); return true; }
        } else {
            // 默认：玩家站立 ±64
            int baseY = loc.getBlockY();
            yLow = Math.max(0, baseY - 64);
            yHigh = Math.min(255, baseY + 64);
        }
        Cuboid cub = new Cuboid(p.getWorld().getUID(),
                cx - r, yLow, cz - r, cx + r, yHigh, cz + r);
        doCreate(p, args[1], cub, null);
        return true;
    }

    // ===== 创建核心：三种方式共用（校验/上限/重叠/经济/保存）=====
    private boolean doCreate(Player p, String name, Cuboid cub,
                             nl.tinyaii.tinyplayer.claim.command.SelectionManager sel) {
        ClaimManager mgr = plugin.getClaimManager();
        // 大小限制
        int sizeX = cub.getMaxX() - cub.getMinX() + 1;
        int sizeZ = cub.getMaxZ() - cub.getMinZ() + 1;
        int sizeY = cub.getMaxY() - cub.getMinY() + 1;
        int maxXZ = plugin.getConfig().getInt("claim.settings.max-size-xz", 256);
        int maxY = plugin.getConfig().getInt("claim.settings.max-size-y", 256);
        int minSize = plugin.getConfig().getInt("claim.settings.min-size", 1);
        if (sizeX > maxXZ || sizeZ > maxXZ) { p.sendMessage(Messages.color("&c领地过大（长宽上限 " + maxXZ + "）。")); return false; }
        if (sizeY > maxY) { p.sendMessage(Messages.color("&c领地过高（高度上限 " + maxY + "）。")); return false; }
        if (sizeX < minSize || sizeZ < minSize || sizeY < minSize) { p.sendMessage(Messages.color("&c领地过小。")); return false; }

        // 上限
        int limit = claimLimit(p);
        if (mgr.getClaimsOf(p.getUniqueId()).size() >= limit) {
            p.sendMessage(Messages.color("&c已达领地数量上限（" + limit + " 个）。"));
            return false;
        }
        // 重叠检测
        if (plugin.getConfig().getBoolean("claim.settings.overlap-forbidden", true)) {
            for (Claim c : mgr.getAll()) {
                if (c.getCuboid().intersects(cub)) {
                    p.sendMessage(Messages.color("&c与领地 &e" + c.getName() + " &c重叠，无法创建。"));
                    return false;
                }
            }
        }
        // 经济扣费
        double cost = plugin.getConfig().getDouble("claim.settings.economy.claim-cost", 0);
        if (cost > 0) {
            String err = plugin.getClaimEcoBridge().tryWithdraw(p, cost);
            if (err != null) { p.sendMessage(Messages.color("&c" + err)); return false; }
        }

        Claim claim = new Claim(name, p.getUniqueId(), cub);
        claim.setId(mgr.allocateId());
        mgr.addClaim(claim);
        plugin.getClaimStorage().saveClaim(claim);
        p.sendMessage(Messages.color("&a领地 &e" + name + " &a创建成功！"));
        return true;
    }

    private int claimLimit(Player p) {
        if (p.hasPermission("claim.admin") || p.isOp()) return Integer.MAX_VALUE;
        int limit = plugin.getConfig().getInt("claim.settings.max-claims-default", 3);
        for (String perm : new String[]{"svip", "vip", "default"}) {
            if (p.hasPermission("claim.limit." + perm)) {
                int v = plugin.getConfig().getInt("claim.settings.max-claims-" + perm, limit);
                if (v > limit) limit = v;
            }
        }
        return limit;
    }

    // ===== 删除 =====
    private boolean delete(Player p, boolean confirmed) {
        ClaimManager mgr = plugin.getClaimManager();
        Claim c = mgr.getClaimAt(p.getLocation().getWorld().getUID(),
                p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ());
        if (c == null) { p.sendMessage(Messages.color("&c你不在任何领地内。")); return true; }
        if (!c.isOwner(p.getUniqueId()) && !isAdmin(p)) { p.sendMessage(Messages.color("&c只有领地主人才能删除。")); return true; }
        if (!confirmed) {
            p.sendMessage(Messages.color("&c确认删除领地 &e" + c.getName() + "&c？再输 &e/领地 删除 确认"));
            return true;
        }
        mgr.removeClaim(c);
        plugin.getClaimStorage().deleteClaim(c.getId());
        p.sendMessage(Messages.color("&a已删除领地 &e" + c.getName()));
        return true;
    }

    // ===== 信息：/领地 信息 <领地名>（无参=查脚下领地）=====
    private boolean info(Player p, String name) {
        ClaimManager mgr = plugin.getClaimManager();
        Claim c = null;
        if (name != null && !name.isEmpty()) {
            c = findClaimByName(name);
            if (c == null) { p.sendMessage(Messages.color("&c找不到领地 &e" + name)); return true; }
        } else {
            c = mgr.getClaimAt(p.getLocation().getWorld().getUID(),
                    p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ());
            if (c == null) { p.sendMessage(Messages.color("&c你不在任何领地内。用法: /领地 信息 <领地名>")); return true; }
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(c.getOwner());
        p.sendMessage(Messages.color("&e===== 领地信息 ====="));
        p.sendMessage(Messages.color("&7名称: &e" + c.getName()));
        p.sendMessage(Messages.color("&7主人: &e" + owner.getName()));
        p.sendMessage(Messages.color("&7成员数: &e" + c.getMembers().size()));
        p.sendMessage(Messages.color("&7范围: &e" + c.getCuboid()));
        return true;
    }

    // ===== 列表 =====
    private boolean list(Player p) {
        List<Claim> claims = plugin.getClaimManager().getClaimsOf(p.getUniqueId());
        if (claims.isEmpty()) { p.sendMessage(Messages.color("&7你还没有领地。")); return true; }
        p.sendMessage(Messages.color("&e===== 我的领地 ====="));
        for (Claim c : claims) {
            p.sendMessage(Messages.color("&7- &e" + c.getName() + " &7(" + c.getCuboid() + ")"));
        }
        return true;
    }

    // ===== 成员：/领地 成员 添加 <玩家> [admin] | /领地 成员 移除 <玩家> =====
    private boolean member(Player p, String[] args) {
        ClaimManager mgr = plugin.getClaimManager();
        Claim c = mgr.getClaimAt(p.getLocation().getWorld().getUID(),
                p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ());
        if (c == null) { p.sendMessage(Messages.color("&c你不在任何领地内。")); return true; }
        if (!c.isOwner(p.getUniqueId()) && !c.isAdmin(p.getUniqueId())) { p.sendMessage(Messages.color("&c只有主人/管理员能管理成员。")); return true; }
        String op = args[1];
        String targetName = args[2];
        UUID target = Bukkit.getOfflinePlayer(targetName).getUniqueId();
        switch (op) {
            case "添加": case "add":
                Claim.Role role = args.length >= 4 && (args[3].equalsIgnoreCase("admin") || args[3].equals("管理员"))
                        ? Claim.Role.ADMIN : Claim.Role.MEMBER;
                c.addMember(target, role);
                plugin.getClaimStorage().saveClaim(c);
                p.sendMessage(Messages.color("&a已添加 &e" + targetName + " &a为" + (role == Claim.Role.ADMIN ? "管理员" : "成员") + "。"));
                Player targetOnline = Bukkit.getPlayerExact(targetName);
                if (targetOnline != null) {
                    targetOnline.sendMessage(Messages.color("&e" + p.getName() + " &a已把你添加为领地 &e" + c.getName()
                            + " &a的" + (role == Claim.Role.ADMIN ? "管理员" : "成员") + "。"));
                }
                return true;
            case "移除": case "remove":
                if (c.isOwner(target)) { p.sendMessage(Messages.color("&c不能移除主人。")); return true; }
                c.removeMember(target);
                plugin.getClaimStorage().saveClaim(c);
                p.sendMessage(Messages.color("&a已移除成员 &e" + targetName));
                return true;
            default:
                p.sendMessage(Messages.color("&c用法: /领地 成员 添加 <玩家> [admin] | /领地 成员 移除 <玩家>"));
                return true;
        }
    }


    // ===== flag =====
    private boolean flag(Player p, String[] args) {
        ClaimManager mgr = plugin.getClaimManager();
        Claim c = mgr.getClaimAt(p.getLocation().getWorld().getUID(),
                p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ());
        if (c == null) { p.sendMessage(Messages.color("&c你不在任何领地内。")); return true; }
        if (!c.isOwner(p.getUniqueId()) && !c.isAdmin(p.getUniqueId())) { p.sendMessage(Messages.color("&c只有主人/管理员能改 flag。")); return true; }
        if (args.length < 2) {
            // 列出全部 flag 及当前值
            p.sendMessage(Messages.color("&e===== Flag ====="));
            for (String name : Flags.PRI_FLAGS.keySet()) {
                p.sendMessage(Messages.color("&7" + Flags.displayName(name) + "(" + name + "): " + (resolve(c, name) ? "&a开" : "&c关")));
            }
            for (String name : Flags.ENV_FLAGS.keySet()) {
                p.sendMessage(Messages.color("&7" + Flags.displayName(name) + "(" + name + "): " + (resolve(c, name) ? "&a开" : "&c关")));
            }
            p.sendMessage(Messages.color("&7用 /领地 flag 设置 <名字> <true|false> 修改"));
            return true;
        }
        String op = args[1];
        if (op.equals("设置") || op.equals("set")) {
            if (args.length < 4) { p.sendMessage(Messages.color("&c用法: /领地 flag 设置 <名字> <true|false>")); return true; }
            String name = args[2];
            boolean value = args[3].equalsIgnoreCase("true") || args[3].equals("开");
            if (!Flags.isKnown(name)) { p.sendMessage(Messages.color("&c未知 flag: &e" + name)); return true; }
            c.setFlagOverride(name, value);
            plugin.getClaimStorage().saveClaim(c);
            plugin.getClaimManager().invalidateCache();
            p.sendMessage(Messages.color("&a已设置 flag &e" + name + " &a为 " + (value ? "&a开" : "&c关")));
            return true;
        }
        if (op.equals("查看") || op.equals("get")) {
            if (args.length < 3) { p.sendMessage(Messages.color("&c用法: /领地 flag 查看 <名字>")); return true; }
            String name = args[2];
            p.sendMessage(Messages.color("&7flag &e" + name + "&7: " + (resolve(c, name) ? "&a开" : "&c关")));
            return true;
        }
        p.sendMessage(Messages.color("&c用法: /领地 flag 设置|查看 <名字> [值]"));
        return true;
    }

    private boolean resolve(Claim c, String name) {
        Boolean overridden = c.getFlagOverride(name);
        if (overridden != null) return overridden;
        boolean def = Flags.ENV_FLAGS.containsKey(name)
                ? plugin.getConfig().getBoolean("claim.flags.default-environment." + name, Flags.ENV_FLAGS.get(name))
                : plugin.getConfig().getBoolean("claim.flags.default-privilege." + name, Flags.PRI_FLAGS.get(name));
        return def;
    }

    // ===== 传送：/领地 传送 <领地名>（无参=查脚下）=====
    private boolean tp(Player p, String name) {
        ClaimManager mgr = plugin.getClaimManager();
        Claim c = null;
        if (name != null && !name.isEmpty()) {
            c = findClaimByName(name);
            if (c == null) { p.sendMessage(Messages.color("&c找不到领地 &e" + name)); return true; }
        } else {
            c = mgr.getClaimAt(p.getLocation().getWorld().getUID(),
                    p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ());
            if (c == null) { p.sendMessage(Messages.color("&c你不在任何领地内。用法: /领地 传送 <领地名>")); return true; }
        }
        // 非本人/非成员领地，需有权限或管理员
        if (!c.isOwner(p.getUniqueId()) && !c.isMember(p.getUniqueId()) && !isAdmin(p)) {
            p.sendMessage(Messages.color("&c你无权传送到领地 &e" + c.getName()));
            return true;
        }
        org.bukkit.World w = p.getWorld();
        p.teleport(c.getCuboid().center(w));
        p.sendMessage(Messages.color("&a已传送到领地 &e" + c.getName() + " &a中心。"));
        return true;
    }

    /** 按名字找领地（全服唯一名匹配；重名取第一个） */
    private Claim findClaimByName(String name) {
        for (Claim c : plugin.getClaimManager().getAll()) {
            if (c.getName().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    // ===== 面板：/领地 面板（任意位置打开全局面板，管理名下所有领地）=====
    private boolean panel(Player p) {
        new nl.tinyaii.tinyplayer.claim.gui.GlobalPanelGui(plugin, p.getUniqueId()).open(p);
        return true;
    }

    // ===== 子领地：/领地 子 创建 <名>（在当前领地内用木锄头选区）=====
    private boolean child(Player p, String[] args) {
        ClaimManager mgr = plugin.getClaimManager();
        if (args.length < 2) { p.sendMessage(Messages.color("&c用法: /领地 子 创建|删除|列表")); return true; }
        String op = args[1];
        if (op.equals("列表") || op.equals("list")) {
            Claim parent = mgr.getClaimAt(p.getWorld().getUID(), p.getLocation().getBlockX(),
                    p.getLocation().getBlockY(), p.getLocation().getBlockZ());
            if (parent == null) { p.sendMessage(Messages.color("&c你不在任何领地内。")); return true; }
            p.sendMessage(Messages.color("&e===== 子领地 ====="));
            for (Claim c : mgr.getChildrenOf(parent.getId())) {
                p.sendMessage(Messages.color("&7- &e" + c.getName() + " &7(" + c.getCuboid() + ")"));
            }
            return true;
        }
        if (op.equals("删除") || op.equals("del")) {
            Claim c = mgr.getClaimAt(p.getWorld().getUID(), p.getLocation().getBlockX(),
                    p.getLocation().getBlockY(), p.getLocation().getBlockZ());
            if (c == null || !c.isChild()) { p.sendMessage(Messages.color("&c当前不在子领地内。")); return true; }
            if (!c.isOwner(p.getUniqueId()) && !isAdmin(p)) { p.sendMessage(Messages.color("&c只有主人/管理员能删除子领地。")); return true; }
            mgr.removeClaim(c);
            plugin.getClaimStorage().deleteClaim(c.getId());
            p.sendMessage(Messages.color("&a已删除子领地 &e" + c.getName()));
            return true;
        }
        if (op.equals("创建") || op.equals("create")) {
            if (args.length < 3) { p.sendMessage(Messages.color("&c用法: /领地 子 创建 <名字>")); return true; }
            Claim parent = mgr.getClaimAt(p.getWorld().getUID(), p.getLocation().getBlockX(),
                    p.getLocation().getBlockY(), p.getLocation().getBlockZ());
            if (parent == null) { p.sendMessage(Messages.color("&c你不在任何领地内，子领地必须建在领地内。")); return true; }
            if (!parent.isOwner(p.getUniqueId()) && !parent.isAdmin(p.getUniqueId()) && !isAdmin(p)) {
                p.sendMessage(Messages.color("&c只有父领地主人/管理员能创建子领地。")); return true;
            }
            nl.tinyaii.tinyplayer.claim.command.SelectionManager sel = plugin.getClaimSelectionManager();
            Location a = sel.getSel1(p.getUniqueId());
            Location b = sel.getSel2(p.getUniqueId());
            if (a == null || b == null) { p.sendMessage(Messages.color("&c请先用木锄头在父领地内选两点。")); return true; }
            Cuboid cub = Cuboid.fromLocations(a, b);
            // 子领地必须完全在父领地内
            if (!parent.getCuboid().contains(cub)) { p.sendMessage(Messages.color("&c子领地必须完全在父领地 " + parent.getName() + " 范围内。")); return true; }
            // 不能与父领地的其他子领地重叠
            for (Claim c : mgr.getChildrenOf(parent.getId())) {
                if (c.getCuboid().intersects(cub)) { p.sendMessage(Messages.color("&c与子领地 &e" + c.getName() + " &c重叠。")); return true; }
            }
            Claim childClaim = new Claim(args[2], p.getUniqueId(), cub);
            childClaim.setParentId(parent.getId());
            childClaim.setId(mgr.allocateId());
            mgr.addClaim(childClaim);
            plugin.getClaimStorage().saveClaim(childClaim);
            sel.clearSelection(p);
            p.sendMessage(Messages.color("&a子领地 &e" + args[2] + " &a创建成功（父领地: " + parent.getName() + "）。"));
            return true;
        }
        p.sendMessage(Messages.color("&c用法: /领地 子 创建|删除|列表"));
        return true;
    }

    // ===== 模板：/领地 模板 应用 <名>（把模板 flag 应用到当前领地）=====
    private boolean template(Player p, String[] args) {
        ClaimManager mgr = plugin.getClaimManager();
        if (args.length < 2) { p.sendMessage(Messages.color("&c用法: /领地 模板 应用 <名>")); return true; }
        String op = args[1];
        if (!op.equals("应用") && !op.equals("apply")) { p.sendMessage(Messages.color("&c用法: /领地 模板 应用 <名>")); return true; }
        if (args.length < 3) { p.sendMessage(Messages.color("&c用法: /领地 模板 应用 <名>")); return true; }
        Claim c = mgr.getClaimAt(p.getWorld().getUID(), p.getLocation().getBlockX(),
                p.getLocation().getBlockY(), p.getLocation().getBlockZ());
        if (c == null) { p.sendMessage(Messages.color("&c你不在任何领地内。")); return true; }
        if (!c.isOwner(p.getUniqueId()) && !c.isAdmin(p.getUniqueId())) { p.sendMessage(Messages.color("&c只有主人/管理员能应用模板。")); return true; }
        org.bukkit.configuration.ConfigurationSection tpl = plugin.getConfig().getConfigurationSection("claim.templates." + args[2]);
        if (tpl == null) {
            p.sendMessage(Messages.color("&c找不到模板 &e" + args[2] + "&c。可用: " + String.join(", ", plugin.getConfig().getConfigurationSection("claim.templates").getKeys(false))));
            return true;
        }
        int applied = 0;
        for (String key : tpl.getKeys(false)) {
            if (Flags.isKnown(key)) {
                c.setFlagOverride(key, tpl.getBoolean(key));
                applied++;
            }
        }
        plugin.getClaimStorage().saveClaim(c);
        plugin.getClaimManager().invalidateCache();
        p.sendMessage(Messages.color("&a已应用模板 &e" + args[2] + " &a（" + applied + " 个 flag）。"));
        return true;
    }

    // ===== 扩展：/领地 扩展 <方向> <格数> =====
    private boolean expand(Player p, String[] args) {
        ClaimManager mgr = plugin.getClaimManager();
        Claim c = mgr.getClaimAt(p.getWorld().getUID(), p.getLocation().getBlockX(),
                p.getLocation().getBlockY(), p.getLocation().getBlockZ());
        if (c == null) { p.sendMessage(Messages.color("&c你不在任何领地内。")); return true; }
        if (!c.isOwner(p.getUniqueId()) && !c.isAdmin(p.getUniqueId())) { p.sendMessage(Messages.color("&c只有主人/管理员能扩展领地。")); return true; }
        int n;
        try { n = Integer.parseInt(args[2]); } catch (NumberFormatException e) { p.sendMessage(Messages.color("&c格数必须是整数。")); return true; }
        if (n < 1 || n > 128) { p.sendMessage(Messages.color("&c格数范围 1-128。")); return true; }
        Cuboid old = c.getCuboid();
        int minX = old.getMinX(), maxX = old.getMaxX();
        int minY = old.getMinY(), maxY = old.getMaxY();
        int minZ = old.getMinZ(), maxZ = old.getMaxZ();
        switch (args[1]) {
            case "上": case "up": maxY = Math.min(255, maxY + n); break;
            case "下": case "down": minY = Math.max(0, minY - n); break;
            case "北": case "north": minZ -= n; break;
            case "南": case "south": maxZ += n; break;
            case "东": case "east": maxX += n; break;
            case "西": case "west": minX -= n; break;
            default: p.sendMessage(Messages.color("&c方向: 上/下/北/南/东/西 (up/down/north/south/east/west)")); return true;
        }
        Cuboid newCub = new Cuboid(old.getWorldUid(), minX, minY, minZ, maxX, maxY, maxZ);
        // 大小限制
        int maxXZ = plugin.getConfig().getInt("claim.settings.max-size-xz", 256);
        if (newCub.getMaxX() - newCub.getMinX() + 1 > maxXZ || newCub.getMaxZ() - newCub.getMinZ() + 1 > maxXZ) {
            p.sendMessage(Messages.color("&c扩展后超过大小上限（" + maxXZ + "）。")); return true;
        }
        // 重叠检测（子领地扩展时与兄弟子领地检查）
        if (c.isChild() && c.getParentId() != null) {
            for (Claim sib : mgr.getChildrenOf(c.getParentId())) {
                if (sib.getId() != c.getId() && sib.getCuboid().intersects(newCub)) {
                    p.sendMessage(Messages.color("&c与兄弟子领地 &e" + sib.getName() + " &c重叠。")); return true;
                }
            }
        } else if (plugin.getConfig().getBoolean("claim.settings.overlap-forbidden", true)) {
            for (Claim other : mgr.getAll()) {
                if (other.getId() != c.getId() && other.getParentId() == null && other.getCuboid().intersects(newCub)) {
                    p.sendMessage(Messages.color("&c与领地 &e" + other.getName() + " &c重叠。")); return true;
                }
            }
        }
        c.setCuboid(newCub);
        plugin.getClaimStorage().saveClaim(c);
        plugin.getClaimManager().invalidateCache();
        p.sendMessage(Messages.color("&a已扩展领地 &e" + c.getName() + " &a(方向 " + args[1] + " +" + n + ")。"));
        return true;
    }

    // ===== 退出语：/领地 退出语 <消息> =====
    private boolean leave(Player p, String[] args) {
        ClaimManager mgr = plugin.getClaimManager();
        Claim c = mgr.getClaimAt(p.getWorld().getUID(), p.getLocation().getBlockX(),
                p.getLocation().getBlockY(), p.getLocation().getBlockZ());
        if (c == null) { p.sendMessage(Messages.color("&c你不在任何领地内。")); return true; }
        if (!c.isOwner(p.getUniqueId()) && !c.isAdmin(p.getUniqueId())) { p.sendMessage(Messages.color("&c只有主人/管理员能设置退出语。")); return true; }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) { if (i > 1) sb.append(" "); sb.append(args[i]); }
        String msg = sb.toString().trim();
        if (msg.isEmpty() || msg.equals("-")) {
            c.setLeaveMsg(null);
            p.sendMessage(Messages.color("&a已清除领地 &e" + c.getName() + " &a的退出语。"));
        } else {
            c.setLeaveMsg(msg);
            p.sendMessage(Messages.color("&a已设置领地 &e" + c.getName() + " &a的退出语: &f" + msg));
        }
        plugin.getClaimStorage().saveClaim(c);
        return true;
    }

    // ===== 欢迎语：/领地 欢迎语 <消息>（当前领地主人/管理员设置，进入领地时 ActionBar 显示）=====
    private boolean welcome(Player p, String[] args) {
        ClaimManager mgr = plugin.getClaimManager();
        Claim c = mgr.getClaimAt(p.getLocation().getWorld().getUID(),
                p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ());
        if (c == null) { p.sendMessage(Messages.color("&c你不在任何领地内。")); return true; }
        if (!c.isOwner(p.getUniqueId()) && !c.isAdmin(p.getUniqueId())) { p.sendMessage(Messages.color("&c只有主人/管理员能设置欢迎语。")); return true; }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) { if (i > 1) sb.append(" "); sb.append(args[i]); }
        String msg = sb.toString().trim();
        if (msg.isEmpty() || msg.equals("-")) {
            c.setWelcomeMsg(null);
            p.sendMessage(Messages.color("&a已清除领地 &e" + c.getName() + " &a的欢迎语。"));
        } else {
            c.setWelcomeMsg(msg);
            p.sendMessage(Messages.color("&a已设置领地 &e" + c.getName() + " &a的欢迎语: &f" + msg));
        }
        plugin.getClaimStorage().saveClaim(c);
        return true;
    }

    // ===== 管理 =====
    private boolean admin(Player p, String[] args) {
        if (args.length < 3) { p.sendMessage(Messages.color("&c用法: /领地 管理 删除 <玩家>")); return true; }
        String op = args[1];
        if (op.equals("删除") || op.equals("delete")) {
            UUID target = Bukkit.getOfflinePlayer(args[2]).getUniqueId();
            List<Claim> claims = plugin.getClaimManager().getClaimsOf(target);
            if (claims.isEmpty()) { p.sendMessage(Messages.color("&c该玩家没有领地。")); return true; }
            for (Claim c : claims) {
                plugin.getClaimManager().removeClaim(c);
                plugin.getClaimStorage().deleteClaim(c.getId());
            }
            p.sendMessage(Messages.color("&a已删除 &e" + args[2] + " &a的全部 &e" + claims.size() + " &a个领地。"));
            return true;
        }
        p.sendMessage(Messages.color("&c用法: /领地 管理 删除 <玩家>"));
        return true;
    }

    private void help(Player p) {
        p.sendMessage(Messages.color("&e===== 领地帮助 ====="));
        p.sendMessage(Messages.color("&7木锄头右键两次 &e- &7选区（第一次起点/第二次终点）"));
        p.sendMessage(Messages.color("&7/领地 清除选区 &e- &7清除当前选区（重新选点）"));
        p.sendMessage(Messages.color("&7/领地 圈地 <名> <x1> <y1> <z1> <x2> <y2> <z2> &e- &7指令圈地（两个完整坐标，Y 为高低）"));
        p.sendMessage(Messages.color("&7/领地 半径 <名> <半径> &e- &7半径圈地（以玩家为中心）"));
        p.sendMessage(Messages.color("&7/领地 创建 <名> &e- &7创建领地"));
        p.sendMessage(Messages.color("&7/领地 删除 [确认] &e- &7删除当前领地"));
        p.sendMessage(Messages.color("&7/领地 信息 [领地名] &e- &7查看领地信息"));
        p.sendMessage(Messages.color("&7/领地 面板 &e- &7打开全局面板（管理所有领地）"));
        p.sendMessage(Messages.color("&7/领地 列表 &e- &7我的领地"));
        p.sendMessage(Messages.color("&7/领地 成员 添加|移除 <玩家> &e- &7管理成员"));
        p.sendMessage(Messages.color("&7/领地 flag 设置 <名> <开|关> &e- &7开关 flag"));
        p.sendMessage(Messages.color("&7/领地 传送 <领地名> &e- &7传送到指定领地中心"));
        p.sendMessage(Messages.color("&7/领地 管理 删除 <玩家> &e- &7(管理)删除他人领地"));
        p.sendMessage(Messages.color("&7/领地 欢迎语 <消息> &e- &7设置进入提示语"));
        p.sendMessage(Messages.color("&7/领地 退出语 <消息> &e- &7设置退出提示语"));
        p.sendMessage(Messages.color("&7/领地 子 创建|删除|列表 &e- &7子领地管理"));
        p.sendMessage(Messages.color("&7/领地 模板 应用 <名> &e- &7应用 flag 模板"));
        p.sendMessage(Messages.color("&7/领地 扩展 <方向> <格数> &e- &7扩展领地（上/下/北/南/东/西）"));
    }

    private String locStr(Location l) {
        return l.getWorld().getName() + " " + (int) l.getX() + "," + (int) l.getY() + "," + (int) l.getZ();
    }
}

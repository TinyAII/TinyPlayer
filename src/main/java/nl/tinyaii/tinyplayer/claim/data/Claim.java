package nl.tinyaii.tinyplayer.claim.data;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 领地数据模型。
 */
public class Claim {

    public enum Role { OWNER, ADMIN, MEMBER }

    private int id;
    private String name;
    private UUID owner;
    private Cuboid cuboid;
    private Integer parentId;          // 父领地 id（null=顶级）
    private long createdAt;
    private String welcomeMsg;         // 欢迎语（null=用默认）
    private String leaveMsg;           // 退出语（null=用默认）
    private final Map<UUID, Role> members = new HashMap<>();
    private final Map<String, Boolean> flagOverrides = new HashMap<>();

    public Claim(String name, UUID owner, Cuboid cuboid) {
        this.name = name;
        this.owner = owner;
        this.cuboid = cuboid;
        this.createdAt = System.currentTimeMillis();
        this.members.put(owner, Role.OWNER);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; members.put(owner, Role.OWNER); }

    public Cuboid getCuboid() { return cuboid; }
    public void setCuboid(Cuboid cuboid) { this.cuboid = cuboid; }

    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }
    public boolean isChild() { return parentId != null; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getWelcomeMsg() { return welcomeMsg; }
    public void setWelcomeMsg(String welcomeMsg) { this.welcomeMsg = welcomeMsg; }

    public String getLeaveMsg() { return leaveMsg; }
    public void setLeaveMsg(String leaveMsg) { this.leaveMsg = leaveMsg; }

    /** 是否包含坐标（本领地范围） */
    public boolean contains(UUID world, int x, int y, int z) {
        return cuboid.contain(world, x, y, z);
    }

    /** 是否在子领地内（子领地覆盖父领地的精确判断） */
    public boolean isInChildren(UUID world, int x, int y, int z, ClaimManager manager) {
        for (Claim c : manager.getChildrenOf(id)) {
            if (c.contains(world, x, y, z)) return true;
        }
        return false;
    }

    // ===== 成员 =====
    public void addMember(UUID uuid, Role role) { members.put(uuid, role); }
    public void removeMember(UUID uuid) { members.remove(uuid); }
    public Role getRole(UUID uuid) {
        Role r = members.get(uuid);
        return r == null ? Role.MEMBER : r;
    }
    public boolean isMember(UUID uuid) { return members.containsKey(uuid); }
    public boolean isOwner(UUID uuid) { return owner.equals(uuid); }
    public boolean isAdmin(UUID uuid) {
        Role r = members.get(uuid);
        return r == Role.OWNER || r == Role.ADMIN;
    }
    public Map<UUID, Role> getMembers() { return members; }

    // ===== Flag =====
    public Boolean getFlagOverride(String flagName) { return flagOverrides.get(flagName); }
    public void setFlagOverride(String flagName, boolean value) { flagOverrides.put(flagName, value); }
    public void clearFlagOverride(String flagName) { flagOverrides.remove(flagName); }
    public Map<String, Boolean> getFlagOverrides() { return flagOverrides; }
}

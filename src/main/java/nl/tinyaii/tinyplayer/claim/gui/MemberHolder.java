package nl.tinyaii.tinyplayer.claim.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * 成员管理 GUI 标识：携带领地 id（0=全局）+ 面板类型 + 角色选择 + 待确认玩家 + owner。
 */
public class MemberHolder implements InventoryHolder {

    public final int claimId;
    public final MemberGui.Type type;
    public final MemberGui.RoleSel role;
    public final UUID pending;
    public final UUID owner;

    public MemberHolder(int claimId, MemberGui.Type type, MemberGui.RoleSel role, UUID pending, UUID owner) {
        this.claimId = claimId;
        this.type = type;
        this.role = role;
        this.pending = pending;
        this.owner = owner;
    }

    @Override
    public Inventory getInventory() { return null; }
}

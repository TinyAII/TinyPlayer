package nl.tinyaii.tinyplayer.claim.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * 领地管理列表 GUI 标识。
 */
public class ClaimListHolder implements InventoryHolder {

    public final UUID owner;

    public ClaimListHolder(UUID owner) {
        this.owner = owner;
    }

    @Override
    public Inventory getInventory() { return null; }
}

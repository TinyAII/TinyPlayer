package nl.tinyaii.tinyplayer.claim.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 子领地列表 GUI 标识。
 */
public class SubClaimHolder implements InventoryHolder {

    public final int parentId;

    public SubClaimHolder(int parentId) {
        this.parentId = parentId;
    }

    @Override
    public Inventory getInventory() { return null; }
}

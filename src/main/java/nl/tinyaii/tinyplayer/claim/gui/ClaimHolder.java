package nl.tinyaii.tinyplayer.claim.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 领地面板 GUI 标识。
 */
public class ClaimHolder implements InventoryHolder {

    public final int claimId;

    public ClaimHolder(int claimId) {
        this.claimId = claimId;
    }

    @Override
    public Inventory getInventory() { return null; }
}

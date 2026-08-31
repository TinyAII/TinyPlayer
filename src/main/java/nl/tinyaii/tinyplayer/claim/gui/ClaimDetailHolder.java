package nl.tinyaii.tinyplayer.claim.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 领地详情页 GUI 标识。
 */
public class ClaimDetailHolder implements InventoryHolder {

    public final int claimId;

    public ClaimDetailHolder(int claimId) {
        this.claimId = claimId;
    }

    @Override
    public Inventory getInventory() { return null; }
}

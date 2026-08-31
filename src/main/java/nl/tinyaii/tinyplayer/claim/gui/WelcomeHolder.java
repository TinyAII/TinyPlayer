package nl.tinyaii.tinyplayer.claim.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 欢迎语设置 GUI 标识。
 */
public class WelcomeHolder implements InventoryHolder {

    public final int claimId;

    public WelcomeHolder(int claimId) {
        this.claimId = claimId;
    }

    @Override
    public Inventory getInventory() { return null; }
}

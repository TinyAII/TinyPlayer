package nl.tinyaii.tinyplayer.claim.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * 全局面板 GUI 标识。
 */
public class GlobalPanelHolder implements InventoryHolder {

    public final UUID owner;

    public GlobalPanelHolder(UUID owner) {
        this.owner = owner;
    }

    @Override
    public Inventory getInventory() { return null; }
}

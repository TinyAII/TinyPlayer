package nl.tinyaii.tinyplayer.home.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * 家列表 GUI 标识。
 */
public class HomeHolder implements InventoryHolder {

    public final UUID owner;

    public HomeHolder(UUID owner) {
        this.owner = owner;
    }

    @Override
    public Inventory getInventory() { return null; }
}
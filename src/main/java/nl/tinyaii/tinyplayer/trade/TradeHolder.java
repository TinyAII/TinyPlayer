package nl.tinyaii.tinyplayer.trade;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * 交易 GUI 标识：携带双方 UUID。
 */
public class TradeHolder implements InventoryHolder {

    public final UUID a;
    public final UUID b;

    public TradeHolder(UUID a, UUID b) {
        this.a = a;
        this.b = b;
    }

    public UUID other(UUID me) { return me.equals(a) ? b : a; }

    @Override
    public Inventory getInventory() { return null; }
}

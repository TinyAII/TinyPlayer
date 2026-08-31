package nl.tinyaii.tinyplayer.trade;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * 交易请求确认 GUI 标识：携带请求方与被请求方 UUID。
 */
public class TradeRequestHolder implements InventoryHolder {

    public final UUID requester;
    public final UUID target;

    public TradeRequestHolder(UUID requester, UUID target) {
        this.requester = requester;
        this.target = target;
    }

    @Override
    public Inventory getInventory() { return null; }
}
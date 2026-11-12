package io.github.townyadvanced.townymenus.listeners;

import io.github.townyadvanced.townymenus.TownyMenus;
import io.github.townyadvanced.townymenus.gui.MenuInventory;
import io.github.townyadvanced.townymenus.gui.MenuItem;
import io.github.townyadvanced.townymenus.gui.action.ClickAction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class InventoryListener implements Listener {
    private final TownyMenus plugin;

    public InventoryListener(TownyMenus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof MenuInventory menu))
            return;

        event.setCancelled(true);

        // Only clicks inside the menu itself should trigger actions, not clicks in the player's own inventory.
        // Raw slots below the menu size belong to the menu, anything else is the player's inventory or outside the window.
        final int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= menu.size())
            return;

        List<ClickAction> actions = menu.actions(rawSlot);
        if (actions != null)
            actions.forEach(action -> action.onClick(menu, event));
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof MenuInventory))
            return;

        for (ItemStack item : event.getView().getBottomInventory()) {
            if (item == null)
                continue;

            final ItemMeta meta = item.getItemMeta();
            if (meta == null)
                continue;

            if (meta.getPersistentDataContainer().has(MenuItem.PDC_KEY, PersistentDataType.BYTE))
                item.setAmount(0);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onItemDrop(final PlayerDropItemEvent event) {
        final ItemMeta meta = event.getItemDrop().getItemStack().getItemMeta();

        if (meta != null && meta.getPersistentDataContainer().has(MenuItem.PDC_KEY, PersistentDataType.BYTE))
            event.getItemDrop().remove();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof MenuInventory menu))
            return;

        event.setCancelled(true);
    }
}

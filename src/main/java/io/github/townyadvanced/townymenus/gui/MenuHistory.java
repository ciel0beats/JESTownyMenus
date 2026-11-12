package io.github.townyadvanced.townymenus.gui;

import io.github.townyadvanced.townymenus.utils.MenuScheduler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Used for tracking what menus a player has opened in their current 'session'.
 * <p>
 * History is read and modified from both async menu building tasks and the main thread,
 * so every access to a player's history list is synchronized on that list.
 */
public class MenuHistory {
    private static final Map<UUID, List<MenuInventory>> historyMap = new ConcurrentHashMap<>();

    /**
     * @return A snapshot copy of the player's history, or {@code null} if they have none.
     */
    @Nullable
    public static List<MenuInventory> getHistory(UUID uuid) {
        final List<MenuInventory> history = historyMap.get(uuid);
        if (history == null)
            return null;

        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    public static void clearHistory(UUID uuid) {
        historyMap.remove(uuid);
    }

    public static void addHistory(UUID uuid, MenuInventory menuInventory) {
        final List<MenuInventory> history = historyMap.computeIfAbsent(uuid, k -> new ArrayList<>());

        synchronized (history) {
            history.add(menuInventory);
        }
    }

    /**
     * Uses history to open the previous menu (if possible).
     * @param player The player to open the previous menu for.
     */
    public static void back(Player player) {
        final List<MenuInventory> history = historyMap.get(player.getUniqueId());
        if (history == null)
            return;

        final MenuInventory previous;
        synchronized (history) {
            // Player does not have enough history
            if (history.size() < 2)
                return;

            // Remove the current inventory from history
            history.removeLast();
            previous = history.getLast();
        }

        // Open the last inventory without adding to history.
        previous.openSilent(player);
    }

    /**
     * Re-opens the last menu for this player.
     * @param player The player to open the last menu for.
     */
    public static void last(Player player) {
        final List<MenuInventory> history = historyMap.get(player.getUniqueId());
        if (history == null)
            return;

        final MenuInventory last;
        synchronized (history) {
            if (history.isEmpty())
                return;

            last = history.getLast();
        }

        last.openSilent(player);
    }

    public static void pop(UUID uuid) {
        final List<MenuInventory> history = historyMap.get(uuid);
        if (history == null)
            return;

        synchronized (history) {
            if (!history.isEmpty())
                history.removeLast();
        }
    }

    public static void reOpen(Player player, Supplier<MenuInventory> supplier) {
        MenuScheduler.scheduleAsync(player.getUniqueId(), () -> {
            pop(player.getUniqueId());
            supplier.get().open(player);
        });
    }
}

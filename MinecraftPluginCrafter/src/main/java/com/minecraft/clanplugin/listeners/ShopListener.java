package com.minecraft.clanplugin.listeners;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.shop.ClanShop;
import com.minecraft.clanplugin.shop.ShopCategory;
import com.minecraft.clanplugin.shop.ShopItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for handling shop-related events.
 */
public class ShopListener implements Listener {
    
    private final ClanPlugin plugin;
    private final ClanShop clanShop;
    private final Map<UUID, String> playerShopStates;
    
    /**
     * Creates a new shop listener.
     * 
     * @param plugin The clan plugin instance
     * @param clanShop The clan shop instance
     */
    public ShopListener(ClanPlugin plugin, ClanShop clanShop) {
        this.plugin = plugin;
        this.clanShop = clanShop;
        this.playerShopStates = new HashMap<>();
    }
    
    /**
     * Handles inventory click events for the shop.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.contains("Clan Shop")) {
            return;
        }
        
        // Cancel all clicks in the shop inventory
        event.setCancelled(true);
        
        // Make sure it's a player
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerUuid = player.getUniqueId();
        ItemStack clickedItem = event.getCurrentItem();
        
        // Skip if null or air
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        // Main shop menu
        if (title.equals(ChatColor.DARK_PURPLE + "Clan Shop")) {
            handleMainMenuClick(player, clickedItem);
        } 
        // Category menu
        else if (title.startsWith(ChatColor.DARK_PURPLE + "Clan Shop: ")) {
            handleCategoryMenuClick(player, clickedItem);
        }
    }
    
    /**
     * Handles clicks in the main shop menu.
     * 
     * @param player The player who clicked
     * @param clickedItem The clicked item
     */
    private void handleMainMenuClick(Player player, ItemStack clickedItem) {
        if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
            String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
            
            for (ShopCategory category : clanShop.getCategories().values()) {
                if (displayName.equals(category.getName())) {
                    // Set player state
                    playerShopStates.put(player.getUniqueId(), category.getKey());
                    
                    // Open category menu
                    clanShop.openCategoryMenu(player, category.getKey());
                    return;
                }
            }
        }
    }
    
    /**
     * Handles clicks in category menus.
     * 
     * @param player The player who clicked
     * @param clickedItem The clicked item
     */
    private void handleCategoryMenuClick(Player player, ItemStack clickedItem) {
        // Back button
        if (clickedItem.getType() == Material.ARROW) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasDisplayName() && 
                ChatColor.stripColor(meta.getDisplayName()).equals("Back to Main Menu")) {
                clanShop.openMainMenu(player);
                return;
            }
        }
        
        // Check if clicking on a shop item
        if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
            String displayName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
            
            // Get current category
            String categoryKey = playerShopStates.get(player.getUniqueId());
            if (categoryKey != null) {
                ShopCategory category = clanShop.getCategories().get(categoryKey);
                if (category != null) {
                    for (ShopItem item : category.getItems()) {
                        if (displayName.equals(item.getName())) {
                            // Attempt to purchase
                            handleItemPurchase(player, item.getItemKey());
                            return;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Handles purchase of a shop item.
     * 
     * @param player The player making the purchase
     * @param itemKey The key of the item to purchase
     */
    private void handleItemPurchase(Player player, String itemKey) {
        boolean success = clanShop.purchaseItem(player, itemKey);
        
        if (success) {
            // Refresh the menu to update affordability status
            String categoryKey = playerShopStates.get(player.getUniqueId());
            if (categoryKey != null) {
                clanShop.openCategoryMenu(player, categoryKey);
            }
        }
    }
    
    /**
     * Handles inventory close events for the shop.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (title.contains("Clan Shop") && event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            playerShopStates.remove(player.getUniqueId());
        }
    }
}
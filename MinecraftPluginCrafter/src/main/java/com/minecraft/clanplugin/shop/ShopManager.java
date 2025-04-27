package com.minecraft.clanplugin.shop;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages shop configuration and dynamic pricing features.
 */
public class ShopManager {
    private final ClanPlugin plugin;
    private final File shopConfigFile;
    private FileConfiguration shopConfig;
    private final Map<String, Double> customPrices = new HashMap<>();
    private String currencySymbol = "$";
    
    /**
     * Creates a new shop manager.
     * 
     * @param plugin The clan plugin instance
     */
    public ShopManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.shopConfigFile = new File(plugin.getDataFolder(), "shop.yml");
        saveDefaultShopConfig();
        loadShopConfig();
    }
    
    /**
     * Saves the default shop configuration if it doesn't exist.
     */
    private void saveDefaultShopConfig() {
        if (!shopConfigFile.exists()) {
            plugin.saveResource("shop.yml", false);
        }
    }
    
    /**
     * Loads the shop configuration from file.
     */
    public void loadShopConfig() {
        shopConfig = YamlConfiguration.loadConfiguration(shopConfigFile);
        
        // Look for defaults in the jar
        InputStream defaultConfigStream = plugin.getResource("shop.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));
            shopConfig.setDefaults(defaultConfig);
        }
        
        // Load currency symbol
        currencySymbol = shopConfig.getString("settings.currency_symbol", "$");
        
        // Load any custom prices
        ConfigurationSection customPricesSection = shopConfig.getConfigurationSection("custom_prices");
        if (customPricesSection != null) {
            for (String itemKey : customPricesSection.getKeys(false)) {
                customPrices.put(itemKey, customPricesSection.getDouble(itemKey));
            }
        }
    }
    
    /**
     * Saves the shop configuration to file.
     */
    public void saveShopConfig() {
        try {
            // Save any custom prices
            for (Map.Entry<String, Double> entry : customPrices.entrySet()) {
                shopConfig.set("custom_prices." + entry.getKey(), entry.getValue());
            }
            
            shopConfig.save(shopConfigFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save shop config to " + shopConfigFile, e);
        }
    }
    
    /**
     * Gets the shop configuration file.
     * 
     * @return The shop configuration
     */
    public FileConfiguration getShopConfig() {
        if (shopConfig == null) {
            loadShopConfig();
        }
        return shopConfig;
    }
    
    /**
     * Gets the base price for an item.
     * 
     * @param itemKey The item key
     * @return The base price from configuration
     */
    public double getBasePrice(String itemKey) {
        return shopConfig.getDouble("items." + itemKey + ".price", 100.0);
    }
    
    /**
     * Gets the actual price for an item after applying any custom prices and discounts.
     * 
     * @param player The player viewing the price
     * @param itemKey The item key
     * @return The final price
     */
    public double getPrice(Player player, String itemKey) {
        // Start with base or custom price
        double price = customPrices.getOrDefault(itemKey, getBasePrice(itemKey));
        
        // Apply discounts if enabled
        if (shopConfig.getBoolean("discounts.level_discount.enabled", true)) {
            Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
            if (clan != null) {
                // Level-based discount
                double discountPerLevel = shopConfig.getDouble("discounts.level_discount.discount_per_level", 1.0);
                double maxDiscount = shopConfig.getDouble("discounts.level_discount.max_discount", 10.0);
                
                int clanLevel = clan.getLevel();
                double levelDiscount = Math.min(clanLevel * discountPerLevel, maxDiscount);
                
                // Apply discount percentage
                price = price * (1 - (levelDiscount / 100.0));
            }
        }
        
        // Apply reputation-based discounts if enabled
        if (shopConfig.getBoolean("discounts.reputation_discount.enabled", true)) {
            Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
            if (clan != null) {
                // Get clan reputation (from ReputationManager)
                double reputation = 0;
                if (plugin.getReputationManager() != null) {
                    reputation = plugin.getReputationManager().getClanReputation(clan.getName());
                }
                double discountPer100Rep = shopConfig.getDouble("discounts.reputation_discount.discount_per_100_rep", 2.0);
                double maxDiscount = shopConfig.getDouble("discounts.reputation_discount.max_discount", 15.0);
                
                double repDiscount = Math.min((reputation / 100.0) * discountPer100Rep, maxDiscount);
                
                // Apply discount percentage
                price = price * (1 - (repDiscount / 100.0));
            }
        }
        
        // Apply event discounts if enabled
        if (shopConfig.getBoolean("discounts.events.weekend.enabled", false)) {
            double discount = shopConfig.getDouble("discounts.events.weekend.discount", 10.0);
            price = price * (1 - (discount / 100.0));
        }
        
        if (shopConfig.getBoolean("discounts.events.holiday.enabled", false)) {
            double discount = shopConfig.getDouble("discounts.events.holiday.discount", 20.0);
            price = price * (1 - (discount / 100.0));
        }
        
        // Round to 2 decimal places
        return Math.round(price * 100.0) / 100.0;
    }
    
    /**
     * Sets a custom price for an item.
     * 
     * @param itemKey The item key
     * @param price The new price
     * @return True if the price was set successfully
     */
    public boolean setCustomPrice(String itemKey, double price) {
        if (price < 0) {
            return false;
        }
        
        if (shopConfig.contains("items." + itemKey)) {
            customPrices.put(itemKey, price);
            saveShopConfig();
            return true;
        }
        
        return false;
    }
    
    /**
     * Clears a custom price for an item, reverting to the base price.
     * 
     * @param itemKey The item key
     * @return True if the custom price was removed
     */
    public boolean clearCustomPrice(String itemKey) {
        if (customPrices.containsKey(itemKey)) {
            customPrices.remove(itemKey);
            shopConfig.set("custom_prices." + itemKey, null);
            saveShopConfig();
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets the currency symbol to use in price displays.
     * 
     * @return The currency symbol
     */
    public String getCurrencySymbol() {
        return currencySymbol;
    }
    
    /**
     * Formats a price with the currency symbol.
     * 
     * @param price The price to format
     * @return The formatted price string
     */
    public String formatPrice(double price) {
        return ChatColor.GOLD + currencySymbol + String.format("%.2f", price);
    }
    
    /**
     * Enables or disables a special event discount.
     * 
     * @param event The event name (weekend, holiday)
     * @param enabled Whether the discount should be enabled
     * @return True if the event was updated successfully
     */
    public boolean setEventDiscount(String event, boolean enabled) {
        if (shopConfig.contains("discounts.events." + event)) {
            shopConfig.set("discounts.events." + event + ".enabled", enabled);
            saveShopConfig();
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if a player has permission to purchase an item.
     * 
     * @param player The player to check
     * @param itemKey The item key
     * @return True if the player has permission
     */
    public boolean hasPermission(Player player, String itemKey) {
        String permission = shopConfig.getString("items." + itemKey + ".permission", null);
        return permission == null || player.hasPermission(permission);
    }
    
    /**
     * Gets a material from the configuration.
     * 
     * @param path The configuration path
     * @param defaultMaterial The default material if not found
     * @return The material
     */
    public Material getMaterial(String path, Material defaultMaterial) {
        String materialName = shopConfig.getString("items." + path);
        if (materialName != null) {
            try {
                return Material.valueOf(materialName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in shop config: " + materialName + " at path: items." + path);
            }
        }
        return defaultMaterial;
    }
}
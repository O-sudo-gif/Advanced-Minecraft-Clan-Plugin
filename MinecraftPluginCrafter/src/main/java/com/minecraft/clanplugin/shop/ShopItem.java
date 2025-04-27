package com.minecraft.clanplugin.shop;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an item in the clan shop.
 */
public class ShopItem {
    private final String itemKey;
    private final String name;
    private final String description;
    private final double price;
    private final Material material;
    private final int quantity;
    private final int minClanLevel;
    private final boolean clanPurchase;
    private final boolean unique;
    private final Map<String, Object> properties;
    
    /**
     * Creates a new shop item.
     * 
     * @param itemKey The unique key for this item
     * @param name The display name of the item
     * @param description A short description of the item
     * @param price The price of the item
     * @param material The material for this item
     * @param quantity The quantity of the item
     * @param minClanLevel The minimum clan level required to purchase
     * @param clanPurchase Whether this is purchased with clan funds
     * @param unique Whether this is a unique purchase that can only be made once
     */
    public ShopItem(String itemKey, String name, String description, double price, Material material, int quantity, int minClanLevel, boolean clanPurchase, boolean unique) {
        this.itemKey = itemKey;
        this.name = name;
        this.description = description;
        this.price = price;
        this.material = material;
        this.quantity = quantity;
        this.minClanLevel = minClanLevel;
        this.clanPurchase = clanPurchase;
        this.unique = unique;
        this.properties = new HashMap<>();
    }
    
    /**
     * Gets the unique key for this item.
     * 
     * @return The item key
     */
    public String getItemKey() {
        return itemKey;
    }
    
    /**
     * Gets the display name of the item.
     * 
     * @return The item name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the description of the item.
     * 
     * @return The item description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the price of the item.
     * 
     * @return The item price
     */
    public double getPrice() {
        return price;
    }
    
    /**
     * Gets the material for this item.
     * 
     * @return The item material
     */
    public Material getMaterial() {
        return material;
    }
    
    /**
     * Gets the quantity of the item.
     * 
     * @return The item quantity
     */
    public int getQuantity() {
        return quantity;
    }
    
    /**
     * Gets the minimum clan level required to purchase this item.
     * 
     * @return The minimum clan level
     */
    public int getMinClanLevel() {
        return minClanLevel;
    }
    
    /**
     * Checks if this item is purchased with clan funds.
     * 
     * @return True if purchased with clan funds
     */
    public boolean isClanPurchase() {
        return clanPurchase;
    }
    
    /**
     * Checks if this is a unique purchase that can only be made once.
     * 
     * @return True if unique
     */
    public boolean isUnique() {
        return unique;
    }
    
    /**
     * Gets a specific property of this item.
     * 
     * @param key The property key
     * @return The property value, or null if not found
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    /**
     * Gets a specific property of this item with a default value.
     * 
     * @param key The property key
     * @param defaultValue The default value to return if not found
     * @return The property value, or the default value if not found
     */
    public Object getProperty(String key, Object defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }
    
    /**
     * Sets a property for this item.
     * 
     * @param key The property key
     * @param value The property value
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    /**
     * Gets all properties for this item.
     * 
     * @return Map of all properties
     */
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }
    
    /**
     * Checks if this item has limited stock.
     * 
     * @return True if this item has stock limits
     */
    public boolean hasLimitedStock() {
        Object stockObj = getProperty("limited_stock");
        return stockObj != null && stockObj instanceof Boolean && (Boolean) stockObj;
    }
    
    /**
     * Gets the current stock of this item.
     * 
     * @return The current stock amount
     */
    public int getStock() {
        return (int) getProperty("stock", 0);
    }
    
    /**
     * Checks if this item has a custom price.
     * 
     * @return True if this item has a custom price different from default
     */
    public boolean hasCustomPrice() {
        return getProperty("custom_price") != null;
    }
    
    /**
     * Gets the time of the last restock for this item.
     * 
     * @return The timestamp of the last restock
     */
    public long getLastRestockTime() {
        Object time = getProperty("last_restock", 0L);
        if (time instanceof Integer) {
            return ((Integer) time).longValue();
        }
        return (long) getProperty("last_restock", 0L);
    }
}
package com.minecraft.clanplugin.shop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a category in the clan shop.
 */
public class ShopCategory {
    private final String key;
    private final String name;
    private final String description;
    private final Material icon;
    private final List<ShopItem> items;
    
    /**
     * Creates a new shop category.
     * 
     * @param key The unique key for this category
     * @param name The display name of the category
     * @param description A short description of the category
     * @param icon The material to use as an icon for this category
     */
    public ShopCategory(String key, String name, String description, Material icon) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.items = new ArrayList<>();
    }
    
    /**
     * Gets the unique key for this category.
     * 
     * @return The category key
     */
    public String getKey() {
        return key;
    }
    
    /**
     * Gets the unique key for this category.
     * Alternative method name for compatibility.
     * 
     * @return The category key
     */
    public String getCategoryKey() {
        return key;
    }
    
    /**
     * Gets the display name of the category.
     * 
     * @return The category name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the description of the category.
     * 
     * @return The category description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the icon material for this category.
     * 
     * @return The icon material
     */
    public Material getIcon() {
        return icon;
    }
    
    /**
     * Gets the list of items in this category.
     * 
     * @return The list of items
     */
    public List<ShopItem> getItems() {
        return items;
    }
    
    /**
     * Adds an item to this category.
     * 
     * @param item The item to add
     */
    public void addItem(ShopItem item) {
        items.add(item);
    }
    
    /**
     * Removes an item from this category.
     * 
     * @param item The item to remove
     * @return True if the item was removed
     */
    public boolean removeItem(ShopItem item) {
        return items.remove(item);
    }
}
package com.minecraft.clanplugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for creating custom items and GUI menus.
 */
public class ItemUtils {
    
    // Special lore string to identify clan items
    private static final String CLAN_IDENTIFIER_PREFIX = ChatColor.DARK_GRAY + "Clan: ";
    
    /**
     * Creates a custom clan flag item.
     * 
     * @param clanName The name of the clan
     * @return The custom clan flag item
     */
    public static ItemStack createClanFlag(String clanName) {
        // Use Material.valueOf to handle different versions of Minecraft (pre-1.13 uses BANNER, 1.13+ uses different banner types)
        Material bannerMaterial;
        try {
            bannerMaterial = Material.valueOf("WHITE_BANNER");
        } catch (IllegalArgumentException e) {
            try {
                bannerMaterial = Material.valueOf("BANNER");
            } catch (IllegalArgumentException ex) {
                // Fallback to a common material if neither exists
                bannerMaterial = Material.PAPER;
            }
        }
        ItemStack flag = new ItemStack(bannerMaterial);
        ItemMeta meta = flag.getItemMeta();
        
        // Set display name
        meta.setDisplayName(ChatColor.GOLD + clanName + " Clan Flag");
        
        // Set lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Official flag of the " + ChatColor.GOLD + clanName + ChatColor.GRAY + " clan");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Place this flag to mark your clan territory");
        meta.setLore(lore);
        
        // Add item flags
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        // Make it unbreakable
        meta.setUnbreakable(true);
        
        flag.setItemMeta(meta);
        return flag;
    }
    
    /**
     * Creates a GUI item with the specified material, name, and lore.
     * 
     * @param material The material for the item
     * @param name The display name for the item
     * @param lore The lore (description) for the item
     * @return The created ItemStack
     */
    public static ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        
        // Set the display name
        meta.setDisplayName(name);
        
        // Set the lore if provided
        if (lore != null) {
            meta.setLore(lore);
        }
        
        // Hide attributes to make the item look cleaner
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Creates a player head item for the specified player.
     * 
     * @param player The player whose head to create
     * @param name The display name for the head
     * @param lore The lore (description) for the head
     * @return The created player head ItemStack
     */
    public static ItemStack createPlayerHead(Player player, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        // Set the owner of the skull
        meta.setOwningPlayer(player);
        
        // Set the display name
        meta.setDisplayName(name);
        
        // Set the lore if provided
        if (lore != null) {
            meta.setLore(lore);
        }
        
        head.setItemMeta(meta);
        return head;
    }
    
    /**
     * Places items in a GUI inventory with empty glass panes around the border.
     * 
     * @param inventory The inventory to decorate
     * @param borderColor The glass pane color for the border (null for no border)
     */
    public static void createGuiBorder(Inventory inventory, Material borderColor) {
        if (borderColor == null) {
            borderColor = Material.BLACK_STAINED_GLASS_PANE;
        }
        
        int size = inventory.getSize();
        int rows = size / 9;
        
        // Create the border item
        ItemStack border = createGuiItem(borderColor, " ", null);
        
        // Place border items around the edge
        for (int i = 0; i < size; i++) {
            // Top row, bottom row, or left/right edge
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, border);
            }
        }
    }
    
    /**
     * Creates an information item for the clan GUI.
     * 
     * @param material The material to use
     * @param title The title of the information
     * @param infoText The information text lines
     * @return The created information ItemStack
     */
    public static ItemStack createInfoItem(Material material, String title, List<String> infoText) {
        List<String> lore = new ArrayList<>();
        
        // Add spacing line
        lore.add("");
        
        // Add each info line with formatting
        for (String line : infoText) {
            lore.add(ChatColor.GRAY + line);
        }
        
        return createGuiItem(material, title, lore);
    }
    
    /**
     * Adds a clan identifier to an item's lore.
     * This is used to mark items as belonging to a specific clan.
     * 
     * @param meta The item meta to modify
     * @param clanName The name of the clan to associate with this item
     */
    public static void addClanIdentifier(ItemMeta meta, String clanName) {
        if (meta == null) return;
        
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        
        // Remove any existing clan identifier
        if (lore != null) {
            lore.removeIf(line -> line.startsWith(CLAN_IDENTIFIER_PREFIX));
            
            // Add the new identifier at the end
            lore.add(CLAN_IDENTIFIER_PREFIX + clanName);
            meta.setLore(lore);
        }
    }
    
    /**
     * Checks if an item has a clan identifier in its lore.
     * 
     * @param meta The item meta to check
     * @return True if the item has a clan identifier
     */
    public static boolean hasClanIdentifier(ItemMeta meta) {
        if (meta == null || !meta.hasLore()) return false;
        
        List<String> lore = meta.getLore();
        if (lore == null) return false;
        
        // Check for any line starting with the clan identifier prefix
        for (String line : lore) {
            if (line.startsWith(CLAN_IDENTIFIER_PREFIX)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets the clan name from an item's clan identifier.
     * 
     * @param meta The item meta to check
     * @return The clan name, or null if no clan identifier is found
     */
    public static String getClanNameFromItem(ItemMeta meta) {
        if (meta == null || !meta.hasLore()) return null;
        
        List<String> lore = meta.getLore();
        if (lore == null) return null;
        
        // Find the clan identifier line and extract the clan name
        for (String line : lore) {
            if (line.startsWith(CLAN_IDENTIFIER_PREFIX)) {
                return line.substring(CLAN_IDENTIFIER_PREFIX.length());
            }
        }
        
        return null;
    }
}

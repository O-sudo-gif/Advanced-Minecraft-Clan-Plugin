package com.minecraft.clanplugin.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.DyeColor;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import com.minecraft.clanplugin.models.Clan;

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
    
    /**
     * Applies clan color to a banner item.
     * 
     * @param banner The banner item to color
     * @param clan The clan whose color to apply
     * @return True if coloring was successful
     */
    public static boolean applyColorToBanner(ItemStack banner, Clan clan) {
        if (banner == null || clan == null) {
            return false;
        }
        
        // Check if item is a banner or colored block
        if (!banner.getType().name().contains("WOOL") && 
            !banner.getType().name().contains("BANNER")) {
            return false;
        }
        
        // Get clan chat color
        ChatColor chatColor = clan.getChatColor();
        
        // Convert ChatColor to DyeColor
        DyeColor dyeColor = convertChatColorToDyeColor(chatColor);
        
        if (banner.getType().name().contains("BANNER")) {
            try {
                // Try to apply banner pattern based on Minecraft version
                BannerMeta meta = (BannerMeta) banner.getItemMeta();
                
                // Set base color
                try {
                    // Method for newer Minecraft versions (1.13+)
                    meta.getClass().getMethod("setBaseColor", DyeColor.class).invoke(meta, dyeColor);
                } catch (Exception e) {
                    // Method for older Minecraft versions
                    meta.getClass().getMethod("setBaseColor", DyeColor.class).invoke(meta, dyeColor);
                }
                
                // Apply unique patterns based on clan name
                applyUniqueClanPatterns(meta, clan);
                
                // Set display name
                if (!meta.hasDisplayName()) {
                    meta.setDisplayName(ChatColor.GOLD + clan.getName() + " Clan Banner");
                }
                
                // Add clan identifier
                addClanIdentifier(meta, clan.getName());
                
                banner.setItemMeta(meta);
                return true;
            } catch (Exception e) {
                // Failed to apply banner pattern
                return false;
            }
        } else {
            // Handle wool items
            try {
                banner.setDurability(dyeColor.getWoolData());
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
    
    /**
     * Converts a ChatColor to the nearest DyeColor.
     * 
     * @param chatColor The ChatColor to convert
     * @return The nearest DyeColor
     */
    private static DyeColor convertChatColorToDyeColor(ChatColor chatColor) {
        switch (chatColor) {
            case BLACK: return DyeColor.BLACK;
            case DARK_BLUE: return DyeColor.BLUE;
            case DARK_GREEN: return DyeColor.GREEN;
            case DARK_AQUA: return DyeColor.CYAN;
            case DARK_RED: return DyeColor.RED;
            case DARK_PURPLE: return DyeColor.PURPLE;
            case GOLD: return DyeColor.ORANGE;
            case GRAY: return DyeColor.GRAY;
            case DARK_GRAY: return DyeColor.GRAY;
            case BLUE: return DyeColor.LIGHT_BLUE;
            case GREEN: return DyeColor.LIME;
            case AQUA: return DyeColor.LIGHT_BLUE;
            case RED: return DyeColor.RED;
            case LIGHT_PURPLE: return DyeColor.MAGENTA;
            case YELLOW: return DyeColor.YELLOW;
            case WHITE: return DyeColor.WHITE;
            default: return DyeColor.WHITE;
        }
    }
    
    /**
     * Applies unique patterns to a clan banner based on the clan's properties.
     * 
     * @param meta The BannerMeta to apply patterns to
     * @param clan The clan to create patterns for
     */
    private static void applyUniqueClanPatterns(BannerMeta meta, Clan clan) {
        if (meta == null || clan == null) return;
        
        // Get clan name hash to generate unique patterns
        String clanName = clan.getName();
        int nameHash = clanName.hashCode();
        
        // Clear existing patterns
        try {
            meta.getPatterns().clear();
        } catch (Exception e) {
            // Ignore - may not be supported in some versions
        }
        
        // Get clan colors
        DyeColor baseColor = convertChatColorToDyeColor(clan.getChatColor());
        DyeColor patternColor = getContrastingColor(baseColor);
        
        // Add patterns based on clan properties
        try {
            // Pattern 1: Determined by first character in clan name
            char firstChar = clanName.charAt(0);
            PatternType firstPattern = getPatternByChar(firstChar);
            meta.addPattern(new Pattern(patternColor, firstPattern));
            
            // Pattern 2: Determined by clan creation time
            long creationTime = clan.getCreationTime();
            PatternType secondPattern = getPatternByCreationTime(creationTime);
            meta.addPattern(new Pattern(baseColor, secondPattern));
            
            // Pattern 3: Determined by clan level
            int level = clan.getLevel();
            PatternType levelPattern = getPatternByLevel(level);
            meta.addPattern(new Pattern(patternColor, levelPattern));
            
            // Add a border based on allies/enemies
            if (clan.getAlliances().size() > clan.getEnemies().size()) {
                // Peaceful clan - add friendly border
                meta.addPattern(new Pattern(DyeColor.WHITE, PatternType.BORDER));
            } else if (clan.getEnemies().size() > 0) {
                // Aggressive clan - add spiky border
                meta.addPattern(new Pattern(DyeColor.BLACK, PatternType.TRIANGLES_BOTTOM));
            }
            
            // If clan is high level, add a special pattern
            if (clan.getLevel() >= 5) {
                meta.addPattern(new Pattern(DyeColor.YELLOW, PatternType.GRADIENT_UP));
            }
            
            // Check if this is a special flag by examining item name or lore
            if (meta.hasDisplayName()) {
                String name = ChatColor.stripColor(meta.getDisplayName()).toLowerCase();
                if (name.contains("custom") || name.contains("special") || name.contains("premium")) {
                    // Add extra fancy patterns for premium flags
                    meta.addPattern(new Pattern(patternColor, PatternType.GLOBE));
                    meta.addPattern(new Pattern(DyeColor.YELLOW, PatternType.GRADIENT));
                    meta.addPattern(new Pattern(baseColor, PatternType.FLOWER));
                }
            }
        } catch (Exception e) {
            // Fallback to simple patterns if anything goes wrong
            try {
                // Simple pattern that should work in all versions
                meta.addPattern(new Pattern(patternColor, PatternType.CROSS));
                meta.addPattern(new Pattern(baseColor, PatternType.BORDER));
            } catch (Exception ex) {
                // Do nothing if even fallback fails
            }
        }
    }
    
    /**
     * Gets a contrasting color for a given base color.
     * 
     * @param baseColor The base color
     * @return A contrasting color
     */
    private static DyeColor getContrastingColor(DyeColor baseColor) {
        switch (baseColor) {
            case BLACK: return DyeColor.WHITE;
            case GRAY: return DyeColor.RED;
            case BLUE: return DyeColor.YELLOW;
            case GREEN: return DyeColor.MAGENTA;
            case CYAN: return DyeColor.ORANGE;
            case RED: return DyeColor.CYAN;
            case PURPLE: return DyeColor.LIME;
            case ORANGE: return DyeColor.BLUE;
            case LIGHT_BLUE: return DyeColor.RED;
            case LIME: return DyeColor.PURPLE;
            case MAGENTA: return DyeColor.GREEN;
            case YELLOW: return DyeColor.BLUE;
            case WHITE: return DyeColor.BLACK;
            default: return DyeColor.BLACK;
        }
    }
    
    /**
     * Gets a banner pattern based on a character.
     * 
     * @param c The character to convert
     * @return A pattern type
     */
    private static PatternType getPatternByChar(char c) {
        // Use character code modulo pattern count to get a consistent pattern
        PatternType[] patterns = PatternType.values();
        int index = Math.abs(c) % patterns.length;
        return patterns[index];
    }
    
    /**
     * Gets a banner pattern based on clan creation time.
     * 
     * @param creationTime The clan creation time
     * @return A pattern type
     */
    private static PatternType getPatternByCreationTime(long creationTime) {
        PatternType[] patterns = PatternType.values();
        int index = (int)(creationTime % patterns.length);
        return patterns[index];
    }
    
    /**
     * Gets a banner pattern based on clan level.
     * 
     * @param level The clan level
     * @return A pattern type
     */
    private static PatternType getPatternByLevel(int level) {
        PatternType[] patterns = {
            PatternType.STRIPE_BOTTOM,   // Level 1
            PatternType.STRIPE_TOP,      // Level 2
            PatternType.RHOMBUS_MIDDLE,  // Level 3
            PatternType.CIRCLE_MIDDLE,   // Level 4
            PatternType.FLOWER,          // Level 5
            PatternType.GRADIENT,        // Level 6
            PatternType.SKULL,           // Level 7
            PatternType.GRADIENT_UP,     // Level 8
            PatternType.CREEPER,         // Level 9
            PatternType.BRICKS           // Level 10+
        };
        
        int index = Math.min(level - 1, patterns.length - 1);
        if (index < 0) index = 0;
        return patterns[index];
    }
}

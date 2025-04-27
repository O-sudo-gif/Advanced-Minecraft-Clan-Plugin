package com.minecraft.clanplugin.examples;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example class showing how to create and customize clan banners.
 * These are decorative items representing clan identity.
 */
public class ClanBannerExample {
    
    private final ClanPlugin plugin;
    private final Map<ChatColor, DyeColor> colorMap;
    
    /**
     * Creates a new clan banner example.
     * 
     * @param plugin The clan plugin instance
     */
    public ClanBannerExample(ClanPlugin plugin) {
        this.plugin = plugin;
        this.colorMap = initColorMap();
    }
    
    /**
     * Initializes the mapping between ChatColor and DyeColor for banners.
     * 
     * @return A map of ChatColor to DyeColor
     */
    private Map<ChatColor, DyeColor> initColorMap() {
        Map<ChatColor, DyeColor> map = new HashMap<>();
        map.put(ChatColor.BLACK, DyeColor.BLACK);
        map.put(ChatColor.DARK_BLUE, DyeColor.BLUE);
        map.put(ChatColor.DARK_GREEN, DyeColor.GREEN);
        map.put(ChatColor.DARK_AQUA, DyeColor.CYAN);
        map.put(ChatColor.DARK_RED, DyeColor.RED);
        map.put(ChatColor.DARK_PURPLE, DyeColor.PURPLE);
        map.put(ChatColor.GOLD, DyeColor.ORANGE);
        map.put(ChatColor.GRAY, DyeColor.LIGHT_GRAY);
        map.put(ChatColor.DARK_GRAY, DyeColor.GRAY);
        map.put(ChatColor.BLUE, DyeColor.LIGHT_BLUE);
        map.put(ChatColor.GREEN, DyeColor.LIME);
        map.put(ChatColor.AQUA, DyeColor.LIGHT_BLUE);
        map.put(ChatColor.RED, DyeColor.RED);
        map.put(ChatColor.LIGHT_PURPLE, DyeColor.PINK);
        map.put(ChatColor.YELLOW, DyeColor.YELLOW);
        map.put(ChatColor.WHITE, DyeColor.WHITE);
        return map;
    }
    
    /**
     * Convert a ChatColor to a DyeColor for banners.
     * 
     * @param chatColor The ChatColor to convert
     * @return The corresponding DyeColor
     */
    private DyeColor chatColorToDyeColor(ChatColor chatColor) {
        return colorMap.getOrDefault(chatColor, DyeColor.WHITE);
    }
    
    /**
     * Creates a basic clan banner with the clan's color and tag.
     * 
     * @param clan The clan to create a banner for
     * @return An ItemStack containing the basic clan banner
     */
    public ItemStack createBasicClanBanner(Clan clan) {
        if (clan == null) {
            return null;
        }
        
        // Get clan color
        ChatColor clanColor = clan.getChatColor();
        DyeColor baseColor = chatColorToDyeColor(clanColor);
        
        // Create banner with the correct base color directly (compatible with newer API)
        ItemStack banner = new ItemStack(Material.valueOf(baseColor.toString() + "_BANNER"));
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        
        // Add clan tag as lore
        List<String> lore = new ArrayList<>();
        lore.add(clanColor + "Clan: " + clan.getName());
        lore.add(clanColor + "Tag: " + clan.getTag());
        meta.setLore(lore);
        
        // Set banner display name
        meta.setDisplayName(clanColor + clan.getName() + "'s Banner");
        
        banner.setItemMeta(meta);
        return banner;
    }
    
    /**
     * Creates an advanced clan banner with the clan's color, emblem, and patterns.
     * 
     * @param clan The clan to create a banner for
     * @param style The banner style (shield, flag, standard)
     * @return An ItemStack containing the advanced clan banner
     */
    public ItemStack createAdvancedClanBanner(Clan clan, BannerStyle style) {
        if (clan == null) {
            return null;
        }
        
        // Get clan color
        ChatColor clanColor = clan.getChatColor();
        DyeColor baseColor = chatColorToDyeColor(clanColor);
        DyeColor accentColor = getAccentColor(baseColor);
        
        // Create banner with the correct base color directly (compatible with newer API)
        ItemStack banner = new ItemStack(Material.valueOf(baseColor.toString() + "_BANNER"));
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        
        // Add patterns based on style
        List<Pattern> patterns = new ArrayList<>();
        switch (style) {
            case SHIELD:
                patterns.add(new Pattern(accentColor, PatternType.BORDER));
                patterns.add(new Pattern(accentColor, PatternType.CURLY_BORDER));
                
                // Use first letter of clan tag as emblem
                char emblemChar = clan.getTag().charAt(0);
                addLetterPattern(patterns, emblemChar, accentColor);
                break;
                
            case FLAG:
                patterns.add(new Pattern(accentColor, PatternType.STRIPE_TOP));
                patterns.add(new Pattern(accentColor, PatternType.STRIPE_BOTTOM));
                patterns.add(new Pattern(baseColor, PatternType.RHOMBUS_MIDDLE));
                patterns.add(new Pattern(accentColor, PatternType.CIRCLE_MIDDLE));
                break;
                
            case STANDARD:
                patterns.add(new Pattern(accentColor, PatternType.HALF_HORIZONTAL));
                patterns.add(new Pattern(baseColor, PatternType.TRIANGLE_TOP));
                patterns.add(new Pattern(accentColor, PatternType.TRIANGLE_BOTTOM));
                patterns.add(new Pattern(baseColor, PatternType.RHOMBUS_MIDDLE));
                break;
                
            case WAVE:
                patterns.add(new Pattern(accentColor, PatternType.GRADIENT));
                patterns.add(new Pattern(accentColor, PatternType.GRADIENT_UP));
                patterns.add(new Pattern(baseColor, PatternType.TRIANGLES_TOP));
                patterns.add(new Pattern(baseColor, PatternType.TRIANGLES_BOTTOM));
                break;
                
            case EMBLEM:
            default:
                patterns.add(new Pattern(accentColor, PatternType.BORDER));
                int level = clan.getLevel();
                
                // Add stars based on clan level
                if (level >= 5) {
                    patterns.add(new Pattern(accentColor, PatternType.FLOWER));
                } else if (level >= 3) {
                    patterns.add(new Pattern(accentColor, PatternType.CROSS));
                } else {
                    patterns.add(new Pattern(accentColor, PatternType.CIRCLE_MIDDLE));
                }
                break;
        }
        
        meta.setPatterns(patterns);
        
        // Add clan info as lore
        List<String> lore = new ArrayList<>();
        lore.add(clanColor + "Clan: " + clan.getName());
        lore.add(clanColor + "Tag: " + clan.getTag());
        lore.add(clanColor + "Level: " + clan.getLevel());
        lore.add(clanColor + "Members: " + clan.getMembers().size());
        meta.setLore(lore);
        
        // Set banner display name
        meta.setDisplayName(clanColor + clan.getName() + "'s " + style.getName());
        
        banner.setItemMeta(meta);
        return banner;
    }
    
    /**
     * Gets a complementary accent color for the banner.
     * 
     * @param baseColor The base color
     * @return A complementary accent color
     */
    private DyeColor getAccentColor(DyeColor baseColor) {
        switch (baseColor) {
            case BLACK:
                return DyeColor.WHITE;
            case GRAY:
            case LIGHT_GRAY:
                return DyeColor.BLACK;
            case RED:
                return DyeColor.BLACK;
            case PINK:
                return DyeColor.PURPLE;
            case GREEN:
            case LIME:
                return DyeColor.BLACK;
            case BROWN:
                return DyeColor.ORANGE;
            case YELLOW:
                return DyeColor.ORANGE;
            case BLUE:
            case LIGHT_BLUE:
                return DyeColor.WHITE;
            case MAGENTA:
            case PURPLE:
                return DyeColor.LIGHT_BLUE;
            case CYAN:
                return DyeColor.BLUE;
            case ORANGE:
                return DyeColor.YELLOW;
            case WHITE:
            default:
                return DyeColor.LIGHT_BLUE;
        }
    }
    
    /**
     * Adds a letter pattern to the banner.
     * This is a simplified version and doesn't actually add true letters,
     * but uses combinations of patterns to approximate letters.
     * 
     * @param patterns The list of patterns to add to
     * @param letter The letter to add
     * @param color The color of the letter
     */
    private void addLetterPattern(List<Pattern> patterns, char letter, DyeColor color) {
        // This is a simplified version that approximates a few letters
        switch (Character.toUpperCase(letter)) {
            case 'A':
                patterns.add(new Pattern(color, PatternType.STRIPE_TOP));
                patterns.add(new Pattern(color, PatternType.STRIPE_LEFT));
                patterns.add(new Pattern(color, PatternType.STRIPE_RIGHT));
                patterns.add(new Pattern(color, PatternType.STRIPE_MIDDLE));
                break;
            case 'T':
                patterns.add(new Pattern(color, PatternType.STRIPE_TOP));
                patterns.add(new Pattern(color, PatternType.STRIPE_CENTER));
                break;
            case 'X':
                patterns.add(new Pattern(color, PatternType.CROSS));
                break;
            case 'O':
                patterns.add(new Pattern(color, PatternType.CIRCLE_MIDDLE));
                break;
            default:
                // Default to a simple symbol
                patterns.add(new Pattern(color, PatternType.RHOMBUS_MIDDLE));
                break;
        }
    }
    
    /**
     * Gives a player a clan banner based on their clan.
     * 
     * @param player The player to give a banner to
     * @param style The banner style
     * @return True if the banner was given successfully
     */
    public boolean giveClanBanner(Player player, BannerStyle style) {
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You must be in a clan to get a clan banner.");
            return false;
        }
        
        ItemStack banner = createAdvancedClanBanner(clan, style);
        if (banner == null) {
            player.sendMessage(ChatColor.RED + "Failed to create a clan banner.");
            return false;
        }
        
        // Give the player the banner
        player.getInventory().addItem(banner);
        player.sendMessage(ChatColor.GREEN + "You have received a " + style.getName() + " for your clan!");
        return true;
    }
    
    /**
     * Banner style enum for different banner types.
     */
    public enum BannerStyle {
        SHIELD("Shield"),
        FLAG("Flag"),
        STANDARD("Standard"),
        WAVE("Wave Banner"),
        EMBLEM("Emblem");
        
        private final String name;
        
        BannerStyle(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    /**
     * Example usage method.
     * 
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        // This is just an example and won't actually run
        System.out.println("This is an example class for creating clan banners in Minecraft.");
    }
}
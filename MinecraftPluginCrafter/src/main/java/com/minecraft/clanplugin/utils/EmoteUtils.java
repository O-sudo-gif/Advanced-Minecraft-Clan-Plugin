package com.minecraft.clanplugin.utils;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for managing clan chat emotes and reactions.
 */
public class EmoteUtils {
    
    private static ClanPlugin plugin;
    private static Map<String, String> globalEmotes;
    private static Map<String, Map<String, String>> clanEmotes;
    private static File emotesFile;
    private static FileConfiguration emotesConfig;
    private static final Pattern EMOTE_PATTERN = Pattern.compile(":(\\w+):");
    
    /**
     * Initializes the emote utilities with plugin instance.
     * 
     * @param pluginInstance The plugin instance
     */
    public static void init(ClanPlugin pluginInstance) {
        plugin = pluginInstance;
        globalEmotes = new HashMap<>();
        clanEmotes = new HashMap<>();
        
        // Load emotes from config
        loadEmotes();
        
        // Add default global emotes if none exist
        if (globalEmotes.isEmpty()) {
            setupDefaultEmotes();
        }
    }
    
    /**
     * Loads emotes from configuration file.
     */
    private static void loadEmotes() {
        // Create/load emotes file
        emotesFile = new File(plugin.getDataFolder(), "emotes.yml");
        
        // Make sure data folder exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        if (!emotesFile.exists()) {
            try {
                // Try to get from resources first
                try {
                    // Try to save the resource
                    plugin.saveResource("emotes.yml", false);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("emotes.yml not found in jar, creating a default file");
                    emotesFile.createNewFile();
                    
                    // Initialize the config before saving default emotes
                    emotesConfig = new YamlConfiguration();
                    saveDefaultEmotes();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create emotes.yml: " + e.getMessage());
            }
        }
        
        // Load the config after ensuring the file exists
        emotesConfig = YamlConfiguration.loadConfiguration(emotesFile);
        
        // Load global emotes
        ConfigurationSection globalSection = emotesConfig.getConfigurationSection("global");
        if (globalSection != null) {
            for (String key : globalSection.getKeys(false)) {
                globalEmotes.put(key, globalSection.getString(key));
            }
        }
        
        // Load clan emotes
        ConfigurationSection clansSection = emotesConfig.getConfigurationSection("clans");
        if (clansSection != null) {
            for (String clanName : clansSection.getKeys(false)) {
                ConfigurationSection clanSection = clansSection.getConfigurationSection(clanName);
                if (clanSection != null) {
                    Map<String, String> clanEmoteMap = new HashMap<>();
                    for (String key : clanSection.getKeys(false)) {
                        clanEmoteMap.put(key, clanSection.getString(key));
                    }
                    clanEmotes.put(clanName, clanEmoteMap);
                }
            }
        }
    }
    
    /**
     * Sets up default emotes.
     */
    private static void setupDefaultEmotes() {
        // Basic emoticons
        globalEmotes.put("smile", "☺");
        globalEmotes.put("happy", "😄");
        globalEmotes.put("sad", "😢");
        globalEmotes.put("grin", "😁");
        globalEmotes.put("frown", "☹");
        globalEmotes.put("wink", "😉");
        globalEmotes.put("laugh", "😂");
        globalEmotes.put("hmm", "🤔");
        globalEmotes.put("cool", "😎");
        
        // Special symbols
        globalEmotes.put("heart", "❤");
        globalEmotes.put("star", "★");
        globalEmotes.put("music", "♫");
        globalEmotes.put("check", "✓");
        globalEmotes.put("x", "✗");
        globalEmotes.put("sword", "⚔");
        globalEmotes.put("shield", "🛡");
        globalEmotes.put("potion", "🧪");
        globalEmotes.put("bow", "🏹");
        
        // Minecraft themed
        globalEmotes.put("diamond", "💎");
        globalEmotes.put("gold", "🪙");
        globalEmotes.put("tnt", "💣");
        globalEmotes.put("creeper", "👾");
        globalEmotes.put("pickaxe", "⛏");
        
        // Save default emotes
        saveEmotes();
    }
    
    /**
     * Saves default emotes to the emotes.yml file.
     */
    private static void saveDefaultEmotes() {
        emotesConfig.createSection("global");
        emotesConfig.createSection("clans");
        saveEmotes();
    }
    
    /**
     * Saves emotes to configuration file.
     */
    public static void saveEmotes() {
        // Save global emotes
        for (Map.Entry<String, String> entry : globalEmotes.entrySet()) {
            emotesConfig.set("global." + entry.getKey(), entry.getValue());
        }
        
        // Save clan emotes
        for (Map.Entry<String, Map<String, String>> clanEntry : clanEmotes.entrySet()) {
            for (Map.Entry<String, String> emoteEntry : clanEntry.getValue().entrySet()) {
                emotesConfig.set("clans." + clanEntry.getKey() + "." + emoteEntry.getKey(), emoteEntry.getValue());
            }
        }
        
        try {
            emotesConfig.save(emotesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save emotes.yml: " + e.getMessage());
        }
    }
    
    /**
     * Adds an emote to a clan.
     * 
     * @param clan The clan to add the emote to
     * @param emoteCode The emote code/name
     * @param emoteText The emote text/symbol
     * @return True if the emote was added successfully
     */
    public static boolean addClanEmote(Clan clan, String emoteCode, String emoteText) {
        // Make sure emote code doesn't have colons
        emoteCode = emoteCode.replaceAll(":", "");
        
        // Get or create clan emote map
        Map<String, String> clanEmoteMap = clanEmotes.getOrDefault(clan.getName(), new HashMap<>());
        
        // Add the emote
        clanEmoteMap.put(emoteCode, emoteText);
        clanEmotes.put(clan.getName(), clanEmoteMap);
        
        // Save changes
        saveEmotes();
        return true;
    }
    
    /**
     * Removes an emote from a clan.
     * 
     * @param clan The clan to remove the emote from
     * @param emoteCode The emote code/name to remove
     * @return True if the emote was removed successfully
     */
    public static boolean removeClanEmote(Clan clan, String emoteCode) {
        Map<String, String> clanEmoteMap = clanEmotes.get(clan.getName());
        if (clanEmoteMap == null || !clanEmoteMap.containsKey(emoteCode)) {
            return false;
        }
        
        clanEmoteMap.remove(emoteCode);
        saveEmotes();
        return true;
    }
    
    /**
     * Processes a message, replacing emote codes with their symbols.
     * 
     * @param message The message to process
     * @param clan The clan for clan-specific emotes, or null for global only
     * @return The processed message with emotes
     */
    public static String processEmotes(String message, Clan clan) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        StringBuffer result = new StringBuffer();
        Matcher matcher = EMOTE_PATTERN.matcher(message);
        
        while (matcher.find()) {
            String emoteCode = matcher.group(1);
            String replacement = null;
            
            // Check clan emotes first if clan is provided
            if (clan != null && clanEmotes.containsKey(clan.getName())) {
                replacement = clanEmotes.get(clan.getName()).get(emoteCode);
            }
            
            // Fall back to global emotes
            if (replacement == null) {
                replacement = globalEmotes.get(emoteCode);
            }
            
            // Use the matched text if no replacement found
            if (replacement == null) {
                replacement = matcher.group();
            }
            
            // Replace the emote in the message
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Lists available emotes to a player.
     * 
     * @param player The player to show emotes to
     * @param clan The player's clan, or null for global emotes only
     */
    public static void listEmotes(Player player, Clan clan) {
        player.sendMessage(ChatColor.GOLD + "=== Available Emotes ===");
        player.sendMessage(ChatColor.GRAY + "Usage: Type :code: in chat to use an emote");
        
        // Global emotes
        player.sendMessage(ChatColor.YELLOW + "Global Emotes:");
        for (Map.Entry<String, String> entry : globalEmotes.entrySet()) {
            player.sendMessage(ChatColor.WHITE + ":" + entry.getKey() + ": " + ChatColor.GRAY + "→ " + 
                    ChatColor.WHITE + entry.getValue());
        }
        
        // Clan emotes
        if (clan != null && clanEmotes.containsKey(clan.getName())) {
            player.sendMessage("");
            player.sendMessage(ChatColor.valueOf(clan.getColor().toUpperCase()) + clan.getName() + 
                    ChatColor.YELLOW + " Clan Emotes:");
            
            Map<String, String> clanEmoteMap = clanEmotes.get(clan.getName());
            for (Map.Entry<String, String> entry : clanEmoteMap.entrySet()) {
                player.sendMessage(ChatColor.WHITE + ":" + entry.getKey() + ": " + ChatColor.GRAY + "→ " + 
                        ChatColor.WHITE + entry.getValue());
            }
        }
    }
    
    /**
     * Adds a reaction to a message.
     * 
     * @param messageId A unique identifier for the message
     * @param playerUuid The UUID of the player adding the reaction
     * @param emoteCode The emote code to use as a reaction
     */
    public static void addReaction(String messageId, UUID playerUuid, String emoteCode) {
        // Implementation for message reactions would go here
        // This would require message storage and tracking who reacted with what
        // For now, this is a placeholder for future implementation
    }
}
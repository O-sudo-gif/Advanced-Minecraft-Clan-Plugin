package com.minecraft.clanplugin.webhook;

import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.Territory;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages webhook notifications to external platforms
 */
public class WebhookManager {
    
    private final JavaPlugin plugin;
    private final Map<String, String> webhookUrls;
    private boolean enabled;
    
    /**
     * Create a new WebhookManager
     *
     * @param plugin The plugin instance
     */
    public WebhookManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.webhookUrls = new HashMap<>();
        loadConfig();
    }
    
    /**
     * Load webhook configuration from config
     */
    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        this.enabled = config.getBoolean("webhooks.enabled", false);
        
        if (!enabled) {
            plugin.getLogger().info("Webhook notifications are disabled");
            return;
        }
        
        // Load webhook URLs from config
        if (config.contains("webhooks.urls")) {
            for (String key : config.getConfigurationSection("webhooks.urls").getKeys(false)) {
                String url = config.getString("webhooks.urls." + key);
                if (url != null && !url.isEmpty()) {
                    webhookUrls.put(key, url);
                    plugin.getLogger().info("Loaded webhook URL for: " + key);
                }
            }
        }
        
        if (webhookUrls.isEmpty()) {
            plugin.getLogger().warning("No webhook URLs configured.");
            enabled = false;
        }
    }
    
    /**
     * Send a notification about clan creation
     *
     * @param clan The clan that was created
     * @param creator The UUID of the player who created the clan
     * @return CompletableFuture that completes when the notification is sent
     */
    public CompletableFuture<Boolean> notifyClanCreation(Clan clan, UUID creator) {
        if (!enabled || !webhookUrls.containsKey("clan_creation")) {
            return CompletableFuture.completedFuture(false);
        }
        
        String creatorName = Bukkit.getOfflinePlayer(creator).getName();
        if (creatorName == null) {
            creatorName = creator.toString().substring(0, 8);
        }
        
        // Get color name safely
        String colorName = getClanColorName(clan.getColor());
        
        // Build the payload with sanitized inputs to prevent JSON injection
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"embeds\": [{");
        json.append("\"title\": \"New Clan Created\",");
        json.append("\"color\": 3066993,");
        json.append("\"description\": \"A new clan has been formed!\",");
        json.append("\"fields\": [");
        json.append("{\"name\": \"Clan Name\", \"value\": \"").append(sanitizeForJson(clan.getName())).append("\", \"inline\": true},");
        json.append("{\"name\": \"Clan Tag\", \"value\": \"").append(sanitizeForJson(clan.getTag())).append("\", \"inline\": true},");
        json.append("{\"name\": \"Founder\", \"value\": \"").append(sanitizeForJson(creatorName)).append("\", \"inline\": true},");
        json.append("{\"name\": \"Color\", \"value\": \"").append(sanitizeForJson(colorName)).append("\", \"inline\": true},");
        json.append("{\"name\": \"Initial Members\", \"value\": \"").append(clan.getMembers().size()).append("\", \"inline\": true}");
        json.append("]");
        json.append("}}]}");
        
        return sendWebhook("clan_creation", json.toString());
    }
    
    /**
     * Send a notification about territory capture
     *
     * @param clan The clan that captured the territory
     * @param territoryName The name of the captured territory
     * @return CompletableFuture that completes when the notification is sent
     */
    public CompletableFuture<Boolean> notifyTerritoryCaptured(Clan clan, String territoryName) {
        if (!enabled || !webhookUrls.containsKey("territory_capture")) {
            return CompletableFuture.completedFuture(false);
        }
        
        // Build the payload with sanitized inputs to prevent JSON injection
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"embeds\": [{");
        json.append("\"title\": \"Territory Captured\",");
        json.append("\"color\": 15105570,");
        json.append("\"description\": \"A clan has captured a new territory!\",");
        json.append("\"fields\": [");
        json.append("{\"name\": \"Clan\", \"value\": \"").append(sanitizeForJson(clan.getName())).append("\", \"inline\": true},");
        json.append("{\"name\": \"Territory\", \"value\": \"").append(sanitizeForJson(territoryName)).append("\", \"inline\": true},");
        json.append("{\"name\": \"Clan Level\", \"value\": \"").append(clan.getLevel()).append("\", \"inline\": true},");
        json.append("{\"name\": \"Clan Members\", \"value\": \"").append(clan.getMembers().size()).append("\", \"inline\": true}");
        json.append("]");
        json.append("}}]}");
        
        return sendWebhook("territory_capture", json.toString());
    }
    
    /**
     * Send a notification about a clan war starting
     *
     * @param attackerClan The attacking clan
     * @param defenderClan The defending clan
     * @return CompletableFuture that completes when the notification is sent
     */
    public CompletableFuture<Boolean> notifyWarStarted(Clan attackerClan, Clan defenderClan) {
        if (!enabled || !webhookUrls.containsKey("clan_war")) {
            return CompletableFuture.completedFuture(false);
        }
        
        // Build the payload
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"embeds\": [{");
        json.append("\"title\": \"Clan War Started\",");
        json.append("\"color\": 15158332,");
        json.append("\"description\": \"A clan war has begun!\",");
        json.append("\"fields\": [");
        json.append("{\"name\": \"Attacker\", \"value\": \"").append(attackerClan.getName()).append("\", \"inline\": true},");
        json.append("{\"name\": \"Defender\", \"value\": \"").append(defenderClan.getName()).append("\", \"inline\": true},");
        json.append("{\"name\": \"Attacker Level\", \"value\": \"").append(attackerClan.getLevel()).append("\", \"inline\": true},");
        json.append("{\"name\": \"Defender Level\", \"value\": \"").append(defenderClan.getLevel()).append("\", \"inline\": true},");
        json.append("{\"name\": \"Attacker Members\", \"value\": \"").append(attackerClan.getMembers().size()).append("\", \"inline\": true},");
        json.append("{\"name\": \"Defender Members\", \"value\": \"").append(defenderClan.getMembers().size()).append("\", \"inline\": true}");
        json.append("]");
        json.append("}}]}");
        
        return sendWebhook("clan_war", json.toString());
    }
    
    /**
     * Send a notification about clan achievement
     *
     * @param clan The clan that achieved something
     * @param achievement The achievement name
     * @param description The achievement description
     * @return CompletableFuture that completes when the notification is sent
     */
    public CompletableFuture<Boolean> notifyAchievement(Clan clan, String achievement, String description) {
        if (!enabled || !webhookUrls.containsKey("achievement")) {
            return CompletableFuture.completedFuture(false);
        }
        
        // Safely handle null values
        String clanName = clan != null ? clan.getName() : "Unknown Clan";
        String achievementName = achievement != null ? achievement : "Unknown Achievement";
        String achievementDesc = description != null ? description : "No description available";
        
        // Build the payload with sanitized inputs to prevent JSON injection
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"embeds\": [{");
        json.append("\"title\": \"Clan Achievement Unlocked\",");
        json.append("\"color\": 7419530,");
        json.append("\"description\": \"A clan has unlocked a new achievement!\",");
        json.append("\"fields\": [");
        json.append("{\"name\": \"Clan\", \"value\": \"").append(sanitizeForJson(clanName)).append("\", \"inline\": true},");
        json.append("{\"name\": \"Achievement\", \"value\": \"").append(sanitizeForJson(achievementName)).append("\", \"inline\": true},");
        json.append("{\"name\": \"Description\", \"value\": \"").append(sanitizeForJson(achievementDesc)).append("\", \"inline\": false}");
        json.append("]");
        json.append("}}]}");
        
        return sendWebhook("achievement", json.toString());
    }
    
    /**
     * Send a webhook message to a specific endpoint
     *
     * @param type The webhook type (key in the webhookUrls map)
     * @param jsonPayload The JSON payload to send
     * @return CompletableFuture that completes when the notification is sent
     */
    private CompletableFuture<Boolean> sendWebhook(String type, String jsonPayload) {
        String url = webhookUrls.get(type);
        if (url == null || url.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        // Validate URL - only allow specific domains for security
        if (!isValidWebhookUrl(url)) {
            plugin.getLogger().warning("Invalid webhook URL detected for type: " + type + ". Ignoring webhook request.");
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            HttpURLConnection connection = null;
            try {
                URL webhookUrl = new URL(url);
                connection = (HttpURLConnection) webhookUrl.openConnection();
                connection.setConnectTimeout(5000); // 5-second timeout
                connection.setReadTimeout(5000);    // 5-second timeout
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "ClanPlugin Webhook");
                connection.setDoOutput(true);
                
                // Send the payload
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                
                // Get the response
                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    plugin.getLogger().info("Successfully sent webhook notification: " + type);
                    return true;
                } else {
                    plugin.getLogger().warning("Failed to send webhook notification: " + type + 
                                               " (Response code: " + responseCode + ")");
                    return false;
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Error sending webhook notification: " + type, e);
                return false;
            } finally {
                if (connection != null) {
                    connection.disconnect(); // Ensure connection is closed
                }
            }
        });
    }
    
    /**
     * Check if webhooks are enabled
     *
     * @return True if webhooks are enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Shuts down the webhook manager, cancelling any pending tasks
     */
    public void shutdown() {
        // In the future, this method would cancel any pending webhook tasks
        // Or close any persistent connections
        plugin.getLogger().info("Shutting down webhook manager...");
    }
    
    /**
     * Send a notification about territory capture
     *
     * @param oldClan The clan that previously controlled the territory (null if unclaimed)
     * @param newClan The clan that now controls the territory
     * @param territory The territory that was captured
     * @param difficulty The difficulty of the conquest
     * @return CompletableFuture that completes when the notification is sent
     */
    public CompletableFuture<Boolean> sendTerritoryConquestWebhook(Clan oldClan, Clan newClan, 
                                                   Territory territory, Object difficulty) {
        if (!enabled || !webhookUrls.containsKey("territory_capture")) {
            return CompletableFuture.completedFuture(false);
        }
        
        // Safely handle null values
        String oldClanName = oldClan != null ? oldClan.getName() : "Unclaimed";
        String territoryName = territory != null ? territory.getName() : "Unknown";
        String newClanName = newClan != null ? newClan.getName() : "Unknown";
        String difficultyStr = difficulty != null ? difficulty.toString() : "Normal";
        
        // Build the payload with sanitized inputs to prevent JSON injection
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"embeds\": [{");
        json.append("\"title\": \"Territory Conquered\",");
        json.append("\"color\": 15105570,");
        json.append("\"description\": \"A territory has changed hands!\",");
        json.append("\"fields\": [");
        json.append("{\"name\": \"Territory\", \"value\": \"").append(sanitizeForJson(territoryName)).append("\", \"inline\": true},");
        json.append("{\"name\": \"Previous Owner\", \"value\": \"").append(sanitizeForJson(oldClanName)).append("\", \"inline\": true},");
        json.append("{\"name\": \"New Owner\", \"value\": \"").append(sanitizeForJson(newClanName)).append("\", \"inline\": true},");
        json.append("{\"name\": \"Conquest Difficulty\", \"value\": \"").append(sanitizeForJson(difficultyStr)).append("\", \"inline\": true}");
        json.append("]");
        json.append("}}]}");
        
        return sendWebhook("territory_capture", json.toString());
    }
    
    /**
     * Convert a ChatColor string to a readable color name
     *
     * @param colorStr The color string from Clan.getColor()
     * @return The human-readable color name
     */
    private String getClanColorName(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return "Gold";
        }
        
        // Remove any § or & characters and get just the color code
        String cleanColor = colorStr.replaceAll("§|&", "").trim();
        if (cleanColor.isEmpty()) {
            return "Gold";
        }
        
        char colorChar = cleanColor.charAt(0);
        switch (colorChar) {
            case '0': return "Black";
            case '1': return "Dark Blue";
            case '2': return "Dark Green";
            case '3': return "Dark Aqua";
            case '4': return "Dark Red";
            case '5': return "Dark Purple";
            case '6': return "Gold";
            case '7': return "Gray";
            case '8': return "Dark Gray";
            case '9': return "Blue";
            case 'a': return "Green";
            case 'b': return "Aqua";
            case 'c': return "Red";
            case 'd': return "Light Purple";
            case 'e': return "Yellow";
            case 'f': return "White";
            default: return "Gold";
        }
    }
    
    /**
     * Validates a webhook URL to ensure it's from an allowed domain
     * This helps prevent malicious webhook URLs from being added to the config
     *
     * @param url The webhook URL to validate
     * @return True if the URL is valid and from an allowed domain
     */
    private boolean isValidWebhookUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        try {
            URL webhookUrl = new URL(url);
            String host = webhookUrl.getHost().toLowerCase();
            
            // Only allow specific domains for webhooks
            // Add more trusted domains as needed
            return host.endsWith("discord.com") || 
                   host.endsWith("discordapp.com") || 
                   host.endsWith("slack.com") || 
                   host.endsWith("github.com") ||
                   host.endsWith("gitlab.com") ||
                   host.endsWith("minecraft-clans.com");
                   
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid webhook URL format: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Helper method to sanitize strings for JSON to prevent injection attacks
     * 
     * @param input The string to sanitize
     * @return A sanitized string safe for JSON inclusion
     */
    private String sanitizeForJson(String input) {
        if (input == null) {
            return "";
        }
        
        return input.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
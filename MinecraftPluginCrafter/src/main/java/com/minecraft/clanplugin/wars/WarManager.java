package com.minecraft.clanplugin.wars;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Manages clan wars.
 */
public class WarManager {
    
    private final ClanPlugin plugin;
    private final Map<String, ClanWar> activeWars;
    private final Map<String, List<ClanWar>> clanWarHistory;
    private final File warsFile;
    
    /**
     * Creates a new WarManager
     * 
     * @param plugin The plugin instance
     */
    public WarManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.activeWars = new HashMap<>();
        this.clanWarHistory = new HashMap<>();
        this.warsFile = new File(plugin.getDataFolder(), "wars.json");
        
        // Load wars from file
        loadWars();
    }
    
    /**
     * Declares war between two clans
     * 
     * @param initiatingClan The clan declaring war
     * @param targetClan The clan being declared war on
     * @return True if the war was successfully declared
     */
    public boolean declareWar(String initiatingClan, String targetClan) {
        // Check if either clan is already at war
        if (isAtWar(initiatingClan) || isAtWar(targetClan)) {
            return false;
        }
        
        // Create the war
        ClanWar war = new ClanWar(initiatingClan, targetClan);
        
        // Add to active wars
        String warId = war.getWarId();
        activeWars.put(warId, war);
        
        // Update clan relations
        Clan initClan = plugin.getStorageManager().getClanStorage().getClan(initiatingClan);
        Clan targClan = plugin.getStorageManager().getClanStorage().getClan(targetClan);
        
        if (initClan != null && targClan != null) {
            // Set as enemies
            initClan.addEnemy(targetClan);
            targClan.addEnemy(initiatingClan);
            
            // Remove any ally relations
            initClan.removeAlliance(targetClan);
            targClan.removeAlliance(initiatingClan);
        }
        
        // Save changes
        saveWars();
        
        return true;
    }
    
    /**
     * Ends a war with a specific outcome
     * 
     * @param warId The ID of the war
     * @param status The outcome status
     * @return True if the war was successfully ended
     */
    public boolean endWar(String warId, WarStatus status) {
        ClanWar war = activeWars.get(warId);
        if (war == null) {
            return false;
        }
        
        // Set the status
        war.setStatus(status);
        
        // Add to war history
        addToWarHistory(war);
        
        // Remove from active wars
        activeWars.remove(warId);
        
        // Save changes
        saveWars();
        
        return true;
    }
    
    /**
     * Surrenders a war
     * 
     * @param clanName The clan surrendering
     * @return True if the surrender was successful
     */
    public boolean surrenderWar(String clanName) {
        ClanWar war = getWarForClan(clanName);
        if (war == null) {
            return false;
        }
        
        // Determine surrender status
        WarStatus status;
        if (clanName.equalsIgnoreCase(war.getInitiatingClan())) {
            status = WarStatus.INITIATOR_SURRENDER;
        } else {
            status = WarStatus.TARGET_SURRENDER;
        }
        
        return endWar(war.getWarId(), status);
    }
    
    /**
     * Registers a kill in a war
     * 
     * @param killer The player who got the kill
     * @param victim The player who was killed
     * @return True if the kill was registered
     */
    public boolean registerKill(Player killer, Player victim) {
        String killerClan = plugin.getStorageManager().getClanStorage().getPlayerClanName(killer.getUniqueId());
        String victimClan = plugin.getStorageManager().getClanStorage().getPlayerClanName(victim.getUniqueId());
        
        if (killerClan == null || victimClan == null) {
            return false; // One or both players are not in a clan
        }
        
        // Find a war involving both clans
        for (ClanWar war : activeWars.values()) {
            if ((war.getInitiatingClan().equalsIgnoreCase(killerClan) && 
                 war.getTargetClan().equalsIgnoreCase(victimClan)) ||
                (war.getInitiatingClan().equalsIgnoreCase(victimClan) && 
                 war.getTargetClan().equalsIgnoreCase(killerClan))) {
                
                // Register the kill
                war.addKill(killer.getUniqueId(), killerClan);
                
                // Save changes
                saveWars();
                
                return true;
            }
        }
        
        return false; // No war between these clans
    }
    
    /**
     * Checks if a clan is currently at war
     * 
     * @param clanName The name of the clan
     * @return True if the clan is at war
     */
    public boolean isAtWar(String clanName) {
        return getWarForClan(clanName) != null;
    }
    
    /**
     * Gets the current war a clan is involved in
     * 
     * @param clanName The name of the clan
     * @return The war, or null if not at war
     */
    public ClanWar getWarForClan(String clanName) {
        for (ClanWar war : activeWars.values()) {
            if (war.involves(clanName)) {
                return war;
            }
        }
        return null;
    }
    
    /**
     * Gets all active wars
     * 
     * @return A list of all active wars
     */
    public List<ClanWar> getAllActiveWars() {
        return new ArrayList<>(activeWars.values());
    }
    
    /**
     * Gets war history for a clan
     * 
     * @param clanName The name of the clan
     * @return A list of past wars
     */
    public List<ClanWar> getWarHistory(String clanName) {
        return clanWarHistory.getOrDefault(clanName.toLowerCase(), new ArrayList<>());
    }
    
    /**
     * Gets war statistics for a clan
     * 
     * @param clanName The name of the clan
     * @return A map with statistics
     */
    public Map<String, Integer> getWarStats(String clanName) {
        List<ClanWar> history = getWarHistory(clanName);
        Map<String, Integer> stats = new HashMap<>();
        
        int victories = 0;
        int defeats = 0;
        int draws = 0;
        
        for (ClanWar war : history) {
            if (war.getStatus() == WarStatus.INITIATOR_VICTORY && 
                war.getInitiatingClan().equalsIgnoreCase(clanName)) {
                victories++;
            } else if (war.getStatus() == WarStatus.TARGET_VICTORY && 
                       war.getTargetClan().equalsIgnoreCase(clanName)) {
                victories++;
            } else if (war.getStatus() == WarStatus.TARGET_SURRENDER && 
                       war.getInitiatingClan().equalsIgnoreCase(clanName)) {
                victories++;
            } else if (war.getStatus() == WarStatus.INITIATOR_SURRENDER && 
                       war.getTargetClan().equalsIgnoreCase(clanName)) {
                victories++;
            } else if (war.getStatus() == WarStatus.DRAW) {
                draws++;
            } else if (war.getStatus() != WarStatus.CANCELLED) {
                defeats++;
            }
        }
        
        stats.put("victories", victories);
        stats.put("defeats", defeats);
        stats.put("draws", draws);
        stats.put("total", history.size());
        
        return stats;
    }
    
    /**
     * Gets top killers in a war
     * 
     * @param warId The ID of the war
     * @param limit The maximum number of killers to return
     * @return A map of player UUIDs to kill counts, sorted by kills
     */
    public Map<UUID, Integer> getTopKillersInWar(String warId, int limit) {
        ClanWar war = activeWars.get(warId);
        if (war == null) {
            return new HashMap<>();
        }
        
        return war.getKills().entrySet().stream()
            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }
    
    /**
     * Processes ended wars
     * Should be called periodically
     */
    public void processEndedWars() {
        List<String> endedWarIds = new ArrayList<>();
        
        for (ClanWar war : activeWars.values()) {
            if (System.currentTimeMillis() >= war.getEndTime()) {
                // War has timed out
                WarStatus status;
                
                if (war.getInitiatingClanScore() > war.getTargetClanScore()) {
                    status = WarStatus.INITIATOR_VICTORY;
                } else if (war.getTargetClanScore() > war.getInitiatingClanScore()) {
                    status = WarStatus.TARGET_VICTORY;
                } else {
                    status = WarStatus.DRAW;
                }
                
                // Set the status
                war.setStatus(status);
                
                // Add to war history
                addToWarHistory(war);
                
                // Mark for removal
                endedWarIds.add(war.getWarId());
            }
        }
        
        // Remove ended wars
        for (String warId : endedWarIds) {
            activeWars.remove(warId);
        }
        
        if (!endedWarIds.isEmpty()) {
            // Save changes
            saveWars();
        }
    }
    
    /**
     * Adds a war to the history for both clans
     * 
     * @param war The war to add
     */
    private void addToWarHistory(ClanWar war) {
        String initClan = war.getInitiatingClan().toLowerCase();
        String targClan = war.getTargetClan().toLowerCase();
        
        if (!clanWarHistory.containsKey(initClan)) {
            clanWarHistory.put(initClan, new ArrayList<>());
        }
        if (!clanWarHistory.containsKey(targClan)) {
            clanWarHistory.put(targClan, new ArrayList<>());
        }
        
        clanWarHistory.get(initClan).add(war);
        clanWarHistory.get(targClan).add(war);
    }
    
    /**
     * Loads wars from file
     */
    @SuppressWarnings("unchecked")
    public void loadWars() {
        activeWars.clear();
        clanWarHistory.clear();
        
        if (!warsFile.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(warsFile)) {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(reader);
            
            // Load active wars
            JSONArray activeWarsArray = (JSONArray) jsonObject.get("active_wars");
            
            if (activeWarsArray != null) {
                for (Object obj : activeWarsArray) {
                    JSONObject warObj = (JSONObject) obj;
                    
                    String initiatingClan = (String) warObj.get("initiating_clan");
                    String targetClan = (String) warObj.get("target_clan");
                    long startTime = (Long) warObj.get("start_time");
                    long endTime = (Long) warObj.get("end_time");
                    WarStatus status = WarStatus.valueOf((String) warObj.get("status"));
                    int initiatingScore = ((Long) warObj.get("initiating_score")).intValue();
                    int targetScore = ((Long) warObj.get("target_score")).intValue();
                    
                    // Load kills
                    Map<UUID, Integer> kills = new HashMap<>();
                    JSONObject killsObj = (JSONObject) warObj.get("kills");
                    
                    if (killsObj != null) {
                        for (Object key : killsObj.keySet()) {
                            String playerIdStr = (String) key;
                            int killCount = ((Long) killsObj.get(playerIdStr)).intValue();
                            kills.put(UUID.fromString(playerIdStr), killCount);
                        }
                    }
                    
                    ClanWar war = new ClanWar(initiatingClan, targetClan, startTime, endTime, 
                                             status, kills, initiatingScore, targetScore);
                    
                    activeWars.put(war.getWarId(), war);
                }
            }
            
            // Load war history
            JSONObject historyObj = (JSONObject) jsonObject.get("war_history");
            
            if (historyObj != null) {
                for (Object clanKey : historyObj.keySet()) {
                    String clanName = (String) clanKey;
                    JSONArray clanWarsArray = (JSONArray) historyObj.get(clanName);
                    
                    List<ClanWar> clanWars = new ArrayList<>();
                    
                    for (Object obj : clanWarsArray) {
                        JSONObject warObj = (JSONObject) obj;
                        
                        String initiatingClan = (String) warObj.get("initiating_clan");
                        String targetClan = (String) warObj.get("target_clan");
                        long startTime = (Long) warObj.get("start_time");
                        long endTime = (Long) warObj.get("end_time");
                        WarStatus status = WarStatus.valueOf((String) warObj.get("status"));
                        int initiatingScore = ((Long) warObj.get("initiating_score")).intValue();
                        int targetScore = ((Long) warObj.get("target_score")).intValue();
                        
                        // Load kills
                        Map<UUID, Integer> kills = new HashMap<>();
                        JSONObject killsObj = (JSONObject) warObj.get("kills");
                        
                        if (killsObj != null) {
                            for (Object key : killsObj.keySet()) {
                                String playerIdStr = (String) key;
                                int killCount = ((Long) killsObj.get(playerIdStr)).intValue();
                                kills.put(UUID.fromString(playerIdStr), killCount);
                            }
                        }
                        
                        ClanWar war = new ClanWar(initiatingClan, targetClan, startTime, endTime, 
                                                 status, kills, initiatingScore, targetScore);
                        
                        clanWars.add(war);
                    }
                    
                    clanWarHistory.put(clanName.toLowerCase(), clanWars);
                }
            }
            
        } catch (IOException | ParseException e) {
            plugin.getLogger().warning("Failed to load wars: " + e.getMessage());
        }
    }
    
    /**
     * Saves wars to file
     */
    @SuppressWarnings("unchecked")
    public void saveWars() {
        JSONObject jsonObject = new JSONObject();
        
        // Save active wars
        JSONArray activeWarsArray = new JSONArray();
        
        for (ClanWar war : activeWars.values()) {
            JSONObject warObj = new JSONObject();
            
            warObj.put("initiating_clan", war.getInitiatingClan());
            warObj.put("target_clan", war.getTargetClan());
            warObj.put("start_time", war.getStartTime());
            warObj.put("end_time", war.getEndTime());
            warObj.put("status", war.getStatus().name());
            warObj.put("initiating_score", war.getInitiatingClanScore());
            warObj.put("target_score", war.getTargetClanScore());
            
            // Save kills
            JSONObject killsObj = new JSONObject();
            for (Map.Entry<UUID, Integer> entry : war.getKills().entrySet()) {
                killsObj.put(entry.getKey().toString(), entry.getValue());
            }
            
            warObj.put("kills", killsObj);
            
            activeWarsArray.add(warObj);
        }
        
        jsonObject.put("active_wars", activeWarsArray);
        
        // Save war history
        JSONObject historyObj = new JSONObject();
        
        for (Map.Entry<String, List<ClanWar>> entry : clanWarHistory.entrySet()) {
            JSONArray clanWarsArray = new JSONArray();
            
            for (ClanWar war : entry.getValue()) {
                JSONObject warObj = new JSONObject();
                
                warObj.put("initiating_clan", war.getInitiatingClan());
                warObj.put("target_clan", war.getTargetClan());
                warObj.put("start_time", war.getStartTime());
                warObj.put("end_time", war.getEndTime());
                warObj.put("status", war.getStatus().name());
                warObj.put("initiating_score", war.getInitiatingClanScore());
                warObj.put("target_score", war.getTargetClanScore());
                
                // Save kills
                JSONObject killsObj = new JSONObject();
                for (Map.Entry<UUID, Integer> killEntry : war.getKills().entrySet()) {
                    killsObj.put(killEntry.getKey().toString(), killEntry.getValue());
                }
                
                warObj.put("kills", killsObj);
                
                clanWarsArray.add(warObj);
            }
            
            historyObj.put(entry.getKey(), clanWarsArray);
        }
        
        jsonObject.put("war_history", historyObj);
        
        try (FileWriter writer = new FileWriter(warsFile)) {
            writer.write(jsonObject.toJSONString());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save wars: " + e.getMessage());
        }
    }
    
    /**
     * Formats time remaining in a war
     * 
     * @param timeMs Time in milliseconds
     * @return Formatted time string
     */
    public String formatTimeRemaining(long timeMs) {
        long days = TimeUnit.MILLISECONDS.toDays(timeMs);
        long hours = TimeUnit.MILLISECONDS.toHours(timeMs) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs) % 60;
        
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }
    
    /**
     * Handles when a clan is deleted
     * 
     * @param clanName The name of the clan
     */
    public void handleClanDeleted(String clanName) {
        // End any active wars involving this clan
        List<String> warsToEnd = new ArrayList<>();
        
        for (ClanWar war : activeWars.values()) {
            if (war.involves(clanName)) {
                warsToEnd.add(war.getWarId());
            }
        }
        
        for (String warId : warsToEnd) {
            endWar(warId, WarStatus.CANCELLED);
        }
        
        // Remove from war history
        clanWarHistory.remove(clanName.toLowerCase());
        
        // Save changes
        saveWars();
    }
}
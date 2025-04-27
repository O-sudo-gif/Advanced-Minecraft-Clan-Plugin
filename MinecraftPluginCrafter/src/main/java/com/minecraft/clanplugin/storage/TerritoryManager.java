package com.minecraft.clanplugin.storage;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.Flag;
import com.minecraft.clanplugin.models.Territory;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Manages territory claims and storage.
 */
public class TerritoryManager {
    
    private final ClanPlugin plugin;
    private final Map<String, Territory> territories;
    private final Map<String, List<String>> clanTerritories;
    private final File territoryFile;
    
    /**
     * Creates a new TerritoryManager
     * 
     * @param plugin The plugin instance
     */
    public TerritoryManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.territories = new ConcurrentHashMap<>();
        this.clanTerritories = new ConcurrentHashMap<>();
        this.territoryFile = new File(plugin.getDataFolder(), "territories.json");
        
        // Load territories from file
        loadTerritories();
    }
    
    /**
     * Claims a territory for a clan
     * 
     * @param chunk The chunk to claim
     * @param clanName The name of the clan
     * @param player The player claiming the territory
     * @return True if the claim was successful
     */
    public boolean claimTerritory(Chunk chunk, String clanName, Player player) {
        // Check if the territory is already claimed
        String territoryKey = getTerritoryKey(chunk);
        if (territories.containsKey(territoryKey)) {
            return false;
        }
        
        // Get the clan
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        if (clan == null) {
            return false;
        }
        
        // Check if the clan has reached its territory limit
        int maxClaims = calculateMaxClaims(clan);
        if (clanTerritories.containsKey(clanName) && 
            clanTerritories.get(clanName).size() >= maxClaims) {
            return false;
        }
        
        // Check if the claim is adjacent to existing territory (except first claim)
        if (clanTerritories.containsKey(clanName) && 
            !clanTerritories.get(clanName).isEmpty() && 
            !isAdjacentToExistingTerritory(chunk, clanName)) {
            return false;
        }
        
        // Create the territory
        Territory territory = new Territory(chunk, clanName);
        
        // Create and add a flag at the player's location
        Flag flag = new Flag(player.getLocation(), player.getUniqueId());
        territory.addFlag(flag);
        
        // Add to maps
        territories.put(territoryKey, territory);
        
        // Add to clan territories list
        if (!clanTerritories.containsKey(clanName)) {
            clanTerritories.put(clanName, new ArrayList<>());
        }
        clanTerritories.get(clanName).add(territoryKey);
        
        // Save to file
        saveTerritories();
        
        return true;
    }
    
    /**
     * Unclaims a territory
     * 
     * @param chunk The chunk to unclaim
     * @param clanName The name of the clan (for verification)
     * @return True if the unclaim was successful
     */
    public boolean unclaimTerritory(Chunk chunk, String clanName) {
        String territoryKey = getTerritoryKey(chunk);
        
        Territory territory = territories.get(territoryKey);
        if (territory == null || !territory.getClanName().equals(clanName)) {
            return false;
        }
        
        // Check if unclaiming this would disconnect other territories
        if (wouldDisconnectTerritory(chunk, clanName)) {
            return false;
        }
        
        // Remove from maps
        territories.remove(territoryKey);
        clanTerritories.get(clanName).remove(territoryKey);
        
        // Save to file
        saveTerritories();
        
        return true;
    }
    
    /**
     * Gets the territory at a specific chunk
     * 
     * @param chunk The chunk to check
     * @return The territory, or null if not claimed
     */
    public Territory getTerritory(Chunk chunk) {
        return territories.get(getTerritoryKey(chunk));
    }
    
    /**
     * Gets all territories owned by a clan
     * 
     * @param clanName The name of the clan
     * @return A list of territories
     */
    public List<Territory> getClanTerritories(String clanName) {
        if (!clanTerritories.containsKey(clanName)) {
            return new ArrayList<>();
        }
        
        return clanTerritories.get(clanName).stream()
            .map(territories::get)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the count of territories owned by a clan
     * 
     * @param clanName The name of the clan
     * @return The number of territories
     */
    public int getClanTerritoryCount(String clanName) {
        if (!clanTerritories.containsKey(clanName)) {
            return 0;
        }
        
        return clanTerritories.get(clanName).size();
    }
    
    /**
     * Adds a flag to a territory
     * 
     * @param chunk The chunk where the flag is placed
     * @param flag The flag to add
     * @param clanName The name of the clan (for verification)
     * @return True if the flag was added successfully
     */
    public boolean addFlag(Chunk chunk, Flag flag, String clanName) {
        String territoryKey = getTerritoryKey(chunk);
        
        Territory territory = territories.get(territoryKey);
        if (territory == null || !territory.getClanName().equals(clanName)) {
            return false;
        }
        
        territory.addFlag(flag);
        
        // Save to file
        saveTerritories();
        
        return true;
    }
    
    /**
     * Removes a flag from a territory
     * 
     * @param location The location of the flag
     * @param clanName The name of the clan (for verification)
     * @return True if the flag was removed successfully
     */
    public boolean removeFlag(Location location, String clanName) {
        String territoryKey = getTerritoryKey(location.getChunk());
        
        Territory territory = territories.get(territoryKey);
        if (territory == null || !territory.getClanName().equals(clanName)) {
            return false;
        }
        
        // Find the flag at the location
        Flag flagToRemove = null;
        for (Flag flag : territory.getFlags()) {
            if (flag.getLocation().distance(location) < 1.0) {
                flagToRemove = flag;
                break;
            }
        }
        
        if (flagToRemove == null) {
            return false;
        }
        
        // Remove the flag
        boolean removed = territory.removeFlag(flagToRemove);
        if (removed) {
            // Save to file
            saveTerritories();
        }
        
        return removed;
    }
    
    /**
     * Upgrades a flag in a territory
     * 
     * @param location The location of the flag
     * @param clanName The name of the clan (for verification)
     * @return True if the flag was upgraded successfully
     */
    public boolean upgradeFlag(Location location, String clanName) {
        String territoryKey = getTerritoryKey(location.getChunk());
        
        Territory territory = territories.get(territoryKey);
        if (territory == null || !territory.getClanName().equals(clanName)) {
            return false;
        }
        
        // Find the flag at the location
        Flag flagToUpgrade = null;
        for (Flag flag : territory.getFlags()) {
            if (flag.getLocation().distance(location) < 1.0) {
                flagToUpgrade = flag;
                break;
            }
        }
        
        if (flagToUpgrade == null) {
            return false;
        }
        
        // Upgrade the flag
        boolean upgraded = flagToUpgrade.upgrade();
        if (upgraded) {
            // Recalculate influence
            territory.setInfluenceLevel(calculateTerritoryInfluence(territory));
            
            // Save to file
            saveTerritories();
        }
        
        return upgraded;
    }
    
    /**
     * Calculates the maximum number of claims a clan can have
     * 
     * @param clan The clan
     * @return The maximum number of claims
     */
    public int calculateMaxClaims(Clan clan) {
        // Base claims (10) + (Members × 2) + (Officer Bonus) + (Alliance Bonus)
        int base = 10;
        int memberBonus = clan.getMembers().size() * 2;
        
        // Count officers (roles > 1 are officers/leaders)
        int officerCount = (int) clan.getMembers().stream()
            .filter(member -> member.getRole().getRoleLevel() > 1)
            .count();
        int officerBonus = officerCount * 3;
        
        // Alliance bonus (1 per ally)
        int allianceBonus = clan.getAlliances().size();
        
        return base + memberBonus + officerBonus + allianceBonus;
    }
    
    /**
     * Loads territories from the territories.json file
     */
    public void loadTerritories() {
        territories.clear();
        clanTerritories.clear();
        
        if (!territoryFile.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(territoryFile)) {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(reader);
            
            JSONArray territoriesArray = (JSONArray) jsonObject.get("territories");
            
            for (Object obj : territoriesArray) {
                JSONObject territoryObj = (JSONObject) obj;
                
                int chunkX = ((Long) territoryObj.get("chunkX")).intValue();
                int chunkZ = ((Long) territoryObj.get("chunkZ")).intValue();
                String worldName = (String) territoryObj.get("worldName");
                String clanName = (String) territoryObj.get("clanName");
                int influenceLevel = ((Long) territoryObj.get("influenceLevel")).intValue();
                long claimTime = (Long) territoryObj.get("claimTime");
                
                List<Flag> flags = new ArrayList<>();
                JSONArray flagsArray = (JSONArray) territoryObj.get("flags");
                
                for (Object flagObj : flagsArray) {
                    JSONObject flag = (JSONObject) flagObj;
                    
                    double x = (Double) flag.get("x");
                    double y = (Double) flag.get("y");
                    double z = (Double) flag.get("z");
                    String flagWorldName = (String) flag.get("world");
                    UUID placedBy = UUID.fromString((String) flag.get("placedBy"));
                    long placedTime = (Long) flag.get("placedTime");
                    int tier = ((Long) flag.get("tier")).intValue();
                    int influenceRadius = ((Long) flag.get("influenceRadius")).intValue();
                    
                    Location location = new Location(
                        plugin.getServer().getWorld(flagWorldName),
                        x, y, z
                    );
                    
                    flags.add(new Flag(location, placedBy, placedTime, tier, influenceRadius));
                }
                
                Territory territory = new Territory(chunkX, chunkZ, worldName, clanName, 
                                                   influenceLevel, flags, claimTime);
                
                String territoryKey = worldName + "_" + chunkX + "_" + chunkZ;
                territories.put(territoryKey, territory);
                
                // Update clan territories map
                if (!clanTerritories.containsKey(clanName)) {
                    clanTerritories.put(clanName, new ArrayList<>());
                }
                clanTerritories.get(clanName).add(territoryKey);
            }
            
        } catch (IOException | ParseException e) {
            plugin.getLogger().warning("Failed to load territories: " + e.getMessage());
        }
    }
    
    /**
     * Saves territories to the territories.json file
     */
    @SuppressWarnings("unchecked")
    public void saveTerritories() {
        JSONObject jsonObject = new JSONObject();
        JSONArray territoriesArray = new JSONArray();
        
        for (Territory territory : territories.values()) {
            JSONObject territoryObj = new JSONObject();
            
            territoryObj.put("chunkX", territory.getChunkX());
            territoryObj.put("chunkZ", territory.getChunkZ());
            territoryObj.put("worldName", territory.getWorldName());
            territoryObj.put("clanName", territory.getClanName());
            territoryObj.put("influenceLevel", territory.getInfluenceLevel());
            territoryObj.put("claimTime", territory.getClaimTime());
            
            JSONArray flagsArray = new JSONArray();
            
            for (Flag flag : territory.getFlags()) {
                JSONObject flagObj = new JSONObject();
                
                Location loc = flag.getLocation();
                flagObj.put("x", loc.getX());
                flagObj.put("y", loc.getY());
                flagObj.put("z", loc.getZ());
                flagObj.put("world", loc.getWorld().getName());
                flagObj.put("placedBy", flag.getPlacedBy().toString());
                flagObj.put("placedTime", flag.getPlacedTime());
                flagObj.put("tier", flag.getTier());
                flagObj.put("influenceRadius", flag.getInfluenceRadius());
                
                flagsArray.add(flagObj);
            }
            
            territoryObj.put("flags", flagsArray);
            territoriesArray.add(territoryObj);
        }
        
        jsonObject.put("territories", territoriesArray);
        
        try (FileWriter writer = new FileWriter(territoryFile)) {
            writer.write(jsonObject.toJSONString());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save territories: " + e.getMessage());
        }
    }
    
    /**
     * Gets the protection level for a location
     * 
     * @param location The location to check
     * @return The protection level, or null if not in a territory
     */
    public String getProtectionLevel(Location location) {
        Territory territory = getTerritory(location.getChunk());
        if (territory == null) {
            return null;
        }
        
        return territory.getProtectionLevel();
    }
    
    /**
     * Checks if a player can build at a location
     * 
     * @param player The player
     * @param location The location
     * @return True if the player can build
     */
    public boolean canBuild(Player player, Location location) {
        Territory territory = getTerritory(location.getChunk());
        if (territory == null) {
            // Not claimed, can build
            return true;
        }
        
        String clanName = territory.getClanName();
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        
        // Clan members can always build
        if (clan.isMember(player.getUniqueId())) {
            return true;
        }
        
        // Check protection level
        String protectionLevel = territory.getProtectionLevel();
        if (protectionLevel.equals("Core") || protectionLevel.equals("Secure")) {
            // Only members can build in Core or Secure territories
            return false;
        } else if (protectionLevel.equals("Contested")) {
            // In Contested territories, allies can build
            String playerClanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
            if (playerClanName != null) {
                Clan playerClan = plugin.getStorageManager().getClanStorage().getClan(playerClanName);
                return playerClan.isAlly(clanName);
            }
            return false;
        } else {
            // In Frontier territories, anyone except enemies can build
            String playerClanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
            if (playerClanName != null) {
                Clan playerClan = plugin.getStorageManager().getClanStorage().getClan(playerClanName);
                return !playerClan.isEnemy(clanName);
            }
            return true;
        }
    }
    
    /**
     * Checks if a player can interact with blocks at a location
     * 
     * @param player The player
     * @param location The location
     * @return True if the player can interact
     */
    public boolean canInteract(Player player, Location location) {
        Territory territory = getTerritory(location.getChunk());
        if (territory == null) {
            // Not claimed, can interact
            return true;
        }
        
        String clanName = territory.getClanName();
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        
        // Clan members can always interact
        if (clan.isMember(player.getUniqueId())) {
            return true;
        }
        
        // Check protection level
        String protectionLevel = territory.getProtectionLevel();
        if (protectionLevel.equals("Core")) {
            // Only members can interact in Core territories
            return false;
        } else if (protectionLevel.equals("Secure") || protectionLevel.equals("Contested")) {
            // In Secure or Contested territories, allies can interact
            String playerClanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
            if (playerClanName != null) {
                Clan playerClan = plugin.getStorageManager().getClanStorage().getClan(playerClanName);
                return playerClan.isAlly(clanName);
            }
            return false;
        } else {
            // In Frontier territories, anyone except enemies can interact
            String playerClanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
            if (playerClanName != null) {
                Clan playerClan = plugin.getStorageManager().getClanStorage().getClan(playerClanName);
                return !playerClan.isEnemy(clanName);
            }
            return true;
        }
    }
    
    /**
     * Check if PvP is enabled at a location
     * 
     * @param location The location
     * @return True if PvP is enabled
     */
    public boolean isPvpEnabled(Location location) {
        Territory territory = getTerritory(location.getChunk());
        if (territory == null) {
            // Not claimed, use server settings
            return true;
        }
        
        // PvP is only enabled in Contested and Frontier territories
        String protectionLevel = territory.getProtectionLevel();
        return protectionLevel.equals("Contested") || protectionLevel.equals("Frontier");
    }
    
    /**
     * Checks if a chunk is adjacent to an existing territory owned by the clan
     * 
     * @param chunk The chunk to check
     * @param clanName The name of the clan
     * @return True if the chunk is adjacent to existing territory
     */
    private boolean isAdjacentToExistingTerritory(Chunk chunk, String clanName) {
        // Check all 8 surrounding chunks
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                
                Chunk adjacent = chunk.getWorld().getChunkAt(chunk.getX() + dx, chunk.getZ() + dz);
                String adjacentKey = getTerritoryKey(adjacent);
                
                Territory adjacentTerritory = territories.get(adjacentKey);
                if (adjacentTerritory != null && adjacentTerritory.getClanName().equals(clanName)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if unclaiming a chunk would disconnect other territories
     * 
     * @param chunk The chunk to unclaim
     * @param clanName The name of the clan
     * @return True if unclaiming would disconnect territories
     */
    private boolean wouldDisconnectTerritory(Chunk chunk, String clanName) {
        // Get all territories owned by the clan
        List<Territory> clanTerrs = getClanTerritories(clanName);
        if (clanTerrs.size() <= 1) {
            // Only one territory, can't disconnect
            return false;
        }
        
        // Create a map of connected chunks
        Map<String, List<String>> connections = new HashMap<>();
        for (Territory t : clanTerrs) {
            String key = t.getWorldName() + "_" + t.getChunkX() + "_" + t.getChunkZ();
            connections.put(key, new ArrayList<>());
        }
        
        // Populate connections
        for (Territory t1 : clanTerrs) {
            String key1 = t1.getWorldName() + "_" + t1.getChunkX() + "_" + t1.getChunkZ();
            
            // Skip the chunk we're unclaiming
            if (t1.getChunkX() == chunk.getX() && t1.getChunkZ() == chunk.getZ() && 
                t1.getWorldName().equals(chunk.getWorld().getName())) {
                continue;
            }
            
            for (Territory t2 : clanTerrs) {
                // Skip the chunk we're unclaiming
                if (t2.getChunkX() == chunk.getX() && t2.getChunkZ() == chunk.getZ() && 
                    t2.getWorldName().equals(chunk.getWorld().getName())) {
                    continue;
                }
                
                String key2 = t2.getWorldName() + "_" + t2.getChunkX() + "_" + t2.getChunkZ();
                
                // Skip self
                if (key1.equals(key2)) {
                    continue;
                }
                
                // Check if adjacent
                int dx = Math.abs(t1.getChunkX() - t2.getChunkX());
                int dz = Math.abs(t1.getChunkZ() - t2.getChunkZ());
                
                if (t1.getWorldName().equals(t2.getWorldName()) && 
                    dx <= 1 && dz <= 1 && (dx + dz <= 2)) {
                    connections.get(key1).add(key2);
                }
            }
        }
        
        // Check if all territories are still connected using BFS
        List<String> visited = new ArrayList<>();
        List<String> queue = new ArrayList<>();
        
        // Start from the first territory (that's not the one being unclaimed)
        for (Territory t : clanTerrs) {
            if (t.getChunkX() != chunk.getX() || t.getChunkZ() != chunk.getZ() || 
                !t.getWorldName().equals(chunk.getWorld().getName())) {
                String startKey = t.getWorldName() + "_" + t.getChunkX() + "_" + t.getChunkZ();
                queue.add(startKey);
                visited.add(startKey);
                break;
            }
        }
        
        // BFS through connected territories
        while (!queue.isEmpty()) {
            String current = queue.remove(0);
            
            for (String neighbor : connections.get(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        
        // Count how many territories should be connected
        int shouldVisit = clanTerrs.size() - 1; // -1 for the one being unclaimed
        
        // If we didn't visit all territories, unclaiming would disconnect them
        return visited.size() < shouldVisit;
    }
    
    /**
     * Calculates the influence in a territory based on flags
     * 
     * @param territory The territory
     * @return The calculated influence level
     */
    private int calculateTerritoryInfluence(Territory territory) {
        // Base influence is 50
        int baseInfluence = 50;
        
        // Add influence from flags
        int flagInfluence = 0;
        for (Flag flag : territory.getFlags()) {
            // Direct flag influence is tier-based
            flagInfluence += 10 + (flag.getTier() * 5);
        }
        
        // Cap at 100
        return Math.min(100, baseInfluence + flagInfluence);
    }
    
    /**
     * Gets a unique key for a territory based on world and coordinates
     * 
     * @param chunk The chunk
     * @return A unique key string
     */
    private String getTerritoryKey(Chunk chunk) {
        return chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();
    }
    
    /**
     * Clears all territories owned by a clan
     * 
     * @param clanName The name of the clan
     */
    public void clearClanTerritories(String clanName) {
        if (!clanTerritories.containsKey(clanName)) {
            return;
        }
        
        List<String> territoryKeys = new ArrayList<>(clanTerritories.get(clanName));
        
        for (String key : territoryKeys) {
            territories.remove(key);
        }
        
        clanTerritories.remove(clanName);
        
        // Save to file
        saveTerritories();
    }
    
    /**
     * Gets territories that would be affected by a raid at a location
     * 
     * @param location The raid beacon location
     * @param raidRadius The raid radius in chunks
     * @return A list of affected territories
     */
    public List<Territory> getTerritoriesInRaidRadius(Location location, int raidRadius) {
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        String worldName = location.getWorld().getName();
        
        List<Territory> affectedTerritories = new ArrayList<>();
        
        // Check all chunks in the radius
        for (int dx = -raidRadius; dx <= raidRadius; dx++) {
            for (int dz = -raidRadius; dz <= raidRadius; dz++) {
                // Calculate distance (in chunks)
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > raidRadius) {
                    continue;
                }
                
                String key = worldName + "_" + (chunkX + dx) + "_" + (chunkZ + dz);
                Territory territory = territories.get(key);
                
                if (territory != null) {
                    affectedTerritories.add(territory);
                }
            }
        }
        
        return affectedTerritories;
    }
    
    /**
     * Initiates a raid on a territory
     * 
     * @param location The raid beacon location
     * @param raidingClanName The name of the raiding clan
     * @param raidRadius The raid radius in chunks
     * @return True if the raid was initiated successfully
     */
    public boolean initiateRaid(Location location, String raidingClanName, int raidRadius) {
        List<Territory> affectedTerritories = getTerritoriesInRaidRadius(location, raidRadius);
        
        if (affectedTerritories.isEmpty()) {
            return false;
        }
        
        // Group territories by clan
        Map<String, List<Territory>> territoriesByClan = new HashMap<>();
        
        for (Territory t : affectedTerritories) {
            if (!territoriesByClan.containsKey(t.getClanName())) {
                territoriesByClan.put(t.getClanName(), new ArrayList<>());
            }
            territoriesByClan.get(t.getClanName()).add(t);
        }
        
        // Reduce influence in all affected territories
        for (List<Territory> territories : territoriesByClan.values()) {
            for (Territory t : territories) {
                // Reduce influence by 25%
                int newInfluence = (int)(t.getInfluenceLevel() * 0.75);
                t.setInfluenceLevel(newInfluence);
            }
        }
        
        // Save changes
        saveTerritories();
        
        return true;
    }
}
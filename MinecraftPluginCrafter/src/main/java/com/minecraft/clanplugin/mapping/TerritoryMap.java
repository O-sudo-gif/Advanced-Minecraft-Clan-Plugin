package com.minecraft.clanplugin.mapping;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.Flag;
import com.minecraft.clanplugin.models.Territory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Generates and displays territory maps to players with dynamic updates.
 */
public class TerritoryMap {
    private final ClanPlugin plugin;
    private final Map<ChatColor, Character> colorSymbols;
    private final Map<ChatColor, Color> colorMapping;
    
    // Store active map views for real-time updates
    private final Map<Player, MapView> activeMapViews;
    private final Map<UUID, BukkitTask> updateTasks;
    private final Set<UUID> mapUsers;
    
    // Constants for map display
    private static final int DEFAULT_RADIUS = 16;
    private static final int DEFAULT_UPDATE_INTERVAL = 20; // 1 second in ticks
    private static final byte UNCLAIMED_COLOR = MapPalette.matchColor(200, 200, 200); // Light gray
    private static final byte BORDER_COLOR = MapPalette.matchColor(40, 40, 40); // Dark gray
    private static final byte PLAYER_COLOR = MapPalette.matchColor(255, 255, 0); // Yellow
    private static final byte ALLY_COLOR = MapPalette.matchColor(0, 255, 0); // Green for allies
    private static final byte ENEMY_COLOR = MapPalette.matchColor(255, 0, 0); // Red for enemies
    private static final byte NEUTRAL_COLOR = MapPalette.matchColor(180, 180, 255); // Light blue for neutrals
    
    // Map zoom levels
    private static final int[] ZOOM_LEVELS = {1, 2, 4, 8}; // 1:1, 1:2, 1:4, 1:8 ratios
    private final Map<UUID, Integer> playerZoomLevels;
    
    // Map visibility settings - what's shown on the map
    private final Set<UUID> showClanMembers;
    private final Set<UUID> showTerrainHeight;
    private final Set<UUID> showAllClans;
    
    /**
     * Creates a new territory map.
     * 
     * @param plugin The clan plugin instance
     */
    public TerritoryMap(ClanPlugin plugin) {
        this.plugin = plugin;
        this.colorSymbols = new HashMap<>();
        this.colorMapping = new HashMap<>();
        this.activeMapViews = new WeakHashMap<>();
        this.updateTasks = new HashMap<>();
        this.mapUsers = new HashSet<>();
        this.playerZoomLevels = new HashMap<>();
        this.showClanMembers = new HashSet<>();
        this.showTerrainHeight = new HashSet<>();
        this.showAllClans = new HashSet<>();
        
        initColorMappings();
    }
    
    /**
     * Initializes the color mappings for symbols and map rendering.
     */
    private void initColorMappings() {
        // Text symbols for chat displays
        colorSymbols.put(ChatColor.RED, '■');
        colorSymbols.put(ChatColor.BLUE, '■');
        colorSymbols.put(ChatColor.GREEN, '■');
        colorSymbols.put(ChatColor.YELLOW, '■');
        colorSymbols.put(ChatColor.LIGHT_PURPLE, '■');
        colorSymbols.put(ChatColor.AQUA, '■');
        colorSymbols.put(ChatColor.GOLD, '■');
        colorSymbols.put(ChatColor.WHITE, '■');
        colorSymbols.put(ChatColor.BLACK, '■');
        colorSymbols.put(ChatColor.GRAY, '□'); // Unclaimed territory
        
        // Map color rendering
        colorMapping.put(ChatColor.RED, new Color(200, 0, 0));
        colorMapping.put(ChatColor.BLUE, new Color(0, 0, 200));
        colorMapping.put(ChatColor.GREEN, new Color(0, 200, 0));
        colorMapping.put(ChatColor.YELLOW, new Color(200, 200, 0));
        colorMapping.put(ChatColor.LIGHT_PURPLE, new Color(200, 0, 200));
        colorMapping.put(ChatColor.AQUA, new Color(0, 200, 200));
        colorMapping.put(ChatColor.GOLD, new Color(200, 150, 0));
        colorMapping.put(ChatColor.WHITE, new Color(255, 255, 255));
        colorMapping.put(ChatColor.BLACK, new Color(0, 0, 0));
        colorMapping.put(ChatColor.GRAY, new Color(150, 150, 150));
    }
    
    /**
     * Displays a territory map to a player in chat.
     * 
     * @param player The player to display the map to
     * @param radius The radius of chunks to display around the player
     */
    public void displayTextMap(Player player, int radius) {
        Chunk centerChunk = player.getLocation().getChunk();
        int centerX = centerChunk.getX();
        int centerZ = centerChunk.getZ();
        
        player.sendMessage(ChatColor.GOLD + "=== Territory Map ===");
        player.sendMessage(ChatColor.YELLOW + "Your Location: " + ChatColor.WHITE + 
                           "X: " + centerX + ", Z: " + centerZ);
        
        // Generate compass header
        StringBuilder compass = new StringBuilder(ChatColor.GRAY + "    ");
        compass.append(ChatColor.AQUA + "N   \n");
        compass.append(ChatColor.GRAY + "    ↑   \n");
        compass.append(ChatColor.GRAY + "W ← " + ChatColor.YELLOW + "+" + ChatColor.GRAY + " → E\n");
        compass.append(ChatColor.GRAY + "    ↓   \n");
        compass.append(ChatColor.GRAY + "    " + ChatColor.AQUA + "S");
        
        player.sendMessage(compass.toString());
        
        // N = -Z, S = +Z, W = -X, E = +X in Minecraft
        
        // For each row (Z coordinate)
        for (int z = centerZ - radius; z <= centerZ + radius; z++) {
            StringBuilder row = new StringBuilder();
            
            // For each column (X coordinate)
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                if (x == centerX && z == centerZ) {
                    // Player's current position
                    row.append(ChatColor.YELLOW + "+" + ChatColor.RESET);
                    continue;
                }
                
                Chunk chunk = player.getWorld().getChunkAt(x, z);
                Territory territory = plugin.getStorageManager().getTerritoryManager().getTerritory(chunk);
                
                if (territory == null) {
                    // Unclaimed territory
                    row.append(ChatColor.GRAY + "□" + ChatColor.RESET);
                } else {
                    // Claimed territory
                    Clan clan = getClan(territory.getClanName());
                    ChatColor clanColor = getClanColor(clan);
                    char symbol = colorSymbols.getOrDefault(clanColor, '■');
                    
                    // Add the territory to the map
                    row.append(clanColor + String.valueOf(symbol) + ChatColor.RESET);
                }
            }
            
            player.sendMessage(row.toString());
        }
        
        // Display legend
        player.sendMessage(ChatColor.GOLD + "=== Legend ===");
        player.sendMessage(ChatColor.YELLOW + "+" + ChatColor.WHITE + " - Your position");
        player.sendMessage(ChatColor.GRAY + "□" + ChatColor.WHITE + " - Unclaimed territory");
        
        // Display clan colors in the legend
        Map<String, Clan> clansInView = new HashMap<>();
        
        for (int z = centerZ - radius; z <= centerZ + radius; z++) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                Chunk chunk = player.getWorld().getChunkAt(x, z);
                Territory territory = plugin.getStorageManager().getTerritoryManager().getTerritory(chunk);
                
                if (territory != null) {
                    Clan clan = getClan(territory.getClanName());
                    if (clan != null && !clansInView.containsKey(clan.getName())) {
                        clansInView.put(clan.getName(), clan);
                    }
                }
            }
        }
        
        for (Clan clan : clansInView.values()) {
            ChatColor clanColor = getClanColor(clan);
            char symbol = colorSymbols.getOrDefault(clanColor, '■');
            player.sendMessage(clanColor + String.valueOf(symbol) + ChatColor.WHITE + " - " + clan.getName());
        }
        
        // Offer the dynamic map
        player.sendMessage(ChatColor.GOLD + "Use " + ChatColor.YELLOW + "/clan territory dynamicmap" + 
                           ChatColor.GOLD + " to view a live updating map in your inventory!");
    }
    
    /**
     * Checks if a player is currently using a dynamic map.
     * 
     * @param playerUUID The UUID of the player to check
     * @return True if the player is using a map
     */
    public boolean isUsingMap(UUID playerUUID) {
        return mapUsers.contains(playerUUID);
    }
    
    /**
     * Creates and gives a dynamic territory map to a player.
     * 
     * @param player The player to give the map to
     * @return True if the map was successfully created and given
     */
    public boolean giveDynamicMap(Player player) {
        // Check if player already has an active map
        if (mapUsers.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You already have an active territory map!");
            return false;
        }
        
        try {
            // Create a new map item
            MapView view = Bukkit.createMap(player.getWorld());
            view.getRenderers().clear();
            view.addRenderer(new TerritoryMapRenderer(player));
            
            // Scale the map to see more territory
            view.setScale(MapView.Scale.NORMAL);
            
            // Create the map item
            ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
            MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
            mapMeta.setMapView(view);
            mapMeta.setDisplayName(ChatColor.GOLD + "Clan Territory Map");
            mapMeta.setLore(java.util.Arrays.asList(
                ChatColor.GRAY + "A real-time map of clan territories",
                ChatColor.YELLOW + "Hold this map to see territories update live!",
                ChatColor.RED + "Drop this map to deactivate it"
            ));
            mapItem.setItemMeta(mapMeta);
            
            // Give the map to the player
            player.getInventory().addItem(mapItem);
            
            // Register the map view for updates
            activeMapViews.put(player, view);
            mapUsers.add(player.getUniqueId());
            
            // Schedule updates
            startMapUpdates(player);
            
            player.sendMessage(ChatColor.GREEN + "Territory map activated! Hold the map to see territories in real-time.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Error creating dynamic map: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "Could not create territory map due to an error.");
            return false;
        }
    }
    
    /**
     * Starts the map update task for a player.
     * 
     * @param player The player to update the map for
     */
    private void startMapUpdates(Player player) {
        // Cancel existing task if present
        stopMapUpdates(player.getUniqueId());
        
        // Create a new update task
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !mapUsers.contains(player.getUniqueId())) {
                    stopMapUpdates(player.getUniqueId());
                    return;
                }
                
                MapView view = activeMapViews.get(player);
                if (view != null) {
                    // Force clients to update the map
                    view.setCenterX(player.getLocation().getBlockX());
                    view.setCenterZ(player.getLocation().getBlockZ());
                }
            }
        }.runTaskTimer(plugin, 0, DEFAULT_UPDATE_INTERVAL);
        
        updateTasks.put(player.getUniqueId(), task);
    }
    
    /**
     * Stops the map update task for a player.
     * 
     * @param playerId The UUID of the player
     */
    public void stopMapUpdates(UUID playerId) {
        BukkitTask task = updateTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        mapUsers.remove(playerId);
    }
    
    /**
     * Displays a detailed territory info for a specific chunk.
     * 
     * @param player The player to display the info to
     * @param chunk The chunk to display info for
     */
    public void displayTerritoryInfo(Player player, Chunk chunk) {
        Territory territory = plugin.getStorageManager().getTerritoryManager().getTerritory(chunk);
        
        player.sendMessage(ChatColor.GOLD + "=== Territory Info ===");
        player.sendMessage(ChatColor.YELLOW + "Chunk: " + ChatColor.WHITE + 
                           "X: " + chunk.getX() + ", Z: " + chunk.getZ());
        
        if (territory == null) {
            player.sendMessage(ChatColor.GRAY + "This territory is unclaimed.");
            return;
        }
        
        Clan clan = getClan(territory.getClanName());
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Error: Territory has no owner clan.");
            return;
        }
        
        player.sendMessage(ChatColor.YELLOW + "Owner: " + getClanColor(clan) + clan.getName());
        player.sendMessage(ChatColor.YELLOW + "Claimed since: " + ChatColor.WHITE + 
                           new java.util.Date(territory.getClaimTime()).toString());
        
        // Display influence and protection level
        player.sendMessage(ChatColor.YELLOW + "Influence: " + ChatColor.WHITE + territory.getInfluenceLevel() + "%");
        
        // Protection level based on influence
        String protectionLevel = territory.getProtectionLevel();
        ChatColor protectionColor = getProtectionLevelColor(territory.getInfluenceLevel());
        
        player.sendMessage(ChatColor.YELLOW + "Protection: " + protectionColor + protectionLevel);
        
        // Display flag count
        int flagCount = territory.getFlags().size();
        player.sendMessage(ChatColor.YELLOW + "Flags: " + ChatColor.WHITE + flagCount);
        
        // Display borders
        boolean isNorthBorder = !isClaimed(chunk.getWorld().getChunkAt(chunk.getX(), chunk.getZ() - 1), clan.getName());
        boolean isSouthBorder = !isClaimed(chunk.getWorld().getChunkAt(chunk.getX(), chunk.getZ() + 1), clan.getName());
        boolean isWestBorder = !isClaimed(chunk.getWorld().getChunkAt(chunk.getX() - 1, chunk.getZ()), clan.getName());
        boolean isEastBorder = !isClaimed(chunk.getWorld().getChunkAt(chunk.getX() + 1, chunk.getZ()), clan.getName());
        
        if (isNorthBorder || isSouthBorder || isWestBorder || isEastBorder) {
            StringBuilder borderInfo = new StringBuilder(ChatColor.YELLOW + "Borders: " + ChatColor.WHITE);
            
            if (isNorthBorder) borderInfo.append("North ");
            if (isEastBorder) borderInfo.append("East ");
            if (isSouthBorder) borderInfo.append("South ");
            if (isWestBorder) borderInfo.append("West");
            
            player.sendMessage(borderInfo.toString());
        }
    }
    
    /**
     * Checks if a chunk is claimed by a specific clan.
     * 
     * @param chunk The chunk to check
     * @param clanName The name of the clan to check ownership for
     * @return True if the chunk is claimed by the clan
     */
    private boolean isClaimed(Chunk chunk, String clanName) {
        Territory territory = plugin.getStorageManager().getTerritoryManager().getTerritory(chunk);
        
        if (territory == null) {
            return false;
        }
        
        return territory.getClanName().equals(clanName);
    }
    
    /**
     * Gets a clan by name.
     * 
     * @param clanName The name of the clan
     * @return The clan, or null if not found
     */
    private Clan getClan(String clanName) {
        return plugin.getStorageManager().getClanStorage().getClan(clanName);
    }
    
    /**
     * Gets the color of a clan.
     * 
     * @param clan The clan to check
     * @return The clan's color, or GRAY if not set
     */
    /**
     * Save all territory data to file.
     * This method is called when the server shuts down to
     * ensure all territory information is persisted.
     */
    public void saveAllTerritories() {
        if (plugin.getStorageManager() != null && 
            plugin.getStorageManager().getTerritoryManager() != null) {
            plugin.getStorageManager().getTerritoryManager().saveAllTerritories();
            plugin.getLogger().info("All territory data has been saved.");
        }
    }
    
    /**
     * Cycles through available zoom levels for a player's map.
     * 
     * @param player The player whose map zoom to change
     * @return The new zoom level descriptor (e.g., "1:2")
     */
    public String cycleZoomLevel(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Initialize zoom level if not set
        if (!playerZoomLevels.containsKey(playerId)) {
            playerZoomLevels.put(playerId, 0); // Default zoom level
        }
        
        // Get current and next zoom level index
        int currentIndex = playerZoomLevels.get(playerId);
        int nextIndex = (currentIndex + 1) % ZOOM_LEVELS.length;
        
        // Update zoom level
        playerZoomLevels.put(playerId, nextIndex);
        
        // Return the zoom description
        return "1:" + ZOOM_LEVELS[nextIndex];
    }
    
    /**
     * Gets the current zoom level for a player.
     * 
     * @param player The player to check
     * @return The zoom level factor
     */
    public int getZoomLevel(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Return default if not set
        if (!playerZoomLevels.containsKey(playerId)) {
            playerZoomLevels.put(playerId, 0);
            return ZOOM_LEVELS[0];
        }
        
        int index = playerZoomLevels.get(playerId);
        return ZOOM_LEVELS[index];
    }
    
    /**
     * Toggles clan member display on the map for a player.
     * 
     * @param player The player to toggle clan member display for
     * @return True if clan member display is now enabled, false if disabled
     */
    public boolean toggleClanMemberDisplay(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (showClanMembers.contains(playerId)) {
            showClanMembers.remove(playerId);
            return false;
        } else {
            showClanMembers.add(playerId);
            return true;
        }
    }
    
    /**
     * Toggles terrain height display on the map for a player.
     * 
     * @param player The player to toggle terrain height display for
     * @return True if terrain height display is now enabled, false if disabled
     */
    public boolean toggleTerrainHeightDisplay(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (showTerrainHeight.contains(playerId)) {
            showTerrainHeight.remove(playerId);
            return false;
        } else {
            showTerrainHeight.add(playerId);
            return true;
        }
    }
    
    /**
     * Toggles display of all clans on the map for a player.
     * 
     * @param player The player to toggle all clans display for
     * @return True if all clans display is now enabled, false if disabled
     */
    public boolean toggleAllClansDisplay(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (showAllClans.contains(playerId)) {
            showAllClans.remove(playerId);
            return false;
        } else {
            showAllClans.add(playerId);
            return true;
        }
    }
    
    /**
     * Checks if clan member display is enabled for a player.
     * 
     * @param player The player to check
     * @return True if clan member display is enabled
     */
    public boolean isShowingClanMembers(Player player) {
        return showClanMembers.contains(player.getUniqueId());
    }
    
    /**
     * Checks if terrain height display is enabled for a player.
     * 
     * @param player The player to check
     * @return True if terrain height display is enabled
     */
    public boolean isShowingTerrainHeight(Player player) {
        return showTerrainHeight.contains(player.getUniqueId());
    }
    
    /**
     * Checks if all clans display is enabled for a player.
     * 
     * @param player The player to check
     * @return True if all clans display is enabled
     */
    public boolean isShowingAllClans(Player player) {
        return showAllClans.contains(player.getUniqueId());
    }
    
    /**
     * Shows online clan members on the territory map.
     * 
     * @param player The player to show the map to
     * @param clan The clan whose members to show
     */
    public void showClanMembersOnMap(Player player, Clan clan) {
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return;
        }
        
        StringBuilder message = new StringBuilder(ChatColor.GOLD + "=== Clan Members on Map ===\n");
        
        // Get player's chunk for reference
        Chunk playerChunk = player.getLocation().getChunk();
        int playerX = playerChunk.getX();
        int playerZ = playerChunk.getZ();
        
        // Loop through online clan members
        boolean foundMembers = false;
        
        for (ClanMember member : clan.getMembers()) {
            Player onlineMember = Bukkit.getPlayer(member.getPlayerUUID());
            
            if (onlineMember != null && onlineMember.isOnline()) {
                // Skip the player themselves
                if (onlineMember.equals(player)) {
                    continue;
                }
                
                Chunk memberChunk = onlineMember.getLocation().getChunk();
                int memberX = memberChunk.getX();
                int memberZ = memberChunk.getZ();
                
                // Calculate distance in chunks
                int distanceX = memberX - playerX;
                int distanceZ = memberZ - playerZ;
                double distance = Math.sqrt(distanceX * distanceX + distanceZ * distanceZ);
                
                // Calculate direction
                String direction = getDirectionString(distanceX, distanceZ);
                
                message.append(ChatColor.YELLOW).append(onlineMember.getName())
                      .append(ChatColor.WHITE).append(" - ")
                      .append(Math.round(distance)).append(" chunks ")
                      .append(direction).append("\n");
                
                foundMembers = true;
            }
        }
        
        if (!foundMembers) {
            message.append(ChatColor.GRAY).append("No clan members are currently online.");
        }
        
        player.sendMessage(message.toString());
    }
    
    /**
     * Gets a string representation of a direction based on X and Z distances.
     * 
     * @param dx The X distance
     * @param dz The Z distance
     * @return A string representing the direction (N, NE, E, etc.)
     */
    private String getDirectionString(int dx, int dz) {
        if (dx == 0 && dz == 0) {
            return "here";
        }
        
        // Calculate the angle in degrees (0 = East, 90 = North, etc.)
        double angle = Math.toDegrees(Math.atan2(-dz, dx));
        if (angle < 0) {
            angle += 360;
        }
        
        // Convert angle to direction
        if (angle >= 337.5 || angle < 22.5) {
            return "East";
        } else if (angle >= 22.5 && angle < 67.5) {
            return "Northeast";
        } else if (angle >= 67.5 && angle < 112.5) {
            return "North";
        } else if (angle >= 112.5 && angle < 157.5) {
            return "Northwest";
        } else if (angle >= 157.5 && angle < 202.5) {
            return "West";
        } else if (angle >= 202.5 && angle < 247.5) {
            return "Southwest";
        } else if (angle >= 247.5 && angle < 292.5) {
            return "South";
        } else {
            return "Southeast";
        }
    }

    private ChatColor getClanColor(Clan clan) {
        if (clan == null) {
            return ChatColor.GRAY;
        }
        
        String colorStr = clan.getColor();
        if (colorStr == null || colorStr.isEmpty()) {
            return ChatColor.GRAY;
        }
        
        try {
            // Try to parse the color string to a ChatColor
            return ChatColor.valueOf(colorStr);
        } catch (IllegalArgumentException e) {
            // If the color string is not a valid ChatColor, return a default
            return ChatColor.GRAY;
        }
    }
    
    /**
     * Gets the color for a protection level based on influence.
     * 
     * @param influence The influence value
     * @return The protection level color
     */
    private ChatColor getProtectionLevelColor(int influence) {
        ConfigurationSection protectionSection = 
            plugin.getConfig().getConfigurationSection("territory.protection");
        
        if (protectionSection == null) {
            // Default thresholds if not configured
            if (influence >= 75) return ChatColor.DARK_RED;
            if (influence >= 50) return ChatColor.RED;
            if (influence >= 25) return ChatColor.GOLD;
            if (influence >= 1) return ChatColor.YELLOW;
            return ChatColor.GRAY;
        }
        
        if (influence >= protectionSection.getInt("core", 75)) return ChatColor.DARK_RED;
        if (influence >= protectionSection.getInt("secure", 50)) return ChatColor.RED;
        if (influence >= protectionSection.getInt("contested", 25)) return ChatColor.GOLD;
        if (influence >= protectionSection.getInt("frontier", 1)) return ChatColor.YELLOW;
        return ChatColor.GRAY;
    }
    
    /**
     * Updates the appearance of the visual territory border blocks for a chunk.
     * 
     * @param chunk The chunk to update
     * @param clan The clan that owns the territory
     */
    public void updateTerritoryBorders(Chunk chunk, Clan clan) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    World world = chunk.getWorld();
                    int chunkX = chunk.getX() << 4; // Multiply by 16
                    int chunkZ = chunk.getZ() << 4; // Multiply by 16
                    
                    // Check adjacent chunks
                    boolean isNorthBorder = !isClaimed(world.getChunkAt(chunk.getX(), chunk.getZ() - 1), clan.getName());
                    boolean isSouthBorder = !isClaimed(world.getChunkAt(chunk.getX(), chunk.getZ() + 1), clan.getName());
                    boolean isWestBorder = !isClaimed(world.getChunkAt(chunk.getX() - 1, chunk.getZ()), clan.getName());
                    boolean isEastBorder = !isClaimed(world.getChunkAt(chunk.getX() + 1, chunk.getZ()), clan.getName());
                    
                    // Only create border markers if this is a border chunk
                    if (!isNorthBorder && !isSouthBorder && !isWestBorder && !isEastBorder) {
                        return;
                    }
                    
                    // For each border, create marker blocks at corners and middle
                    if (isNorthBorder) {
                        createBorderMarker(world, chunkX, chunkZ);
                        createBorderMarker(world, chunkX + 7, chunkZ);
                        createBorderMarker(world, chunkX + 15, chunkZ);
                    }
                    
                    if (isSouthBorder) {
                        createBorderMarker(world, chunkX, chunkZ + 15);
                        createBorderMarker(world, chunkX + 7, chunkZ + 15);
                        createBorderMarker(world, chunkX + 15, chunkZ + 15);
                    }
                    
                    if (isWestBorder) {
                        createBorderMarker(world, chunkX, chunkZ);
                        createBorderMarker(world, chunkX, chunkZ + 7);
                        createBorderMarker(world, chunkX, chunkZ + 15);
                    }
                    
                    if (isEastBorder) {
                        createBorderMarker(world, chunkX + 15, chunkZ);
                        createBorderMarker(world, chunkX + 15, chunkZ + 7);
                        createBorderMarker(world, chunkX + 15, chunkZ + 15);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error updating territory borders: " + e.getMessage());
                }
            }
        }.runTask(plugin);
    }
    
    /**
     * Creates a border marker at the specified location.
     * 
     * @param world The world
     * @param x The x coordinate
     * @param z The z coordinate
     */
    private void createBorderMarker(World world, int x, int z) {
        // Find the highest non-air block
        int y = world.getHighestBlockYAt(x, z);
        Block block = world.getBlockAt(x, y, z);
        
        // Skip if it's a block we shouldn't replace
        Material type = block.getType();
        if (type.isSolid() && !type.equals(Material.GRASS) && !type.equals(Material.DIRT) && 
            !type.equals(Material.STONE) && !type.equals(Material.SAND)) {
            return;
        }
        
        // Create a temporary border marker
        boolean configEnabled = plugin.getConfig().getBoolean("territory.visual_borders", true);
        if (configEnabled) {
            // Place a particle effect or glowing block
            // This is a placeholder - in a real implementation, you'd use persistent markers
            // or particle effects that are refreshed periodically
        }
    }
    
    /**
     * Custom map renderer for territory maps.
     */
    private class TerritoryMapRenderer extends MapRenderer {
        private final UUID playerUUID;
        private int lastX = -999999;
        private int lastZ = -999999;
        
        /**
         * Creates a new territory map renderer for a specific player.
         * 
         * @param player The player to render the map for
         */
        public TerritoryMapRenderer(Player player) {
            super(true); // Always contextual
            this.playerUUID = player.getUniqueId();
        }
        
        @Override
        public void render(MapView map, MapCanvas canvas, Player player) {
            if (!player.getUniqueId().equals(playerUUID)) {
                return; // Only render for the right player
            }
            
            // Get player location
            Location location = player.getLocation();
            int centerX = location.getBlockX();
            int centerZ = location.getBlockZ();
            
            // Get current zoom level
            int zoomLevel = getZoomLevel(player);
            
            // Don't re-render if position hasn't changed enough (based on zoom level)
            int minMovement = 4 * zoomLevel; // Allow less frequent updates at higher zoom levels
            if (Math.abs(centerX - lastX) < minMovement && Math.abs(centerZ - lastZ) < minMovement) {
                return;
            }
            
            lastX = centerX;
            lastZ = centerZ;
            
            // Center the map on player
            map.setCenterX(centerX);
            map.setCenterZ(centerZ);
            
            // Clear the map with a background color
            for (int x = 0; x < 128; x++) {
                for (int z = 0; z < 128; z++) {
                    canvas.setPixel(x, z, UNCLAIMED_COLOR);
                }
            }
            
            // Calculate the chunk range to render based on zoom level
            World world = player.getWorld();
            int mapWidth = 128;
            int mapRadius = mapWidth / 2;
            
            // Adjust chunk radius based on zoom level - higher zoom = more chunks visible
            int chunkRadius = ((mapRadius / 16) + 1) * zoomLevel;
            
            // Get player's clan for highlighting
            String playerClanName = null;
            Clan playerClan = plugin.getStorageManager().getClanStorage().getPlayerClan(player.getUniqueId());
            if (playerClan != null) {
                playerClanName = playerClan.getName();
            }
            
            // Draw each chunk's territory
            for (int chunkX = centerX / 16 - chunkRadius; chunkX <= centerX / 16 + chunkRadius; chunkX++) {
                for (int chunkZ = centerZ / 16 - chunkRadius; chunkZ <= centerZ / 16 + chunkRadius; chunkZ++) {
                    renderChunk(canvas, world, chunkX, chunkZ, centerX, centerZ, playerClanName);
                }
            }
            
            // Draw player position
            int playerPixelX = mapWidth / 2;
            int playerPixelZ = mapWidth / 2;
            
            // 3x3 marker for player position
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    int pixelX = playerPixelX + x;
                    int pixelZ = playerPixelZ + z;
                    
                    if (pixelX >= 0 && pixelX < mapWidth && pixelZ >= 0 && pixelZ < mapWidth) {
                        canvas.setPixel(pixelX, pixelZ, PLAYER_COLOR);
                    }
                }
            }
            
            // Add cursors for important landmarks
            addMapCursors(map, canvas, player, centerX, centerZ);
        }
        
        /**
         * Renders a single chunk on the map.
         */
        private void renderChunk(MapCanvas canvas, World world, int chunkX, int chunkZ, 
                                int centerX, int centerZ, String playerClanName) {
            
            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            Territory territory = plugin.getStorageManager().getTerritoryManager().getTerritory(chunk);
            
            // Get current zoom level
            int zoomLevel = getZoomLevel(Bukkit.getPlayer(playerUUID));
            
            // Calculate pixel coordinates - adjust for zoom level
            int pixelStartX = 64 + ((chunkX * 16) - centerX) / (2 * zoomLevel);
            int pixelStartZ = 64 + ((chunkZ * 16) - centerZ) / (2 * zoomLevel);
            
            // Adjust pixel size based on zoom level - smaller chunks at higher zoom
            int pixelSize = Math.max(1, 8 / zoomLevel); // Minimum 1 pixel
            
            if (pixelStartX < -pixelSize || pixelStartX >= 128 || 
                pixelStartZ < -pixelSize || pixelStartZ >= 128) {
                return; // Skip if outside map
            }
            
            // Draw the chunk
            byte color = UNCLAIMED_COLOR;
            
            if (territory != null) {
                // Claimed territory
                Clan clan = getClan(territory.getClanName());
                
                if (clan != null) {
                    Color awtColor = colorMapping.getOrDefault(getClanColor(clan), new Color(150, 150, 150));
                    color = MapPalette.matchColor(awtColor);
                    
                    // Special rendering for player's clan
                    if (playerClanName != null && playerClanName.equals(clan.getName())) {
                        // Make player's clan territory slightly brighter
                        color = MapPalette.matchColor(
                            Math.min(255, awtColor.getRed() + 30), 
                            Math.min(255, awtColor.getGreen() + 30), 
                            Math.min(255, awtColor.getBlue() + 30)
                        );
                    }
                }
            }
            
            // Fill the chunk area
            for (int x = 0; x < pixelSize; x++) {
                for (int z = 0; z < pixelSize; z++) {
                    int pixelX = pixelStartX + x;
                    int pixelZ = pixelStartZ + z;
                    
                    if (pixelX >= 0 && pixelX < 128 && pixelZ >= 0 && pixelZ < 128) {
                        // Draw border if it's the edge of the chunk
                        if (x == 0 || x == pixelSize - 1 || z == 0 || z == pixelSize - 1) {
                            canvas.setPixel(pixelX, pixelZ, BORDER_COLOR);
                        } else {
                            canvas.setPixel(pixelX, pixelZ, color);
                        }
                    }
                }
            }
        }
        
        /**
         * Adds map cursors for important locations.
         */
        private void addMapCursors(MapView map, MapCanvas canvas, Player player, int centerX, int centerZ) {
            MapCursorCollection cursors = new MapCursorCollection();
            
            // Get current zoom level to scale cursor positions
            int zoomLevel = getZoomLevel(player);
            
            // Add clan homes, flags, etc. as cursors
            String playerClanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
            
            if (playerClanName != null) {
                Clan playerClan = getClan(playerClanName);
                
                if (playerClan != null) {
                    // Add clan home if it exists
                    if (playerClan.getHome() != null) {
                        Location home = playerClan.getHome();
                        
                        byte cursorType = MapCursor.Type.RED_MARKER.getValue();
                        int cursorX = (int) (128 * (home.getBlockX() - centerX) / (128 * 2 * zoomLevel));
                        int cursorZ = (int) (128 * (home.getBlockZ() - centerZ) / (128 * 2 * zoomLevel));
                        
                        if (cursorX >= -128 && cursorX <= 127 && cursorZ >= -128 && cursorZ <= 127) {
                            cursors.addCursor(cursorX, cursorZ, (byte) 0, cursorType, true);
                        }
                    }
                    
                    // Add clan members as cursors (blue)
                    for (ClanMember member : playerClan.getMembers()) {
                        // Skip the current player - they're already shown as the center
                        if (member.getPlayerUUID().equals(player.getUniqueId())) {
                            continue;
                        }
                        
                        // Get online player
                        Player memberPlayer = Bukkit.getPlayer(member.getPlayerUUID());
                        
                        if (memberPlayer != null && memberPlayer.isOnline() && 
                            memberPlayer.getWorld().equals(player.getWorld())) {
                            
                            Location memberLoc = memberPlayer.getLocation();
                            byte cursorType = MapCursor.Type.BLUE_POINTER.getValue();
                            
                            int cursorX = (int) (128 * (memberLoc.getBlockX() - centerX) / (128 * 2 * zoomLevel));
                            int cursorZ = (int) (128 * (memberLoc.getBlockZ() - centerZ) / (128 * 2 * zoomLevel));
                            
                            // Calculate rotation angle (0-15) based on player yaw
                            byte rotation = (byte) ((int) (memberLoc.getYaw() / 22.5) & 0xF);
                            
                            if (cursorX >= -128 && cursorX <= 127 && cursorZ >= -128 && cursorZ <= 127) {
                                cursors.addCursor(cursorX, cursorZ, rotation, cursorType, true);
                            }
                        }
                    }
                }
            }
            
            // Add territory flags as green cursors
            int searchRadius = 32 * zoomLevel; // Bigger search at higher zoom levels
            for (int chunkX = centerX / 16 - searchRadius / 16; chunkX <= centerX / 16 + searchRadius / 16; chunkX++) {
                for (int chunkZ = centerZ / 16 - searchRadius / 16; chunkZ <= centerZ / 16 + searchRadius / 16; chunkZ++) {
                    Chunk chunk = player.getWorld().getChunkAt(chunkX, chunkZ);
                    Territory territory = plugin.getStorageManager().getTerritoryManager().getTerritory(chunk);
                    
                    if (territory != null && !territory.getFlags().isEmpty()) {
                        for (Flag flag : territory.getFlags()) {
                            Location flagLoc = flag.getLocation();
                            byte cursorType = MapCursor.Type.GREEN_POINTER.getValue();
                            
                            int cursorX = (int) (128 * (flagLoc.getBlockX() - centerX) / (128 * 2 * zoomLevel));
                            int cursorZ = (int) (128 * (flagLoc.getBlockZ() - centerZ) / (128 * 2 * zoomLevel));
                            
                            if (cursorX >= -128 && cursorX <= 127 && cursorZ >= -128 && cursorZ <= 127) {
                                cursors.addCursor(cursorX, cursorZ, (byte) 0, cursorType, true);
                            }
                        }
                    }
                }
            }
            
            // Set the cursors on the map
            canvas.setCursors(cursors);
        }
    }
}
package com.minecraft.clanplugin.utils;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.Territory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Utility class for handling dynamic territory effects.
 */
public class TerritoryEffectUtils {
    
    private static ClanPlugin plugin;
    private static final Map<UUID, Set<BukkitTask>> playerEffectTasks = new HashMap<>();
    private static final Map<String, Long> territoryLastVisualized = new HashMap<>();
    private static final long VISUALIZATION_COOLDOWN = 5000; // 5 seconds cooldown
    
    /**
     * Initializes territory effect utilities with plugin instance.
     * 
     * @param pluginInstance The plugin instance
     */
    public static void init(ClanPlugin pluginInstance) {
        plugin = pluginInstance;
    }
    
    /**
     * Shows a border effect around a territory chunk for a player.
     * 
     * @param player The player to show the effect to
     * @param territory The territory to visualize
     * @param duration The duration of the effect in seconds
     */
    public static void showTerritoryBorder(Player player, Territory territory, int duration) {
        // Get clan for color
        Clan clan = plugin.getStorageManager().getClan(territory.getClanName());
        if (clan == null) return;
        
        // Get territory location
        World world = Bukkit.getWorld(territory.getWorldName());
        if (world == null) return;
        
        int chunkX = territory.getChunkX();
        int chunkZ = territory.getChunkZ();
        
        // Convert to world coordinates
        int startX = chunkX << 4; // multiply by 16
        int startZ = chunkZ << 4; // multiply by 16
        int endX = startX + 16;
        int endZ = startZ + 16;
        
        // Get territory ID for cooldown tracking
        String territoryId = territory.getWorldName() + ":" + chunkX + ":" + chunkZ;
        
        // Check cooldown
        long now = System.currentTimeMillis();
        if (territoryLastVisualized.containsKey(territoryId)) {
            long lastTime = territoryLastVisualized.get(territoryId);
            if (now - lastTime < VISUALIZATION_COOLDOWN) {
                return; // Still on cooldown
            }
        }
        
        // Update last visualized time
        territoryLastVisualized.put(territoryId, now);
        
        // Color based on clan color
        Color particleColor = parseColor(clan.getColor());
        DustOptions dustOptions = new DustOptions(particleColor, 1.0f);
        
        // List of tasks for this visualization
        Set<BukkitTask> tasks = new HashSet<>();
        UUID playerUuid = player.getUniqueId();
        
        // Cancel existing tasks for this player
        cancelEffects(playerUuid);
        
        // Create a task that runs every 5 ticks (0.25 seconds)
        BukkitTask borderTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Check if player is still online and in the same world
            if (!player.isOnline() || !player.getWorld().equals(world)) {
                cancelEffects(playerUuid);
                return;
            }
            
            // If player is too far, don't show particles
            Location playerLoc = player.getLocation();
            int playerChunkX = playerLoc.getBlockX() >> 4;
            int playerChunkZ = playerLoc.getBlockZ() >> 4;
            
            // Only show if player is within 5 chunks
            if (Math.abs(playerChunkX - chunkX) > 5 || Math.abs(playerChunkZ - chunkZ) > 5) {
                return;
            }
            
            // Draw boundary particles at y level near player
            int y = Math.max(0, Math.min(255, playerLoc.getBlockY()));
            
            // Draw animated border - north and south edges
            for (int x = startX; x <= endX; x += 2) {
                showBoundaryParticle(player, world, x, y, startZ, dustOptions);
                showBoundaryParticle(player, world, x, y, endZ, dustOptions);
            }
            
            // Draw animated border - east and west edges
            for (int z = startZ + 2; z < endZ; z += 2) {
                showBoundaryParticle(player, world, startX, y, z, dustOptions);
                showBoundaryParticle(player, world, endX, y, z, dustOptions);
            }
            
            // Add corner pillars for a more 3D effect
            for (int yOffset = -2; yOffset <= 2; yOffset++) {
                int displayY = y + yOffset;
                if (displayY < 0 || displayY > 255) continue;
                
                showBoundaryParticle(player, world, startX, displayY, startZ, dustOptions);
                showBoundaryParticle(player, world, endX, displayY, startZ, dustOptions);
                showBoundaryParticle(player, world, startX, displayY, endZ, dustOptions);
                showBoundaryParticle(player, world, endX, displayY, endZ, dustOptions);
            }
        }, 0L, 5L);
        
        tasks.add(borderTask);
        
        // Add flag indicator at center
        BukkitTask flagTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Check if player is still online and in the same world
            if (!player.isOnline() || !player.getWorld().equals(world)) {
                cancelEffects(playerUuid);
                return;
            }
            
            // If player is too far, don't show particles
            Location playerLoc = player.getLocation();
            int playerChunkX = playerLoc.getBlockX() >> 4;
            int playerChunkZ = playerLoc.getBlockZ() >> 4;
            
            // Only show if player is within 5 chunks
            if (Math.abs(playerChunkX - chunkX) > 5 || Math.abs(playerChunkZ - chunkZ) > 5) {
                return;
            }
            
            // Calculate center of chunk
            int centerX = startX + 8;
            int centerZ = startZ + 8;
            int y = Math.max(0, Math.min(255, playerLoc.getBlockY()));
            
            // Create animated flag/banner effect
            long time = System.currentTimeMillis();
            double animationPhase = (time % 2000) / 2000.0 * Math.PI * 2;
            
            for (int yOffset = 0; yOffset < 5; yOffset++) {
                int displayY = y + yOffset;
                if (displayY < 0 || displayY > 255) continue;
                
                // Calculate wave effect based on height
                double xOffset = Math.sin(animationPhase + yOffset * 0.5) * 0.5;
                
                Location particleLoc = new Location(world, centerX + xOffset, displayY, centerZ);
                player.spawnParticle(Particle.REDSTONE, particleLoc, 1, 0, 0, 0, 0, dustOptions);
            }
        }, 0L, 3L);
        
        tasks.add(flagTask);
        
        // Store tasks for player
        playerEffectTasks.put(playerUuid, tasks);
        
        // Schedule task cancellation after duration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            cancelEffects(playerUuid);
        }, duration * 20L); // Convert seconds to ticks
    }
    
    /**
     * Shows a territory visualization effect for an entire clan's territory.
     * 
     * @param player The player to show the effect to
     * @param clan The clan whose territory to visualize
     * @param duration The duration of the effect in seconds
     */
    public static void showClanTerritory(Player player, Clan clan, int duration) {
        List<Territory> territories = plugin.getStorageManager().getTerritoryManager().getClanTerritories(clan.getName());
        
        if (territories.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Clan doesn't have any territories to visualize.");
            return;
        }
        
        // Schedule territory visualization with delays to spread out effects
        List<BukkitTask> tasks = new ArrayList<>();
        UUID playerUuid = player.getUniqueId();
        
        // Cancel existing effects
        cancelEffects(playerUuid);
        
        // Show message
        player.sendMessage(ChatColor.GREEN + "Visualizing " + territories.size() + 
                " territory chunks for clan " + clan.getName() + "...");
        
        // Create a set for tasks
        Set<BukkitTask> effectTasks = new HashSet<>();
        
        // Visualize each territory with a small delay between them
        for (int i = 0; i < territories.size(); i++) {
            Territory territory = territories.get(i);
            int delay = i * 2; // 2 ticks delay between each territory
            
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    showTerritoryBorder(player, territory, duration);
                }
            }, delay);
            
            effectTasks.add(task);
        }
        
        // Store tasks for this player
        playerEffectTasks.put(playerUuid, effectTasks);
        
        // Schedule automatic cleanup
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            cancelEffects(playerUuid);
        }, duration * 20L + territories.size() * 2); // Account for spawn delays
    }
    
    /**
     * Shows a single boundary particle at the specified location.
     * 
     * @param player The player to show the particle to
     * @param world The world to show the particle in
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @param z The z-coordinate
     * @param dustOptions The particle dust options
     */
    private static void showBoundaryParticle(Player player, World world, int x, int y, int z, DustOptions dustOptions) {
        Location loc = new Location(world, x + 0.5, y, z + 0.5);
        player.spawnParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0, dustOptions);
    }
    
    /**
     * Cancels all effects for a player.
     * 
     * @param playerUuid The UUID of the player
     */
    public static void cancelEffects(UUID playerUuid) {
        Set<BukkitTask> tasks = playerEffectTasks.get(playerUuid);
        if (tasks != null) {
            tasks.forEach(BukkitTask::cancel);
            playerEffectTasks.remove(playerUuid);
        }
    }
    
    /**
     * Parses a ChatColor string into a Bukkit Color for particles.
     * 
     * @param colorStr The color string to parse
     * @return The Bukkit Color object
     */
    private static Color parseColor(String colorStr) {
        try {
            // Convert color code (§6) to ChatColor name (GOLD)
            String chatColorName = colorStr.replace("§", "").toUpperCase();
            ChatColor chatColor = ChatColor.valueOf(chatColorName);
            
            switch (chatColor) {
                case BLACK: return Color.BLACK;
                case DARK_BLUE: return Color.NAVY;
                case DARK_GREEN: return Color.GREEN;
                case DARK_AQUA: return Color.TEAL;
                case DARK_RED: return Color.MAROON;
                case DARK_PURPLE: return Color.PURPLE;
                case GOLD: return Color.ORANGE;
                case GRAY: return Color.SILVER;
                case DARK_GRAY: return Color.GRAY;
                case BLUE: return Color.BLUE;
                case GREEN: return Color.LIME;
                case AQUA: return Color.AQUA;
                case RED: return Color.RED;
                case LIGHT_PURPLE: return Color.FUCHSIA;
                case YELLOW: return Color.YELLOW;
                case WHITE: return Color.WHITE;
                default: return Color.WHITE;
            }
        } catch (Exception e) {
            // Default to gold if parsing fails
            return Color.ORANGE;
        }
    }
}
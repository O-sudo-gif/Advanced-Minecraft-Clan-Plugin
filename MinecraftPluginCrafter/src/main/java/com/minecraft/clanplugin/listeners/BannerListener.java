package com.minecraft.clanplugin.listeners;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.hologram.BannerManager;
import com.minecraft.clanplugin.hologram.ClanBanner;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles events related to holographic banners.
 */
public class BannerListener implements Listener {

    private final ClanPlugin plugin;
    private final Map<String, List<UUID>> chunkBannerMap;
    
    /**
     * Create a new banner listener.
     * 
     * @param plugin The plugin instance
     */
    public BannerListener(ClanPlugin plugin) {
        this.plugin = plugin;
        this.chunkBannerMap = new HashMap<>();
    }
    
    /**
     * Handle chunk load events to spawn banners in loaded chunks.
     * 
     * @param event The chunk load event
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        String chunkKey = getChunkKey(chunk);
        
        BannerManager bannerManager = plugin.getBannerManager();
        Map<UUID, ClanBanner> banners = bannerManager.getAllBanners();
        
        // Spawn banners that are in this chunk
        for (ClanBanner banner : banners.values()) {
            Location bannerLoc = banner.getLocation();
            
            if (isLocationInChunk(bannerLoc, chunk) && banner.isVisible()) {
                banner.spawn();
            }
        }
    }
    
    /**
     * Handle chunk unload events to despawn banners in unloaded chunks.
     * 
     * @param event The chunk unload event
     */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        String chunkKey = getChunkKey(chunk);
        
        BannerManager bannerManager = plugin.getBannerManager();
        Map<UUID, ClanBanner> banners = bannerManager.getAllBanners();
        
        // Despawn banners that are in this chunk
        for (ClanBanner banner : banners.values()) {
            Location bannerLoc = banner.getLocation();
            
            if (isLocationInChunk(bannerLoc, chunk)) {
                // Just remove the armor stands; don't call despawn() as that clears the IDs
                for (UUID armorStandId : banner.getArmorStandIds()) {
                    Entity entity = findEntityById(chunk.getWorld(), armorStandId);
                    if (entity != null) {
                        entity.remove();
                    }
                }
            }
        }
    }
    
    /**
     * Handle entity damage events to prevent damage to banner armor stands.
     * 
     * @param event The entity damage event
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        
        // Cancel damage to banner armor stands (they should be invulnerable already, but just in case)
        if (entity instanceof ArmorStand) {
            ArmorStand stand = (ArmorStand) entity;
            if (entity.isInvulnerable() && !stand.isVisible()) {
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * Handle player interactions with armor stands to prevent manipulation.
     * 
     * @param event The player interact at entity event
     */
    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        
        // Cancel interactions with banner armor stands
        if (entity instanceof ArmorStand) {
            ArmorStand stand = (ArmorStand) entity;
            if (entity.isInvulnerable() && !stand.isVisible()) {
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * Find an entity by its UUID.
     * 
     * @param world The world to search in
     * @param entityId The entity UUID
     * @return The entity, or null if not found
     */
    private Entity findEntityById(World world, UUID entityId) {
        for (Entity entity : world.getEntities()) {
            if (entity.getUniqueId().equals(entityId)) {
                return entity;
            }
        }
        return null;
    }
    
    /**
     * Check if a location is in a chunk.
     * 
     * @param location The location
     * @param chunk The chunk
     * @return True if the location is in the chunk
     */
    private boolean isLocationInChunk(Location location, Chunk chunk) {
        return location != null && 
               location.getWorld() != null && 
               location.getWorld().equals(chunk.getWorld()) && 
               location.getBlockX() >> 4 == chunk.getX() && 
               location.getBlockZ() >> 4 == chunk.getZ();
    }
    
    /**
     * Get a unique key for a chunk.
     * 
     * @param chunk The chunk
     * @return The chunk key
     */
    private String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }
}
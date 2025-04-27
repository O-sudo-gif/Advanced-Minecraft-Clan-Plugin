package com.minecraft.clanplugin.models;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a territory chunk claimed by a clan.
 */
public class Territory {
    private final int chunkX;
    private final int chunkZ;
    private final String worldName;
    private final String clanName;
    private int influenceLevel;
    private List<Flag> flags;
    private long claimTime;

    /**
     * Creates a new territory claim
     * 
     * @param chunk The chunk to claim
     * @param clanName The name of the clan claiming the territory
     */
    public Territory(Chunk chunk, String clanName) {
        this.chunkX = chunk.getX();
        this.chunkZ = chunk.getZ();
        this.worldName = chunk.getWorld().getName();
        this.clanName = clanName;
        this.influenceLevel = 100; // Default max influence
        this.flags = new ArrayList<>();
        this.claimTime = System.currentTimeMillis();
    }
    
    /**
     * Create territory from stored data
     */
    public Territory(int chunkX, int chunkZ, String worldName, String clanName, 
                     int influenceLevel, List<Flag> flags, long claimTime) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.worldName = worldName;
        this.clanName = clanName;
        this.influenceLevel = influenceLevel;
        this.flags = flags != null ? flags : new ArrayList<>();
        this.claimTime = claimTime;
    }
    
    /**
     * Gets the chunk's X coordinate
     * 
     * @return The X coordinate
     */
    public int getChunkX() {
        return chunkX;
    }
    
    /**
     * Gets the chunk's Z coordinate
     * 
     * @return The Z coordinate
     */
    public int getChunkZ() {
        return chunkZ;
    }
    
    /**
     * Gets the name of the world this territory is in
     * 
     * @return The world name
     */
    public String getWorldName() {
        return worldName;
    }
    
    /**
     * Gets the name of the clan that owns this territory
     * 
     * @return The clan name
     */
    public String getClanName() {
        return clanName;
    }
    
    /**
     * Gets the current influence level in this territory
     * 
     * @return The influence level from 0-100
     */
    public int getInfluenceLevel() {
        return influenceLevel;
    }
    
    /**
     * Sets the influence level for this territory
     * 
     * @param influenceLevel The new influence level (0-100)
     */
    public void setInfluenceLevel(int influenceLevel) {
        this.influenceLevel = Math.max(0, Math.min(100, influenceLevel));
    }
    
    /**
     * Gets the list of flags placed in this territory
     * 
     * @return The list of flags
     */
    public List<Flag> getFlags() {
        return flags;
    }
    
    /**
     * Adds a flag to this territory
     * 
     * @param flag The flag to add
     */
    public void addFlag(Flag flag) {
        this.flags.add(flag);
        recalculateInfluence();
    }
    
    /**
     * Removes a flag from this territory
     * 
     * @param flag The flag to remove
     * @return True if the flag was removed, false otherwise
     */
    public boolean removeFlag(Flag flag) {
        boolean removed = this.flags.remove(flag);
        if (removed) {
            recalculateInfluence();
        }
        return removed;
    }
    
    /**
     * Gets the time when this territory was claimed
     * 
     * @return The claim time in milliseconds
     */
    public long getClaimTime() {
        return claimTime;
    }
    
    /**
     * Recalculates the influence level based on the number and tier of flags
     */
    private void recalculateInfluence() {
        // Base influence is 50
        int newInfluence = 50;
        
        // Each flag adds 10 influence, plus 5 per tier
        for (Flag flag : flags) {
            newInfluence += 10 + (flag.getTier() * 5);
        }
        
        // Cap at 100
        this.influenceLevel = Math.min(100, newInfluence);
    }
    
    /**
     * Gets the protection level name based on influence
     * 
     * @return The protection level name
     */
    public String getProtectionLevel() {
        if (influenceLevel >= 75) {
            return "Core";
        } else if (influenceLevel >= 50) {
            return "Secure";
        } else if (influenceLevel >= 25) {
            return "Contested";
        } else {
            return "Frontier";
        }
    }
    
    /**
     * Generates a unique hash for this territory based on world and coordinates
     * 
     * @return A unique string identifier
     */
    public String getUniqueHash() {
        return worldName + "_" + chunkX + "_" + chunkZ;
    }
    
    /**
     * Checks if the given chunk matches this territory
     * 
     * @param chunk The chunk to check
     * @return True if the chunk matches this territory
     */
    public boolean matchesChunk(Chunk chunk) {
        return chunk.getX() == chunkX && 
               chunk.getZ() == chunkZ && 
               chunk.getWorld().getName().equals(worldName);
    }
    
    /**
     * Checks if a location is within this territory
     * 
     * @param location The location to check
     * @return True if the location is within this territory
     */
    public boolean containsLocation(Location location) {
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }
        
        Chunk locationChunk = location.getChunk();
        return locationChunk.getX() == chunkX && locationChunk.getZ() == chunkZ;
    }
    
    /**
     * Gets the unique identifier for this territory.
     * 
     * @return The unique territory ID
     */
    public String getId() {
        return getUniqueHash();
    }
    
    /**
     * Gets a user-friendly name for this territory.
     * 
     * @return The name of the territory
     */
    public String getName() {
        // Provide a friendly name if there's a flag with a name, otherwise a default name
        for (Flag flag : flags) {
            if (flag.getName() != null && !flag.getName().isEmpty()) {
                return flag.getName() + " Territory";
            }
        }
        
        // Default name based on chunk coordinates
        return "Territory (" + chunkX + "," + chunkZ + ")";
    }
    
    /**
     * Gets the center location of this territory.
     * 
     * @return The center location
     */
    public Location getCenter() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        
        // A chunk is 16x16 blocks
        int blockX = chunkX * 16 + 8;
        int blockZ = chunkZ * 16 + 8;
        
        // Find a safe Y coordinate
        int blockY = world.getHighestBlockYAt(blockX, blockZ);
        
        return new Location(world, blockX, blockY, blockZ);
    }
    
    /**
     * Gets the size of this territory in square blocks.
     * 
     * @return The size of the territory
     */
    public double getSize() {
        // A chunk is 16x16 blocks (256 square blocks)
        return 256.0;
    }
    
    /**
     * Gets the strategic value of this territory.
     * 
     * @return The territory value (1-10)
     */
    public int getValue() {
        // Base value from 1-5
        int baseValue = Math.max(1, Math.min(5, (influenceLevel / 20)));
        
        // Add 1 for each flag
        baseValue += Math.min(5, flags.size());
        
        // Ensure value is between 1 and 10
        return Math.max(1, Math.min(10, baseValue));
    }
}
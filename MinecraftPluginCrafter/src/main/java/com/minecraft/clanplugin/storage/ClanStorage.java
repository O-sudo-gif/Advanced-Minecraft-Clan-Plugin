package com.minecraft.clanplugin.storage;

import com.minecraft.clanplugin.models.Clan;

import java.util.Set;
import java.util.UUID;

/**
 * Interface for clan data storage.
 */
public interface ClanStorage {
    
    /**
     * Add a clan to storage.
     * 
     * @param clan The clan to add
     */
    void addClan(Clan clan);
    
    /**
     * Remove a clan from storage.
     * 
     * @param clanName The name of the clan to remove
     * @return True if the clan was removed, false if not found
     */
    boolean removeClan(String clanName);
    
    /**
     * Get a clan by name.
     * 
     * @param clanName The name of the clan
     * @return The clan, or null if not found
     */
    Clan getClan(String clanName);
    
    /**
     * Get the clan a player belongs to.
     * 
     * @param playerUUID The UUID of the player
     * @return The player's clan, or null if not in a clan
     */
    Clan getPlayerClan(UUID playerUUID);
    
    /**
     * Get all clans in storage.
     * 
     * @return Set of all clans
     */
    Set<Clan> getAllClans();
    
    /**
     * Save a clan to storage (updates an existing clan).
     * 
     * @param clan The clan to save
     * @return True if the operation was successful
     */
    boolean saveClan(Clan clan);
    
    /**
     * Get a player's clan name.
     * 
     * @param playerUUID The UUID of the player
     * @return The clan name, or null if the player is not in a clan
     */
    String getPlayerClanName(UUID playerUUID);
}

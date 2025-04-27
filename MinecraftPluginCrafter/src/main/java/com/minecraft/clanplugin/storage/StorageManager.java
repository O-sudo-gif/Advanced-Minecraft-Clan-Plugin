package com.minecraft.clanplugin.storage;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of clan storage using in-memory data structures.
 */
public class StorageManager implements ClanStorage {
    
    private final ClanPlugin plugin;
    private final Map<String, Clan> clansByName;
    private final Map<UUID, Clan> clansByPlayer;
    private final TerritoryManager territoryManager;

    public StorageManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.clansByName = new HashMap<>();
        this.clansByPlayer = new HashMap<>();
        this.territoryManager = new TerritoryManager(plugin);
    }
    
    /**
     * Gets the clan storage interface (this object itself).
     * 
     * @return The clan storage interface
     */
    public ClanStorage getClanStorage() {
        return this;
    }

    @Override
    public void addClan(Clan clan) {
        clansByName.put(clan.getName().toLowerCase(), clan);
        
        // Map all players to this clan
        for (ClanMember member : clan.getMembers()) {
            clansByPlayer.put(member.getPlayerUUID(), clan);
        }
    }

    @Override
    public boolean removeClan(String clanName) {
        Clan clan = clansByName.remove(clanName.toLowerCase());
        
        if (clan != null) {
            // Remove all player mappings for this clan
            for (ClanMember member : clan.getMembers()) {
                clansByPlayer.remove(member.getPlayerUUID());
            }
            return true;
        }
        
        return false;
    }

    @Override
    public Clan getClan(String clanName) {
        return clansByName.get(clanName.toLowerCase());
    }

    @Override
    public Clan getPlayerClan(UUID playerUUID) {
        return clansByPlayer.get(playerUUID);
    }

    @Override
    public String getPlayerClanName(UUID playerUUID) {
        Clan clan = getPlayerClan(playerUUID);
        return clan != null ? clan.getName() : null;
    }

    @Override
    public Set<Clan> getAllClans() {
        return new HashSet<>(clansByName.values());
    }
    
    @Override
    public boolean saveClan(Clan clan) {
        if (clan == null) {
            return false;
        }
        
        // Check if the clan already exists
        if (clansByName.containsKey(clan.getName().toLowerCase())) {
            // Update the clan in storage
            clansByName.put(clan.getName().toLowerCase(), clan);
            
            // Update player mappings
            for (ClanMember member : clan.getMembers()) {
                clansByPlayer.put(member.getPlayerUUID(), clan);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Update player mappings when a player joins or leaves a clan.
     * 
     * @param playerUUID The UUID of the player
     * @param clan The clan to map to, or null to remove mapping
     */
    public void updatePlayerClan(UUID playerUUID, Clan clan) {
        if (clan != null) {
            clansByPlayer.put(playerUUID, clan);
        } else {
            clansByPlayer.remove(playerUUID);
        }
    }
    
    /**
     * Gets the territory manager.
     * 
     * @return The territory manager
     */
    public TerritoryManager getTerritoryManager() {
        return territoryManager;
    }
}

package com.minecraft.clanplugin.models;

import java.util.UUID;

/**
 * Represents a member of a clan.
 */
public class ClanMember {
    
    private final UUID playerUUID;
    private String playerName;
    private ClanRole role;

    /**
     * Create a new clan member.
     * 
     * @param playerUUID The UUID of the player
     * @param playerName The name of the player
     * @param role The role of the member in the clan
     */
    public ClanMember(UUID playerUUID, String playerName, ClanRole role) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.role = role;
    }

    /**
     * Get the UUID of the player.
     * 
     * @return The player's UUID
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    /**
     * Get the name of the player.
     * 
     * @return The player's name
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Set the name of the player.
     * 
     * @param playerName The new player name
     */
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    /**
     * Get the role of the member in the clan.
     * 
     * @return The member's role
     */
    public ClanRole getRole() {
        return role;
    }

    /**
     * Set the role of the member in the clan.
     * 
     * @param role The new role
     */
    public void setRole(ClanRole role) {
        this.role = role;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ClanMember other = (ClanMember) obj;
        return playerUUID.equals(other.playerUUID);
    }
    
    @Override
    public int hashCode() {
        return playerUUID.hashCode();
    }
}

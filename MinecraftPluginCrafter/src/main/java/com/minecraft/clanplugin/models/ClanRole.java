package com.minecraft.clanplugin.models;

/**
 * Enum representing the different roles within a clan.
 */
public enum ClanRole {
    /**
     * Clan leader - Has full permissions.
     */
    LEADER(3),
    
    /**
     * Clan officer - Has moderate permissions.
     */
    OFFICER(2),
    
    /**
     * Regular clan member - Has basic permissions.
     */
    MEMBER(1);
    
    private final int roleLevel;
    
    /**
     * Constructor for ClanRole
     * 
     * @param roleLevel The numeric level of the role (higher = more privileges)
     */
    ClanRole(int roleLevel) {
        this.roleLevel = roleLevel;
    }
    
    /**
     * Get the numeric level of this role
     * 
     * @return The role level
     */
    public int getRoleLevel() {
        return roleLevel;
    }
}

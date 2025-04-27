package com.minecraft.clanplugin.wars;

/**
 * Represents the status of a clan war.
 */
public enum WarStatus {
    /**
     * The war is currently active
     */
    ACTIVE,
    
    /**
     * The war has ended in the initiating clan's victory
     */
    INITIATOR_VICTORY,
    
    /**
     * The war has ended in the target clan's victory
     */
    TARGET_VICTORY,
    
    /**
     * The war has ended in a draw
     */
    DRAW,
    
    /**
     * The war was surrendered by the initiating clan
     */
    INITIATOR_SURRENDER,
    
    /**
     * The war was surrendered by the target clan
     */
    TARGET_SURRENDER,
    
    /**
     * The war was cancelled by mutual agreement
     */
    CANCELLED
}
package com.minecraft.clanplugin.hologram;

import com.minecraft.clanplugin.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a holographic clan banner that can be displayed in the world.
 */
public class ClanBanner {

    private final UUID id;
    private final String clanName;
    private String name;
    private Location location;
    private List<String> content;
    private List<UUID> armorStandIds;
    private BannerStyle style;
    private boolean visible;
    private BannerPermission permission;
    
    /**
     * The permission level required to edit or remove this banner.
     */
    public enum BannerPermission {
        ALL(0, "Any clan member can edit this banner"),
        MEMBER(0, "Any clan member can edit this banner"),
        OFFICER(1, "Only officers and leader can edit this banner"),
        LEADER(2, "Only the clan leader can edit this banner"),
        CREATOR_ONLY(3, "Only the banner creator can edit this banner"),
        ADMIN(4, "Only server administrators can edit this banner");
        
        private final int level;
        private final String description;
        
        BannerPermission(int level, String description) {
            this.level = level;
            this.description = description;
        }
        
        public int getLevel() {
            return level;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * Gets a user-friendly display name for this permission.
         * 
         * @return A colored string representing the permission level
         */
        public String getDisplayName() {
            switch (this) {
                case ALL:
                case MEMBER:
                    return ChatColor.GREEN + "All Members";
                case OFFICER:
                    return ChatColor.GOLD + "Officers+";
                case LEADER:
                    return ChatColor.RED + "Leader Only";
                case CREATOR_ONLY:
                    return ChatColor.LIGHT_PURPLE + "Creator Only";
                case ADMIN:
                    return ChatColor.DARK_RED + "Admins Only";
                default:
                    return ChatColor.GRAY + name();
            }
        }
    }
    
    /**
     * The style of the holographic banner.
     */
    public enum BannerStyle {
        DEFAULT(ChatColor.YELLOW, ChatColor.WHITE, true, "⚑ ", " ⚑"),
        MINIMALIST(ChatColor.GRAY, ChatColor.WHITE, false, "", ""),
        ELEGANT(ChatColor.GOLD, ChatColor.YELLOW, true, "❦ ", " ❦"),
        WARRIOR(ChatColor.RED, ChatColor.DARK_RED, true, "⚔ ", " ⚔"),
        ROYAL(ChatColor.DARK_PURPLE, ChatColor.LIGHT_PURPLE, true, "♚ ", " ♚"),
        MYSTICAL(ChatColor.AQUA, ChatColor.DARK_AQUA, true, "✧ ", " ✧"),
        NATURE(ChatColor.GREEN, ChatColor.DARK_GREEN, true, "❀ ", " ❀");
        
        private final ChatColor titleColor;
        private final ChatColor textColor;
        private final boolean borders;
        private final String prefix;
        private final String suffix;
        
        BannerStyle(ChatColor titleColor, ChatColor textColor, boolean borders, String prefix, String suffix) {
            this.titleColor = titleColor;
            this.textColor = textColor;
            this.borders = borders;
            this.prefix = prefix;
            this.suffix = suffix;
        }
        
        public ChatColor getTitleColor() {
            return titleColor;
        }
        
        public ChatColor getTextColor() {
            return textColor;
        }
        
        public boolean hasBorders() {
            return borders;
        }
        
        public String getPrefix() {
            return prefix;
        }
        
        public String getSuffix() {
            return suffix;
        }
    }
    
    /**
     * Creates a new clan banner.
     * 
     * @param clan The clan this banner belongs to
     * @param name The banner name
     * @param location The banner location
     * @param content The banner content lines
     * @param style The banner style
     * @param permission The permission level required to edit this banner
     */
    public ClanBanner(Clan clan, String name, Location location, List<String> content, 
                      BannerStyle style, BannerPermission permission) {
        this.id = UUID.randomUUID();
        this.clanName = clan.getName();
        this.name = name;
        this.location = location;
        this.content = new ArrayList<>(content);
        this.style = style;
        this.armorStandIds = new ArrayList<>();
        this.visible = true;
        this.permission = permission;
    }
    
    /**
     * Creates a banner from stored data.
     * 
     * @param id The banner ID
     * @param clanName The clan name
     * @param name The banner name
     * @param location The banner location
     * @param content The banner content
     * @param style The banner style
     * @param armorStandIds The IDs of armor stands used by this banner
     * @param visible Whether the banner is visible
     * @param permission The banner permission level
     */
    public ClanBanner(UUID id, String clanName, String name, Location location, List<String> content, 
                     BannerStyle style, List<UUID> armorStandIds, boolean visible, BannerPermission permission) {
        this.id = id;
        this.clanName = clanName;
        this.name = name;
        this.location = location;
        this.content = new ArrayList<>(content);
        this.style = style;
        this.armorStandIds = new ArrayList<>(armorStandIds);
        this.visible = visible;
        this.permission = permission;
    }
    
    /**
     * Gets the banner ID.
     * 
     * @return The banner ID
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Gets the clan name this banner belongs to.
     * 
     * @return The clan name
     */
    public String getClanName() {
        return clanName;
    }
    
    /**
     * Gets the banner name.
     * 
     * @return The banner name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the banner name.
     * 
     * @param name The new banner name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Gets the banner location.
     * 
     * @return The banner location
     */
    public Location getLocation() {
        return location.clone();
    }
    
    /**
     * Sets the banner location.
     * 
     * @param location The new banner location
     */
    public void setLocation(Location location) {
        this.location = location.clone();
    }
    
    /**
     * Gets the banner content lines.
     * 
     * @return The content lines
     */
    public List<String> getContent() {
        return new ArrayList<>(content);
    }
    
    /**
     * Sets the banner content lines.
     * 
     * @param content The new content lines
     */
    public void setContent(List<String> content) {
        this.content = new ArrayList<>(content);
    }
    
    /**
     * Gets the banner style.
     * 
     * @return The banner style
     */
    public BannerStyle getStyle() {
        return style;
    }
    
    /**
     * Sets the banner style.
     * 
     * @param style The new banner style
     */
    public void setStyle(BannerStyle style) {
        this.style = style;
    }
    
    /**
     * Gets the IDs of armor stands used by this banner.
     * 
     * @return The armor stand IDs
     */
    public List<UUID> getArmorStandIds() {
        return new ArrayList<>(armorStandIds);
    }
    
    /**
     * Adds an armor stand ID to this banner.
     * 
     * @param armorStandId The armor stand ID to add
     */
    public void addArmorStandId(UUID armorStandId) {
        this.armorStandIds.add(armorStandId);
    }
    
    /**
     * Clears all armor stand IDs from this banner.
     */
    public void clearArmorStandIds() {
        this.armorStandIds.clear();
    }
    
    /**
     * Checks if this banner is visible.
     * 
     * @return True if the banner is visible
     */
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Sets whether this banner is visible.
     * 
     * @param visible Whether the banner should be visible
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    /**
     * Gets the permission level required to edit this banner.
     * 
     * @return The banner permission
     */
    public BannerPermission getPermission() {
        return permission;
    }
    
    /**
     * Sets the permission level required to edit this banner.
     * 
     * @param permission The new permission level
     */
    public void setPermission(BannerPermission permission) {
        this.permission = permission;
    }
    
    /**
     * Spawns the holographic banner at its location.
     * 
     * @return True if the banner was spawned successfully
     */
    public boolean spawn() {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        
        // Remove any existing armor stands first
        despawn();
        
        // Clear armor stand IDs
        clearArmorStandIds();
        
        // Calculate total height
        int lines = 0;
        
        // Add title line
        lines++;
        
        // Add separator if we have borders
        if (style.hasBorders()) {
            lines++;
        }
        
        // Add content lines
        lines += content.size();
        
        // Start from the top and work down
        double currentY = location.getY() + (lines * 0.25);
        
        // Create title armor stand
        ArmorStand titleStand = createArmorStand(
            location.clone().add(0, currentY, 0),
            style.getPrefix() + style.getTitleColor() + name + style.getSuffix()
        );
        addArmorStandId(titleStand.getUniqueId());
        currentY -= 0.25;
        
        // Create separator line if needed
        if (style.hasBorders()) {
            String separator = style.getTitleColor() + "───────────";
            ArmorStand separatorStand = createArmorStand(
                location.clone().add(0, currentY, 0),
                separator
            );
            addArmorStandId(separatorStand.getUniqueId());
            currentY -= 0.25;
        }
        
        // Create content lines
        for (String line : content) {
            ArmorStand contentStand = createArmorStand(
                location.clone().add(0, currentY, 0),
                style.getTextColor() + line
            );
            addArmorStandId(contentStand.getUniqueId());
            currentY -= 0.25;
        }
        
        return true;
    }
    
    /**
     * Creates an armor stand with the specified properties.
     * 
     * @param location The armor stand location
     * @param text The armor stand custom name
     * @return The created armor stand
     */
    private ArmorStand createArmorStand(Location location, String text) {
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        
        // Configure the armor stand
        stand.setVisible(false);
        stand.setCustomNameVisible(true);
        stand.setCustomName(text);
        stand.setGravity(false);
        stand.setMarker(true);
        stand.setSmall(true);
        stand.setInvulnerable(true);
        
        return stand;
    }
    
    /**
     * Despawns the holographic banner.
     */
    public void despawn() {
        if (location == null || location.getWorld() == null) {
            return;
        }
        
        // Remove all armor stands
        for (UUID armorStandId : armorStandIds) {
            Entity entity = findEntityById(location.getWorld(), armorStandId);
            if (entity != null) {
                entity.remove();
            }
        }
    }
    
    /**
     * Updates the banner display.
     */
    public void update() {
        if (visible) {
            despawn();
            spawn();
        }
    }
    
    /**
     * Find an entity by its UUID.
     * 
     * @param world The world to search in
     * @param entityId The entity UUID
     * @return The entity, or null if not found
     */
    private Entity findEntityById(org.bukkit.World world, UUID entityId) {
        for (Entity entity : world.getEntities()) {
            if (entity.getUniqueId().equals(entityId)) {
                return entity;
            }
        }
        return null;
    }
}
package com.minecraft.clanplugin.models;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.*;

/**
 * Represents a player clan.
 */
public class Clan {
    
    private final String name;
    private final String tag;
    private String color; // Custom color for the clan
    private final Set<ClanMember> members;
    private final Set<UUID> invites;
    private final Set<String> allies; // Stores clan names of allies
    private final Set<String> enemies; // Stores clan names of enemies
    private Location home;
    private Map<String, Location> additionalHomes; // Additional home locations
    private long creationTime; // When the clan was created
    private int level; // Clan level (for progression)
    private int experience; // Clan experience (for leveling)
    private Map<String, Integer> stats; // Various clan statistics
    private boolean coloredArmor; // Whether members get colored leather armor

    /**
     * Create a new clan with the given name.
     * 
     * @param name The clan name
     */
    public Clan(String name) {
        this.name = name;
        this.tag = generateTag(name);
        this.color = ChatColor.GOLD.toString(); // Default color is gold
        this.members = new HashSet<>();
        this.invites = new HashSet<>();
        this.allies = new HashSet<>();
        this.enemies = new HashSet<>();
        this.home = null;
        this.additionalHomes = new HashMap<>();
        this.creationTime = System.currentTimeMillis();
        this.level = 1;
        this.experience = 0;
        this.stats = new HashMap<>();
        this.coloredArmor = false; // Colored armor is disabled by default
    }

    /**
     * Generate a tag from the clan name.
     * 
     * @param name The clan name
     * @return A 3-4 character tag derived from the clan name
     */
    private String generateTag(String name) {
        if (name.length() <= 4) {
            return name.toUpperCase();
        } else {
            // Use first and last letters, plus some in the middle
            String nameCaps = name.toUpperCase();
            return nameCaps.substring(0, 1) + 
                   nameCaps.substring(name.length() / 2, name.length() / 2 + 1) + 
                   nameCaps.substring(name.length() - 1);
        }
    }

    /**
     * Get the clan name.
     * 
     * @return The clan name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the clan tag.
     * 
     * @return The clan tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * Get the clan color.
     * 
     * @return The clan color as a string
     */
    public String getColor() {
        return color;
    }

    /**
     * Set the clan color.
     * 
     * @param color The new clan color
     */
    public void setColor(String color) {
        this.color = color;
    }

    /**
     * Get all clan members.
     * 
     * @return Set of clan members
     */
    public Set<ClanMember> getMembers() {
        return Collections.unmodifiableSet(members);
    }
    
    /**
     * Get UUIDs of all clan members.
     * 
     * @return Set of member UUIDs
     */
    public Set<UUID> getMemberIds() {
        Set<UUID> memberIds = new HashSet<>();
        for (ClanMember member : members) {
            memberIds.add(member.getPlayerUUID());
        }
        return memberIds;
    }

    /**
     * Add a member to the clan.
     * 
     * @param member The member to add
     */
    public void addMember(ClanMember member) {
        members.add(member);
    }

    /**
     * Remove a member from the clan.
     * 
     * @param playerUUID The UUID of the player to remove
     * @return True if the member was removed, false if not found
     */
    public boolean removeMember(UUID playerUUID) {
        Iterator<ClanMember> iterator = members.iterator();
        while (iterator.hasNext()) {
            ClanMember member = iterator.next();
            if (member.getPlayerUUID().equals(playerUUID)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Get a clan member by UUID.
     * 
     * @param playerUUID The UUID of the player
     * @return The clan member, or null if not found
     */
    public ClanMember getMember(UUID playerUUID) {
        for (ClanMember member : members) {
            if (member.getPlayerUUID().equals(playerUUID)) {
                return member;
            }
        }
        return null;
    }
    
    /**
     * Check if a player is a member of the clan.
     * 
     * @param playerUUID The UUID of the player to check
     * @return True if the player is a member of the clan
     */
    public boolean isMember(UUID playerUUID) {
        return getMember(playerUUID) != null;
    }

    /**
     * Add a player invitation to the clan.
     * 
     * @param playerUUID The UUID of the invited player
     */
    public void addInvite(UUID playerUUID) {
        invites.add(playerUUID);
    }

    /**
     * Remove a player invitation from the clan.
     * 
     * @param playerUUID The UUID of the invited player
     * @return True if the invite was removed, false if not found
     */
    public boolean removeInvite(UUID playerUUID) {
        return invites.remove(playerUUID);
    }

    /**
     * Check if a player is invited to the clan.
     * 
     * @param playerUUID The UUID of the player to check
     * @return True if the player is invited, false otherwise
     */
    public boolean isInvited(UUID playerUUID) {
        return invites.contains(playerUUID);
    }
    
    /**
     * Get all invited players.
     * 
     * @return Set of invited player UUIDs
     */
    public Set<UUID> getInvitedPlayers() {
        return Collections.unmodifiableSet(invites);
    }

    /**
     * Add a clan to the alliance list.
     * 
     * @param clanName The name of the allied clan
     */
    public void addAlliance(String clanName) {
        allies.add(clanName.toLowerCase());
        // Remove from enemies if they were enemies before
        enemies.remove(clanName.toLowerCase());
    }

    /**
     * Remove a clan from the alliance list.
     * 
     * @param clanName The name of the clan to remove from allies
     * @return True if the clan was removed, false if not found
     */
    public boolean removeAlliance(String clanName) {
        return allies.remove(clanName.toLowerCase());
    }

    /**
     * Check if a clan is an ally.
     * 
     * @param clanName The name of the clan to check
     * @return True if the clan is an ally, false otherwise
     */
    public boolean isAllied(String clanName) {
        return allies.contains(clanName.toLowerCase());
    }
    
    /**
     * Alternative method name for isAllied to maintain API compatibility.
     * 
     * @param clanName The name of the clan to check
     * @return True if the clan is an ally, false otherwise
     */
    public boolean isAlly(String clanName) {
        return isAllied(clanName);
    }

    /**
     * Get all allied clan names.
     * 
     * @return Set of allied clan names
     */
    public Set<String> getAlliances() {
        return Collections.unmodifiableSet(allies);
    }

    /**
     * Add a clan to the enemy list.
     * 
     * @param clanName The name of the enemy clan
     */
    public void addEnemy(String clanName) {
        enemies.add(clanName.toLowerCase());
        // Remove from allies if they were allies before
        allies.remove(clanName.toLowerCase());
    }

    /**
     * Remove a clan from the enemy list.
     * 
     * @param clanName The name of the clan to remove from enemies
     * @return True if the clan was removed, false if not found
     */
    public boolean removeEnemy(String clanName) {
        return enemies.remove(clanName.toLowerCase());
    }

    /**
     * Check if a clan is an enemy.
     * 
     * @param clanName The name of the clan to check
     * @return True if the clan is an enemy, false otherwise
     */
    public boolean isEnemy(String clanName) {
        return enemies.contains(clanName.toLowerCase());
    }

    /**
     * Get all enemy clan names.
     * 
     * @return Set of enemy clan names
     */
    public Set<String> getEnemies() {
        return Collections.unmodifiableSet(enemies);
    }

    /**
     * Get the clan home location.
     * 
     * @return The clan home location, or null if not set
     */
    public Location getHome() {
        return home;
    }

    /**
     * Set the clan home location.
     * 
     * @param home The new clan home location
     */
    public void setHome(Location home) {
        this.home = home;
    }
    
    /**
     * Get an additional home location by name.
     * 
     * @param name The name of the home
     * @return The home location, or null if not set
     */
    public Location getAdditionalHome(String name) {
        return additionalHomes.get(name.toLowerCase());
    }
    
    /**
     * Set an additional home location.
     * 
     * @param name The name of the home
     * @param location The location of the home
     */
    public void setAdditionalHome(String name, Location location) {
        additionalHomes.put(name.toLowerCase(), location);
    }
    
    /**
     * Remove an additional home location.
     * 
     * @param name The name of the home to remove
     * @return True if the home was removed, false if not found
     */
    public boolean removeAdditionalHome(String name) {
        return additionalHomes.remove(name.toLowerCase()) != null;
    }
    
    /**
     * Get all additional home locations.
     * 
     * @return Map of home names to locations
     */
    public Map<String, Location> getAdditionalHomes() {
        return Collections.unmodifiableMap(additionalHomes);
    }
    
    /**
     * Get the creation time of the clan.
     * 
     * @return The creation time in milliseconds
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    /**
     * Get the age of the clan in days.
     * 
     * @return The age in days
     */
    public int getAgeInDays() {
        long currentTime = System.currentTimeMillis();
        long ageMillis = currentTime - creationTime;
        return (int) (ageMillis / (1000 * 60 * 60 * 24));
    }
    
    /**
     * Get the clan level.
     * 
     * @return The clan level
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Set the clan level.
     * 
     * @param level The new clan level
     */
    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }
    
    /**
     * Get the clan experience.
     * 
     * @return The clan experience
     */
    public int getExperience() {
        return experience;
    }
    
    /**
     * Set the clan experience.
     * 
     * @param experience The new clan experience
     */
    public void setExperience(int experience) {
        this.experience = Math.max(0, experience);
    }
    
    /**
     * Add experience to the clan.
     * 
     * @param amount The amount of experience to add
     */
    public void addExperience(int amount) {
        if (amount > 0) {
            this.experience += amount;
        }
    }
    
    /**
     * Get a clan statistic.
     * 
     * @param statName The name of the statistic
     * @return The value of the statistic, or 0 if not set
     */
    public int getStat(String statName) {
        return stats.getOrDefault(statName.toLowerCase(), 0);
    }
    
    /**
     * Set a clan statistic.
     * 
     * @param statName The name of the statistic
     * @param value The value of the statistic
     */
    public void setStat(String statName, int value) {
        stats.put(statName.toLowerCase(), value);
    }
    
    /**
     * Increment a clan statistic.
     * 
     * @param statName The name of the statistic
     * @param amount The amount to increment by
     */
    public void incrementStat(String statName, int amount) {
        if (amount <= 0) {
            return;
        }
        
        String key = statName.toLowerCase();
        int currentValue = stats.getOrDefault(key, 0);
        stats.put(key, currentValue + amount);
    }
    
    /**
     * Get all clan statistics.
     * 
     * @return Map of statistic names to values
     */
    public Map<String, Integer> getAllStats() {
        return Collections.unmodifiableMap(stats);
    }
    
    /**
     * Get the ChatColor object for this clan's color.
     * 
     * @return The ChatColor, or GOLD if not valid
     */
    public ChatColor getChatColor() {
        try {
            return ChatColor.valueOf(this.color);
        } catch (IllegalArgumentException e) {
            return ChatColor.GOLD;
        }
    }
    
    /**
     * Check if this clan uses colored armor for its members.
     * 
     * @return True if the clan uses colored armor
     */
    public boolean hasColoredArmor() {
        return coloredArmor;
    }
    
    /**
     * Set whether this clan should use colored armor for its members.
     * 
     * @param coloredArmor Whether to use colored armor
     */
    public void setColoredArmor(boolean coloredArmor) {
        this.coloredArmor = coloredArmor;
    }
}

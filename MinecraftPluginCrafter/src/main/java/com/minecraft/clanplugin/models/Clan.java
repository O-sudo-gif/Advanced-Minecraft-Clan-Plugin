package com.minecraft.clanplugin.models;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.*;

/**
 * Represents a player clan.
 */
public class Clan {
    
    private final String name;
    private String tag;
    private String color; // Custom color for the clan
    private String description; // Description of the clan
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
    private int skillPoints; // Additional skill points awarded to the clan
    private int maxMembers; // Maximum number of members the clan can have
    private int maxTerritories; // Maximum number of territories the clan can claim
    private int incomeBoost; // Percentage boost to clan income
    private int warWins; // Number of clan wars won
    private int warLosses; // Number of clan wars lost
    private Map<String, Long> recentRecruits; // Map of recent recruits (UUID, join timestamp)
    private Map<String, Double> memberContributions; // Map of member contributions to the clan treasury

    /**
     * Create a new clan with the given name.
     * 
     * @param name The clan name
     */
    public Clan(String name) {
        this.name = name;
        this.tag = generateTag(name);
        this.color = ChatColor.GOLD.toString(); // Default color is gold
        this.description = "A mighty clan of warriors"; // Default description
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
        this.coloredArmor = true; // Colored armor is enabled by default
        this.skillPoints = 0;
        this.maxMembers = 10; // Default max members
        this.maxTerritories = 5; // Default max territories
        this.incomeBoost = 0; // Default income boost
        this.warWins = 0;
        this.warLosses = 0;
        this.recentRecruits = new HashMap<>();
        this.memberContributions = new HashMap<>();
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
     * Set the clan tag.
     * 
     * @param tag The new clan tag
     */
    public void setTag(String tag) {
        this.tag = tag;
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
     * Get the clan color as a ChatColor.
     * 
     * @return The clan ChatColor
     */
    public ChatColor getChatColor() {
        try {
            // Try to parse it directly if it's a valid ChatColor name
            return ChatColor.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            // If it's a color code (§ format), map it to the corresponding ChatColor
            if (color.startsWith("§")) {
                switch (color) {
                    case "§0": return ChatColor.BLACK;
                    case "§1": return ChatColor.DARK_BLUE;
                    case "§2": return ChatColor.DARK_GREEN;
                    case "§3": return ChatColor.DARK_AQUA;
                    case "§4": return ChatColor.DARK_RED;
                    case "§5": return ChatColor.DARK_PURPLE;
                    case "§6": return ChatColor.GOLD;
                    case "§7": return ChatColor.GRAY;
                    case "§8": return ChatColor.DARK_GRAY;
                    case "§9": return ChatColor.BLUE;
                    case "§a": return ChatColor.GREEN;
                    case "§b": return ChatColor.AQUA;
                    case "§c": return ChatColor.RED;
                    case "§d": return ChatColor.LIGHT_PURPLE;
                    case "§e": return ChatColor.YELLOW;
                    case "§f": return ChatColor.WHITE;
                    default: return ChatColor.GOLD; // Default to gold if unknown
                }
            }
            // Default to gold if there was an error
            return ChatColor.GOLD;
        }
    }
    
    /**
     * Get the clan description.
     * 
     * @return The clan description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the clan description.
     * 
     * @param description The new clan description
     */
    public void setDescription(String description) {
        this.description = description;
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
    
    // Enhanced getChatColor() method already defined above
    
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
    
    /**
     * Get the additional skill points for the clan.
     * 
     * @return The number of additional skill points
     */
    public int getSkillPoints() {
        return skillPoints;
    }
    
    /**
     * Set the additional skill points for the clan.
     * 
     * @param skillPoints The number of additional skill points
     */
    public void setSkillPoints(int skillPoints) {
        this.skillPoints = Math.max(0, skillPoints);
    }
    
    /**
     * Add skill points to the clan.
     * 
     * @param amount The amount of skill points to add
     */
    public void addSkillPoints(int amount) {
        if (amount > 0) {
            this.skillPoints += amount;
        }
    }
    
    /**
     * Get the maximum number of members allowed in the clan.
     * 
     * @return The maximum number of members
     */
    public int getMaxMembers() {
        return maxMembers;
    }
    
    /**
     * Set the maximum number of members allowed in the clan.
     * 
     * @param maxMembers The new maximum number of members
     */
    public void setMaxMembers(int maxMembers) {
        this.maxMembers = Math.max(1, maxMembers);
    }
    
    /**
     * Get the maximum number of territories the clan can claim.
     * 
     * @return The maximum number of territories
     */
    public int getMaxTerritories() {
        return maxTerritories;
    }
    
    /**
     * Set the maximum number of territories the clan can claim.
     * 
     * @param maxTerritories The new maximum number of territories
     */
    public void setMaxTerritories(int maxTerritories) {
        this.maxTerritories = Math.max(1, maxTerritories);
    }
    
    /**
     * Get the income boost percentage for the clan.
     * 
     * @return The income boost percentage
     */
    public int getIncomeBoost() {
        return incomeBoost;
    }
    
    /**
     * Set the income boost percentage for the clan.
     * 
     * @param incomeBoost The new income boost percentage
     */
    public void setIncomeBoost(int incomeBoost) {
        this.incomeBoost = Math.max(0, incomeBoost);
    }
    
    /**
     * Check if the clan's leader is the specified player.
     * 
     * @param playerUuid The UUID of the player to check
     * @return True if the player is the leader
     */
    public boolean isLeader(UUID playerUuid) {
        ClanMember member = getMember(playerUuid);
        return member != null && member.getRole() == ClanRole.LEADER;
    }
    
    /**
     * Check if the clan's officer is the specified player.
     * 
     * @param playerUuid The UUID of the player to check
     * @return True if the player is an officer
     */
    public boolean isOfficer(UUID playerUuid) {
        ClanMember member = getMember(playerUuid);
        return member != null && member.getRole() == ClanRole.OFFICER;
    }
    
    /**
     * Get the number of clan wars won.
     * 
     * @return The number of clan wars won
     */
    public int getWarWins() {
        return warWins;
    }
    
    /**
     * Set the number of clan wars won.
     * 
     * @param warWins The new number of clan wars won
     */
    public void setWarWins(int warWins) {
        this.warWins = Math.max(0, warWins);
    }
    
    /**
     * Increment the number of clan wars won by 1.
     */
    public void incrementWarWins() {
        this.warWins++;
    }
    
    /**
     * Get the number of clan wars lost.
     * 
     * @return The number of clan wars lost
     */
    public int getWarLosses() {
        return warLosses;
    }
    
    /**
     * Set the number of clan wars lost.
     * 
     * @param warLosses The new number of clan wars lost
     */
    public void setWarLosses(int warLosses) {
        this.warLosses = Math.max(0, warLosses);
    }
    
    /**
     * Increment the number of clan wars lost by 1.
     */
    public void incrementWarLosses() {
        this.warLosses++;
    }
    
    /**
     * Add a recruit to the clan's recent recruits list.
     * 
     * @param playerUuidStr The UUID of the recruited player as a string
     */
    public void addRecruit(String playerUuidStr) {
        recentRecruits.put(playerUuidStr, System.currentTimeMillis());
    }
    
    /**
     * Get the number of recent recruits (within the last 30 days).
     * 
     * @return The number of recent recruits
     */
    public int getRecruitCount() {
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        // Remove old recruits
        recentRecruits.entrySet().removeIf(entry -> entry.getValue() < thirtyDaysAgo);
        return recentRecruits.size();
    }
    
    /**
     * Get the map of recent recruits.
     * 
     * @return Unmodifiable map of recent recruits (UUID string to join timestamp)
     */
    public Map<String, Long> getRecentRecruits() {
        return Collections.unmodifiableMap(recentRecruits);
    }
    
    /**
     * Add a contribution to a member's total contributions.
     * 
     * @param playerUuidStr The UUID of the player as a string
     * @param amount The amount contributed
     */
    public void addContribution(String playerUuidStr, double amount) {
        if (amount <= 0) return;
        
        double currentAmount = memberContributions.getOrDefault(playerUuidStr, 0.0);
        memberContributions.put(playerUuidStr, currentAmount + amount);
    }
    
    /**
     * Get a member's total contributions.
     * 
     * @param playerUuidStr The UUID of the player as a string
     * @return The player's total contributions
     */
    public double getContribution(String playerUuidStr) {
        return memberContributions.getOrDefault(playerUuidStr, 0.0);
    }
    
    /**
     * Get all member contributions.
     * 
     * @return Unmodifiable map of member contributions (UUID string to amount)
     */
    public Map<String, Double> getMemberContributions() {
        return Collections.unmodifiableMap(memberContributions);
    }
}

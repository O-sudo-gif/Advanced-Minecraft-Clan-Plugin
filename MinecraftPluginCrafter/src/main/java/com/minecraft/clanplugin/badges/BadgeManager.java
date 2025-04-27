package com.minecraft.clanplugin.badges;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.ClanRole;
import com.minecraft.clanplugin.utils.ItemUtils;
import com.minecraft.clanplugin.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages personalized clan member badges.
 */
public class BadgeManager {

    private final ClanPlugin plugin;
    private final Map<UUID, Set<MemberBadge>> playerBadges;
    private final Map<UUID, MemberBadge> activeBadges;
    private final Map<String, Set<MemberBadge>> clanBadgePools;
    private final File badgesFile;
    private FileConfiguration badgesConfig;
    
    /**
     * Creates a new badge manager.
     * 
     * @param plugin The clan plugin instance
     */
    public BadgeManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.playerBadges = new HashMap<>();
        this.activeBadges = new HashMap<>();
        this.clanBadgePools = new HashMap<>();
        this.badgesFile = new File(plugin.getDataFolder(), "badges.yml");
        
        // Load badges from file
        loadBadges();
    }
    
    /**
     * Load badges from configuration.
     */
    private void loadBadges() {
        if (!badgesFile.exists()) {
            try {
                // Create default badges file
                badgesFile.createNewFile();
                badgesConfig = YamlConfiguration.loadConfiguration(badgesFile);
                createDefaultBadges();
                saveBadges();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create badges.yml", e);
                badgesConfig = new YamlConfiguration();
            }
        } else {
            badgesConfig = YamlConfiguration.loadConfiguration(badgesFile);
        }
        
        // Load player badges
        ConfigurationSection playerSection = badgesConfig.getConfigurationSection("players");
        if (playerSection != null) {
            for (String playerUuidStr : playerSection.getKeys(false)) {
                try {
                    UUID playerUuid = UUID.fromString(playerUuidStr);
                    ConfigurationSection badgesSection = playerSection.getConfigurationSection(playerUuidStr + ".badges");
                    
                    if (badgesSection != null) {
                        Set<MemberBadge> badges = new HashSet<>();
                        
                        for (String badgeIdStr : badgesSection.getKeys(false)) {
                            ConfigurationSection badgeSection = badgesSection.getConfigurationSection(badgeIdStr);
                            if (badgeSection != null) {
                                String name = badgeSection.getString("name", "Badge");
                                String description = badgeSection.getString("description", "A clan badge");
                                MemberBadge.BadgeType type = 
                                    MemberBadge.BadgeType.valueOf(badgeSection.getString("type", "CUSTOM"));
                                int tier = badgeSection.getInt("tier", 1);
                                ChatColor color = ChatColor.valueOf(badgeSection.getString("color", "WHITE"));
                                Material iconMaterial = 
                                    Material.valueOf(badgeSection.getString("material", "EMERALD"));
                                boolean hidden = badgeSection.getBoolean("hidden", false);
                                
                                MemberBadge badge = new MemberBadge(name, description, type, tier, color, iconMaterial, hidden);
                                badges.add(badge);
                            }
                        }
                        
                        playerBadges.put(playerUuid, badges);
                    }
                    
                    // Load active badge if exists
                    String activeBadgeId = playerSection.getString(playerUuidStr + ".activeBadge");
                    if (activeBadgeId != null && !activeBadgeId.isEmpty()) {
                        for (MemberBadge badge : playerBadges.get(playerUuid)) {
                            if (badge.getBadgeId().toString().equals(activeBadgeId)) {
                                activeBadges.put(playerUuid, badge);
                                break;
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID or badge data for player: " + playerUuidStr);
                }
            }
        }
        
        // Load clan badge pools
        ConfigurationSection clansSection = badgesConfig.getConfigurationSection("clans");
        if (clansSection != null) {
            for (String clanName : clansSection.getKeys(false)) {
                ConfigurationSection badgesSection = clansSection.getConfigurationSection(clanName);
                
                if (badgesSection != null) {
                    Set<MemberBadge> badges = new HashSet<>();
                    
                    for (String badgeIdStr : badgesSection.getKeys(false)) {
                        ConfigurationSection badgeSection = badgesSection.getConfigurationSection(badgeIdStr);
                        if (badgeSection != null) {
                            String name = badgeSection.getString("name", "Badge");
                            String description = badgeSection.getString("description", "A clan badge");
                            MemberBadge.BadgeType type = 
                                MemberBadge.BadgeType.valueOf(badgeSection.getString("type", "CUSTOM"));
                            int tier = badgeSection.getInt("tier", 1);
                            ChatColor color = ChatColor.valueOf(badgeSection.getString("color", "WHITE"));
                            Material iconMaterial = 
                                Material.valueOf(badgeSection.getString("material", "EMERALD"));
                            boolean hidden = badgeSection.getBoolean("hidden", false);
                            
                            MemberBadge badge = new MemberBadge(name, description, type, tier, color, iconMaterial, hidden);
                            badges.add(badge);
                        }
                    }
                    
                    clanBadgePools.put(clanName.toLowerCase(), badges);
                }
            }
        }
    }
    
    /**
     * Create default badges.
     */
    private void createDefaultBadges() {
        ConfigurationSection clansSection = badgesConfig.createSection("clans");
        
        // Create default clan badge pool
        ConfigurationSection defaultClanSection = clansSection.createSection("default");
        
        // Add some starter badges
        MemberBadge founderBadge = MemberBadge.createFounderBadge();
        saveBadgeToSection(defaultClanSection, founderBadge);
        
        MemberBadge leaderBadge = new MemberBadge(MemberBadge.BadgeType.LEADER, 1, null);
        saveBadgeToSection(defaultClanSection, leaderBadge);
        
        MemberBadge officerBadge = new MemberBadge(MemberBadge.BadgeType.OFFICER, 1, null);
        saveBadgeToSection(defaultClanSection, officerBadge);
        
        MemberBadge veteranBadge = new MemberBadge(MemberBadge.BadgeType.VETERAN, 1, null);
        saveBadgeToSection(defaultClanSection, veteranBadge);
        
        MemberBadge warriorBadge = new MemberBadge(MemberBadge.BadgeType.WARRIOR, 1, null);
        saveBadgeToSection(defaultClanSection, warriorBadge);
        
        MemberBadge defenderBadge = new MemberBadge(MemberBadge.BadgeType.DEFENDER, 1, null);
        saveBadgeToSection(defaultClanSection, defenderBadge);
        
        MemberBadge contributorBadge = new MemberBadge(MemberBadge.BadgeType.TOP_CONTRIBUTOR, 1, null);
        saveBadgeToSection(defaultClanSection, contributorBadge);
        
        // Create players section (will be populated as players earn badges)
        badgesConfig.createSection("players");
    }
    
    /**
     * Save a badge to a configuration section.
     * 
     * @param section The section to save to
     * @param badge The badge to save
     */
    private void saveBadgeToSection(ConfigurationSection section, MemberBadge badge) {
        ConfigurationSection badgeSection = section.createSection(badge.getBadgeId().toString());
        badgeSection.set("name", badge.getName());
        badgeSection.set("description", badge.getDescription());
        badgeSection.set("type", badge.getType().name());
        badgeSection.set("tier", badge.getTier());
        badgeSection.set("color", badge.getColor().name());
        badgeSection.set("material", badge.getIconMaterial().name());
        badgeSection.set("hidden", badge.isHidden());
    }
    
    /**
     * Save badges to configuration.
     */
    public void saveBadges() {
        try {
            badgesConfig.save(badgesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save badges to " + badgesFile, e);
        }
    }
    
    /**
     * Gets all badges owned by a player.
     * 
     * @param playerUuid The player's UUID
     * @return Set of all badges owned
     */
    public Set<MemberBadge> getPlayerBadges(UUID playerUuid) {
        return playerBadges.getOrDefault(playerUuid, new HashSet<>());
    }
    
    /**
     * Gets only the visible badges owned by a player.
     * 
     * @param playerUuid The player's UUID
     * @return Set of visible badges
     */
    public Set<MemberBadge> getVisiblePlayerBadges(UUID playerUuid) {
        Set<MemberBadge> badges = getPlayerBadges(playerUuid);
        return badges.stream()
            .filter(badge -> !badge.isHidden())
            .collect(Collectors.toSet());
    }
    
    /**
     * Gets a player's active badge.
     * 
     * @param playerUuid The player's UUID
     * @return The active badge, or null if none is active
     */
    public MemberBadge getActiveBadge(UUID playerUuid) {
        return activeBadges.get(playerUuid);
    }
    
    /**
     * Sets a player's active badge.
     * 
     * @param playerUuid The player's UUID
     * @param badge The badge to set as active
     * @return True if the badge was set successfully
     */
    public boolean setActiveBadge(UUID playerUuid, MemberBadge badge) {
        Set<MemberBadge> playerBadgeSet = getPlayerBadges(playerUuid);
        
        // Check if player owns this badge
        if (!playerBadgeSet.contains(badge)) {
            return false;
        }
        
        activeBadges.put(playerUuid, badge);
        
        // Update configuration
        ConfigurationSection playerSection = badgesConfig.getConfigurationSection("players");
        if (playerSection != null) {
            playerSection.set(playerUuid.toString() + ".activeBadge", badge.getBadgeId().toString());
            saveBadges();
        }
        
        return true;
    }
    
    /**
     * Clears a player's active badge.
     * 
     * @param playerUuid The player's UUID
     */
    public void clearActiveBadge(UUID playerUuid) {
        activeBadges.remove(playerUuid);
        
        // Update configuration
        ConfigurationSection playerSection = badgesConfig.getConfigurationSection("players");
        if (playerSection != null) {
            playerSection.set(playerUuid.toString() + ".activeBadge", null);
            saveBadges();
        }
    }
    
    /**
     * Award a badge to a player.
     * 
     * @param playerUuid The player's UUID
     * @param badge The badge to award
     * @param notifyPlayer Whether to notify the player
     * @return True if the badge was awarded successfully
     */
    public boolean awardBadge(UUID playerUuid, MemberBadge badge, boolean notifyPlayer) {
        Set<MemberBadge> badges = playerBadges.getOrDefault(playerUuid, new HashSet<>());
        
        // Don't award the same badge twice
        for (MemberBadge existing : badges) {
            if (existing.getType() == badge.getType() && existing.getTier() >= badge.getTier()) {
                return false;
            }
        }
        
        // Add the badge
        badges.add(badge);
        playerBadges.put(playerUuid, badges);
        
        // Update configuration
        ConfigurationSection playerSection = badgesConfig.getConfigurationSection("players");
        if (playerSection == null) {
            playerSection = badgesConfig.createSection("players");
        }
        
        String playerKey = playerUuid.toString();
        ConfigurationSection playerBadgesSection = playerSection.getConfigurationSection(playerKey + ".badges");
        if (playerBadgesSection == null) {
            playerBadgesSection = playerSection.createSection(playerKey + ".badges");
        }
        
        saveBadgeToSection(playerBadgesSection, badge);
        saveBadges();
        
        // Notify the player if they're online
        if (notifyPlayer) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(ChatColor.GREEN + "You've earned a new badge: " + badge.getColor() + badge.getName());
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                
                // Show badge animation
                showBadgeAnimation(player, badge);
            }
        }
        
        return true;
    }
    
    /**
     * Award a badge to a player from their clan's badge pool.
     * 
     * @param playerUuid The player's UUID
     * @param clanName The clan name
     * @param badgeType The badge type to award
     * @param tier The badge tier
     * @param notifyPlayer Whether to notify the player
     * @return True if the badge was awarded successfully
     */
    public boolean awardClanBadge(UUID playerUuid, String clanName, MemberBadge.BadgeType badgeType, 
                                 int tier, boolean notifyPlayer) {
        // Check if the clan has a badge pool
        Set<MemberBadge> badgePool = getClanBadgePool(clanName);
        
        // Find a matching badge in the pool
        MemberBadge matchingBadge = null;
        for (MemberBadge badge : badgePool) {
            if (badge.getType() == badgeType && badge.getTier() == tier) {
                matchingBadge = badge;
                break;
            }
        }
        
        // If no matching badge, create one from default templates
        if (matchingBadge == null) {
            matchingBadge = new MemberBadge(badgeType, tier, null);
            
            // Add it to the clan's badge pool
            addBadgeToClanPool(clanName, matchingBadge);
        }
        
        // Award the badge to the player
        return awardBadge(playerUuid, matchingBadge, notifyPlayer);
    }
    
    /**
     * Gets a clan's badge pool.
     * 
     * @param clanName The clan name
     * @return Set of badges in the clan's pool
     */
    public Set<MemberBadge> getClanBadgePool(String clanName) {
        // Check if this clan has a badge pool
        if (clanBadgePools.containsKey(clanName.toLowerCase())) {
            return clanBadgePools.get(clanName.toLowerCase());
        }
        
        // If not, use the default pool
        if (clanBadgePools.containsKey("default")) {
            return clanBadgePools.get("default");
        }
        
        // If no default pool, create an empty set
        return new HashSet<>();
    }
    
    /**
     * Adds a badge to a clan's badge pool.
     * 
     * @param clanName The clan name
     * @param badge The badge to add
     */
    public void addBadgeToClanPool(String clanName, MemberBadge badge) {
        Set<MemberBadge> badges = clanBadgePools.getOrDefault(clanName.toLowerCase(), new HashSet<>());
        badges.add(badge);
        clanBadgePools.put(clanName.toLowerCase(), badges);
        
        // Update configuration
        ConfigurationSection clansSection = badgesConfig.getConfigurationSection("clans");
        if (clansSection == null) {
            clansSection = badgesConfig.createSection("clans");
        }
        
        ConfigurationSection clanSection = clansSection.getConfigurationSection(clanName.toLowerCase());
        if (clanSection == null) {
            clanSection = clansSection.createSection(clanName.toLowerCase());
        }
        
        saveBadgeToSection(clanSection, badge);
        saveBadges();
    }
    
    /**
     * Create a custom badge for a clan.
     * 
     * @param clanName The clan name
     * @param name The badge name
     * @param description The badge description
     * @param material The badge icon material
     * @param color The badge color
     * @param tier The badge tier
     * @param hidden Whether the badge is hidden until awarded
     * @return The created badge
     */
    public MemberBadge createCustomClanBadge(String clanName, String name, String description,
                                            Material material, ChatColor color, int tier, boolean hidden) {
        MemberBadge badge = MemberBadge.createCustomBadge(name, description, material, color, tier, hidden);
        addBadgeToClanPool(clanName, badge);
        return badge;
    }
    
    /**
     * Show a badge animation to a player.
     * 
     * @param player The player to show the animation to
     * @param badge The badge that was earned
     */
    private void showBadgeAnimation(Player player, MemberBadge badge) {
        // Create a GUI to display the badge
        Inventory animation = Bukkit.createInventory(null, 27, 
            ChatColor.GOLD + "New Badge Earned!");
        
        // Add badge item in the center
        ItemStack badgeItem = badge.createItemStack();
        animation.setItem(13, badgeItem);
        
        // Add decorative items
        for (int i = 0; i < 27; i++) {
            if (i != 13) {
                Material material;
                if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) {
                    material = Material.YELLOW_STAINED_GLASS_PANE;
                } else {
                    material = Material.ORANGE_STAINED_GLASS_PANE;
                }
                
                ItemStack decorative = new ItemStack(material);
                ItemMeta meta = decorative.getItemMeta();
                meta.setDisplayName(" ");
                decorative.setItemMeta(meta);
                
                animation.setItem(i, decorative);
            }
        }
        
        // Add congratulatory message
        List<String> congratsLore = new ArrayList<>();
        congratsLore.add(ChatColor.GOLD + "Congratulations!");
        congratsLore.add(ChatColor.YELLOW + "You've earned a new badge:");
        congratsLore.add(badge.getColor() + badge.getName());
        congratsLore.add("");
        congratsLore.add(ChatColor.GRAY + "Click to close");
        
        ItemStack congratsItem = ItemUtils.createGuiItem(
            Material.NETHER_STAR,
            ChatColor.GOLD + "Achievement Unlocked!",
            congratsLore
        );
        animation.setItem(4, congratsItem);
        
        // Add info about using badges
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/clan badges");
        infoLore.add(ChatColor.GRAY + "to view and manage your badges.");
        infoLore.add("");
        infoLore.add(ChatColor.GRAY + "You can set one badge as your");
        infoLore.add(ChatColor.GRAY + "active badge to display it next");
        infoLore.add(ChatColor.GRAY + "to your name in clan chat.");
        
        ItemStack infoItem = ItemUtils.createGuiItem(
            Material.BOOK,
            ChatColor.AQUA + "Badge Info",
            infoLore
        );
        animation.setItem(22, infoItem);
        
        // Open the inventory
        player.openInventory(animation);
        
        // Play sound effects
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.2f);
    }
    
    /**
     * Show the badges GUI to a player.
     * 
     * @param player The player
     * @param targetUuid The UUID of the player whose badges to show
     */
    public void showBadgesGUI(Player player, UUID targetUuid) {
        // Get the player's badges
        Set<MemberBadge> badges = getVisiblePlayerBadges(targetUuid);
        
        // Determine GUI size based on badge count (9, 18, 27, 36, 45, or 54)
        int size = 9 * (int) Math.ceil(badges.size() / 9.0);
        size = Math.max(9, Math.min(54, size));
        
        // Create GUI
        String title;
        if (player.getUniqueId().equals(targetUuid)) {
            title = ChatColor.GOLD + "Your Badges";
        } else {
            Player target = Bukkit.getPlayer(targetUuid);
            String name = target != null ? target.getName() : "Player";
            title = ChatColor.GOLD + name + "'s Badges";
        }
        
        Inventory badgesGUI = Bukkit.createInventory(null, size, title);
        
        // Add badges
        int slot = 0;
        for (MemberBadge badge : badges) {
            if (slot >= size) break;
            
            ItemStack badgeItem = badge.createItemStack();
            
            // Mark active badge
            MemberBadge activeBadge = getActiveBadge(targetUuid);
            if (activeBadge != null && activeBadge.getBadgeId().equals(badge.getBadgeId())) {
                ItemMeta meta = badgeItem.getItemMeta();
                List<String> lore = meta.getLore();
                lore.add("");
                lore.add(ChatColor.GREEN + "✓ ACTIVE");
                meta.setLore(lore);
                badgeItem.setItemMeta(meta);
            }
            
            // Add activation option if viewing own badges
            if (player.getUniqueId().equals(targetUuid)) {
                ItemMeta meta = badgeItem.getItemMeta();
                List<String> lore = meta.getLore();
                lore.add("");
                
                if (activeBadge != null && activeBadge.getBadgeId().equals(badge.getBadgeId())) {
                    lore.add(ChatColor.YELLOW + "Click to deactivate");
                } else {
                    lore.add(ChatColor.YELLOW + "Click to set as active");
                }
                
                meta.setLore(lore);
                badgeItem.setItemMeta(meta);
            }
            
            badgesGUI.setItem(slot, badgeItem);
            slot++;
        }
        
        // If no badges, add a placeholder
        if (badges.isEmpty()) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "No badges yet!");
            lore.add("");
            lore.add(ChatColor.GRAY + "Earn badges by participating in");
            lore.add(ChatColor.GRAY + "clan activities and contributing");
            lore.add(ChatColor.GRAY + "to the clan's success.");
            
            ItemStack placeholder = ItemUtils.createGuiItem(
                Material.BARRIER,
                ChatColor.RED + "No Badges",
                lore
            );
            badgesGUI.setItem(4, placeholder);
        }
        
        // Open the GUI
        player.openInventory(badgesGUI);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 1.0f);
    }
    
    /**
     * Handle a click in the badges GUI.
     * 
     * @param player The player who clicked
     * @param slot The slot that was clicked
     * @param isSelfView Whether player is viewing their own badges
     * @return True if the click was handled
     */
    public boolean handleBadgesGUIClick(Player player, int slot, boolean isSelfView) {
        if (!isSelfView) {
            return false; // Can't interact with other players' badges
        }
        
        Set<MemberBadge> badges = getVisiblePlayerBadges(player.getUniqueId());
        if (badges.size() <= slot) {
            return false; // Clicked an empty slot
        }
        
        // Get the badge at this position
        MemberBadge clicked = null;
        int count = 0;
        for (MemberBadge badge : badges) {
            if (count == slot) {
                clicked = badge;
                break;
            }
            count++;
        }
        
        if (clicked == null) {
            return false;
        }
        
        // Toggle active status
        MemberBadge activeBadge = getActiveBadge(player.getUniqueId());
        if (activeBadge != null && activeBadge.getBadgeId().equals(clicked.getBadgeId())) {
            // Deactivate
            clearActiveBadge(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "Badge deactivated.");
        } else {
            // Activate
            setActiveBadge(player.getUniqueId(), clicked);
            player.sendMessage(ChatColor.GREEN + "Badge " + clicked.getColor() + clicked.getName() + 
                ChatColor.GREEN + " set as active.");
        }
        
        // Refresh the GUI
        showBadgesGUI(player, player.getUniqueId());
        
        return true;
    }
    
    /**
     * Check and award appropriate role badges to a player.
     * 
     * @param player The player
     * @param clan The player's clan
     */
    public void checkAndAwardRoleBadges(Player player, Clan clan) {
        ClanMember member = clan.getMember(player.getUniqueId());
        if (member == null) {
            return;
        }
        
        // Determine role level
        int roleLevel;
        if (member.getRole() == ClanRole.LEADER) {
            roleLevel = 3;
        } else if (member.getRole() == ClanRole.OFFICER) {
            roleLevel = 2;
        } else if (member.getRole() == ClanRole.MEMBER) {
            // Check if they're veteran (been in clan for 14+ days)
            long joinTime = member.getLastActive() - (24 * 60 * 60 * 1000); // Approximate join time
            long daysInClan = (System.currentTimeMillis() - joinTime) / (24 * 60 * 60 * 1000);
            
            if (daysInClan >= 14) {
                roleLevel = 1; // Veteran
            } else {
                roleLevel = 0; // Regular member
            }
        } else {
            roleLevel = 0; // Recruit or unknown
        }
        
        // Award appropriate badge
        awardClanBadge(player.getUniqueId(), clan.getName(), getRoleBadgeType(roleLevel), 1, true);
        
        // Check if founder
        if (roleLevel == 3 && clan.getAgeInDays() <= 7) { // Likely a founder if leader in first week
            awardClanBadge(player.getUniqueId(), clan.getName(), MemberBadge.BadgeType.FOUNDER, 1, true);
        }
    }
    
    /**
     * Get the badge type for a role level.
     * 
     * @param roleLevel The role level
     * @return The corresponding badge type
     */
    private MemberBadge.BadgeType getRoleBadgeType(int roleLevel) {
        switch (roleLevel) {
            case 3: return MemberBadge.BadgeType.LEADER;
            case 2: return MemberBadge.BadgeType.OFFICER;
            case 1: return MemberBadge.BadgeType.VETERAN;
            case 0:
            default: return MemberBadge.BadgeType.MEMBER;
        }
    }
    
    /**
     * Check and award badges based on player activity.
     * This should be called periodically, such as on player login.
     * 
     * @param player The player
     * @param clan The player's clan
     */
    public void checkAndAwardActivityBadges(Player player, Clan clan) {
        ClanMember member = clan.getMember(player.getUniqueId());
        if (member == null) {
            return;
        }
        
        // Check for activity badges based on login frequency
        long lastLogin = member.getLastActive();
        long now = System.currentTimeMillis();
        int daysSinceLastLogin = (int) ((now - lastLogin) / (24 * 60 * 60 * 1000));
        
        // If they've been active within the last 3 days
        if (daysSinceLastLogin <= 3) {
            // Check how many consecutive logins they've had
            int consecutiveLogins = getConsecutiveLogins(player.getUniqueId());
            
            // Award based on consistency
            if (consecutiveLogins >= 30) {
                awardClanBadge(player.getUniqueId(), clan.getName(), MemberBadge.BadgeType.DEDICATED_PLAYER, 3, true);
            } else if (consecutiveLogins >= 15) {
                awardClanBadge(player.getUniqueId(), clan.getName(), MemberBadge.BadgeType.DEDICATED_PLAYER, 2, true);
            } else if (consecutiveLogins >= 7) {
                awardClanBadge(player.getUniqueId(), clan.getName(), MemberBadge.BadgeType.DEDICATED_PLAYER, 1, true);
            }
            
            // Check for active member badge based on participation
            int participationScore = calculateParticipationScore(player.getUniqueId(), clan);
            
            if (participationScore >= 50) {
                awardClanBadge(player.getUniqueId(), clan.getName(), MemberBadge.BadgeType.ACTIVE_MEMBER, 2, true);
            } else if (participationScore >= 20) {
                awardClanBadge(player.getUniqueId(), clan.getName(), MemberBadge.BadgeType.ACTIVE_MEMBER, 1, true);
            }
        }
    }
    
    /**
     * Get the number of consecutive login days for a player.
     * 
     * @param playerUuid The player's UUID
     * @return The number of consecutive days
     */
    private int getConsecutiveLogins(UUID playerUuid) {
        // This would be implemented with a tracking system
        // For now, return a placeholder value
        return 1;
    }
    
    /**
     * Calculate a participation score for badge awards.
     * 
     * @param playerUuid The player's UUID
     * @param clan The player's clan
     * @return A participation score
     */
    private int calculateParticipationScore(UUID playerUuid, Clan clan) {
        // This would calculate based on various participation metrics
        // For now, return a placeholder value
        return 10;
    }
    
    /**
     * Format a display name with badge.
     * 
     * @param playerUuid The player's UUID
     * @param originalName The original display name
     * @return The formatted name with badge if applicable
     */
    public String formatNameWithBadge(UUID playerUuid, String originalName) {
        MemberBadge badge = getActiveBadge(playerUuid);
        if (badge != null) {
            return badge.getDisplayText() + " " + originalName;
        }
        return originalName;
    }
    
    /**
     * Clean up resources when the plugin is disabled.
     */
    public void saveOnDisable() {
        saveBadges();
    }
}
package com.minecraft.clanplugin.hologram;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.ClanRole;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages clan holographic banners.
 */
public class BannerManager {

    private final ClanPlugin plugin;
    private final Map<UUID, ClanBanner> banners;
    private final Map<String, List<UUID>> clanBanners;
    private final File bannersFile;
    private FileConfiguration bannersConfig;
    
    /**
     * Creates a new banner manager.
     * 
     * @param plugin The plugin instance
     */
    public BannerManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.banners = new HashMap<>();
        this.clanBanners = new HashMap<>();
        this.bannersFile = new File(plugin.getDataFolder(), "banners.yml");
        
        // Load banners from file
        loadBanners();
    }
    
    /**
     * Load banners from configuration.
     */
    private void loadBanners() {
        if (!bannersFile.exists()) {
            try {
                bannersFile.createNewFile();
                bannersConfig = YamlConfiguration.loadConfiguration(bannersFile);
                saveBanners();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create banners.yml", e);
                bannersConfig = new YamlConfiguration();
            }
        } else {
            bannersConfig = YamlConfiguration.loadConfiguration(bannersFile);
        }
        
        // Load banners from config
        ConfigurationSection bannersSection = bannersConfig.getConfigurationSection("banners");
        if (bannersSection != null) {
            for (String key : bannersSection.getKeys(false)) {
                try {
                    ConfigurationSection bannerSection = bannersSection.getConfigurationSection(key);
                    if (bannerSection == null) continue;
                    
                    UUID bannerId = UUID.fromString(key);
                    String clanName = bannerSection.getString("clan");
                    String name = bannerSection.getString("name");
                    
                    // Load location
                    double x = bannerSection.getDouble("location.x");
                    double y = bannerSection.getDouble("location.y");
                    double z = bannerSection.getDouble("location.z");
                    float yaw = (float) bannerSection.getDouble("location.yaw");
                    float pitch = (float) bannerSection.getDouble("location.pitch");
                    String worldName = bannerSection.getString("location.world");
                    World world = Bukkit.getWorld(worldName);
                    
                    if (world == null) {
                        plugin.getLogger().warning("Could not load banner " + key + ": world '" + worldName + "' not found");
                        continue;
                    }
                    
                    Location location = new Location(world, x, y, z, yaw, pitch);
                    
                    // Load content
                    List<String> content = bannerSection.getStringList("content");
                    
                    // Load style
                    ClanBanner.BannerStyle style;
                    try {
                        style = ClanBanner.BannerStyle.valueOf(bannerSection.getString("style", "DEFAULT"));
                    } catch (IllegalArgumentException e) {
                        style = ClanBanner.BannerStyle.DEFAULT;
                    }
                    
                    // Load armor stand IDs
                    List<UUID> armorStandIds = new ArrayList<>();
                    List<String> armorStandIdStrs = bannerSection.getStringList("armorStands");
                    for (String idStr : armorStandIdStrs) {
                        try {
                            armorStandIds.add(UUID.fromString(idStr));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid armor stand ID in banner " + key + ": " + idStr);
                        }
                    }
                    
                    // Load visibility
                    boolean visible = bannerSection.getBoolean("visible", true);
                    
                    // Load permission
                    ClanBanner.BannerPermission permission;
                    try {
                        permission = ClanBanner.BannerPermission.valueOf(
                            bannerSection.getString("permission", "OFFICER"));
                    } catch (IllegalArgumentException e) {
                        permission = ClanBanner.BannerPermission.OFFICER;
                    }
                    
                    // Create and add the banner
                    ClanBanner banner = new ClanBanner(
                        bannerId, clanName, name, location, content, style, armorStandIds, visible, permission);
                    banners.put(bannerId, banner);
                    
                    // Add to clan banner list
                    addBannerToClanList(clanName, bannerId);
                    
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error loading banner: " + key, e);
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + banners.size() + " holographic banners");
    }
    
    /**
     * Save banners to configuration.
     */
    public void saveBanners() {
        bannersConfig.set("banners", null);
        ConfigurationSection bannersSection = bannersConfig.createSection("banners");
        
        for (Map.Entry<UUID, ClanBanner> entry : banners.entrySet()) {
            UUID bannerId = entry.getKey();
            ClanBanner banner = entry.getValue();
            
            ConfigurationSection bannerSection = bannersSection.createSection(bannerId.toString());
            bannerSection.set("clan", banner.getClanName());
            bannerSection.set("name", banner.getName());
            
            // Save location
            Location loc = banner.getLocation();
            bannerSection.set("location.x", loc.getX());
            bannerSection.set("location.y", loc.getY());
            bannerSection.set("location.z", loc.getZ());
            bannerSection.set("location.yaw", loc.getYaw());
            bannerSection.set("location.pitch", loc.getPitch());
            bannerSection.set("location.world", loc.getWorld().getName());
            
            // Save content
            bannerSection.set("content", banner.getContent());
            
            // Save style
            bannerSection.set("style", banner.getStyle().name());
            
            // Save armor stand IDs
            List<String> armorStandIds = new ArrayList<>();
            for (UUID id : banner.getArmorStandIds()) {
                armorStandIds.add(id.toString());
            }
            bannerSection.set("armorStands", armorStandIds);
            
            // Save visibility
            bannerSection.set("visible", banner.isVisible());
            
            // Save permission
            bannerSection.set("permission", banner.getPermission().name());
        }
        
        try {
            bannersConfig.save(bannersFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save banners.yml", e);
        }
    }
    
    /**
     * Add a banner to a clan's banner list.
     * 
     * @param clanName The clan name
     * @param bannerId The banner ID
     */
    private void addBannerToClanList(String clanName, UUID bannerId) {
        List<UUID> clanBannerList = clanBanners.computeIfAbsent(
            clanName.toLowerCase(), k -> new ArrayList<>());
        
        if (!clanBannerList.contains(bannerId)) {
            clanBannerList.add(bannerId);
        }
    }
    
    /**
     * Remove a banner from a clan's banner list.
     * 
     * @param clanName The clan name
     * @param bannerId The banner ID
     */
    private void removeBannerFromClanList(String clanName, UUID bannerId) {
        List<UUID> clanBannerList = clanBanners.get(clanName.toLowerCase());
        if (clanBannerList != null) {
            clanBannerList.remove(bannerId);
        }
    }
    
    /**
     * Create a new clan banner.
     * 
     * @param clan The clan
     * @param player The player creating the banner
     * @param name The banner name
     * @param content The banner content
     * @param style The banner style
     * @param permission The banner permission level
     * @return The created banner, or null if it couldn't be created
     */
    public ClanBanner createBanner(Clan clan, Player player, String name, List<String> content, 
                                 ClanBanner.BannerStyle style, ClanBanner.BannerPermission permission) {
        // Check if player has permission to create a banner
        if (!canManageBanners(player, clan, permission)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to create banners for this clan!");
            return null;
        }
        
        // Check banner count limit (limit to 5 per clan for performance)
        List<UUID> clanBannerList = clanBanners.getOrDefault(clan.getName().toLowerCase(), new ArrayList<>());
        if (clanBannerList.size() >= 5) {
            player.sendMessage(ChatColor.RED + "Your clan has reached the maximum number of banners (5).");
            return null;
        }
        
        // Check banner name length
        if (name.length() > 32) {
            player.sendMessage(ChatColor.RED + "Banner name is too long. Maximum length is 32 characters.");
            return null;
        }
        
        // Check banner content
        if (content.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Banner must have at least one line of content.");
            return null;
        }
        
        // Check content line length
        for (String line : content) {
            if (line.length() > 48) {
                player.sendMessage(ChatColor.RED + "Content lines must be 48 characters or less.");
                return null;
            }
        }
        
        // Get player's location but remove pitch/yaw for better banner placement
        Location location = player.getLocation().clone();
        location.setPitch(0);
        location.setYaw(0);
        
        // Create the banner
        ClanBanner banner = new ClanBanner(clan, name, location, content, style, permission);
        
        // Save and add to lists
        banners.put(banner.getId(), banner);
        addBannerToClanList(clan.getName(), banner.getId());
        
        // Spawn the banner
        banner.spawn();
        
        // Save to config
        saveBanners();
        
        return banner;
    }
    
    /**
     * Delete a clan banner.
     * 
     * @param player The player deleting the banner
     * @param bannerId The banner ID
     * @return True if the banner was deleted
     */
    public boolean deleteBanner(Player player, UUID bannerId) {
        ClanBanner banner = banners.get(bannerId);
        if (banner == null) {
            player.sendMessage(ChatColor.RED + "Banner not found!");
            return false;
        }
        
        // Check if player has permission to delete this banner
        Clan clan = plugin.getStorageManager().getClanByName(banner.getClanName());
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Clan not found!");
            return false;
        }
        
        if (!canManageBanners(player, clan, banner.getPermission())) {
            player.sendMessage(ChatColor.RED + "You don't have permission to delete this banner!");
            return false;
        }
        
        // Despawn and remove the banner
        banner.despawn();
        banners.remove(bannerId);
        removeBannerFromClanList(banner.getClanName(), bannerId);
        
        // Save to config
        saveBanners();
        
        return true;
    }
    
    /**
     * Move a banner to a new location.
     * 
     * @param player The player moving the banner
     * @param bannerId The banner ID
     * @return True if the banner was moved
     */
    public boolean moveBanner(Player player, UUID bannerId) {
        ClanBanner banner = banners.get(bannerId);
        if (banner == null) {
            player.sendMessage(ChatColor.RED + "Banner not found!");
            return false;
        }
        
        // Check if player has permission to move this banner
        Clan clan = plugin.getStorageManager().getClanByName(banner.getClanName());
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Clan not found!");
            return false;
        }
        
        if (!canManageBanners(player, clan, banner.getPermission())) {
            player.sendMessage(ChatColor.RED + "You don't have permission to move this banner!");
            return false;
        }
        
        // Update the banner location
        Location newLoc = player.getLocation().clone();
        newLoc.setPitch(0);
        newLoc.setYaw(0);
        banner.setLocation(newLoc);
        
        // Respawn the banner
        banner.update();
        
        // Save to config
        saveBanners();
        
        return true;
    }
    
    /**
     * Update a banner's content.
     * 
     * @param player The player updating the banner
     * @param bannerId The banner ID
     * @param content The new content
     * @return True if the banner was updated
     */
    public boolean updateBannerContent(Player player, UUID bannerId, List<String> content) {
        ClanBanner banner = banners.get(bannerId);
        if (banner == null) {
            player.sendMessage(ChatColor.RED + "Banner not found!");
            return false;
        }
        
        // Check if player has permission to update this banner
        Clan clan = plugin.getStorageManager().getClanByName(banner.getClanName());
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Clan not found!");
            return false;
        }
        
        if (!canManageBanners(player, clan, banner.getPermission())) {
            player.sendMessage(ChatColor.RED + "You don't have permission to update this banner!");
            return false;
        }
        
        // Check content
        if (content.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Banner must have at least one line of content.");
            return false;
        }
        
        // Check content line length
        for (String line : content) {
            if (line.length() > 48) {
                player.sendMessage(ChatColor.RED + "Content lines must be 48 characters or less.");
                return false;
            }
        }
        
        // Update the banner content
        banner.setContent(content);
        
        // Respawn the banner
        banner.update();
        
        // Save to config
        saveBanners();
        
        return true;
    }
    
    /**
     * Update a banner's style.
     * 
     * @param player The player updating the banner
     * @param bannerId The banner ID
     * @param style The new style
     * @return True if the banner was updated
     */
    public boolean updateBannerStyle(Player player, UUID bannerId, ClanBanner.BannerStyle style) {
        ClanBanner banner = banners.get(bannerId);
        if (banner == null) {
            player.sendMessage(ChatColor.RED + "Banner not found!");
            return false;
        }
        
        // Check if player has permission to update this banner
        Clan clan = plugin.getStorageManager().getClanByName(banner.getClanName());
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Clan not found!");
            return false;
        }
        
        if (!canManageBanners(player, clan, banner.getPermission())) {
            player.sendMessage(ChatColor.RED + "You don't have permission to update this banner!");
            return false;
        }
        
        // Update the banner style
        banner.setStyle(style);
        
        // Respawn the banner
        banner.update();
        
        // Save to config
        saveBanners();
        
        return true;
    }
    
    /**
     * Toggle a banner's visibility.
     * 
     * @param player The player toggling the banner
     * @param bannerId The banner ID
     * @return True if the banner was toggled
     */
    public boolean toggleBannerVisibility(Player player, UUID bannerId) {
        ClanBanner banner = banners.get(bannerId);
        if (banner == null) {
            player.sendMessage(ChatColor.RED + "Banner not found!");
            return false;
        }
        
        // Check if player has permission to update this banner
        Clan clan = plugin.getStorageManager().getClanByName(banner.getClanName());
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Clan not found!");
            return false;
        }
        
        if (!canManageBanners(player, clan, banner.getPermission())) {
            player.sendMessage(ChatColor.RED + "You don't have permission to update this banner!");
            return false;
        }
        
        // Toggle visibility
        boolean newVisibility = !banner.isVisible();
        banner.setVisible(newVisibility);
        
        if (newVisibility) {
            banner.spawn();
        } else {
            banner.despawn();
        }
        
        // Save to config
        saveBanners();
        
        return true;
    }
    
    /**
     * Get a banner by ID.
     * 
     * @param bannerId The banner ID
     * @return The banner, or null if not found
     */
    public ClanBanner getBanner(UUID bannerId) {
        return banners.get(bannerId);
    }
    
    /**
     * Get all banner IDs for a clan.
     * 
     * @param clanName The clan name
     * @return List of banner IDs
     */
    public List<UUID> getClanBannerIds(String clanName) {
        return new ArrayList<>(clanBanners.getOrDefault(clanName.toLowerCase(), new ArrayList<>()));
    }
    
    /**
     * Get all banners for a clan.
     * 
     * @param clanName The clan name
     * @return List of clan banners
     */
    public List<ClanBanner> getClanBanners(String clanName) {
        List<UUID> bannerIds = clanBanners.getOrDefault(clanName.toLowerCase(), new ArrayList<>());
        List<ClanBanner> clanBannerList = new ArrayList<>();
        
        for (UUID id : bannerIds) {
            ClanBanner banner = banners.get(id);
            if (banner != null) {
                clanBannerList.add(banner);
            }
        }
        
        return clanBannerList;
    }
    
    /**
     * Update a banner's permission level.
     * 
     * @param player The player updating the banner
     * @param bannerId The banner ID
     * @param permissionStr The new permission level as a string
     * @return True if the banner was updated
     */
    public boolean updateBannerPermission(Player player, UUID bannerId, String permissionStr) {
        ClanBanner banner = banners.get(bannerId);
        if (banner == null) {
            player.sendMessage(ChatColor.RED + "Banner not found!");
            return false;
        }
        
        // Check if player has permission to update this banner
        Clan clan = plugin.getStorageManager().getClanByName(banner.getClanName());
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Clan not found!");
            return false;
        }
        
        // Check if player has admin permission or is a clan leader
        boolean isAdmin = player.hasPermission("clan.admin.banner");
        ClanMember member = clan.getMember(player.getUniqueId());
        boolean isLeader = (member != null && member.getRole() == ClanRole.LEADER);
        
        // Only clan leaders or admins can change permissions
        if (!isLeader && !isAdmin) {
            player.sendMessage(ChatColor.RED + "Only clan leaders can change banner permissions!");
            return false;
        }
        
        // Convert string to enum
        ClanBanner.BannerPermission permission;
        try {
            permission = ClanBanner.BannerPermission.valueOf(permissionStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid permission: " + permissionStr);
            player.sendMessage(ChatColor.YELLOW + "Available permissions:");
            for (ClanBanner.BannerPermission perm : ClanBanner.BannerPermission.values()) {
                player.sendMessage(ChatColor.GRAY + "• " + perm.name() + " - " + perm.getDescription());
            }
            return false;
        }
        
        // Restrict non-admins from setting ADMIN permission level
        if (permission == ClanBanner.BannerPermission.ADMIN && !isAdmin) {
            player.sendMessage(ChatColor.RED + "Only server administrators can set the ADMIN permission level!");
            return false;
        }
        
        // Update the banner permission
        banner.setPermission(permission);
        
        // Save to config
        saveBanners();
        
        player.sendMessage(ChatColor.GREEN + "Banner permission updated to " + permission.getDisplayName() + ChatColor.GREEN + "!");
        return true;
    }
    
    /**
     * Get all banners.
     * 
     * @return Map of all banners
     */
    public Map<UUID, ClanBanner> getAllBanners() {
        return new HashMap<>(banners);
    }
    
    /**
     * Spawn all banners.
     */
    public void spawnAllBanners() {
        for (ClanBanner banner : banners.values()) {
            if (banner.isVisible()) {
                banner.spawn();
            }
        }
    }
    
    /**
     * Despawn all banners.
     */
    public void despawnAllBanners() {
        for (ClanBanner banner : banners.values()) {
            banner.despawn();
        }
    }
    
    /**
     * Clean up when plugin is disabled.
     */
    public void shutdown() {
        // Despawn all banners to avoid orphaned entities
        despawnAllBanners();
        
        // Save configuration
        saveBanners();
    }
    
    /**
     * Check if a player can manage banners for a clan.
     * 
     * @param player The player
     * @param clan The clan
     * @param requiredPermission The required permission level
     * @return True if the player can manage banners
     */
    public boolean canManageBanners(Player player, Clan clan, ClanBanner.BannerPermission requiredPermission) {
        // Check for admin permission that overrides all other checks
        if (player.hasPermission("clan.admin.banner")) {
            return true;
        }
        
        // Check if player is in the clan
        ClanMember member = clan.getMember(player.getUniqueId());
        if (member == null) {
            // Allow access only if the permission is ADMIN or if player has specific admin permission
            return requiredPermission == ClanBanner.BannerPermission.ADMIN && 
                   player.hasPermission("clan.admin.banner");
        }
        
        // Get player permission level based on role
        int playerLevel;
        if (member.getRole() == ClanRole.LEADER) {
            playerLevel = 2;
        } else if (member.getRole() == ClanRole.OFFICER) {
            playerLevel = 1;
        } else {
            playerLevel = 0;
        }
        
        // Handle special CREATOR_ONLY case
        if (requiredPermission == ClanBanner.BannerPermission.CREATOR_ONLY) {
            // For now, assume creator is leader - this should be enhanced with creator UUID in banner
            return member.getRole() == ClanRole.LEADER;
        }
        
        // Handle ADMIN permission check
        if (requiredPermission == ClanBanner.BannerPermission.ADMIN) {
            return player.hasPermission("clan.admin.banner");
        }
        
        // Standard permission level check
        return playerLevel >= requiredPermission.getLevel();
    }
    
    /**
     * Check if a player has permission to perform a specific action on a banner.
     * This method combines both in-game role permission with server permissions.
     * 
     * @param player The player
     * @param banner The banner
     * @param action The action type (create, delete, edit, etc.)
     * @return True if the player has permission
     */
    public boolean hasPermission(Player player, ClanBanner banner, String action) {
        // Admin always has permission
        if (player.hasPermission("clan.admin.banner")) {
            return true;
        }
        
        // Check specific action permission
        String permNode = "clan.banner.hologram." + action.toLowerCase();
        if (!player.hasPermission(permNode)) {
            return false;
        }
        
        // Get the clan
        Clan clan = plugin.getStorageManager().getClanByName(banner.getClanName());
        if (clan == null) {
            return false; // Clan doesn't exist
        }
        
        // Check if player can manage this banner based on its permission setting
        return canManageBanners(player, clan, banner.getPermission());
    }
}
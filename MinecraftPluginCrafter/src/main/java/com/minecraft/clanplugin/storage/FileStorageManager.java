package com.minecraft.clanplugin.storage;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.ClanRole;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Implementation of clan storage using YAML files.
 */
public class FileStorageManager implements ClanStorage {
    
    private final ClanPlugin plugin;
    private final File clanDataFile;
    private final Map<String, Clan> clansByName;
    private final Map<UUID, Clan> clansByPlayer;
    
    public FileStorageManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.clanDataFile = new File(plugin.getDataFolder(), "clans.yml");
        this.clansByName = new HashMap<>();
        this.clansByPlayer = new HashMap<>();
        
        // Load clans from file
        loadClans();
    }
    
    /**
     * Load all clan data from file.
     */
    private void loadClans() {
        if (!clanDataFile.exists()) {
            plugin.getLogger().info("No clan data file found, creating a new one.");
            try {
                plugin.getDataFolder().mkdirs();
                clanDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create clan data file", e);
            }
            return;
        }
        
        FileConfiguration data = YamlConfiguration.loadConfiguration(clanDataFile);
        ConfigurationSection clansSection = data.getConfigurationSection("clans");
        
        if (clansSection == null) {
            return;
        }
        
        for (String clanName : clansSection.getKeys(false)) {
            ConfigurationSection clanSection = clansSection.getConfigurationSection(clanName);
            if (clanSection == null) continue;
            
            // Create clan object
            Clan clan = new Clan(clanName);
            
            // Set clan color if exists
            if (clanSection.contains("color")) {
                clan.setColor(clanSection.getString("color"));
            }
            
            // Load home location if exists
            if (clanSection.contains("home")) {
                ConfigurationSection homeSection = clanSection.getConfigurationSection("home");
                if (homeSection != null) {
                    String worldName = homeSection.getString("world");
                    double x = homeSection.getDouble("x");
                    double y = homeSection.getDouble("y");
                    double z = homeSection.getDouble("z");
                    float yaw = (float) homeSection.getDouble("yaw");
                    float pitch = (float) homeSection.getDouble("pitch");
                    
                    if (worldName != null && Bukkit.getWorld(worldName) != null) {
                        Location home = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                        clan.setHome(home);
                    }
                }
            }
            
            // Load members
            ConfigurationSection membersSection = clanSection.getConfigurationSection("members");
            if (membersSection != null) {
                for (String uuidString : membersSection.getKeys(false)) {
                    ConfigurationSection memberSection = membersSection.getConfigurationSection(uuidString);
                    if (memberSection == null) continue;
                    
                    UUID playerUUID = UUID.fromString(uuidString);
                    String playerName = memberSection.getString("name", "Unknown");
                    String roleString = memberSection.getString("role", "MEMBER");
                    ClanRole role = ClanRole.valueOf(roleString);
                    
                    ClanMember member = new ClanMember(playerUUID, playerName, role);
                    clan.addMember(member);
                }
            }
            
            // Load alliance list
            List<String> allies = clanSection.getStringList("allies");
            for (String ally : allies) {
                clan.addAlliance(ally);
            }
            
            // Load enemy list
            List<String> enemies = clanSection.getStringList("enemies");
            for (String enemy : enemies) {
                clan.addEnemy(enemy);
            }
            
            // Load invites
            List<String> invitesList = clanSection.getStringList("invites");
            for (String inviteUuidString : invitesList) {
                clan.addInvite(UUID.fromString(inviteUuidString));
            }
            
            // Add clan to maps
            clansByName.put(clanName.toLowerCase(), clan);
            for (ClanMember member : clan.getMembers()) {
                clansByPlayer.put(member.getPlayerUUID(), clan);
            }
        }
        
        plugin.getLogger().info("Loaded " + clansByName.size() + " clans from storage.");
    }
    
    /**
     * Save all clan data to file.
     */
    public void saveClans() {
        FileConfiguration data = new YamlConfiguration();
        ConfigurationSection clansSection = data.createSection("clans");
        
        for (Clan clan : clansByName.values()) {
            ConfigurationSection clanSection = clansSection.createSection(clan.getName());
            
            // Save clan color
            clanSection.set("color", clan.getColor());
            
            // Save home location if exists
            Location home = clan.getHome();
            if (home != null) {
                ConfigurationSection homeSection = clanSection.createSection("home");
                homeSection.set("world", home.getWorld().getName());
                homeSection.set("x", home.getX());
                homeSection.set("y", home.getY());
                homeSection.set("z", home.getZ());
                homeSection.set("yaw", home.getYaw());
                homeSection.set("pitch", home.getPitch());
            }
            
            // Save members
            ConfigurationSection membersSection = clanSection.createSection("members");
            for (ClanMember member : clan.getMembers()) {
                ConfigurationSection memberSection = membersSection.createSection(member.getPlayerUUID().toString());
                memberSection.set("name", member.getPlayerName());
                memberSection.set("role", member.getRole().name());
            }
            
            // Save alliance list
            clanSection.set("allies", new ArrayList<>(clan.getAlliances()));
            
            // Save enemy list
            clanSection.set("enemies", new ArrayList<>(clan.getEnemies()));
            
            // Save invites
            List<String> invitesList = new ArrayList<>();
            for (UUID invitedPlayer : clan.getInvitedPlayers()) {
                invitesList.add(invitedPlayer.toString());
            }
            clanSection.set("invites", invitesList);
        }
        
        try {
            data.save(clanDataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save clan data", e);
        }
    }

    @Override
    public void addClan(Clan clan) {
        clansByName.put(clan.getName().toLowerCase(), clan);
        
        // Map all players to this clan
        for (ClanMember member : clan.getMembers()) {
            clansByPlayer.put(member.getPlayerUUID(), clan);
        }
        
        // Save changes to file
        saveClans();
    }

    @Override
    public boolean removeClan(String clanName) {
        Clan clan = clansByName.remove(clanName.toLowerCase());
        
        if (clan != null) {
            // Remove all player mappings for this clan
            for (ClanMember member : clan.getMembers()) {
                clansByPlayer.remove(member.getPlayerUUID());
            }
            
            // Save changes to file
            saveClans();
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
            
            // Save changes to file
            saveClans();
            return true;
        }
        
        return false;
    }
    
    @Override
    public String getPlayerClanName(UUID playerUUID) {
        Clan clan = getPlayerClan(playerUUID);
        return clan != null ? clan.getName() : null;
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
        
        // Save changes to file
        saveClans();
    }
}
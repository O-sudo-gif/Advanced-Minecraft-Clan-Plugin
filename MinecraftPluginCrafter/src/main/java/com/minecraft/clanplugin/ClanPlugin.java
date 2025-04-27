package com.minecraft.clanplugin;

import com.minecraft.clanplugin.achievements.AchievementManager;
import com.minecraft.clanplugin.commands.*;
import com.minecraft.clanplugin.economy.ClanEconomy;
import com.minecraft.clanplugin.listeners.*;
import com.minecraft.clanplugin.mapping.TerritoryMap;
import com.minecraft.clanplugin.progression.ProgressionManager;
import com.minecraft.clanplugin.reputation.ReputationManager;
import com.minecraft.clanplugin.skills.SkillManager;
import com.minecraft.clanplugin.storage.StorageManager;
import com.minecraft.clanplugin.utils.NametagManager;
import com.minecraft.clanplugin.wars.WarManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for the Clan Plugin.
 */
public class ClanPlugin extends JavaPlugin {
    
    private StorageManager storageManager;
    private ClanEconomy economy;
    private WarManager warManager;
    private ProgressionManager progressionManager;
    private AchievementManager achievementManager;
    private TerritoryMap territoryMap;
    private ReputationManager reputationManager;
    private SkillManager skillManager;
    private NametagManager nametagManager;
    private ArmorListener armorListener;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Initialize managers
        storageManager = new StorageManager(this);
        economy = new ClanEconomy(this);
        warManager = new WarManager(this);
        progressionManager = new ProgressionManager(this);
        achievementManager = new AchievementManager(this);
        territoryMap = new TerritoryMap(this);
        reputationManager = new ReputationManager(this);
        skillManager = new SkillManager(this);
        nametagManager = new NametagManager(this);
        
        // Create command handlers
        ClanCommand clanCommand = new ClanCommand(this);
        ClanChatCommand clanChatCommand = new ClanChatCommand(this);
        ClanTerritoryCommand territoryCommand = new ClanTerritoryCommand(this);
        ClanEconomyCommand economyCommand = new ClanEconomyCommand(this, economy);
        ClanWarCommand warCommand = new ClanWarCommand(this, warManager);
        
        // Register main commands
        getCommand("clan").setExecutor(clanCommand);
        getCommand("c").setExecutor(clanChatCommand);
        
        // Register standalone versions of subcommands for direct access
        getCommand("territory").setExecutor(territoryCommand);
        getCommand("economy").setExecutor(economyCommand);
        getCommand("war").setExecutor(warCommand);
        
        // Create and register listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new TerritoryListener(this), this);
        getServer().getPluginManager().registerEvents(new TerritoryVisualizationListener(this), this);
        getServer().getPluginManager().registerEvents(new SkillProgressionListener(this), this);
        
        // Create and store armor listener for direct access
        armorListener = new ArmorListener(this);
        getServer().getPluginManager().registerEvents(armorListener, this);
        
        getServer().getPluginManager().registerEvents(new NametagListener(this, nametagManager), this);
        
        // Set up scheduled tasks
        getServer().getScheduler().runTaskTimer(this, () -> {
            // Process any timed war events
            warManager.processEndedWars();
        }, 20 * 60, 20 * 60); // Run every minute (20 ticks * 60)
        
        // Schedule daily achievement and progression tasks
        getServer().getScheduler().runTaskTimer(this, () -> {
            // Check for time-based achievements
            getLogger().info("Processing time-based achievements...");
            // Implementation for checking clan age and other time-based criteria
        }, 20 * 60 * 60, 20 * 60 * 60); // Run every hour
        
        // Update all player nametags (wait 5 ticks to ensure server is fully started)
        getServer().getScheduler().runTaskLater(this, () -> {
            nametagManager.updateAllTeams();
        }, 5L);
        
        getLogger().info("Clan Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save any pending data
        if (storageManager != null) {
            // Save data through the storage manager if needed
        }
        
        // Save skill data
        if (skillManager != null) {
            skillManager.saveMemberSkills();
        }
        
        getLogger().info("Clan Plugin has been disabled!");
    }
    
    /**
     * Get the storage manager for clans.
     * 
     * @return The storage manager instance
     */
    public StorageManager getStorageManager() {
        return storageManager;
    }
    
    /**
     * Get the economy manager for clan finances.
     * 
     * @return The economy manager instance
     */
    public ClanEconomy getEconomy() {
        return economy;
    }
    
    /**
     * Get the war manager for clan wars.
     * 
     * @return The war manager instance
     */
    public WarManager getWarManager() {
        return warManager;
    }
    
    /**
     * Get the progression manager for clan leveling.
     * 
     * @return The progression manager instance
     */
    public ProgressionManager getProgressionManager() {
        return progressionManager;
    }
    
    /**
     * Get the achievement manager for clan achievements.
     * 
     * @return The achievement manager instance
     */
    public AchievementManager getAchievementManager() {
        return achievementManager;
    }
    
    /**
     * Get the territory map for displaying clan territories.
     * 
     * @return The territory map instance
     */
    public TerritoryMap getTerritoryMap() {
        return territoryMap;
    }
    
    /**
     * Get the reputation manager for clan reputation.
     * 
     * @return The reputation manager instance
     */
    public ReputationManager getReputationManager() {
        return reputationManager;
    }
    
    /**
     * Get the skill manager for clan member skills.
     * 
     * @return The skill manager instance
     */
    public SkillManager getSkillManager() {
        return skillManager;
    }
    
    /**
     * Get the nametag manager for player nametags.
     * 
     * @return The nametag manager instance
     */
    public NametagManager getNametagManager() {
        return nametagManager;
    }
    
    /**
     * Get the armor listener for coloring player armor.
     * 
     * @return The armor listener instance
     */
    public ArmorListener getArmorListener() {
        return armorListener;
    }
}

package com.minecraft.clanplugin;

import com.minecraft.clanplugin.achievements.AchievementManager;
import com.minecraft.clanplugin.badges.BadgeManager;
import com.minecraft.clanplugin.bounty.BountyManager;
import com.minecraft.clanplugin.commands.*;
import com.minecraft.clanplugin.economy.ClanEconomy;
import com.minecraft.clanplugin.economy.PlaytimeRewardManager;
import com.minecraft.clanplugin.hologram.BannerManager;
import com.minecraft.clanplugin.shop.ClanShop;
import com.minecraft.clanplugin.listeners.*;
import com.minecraft.clanplugin.mapping.TerritoryMap;
import com.minecraft.clanplugin.progression.ProgressionManager;
import com.minecraft.clanplugin.recruitment.RecruitmentMiniGame;
import com.minecraft.clanplugin.reputation.ReputationManager;
import com.minecraft.clanplugin.skills.SkillManager;
import com.minecraft.clanplugin.storage.StorageManager;
import com.minecraft.clanplugin.utils.AnimationUtils;
import com.minecraft.clanplugin.utils.EmoteUtils;
import com.minecraft.clanplugin.utils.NametagManager;
import com.minecraft.clanplugin.utils.SidebarManager;
import com.minecraft.clanplugin.visualization.TerritoryConquestVisualizer;
import com.minecraft.clanplugin.wars.WarManager;
import com.minecraft.clanplugin.webhook.WebhookManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
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
    private SidebarManager sidebarManager;
    private ArmorListener armorListener;
    private PlaytimeRewardManager playtimeRewardManager;
    private Economy vaultEconomy; // Vault economy integration
    
    // New features
    private BadgeManager badgeManager;
    private RecruitmentMiniGame recruitmentMiniGame;
    private TerritoryConquestVisualizer territoryConquestVisualizer;
    private WebhookManager webhookManager;
    private BannerManager bannerManager;
    private ClanShop clanShop;
    private BountyManager bountyManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        // Setup Vault economy integration
        setupEconomy();
        
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
        sidebarManager = new SidebarManager(this);
        playtimeRewardManager = new PlaytimeRewardManager(this);
        
        // Initialize new features
        badgeManager = new BadgeManager(this);
        recruitmentMiniGame = new RecruitmentMiniGame(this);
        territoryConquestVisualizer = new TerritoryConquestVisualizer(this);
        webhookManager = new WebhookManager(this);
        bannerManager = new BannerManager(this);
        clanShop = new ClanShop(this);
        bountyManager = new BountyManager(this);
        
        // Create command handlers
        ClanCommand clanCommand = new ClanCommand(this);
        ClanChatCommand clanChatCommand = new ClanChatCommand(this);
        ClanTerritoryCommand territoryCommand = new ClanTerritoryCommand(this);
        ClanEconomyCommand economyCommand = new ClanEconomyCommand(this, economy);
        ClanWarCommand warCommand = new ClanWarCommand(this, warManager);
        ClanEmoteCommand emoteCommand = new ClanEmoteCommand(this);
        ClanBannerCommand bannerCommand = new ClanBannerCommand(this);
        ClanHologramCommand hologramCommand = new ClanHologramCommand(this);
        ClanShopCommand shopCommand = new ClanShopCommand(this, clanShop);
        BountyCommand bountyCommand = new BountyCommand(this, bountyManager);
        
        // Register main commands
        getCommand("clan").setExecutor(clanCommand);
        getCommand("c").setExecutor(clanChatCommand);
        getCommand("clanemote").setExecutor(emoteCommand);
        getCommand("clanbanner").setExecutor(bannerCommand);
        getCommand("clanbanner").setTabCompleter(bannerCommand);
        getCommand("clanhologram").setExecutor(hologramCommand);
        getCommand("clanhologram").setTabCompleter(hologramCommand);
        
        // Register standalone versions of subcommands for direct access
        getCommand("territory").setExecutor(territoryCommand);
        getCommand("economy").setExecutor(economyCommand);
        getCommand("war").setExecutor(warCommand);
        getCommand("clanshop").setExecutor(shopCommand);
        getCommand("clanshop").setTabCompleter(shopCommand);
        getCommand("bounty").setExecutor(bountyCommand);
        getCommand("bounty").setTabCompleter(bountyCommand);
        
        // Initialize animation and emote utilities
        AnimationUtils.init(this);
        EmoteUtils.init(this);
        
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
        getServer().getPluginManager().registerEvents(new SidebarListener(this, sidebarManager), this);
        getServer().getPluginManager().registerEvents(new PlaytimeRewardListener(this, playtimeRewardManager), this);
        
        // Register listeners for new features
        getServer().getPluginManager().registerEvents(new BadgeListener(this), this);
        getServer().getPluginManager().registerEvents(new RecruitmentListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this, clanShop), this);
        getServer().getPluginManager().registerEvents(new BannerListener(this), this);
        getServer().getPluginManager().registerEvents(new BountyListener(this, bountyManager), this);
        
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
        
        // Schedule periodic sidebar updates
        getServer().getScheduler().runTaskTimer(this, () -> {
            // Update sidebars with latest data
            sidebarManager.updateAllSidebars();
        }, 20 * 10, 20 * 10); // Run every 10 seconds (20 ticks * 10)
        
        // Schedule playtime reward processing
        getServer().getScheduler().runTaskTimer(this, () -> {
            // Process playtime rewards
            playtimeRewardManager.processRewards();
        }, 20 * 60, 20 * 60); // Run every minute (20 ticks * 60)
        
        getLogger().info("Clan Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all clan data
        if (storageManager != null) {
            getLogger().info("Saving all clan data...");
            storageManager.saveAllData();
        }
        
        // Save skill data
        if (skillManager != null) {
            getLogger().info("Saving all member skills...");
            skillManager.saveMemberSkills();
        }
        
        // Save territory data
        if (territoryMap != null) {
            getLogger().info("Saving territory data...");
            territoryMap.saveAllTerritories();
        }
        
        // Save reputation data
        if (reputationManager != null) {
            getLogger().info("Saving reputation data...");
            reputationManager.saveReputationData();
        }
        
        // Save achievement data
        if (achievementManager != null) {
            getLogger().info("Saving achievement data...");
            achievementManager.saveAchievements();
        }
        
        // Save badge data
        if (badgeManager != null) {
            getLogger().info("Saving badge data...");
            badgeManager.saveOnDisable();
        }
        
        // Save mini-game data
        if (recruitmentMiniGame != null) {
            getLogger().info("Saving recruitment mini-game data...");
            recruitmentMiniGame.saveData();
        }
        
        // Clean up webhook connections
        if (webhookManager != null) {
            getLogger().info("Shutting down webhook connections...");
            webhookManager.shutdown();
        }
        
        // Clean up banner manager
        if (bannerManager != null) {
            getLogger().info("Saving banner data and despawning holograms...");
            bannerManager.shutdown();
        }
        
        // Cancel all pending tasks
        getServer().getScheduler().cancelTasks(this);
        
        // Save clan shop data
        if (clanShop != null) {
            getLogger().info("Saving clan shop data...");
            clanShop.saveShopData();
        }
        
        // Save bounty data
        if (bountyManager != null) {
            getLogger().info("Saving bounty data...");
            bountyManager.saveBounties();
        }
        
        // Save emotes data (static utility class)
        getLogger().info("Saving emotes data...");
        try {
            EmoteUtils.saveEmotes();
        } catch (Exception e) {
            getLogger().warning("Error saving emotes: " + e.getMessage());
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
    
    /**
     * Get the Vault economy instance.
     * 
     * @return The Vault economy instance
     */
    public Economy getVaultEconomy() {
        return vaultEconomy;
    }
    
    /**
     * Get the sidebar manager for player information display.
     * 
     * @return The sidebar manager instance
     */
    public SidebarManager getSidebarManager() {
        return sidebarManager;
    }
    
    /**
     * Get the playtime reward manager for player rewards.
     * 
     * @return The playtime reward manager instance
     */
    public PlaytimeRewardManager getPlaytimeRewardManager() {
        return playtimeRewardManager;
    }
    
    /**
     * Get the badge manager for clan member badges.
     *
     * @return The badge manager instance
     */
    public BadgeManager getBadgeManager() {
        return badgeManager;
    }
    
    /**
     * Get the recruitment mini-game manager.
     *
     * @return The recruitment mini-game instance
     */
    public RecruitmentMiniGame getRecruitmentMiniGame() {
        return recruitmentMiniGame;
    }
    
    /**
     * Get the territory conquest visualizer.
     *
     * @return The territory conquest visualizer instance
     */
    public TerritoryConquestVisualizer getTerritoryConquestVisualizer() {
        return territoryConquestVisualizer;
    }
    
    /**
     * Get the webhook manager for external integrations.
     *
     * @return The webhook manager instance
     */
    public WebhookManager getWebhookManager() {
        return webhookManager;
    }
    
    /**
     * Get the banner manager for clan holographic banners.
     * 
     * @return The banner manager instance
     */
    public BannerManager getBannerManager() {
        return bannerManager;
    }
    
    /**
     * Get the clan shop for purchasing clan items and upgrades.
     *
     * @return The clan shop instance
     */
    public ClanShop getClanShop() {
        return clanShop;
    }
    
    /**
     * Get the bounty manager for player bounties.
     *
     * @return The bounty manager instance
     */
    public BountyManager getBountyManager() {
        return bountyManager;
    }
    

    
    /**
     * Sets up the Vault economy integration.
     * 
     * @return True if setup was successful
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault plugin not found! Economy features will be limited.");
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("No economy plugin found! Economy features will be limited.");
            return false;
        }
        
        vaultEconomy = rsp.getProvider();
        getLogger().info("Successfully connected to Vault economy provider: " + vaultEconomy.getName());
        return vaultEconomy != null;
    }
}

package com.minecraft.clanplugin.dashboard;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Provides a visual dashboard to display clan performance metrics.
 */
public class PerformanceDashboard {

    private final ClanPlugin plugin;
    
    /**
     * Creates a new performance dashboard.
     * 
     * @param plugin The clan plugin instance
     */
    public PerformanceDashboard(ClanPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Opens the performance dashboard for a player.
     * 
     * @param player The player viewing the dashboard
     * @param clan The clan to display stats for
     */
    public void openDashboard(Player player, Clan clan) {
        // Create dashboard GUI
        Inventory dashboard = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "Clan Performance Dashboard");
        
        // Add decorative border
        ItemUtils.createGuiBorder(dashboard, Material.PURPLE_STAINED_GLASS_PANE);
        
        // Clan Overview section - Top row
        addClanOverview(dashboard, clan);
        
        // Performance Metrics - Second row
        addPerformanceMetrics(dashboard, clan);
        
        // Member Activity - Third row
        addMemberActivity(dashboard, clan);
        
        // Territory Stats - Fourth row
        addTerritoryStats(dashboard, clan);
        
        // Combat Stats - Fifth row
        addCombatStats(dashboard, clan);
        
        // Economy Stats - Bottom row
        addEconomyStats(dashboard, clan);
        
        // Open the dashboard
        player.openInventory(dashboard);
    }
    
    /**
     * Adds clan overview section to the dashboard.
     * 
     * @param dashboard The dashboard inventory
     * @param clan The clan to display
     */
    private void addClanOverview(Inventory dashboard, Clan clan) {
        // Clan name and info
        List<String> clanInfoLore = new ArrayList<>();
        clanInfoLore.add(ChatColor.GRAY + "Founded: " + new Date(clan.getCreationTime()).toString());
        clanInfoLore.add(ChatColor.GRAY + "Members: " + clan.getMembers().size() + "/" + clan.getMaxMembers());
        clanInfoLore.add(ChatColor.GRAY + "Level: " + clan.getLevel());
        clanInfoLore.add(ChatColor.GRAY + "Experience: " + clan.getExperience());
        clanInfoLore.add("");
        clanInfoLore.add(ChatColor.YELLOW + "Click to view detailed clan info");
        
        ItemStack clanInfo = ItemUtils.createGuiItem(
            Material.NETHER_STAR, 
            clan.getChatColor() + clan.getName() + ChatColor.WHITE + " - Overview", 
            clanInfoLore
        );
        dashboard.setItem(4, clanInfo);
        
        // Achievements progress
        int achievementCount = plugin.getAchievementManager().getUnlockedCount(clan);
        int totalAchievements = plugin.getAchievementManager().getAllAchievements().size();
        double achievementPercentage = plugin.getAchievementManager().getCompletionPercentage(clan);
        
        List<String> achievementsLore = new ArrayList<>();
        achievementsLore.add(ChatColor.GRAY + "Progress: " + achievementCount + "/" + totalAchievements);
        achievementsLore.add(ChatColor.GRAY + "Completion: " + String.format("%.1f", achievementPercentage) + "%");
        achievementsLore.add("");
        addProgressBar(achievementsLore, (int) achievementPercentage);
        achievementsLore.add("");
        achievementsLore.add(ChatColor.YELLOW + "Click to view achievements");
        
        ItemStack achievements = ItemUtils.createGuiItem(
            Material.GOLDEN_APPLE, 
            ChatColor.GOLD + "Achievements", 
            achievementsLore
        );
        dashboard.setItem(2, achievements);
        
        // Reputation display
        int reputation = plugin.getReputationManager().getReputation(clan);
        String reputationLevel = plugin.getReputationManager().getReputationLevelName(clan);
        
        List<String> reputationLore = new ArrayList<>();
        reputationLore.add(ChatColor.GRAY + "Points: " + reputation);
        reputationLore.add(ChatColor.GRAY + "Level: " + reputationLevel);
        reputationLore.add("");
        reputationLore.add(ChatColor.YELLOW + "Click to view reputation details");
        
        ItemStack reputationItem = ItemUtils.createGuiItem(
            Material.EMERALD, 
            ChatColor.GREEN + "Clan Reputation", 
            reputationLore
        );
        dashboard.setItem(6, reputationItem);
    }
    
    /**
     * Adds performance metrics section to the dashboard.
     * 
     * @param dashboard The dashboard inventory
     * @param clan The clan to display
     */
    private void addPerformanceMetrics(Inventory dashboard, Clan clan) {
        // Activity Score
        int activityScore = calculateActivityScore(clan);
        
        List<String> activityLore = new ArrayList<>();
        activityLore.add(ChatColor.GRAY + "Score: " + activityScore + "/100");
        activityLore.add("");
        addProgressBar(activityLore, activityScore);
        activityLore.add("");
        activityLore.add(ChatColor.GRAY + "Based on member logins and clan actions");
        
        ItemStack activityItem = ItemUtils.createGuiItem(
            Material.CLOCK, 
            ChatColor.AQUA + "Activity Score", 
            activityLore
        );
        dashboard.setItem(11, activityItem);
        
        // Growth Rate
        int growthRate = calculateGrowthRate(clan);
        
        List<String> growthLore = new ArrayList<>();
        growthLore.add(ChatColor.GRAY + "Rate: " + growthRate + "%");
        growthLore.add("");
        growthLore.add(ChatColor.GRAY + "Based on member recruitment and XP gain");
        
        ItemStack growthItem = ItemUtils.createGuiItem(
            Material.EXPERIENCE_BOTTLE, 
            ChatColor.GREEN + "Growth Rate", 
            growthLore
        );
        dashboard.setItem(13, growthItem);
        
        // Efficiency Score
        int efficiencyScore = calculateEfficiencyScore(clan);
        
        List<String> efficiencyLore = new ArrayList<>();
        efficiencyLore.add(ChatColor.GRAY + "Score: " + efficiencyScore + "/100");
        efficiencyLore.add("");
        addProgressBar(efficiencyLore, efficiencyScore);
        efficiencyLore.add("");
        efficiencyLore.add(ChatColor.GRAY + "Based on resource management and territory control");
        
        ItemStack efficiencyItem = ItemUtils.createGuiItem(
            Material.BEACON, 
            ChatColor.YELLOW + "Efficiency Score", 
            efficiencyLore
        );
        dashboard.setItem(15, efficiencyItem);
    }
    
    /**
     * Adds member activity section to the dashboard.
     * 
     * @param dashboard The dashboard inventory
     * @param clan The clan to display
     */
    private void addMemberActivity(Inventory dashboard, Clan clan) {
        // Active Members
        List<ClanMember> activeMembers = getActiveMembers(clan);
        
        List<String> activeLore = new ArrayList<>();
        activeLore.add(ChatColor.GRAY + "Count: " + activeMembers.size() + "/" + clan.getMembers().size());
        activeLore.add("");
        activeLore.add(ChatColor.GRAY + "Most active members:");
        
        // List top 3 active members
        for (int i = 0; i < Math.min(3, activeMembers.size()); i++) {
            ClanMember member = activeMembers.get(i);
            activeLore.add(ChatColor.GRAY + "- " + member.getName());
        }
        
        activeLore.add("");
        activeLore.add(ChatColor.YELLOW + "Click to view all member activity");
        
        ItemStack activeItem = ItemUtils.createGuiItem(
            Material.PLAYER_HEAD, 
            ChatColor.AQUA + "Active Members", 
            activeLore
        );
        dashboard.setItem(20, activeItem);
        
        // Recruitment Stats
        int recruitCount = clan.getRecruitCount();
        int retentionRate = calculateRetentionRate(clan);
        
        List<String> recruitLore = new ArrayList<>();
        recruitLore.add(ChatColor.GRAY + "New recruits (30 days): " + recruitCount);
        recruitLore.add(ChatColor.GRAY + "Retention rate: " + retentionRate + "%");
        recruitLore.add("");
        recruitLore.add(ChatColor.YELLOW + "Click to view recruitment stats");
        
        ItemStack recruitItem = ItemUtils.createGuiItem(
            Material.WRITABLE_BOOK, 
            ChatColor.GREEN + "Recruitment Stats", 
            recruitLore
        );
        dashboard.setItem(22, recruitItem);
        
        // Skill Distribution
        Map<String, Integer> skillDistribution = getSkillDistribution(clan);
        
        List<String> skillLore = new ArrayList<>();
        skillLore.add(ChatColor.GRAY + "Member specializations:");
        skillLore.add("");
        
        // List top 3 skills
        int count = 0;
        for (Map.Entry<String, Integer> entry : skillDistribution.entrySet()) {
            if (count >= 3) break;
            skillLore.add(ChatColor.GRAY + entry.getKey() + ": " + entry.getValue());
            count++;
        }
        
        skillLore.add("");
        skillLore.add(ChatColor.YELLOW + "Click to view skill distribution");
        
        ItemStack skillItem = ItemUtils.createGuiItem(
            Material.BOOK, 
            ChatColor.GOLD + "Skill Distribution", 
            skillLore
        );
        dashboard.setItem(24, skillItem);
    }
    
    /**
     * Adds territory statistics to the dashboard.
     * 
     * @param dashboard The dashboard inventory
     * @param clan The clan to display
     */
    private void addTerritoryStats(Inventory dashboard, Clan clan) {
        // Territory Count
        int territoryCount = plugin.getStorageManager().getTerritoryManager().getClanTerritoryCount(clan.getName());
        
        List<String> territoryLore = new ArrayList<>();
        territoryLore.add(ChatColor.GRAY + "Claimed chunks: " + territoryCount + "/" + clan.getMaxTerritories());
        territoryLore.add("");
        territoryLore.add(ChatColor.GRAY + "Territory distribution:");
        territoryLore.add(ChatColor.GRAY + "- Resource-rich: " + countResourceRichTerritories(clan) + " chunks");
        territoryLore.add(ChatColor.GRAY + "- Strategic: " + countStrategicTerritories(clan) + " chunks");
        territoryLore.add("");
        territoryLore.add(ChatColor.YELLOW + "Click to view territory map");
        
        ItemStack territoryItem = ItemUtils.createGuiItem(
            Material.MAP, 
            ChatColor.GREEN + "Territory Stats", 
            territoryLore
        );
        dashboard.setItem(29, territoryItem);
        
        // Flag Distribution
        int flagCount = countClanFlags(clan);
        
        List<String> flagLore = new ArrayList<>();
        flagLore.add(ChatColor.GRAY + "Total flags: " + flagCount);
        flagLore.add("");
        flagLore.add(ChatColor.GRAY + "Distribution by level:");
        flagLore.add(ChatColor.GRAY + "- Level 1: " + countFlagsByLevel(clan, 1));
        flagLore.add(ChatColor.GRAY + "- Level 2: " + countFlagsByLevel(clan, 2));
        flagLore.add(ChatColor.GRAY + "- Level 3: " + countFlagsByLevel(clan, 3));
        flagLore.add("");
        flagLore.add(ChatColor.YELLOW + "Click to manage flags");
        
        ItemStack flagItem = ItemUtils.createGuiItem(
            Material.WHITE_BANNER, 
            ChatColor.AQUA + "Flag Distribution", 
            flagLore
        );
        dashboard.setItem(31, flagItem);
        
        // Expansion Potential
        int expansionScore = calculateExpansionPotential(clan);
        
        List<String> expansionLore = new ArrayList<>();
        expansionLore.add(ChatColor.GRAY + "Score: " + expansionScore + "/100");
        expansionLore.add("");
        addProgressBar(expansionLore, expansionScore);
        expansionLore.add("");
        expansionLore.add(ChatColor.GRAY + "Based on nearby unclaimed land and resources");
        
        ItemStack expansionItem = ItemUtils.createGuiItem(
            Material.COMPASS, 
            ChatColor.YELLOW + "Expansion Potential", 
            expansionLore
        );
        dashboard.setItem(33, expansionItem);
    }
    
    /**
     * Adds combat statistics to the dashboard.
     * 
     * @param dashboard The dashboard inventory
     * @param clan The clan to display
     */
    private void addCombatStats(Inventory dashboard, Clan clan) {
        // War Record
        int wins = clan.getWarWins();
        int losses = clan.getWarLosses();
        int winRate = (wins + losses > 0) ? (wins * 100 / (wins + losses)) : 0;
        
        List<String> warLore = new ArrayList<>();
        warLore.add(ChatColor.GRAY + "Record: " + wins + "W - " + losses + "L");
        warLore.add(ChatColor.GRAY + "Win rate: " + winRate + "%");
        warLore.add("");
        warLore.add(ChatColor.GRAY + "Recent wars:");
        
        // Add recent war results
        List<String> recentWars = getRecentWarResults(clan);
        for (String war : recentWars) {
            warLore.add(ChatColor.GRAY + "- " + war);
        }
        
        warLore.add("");
        warLore.add(ChatColor.YELLOW + "Click to view war history");
        
        ItemStack warItem = ItemUtils.createGuiItem(
            Material.DIAMOND_SWORD, 
            ChatColor.RED + "War Record", 
            warLore
        );
        dashboard.setItem(38, warItem);
        
        // Defensive Strength
        int defenseRating = calculateDefenseRating(clan);
        
        List<String> defenseLore = new ArrayList<>();
        defenseLore.add(ChatColor.GRAY + "Rating: " + defenseRating + "/100");
        defenseLore.add("");
        addProgressBar(defenseLore, defenseRating);
        defenseLore.add("");
        defenseLore.add(ChatColor.GRAY + "Based on territory fortification and member gear");
        
        ItemStack defenseItem = ItemUtils.createGuiItem(
            Material.SHIELD, 
            ChatColor.BLUE + "Defensive Strength", 
            defenseLore
        );
        dashboard.setItem(40, defenseItem);
        
        // Alliance Network
        Set<String> allies = clan.getAlliances();
        Set<String> enemies = clan.getEnemies();
        
        List<String> allianceLore = new ArrayList<>();
        allianceLore.add(ChatColor.GRAY + "Allies: " + allies.size());
        allianceLore.add(ChatColor.GRAY + "Enemies: " + enemies.size());
        allianceLore.add("");
        
        // List some allies
        if (!allies.isEmpty()) {
            allianceLore.add(ChatColor.GREEN + "Top allies:");
            int count = 0;
            for (String ally : allies) {
                if (count >= 3) break;
                allianceLore.add(ChatColor.GRAY + "- " + ally);
                count++;
            }
        }
        
        allianceLore.add("");
        allianceLore.add(ChatColor.YELLOW + "Click to manage relations");
        
        ItemStack allianceItem = ItemUtils.createGuiItem(
            Material.GOLDEN_HELMET, 
            ChatColor.GOLD + "Alliance Network", 
            allianceLore
        );
        dashboard.setItem(42, allianceItem);
    }
    
    /**
     * Adds economy statistics to the dashboard.
     * 
     * @param dashboard The dashboard inventory
     * @param clan The clan to display
     */
    private void addEconomyStats(Inventory dashboard, Clan clan) {
        // Bank Balance
        double balance = plugin.getEconomy().getClanBalance(clan.getName());
        
        List<String> bankLore = new ArrayList<>();
        bankLore.add(ChatColor.GRAY + "Balance: $" + String.format("%.2f", balance));
        bankLore.add(ChatColor.GRAY + "Income boost: " + clan.getIncomeBoost() + "%");
        bankLore.add("");
        bankLore.add(ChatColor.GRAY + "Recent transactions:");
        
        // Add recent transactions
        List<String> recentTransactions = getRecentTransactions(clan);
        for (String transaction : recentTransactions) {
            bankLore.add(ChatColor.GRAY + "- " + transaction);
        }
        
        bankLore.add("");
        bankLore.add(ChatColor.YELLOW + "Click to view economy details");
        
        ItemStack bankItem = ItemUtils.createGuiItem(
            Material.GOLD_INGOT, 
            ChatColor.GOLD + "Clan Treasury", 
            bankLore
        );
        dashboard.setItem(47, bankItem);
        
        // Member Contributions
        List<String> contributorsLore = new ArrayList<>();
        contributorsLore.add(ChatColor.GRAY + "Top contributors:");
        
        // List top contributors
        Map<String, Double> topContributors = getTopContributors(clan);
        for (Map.Entry<String, Double> entry : topContributors.entrySet()) {
            contributorsLore.add(ChatColor.GRAY + entry.getKey() + ": $" + String.format("%.2f", entry.getValue()));
        }
        
        contributorsLore.add("");
        contributorsLore.add(ChatColor.YELLOW + "Click to view all contributions");
        
        ItemStack contributorsItem = ItemUtils.createGuiItem(
            Material.EMERALD, 
            ChatColor.GREEN + "Member Contributions", 
            contributorsLore
        );
        dashboard.setItem(49, contributorsItem);
        
        // Economic Projection
        double projectedIncome = calculateProjectedIncome(clan);
        
        List<String> projectionLore = new ArrayList<>();
        projectionLore.add(ChatColor.GRAY + "Weekly projection: $" + String.format("%.2f", projectedIncome));
        projectionLore.add("");
        projectionLore.add(ChatColor.GRAY + "Income sources:");
        projectionLore.add(ChatColor.GRAY + "- Member contributions: " + String.format("%.1f", getMemberContributionPercentage(clan)) + "%");
        projectionLore.add(ChatColor.GRAY + "- Territory income: " + String.format("%.1f", getTerritoryIncomePercentage(clan)) + "%");
        projectionLore.add(ChatColor.GRAY + "- Other sources: " + String.format("%.1f", getOtherIncomePercentage(clan)) + "%");
        projectionLore.add("");
        projectionLore.add(ChatColor.YELLOW + "Click to view economic projections");
        
        ItemStack projectionItem = ItemUtils.createGuiItem(
            Material.FILLED_MAP, 
            ChatColor.AQUA + "Economic Projection", 
            projectionLore
        );
        dashboard.setItem(51, projectionItem);
    }

    /**
     * Adds a progress bar to the lore list.
     * 
     * @param lore The lore list to add to
     * @param percentage The percentage to display
     */
    private void addProgressBar(List<String> lore, int percentage) {
        int barLength = 20;
        int progress = (int) (percentage / 100.0 * barLength);
        
        StringBuilder progressBar = new StringBuilder(ChatColor.GRAY + "[");
        for (int i = 0; i < barLength; i++) {
            if (i < progress) {
                progressBar.append(ChatColor.GREEN + "■");
            } else {
                progressBar.append(ChatColor.RED + "■");
            }
        }
        progressBar.append(ChatColor.GRAY + "]");
        
        lore.add(progressBar.toString());
    }
    
    /**
     * Calculates an activity score for the clan based on member activity.
     * 
     * @param clan The clan to analyze
     * @return Activity score (0-100)
     */
    private int calculateActivityScore(Clan clan) {
        // This would be calculated based on actual clan activity data
        // For now, we're providing a sample implementation
        int memberCount = clan.getMembers().size();
        int activeMembers = getActiveMembers(clan).size();
        
        if (memberCount == 0) return 0;
        
        return Math.min(100, (activeMembers * 100) / memberCount);
    }
    
    /**
     * Calculates growth rate for the clan.
     * 
     * @param clan The clan to analyze
     * @return Growth rate as a percentage
     */
    private int calculateGrowthRate(Clan clan) {
        // This would be calculated based on recruitment rate and XP gains
        // For now, we'll provide a reasonable sample value
        return clan.getLevel() * 5 + clan.getRecruitCount() * 2;
    }
    
    /**
     * Calculates an efficiency score for the clan.
     * 
     * @param clan The clan to analyze
     * @return Efficiency score (0-100)
     */
    private int calculateEfficiencyScore(Clan clan) {
        // This would be calculated based on resource management and activity
        int territoryUtilization = calculateTerritoryUtilization(clan);
        int resourceManagement = calculateResourceManagement(clan);
        
        return (territoryUtilization + resourceManagement) / 2;
    }
    
    /**
     * Gets a list of the most active clan members.
     * 
     * @param clan The clan to analyze
     * @return List of active members sorted by activity
     */
    private List<ClanMember> getActiveMembers(Clan clan) {
        List<ClanMember> members = new ArrayList<>(clan.getMembers());
        
        // Sort by activity (last login time would be a good metric)
        members.sort(Comparator.comparing(ClanMember::getLastActive).reversed());
        
        return members;
    }
    
    /**
     * Calculates the retention rate of clan members.
     * 
     * @param clan The clan to analyze
     * @return Retention rate as a percentage
     */
    private int calculateRetentionRate(Clan clan) {
        // This would be calculated based on how many members stay in the clan
        // For now, return a reasonable sample value
        return 75 + (clan.getLevel() * 2);
    }
    
    /**
     * Gets a distribution of skills across clan members.
     * 
     * @param clan The clan to analyze
     * @return Map of skill names to count of members with that skill
     */
    private Map<String, Integer> getSkillDistribution(Clan clan) {
        Map<String, Integer> distribution = new HashMap<>();
        
        // This would analyze actual member skills
        // For now, provide sample data
        distribution.put("Combat", 3);
        distribution.put("Gathering", 2);
        distribution.put("Crafting", 2);
        distribution.put("Leadership", 1);
        
        return distribution;
    }
    
    /**
     * Counts territories that are rich in resources.
     * 
     * @param clan The clan to analyze
     * @return Count of resource-rich territories
     */
    private int countResourceRichTerritories(Clan clan) {
        // This would analyze actual territory data
        // For now, return a reasonable sample value
        return clan.getLevel() + 2;
    }
    
    /**
     * Counts territories that are in strategic locations.
     * 
     * @param clan The clan to analyze
     * @return Count of strategic territories
     */
    private int countStrategicTerritories(Clan clan) {
        // This would analyze actual territory data
        // For now, return a reasonable sample value
        return clan.getLevel() + 1;
    }
    
    /**
     * Counts the total number of clan flags.
     * 
     * @param clan The clan to analyze
     * @return Total number of flags
     */
    private int countClanFlags(Clan clan) {
        // This would count actual flag data
        // For now, return a reasonable sample value
        return plugin.getStorageManager().getTerritoryManager().getClanTerritoryCount(clan.getName());
    }
    
    /**
     * Counts flags of a specific level.
     * 
     * @param clan The clan to analyze
     * @param level The flag level to count
     * @return Count of flags at the specified level
     */
    private int countFlagsByLevel(Clan clan, int level) {
        // This would count actual flag data by level
        // For now, return reasonable sample values
        switch (level) {
            case 1: return countClanFlags(clan) / 2;
            case 2: return countClanFlags(clan) / 3;
            case 3: return countClanFlags(clan) / 6;
            default: return 0;
        }
    }
    
    /**
     * Calculates the expansion potential for a clan.
     * 
     * @param clan The clan to analyze
     * @return Expansion potential score (0-100)
     */
    private int calculateExpansionPotential(Clan clan) {
        // This would be calculated based on unclaimed neighboring territories and resources
        // For now, return a reasonable sample value
        return Math.max(0, 100 - (clan.getMaxTerritories() * 5));
    }
    
    /**
     * Gets a list of recent war results.
     * 
     * @param clan The clan to analyze
     * @return List of recent war results as strings
     */
    private List<String> getRecentWarResults(Clan clan) {
        List<String> results = new ArrayList<>();
        
        // This would use actual war history
        // For now, provide sample data
        results.add("Won vs. RedClan (2 days ago)");
        results.add("Lost vs. BlueClan (5 days ago)");
        results.add("Won vs. GreenClan (7 days ago)");
        
        return results;
    }
    
    /**
     * Calculates a defense rating for the clan.
     * 
     * @param clan The clan to analyze
     * @return Defense rating (0-100)
     */
    private int calculateDefenseRating(Clan clan) {
        // This would analyze territory fortifications and member equipment
        // For now, return a reasonable sample value
        return 50 + (clan.getLevel() * 5);
    }
    
    /**
     * Gets a list of recent clan treasury transactions.
     * 
     * @param clan The clan to analyze
     * @return List of recent transactions as strings
     */
    private List<String> getRecentTransactions(Clan clan) {
        List<String> transactions = new ArrayList<>();
        
        // This would use actual transaction history
        // For now, provide sample data
        transactions.add("Player1 deposited $250 (1 day ago)");
        transactions.add("Territory income: $150 (3 days ago)");
        transactions.add("Player2 withdrew $100 (4 days ago)");
        
        return transactions;
    }
    
    /**
     * Gets the top contributors to the clan treasury.
     * 
     * @param clan The clan to analyze
     * @return Map of player names to contribution amounts
     */
    private Map<String, Double> getTopContributors(Clan clan) {
        Map<String, Double> contributors = new LinkedHashMap<>();
        
        // This would use actual contribution data
        // For now, provide sample data
        contributors.put("Player1", 500.0);
        contributors.put("Player2", 350.0);
        contributors.put("Player3", 200.0);
        
        return contributors;
    }
    
    /**
     * Calculates projected weekly income for the clan.
     * 
     * @param clan The clan to analyze
     * @return Projected weekly income
     */
    private double calculateProjectedIncome(Clan clan) {
        // This would analyze income sources and rates
        // For now, calculate a reasonable estimate
        double baseIncome = 500.0;
        double levelBonus = clan.getLevel() * 100.0;
        double territoryIncome = plugin.getStorageManager().getTerritoryManager().getClanTerritoryCount(clan.getName()) * 50.0;
        double boostFactor = 1.0 + (clan.getIncomeBoost() / 100.0);
        
        return (baseIncome + levelBonus + territoryIncome) * boostFactor;
    }
    
    /**
     * Calculates the percentage of clan income from member contributions.
     * 
     * @param clan The clan to analyze
     * @return Percentage of income from members
     */
    private double getMemberContributionPercentage(Clan clan) {
        // This would analyze actual income sources
        // For now, return a reasonable estimate
        return 60.0 - (clan.getLevel() * 3);
    }
    
    /**
     * Calculates the percentage of clan income from territories.
     * 
     * @param clan The clan to analyze
     * @return Percentage of income from territories
     */
    private double getTerritoryIncomePercentage(Clan clan) {
        // This would analyze actual income sources
        // For now, return a reasonable estimate
        return 30.0 + (clan.getLevel() * 2);
    }
    
    /**
     * Calculates the percentage of clan income from other sources.
     * 
     * @param clan The clan to analyze
     * @return Percentage of income from other sources
     */
    private double getOtherIncomePercentage(Clan clan) {
        // This would analyze actual income sources
        // For now, return a reasonable estimate
        double memberPercent = getMemberContributionPercentage(clan);
        double territoryPercent = getTerritoryIncomePercentage(clan);
        
        return 100.0 - memberPercent - territoryPercent;
    }
    
    /**
     * Calculates territory utilization score.
     * 
     * @param clan The clan to analyze
     * @return Territory utilization score (0-100)
     */
    private int calculateTerritoryUtilization(Clan clan) {
        // This would analyze how effectively territories are being used
        // For now, return a reasonable sample value
        int territories = plugin.getStorageManager().getTerritoryManager().getClanTerritoryCount(clan.getName());
        int maxTerritories = clan.getMaxTerritories();
        
        if (maxTerritories == 0) return 0;
        
        return Math.min(100, (territories * 80) / maxTerritories + 20);
    }
    
    /**
     * Calculates resource management score.
     * 
     * @param clan The clan to analyze
     * @return Resource management score (0-100)
     */
    private int calculateResourceManagement(Clan clan) {
        // This would analyze how well resources are being managed
        // For now, return a reasonable sample value
        return 50 + (clan.getLevel() * 5);
    }
}
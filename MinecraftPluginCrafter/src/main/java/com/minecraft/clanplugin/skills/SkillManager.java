package com.minecraft.clanplugin.skills;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import org.bukkit.ChatColor;
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

/**
 * Manages clan member skills, skill trees, and specializations.
 */
public class SkillManager {
    private ClanPlugin plugin;
    private Map<String, ClanSkill> skills;
    private Map<UUID, MemberSkills> memberSkills;
    private File skillsFile;
    private FileConfiguration skillsConfig;
    
    /**
     * Creates a new skill manager.
     * 
     * @param plugin The clan plugin instance
     */
    public SkillManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.skills = new HashMap<>();
        this.memberSkills = new HashMap<>();
        
        // Initialize skills file
        this.skillsFile = new File(plugin.getDataFolder(), "skills.yml");
        if (!skillsFile.exists()) {
            try {
                skillsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create skills.yml!");
                e.printStackTrace();
            }
        }
        
        this.skillsConfig = YamlConfiguration.loadConfiguration(skillsFile);
        
        // Load skills and member data
        loadSkills();
        loadMemberSkills();
    }
    
    /**
     * Loads skills from configuration.
     */
    private void loadSkills() {
        ConfigurationSection skillsSection = plugin.getConfig().getConfigurationSection("skills");
        
        if (skillsSection == null) {
            // Create default skills if not configured
            setupDefaultSkills();
            return;
        }
        
        for (String skillId : skillsSection.getKeys(false)) {
            ConfigurationSection skillSection = skillsSection.getConfigurationSection(skillId);
            
            if (skillSection != null) {
                String name = skillSection.getString("name");
                String description = skillSection.getString("description");
                String treeStr = skillSection.getString("tree", "UTILITY");
                int maxLevel = skillSection.getInt("max_level", 3);
                
                SkillTree tree;
                try {
                    tree = SkillTree.valueOf(treeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    tree = SkillTree.UTILITY;
                }
                
                ClanSkill skill = new ClanSkill(skillId, name, description, tree, maxLevel);
                
                // Load level costs
                ConfigurationSection costsSection = skillSection.getConfigurationSection("level_costs");
                if (costsSection != null) {
                    for (String levelStr : costsSection.getKeys(false)) {
                        int level = Integer.parseInt(levelStr);
                        int cost = costsSection.getInt(levelStr);
                        skill.setLevelCost(level, cost);
                    }
                }
                
                // Load level effects
                ConfigurationSection effectsSection = skillSection.getConfigurationSection("level_effects");
                if (effectsSection != null) {
                    for (String levelStr : effectsSection.getKeys(false)) {
                        int level = Integer.parseInt(levelStr);
                        ConfigurationSection levelEffectsSection = effectsSection.getConfigurationSection(levelStr);
                        
                        if (levelEffectsSection != null) {
                            for (String effectName : levelEffectsSection.getKeys(false)) {
                                int value = levelEffectsSection.getInt(effectName);
                                skill.addLevelEffect(level, effectName, value);
                            }
                        }
                    }
                }
                
                // Load prerequisites
                List<String> prerequisites = skillSection.getStringList("prerequisites");
                for (String prereq : prerequisites) {
                    skill.addPrerequisite(prereq);
                }
                
                skills.put(skillId, skill);
            }
        }
    }
    
    /**
     * Gets the available skill points for a clan.
     * 
     * @param clan The clan to get skill points for
     * @return The number of available skill points
     */
    public int getAvailableSkillPoints(Clan clan) {
        // Base skill points on clan level and any bonus points from achievements or events
        int basePoints = clan.getLevel() * 2; // 2 points per level
        
        // Add any bonus points stored in clan data
        int bonusPoints = clan.getSkillPoints();
        
        // Calculate used points
        int usedPoints = calculateUsedSkillPoints(clan);
        
        return basePoints + bonusPoints - usedPoints;
    }
    
    /**
     * Gets the skill tree for a clan.
     * 
     * @param clan The clan to get the skill tree for
     * @return The clan's skill tree
     */
    public SkillTree getClanSkillTree(Clan clan) {
        // In this implementation, we're returning the COMBAT tree as default
        // In a real implementation, this would likely pull from clan data about their specialization
        return SkillTree.COMBAT;
    }
    
    /**
     * Calculates the total skill points used by a clan.
     * 
     * @param clan The clan to calculate for
     * @return The number of used skill points
     */
    private int calculateUsedSkillPoints(Clan clan) {
        int totalUsed = 0;
        
        // This is a simplified implementation
        // In a complete system, this would iterate through all unlocked skills
        // and sum up their costs based on level
        
        // For demo purposes, return a placeholder value
        return totalUsed;
    }
    
    /**
     * Sets up default skills if not configured.
     */
    private void setupDefaultSkills() {
        // Combat skills
        ClanSkill swordMastery = new ClanSkill("sword_mastery", "Sword Mastery", 
                "Increases damage dealt with swords", SkillTree.COMBAT, 5);
        for (int i = 1; i <= 5; i++) {
            swordMastery.setLevelCost(i, i * 2);
            swordMastery.addLevelEffect(i, "damage_bonus", i * 5);
        }
        skills.put("sword_mastery", swordMastery);
        
        ClanSkill criticalStrikes = new ClanSkill("critical_strikes", "Critical Strikes", 
                "Increases chance of landing critical hits", SkillTree.COMBAT, 3);
        criticalStrikes.addPrerequisite("sword_mastery");
        for (int i = 1; i <= 3; i++) {
            criticalStrikes.setLevelCost(i, i * 3);
            criticalStrikes.addLevelEffect(i, "crit_chance", i * 2);
            criticalStrikes.addLevelEffect(i, "crit_damage", i * 10);
        }
        skills.put("critical_strikes", criticalStrikes);
        
        ClanSkill resilience = new ClanSkill("resilience", "Resilience", 
                "Reduces damage taken in clan territories", SkillTree.COMBAT, 3);
        for (int i = 1; i <= 3; i++) {
            resilience.setLevelCost(i, i * 2);
            resilience.addLevelEffect(i, "damage_reduction", i * 5);
        }
        skills.put("resilience", resilience);
        
        // Territory skills
        ClanSkill territoryInfluence = new ClanSkill("territory_influence", "Territory Influence", 
                "Increases influence generation in territories", SkillTree.TERRITORY, 5);
        for (int i = 1; i <= 5; i++) {
            territoryInfluence.setLevelCost(i, i * 2);
            territoryInfluence.addLevelEffect(i, "territory_influence", i * 5);
        }
        skills.put("territory_influence", territoryInfluence);
        
        ClanSkill flagMastery = new ClanSkill("flag_mastery", "Flag Mastery", 
                "Increases the strength and range of clan flags", SkillTree.TERRITORY, 3);
        flagMastery.addPrerequisite("territory_influence");
        for (int i = 1; i <= 3; i++) {
            flagMastery.setLevelCost(i, i * 3);
            flagMastery.addLevelEffect(i, "flag_strength", i * 10);
            flagMastery.addLevelEffect(i, "flag_range", i);
        }
        skills.put("flag_mastery", flagMastery);
        
        ClanSkill efficientClaiming = new ClanSkill("efficient_claiming", "Efficient Claiming", 
                "Reduces cost and upkeep of territory claims", SkillTree.TERRITORY, 3);
        for (int i = 1; i <= 3; i++) {
            efficientClaiming.setLevelCost(i, i * 2);
            efficientClaiming.addLevelEffect(i, "claim_cost_reduction", i * 5);
            efficientClaiming.addLevelEffect(i, "upkeep_reduction", i * 5);
        }
        skills.put("efficient_claiming", efficientClaiming);
        
        // Economy skills
        ClanSkill wealthGeneration = new ClanSkill("wealth_generation", "Wealth Generation", 
                "Increases income from clan activities", SkillTree.ECONOMY, 5);
        for (int i = 1; i <= 5; i++) {
            wealthGeneration.setLevelCost(i, i * 2);
            wealthGeneration.addLevelEffect(i, "income_bonus", i * 5);
        }
        skills.put("wealth_generation", wealthGeneration);
        
        ClanSkill efficientTaxation = new ClanSkill("efficient_taxation", "Efficient Taxation", 
                "Improves tax collection and reduces member dissatisfaction", SkillTree.ECONOMY, 3);
        efficientTaxation.addPrerequisite("wealth_generation");
        for (int i = 1; i <= 3; i++) {
            efficientTaxation.setLevelCost(i, i * 3);
            efficientTaxation.addLevelEffect(i, "tax_bonus", i * 10);
            efficientTaxation.addLevelEffect(i, "tax_penalty_reduction", i * 5);
        }
        skills.put("efficient_taxation", efficientTaxation);
        
        ClanSkill marketMaster = new ClanSkill("market_master", "Market Master", 
                "Reduces costs when purchasing items and resources", SkillTree.ECONOMY, 3);
        for (int i = 1; i <= 3; i++) {
            marketMaster.setLevelCost(i, i * 2);
            marketMaster.addLevelEffect(i, "cost_reduction", i * 5);
        }
        skills.put("market_master", marketMaster);
        
        // Diplomacy skills
        ClanSkill allianceCoordination = new ClanSkill("alliance_coordination", "Alliance Coordination", 
                "Improves benefits from allied clans", SkillTree.DIPLOMACY, 5);
        for (int i = 1; i <= 5; i++) {
            allianceCoordination.setLevelCost(i, i * 2);
            allianceCoordination.addLevelEffect(i, "alliance_bonus", i * 5);
        }
        skills.put("alliance_coordination", allianceCoordination);
        
        ClanSkill reputationInfluence = new ClanSkill("reputation_influence", "Reputation Influence", 
                "Increases reputation gain from actions", SkillTree.DIPLOMACY, 3);
        reputationInfluence.addPrerequisite("alliance_coordination");
        for (int i = 1; i <= 3; i++) {
            reputationInfluence.setLevelCost(i, i * 3);
            reputationInfluence.addLevelEffect(i, "reputation_gain", i * 10);
        }
        skills.put("reputation_influence", reputationInfluence);
        
        ClanSkill peacefulResolution = new ClanSkill("peaceful_resolution", "Peaceful Resolution", 
                "Reduces reputation loss from negative actions", SkillTree.DIPLOMACY, 3);
        for (int i = 1; i <= 3; i++) {
            peacefulResolution.setLevelCost(i, i * 2);
            peacefulResolution.addLevelEffect(i, "reputation_loss_reduction", i * 10);
        }
        skills.put("peaceful_resolution", peacefulResolution);
        
        // Utility skills
        ClanSkill homeTeleportion = new ClanSkill("home_teleportation", "Home Teleportation", 
                "Reduces cooldown and improves clan home teleportation", SkillTree.UTILITY, 3);
        for (int i = 1; i <= 3; i++) {
            homeTeleportion.setLevelCost(i, i * 2);
            homeTeleportion.addLevelEffect(i, "home_cooldown_reduction", i * 10);
            if (i >= 2) {
                homeTeleportion.addLevelEffect(i, "home_count", i - 1);
            }
        }
        skills.put("home_teleportation", homeTeleportion);
        
        ClanSkill resourceSharing = new ClanSkill("resource_sharing", "Resource Sharing", 
                "Allows sharing resources with clan members more efficiently", SkillTree.UTILITY, 3);
        for (int i = 1; i <= 3; i++) {
            resourceSharing.setLevelCost(i, i * 2);
            resourceSharing.addLevelEffect(i, "sharing_efficiency", i * 10);
            if (i >= 2) {
                resourceSharing.addLevelEffect(i, "shared_storage", i);
            }
        }
        skills.put("resource_sharing", resourceSharing);
        
        // ==== ROLE-BASED SKILLS ====
        
        // Miner Skills
        ClanSkill miningEfficiency = new ClanSkill("mining_efficiency", "Mining Efficiency", 
                "Increases mining speed for all block types", SkillTree.MINER, 5);
        for (int i = 1; i <= 5; i++) {
            miningEfficiency.setLevelCost(i, i * 2);
            miningEfficiency.addLevelEffect(i, "mining_speed", i * 10);
        }
        skills.put("mining_efficiency", miningEfficiency);
        
        ClanSkill fortuneSeeker = new ClanSkill("fortune_seeker", "Fortune Seeker", 
                "Increases chance of bonus drops from ores", SkillTree.MINER, 3);
        fortuneSeeker.addPrerequisite("mining_efficiency");
        for (int i = 1; i <= 3; i++) {
            fortuneSeeker.setLevelCost(i, i * 3);
            fortuneSeeker.addLevelEffect(i, "ore_yield", i * 15);
        }
        skills.put("fortune_seeker", fortuneSeeker);
        
        ClanSkill mineralDetection = new ClanSkill("mineral_detection", "Mineral Detection", 
                "Highlights nearby ores for a short time", SkillTree.MINER, 3);
        for (int i = 1; i <= 3; i++) {
            mineralDetection.setLevelCost(i, i * 3);
            mineralDetection.addLevelEffect(i, "ore_detection_range", i * 5);
            mineralDetection.addLevelEffect(i, "ore_detection_duration", i * 3);
        }
        skills.put("mineral_detection", mineralDetection);
        
        // Farmer Skills
        ClanSkill harvestMastery = new ClanSkill("harvest_mastery", "Harvest Mastery", 
                "Increases crop yields when harvesting", SkillTree.FARMER, 5);
        for (int i = 1; i <= 5; i++) {
            harvestMastery.setLevelCost(i, i * 2);
            harvestMastery.addLevelEffect(i, "food_yield", i * 10);
        }
        skills.put("harvest_mastery", harvestMastery);
        
        ClanSkill greenThumb = new ClanSkill("green_thumb", "Green Thumb", 
                "Accelerates crop growth in clan territories", SkillTree.FARMER, 3);
        greenThumb.addPrerequisite("harvest_mastery");
        for (int i = 1; i <= 3; i++) {
            greenThumb.setLevelCost(i, i * 3);
            greenThumb.addLevelEffect(i, "crop_growth", i * 15);
        }
        skills.put("green_thumb", greenThumb);
        
        ClanSkill animalHusbandry = new ClanSkill("animal_husbandry", "Animal Husbandry", 
                "Improves breeding and yields from farm animals", SkillTree.FARMER, 3);
        for (int i = 1; i <= 3; i++) {
            animalHusbandry.setLevelCost(i, i * 3);
            animalHusbandry.addLevelEffect(i, "breeding_cooldown_reduction", i * 10);
            animalHusbandry.addLevelEffect(i, "animal_product_yield", i * 15);
        }
        skills.put("animal_husbandry", animalHusbandry);
        
        // Builder Skills
        ClanSkill constructionMastery = new ClanSkill("construction_mastery", "Construction Mastery", 
                "Increases building speed and efficiency", SkillTree.BUILDER, 5);
        for (int i = 1; i <= 5; i++) {
            constructionMastery.setLevelCost(i, i * 2);
            constructionMastery.addLevelEffect(i, "build_speed", i * 10);
        }
        skills.put("construction_mastery", constructionMastery);
        
        ClanSkill resourceConservation = new ClanSkill("resource_conservation", "Resource Conservation", 
                "Chance to not consume materials when building", SkillTree.BUILDER, 3);
        resourceConservation.addPrerequisite("construction_mastery");
        for (int i = 1; i <= 3; i++) {
            resourceConservation.setLevelCost(i, i * 3);
            resourceConservation.addLevelEffect(i, "resource_conservation", i * 10);
        }
        skills.put("resource_conservation", resourceConservation);
        
        ClanSkill masterCrafting = new ClanSkill("master_crafting", "Master Crafting", 
                "Crafts higher quality items and tools", SkillTree.BUILDER, 3);
        for (int i = 1; i <= 3; i++) {
            masterCrafting.setLevelCost(i, i * 3);
            masterCrafting.addLevelEffect(i, "crafting_quality", i * 15);
            masterCrafting.addLevelEffect(i, "tool_durability", i * 20);
        }
        skills.put("master_crafting", masterCrafting);
        
        // Hunter Skills
        ClanSkill huntingProwess = new ClanSkill("hunting_prowess", "Hunting Prowess", 
                "Increases damage against animals and monsters", SkillTree.HUNTER, 5);
        for (int i = 1; i <= 5; i++) {
            huntingProwess.setLevelCost(i, i * 2);
            huntingProwess.addLevelEffect(i, "damage_bonus", i * 10);
        }
        skills.put("hunting_prowess", huntingProwess);
        
        ClanSkill bowMastery = new ClanSkill("bow_mastery", "Bow Mastery", 
                "Increases bow damage and accuracy", SkillTree.HUNTER, 3);
        bowMastery.addPrerequisite("hunting_prowess");
        for (int i = 1; i <= 3; i++) {
            bowMastery.setLevelCost(i, i * 3);
            bowMastery.addLevelEffect(i, "bow_damage", i * 15);
            bowMastery.addLevelEffect(i, "bow_accuracy", i * 10);
        }
        skills.put("bow_mastery", bowMastery);
        
        ClanSkill bountifulHunting = new ClanSkill("bountiful_hunting", "Bountiful Hunting", 
                "Increases drops from killed monsters and animals", SkillTree.HUNTER, 3);
        for (int i = 1; i <= 3; i++) {
            bountifulHunting.setLevelCost(i, i * 3);
            bountifulHunting.addLevelEffect(i, "mob_loot", i * 15);
            bountifulHunting.addLevelEffect(i, "rare_drop_chance", i * 5);
        }
        skills.put("bountiful_hunting", bountifulHunting);
    }
    
    /**
     * Loads member skills from configuration.
     */
    private void loadMemberSkills() {
        ConfigurationSection membersSection = skillsConfig.getConfigurationSection("members");
        
        if (membersSection == null) {
            return;
        }
        
        for (String uuidStr : membersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection memberSection = membersSection.getConfigurationSection(uuidStr);
                
                if (memberSection != null) {
                    MemberSkills skills = new MemberSkills(uuid);
                    
                    // Load skill points
                    skills.setSkillPoints(memberSection.getInt("skill_points", 0));
                    
                    // Load specialization if any
                    String specializationStr = memberSection.getString("specialization", "");
                    if (!specializationStr.isEmpty()) {
                        try {
                            SkillTree specialization = SkillTree.valueOf(specializationStr.toUpperCase());
                            skills.setSpecialization(specialization);
                        } catch (IllegalArgumentException e) {
                            // Invalid specialization, ignore
                        }
                    }
                    
                    // Load skill levels
                    ConfigurationSection skillsSection = memberSection.getConfigurationSection("skills");
                    if (skillsSection != null) {
                        for (String skillId : skillsSection.getKeys(false)) {
                            int level = skillsSection.getInt(skillId);
                            skills.setSkillLevel(skillId, level);
                        }
                    }
                    
                    memberSkills.put(uuid, skills);
                }
            } catch (IllegalArgumentException e) {
                // Invalid UUID, skip
            }
        }
    }
    
    /**
     * Saves member skills to configuration.
     */
    public void saveMemberSkills() {
        // Clear existing data
        skillsConfig.set("members", null);
        
        // Save current data
        for (Map.Entry<UUID, MemberSkills> entry : memberSkills.entrySet()) {
            UUID uuid = entry.getKey();
            MemberSkills skills = entry.getValue();
            
            String path = "members." + uuid.toString();
            
            // Save skill points
            skillsConfig.set(path + ".skill_points", skills.getSkillPoints());
            
            // Save specialization if any
            if (skills.getSpecialization() != null) {
                skillsConfig.set(path + ".specialization", skills.getSpecialization().name());
            }
            
            // Save skill levels
            Map<String, Integer> skillLevels = skills.getSkillLevels();
            for (Map.Entry<String, Integer> skillEntry : skillLevels.entrySet()) {
                if (skillEntry.getValue() > 0) {
                    skillsConfig.set(path + ".skills." + skillEntry.getKey(), skillEntry.getValue());
                }
            }
        }
        
        try {
            skillsConfig.save(skillsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save member skills data!");
            e.printStackTrace();
        }
    }
    
    /**
     * Gets a skill by its ID.
     * 
     * @param skillId The ID of the skill
     * @return The skill, or null if not found
     */
    public ClanSkill getSkill(String skillId) {
        return skills.get(skillId);
    }
    
    /**
     * Gets all skills.
     * 
     * @return A collection of all skills
     */
    public Collection<ClanSkill> getAllSkills() {
        return skills.values();
    }
    
    /**
     * Gets all skills in a specific tree.
     * 
     * @param tree The tree to filter by
     * @return A list of skills in the tree
     */
    public List<ClanSkill> getSkillsByTree(SkillTree tree) {
        List<ClanSkill> treeSkills = new ArrayList<>();
        
        for (ClanSkill skill : skills.values()) {
            if (skill.getTree() == tree) {
                treeSkills.add(skill);
            }
        }
        
        return treeSkills;
    }
    
    /**
     * Gets a member's skills.
     * 
     * @param playerUUID The UUID of the player
     * @return The member's skills
     */
    public MemberSkills getMemberSkills(UUID playerUUID) {
        MemberSkills skills = memberSkills.get(playerUUID);
        
        if (skills == null) {
            skills = new MemberSkills(playerUUID);
            memberSkills.put(playerUUID, skills);
        }
        
        return skills;
    }
    
    /**
     * Awards skill points to a player.
     * 
     * @param playerUUID The UUID of the player
     * @param points The number of points to award
     */
    public void awardSkillPoints(UUID playerUUID, int points) {
        if (points <= 0) {
            return;
        }
        
        MemberSkills skills = getMemberSkills(playerUUID);
        skills.addSkillPoints(points);
        
        Player player = plugin.getServer().getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            player.sendMessage(ChatColor.GREEN + "You have earned " + ChatColor.GOLD + points + 
                    ChatColor.GREEN + " skill points! Use /clan skills to spend them.");
        }
    }
    
    /**
     * Levels up a skill for a player.
     * 
     * @param player The player
     * @param skillId The ID of the skill to level up
     * @return True if the skill was successfully leveled up
     */
    public boolean levelUpSkill(Player player, String skillId) {
        ClanSkill skill = getSkill(skillId);
        
        if (skill == null) {
            player.sendMessage(ChatColor.RED + "Skill not found!");
            return false;
        }
        
        MemberSkills memberSkills = getMemberSkills(player.getUniqueId());
        
        if (!memberSkills.canLevelUp(skill, this)) {
            player.sendMessage(ChatColor.RED + "You cannot level up this skill!");
            
            int currentLevel = memberSkills.getSkillLevel(skillId);
            int nextLevel = currentLevel + 1;
            
            if (currentLevel >= skill.getMaxLevel()) {
                player.sendMessage(ChatColor.RED + "This skill is already at maximum level.");
            } else if (memberSkills.getSkillPoints() < skill.getLevelCost(nextLevel)) {
                player.sendMessage(ChatColor.RED + "You don't have enough skill points.");
            } else {
                for (String prereqId : skill.getPrerequisites()) {
                    if (memberSkills.getSkillLevel(prereqId) == 0) {
                        ClanSkill prereqSkill = getSkill(prereqId);
                        if (prereqSkill != null) {
                            player.sendMessage(ChatColor.RED + "You need to learn " + 
                                    ChatColor.YELLOW + prereqSkill.getName() + 
                                    ChatColor.RED + " first.");
                        }
                    }
                }
            }
            
            return false;
        }
        
        boolean success = memberSkills.levelUpSkill(skill);
        
        if (success) {
            int newLevel = memberSkills.getSkillLevel(skillId);
            
            player.sendMessage(ChatColor.GREEN + "You have leveled up " + 
                    ChatColor.YELLOW + skill.getName() + 
                    ChatColor.GREEN + " to level " + newLevel + "!");
            
            // Show new effects
            Map<String, Integer> effects = skill.getLevelEffects(newLevel);
            
            if (!effects.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "New effects:");
                
                for (Map.Entry<String, Integer> effect : effects.entrySet()) {
                    player.sendMessage(ChatColor.GRAY + "- " + 
                            formatEffectName(effect.getKey()) + ": " + 
                            formatEffectValue(effect.getKey(), effect.getValue()));
                }
            }
            
            // Check if specialization changed
            updateSpecialization(player.getUniqueId());
            
            // Save data
            saveMemberSkills();
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Updates a player's specialization based on their skill distribution.
     * 
     * @param playerUUID The UUID of the player
     */
    public void updateSpecialization(UUID playerUUID) {
        MemberSkills skills = getMemberSkills(playerUUID);
        SkillTree calculatedSpecialization = skills.calculateSpecialization(this);
        
        if (calculatedSpecialization != skills.getSpecialization()) {
            skills.setSpecialization(calculatedSpecialization);
            
            Player player = plugin.getServer().getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                if (calculatedSpecialization != null) {
                    player.sendMessage(ChatColor.GREEN + "Your specialization has changed to " + 
                            calculatedSpecialization.getColoredName() + ChatColor.GREEN + "!");
                    player.sendMessage(ChatColor.YELLOW + "You now receive bonuses to " + 
                            calculatedSpecialization.getDisplayName() + " skills.");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "You are no longer specialized in any skill tree.");
                }
            }
            
            saveMemberSkills();
        }
    }
    
    /**
     * Formats an effect name for display.
     * 
     * @param effectName The raw effect name
     * @return The formatted effect name
     */
    private String formatEffectName(String effectName) {
        String[] parts = effectName.split("_");
        StringBuilder formatted = new StringBuilder();
        
        for (String part : parts) {
            formatted.append(part.substring(0, 1).toUpperCase())
                    .append(part.substring(1))
                    .append(" ");
        }
        
        return formatted.toString().trim();
    }
    
    /**
     * Formats an effect value for display.
     * 
     * @param effectName The effect name
     * @param value The effect value
     * @return The formatted effect value
     */
    private String formatEffectValue(String effectName, int value) {
        if (effectName.endsWith("_percent")) {
            return "+" + value + "%";
        } else if (effectName.contains("reduction")) {
            return "-" + value + "%";
        } else if (value > 0) {
            return "+" + value;
        } else {
            return String.valueOf(value);
        }
    }
    
    /**
     * Displays skill information to a player.
     * 
     * @param player The player to display the info to
     * @param skillId The ID of the skill to display
     */
    public void displaySkillInfo(Player player, String skillId) {
        ClanSkill skill = getSkill(skillId);
        
        if (skill == null) {
            player.sendMessage(ChatColor.RED + "Skill not found!");
            return;
        }
        
        MemberSkills memberSkills = getMemberSkills(player.getUniqueId());
        int currentLevel = memberSkills.getSkillLevel(skillId);
        int skillPoints = memberSkills.getSkillPoints();
        
        player.sendMessage(ChatColor.GOLD + "=== Skill Information ===");
        player.sendMessage(skill.getDisplayString(currentLevel, skillPoints));
    }
    
    /**
     * Displays skill tree information to a player.
     * 
     * @param player The player to display the info to
     * @param tree The skill tree to display
     */
    public void displaySkillTreeInfo(Player player, SkillTree tree) {
        List<ClanSkill> treeSkills = getSkillsByTree(tree);
        MemberSkills memberSkills = getMemberSkills(player.getUniqueId());
        
        player.sendMessage(ChatColor.GOLD + "=== " + tree.getColoredName() + ChatColor.GOLD + " Skills ===");
        player.sendMessage(ChatColor.GRAY + tree.getDescription());
        player.sendMessage(ChatColor.YELLOW + "Skill Points: " + ChatColor.WHITE + memberSkills.getSkillPoints());
        
        // Show specialization status
        if (memberSkills.getSpecialization() == tree) {
            player.sendMessage(ChatColor.GOLD + "You are specialized in this tree! (+20% to effects)");
        }
        
        // Show skills
        for (ClanSkill skill : treeSkills) {
            int level = memberSkills.getSkillLevel(skill.getId());
            
            ChatColor nameColor = level > 0 ? ChatColor.GREEN : ChatColor.YELLOW;
            player.sendMessage(nameColor + skill.getName() + ChatColor.GRAY + " [" + 
                    level + "/" + skill.getMaxLevel() + "]");
        }
    }
    
    /**
     * Displays a player's skill overview.
     * 
     * @param player The player to display the overview to
     */
    public void displaySkillOverview(Player player) {
        MemberSkills memberSkills = getMemberSkills(player.getUniqueId());
        
        player.sendMessage(ChatColor.GOLD + "=== Your Skills ===");
        player.sendMessage(ChatColor.YELLOW + "Skill Points: " + ChatColor.WHITE + memberSkills.getSkillPoints());
        
        // Show specialization if any
        SkillTree specialization = memberSkills.getSpecialization();
        if (specialization != null) {
            player.sendMessage(ChatColor.YELLOW + "Specialization: " + specialization.getColoredName());
        } else {
            player.sendMessage(ChatColor.YELLOW + "Specialization: " + ChatColor.GRAY + "None");
        }
        
        // Show skills by tree
        for (SkillTree tree : SkillTree.values()) {
            List<ClanSkill> treeSkills = getSkillsByTree(tree);
            
            int treeLevels = 0;
            int learnedSkills = 0;
            
            for (ClanSkill skill : treeSkills) {
                int level = memberSkills.getSkillLevel(skill.getId());
                
                if (level > 0) {
                    treeLevels += level;
                    learnedSkills++;
                }
            }
            
            if (treeLevels > 0) {
                player.sendMessage(tree.getColoredName() + ChatColor.WHITE + ": " + 
                        learnedSkills + "/" + treeSkills.size() + " skills, " + 
                        treeLevels + " total levels");
            }
        }
        
        // Show top skills
        player.sendMessage(ChatColor.GOLD + "Top Skills:");
        
        Map<String, Integer> skillLevels = memberSkills.getSkillLevels();
        List<Map.Entry<String, Integer>> sortedSkills = new ArrayList<>(skillLevels.entrySet());
        
        sortedSkills.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        
        int count = 0;
        for (Map.Entry<String, Integer> entry : sortedSkills) {
            if (entry.getValue() <= 0) {
                continue;
            }
            
            ClanSkill skill = getSkill(entry.getKey());
            
            if (skill != null) {
                player.sendMessage(ChatColor.GRAY + "- " + 
                        skill.getTree().getColor() + skill.getName() + 
                        ChatColor.WHITE + " (Level " + entry.getValue() + ")");
                
                count++;
                
                if (count >= 5) {
                    break;
                }
            }
        }
    }
    
    /**
     * Applies all skill effects to a value.
     * 
     * @param baseValue The base value
     * @param player The player
     * @param effectName The name of the effect to apply
     * @return The modified value
     */
    public double applySkillEffects(double baseValue, Player player, String effectName) {
        MemberSkills memberSkills = getMemberSkills(player.getUniqueId());
        int effectValue = memberSkills.getTotalEffectValue(effectName, this);
        
        if (effectName.endsWith("_percent") || effectName.contains("bonus")) {
            return baseValue * (1 + effectValue / 100.0);
        } else if (effectName.contains("reduction")) {
            return baseValue * (1 - effectValue / 100.0);
        } else {
            return baseValue + effectValue;
        }
    }
}
package com.minecraft.clanplugin.listeners;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.skills.MemberSkills;
import com.minecraft.clanplugin.skills.SkillTree;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listens for events related to player activities and awards skill progression
 * and specialization progress accordingly.
 */
public class SkillProgressionListener implements Listener {
    
    private final ClanPlugin plugin;
    private final Map<UUID, Map<SkillTree, Integer>> activityPoints;
    private final int POINTS_THRESHOLD = 100; // Points needed for specialization update
    
    /**
     * Creates a new skill progression listener.
     * 
     * @param plugin The clan plugin instance
     */
    public SkillProgressionListener(ClanPlugin plugin) {
        this.plugin = plugin;
        this.activityPoints = new HashMap<>();
    }
    
    /**
     * Handles the block break event for mining specialization.
     * 
     * @param event The block break event
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Get player's clan and skills
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;
        
        MemberSkills skills = plugin.getSkillManager().getMemberSkills(player.getUniqueId());
        if (skills == null) return;
        
        // Award points based on block type
        int points = 0;
        Material type = block.getType();
        
        // Mining activity points
        if (isMiningBlock(type)) {
            switch (type) {
                case DIAMOND_ORE:
                case DEEPSLATE_DIAMOND_ORE:
                    points = 5;
                    break;
                case GOLD_ORE:
                case DEEPSLATE_GOLD_ORE:
                case ANCIENT_DEBRIS:
                    points = 4;
                    break;
                case IRON_ORE:
                case DEEPSLATE_IRON_ORE:
                case LAPIS_ORE:
                case DEEPSLATE_LAPIS_ORE:
                case REDSTONE_ORE:
                case DEEPSLATE_REDSTONE_ORE:
                    points = 3;
                    break;
                case COPPER_ORE:
                case DEEPSLATE_COPPER_ORE:
                case COAL_ORE:
                case DEEPSLATE_COAL_ORE:
                    points = 2;
                    break;
                default:
                    points = 1;
                    break;
            }
            
            addActivityPoints(player.getUniqueId(), SkillTree.MINER, points);
            
            // Award skill points occasionally for mining activity
            if (Math.random() < 0.05) { // 5% chance
                skills.addSkillPoints(1);
                player.sendMessage(ChatColor.GREEN + "You gained 1 skill point from mining!");
                plugin.getSkillManager().saveMemberSkills();
            }
        }
    }
    
    /**
     * Handles the block harvest event for farming specialization.
     * 
     * @param event The player harvest block event
     */
    @EventHandler
    public void onHarvest(PlayerHarvestBlockEvent event) {
        Player player = event.getPlayer();
        
        // Get player's clan and skills
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;
        
        MemberSkills skills = plugin.getSkillManager().getMemberSkills(player.getUniqueId());
        if (skills == null) return;
        
        // Award farming points based on crop type
        int points = 0;
        Block block = event.getHarvestedBlock();
        
        if (isFarmingBlock(block.getType())) {
            // More points for rarer crops
            switch (block.getType()) {
                case SWEET_BERRY_BUSH:
                case COCOA:
                    points = 3;
                    break;
                case WHEAT:
                case CARROTS:
                case POTATOES:
                case BEETROOTS:
                case NETHER_WART:
                    points = 2;
                    break;
                default:
                    points = 1;
                    break;
            }
            
            addActivityPoints(player.getUniqueId(), SkillTree.FARMER, points);
            
            // Award skill points occasionally for farming activity
            if (Math.random() < 0.05) { // 5% chance
                skills.addSkillPoints(1);
                player.sendMessage(ChatColor.GREEN + "You gained 1 skill point from farming!");
                plugin.getSkillManager().saveMemberSkills();
            }
        }
    }
    
    /**
     * Handles the entity death event for hunting specialization.
     * 
     * @param event The entity death event
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity killed = event.getEntity();
        Player killer = event.getEntity().getKiller();
        
        if (killer == null) return;
        
        // Get player's clan and skills
        Clan clan = plugin.getStorageManager().getPlayerClan(killer.getUniqueId());
        if (clan == null) return;
        
        MemberSkills skills = plugin.getSkillManager().getMemberSkills(killer.getUniqueId());
        if (skills == null) return;
        
        // Award hunter points based on entity type
        int points = 0;
        EntityType type = killed.getType();
        
        if (isHostileEntity(type)) {
            // More points for more dangerous mobs
            switch (type) {
                case ENDER_DRAGON:
                case WITHER:
                    points = 20;
                    break;
                case ELDER_GUARDIAN:
                case WARDEN:
                    points = 15;
                    break;
                case RAVAGER:
                case EVOKER:
                    points = 10;
                    break;
                case ENDERMAN:
                case BLAZE:
                case WITCH:
                    points = 5;
                    break;
                case CREEPER:
                case SKELETON:
                case SPIDER:
                case ZOMBIE:
                    points = 3;
                    break;
                default:
                    points = 1;
                    break;
            }
            
            addActivityPoints(killer.getUniqueId(), SkillTree.HUNTER, points);
            
            // Award skill points occasionally for hunting activity
            if (Math.random() < 0.1) { // 10% chance for hostile mobs
                skills.addSkillPoints(1);
                killer.sendMessage(ChatColor.GREEN + "You gained 1 skill point from hunting!");
                plugin.getSkillManager().saveMemberSkills();
            }
        }
    }
    
    /**
     * Handles the block place event for building specialization.
     * 
     * @param event The block place event
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Get player's clan and skills
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;
        
        MemberSkills skills = plugin.getSkillManager().getMemberSkills(player.getUniqueId());
        if (skills == null) return;
        
        // Award building points
        int points = 0;
        Material type = block.getType();
        
        if (isBuildingBlock(type)) {
            // More points for more advanced building blocks
            if (isDecorativeBlock(type)) {
                points = 2;
            } else {
                points = 1;
            }
            
            addActivityPoints(player.getUniqueId(), SkillTree.BUILDER, points);
            
            // Award skill points after placing many blocks
            // Less frequent than other activities since building involves placing many blocks
            if (Math.random() < 0.01) { // 1% chance
                skills.addSkillPoints(1);
                player.sendMessage(ChatColor.GREEN + "You gained 1 skill point from building!");
                plugin.getSkillManager().saveMemberSkills();
            }
        }
    }
    
    /**
     * Adds activity points to a player for a specific skill tree.
     * Updates specialization if threshold is reached.
     * 
     * @param uuid The UUID of the player
     * @param tree The skill tree to add points for
     * @param points The number of points to add
     */
    private void addActivityPoints(UUID uuid, SkillTree tree, int points) {
        // Initialize player's activity points if not already tracked
        activityPoints.putIfAbsent(uuid, new HashMap<>());
        Map<SkillTree, Integer> playerPoints = activityPoints.get(uuid);
        
        // Add points for this tree
        int currentPoints = playerPoints.getOrDefault(tree, 0);
        playerPoints.put(tree, currentPoints + points);
        
        // Check if specialization update is needed
        if (currentPoints + points >= POINTS_THRESHOLD) {
            updateSpecialization(uuid, tree);
            
            // Reset points for this tree after specialization update
            playerPoints.put(tree, 0);
        }
    }
    
    /**
     * Updates a player's specialization based on their activity.
     * 
     * @param uuid The UUID of the player
     * @param tree The skill tree to specialize in
     */
    private void updateSpecialization(UUID uuid, SkillTree tree) {
        Player player = plugin.getServer().getPlayer(uuid);
        if (player == null) return;
        
        MemberSkills skills = plugin.getSkillManager().getMemberSkills(uuid);
        if (skills == null) return;
        
        // Check if already specialized in this tree
        if (tree.equals(skills.getSpecialization())) {
            // Already specialized, just award extra skill points
            skills.addSkillPoints(5);
            player.sendMessage(ChatColor.GREEN + "Your expertise in " + tree.getColoredName() + 
                              ChatColor.GREEN + " has deepened! You gained 5 skill points.");
        } else {
            // Update specialization
            SkillTree oldSpec = skills.getSpecialization();
            skills.setSpecialization(tree);
            
            // Notify player
            player.sendMessage(ChatColor.GOLD + "=== Specialization Update ===");
            
            if (oldSpec != null) {
                player.sendMessage(ChatColor.YELLOW + "Your specialization has changed from " + 
                                  oldSpec.getColoredName() + ChatColor.YELLOW + " to " + 
                                  tree.getColoredName() + ChatColor.YELLOW + "!");
            } else {
                player.sendMessage(ChatColor.YELLOW + "You have specialized in " + 
                                  tree.getColoredName() + ChatColor.YELLOW + "!");
            }
            
            player.sendMessage(ChatColor.GREEN + "You gained 10 skill points to invest in your new specialization!");
            skills.addSkillPoints(10);
            
            // Show tree description
            player.sendMessage(ChatColor.GRAY + tree.getDescription());
            player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/clan gui" + 
                              ChatColor.GRAY + " to allocate your skill points.");
        }
        
        // Save updated skills
        plugin.getSkillManager().saveMemberSkills();
    }
    
    /**
     * Checks if a block is a mining-related block.
     * 
     * @param type The material type
     * @return True if mining related
     */
    private boolean isMiningBlock(Material type) {
        switch (type) {
            case STONE:
            case COBBLESTONE:
            case DEEPSLATE:
            case ANDESITE:
            case DIORITE:
            case GRANITE:
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
            case NETHER_GOLD_ORE:
            case NETHER_QUARTZ_ORE:
            case ANCIENT_DEBRIS:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Checks if a block is a farming-related block.
     * 
     * @param type The material type
     * @return True if farming related
     */
    private boolean isFarmingBlock(Material type) {
        switch (type) {
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
            case MELON:
            case PUMPKIN:
            case SUGAR_CANE:
            case CACTUS:
            case BAMBOO:
            case COCOA:
            case NETHER_WART:
            case SWEET_BERRY_BUSH:
            case KELP:
            case SEA_PICKLE:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Checks if an entity is a hostile mob.
     * 
     * @param type The entity type
     * @return True if hostile
     */
    private boolean isHostileEntity(EntityType type) {
        switch (type) {
            case ZOMBIE:
            case SKELETON:
            case CREEPER:
            case SPIDER:
            case CAVE_SPIDER:
            case ENDERMAN:
            case WITCH:
            case SLIME:
            case MAGMA_CUBE:
            case BLAZE:
            case GHAST:
            case WITHER_SKELETON:
            case GUARDIAN:
            case ELDER_GUARDIAN:
            case SHULKER:
            case EVOKER:
            case VINDICATOR:
            case VEX:
            case PHANTOM:
            case DROWNED:
            case PILLAGER:
            case RAVAGER:
            case HOGLIN:
            case PIGLIN:
            case PIGLIN_BRUTE:
            case ZOGLIN:
            case WITHER:
            case ENDER_DRAGON:
            case WARDEN:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Checks if a block is a building-related block.
     * 
     * @param type The material type
     * @return True if building related
     */
    private boolean isBuildingBlock(Material type) {
        // Ignore natural blocks and tools/weapons
        if (type.isAir() || type.isItem() || isNaturalBlock(type)) {
            return false;
        }
        
        // Consider most solid blocks as building blocks
        return type.isSolid() || isDecorativeBlock(type);
    }
    
    /**
     * Checks if a block is a decorative building block.
     * 
     * @param type The material type
     * @return True if decorative
     */
    private boolean isDecorativeBlock(Material type) {
        switch (type) {
            case CRAFTING_TABLE:
            case FURNACE:
            case BLAST_FURNACE:
            case SMOKER:
            case SMITHING_TABLE:
            case LOOM:
            case CARTOGRAPHY_TABLE:
            case FLETCHING_TABLE:
            case BREWING_STAND:
            case ENCHANTING_TABLE:
            case BOOKSHELF:
            case CHEST:
            case BARREL:
            case LANTERN:
            case TORCH:
            case SOUL_TORCH:
            case CAMPFIRE:
            case SOUL_CAMPFIRE:
            case GLASS:
            case GLASS_PANE:
            case FLOWER_POT:
            case PAINTING:
            case ITEM_FRAME:
            case ARMOR_STAND:
                return true;
            default:
                // Check if it's a decorative variant
                String name = type.name();
                return name.contains("WALL") || 
                       name.contains("FENCE") || 
                       name.contains("DOOR") || 
                       name.contains("TRAPDOOR") || 
                       name.contains("STAIRS") || 
                       name.contains("SLAB") || 
                       name.contains("CARPET") || 
                       name.contains("BED") ||
                       name.contains("STAINED_GLASS") ||
                       name.contains("COLORED_GLASS") ||
                       name.contains("_GLASS_") ||
                       name.contains("_TERRACOTTA") ||
                       name.contains("_WOOL");
        }
    }
    
    /**
     * Checks if a block is a naturally occurring block.
     * 
     * @param type The material type
     * @return True if natural
     */
    private boolean isNaturalBlock(Material type) {
        switch (type) {
            case GRASS:
            case TALL_GRASS:
            case DIRT:
            case GRAVEL:
            case SAND:
            case CLAY:
            case SNOW:
            case ICE:
            case PACKED_ICE:
            case BLUE_ICE:
            case WATER:
            case LAVA:
            case BEDROCK:
                return true;
            default:
                // Check if it's natural by name
                String name = type.name();
                return name.contains("SAPLING") || 
                       name.contains("LEAVES") || 
                       name.contains("LOG") || 
                       name.contains("FLOWER") || 
                       name.contains("MUSHROOM");
        }
    }
}
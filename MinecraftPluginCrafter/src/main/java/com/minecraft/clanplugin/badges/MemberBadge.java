package com.minecraft.clanplugin.badges;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a personalized clan member badge.
 * Badges are visual indicators of a member's status, achievements,
 * and contributions to the clan.
 */
public class MemberBadge {

    private final UUID badgeId;
    private final String name;
    private final String description;
    private final BadgeType type;
    private final Material iconMaterial;
    private final int tier;
    private final ChatColor color;
    private final long creationTime;
    private final boolean hidden;

    /**
     * Badge types for different achievements and milestones.
     */
    public enum BadgeType {
        // Role-based badges
        LEADER(ChatColor.GOLD, "Clan Leader", Material.DRAGON_HEAD),
        OFFICER(ChatColor.DARK_PURPLE, "Clan Officer", Material.GOLDEN_HELMET),
        VETERAN(ChatColor.AQUA, "Clan Veteran", Material.DIAMOND_SWORD),
        MEMBER(ChatColor.GREEN, "Clan Member", Material.IRON_SWORD),
        RECRUIT(ChatColor.GRAY, "Clan Recruit", Material.WOODEN_SWORD),
        
        // Activity badges
        ACTIVE_MEMBER(ChatColor.YELLOW, "Active Member", Material.CLOCK),
        DEDICATED_PLAYER(ChatColor.LIGHT_PURPLE, "Dedicated Player", Material.CLOCK),
        
        // Contribution badges
        TOP_CONTRIBUTOR(ChatColor.GOLD, "Top Contributor", Material.GOLD_INGOT),
        TREASURY_DONOR(ChatColor.GREEN, "Treasury Donor", Material.EMERALD),
        RESOURCE_GATHERER(ChatColor.DARK_GREEN, "Resource Gatherer", Material.IRON_PICKAXE),
        
        // Combat badges
        WARRIOR(ChatColor.RED, "Clan Warrior", Material.IRON_SWORD),
        DEFENDER(ChatColor.BLUE, "Clan Defender", Material.SHIELD),
        WAR_HERO(ChatColor.DARK_RED, "War Hero", Material.DIAMOND_SWORD),
        
        // Achievement badges
        ACHIEVEMENT_HUNTER(ChatColor.LIGHT_PURPLE, "Achievement Hunter", Material.EXPERIENCE_BOTTLE),
        MASTER_BUILDER(ChatColor.YELLOW, "Master Builder", Material.BRICKS),
        EXPLORER(ChatColor.DARK_GREEN, "Explorer", Material.COMPASS),
        
        // Special badges
        FOUNDER(ChatColor.GOLD, "Clan Founder", Material.NETHER_STAR),
        RECRUITER(ChatColor.AQUA, "Recruiter", Material.NAME_TAG),
        DIPLOMAT(ChatColor.WHITE, "Diplomat", Material.WRITABLE_BOOK),
        
        // Seasonal badges
        WINTER_WARRIOR(ChatColor.AQUA, "Winter Warrior", Material.SNOW_BLOCK),
        SUMMER_CHAMPION(ChatColor.YELLOW, "Summer Champion", Material.SUNFLOWER),
        HALLOWEEN_HERO(ChatColor.DARK_PURPLE, "Halloween Hero", Material.JACK_O_LANTERN),
        
        // Custom badge
        CUSTOM(ChatColor.WHITE, "Custom Badge", Material.EMERALD);
        
        private final ChatColor defaultColor;
        private final String defaultName;
        private final Material defaultIcon;
        
        BadgeType(ChatColor defaultColor, String defaultName, Material defaultIcon) {
            this.defaultColor = defaultColor;
            this.defaultName = defaultName;
            this.defaultIcon = defaultIcon;
        }
        
        public ChatColor getDefaultColor() {
            return defaultColor;
        }
        
        public String getDefaultName() {
            return defaultName;
        }
        
        public Material getDefaultIcon() {
            return defaultIcon;
        }
    }
    
    /**
     * Create a new badge.
     * 
     * @param name The badge name
     * @param description The badge description
     * @param type The badge type
     * @param tier The badge tier (1-5)
     * @param color The badge color
     * @param iconMaterial The badge icon material
     * @param hidden Whether the badge is hidden until unlocked
     */
    public MemberBadge(String name, String description, BadgeType type, 
                       int tier, ChatColor color, Material iconMaterial, boolean hidden) {
        this.badgeId = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.type = type;
        this.tier = Math.max(1, Math.min(5, tier)); // Clamp between 1-5
        this.color = color;
        this.iconMaterial = iconMaterial;
        this.creationTime = System.currentTimeMillis();
        this.hidden = hidden;
    }
    
    /**
     * Create a new badge with a predefined type.
     * 
     * @param type The badge type
     * @param tier The badge tier (1-5)
     * @param description Custom description (or null for default)
     */
    public MemberBadge(BadgeType type, int tier, String description) {
        this.badgeId = UUID.randomUUID();
        this.type = type;
        this.tier = Math.max(1, Math.min(5, tier)); // Clamp between 1-5
        this.name = type.getDefaultName() + (tier > 1 ? " " + getRomanNumeral(tier) : "");
        this.description = description != null ? description : getDefaultDescription(type, tier);
        this.color = type.getDefaultColor();
        this.iconMaterial = type.getDefaultIcon();
        this.creationTime = System.currentTimeMillis();
        this.hidden = false;
    }
    
    /**
     * Create a fully custom badge.
     * 
     * @param name Custom badge name
     * @param description Custom badge description
     * @param iconMaterial Custom badge icon
     * @param color Custom badge color
     * @param tier Badge tier (1-5)
     * @param hidden Whether the badge is hidden
     * @return A new custom badge
     */
    public static MemberBadge createCustomBadge(String name, String description,
                                              Material iconMaterial, ChatColor color,
                                              int tier, boolean hidden) {
        return new MemberBadge(name, description, BadgeType.CUSTOM, tier, color, iconMaterial, hidden);
    }
    
    /**
     * Create a badge for a clan role.
     * 
     * @param role The role number (3=leader, 2=officer, 1=member, 0=recruit)
     * @return A badge appropriate for the role
     */
    public static MemberBadge createRoleBadge(int role) {
        BadgeType type;
        switch (role) {
            case 3: type = BadgeType.LEADER; break;
            case 2: type = BadgeType.OFFICER; break;
            case 1: type = BadgeType.VETERAN; break;
            case 0: 
            default: type = BadgeType.RECRUIT; break;
        }
        
        return new MemberBadge(type, 1, null);
    }
    
    /**
     * Create a founder badge.
     * 
     * @return A founder badge
     */
    public static MemberBadge createFounderBadge() {
        return new MemberBadge(BadgeType.FOUNDER, 1, null);
    }
    
    /**
     * Get default description for a badge type and tier.
     * 
     * @param type The badge type
     * @param tier The badge tier
     * @return A default description
     */
    private String getDefaultDescription(BadgeType type, int tier) {
        switch (type) {
            case LEADER:
                return "Leader of the clan, responsible for clan direction and management.";
            case OFFICER:
                return "Officer of the clan, helping manage its operations.";
            case VETERAN:
                return "Long-standing dedicated member of the clan.";
            case MEMBER:
                return "A full member of the clan.";
            case RECRUIT:
                return "A new member of the clan still proving themselves.";
            case ACTIVE_MEMBER:
                if (tier == 1) return "Awarded for regular participation in clan activities.";
                return "Awarded for impressive dedication to clan activities.";
            case DEDICATED_PLAYER:
                return "Awarded for playing " + (tier * 30) + "+ hours with the clan.";
            case TOP_CONTRIBUTOR:
                return "One of the top " + (tier == 1 ? "" : tier * 5) + " contributors to the clan.";
            case TREASURY_DONOR:
                return "Donated over " + (1000 * tier) + " to the clan treasury.";
            case RESOURCE_GATHERER:
                return "Gathered and contributed significant resources to the clan.";
            case WARRIOR:
                return "Participated in " + (3 * tier) + "+ clan wars.";
            case DEFENDER:
                return "Successfully defended clan territory " + (2 * tier) + "+ times.";
            case WAR_HERO:
                return "Made exceptional contributions during clan wars.";
            case ACHIEVEMENT_HUNTER:
                return "Helped unlock " + (tier * 5) + "+ clan achievements.";
            case MASTER_BUILDER:
                return "Made significant contributions to clan buildings.";
            case EXPLORER:
                return "Helped discover and claim new territories for the clan.";
            case FOUNDER:
                return "One of the original founders of the clan.";
            case RECRUITER:
                return "Brought " + (tier * 3) + "+ new members to the clan.";
            case DIPLOMAT:
                return "Established " + (tier * 2) + "+ alliances for the clan.";
            case WINTER_WARRIOR:
                return "Participated in clan activities during the winter season.";
            case SUMMER_CHAMPION:
                return "Participated in clan activities during the summer season.";
            case HALLOWEEN_HERO:
                return "Participated in clan activities during Halloween.";
            case CUSTOM:
            default:
                return "A special badge awarded for unique contributions to the clan.";
        }
    }
    
    /**
     * Convert a number to Roman numeral.
     * 
     * @param number The number to convert
     * @return The Roman numeral
     */
    private String getRomanNumeral(int number) {
        if (number < 1 || number > 5) {
            return String.valueOf(number);
        }
        
        String[] numerals = {"I", "II", "III", "IV", "V"};
        return numerals[number - 1];
    }
    
    /**
     * Get the badge ID.
     * 
     * @return The badge UUID
     */
    public UUID getBadgeId() {
        return badgeId;
    }
    
    /**
     * Get the badge name.
     * 
     * @return The badge name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the badge description.
     * 
     * @return The badge description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the badge type.
     * 
     * @return The badge type
     */
    public BadgeType getType() {
        return type;
    }
    
    /**
     * Get the badge tier.
     * 
     * @return The badge tier (1-5)
     */
    public int getTier() {
        return tier;
    }
    
    /**
     * Get the badge color.
     * 
     * @return The badge color
     */
    public ChatColor getColor() {
        return color;
    }
    
    /**
     * Get the badge icon material.
     * 
     * @return The badge icon material
     */
    public Material getIconMaterial() {
        return iconMaterial;
    }
    
    /**
     * Get the time the badge was created.
     * 
     * @return The creation time
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    /**
     * Check if the badge is hidden until unlocked.
     * 
     * @return True if the badge is hidden
     */
    public boolean isHidden() {
        return hidden;
    }
    
    /**
     * Create an item stack representation of this badge.
     * 
     * @return An ItemStack representing the badge
     */
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();
        
        // Set the name with tier indicators for higher tiers
        meta.setDisplayName(color + name);
        
        // Add description and tier as lore
        List<String> lore = new ArrayList<>();
        
        // Add tier stars for visual representation
        StringBuilder tierStars = new StringBuilder();
        for (int i = 0; i < tier; i++) {
            tierStars.append("★");
        }
        for (int i = tier; i < 5; i++) {
            tierStars.append("☆");
        }
        
        lore.add(color + tierStars.toString());
        lore.add(ChatColor.GRAY + "Tier " + tier);
        lore.add("");
        
        // Split description into multiple lines if needed
        String[] descriptionLines = description.split("\\. ");
        for (String line : descriptionLines) {
            if (!line.endsWith(".")) line += ".";
            lore.add(ChatColor.WHITE + line);
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create a displayable text representation of this badge.
     * 
     * @return A text representation for chat display
     */
    public String getDisplayText() {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < tier; i++) {
            stars.append("★");
        }
        
        return color + "[" + name + " " + stars + "]";
    }
}
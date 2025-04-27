package com.minecraft.clanplugin.shop;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.ClanRole;
import com.minecraft.clanplugin.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the clan shop system, allowing players to purchase special items and upgrades.
 * Uses a dedicated configuration file for prices and shop structure.
 */
public class ClanShop {
    private final ClanPlugin plugin;
    private final Map<String, ShopCategory> categories;
    private final Map<String, ShopItem> allItems;
    private final ShopManager shopManager;
    
    /**
     * Creates a new clan shop manager.
     * 
     * @param plugin The clan plugin instance
     */
    public ClanShop(ClanPlugin plugin) {
        this.plugin = plugin;
        this.categories = new HashMap<>();
        this.allItems = new HashMap<>();
        this.shopManager = new ShopManager(plugin);
        
        loadShopItems();
    }
    
    /**
     * Loads shop items from dedicated shop configuration.
     */
    private void loadShopItems() {
        FileConfiguration shopConfig = shopManager.getShopConfig();
        
        if (shopConfig == null) {
            // No shop configuration found, create default shop
            plugin.getLogger().warning("Failed to load shop config. Using defaults.");
            createDefaultShop();
            return;
        }
        
        // Load categories
        ConfigurationSection categoriesSection = shopConfig.getConfigurationSection("categories");
        if (categoriesSection != null) {
            for (String categoryKey : categoriesSection.getKeys(false)) {
                ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryKey);
                if (categorySection != null) {
                    String name = categorySection.getString("name", categoryKey);
                    String description = categorySection.getString("description", "");
                    Material icon = shopManager.getMaterial("categories." + categoryKey + ".icon", Material.CHEST);
                    
                    ShopCategory category = new ShopCategory(categoryKey, name, description, icon);
                    categories.put(categoryKey, category);
                }
            }
        }
        
        // Load items
        ConfigurationSection itemsSection = shopConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String itemKey : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                if (itemSection != null) {
                    String name = itemSection.getString("name", itemKey);
                    String description = itemSection.getString("description", "");
                    double price = itemSection.getDouble("price", 100.0);
                    String categoryKey = itemSection.getString("category", "misc");
                    Material material = shopManager.getMaterial(itemKey + ".material", Material.STONE);
                    int quantity = itemSection.getInt("quantity", 1);
                    int minClanLevel = itemSection.getInt("min_clan_level", 1);
                    boolean clanPurchase = itemSection.getBoolean("clan_purchase", false);
                    boolean unique = itemSection.getBoolean("unique", false);
                    
                    ShopItem item = new ShopItem(itemKey, name, description, price, material, quantity, minClanLevel, clanPurchase, unique);
                    
                    // Add special item properties if defined
                    ConfigurationSection propertiesSection = itemSection.getConfigurationSection("properties");
                    if (propertiesSection != null) {
                        for (String property : propertiesSection.getKeys(false)) {
                            item.setProperty(property, propertiesSection.get(property));
                        }
                    }
                    
                    // Add to category
                    ShopCategory category = categories.get(categoryKey);
                    if (category != null) {
                        category.addItem(item);
                    } else {
                        // Create miscellaneous category if it doesn't exist
                        if (!categories.containsKey("misc")) {
                            ShopCategory miscCategory = new ShopCategory("misc", "Miscellaneous", "Various items", Material.CHEST);
                            categories.put("misc", miscCategory);
                        }
                        categories.get("misc").addItem(item);
                    }
                    
                    // Add to all items map
                    allItems.put(itemKey, item);
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + allItems.size() + " shop items in " + categories.size() + " categories");
    }
    
    /**
     * Creates a default shop configuration.
     */
    private void createDefaultShop() {
        // Create default categories
        ShopCategory flagsCategory = new ShopCategory("flags", "Clan Flags", "Custom flags for your clan", Material.WHITE_WOOL);
        categories.put("flags", flagsCategory);
        
        ShopCategory armorCategory = new ShopCategory("armor", "Clan Armor", "Special armor for your clan", Material.DIAMOND_CHESTPLATE);
        categories.put("armor", armorCategory);
        
        ShopCategory upgradesCategory = new ShopCategory("upgrades", "Clan Upgrades", "Special upgrades for your clan", Material.NETHER_STAR);
        categories.put("upgrades", upgradesCategory);
        
        ShopCategory specialCategory = new ShopCategory("special", "Special Items", "Unique items for clan members", Material.ENDER_CHEST);
        categories.put("special", specialCategory);
        
        // Add default items
        
        // Flags
        ShopItem basicFlag = new ShopItem("basic_flag", "Basic Clan Flag", "A standard flag with your clan's color", 500.0, Material.WHITE_BANNER, 1, 1, true, false);
        flagsCategory.addItem(basicFlag);
        allItems.put("basic_flag", basicFlag);
        
        ShopItem customFlag = new ShopItem("custom_flag", "Custom Clan Flag", "A customizable flag with patterns", 1500.0, Material.WHITE_BANNER, 1, 2, true, false);
        flagsCategory.addItem(customFlag);
        allItems.put("custom_flag", customFlag);
        
        // Armor
        ShopItem standardArmor = new ShopItem("standard_armor", "Standard Clan Armor", "Basic armor with your clan's color", 1000.0, Material.IRON_CHESTPLATE, 1, 1, true, false);
        armorCategory.addItem(standardArmor);
        allItems.put("standard_armor", standardArmor);
        
        ShopItem eliteArmor = new ShopItem("elite_armor", "Elite Clan Armor", "Enhanced armor with your clan's color and effects", 3000.0, Material.DIAMOND_CHESTPLATE, 1, 3, true, false);
        armorCategory.addItem(eliteArmor);
        allItems.put("elite_armor", eliteArmor);
        
        // Upgrades
        ShopItem memberSlot = new ShopItem("member_slot", "Additional Member Slot", "Increases max clan members by 1", 2000.0, Material.PLAYER_HEAD, 1, 1, true, true);
        memberSlot.setProperty("slot_increase", 1);
        upgradesCategory.addItem(memberSlot);
        allItems.put("member_slot", memberSlot);
        
        ShopItem territorySlot = new ShopItem("territory_slot", "Additional Territory Slot", "Increases max clan territories by 1", 3000.0, Material.GRASS_BLOCK, 1, 1, true, true);
        territorySlot.setProperty("territory_increase", 1);
        upgradesCategory.addItem(territorySlot);
        allItems.put("territory_slot", territorySlot);
        
        ShopItem incomeBoost = new ShopItem("income_boost", "Income Boost", "Increases clan income by 10%", 5000.0, Material.GOLD_INGOT, 1, 2, true, true);
        incomeBoost.setProperty("income_boost", 10);
        upgradesCategory.addItem(incomeBoost);
        allItems.put("income_boost", incomeBoost);
        
        // Special Items
        ShopItem raidBeacon = new ShopItem("raid_beacon", "Raid Beacon", "Signals a clan raid and provides buffs", 8000.0, Material.BEACON, 1, 4, true, false);
        specialCategory.addItem(raidBeacon);
        allItems.put("raid_beacon", raidBeacon);
        
        ShopItem teleportCrystal = new ShopItem("teleport_crystal", "Teleport Crystal", "Teleport to your clan's territories", 5000.0, Material.ENDER_PEARL, 1, 3, false, false);
        specialCategory.addItem(teleportCrystal);
        allItems.put("teleport_crystal", teleportCrystal);
        
        // Save default shop configuration
        saveDefaultConfig();
    }
    
    /**
     * Saves the default shop configuration to config.
     */
    private void saveDefaultConfig() {
        // Categories
        for (Map.Entry<String, ShopCategory> entry : categories.entrySet()) {
            String categoryKey = entry.getKey();
            ShopCategory category = entry.getValue();
            
            plugin.getConfig().set("shop.categories." + categoryKey + ".name", category.getName());
            plugin.getConfig().set("shop.categories." + categoryKey + ".description", category.getDescription());
            plugin.getConfig().set("shop.categories." + categoryKey + ".icon", category.getIcon().toString());
        }
        
        // Items
        for (Map.Entry<String, ShopItem> entry : allItems.entrySet()) {
            String itemKey = entry.getKey();
            ShopItem item = entry.getValue();
            
            plugin.getConfig().set("shop.items." + itemKey + ".name", item.getName());
            plugin.getConfig().set("shop.items." + itemKey + ".description", item.getDescription());
            plugin.getConfig().set("shop.items." + itemKey + ".price", item.getPrice());
            plugin.getConfig().set("shop.items." + itemKey + ".material", item.getMaterial().toString());
            plugin.getConfig().set("shop.items." + itemKey + ".quantity", item.getQuantity());
            plugin.getConfig().set("shop.items." + itemKey + ".min_clan_level", item.getMinClanLevel());
            plugin.getConfig().set("shop.items." + itemKey + ".clan_purchase", item.isClanPurchase());
            plugin.getConfig().set("shop.items." + itemKey + ".unique", item.isUnique());
            
            // Save properties
            for (Map.Entry<String, Object> propEntry : item.getProperties().entrySet()) {
                plugin.getConfig().set("shop.items." + itemKey + ".properties." + propEntry.getKey(), propEntry.getValue());
            }
        }
        
        plugin.saveConfig();
    }
    
    /**
     * Opens the main shop menu for a player.
     * 
     * @param player The player to open the shop for
     */
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + "Clan Shop");
        
        // Add category items
        int slot = 10;
        for (ShopCategory category : categories.values()) {
            ItemStack icon = new ItemStack(category.getIcon());
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + category.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + category.getDescription());
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to browse " + category.getName());
            meta.setLore(lore);
            
            icon.setItemMeta(meta);
            inv.setItem(slot++, icon);
            
            // Skip middle row for spacing
            if (slot == 13) slot = 14;
        }
        
        // Add decorative items
        ItemStack glass = new ItemStack(Material.GLASS_PANE, 1); // Glass pane
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
        
        player.openInventory(inv);
    }
    
    /**
     * Opens a category menu for a player.
     * 
     * @param player The player to open the menu for
     * @param categoryKey The key of the category to open
     */
    public void openCategoryMenu(Player player, String categoryKey) {
        ShopCategory category = categories.get(categoryKey);
        if (category == null) {
            player.sendMessage(ChatColor.RED + "Category not found!");
            return;
        }
        
        List<ShopItem> items = category.getItems();
        int size = Math.min(54, ((items.size() / 9) + 1) * 9);
        
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.DARK_PURPLE + "Clan Shop: " + category.getName());
        
        // Add category items
        for (int i = 0; i < items.size(); i++) {
            ShopItem item = items.get(i);
            ItemStack icon = createShopItemIcon(player, item);
            inv.setItem(i, icon);
        }
        
        // Add back button
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(ChatColor.YELLOW + "Back to Main Menu");
        backButton.setItemMeta(backMeta);
        inv.setItem(size - 5, backButton);
        
        player.openInventory(inv);
    }
    
    /**
     * Creates an item icon for a shop item.
     * 
     * @param player The player viewing the shop
     * @param item The shop item
     * @return ItemStack representing the shop item
     */
    private ItemStack createShopItemIcon(Player player, ShopItem item) {
        ItemStack icon = new ItemStack(item.getMaterial(), item.getQuantity());
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + item.getName());
        
        // Get dynamic price from shop manager
        double finalPrice = shopManager.getPrice(player, item.getItemKey());
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + item.getDescription());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Price: " + shopManager.formatPrice(finalPrice));
        
        // Add clan level requirement
        if (item.getMinClanLevel() > 1) {
            lore.add(ChatColor.YELLOW + "Required Clan Level: " + ChatColor.WHITE + item.getMinClanLevel());
        }
        
        // Add purchase type
        if (item.isClanPurchase()) {
            lore.add(ChatColor.YELLOW + "Purchase Type: " + ChatColor.AQUA + "Clan Bank");
        } else {
            lore.add(ChatColor.YELLOW + "Purchase Type: " + ChatColor.GREEN + "Personal");
        }
        
        // Add permission check
        if (!shopManager.hasPermission(player, item.getItemKey())) {
            lore.add(ChatColor.RED + "You don't have permission to buy this item");
        }
        
        // Check if player can afford
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        boolean canAfford = false;
        boolean meetsLevelReq = false;
        
        if (clan != null) {
            meetsLevelReq = clan.getLevel() >= item.getMinClanLevel();
            
            if (item.isClanPurchase()) {
                double clanBalance = plugin.getEconomy().getClanBalance(clan.getName());
                canAfford = clanBalance >= finalPrice;
            } else {
                if (plugin.getVaultEconomy() != null) {
                    canAfford = plugin.getVaultEconomy().getBalance(player) >= finalPrice;
                }
            }
        }
        
        // Apply any active discounts
        boolean hasDiscount = finalPrice < item.getPrice();
        if (hasDiscount) {
            double discountPercent = 100 - ((finalPrice / item.getPrice()) * 100);
            lore.add(ChatColor.GREEN + "Discount: " + String.format("%.1f", discountPercent) + "%");
        }
        
        lore.add("");
        if (!shopManager.hasPermission(player, item.getItemKey())) {
            lore.add(ChatColor.RED + "You don't have permission to buy this!");
        } else if (!meetsLevelReq) {
            lore.add(ChatColor.RED + "Your clan doesn't meet the level requirement!");
        } else if (!canAfford) {
            lore.add(ChatColor.RED + "You can't afford this item!");
        } else {
            lore.add(ChatColor.GREEN + "Click to purchase");
        }
        
        meta.setLore(lore);
        icon.setItemMeta(meta);
        
        return icon;
    }
    
    /**
     * Processes a player's purchase of a shop item.
     * 
     * @param player The player making the purchase
     * @param itemKey The key of the item being purchased
     * @return True if the purchase was successful
     */
    public boolean purchaseItem(Player player, String itemKey) {
        ShopItem item = allItems.get(itemKey);
        if (item == null) {
            player.sendMessage(ChatColor.RED + "Item not found!");
            return false;
        }
        
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You need to be in a clan to make purchases!");
            return false;
        }
        
        // Check clan level requirement
        if (clan.getLevel() < item.getMinClanLevel()) {
            player.sendMessage(ChatColor.RED + "Your clan needs to be level " + item.getMinClanLevel() + " to purchase this!");
            return false;
        }
        
        // Check if unique and already purchased
        if (item.isUnique() && hasUniquePurchase(clan, itemKey)) {
            player.sendMessage(ChatColor.RED + "Your clan has already purchased this unique item!");
            return false;
        }
        
        // Get the dynamic price from the shop manager
        double finalPrice = shopManager.getPrice(player, itemKey);
        
        // Check permission
        if (!shopManager.hasPermission(player, itemKey)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to purchase this item!");
            return false;
        }
        
        // Process payment
        boolean success = false;
        if (item.isClanPurchase()) {
            // Check if player has permission (leader/officer)
            if (!isLeaderOrOfficer(clan, player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Only clan leaders and officers can make clan purchases!");
                return false;
            }
            
            // Check clan bank balance
            double clanBalance = plugin.getEconomy().getClanBalance(clan.getName());
            if (clanBalance < finalPrice) {
                player.sendMessage(ChatColor.RED + "Your clan bank doesn't have enough funds! Needed: " 
                    + shopManager.formatPrice(finalPrice));
                return false;
            }
            
            // Withdraw from clan bank
            success = plugin.getEconomy().withdrawFromClan(clan.getName(), finalPrice);
        } else {
            // Check player balance
            if (plugin.getVaultEconomy() != null) {
                double playerBalance = plugin.getVaultEconomy().getBalance(player);
                if (playerBalance < finalPrice) {
                    player.sendMessage(ChatColor.RED + "You don't have enough money! Needed: " 
                        + shopManager.formatPrice(finalPrice));
                    return false;
                }
                
                // Withdraw from player
                success = plugin.getVaultEconomy().withdrawPlayer(player, finalPrice).transactionSuccess();
            } else {
                player.sendMessage(ChatColor.RED + "Economy system is not available. Personal purchases disabled.");
                return false;
            }
        }
        
        if (!success) {
            player.sendMessage(ChatColor.RED + "Error processing payment!");
            return false;
        }
        
        // Process successful purchase
        if (item.isUnique()) {
            // Record unique item purchase
            recordUniquePurchase(clan, itemKey);
            
            // Apply unique item effects
            applyUniqueItemEffects(clan, player, item);
        } else {
            // Give physical item to player
            giveItemToPlayer(player, item);
        }
        
        // Send success message
        player.sendMessage(ChatColor.GREEN + "Successfully purchased " + ChatColor.GOLD + item.getName() + ChatColor.GREEN + "!");
        
        return true;
    }
    
    /**
     * Checks if a player is a leader or officer in the clan.
     * 
     * @param clan The clan to check
     * @param playerUuid The player's UUID
     * @return True if the player is a leader or officer
     */
    private boolean isLeaderOrOfficer(Clan clan, java.util.UUID playerUuid) {
        if (clan == null) return false;
        
        com.minecraft.clanplugin.models.ClanMember member = clan.getMember(playerUuid);
        return member != null && 
               (member.getRole() == com.minecraft.clanplugin.models.ClanRole.LEADER || 
                member.getRole() == com.minecraft.clanplugin.models.ClanRole.OFFICER);
    }
    
    /**
     * Checks if a clan has already purchased a unique item.
     * 
     * @param clan The clan to check
     * @param itemKey The item key to check
     * @return True if the clan has already purchased this unique item
     */
    private boolean hasUniquePurchase(Clan clan, String itemKey) {
        List<String> uniquePurchases = plugin.getConfig().getStringList("clan_purchases." + clan.getName());
        return uniquePurchases.contains(itemKey);
    }
    
    /**
     * Records a unique purchase for a clan.
     * 
     * @param clan The clan making the purchase
     * @param itemKey The item key purchased
     */
    private void recordUniquePurchase(Clan clan, String itemKey) {
        List<String> uniquePurchases = plugin.getConfig().getStringList("clan_purchases." + clan.getName());
        uniquePurchases.add(itemKey);
        plugin.getConfig().set("clan_purchases." + clan.getName(), uniquePurchases);
        plugin.saveConfig();
    }
    
    /**
     * Applies effects for unique items.
     * 
     * @param clan The clan that purchased the item
     * @param player The player who made the purchase
     * @param item The item purchased
     */
    private void applyUniqueItemEffects(Clan clan, Player player, ShopItem item) {
        String itemKey = item.getItemKey();
        
        switch (itemKey) {
            case "member_slot":
                int slotIncrease = (int) item.getProperty("slot_increase", 1);
                int currentMaxMembers = clan.getMaxMembers();
                clan.setMaxMembers(currentMaxMembers + slotIncrease);
                player.sendMessage(ChatColor.GREEN + "Your clan's member limit has been increased to " + clan.getMaxMembers() + "!");
                break;
                
            case "territory_slot":
                int territoryIncrease = (int) item.getProperty("territory_increase", 1);
                int currentMaxTerritories = clan.getMaxTerritories();
                clan.setMaxTerritories(currentMaxTerritories + territoryIncrease);
                player.sendMessage(ChatColor.GREEN + "Your clan's territory limit has been increased to " + clan.getMaxTerritories() + "!");
                break;
                
            case "income_boost":
                int incomeBoost = (int) item.getProperty("income_boost", 10);
                int currentIncomeBoost = clan.getIncomeBoost();
                clan.setIncomeBoost(currentIncomeBoost + incomeBoost);
                player.sendMessage(ChatColor.GREEN + "Your clan's income boost has been increased to " + clan.getIncomeBoost() + "%!");
                break;
                
            default:
                player.sendMessage(ChatColor.YELLOW + "Unique item effect applied!");
                break;
        }
    }
    
    /**
     * Gives a physical item to a player.
     * 
     * @param player The player to give the item to
     * @param item The shop item to give
     */
    private void giveItemToPlayer(Player player, ShopItem item) {
        ItemStack itemStack = new ItemStack(item.getMaterial(), item.getQuantity());
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + item.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + item.getDescription());
        
        // Add special properties as lore
        for (Map.Entry<String, Object> property : item.getProperties().entrySet()) {
            lore.add(ChatColor.BLUE + property.getKey() + ": " + ChatColor.WHITE + property.getValue());
        }
        
        // Add clan identification
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        if (clan != null) {
            lore.add("");
            lore.add(clan.getChatColor() + "Clan: " + clan.getName());
        }
        
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
        
        // Apply special effects based on item type
        if (item.getItemKey().contains("flag")) {
            // Apply clan colors to banner
            ItemUtils.applyColorToBanner(itemStack, clan);
        }
        
        // Add item to player inventory or drop at their location if full
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), itemStack);
            player.sendMessage(ChatColor.YELLOW + "Your inventory was full, so the item was dropped at your feet.");
        } else {
            player.getInventory().addItem(itemStack);
        }
    }
    
    /**
     * Gets a shop item by its key.
     * 
     * @param itemKey The key of the item to get
     * @return The shop item, or null if not found
     */
    public ShopItem getItem(String itemKey) {
        return allItems.get(itemKey);
    }
    
    /**
     * Gets all shop categories.
     * 
     * @return Map of all shop categories
     */
    public Map<String, ShopCategory> getCategories() {
        return categories;
    }
    
    /**
     * Gets all shop items.
     * 
     * @return Map of all shop items
     */
    public Map<String, ShopItem> getAllItems() {
        return allItems;
    }
    
    /**
     * Saves shop data to configuration files.
     * This includes purchase history, stock levels, and custom pricing.
     */
    public void saveShopData() {
        try {
            // Create a dedicated shop data file if it doesn't exist
            java.io.File shopDataFile = new java.io.File(plugin.getDataFolder(), "shop_data.yml");
            if (!shopDataFile.exists()) {
                shopDataFile.createNewFile();
            }
            
            // Create configuration from file
            org.bukkit.configuration.file.YamlConfiguration shopConfig = 
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(shopDataFile);
            
            // Save current shop state
            for (Map.Entry<String, ShopItem> entry : allItems.entrySet()) {
                ShopItem item = entry.getValue();
                String path = "items." + entry.getKey();
                
                // Save stock levels if this is a limited stock item
                if (item.hasLimitedStock()) {
                    shopConfig.set(path + ".stock", item.getStock());
                }
                
                // Save custom price if different from default
                if (item.hasCustomPrice()) {
                    shopConfig.set(path + ".custom_price", item.getPrice());
                }
                
                // Save last restock time if applicable
                if (item.getLastRestockTime() > 0) {
                    shopConfig.set(path + ".last_restock", item.getLastRestockTime());
                }
            }
            
            // Save category display order if customized
            int categoryIndex = 0;
            for (String categoryKey : categories.keySet()) {
                shopConfig.set("category_order." + categoryKey, categoryIndex++);
            }
            
            // Save the configuration to file
            shopConfig.save(shopDataFile);
            plugin.getLogger().info("Shop data saved successfully!");
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save shop data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
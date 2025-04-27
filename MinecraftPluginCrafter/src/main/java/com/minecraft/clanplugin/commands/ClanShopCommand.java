package com.minecraft.clanplugin.commands;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.shop.ClanShop;
import com.minecraft.clanplugin.shop.ShopCategory;
import com.minecraft.clanplugin.shop.ShopItem;
import com.minecraft.clanplugin.shop.ShopManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command handler for clan shop management commands.
 */
public class ClanShopCommand implements CommandExecutor, TabCompleter {
    
    private final ClanPlugin plugin;
    private final ClanShop clanShop;
    
    /**
     * Creates a new clan shop command handler.
     * 
     * @param plugin The clan plugin instance
     * @param clanShop The clan shop instance
     */
    public ClanShopCommand(ClanPlugin plugin, ClanShop clanShop) {
        this.plugin = plugin;
        this.clanShop = clanShop;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "browse":
                return handleBrowse(sender);
            case "list":
                return handleList(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "price":
                return handlePrice(sender, args);
            case "discount":
                return handleDiscount(sender, args);
            case "reload":
                return handleReload(sender);
            default:
                sendHelpMessage(sender);
                return true;
        }
    }
    
    /**
     * Handles the browse command.
     * 
     * @param sender The command sender
     * @return True if the command was handled
     */
    private boolean handleBrowse(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can browse the shop!");
            return true;
        }
        
        Player player = (Player) sender;
        clanShop.openMainMenu(player);
        return true;
    }
    
    /**
     * Handles the list command.
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleList(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.GOLD + "=== Shop Categories ===");
            for (ShopCategory category : clanShop.getCategories().values()) {
                sender.sendMessage(ChatColor.YELLOW + "- " + category.getName() + 
                                  ChatColor.GRAY + " (" + category.getItems().size() + " items)");
            }
            sender.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.WHITE + "/clanshop list <category>" + 
                              ChatColor.YELLOW + " to see items in a category.");
            return true;
        }
        
        String categoryName = args[1].toLowerCase();
        ShopCategory category = null;
        
        // Find category by name or key
        for (ShopCategory cat : clanShop.getCategories().values()) {
            if (cat.getName().toLowerCase().equals(categoryName) || 
                cat.getCategoryKey().toLowerCase().equals(categoryName)) {
                category = cat;
                break;
            }
        }
        
        if (category == null) {
            sender.sendMessage(ChatColor.RED + "Category not found!");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== Items in " + category.getName() + " ===");
        for (ShopItem item : category.getItems()) {
            ShopManager shopManager = getShopManager();
            String priceDisplay = (shopManager != null) ? shopManager.formatPrice(item.getPrice()) : 
                                  ChatColor.GOLD + "$" + String.format("%.2f", item.getPrice());
            
            sender.sendMessage(ChatColor.YELLOW + "- " + item.getName() + 
                              ChatColor.GRAY + " (" + priceDisplay + ChatColor.GRAY + ")");
        }
        
        return true;
    }
    
    /**
     * Handles the info command.
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /clanshop info <item>");
            return true;
        }
        
        String itemKey = args[1].toLowerCase();
        ShopItem item = clanShop.getItem(itemKey);
        
        if (item == null) {
            // Try to find by name
            for (ShopItem shopItem : clanShop.getAllItems().values()) {
                if (shopItem.getName().toLowerCase().contains(itemKey)) {
                    item = shopItem;
                    break;
                }
            }
        }
        
        if (item == null) {
            sender.sendMessage(ChatColor.RED + "Item not found!");
            return true;
        }
        
        ShopManager shopManager = getShopManager();
        String priceDisplay = (shopManager != null) ? shopManager.formatPrice(item.getPrice()) : 
                              ChatColor.GOLD + "$" + String.format("%.2f", item.getPrice());
        
        sender.sendMessage(ChatColor.GOLD + "=== Item Info: " + item.getName() + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Key: " + ChatColor.WHITE + item.getItemKey());
        sender.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.GRAY + item.getDescription());
        sender.sendMessage(ChatColor.YELLOW + "Price: " + priceDisplay);
        sender.sendMessage(ChatColor.YELLOW + "Material: " + ChatColor.WHITE + item.getMaterial());
        sender.sendMessage(ChatColor.YELLOW + "Min Clan Level: " + ChatColor.WHITE + item.getMinClanLevel());
        sender.sendMessage(ChatColor.YELLOW + "Clan Purchase: " + ChatColor.WHITE + (item.isClanPurchase() ? "Yes" : "No"));
        sender.sendMessage(ChatColor.YELLOW + "Unique: " + ChatColor.WHITE + (item.isUnique() ? "Yes" : "No"));
        
        if (!item.getProperties().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Properties:");
            for (Map.Entry<String, Object> property : item.getProperties().entrySet()) {
                sender.sendMessage(ChatColor.GRAY + "  - " + property.getKey() + ": " + property.getValue());
            }
        }
        
        return true;
    }
    
    /**
     * Handles the price command.
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handlePrice(CommandSender sender, String[] args) {
        if (!sender.hasPermission("clan.admin.shop")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to change shop prices!");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /clanshop price <item> <price>");
            sender.sendMessage(ChatColor.RED + "Use 'reset' as the price to restore the default price.");
            return true;
        }
        
        String itemKey = args[1].toLowerCase();
        ShopItem item = clanShop.getItem(itemKey);
        
        if (item == null) {
            // Try to find by name
            for (ShopItem shopItem : clanShop.getAllItems().values()) {
                if (shopItem.getName().toLowerCase().contains(itemKey)) {
                    item = shopItem;
                    itemKey = item.getItemKey();
                    break;
                }
            }
        }
        
        if (item == null) {
            sender.sendMessage(ChatColor.RED + "Item not found!");
            return true;
        }
        
        ShopManager shopManager = getShopManager();
        if (shopManager == null) {
            sender.sendMessage(ChatColor.RED + "Shop manager is not available!");
            return true;
        }
        
        if (args[2].equalsIgnoreCase("reset")) {
            // Reset price to default
            shopManager.clearCustomPrice(itemKey);
            sender.sendMessage(ChatColor.GREEN + "Reset price of " + ChatColor.GOLD + item.getName() + 
                           ChatColor.GREEN + " to default: " + shopManager.formatPrice(shopManager.getBasePrice(itemKey)));
            return true;
        }
        
        double price;
        try {
            price = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid price! Must be a number.");
            return true;
        }
        
        if (price < 0) {
            sender.sendMessage(ChatColor.RED + "Price cannot be negative!");
            return true;
        }
        
        shopManager.setCustomPrice(itemKey, price);
        sender.sendMessage(ChatColor.GREEN + "Set price of " + ChatColor.GOLD + item.getName() + 
                       ChatColor.GREEN + " to " + shopManager.formatPrice(price));
        
        return true;
    }
    
    /**
     * Handles the discount command.
     * 
     * @param sender The command sender
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleDiscount(CommandSender sender, String[] args) {
        if (!sender.hasPermission("clan.admin.shop")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to manage shop discounts!");
            return true;
        }
        
        ShopManager shopManager = getShopManager();
        if (shopManager == null) {
            sender.sendMessage(ChatColor.RED + "Shop manager is not available!");
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /clanshop discount <event> <on|off>");
            sender.sendMessage(ChatColor.YELLOW + "Available events: weekend, holiday");
            return true;
        }
        
        String event = args[1].toLowerCase();
        if (!event.equals("weekend") && !event.equals("holiday")) {
            sender.sendMessage(ChatColor.RED + "Invalid event! Must be 'weekend' or 'holiday'.");
            return true;
        }
        
        boolean enabled;
        if (args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("true")) {
            enabled = true;
        } else if (args[2].equalsIgnoreCase("off") || args[2].equalsIgnoreCase("false")) {
            enabled = false;
        } else {
            sender.sendMessage(ChatColor.RED + "Invalid option! Must be 'on' or 'off'.");
            return true;
        }
        
        if (shopManager.setEventDiscount(event, enabled)) {
            sender.sendMessage(ChatColor.GREEN + (enabled ? "Enabled" : "Disabled") + " the " + 
                           ChatColor.GOLD + event + ChatColor.GREEN + " discount event.");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to update discount event!");
        }
        
        return true;
    }
    
    /**
     * Handles the reload command.
     * 
     * @param sender The command sender
     * @return True if the command was handled
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("clan.admin.shop")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to reload the shop!");
            return true;
        }
        
        ShopManager shopManager = getShopManager();
        if (shopManager == null) {
            sender.sendMessage(ChatColor.RED + "Shop manager is not available!");
            return true;
        }
        
        shopManager.loadShopConfig();
        sender.sendMessage(ChatColor.GREEN + "Shop configuration reloaded!");
        
        return true;
    }
    
    /**
     * Gets the shop manager instance.
     * 
     * @return The shop manager, or null if unavailable
     */
    private ShopManager getShopManager() {
        // This is a bit of a workaround since we don't have direct access to the shop manager
        try {
            java.lang.reflect.Field field = ClanShop.class.getDeclaredField("shopManager");
            field.setAccessible(true);
            return (ShopManager) field.get(clanShop);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to access shop manager: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Sends the help message to a command sender.
     * 
     * @param sender The command sender
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Clan Shop Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/clanshop browse" + ChatColor.GRAY + " - Open the shop GUI");
        sender.sendMessage(ChatColor.YELLOW + "/clanshop list [category]" + ChatColor.GRAY + " - List shop categories or items");
        sender.sendMessage(ChatColor.YELLOW + "/clanshop info <item>" + ChatColor.GRAY + " - View detailed info about an item");
        
        if (sender.hasPermission("clan.admin.shop")) {
            sender.sendMessage(ChatColor.YELLOW + "/clanshop price <item> <price>" + ChatColor.GRAY + " - Set a custom price for an item");
            sender.sendMessage(ChatColor.YELLOW + "/clanshop discount <event> <on|off>" + ChatColor.GRAY + " - Enable or disable discount events");
            sender.sendMessage(ChatColor.YELLOW + "/clanshop reload" + ChatColor.GRAY + " - Reload shop configuration");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            subCommands.add("browse");
            subCommands.add("list");
            subCommands.add("info");
            
            if (sender.hasPermission("clan.admin.shop")) {
                subCommands.add("price");
                subCommands.add("discount");
                subCommands.add("reload");
            }
            
            return filterCompletions(subCommands, args[0]);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("list")) {
                return filterCompletions(
                    clanShop.getCategories().values().stream()
                            .map(ShopCategory::getCategoryKey)
                            .collect(Collectors.toList()), 
                    args[1]
                );
            } else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("price")) {
                return filterCompletions(
                    clanShop.getAllItems().values().stream()
                            .map(ShopItem::getItemKey)
                            .collect(Collectors.toList()), 
                    args[1]
                );
            } else if (args[0].equalsIgnoreCase("discount")) {
                List<String> events = new ArrayList<>();
                events.add("weekend");
                events.add("holiday");
                return filterCompletions(events, args[1]);
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("price")) {
                List<String> options = new ArrayList<>();
                options.add("reset");
                return filterCompletions(options, args[2]);
            } else if (args[0].equalsIgnoreCase("discount")) {
                List<String> options = new ArrayList<>();
                options.add("on");
                options.add("off");
                return filterCompletions(options, args[2]);
            }
        }
        
        return completions;
    }
    
    /**
     * Filters tab completions based on the current input.
     * 
     * @param options The list of available options
     * @param input The current input
     * @return A filtered list of completions
     */
    private List<String> filterCompletions(List<String> options, String input) {
        if (input.isEmpty()) return options;
        
        List<String> completions = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(input.toLowerCase())) {
                completions.add(option);
            }
        }
        
        return completions;
    }
}
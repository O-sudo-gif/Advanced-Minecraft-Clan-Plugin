package com.minecraft.clanplugin.bounty;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Bounty;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.Currency;

/**
 * GUI for the bounty system.
 */
public class BountyGUI {
    
    private final ClanPlugin plugin;
    private final BountyManager bountyManager;
    private final SimpleDateFormat dateFormat;
    private final NumberFormat currencyFormat;
    
    // GUI constants
    private static final String GUI_TITLE = ChatColor.DARK_RED + "Bounty System";
    private static final String TOP_BOUNTIES_TITLE = ChatColor.DARK_RED + "Top Bounties";
    private static final String MY_BOUNTIES_TITLE = ChatColor.DARK_RED + "My Bounties";
    private static final String PLAYER_BOUNTIES_TITLE = ChatColor.DARK_RED + "Player Bounties: ";
    
    /**
     * Creates a new bounty GUI.
     * 
     * @param plugin The clan plugin instance
     * @param bountyManager The bounty manager
     */
    public BountyGUI(ClanPlugin plugin, BountyManager bountyManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
        this.dateFormat = new SimpleDateFormat("MMM dd HH:mm");
        this.currencyFormat = NumberFormat.getCurrencyInstance();
        this.currencyFormat.setCurrency(Currency.getInstance("USD"));
    }
    
    /**
     * Opens the main bounty menu for a player.
     * 
     * @param player The player to open the menu for
     */
    public void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, GUI_TITLE);
        
        // Check if there are any top bounties to display
        Map<UUID, Double> topBounties = bountyManager.getTopBounties(1);
        UUID topTargetUUID = topBounties.isEmpty() ? null : topBounties.keySet().iterator().next();
        
        // Top bounties button - uses player head of top bounty if available
        ItemStack topBountiesItem;
        if (topTargetUUID != null) {
            OfflinePlayer topTarget = Bukkit.getOfflinePlayer(topTargetUUID);
            double amount = topBounties.get(topTargetUUID);
            
            topBountiesItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) topBountiesItem.getItemMeta();
            meta.setOwningPlayer(topTarget);
            meta.setDisplayName(ChatColor.GOLD + "Top Bounties");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "View players with the highest bounties");
            lore.add("");
            lore.add(ChatColor.RED + "Most Wanted: " + (topTarget.getName() != null ? topTarget.getName() : "Unknown"));
            lore.add(ChatColor.GOLD + "Bounty: " + ChatColor.YELLOW + "$" + String.format("%.2f", amount));
            meta.setLore(lore);
            
            topBountiesItem.setItemMeta(meta);
        } else {
            topBountiesItem = createGuiItem(Material.GOLD_INGOT, 
                    ChatColor.GOLD + "Top Bounties", 
                    ChatColor.YELLOW + "View players with the highest bounties");
        }
        inventory.setItem(11, topBountiesItem);
        
        // My placed bounties button - shows a paper with number of bounties placed
        List<Bounty> placedBounties = bountyManager.getActiveBountiesPlacedBy(player.getUniqueId());
        ItemStack myBountiesItem = createGuiItem(Material.PAPER, 
                ChatColor.AQUA + "My Placed Bounties", 
                ChatColor.YELLOW + "View bounties you've placed on others",
                ChatColor.WHITE + "Active Bounties: " + ChatColor.GREEN + placedBounties.size());
        inventory.setItem(13, myBountiesItem);
        
        // Bounties on me button - shows player's own head with bounty info
        double bountyOnMe = bountyManager.getTotalBountyValue(player.getUniqueId());
        ItemStack bountiesOnMeItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta myMeta = (SkullMeta) bountiesOnMeItem.getItemMeta();
        myMeta.setOwningPlayer(player);
        myMeta.setDisplayName(ChatColor.RED + "Bounties On Me");
        
        List<String> myLore = new ArrayList<>();
        myLore.add(ChatColor.YELLOW + "View bounties placed on you");
        if (bountyOnMe > 0) {
            myLore.add("");
            myLore.add(ChatColor.GOLD + "Your Bounty: " + ChatColor.YELLOW + "$" + String.format("%.2f", bountyOnMe));
        }
        myMeta.setLore(myLore);
        bountiesOnMeItem.setItemMeta(myMeta);
        inventory.setItem(15, bountiesOnMeItem);
        
        // Close button
        ItemStack closeItem = createGuiItem(Material.BARRIER, 
                ChatColor.RED + "Close", 
                ChatColor.GRAY + "Close this menu");
        inventory.setItem(26, closeItem);
        
        player.openInventory(inventory);
    }
    
    /**
     * Opens the top bounties menu.
     * 
     * @param player The player to open the menu for
     */
    public void openTopBountiesMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, TOP_BOUNTIES_TITLE);
        
        // Get top bounties (limited to 45 to leave space for navigation)
        Map<UUID, Double> topBounties = bountyManager.getTopBounties(45);
        
        // Display a message if there are no active bounties
        if (topBounties.isEmpty()) {
            ItemStack noBountiesItem = createGuiItem(Material.BARRIER, 
                    ChatColor.RED + "No Active Bounties", 
                    ChatColor.GRAY + "There are no active bounties at this time.");
            inventory.setItem(22, noBountiesItem);
        } else {
            int rank = 1;
            int slot = 0;
            
            // Use a special layout with the first 3 bounties being more prominent
            for (Map.Entry<UUID, Double> entry : topBounties.entrySet()) {
                UUID targetUUID = entry.getKey();
                double amount = entry.getValue();
                
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
                String targetName = target.getName() != null ? target.getName() : "Unknown";
                
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwningPlayer(target);
                
                if (rank <= 3) {
                    // Special formatting for top 3
                    String prefix;
                    ChatColor nameColor;
                    ChatColor valueColor;
                    
                    if (rank == 1) {
                        prefix = "🏆 ";
                        nameColor = ChatColor.GOLD;
                        valueColor = ChatColor.YELLOW;
                        slot = 13; // Center top spot
                    } else if (rank == 2) {
                        prefix = "🥈 ";
                        nameColor = ChatColor.WHITE;
                        valueColor = ChatColor.GRAY;
                        slot = 11; // Left of top
                    } else { // rank 3
                        prefix = "🥉 ";
                        nameColor = ChatColor.DARK_RED;
                        valueColor = ChatColor.RED;
                        slot = 15; // Right of top
                    }
                    
                    meta.setDisplayName(prefix + nameColor + "#" + rank + ": " + targetName);
                    
                    List<String> lore = new ArrayList<>();
                    lore.add(valueColor + "Bounty: " + ChatColor.GOLD + "$" + String.format("%.2f", amount));
                    lore.add("");
                    lore.add(ChatColor.YELLOW + "Click to view details or place a bounty");
                    meta.setLore(lore);
                    
                    head.setItemMeta(meta);
                    inventory.setItem(slot, head);
                    
                    // Start the regular list after the top 3 display
                    if (rank == 3) {
                        slot = 18; // First slot in 3rd row
                    }
                } else {
                    // Regular formatting for remaining bounties
                    meta.setDisplayName(ChatColor.RED + "#" + rank + ": " + targetName);
                    
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GOLD + "Bounty: " + ChatColor.YELLOW + "$" + String.format("%.2f", amount));
                    lore.add(ChatColor.GRAY + "Click for details");
                    meta.setLore(lore);
                    
                    head.setItemMeta(meta);
                    inventory.setItem(slot++, head);
                }
                
                rank++;
                
                if (slot >= 45) { // Save room for navigation buttons
                    break;
                }
            }
        }
        
        // Back button
        ItemStack backItem = createGuiItem(Material.ARROW, 
                ChatColor.YELLOW + "Back", 
                ChatColor.GRAY + "Return to main menu");
        inventory.setItem(49, backItem);
        
        player.openInventory(inventory);
    }
    
    /**
     * Opens a menu showing bounties placed by a player.
     * 
     * @param player The player to open the menu for
     */
    public void openMyBountiesMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, MY_BOUNTIES_TITLE);
        
        List<Bounty> placedBounties = bountyManager.getActiveBountiesPlacedBy(player.getUniqueId());
        Map<UUID, List<Bounty>> bountyMap = new HashMap<>();
        
        // Group bounties by target
        for (Bounty bounty : placedBounties) {
            UUID targetUUID = bounty.getTargetUUID();
            if (!bountyMap.containsKey(targetUUID)) {
                bountyMap.put(targetUUID, new ArrayList<>());
            }
            bountyMap.get(targetUUID).add(bounty);
        }
        
        int slot = 0;
        for (Map.Entry<UUID, List<Bounty>> entry : bountyMap.entrySet()) {
            UUID targetUUID = entry.getKey();
            List<Bounty> bounties = entry.getValue();
            
            double totalAmount = bounties.stream().mapToDouble(Bounty::getAmount).sum();
            
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
            String targetName = target.getName() != null ? target.getName() : "Unknown";
            
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.setDisplayName(ChatColor.RED + targetName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GOLD + "Total Bounty: " + ChatColor.YELLOW + "$" + String.format("%.2f", totalAmount));
            lore.add(ChatColor.YELLOW + "Bounties: " + ChatColor.WHITE + bounties.size());
            
            // Add individual bounties
            for (int i = 0; i < Math.min(bounties.size(), 5); i++) {
                Bounty bounty = bounties.get(i);
                String date = dateFormat.format(new Date(bounty.getTimestamp()));
                lore.add(ChatColor.GRAY + date + ": " + ChatColor.GOLD + "$" + String.format("%.2f", bounty.getAmount()));
            }
            
            if (bounties.size() > 5) {
                lore.add(ChatColor.GRAY + "... and " + (bounties.size() - 5) + " more");
            }
            
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to view/manage");
            
            meta.setLore(lore);
            head.setItemMeta(meta);
            
            inventory.setItem(slot++, head);
            
            if (slot >= 45) { // Save room for navigation buttons
                break;
            }
        }
        
        // Back button
        ItemStack backItem = createGuiItem(Material.ARROW, 
                ChatColor.YELLOW + "Back", 
                ChatColor.GRAY + "Return to main menu");
        inventory.setItem(49, backItem);
        
        player.openInventory(inventory);
    }
    
    /**
     * Opens a menu showing bounties on a specific player.
     * 
     * @param player The player viewing the menu
     * @param targetPlayer The player whose bounties to show
     */
    public void openPlayerBountiesMenu(Player player, Player targetPlayer) {
        UUID targetUUID = targetPlayer.getUniqueId();
        String title = PLAYER_BOUNTIES_TITLE + targetPlayer.getName();
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        
        List<Bounty> bounties = bountyManager.getActiveBountiesForTarget(targetUUID);
        double totalBounty = bountyManager.getTotalBountyValue(targetUUID);
        
        // Player head with total bounty
        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) headItem.getItemMeta();
        headMeta.setOwningPlayer(targetPlayer);
        headMeta.setDisplayName(ChatColor.RED + targetPlayer.getName());
        
        List<String> headLore = new ArrayList<>();
        headLore.add(ChatColor.GOLD + "Total Bounty: " + ChatColor.YELLOW + "$" + String.format("%.2f", totalBounty));
        headLore.add(ChatColor.YELLOW + "Number of Bounties: " + ChatColor.WHITE + bounties.size());
        headMeta.setLore(headLore);
        
        headItem.setItemMeta(headMeta);
        inventory.setItem(4, headItem);
        
        // Individual bounties
        int slot = 18;
        for (Bounty bounty : bounties) {
            OfflinePlayer placer = Bukkit.getOfflinePlayer(bounty.getPlacerUUID());
            String placerName = placer.getName() != null ? placer.getName() : "Unknown";
            boolean isOwnBounty = bounty.isPlacedBy(player.getUniqueId());
            
            ItemStack item = createGuiItem(
                    isOwnBounty ? Material.EMERALD : Material.GOLD_NUGGET,
                    (isOwnBounty ? ChatColor.GREEN : ChatColor.GOLD) + "Bounty by " + placerName,
                    ChatColor.YELLOW + "Amount: " + ChatColor.GOLD + "$" + String.format("%.2f", bounty.getAmount()),
                    ChatColor.GRAY + "Date: " + dateFormat.format(new Date(bounty.getTimestamp())));
            
            // Add cancel option for own bounties
            if (isOwnBounty) {
                ItemMeta meta = item.getItemMeta();
                List<String> lore = meta.getLore();
                lore.add("");
                lore.add(ChatColor.RED + "Click to cancel (penalty applies)");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            
            inventory.setItem(slot++, item);
            
            if (slot >= 45) { // Save room for navigation
                break;
            }
        }
        
        // Add bounty button
        if (!targetPlayer.equals(player)) {
            ItemStack addBountyItem = createGuiItem(Material.GOLD_INGOT, 
                    ChatColor.GREEN + "Place Bounty", 
                    ChatColor.YELLOW + "Place a new bounty on this player",
                    ChatColor.GRAY + "Minimum: $" + bountyManager.getMinimumBountyAmount());
            inventory.setItem(48, addBountyItem);
        }
        
        // Back button
        ItemStack backItem = createGuiItem(Material.ARROW, 
                ChatColor.YELLOW + "Back", 
                ChatColor.GRAY + "Return to main menu");
        inventory.setItem(50, backItem);
        
        player.openInventory(inventory);
    }
    
    /**
     * Opens a menu showing bounties on the player.
     * 
     * @param player The player to show bounties for
     */
    public void openBountiesOnMeMenu(Player player) {
        openPlayerBountiesMenu(player, player);
    }
    
    /**
     * Creates a GUI item with a custom name and lore.
     * 
     * @param material The material of the item
     * @param name The name of the item
     * @param lore The lore of the item
     * @return The created ItemStack
     */
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(name);
        
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(line);
        }
        
        meta.setLore(loreList);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Creates a player head item for a bounty target with formatted information.
     * 
     * @param target The target player
     * @param bountyAmount The bounty amount on this player
     * @param rank Optional rank number (for top bounties display)
     * @param additionalInfo Optional additional information to display
     * @return The created ItemStack with the player's head
     */
    private ItemStack createTargetHeadItem(OfflinePlayer target, double bountyAmount, Integer rank, String... additionalInfo) {
        String targetName = target.getName() != null ? target.getName() : "Unknown";
        
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(target);
        
        // Format the display name based on rank
        if (rank != null) {
            meta.setDisplayName(ChatColor.RED + "#" + rank + ": " + targetName);
        } else {
            meta.setDisplayName(ChatColor.RED + targetName);
        }
        
        // Add bounty information
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "Bounty: " + ChatColor.YELLOW + "$" + String.format("%.2f", bountyAmount));
        
        // Add any additional information
        for (String info : additionalInfo) {
            lore.add(info);
        }
        
        // Add click instruction if no other info was provided
        if (additionalInfo.length == 0) {
            lore.add(ChatColor.GRAY + "Click for details");
        }
        
        meta.setLore(lore);
        head.setItemMeta(meta);
        
        return head;
    }
}
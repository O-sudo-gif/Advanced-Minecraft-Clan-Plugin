package com.minecraft.clanplugin.commands;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.bounty.BountyGUI;
import com.minecraft.clanplugin.bounty.BountyManager;
import com.minecraft.clanplugin.models.Bounty;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command handler for bounty system commands.
 */
public class BountyCommand implements CommandExecutor, TabCompleter {
    
    private final ClanPlugin plugin;
    private final BountyManager bountyManager;
    private final BountyGUI bountyGUI;
    
    /**
     * Creates a new bounty command handler.
     * 
     * @param plugin The clan plugin instance
     * @param bountyManager The bounty manager
     */
    public BountyCommand(ClanPlugin plugin, BountyManager bountyManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
        this.bountyGUI = new BountyGUI(plugin, bountyManager);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Open the main GUI
            bountyGUI.openMainMenu(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "place":
                return handlePlaceBounty(player, args);
            case "list":
                if (args.length > 1 && args[1].equalsIgnoreCase("placed")) {
                    bountyGUI.openMyBountiesMenu(player);
                } else if (args.length > 2 && args[1].equalsIgnoreCase("target")) {
                    Player targetPlayer = Bukkit.getPlayer(args[2]);
                    if (targetPlayer != null) {
                        bountyGUI.openPlayerBountiesMenu(player, targetPlayer);
                    } else {
                        player.sendMessage(ChatColor.RED + "Player not found or not online!");
                    }
                } else {
                    bountyGUI.openTopBountiesMenu(player);
                }
                return true;
            case "info":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /bounty info <player>");
                    return true;
                }
                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer != null) {
                    bountyGUI.openPlayerBountiesMenu(player, targetPlayer);
                } else {
                    player.sendMessage(ChatColor.RED + "Player not found or not online!");
                }
                return true;
            case "top":
                bountyGUI.openTopBountiesMenu(player);
                return true;
            case "me":
                bountyGUI.openBountiesOnMeMenu(player);
                return true;
            case "gui":
                bountyGUI.openMainMenu(player);
                return true;
            case "remove":
            case "cancel":
                return handleCancelBounty(player, args);
            case "help":
                sendHelpMessage(player);
                return true;
            default:
                player.sendMessage(ChatColor.RED + "Unknown bounty command. Use /bounty help for a list of commands.");
                return true;
        }
    }
    
    /**
     * Handles the place bounty command.
     * 
     * @param player The player running the command
     * @param args The command arguments
     * @return True if the command was handled successfully
     */
    private boolean handlePlaceBounty(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /bounty place <player> <amount>");
            return true;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player not found or not online!");
            return true;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount! Please enter a valid number.");
            return true;
        }
        
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Bounty amount must be positive!");
            return true;
        }
        
        boolean success = bountyManager.placeBounty(targetPlayer, player, amount);
        
        if (!success) {
            // Error messages handled in bounty manager
            return true;
        }
        
        player.sendMessage(ChatColor.GREEN + "You have placed a " + ChatColor.GOLD + "$" + amount + 
                         ChatColor.GREEN + " bounty on " + ChatColor.RED + targetPlayer.getName() + ChatColor.GREEN + "!");
        
        return true;
    }
    
    /**
     * Handles the list bounties command.
     * 
     * @param player The player running the command
     * @param args The command arguments
     * @return True if the command was handled successfully
     */
    private boolean handleListBounties(Player player, String[] args) {
        if (args.length > 1) {
            String subType = args[1].toLowerCase();
            
            if (subType.equals("placed")) {
                // Show bounties placed by the player
                List<Bounty> placedBounties = bountyManager.getActiveBountiesPlacedBy(player.getUniqueId());
                
                if (placedBounties.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "You have not placed any active bounties.");
                    return true;
                }
                
                player.sendMessage(ChatColor.GOLD + "=== Your Placed Bounties ===");
                int index = 0;
                for (Bounty bounty : placedBounties) {
                    OfflinePlayer target = Bukkit.getOfflinePlayer(bounty.getTargetUUID());
                    String targetName = target.getName() != null ? target.getName() : "Unknown";
                    
                    player.sendMessage(ChatColor.YELLOW + "[" + index++ + "] " + ChatColor.RED + targetName + 
                                     ChatColor.YELLOW + " - " + ChatColor.GOLD + "$" + String.format("%.2f", bounty.getAmount()));
                }
                return true;
            } else if (subType.equals("target") && args.length > 2) {
                // Show bounties on a specific target
                String targetName = args[2];
                Player targetPlayer = Bukkit.getPlayer(targetName);
                
                if (targetPlayer == null) {
                    player.sendMessage(ChatColor.RED + "Player not found or not online!");
                    return true;
                }
                
                List<Bounty> targetBounties = bountyManager.getActiveBountiesForTarget(targetPlayer.getUniqueId());
                
                if (targetBounties.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "There are no active bounties on " + targetPlayer.getName() + ".");
                    return true;
                }
                
                player.sendMessage(ChatColor.GOLD + "=== Bounties on " + targetPlayer.getName() + " ===");
                for (Bounty bounty : targetBounties) {
                    OfflinePlayer placer = Bukkit.getOfflinePlayer(bounty.getPlacerUUID());
                    String placerName = placer.getName() != null ? placer.getName() : "Unknown";
                    
                    player.sendMessage(ChatColor.RED + placerName + ChatColor.YELLOW + " - " + 
                                     ChatColor.GOLD + "$" + String.format("%.2f", bounty.getAmount()));
                }
                return true;
            }
        }
        
        // Default: show all active bounties
        Set<UUID> targetsWithBounties = bountyManager.getAllTargetsWithBounties();
        
        if (targetsWithBounties.isEmpty()) {
            player.sendMessage(ChatColor.RED + "There are no active bounties at the moment.");
            return true;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Active Bounties ===");
        for (UUID targetUUID : targetsWithBounties) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
            String targetName = target.getName() != null ? target.getName() : "Unknown";
            double totalValue = bountyManager.getTotalBountyValue(targetUUID);
            
            player.sendMessage(ChatColor.RED + targetName + ChatColor.YELLOW + " - Total: " + 
                             ChatColor.GOLD + "$" + String.format("%.2f", totalValue));
        }
        
        return true;
    }
    
    /**
     * Handles the bounty info command.
     * 
     * @param player The player running the command
     * @param args The command arguments
     * @return True if the command was handled successfully
     */
    private boolean handleBountyInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /bounty info <player>");
            return true;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player not found or not online!");
            return true;
        }
        
        UUID targetUUID = targetPlayer.getUniqueId();
        List<Bounty> bounties = bountyManager.getActiveBountiesForTarget(targetUUID);
        
        if (bounties.isEmpty()) {
            player.sendMessage(ChatColor.RED + "There are no active bounties on " + targetPlayer.getName() + ".");
            return true;
        }
        
        double totalValue = bountyManager.getTotalBountyValue(targetUUID);
        
        player.sendMessage(ChatColor.GOLD + "=== Bounty Info: " + targetPlayer.getName() + " ===");
        player.sendMessage(ChatColor.YELLOW + "Total Value: " + ChatColor.GOLD + "$" + String.format("%.2f", totalValue));
        player.sendMessage(ChatColor.YELLOW + "Number of Bounties: " + ChatColor.WHITE + bounties.size());
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Placed by:");
        
        for (Bounty bounty : bounties) {
            OfflinePlayer placer = Bukkit.getOfflinePlayer(bounty.getPlacerUUID());
            String placerName = placer.getName() != null ? placer.getName() : "Unknown";
            
            player.sendMessage(ChatColor.RED + placerName + ChatColor.YELLOW + " - " + 
                             ChatColor.GOLD + "$" + String.format("%.2f", bounty.getAmount()));
        }
        
        return true;
    }
    
    /**
     * Handles the top bounties command.
     * 
     * @param player The player running the command
     * @param args The command arguments
     * @return True if the command was handled successfully
     */
    private boolean handleTopBounties(Player player, String[] args) {
        int limit = 10;
        
        if (args.length > 1) {
            try {
                limit = Integer.parseInt(args[1]);
                limit = Math.max(1, Math.min(50, limit)); // Limit between 1 and 50
            } catch (NumberFormatException e) {
                // Just use the default limit
            }
        }
        
        Map<UUID, Double> topBounties = bountyManager.getTopBounties(limit);
        
        if (topBounties.isEmpty()) {
            player.sendMessage(ChatColor.RED + "There are no active bounties at the moment.");
            return true;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Top Bounties ===");
        
        int rank = 1;
        for (Map.Entry<UUID, Double> entry : topBounties.entrySet()) {
            UUID targetUUID = entry.getKey();
            double totalValue = entry.getValue();
            
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
            String targetName = target.getName() != null ? target.getName() : "Unknown";
            
            player.sendMessage(ChatColor.YELLOW + "#" + rank++ + ": " + 
                             ChatColor.RED + targetName + ChatColor.YELLOW + " - " + 
                             ChatColor.GOLD + "$" + String.format("%.2f", totalValue));
        }
        
        return true;
    }
    
    /**
     * Handles the cancel bounty command.
     * 
     * @param player The player running the command
     * @param args The command arguments
     * @return True if the command was handled successfully
     */
    private boolean handleCancelBounty(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /bounty cancel <player> <index>");
            player.sendMessage(ChatColor.RED + "Use /bounty list placed to see your placed bounties and their indices.");
            return true;
        }
        
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player not found or not online!");
            return true;
        }
        
        int index;
        try {
            index = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid index! Please enter a valid number.");
            return true;
        }
        
        boolean success = bountyManager.cancelBounty(player, targetPlayer.getUniqueId(), index);
        
        // Success message handled in bounty manager
        return true;
    }
    
    /**
     * Sends the help message to a player.
     * 
     * @param player The player to send the message to
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Bounty Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/bounty" + ChatColor.GRAY + " - Open the main bounty GUI");
        player.sendMessage(ChatColor.YELLOW + "/bounty gui" + ChatColor.GRAY + " - Open the main bounty GUI");
        player.sendMessage(ChatColor.YELLOW + "/bounty place <player> <amount>" + ChatColor.GRAY + " - Place a bounty on a player");
        player.sendMessage(ChatColor.YELLOW + "/bounty list" + ChatColor.GRAY + " - Open top bounties GUI");
        player.sendMessage(ChatColor.YELLOW + "/bounty list placed" + ChatColor.GRAY + " - View bounties you've placed GUI");
        player.sendMessage(ChatColor.YELLOW + "/bounty list target <player>" + ChatColor.GRAY + " - View bounties on a player GUI");
        player.sendMessage(ChatColor.YELLOW + "/bounty info <player>" + ChatColor.GRAY + " - View detailed bounty info for a player");
        player.sendMessage(ChatColor.YELLOW + "/bounty top" + ChatColor.GRAY + " - View players with highest bounties");
        player.sendMessage(ChatColor.YELLOW + "/bounty me" + ChatColor.GRAY + " - View bounties placed on you");
        player.sendMessage(ChatColor.YELLOW + "/bounty cancel <player> <index>" + ChatColor.GRAY + " - Cancel your bounty on a player");
        player.sendMessage(ChatColor.GOLD + "==================");
        
        double minAmount = bountyManager.getMinimumBountyAmount();
        player.sendMessage(ChatColor.YELLOW + "Minimum Bounty Amount: " + ChatColor.GOLD + "$" + minAmount);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = Arrays.asList("place", "list", "info", "top", "cancel", "help");
            return filterCompletions(completions, args[0]);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("place") || 
                args[0].equalsIgnoreCase("info") || 
                args[0].equalsIgnoreCase("cancel")) {
                
                return getOnlinePlayerNames(args[1]);
                
            } else if (args[0].equalsIgnoreCase("list")) {
                List<String> completions = Arrays.asList("placed", "target");
                return filterCompletions(completions, args[1]);
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("list") && args[1].equalsIgnoreCase("target")) {
                return getOnlinePlayerNames(args[2]);
            } else if (args[0].equalsIgnoreCase("place")) {
                // Suggest some common bounty amounts
                List<String> amounts = Arrays.asList("100", "500", "1000", "5000", "10000");
                return filterCompletions(amounts, args[2]);
            }
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Filters a list of completions based on a partial input.
     * 
     * @param completions The list of possible completions
     * @param input The partial input to filter by
     * @return The filtered list of completions
     */
    private List<String> filterCompletions(List<String> completions, String input) {
        String lowercaseInput = input.toLowerCase();
        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(lowercaseInput))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets a list of online player names matching a partial name.
     * 
     * @param partialName The partial name to match
     * @return The list of matching player names
     */
    private List<String> getOnlinePlayerNames(String partialName) {
        String lowercasePartialName = partialName.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(lowercasePartialName))
                .collect(Collectors.toList());
    }
}
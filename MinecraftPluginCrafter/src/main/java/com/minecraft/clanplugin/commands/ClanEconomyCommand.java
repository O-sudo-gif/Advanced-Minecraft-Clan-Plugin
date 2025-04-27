package com.minecraft.clanplugin.commands;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.economy.ClanEconomy;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.ClanRole;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

/**
 * Command handler for clan economy-related commands.
 */
public class ClanEconomyCommand implements CommandExecutor {
    
    private final ClanPlugin plugin;
    private final ClanEconomy economy;
    
    public ClanEconomyCommand(ClanPlugin plugin, ClanEconomy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use clan economy commands!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "balance":
                return handleBalance(player);
            case "deposit":
                return handleDeposit(player, args);
            case "withdraw":
                return handleWithdraw(player, args);
            case "transfer":
                return handleTransfer(player, args);
            case "tax":
                return handleTax(player, args);
            case "collect":
                return handleCollect(player);
            default:
                sendHelpMessage(player);
                return true;
        }
    }
    
    /**
     * Handles the balance command.
     * 
     * @param player The player
     * @return True if the command was handled
     */
    private boolean handleBalance(Player player) {
        String clanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
        if (clanName == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        double balance = economy.getClanBalance(clanName);
        player.sendMessage(ChatColor.GOLD + "Clan Balance: " + economy.formatCurrency(balance));
        
        // Show tax information for leaders and officers
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        ClanMember member = clan.getMember(player.getUniqueId());
        
        if (member.getRole().getRoleLevel() >= ClanRole.OFFICER.getRoleLevel()) {
            double taxRate = economy.getTaxRate(clanName);
            player.sendMessage(ChatColor.YELLOW + "Tax Rate: " + ChatColor.WHITE + 
                              String.format("%.1f%%", taxRate * 100));
            
            // Check tax collection cooldown
            long cooldown = economy.getTaxCooldownRemaining(player.getUniqueId());
            if (cooldown > 0) {
                long hours = TimeUnit.MILLISECONDS.toHours(cooldown);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(cooldown) % 60;
                player.sendMessage(ChatColor.YELLOW + "Next Tax Collection: " + ChatColor.WHITE + 
                                  hours + "h " + minutes + "m");
            } else {
                player.sendMessage(ChatColor.YELLOW + "Taxes can be collected now with " + 
                                  ChatColor.GREEN + "/clan economy collect");
            }
        }
        
        return true;
    }
    
    /**
     * Handles the deposit command.
     * 
     * @param player The player
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleDeposit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan economy deposit <amount>");
            return true;
        }
        
        String clanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
        if (clanName == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount! Must be a number.");
            return true;
        }
        
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Amount must be greater than zero!");
            return true;
        }
        
        // Use Vault integration to handle the deposit
        if (economy.playerDepositToClan(player, clanName, amount)) {
            player.sendMessage(ChatColor.GREEN + "Successfully deposited " + 
                              economy.formatCurrency(amount) + ChatColor.GREEN + " to clan bank!");
            
            // Broadcast to clan members
            Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
            for (ClanMember member : clan.getMembers()) {
                Player memberPlayer = plugin.getServer().getPlayer(member.getPlayerUUID());
                if (memberPlayer != null && !memberPlayer.equals(player)) {
                    memberPlayer.sendMessage(ChatColor.GREEN + player.getName() + " has deposited " + 
                                            economy.formatCurrency(amount) + ChatColor.GREEN + " to your clan bank!");
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Failed to deposit to clan bank!");
        }
        
        return true;
    }
    
    /**
     * Handles the withdraw command.
     * 
     * @param player The player
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan economy withdraw <amount>");
            return true;
        }
        
        String clanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
        if (clanName == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        // Check if player has permission to withdraw
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        ClanMember member = clan.getMember(player.getUniqueId());
        
        if (member.getRole().getRoleLevel() < ClanRole.OFFICER.getRoleLevel()) {
            player.sendMessage(ChatColor.RED + "Only officers and leaders can withdraw from the clan bank!");
            return true;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount! Must be a number.");
            return true;
        }
        
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Amount must be greater than zero!");
            return true;
        }
        
        if (economy.playerWithdrawFromClan(player, clanName, amount)) {
            // Player already receives a success message in the playerWithdrawFromClan method
            
            // Broadcast to clan members
            for (ClanMember m : clan.getMembers()) {
                if (m.getRole().getRoleLevel() >= ClanRole.OFFICER.getRoleLevel()) {
                    Player memberPlayer = plugin.getServer().getPlayer(m.getPlayerUUID());
                    if (memberPlayer != null && !memberPlayer.equals(player)) {
                        memberPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " has withdrawn " + 
                                                economy.formatCurrency(amount) + ChatColor.YELLOW + " from your clan bank!");
                    }
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Failed to withdraw from clan bank! Check that there are sufficient funds.");
        }
        
        return true;
    }
    
    /**
     * Handles the transfer command.
     * 
     * @param player The player
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleTransfer(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /clan economy transfer <clan> <amount>");
            return true;
        }
        
        String clanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
        if (clanName == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        // Check if player has permission to transfer
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        ClanMember member = clan.getMember(player.getUniqueId());
        
        if (member.getRole().getRoleLevel() < ClanRole.LEADER.getRoleLevel()) {
            player.sendMessage(ChatColor.RED + "Only clan leaders can transfer funds to other clans!");
            return true;
        }
        
        String targetClanName = args[1];
        Clan targetClan = plugin.getStorageManager().getClanStorage().getClan(targetClanName);
        
        if (targetClan == null) {
            player.sendMessage(ChatColor.RED + "Clan " + targetClanName + " does not exist!");
            return true;
        }
        
        if (targetClanName.equalsIgnoreCase(clanName)) {
            player.sendMessage(ChatColor.RED + "You cannot transfer funds to your own clan!");
            return true;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount! Must be a number.");
            return true;
        }
        
        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Amount must be greater than zero!");
            return true;
        }
        
        if (economy.transferBetweenClans(clanName, targetClanName, amount)) {
            player.sendMessage(ChatColor.GREEN + "Successfully transferred " + 
                              economy.formatCurrency(amount) + ChatColor.GREEN + " to clan " + targetClanName + "!");
            
            // Notify source clan
            for (ClanMember m : clan.getMembers()) {
                if (m.getRole().getRoleLevel() >= ClanRole.OFFICER.getRoleLevel()) {
                    Player memberPlayer = plugin.getServer().getPlayer(m.getPlayerUUID());
                    if (memberPlayer != null && !memberPlayer.equals(player)) {
                        memberPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " has transferred " + 
                                                economy.formatCurrency(amount) + ChatColor.YELLOW + " to clan " + targetClanName + "!");
                    }
                }
            }
            
            // Notify target clan
            for (ClanMember m : targetClan.getMembers()) {
                if (m.getRole().getRoleLevel() >= ClanRole.OFFICER.getRoleLevel()) {
                    Player memberPlayer = plugin.getServer().getPlayer(m.getPlayerUUID());
                    if (memberPlayer != null) {
                        memberPlayer.sendMessage(ChatColor.GREEN + "Your clan has received a transfer of " + 
                                                economy.formatCurrency(amount) + ChatColor.GREEN + " from clan " + clanName + "!");
                    }
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Failed to transfer funds! Check that there are sufficient funds.");
        }
        
        return true;
    }
    
    /**
     * Handles the tax command.
     * 
     * @param player The player
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleTax(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan economy tax <rate>");
            return true;
        }
        
        String clanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
        if (clanName == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        // Check if player has permission to set tax rate
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        ClanMember member = clan.getMember(player.getUniqueId());
        
        if (member.getRole().getRoleLevel() < ClanRole.LEADER.getRoleLevel()) {
            player.sendMessage(ChatColor.RED + "Only clan leaders can set the tax rate!");
            return true;
        }
        
        double rate;
        try {
            rate = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid rate! Must be a number between 0 and 100.");
            return true;
        }
        
        if (rate < 0 || rate > 100) {
            player.sendMessage(ChatColor.RED + "Tax rate must be between 0 and 100!");
            return true;
        }
        
        // Convert percentage to decimal
        rate = rate / 100.0;
        
        economy.setTaxRate(clanName, rate);
        
        player.sendMessage(ChatColor.GREEN + "Tax rate set to " + String.format("%.1f%%", rate * 100) + "!");
        
        // Notify clan members
        for (ClanMember m : clan.getMembers()) {
            Player memberPlayer = plugin.getServer().getPlayer(m.getPlayerUUID());
            if (memberPlayer != null && !memberPlayer.equals(player)) {
                memberPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " has set the clan tax rate to " + 
                                        String.format("%.1f%%", rate * 100) + "!");
            }
        }
        
        return true;
    }
    
    /**
     * Handles the collect command.
     * 
     * @param player The player
     * @return True if the command was handled
     */
    private boolean handleCollect(Player player) {
        String clanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
        if (clanName == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        // Check if player has permission to collect taxes
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        ClanMember member = clan.getMember(player.getUniqueId());
        
        if (member.getRole().getRoleLevel() < ClanRole.OFFICER.getRoleLevel()) {
            player.sendMessage(ChatColor.RED + "Only officers and leaders can collect taxes!");
            return true;
        }
        
        // Try to collect taxes using Vault integration if available
        double collected = economy.collectTaxesWithVault(clanName, player.getUniqueId());
        
        if (collected == -1) {
            // On cooldown
            long cooldown = economy.getTaxCooldownRemaining(player.getUniqueId());
            long hours = TimeUnit.MILLISECONDS.toHours(cooldown);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(cooldown) % 60;
            
            player.sendMessage(ChatColor.RED + "You cannot collect taxes yet! " + 
                              "You must wait " + hours + "h " + minutes + "m.");
        } else if (collected > 0) {
            player.sendMessage(ChatColor.GREEN + "Successfully collected " + 
                              economy.formatCurrency(collected) + ChatColor.GREEN + " in taxes!");
            
            // Notify clan members
            for (ClanMember m : clan.getMembers()) {
                if (m.getRole().getRoleLevel() >= ClanRole.OFFICER.getRoleLevel()) {
                    Player memberPlayer = plugin.getServer().getPlayer(m.getPlayerUUID());
                    if (memberPlayer != null && !memberPlayer.equals(player)) {
                        memberPlayer.sendMessage(ChatColor.GREEN + player.getName() + " has collected " + 
                                                economy.formatCurrency(collected) + ChatColor.GREEN + " in taxes!");
                    }
                }
            }
        } else {
            player.sendMessage(ChatColor.YELLOW + "No taxes were collected. Either there are no members to tax or the tax rate is 0%.");
        }
        
        return true;
    }
    
    /**
     * Sends the help message for economy commands.
     * 
     * @param player The player
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Clan Economy Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/clan economy balance" + ChatColor.WHITE + " - Check your clan's balance");
        player.sendMessage(ChatColor.YELLOW + "/clan economy deposit <amount>" + ChatColor.WHITE + " - Deposit money to clan bank");
        player.sendMessage(ChatColor.YELLOW + "/clan economy withdraw <amount>" + ChatColor.WHITE + " - Withdraw money from clan bank (Officers+)");
        player.sendMessage(ChatColor.YELLOW + "/clan economy transfer <clan> <amount>" + ChatColor.WHITE + " - Transfer money to another clan (Leader only)");
        player.sendMessage(ChatColor.YELLOW + "/clan economy tax <rate>" + ChatColor.WHITE + " - Set tax rate (Leader only)");
        player.sendMessage(ChatColor.YELLOW + "/clan economy collect" + ChatColor.WHITE + " - Collect taxes from members (Officers+)");
    }
}
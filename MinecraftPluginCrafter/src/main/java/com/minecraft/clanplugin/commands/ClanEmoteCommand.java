package com.minecraft.clanplugin.commands;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.ClanRole;
import com.minecraft.clanplugin.utils.EmoteUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command handler for clan emote management.
 */
public class ClanEmoteCommand implements CommandExecutor, TabCompleter {
    
    private final ClanPlugin plugin;
    
    /**
     * Creates a new clan emote command handler.
     * 
     * @param plugin The clan plugin instance
     */
    public ClanEmoteCommand(ClanPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player has permission
        if (!player.hasPermission("clan.emote")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use clan emotes.");
            return true;
        }
        
        if (args.length == 0) {
            // No arguments, show usage
            showUsage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "list":
                return handleList(player);
                
            case "add":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /clanemote add <code> <emote>");
                    return true;
                }
                return handleAdd(player, args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                
            case "remove":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /clanemote remove <code>");
                    return true;
                }
                return handleRemove(player, args[1]);
                
            case "help":
                showHelp(player);
                return true;
                
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand: " + subCommand);
                showUsage(player);
                return true;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommand
            String[] subCommands = {"list", "add", "remove", "help"};
            String input = args[0].toLowerCase();
            
            for (String subCmd : subCommands) {
                if (subCmd.startsWith(input)) {
                    completions.add(subCmd);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            // Second argument for remove - show available emote codes for the player's clan
            if (sender instanceof Player) {
                Player player = (Player) sender;
                Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
                
                if (clan != null) {
                    // This would need implementation in EmoteUtils to get clan emote codes
                    // For now, we'll return an empty list
                }
            }
        }
        
        return completions;
    }
    
    /**
     * Shows the command usage help to a player.
     * 
     * @param player The player to show usage to
     */
    private void showUsage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Clan Emote Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/clanemote list " + ChatColor.WHITE + "- View available emotes");
        player.sendMessage(ChatColor.YELLOW + "/clanemote add <code> <emote> " + ChatColor.WHITE + "- Add a clan emote");
        player.sendMessage(ChatColor.YELLOW + "/clanemote remove <code> " + ChatColor.WHITE + "- Remove a clan emote");
        player.sendMessage(ChatColor.YELLOW + "/clanemote help " + ChatColor.WHITE + "- Show detailed help");
    }
    
    /**
     * Shows detailed help about the emote system.
     * 
     * @param player The player to show help to
     */
    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Clan Emote System ===");
        player.sendMessage(ChatColor.WHITE + "Emotes are special codes that can be used in chat messages.");
        player.sendMessage(ChatColor.WHITE + "To use an emote, type the code surrounded by colons. Example: " + 
                ChatColor.YELLOW + ":smile:" + ChatColor.WHITE + " will show as ☺");
        
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Global Emotes:");
        player.sendMessage(ChatColor.WHITE + "These are available to all players, regardless of clan.");
        
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Clan Emotes:");
        player.sendMessage(ChatColor.WHITE + "Each clan can have their own custom emotes that only members can use.");
        player.sendMessage(ChatColor.WHITE + "Officers and leaders can add/remove clan emotes.");
        
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Usage Examples:");
        player.sendMessage(ChatColor.WHITE + "• \"Hello there :smile:\" → \"Hello there ☺\"");
        player.sendMessage(ChatColor.WHITE + "• \"Good fight :sword: :shield:\" → \"Good fight ⚔ 🛡\"");
        
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.WHITE + "/clanemote list" + 
                ChatColor.YELLOW + " to see all available emotes.");
    }
    
    /**
     * Handles the list subcommand.
     * 
     * @param player The player executing the command
     * @return True if the command was handled
     */
    private boolean handleList(Player player) {
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        EmoteUtils.listEmotes(player, clan);
        return true;
    }
    
    /**
     * Handles the add subcommand.
     * 
     * @param player The player executing the command
     * @param emoteCode The emote code to add
     * @param emoteText The emote text/symbol
     * @return True if the command was handled
     */
    private boolean handleAdd(Player player, String emoteCode, String emoteText) {
        // Check if player is in a clan
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        // Check if player has permission to add emotes (officer+)
        ClanMember member = clan.getMember(player.getUniqueId());
        if (member.getRole().getRoleLevel() < ClanRole.OFFICER.getRoleLevel()) {
            player.sendMessage(ChatColor.RED + "Only officers and leaders can add clan emotes!");
            return true;
        }
        
        // Validate emote code
        if (emoteCode.length() < 2 || emoteCode.length() > 15) {
            player.sendMessage(ChatColor.RED + "Emote code must be between 2-15 characters!");
            return true;
        }
        
        if (!emoteCode.matches("[a-zA-Z0-9_]+")) {
            player.sendMessage(ChatColor.RED + "Emote code must contain only letters, numbers, and underscores!");
            return true;
        }
        
        // Validate emote text
        if (emoteText.length() < 1 || emoteText.length() > 5) {
            player.sendMessage(ChatColor.RED + "Emote text must be between 1-5 characters!");
            return true;
        }
        
        // Add the emote
        boolean result = EmoteUtils.addClanEmote(clan, emoteCode, emoteText);
        
        if (result) {
            player.sendMessage(ChatColor.GREEN + "Added clan emote: " + ChatColor.YELLOW + ":" + 
                    emoteCode + ": " + ChatColor.WHITE + "→ " + emoteText);
        } else {
            player.sendMessage(ChatColor.RED + "Failed to add clan emote. It may already exist.");
        }
        
        return true;
    }
    
    /**
     * Handles the remove subcommand.
     * 
     * @param player The player executing the command
     * @param emoteCode The emote code to remove
     * @return True if the command was handled
     */
    private boolean handleRemove(Player player, String emoteCode) {
        // Check if player is in a clan
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        // Check if player has permission to remove emotes (officer+)
        ClanMember member = clan.getMember(player.getUniqueId());
        if (member.getRole().getRoleLevel() < ClanRole.OFFICER.getRoleLevel()) {
            player.sendMessage(ChatColor.RED + "Only officers and leaders can remove clan emotes!");
            return true;
        }
        
        // Remove the emote
        boolean result = EmoteUtils.removeClanEmote(clan, emoteCode);
        
        if (result) {
            player.sendMessage(ChatColor.GREEN + "Removed clan emote: " + ChatColor.YELLOW + ":" + emoteCode + ":");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to remove clan emote. It may not exist.");
        }
        
        return true;
    }
}
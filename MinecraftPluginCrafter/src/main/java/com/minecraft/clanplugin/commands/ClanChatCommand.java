package com.minecraft.clanplugin.commands;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.utils.MessageUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command handler for clan chat.
 */
public class ClanChatCommand implements CommandExecutor {

    private final ClanPlugin plugin;

    public ClanChatCommand(ClanPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use clan chat!");
            return true;
        }

        Player player = (Player) sender;
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());

        // Check if player is in a clan
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }

        // Check if a message was provided
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /c <message>");
            return true;
        }

        // Combine all arguments into a single message
        StringBuilder messageBuilder = new StringBuilder();
        for (String arg : args) {
            messageBuilder.append(arg).append(" ");
        }
        String message = messageBuilder.toString().trim();

        // Get member information
        ClanMember member = clan.getMember(player.getUniqueId());
        
        // Format clan chat message
        String formattedMessage = MessageUtils.formatClanChatMessage(clan, member, message);
        
        // Send message to all online clan members
        MessageUtils.sendClanMessage(clan, formattedMessage);
        
        return true;
    }
}

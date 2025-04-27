package com.minecraft.clanplugin.utils;

import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.ClanRole;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Utility class for handling messages and notifications.
 */
public class MessageUtils {
    
    /**
     * Format a clan chat message with proper formatting.
     * 
     * @param clan The clan
     * @param member The clan member sending the message
     * @param message The message content
     * @return The formatted message
     */
    public static String formatClanChatMessage(Clan clan, ClanMember member, String message) {
        ChatColor roleColor;
        
        // Set color based on role
        switch (member.getRole()) {
            case LEADER:
                roleColor = ChatColor.GOLD;
                break;
            case OFFICER:
                roleColor = ChatColor.YELLOW;
                break;
            default:
                roleColor = ChatColor.WHITE;
        }
        
        return ChatColor.GRAY + "[" + ChatColor.AQUA + "Clan" + ChatColor.GRAY + "] " + 
               roleColor + member.getPlayerName() + ChatColor.WHITE + ": " + message;
    }
    
    /**
     * Send a message to all online members of a clan.
     * 
     * @param clan The clan
     * @param message The message to send
     */
    public static void sendClanMessage(Clan clan, String message) {
        for (ClanMember member : clan.getMembers()) {
            Player player = Bukkit.getPlayer(member.getPlayerUUID());
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }
    
    /**
     * Send a notification to all online members of a clan.
     * 
     * @param clan The clan
     * @param message The notification message
     */
    public static void notifyClan(Clan clan, String message) {
        String formattedMessage = ChatColor.GRAY + "[" + ChatColor.AQUA + "Clan" + ChatColor.GRAY + "] " + message;
        sendClanMessage(clan, formattedMessage);
    }
    
    /**
     * Get a colored string representation of a clan role.
     * 
     * @param role The clan role
     * @return The colored role name
     */
    public static String getColoredRoleName(ClanRole role) {
        switch (role) {
            case LEADER:
                return ChatColor.GOLD + "Leader";
            case OFFICER:
                return ChatColor.YELLOW + "Officer";
            default:
                return ChatColor.WHITE + "Member";
        }
    }
    
    /**
     * Format a message about clan visuals (armor and nametags).
     * 
     * @param action The action performed
     * @param target The target of the action
     * @return The formatted message
     */
    public static String formatVisualMessage(String action, String target) {
        return ChatColor.GRAY + "[" + ChatColor.LIGHT_PURPLE + "Visual" + ChatColor.GRAY + "] " +
               ChatColor.YELLOW + action + " " + ChatColor.WHITE + target;
    }
    
    /**
     * Send a message to a player when their armor is colored.
     * 
     * @param player The player
     * @param clan The clan
     */
    public static void sendArmorColoredMessage(Player player, Clan clan) {
        ChatColor clanColor = clan.getChatColor();
        player.sendMessage(formatVisualMessage("Your armor has been colored to match your clan:", 
                clanColor + clan.getName() + ChatColor.WHITE + "'s colors"));
    }
    
    /**
     * Send a message to a player when their nametag is updated.
     * 
     * @param player The player
     * @param clan The clan
     */
    public static void sendNametagUpdatedMessage(Player player, Clan clan) {
        ChatColor clanColor = clan.getChatColor();
        player.sendMessage(formatVisualMessage("Your nametag now displays your clan:", 
                clanColor + clan.getName() + ChatColor.WHITE + " [" + clanColor + clan.getTag() + ChatColor.WHITE + "]"));
    }
    
    /**
     * Send a message to a player when visual settings are toggled.
     * 
     * @param player The player
     * @param visualType The type of visual (armor/nametag)
     * @param enabled Whether the visual is now enabled or disabled
     */
    public static void sendVisualToggleMessage(Player player, String visualType, boolean enabled) {
        String status = enabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";
        player.sendMessage(formatVisualMessage("Clan " + visualType + " has been", status));
    }
}

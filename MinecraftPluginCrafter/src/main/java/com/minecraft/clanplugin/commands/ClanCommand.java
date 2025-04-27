package com.minecraft.clanplugin.commands;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.ClanRole;
import com.minecraft.clanplugin.skills.ClanSkill;
import com.minecraft.clanplugin.skills.MemberSkills;
import com.minecraft.clanplugin.skills.SkillTree;
import com.minecraft.clanplugin.wars.ClanWar;
import com.minecraft.clanplugin.utils.ItemUtils;
import com.minecraft.clanplugin.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Command handler for all clan-related commands.
 */
public class ClanCommand implements CommandExecutor {

    private final ClanPlugin plugin;

    public ClanCommand(ClanPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use clan commands!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                return handleCreate(player, args);
            case "join":
                return handleJoin(player, args);
            case "leave":
                return handleLeave(player);
            case "info":
                return handleInfo(player, args);
            case "invite":
                return handleInvite(player, args);
            case "kick":
                return handleKick(player, args);
            case "promote":
                return handlePromote(player, args);
            case "demote":
                return handleDemote(player, args);
            case "sethome":
                return handleSetHome(player);
            case "home":
                return handleHome(player);
            case "list":
                return handleList(player);
            case "ally":
                return handleAlly(player, args);
            case "unally":
                return handleUnally(player, args);
            case "enemy":
                return handleEnemy(player, args);
            case "unenemy":
                return handleUnenemy(player, args);
            case "color":
                return handleColor(player, args);
            case "gui":
                return handleGUI(player);
            case "territory":
                // Forward to territory command
                if (args.length > 1) {
                    String[] newArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                    plugin.getCommand("territory").execute(player, "territory", newArgs);
                } else {
                    plugin.getCommand("territory").execute(player, "territory", new String[0]);
                }
                return true;
            case "economy":
                // Forward to economy command
                if (args.length > 1) {
                    String[] newArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                    plugin.getCommand("economy").execute(player, "economy", newArgs);
                } else {
                    plugin.getCommand("economy").execute(player, "economy", new String[0]);
                }
                return true;
            case "war":
                // Forward to war command
                if (args.length > 1) {
                    String[] newArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                    plugin.getCommand("war").execute(player, "war", newArgs);
                } else {
                    plugin.getCommand("war").execute(player, "war", new String[0]);
                }
                return true;
            case "armor":
                return handleArmor(player, args);
            case "nametag":
                return handleNametag(player, args);
            case "level":
                return handleLevelCommand(player, args);
            case "skills":
                return handleSkills(player, args);
            case "help":
            default:
                sendHelpMessage(player);
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan create <name>");
            return true;
        }

        String clanName = args[1];

        // Check if player is already in a clan
        if (plugin.getStorageManager().getPlayerClan(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "You are already in a clan. Leave your current clan first.");
            return true;
        }

        // Check if clan name already exists
        if (plugin.getStorageManager().getClan(clanName) != null) {
            player.sendMessage(ChatColor.RED + "A clan with that name already exists!");
            return true;
        }

        // Create new clan
        Clan clan = new Clan(clanName);
        ClanMember member = new ClanMember(player.getUniqueId(), player.getName(), ClanRole.LEADER);
        clan.addMember(member);
        
        plugin.getStorageManager().addClan(clan);
        
        // Create and give clan flag
        ItemStack clanFlag = ItemUtils.createClanFlag(clanName);
        player.getInventory().addItem(clanFlag);
        
        player.sendMessage(ChatColor.GREEN + "You have created clan " + ChatColor.GOLD + clanName + ChatColor.GREEN + "!");
        return true;
    }

    private boolean handleJoin(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan join <name>");
            return true;
        }

        String clanName = args[1];
        Clan clan = plugin.getStorageManager().getClan(clanName);

        // Check if clan exists
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "No clan found with name: " + clanName);
            return true;
        }

        // Check if player is already in a clan
        if (plugin.getStorageManager().getPlayerClan(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "You are already in a clan. Leave your current clan first.");
            return true;
        }

        // Check if player is invited to this clan
        if (!clan.isInvited(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You need an invitation to join this clan.");
            return true;
        }

        // Add player to clan
        ClanMember member = new ClanMember(player.getUniqueId(), player.getName(), ClanRole.MEMBER);
        clan.addMember(member);
        clan.removeInvite(player.getUniqueId());
        
        player.sendMessage(ChatColor.GREEN + "You have joined clan " + ChatColor.GOLD + clanName + ChatColor.GREEN + "!");
        
        // Notify online clan members
        MessageUtils.notifyClan(clan, ChatColor.GREEN + player.getName() + " has joined the clan!");
        
        return true;
    }

    private boolean handleLeave(Player player) {
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        // Check if player is in a clan
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        ClanMember member = clan.getMember(player.getUniqueId());
        
        // Check if player is the leader
        if (member.getRole() == ClanRole.LEADER) {
            player.sendMessage(ChatColor.RED + "As the leader, you cannot leave the clan. Transfer leadership first or disband the clan.");
            return true;
        }
        
        // Remove player from clan
        clan.removeMember(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "You have left clan " + ChatColor.GOLD + clan.getName() + ChatColor.GREEN + "!");
        
        // Notify online clan members
        MessageUtils.notifyClan(clan, ChatColor.YELLOW + player.getName() + " has left the clan.");
        
        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        Clan clan;
        
        if (args.length >= 2) {
            // Get info for specified clan
            String clanName = args[1];
            clan = plugin.getStorageManager().getClan(clanName);
            
            if (clan == null) {
                player.sendMessage(ChatColor.RED + "No clan found with name: " + clanName);
                return true;
            }
        } else {
            // Get info for player's clan
            clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
            
            if (clan == null) {
                player.sendMessage(ChatColor.RED + "You are not in a clan! Use /clan info <name> to view other clans.");
                return true;
            }
        }
        
        // Display clan info
        player.sendMessage(ChatColor.GOLD + "=== Clan Info: " + clan.getName() + " ===");
        player.sendMessage(ChatColor.YELLOW + "Tag: " + clan.getTag());
        player.sendMessage(ChatColor.YELLOW + "Members: " + clan.getMembers().size());
        
        player.sendMessage(ChatColor.YELLOW + "Leader: ");
        for (ClanMember member : clan.getMembers()) {
            if (member.getRole() == ClanRole.LEADER) {
                player.sendMessage(ChatColor.WHITE + "  - " + member.getPlayerName());
            }
        }
        
        player.sendMessage(ChatColor.YELLOW + "Officers: ");
        for (ClanMember member : clan.getMembers()) {
            if (member.getRole() == ClanRole.OFFICER) {
                player.sendMessage(ChatColor.WHITE + "  - " + member.getPlayerName());
            }
        }
        
        player.sendMessage(ChatColor.YELLOW + "Members: ");
        for (ClanMember member : clan.getMembers()) {
            if (member.getRole() == ClanRole.MEMBER) {
                player.sendMessage(ChatColor.WHITE + "  - " + member.getPlayerName());
            }
        }
        
        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan invite <player>");
            return true;
        }
        
        String targetName = args[1];
        Player targetPlayer = plugin.getServer().getPlayer(targetName);
        
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player not found or not online.");
            return true;
        }
        
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        // Check if player is in a clan
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        ClanMember member = clan.getMember(player.getUniqueId());
        
        // Check if player has permission to invite
        if (member.getRole() == ClanRole.MEMBER) {
            player.sendMessage(ChatColor.RED + "Only clan leaders and officers can invite players.");
            return true;
        }
        
        // Check if target is already in a clan
        if (plugin.getStorageManager().getPlayerClan(targetPlayer.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + targetPlayer.getName() + " is already in a clan.");
            return true;
        }
        
        // Add invite
        clan.addInvite(targetPlayer.getUniqueId());
        
        player.sendMessage(ChatColor.GREEN + "You have invited " + targetPlayer.getName() + " to join your clan.");
        targetPlayer.sendMessage(ChatColor.GREEN + "You have been invited to join clan " + 
                                ChatColor.GOLD + clan.getName() + 
                                ChatColor.GREEN + ". Use /clan join " + clan.getName() + " to accept.");
        
        return true;
    }

    private boolean handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan kick <player>");
            return true;
        }
        
        String targetName = args[1];
        
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        // Check if player is in a clan
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        ClanMember playerMember = clan.getMember(player.getUniqueId());
        
        // Check if player has permission to kick
        if (playerMember.getRole() == ClanRole.MEMBER) {
            player.sendMessage(ChatColor.RED + "Only clan leaders and officers can kick players.");
            return true;
        }
        
        // Find target member
        ClanMember targetMember = null;
        for (ClanMember member : clan.getMembers()) {
            if (member.getPlayerName().equalsIgnoreCase(targetName)) {
                targetMember = member;
                break;
            }
        }
        
        if (targetMember == null) {
            player.sendMessage(ChatColor.RED + "Player not found in your clan.");
            return true;
        }
        
        // Check if target has higher role
        if (targetMember.getRole().ordinal() <= playerMember.getRole().ordinal()) {
            player.sendMessage(ChatColor.RED + "You cannot kick members with equal or higher rank than you.");
            return true;
        }
        
        // Remove member from clan
        clan.removeMember(targetMember.getPlayerUUID());
        
        player.sendMessage(ChatColor.GREEN + "You have kicked " + targetMember.getPlayerName() + " from the clan.");
        
        // Notify target player if online
        Player targetPlayer = plugin.getServer().getPlayer(targetMember.getPlayerUUID());
        if (targetPlayer != null) {
            targetPlayer.sendMessage(ChatColor.RED + "You have been kicked from clan " + clan.getName() + ".");
        }
        
        // Notify online clan members
        MessageUtils.notifyClan(clan, ChatColor.YELLOW + targetMember.getPlayerName() + " has been kicked from the clan.");
        
        return true;
    }

    private boolean handlePromote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan promote <player>");
            return true;
        }
        
        String targetName = args[1];
        
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        // Check if player is in a clan
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        ClanMember playerMember = clan.getMember(player.getUniqueId());
        
        // Check if player has permission to promote
        if (playerMember.getRole() != ClanRole.LEADER) {
            player.sendMessage(ChatColor.RED + "Only clan leaders can promote players.");
            return true;
        }
        
        // Find target member
        ClanMember targetMember = null;
        for (ClanMember member : clan.getMembers()) {
            if (member.getPlayerName().equalsIgnoreCase(targetName)) {
                targetMember = member;
                break;
            }
        }
        
        if (targetMember == null) {
            player.sendMessage(ChatColor.RED + "Player not found in your clan.");
            return true;
        }
        
        // Check if target is already an officer
        if (targetMember.getRole() == ClanRole.OFFICER) {
            player.sendMessage(ChatColor.RED + "This player is already an officer.");
            return true;
        }
        
        // Promote member
        targetMember.setRole(ClanRole.OFFICER);
        
        player.sendMessage(ChatColor.GREEN + "You have promoted " + targetMember.getPlayerName() + " to Officer.");
        
        // Notify target player if online
        Player targetPlayer = plugin.getServer().getPlayer(targetMember.getPlayerUUID());
        if (targetPlayer != null) {
            targetPlayer.sendMessage(ChatColor.GREEN + "You have been promoted to Officer in your clan.");
        }
        
        // Notify online clan members
        MessageUtils.notifyClan(clan, ChatColor.YELLOW + targetMember.getPlayerName() + " has been promoted to Officer.");
        
        return true;
    }

    private boolean handleDemote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan demote <player>");
            return true;
        }
        
        String targetName = args[1];
        
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        // Check if player is in a clan
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        ClanMember playerMember = clan.getMember(player.getUniqueId());
        
        // Check if player has permission to demote
        if (playerMember.getRole() != ClanRole.LEADER) {
            player.sendMessage(ChatColor.RED + "Only clan leaders can demote players.");
            return true;
        }
        
        // Find target member
        ClanMember targetMember = null;
        for (ClanMember member : clan.getMembers()) {
            if (member.getPlayerName().equalsIgnoreCase(targetName)) {
                targetMember = member;
                break;
            }
        }
        
        if (targetMember == null) {
            player.sendMessage(ChatColor.RED + "Player not found in your clan.");
            return true;
        }
        
        // Check if target is already a member
        if (targetMember.getRole() == ClanRole.MEMBER) {
            player.sendMessage(ChatColor.RED + "This player is already a regular member.");
            return true;
        }
        
        // Demote member
        targetMember.setRole(ClanRole.MEMBER);
        
        player.sendMessage(ChatColor.GREEN + "You have demoted " + targetMember.getPlayerName() + " to Member.");
        
        // Notify target player if online
        Player targetPlayer = plugin.getServer().getPlayer(targetMember.getPlayerUUID());
        if (targetPlayer != null) {
            targetPlayer.sendMessage(ChatColor.YELLOW + "You have been demoted to Member in your clan.");
        }
        
        // Notify online clan members
        MessageUtils.notifyClan(clan, ChatColor.YELLOW + targetMember.getPlayerName() + " has been demoted to Member.");
        
        return true;
    }

    private boolean handleSetHome(Player player) {
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        // Check if player is in a clan
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        ClanMember member = clan.getMember(player.getUniqueId());
        
        // Check if player has permission to set home
        if (member.getRole() == ClanRole.MEMBER) {
            player.sendMessage(ChatColor.RED + "Only clan leaders and officers can set the clan home.");
            return true;
        }
        
        // Set clan home
        Location location = player.getLocation();
        clan.setHome(location);
        
        player.sendMessage(ChatColor.GREEN + "Clan home has been set at your current location.");
        
        // Notify online clan members
        MessageUtils.notifyClan(clan, ChatColor.YELLOW + "Clan home has been updated by " + player.getName() + ".");
        
        return true;
    }

    private boolean handleHome(Player player) {
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        // Check if player is in a clan
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        // Check if clan has a home set
        if (clan.getHome() == null) {
            player.sendMessage(ChatColor.RED + "Your clan does not have a home set yet.");
            return true;
        }
        
        // Teleport player to clan home
        player.teleport(clan.getHome());
        player.sendMessage(ChatColor.GREEN + "Teleported to clan home.");
        
        return true;
    }

    private boolean handleList(Player player) {
        Set<Clan> clans = plugin.getStorageManager().getAllClans();
        
        if (clans.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "There are no clans created yet.");
            return true;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Clans List ===");
        for (Clan clan : clans) {
            player.sendMessage(ChatColor.YELLOW + "- " + clan.getName() + 
                              ChatColor.WHITE + " (" + clan.getMembers().size() + " members)");
        }
        
        return true;
    }
    
    private boolean handleAlly(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan ally <clan name>");
            return true;
        }
        
        String targetClanName = args[1];
        Clan playerClan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        // Check if player is in a clan
        if (playerClan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        // Check if player has permission
        ClanMember member = playerClan.getMember(player.getUniqueId());
        if (member.getRole() != ClanRole.LEADER) {
            player.sendMessage(ChatColor.RED + "Only clan leaders can create alliances.");
            return true;
        }
        
        // Check if target clan exists
        Clan targetClan = plugin.getStorageManager().getClan(targetClanName);
        if (targetClan == null) {
            player.sendMessage(ChatColor.RED + "No clan found with name: " + targetClanName);
            return true;
        }
        
        // Check if trying to ally with own clan
        if (playerClan.getName().equalsIgnoreCase(targetClanName)) {
            player.sendMessage(ChatColor.RED + "You cannot ally with your own clan.");
            return true;
        }
        
        // Check if already allied
        if (playerClan.isAllied(targetClanName)) {
            player.sendMessage(ChatColor.RED + "You are already allied with " + targetClanName);
            return true;
        }
        
        // Add alliance
        playerClan.addAlliance(targetClanName);
        player.sendMessage(ChatColor.GREEN + "You have marked " + targetClanName + " as an ally. They will need to ally you back for a mutual alliance.");
        
        // Notify other clan's online members
        for (ClanMember targetMember : targetClan.getMembers()) {
            Player targetPlayer = plugin.getServer().getPlayer(targetMember.getPlayerUUID());
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(ChatColor.GREEN + "Clan " + playerClan.getName() + " has marked your clan as an ally.");
            }
        }
        
        // Save changes
        plugin.getStorageManager().saveClan(playerClan);
        
        return true;
    }
    
    private boolean handleUnally(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan unally <clan name>");
            return true;
        }
        
        String targetClanName = args[1];
        Clan playerClan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        // Check if player is in a clan
        if (playerClan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        // Check if player has permission
        ClanMember member = playerClan.getMember(player.getUniqueId());
        if (member.getRole() != ClanRole.LEADER) {
            player.sendMessage(ChatColor.RED + "Only clan leaders can remove alliances.");
            return true;
        }
        
        // Check if actually allied
        if (!playerClan.isAllied(targetClanName)) {
            player.sendMessage(ChatColor.RED + "You are not allied with " + targetClanName);
            return true;
        }
        
        // Remove alliance
        playerClan.removeAlliance(targetClanName);
        player.sendMessage(ChatColor.GREEN + "You are no longer allied with " + targetClanName);
        
        // Notify other clan's online members if they exist
        Clan targetClan = plugin.getStorageManager().getClan(targetClanName);
        if (targetClan != null) {
            for (ClanMember targetMember : targetClan.getMembers()) {
                Player targetPlayer = plugin.getServer().getPlayer(targetMember.getPlayerUUID());
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    targetPlayer.sendMessage(ChatColor.YELLOW + "Clan " + playerClan.getName() + " has removed their alliance with your clan.");
                }
            }
        }
        
        // Save changes
        plugin.getStorageManager().saveClan(playerClan);
        
        return true;
    }
    
    private boolean handleEnemy(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan enemy <clan name>");
            return true;
        }
        
        String targetClanName = args[1];
        Clan playerClan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        // Check if player is in a clan
        if (playerClan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        // Check if player has permission
        ClanMember member = playerClan.getMember(player.getUniqueId());
        if (member.getRole() != ClanRole.LEADER) {
            player.sendMessage(ChatColor.RED + "Only clan leaders can declare enemies.");
            return true;
        }
        
        // Check if target clan exists
        Clan targetClan = plugin.getStorageManager().getClan(targetClanName);
        if (targetClan == null) {
            player.sendMessage(ChatColor.RED + "No clan found with name: " + targetClanName);
            return true;
        }
        
        // Check if trying to enemy own clan
        if (playerClan.getName().equalsIgnoreCase(targetClanName)) {
            player.sendMessage(ChatColor.RED + "You cannot mark your own clan as an enemy.");
            return true;
        }
        
        // Check if already an enemy
        if (playerClan.isEnemy(targetClanName)) {
            player.sendMessage(ChatColor.RED + "You have already marked " + targetClanName + " as an enemy.");
            return true;
        }
        
        // Add enemy
        playerClan.addEnemy(targetClanName);
        player.sendMessage(ChatColor.GREEN + "You have marked " + targetClanName + " as an enemy.");
        
        // Notify other clan's online members
        for (ClanMember targetMember : targetClan.getMembers()) {
            Player targetPlayer = plugin.getServer().getPlayer(targetMember.getPlayerUUID());
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(ChatColor.RED + "Warning: Clan " + playerClan.getName() + " has marked your clan as an enemy!");
            }
        }
        
        // Save changes
        plugin.getStorageManager().saveClan(playerClan);
        
        return true;
    }
    
    private boolean handleUnenemy(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan unenemy <clan name>");
            return true;
        }
        
        String targetClanName = args[1];
        Clan playerClan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        // Check if player is in a clan
        if (playerClan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        // Check if player has permission
        ClanMember member = playerClan.getMember(player.getUniqueId());
        if (member.getRole() != ClanRole.LEADER) {
            player.sendMessage(ChatColor.RED + "Only clan leaders can remove enemy status.");
            return true;
        }
        
        // Check if actually an enemy
        if (!playerClan.isEnemy(targetClanName)) {
            player.sendMessage(ChatColor.RED + targetClanName + " is not marked as an enemy.");
            return true;
        }
        
        // Remove enemy
        playerClan.removeEnemy(targetClanName);
        player.sendMessage(ChatColor.GREEN + "You have removed " + targetClanName + " from your enemies list.");
        
        // Notify other clan's online members if they exist
        Clan targetClan = plugin.getStorageManager().getClan(targetClanName);
        if (targetClan != null) {
            for (ClanMember targetMember : targetClan.getMembers()) {
                Player targetPlayer = plugin.getServer().getPlayer(targetMember.getPlayerUUID());
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    targetPlayer.sendMessage(ChatColor.GREEN + "Clan " + playerClan.getName() + " has removed your clan from their enemies list.");
                }
            }
        }
        
        // Save changes
        plugin.getStorageManager().saveClan(playerClan);
        
        return true;
    }
    
    private boolean handleColor(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan color <color>");
            player.sendMessage(ChatColor.RED + "Available colors: RED, BLUE, GREEN, YELLOW, PURPLE, AQUA, GOLD, BLACK, WHITE");
            return true;
        }
        
        String colorName = args[1].toUpperCase();
        Clan playerClan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        // Check if player is in a clan
        if (playerClan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        // Check if player has permission
        ClanMember member = playerClan.getMember(player.getUniqueId());
        if (member.getRole() != ClanRole.LEADER) {
            player.sendMessage(ChatColor.RED + "Only clan leaders can change clan color.");
            return true;
        }
        
        // Map color name to ChatColor
        ChatColor color;
        try {
            color = ChatColor.valueOf(colorName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid color. Available colors: RED, BLUE, GREEN, YELLOW, PURPLE, AQUA, GOLD, BLACK, WHITE");
            return true;
        }
        
        // Set clan color
        playerClan.setColor(color.toString());
        player.sendMessage(ChatColor.GREEN + "Clan color set to " + color + colorName);
        
        // Notify clan members
        MessageUtils.notifyClan(playerClan, "Clan color has been changed to " + color + colorName);
        
        // Save changes
        plugin.getStorageManager().saveClan(playerClan);
        
        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Clan Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/clan create <name>" + ChatColor.WHITE + " - Create a new clan");
        player.sendMessage(ChatColor.YELLOW + "/clan join <name>" + ChatColor.WHITE + " - Join a clan (requires invitation)");
        player.sendMessage(ChatColor.YELLOW + "/clan leave" + ChatColor.WHITE + " - Leave your current clan");
        player.sendMessage(ChatColor.YELLOW + "/clan info [name]" + ChatColor.WHITE + " - View clan information");
        player.sendMessage(ChatColor.YELLOW + "/clan invite <player>" + ChatColor.WHITE + " - Invite a player to your clan");
        player.sendMessage(ChatColor.YELLOW + "/clan kick <player>" + ChatColor.WHITE + " - Kick a player from your clan");
        player.sendMessage(ChatColor.YELLOW + "/clan promote <player>" + ChatColor.WHITE + " - Promote a member to officer");
        player.sendMessage(ChatColor.YELLOW + "/clan demote <player>" + ChatColor.WHITE + " - Demote an officer to member");
        player.sendMessage(ChatColor.YELLOW + "/clan sethome" + ChatColor.WHITE + " - Set your clan's home location");
        player.sendMessage(ChatColor.YELLOW + "/clan home" + ChatColor.WHITE + " - Teleport to your clan's home");
        player.sendMessage(ChatColor.YELLOW + "/clan list" + ChatColor.WHITE + " - List all clans on the server");
        player.sendMessage(ChatColor.YELLOW + "/clan ally <name>" + ChatColor.WHITE + " - Form an alliance with another clan");
        player.sendMessage(ChatColor.YELLOW + "/clan unally <name>" + ChatColor.WHITE + " - Remove an alliance");
        player.sendMessage(ChatColor.YELLOW + "/clan enemy <name>" + ChatColor.WHITE + " - Mark a clan as an enemy");
        player.sendMessage(ChatColor.YELLOW + "/clan unenemy <name>" + ChatColor.WHITE + " - Remove a clan from enemy list");
        player.sendMessage(ChatColor.YELLOW + "/clan color <color>" + ChatColor.WHITE + " - Set your clan's color");
        player.sendMessage(ChatColor.YELLOW + "/clan armor" + ChatColor.WHITE + " - Toggle clan-colored armor");
        player.sendMessage(ChatColor.YELLOW + "/clan nametag" + ChatColor.WHITE + " - Refresh clan nametags");
        player.sendMessage(ChatColor.YELLOW + "/clan skills" + ChatColor.WHITE + " - View and manage clan member skills");
        player.sendMessage(ChatColor.YELLOW + "/clan gui" + ChatColor.WHITE + " - Open the clan management GUI");
        player.sendMessage(ChatColor.YELLOW + "/c <message>" + ChatColor.WHITE + " - Send a message to clan chat");
        
        player.sendMessage(ChatColor.GOLD + "=== Skill Specializations ===");
        player.sendMessage(ChatColor.YELLOW + "Miner" + ChatColor.WHITE + " - Bonuses for mining activities and ore processing");
        player.sendMessage(ChatColor.YELLOW + "Farmer" + ChatColor.WHITE + " - Bonuses for farming, animal husbandry, and food production");
        player.sendMessage(ChatColor.YELLOW + "Builder" + ChatColor.WHITE + " - Bonuses for construction projects and block placement");
        player.sendMessage(ChatColor.YELLOW + "Hunter" + ChatColor.WHITE + " - Bonuses for combat, mob hunting, and weaponry");
    }
    
    /**
     * Handles the armor command to toggle colored armor for a clan.
     * 
     * @param player The player executing the command
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleArmor(Player player, String[] args) {
        // Check if player has permission to use this command
        if (!player.hasPermission("clan.visual.armor")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        // Check if player is in a clan
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        ClanMember member = clan.getMember(player.getUniqueId());
        
        // Check if player has permission to toggle armor
        if (member.getRole() != ClanRole.LEADER && member.getRole() != ClanRole.OFFICER) {
            player.sendMessage(ChatColor.RED + "Only clan leaders and officers can toggle clan armor.");
            return true;
        }
        
        // Check if visual identity is enabled in config
        if (!plugin.getConfig().getBoolean("visual_identity.armor.enabled", true)) {
            player.sendMessage(ChatColor.RED + "Clan armor is disabled on this server.");
            return true;
        }
        
        // Check if clan level is high enough
        int minLevel = plugin.getConfig().getInt("visual_identity.armor.min_clan_level", 2);
        if (clan.getLevel() < minLevel) {
            player.sendMessage(ChatColor.RED + "Your clan must be at least level " + minLevel + " to use colored armor.");
            return true;
        }
        
        // Toggle colored armor
        boolean newState = !clan.hasColoredArmor();
        clan.setColoredArmor(newState);
        
        // Send toggle message
        if (newState) {
            player.sendMessage(ChatColor.GREEN + "Clan-colored armor has been " + ChatColor.GOLD + "enabled" + 
                              ChatColor.GREEN + " for all clan members!");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Clan-colored armor has been " + ChatColor.RED + "disabled" + 
                              ChatColor.YELLOW + " for all clan members.");
        }
        
        // Apply armor colors if enabled
        if (newState) {
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                Clan playerClan = plugin.getStorageManager().getPlayerClan(onlinePlayer.getUniqueId());
                if (playerClan != null && playerClan.equals(clan)) {
                    plugin.getArmorListener().colorPlayerArmor(onlinePlayer);
                }
            }
        }
        
        return true;
    }
    
    /**
     * Handles the nametag command to toggle clan nametags in TAB list.
     * 
     * @param player The player executing the command
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleNametag(Player player, String[] args) {
        // Check if player has permission to use this command
        if (!player.hasPermission("clan.visual.nametag")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        // Check if nametags are enabled in config
        if (!plugin.getConfig().getBoolean("visual_identity.nametags.enabled", true)) {
            player.sendMessage(ChatColor.RED + "Clan nametags are disabled on this server.");
            return true;
        }
        
        // Force update nametags for all players
        plugin.getNametagManager().updateAllTeams();
        player.sendMessage(ChatColor.GREEN + "All clan nametags have been refreshed!");
        
        return true;
    }
    
    private boolean handleGUI(Player player) {
        Clan playerClan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        // Check if player is in a clan
        if (playerClan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        // Get player's clan role
        ClanMember playerMember = playerClan.getMember(player.getUniqueId());
        String playerRoleName = MessageUtils.getColoredRoleName(playerMember.getRole());
        
        // Create inventory for GUI (6 rows of 9 slots)
        Inventory gui = plugin.getServer().createInventory(player, 54, ChatColor.DARK_PURPLE + "Clan Management");
        
        // Add glass pane border with a more attractive pattern - use different colors for top/bottom
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, ItemUtils.createGuiItem(Material.PURPLE_STAINED_GLASS_PANE, " ", null));
            gui.setItem(45 + i, ItemUtils.createGuiItem(Material.PURPLE_STAINED_GLASS_PANE, " ", null));
        }
        
        // Side borders with alternating colors
        for (int i = 1; i < 5; i++) {
            Material leftMaterial = i % 2 == 0 ? Material.PURPLE_STAINED_GLASS_PANE : Material.MAGENTA_STAINED_GLASS_PANE;
            Material rightMaterial = i % 2 == 0 ? Material.MAGENTA_STAINED_GLASS_PANE : Material.PURPLE_STAINED_GLASS_PANE;
            
            gui.setItem(i * 9, ItemUtils.createGuiItem(leftMaterial, " ", null));
            gui.setItem(i * 9 + 8, ItemUtils.createGuiItem(rightMaterial, " ", null));
        }
        
        // ------------- TOP ROW: CLAN INFO SECTION -------------
        
        // Clan emblem/banner - slot 4 (center of top row)
        Material bannerMaterial = Material.WHITE_BANNER;
        // Try to convert clan color to banner color
        try {
            String colorName = playerClan.getColor().toUpperCase() + "_BANNER";
            bannerMaterial = Material.valueOf(colorName);
        } catch (Exception e) {
            // Use default if conversion fails
        }
        
        // Clan name and tag - slot 4 (center)
        List<String> clanInfo = new ArrayList<>();
        clanInfo.add(ChatColor.GOLD + "====================");
        clanInfo.add(ChatColor.WHITE + "Tag: " + ChatColor.AQUA + "[" + playerClan.getTag() + "]");
        clanInfo.add(ChatColor.WHITE + "Members: " + ChatColor.GREEN + playerClan.getMembers().size() + "/" + plugin.getConfig().getInt("clan.max_members", 20));
        clanInfo.add(ChatColor.WHITE + "Level: " + ChatColor.GOLD + playerClan.getLevel());
        clanInfo.add(ChatColor.WHITE + "Your Role: " + playerRoleName);
        clanInfo.add(ChatColor.GOLD + "====================");
        
        // Create clan info item
        ItemStack clanInfoItem = ItemUtils.createInfoItem(
            bannerMaterial, 
            ChatColor.GOLD + "❖ " + playerClan.getName() + " ❖", 
            clanInfo
        );
        gui.setItem(4, clanInfoItem);
        
        // ------------- SECOND ROW: CLAN STATS AND PROGRESSION -------------
        
        // Level and XP display - slot 11
        List<String> levelLore = new ArrayList<>();
        levelLore.add("");
        levelLore.add(ChatColor.WHITE + "Current Level: " + ChatColor.GOLD + playerClan.getLevel());
        levelLore.add(ChatColor.WHITE + "Experience: " + ChatColor.YELLOW + playerClan.getExperience() + " points");
        
        // Add next level info if not at max level
        int nextLevelXP = plugin.getProgressionManager().getExperienceForNextLevel(playerClan);
        if (nextLevelXP > 0) {
            levelLore.add("");
            levelLore.add(ChatColor.WHITE + "Next Level: " + ChatColor.YELLOW + nextLevelXP + " XP needed");
            
            // Calculate progress percentage
            int currentLevelXP = plugin.getProgressionManager().getRequiredExperienceForLevel(playerClan.getLevel());
            int nextTotalXP = plugin.getProgressionManager().getRequiredExperienceForLevel(playerClan.getLevel() + 1);
            int progress = (int) (((double) (playerClan.getExperience() - currentLevelXP) / 
                                  (nextTotalXP - currentLevelXP)) * 100);
            
            // Create a visual progress bar
            StringBuilder progressBar = new StringBuilder(ChatColor.WHITE + "[");
            int barLength = 20;
            int filledBars = (int) ((progress / 100.0) * barLength);
            
            for (int i = 0; i < barLength; i++) {
                if (i < filledBars) {
                    progressBar.append(ChatColor.GREEN + "█");
                } else {
                    progressBar.append(ChatColor.GRAY + "█");
                }
            }
            progressBar.append(ChatColor.WHITE + "] " + ChatColor.YELLOW + progress + "%");
            
            levelLore.add(progressBar.toString());
        } else {
            levelLore.add("");
            levelLore.add(ChatColor.GREEN + "Maximum level reached!");
            levelLore.add(ChatColor.GREEN + "Congratulations!");
        }
        
        levelLore.add("");
        levelLore.add(ChatColor.YELLOW + "Click to view progression details");
        
        ItemStack levelItem = ItemUtils.createGuiItem(
            Material.EXPERIENCE_BOTTLE, 
            ChatColor.GREEN + "Clan Level & Progression", 
            levelLore
        );
        gui.setItem(11, levelItem);
        
        // Economy status - slot 13
        List<String> economyLore = new ArrayList<>();
        economyLore.add("");
        
        // Get clan bank balance
        double balance = 0;
        if (plugin.getEconomy() != null) {
            balance = plugin.getEconomy().getClanBalance(playerClan.getName());
        }
        
        economyLore.add(ChatColor.WHITE + "Bank Balance: " + ChatColor.GOLD + String.format("%.2f", balance));
        economyLore.add("");
        
        // Display tax rate if enabled
        if (plugin.getConfig().getBoolean("economy.tax_enabled", true)) {
            double taxRate = plugin.getConfig().getDouble("economy.tax_rate", 0.05);
            economyLore.add(ChatColor.WHITE + "Tax Rate: " + ChatColor.YELLOW + (taxRate * 100) + "%");
            economyLore.add(ChatColor.GRAY + "Collected every " + 
                          plugin.getConfig().getInt("economy.tax_interval", 24) + " hours");
        }
        
        economyLore.add("");
        economyLore.add(ChatColor.YELLOW + "Click to manage clan economy");
        
        ItemStack economyItem = ItemUtils.createGuiItem(
            Material.GOLD_INGOT, 
            ChatColor.GOLD + "Clan Economy", 
            economyLore
        );
        gui.setItem(13, economyItem);
        
        // Territory status - slot 15
        List<String> territoryLore = new ArrayList<>();
        territoryLore.add("");
        
        // Get claimed territory count
        int territoryCount = plugin.getStorageManager().getTerritoryManager().getClanTerritories(playerClan.getName()).size();
        int maxClaims = plugin.getStorageManager().getTerritoryManager().calculateMaxClaims(playerClan);
        
        territoryLore.add(ChatColor.WHITE + "Claimed Chunks: " + ChatColor.GREEN + territoryCount + "/" + maxClaims);
        
        // Check if clan has a home
        if (playerClan.getHome() != null) {
            Location home = playerClan.getHome();
            territoryLore.add("");
            territoryLore.add(ChatColor.WHITE + "Clan Home: " + ChatColor.YELLOW + 
                           "X: " + home.getBlockX() + 
                           ", Y: " + home.getBlockY() + 
                           ", Z: " + home.getBlockZ());
        }
        
        territoryLore.add("");
        territoryLore.add(ChatColor.YELLOW + "Click to manage territories");
        
        ItemStack territoryItem = ItemUtils.createGuiItem(
            Material.MAP, 
            ChatColor.DARK_GREEN + "Clan Territory", 
            territoryLore
        );
        gui.setItem(15, territoryItem);
        
        // ------------- THIRD ROW: MEMBER MANAGEMENT AND CLAN HOME -------------
        
        // Member management - slot 20
        List<String> memberLore = new ArrayList<>();
        memberLore.add("");
        memberLore.add(ChatColor.WHITE + "Total Members: " + ChatColor.GREEN + playerClan.getMembers().size());
        
        // Count online members
        int onlineCount = 0;
        for (ClanMember member : playerClan.getMembers()) {
            if (Bukkit.getPlayer(member.getPlayerUUID()) != null) {
                onlineCount++;
            }
        }
        
        memberLore.add(ChatColor.WHITE + "Online Members: " + ChatColor.GREEN + onlineCount);
        memberLore.add("");
        
        // Add role counts
        int leaders = 0, officers = 0, members = 0;
        for (ClanMember member : playerClan.getMembers()) {
            if (member.getRole() == ClanRole.LEADER) leaders++;
            else if (member.getRole() == ClanRole.OFFICER) officers++;
            else members++;
        }
        
        memberLore.add(ChatColor.WHITE + "Leaders: " + ChatColor.GOLD + leaders);
        memberLore.add(ChatColor.WHITE + "Officers: " + ChatColor.BLUE + officers);
        memberLore.add(ChatColor.WHITE + "Members: " + ChatColor.GREEN + members);
        memberLore.add("");
        memberLore.add(ChatColor.YELLOW + "Click to manage clan members");
        
        ItemStack membersItem = ItemUtils.createGuiItem(
            Material.PLAYER_HEAD, 
            ChatColor.AQUA + "Manage Members", 
            memberLore
        );
        gui.setItem(20, membersItem);
        
        // Clan home teleport - slot 22
        List<String> homeLore = new ArrayList<>();
        homeLore.add("");
        if (playerClan.getHome() != null) {
            Location home = playerClan.getHome();
            homeLore.add(ChatColor.WHITE + "Location: " + ChatColor.YELLOW + 
                       "X: " + home.getBlockX() + 
                       ", Y: " + home.getBlockY() + 
                       ", Z: " + home.getBlockZ());
            homeLore.add(ChatColor.WHITE + "World: " + ChatColor.YELLOW + home.getWorld().getName());
            homeLore.add("");
            homeLore.add(ChatColor.YELLOW + "Click to teleport to clan home");
            
            if (playerMember.getRole().getRoleLevel() >= ClanRole.OFFICER.getRoleLevel()) {
                homeLore.add(ChatColor.GRAY + "Use /clan sethome to change location");
            }
        } else {
            homeLore.add(ChatColor.RED + "Your clan doesn't have a home yet");
            
            if (playerMember.getRole().getRoleLevel() >= ClanRole.OFFICER.getRoleLevel()) {
                homeLore.add("");
                homeLore.add(ChatColor.YELLOW + "Use /clan sethome to set a home");
            }
        }
        
        ItemStack homeItem = ItemUtils.createGuiItem(
            Material.RED_BED, 
            ChatColor.GREEN + "Clan Home", 
            homeLore
        );
        gui.setItem(22, homeItem);
        
        // Relations management - slot 24
        List<String> relationLore = new ArrayList<>();
        relationLore.add("");
        relationLore.add(ChatColor.BLUE + "Alliances: " + ChatColor.WHITE + playerClan.getAlliances().size());
        relationLore.add(ChatColor.RED + "Enemies: " + ChatColor.WHITE + playerClan.getEnemies().size());
        relationLore.add("");
        
        if (playerMember.getRole().getRoleLevel() >= ClanRole.OFFICER.getRoleLevel()) {
            relationLore.add(ChatColor.YELLOW + "Click to manage clan relations");
        } else {
            relationLore.add(ChatColor.YELLOW + "Click to view clan relations");
            relationLore.add(ChatColor.GRAY + "Only officers/leaders can change relations");
        }
        
        ItemStack relationsItem = ItemUtils.createGuiItem(
            Material.SHIELD, 
            ChatColor.GOLD + "Clan Relations", 
            relationLore
        );
        gui.setItem(24, relationsItem);
        
        // ------------- FOURTH ROW: VARIOUS CLAN FEATURES -------------
        
        // Wars section - slot 29
        List<String> warLore = new ArrayList<>();
        warLore.add("");
        
        // Get active war count if any
        int activeWars = 0;
        if (plugin.getWarManager() != null) {
            ClanWar clanWar = plugin.getWarManager().getWarForClan(playerClan.getName());
            activeWars = clanWar != null ? 1 : 0;
        }
        
        warLore.add(ChatColor.WHITE + "Active Wars: " + (activeWars > 0 ? ChatColor.RED : ChatColor.GREEN) + activeWars);
        
        if (activeWars > 0) {
            warLore.add(ChatColor.RED + "Currently at war!");
        } else {
            warLore.add(ChatColor.GREEN + "Currently at peace");
        }
        
        warLore.add("");
        warLore.add(ChatColor.YELLOW + "Click to manage clan wars");
        
        ItemStack warItem = ItemUtils.createGuiItem(
            Material.IRON_SWORD, 
            ChatColor.DARK_RED + "Clan Wars", 
            warLore
        );
        gui.setItem(29, warItem);
        
        // Skills system - slot 31
        if (plugin.getConfig().getBoolean("skills_enabled", true)) {
            List<String> skillsLore = new ArrayList<>();
            skillsLore.add("");
            
            int skillPoints = plugin.getSkillManager().getAvailableSkillPoints(playerClan);
            skillsLore.add(ChatColor.WHITE + "Available Skill Points: " + ChatColor.GOLD + skillPoints);
            
            SkillTree clanSkillTree = plugin.getSkillManager().getClanSkillTree(playerClan);
            if (clanSkillTree != null) {
                int unlockedCount = clanSkillTree.getUnlockedSkills().size();
                int totalCount = plugin.getSkillManager().getAllSkills().size();
                skillsLore.add(ChatColor.WHITE + "Unlocked Skills: " + ChatColor.GREEN + unlockedCount + "/" + totalCount);
                
                // List a few skills if any are unlocked
                if (unlockedCount > 0) {
                    skillsLore.add("");
                    skillsLore.add(ChatColor.WHITE + "Active Skills:");
                    
                    int count = 0;
                    for (ClanSkill skill : clanSkillTree.getUnlockedSkills()) {
                        if (count < 3) { // Show max 3 skills
                            skillsLore.add(ChatColor.YELLOW + "- " + skill.getName());
                            count++;
                        } else {
                            int remaining = unlockedCount - 3;
                            if (remaining > 0) {
                                skillsLore.add(ChatColor.GRAY + "... and " + remaining + " more");
                            }
                            break;
                        }
                    }
                }
            }
            
            skillsLore.add("");
            skillsLore.add(ChatColor.YELLOW + "Click to view and manage skills");
            
            ItemStack skillsItem = ItemUtils.createGuiItem(
                Material.ENCHANTED_BOOK, 
                ChatColor.DARK_AQUA + "Clan Skills", 
                skillsLore
            );
            gui.setItem(31, skillsItem);
        } else {
            // If skills are disabled, put clan progress item here instead
            List<String> progLore = new ArrayList<>();
            progLore.add("");
            progLore.add(ChatColor.WHITE + "Current Level: " + ChatColor.GOLD + playerClan.getLevel());
            progLore.add(ChatColor.WHITE + "Experience: " + ChatColor.YELLOW + playerClan.getExperience() + " points");
            
            if (nextLevelXP > 0) {
                progLore.add("");
                progLore.add(ChatColor.WHITE + "Next Level: " + ChatColor.YELLOW + nextLevelXP + " XP needed");
            } else {
                progLore.add("");
                progLore.add(ChatColor.GREEN + "Maximum level reached!");
            }
            
            progLore.add("");
            progLore.add(ChatColor.YELLOW + "Click to view progression details");
            
            ItemStack progressionItem = ItemUtils.createGuiItem(
                Material.EXPERIENCE_BOTTLE, 
                ChatColor.GREEN + "Clan Progression", 
                progLore
            );
            gui.setItem(31, progressionItem);
        }
        
        // Armor and appearance - slot 33
        List<String> appearanceLore = new ArrayList<>();
        appearanceLore.add("");
        // Get clan color name and convert to ChatColor
        String colorName = playerClan.getColor();
        ChatColor clanColor;
        
        try {
            // Try to parse it directly if it's a valid ChatColor name
            clanColor = ChatColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // If it's a color code, map it manually
            if (colorName.startsWith("§")) {
                switch (colorName) {
                    case "§0": clanColor = ChatColor.BLACK; break;
                    case "§1": clanColor = ChatColor.DARK_BLUE; break;
                    case "§2": clanColor = ChatColor.DARK_GREEN; break;
                    case "§3": clanColor = ChatColor.DARK_AQUA; break;
                    case "§4": clanColor = ChatColor.DARK_RED; break;
                    case "§5": clanColor = ChatColor.DARK_PURPLE; break;
                    case "§6": clanColor = ChatColor.GOLD; break;
                    case "§7": clanColor = ChatColor.GRAY; break;
                    case "§8": clanColor = ChatColor.DARK_GRAY; break;
                    case "§9": clanColor = ChatColor.BLUE; break;
                    case "§a": clanColor = ChatColor.GREEN; break;
                    case "§b": clanColor = ChatColor.AQUA; break;
                    case "§c": clanColor = ChatColor.RED; break;
                    case "§d": clanColor = ChatColor.LIGHT_PURPLE; break;
                    case "§e": clanColor = ChatColor.YELLOW; break;
                    case "§f": clanColor = ChatColor.WHITE; break;
                    default: clanColor = ChatColor.GOLD; break;
                }
            } else {
                // Default to GOLD if there's an error
                plugin.getLogger().warning("Invalid clan color: " + colorName);
                clanColor = ChatColor.GOLD;
            }
        }
        appearanceLore.add(ChatColor.WHITE + "Clan Color: " + clanColor + "■■■");
        
        appearanceLore.add("");
        appearanceLore.add(ChatColor.YELLOW + "Click to customize clan appearance");
        appearanceLore.add(ChatColor.GRAY + "• Change clan colors");
        appearanceLore.add(ChatColor.GRAY + "• Manage armor appearance");
        appearanceLore.add(ChatColor.GRAY + "• Configure nametags");
        
        ItemStack appearanceItem = ItemUtils.createGuiItem(
            Material.LEATHER_CHESTPLATE, 
            ChatColor.LIGHT_PURPLE + "Clan Appearance", 
            appearanceLore
        );
        gui.setItem(33, appearanceItem);
        
        // Bottom row: Settings and Help buttons (already have border glass)
        
        // Settings - slot 47 (bottom row, 2nd position)
        List<String> settingsLore = new ArrayList<>();
        settingsLore.add("");
        settingsLore.add(ChatColor.GRAY + "Configure various clan settings:");
        settingsLore.add(ChatColor.GRAY + "• Chat preferences");
        settingsLore.add(ChatColor.GRAY + "• Privacy settings");
        settingsLore.add(ChatColor.GRAY + "• Notification options");
        settingsLore.add("");
        
        if (playerMember.getRole().getRoleLevel() >= ClanRole.OFFICER.getRoleLevel()) {
            settingsLore.add(ChatColor.YELLOW + "Click to manage clan settings");
        } else {
            settingsLore.add(ChatColor.YELLOW + "Click to view clan settings");
            settingsLore.add(ChatColor.GRAY + "Only officers/leaders can change settings");
        }
        
        // Use Material.valueOf to handle different versions of Minecraft
        Material comparatorMaterial;
        try {
            comparatorMaterial = Material.valueOf("COMPARATOR");
        } catch (IllegalArgumentException e) {
            try {
                comparatorMaterial = Material.valueOf("REDSTONE_COMPARATOR");
            } catch (IllegalArgumentException ex) {
                // Fallback to a common material if neither exists
                comparatorMaterial = Material.REDSTONE;
            }
        }
        
        ItemStack settingsItem = ItemUtils.createGuiItem(
            comparatorMaterial, 
            ChatColor.GOLD + "Clan Settings", 
            settingsLore
        );
        gui.setItem(47, settingsItem);
        
        // Help button - slot 51 (bottom row, middle right)
        List<String> helpLore = new ArrayList<>();
        helpLore.add("");
        helpLore.add(ChatColor.GRAY + "• View all clan commands");
        helpLore.add(ChatColor.GRAY + "• Get usage tips and information");
        helpLore.add(ChatColor.GRAY + "• Learn about clan features");
        helpLore.add("");
        helpLore.add(ChatColor.YELLOW + "Click to see help information");
        
        ItemStack helpItem = ItemUtils.createGuiItem(
            Material.WRITABLE_BOOK, 
            ChatColor.AQUA + "Clan Help", 
            helpLore
        );
        gui.setItem(51, helpItem);
        
        player.openInventory(gui);
        return true;
    }
    
    /**
     * Handles the level command for managing clan progression.
     * 
     * @param player The player executing the command
     * @param args The command arguments
     * @return true if the command was handled, false otherwise
     */
    private boolean handleLevelCommand(Player player, String[] args) {
        // Check if player has admin permission to manage clan levels
        if (!player.hasPermission("clan.admin.level")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to manage clan levels.");
            return true;
        }
        
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /clan level <add|set|view> <clan name> [amount]");
            return true;
        }
        
        String action = args[1].toLowerCase();
        String clanName = args[2];
        
        // Get the target clan
        Clan clan = plugin.getStorageManager().getClan(clanName);
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Clan not found: " + clanName);
            return true;
        }
        
        switch (action) {
            case "view":
                // Show current level and XP
                player.sendMessage(ChatColor.GOLD + "=== Clan Level Info: " + clan.getName() + " ===");
                player.sendMessage(ChatColor.YELLOW + "Current Level: " + ChatColor.WHITE + clan.getLevel());
                player.sendMessage(ChatColor.YELLOW + "Current XP: " + ChatColor.WHITE + clan.getExperience());
                
                // Show benefits
                player.sendMessage(ChatColor.GOLD + "Level Benefits:");
                Map<String, Integer> benefits = plugin.getProgressionManager().getAllClanBenefits(clan);
                for (Map.Entry<String, Integer> benefit : benefits.entrySet()) {
                    String formattedName = plugin.getProgressionManager().formatBenefitName(benefit.getKey());
                    player.sendMessage(ChatColor.GRAY + "- " + formattedName + ": " + 
                                      ChatColor.WHITE + benefit.getValue());
                }
                break;
                
            case "add":
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Usage: /clan level add <clan name> <amount>");
                    return true;
                }
                
                try {
                    int amount = Integer.parseInt(args[3]);
                    boolean leveledUp = plugin.getProgressionManager().addExperience(clan, amount);
                    
                    player.sendMessage(ChatColor.GREEN + "Added " + amount + " XP to clan " + 
                                      clan.getName() + ". New total: " + clan.getExperience());
                    
                    if (leveledUp) {
                        player.sendMessage(ChatColor.GOLD + "The clan has leveled up to level " + clan.getLevel() + "!");
                        
                        // Notify clan members
                        MessageUtils.notifyClan(clan, ChatColor.GOLD + "Your clan has reached level " + 
                                               clan.getLevel() + "!");
                    }
                    
                    // Save the clan
                    plugin.getStorageManager().saveClan(clan);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid XP amount: " + args[3]);
                }
                break;
                
            case "set":
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Usage: /clan level set <clan name> <level>");
                    return true;
                }
                
                try {
                    int level = Integer.parseInt(args[3]);
                    
                    // Validate level
                    if (level < 1 || level > plugin.getProgressionManager().getMaxLevel()) {
                        player.sendMessage(ChatColor.RED + "Invalid level. Must be between 1 and " + 
                                          plugin.getProgressionManager().getMaxLevel());
                        return true;
                    }
                    
                    // Set the clan level
                    int oldLevel = clan.getLevel();
                    clan.setLevel(level);
                    
                    // Set appropriate experience for the level
                    if (level > 1) {
                        int expRequired = plugin.getProgressionManager().getRequiredExperienceForLevel(level);
                        clan.setExperience(expRequired);
                    } else {
                        clan.setExperience(0);
                    }
                    
                    player.sendMessage(ChatColor.GREEN + "Set clan " + clan.getName() + " to level " + level);
                    
                    // Notify clan members if level changed
                    if (oldLevel != level) {
                        MessageUtils.notifyClan(clan, ChatColor.GOLD + "Your clan is now level " + level + "!");
                    }
                    
                    // Save the clan
                    plugin.getStorageManager().saveClan(clan);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid level: " + args[3]);
                }
                break;
                
            default:
                player.sendMessage(ChatColor.RED + "Unknown action: " + action);
                player.sendMessage(ChatColor.YELLOW + "Valid actions: view, add, set");
                break;
        }
        
        return true;
    }
    
    /**
     * Handles the skills command to view and manage clan member skills.
     * 
     * @param player The player executing the command
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleSkills(Player player, String[] args) {
        // Check if player has permission
        if (!player.hasPermission("clan.skills")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        // Check if player is in a clan
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return true;
        }
        
        // Get member skills
        MemberSkills skills = plugin.getSkillManager().getMemberSkills(player.getUniqueId());
        // The getMemberSkills method will create skills if they don't exist
        
        // Display skill information
        player.sendMessage(ChatColor.GOLD + "=== Your Clan Skills ===");
        player.sendMessage(ChatColor.YELLOW + "Skill Points: " + ChatColor.WHITE + skills.getSkillPoints());
        
        // Display specialization
        SkillTree specialization = skills.getSpecialization();
        if (specialization != null) {
            player.sendMessage(ChatColor.YELLOW + "Specialization: " + ChatColor.WHITE + specialization.toString());
        } else {
            player.sendMessage(ChatColor.YELLOW + "Specialization: " + ChatColor.RED + "None");
        }
        
        // Display learned skills
        player.sendMessage(ChatColor.YELLOW + "Learned Skills: " + ChatColor.WHITE + skills.getLearnedSkillCount());
        player.sendMessage(ChatColor.YELLOW + "Total Skill Levels: " + ChatColor.WHITE + skills.getTotalSkillLevels());
        
        player.sendMessage(ChatColor.GREEN + "Use " + ChatColor.YELLOW + "/clan gui" + 
                          ChatColor.GREEN + " to manage your skills and specializations.");
        
        return true;
    }
}

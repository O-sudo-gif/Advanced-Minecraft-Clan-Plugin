package com.minecraft.clanplugin.commands;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.ClanRole;
import com.minecraft.clanplugin.models.Territory;
import com.minecraft.clanplugin.storage.TerritoryManager;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Command handler for territory-related commands.
 */
public class ClanTerritoryCommand implements CommandExecutor {
    
    private final ClanPlugin plugin;
    
    public ClanTerritoryCommand(ClanPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use territory commands!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "map":
                return handleMap(player);
            case "dynamicmap":
                return handleDynamicMap(player);
            case "info":
                return handleInfo(player);
            case "claim":
                return handleClaim(player);
            case "unclaim":
                return handleUnclaim(player);
            case "list":
                return handleList(player);
            case "admin":
                return handleAdmin(player, args);
            default:
                sendHelpMessage(player);
                return true;
        }
    }
    
    /**
     * Handles the territory map command.
     * 
     * @param player The player
     * @return True if the command was handled
     */
    private boolean handleMap(Player player) {
        // Use the advanced territory map renderer for better display
        try {
            // Use the text-based map from TerritoryMap class
            plugin.getTerritoryMap().displayTextMap(player, 6);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Error displaying territory map: " + e.getMessage());
            
            // Fallback to basic map
            player.sendMessage(ChatColor.RED + "Error showing detailed map. Using basic map instead.");
            return showBasicMap(player);
        }
    }
    
    /**
     * Shows a basic text map as fallback
     * 
     * @param player The player to show the map to
     * @return True if handled successfully
     */
    private boolean showBasicMap(Player player) {
        // Get surrounding chunks
        Chunk centerChunk = player.getLocation().getChunk();
        int centerX = centerChunk.getX();
        int centerZ = centerChunk.getZ();
        
        // Create a 9x9 map (4 chunks in each direction)
        StringBuilder mapBuilder = new StringBuilder();
        mapBuilder.append(ChatColor.GOLD).append("=== Territory Map ===\n");
        
        TerritoryManager territoryManager = plugin.getStorageManager().getTerritoryManager();
        
        // Get player's clan
        String playerClanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
        
        for (int z = centerZ - 4; z <= centerZ + 4; z++) {
            for (int x = centerX - 4; x <= centerX + 4; x++) {
                // Get the chunk
                Chunk chunk = player.getWorld().getChunkAt(x, z);
                Territory territory = territoryManager.getTerritory(chunk);
                
                if (x == centerX && z == centerZ) {
                    // Player's current position
                    mapBuilder.append(ChatColor.WHITE).append("X");
                } else if (territory == null) {
                    // Unclaimed
                    mapBuilder.append(ChatColor.GRAY).append("-");
                } else {
                    // Claimed territory
                    if (playerClanName != null && playerClanName.equals(territory.getClanName())) {
                        // Player's clan
                        mapBuilder.append(ChatColor.GREEN).append("O");
                    } else {
                        // Get relationship with this clan
                        if (playerClanName != null) {
                            Clan playerClan = plugin.getStorageManager().getClanStorage().getClan(playerClanName);
                            if (playerClan.isAlly(territory.getClanName())) {
                                mapBuilder.append(ChatColor.BLUE).append("A");
                            } else if (playerClan.isEnemy(territory.getClanName())) {
                                mapBuilder.append(ChatColor.RED).append("E");
                            } else {
                                mapBuilder.append(ChatColor.YELLOW).append("C");
                            }
                        } else {
                            mapBuilder.append(ChatColor.YELLOW).append("C");
                        }
                    }
                }
            }
            mapBuilder.append("\n");
        }
        
        // Add legend
        mapBuilder.append(ChatColor.WHITE).append("X").append(ChatColor.YELLOW).append(" - Your position, ");
        mapBuilder.append(ChatColor.GREEN).append("O").append(ChatColor.YELLOW).append(" - Your clan, ");
        mapBuilder.append(ChatColor.BLUE).append("A").append(ChatColor.YELLOW).append(" - Ally, ");
        mapBuilder.append(ChatColor.RED).append("E").append(ChatColor.YELLOW).append(" - Enemy, ");
        mapBuilder.append(ChatColor.YELLOW).append("C").append(ChatColor.YELLOW).append(" - Other clan, ");
        mapBuilder.append(ChatColor.GRAY).append("-").append(ChatColor.YELLOW).append(" - Unclaimed");
        
        player.sendMessage(mapBuilder.toString());
        return true;
    }
    
    /**
     * Handles the dynamic territory map command.
     * This gives the player a real-time updating map item.
     * 
     * @param player The player
     * @return True if the command was handled
     */
    private boolean handleDynamicMap(Player player) {
        // Check if player has permission to use dynamic maps
        if (!player.hasPermission("clan.territory.dynamicmap")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use dynamic territory maps!");
            return true;
        }
        
        // Attempt to give the player a dynamic map
        if (plugin.getTerritoryMap().giveDynamicMap(player)) {
            return true;
        } else {
            // If failed, suggest using the regular map command
            player.sendMessage(ChatColor.RED + "Could not create dynamic map. Try using /clan territory map instead.");
            return true;
        }
    }
    
    /**
     * Handles the territory info command.
     * 
     * @param player The player
     * @return True if the command was handled
     */
    private boolean handleInfo(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        TerritoryManager territoryManager = plugin.getStorageManager().getTerritoryManager();
        Territory territory = territoryManager.getTerritory(chunk);
        
        if (territory == null) {
            player.sendMessage(ChatColor.YELLOW + "This chunk is unclaimed.");
            return true;
        }
        
        String clanName = territory.getClanName();
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Error: Territory claimed by unknown clan.");
            return true;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Territory Info ===");
        player.sendMessage(ChatColor.YELLOW + "Clan: " + ChatColor.WHITE + clanName);
        player.sendMessage(ChatColor.YELLOW + "Protection: " + ChatColor.WHITE + territory.getProtectionLevel() + 
                          " (" + territory.getInfluenceLevel() + "% influence)");
        player.sendMessage(ChatColor.YELLOW + "Flags: " + ChatColor.WHITE + territory.getFlags().size());
        player.sendMessage(ChatColor.YELLOW + "Chunk: " + ChatColor.WHITE + 
                          chunk.getX() + ", " + chunk.getZ() + " in " + chunk.getWorld().getName());
        
        // Show relationship if player is in a clan
        String playerClanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
        if (playerClanName != null && !playerClanName.equals(clanName)) {
            Clan playerClan = plugin.getStorageManager().getClanStorage().getClan(playerClanName);
            if (playerClan.isAlly(clanName)) {
                player.sendMessage(ChatColor.YELLOW + "Relationship: " + ChatColor.BLUE + "Ally");
            } else if (playerClan.isEnemy(clanName)) {
                player.sendMessage(ChatColor.YELLOW + "Relationship: " + ChatColor.RED + "Enemy");
            } else {
                player.sendMessage(ChatColor.YELLOW + "Relationship: " + ChatColor.GRAY + "Neutral");
            }
        }
        
        return true;
    }
    
    /**
     * Handles the territory claim command.
     * 
     * @param player The player
     * @return True if the command was handled
     */
    private boolean handleClaim(Player player) {
        // Check if the player is in a clan
        String clanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
        if (clanName == null) {
            player.sendMessage(ChatColor.RED + "You must be in a clan to claim territory!");
            return true;
        }
        
        // Check if the player has permission to claim territory
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        ClanMember member = clan.getMember(player.getUniqueId());
        
        if (member == null || member.getRole().getRoleLevel() < ClanRole.OFFICER.getRoleLevel()) {
            player.sendMessage(ChatColor.RED + "You must be an officer or leader to claim territory!");
            return true;
        }
        
        // Check if the chunk is already claimed
        Chunk chunk = player.getLocation().getChunk();
        TerritoryManager territoryManager = plugin.getStorageManager().getTerritoryManager();
        Territory territory = territoryManager.getTerritory(chunk);
        
        if (territory != null) {
            player.sendMessage(ChatColor.RED + "This chunk is already claimed by clan " + territory.getClanName() + "!");
            return true;
        }
        
        // Try to claim the territory
        if (territoryManager.claimTerritory(chunk, clanName, player)) {
            player.sendMessage(ChatColor.GREEN + "Territory claimed for clan " + 
                              ChatColor.GOLD + clanName + ChatColor.GREEN + "!");
            
            // Broadcast to clan members
            for (UUID memberId : clan.getMemberIds()) {
                Player clanMember = plugin.getServer().getPlayer(memberId);
                if (clanMember != null && !clanMember.equals(player)) {
                    clanMember.sendMessage(ChatColor.GREEN + player.getName() + " has claimed new territory for your clan!");
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Could not claim this territory. Make sure it's adjacent to existing territory.");
        }
        
        return true;
    }
    
    /**
     * Handles the territory unclaim command.
     * 
     * @param player The player
     * @return True if the command was handled
     */
    private boolean handleUnclaim(Player player) {
        // Check if the player is in a clan
        String clanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
        if (clanName == null) {
            player.sendMessage(ChatColor.RED + "You must be in a clan to unclaim territory!");
            return true;
        }
        
        // Check if the player has permission to unclaim territory
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        ClanMember member = clan.getMember(player.getUniqueId());
        
        if (member == null || member.getRole().getRoleLevel() < ClanRole.OFFICER.getRoleLevel()) {
            player.sendMessage(ChatColor.RED + "You must be an officer or leader to unclaim territory!");
            return true;
        }
        
        // Check if the chunk is claimed by player's clan
        Chunk chunk = player.getLocation().getChunk();
        TerritoryManager territoryManager = plugin.getStorageManager().getTerritoryManager();
        Territory territory = territoryManager.getTerritory(chunk);
        
        if (territory == null) {
            player.sendMessage(ChatColor.RED + "This chunk is not claimed!");
            return true;
        }
        
        if (!territory.getClanName().equals(clanName)) {
            player.sendMessage(ChatColor.RED + "This chunk is claimed by clan " + territory.getClanName() + ", not your clan!");
            return true;
        }
        
        // Try to unclaim the territory
        if (territoryManager.unclaimTerritory(chunk, clanName)) {
            player.sendMessage(ChatColor.GREEN + "Territory unclaimed!");
            
            // Broadcast to clan members
            for (UUID memberId : clan.getMemberIds()) {
                Player clanMember = plugin.getServer().getPlayer(memberId);
                if (clanMember != null && !clanMember.equals(player)) {
                    clanMember.sendMessage(ChatColor.YELLOW + player.getName() + " has unclaimed territory from your clan.");
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + "Could not unclaim this territory. It may disconnect other territories.");
        }
        
        return true;
    }
    
    /**
     * Handles the territory list command.
     * 
     * @param player The player
     * @return True if the command was handled
     */
    private boolean handleList(Player player) {
        // Check if the player is in a clan
        String clanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
        if (clanName == null) {
            player.sendMessage(ChatColor.RED + "You must be in a clan to view territory list!");
            return true;
        }
        
        TerritoryManager territoryManager = plugin.getStorageManager().getTerritoryManager();
        List<Territory> territories = territoryManager.getClanTerritories(clanName);
        
        if (territories.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Your clan does not have any claimed territories.");
            return true;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Clan Territories (" + territories.size() + ") ===");
        
        for (int i = 0; i < Math.min(10, territories.size()); i++) {
            Territory territory = territories.get(i);
            player.sendMessage(ChatColor.YELLOW + String.valueOf(i + 1) + ". " + ChatColor.WHITE + 
                              "Chunk: " + territory.getChunkX() + ", " + territory.getChunkZ() + 
                              " (" + territory.getProtectionLevel() + ")");
        }
        
        if (territories.size() > 10) {
            player.sendMessage(ChatColor.GRAY + "... and " + (territories.size() - 10) + " more territories.");
        }
        
        // Calculate max claims
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        int maxClaims = territoryManager.calculateMaxClaims(clan);
        
        player.sendMessage(ChatColor.YELLOW + "Total claims: " + ChatColor.WHITE + 
                          territories.size() + "/" + maxClaims);
        
        return true;
    }
    
    /**
     * Handles the territory admin commands.
     * 
     * @param player The player
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleAdmin(Player player, String[] args) {
        // Check if the player has admin permission
        if (!player.hasPermission("clan.admin.territory")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use territory admin commands!");
            return true;
        }
        
        if (args.length < 2) {
            sendAdminHelpMessage(player);
            return true;
        }
        
        String adminSubCommand = args[1].toLowerCase();
        
        switch (adminSubCommand) {
            case "clear":
                return handleAdminClear(player, args);
            case "set":
                return handleAdminSet(player, args);
            case "bypass":
                return handleAdminBypass(player);
            default:
                sendAdminHelpMessage(player);
                return true;
        }
    }
    
    /**
     * Handles the admin clear command.
     * 
     * @param player The player
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleAdminClear(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /clan territory admin clear <clan>");
            return true;
        }
        
        String clanName = args[2];
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Clan " + clanName + " does not exist!");
            return true;
        }
        
        TerritoryManager territoryManager = plugin.getStorageManager().getTerritoryManager();
        territoryManager.clearClanTerritories(clanName);
        
        player.sendMessage(ChatColor.GREEN + "Cleared all territories for clan " + clanName + ".");
        
        return true;
    }
    
    /**
     * Handles the admin set command.
     * 
     * @param player The player
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleAdminSet(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /clan territory admin set <clan>");
            return true;
        }
        
        String clanName = args[2];
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Clan " + clanName + " does not exist!");
            return true;
        }
        
        Chunk chunk = player.getLocation().getChunk();
        TerritoryManager territoryManager = plugin.getStorageManager().getTerritoryManager();
        
        // First unclaim if claimed
        Territory existing = territoryManager.getTerritory(chunk);
        if (existing != null) {
            territoryManager.unclaimTerritory(chunk, existing.getClanName());
        }
        
        // Claim for the specified clan
        if (territoryManager.claimTerritory(chunk, clanName, player)) {
            player.sendMessage(ChatColor.GREEN + "Set territory ownership to clan " + clanName + ".");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to set territory ownership.");
        }
        
        return true;
    }
    
    /**
     * Handles the admin bypass command.
     * 
     * @param player The player
     * @return True if the command was handled
     */
    private boolean handleAdminBypass(Player player) {
        // Toggle the bypass permission
        if (player.hasPermission("clan.admin.territory.bypass")) {
            // Remove permission
            player.sendMessage(ChatColor.YELLOW + "Territory bypass mode " + ChatColor.RED + "disabled" + 
                              ChatColor.YELLOW + ".");
            
            // This would be implemented with a permission plugin in a real server
            player.sendMessage(ChatColor.GRAY + "(This would remove the bypass permission in a real server)");
        } else {
            // Add permission
            player.sendMessage(ChatColor.YELLOW + "Territory bypass mode " + ChatColor.GREEN + "enabled" + 
                              ChatColor.YELLOW + ".");
            
            // This would be implemented with a permission plugin in a real server
            player.sendMessage(ChatColor.GRAY + "(This would add the bypass permission in a real server)");
        }
        
        return true;
    }
    
    /**
     * Sends the help message for territory commands.
     * 
     * @param player The player
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Territory Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/clan territory map" + ChatColor.WHITE + " - Shows a map of nearby territories");
        player.sendMessage(ChatColor.YELLOW + "/clan territory info" + ChatColor.WHITE + " - Shows information about the current chunk");
        player.sendMessage(ChatColor.YELLOW + "/clan territory claim" + ChatColor.WHITE + " - Claims the current chunk for your clan");
        player.sendMessage(ChatColor.YELLOW + "/clan territory unclaim" + ChatColor.WHITE + " - Unclaims the current chunk");
        player.sendMessage(ChatColor.YELLOW + "/clan territory list" + ChatColor.WHITE + " - Lists all territory chunks owned by your clan");
        
        if (player.hasPermission("clan.admin.territory")) {
            player.sendMessage(ChatColor.GOLD + "=== Admin Commands ===");
            player.sendMessage(ChatColor.YELLOW + "/clan territory admin clear <clan>" + ChatColor.WHITE + " - Clears all territory for a clan");
            player.sendMessage(ChatColor.YELLOW + "/clan territory admin set <clan>" + ChatColor.WHITE + " - Sets the current chunk ownership");
            player.sendMessage(ChatColor.YELLOW + "/clan territory admin bypass" + ChatColor.WHITE + " - Toggles admin bypass mode");
        }
    }
    
    /**
     * Sends the help message for territory admin commands.
     * 
     * @param player The player
     */
    private void sendAdminHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Territory Admin Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/clan territory admin clear <clan>" + ChatColor.WHITE + " - Clears all territory for a clan");
        player.sendMessage(ChatColor.YELLOW + "/clan territory admin set <clan>" + ChatColor.WHITE + " - Sets the current chunk ownership");
        player.sendMessage(ChatColor.YELLOW + "/clan territory admin bypass" + ChatColor.WHITE + " - Toggles admin bypass mode");
    }
}
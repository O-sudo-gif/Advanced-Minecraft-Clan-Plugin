package com.minecraft.clanplugin.listeners;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.Territory;
import com.minecraft.clanplugin.storage.TerritoryManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for territory-related events.
 */
public class TerritoryListener implements Listener {
    
    private final ClanPlugin plugin;
    private final Map<UUID, String> lastTerritoryMessages;
    
    public TerritoryListener(ClanPlugin plugin) {
        this.plugin = plugin;
        this.lastTerritoryMessages = new HashMap<>();
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Check if the block is in a protected territory
        if (!canBuild(player, block.getLocation())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot break blocks in this territory!");
        }
        
        // Check if the block is a flag (using Material name comparison since Material.BANNER might not exist in all versions)
        if (block.getType().name().contains("BANNER")) {
            TerritoryManager territoryManager = plugin.getStorageManager().getTerritoryManager();
            Territory territory = territoryManager.getTerritory(block.getChunk());
            
            if (territory != null) {
                // Check if player is in the territory clan or has admin permission
                String playerClanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
                boolean isAdmin = player.hasPermission("clan.admin.territory.bypass");
                
                if (!isAdmin && (playerClanName == null || !playerClanName.equals(territory.getClanName()))) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You cannot remove flags from another clan's territory!");
                    return;
                }
                
                // Remove the flag
                if (territoryManager.removeFlag(block.getLocation(), territory.getClanName())) {
                    player.sendMessage(ChatColor.YELLOW + "Flag removed from territory!");
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Check if the block is in a protected territory
        if (!canBuild(player, block.getLocation())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot place blocks in this territory!");
            return;
        }
        
        // Check if the block is a banner (potential flag)
        if (block.getType().name().contains("BANNER")) {
            String playerClanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
            
            if (playerClanName == null) {
                return; // Not in a clan, just a regular banner
            }
            
            TerritoryManager territoryManager = plugin.getStorageManager().getTerritoryManager();
            Territory territory = territoryManager.getTerritory(block.getChunk());
            
            if (territory == null) {
                // Unclaimed chunk, try to claim it with a flag
                Clan playerClan = plugin.getStorageManager().getClanStorage().getClan(playerClanName);
                
                if (territoryManager.claimTerritory(block.getChunk(), playerClanName, player)) {
                    player.sendMessage(ChatColor.GREEN + "Territory claimed for clan " + 
                                      ChatColor.GOLD + playerClanName + ChatColor.GREEN + "!");
                    
                    // Broadcast to clan members
                    for (UUID memberId : playerClan.getMemberIds()) {
                        Player member = plugin.getServer().getPlayer(memberId);
                        if (member != null && !member.equals(player)) {
                            member.sendMessage(ChatColor.GREEN + player.getName() + " has claimed new territory for your clan!");
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Could not claim this territory. Make sure it's adjacent to existing territory.");
                }
            } else if (territory.getClanName().equals(playerClanName)) {
                // Already claimed by player's clan, add flag to strengthen control
                com.minecraft.clanplugin.models.Flag flag = new com.minecraft.clanplugin.models.Flag(
                    block.getLocation(), player.getUniqueId());
                
                if (territoryManager.addFlag(block.getChunk(), flag, playerClanName)) {
                    player.sendMessage(ChatColor.GREEN + "Flag added to strengthen territory control!");
                } else {
                    player.sendMessage(ChatColor.RED + "Could not add flag to territory.");
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        
        // Check if the block is in a protected territory
        if (!canInteract(player, block.getLocation())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot interact with blocks in this territory!");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        
        // Check if PvP is enabled in this territory
        TerritoryManager territoryManager = plugin.getStorageManager().getTerritoryManager();
        if (!territoryManager.isPvpEnabled(victim.getLocation())) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "PvP is disabled in this territory!");
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check when the player moves to a new chunk
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        TerritoryManager territoryManager = plugin.getStorageManager().getTerritoryManager();
        
        Territory fromTerritory = territoryManager.getTerritory(event.getFrom().getChunk());
        Territory toTerritory = territoryManager.getTerritory(event.getTo().getChunk());
        
        // Handle leaving a territory
        if (fromTerritory != null && (toTerritory == null || !toTerritory.getClanName().equals(fromTerritory.getClanName()))) {
            player.sendMessage(ChatColor.YELLOW + "Leaving territory of clan " + 
                              ChatColor.GOLD + fromTerritory.getClanName());
        }
        
        // Handle entering a territory
        if (toTerritory != null && (fromTerritory == null || !fromTerritory.getClanName().equals(toTerritory.getClanName()))) {
            String message = ChatColor.YELLOW + "Entering territory of clan " + 
                            ChatColor.GOLD + toTerritory.getClanName() + 
                            ChatColor.YELLOW + " (" + toTerritory.getProtectionLevel() + ")";
            
            // Check if this is the same message as last time
            if (!message.equals(lastTerritoryMessages.get(playerId))) {
                player.sendMessage(message);
                lastTerritoryMessages.put(playerId, message);
            }
            
            // Check if entering enemy territory
            String playerClanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(playerId);
            if (playerClanName != null) {
                Clan playerClan = plugin.getStorageManager().getClanStorage().getClan(playerClanName);
                if (playerClan.isEnemy(toTerritory.getClanName())) {
                    player.sendMessage(ChatColor.RED + "Warning: You are entering enemy territory!");
                } else if (playerClan.isAlly(toTerritory.getClanName())) {
                    player.sendMessage(ChatColor.GREEN + "This territory belongs to an ally clan.");
                }
            }
        }
    }
    
    /**
     * Checks if a player can build at a location.
     * 
     * @param player The player
     * @param location The location
     * @return True if the player can build
     */
    private boolean canBuild(Player player, Location location) {
        // Admin bypass
        if (player.hasPermission("clan.admin.territory.bypass")) {
            return true;
        }
        
        TerritoryManager territoryManager = plugin.getStorageManager().getTerritoryManager();
        return territoryManager.canBuild(player, location);
    }
    
    /**
     * Checks if a player can interact with blocks at a location.
     * 
     * @param player The player
     * @param location The location
     * @return True if the player can interact
     */
    private boolean canInteract(Player player, Location location) {
        // Admin bypass
        if (player.hasPermission("clan.admin.territory.bypass")) {
            return true;
        }
        
        TerritoryManager territoryManager = plugin.getStorageManager().getTerritoryManager();
        return territoryManager.canInteract(player, location);
    }
}
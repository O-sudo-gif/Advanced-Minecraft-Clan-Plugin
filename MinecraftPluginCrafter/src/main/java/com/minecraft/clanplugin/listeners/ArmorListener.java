package com.minecraft.clanplugin.listeners;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.utils.ItemUtils;
import com.minecraft.clanplugin.utils.MessageUtils;
import org.bukkit.Color;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/**
 * Listener for armor-related events to handle clan-colored armor.
 */
public class ArmorListener implements Listener {

    private final ClanPlugin plugin;
    
    /**
     * Creates a new armor listener.
     * 
     * @param plugin The clan plugin instance
     */
    public ArmorListener(ClanPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handles player join events to apply clan-colored armor.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Delay the color application to ensure the player is fully loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            colorPlayerArmor(event.getPlayer());
        }, 10L); // 10 ticks (0.5 seconds) delay
    }
    
    /**
     * Handles player respawn events to reapply clan-colored armor.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Delay to ensure inventory is set after respawn
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            colorPlayerArmor(event.getPlayer());
        }, 5L);
    }
    
    /**
     * Preserves colored armor when player dies.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        if (clan != null && clan.hasColoredArmor()) {
            // Optionally prevent armor from dropping
            if (plugin.getConfig().getBoolean("prevent_clan_armor_drops", true)) {
                ItemStack[] armor = player.getInventory().getArmorContents();
                for (int i = 0; i < armor.length; i++) {
                    if (armor[i] != null && isClanColoredArmor(armor[i])) {
                        event.getDrops().remove(armor[i]);
                    }
                }
            }
        }
    }
    
    /**
     * Prevents players from removing clan-colored armor if configured.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        if (clan != null && clan.hasColoredArmor() && 
            plugin.getConfig().getBoolean("prevent_clan_armor_removal", false)) {
            
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && isClanColoredArmor(clickedItem)) {
                // Check if player is trying to remove armor
                if (event.getSlot() >= 36 && event.getSlot() <= 39) { // Armor slots
                    event.setCancelled(true);
                    MessageUtils.sendVisualToggleMessage(player, "armor", true);
                    player.sendMessage(ChatColor.RED + "You cannot remove your clan armor!");
                }
            }
        }
    }
    
    /**
     * Colors a player's armor based on their clan color.
     * 
     * @param player The player to update armor for
     */
    public void colorPlayerArmor(Player player) {
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        if (clan == null) {
            // Player doesn't have a clan
            return;
        }
        
        // Get the clan color - applying colors automatically regardless of hasColoredArmor setting
        // This makes all clan members have colored armor automatically
        Color armorColor = convertChatColorToColor(clan.getChatColor());
        
        // Process each armor piece
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean armorChanged = false;
        
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && isLeatherArmor(armor[i])) {
                // Check if this is a piece we want to color
                boolean shouldColor = false;
                
                // Get the armor coloring configuration
                String colorArmorPieces = plugin.getConfig().getString("clan_color_armor_pieces", "ALL");
                
                switch (i) {
                    case 0: // Boots
                        shouldColor = colorArmorPieces.contains("ALL") || colorArmorPieces.contains("BOOTS");
                        break;
                    case 1: // Leggings
                        shouldColor = colorArmorPieces.contains("ALL") || colorArmorPieces.contains("LEGGINGS");
                        break;
                    case 2: // Chestplate
                        shouldColor = colorArmorPieces.contains("ALL") || colorArmorPieces.contains("CHESTPLATE");
                        break;
                    case 3: // Helmet
                        shouldColor = colorArmorPieces.contains("ALL") || colorArmorPieces.contains("HELMET");
                        break;
                }
                
                if (shouldColor) {
                    LeatherArmorMeta meta = (LeatherArmorMeta) armor[i].getItemMeta();
                    
                    // Only change color if needed
                    if (meta != null && !meta.getColor().equals(armorColor)) {
                        meta.setColor(armorColor);
                        
                        // Add clan identifier lore
                        ItemUtils.addClanIdentifier(meta, clan.getName());
                        
                        armor[i].setItemMeta(meta);
                        armorChanged = true;
                    }
                }
            }
        }
        
        // Update the armor if changes were made
        if (armorChanged) {
            player.getInventory().setArmorContents(armor);
            player.updateInventory();
            
            // Send a message to the player
            MessageUtils.sendArmorColoredMessage(player, clan);
        }
    }
    
    /**
     * Provides default leather armor to a player if needed.
     * 
     * @param player The player to give armor to
     */
    public void provideDefaultArmor(Player player) {
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        if (clan == null || !clan.hasColoredArmor() || 
            !plugin.getConfig().getBoolean("provide_clan_armor", false)) {
            return;
        }
        
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean armorChanged = false;
        
        // Check and provide armor for each slot if missing
        if (armor[0] == null || armor[0].getType() == Material.AIR) {
            armor[0] = new ItemStack(Material.LEATHER_BOOTS);
            armorChanged = true;
        }
        
        if (armor[1] == null || armor[1].getType() == Material.AIR) {
            armor[1] = new ItemStack(Material.LEATHER_LEGGINGS);
            armorChanged = true;
        }
        
        if (armor[2] == null || armor[2].getType() == Material.AIR) {
            armor[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
            armorChanged = true;
        }
        
        if (armor[3] == null || armor[3].getType() == Material.AIR) {
            armor[3] = new ItemStack(Material.LEATHER_HELMET);
            armorChanged = true;
        }
        
        if (armorChanged) {
            player.getInventory().setArmorContents(armor);
            colorPlayerArmor(player); // Apply colors to the new armor
        }
    }
    
    /**
     * Converts a ChatColor to a bukkit Color for armor dyeing.
     * 
     * @param chatColor The ChatColor to convert
     * @return The corresponding bukkit Color
     */
    private Color convertChatColorToColor(ChatColor chatColor) {
        if (chatColor == null) {
            return Color.fromRGB(255, 170, 0); // Default to gold if null
        }
        
        switch (chatColor) {
            case BLACK:
                return Color.fromRGB(0, 0, 0);
            case DARK_BLUE:
                return Color.fromRGB(0, 0, 170);
            case DARK_GREEN:
                return Color.fromRGB(0, 170, 0);
            case DARK_AQUA:
                return Color.fromRGB(0, 170, 170);
            case DARK_RED:
                return Color.fromRGB(170, 0, 0);
            case DARK_PURPLE:
                return Color.fromRGB(170, 0, 170);
            case GOLD:
                return Color.fromRGB(255, 170, 0);
            case GRAY:
                return Color.fromRGB(170, 170, 170);
            case DARK_GRAY:
                return Color.fromRGB(85, 85, 85);
            case BLUE:
                return Color.fromRGB(85, 85, 255);
            case GREEN:
                return Color.fromRGB(85, 255, 85);
            case AQUA:
                return Color.fromRGB(85, 255, 255);
            case RED:
                return Color.fromRGB(255, 85, 85);
            case LIGHT_PURPLE:
                return Color.fromRGB(255, 85, 255);
            case YELLOW:
                return Color.fromRGB(255, 255, 85);
            case WHITE:
            default:
                return Color.fromRGB(255, 255, 255);
        }
    }
    
    /**
     * Checks if an item is leather armor.
     * 
     * @param item The item to check
     * @return True if the item is leather armor
     */
    private boolean isLeatherArmor(ItemStack item) {
        if (item == null) return false;
        
        Material type = item.getType();
        return type == Material.LEATHER_HELMET || 
               type == Material.LEATHER_CHESTPLATE || 
               type == Material.LEATHER_LEGGINGS || 
               type == Material.LEATHER_BOOTS;
    }
    
    /**
     * Checks if an item is clan-colored armor.
     * 
     * @param item The item to check
     * @return True if the item is clan-colored armor
     */
    private boolean isClanColoredArmor(ItemStack item) {
        if (!isLeatherArmor(item)) return false;
        
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            return ItemUtils.hasClanIdentifier(item.getItemMeta());
        }
        
        return false;
    }
}
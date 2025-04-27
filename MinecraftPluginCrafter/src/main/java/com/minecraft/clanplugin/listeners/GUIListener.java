package com.minecraft.clanplugin.listeners;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.ClanRole;
import com.minecraft.clanplugin.skills.ClanSkill;
import com.minecraft.clanplugin.skills.MemberSkills;
import com.minecraft.clanplugin.skills.SkillTree;
import com.minecraft.clanplugin.utils.ItemUtils;
import com.minecraft.clanplugin.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Listener class for clan GUI interaction events.
 */
public class GUIListener implements Listener {
    
    private final ClanPlugin plugin;
    
    public GUIListener(ClanPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String inventoryTitle = event.getView().getTitle();
        
        // Check if the inventory is a clan GUI
        if (inventoryTitle.equals(ChatColor.DARK_PURPLE + "Clan Management")) {
            // Cancel the event to prevent taking items
            event.setCancelled(true);
            
            // Get the clicked item
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }
            
            Clan playerClan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
            if (playerClan == null) {
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "You are not in a clan!");
                return;
            }
            
            // Handle click based on the item
            handleMainMenuClick(player, clickedItem, event.getSlot(), playerClan);
        } else if (inventoryTitle.equals(ChatColor.BLUE + "Clan Alliances")) {
            // Handle alliance management GUI
            event.setCancelled(true);
            handleAllianceClick(player, event.getCurrentItem());
        } else if (inventoryTitle.equals(ChatColor.RED + "Clan Enemies")) {
            // Handle enemy management GUI
            event.setCancelled(true);
            handleEnemyClick(player, event.getCurrentItem());
        } else if (inventoryTitle.equals(ChatColor.AQUA + "Clan Members")) {
            // Handle member management GUI
            event.setCancelled(true);
            handleMemberClick(player, event.getCurrentItem());
        } else if (inventoryTitle.equals(ChatColor.LIGHT_PURPLE + "Clan Color")) {
            // Handle color selection GUI
            event.setCancelled(true);
            handleColorClick(player, event.getCurrentItem());
        } else if (inventoryTitle.equals(ChatColor.GOLD + "Clan Settings")) {
            // Handle settings GUI
            event.setCancelled(true);
            Clan playerClan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
            if (playerClan != null) {
                handleSettingsClick(player, event.getCurrentItem(), playerClan);
            }
        } else if (inventoryTitle.equals(ChatColor.GREEN + "Clan Progression")) {
            // Handle progression GUI
            event.setCancelled(true);
            handleProgressionClick(player, event.getCurrentItem());
        } else if (inventoryTitle.equals(ChatColor.DARK_AQUA + "Clan Skills")) {
            // Handle skills GUI
            event.setCancelled(true);
            handleSkillsClick(player, event.getCurrentItem());
        } else if (inventoryTitle.startsWith(ChatColor.DARK_AQUA + "Skills: ")) {
            // Handle specific skill tree GUI
            event.setCancelled(true);
            handleSkillTreeClick(player, event.getCurrentItem(), event.getView().getTitle());
        }
    }
    
    private void handleMainMenuClick(Player player, ItemStack clickedItem, int slot, Clan clan) {
        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        // Handle different menu items
        if (itemName.equals(ChatColor.AQUA + "Manage Members")) {
            openMembersGUI(player, clan);
        } else if (itemName.equals(ChatColor.GREEN + "Clan Home")) {
            // Teleport player to clan home
            player.closeInventory();
            if (clan.getHome() != null) {
                player.teleport(clan.getHome());
                player.sendMessage(ChatColor.GREEN + "Teleported to clan home!");
            } else {
                player.sendMessage(ChatColor.RED + "Your clan doesn't have a home set yet!");
            }
        } else if (itemName.equals(ChatColor.BLUE + "Alliances")) {
            openAlliancesGUI(player, clan);
        } else if (itemName.equals(ChatColor.RED + "Enemies")) {
            openEnemiesGUI(player, clan);
        } else if (itemName.equals(ChatColor.LIGHT_PURPLE + "Clan Color")) {
            openColorGUI(player, clan);
        } else if (itemName.equals(ChatColor.DARK_GREEN + "Territory")) {
            // Show territory commands help
            player.closeInventory();
            player.performCommand("clan territory");
        } else if (itemName.equals(ChatColor.DARK_RED + "Clan Wars")) {
            // Show war status or war commands
            player.closeInventory();
            player.performCommand("clan war status");
        } else if (itemName.equals(ChatColor.GOLD + "Clan Economy")) {
            // Show economy commands help
            player.closeInventory();
            player.performCommand("clan economy balance");
        } else if (itemName.equals(ChatColor.GREEN + "Clan Progression")) {
            // Open progression GUI
            openProgressionGUI(player, clan);
        } else if (itemName.equals(ChatColor.GOLD + "Clan Settings")) {
            openSettingsGUI(player, clan);
        } else if (itemName.equals(ChatColor.DARK_AQUA + "Clan Skills")) {
            // Open skills GUI
            openSkillsGUI(player, clan);
        }
    }
    
    private void openMembersGUI(Player player, Clan clan) {
        Inventory gui = Bukkit.createInventory(player, 54, ChatColor.AQUA + "Clan Members");
        
        // Add glass pane border
        ItemUtils.createGuiBorder(gui, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        
        // Add member heads
        int slot = 10;
        for (ClanMember member : clan.getMembers()) {
            // Get the player's role in the clan
            String roleName = MessageUtils.getColoredRoleName(member.getRole());
            
            // Create lore with role info
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Role: " + roleName);
            lore.add("");
            
            // Add actions based on player's role
            ClanMember playerMember = clan.getMember(player.getUniqueId());
            if (playerMember.getRole() == ClanRole.LEADER) {
                // Leader can promote/demote/kick
                if (member.getRole() == ClanRole.MEMBER) {
                    lore.add(ChatColor.YELLOW + "Click to promote to Officer");
                } else if (member.getRole() == ClanRole.OFFICER) {
                    lore.add(ChatColor.YELLOW + "Click to demote to Member");
                }
                
                if (member.getRole() != ClanRole.LEADER) {
                    lore.add(ChatColor.RED + "Shift-click to kick from clan");
                }
            } else if (playerMember.getRole() == ClanRole.OFFICER) {
                // Officers can kick regular members
                if (member.getRole() == ClanRole.MEMBER) {
                    lore.add(ChatColor.RED + "Shift-click to kick from clan");
                }
            }
            
            // Create the player head item
            Player memberPlayer = Bukkit.getPlayer(member.getPlayerUUID());
            ItemStack memberItem;
            if (memberPlayer != null && memberPlayer.isOnline()) {
                memberItem = ItemUtils.createPlayerHead(memberPlayer, 
                    ChatColor.GREEN + member.getPlayerName() + ChatColor.GRAY + " (Online)", lore);
            } else {
                // Use default skull for offline players
                memberItem = ItemUtils.createGuiItem(
                    Material.PLAYER_HEAD,
                    ChatColor.YELLOW + member.getPlayerName() + ChatColor.GRAY + " (Offline)", 
                    lore
                );
            }
            
            gui.setItem(slot, memberItem);
            
            // Move to next slot
            slot++;
            if (slot % 9 == 8) {
                slot += 2;
            }
            
            if (slot >= 44) break; // Maximum number of members we can display
        }
        
        // Add back button
        List<String> backLore = new ArrayList<>();
        backLore.add("");
        backLore.add(ChatColor.GRAY + "Return to main menu");
        ItemStack backButton = ItemUtils.createGuiItem(
            Material.ARROW, 
            ChatColor.YELLOW + "Back", 
            backLore
        );
        gui.setItem(49, backButton);
        
        // Open the GUI
        player.openInventory(gui);
    }
    
    private void openAlliancesGUI(Player player, Clan clan) {
        Inventory gui = Bukkit.createInventory(player, 54, ChatColor.BLUE + "Clan Alliances");
        
        // Add glass pane border
        ItemUtils.createGuiBorder(gui, Material.BLUE_STAINED_GLASS_PANE);
        
        // Add clan info
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add(ChatColor.GRAY + "Current Alliances: " + clan.getAlliances().size());
        infoLore.add("");
        if (clan.getMember(player.getUniqueId()).getRole() == ClanRole.LEADER) {
            infoLore.add(ChatColor.YELLOW + "Use /clan ally <name> to add alliances");
            infoLore.add(ChatColor.YELLOW + "Click on an alliance to remove it");
        } else {
            infoLore.add(ChatColor.RED + "Only the clan leader can manage alliances");
        }
        
        ItemStack infoItem = ItemUtils.createInfoItem(
            Material.BOOK, 
            ChatColor.GOLD + "Alliance Information", 
            infoLore
        );
        gui.setItem(4, infoItem);
        
        // Add alliances list
        int slot = 10;
        for (String allyName : clan.getAlliances()) {
            Clan allyClan = plugin.getStorageManager().getClan(allyName);
            if (allyClan == null) continue;
            
            List<String> allyLore = new ArrayList<>();
            allyLore.add("");
            allyLore.add(ChatColor.GRAY + "Members: " + allyClan.getMembers().size());
            
            if (clan.getMember(player.getUniqueId()).getRole() == ClanRole.LEADER) {
                allyLore.add("");
                allyLore.add(ChatColor.RED + "Click to remove alliance");
            }
            
            ItemStack allyItem = ItemUtils.createGuiItem(
                Material.BLUE_BANNER, 
                ChatColor.BLUE + allyClan.getName(), 
                allyLore
            );
            gui.setItem(slot, allyItem);
            
            // Move to next slot
            slot++;
            if (slot % 9 == 8) {
                slot += 2;
            }
            
            if (slot >= 44) break; // Maximum number of alliances we can display
        }
        
        // Add back button
        List<String> backLore = new ArrayList<>();
        backLore.add("");
        backLore.add(ChatColor.GRAY + "Return to main menu");
        ItemStack backButton = ItemUtils.createGuiItem(
            Material.ARROW, 
            ChatColor.YELLOW + "Back", 
            backLore
        );
        gui.setItem(49, backButton);
        
        // Open the GUI
        player.openInventory(gui);
    }
    
    private void openEnemiesGUI(Player player, Clan clan) {
        Inventory gui = Bukkit.createInventory(player, 54, ChatColor.RED + "Clan Enemies");
        
        // Add glass pane border
        ItemUtils.createGuiBorder(gui, Material.RED_STAINED_GLASS_PANE);
        
        // Add clan info
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add(ChatColor.GRAY + "Current Enemies: " + clan.getEnemies().size());
        infoLore.add("");
        if (clan.getMember(player.getUniqueId()).getRole() == ClanRole.LEADER) {
            infoLore.add(ChatColor.YELLOW + "Use /clan enemy <name> to add enemies");
            infoLore.add(ChatColor.YELLOW + "Click on an enemy to remove it");
        } else {
            infoLore.add(ChatColor.RED + "Only the clan leader can manage enemies");
        }
        
        ItemStack infoItem = ItemUtils.createInfoItem(
            Material.BOOK, 
            ChatColor.GOLD + "Enemy Information", 
            infoLore
        );
        gui.setItem(4, infoItem);
        
        // Add enemies list
        int slot = 10;
        for (String enemyName : clan.getEnemies()) {
            Clan enemyClan = plugin.getStorageManager().getClan(enemyName);
            if (enemyClan == null) continue;
            
            List<String> enemyLore = new ArrayList<>();
            enemyLore.add("");
            enemyLore.add(ChatColor.GRAY + "Members: " + enemyClan.getMembers().size());
            
            if (clan.getMember(player.getUniqueId()).getRole() == ClanRole.LEADER) {
                enemyLore.add("");
                enemyLore.add(ChatColor.GREEN + "Click to remove from enemies");
            }
            
            ItemStack enemyItem = ItemUtils.createGuiItem(
                Material.RED_BANNER, 
                ChatColor.RED + enemyClan.getName(), 
                enemyLore
            );
            gui.setItem(slot, enemyItem);
            
            // Move to next slot
            slot++;
            if (slot % 9 == 8) {
                slot += 2;
            }
            
            if (slot >= 44) break; // Maximum number of enemies we can display
        }
        
        // Add back button
        List<String> backLore = new ArrayList<>();
        backLore.add("");
        backLore.add(ChatColor.GRAY + "Return to main menu");
        ItemStack backButton = ItemUtils.createGuiItem(
            Material.ARROW, 
            ChatColor.YELLOW + "Back", 
            backLore
        );
        gui.setItem(49, backButton);
        
        // Open the GUI
        player.openInventory(gui);
    }
    
    /**
     * Opens the clan settings GUI with various options.
     * 
     * @param player The player to show the GUI to
     * @param clan The clan the player is in
     */
    private void openSettingsGUI(Player player, Clan clan) {
        Inventory gui = Bukkit.createInventory(player, 36, ChatColor.GOLD + "Clan Settings");
        
        // Add glass pane border
        ItemUtils.createGuiBorder(gui, Material.ORANGE_STAINED_GLASS_PANE);
        
        // Add clan info
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add(ChatColor.GRAY + "Configure your clan's settings");
        infoLore.add(ChatColor.GRAY + "and appearance options.");
        
        ItemStack infoItem = ItemUtils.createInfoItem(
            Material.BOOK, 
            ChatColor.GOLD + "Clan Settings", 
            infoLore
        );
        gui.setItem(4, infoItem);
        
        // Visual Identity: Armor - Toggle colored armor
        List<String> armorLore = new ArrayList<>();
        armorLore.add("");
        armorLore.add(ChatColor.GRAY + "Current status: " + 
                     (clan.hasColoredArmor() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        armorLore.add("");
        
        boolean canManageArmor = (clan.getMember(player.getUniqueId()).getRole() == ClanRole.LEADER || 
                                 clan.getMember(player.getUniqueId()).getRole() == ClanRole.OFFICER) &&
                                 player.hasPermission("clan.visual.armor");
        
        if (canManageArmor) {
            armorLore.add(ChatColor.YELLOW + "Click to " + 
                         (clan.hasColoredArmor() ? "disable" : "enable") + 
                         " clan-colored armor");
        } else {
            armorLore.add(ChatColor.RED + "Only clan leaders and officers");
            armorLore.add(ChatColor.RED + "can manage this setting");
        }
        
        ItemStack armorItem = ItemUtils.createGuiItem(
            Material.LEATHER_CHESTPLATE, 
            ChatColor.AQUA + "Clan Armor", 
            armorLore
        );
        gui.setItem(11, armorItem);
        
        // Visual Identity: Nametags
        List<String> nametagLore = new ArrayList<>();
        nametagLore.add("");
        nametagLore.add(ChatColor.GRAY + "Refresh clan nametags in");
        nametagLore.add(ChatColor.GRAY + "the TAB list and above players");
        nametagLore.add("");
        
        if (player.hasPermission("clan.visual.nametag")) {
            nametagLore.add(ChatColor.YELLOW + "Click to refresh all nametags");
        } else {
            nametagLore.add(ChatColor.RED + "You don't have permission");
            nametagLore.add(ChatColor.RED + "to refresh nametags");
        }
        
        ItemStack nametagItem = ItemUtils.createGuiItem(
            Material.NAME_TAG, 
            ChatColor.LIGHT_PURPLE + "Clan Nametags", 
            nametagLore
        );
        gui.setItem(13, nametagItem);
        
        // Visual Identity: Colors
        List<String> colorLore = new ArrayList<>();
        colorLore.add("");
        colorLore.add(ChatColor.GRAY + "Change your clan's color");
        colorLore.add("");
        colorLore.add(ChatColor.GRAY + "Current color: " + clan.getColor() + "■");
        colorLore.add("");
        colorLore.add(ChatColor.YELLOW + "Click to open color selection");
        
        ItemStack colorItem = ItemUtils.createGuiItem(
            Material.FIREWORK_STAR, 
            ChatColor.GREEN + "Clan Color", 
            colorLore
        );
        gui.setItem(15, colorItem);
        
        // Add back button
        List<String> backLore = new ArrayList<>();
        backLore.add("");
        backLore.add(ChatColor.GRAY + "Return to main menu");
        ItemStack backButton = ItemUtils.createGuiItem(
            Material.ARROW, 
            ChatColor.YELLOW + "Back", 
            backLore
        );
        gui.setItem(31, backButton);
        
        // Open the GUI
        player.openInventory(gui);
    }
    
    /**
     * Handles clicks in the clan settings GUI.
     * 
     * @param player The player clicking
     * @param clickedItem The item that was clicked
     * @param clan The player's clan
     */
    private void handleSettingsClick(Player player, ItemStack clickedItem, Clan clan) {
        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        if (itemName.equals(ChatColor.YELLOW + "Back")) {
            // Return to main clan GUI
            player.performCommand("clan gui");
            return;
        } else if (itemName.equals(ChatColor.AQUA + "Clan Armor")) {
            // Check if player has permission to change armor settings
            if ((clan.getMember(player.getUniqueId()).getRole() == ClanRole.LEADER || 
                clan.getMember(player.getUniqueId()).getRole() == ClanRole.OFFICER) && 
                player.hasPermission("clan.visual.armor")) {
                
                // Toggle colored armor
                player.closeInventory();
                player.performCommand("clan armor");
            }
        } else if (itemName.equals(ChatColor.LIGHT_PURPLE + "Clan Nametags")) {
            // Check if player has permission to refresh nametags
            if (player.hasPermission("clan.visual.nametag")) {
                player.closeInventory();
                player.performCommand("clan nametag");
            }
        } else if (itemName.equals(ChatColor.GREEN + "Clan Color")) {
            // Open color selection GUI
            openColorGUI(player, clan);
        }
    }
    
    private void openColorGUI(Player player, Clan clan) {
        Inventory gui = Bukkit.createInventory(player, 27, ChatColor.LIGHT_PURPLE + "Clan Color");
        
        // Add glass pane border
        ItemUtils.createGuiBorder(gui, Material.PURPLE_STAINED_GLASS_PANE);
        
        // Check if player has permission
        boolean canChangeColor = clan.getMember(player.getUniqueId()).getRole() == ClanRole.LEADER;
        
        // Add color options
        ChatColor[] colors = {
            ChatColor.RED, ChatColor.BLUE, ChatColor.GREEN, 
            ChatColor.YELLOW, ChatColor.LIGHT_PURPLE, ChatColor.AQUA, 
            ChatColor.GOLD, ChatColor.BLACK, ChatColor.WHITE
        };
        
        Material[] materials = {
            Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL,
            Material.YELLOW_WOOL, Material.PURPLE_WOOL, Material.CYAN_WOOL,
            Material.ORANGE_WOOL, Material.BLACK_WOOL, Material.WHITE_WOOL
        };
        
        for (int i = 0; i < colors.length; i++) {
            List<String> colorLore = new ArrayList<>();
            colorLore.add("");
            
            if (canChangeColor) {
                colorLore.add(ChatColor.GRAY + "Click to set clan color to " + colors[i] + colors[i].name());
            } else {
                colorLore.add(ChatColor.RED + "Only clan leaders can change colors");
            }
            
            ItemStack colorItem = ItemUtils.createGuiItem(
                materials[i], 
                colors[i] + colors[i].name(), 
                colorLore
            );
            gui.setItem(10 + i, colorItem);
        }
        
        // Add back button
        List<String> backLore = new ArrayList<>();
        backLore.add("");
        backLore.add(ChatColor.GRAY + "Return to main menu");
        ItemStack backButton = ItemUtils.createGuiItem(
            Material.ARROW, 
            ChatColor.YELLOW + "Back", 
            backLore
        );
        gui.setItem(22, backButton);
        
        // Open the GUI
        player.openInventory(gui);
    }
    
    private void handleAllianceClick(Player player, ItemStack clickedItem) {
        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        if (itemName.equals(ChatColor.YELLOW + "Back")) {
            // Return to main clan GUI
            player.performCommand("clan gui");
            return;
        }
        
        Clan playerClan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        if (playerClan == null) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return;
        }
        
        if (playerClan.getMember(player.getUniqueId()).getRole() != ClanRole.LEADER) {
            player.sendMessage(ChatColor.RED + "Only the clan leader can manage alliances!");
            return;
        }
        
        // Extract clan name from item display name
        String allyName = ChatColor.stripColor(itemName);
        
        if (playerClan.isAllied(allyName)) {
            // Remove alliance
            playerClan.removeAlliance(allyName);
            plugin.getStorageManager().saveClan(playerClan);
            
            player.sendMessage(ChatColor.GREEN + "Alliance with " + allyName + " has been removed.");
            
            // Notify the other clan
            Clan allyClan = plugin.getStorageManager().getClan(allyName);
            if (allyClan != null) {
                for (ClanMember member : allyClan.getMembers()) {
                    Player allyPlayer = Bukkit.getPlayer(member.getPlayerUUID());
                    if (allyPlayer != null && allyPlayer.isOnline()) {
                        allyPlayer.sendMessage(ChatColor.YELLOW + "Clan " + playerClan.getName() + " has removed their alliance with your clan.");
                    }
                }
            }
            
            // Refresh the GUI
            openAlliancesGUI(player, playerClan);
        }
    }
    
    private void handleEnemyClick(Player player, ItemStack clickedItem) {
        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        if (itemName.equals(ChatColor.YELLOW + "Back")) {
            // Return to main clan GUI
            player.performCommand("clan gui");
            return;
        }
        
        Clan playerClan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        if (playerClan == null) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return;
        }
        
        if (playerClan.getMember(player.getUniqueId()).getRole() != ClanRole.LEADER) {
            player.sendMessage(ChatColor.RED + "Only the clan leader can manage enemies!");
            return;
        }
        
        // Extract clan name from item display name
        String enemyName = ChatColor.stripColor(itemName);
        
        if (playerClan.isEnemy(enemyName)) {
            // Remove enemy status
            playerClan.removeEnemy(enemyName);
            plugin.getStorageManager().saveClan(playerClan);
            
            player.sendMessage(ChatColor.GREEN + "Removed " + enemyName + " from your enemies list.");
            
            // Notify the other clan
            Clan enemyClan = plugin.getStorageManager().getClan(enemyName);
            if (enemyClan != null) {
                for (ClanMember member : enemyClan.getMembers()) {
                    Player enemyPlayer = Bukkit.getPlayer(member.getPlayerUUID());
                    if (enemyPlayer != null && enemyPlayer.isOnline()) {
                        enemyPlayer.sendMessage(ChatColor.GREEN + "Clan " + playerClan.getName() + " has removed your clan from their enemies list.");
                    }
                }
            }
            
            // Refresh the GUI
            openEnemiesGUI(player, playerClan);
        }
    }
    
    private void handleMemberClick(Player player, ItemStack clickedItem) {
        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        if (itemName.equals(ChatColor.YELLOW + "Back")) {
            // Return to main clan GUI
            player.performCommand("clan gui");
            return;
        }
        
        Clan playerClan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        if (playerClan == null) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return;
        }
        
        ClanMember playerMember = playerClan.getMember(player.getUniqueId());
        
        // Check if player has permission to manage members
        if (playerMember.getRole() != ClanRole.LEADER && playerMember.getRole() != ClanRole.OFFICER) {
            player.sendMessage(ChatColor.RED + "You don't have permission to manage clan members!");
            return;
        }
        
        // Extract member name from item display name
        String memberName = ChatColor.stripColor(itemName).replace(" (Online)", "").replace(" (Offline)", "");
        
        // Find the target member
        for (ClanMember member : playerClan.getMembers()) {
            if (member.getPlayerName().equals(memberName)) {
                // Handle based on click and roles
                if (player.isSneaking() && 
                    (playerMember.getRole() == ClanRole.LEADER || 
                     (playerMember.getRole() == ClanRole.OFFICER && member.getRole() == ClanRole.MEMBER))) {
                    // Kick member
                    playerClan.removeMember(member.getPlayerUUID());
                    plugin.getStorageManager().saveClan(playerClan);
                    
                    player.sendMessage(ChatColor.GREEN + "Kicked " + memberName + " from the clan.");
                    
                    // Notify the kicked player if online
                    Player kickedPlayer = Bukkit.getPlayer(member.getPlayerUUID());
                    if (kickedPlayer != null && kickedPlayer.isOnline()) {
                        kickedPlayer.sendMessage(ChatColor.RED + "You have been kicked from clan " + playerClan.getName() + "!");
                    }
                    
                    // Refresh the GUI
                    openMembersGUI(player, playerClan);
                } else if (playerMember.getRole() == ClanRole.LEADER) {
                    if (member.getRole() == ClanRole.MEMBER) {
                        // Promote to officer
                        member.setRole(ClanRole.OFFICER);
                        plugin.getStorageManager().saveClan(playerClan);
                        
                        player.sendMessage(ChatColor.GREEN + "Promoted " + memberName + " to Officer.");
                        
                        // Notify the promoted player if online
                        Player promotedPlayer = Bukkit.getPlayer(member.getPlayerUUID());
                        if (promotedPlayer != null && promotedPlayer.isOnline()) {
                            promotedPlayer.sendMessage(ChatColor.GREEN + "You have been promoted to Officer in clan " + playerClan.getName() + "!");
                        }
                        
                        // Refresh the GUI
                        openMembersGUI(player, playerClan);
                    } else if (member.getRole() == ClanRole.OFFICER) {
                        // Demote to member
                        member.setRole(ClanRole.MEMBER);
                        plugin.getStorageManager().saveClan(playerClan);
                        
                        player.sendMessage(ChatColor.YELLOW + "Demoted " + memberName + " to Member.");
                        
                        // Notify the demoted player if online
                        Player demotedPlayer = Bukkit.getPlayer(member.getPlayerUUID());
                        if (demotedPlayer != null && demotedPlayer.isOnline()) {
                            demotedPlayer.sendMessage(ChatColor.YELLOW + "You have been demoted to Member in clan " + playerClan.getName() + ".");
                        }
                        
                        // Refresh the GUI
                        openMembersGUI(player, playerClan);
                    }
                }
                
                break;
            }
        }
    }
    
    private void handleColorClick(Player player, ItemStack clickedItem) {
        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        if (itemName.equals(ChatColor.YELLOW + "Back")) {
            // Return to main clan GUI
            player.performCommand("clan gui");
            return;
        }
        
        Clan playerClan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        if (playerClan == null) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return;
        }
        
        if (playerClan.getMember(player.getUniqueId()).getRole() != ClanRole.LEADER) {
            player.sendMessage(ChatColor.RED + "Only the clan leader can change the clan color!");
            return;
        }
        
        // Extract color name
        String colorName = ChatColor.stripColor(itemName);
        
        try {
            ChatColor color = ChatColor.valueOf(colorName);
            playerClan.setColor(color.toString());
            plugin.getStorageManager().saveClan(playerClan);
            
            player.sendMessage(ChatColor.GREEN + "Clan color set to " + color + colorName);
            
            // Notify clan members
            MessageUtils.notifyClan(playerClan, "Clan color has been changed to " + color + colorName);
            
            // Return to main GUI
            player.performCommand("clan gui");
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid color selected!");
        }
    }
    
    /**
     * Opens the progression GUI showing level information and benefits.
     * 
     * @param player The player to show the GUI to
     * @param clan The clan to show progression for
     */
    private void openProgressionGUI(Player player, Clan clan) {
        Inventory gui = Bukkit.createInventory(player, 54, ChatColor.GREEN + "Clan Progression");
        
        // Add glass pane border
        ItemUtils.createGuiBorder(gui, Material.LIME_STAINED_GLASS_PANE);
        
        // Add current level info
        List<String> currentLevelLore = new ArrayList<>();
        currentLevelLore.add("");
        currentLevelLore.add(ChatColor.YELLOW + "Current Level: " + ChatColor.GOLD + clan.getLevel());
        currentLevelLore.add(ChatColor.YELLOW + "Current XP: " + ChatColor.WHITE + clan.getExperience());
        
        // Add next level info if not at max level
        int expForNextLevel = plugin.getProgressionManager().getExperienceForNextLevel(clan);
        if (expForNextLevel > 0) {
            currentLevelLore.add(ChatColor.YELLOW + "XP Needed for Next Level: " + ChatColor.WHITE + expForNextLevel);
            
            // Calculate and show percentage to next level
            int currentExp = clan.getExperience();
            int nextLevelTotalXP = plugin.getProgressionManager().getRequiredExperienceForLevel(clan.getLevel() + 1);
            double percentage = (double) currentExp / nextLevelTotalXP * 100;
            currentLevelLore.add(ChatColor.YELLOW + "Progress: " + ChatColor.WHITE + String.format("%.1f%%", percentage));
            
            // Create a text-based progress bar
            StringBuilder progressBar = new StringBuilder(ChatColor.GRAY + "[");
            int barLength = 20;
            int filledBars = (int) Math.round((percentage / 100) * barLength);
            
            for (int i = 0; i < barLength; i++) {
                if (i < filledBars) {
                    progressBar.append(ChatColor.GREEN + "■");
                } else {
                    progressBar.append(ChatColor.WHITE + "□");
                }
            }
            progressBar.append(ChatColor.GRAY + "]");
            currentLevelLore.add(progressBar.toString());
        } else {
            currentLevelLore.add(ChatColor.GREEN + "Maximum level reached!");
        }
        
        currentLevelLore.add("");
        currentLevelLore.add(ChatColor.GRAY + "Earn XP by:");
        currentLevelLore.add(ChatColor.GRAY + "- Claiming territory (+50 XP)");
        currentLevelLore.add(ChatColor.GRAY + "- Winning wars (+500 XP)");
        currentLevelLore.add(ChatColor.GRAY + "- Making alliances (+100 XP)");
        currentLevelLore.add(ChatColor.GRAY + "- Recruiting members (+50 XP)");
        
        ItemStack currentLevelItem = ItemUtils.createInfoItem(
            Material.EXPERIENCE_BOTTLE,
            ChatColor.GOLD + "Clan Progression",
            currentLevelLore
        );
        gui.setItem(4, currentLevelItem);
        
        // Add all level information items with benefits
        int slot = 19;
        for (int i = 1; i <= plugin.getProgressionManager().getMaxLevel(); i++) {
            List<String> levelLore = new ArrayList<>();
            levelLore.add("");
            
            // Show XP required
            if (i == 1) {
                levelLore.add(ChatColor.YELLOW + "Starting Level - 0 XP required");
            } else {
                int xpRequired = plugin.getProgressionManager().getRequiredExperienceForLevel(i);
                levelLore.add(ChatColor.YELLOW + "XP Required: " + ChatColor.WHITE + xpRequired);
            }
            
            levelLore.add("");
            levelLore.add(ChatColor.AQUA + "Benefits:");
            
            // Get benefits for this level
            Map<String, Integer> benefits = plugin.getProgressionManager().getLevelBenefits(i);
            for (Map.Entry<String, Integer> benefit : benefits.entrySet()) {
                String formattedName = benefit.getKey().replace("_", " ");
                String benefitName = formattedName.substring(0, 1).toUpperCase() + formattedName.substring(1);
                levelLore.add(ChatColor.GRAY + "- " + benefitName + ": " + 
                               ChatColor.WHITE + benefit.getValue());
            }
            
            // Highlight the current clan level
            Material material = (clan.getLevel() == i) ? 
                              Material.ENCHANTED_BOOK : Material.BOOK;
            String displayName = (clan.getLevel() == i) ? 
                               ChatColor.GREEN + "Level " + i + " (Current)" :
                               ChatColor.YELLOW + "Level " + i;
            
            ItemStack levelItem = ItemUtils.createGuiItem(material, displayName, levelLore);
            gui.setItem(slot, levelItem);
            
            // Move to next slot
            slot += 2;
        }
        
        // Add back button
        List<String> backLore = new ArrayList<>();
        backLore.add("");
        backLore.add(ChatColor.GRAY + "Return to main menu");
        ItemStack backButton = ItemUtils.createGuiItem(
            Material.ARROW, 
            ChatColor.YELLOW + "Back", 
            backLore
        );
        gui.setItem(49, backButton);
        
        // Open the GUI
        player.openInventory(gui);
    }
    
    /**
     * Handles clicks in the progression GUI.
     * 
     * @param player The player clicking
     * @param clickedItem The item that was clicked
     */
    private void handleProgressionClick(Player player, ItemStack clickedItem) {
        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        if (itemName.equals(ChatColor.YELLOW + "Back")) {
            // Return to main clan GUI
            player.performCommand("clan gui");
        }
    }
    
    /**
     * Handles clicks in the clan skills GUI
     */
    private void handleSkillsClick(Player player, ItemStack clickedItem) {
        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String itemName = clickedItem.getItemMeta().getDisplayName();
        Clan playerClan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        if (playerClan == null) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return;
        }
        
        if (itemName.equals(ChatColor.YELLOW + "Back")) {
            // Return to main menu
            player.performCommand("clan gui");
        } else {
            // Check if it's a skill tree
            for (SkillTree tree : SkillTree.values()) {
                if (itemName.equals(tree.getColoredName() + " Skills")) {
                    openSkillTreeGUI(player, playerClan, tree);
                    return;
                }
            }
            
            // Check for member specializations
            if (itemName.startsWith(ChatColor.GOLD + "Assign Specialization")) {
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Use /clan skill specialize <role> to specialize in a role.");
                player.sendMessage(ChatColor.YELLOW + "Available specializations: miner, farmer, builder, hunter");
            }
        }
    }
    
    /**
     * Handles clicks in a specific skill tree GUI
     */
    private void handleSkillTreeClick(Player player, ItemStack clickedItem, String title) {
        if (clickedItem == null || !clickedItem.hasItemMeta() || !clickedItem.getItemMeta().hasDisplayName()) {
            return;
        }
        
        String itemName = clickedItem.getItemMeta().getDisplayName();
        Clan playerClan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        if (playerClan == null) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "You are not in a clan!");
            return;
        }
        
        if (itemName.equals(ChatColor.YELLOW + "Back")) {
            // Return to skills menu
            openSkillsGUI(player, playerClan);
        } else if (itemName.equals(ChatColor.GREEN + "Learn Skill")) {
            // Get skill ID from item lore
            ItemMeta meta = clickedItem.getItemMeta();
            List<String> lore = meta.getLore();
            if (lore == null || lore.isEmpty()) return;
            
            String skillId = "";
            for (String line : lore) {
                if (line.startsWith(ChatColor.DARK_GRAY + "ID: ")) {
                    skillId = line.substring((ChatColor.DARK_GRAY + "ID: ").length());
                    break;
                }
            }
            
            if (skillId.isEmpty()) return;
            
            // Try to learn the skill
            ClanMember member = playerClan.getMember(player.getUniqueId());
            if (member == null) return;
            
            MemberSkills skills = plugin.getSkillManager().getMemberSkills(player.getUniqueId());
            
            ClanSkill skill = plugin.getSkillManager().getSkill(skillId);
            if (skill == null) {
                player.sendMessage(ChatColor.RED + "Skill not found: " + skillId);
                return;
            }
            
            // Check if player has enough clan points
            int playerPoints = skills.getSkillPoints();
            int skillCost = skill.getLevelCost(skills.getSkillLevel(skillId) + 1);
            
            if (playerPoints < skillCost) {
                player.sendMessage(ChatColor.RED + "Not enough skill points! You need " + skillCost + 
                                   " points, but only have " + playerPoints + ".");
                return;
            }
            
            // Check prerequisites
            for (String prereq : skill.getPrerequisites()) {
                if (skills.getSkillLevel(prereq) <= 0) {
                    ClanSkill prereqSkill = plugin.getSkillManager().getSkill(prereq);
                    if (prereqSkill != null) {
                        player.sendMessage(ChatColor.RED + "You need to learn " + 
                                          prereqSkill.getName() + " first!");
                    } else {
                        player.sendMessage(ChatColor.RED + "You don't meet the prerequisites for this skill!");
                    }
                    return;
                }
            }
            
            // Learn the skill
            skills.increaseSkillLevel(skillId, 1);
            int newLevel = skills.getSkillLevel(skillId);
            skills.setSkillPoints(skills.getSkillPoints() - skillCost);
            plugin.getSkillManager().saveMemberSkills();
            
            player.sendMessage(ChatColor.GREEN + "You've learned " + skill.getName() + 
                              " (Level " + newLevel + ")!");
            
            // Refresh the GUI
            SkillTree tree = skill.getTree();
            openSkillTreeGUI(player, playerClan, tree);
        }
    }
    
    /**
     * Opens the clan skills GUI showing all available skill trees
     */
    private void openSkillsGUI(Player player, Clan clan) {
        Inventory gui = Bukkit.createInventory(player, 54, ChatColor.DARK_AQUA + "Clan Skills");
        
        // Add glass pane border
        ItemUtils.createGuiBorder(gui, Material.CYAN_STAINED_GLASS_PANE);
        
        // Add clan info
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add(ChatColor.GRAY + "Manage your clan's skills and specializations.");
        infoLore.add("");
        infoLore.add(ChatColor.YELLOW + "Click on a skill tree to view and learn skills.");
        
        ItemStack infoItem = ItemUtils.createInfoItem(
            Material.BOOK, 
            ChatColor.GOLD + "Clan Skills System", 
            infoLore
        );
        gui.setItem(4, infoItem);
        
        // Get member skills
        MemberSkills skills = plugin.getSkillManager().getMemberSkills(player.getUniqueId());
        int availablePoints = (skills != null) ? skills.getSkillPoints() : 0;
        
        // Add player info
        List<String> playerInfoLore = new ArrayList<>();
        playerInfoLore.add("");
        playerInfoLore.add(ChatColor.GRAY + "Your specialization: " + 
                         (skills != null && skills.getSpecialization() != null ? 
                          skills.getSpecialization().getColoredName() : 
                          ChatColor.RED + "None"));
        playerInfoLore.add("");
        playerInfoLore.add(ChatColor.YELLOW + "Available Skill Points: " + 
                         ChatColor.WHITE + availablePoints);
        playerInfoLore.add("");
        playerInfoLore.add(ChatColor.GRAY + "Earn skill points through clan");
        playerInfoLore.add(ChatColor.GRAY + "activities and level progression");
        
        ItemStack playerItem = ItemUtils.createPlayerHead(player, 
            ChatColor.GOLD + "Your Skills", 
            playerInfoLore
        );
        gui.setItem(13, playerItem);
        
        // Add skill trees - General skill trees first
        ItemStack combatTree = createSkillTreeItem(SkillTree.COMBAT, player);
        ItemStack utilityTree = createSkillTreeItem(SkillTree.UTILITY, player);
        ItemStack diplomacyTree = createSkillTreeItem(SkillTree.DIPLOMACY, player);
        ItemStack territoryTree = createSkillTreeItem(SkillTree.TERRITORY, player);
        ItemStack economyTree = createSkillTreeItem(SkillTree.ECONOMY, player);
        
        gui.setItem(19, combatTree);
        gui.setItem(21, utilityTree);
        gui.setItem(23, diplomacyTree);
        gui.setItem(25, territoryTree);
        gui.setItem(29, economyTree);
        
        // Role-based specialization trees
        ItemStack minerTree = createSkillTreeItem(SkillTree.MINER, player);
        ItemStack farmerTree = createSkillTreeItem(SkillTree.FARMER, player);
        ItemStack builderTree = createSkillTreeItem(SkillTree.BUILDER, player);
        ItemStack hunterTree = createSkillTreeItem(SkillTree.HUNTER, player);
        
        gui.setItem(31, minerTree);
        gui.setItem(33, farmerTree);
        gui.setItem(39, builderTree);
        gui.setItem(41, hunterTree);
        
        // Add specialization assignment item
        List<String> specializeLore = new ArrayList<>();
        specializeLore.add("");
        specializeLore.add(ChatColor.GRAY + "Assign yourself a specialization");
        specializeLore.add(ChatColor.GRAY + "to gain role-specific bonuses.");
        specializeLore.add("");
        specializeLore.add(ChatColor.YELLOW + "Current specialization: " + 
                          (skills != null && skills.getSpecialization() != null ? 
                           skills.getSpecialization().getColoredName() : 
                           ChatColor.RED + "None"));
        specializeLore.add("");
        specializeLore.add(ChatColor.GRAY + "Specializations are earned through");
        specializeLore.add(ChatColor.GRAY + "performing related activities or");
        specializeLore.add(ChatColor.GRAY + "manually assigned by clan leadership.");
        specializeLore.add("");
        specializeLore.add(ChatColor.YELLOW + "Click to change specialization");
        
        ItemStack specializeItem = ItemUtils.createGuiItem(
            Material.NETHER_STAR,
            ChatColor.GOLD + "Assign Specialization",
            specializeLore
        );
        gui.setItem(49, specializeItem);
        
        // Add back button
        List<String> backLore = new ArrayList<>();
        backLore.add("");
        backLore.add(ChatColor.GRAY + "Return to main menu");
        ItemStack backButton = ItemUtils.createGuiItem(
            Material.ARROW, 
            ChatColor.YELLOW + "Back", 
            backLore
        );
        gui.setItem(45, backButton);
        
        // Open the GUI
        player.openInventory(gui);
    }
    
    /**
     * Opens a specific skill tree GUI
     */
    private void openSkillTreeGUI(Player player, Clan clan, SkillTree tree) {
        Inventory gui = Bukkit.createInventory(player, 54, 
            ChatColor.DARK_AQUA + "Skills: " + tree.getColoredName());
        
        // Add glass pane border with tree color
        Material paneMaterial;
        switch (tree) {
            case COMBAT:
                paneMaterial = Material.RED_STAINED_GLASS_PANE;
                break;
            case TERRITORY:
                paneMaterial = Material.GREEN_STAINED_GLASS_PANE;
                break;
            case ECONOMY:
                paneMaterial = Material.YELLOW_STAINED_GLASS_PANE;
                break;
            case DIPLOMACY:
                paneMaterial = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                break;
            case UTILITY:
                paneMaterial = Material.PURPLE_STAINED_GLASS_PANE;
                break;
            case MINER:
                paneMaterial = Material.GRAY_STAINED_GLASS_PANE;
                break;
            case FARMER:
                paneMaterial = Material.LIME_STAINED_GLASS_PANE;
                break;
            case BUILDER:
                paneMaterial = Material.ORANGE_STAINED_GLASS_PANE;
                break;
            case HUNTER:
                paneMaterial = Material.BROWN_STAINED_GLASS_PANE;
                break;
            default:
                paneMaterial = Material.CYAN_STAINED_GLASS_PANE;
        }
        ItemUtils.createGuiBorder(gui, paneMaterial);
        
        // Add tree info
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add(ChatColor.GRAY + tree.getDescription());
        infoLore.add("");
        
        // Get player skills
        MemberSkills skills = plugin.getSkillManager().getMemberSkills(player.getUniqueId());
        int availablePoints = (skills != null) ? skills.getSkillPoints() : 0;
        
        infoLore.add(ChatColor.YELLOW + "Available Skill Points: " + 
                    ChatColor.WHITE + availablePoints);
        
        // Show specialization info for role-based trees
        if (tree == SkillTree.MINER || tree == SkillTree.FARMER || 
            tree == SkillTree.BUILDER || tree == SkillTree.HUNTER) {
            infoLore.add("");
            infoLore.add(ChatColor.GOLD + "Role-based Specialization");
            
            if (skills != null && skills.getSpecialization() == tree) {
                infoLore.add(ChatColor.GREEN + "You are specialized in this role!");
                infoLore.add(ChatColor.GRAY + "You gain extra bonuses from these skills.");
            } else {
                infoLore.add(ChatColor.RED + "You are not specialized in this role");
                infoLore.add(ChatColor.GRAY + "Specialize to gain enhanced benefits.");
            }
        }
        
        ItemStack infoItem = ItemUtils.createInfoItem(
            Material.ENCHANTED_BOOK, 
            tree.getColoredName() + " Skills", 
            infoLore
        );
        gui.setItem(4, infoItem);
        
        // Add skills for this tree
        List<ClanSkill> treeSkills = plugin.getSkillManager().getSkillsByTree(tree);
        Map<String, ClanSkill> treeSkillsMap = new HashMap<>();
        for (ClanSkill skill : treeSkills) {
            treeSkillsMap.put(skill.getId(), skill);
        }
        
        // Map to track skill locations
        Map<String, Integer> skillSlots = new HashMap<>();
        
        // First, place base skills (those with no prerequisites)
        int baseSlot = 19;
        for (ClanSkill skill : treeSkills) {
            if (skill.getPrerequisites().isEmpty()) {
                ItemStack skillItem = createSkillItem(skill, player);
                gui.setItem(baseSlot, skillItem);
                skillSlots.put(skill.getId(), baseSlot);
                baseSlot += 2;
                if (baseSlot > 25) break;  // Maximum of 4 base skills per row
            }
        }
        
        // Then, place skills with prerequisites
        int nextSlot = 28;
        for (ClanSkill skill : treeSkills) {
            if (!skill.getPrerequisites().isEmpty() && !skillSlots.containsKey(skill.getId())) {
                // Find the slot of the first prerequisite to position this below it
                String firstPrereq = skill.getPrerequisites().get(0);
                Integer prereqSlot = skillSlots.get(firstPrereq);
                
                if (prereqSlot != null) {
                    // Place this skill below its prerequisite
                    int row = prereqSlot / 9;
                    int col = prereqSlot % 9;
                    int newRow = row + 1;
                    int newSlot = newRow * 9 + col;
                    
                    // Check if the slot is already taken
                    if (gui.getItem(newSlot) == null || 
                        gui.getItem(newSlot).getType() == paneMaterial) {
                        ItemStack skillItem = createSkillItem(skill, player);
                        gui.setItem(newSlot, skillItem);
                        skillSlots.put(skill.getId(), newSlot);
                    } else {
                        // Find the next available slot
                        while (nextSlot < 45) {
                            if (gui.getItem(nextSlot) == null || 
                                gui.getItem(nextSlot).getType() == paneMaterial) {
                                ItemStack skillItem = createSkillItem(skill, player);
                                gui.setItem(nextSlot, skillItem);
                                skillSlots.put(skill.getId(), nextSlot);
                                nextSlot++;
                                break;
                            }
                            nextSlot++;
                        }
                    }
                } else {
                    // If prerequisite not found yet, place in next available slot
                    while (nextSlot < 45) {
                        if (gui.getItem(nextSlot) == null || 
                            gui.getItem(nextSlot).getType() == paneMaterial) {
                            ItemStack skillItem = createSkillItem(skill, player);
                            gui.setItem(nextSlot, skillItem);
                            skillSlots.put(skill.getId(), nextSlot);
                            nextSlot++;
                            break;
                        }
                        nextSlot++;
                    }
                }
            }
        }
        
        // Add back button
        List<String> backLore = new ArrayList<>();
        backLore.add("");
        backLore.add(ChatColor.GRAY + "Return to skills menu");
        ItemStack backButton = ItemUtils.createGuiItem(
            Material.ARROW, 
            ChatColor.YELLOW + "Back", 
            backLore
        );
        gui.setItem(49, backButton);
        
        // Open the GUI
        player.openInventory(gui);
    }
    
    /**
     * Creates an item representing a skill tree
     */
    private ItemStack createSkillTreeItem(SkillTree tree, Player player) {
        Material material;
        switch (tree) {
            case COMBAT:
                material = Material.IRON_SWORD;
                break;
            case TERRITORY:
                material = Material.GRASS_BLOCK;
                break;
            case ECONOMY:
                material = Material.GOLD_INGOT;
                break;
            case DIPLOMACY:
                material = Material.PAPER;
                break;
            case UTILITY:
                material = Material.CHEST;
                break;
            case MINER:
                material = Material.DIAMOND_PICKAXE;
                break;
            case FARMER:
                material = Material.WHEAT;
                break;
            case BUILDER:
                material = Material.BRICKS;
                break;
            case HUNTER:
                material = Material.BOW;
                break;
            default:
                material = Material.BOOK;
        }
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + tree.getDescription());
        lore.add("");
        
        // Get player skills in this tree
        MemberSkills skills = plugin.getSkillManager().getMemberSkills(player.getUniqueId());
        List<ClanSkill> treeSkillsList = plugin.getSkillManager().getSkillsByTree(tree);
        int learnedSkills = 0;
        
        if (skills != null) {
            for (ClanSkill skill : treeSkillsList) {
                if (skills.getSkillLevel(skill.getId()) > 0) {
                    learnedSkills++;
                }
            }
        }
        
        lore.add(ChatColor.YELLOW + "Skills Learned: " + ChatColor.WHITE + 
                 learnedSkills + "/" + treeSkillsList.size());
        
        // Add specialization info for role-based trees
        if (tree == SkillTree.MINER || tree == SkillTree.FARMER || 
            tree == SkillTree.BUILDER || tree == SkillTree.HUNTER) {
            lore.add("");
            if (skills != null && skills.getSpecialization() == tree) {
                lore.add(ChatColor.GREEN + "Your specialization");
            }
        }
        
        lore.add("");
        lore.add(ChatColor.AQUA + "Click to view skills");
        
        return ItemUtils.createGuiItem(
            material,
            tree.getColoredName() + " Skills",
            lore
        );
    }
    
    /**
     * Creates an item representing a skill
     */
    private ItemStack createSkillItem(ClanSkill skill, Player player) {
        Material material;
        boolean isLearned = false;
        int currentLevel = 0;
        boolean canLearn = false;
        boolean meetsPrereqs = true;
        
        // Determine material based on skill tree
        switch (skill.getTree()) {
            case COMBAT:
                material = Material.IRON_SWORD;
                break;
            case TERRITORY:
                material = Material.GRASS_BLOCK;
                break;
            case ECONOMY:
                material = Material.GOLD_INGOT;
                break;
            case DIPLOMACY:
                material = Material.PAPER;
                break;
            case UTILITY:
                material = Material.CHEST;
                break;
            case MINER:
                material = Material.DIAMOND_PICKAXE;
                break;
            case FARMER:
                material = Material.WHEAT;
                break;
            case BUILDER:
                material = Material.BRICKS;
                break;
            case HUNTER:
                material = Material.BOW;
                break;
            default:
                material = Material.BOOK;
        }
        
        // Get player's skill info
        MemberSkills skills = plugin.getSkillManager().getMemberSkills(player.getUniqueId());
        if (skills != null) {
            currentLevel = skills.getSkillLevel(skill.getId());
            isLearned = currentLevel > 0;
            
            // Check if player can learn next level
            if (currentLevel < skill.getMaxLevel()) {
                // Check prerequisites
                for (String prereq : skill.getPrerequisites()) {
                    if (skills.getSkillLevel(prereq) <= 0) {
                        meetsPrereqs = false;
                        break;
                    }
                }
                
                // Check if player has enough points
                int pointsNeeded = skill.getLevelCost(currentLevel + 1);
                canLearn = meetsPrereqs && skills.getSkillPoints() >= pointsNeeded;
            }
        }
        
        // Create lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "ID: " + skill.getId());
        lore.add("");
        lore.add(ChatColor.GRAY + skill.getDescription());
        lore.add("");
        
        // Add current level info
        if (isLearned) {
            lore.add(ChatColor.YELLOW + "Current Level: " + ChatColor.GREEN + currentLevel + 
                    "/" + skill.getMaxLevel());
            
            // Add current effects
            lore.add("");
            lore.add(ChatColor.AQUA + "Current effects:");
            Map<String, Integer> effects = skill.getLevelEffects(currentLevel);
            for (Map.Entry<String, Integer> effect : effects.entrySet()) {
                lore.add(ChatColor.GRAY + "• " + formatEffectName(effect.getKey()) + 
                        ": " + ChatColor.WHITE + formatEffectValue(effect.getKey(), effect.getValue()));
            }
        } else {
            lore.add(ChatColor.YELLOW + "Current Level: " + ChatColor.RED + "Not learned");
        }
        
        // Add next level info if not maxed
        if (currentLevel < skill.getMaxLevel()) {
            int nextLevel = currentLevel + 1;
            int cost = skill.getLevelCost(nextLevel);
            
            lore.add("");
            lore.add(ChatColor.GOLD + "Next Level (" + nextLevel + "):");
            lore.add(ChatColor.YELLOW + "Cost: " + ChatColor.WHITE + cost + " points");
            
            // Add next level effects
            Map<String, Integer> nextEffects = skill.getLevelEffects(nextLevel);
            if (!nextEffects.isEmpty()) {
                lore.add("");
                lore.add(ChatColor.AQUA + "Effects:");
                for (Map.Entry<String, Integer> effect : nextEffects.entrySet()) {
                    lore.add(ChatColor.GRAY + "• " + formatEffectName(effect.getKey()) + 
                            ": " + ChatColor.WHITE + formatEffectValue(effect.getKey(), effect.getValue()));
                }
            }
            
            // Prerequisites
            if (!skill.getPrerequisites().isEmpty()) {
                lore.add("");
                lore.add(ChatColor.RED + "Prerequisites:");
                for (String prereqId : skill.getPrerequisites()) {
                    ClanSkill prereq = plugin.getSkillManager().getSkill(prereqId);
                    if (prereq != null) {
                        boolean hasPrereq = skills != null && skills.getSkillLevel(prereqId) > 0;
                        lore.add((hasPrereq ? ChatColor.GREEN : ChatColor.RED) + 
                                "• " + prereq.getName());
                    }
                }
            }
            
            // Can learn?
            lore.add("");
            if (canLearn) {
                lore.add(ChatColor.GREEN + "Click to learn this skill!");
            } else if (!meetsPrereqs) {
                lore.add(ChatColor.RED + "You don't meet the prerequisites!");
            } else {
                lore.add(ChatColor.RED + "Not enough skill points!");
                if (skills != null) {
                    lore.add(ChatColor.RED + "You have: " + skills.getSkillPoints() + 
                            "/" + cost + " needed");
                }
            }
        } else {
            lore.add("");
            lore.add(ChatColor.GOLD + "Maximum level reached!");
        }
        
        // Create the item with appropriate material and color
        String displayName = (isLearned ? skill.getTree().getColor() : ChatColor.GRAY) + skill.getName();
        
        ItemStack item = ItemUtils.createGuiItem(
            (isLearned ? material : Material.BOOK),
            displayName,
            lore
        );
        
        // If player can learn, add glowing effect and "Learn" button
        if (canLearn) {
            List<String> learnLore = new ArrayList<>(lore);
            ItemStack learnButton = ItemUtils.createGuiItem(
                Material.EMERALD,
                ChatColor.GREEN + "Learn Skill",
                learnLore
            );
            return learnButton;
        }
        
        return item;
    }
    
    /**
     * Formats an effect name for display
     */
    private String formatEffectName(String effectId) {
        // Convert effect_name to Effect Name
        String[] words = effectId.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append(word.substring(0, 1).toUpperCase())
                  .append(word.substring(1))
                  .append(" ");
        }
        return result.toString().trim();
    }
    
    /**
     * Formats an effect value based on its type
     */
    private String formatEffectValue(String effectId, int value) {
        // Format based on effect type (percentage, multiplier, etc.)
        if (effectId.contains("chance") || 
            effectId.contains("bonus") || 
            effectId.contains("efficiency")) {
            return "+" + value + "%";
        } else if (effectId.contains("speed")) {
            return value + "% faster";
        } else if (effectId.contains("reduction")) {
            return "-" + value + "%";
        } else if (effectId.contains("level")) {
            return "+" + value + " levels";
        } else {
            return "+" + value;
        }
    }
}
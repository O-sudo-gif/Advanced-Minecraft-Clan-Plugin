package com.minecraft.clanplugin.commands;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.hologram.BannerManager;
import com.minecraft.clanplugin.hologram.ClanBanner;
import com.minecraft.clanplugin.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command for managing clan holographic banners.
 */
public class ClanHologramCommand implements CommandExecutor, TabCompleter {
    
    private final ClanPlugin plugin;
    
    /**
     * Creates a new clan hologram command handler.
     * 
     * @param plugin The clan plugin instance
     */
    public ClanHologramCommand(ClanPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player is in a clan
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You must be in a clan to use this command.");
            return true;
        }
        
        // No arguments - show help
        if (args.length == 0) {
            return showHelp(player);
        }
        
        String subCommand = args[0].toLowerCase();
        BannerManager bannerManager = plugin.getBannerManager();
        
        switch (subCommand) {
            case "create":
                // Create a new holographic banner at the player's location
                if (!player.hasPermission("clan.banner.hologram")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to create holographic banners.");
                    return true;
                }
                
                // Create default content
                List<String> defaultContent = new ArrayList<>();
                defaultContent.add("Clan: " + clan.getName());
                defaultContent.add("Click to edit");
                
                ClanBanner.BannerStyle style = ClanBanner.BannerStyle.DEFAULT;
                ClanBanner.BannerPermission perm = ClanBanner.BannerPermission.OFFICER;
                
                ClanBanner banner = bannerManager.createBanner(clan, player, "Banner", defaultContent, style, perm);
                if (banner != null) {
                    player.sendMessage(ChatColor.GREEN + "Holographic banner created successfully! ID: " + banner.getId());
                    player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.WHITE + "/clanhologram content <id> <text...>" + 
                                      ChatColor.YELLOW + " to set the banner text.");
                }
                return true;
                
            case "delete":
                // Delete a banner
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /clanhologram delete <banner_id>");
                    return true;
                }
                
                try {
                    UUID bannerId = UUID.fromString(args[1]);
                    boolean deleted = bannerManager.deleteBanner(player, bannerId);
                    
                    if (deleted) {
                        player.sendMessage(ChatColor.GREEN + "Banner deleted successfully!");
                    }
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "Invalid banner ID. Use /clanhologram list to see valid IDs.");
                }
                return true;
                
            case "move":
                // Move a banner to the player's current location
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /clanhologram move <banner_id>");
                    return true;
                }
                
                try {
                    UUID bannerId = UUID.fromString(args[1]);
                    boolean moved = bannerManager.moveBanner(player, bannerId);
                    
                    if (moved) {
                        player.sendMessage(ChatColor.GREEN + "Banner moved to your location!");
                    }
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "Invalid banner ID. Use /clanhologram list to see valid IDs.");
                }
                return true;
                
            case "content":
                // Update the content of a banner
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /clanhologram content <banner_id> <text...>");
                    return true;
                }
                
                try {
                    UUID bannerId = UUID.fromString(args[1]);
                    
                    // Combine all remaining arguments into the content
                    List<String> content = new ArrayList<>();
                    StringBuilder line = new StringBuilder();
                    
                    for (int i = 2; i < args.length; i++) {
                        if (args[i].equals("\\n")) {
                            content.add(line.toString());
                            line = new StringBuilder();
                        } else {
                            if (line.length() > 0) {
                                line.append(" ");
                            }
                            line.append(args[i]);
                        }
                    }
                    
                    if (line.length() > 0) {
                        content.add(line.toString());
                    }
                    
                    boolean updated = bannerManager.updateBannerContent(player, bannerId, content);
                    
                    if (updated) {
                        player.sendMessage(ChatColor.GREEN + "Banner content updated!");
                        player.sendMessage(ChatColor.YELLOW + "Tip: Use " + ChatColor.WHITE + "\\n" + 
                                          ChatColor.YELLOW + " to create a new line.");
                    }
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "Invalid banner ID. Use /clanhologram list to see valid IDs.");
                }
                return true;
                
            case "style":
                // Change the style of a banner (colors, bold, italic, etc.)
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /clanhologram style <banner_id> <style>");
                    player.sendMessage(ChatColor.YELLOW + "Available styles: " + 
                                      ChatColor.WHITE + "DEFAULT, MINIMALIST, ELEGANT, WARRIOR, ROYAL, MYSTICAL, NATURE");
                    return true;
                }
                
                try {
                    UUID bannerId = UUID.fromString(args[1]);
                    
                    // Convert style string to enum
                    ClanBanner.BannerStyle bannerStyle;
                    try {
                        bannerStyle = ClanBanner.BannerStyle.valueOf(args[2].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(ChatColor.RED + "Invalid style: " + args[2]);
                        player.sendMessage(ChatColor.YELLOW + "Available styles: " + 
                                          ChatColor.WHITE + "DEFAULT, MINIMALIST, ELEGANT, WARRIOR, ROYAL, MYSTICAL, NATURE");
                        return true;
                    }
                    
                    boolean updated = bannerManager.updateBannerStyle(player, bannerId, bannerStyle);
                    
                    if (updated) {
                        player.sendMessage(ChatColor.GREEN + "Banner style updated to " + bannerStyle.name() + "!");
                    }
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "Invalid banner ID.");
                }
                return true;
                
            case "toggle":
                // Toggle the visibility of a banner
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /clanhologram toggle <banner_id>");
                    return true;
                }
                
                try {
                    UUID bannerId = UUID.fromString(args[1]);
                    boolean visible = bannerManager.toggleBannerVisibility(player, bannerId);
                    
                    if (visible) {
                        player.sendMessage(ChatColor.GREEN + "Banner is now visible!");
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "Banner is now hidden!");
                    }
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "Invalid banner ID. Use /clanhologram list to see valid IDs.");
                }
                return true;
                
            case "list":
                // List all banners belonging to the player's clan
                List<ClanBanner> clanBanners = bannerManager.getClanBanners(clan.getName());
                
                if (clanBanners.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "Your clan has no holographic banners.");
                    player.sendMessage(ChatColor.YELLOW + "Create one with " + ChatColor.WHITE + "/clanhologram create");
                } else {
                    player.sendMessage(ChatColor.GREEN + "=== " + clan.getName() + "'s Holographic Banners ===");
                    for (ClanBanner b : clanBanners) {
                        String location = String.format("%.1f, %.1f, %.1f", 
                                                       b.getLocation().getX(), 
                                                       b.getLocation().getY(), 
                                                       b.getLocation().getZ());
                        
                        String visibility = b.isVisible() ? ChatColor.GREEN + "Visible" : ChatColor.RED + "Hidden";
                        String contentPreview = b.getContent().isEmpty() ? "<empty>" : 
                                               b.getContent().get(0) + (b.getContent().size() > 1 ? "..." : "");
                        
                        player.sendMessage(ChatColor.GOLD + b.getId().toString().substring(0, 8) + "... - " + 
                                          visibility + ChatColor.YELLOW + " @ " + location);
                        player.sendMessage(ChatColor.GRAY + "  Content: " + contentPreview);
                    }
                }
                return true;
                
            case "info":
                // Show detailed info about a banner
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /clanhologram info <banner_id>");
                    return true;
                }
                
                try {
                    UUID bannerId = UUID.fromString(args[1]);
                    ClanBanner b = bannerManager.getBanner(bannerId);
                    
                    if (b == null || !b.getClanName().equals(clan.getName())) {
                        player.sendMessage(ChatColor.RED + "Banner not found or doesn't belong to your clan.");
                        return true;
                    }
                    
                    player.sendMessage(ChatColor.GREEN + "=== Banner Info ===");
                    player.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + b.getId());
                    player.sendMessage(ChatColor.YELLOW + "Clan: " + ChatColor.WHITE + b.getClanName());
                    player.sendMessage(ChatColor.YELLOW + "Visible: " + 
                                      (b.isVisible() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
                    player.sendMessage(ChatColor.YELLOW + "Permission: " + ChatColor.WHITE + b.getPermission());
                    player.sendMessage(ChatColor.YELLOW + "Location: " + ChatColor.WHITE + 
                                      String.format("%.1f, %.1f, %.1f", 
                                                  b.getLocation().getX(), 
                                                  b.getLocation().getY(), 
                                                  b.getLocation().getZ()));
                    
                    player.sendMessage(ChatColor.YELLOW + "Content:");
                    if (b.getContent().isEmpty()) {
                        player.sendMessage(ChatColor.GRAY + "  <empty>");
                    } else {
                        for (String line : b.getContent()) {
                            player.sendMessage(ChatColor.GRAY + "  " + line);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "Invalid banner ID. Use /clanhologram list to see valid IDs.");
                }
                return true;
                
            case "permission":
                // Change who can edit a banner
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /clanhologram permission <banner_id> <permission>");
                    player.sendMessage(ChatColor.YELLOW + "Available permissions:");
                    for (ClanBanner.BannerPermission permOption : ClanBanner.BannerPermission.values()) {
                        player.sendMessage(permOption.getDisplayName() + ChatColor.GRAY + " - " + permOption.getDescription());
                    }
                    return true;
                }
                
                try {
                    UUID bannerId = UUID.fromString(args[1]);
                    String permissionStr = args[2].toUpperCase();
                    
                    // Get the banner first
                    ClanBanner targetBanner = bannerManager.getBanner(bannerId);
                    if (targetBanner == null) {
                        player.sendMessage(ChatColor.RED + "Banner not found!");
                        return true;
                    }
                    
                    // Check if player has admin permission
                    if (!bannerManager.hasPermission(player, targetBanner, "edit") && 
                        !player.hasPermission("clan.admin.banner")) {
                        player.sendMessage(ChatColor.RED + "You don't have permission to update this banner's permissions!");
                        return true;
                    }
                    
                    // Parse permission enum
                    ClanBanner.BannerPermission permission;
                    try {
                        permission = ClanBanner.BannerPermission.valueOf(permissionStr);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(ChatColor.RED + "Invalid permission: " + permissionStr);
                        player.sendMessage(ChatColor.YELLOW + "Available permissions:");
                        for (ClanBanner.BannerPermission permValue : ClanBanner.BannerPermission.values()) {
                            player.sendMessage(permValue.getDisplayName() + ChatColor.GRAY + " - " + permValue.getDescription());
                        }
                        return true;
                    }
                    
                    boolean updated = bannerManager.updateBannerPermission(player, bannerId, permissionStr);
                    
                    if (updated) {
                        player.sendMessage(ChatColor.GREEN + "Banner permission updated to " + permission.getDisplayName() + ChatColor.GREEN + "!");
                    }
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "Invalid banner ID.");
                }
                return true;
                
            case "help":
            default:
                return showHelp(player);
        }
    }
    
    /**
     * Shows the help menu for the command.
     * 
     * @param player The player to show help to
     * @return Always true
     */
    private boolean showHelp(Player player) {
        player.sendMessage(ChatColor.GREEN + "=== Clan Holographic Banner Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/clanhologram create" + ChatColor.WHITE + " - Create a new holographic banner");
        player.sendMessage(ChatColor.YELLOW + "/clanhologram delete <id>" + ChatColor.WHITE + " - Delete a banner");
        player.sendMessage(ChatColor.YELLOW + "/clanhologram move <id>" + ChatColor.WHITE + " - Move a banner to your location");
        player.sendMessage(ChatColor.YELLOW + "/clanhologram content <id> <text>" + ChatColor.WHITE + " - Set banner text");
        player.sendMessage(ChatColor.YELLOW + "/clanhologram style <id> <style>" + ChatColor.WHITE + " - Change banner style");
        player.sendMessage(ChatColor.YELLOW + "/clanhologram toggle <id>" + ChatColor.WHITE + " - Toggle banner visibility");
        player.sendMessage(ChatColor.YELLOW + "/clanhologram list" + ChatColor.WHITE + " - List all banners");
        player.sendMessage(ChatColor.YELLOW + "/clanhologram info <id>" + ChatColor.WHITE + " - Show banner details");
        player.sendMessage(ChatColor.YELLOW + "/clanhologram permission <id> <perm>" + ChatColor.WHITE + " - Change who can edit");
        player.sendMessage(ChatColor.YELLOW + "/clanhologram help" + ChatColor.WHITE + " - Show this help menu");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        
        Player player = (Player) sender;
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        if (clan == null) {
            return Collections.emptyList();
        }
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("create", "delete", "move", "content", "style", 
                                                    "toggle", "list", "info", "permission", "help");
            
            return subCommands.stream()
                   .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                   .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            // For commands that require a banner ID
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("delete") || subCommand.equals("move") || 
                subCommand.equals("content") || subCommand.equals("style") || 
                subCommand.equals("toggle") || subCommand.equals("info") || 
                subCommand.equals("permission")) {
                
                // Get banner IDs for this clan
                BannerManager bannerManager = plugin.getBannerManager();
                List<UUID> bannerIds = bannerManager.getClanBannerIds(clan.getName());
                List<String> idStrings = new ArrayList<>();
                
                for (UUID id : bannerIds) {
                    String idStr = id.toString();
                    if (idStr.startsWith(args[1])) {
                        idStrings.add(idStr);
                    }
                }
                
                return idStrings;
            }
        }
        
        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("style")) {
                List<String> styles = Arrays.asList("DEFAULT", "MINIMALIST", "ELEGANT", "WARRIOR", "ROYAL", "MYSTICAL", "NATURE");
                return styles.stream()
                       .filter(style -> style.startsWith(args[2].toUpperCase()))
                       .collect(Collectors.toList());
            }
            
            if (subCommand.equals("permission")) {
                List<String> permissions = Arrays.stream(ClanBanner.BannerPermission.values())
                                         .map(ClanBanner.BannerPermission::name)
                                         .collect(Collectors.toList());
                return permissions.stream()
                       .filter(perm -> perm.startsWith(args[2].toUpperCase()))
                       .collect(Collectors.toList());
            }
        }
        
        return Collections.emptyList();
    }
}
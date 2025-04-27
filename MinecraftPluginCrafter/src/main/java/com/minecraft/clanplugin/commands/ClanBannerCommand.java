package com.minecraft.clanplugin.commands;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.examples.ClanBannerExample;
import com.minecraft.clanplugin.models.Clan;
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
 * Command for getting clan banners.
 */
public class ClanBannerCommand implements CommandExecutor, TabCompleter {

    private final ClanPlugin plugin;
    private final ClanBannerExample bannerExample;
    
    /**
     * Creates a new clan banner command.
     * 
     * @param plugin The clan plugin instance
     */
    public ClanBannerCommand(ClanPlugin plugin) {
        this.plugin = plugin;
        this.bannerExample = new ClanBannerExample(plugin);
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
        
        // Default to basic if no style specified
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Available banner styles: " + 
                              ChatColor.GOLD + "BASIC, SHIELD, FLAG, STANDARD, WAVE, EMBLEM");
            player.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/clanbanner <style>");
            return true;
        }
        
        // Handle different styles
        String styleArg = args[0].toUpperCase();
        if (styleArg.equals("BASIC")) {
            giveBasicBanner(player, clan);
            return true;
        }
        
        // Try to parse as enum
        try {
            ClanBannerExample.BannerStyle style = ClanBannerExample.BannerStyle.valueOf(styleArg);
            bannerExample.giveClanBanner(player, style);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid banner style: " + styleArg);
            player.sendMessage(ChatColor.YELLOW + "Available styles: " + 
                              ChatColor.GOLD + "BASIC, SHIELD, FLAG, STANDARD, WAVE, EMBLEM");
        }
        
        return true;
    }
    
    /**
     * Gives a player a basic clan banner.
     * 
     * @param player The player to give the banner to
     * @param clan The player's clan
     */
    private void giveBasicBanner(Player player, Clan clan) {
        player.getInventory().addItem(bannerExample.createBasicClanBanner(clan));
        player.sendMessage(ChatColor.GREEN + "You have received a basic banner for your clan!");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> styles = new ArrayList<>();
            styles.add("BASIC");
            styles.addAll(Arrays.stream(ClanBannerExample.BannerStyle.values())
                         .map(Enum::name)
                         .collect(Collectors.toList()));
            
            return styles.stream()
                   .filter(style -> style.startsWith(args[0].toUpperCase()))
                   .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
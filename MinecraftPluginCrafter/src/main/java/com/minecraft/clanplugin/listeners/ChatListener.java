package com.minecraft.clanplugin.listeners;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Listener for chat events to display clan tags.
 */
public class ChatListener implements Listener {

    private final ClanPlugin plugin;

    public ChatListener(ClanPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        if (clan != null) {
            ClanMember member = clan.getMember(player.getUniqueId());
            String clanTag = clan.getTag();
            
            // Display clan tag before player name
            String format = event.getFormat();
            format = ChatColor.GRAY + "[" + ChatColor.GOLD + clanTag + ChatColor.GRAY + "] " + format;
            
            event.setFormat(format);
        }
    }
}

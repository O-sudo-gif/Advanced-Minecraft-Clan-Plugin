package com.minecraft.clanplugin.utils;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.achievements.Achievement;
import com.minecraft.clanplugin.achievements.AchievementDifficulty;
import com.minecraft.clanplugin.models.Clan;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.Particle;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling visual animations in the plugin.
 */
public class AnimationUtils {
    
    private static ClanPlugin plugin;
    
    /**
     * Initializes the animation utils with the plugin instance.
     * 
     * @param pluginInstance The clan plugin instance
     */
    public static void init(ClanPlugin pluginInstance) {
        plugin = pluginInstance;
    }
    
    /**
     * Plays an achievement celebration animation for all online clan members.
     * 
     * @param clan The clan that earned the achievement
     * @param achievement The achievement that was earned
     */
    public static void playClanAchievementAnimation(Clan clan, Achievement achievement) {
        // Get the clan color for firework effects
        Color fireworkColor = parseColor(clan.getColor());
        
        // Send messages to all clan members
        for (Player player : getOnlineClanMembers(clan)) {
            // Play sound effect
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            
            // Show achievement message with animation
            showAnimatedTitle(player, achievement);
            
            // Launch celebration fireworks
            launchFirework(player.getLocation(), fireworkColor, achievement.getDifficulty().getPoints());
        }
        
        // Broadcast achievement to server if it's a significant one
        if (achievement.getDifficulty() == AchievementDifficulty.LEGENDARY || 
            achievement.getDifficulty() == AchievementDifficulty.EPIC) {
            
            Bukkit.broadcastMessage(
                ChatColor.GOLD + "✦ " + ChatColor.WHITE + "Clan " + 
                ChatColor.valueOf(clan.getColor().toUpperCase()) + clan.getName() + 
                ChatColor.WHITE + " has earned the " + 
                achievement.getDifficulty().getColor() + achievement.getName() + 
                ChatColor.WHITE + " achievement!"
            );
        }
    }
    
    /**
     * Shows an animated title and subtitle to the player for an achievement.
     * 
     * @param player The player to show the title to
     * @param achievement The achievement to display
     */
    private static void showAnimatedTitle(Player player, Achievement achievement) {
        final String achievementName = achievement.getName();
        final ChatColor difficultyColor = achievement.getDifficulty().getColor();
        
        // Schedule a sequence of changing titles
        new BukkitRunnable() {
            private int step = 0;
            
            @Override
            public void run() {
                if (step == 0) {
                    // First display
                    player.sendTitle(
                        ChatColor.YELLOW + "⭐ Achievement Unlocked ⭐",
                        difficultyColor + achievementName,
                        10, 40, 10
                    );
                    
                    // Particles around the player
                    player.getWorld().spawnParticle(
                        Particle.VILLAGER_HAPPY, 
                        player.getLocation().add(0, 1, 0), 
                        30, 0.5, 0.5, 0.5, 0.1
                    );
                } 
                else if (step == 1) {
                    // Second display with more emphasis
                    player.sendTitle(
                        ChatColor.GOLD + "⭐ Achievement Unlocked ⭐",
                        difficultyColor + "» " + achievementName + " «",
                        0, 30, 10
                    );
                }
                else {
                    // Final display with achievement description
                    player.sendTitle(
                        difficultyColor + achievementName,
                        ChatColor.WHITE + achievement.getDescription(),
                        0, 50, 20
                    );
                    
                    // Send details in chat
                    player.sendMessage("");
                    player.sendMessage(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                    player.sendMessage(ChatColor.GOLD + "✦ Achievement Unlocked: " + difficultyColor + achievementName);
                    player.sendMessage(ChatColor.WHITE + achievement.getDescription());
                    
                    if (achievement.getReward() != null && !achievement.getReward().isEmpty()) {
                        player.sendMessage(ChatColor.YELLOW + "Reward: " + ChatColor.WHITE + achievement.getReward());
                    }
                    
                    player.sendMessage(ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                    player.sendMessage("");
                    
                    // Cancel the repeating task
                    this.cancel();
                }
                
                step++;
            }
        }.runTaskTimer(plugin, 0L, 25L); // Run every 1.25 seconds
    }
    
    /**
     * Launches a celebration firework at the given location.
     * 
     * @param location The location to launch the firework
     * @param color The color of the firework
     * @param power The power (height) of the firework, 1-5
     */
    private static void launchFirework(Location location, Color color, int power) {
        // Clamp power between 1 and 5
        int fireworkPower = Math.min(Math.max(power, 1), 5);
        
        // Create random offset for aesthetics
        Location launchLoc = location.clone().add(
            Math.random() * 2 - 1, 
            0, 
            Math.random() * 2 - 1
        );
        
        // Spawn the firework and set properties
        Firework firework = (Firework) location.getWorld().spawnEntity(launchLoc, EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();
        
        // Random firework types
        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        FireworkEffect.Type type = types[(int) (Math.random() * types.length)];
        
        // Create firework effect with clan colors
        FireworkEffect effect = FireworkEffect.builder()
            .withColor(color)
            .withFade(Color.WHITE)
            .with(type)
            .trail(true)
            .flicker(Math.random() > 0.5)
            .build();
        
        meta.addEffect(effect);
        meta.setPower(fireworkPower);
        firework.setFireworkMeta(meta);
    }
    
    /**
     * Gets all online players that are members of the specified clan.
     * 
     * @param clan The clan to get online members for
     * @return A list of online clan members
     */
    private static List<Player> getOnlineClanMembers(Clan clan) {
        List<Player> onlineMembers = new ArrayList<>();
        
        clan.getMemberIds().forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                onlineMembers.add(player);
            }
        });
        
        return onlineMembers;
    }
    
    /**
     * Parses a ChatColor string into a Bukkit Color for fireworks.
     * 
     * @param colorStr The color string to parse
     * @return The Bukkit Color object
     */
    private static Color parseColor(String colorStr) {
        try {
            ChatColor chatColor = ChatColor.valueOf(colorStr.toUpperCase().replace("§", ""));
            
            switch (chatColor) {
                case BLACK: return Color.BLACK;
                case DARK_BLUE: return Color.NAVY;
                case DARK_GREEN: return Color.GREEN;
                case DARK_AQUA: return Color.TEAL;
                case DARK_RED: return Color.MAROON;
                case DARK_PURPLE: return Color.PURPLE;
                case GOLD: return Color.ORANGE;
                case GRAY: return Color.SILVER;
                case DARK_GRAY: return Color.GRAY;
                case BLUE: return Color.BLUE;
                case GREEN: return Color.LIME;
                case AQUA: return Color.AQUA;
                case RED: return Color.RED;
                case LIGHT_PURPLE: return Color.FUCHSIA;
                case YELLOW: return Color.YELLOW;
                case WHITE: return Color.WHITE;
                default: return Color.WHITE;
            }
        } catch (Exception e) {
            // Default to gold if parsing fails
            return Color.ORANGE;
        }
    }
}
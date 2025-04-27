package com.minecraft.clanplugin.visualization;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.Territory;
import com.minecraft.clanplugin.utils.AnimationUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Creates animated visualization effects for territory conquest.
 */
public class TerritoryConquestVisualizer {

    private final ClanPlugin plugin;
    private final Map<UUID, ConquestAnimation> activeAnimations;
    private final Map<String, Long> territoryCooldowns;
    
    /**
     * Creates a new territory conquest visualizer.
     * 
     * @param plugin The plugin instance
     */
    public TerritoryConquestVisualizer(ClanPlugin plugin) {
        this.plugin = plugin;
        this.activeAnimations = new HashMap<>();
        this.territoryCooldowns = new HashMap<>();
    }
    
    /**
     * The difficulty level of a territory conquest.
     * Affects animation style and duration.
     */
    public enum ConquestDifficulty {
        EASY(1.0, 5, Color.GREEN, "easy", Sound.BLOCK_NOTE_BLOCK_PLING),
        MEDIUM(1.5, 8, Color.YELLOW, "challenging", Sound.BLOCK_NOTE_BLOCK_CHIME),
        HARD(2.0, 12, Color.RED, "difficult", Sound.BLOCK_NOTE_BLOCK_BELL),
        EPIC(3.0, 15, Color.PURPLE, "epic", Sound.ENTITY_ENDER_DRAGON_GROWL);
        
        private final double durationMultiplier;
        private final int particleIntensity;
        private final Color color;
        private final String descriptor;
        private final Sound sound;
        
        ConquestDifficulty(double durationMultiplier, int particleIntensity, Color color, String descriptor, Sound sound) {
            this.durationMultiplier = durationMultiplier;
            this.particleIntensity = particleIntensity;
            this.color = color;
            this.descriptor = descriptor;
            this.sound = sound;
        }
        
        public double getDurationMultiplier() {
            return durationMultiplier;
        }
        
        public int getParticleIntensity() {
            return particleIntensity;
        }
        
        public Color getColor() {
            return color;
        }
        
        public String getDescriptor() {
            return descriptor;
        }
        
        public Sound getSound() {
            return sound;
        }
    }
    
    /**
     * Start a territory conquest animation for all players in a clan and nearby.
     * 
     * @param conqueredTerritory The territory being conquered
     * @param conqueringClan The clan taking control of the territory
     * @param previousClan The clan that previously controlled the territory, if any
     * @param difficulty The difficulty of the conquest
     */
    public void startConquestAnimation(Territory conqueredTerritory, Clan conqueringClan, 
                                      Clan previousClan, ConquestDifficulty difficulty) {
        String territoryId = conqueredTerritory.getId();
        
        // Check if territory is on cooldown
        if (territoryCooldowns.containsKey(territoryId)) {
            long cooldownEnd = territoryCooldowns.get(territoryId);
            if (cooldownEnd > System.currentTimeMillis()) {
                return; // Still on cooldown
            }
        }
        
        // Set cooldown for this territory (2 minutes)
        territoryCooldowns.put(territoryId, System.currentTimeMillis() + (2 * 60 * 1000));
        
        // Get territory center location
        Location center = conqueredTerritory.getCenter();
        if (center == null || center.getWorld() == null) {
            return; // Can't visualize without a valid location
        }
        
        // Create the conquest animation
        ConquestAnimation animation = new ConquestAnimation(
            conqueredTerritory, conqueringClan, previousClan, difficulty, center);
        
        // Get all players to show the animation to
        Set<Player> viewers = new HashSet<>();
        
        // Add members of the conquering clan
        for (ClanMember clanMember : conqueringClan.getMembers()) {
            UUID memberId = clanMember.getPlayerUUID();
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                viewers.add(member);
            }
        }
        
        // Add members of the previous clan if any
        if (previousClan != null) {
            for (ClanMember clanMember : previousClan.getMembers()) {
                UUID memberId = clanMember.getPlayerUUID();
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    viewers.add(member);
                }
            }
        }
        
        // Add nearby players (within 200 blocks)
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == center.getWorld() && 
                player.getLocation().distance(center) <= 200) {
                viewers.add(player);
            }
        }
        
        // Start the animation for all viewers
        for (Player viewer : viewers) {
            startAnimationForPlayer(viewer, animation);
        }
        
        // Broadcast conquest message to all players
        String broadcastMessage;
        if (previousClan == null) {
            broadcastMessage = ChatColor.GOLD + conqueringClan.getName() + ChatColor.YELLOW + 
                " has claimed " + ChatColor.WHITE + conqueredTerritory.getName() + ChatColor.YELLOW + "!";
        } else {
            broadcastMessage = ChatColor.GOLD + conqueringClan.getName() + ChatColor.YELLOW + 
                " has conquered " + ChatColor.WHITE + conqueredTerritory.getName() + ChatColor.YELLOW +
                " from " + ChatColor.RED + previousClan.getName() + ChatColor.YELLOW + "!";
        }
        
        if (previousClan != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(broadcastMessage);
                
                // Play sound for all players, volume based on proximity
                double distanceToTerritory = (player.getLocation().getWorld() == center.getWorld()) ? 
                    player.getLocation().distance(center) : Double.MAX_VALUE;
                
                if (distanceToTerritory < 1000) {
                    float volume = (float) (1.0f - (distanceToTerritory / 1000.0f));
                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, volume * 0.5f, 0.5f);
                }
            }
        }
        
        // If this is a webhook-worthy conquest, report it
        if (previousClan != null || difficulty.ordinal() >= ConquestDifficulty.HARD.ordinal()) {
            plugin.getWebhookManager().sendTerritoryConquestWebhook(
                conqueringClan, previousClan, conqueredTerritory, difficulty);
        }
    }
    
    /**
     * Start a conquest animation for a specific player.
     * 
     * @param player The player to show the animation to
     * @param animation The animation to display
     */
    private void startAnimationForPlayer(Player player, ConquestAnimation animation) {
        // Cancel any existing animation for this player
        if (activeAnimations.containsKey(player.getUniqueId())) {
            activeAnimations.get(player.getUniqueId()).cancel();
        }
        
        // Copy the animation for this player
        ConquestAnimation playerAnimation = animation.copy();
        activeAnimations.put(player.getUniqueId(), playerAnimation);
        
        // Start the animation
        playerAnimation.start(player);
    }
    
    /**
     * Stop all animations for a player.
     * 
     * @param player The player to stop animations for
     */
    public void stopAnimations(Player player) {
        if (activeAnimations.containsKey(player.getUniqueId())) {
            activeAnimations.get(player.getUniqueId()).cancel();
            activeAnimations.remove(player.getUniqueId());
        }
    }
    
    /**
     * Get a circular array of points around a center.
     * 
     * @param center The center point
     * @param radius The radius of the circle
     * @param numPoints The number of points to generate
     * @return Array of locations forming a circle
     */
    private Location[] getCirclePoints(Location center, double radius, int numPoints) {
        World world = center.getWorld();
        Location[] points = new Location[numPoints];
        
        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            
            // Find suitable Y coordinate (first solid block from top)
            int y = center.getBlockY();
            
            // Use the same Y coordinate for all points for simplicity
            points[i] = new Location(world, x, y, z);
        }
        
        return points;
    }
    
    /**
     * Represents an ongoing conquest animation.
     */
    private class ConquestAnimation {
        private final Territory territory;
        private final Clan conqueringClan;
        private final Clan previousClan;
        private final ConquestDifficulty difficulty;
        private final Location center;
        private final List<BukkitRunnable> tasks;
        private boolean cancelled;
        private final Random random;
        
        /**
         * Create a new conquest animation.
         * 
         * @param territory The territory being conquered
         * @param conqueringClan The conquering clan
         * @param previousClan The previous owner, if any
         * @param difficulty The conquest difficulty
         * @param center The center location of the territory
         */
        public ConquestAnimation(Territory territory, Clan conqueringClan, Clan previousClan,
                               ConquestDifficulty difficulty, Location center) {
            this.territory = territory;
            this.conqueringClan = conqueringClan;
            this.previousClan = previousClan;
            this.difficulty = difficulty;
            this.center = center;
            this.tasks = new ArrayList<>();
            this.cancelled = false;
            this.random = new Random();
        }
        
        /**
         * Start the animation for a player.
         * 
         * @param player The player to show the animation to
         */
        public void start(Player player) {
            // Send initial message
            String conquestMessage;
            if (previousClan == null) {
                conquestMessage = ChatColor.GOLD + "Your clan has claimed " + ChatColor.WHITE + 
                    territory.getName() + ChatColor.GOLD + "!";
            } else {
                conquestMessage = ChatColor.GOLD + "Your clan has conquered " + ChatColor.WHITE + 
                    territory.getName() + ChatColor.GOLD + " from " + ChatColor.RED + 
                    previousClan.getName() + ChatColor.GOLD + "!";
            }
            
            // Modify message based on player's clan
            if (player.getUniqueId() != null) {
                Clan playerClan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
                if (playerClan != null) {
                    if (playerClan.equals(previousClan)) {
                        conquestMessage = ChatColor.RED + "Your clan has lost " + ChatColor.WHITE + 
                            territory.getName() + ChatColor.RED + " to " + ChatColor.GOLD + 
                            conqueringClan.getName() + ChatColor.RED + "!";
                    } else if (!playerClan.equals(conqueringClan)) {
                        if (previousClan == null) {
                            conquestMessage = ChatColor.YELLOW + "Clan " + ChatColor.GOLD + 
                                conqueringClan.getName() + ChatColor.YELLOW + " has claimed " + 
                                ChatColor.WHITE + territory.getName() + ChatColor.YELLOW + "!";
                        } else {
                            conquestMessage = ChatColor.YELLOW + "Clan " + ChatColor.GOLD + 
                                conqueringClan.getName() + ChatColor.YELLOW + " has conquered " + 
                                ChatColor.WHITE + territory.getName() + ChatColor.YELLOW + " from " + 
                                ChatColor.RED + previousClan.getName() + ChatColor.YELLOW + "!";
                        }
                    }
                }
            }
            
            player.sendMessage(conquestMessage);
            
            // Play appropriate sound
            player.playSound(player.getLocation(), difficulty.getSound(), 1.0f, 1.0f);
            
            // Start visual effects
            startBeamEffect(player);
            startParticleEffects(player);
            startGroundEffect(player);
            
            // Additional effect for higher difficulty conquests
            if (difficulty.ordinal() >= ConquestDifficulty.HARD.ordinal()) {
                startSkyEffect(player);
            }
        }
        
        /**
         * Create a copy of this animation.
         * 
         * @return A copy of the animation
         */
        public ConquestAnimation copy() {
            return new ConquestAnimation(territory, conqueringClan, previousClan, difficulty, center);
        }
        
        /**
         * Start the central beam effect.
         * 
         * @param player The player to show the effect to
         */
        private void startBeamEffect(Player player) {
            String colorStr = conqueringClan.getColor();
            ChatColor clanColor = getChatColorFromString(colorStr);
            Color particleColor = getColorFromChatColor(clanColor);
            
            // Create a central beam effect
            BukkitRunnable beamTask = new BukkitRunnable() {
                private double height = 0;
                private final double maxHeight = 120;
                private final double heightIncrement = 2;
                
                @Override
                public void run() {
                    if (cancelled || !player.isOnline() || height >= maxHeight) {
                        cancel();
                        tasks.remove(this);
                        return;
                    }
                    
                    // Create beam effect
                    Location beamLoc = center.clone().add(0, height, 0);
                    
                    // Create a ring of particles
                    double radius = 2.0 + (height / 30.0);
                    for (int i = 0; i < 10; i++) {
                        double angle = 2 * Math.PI * i / 10;
                        double x = radius * Math.cos(angle);
                        double z = radius * Math.sin(angle);
                        
                        Location particleLoc = beamLoc.clone().add(x, 0, z);
                        player.spawnParticle(Particle.REDSTONE, particleLoc, 1, 
                            new Particle.DustOptions(particleColor, 2.0f));
                    }
                    
                    // Create core beam
                    player.spawnParticle(Particle.END_ROD, beamLoc, 2, 0, 0, 0, 0.05);
                    
                    // Play sound occasionally
                    if (height % 10 == 0) {
                        player.playSound(beamLoc, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 2.0f);
                    }
                    
                    height += heightIncrement;
                }
            };
            
            beamTask.runTaskTimer(plugin, 0L, 2L);
            tasks.add(beamTask);
        }
        
        /**
         * Start particle effects around the territory.
         * 
         * @param player The player to show the effect to
         */
        private void startParticleEffects(Player player) {
            String colorStr = conqueringClan.getColor();
            ChatColor clanColor = getChatColorFromString(colorStr);
            Color particleColor = getColorFromChatColor(clanColor);
            
            // Get territory bounds
            double size = territory.getSize();
            double radius = Math.sqrt(size / Math.PI); // Approximate circular radius
            
            // Create a spiral effect
            BukkitRunnable spiralTask = new BukkitRunnable() {
                private double angle = 0;
                private final double maxAngle = 8 * Math.PI;
                private final double angleIncrement = Math.PI / 16;
                
                @Override
                public void run() {
                    if (cancelled || !player.isOnline() || angle >= maxAngle) {
                        cancel();
                        tasks.remove(this);
                        return;
                    }
                    
                    // Create an expanding spiral effect
                    double currentRadius = (angle / maxAngle) * radius;
                    double x = center.getX() + currentRadius * Math.cos(angle);
                    double z = center.getZ() + currentRadius * Math.sin(angle);
                    
                    Location particleLoc = new Location(center.getWorld(), x, center.getY() + 1, z);
                    
                    // Spawn particles
                    player.spawnParticle(Particle.REDSTONE, particleLoc, 5, 0.5, 0.5, 0.5, 
                        new Particle.DustOptions(particleColor, 1.5f));
                    
                    // Play sound occasionally
                    if (random.nextInt(10) == 0) {
                        player.playSound(particleLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.2f, 1.0f + (float)(angle / maxAngle));
                    }
                    
                    angle += angleIncrement;
                }
            };
            
            spiralTask.runTaskTimer(plugin, 0L, 1L);
            tasks.add(spiralTask);
            
            // Create boundary particles
            BukkitRunnable boundaryTask = new BukkitRunnable() {
                private int tick = 0;
                private final int maxTicks = 200;
                
                @Override
                public void run() {
                    if (cancelled || !player.isOnline() || tick >= maxTicks) {
                        cancel();
                        tasks.remove(this);
                        return;
                    }
                    
                    // Create particles at the territory boundary
                    Location[] circlePoints = getCirclePoints(center, radius, 20);
                    for (Location point : circlePoints) {
                        if (random.nextInt(3) == 0) {
                            player.spawnParticle(Particle.REDSTONE, point, 1, 0.2, 0.5, 0.2,
                                new Particle.DustOptions(particleColor, 1.0f));
                        }
                    }
                    
                    tick++;
                }
            };
            
            boundaryTask.runTaskTimer(plugin, 10L, 2L);
            tasks.add(boundaryTask);
        }
        
        /**
         * Start ground effect animation.
         * 
         * @param player The player to show the effect to
         */
        private void startGroundEffect(Player player) {
            String colorStr = conqueringClan.getColor();
            ChatColor clanColor = ChatColor.valueOf(colorStr);
            Color particleColor = getColorFromChatColor(clanColor);
            
            // Calculate the area to affect
            double size = territory.getSize();
            double radius = Math.sqrt(size / Math.PI); // Approximate circular radius
            
            BukkitRunnable groundTask = new BukkitRunnable() {
                private double currentRadius = 0;
                private final double radiusIncrement = radius / 20;
                
                @Override
                public void run() {
                    if (cancelled || !player.isOnline() || currentRadius >= radius) {
                        cancel();
                        tasks.remove(this);
                        return;
                    }
                    
                    // Create circle of particles on the ground
                    Location[] circlePoints = getCirclePoints(center, currentRadius, 30);
                    for (Location point : circlePoints) {
                        // Find ground level
                        Location groundPoint = findGroundLocation(point);
                        if (groundPoint != null) {
                            player.spawnParticle(Particle.REDSTONE, 
                                groundPoint.clone().add(0, 0.2, 0), 2, 0.1, 0.1, 0.1,
                                new Particle.DustOptions(particleColor, 1.0f));
                        }
                    }
                    
                    // Play sound
                    if (random.nextInt(3) == 0) {
                        player.playSound(center, Sound.BLOCK_GRASS_STEP, 0.5f, 0.8f);
                    }
                    
                    currentRadius += radiusIncrement;
                }
            };
            
            groundTask.runTaskTimer(plugin, 20L, 5L);
            tasks.add(groundTask);
        }
        
        /**
         * Start sky effect animation.
         * 
         * @param player The player to show the effect to
         */
        private void startSkyEffect(Player player) {
            BukkitRunnable skyTask = new BukkitRunnable() {
                private int tick = 0;
                private final int duration = 60;
                
                @Override
                public void run() {
                    if (cancelled || !player.isOnline() || tick >= duration) {
                        cancel();
                        tasks.remove(this);
                        return;
                    }
                    
                    // Random lightning effects in the sky
                    if (random.nextInt(10) == 0) {
                        double offsetX = (random.nextDouble() - 0.5) * 40;
                        double offsetZ = (random.nextDouble() - 0.5) * 40;
                        Location lightningLoc = center.clone().add(offsetX, 0, offsetZ);
                        
                        // Just visual effect, no damage
                        player.playSound(lightningLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.2f);
                        player.spawnParticle(Particle.FLASH, lightningLoc, 1, 0.5, 5, 0.5, 0);
                    }
                    
                    tick++;
                }
            };
            
            skyTask.runTaskTimer(plugin, 10L, 2L);
            tasks.add(skyTask);
        }
        
        /**
         * Cancel all animation tasks.
         */
        public void cancel() {
            cancelled = true;
            for (BukkitRunnable task : tasks) {
                task.cancel();
            }
            tasks.clear();
        }
    }
    
    /**
     * Find the ground location below a given location.
     * 
     * @param location The location to start from
     * @return The ground location or null if not found
     */
    private Location findGroundLocation(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        
        // Search from 10 blocks above to 10 blocks below
        int startY = Math.min(location.getBlockY() + 10, world.getMaxHeight() - 1);
        
        for (int y = startY; y > 0; y--) {
            Block block = world.getBlockAt(x, y, z);
            Block blockAbove = world.getBlockAt(x, y + 1, z);
            
            if (!block.isEmpty() && blockAbove.isEmpty()) {
                return new Location(world, x + 0.5, y + 1, z + 0.5);
            }
        }
        
        return null;
    }
    
    /**
     * Convert a String color to ChatColor.
     * 
     * @param colorStr The color string
     * @return The corresponding ChatColor
     */
    private ChatColor getChatColorFromString(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return ChatColor.GOLD; // Default
        }
        
        // Try to parse as a ChatColor name
        try {
            // Remove formatting characters if present
            if (colorStr.startsWith("§") || colorStr.startsWith("&")) {
                colorStr = colorStr.substring(1);
            }
            
            // Convert single character code to full name
            switch (colorStr.toLowerCase()) {
                case "0": return ChatColor.BLACK;
                case "1": return ChatColor.DARK_BLUE;
                case "2": return ChatColor.DARK_GREEN;
                case "3": return ChatColor.DARK_AQUA;
                case "4": return ChatColor.DARK_RED;
                case "5": return ChatColor.DARK_PURPLE;
                case "6": return ChatColor.GOLD;
                case "7": return ChatColor.GRAY;
                case "8": return ChatColor.DARK_GRAY;
                case "9": return ChatColor.BLUE;
                case "a": return ChatColor.GREEN;
                case "b": return ChatColor.AQUA;
                case "c": return ChatColor.RED;
                case "d": return ChatColor.LIGHT_PURPLE;
                case "e": return ChatColor.YELLOW;
                case "f": return ChatColor.WHITE;
                default:
                    // Try to match the name
                    return ChatColor.valueOf(colorStr.toUpperCase());
            }
        } catch (Exception e) {
            return ChatColor.GOLD; // Default fallback
        }
    }
    
    /**
     * Convert ChatColor to particle Color.
     * 
     * @param chatColor The ChatColor to convert
     * @return The corresponding particle Color
     */
    private Color getColorFromChatColor(ChatColor chatColor) {
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
    }
}
package com.minecraft.clanplugin.recruitment;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the clan recruitment mini-game challenges and sessions.
 */
public class RecruitmentMiniGame {
    
    private final JavaPlugin plugin;
    private final Map<UUID, PlayerSession> activeSessions;
    private final List<RecruitmentChallenge> availableChallenges;
    
    /**
     * Creates a new RecruitmentMiniGame.
     * 
     * @param plugin The plugin instance
     */
    public RecruitmentMiniGame(JavaPlugin plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        this.availableChallenges = new ArrayList<>();
        
        // Initialize default challenges
        initializeChallenges();
    }
    
    /**
     * Initialize the default challenges.
     */
    private void initializeChallenges() {
        // Combat Challenges
        addChallenge(new RecruitmentChallenge(
            "combat_zombie", 
            "Zombie Slayer", 
            "Kill 5 zombies within 5 minutes",
            2, 
            RecruitmentChallenge.ChallengeType.COMBAT, 
            20
        ));
        
        addChallenge(new RecruitmentChallenge(
            "combat_skeleton", 
            "Skeleton Hunter", 
            "Kill 5 skeletons within 5 minutes",
            2, 
            RecruitmentChallenge.ChallengeType.COMBAT, 
            20
        ));
        
        // Gathering Challenges
        addChallenge(new RecruitmentChallenge(
            "gather_wood", 
            "Lumberjack", 
            "Collect 32 wood logs within 5 minutes",
            1, 
            RecruitmentChallenge.ChallengeType.GATHERING, 
            15
        ));
        
        addChallenge(new RecruitmentChallenge(
            "gather_stone", 
            "Stone Collector", 
            "Mine 32 stone blocks within 5 minutes",
            1, 
            RecruitmentChallenge.ChallengeType.GATHERING, 
            15
        ));
        
        // Building Challenges
        addChallenge(new RecruitmentChallenge(
            "build_tower", 
            "Tower Builder", 
            "Build a 10-block tall tower within 3 minutes",
            3, 
            RecruitmentChallenge.ChallengeType.BUILDING, 
            25
        ));
        
        // Exploration Challenges
        addChallenge(new RecruitmentChallenge(
            "explore_biomes", 
            "Explorer", 
            "Visit 3 different biomes within 10 minutes",
            4, 
            RecruitmentChallenge.ChallengeType.EXPLORATION, 
            30
        ));
        
        // Quiz Challenges
        addChallenge(new QuizChallenge(
            "quiz_minecraft", 
            "Minecraft Trivia", 
            "Answer 5 questions about Minecraft",
            3, 
            RecruitmentChallenge.ChallengeType.QUIZ, 
            25
        ));
        
        // Memory Challenge
        addChallenge(new MemoryChallenge(
            "memory_pattern", 
            "Memory Challenge", 
            "Memorize and repeat color patterns",
            3, 
            RecruitmentChallenge.ChallengeType.QUIZ, 
            20
        ));
        
        // Sequence Challenge
        addChallenge(new SequenceChallenge(
            "sequence_items", 
            "Item Sequence", 
            "Remember and recreate a sequence of items",
            4, 
            RecruitmentChallenge.ChallengeType.QUIZ, 
            30
        ));
    }
    
    /**
     * Start a recruitment session for a player.
     * 
     * @param player The player
     * @param requiredPoints The points needed to pass
     * @return True if the session was started
     */
    public boolean startSession(Player player, int requiredPoints) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            return false; // Already has an active session
        }
        
        // Create a new session
        PlayerSession session = new PlayerSession(player.getUniqueId(), requiredPoints);
        
        // Randomly select 3 challenges
        List<RecruitmentChallenge> selectedChallenges = selectRandomChallenges(3);
        session.setChallenges(selectedChallenges);
        
        activeSessions.put(player.getUniqueId(), session);
        
        // Notify the player
        player.sendMessage("§6You have started the clan recruitment challenge!");
        player.sendMessage("§eYou need §c" + requiredPoints + " points §eto pass the challenge.");
        player.sendMessage("§eType §c/clan challenge start §eto begin your first challenge.");
        
        return true;
    }
    
    /**
     * End a player's recruitment session.
     * 
     * @param playerUUID The player UUID
     * @param success Whether the session was successful
     * @return The total points earned, or -1 if no session was found
     */
    public int endSession(UUID playerUUID, boolean success) {
        PlayerSession session = activeSessions.get(playerUUID);
        if (session == null) {
            return -1;
        }
        
        int totalPoints = session.getTotalPoints();
        activeSessions.remove(playerUUID);
        
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            if (success) {
                player.sendMessage("§aCongratulations! You have completed the recruitment challenge.");
                player.sendMessage("§eYou earned §c" + totalPoints + " points §ein total.");
            } else {
                player.sendMessage("§cYou have abandoned the recruitment challenge.");
                player.sendMessage("§eYou earned §c" + totalPoints + " points §ebefore quitting.");
            }
        }
        
        return totalPoints;
    }
    
    /**
     * Get active challenges for a player.
     * 
     * @param playerUUID The player UUID
     * @return List of challenges, or empty list if no session
     */
    public List<RecruitmentChallenge> getPlayerChallenges(UUID playerUUID) {
        PlayerSession session = activeSessions.get(playerUUID);
        if (session == null) {
            return Collections.emptyList();
        }
        return session.getChallenges();
    }
    
    /**
     * Get the current challenge for a player.
     * 
     * @param playerUUID The player UUID
     * @return The current challenge, or null if no session or no current challenge
     */
    public RecruitmentChallenge getCurrentChallenge(UUID playerUUID) {
        PlayerSession session = activeSessions.get(playerUUID);
        if (session == null || session.getCurrentChallengeIndex() >= session.getChallenges().size()) {
            return null;
        }
        return session.getChallenges().get(session.getCurrentChallengeIndex());
    }
    
    /**
     * Check if a player has an active recruitment session.
     * 
     * @param playerUUID The player UUID
     * @return True if the player has an active session
     */
    public boolean hasActiveSession(UUID playerUUID) {
        return activeSessions.containsKey(playerUUID);
    }
    
    /**
     * Complete the current challenge for a player.
     * 
     * @param playerUUID The player UUID
     * @return True if the challenge was completed, false if no more challenges or no session
     */
    public boolean completeCurrentChallenge(UUID playerUUID) {
        PlayerSession session = activeSessions.get(playerUUID);
        if (session == null) {
            return false;
        }
        
        // Get current challenge and mark as completed
        int index = session.getCurrentChallengeIndex();
        if (index >= session.getChallenges().size()) {
            return false; // No more challenges
        }
        
        RecruitmentChallenge challenge = session.getChallenges().get(index);
        session.addPoints(challenge.getRewardPoints());
        session.setCurrentChallengeIndex(index + 1);
        
        // Check if all challenges are completed
        if (session.getCurrentChallengeIndex() >= session.getChallenges().size()) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                player.sendMessage("§aYou have completed all challenges!");
                if (session.getTotalPoints() >= session.getRequiredPoints()) {
                    player.sendMessage("§aYou have earned enough points to pass the recruitment test!");
                    player.sendMessage("§eFinal score: §c" + session.getTotalPoints() + "/" + session.getRequiredPoints());
                } else {
                    player.sendMessage("§cYou did not earn enough points to pass the recruitment test.");
                    player.sendMessage("§eFinal score: §c" + session.getTotalPoints() + "/" + session.getRequiredPoints());
                }
            }
        } else {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                player.sendMessage("§aChallenge completed! You earned §c" + challenge.getRewardPoints() + " points§a.");
                player.sendMessage("§eYour total is now §c" + session.getTotalPoints() + " points§e.");
                player.sendMessage("§eNext challenge: §c" + session.getChallenges().get(session.getCurrentChallengeIndex()).getName());
            }
        }
        
        return true;
    }
    
    /**
     * Skip the current challenge for a player.
     * 
     * @param playerUUID The player UUID
     * @return True if the challenge was skipped, false if no more challenges or no session
     */
    public boolean skipCurrentChallenge(UUID playerUUID) {
        PlayerSession session = activeSessions.get(playerUUID);
        if (session == null) {
            return false;
        }
        
        int index = session.getCurrentChallengeIndex();
        if (index >= session.getChallenges().size()) {
            return false; // No more challenges
        }
        
        session.setCurrentChallengeIndex(index + 1);
        
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            player.sendMessage("§eYou skipped a challenge. No points awarded.");
            if (session.getCurrentChallengeIndex() < session.getChallenges().size()) {
                player.sendMessage("§eNext challenge: §c" + session.getChallenges().get(session.getCurrentChallengeIndex()).getName());
            } else {
                player.sendMessage("§eThat was your last challenge.");
            }
        }
        
        return true;
    }
    
    /**
     * Check if a player has completed all challenges.
     * 
     * @param playerUUID The player UUID
     * @return True if all challenges are completed
     */
    public boolean hasCompletedAllChallenges(UUID playerUUID) {
        PlayerSession session = activeSessions.get(playerUUID);
        if (session == null) {
            return false;
        }
        
        return session.getCurrentChallengeIndex() >= session.getChallenges().size();
    }
    
    /**
     * Check if a player has earned enough points to pass.
     * 
     * @param playerUUID The player UUID
     * @return True if the player has enough points
     */
    public boolean hasPassedRecruitment(UUID playerUUID) {
        PlayerSession session = activeSessions.get(playerUUID);
        if (session == null) {
            return false;
        }
        
        return session.getTotalPoints() >= session.getRequiredPoints();
    }
    
    /**
     * Get the total points a player has earned.
     * 
     * @param playerUUID The player UUID
     * @return The total points, or 0 if no session
     */
    public int getPlayerPoints(UUID playerUUID) {
        PlayerSession session = activeSessions.get(playerUUID);
        if (session == null) {
            return 0;
        }
        
        return session.getTotalPoints();
    }
    
    /**
     * Get the required points for a player to pass.
     * 
     * @param playerUUID The player UUID
     * @return The required points, or 0 if no session
     */
    public int getRequiredPoints(UUID playerUUID) {
        PlayerSession session = activeSessions.get(playerUUID);
        if (session == null) {
            return 0;
        }
        
        return session.getRequiredPoints();
    }
    
    /**
     * Add a challenge to the available challenges.
     * 
     * @param challenge The challenge to add
     */
    public void addChallenge(RecruitmentChallenge challenge) {
        availableChallenges.add(challenge);
    }
    
    /**
     * Select random challenges from the available challenges.
     * 
     * @param count The number of challenges to select
     * @return List of selected challenges
     */
    private List<RecruitmentChallenge> selectRandomChallenges(int count) {
        List<RecruitmentChallenge> selected = new ArrayList<>();
        List<RecruitmentChallenge> available = new ArrayList<>(availableChallenges);
        
        // Ensure we don't try to select more challenges than available
        int selectCount = Math.min(count, available.size());
        
        Random random = new Random();
        for (int i = 0; i < selectCount; i++) {
            int index = random.nextInt(available.size());
            selected.add(available.remove(index));
        }
        
        return selected;
    }
    
    /**
     * Get all available challenges.
     * 
     * @return List of all available challenges
     */
    public List<RecruitmentChallenge> getAvailableChallenges() {
        return Collections.unmodifiableList(availableChallenges);
    }
    
    /**
     * Save mini-game data to persistent storage.
     * Currently a stub method for future implementation.
     */
    public void saveData() {
        // In the future, we would save challenge completions and statistics
        // For now, this is just a stub method
    }
    
    /**
     * Represents a player's recruitment session.
     */
    public class PlayerSession {
        private final UUID playerUUID;
        private final int requiredPoints;
        private List<RecruitmentChallenge> challenges;
        private int currentChallengeIndex;
        private int totalPoints;
        private BukkitTask timeoutTask;
        
        /**
         * Create a new player session.
         * 
         * @param playerUUID The player UUID
         * @param requiredPoints The points required to pass
         */
        public PlayerSession(UUID playerUUID, int requiredPoints) {
            this.playerUUID = playerUUID;
            this.requiredPoints = requiredPoints;
            this.challenges = new ArrayList<>();
            this.currentChallengeIndex = 0;
            this.totalPoints = 0;
            
            // Set up a timeout task to automatically end the session after 30 minutes
            this.timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                endSession(playerUUID, false);
                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    player.sendMessage("§cYour recruitment challenge has timed out.");
                }
            }, 20 * 60 * 30); // 30 minutes
        }
        
        public UUID getPlayerUUID() {
            return playerUUID;
        }
        
        public int getRequiredPoints() {
            return requiredPoints;
        }
        
        public List<RecruitmentChallenge> getChallenges() {
            return challenges;
        }
        
        public void setChallenges(List<RecruitmentChallenge> challenges) {
            this.challenges = challenges;
        }
        
        public int getCurrentChallengeIndex() {
            return currentChallengeIndex;
        }
        
        public void setCurrentChallengeIndex(int currentChallengeIndex) {
            this.currentChallengeIndex = currentChallengeIndex;
        }
        
        public int getTotalPoints() {
            return totalPoints;
        }
        
        public void addPoints(int points) {
            if (points > 0) {
                this.totalPoints += points;
            }
        }
        
        public void cancelTimeout() {
            if (timeoutTask != null) {
                timeoutTask.cancel();
                timeoutTask = null;
            }
        }
    }
    
    /**
     * A challenge that involves answering quiz questions.
     */
    public class QuizChallenge extends RecruitmentChallenge {
        private final List<QuizQuestion> questions;
        private boolean completed;
        
        /**
         * Create a new quiz challenge.
         * 
         * @param id The challenge ID
         * @param name The challenge name
         * @param description The challenge description
         * @param difficulty The challenge difficulty
         * @param type The challenge type
         * @param rewardPoints The reward points
         */
        public QuizChallenge(String id, String name, String description, int difficulty, 
                             RecruitmentChallenge.ChallengeType type, int rewardPoints) {
            super(id, name, description, difficulty, type, rewardPoints);
            this.questions = new ArrayList<>();
            this.completed = false;
            
            // Add some default questions
            addQuestion("What is the maximum stack size for most items?", "64");
            addQuestion("What material is needed to make a beacon?", "Nether Star");
            addQuestion("How many obsidian blocks are needed for a nether portal?", "10");
            addQuestion("What hostile mob can pick up blocks?", "Enderman");
            addQuestion("What item do you need to tame a wolf?", "Bone");
        }
        
        public void addQuestion(String question, String answer) {
            questions.add(new QuizQuestion(question, answer));
        }
        
        public List<QuizQuestion> getQuestions() {
            return questions;
        }
        
        public boolean isCompleted() {
            return completed;
        }
        
        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
        
        /**
         * A quiz question with a question and an answer.
         */
        public class QuizQuestion {
            private final String question;
            private final String answer;
            
            public QuizQuestion(String question, String answer) {
                this.question = question;
                this.answer = answer;
            }
            
            public String getQuestion() {
                return question;
            }
            
            public String getAnswer() {
                return answer;
            }
            
            public boolean checkAnswer(String playerAnswer) {
                return playerAnswer != null && playerAnswer.trim().equalsIgnoreCase(answer.trim());
            }
        }
    }
    
    /**
     * A challenge that involves memorizing a pattern of colors.
     */
    public class MemoryChallenge extends RecruitmentChallenge {
        private final List<String> colorSequence;
        private boolean completed;
        
        /**
         * Create a new memory challenge.
         * 
         * @param id The challenge ID
         * @param name The challenge name
         * @param description The challenge description
         * @param difficulty The challenge difficulty
         * @param type The challenge type
         * @param rewardPoints The reward points
         */
        public MemoryChallenge(String id, String name, String description, int difficulty, 
                              RecruitmentChallenge.ChallengeType type, int rewardPoints) {
            super(id, name, description, difficulty, type, rewardPoints);
            this.colorSequence = new ArrayList<>();
            this.completed = false;
            
            // Generate a color sequence
            generateColorSequence();
        }
        
        private void generateColorSequence() {
            String[] colors = {"Red", "Blue", "Green", "Yellow", "Purple", "Orange"};
            Random random = new Random();
            
            // Generate a sequence of 5 colors
            for (int i = 0; i < 5; i++) {
                colorSequence.add(colors[random.nextInt(colors.length)]);
            }
        }
        
        public List<String> getColorSequence() {
            return colorSequence;
        }
        
        public boolean isCompleted() {
            return completed;
        }
        
        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
        
        public boolean checkSequence(List<String> playerSequence) {
            if (playerSequence.size() != colorSequence.size()) {
                return false;
            }
            
            for (int i = 0; i < colorSequence.size(); i++) {
                if (!colorSequence.get(i).equalsIgnoreCase(playerSequence.get(i))) {
                    return false;
                }
            }
            
            return true;
        }
    }
    
    /**
     * A challenge that involves remembering a sequence of items.
     */
    public class SequenceChallenge extends RecruitmentChallenge {
        private final List<Material> itemSequence;
        private boolean completed;
        
        /**
         * Create a new sequence challenge.
         * 
         * @param id The challenge ID
         * @param name The challenge name
         * @param description The challenge description
         * @param difficulty The challenge difficulty
         * @param type The challenge type
         * @param rewardPoints The reward points
         */
        public SequenceChallenge(String id, String name, String description, int difficulty, 
                                RecruitmentChallenge.ChallengeType type, int rewardPoints) {
            super(id, name, description, difficulty, type, rewardPoints);
            this.itemSequence = new ArrayList<>();
            this.completed = false;
            
            // Generate an item sequence
            generateItemSequence();
        }
        
        private void generateItemSequence() {
            Material[] items = {
                Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, 
                Material.IRON_INGOT, Material.COAL, Material.REDSTONE
            };
            Random random = new Random();
            
            // Generate a sequence of 6 items
            for (int i = 0; i < 6; i++) {
                itemSequence.add(items[random.nextInt(items.length)]);
            }
        }
        
        public List<Material> getItemSequence() {
            return itemSequence;
        }
        
        public List<ItemStack> getItemStackSequence() {
            List<ItemStack> itemStacks = new ArrayList<>();
            for (Material material : itemSequence) {
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§e" + formatMaterialName(material.name()));
                item.setItemMeta(meta);
                itemStacks.add(item);
            }
            return itemStacks;
        }
        
        private String formatMaterialName(String materialName) {
            String[] words = materialName.split("_");
            StringBuilder formattedName = new StringBuilder();
            for (String word : words) {
                formattedName.append(word.substring(0, 1).toUpperCase())
                             .append(word.substring(1).toLowerCase())
                             .append(" ");
            }
            return formattedName.toString().trim();
        }
        
        public boolean isCompleted() {
            return completed;
        }
        
        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
        
        public boolean checkSequence(List<Material> playerSequence) {
            if (playerSequence.size() != itemSequence.size()) {
                return false;
            }
            
            for (int i = 0; i < itemSequence.size(); i++) {
                if (itemSequence.get(i) != playerSequence.get(i)) {
                    return false;
                }
            }
            
            return true;
        }
    }
    
    /**
     * A challenge from the recruitment mini-game.
     */
    public static class RecruitmentChallenge {
        private final String id;
        private final String name;
        private final String description;
        private final int difficulty;
        private final ChallengeType type;
        private final int rewardPoints;
        
        /**
         * The type of recruitment challenge.
         */
        public enum ChallengeType {
            COMBAT,
            GATHERING,
            BUILDING,
            EXPLORATION,
            QUIZ
        }
        
        /**
         * Create a new recruitment challenge.
         * 
         * @param id The challenge ID
         * @param name The challenge name
         * @param description The challenge description
         * @param difficulty The challenge difficulty (1-5)
         * @param type The challenge type
         * @param rewardPoints The points awarded for completing this challenge
         */
        public RecruitmentChallenge(String id, String name, String description, int difficulty, ChallengeType type, int rewardPoints) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.difficulty = difficulty;
            this.type = type;
            this.rewardPoints = rewardPoints;
        }
        
        /**
         * Get the challenge ID.
         * 
         * @return The challenge ID
         */
        public String getId() {
            return id;
        }
        
        /**
         * Get the challenge name.
         * 
         * @return The challenge name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Get the challenge description.
         * 
         * @return The challenge description
         */
        public String getDescription() {
            return description;
        }
        
        /**
         * Get the challenge difficulty.
         * 
         * @return The challenge difficulty (1-5)
         */
        public int getDifficulty() {
            return difficulty;
        }
        
        /**
         * Get the challenge type.
         * 
         * @return The challenge type
         */
        public ChallengeType getType() {
            return type;
        }
        
        /**
         * Get the points awarded for completing this challenge.
         * 
         * @return The reward points
         */
        public int getRewardPoints() {
            return rewardPoints;
        }
        
        /**
         * Check if this challenge is completed.
         * 
         * @return True if the challenge is completed
         */
        public boolean isCompleted() {
            return false; // Base implementation
        }
    }
}
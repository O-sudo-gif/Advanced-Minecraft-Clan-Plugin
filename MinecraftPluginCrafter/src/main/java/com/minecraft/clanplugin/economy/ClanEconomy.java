package com.minecraft.clanplugin.economy;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.ClanRole;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages clan economy features including clan bank, taxes, and economy-related operations.
 */
public class ClanEconomy {
    
    private final ClanPlugin plugin;
    private final Map<String, Double> clanBalances;
    private final Map<String, Double> clanTaxRates;
    private final Map<UUID, Long> lastTaxCollectionTime;
    private final File economyFile;
    
    // Default values
    private final double DEFAULT_STARTING_BALANCE = 0.0;
    private final double DEFAULT_TAX_RATE = 0.05; // 5%
    private final long TAX_COLLECTION_COOLDOWN = 86400000; // 24 hours in milliseconds
    
    /**
     * Creates a new ClanEconomy instance
     * 
     * @param plugin The plugin instance
     */
    public ClanEconomy(ClanPlugin plugin) {
        this.plugin = plugin;
        this.clanBalances = new HashMap<>();
        this.clanTaxRates = new HashMap<>();
        this.lastTaxCollectionTime = new HashMap<>();
        this.economyFile = new File(plugin.getDataFolder(), "economy.yml");
        
        // Load economy data
        loadEconomyData();
    }
    
    /**
     * Gets the balance of a clan
     * 
     * @param clanName The name of the clan
     * @return The clan's balance
     */
    public double getClanBalance(String clanName) {
        return clanBalances.getOrDefault(clanName.toLowerCase(), DEFAULT_STARTING_BALANCE);
    }
    
    /**
     * Sets the balance of a clan
     * 
     * @param clanName The name of the clan
     * @param amount The new balance amount
     */
    public void setClanBalance(String clanName, double amount) {
        clanBalances.put(clanName.toLowerCase(), Math.max(0, amount));
        
        // Save changes
        saveEconomyData();
    }
    
    /**
     * Deposits money into a clan's bank
     * 
     * @param clanName The name of the clan
     * @param amount The amount to deposit
     * @return True if the deposit was successful
     */
    public boolean depositToClan(String clanName, double amount) {
        if (amount <= 0) {
            return false;
        }
        
        double currentBalance = getClanBalance(clanName);
        setClanBalance(clanName, currentBalance + amount);
        
        return true;
    }
    
    /**
     * Withdraws money from a clan's bank
     * 
     * @param clanName The name of the clan
     * @param amount The amount to withdraw
     * @return True if the withdrawal was successful
     */
    public boolean withdrawFromClan(String clanName, double amount) {
        if (amount <= 0) {
            return false;
        }
        
        double currentBalance = getClanBalance(clanName);
        
        if (currentBalance < amount) {
            return false; // Not enough funds
        }
        
        setClanBalance(clanName, currentBalance - amount);
        
        return true;
    }
    
    /**
     * Transfers money from one clan to another
     * 
     * @param fromClanName The name of the sending clan
     * @param toClanName The name of the receiving clan
     * @param amount The amount to transfer
     * @return True if the transfer was successful
     */
    public boolean transferBetweenClans(String fromClanName, String toClanName, double amount) {
        if (amount <= 0) {
            return false;
        }
        
        if (withdrawFromClan(fromClanName, amount)) {
            depositToClan(toClanName, amount);
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets the tax rate for a clan
     * 
     * @param clanName The name of the clan
     * @return The tax rate (0.0 to 1.0)
     */
    public double getTaxRate(String clanName) {
        return clanTaxRates.getOrDefault(clanName.toLowerCase(), DEFAULT_TAX_RATE);
    }
    
    /**
     * Sets the tax rate for a clan
     * 
     * @param clanName The name of the clan
     * @param rate The new tax rate (0.0 to 1.0)
     */
    public void setTaxRate(String clanName, double rate) {
        // Ensure rate is between 0 and 1
        rate = Math.max(0.0, Math.min(1.0, rate));
        
        clanTaxRates.put(clanName.toLowerCase(), rate);
        
        // Save changes
        saveEconomyData();
    }
    
    /**
     * Collects taxes from clan members
     * 
     * @param clanName The name of the clan
     * @param collectorUUID The UUID of the player collecting taxes
     * @return The amount collected, or -1 if on cooldown
     */
    public double collectTaxes(String clanName, UUID collectorUUID) {
        // Check cooldown
        long currentTime = System.currentTimeMillis();
        if (lastTaxCollectionTime.containsKey(collectorUUID)) {
            long lastCollection = lastTaxCollectionTime.get(collectorUUID);
            if (currentTime - lastCollection < TAX_COLLECTION_COOLDOWN) {
                return -1; // Still on cooldown
            }
        }
        
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        if (clan == null) {
            return 0;
        }
        
        double taxRate = getTaxRate(clanName);
        double totalCollected = 0.0;
        
        // This would integrate with a server economy plugin like Vault
        // Since we're just designing the plugin, this is a placeholder
        // In a real implementation, you'd loop through members and collect a percentage
        // of their balance based on the tax rate
        
        // For now, let's just add a flat amount per member
        int memberCount = clan.getMembers().size();
        double flatTaxAmount = 10.0; // Base amount per member
        totalCollected = memberCount * flatTaxAmount * taxRate;
        
        // Update clan balance
        depositToClan(clanName, totalCollected);
        
        // Update cooldown
        lastTaxCollectionTime.put(collectorUUID, currentTime);
        
        return totalCollected;
    }
    
    /**
     * Gets the time remaining until a player can collect taxes again
     * 
     * @param playerUUID The UUID of the player
     * @return The time remaining in milliseconds, or 0 if no cooldown
     */
    public long getTaxCooldownRemaining(UUID playerUUID) {
        if (!lastTaxCollectionTime.containsKey(playerUUID)) {
            return 0;
        }
        
        long lastCollection = lastTaxCollectionTime.get(playerUUID);
        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - lastCollection;
        
        if (timeElapsed >= TAX_COLLECTION_COOLDOWN) {
            return 0;
        }
        
        return TAX_COLLECTION_COOLDOWN - timeElapsed;
    }
    
    /**
     * Pays for a clan territory upkeep cost
     * 
     * @param clanName The name of the clan
     * @param cost The upkeep cost
     * @return True if the payment was successful
     */
    public boolean payTerritoryUpkeep(String clanName, double cost) {
        return withdrawFromClan(clanName, cost);
    }
    
    /**
     * Loads economy data from file
     */
    private void loadEconomyData() {
        clanBalances.clear();
        clanTaxRates.clear();
        lastTaxCollectionTime.clear();
        
        if (!economyFile.exists()) {
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(economyFile);
        
        // Load balances
        if (config.contains("balances")) {
            for (String clan : config.getConfigurationSection("balances").getKeys(false)) {
                double balance = config.getDouble("balances." + clan);
                clanBalances.put(clan.toLowerCase(), balance);
            }
        }
        
        // Load tax rates
        if (config.contains("tax_rates")) {
            for (String clan : config.getConfigurationSection("tax_rates").getKeys(false)) {
                double rate = config.getDouble("tax_rates." + clan);
                clanTaxRates.put(clan.toLowerCase(), rate);
            }
        }
        
        // Load cooldowns
        if (config.contains("tax_cooldowns")) {
            for (String player : config.getConfigurationSection("tax_cooldowns").getKeys(false)) {
                long time = config.getLong("tax_cooldowns." + player);
                lastTaxCollectionTime.put(UUID.fromString(player), time);
            }
        }
    }
    
    /**
     * Saves economy data to file
     */
    private void saveEconomyData() {
        FileConfiguration config = new YamlConfiguration();
        
        // Save balances
        for (Map.Entry<String, Double> entry : clanBalances.entrySet()) {
            config.set("balances." + entry.getKey(), entry.getValue());
        }
        
        // Save tax rates
        for (Map.Entry<String, Double> entry : clanTaxRates.entrySet()) {
            config.set("tax_rates." + entry.getKey(), entry.getValue());
        }
        
        // Save cooldowns
        for (Map.Entry<UUID, Long> entry : lastTaxCollectionTime.entrySet()) {
            config.set("tax_cooldowns." + entry.getKey().toString(), entry.getValue());
        }
        
        try {
            config.save(economyFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save economy data: " + e.getMessage());
        }
    }
    
    /**
     * Formats a currency amount for display
     * 
     * @param amount The amount to format
     * @return The formatted amount string
     */
    public String formatCurrency(double amount) {
        return ChatColor.GOLD + "$" + String.format("%.2f", amount);
    }
    
    /**
     * Handles when a clan is deleted
     * 
     * @param clanName The name of the clan
     */
    public void handleClanDeleted(String clanName) {
        clanBalances.remove(clanName.toLowerCase());
        clanTaxRates.remove(clanName.toLowerCase());
        saveEconomyData();
    }
    
    /**
     * Player deposits money into the clan bank
     * 
     * @param player The player making the deposit
     * @param clanName The name of the clan
     * @param amount The amount to deposit
     * @return True if the deposit was successful
     */
    public boolean playerDepositToClan(Player player, String clanName, double amount) {
        if (amount <= 0) {
            return false;
        }
        
        // Check if Vault is available
        Economy vaultEconomy = plugin.getVaultEconomy();
        if (vaultEconomy == null) {
            player.sendMessage(ChatColor.RED + "Vault economy is not available. Cannot process transaction.");
            return false;
        }
        
        // Check player has sufficient funds
        if (!vaultEconomy.has(player, amount)) {
            player.sendMessage(ChatColor.RED + "You don't have enough money to deposit " + formatCurrency(amount));
            return false;
        }
        
        // Withdraw from player
        EconomyResponse response = vaultEconomy.withdrawPlayer(player, amount);
        if (!response.transactionSuccess()) {
            player.sendMessage(ChatColor.RED + "Failed to withdraw funds: " + response.errorMessage);
            return false;
        }
        
        // Deposit to clan bank
        depositToClan(clanName, amount);
        player.sendMessage(ChatColor.GREEN + "Successfully deposited " + formatCurrency(amount) + 
                       " to your clan bank. New clan balance: " + formatCurrency(getClanBalance(clanName)));
        
        return true;
    }
    
    /**
     * Withdraw money from clan bank to player
     * 
     * @param player The player making the withdrawal
     * @param clanName The name of the clan
     * @param amount The amount to withdraw
     * @return True if the withdrawal was successful
     */
    public boolean playerWithdrawFromClan(Player player, String clanName, double amount) {
        if (amount <= 0) {
            return false;
        }
        
        // Check if player has permission
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        if (clan == null) {
            return false;
        }
        
        ClanMember member = clan.getMember(player.getUniqueId());
        if (member == null || (member.getRole() != ClanRole.LEADER && member.getRole() != ClanRole.OFFICER)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to withdraw funds from the clan bank.");
            return false;
        }
        
        // Check clan has sufficient funds
        if (getClanBalance(clanName) < amount) {
            player.sendMessage(ChatColor.RED + "The clan doesn't have enough money to withdraw " + formatCurrency(amount));
            return false;
        }
        
        // Check if Vault is available
        Economy vaultEconomy = plugin.getVaultEconomy();
        if (vaultEconomy == null) {
            player.sendMessage(ChatColor.RED + "Vault economy is not available. Cannot process transaction.");
            return false;
        }
        
        // Withdraw from clan bank
        if (!withdrawFromClan(clanName, amount)) {
            return false;
        }
        
        // Deposit to player
        EconomyResponse response = vaultEconomy.depositPlayer(player, amount);
        if (!response.transactionSuccess()) {
            // Refund to clan bank if player deposit fails
            depositToClan(clanName, amount);
            player.sendMessage(ChatColor.RED + "Failed to deposit funds: " + response.errorMessage);
            return false;
        }
        
        player.sendMessage(ChatColor.GREEN + "Successfully withdrew " + formatCurrency(amount) + 
                       " from your clan bank. New clan balance: " + formatCurrency(getClanBalance(clanName)));
        
        return true;
    }
    
    /**
     * Collects taxes from all online clan members
     * 
     * @param clanName The name of the clan
     * @param collectorUUID The UUID of the player collecting taxes
     * @return The amount collected, or -1 if on cooldown
     */
    public double collectTaxesWithVault(String clanName, UUID collectorUUID) {
        // Check cooldown
        long currentTime = System.currentTimeMillis();
        if (lastTaxCollectionTime.containsKey(collectorUUID)) {
            long lastCollection = lastTaxCollectionTime.get(collectorUUID);
            if (currentTime - lastCollection < TAX_COLLECTION_COOLDOWN) {
                return -1; // Still on cooldown
            }
        }
        
        Clan clan = plugin.getStorageManager().getClanStorage().getClan(clanName);
        if (clan == null) {
            return 0;
        }
        
        // Check if Vault is available
        Economy vaultEconomy = plugin.getVaultEconomy();
        if (vaultEconomy == null) {
            return collectTaxes(clanName, collectorUUID); // Fall back to basic tax collection
        }
        
        double taxRate = getTaxRate(clanName);
        double totalCollected = 0.0;
        
        // Loop through all members and tax them based on their online/offline status
        for (ClanMember member : clan.getMembers()) {
            UUID memberUUID = member.getPlayerUUID();
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberUUID);
            
            // Skip the clan leader (optional based on policy)
            if (member.getRole() == ClanRole.LEADER) {
                continue;
            }
            
            // Calculate tax amount based on player's balance
            double playerBalance = vaultEconomy.getBalance(offlinePlayer);
            double taxAmount = playerBalance * taxRate;
            
            // Set a minimum tax amount if desired
            double minimumTax = 10.0;
            taxAmount = Math.min(taxAmount, minimumTax); // Cap at minimum amount
            
            // Only collect if player has enough money
            if (vaultEconomy.has(offlinePlayer, taxAmount)) {
                // Withdraw tax from player
                vaultEconomy.withdrawPlayer(offlinePlayer, taxAmount);
                totalCollected += taxAmount;
                
                // Notify the player if they're online
                Player onlinePlayer = offlinePlayer.getPlayer();
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    onlinePlayer.sendMessage(ChatColor.GOLD + "Your clan has collected " + 
                            formatCurrency(taxAmount) + " in taxes from you.");
                }
            }
        }
        
        // Update clan balance
        depositToClan(clanName, totalCollected);
        
        // Update cooldown
        lastTaxCollectionTime.put(collectorUUID, currentTime);
        
        return totalCollected;
    }
}
# Minecraft Clan Plugin

A comprehensive Minecraft plugin for creating and managing player clans with advanced features including territory control, clan economy, and clan wars.

## Security & Performance Enhancements (v1.0.0)

The latest version includes several security enhancements:

1. **Webhook Security Improvements**:
   - Domain validation to prevent malicious webhook URLs
   - JSON input sanitization to prevent injection attacks
   - Connection timeout settings to prevent hanging connections
   - Enhanced error handling and logging
   
2. **Resource Management**:
   - Improved shutdown procedures with proper cleanup
   - Null-safety throughout the plugin code
   - Proper closing of all connections and resources
   
3. **Data Protection**:
   - Added robust shop data persistence
   - Improved EmoteUtils data handling
   - Enhanced config validation

## Features

### Core Clan Features
- **Clan Creation & Management**: Create, join, leave, and manage player clans
- **Role Hierarchy**: Leader, Officer, and Member roles with appropriate permissions
- **Clan Chat**: Private chat channel for clan members
- **Clan Homes**: Set and teleport to clan home locations
- **Member Management**: Invite, kick, promote, and demote clan members
- **Clan Information**: View detailed information about clans
- **Custom Colors**: Personalize your clan with custom colors

### Alliance & Enemy System
- **Alliances**: Form alliances with other clans for mutual benefits
- **Enemies**: Declare other clans as enemies
- **Relationship Management**: Add, remove, and view clan relationships

### Territory System
- **Land Claiming**: Claim and control chunks of land
- **Protection Levels**: Different levels of protection based on influence
- **Clan Flags**: Strengthen territory control with clan flags
- **Territory Map**: Visual representation of nearby territories
- **Influence System**: Dynamic influence levels affecting protection
- **PvP Control**: Enable/disable PvP in territories based on protection level

### Economy System
- **Clan Bank**: Shared clan bank for funds
- **Transaction System**: Deposit, withdraw, and transfer funds
- **Tax System**: Set and collect taxes from clan members
- **Territory Upkeep**: Pay for territory maintenance

### Clan Wars
- **War Declaration**: Formally declare war on other clans
- **War Duration**: Wars last for a specified period (default 7 days)
- **Kill Tracking**: Track kills during wars
- **Leaderboards**: View top killers in wars
- **War Statistics**: Comprehensive statistics tracking
- **Victory Conditions**: Multiple victory conditions

### GUI System
- **Intuitive Interface**: Easy-to-use graphical interface
- **Member Management**: Visual management of clan members
- **Alliance & Enemy Management**: Visually manage relationships
- **Color Selection**: Simple color picker
- **Territory Management**: Visual territory control

## Commands

### Main Commands
- `/clan create <name>` - Create a new clan
- `/clan join <name>` - Join a clan (requires invitation)
- `/clan leave` - Leave your current clan
- `/clan info [name]` - View clan information
- `/clan invite <player>` - Invite a player to your clan
- `/clan kick <player>` - Kick a player from your clan
- `/clan promote <player>` - Promote a member to officer
- `/clan demote <player>` - Demote an officer to member
- `/clan sethome` - Set your clan's home location
- `/clan home` - Teleport to your clan's home
- `/clan list` - List all clans on the server
- `/clan gui` - Open the clan management GUI
- `/c <message>` - Send a message to clan chat

### Alliance & Enemy Commands
- `/clan ally <name>` - Form an alliance with another clan
- `/clan unally <name>` - Remove an alliance
- `/clan enemy <name>` - Mark a clan as an enemy
- `/clan unenemy <name>` - Remove a clan from enemy list

### Territory Commands
- `/clan territory map` - Shows a map of nearby territories
- `/clan territory info` - Shows information about the current chunk
- `/clan territory claim` - Claims the current chunk for your clan
- `/clan territory unclaim` - Unclaims the current chunk
- `/clan territory list` - Lists all territory chunks owned by your clan

### Economy Commands
- `/clan economy balance` - Check your clan's balance
- `/clan economy deposit <amount>` - Deposit money to clan bank
- `/clan economy withdraw <amount>` - Withdraw money from clan bank
- `/clan economy transfer <clan> <amount>` - Transfer money to another clan
- `/clan economy tax <rate>` - Set tax rate
- `/clan economy collect` - Collect taxes from members

### War Commands
- `/clan war declare <clan>` - Declare war on another clan
- `/clan war status` - View the status of your clan's current war
- `/clan war surrender` - Surrender the current war
- `/clan war stats [clan]` - View war statistics for a clan
- `/clan war leaderboard` - View the current war's kill leaderboard

## Permissions

- `clan.territory` - Access to territory commands
- `clan.economy` - Access to economy commands
- `clan.war` - Access to war commands
- `clan.admin.territory` - Admin territory management
- `clan.admin.territory.bypass` - Bypass territory restrictions

## Installation

1. Download the ClanPlugin-1.0.0.jar file
2. Place it in your server's plugins folder
3. Restart your Minecraft server
4. The plugin will generate default configuration files
5. Configure options in the config.yml file to match your server's needs
6. If using Vault for economy, ensure it's installed and configured
7. Use `/clan help` in-game to see available commands

### First-Time Setup

1. After installation, server administrators should:
   - Review and adjust permissions in your permissions plugin
   - Configure economy settings if using Vault integration
   - Set territory claim limits appropriate for your world size
   - Configure webhook URLs if using external notifications
   - Adjust role permissions as needed

2. For optimal performance:
   - Set appropriate territory limits based on server capacity
   - Configure emotes and achievements to match your server theme
   - Test the plugin on a staging server before production use

### Security Recommendations

When using this plugin, follow these security practices:

1. **Webhook Security**: 
   - If using webhooks, ensure your Discord/Slack webhooks are properly secured and not publicly accessible
   - Use only trusted webhook domains (the plugin validates URLs for common services like Discord, Slack, GitHub)
   - Never expose webhook URLs in public forums or chats

2. **Permission Management**: 
   - Carefully assign admin permissions only to trusted staff
   - Regularly audit who has access to administrative commands
   - Set up permission inheritance properly to avoid unintended access

3. **Data Protection**:
   - Regular backups of clan data are essential to prevent loss
   - Store backups in multiple locations
   - Implement a rotation schedule for backups

4. **Plugin Maintenance**:
   - Keep the plugin updated to receive security patches
   - Check for updates regularly
   - Review changelogs for security-related fixes

5. **Server Configuration**:
   - Review all configuration options before use on a production server
   - Set appropriate limits for clan territory claims to prevent abuse
   - Configure economy settings carefully to maintain server balance

6. **API Usage**:
   - If integrating with other plugins via API, ensure proper validation of inputs
   - Monitor for unusual API usage patterns that might indicate exploitation attempts

## Dependencies

- Bukkit/Spigot 1.16+
- Vault (optional, for economy integration)

## Configuration

The plugin creates a config.yml file with the following options:

```yaml
# Default settings can be modified in this file
settings:
  max_clan_name_length: 16
  max_clan_tag_length: 5
  territory:
    enabled: true
    max_base_claims: 10
    claim_per_member: 2
  economy:
    enabled: true
    starting_balance: 0
    default_tax_rate: 0.05
  wars:
    enabled: true
    default_duration_days: 7
```

## Credits

Developed as a custom plugin for enhanced clan management in Minecraft servers by Darkhell.

# Advanced Territory System for Clan Plugin

## Overview

The Clan Plugin's territory system allows clans to claim, control, and protect specific regions of the Minecraft world. This document outlines the design, mechanics, and implementation details of this advanced territory system.

## Core Concepts

### 1. Territory Chunks

- Territories are claimed in chunk-sized sections (16x16 blocks)
- Claims extend from bedrock to build height
- Each clan has a maximum number of chunks they can claim based on member count
- Chunks must be contiguous (connected to existing territory)

### 2. Clan Flags

- Territory is claimed by placing special Clan Flags
- Flags are crafted items that serve as the physical marker of a claim
- Each flag creates a "power zone" around it that strengthens clan control
- Flags can be upgraded to expand their influence radius

### 3. Influence System

- Each chunk has an "influence level" for each clan
- Influence determines control and protection level
- Influence decreases with distance from clan flags
- Border chunks have lower protection than core territory

## Claim Mechanics

### Claiming Territory

1. A clan member places a clan flag in unclaimed territory
2. The chunk is immediately claimed if:
   - The clan has remaining claim slots
   - The chunk is adjacent to existing territory (except first claim)
   - No other clan has influence in the chunk

### Territory Management

- Clan leaders and officers can view territory on an in-game map
- Claims can be abandoned to free up claim slots
- Premium flags provide additional benefits in their influence radius

### Claim Limits

The maximum number of chunks a clan can claim is calculated as:
```
Base Claims (10) + (Members × 2) + (Officer Bonus) + (Alliance Bonus)
```

## Protection System

### Protection Levels

1. **Core** (100% influence): Full protection, only clan members can build/interact
2. **Secure** (75-99% influence): Clan members have full access, allies can interact but not build
3. **Contested** (25-74% influence): PvP enabled, limited building for clan members
4. **Frontier** (1-24% influence): Minimal protection, mainly for territorial display

### Raid Mechanics

- Enemy clans can "raid" territory by placing raiding beacons
- Raids temporarily reduce influence in chunks
- Successful defense restores and strengthens influence
- Failed defense may result in territory loss

## Conflict Resolution

### Territory Disputes

- Overlapping claims are resolved through the influence system
- Higher influence determines ownership
- Influence can shift based on player activity in disputed areas

### War System

- Clans can declare war on rivals
- During war, influence decay in enemy territory is accelerated
- War has time limits and victory conditions

## Technical Implementation

### Data Structure

```java
public class Territory {
    private final int chunkX;
    private final int chunkZ;
    private final String worldName;
    private final String clanName;
    private int influenceLevel;
    private List<Flag> flags;
    private long claimTime;
    
    // Methods for influence calculation, flag management, etc.
}

public class Flag {
    private final Location location;
    private final UUID placedBy;
    private final long placedTime;
    private int tier;  // Flag upgrade level
    private int influenceRadius;
    
    // Methods for influence projection, upgrade management, etc.
}
```

### Storage

- Territory data is stored in a dedicated territory.json file
- Fast lookup is implemented using chunk coordinate hashing
- Territory data is cached for performance
- Changes are persisted on chunk claim/unclaim events

### Performance Considerations

- Influence calculations are cached and updated on a schedule
- Territory lookups use spatial hashing for O(1) performance
- Flag influence uses distance-squared calculations for efficiency

## Permissions and Commands

### Territory Commands

- `/clan territory map` - Shows a map of nearby territories
- `/clan territory info` - Shows information about the current chunk
- `/clan territory claim` - Claims the current chunk if eligible
- `/clan territory unclaim` - Unclaims the current chunk
- `/clan territory list` - Lists all territory chunks owned by the clan

### Admin Commands

- `/clan admin territory clear <clan>` - Removes all territory for a clan
- `/clan admin territory set <clan>` - Sets the current chunk ownership
- `/clan admin territory bypass` - Toggles admin bypass mode

## Future Expansion

### Planned Features

1. Territory tax system for clan economy
2. Specialized territory types (farming, mining, etc.)
3. Seasonal territory contests for rewards
4. Integration with other plugins for region-specific effects
5. Dynamic influence based on player activity

---

This territory system provides a robust framework for clan-based territorial control while balancing gameplay mechanics and server performance. The system encourages clan cooperation, strategic planning, and engaging PvP encounters in a way that enhances the overall Minecraft multiplayer experience.
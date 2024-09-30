# PlayerDataRollback​

## Overview
Losing player data unexpectedly is incredibly frustrating, and finding a reliable solution can be difficult. That's why I created PlayerDataRollback—a plugin designed to automatically back up and restore player data, giving you peace of mind.

The plugin automatically creates a backup every time the server starts. You can also manually back up, restore, and manage backups with simple commands.

Backups are saved in plugins/PlayerDataRollback/Backups

## Features
Automatically backs up player data on server startup.
Manually create, rollback, and remove player data backups.
Filter backups by specific time periods such as days, months, or years.
Restore individual player data or all players at once.
Supports multi-language (English, Chinese Simplified, Chinese Traditional, and German).
Paginated display of backup lists.
Kicks all players when rolling back all data to avoid conflicts.
## Commands
Backup Management
/pld backup create <backup name> - Create a backup of all player data.
/pld list (:<xd/xm/xy>) - List backups filtered by time (days/months/years ago).
/pld page <number> - Navigate through the list of backups (used after /pld list).
Rollback
/pld rollback <playername> <backup name> - Rollback a player's data to a specific backup.
/pld rollbackall (<xd/xm/xy>) - Rollback all player data from a specified time period or backup (this will kick all online players).
## Backup Removal
/pld backup remove <backup name> - Delete a specific backup.
/pld backup removeall (<xd/xm/xy>) - Delete all backups from a specified time period (days/months/years ago).
## Multi-language Support
The plugin supports multiple languages:

English (en_US)
Chinese (Simplified) (zh_CN)
Chinese (Traditional) (zh_TW)
German (de_DE)
Translations are applied automatically based on the server's locale.

## Installation
Download the latest version of PlayerDataRollback from SpigotMC.
Place the .jar file into the plugins folder.
Restart the server to generate the configuration and necessary files.
Backups will be automatically created on each server startup, or use the commands for manual control.
Permissions
playerdata.use - Grants access to all plugin commands.
playerdata.admin - Grants admin access to delete or rollback backups.
Configuration
You can configure the plugin in the config.yml file, located in plugins/PlayerDataRollback. It allows you to change the world name, backup location, and language settings.

## Example Usage
Create a backup: /pld backup create myBackup
Rollback a player's data: /pld rollback Steve myBackup
Rollback all player data from 7 days ago: /pld rollbackall -t:7d
List backups from the last 3 months: /pld list -t:3m
## Support
If you encounter any issues or have suggestions for new features, feel free to reach out via SpigotMC or open a issue on plugin's github repository.


## bStats
![https://bstats.org/signatures/bukkit/PlayerDataRollback.svg](https://bstats.org/plugin/bukkit/PlayerDataRollback/23504)

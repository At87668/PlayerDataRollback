# PlayerDataBackup

A Spigot plugin to auto backup playerdata

I hate the sudden loss of player data, it's really troublesome.
But again I can't find a good playerdata backup plugin, so I wrote this and hope to help more people.

You can get the plugin on https://www.spigotmc.org/resources/playerdatabackup.119720/

The plugin will auto create a backup on server startup.

Backup is save in plugins/PlayerDataBackup/Backups/yyyy-mm-dd

Commands
- /playerdata backup - Backup all playerdata
- /playerdata rollback {playername} {days} - Rollback playerdata n days ago
- /playerdata rollbackall {days} - Rollback all playerdata n days ago
- /playerdata remove {days} - Remove playerdata n days ago
- /playerdata removeall {days} - Remove all playerdata n days ago
- /playerdata backup - Backup all playerdata

Config.yml
Code (Text):
backup-auto-remove: 7 # Automatically deletes backups older than the specified number of days
backup-interval: 86400 # Automatic backup interval in seconds
world-name: world # The name of the world where playerdata is stored
language: "en_US" # Language
en_US.yml
Code (Text):
messages:
  backup-completed: "Backup completed successfully."
  rollback-completed: "Rolled back data for player {player}."
  rollback-all-completed: "Rolled back data for all players to {days} days ago."
  removeall-completed: "Removed backups older than {days} days."
  reload-completed: "Plugin configuration reloaded."
  usage-error: "Usage: /playerdata <backup|rollback|rollbackall|removeall|reload>"
  no-backup-found: "No backup found from {days} days ago."
  no-playerdata: "Player data folder not found!"
zh_CN.yml
Code (Text):
messages:
  backup-completed: "备份成功完成。"
  rollback-completed: "已回滚玩家 {player} 的数据。"
  rollback-all-completed: "已回滚所有玩家的数据到 {days} 天前。"
  removeall-completed: "已删除超过 {days} 天的备份。"
  reload-completed: "插件配置已重新加载。"
  usage-error: "用法: /playerdata <backup|rollback|rollbackall|removeall|reload>"
  no-backup-found: "没有找到 {days} 天前的备份。"
  no-playerdata: "未找到玩家数据文件夹！"

## Authors

- [Author87668](https://github.com/At87668)

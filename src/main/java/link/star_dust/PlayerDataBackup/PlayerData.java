package link.star_dust.PlayerDataBackup;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData extends JavaPlugin {

    private FileConfiguration languageConfig;
    private Map<String, String> messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLanguage();
        startBackupTask();
    }

    @Override
    public void onDisable() {
		// pass
	}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("playerdata.use")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("usage-error"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "backup":
                if (sender.hasPermission("playerdata.backup")) {
                    backupPlayerData();
                    sender.sendMessage(getMessage("backup-completed"));
                } else {
                    sender.sendMessage(getMessage("no-permission"));
                }
                break;

            case "rollback":
                if (args.length == 3 && sender.hasPermission("playerdata.rollback")) {
                    rollbackPlayerData(args[1], Integer.parseInt(args[2]));
                    sender.sendMessage(getMessage("rollback-completed").replace("{player}", args[1]));
                } else {
                    sender.sendMessage(getMessage("usage-error"));
                }
                break;

            case "rollbackall":
                if (args.length == 2 && sender.hasPermission("playerdata.rollbackall")) {
                    rollbackAllPlayerData(Integer.parseInt(args[1]));
                    sender.sendMessage(getMessage("rollback-all-completed").replace("{days}", args[1]));
                } else {
                    sender.sendMessage(getMessage("usage-error"));
                }
                break;

            case "removeall":
                if (args.length == 2 && sender.hasPermission("playerdata.removeall")) {
                    removeAllOldBackups(Integer.parseInt(args[1]));
                    sender.sendMessage(getMessage("removeall-completed").replace("{days}", args[1]));
                } else {
                    sender.sendMessage(getMessage("usage-error"));
                }
                break;

            case "reload":
                if (sender.hasPermission("playerdata.reload")) {
                    reloadConfig();
                    loadLanguage();
                    sender.sendMessage(getMessage("reload-completed"));
                } else {
                    sender.sendMessage(getMessage("no-permission"));
                }
                break;

            default:
                sender.sendMessage(getMessage("usage-error"));
        }

        return true;
    }

    private void startBackupTask() {
        int interval = getConfig().getInt("backup-interval", 86400);
        new BukkitRunnable() {
            @Override
            public void run() {
                backupPlayerData();
                autoRemoveOldBackups();
            }
        }.runTaskTimer(this, 0, interval);
    }

    // Backup all playerdata
    private void backupPlayerData() {
        String worldName = getConfig().getString("world-name", "world");
        File playerDataFolder = new File(getServer().getWorldContainer(), worldName + "/playerdata");
        File backupFolder = new File(getDataFolder(), "Backups/" + LocalDate.now().toString());

        if (!playerDataFolder.exists()) {
            getLogger().warning(getMessage("no-playerdata"));
            return;
        }

        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        try {
            for (File file : playerDataFolder.listFiles()) {
                Path sourcePath = file.toPath();
                Path destinationPath = Paths.get(backupFolder.getAbsolutePath(), file.getName());

                Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            }
            getLogger().info(getMessage("backup-completed"));
        } catch (IOException e) {
            getLogger().severe("Failed to backup player data: " + e.getMessage());
        }
    }

    // Rollback playerdata
    private void rollbackPlayerData(String playerName, int daysAgo) {
        UUID playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        String worldName = getConfig().getString("world-name", "world");
        File backupFolder = new File(getDataFolder(), "Backups/" + LocalDate.now().minusDays(daysAgo).toString());
        File backupFile = new File(backupFolder, playerUUID + ".dat");

        if (backupFile.exists()) {
            try {
                Path sourcePath = backupFile.toPath();
                Path destinationPath = new File(getServer().getWorldContainer(), worldName + "/playerdata/" + playerUUID + ".dat").toPath();

                Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                getLogger().info(getMessage("rollback-completed").replace("{player}", playerName));
            } catch (IOException e) {
                getLogger().severe("Failed to rollback player data: " + e.getMessage());
            }
        } else {
            getLogger().warning(getMessage("no-backup-found").replace("{days}", String.valueOf(daysAgo)));
        }
    }

    // Rollback all playerdata
    private void rollbackAllPlayerData(int daysAgo) {
        String worldName = getConfig().getString("world-name", "world");
        File backupFolder = new File(getDataFolder(), "Backups/" + LocalDate.now().minusDays(daysAgo).toString());

        if (backupFolder.exists()) {
            for (File backupFile : backupFolder.listFiles()) {
                String fileName = backupFile.getName();
                try {
                    Path sourcePath = backupFile.toPath();
                    Path destinationPath = new File(getServer().getWorldContainer(), worldName + "/playerdata/" + fileName).toPath();

                    Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    getLogger().severe("Failed to rollback player data for " + fileName + ": " + e.getMessage());
                }
            }
            getLogger().info(getMessage("rollback-all-completed").replace("{days}", String.valueOf(daysAgo)));
        } else {
            getLogger().warning(getMessage("no-backup-found").replace("{days}", String.valueOf(daysAgo)));
        }
    }

    // Remove old Backup n days ago
    private void removeAllOldBackups(int daysAgo) {
        File backupFolder = new File(getDataFolder(), "Backups/" + LocalDate.now().minusDays(daysAgo).toString());

        if (backupFolder.exists()) {
            for (File backupFile : backupFolder.listFiles()) {
                backupFile.delete();
            }
            backupFolder.delete();
            getLogger().info(getMessage("removeall-completed").replace("{days}", String.valueOf(daysAgo)));
        } else {
            getLogger().warning(getMessage("no-backup-found").replace("{days}", String.valueOf(daysAgo)));
        }
    }

    // Auto Remove old Backup
    private void autoRemoveOldBackups() {
        int daysToKeep = getConfig().getInt("backup-auto-remove", 7);
        LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);

        File backupsFolder = new File(getDataFolder(), "Backups");
        if (backupsFolder.exists()) {
            for (File folder : backupsFolder.listFiles()) {
                LocalDate folderDate = LocalDate.parse(folder.getName());
                if (folderDate.isBefore(cutoffDate)) {
                    for (File backupFile : folder.listFiles()) {
                        backupFile.delete();
                    }
                    folder.delete();
                }
            }
            getLogger().info("Automatically removed backups older than " + daysToKeep + " days.");
        }
    }

    // Load Language File
    private void loadLanguage() {
        String lang = getConfig().getString("language", "en_US");
        File langFile = new File(getDataFolder(), "Languages/" + lang + ".yml");

        if (!langFile.exists()) {
            saveResource("Languages/en_US.yml", false); // 确保默认语言存在
            langFile = new File(getDataFolder(), "Languages/en_US.yml");
        }

        languageConfig = YamlConfiguration.loadConfiguration(langFile);
        messages = new HashMap<>();
        for (String key : languageConfig.getConfigurationSection("messages").getKeys(false)) {
            messages.put(key, languageConfig.getString("messages." + key));
        }
    }

    // Get Message
    private String getMessage(String key) {
        return messages.getOrDefault(key, "Message not found: " + key);
    }
}

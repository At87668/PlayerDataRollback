package link.star_dust.PlayerDataRollback;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import org.bstats.bukkit.Metrics;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerData extends JavaPlugin {

    private Map<String, String> messages;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private List<File> cachedBackups = null;
    private int totalPages = 0;

    @Override
    public void onEnable() {
    	int pluginId = 23504;
    	new Metrics(this, pluginId);
        
    	getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
    	
    	loadConfig();
        loadLanguage();
        startBackupTask();
        checkForUpdates();
    }

    @Override
    public void onDisable() {
    	// pass
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("playerdata.use")) {
            sender.sendMessage(applyColorCodes(getMessage("no-permission")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(applyColorCodes("&aPlayerData&bRollback &2v"+ getDescription().getVersion() +"\n&eby &6Author87668\n\n&ahttps://www.spigotmc.org/resources/119720/"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                if (sender.hasPermission("playerdata.help")) {
                    sendHelpMessage(sender);
                } else {
                    sender.sendMessage(applyColorCodes(getMessage("no-permission")));
                }
                break;

            case "backup":
                if (sender.hasPermission("playerdata.backup")) {
                    if (args.length >= 2) {
                        if ("create".equalsIgnoreCase(args[1]) && sender.hasPermission("playerdata.backup.create")) {
                            backupPlayerData(args.length == 3 ? args[2] : null);
                            sender.sendMessage(applyColorCodes(getMessage("backup-completed")));
                        } else if ("remove".equalsIgnoreCase(args[1]) && args.length == 3 && sender.hasPermission("playerdata.backup.remove")) {
                            removeBackup(args[2]);
                            sender.sendMessage(applyColorCodes(getMessage("remove-completed").replace("{name}", args[2])));
                        } else if ("removeall".equalsIgnoreCase(args[1]) && args.length == 3 && sender.hasPermission("playerdata.backup.removeall")) {
                            if (args[2].matches("\\d+[dmy]")) {
                                // Call the removeAllOldBackups method and check the result
                                boolean backupsRemoved = removeAllOldBackups(args[2]);
                                if (backupsRemoved) {
                                    sender.sendMessage(applyColorCodes(getMessage("removeall-completed").replace("{time}", args[2])));
                                } else {
                                    sender.sendMessage(applyColorCodes(getMessage("no-backups-found")));
                                }
                            } else {
                                sender.sendMessage(applyColorCodes(getMessage("invalid-time-filter").replace("{filter}", args[2])));
                            }
                        } else {
                            sender.sendMessage(applyColorCodes(getMessage("usage-error")));
                            sender.sendMessage(applyColorCodes(getMessage("help-backup-create")));
                            sender.sendMessage(applyColorCodes(getMessage("help-backup-remove")));
                            sender.sendMessage(applyColorCodes(getMessage("help-backup-removeall")));
                            sender.sendMessage(applyColorCodes(getMessage("help-backup-list")));
                        }
                    } else {
                        sender.sendMessage(applyColorCodes(getMessage("usage-error")));
                        sender.sendMessage(applyColorCodes(getMessage("help-backup-create")));
                        sender.sendMessage(applyColorCodes(getMessage("help-backup-remove")));
                        sender.sendMessage(applyColorCodes(getMessage("help-backup-removeall")));
                        sender.sendMessage(applyColorCodes(getMessage("help-backup-list")));
                    }
                } else {
                    sender.sendMessage(applyColorCodes(getMessage("no-permission")));
                }
                break;


            case "rollback":
                if (args.length == 3 && sender.hasPermission("playerdata.rollback")) {
                    rollbackPlayerData(args[1], args[2]);
                    sender.sendMessage(applyColorCodes(getMessage("rollback-completed").replace("{player}", args[1])));
                } else {
                    sender.sendMessage(applyColorCodes(getMessage("usage-error")));
                    sender.sendMessage(applyColorCodes(getMessage("help-rollback")));
                    sender.sendMessage(applyColorCodes(getMessage("help-rollbackall")));
                }
                break;
                
            case "rollbackall":
                if (args.length == 2 && sender.hasPermission("playerdata.rollbackall")) {
                    rollbackAllPlayerData(args[1]);
                    sender.sendMessage(applyColorCodes(getMessage("rollback-all-completed").replace("{name}", args[1])));
                } else {
                    sender.sendMessage(applyColorCodes(getMessage("usage-error")));
                    sender.sendMessage(applyColorCodes(getMessage("help-rollback")));
                    sender.sendMessage(applyColorCodes(getMessage("help-rollbackall")));
                }
                break;

            case "reload":
                if (sender.hasPermission("playerdata.reload")) {
                    reloadConfig();
                    loadLanguage();
                    sender.sendMessage(applyColorCodes(getMessage("reload-completed")));
                } else {
                    sender.sendMessage(applyColorCodes(getMessage("no-permission")));
                }
                break;

            case "list":
                if (sender.hasPermission("playerdata.list")) {
                    listBackups(sender, args.length == 2 ? args[1] : null);
                } else {
                    sender.sendMessage(applyColorCodes(getMessage("no-permission")));
                }
                break;

            case "page":
                if (sender.hasPermission("playerdata.list")) {
                    if (args.length == 2) {
                        try {
                            int page = Integer.parseInt(args[1]);
                            displayBackupPage(sender, page);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(applyColorCodes(getMessage("invalid-page")));
                        }
                    } else {
                        sender.sendMessage(applyColorCodes(getMessage("usage-error")));
                    }
                } else {
                    sender.sendMessage(applyColorCodes(getMessage("no-permission")));
                }
                break;

            default:
                sender.sendMessage(applyColorCodes(getMessage("usage-error")));
        }

        return true;
    }

    private void startBackupTask() {
        int interval = getConfig().getInt("backup-interval", 72000); 
        new BukkitRunnable() {
            @Override
            public void run() {
                backupPlayerData(null);
                autoRemoveOldBackups(); 
            }
        }.runTaskTimer(this, 0, interval);
    }

    private void backupPlayerData(String customName) {
        String worldName = getConfig().getString("world-name", "world");
        File playerDataFolder = new File(getServer().getWorldContainer(), worldName + "/playerdata");
        String backupName = (customName != null && !customName.isEmpty()) ? customName : formatter.format(LocalDateTime.now());
        File backupFolder = new File(getDataFolder(), "Backups/" + backupName);

        if (!playerDataFolder.exists()) {
            getLogger().warning(applyColorCodes(getMessage("no-playerdata")));
            return;
        }

        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        try {
            for (File file : playerDataFolder.listFiles()) {
                // Skip files ending with .dat_old to save space
                if (file.getName().endsWith(".dat_old")) {
                    continue;
                }

                Path sourcePath = file.toPath();
                Path destinationPath = Paths.get(backupFolder.getAbsolutePath(), file.getName());

                Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            }
            // getLogger().info(applyColorCodes(getMessage("backup-completed")));
        } catch (IOException e) {
            getLogger().severe("Failed to backup player data: " + e.getMessage());
        }
    }


    private void rollbackPlayerData(String playerName, String backupName) {
        UUID playerUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        String worldName = getConfig().getString("world-name", "world");
        File backupFolder = new File(getDataFolder(), "Backups/" + backupName);
        File backupFile = new File(backupFolder, playerUUID + ".dat");

        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            player.kickPlayer(applyColorCodes(getMessage("rollback-kick")));
        }

        if (backupFile.exists()) {
            try {
                Path sourcePath = backupFile.toPath();
                Path destinationPath = new File(getServer().getWorldContainer(), worldName + "/playerdata/" + playerUUID + ".dat").toPath();

                Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                // getLogger().info(applyColorCodes(getMessage("rollback-completed").replace("{player}", playerName)));
            } catch (IOException e) {
                getLogger().severe("Failed to rollback player data: " + e.getMessage());
            }
        } else {
            getLogger().warning(applyColorCodes(getMessage("no-backup-found").replace("{name}", backupName)));
        }
    }

    private void rollbackAllPlayerData(String backupName) {
        String worldName = getConfig().getString("world-name", "world");
        File backupFolder = new File(getDataFolder(), "Backups/" + backupName);

        if (!backupFolder.exists() || !backupFolder.isDirectory()) {
            getLogger().warning(applyColorCodes(getMessage("no-backup-found").replace("{name}", backupName)));
            return;
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(applyColorCodes(getMessage("rollbackall-kick")));
        }


        for (File backupFile : backupFolder.listFiles()) {
            String fileName = backupFile.getName();
            try {
                Path sourcePath = backupFile.toPath();
                Path destinationPath = new File(getServer().getWorldContainer(), worldName + "/playerdata/" + fileName).toPath();

                Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                getLogger().severe("Failed to rollback player data: " + e.getMessage());
            }
        }

        // getLogger().info(applyColorCodes(getMessage("rollback-all-completed").replace("{name}", backupName)));
    }

    private void removeBackup(String backupName) {
        File backupFolder = new File(getDataFolder(), "Backups/" + backupName);
        if (backupFolder.exists() && backupFolder.isDirectory()) {
            try {
                for (File file : backupFolder.listFiles()) {
                    Files.delete(file.toPath());
                }
                Files.delete(backupFolder.toPath());
                // getLogger().info(applyColorCodes(getMessage("remove-completed").replace("{name}", backupName)));
            } catch (IOException e) {
                getLogger().severe("Failed to remove backup: " + e.getMessage());
            }
        } else {
            getLogger().warning(applyColorCodes(getMessage("no-backup-found").replace("{name}", backupName)));
        }
    }

    private boolean removeAllOldBackups(String timeFilter) {
        List<File> matchingBackups = getMatchingBackups(timeFilter);

        // If no backups were found, return false
        if (matchingBackups.isEmpty()) {
            getLogger().warning(applyColorCodes(getMessage("no-backups-found")));
            return false;
        }

        // Iterate through and delete each backup folder and its contents
        for (File backupFolder : matchingBackups) {
            try {
                for (File file : backupFolder.listFiles()) {
                    Files.delete(file.toPath());
                }
                Files.delete(backupFolder.toPath());
            } catch (IOException e) {
                getLogger().severe("Failed to remove backup: " + e.getMessage());
            }
        }

        // Return true if backups were found and deleted
        return true;
    }



    private void listBackups(CommandSender sender, String timeFilter) {
        List<File> matchingBackups = getMatchingBackups(timeFilter);

        if (matchingBackups.isEmpty()) {
            sender.sendMessage(applyColorCodes(getMessage("no-backups-found")));
            return;
        }

        cachedBackups = matchingBackups.stream().sorted((file1, file2) -> {
            try {
                Path path1 = Paths.get(file1.getAbsolutePath());
                Path path2 = Paths.get(file2.getAbsolutePath());
                Instant creationTime1 = Files.readAttributes(path1, BasicFileAttributes.class).creationTime().toInstant();
                Instant creationTime2 = Files.readAttributes(path2, BasicFileAttributes.class).creationTime().toInstant();
                return creationTime2.compareTo(creationTime1);
            } catch (IOException e) {
                return 0;
            }
        }).collect(Collectors.toList());

        totalPages = (cachedBackups.size() + 9) / 10;

        int page = 1;
        int start = (page - 1) * 10;
        int end = Math.min(start + 10, cachedBackups.size());

        sender.sendMessage(applyColorCodes(getMessage("backup-list-header")));

        for (int i = start; i < end; i++) {
            File backupFolder = cachedBackups.get(i);
            String backupName = backupFolder.getName();
            String backupTimeStr;

            try {
                Path folderPath = Paths.get(backupFolder.getAbsolutePath());
                BasicFileAttributes attributes = Files.readAttributes(folderPath, BasicFileAttributes.class);
                Instant creationInstant = attributes.creationTime().toInstant();
                LocalDateTime backupTime = LocalDateTime.ofInstant(creationInstant, ZoneId.systemDefault());
                backupTimeStr = displayFormatter.format(backupTime);
            } catch (IOException e) {
                backupTimeStr = "ERROR";
            }

            sender.sendMessage(applyColorCodes(getMessage("backup-list-item")
                    .replace("{id}", String.valueOf(i + 1))
                    .replace("{folder}", backupName)
                    .replace("{time}", backupTimeStr)));
        }

        sender.sendMessage(applyColorCodes(getMessage("page-navigation")
                .replace("{current}", String.valueOf(page))
                .replace("{total}", String.valueOf(totalPages))));
    }

    private void displayBackupPage(CommandSender sender, int page) {
        if (cachedBackups == null || cachedBackups.isEmpty()) {
            sender.sendMessage(applyColorCodes(getMessage("no-backups-found")));
            return;
        }

        if (page < 1 || page > totalPages) {
            sender.sendMessage(applyColorCodes(getMessage("invalid-page")));
            return;
        }

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, cachedBackups.size());

        sender.sendMessage(applyColorCodes(getMessage("backup-list-header")));

        for (int i = start; i < end; i++) {
            File backupFolder = cachedBackups.get(i);
            String backupName = backupFolder.getName();
            String backupTimeStr;

            try {
                Path folderPath = Paths.get(backupFolder.getAbsolutePath());
                BasicFileAttributes attributes = Files.readAttributes(folderPath, BasicFileAttributes.class);
                Instant creationInstant = attributes.creationTime().toInstant();
                LocalDateTime backupTime = LocalDateTime.ofInstant(creationInstant, ZoneId.systemDefault());
                backupTimeStr = displayFormatter.format(backupTime);
            } catch (IOException e) {
                backupTimeStr = "ERROR";
            }

            sender.sendMessage(applyColorCodes(getMessage("backup-list-item")
                    .replace("{id}", String.valueOf(i + 1))
                    .replace("{folder}", backupName)
                    .replace("{time}", backupTimeStr)));
        }

        sender.sendMessage(applyColorCodes(getMessage("page-navigation")
                .replace("{current}", String.valueOf(page))
                .replace("{total}", String.valueOf(totalPages))));
    }


    private List<File> getMatchingBackups(String timeFilter) {
        File backupsDir = new File(getDataFolder(), "Backups");
        File[] allBackups = backupsDir.listFiles();

        if (allBackups == null || allBackups.length == 0) {
            return Collections.emptyList();
        }

        if (timeFilter == null || timeFilter.isEmpty()) {
            return Arrays.asList(allBackups);
        }

        LocalDateTime cutoffTime = LocalDateTime.now();
        if (timeFilter.endsWith("d")) {
            int days = Integer.parseInt(timeFilter.replace("d", ""));
            cutoffTime = cutoffTime.minusDays(days);
        } else if (timeFilter.endsWith("m")) {
            int months = Integer.parseInt(timeFilter.replace("m", ""));
            cutoffTime = cutoffTime.minusMonths(months);
        } else if (timeFilter.endsWith("y")) {
            int years = Integer.parseInt(timeFilter.replace("y", ""));
            cutoffTime = cutoffTime.minusYears(years);
        }

        long cutoffMillis = cutoffTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        List<File> matchingBackups = new ArrayList<>();

        for (File backup : allBackups) {
            long lastModified = backup.lastModified();

            if (lastModified < cutoffMillis) {
                matchingBackups.add(backup);
            }
        }

        return matchingBackups;
    }

    
    private void loadConfig() {
        saveDefaultConfig();
        File configFile = new File(getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        InputStream defaultConfigStream = getResource("config.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));

            for (String key : defaultConfig.getKeys(false)) {
                if (!config.contains(key)) {
                    config.set(key, defaultConfig.get(key));
                }
            }

            try {
                config.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadLanguage() {
        String[] supportedLanguages = {"en_US", "de_DE", "zh_CN", "zh_TW", "fr_FR", "ja_JP", "ru_MO"};

        for (String lang : supportedLanguages) {
            File langFile = new File(getDataFolder(), "Languages/" + lang + ".yml");
            if (!langFile.exists()) {
                saveResource("Languages/" + lang + ".yml", false);
            }
        }

        String lang = getConfig().getString("language", "en_US");
        File langFile = new File(getDataFolder(), "Languages/" + lang + ".yml");

        if (!langFile.exists()) {
            lang = "en_US";
            langFile = new File(getDataFolder(), "Languages/en_US.yml");
        }

        File defaultLangFile = new File(getDataFolder(), "Languages/en_US.yml");
        YamlConfiguration defaultLangConfig = YamlConfiguration.loadConfiguration(defaultLangFile);

        YamlConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);

        for (String key : defaultLangConfig.getConfigurationSection("messages").getKeys(false)) {
            if (!langConfig.contains("messages." + key)) {
                langConfig.set("messages." + key, defaultLangConfig.getString("messages." + key));
            }
        }

        try {
            langConfig.save(langFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        messages = new HashMap<>();
        String prefix = langConfig.getString("prefix", "&7[&aPlayerData&bRollback&7]");

        for (String key : langConfig.getConfigurationSection("messages").getKeys(false)) {
            String message = langConfig.getString("messages." + key);
            if (message != null) {
                message = ChatColor.translateAlternateColorCodes('&', message.replace("%prefix%", prefix));
            }
            messages.put(key, message);
        }
    }

    public class PlayerJoinListener implements Listener {

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            // Check for updates only if the player has permission
            if (player.hasPermission("playerdata.checkupdate")) {
                String latestVersion = getLatestVersionFromSpigot();
                String currentVersion = getDescription().getVersion();

                if (latestVersion != null && !currentVersion.equals(latestVersion)) {
                    String updateMessage = getMessage("update-available")
                            .replace("{version}", latestVersion);
                    player.sendMessage(applyColorCodes(updateMessage));
                }
            }
        }
    }
    
    public void checkForUpdates() {
        if (!getConfig().getBoolean("update-checker", true)) {
            return;
        }

        String currentVersion = getDescription().getVersion();
        String latestVersion = getLatestVersionFromSpigot();

        if (latestVersion == null || currentVersion.equals(latestVersion)) {
            return;
        }
        
        String updateMessage = getMessage("update-available")
                .replace("{version}", latestVersion);
        getServer().getConsoleSender().sendMessage(applyColorCodes(updateMessage));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("playerdata.checkupdate")) {
                player.sendMessage(applyColorCodes(updateMessage));
            }
        }
    }

    private String getLatestVersionFromSpigot() {
        try {
            URL url = new URL("https://api.spigotmc.org/simple/0.2/index.php?action=getResource&id=119720");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Get "current_version"
            JSONObject jsonResponse = new JSONObject(response.toString());
            return jsonResponse.getString("current_version");
            
        } catch (IOException | JSONException e) {
            getLogger().warning("Failed to check for updates: " + e.getMessage());
            return null;
        }
    }

    private String getMessage(String key) {
        return messages.getOrDefault(key, key);
    }

    private String applyColorCodes(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private void autoRemoveOldBackups() {
        String autoRemoveTimeFilter = getConfig().getString("auto-remove-time-filter");
        if (autoRemoveTimeFilter != null && Integer.valueOf(autoRemoveTimeFilter).intValue() != -1 && !autoRemoveTimeFilter.isEmpty()) {
            removeAllOldBackups(autoRemoveTimeFilter);
        }
    }
    
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();
        
        if (!sender.hasPermission("playerdata.use")) {
            return suggestions;
        }

        if (args.length == 1) {
            List<String> mainCommands = Arrays.asList("help", "backup", "rollback", "reload", "list", "page");
            for (String cmd : mainCommands) {
                if (cmd.toLowerCase().startsWith(args[0].toLowerCase())) {
                    suggestions.add(cmd);
                }
            }
        } 
        else if (args.length == 2 && args[0].equalsIgnoreCase("backup")) {
            List<String> backupCommands = Arrays.asList("create", "remove", "removeall", "list");
            for (String cmd : backupCommands) {
                if (cmd.toLowerCase().startsWith(args[1].toLowerCase())) {
                    suggestions.add(cmd);
                }
            }
        } 
        else if (args[0].equalsIgnoreCase("rollback")) {
            if (args.length == 2) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        suggestions.add(player.getName());
                    }
                }
            } else if (args.length == 3) {
                File backupsDir = new File(getDataFolder(), "Backups");
                if (backupsDir.exists()) {
                    for (File backup : backupsDir.listFiles()) {
                        if (backup.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                            suggestions.add(backup.getName());
                        }
                    }
                }
            }
        } 
        else if (args[0].equalsIgnoreCase("page") && args.length == 2) {
            suggestions.add("1");
            suggestions.add("2");
            suggestions.add("3");
        }
        
        return suggestions;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(applyColorCodes(getMessage("help-title")));
        sender.sendMessage(applyColorCodes(getMessage("help-reload")));
        sender.sendMessage(applyColorCodes(getMessage("help-backup-create")));
        sender.sendMessage(applyColorCodes(getMessage("help-backup-remove")));
        sender.sendMessage(applyColorCodes(getMessage("help-backup-removeall")));
        sender.sendMessage(applyColorCodes(getMessage("help-backup-list")));
        sender.sendMessage(applyColorCodes(getMessage("help-rollback")));
        sender.sendMessage(applyColorCodes(getMessage("help-rollbackall")));
        sender.sendMessage(applyColorCodes(getMessage("help-end")));
    }
}

package de.shinjinjin.customjoinleave;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.Player;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.ChatColor;

public class CustomJoinLeave extends JavaPlugin implements Listener, CommandExecutor {

    @Override
    public void onEnable() {
        getLogger().info("CustomJoinLeave Plugin enabled!");
        getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        this.getCommand("customjoinleave").setExecutor(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomJoinLeave Plugin disabled!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String group = getPrimaryGroup(player);
        String prefix = getPrefix(player);

        FileConfiguration config = getConfig();
        String joinMessage = config.getString("groups." + group + ".join", config.getString("default.join"));

        // Debug-Nachricht
        getLogger().info("Join message before replacement: " + joinMessage);

        // Ersetze Platzhalter und wende Farben und Hex-Farbcodes an
        joinMessage = translateHexColorCodes(joinMessage);
        joinMessage = ChatColor.translateAlternateColorCodes('&', joinMessage);
        joinMessage = joinMessage.replace("%player%", player.getName());
        joinMessage = joinMessage.replace("%prefix%", prefix);

        // Debug-Nachricht
        getLogger().info("Join message after replacement: " + joinMessage);

        event.setJoinMessage(joinMessage);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String group = getPrimaryGroup(player);
        String prefix = getPrefix(player);

        FileConfiguration config = getConfig();
        String quitMessage = config.getString("groups." + group + ".leave", config.getString("default.leave"));

        // Debug-Nachricht
        getLogger().info("Quit message before replacement: " + quitMessage);

        // Ersetze Platzhalter und wende Farben und Hex-Farbcodes an
        quitMessage = translateHexColorCodes(quitMessage);
        quitMessage = ChatColor.translateAlternateColorCodes('&', quitMessage);
        quitMessage = quitMessage.replace("%player%", player.getName());
        quitMessage = quitMessage.replace("%prefix%", prefix);

        // Debug-Nachricht
        getLogger().info("Quit message after replacement: " + quitMessage);

        event.setQuitMessage(quitMessage);
    }

    private String getPrimaryGroup(Player player) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            return user.getPrimaryGroup();
        } else {
            return "default";  // Fallback, falls der Spieler keine Gruppe hat
        }
    }

    private String getPrefix(Player player) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            String prefix = user.getCachedData().getMetaData().getPrefix();
            if (prefix != null) {
                // Manuelles Ersetzen von & durch §
                prefix = prefix.replace("&", "§");

                // Hex-Farbcodes übersetzen
                prefix = translateHexColorCodes(prefix);
                return prefix;
            }
        }
        return ""; // Rückfall, falls kein Prefix vorhanden ist
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("customjoinleave")) { // Hauptbefehl
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) { // Unterbefehl
                    if (sender.hasPermission("customjoinleave.reload")) {
                        reloadConfig();
                        sender.sendMessage(ChatColor.GREEN + "CustomJoinLeave configuration reloaded!");
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Unknown subcommand. Usage: /customjoinleave reload");
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /customjoinleave <subcommand>");
                return true;
            }
        }
        return false;
    }

    // Methode zur Übersetzung von Hex-Farbcodes
    private String translateHexColorCodes(String message) {
        StringBuilder builder = new StringBuilder();
        int length = message.length();

        for (int i = 0; i < length; i++) {
            char c = message.charAt(i);
            if (c == '#' && i + 7 <= length) {
                String hexCode = message.substring(i + 1, i + 7);
                if (hexCode.matches("[A-Fa-f0-9]{6}")) {
                    builder.append("§x");
                    for (char hexChar : hexCode.toCharArray()) {
                        builder.append('§').append(hexChar);
                    }
                    i += 6; // Überspringe die 6 Zeichen des Hexcodes
                } else {
                    builder.append(c); // Es ist kein gültiger Hex-Farbcode, füge das Zeichen normal hinzu
                }
            } else {
                builder.append(c); // Kein Hex-Farbcode, füge das Zeichen normal hinzu
            }
        }

        return builder.toString();
    }
}

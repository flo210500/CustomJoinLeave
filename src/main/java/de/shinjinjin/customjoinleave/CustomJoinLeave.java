package de.shinjinjin.customjoinleave;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class CustomJoinLeave extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("customjoinleave")).setExecutor(this);
        Objects.requireNonNull(getCommand("customjoinleave")).setTabCompleter(this);
        getLogger().info("CustomJoinLeave v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomJoinLeave disabled.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String message = resolveMessage(player, "join");
        if (message == null || message.equalsIgnoreCase("none")) {
            event.joinMessage(null);
        } else {
            event.joinMessage(formatMessage(message, player));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String message = resolveMessage(player, "leave");
        if (message == null || message.equalsIgnoreCase("none")) {
            event.quitMessage(null);
        } else {
            event.quitMessage(formatMessage(message, player));
        }
    }

    private String resolveMessage(Player player, String type) {
        FileConfiguration config = getConfig();
        String group = getPrimaryGroup(player);
        String msg = config.getString("groups." + group + "." + type);
        if (msg == null) {
            msg = config.getString("default." + type);
        }
        return msg;
    }

    private Component formatMessage(String message, Player player) {
        String prefix = getPrefix(player);
        message = message
                .replace("%player%", player.getName())
                .replace("%prefix%", prefix != null ? prefix : "");
        message = legacyToMiniMessage(message);
        return MINI_MESSAGE.deserialize(message);
    }

    private String legacyToMiniMessage(String input) {
        if (input == null) return "";
        // Hex: &#RRGGBB oder #RRGGBB -> <#RRGGBB>
        input = input.replaceAll("&(#[A-Fa-f0-9]{6})", "<$1>");
        input = input.replaceAll("(?<!<)(#[A-Fa-f0-9]{6})(?![>A-Fa-f0-9])", "<$1>");
        // Legacy &-Codes
        input = input
                .replace("&0", "<black>").replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>").replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                .replace("&6", "<gold>").replace("&7", "<gray>")
                .replace("&8", "<dark_gray>").replace("&9", "<blue>")
                .replace("&a", "<green>").replace("&b", "<aqua>")
                .replace("&c", "<red>").replace("&d", "<light_purple>")
                .replace("&e", "<yellow>").replace("&f", "<white>")
                .replace("&A", "<green>").replace("&B", "<aqua>")
                .replace("&C", "<red>").replace("&D", "<light_purple>")
                .replace("&E", "<yellow>").replace("&F", "<white>")
                .replace("&l", "<bold>").replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>").replace("&o", "<italic>")
                .replace("&k", "<obfuscated>").replace("&r", "<reset>");
        return input;
    }

    private String getPrimaryGroup(Player player) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            return user != null ? user.getPrimaryGroup() : "default";
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Konnte LuckPerms-Gruppe fuer " + player.getName() + " nicht laden.", e);
            return "default";
        }
    }

    private String getPrefix(Player player) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                return user.getCachedData().getMetaData().getPrefix();
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Konnte LuckPerms-Prefix fuer " + player.getName() + " nicht laden.", e);
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("customjoinleave")) return false;

        if (args.length == 0) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Verwendung: /customjoinleave <reload>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("customjoinleave.reload")) {
                    sender.sendMessage(MINI_MESSAGE.deserialize("<red>Du hast keine Berechtigung fuer diesen Befehl."));
                    return true;
                }
                reloadConfig();
                sender.sendMessage(MINI_MESSAGE.deserialize("<green>CustomJoinLeave Konfiguration wurde neu geladen!"));
                break;
            default:
                sender.sendMessage(MINI_MESSAGE.deserialize("<red>Unbekannter Unterbefehl. Verwendung: /customjoinleave reload"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload");
        }
        return List.of();
    }
}

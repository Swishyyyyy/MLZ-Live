package com.mlz.boatlag;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /boatlag reload  -> re-reads config.yml
 * /boatlag debug   -> shows every snap-back near another boat in your action bar (for testing)
 */
public final class BoatLagCommand implements TabExecutor {

    private final MLZBoatLag plugin;

    public BoatLagCommand(MLZBoatLag plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false; // shows the usage line from plugin.yml
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadSettings();
                sender.sendMessage("MLZBoatLag reloaded. Snap-back matched to "
                        + plugin.settings().referencePingMillis() + " ms ping"
                        + (plugin.settings().enabled() ? "." : " (OFF: plain vanilla)."));
            }
            case "debug" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only players can use debug.");
                    return true;
                }
                boolean on = plugin.toggleDebug(player.getUniqueId());
                sender.sendMessage("Snap-back readout " + (on ? "ON" : "OFF") + ".");
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? List.of("reload", "debug") : List.of();
    }
}

package fr.minigames.commands;

import fr.minigames.Main;
import fr.minigames.api.MiniGame;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HubCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;

        MiniGame game = Main.getInstance().getGameManager().getCurrentGame();
        if (game != null && game.getPlayers().contains(player)) {
            game.removePlayer(player);
            player.sendMessage("§eTu es retourné au Hub.");
            // TP au spawn du serveur
            player.teleport(player.getWorld().getSpawnLocation());
        } else {
            player.sendMessage("§cTu n'es pas dans un mini-jeu.");
        }
        return true;
    }
}
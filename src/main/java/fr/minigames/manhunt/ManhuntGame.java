package fr.minigames.manhunt;

import fr.minigames.Main;
import fr.minigames.api.MiniGame;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class ManhuntGame extends MiniGame {

    @Override
    public String getName() {
        return "Manhunt";
    }

    @Override
    public void onStart() {
        Bukkit.broadcastMessage("§6===============================");
        Bukkit.broadcastMessage("§e Démarrage du MANHUNT !");
        Bukkit.broadcastMessage("§6===============================");

        // Enregistrer les événements DE CE JEU SEULEMENT
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());

        // Téléporter et équiper les joueurs
        for (Player p : players) {
            p.teleport(new Location(p.getWorld(), 0, 100, 0)); // Exemple
            p.getInventory().clear();
            p.getInventory().addItem(new ItemStack(Material.COMPASS));
            p.sendMessage("§eLa partie commence !");
        }
    }

    @Override
    public void onStop() {
        Bukkit.broadcastMessage("§cFin du Manhunt !");

        // IMPORTANT: Désenregistrer les événements pour ne pas qu'ils continuent
        // de tourner quand le jeu est fini !
        HandlerList.unregisterAll(this);

        players.clear();
    }

    // Exemple d'événement qui ne marche QUE si le jeu est lancé
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // On vérifie si le joueur fait partie de ce jeu
        if (players.contains(event.getPlayer())) {
            // Logique spécifique au Manhunt ici...
        }
    }
}
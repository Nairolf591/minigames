package fr.minigames.listeners;

import fr.minigames.Main;
import fr.minigames.api.MiniGame;
import fr.minigames.games.manhunt.ManhuntGame;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§8Sélection de Jeux")) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();

        if (event.getCurrentItem().getType() == Material.COMPASS) {
            player.closeInventory();

            // 1. On vérifie si un jeu existe déjà
            MiniGame game = Main.getInstance().getGameManager().getCurrentGame();

            // 2. Si aucun jeu n'est lancé, on en crée UN seul
            if (game == null) {
                player.sendMessage("§7Création d'un nouveau lobby Manhunt...");
                game = new ManhuntGame();
                Main.getInstance().getGameManager().setGame(game);
            }

            // 3. On ajoute le joueur au jeu existant (ou celui qu'on vient de créer)
            Main.getInstance().getGameManager().joinCurrentGame(player);
        }
    }
}
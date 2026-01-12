package fr.minigames.listeners;

import fr.minigames.Main;
import fr.minigames.manhunt.ManhuntGame;
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
            player.sendMessage("§7Préparation du Manhunt...");

            // 1. On instancie le jeu
            ManhuntGame game = new ManhuntGame();

            // 2. On dit au Manager : "C'est ce jeu qui est actif maintenant"
            Main.getInstance().getGameManager().setGame(game);

            // 3. On ajoute le joueur
            Main.getInstance().getGameManager().joinCurrentGame(player);
        }
    }
}
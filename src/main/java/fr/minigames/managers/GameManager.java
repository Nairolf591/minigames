package fr.minigames.managers;

import fr.minigames.api.MiniGame;
import org.bukkit.entity.Player;

public class GameManager {

    private MiniGame currentGame; // Le jeu actuellement en cours

    public void setGame(MiniGame game) {
        // Si un jeu tournait déjà, on l'arrête proprement
        if (this.currentGame != null) {
            this.currentGame.onStop();
        }

        this.currentGame = game;

        // On lance le nouveau
        if (this.currentGame != null) {
            this.currentGame.onStart();
        }
    }

    public MiniGame getCurrentGame() {
        return currentGame;
    }

    // Méthode helper pour ajouter un joueur au jeu actuel
    public void joinCurrentGame(Player player) {
        if (currentGame != null) {
            currentGame.addPlayer(player);
        } else {
            player.sendMessage("§cAucun jeu n'est lancé pour le moment !");
        }
    }
}
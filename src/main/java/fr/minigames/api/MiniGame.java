package fr.minigames.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public abstract class MiniGame implements Listener {

    protected List<Player> players = new ArrayList<>();

    // Chaque jeu doit avoir un nom
    public abstract String getName();

    // Ce qui se passe au démarrage du jeu
    public abstract void onStart();

    // Ce qui se passe à l'arrêt du jeu
    public abstract void onStop();

    public void addPlayer(Player player) {
        players.add(player);
        player.sendMessage("§a[MiniGames] §7Tu as rejoint : §e" + getName());
    }

    public void removePlayer(Player player) {
        players.remove(player);
        player.sendMessage("§c[MiniGames] §7Tu as quitté le jeu.");
    }

    public List<Player> getPlayers() {
        return players;
    }
}
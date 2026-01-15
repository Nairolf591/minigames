package fr.minigames;

import fr.minigames.commands.HubCommand;
import fr.minigames.games.manhunt.ManhuntCommand;
import fr.minigames.commands.MenuCommand;
import fr.minigames.listeners.MenuListener;
import fr.minigames.managers.GameManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialiser le Manager
        this.gameManager = new GameManager();

        // Enregistrer la commande /menu
        getCommand("menu").setExecutor(new MenuCommand());

        //Enregistrer les événements (les clics)
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        // Enregistrer la commande /manhunt
        ManhuntCommand manhuntCmd = new ManhuntCommand();
        getCommand("manhunt_config").setExecutor(manhuntCmd);
        getServer().getPluginManager().registerEvents(manhuntCmd, this); // Important pour les clics !

        // Enregistrer le /hub
        getCommand("hub").setExecutor(new HubCommand());

        getLogger().info("Le plugin MiniGames est activé !");
    }

    public static Main getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
}
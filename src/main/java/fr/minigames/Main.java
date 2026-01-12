package fr.minigames;

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

        // 1. Initialiser le Manager
        this.gameManager = new GameManager();

        // 2. Enregistrer la commande /menu
        getCommand("menu").setExecutor(new MenuCommand());

        // 3. Enregistrer les événements (les clics)
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        getLogger().info("Le plugin MiniGames est activé !");
    }

    public static Main getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
}
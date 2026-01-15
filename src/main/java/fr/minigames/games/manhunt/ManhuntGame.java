package fr.minigames.games.manhunt;

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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class ManhuntGame extends MiniGame {

    private final ManhuntConfig config;
    private boolean isStarted = false;
    private boolean isFrozen = false;
    private final java.util.Map<java.util.UUID, Integer> hunterTargets = new java.util.HashMap<>();

    public ManhuntGame() {
        this.config = new ManhuntConfig();
    }

    public ManhuntConfig getConfig() { return config; }

    @Override
    public String getName() { return "Manhunt"; }

    // --- LOGIQUE DU LOBBY ---

    @Override
    public void onStart() {
        // Cette méthode est appelée quand le plugin met le jeu en "Mode Actif" (Lobby)
        Bukkit.broadcastMessage("§e[Manhunt] §7Le Lobby est ouvert ! Rejoignez via le menu.");
    }

    @Override
    public void addPlayer(Player player) {
        // On vérifie si le joueur est déjà dedans avant d'ajouter (sécurité)
        if (players.contains(player)) {
            player.sendMessage("§cTu es déjà dans ce lobby !");
            return;
        }

        super.addPlayer(player); // Ajoute à la liste et envoie le message de base

        // Téléportation et Clear
        player.teleport(new Location(player.getWorld(), 0.5, 100, 0.5)); // .5 pour être au centre du bloc
        player.getInventory().clear();

        // Message à TOUS les joueurs DE LA PARTIE (pas tout le serveur)
        String joinMsg = "§7[§a+§7] §f" + player.getName() + " §7a rejoint le Manhunt (§e" + players.size() + "§7)";
        for (Player p : players) {
            p.sendMessage(joinMsg);
        }
    }

    // --- LOGIQUE DU DÉMARRAGE ---

    public void startGameReal() {
        if (players.size() < 2) {
            Bukkit.broadcastMessage("§cPas assez de joueurs !");
            return;
        }

        isStarted = true;
        // --- DANS startGameReal() ---

        // Tâche automatique : Met à jour la boussole toutes les 2 secondes (40 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isStarted) {
                    this.cancel();
                    return;
                }

                // On parcourt tous les joueurs
                for (Player p : players) {
                    // Si c'est un Hunter et qu'il tient une boussole
                    if (!config.isRunner(p.getUniqueId()) && p.getInventory().contains(Material.COMPASS)) {
                        updateCompassLocation(p);
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 40L); // 40 ticks = 2 secondes

        isFrozen = true; // On active le freeze

        // 1. Choisir un point de départ aléatoire
        // Pour l'instant on prend un joueur au pif comme "ancre" ou on génère des coords
        Player anchor = players.get(0);
        Location startLoc = anchor.getLocation(); // À changer plus tard pour un random TP

        // 2. TP tout le monde et donner les kits
        for (Player p : players) {
            p.teleport(startLoc);
            p.getInventory().clear();

            // Distribution des rôles et kits
            if (config.isRunner(p.getUniqueId())) {
                p.sendMessage("§6§lTU ES RUNNER ! §eCours !");
                if (config.getRunnerKit() != null) p.getInventory().setContents(config.getRunnerKit());
            } else {
                p.sendMessage("§b§lTU ES HUNTER ! §7Attends la fin du freeze...");
                p.getInventory().addItem(new ItemStack(Material.COMPASS)); // Toujours la boussole
                if (config.getHunterKit() != null) p.getInventory().setContents(config.getHunterKit());
            }
        }

        Bukkit.broadcastMessage("§cLA PARTIE COMMENCE !");

        // 3. Gestion du Timer de Freeze
        new BukkitRunnable() {
            int timer = config.getFreezeTime();

            @Override
            public void run() {
                if (!isStarted) { this.cancel(); return; } // Si le jeu est coupé de force

                if (timer > 0) {
                    Bukkit.broadcastMessage("§bLes Hunters sont gelés encore " + timer + "s...");
                    timer--;
                } else {
                    isFrozen = false; // FIN DU FREEZE
                    Bukkit.broadcastMessage("§c§lLES HUNTERS SONT LIBÉRÉS !");
                    this.cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L); // 20 ticks = 1 seconde
    }

    @Override
    public void onStop() {
        HandlerList.unregisterAll(this);
        players.clear();
        isStarted = false;
    }

    // --- ÉVÉNEMENTS ---

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!isStarted || !isFrozen) return; // Si le jeu n'a pas commencé ou plus de freeze, on s'en fiche

        Player p = event.getPlayer();
        if (!players.contains(p)) return;

        // Si le joueur est un HUNTER et qu'on est en période de FREEZE
        if (!config.isRunner(p.getUniqueId())) {
            // On empêche le mouvement X et Z, mais on laisse la caméra bouger
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
            }
        }
    }
    @EventHandler
    public void onCompassUse(org.bukkit.event.player.PlayerInteractEvent event) {
        if (!isStarted) return;

        Player hunter = event.getPlayer();
        if (event.getItem() == null || event.getItem().getType() != Material.COMPASS) return;
        if (!event.getAction().name().contains("RIGHT")) return; // Clic droit seulement
        if (config.isRunner(hunter.getUniqueId())) return; // Les runners n'utilisent pas la boussole

        List<java.util.UUID> runners = config.getRunners();
        if (runners.isEmpty()) {
            hunter.sendMessage("§cJl n'y a aucun Runner à traquer !");
            return;
        }

        // --- LOGIQUE DE CHANGEMENT DE CIBLE ---

        // 1. On récupère l'index actuel
        int currentIndex = hunterTargets.getOrDefault(hunter.getUniqueId(), -1);

        // 2. On passe au suivant (+1)
        int nextIndex = currentIndex + 1;

        // 3. Si on dépasse la fin de la liste, on boucle au début (0)
        if (nextIndex >= runners.size()) {
            nextIndex = 0;
        }

        // 4. On sauvegarde le nouveau choix
        hunterTargets.put(hunter.getUniqueId(), nextIndex);

        // 5. On informe le joueur et on force la mise à jour immédiate
        java.util.UUID targetUUID = runners.get(nextIndex);
        Player targetPlayer = Bukkit.getPlayer(targetUUID);

        String targetName = (targetPlayer != null) ? targetPlayer.getName() : "Déconnecté";

        hunter.sendMessage("§aTu traques maintenant : §6§l" + targetName);
        hunter.playSound(hunter.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 2f);

        updateCompassLocation(hunter);
    }

    private void updateCompassLocation(Player hunter) {
        List<java.util.UUID> runners = config.getRunners();
        if (runners.isEmpty()) return;

        // Récupérer l'index actuel, ou 0 par défaut
        int index = hunterTargets.getOrDefault(hunter.getUniqueId(), 0);

        // Sécurité : si l'index dépasse la liste (ex: un runner a quitté), on revient à 0
        if (index >= runners.size()) index = 0;

        java.util.UUID targetUUID = runners.get(index);
        Player targetRunner = Bukkit.getPlayer(targetUUID);

        // Si le runner est connecté et dans le même monde
        if (targetRunner != null && targetRunner.isOnline() && targetRunner.getWorld().equals(hunter.getWorld())) {
            hunter.setCompassTarget(targetRunner.getLocation());
            // On ne spamme pas de message ici, car ça s'exécute toutes les 2 secondes
        }
    }
}
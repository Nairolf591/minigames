package fr.minigames.games.manhunt;

import fr.minigames.Main;
import fr.minigames.api.MiniGame;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ManhuntGame extends MiniGame {

    private final ManhuntConfig config;
    private boolean isStarted = false;
    private boolean isFrozen = false;
    private final java.util.Map<UUID, Integer> hunterTargets = new java.util.HashMap<>();

    public ManhuntGame() {
        this.config = new ManhuntConfig();
    }

    public ManhuntConfig getConfig() { return config; }

    @Override
    public String getName() { return "Manhunt"; }

    // --- LOBBY ---

    @Override
    public void onStart() {
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());

        Bukkit.broadcastMessage("§e[Manhunt] §7Le Lobby est ouvert ! Rejoignez via le menu.");
    }

    @Override
    public void addPlayer(Player player) {
        if (players.contains(player)) {
            player.sendMessage("§cTu es déjà dans ce lobby !");
            return;
        }
        super.addPlayer(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(new Location(player.getWorld(), 0.5, 100, 0.5));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 3 * 20, 255, false, false));
        player.getInventory().clear();
        player.setHealth(20);
        player.setFoodLevel(20);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        String joinMsg = "§7[§a+§7] §f" + player.getName() + " §7a rejoint le Manhunt (§e" + players.size() + "§7)";
        for (Player p : players) p.sendMessage(joinMsg);
    }

    // --- DÉMARRAGE ---

    public void startGameReal() {
        if (players.size() < 2) {
            Bukkit.broadcastMessage("§cPas assez de joueurs (Min 2) !");
            return;
        }

        // Vérif runner/hunter
        int runnerCount = 0;
        for (Player p : players) {
            if (config.isRunner(p.getUniqueId())) runnerCount++;
        }
        if (runnerCount == 0) {
            Bukkit.broadcastMessage("§cIl faut au moins un RUNNER !");
            return;
        }
        if (players.size() - runnerCount == 0) {
            Bukkit.broadcastMessage("§cIl faut au moins un HUNTER !");
            return;
        }

        isStarted = true;

        // 1. Tâche Boussole
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isStarted) { this.cancel(); return; }
                for (Player p : players) {
                    // Seuls les Hunters VIVANTS (Survival) ont besoin de la boussole à jour
                    if (!config.isRunner(p.getUniqueId())
                            && p.getGameMode() == GameMode.SURVIVAL
                            && p.getInventory().contains(Material.COMPASS)) {
                        updateCompassLocation(p);
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 40L);

        // 2. Freeze
        isFrozen = true;
        final int totalFreezeTime = config.getFreezeTime();

        for (Player p : players) {
            if (!config.isRunner(p.getUniqueId())) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, totalFreezeTime * 20, 255, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, totalFreezeTime * 20, 255, false, false));
                            }
        }

        new BukkitRunnable() {
            int timer = totalFreezeTime;

            @Override
            public void run() {
                if (!isStarted) { this.cancel(); return; }
                if (timer > 0) {
                    if (timer <= 5 || timer % 10 == 0) {
                        Bukkit.broadcastMessage("§bLibération des Hunters dans §f" + timer + "s");
                        for(Player p : players) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                    }
                    timer--;
                } else {
                    isFrozen = false;
                    Bukkit.broadcastMessage("§c§lLES HUNTERS SONT LIBÉRÉS !");
                    for(Player p : players) {
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
                        p.removePotionEffect(PotionEffectType.SLOWNESS);
                        p.removePotionEffect(PotionEffectType.RESISTANCE);
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0L, 20L);

// 3. Téléportation aléatoire et Kits
        Bukkit.broadcastMessage("§7Recherche d'un point de départ aléatoire...");

        // On utilise le monde du premier joueur pour chercher la position
        org.bukkit.World gameWorld = players.get(0).getWorld();
        Location startLoc = findRandomLocation(gameWorld);

        Bukkit.broadcastMessage("§aPoint de départ trouvé ! Téléportation...");

        for (Player p : players) {
            p.teleport(startLoc); // Tout le monde au même endroit aléatoire
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.setHealth(20);
            p.setFoodLevel(20);
            if (config.isRunner(p.getUniqueId())) {
                p.sendMessage("§6Rôle: §lRUNNER §e(Survivre)");
                if (config.getRunnerKit() != null) p.getInventory().setContents(config.getRunnerKit());
            } else {
                p.sendMessage("§bRôle: §lHUNTER §3(Tuer)");
                p.getInventory().addItem(new ItemStack(Material.COMPASS));
                if (config.getHunterKit() != null) p.getInventory().setContents(config.getHunterKit());
            }
        }
    }

    @Override
    public void onStop() {
        HandlerList.unregisterAll(this);

        players.clear();
        isStarted = false;
        hunterTargets.clear();
    }

    // --- GESTION DE LA MORT ---

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!isStarted) return;
        Player victim = event.getEntity();
        if (!players.contains(victim)) return;

        // On gère le drop nous-même
        event.getDrops().forEach(item -> victim.getWorld().dropItemNaturally(victim.getLocation(), item));
        event.getDrops().clear();

        // Message personnalisé et masquage du message vanilla
        event.setDeathMessage(null);

        if (config.isRunner(victim.getUniqueId())) {
            Bukkit.broadcastMessage("§c☠ Le Runner §l" + victim.getName() + " §cest mort !");

            // Forcer le respawn pour qu'il passe en spectateur
            new BukkitRunnable() {
                @Override
                public void run() {
                    victim.spigot().respawn();
                }
            }.runTaskLater(Main.getInstance(), 2L);

            // Vérification VICTOIRE (Délai augmenté à 20 ticks / 1 seconde pour être sûr qu'il est bien spectateur)
            new BukkitRunnable() {
                @Override
                public void run() {
                    checkWinCondition();
                }
            }.runTaskLater(Main.getInstance(), 20L);

        } else {
            Bukkit.broadcastMessage("§b☠ Le Hunter " + victim.getName() + " est mort (Respawn...)");
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!isStarted) return;
        Player p = event.getPlayer();
        if (!players.contains(p)) return;

        if (config.isRunner(p.getUniqueId())) {
            // --- RUNNER : PASSAGE EN SPECTATEUR ---
            event.setRespawnLocation(p.getLocation());
            p.setGameMode(GameMode.SPECTATOR);
            p.sendMessage("§cTu es éliminé !");

            // Vérification de secours : Si la verif de onDeath a échoué, celle-ci marchera
            new BukkitRunnable() {
                @Override
                public void run() {
                    checkWinCondition();
                }
            }.runTaskLater(Main.getInstance(), 10L);

        } else {
            // --- HUNTER : RETOUR AU COMBAT ---
            p.setGameMode(GameMode.SURVIVAL);
            p.sendMessage("§aTu respawn ! Go l'attraper !");

            // On rend le stuff après un petit délai
            new BukkitRunnable() {
                @Override
                public void run() {
                    p.getInventory().clear();
                    p.getInventory().addItem(new ItemStack(Material.COMPASS));
                    if (config.getHunterKit() != null) {
                        p.getInventory().setContents(config.getHunterKit());
                    }
                }
            }.runTaskLater(Main.getInstance(), 5L);
        }
    }

    private void checkWinCondition() {
        if (!isStarted) return;

        int runnersAlive = 0;
        int huntersPresent = 0;

        for (Player p : players) {
            if (p.isOnline()) {
                if (config.isRunner(p.getUniqueId())) {
                    // CRUCIAL : On vérifie s'il est ENCORE en Survie.
                    // Si onDeath/onRespawn a bien marché, il devrait être en SPECTATOR.
                    if (p.getGameMode() == GameMode.SURVIVAL) {
                        runnersAlive++;
                    }
                } else {
                    huntersPresent++;
                }
            }
        }

        // --- DEBUG CONSOLE (Regarde ta console noire quand tu tues le runner) ---
        Bukkit.getLogger().info("[Manhunt Debug] Runners Vivants: " + runnersAlive + " | Hunters Présents: " + huntersPresent);

        // Victoire Hunters : Plus aucun Runner en survie
        if (runnersAlive == 0) {
            finishGame("§b§lLES HUNTERS");
        }
        // Victoire Runners : Tous les Hunters ont quitté
        else if (huntersPresent == 0) {
            finishGame("§6§lLES RUNNERS (Forfait)");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (isStarted && players.contains(event.getPlayer())) {
            // Si un Runner quitte, ça peut finir la game
            // Si un Hunter quitte, la game continue
            new BukkitRunnable() {
                @Override
                public void run() {
                    checkWinCondition();
                }
            }.runTaskLater(Main.getInstance(), 5L);
        }
    }


    private void finishGame(String winners) {
        isStarted = false;
        Bukkit.broadcastMessage("§f=================================");
        Bukkit.broadcastMessage("§f       §lFIN DE LA PARTIE");
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§f   Vainqueurs : " + winners);
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§7 Retour au hub dans 10 secondes...");
        Bukkit.broadcastMessage("§f=================================");

        for (Player p : players) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            p.setGameMode(GameMode.SPECTATOR);
        }

        // Timer de retour au Hub
        new BukkitRunnable() {
            @Override
            public void run() {
                Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
                for (Player p : players) {
                    p.teleport(spawn);
                    p.setGameMode(GameMode.SURVIVAL);
                    p.getInventory().clear();
                    p.getActivePotionEffects().forEach(eff -> p.removePotionEffect(eff.getType()));
                    p.sendMessage("§eMerci d'avoir joué !");
                }
                Main.getInstance().getGameManager().stopGame();
            }
        }.runTaskLater(Main.getInstance(), 200L);
    }

    // --- AUTRES EVENTS ---

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // Optionnel
    }

    @EventHandler
    public void onCompassUse(org.bukkit.event.player.PlayerInteractEvent event) {
        if (!isStarted) return;
        Player hunter = event.getPlayer();
        if (event.getItem() == null || event.getItem().getType() != Material.COMPASS) return;
        if (!event.getAction().name().contains("RIGHT")) return;
        if (config.isRunner(hunter.getUniqueId())) return;

        List<UUID> runners = config.getRunners();
        // Filtrer pour ne garder que les runners VIVANTS
        List<UUID> aliveRunners = new ArrayList<>();
        for (UUID uuid : runners) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline() && p.getGameMode() == GameMode.SURVIVAL) {
                aliveRunners.add(uuid);
            }
        }

        if (aliveRunners.isEmpty()) {
            hunter.sendMessage("§cTous les runners sont morts !");
            return;
        }

        int currentIndex = hunterTargets.getOrDefault(hunter.getUniqueId(), -1);
        int nextIndex = currentIndex + 1;
        if (nextIndex >= aliveRunners.size()) nextIndex = 0;

        hunterTargets.put(hunter.getUniqueId(), nextIndex);

        UUID targetUUID = aliveRunners.get(nextIndex);
        Player targetPlayer = Bukkit.getPlayer(targetUUID);

        hunter.sendMessage("§aTu traques : §6§l" + targetPlayer.getName());
        hunter.playSound(hunter.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 2f);
        updateCompassLocation(hunter);
    }

    private void updateCompassLocation(Player hunter) {
        List<UUID> runners = config.getRunners();
        if (runners.isEmpty()) return;
        int index = hunterTargets.getOrDefault(hunter.getUniqueId(), 0);
        if (index >= runners.size()) index = 0;
        UUID targetUUID = runners.get(index);
        Player targetRunner = Bukkit.getPlayer(targetUUID);
        if (targetRunner != null && targetRunner.isOnline() && targetRunner.getWorld().equals(hunter.getWorld())) {
            hunter.setCompassTarget(targetRunner.getLocation());
        }
    }

    // --- UTILITAIRE : RANDOM TP ---

    private Location findRandomLocation(org.bukkit.World world) {
        java.util.Random random = new java.util.Random();
        int attempts = 0;

        // On essaie 50 fois de trouver un bon endroit. Si on trouve pas, on prend le spawn par défaut.
        while (attempts < 50) {
            // Génère X et Z entre -2000 et 2000
            int x = random.nextInt(4000) - 2000;
            int z = random.nextInt(4000) - 2000;

            // Trouve le bloc le plus haut (le sol)
            int y = world.getHighestBlockYAt(x, z);

            // On vérifie le bloc
            org.bukkit.block.Block block = world.getBlockAt(x, y, z);

            // Si le bloc n'est PAS liquide (Eau/Lave) et n'est pas du vide
            if (!block.isLiquid() && block.getType() != Material.AIR) {
                // C'est bon ! On charge le chunk pour éviter le lag au TP
                world.getChunkAt(block.getLocation()).load(true);

                // On retourne la position (Y + 1.5 pour atterrir debout sur le bloc)
                return new Location(world, x + 0.5, y + 1.5, z + 0.5);
            }
            attempts++;
        }

        // Sécurité : si on a vraiment pas de bol (monde océan ?), on retourne le spawn du monde
        return world.getSpawnLocation();
    }
}
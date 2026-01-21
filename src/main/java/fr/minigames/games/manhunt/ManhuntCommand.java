package fr.minigames.games.manhunt;

import fr.minigames.Main;
import fr.minigames.api.MiniGame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ManhuntCommand implements CommandExecutor, Listener {

    // Pour sauvegarder l'inventaire de l'admin pendant qu'il édite le kit
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, String> editingMode = new HashMap<>(); // "RUNNER" ou "HUNTER"

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;

        MiniGame currentGame = Main.getInstance().getGameManager().getCurrentGame();
        if (!(currentGame instanceof ManhuntGame)) {
            player.sendMessage("§cAucun Manhunt n'est lancé !");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage("§cRéservé aux admins.");
            return true;
        }

        // On ouvre directement le GUI (plus besoin de vérifier "args[0] == config")
        openConfigGUI(player, (ManhuntGame) currentGame);
        return true;
    }


    private void openConfigGUI(Player player, ManhuntGame game) {
        Inventory gui = Bukkit.createInventory(null, 27, "§8Config Manhunt");

        // Bouton Start
        ItemStack start = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = start.getItemMeta();
        meta.setDisplayName("§a§lDÉMARRER LA PARTIE");
        start.setItemMeta(meta);
        gui.setItem(22, start);

        // Bouton Edit Kit Runner
        ItemStack kitRunner = new ItemStack(Material.LEATHER_BOOTS);
        ItemMeta krm = kitRunner.getItemMeta();
        krm.setDisplayName("§eÉditer Kit Runner");
        kitRunner.setItemMeta(krm);
        gui.setItem(10, kitRunner);

        // Bouton Edit Kit Hunter
        ItemStack kitHunter = new ItemStack(Material.IRON_SWORD);
        ItemMeta khm = kitHunter.getItemMeta();
        khm.setDisplayName("§bÉditer Kit Hunter");
        kitHunter.setItemMeta(khm);
        gui.setItem(16, kitHunter);

// Dans openConfigGUI : Modifier l'item de l'horloge
        ItemStack freezeBtn = new ItemStack(Material.CLOCK);
        ItemMeta fMeta = freezeBtn.getItemMeta();
        fMeta.setDisplayName("§bTemps de Freeze: §f" + game.getConfig().getFreezeTime() + "s");
        fMeta.setLore(java.util.List.of(
                "§7Clic Gauche : §a+5 secondes",
                "§7Clic Droit : §c-5 secondes"
        ));
        freezeBtn.setItemMeta(fMeta);
        gui.setItem(13, freezeBtn);

        // Liste des joueurs
        int slot = 0;
        for (Player p : game.getPlayers()) {
            if (slot > 9) break;

            boolean isRunner = game.getConfig().isRunner(p.getUniqueId());
            ItemStack head = new ItemStack(isRunner ? Material.GOLDEN_HELMET : Material.PLAYER_HEAD);
            ItemMeta headMeta = head.getItemMeta();
            headMeta.setDisplayName(p.getName());
            headMeta.setLore(java.util.List.of(isRunner ? "§6Rôle: RUNNER" : "§7Rôle: HUNTER", "§eClique pour changer"));
            head.setItemMeta(headMeta);

            gui.setItem(slot, head);
            slot++;
        }

        player.openInventory(gui);
    }

    // --- GESTION DES CLICS GUI ---

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack item = event.getCurrentItem();

        // 1. GESTION DU MENU CONFIG (Le GUI coffre)
        if (title.equals("§8Config Manhunt")) {
            event.setCancelled(true);
            if (item == null || item.getType() == Material.AIR) return;

            ManhuntGame game = (ManhuntGame) Main.getInstance().getGameManager().getCurrentGame();

            if (item.getType() == Material.EMERALD_BLOCK) {
                player.closeInventory();
                game.startGameReal();
            } else if (item.getType() == Material.PLAYER_HEAD || item.getType() == Material.GOLDEN_HELMET) {
                String playerName = item.getItemMeta().getDisplayName();
                Player target = Bukkit.getPlayer(playerName);
                if (target != null) {
                    if (game.getConfig().isRunner(target.getUniqueId())) {
                        game.getConfig().removeRunner(target.getUniqueId());
                    } else {
                        game.getConfig().addRunner(target.getUniqueId());
                    }
                    openConfigGUI(player, game);
                }
            } else if (item.getItemMeta().getDisplayName().contains("Kit Runner")) {
                startEditingKit(player, "RUNNER");
            } else if (item.getItemMeta().getDisplayName().contains("Kit Hunter")) {
                startEditingKit(player, "HUNTER");
            } else if (item.getType() == Material.CLOCK) {
                // Gestion du temps de freeze
                if (event.isLeftClick()) {
                    game.getConfig().addFreezeTime(5);
                } else if (event.isRightClick()) {
                    game.getConfig().removeFreezeTime(5);
                }
                // IMPORTANT : On rouvre le menu pour mettre à jour l'affichage du temps
                openConfigGUI(player, game);
            }

            return;
        }



        // 2. GESTION DU MODE ÉDITION (Clic sur la slimeball DANS l'inventaire "E")
        if (editingMode.containsKey(player.getUniqueId())) {
            // Si on clique sur la slimeball, on annule le déplacement et on sauvegarde
            if (item != null && item.getType() == Material.SLIME_BALL && item.getItemMeta().getDisplayName().contains("SAUVEGARDER")) {
                event.setCancelled(true);
                saveKit(player);
            }
        }
    }

    // --- GESTION DU CLIC DROIT AVEC LA SLIMEBALL (INTERACT) ---
    // C'est ce qui manquait pour que ce soit fluide !
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // On vérifie si le joueur est en mode édition
        if (editingMode.containsKey(player.getUniqueId())) {
            ItemStack item = event.getItem();

            // Si clic droit avec la slimeball
            if (item != null && item.getType() == Material.SLIME_BALL && item.getItemMeta().getDisplayName().contains("SAUVEGARDER")) {
                if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    event.setCancelled(true);
                    saveKit(player);
                }
            }
        }
    }

    // --- LOGIQUE D'ÉDITION DE KIT ---

    private void startEditingKit(Player player, String mode) {
        player.closeInventory();
        ManhuntGame game = (ManhuntGame) Main.getInstance().getGameManager().getCurrentGame();

        // 1. Sauvegarder l'inventaire actuel de l'admin
        savedInventories.put(player.getUniqueId(), player.getInventory().getContents());
        editingMode.put(player.getUniqueId(), mode);

        // 2. Vider l'inventaire
        player.getInventory().clear();

        // 3. CHARGEMENT DU KIT EXISTANT (C'est ça qui te manquait pour la persistance)
        ItemStack[] existingKit = null;
        if (mode.equals("RUNNER")) {
            existingKit = game.getConfig().getRunnerKit();
        } else {
            existingKit = game.getConfig().getHunterKit();
        }

        // Si un kit existe déjà, on le met dans l'inventaire de l'admin
        if (existingKit != null) {
            player.getInventory().setContents(existingKit);
        }

        player.sendMessage("§eMode Édition (" + mode + "): §fPrépare ton inventaire.");
        player.sendMessage("§7Fais Clic-Droit avec la Slimeball pour valider.");

        // 4. Donner le bouton de validation (Slot 9)
        ItemStack validate = new ItemStack(Material.SLIME_BALL);
        ItemMeta meta = validate.getItemMeta();
        meta.setDisplayName("§a§lSAUVEGARDER LE KIT");
        validate.setItemMeta(meta);
        player.getInventory().setItem(8, validate);
    }

    private void saveKit(Player player) {
        String mode = editingMode.get(player.getUniqueId());
        ManhuntGame game = (ManhuntGame) Main.getInstance().getGameManager().getCurrentGame();

        // On récupère tout le contenu
        ItemStack[] kitContent = player.getInventory().getContents();

        // IMPORTANT : On parcourt le kit pour retirer la Slimeball de sauvegarde
        // (au cas où le joueur l'aurait déplacée du slot 8)
        for (int i = 0; i < kitContent.length; i++) {
            ItemStack item = kitContent[i];
            if (item != null && item.getType() == Material.SLIME_BALL && item.getItemMeta().getDisplayName().contains("SAUVEGARDER")) {
                kitContent[i] = null; // On supprime la slimeball du kit final
            }
        }

        if (mode.equals("RUNNER")) {
            game.getConfig().setRunnerKit(kitContent);
            player.sendMessage("§aKit Runner sauvegardé !");
        } else {
            game.getConfig().setHunterKit(kitContent);
            player.sendMessage("§aKit Hunter sauvegardé !");
        }

        // Restauration de l'inventaire original de l'admin
        if (savedInventories.containsKey(player.getUniqueId())) {
            player.getInventory().setContents(savedInventories.get(player.getUniqueId()));
            savedInventories.remove(player.getUniqueId());
        }
        editingMode.remove(player.getUniqueId());

        // Retour au menu
        openConfigGUI(player, game);
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (editingMode.containsKey(uuid)) {
            // On lui rend son inventaire s'il déco pour ne pas qu'il perde ses items perso
            event.getPlayer().getInventory().setContents(savedInventories.get(uuid));
            editingMode.remove(uuid);
            savedInventories.remove(uuid);
        }
    }
}
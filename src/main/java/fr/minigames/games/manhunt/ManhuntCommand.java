package fr.minigames.games.manhunt;

import fr.minigames.Main;
import fr.minigames.api.MiniGame;
import fr.minigames.games.manhunt.ManhuntGame;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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

        // On ouvre directement le menu si c'est un OP
        if (!player.isOp()) {
            player.sendMessage("§cRéservé aux admins.");
            return true;
        }

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
        gui.setItem(22, start); // Au milieu bas

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

        // Liste des joueurs pour choisir les runners
        int slot = 0;
        for (Player p : game.getPlayers()) {
            if (slot > 9) break; // Sécurité pour pas dépasser

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

    // --- GESTION DES CLICS ---

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // 1. GESTION DU MENU CONFIG
        if (title.equals("§8Config Manhunt")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;

            ManhuntGame game = (ManhuntGame) Main.getInstance().getGameManager().getCurrentGame();

            // Gestion start
            if (item.getType() == Material.EMERALD_BLOCK) {
                player.closeInventory();
                game.startGameReal();
                return;
            }

            // Gestion changement de rôle (clic sur tête/casque)
            if (item.getType() == Material.PLAYER_HEAD || item.getType() == Material.GOLDEN_HELMET) {
                String playerName = item.getItemMeta().getDisplayName();
                Player target = Bukkit.getPlayer(playerName);
                if (target != null) {
                    if (game.getConfig().isRunner(target.getUniqueId())) {
                        game.getConfig().removeRunner(target.getUniqueId());
                    } else {
                        game.getConfig().addRunner(target.getUniqueId());
                    }
                    openConfigGUI(player, game); // Rafraîchir
                }
            }

            // Gestion Édition Kit
            if (item.getItemMeta().getDisplayName().contains("Kit Runner")) {
                startEditingKit(player, "RUNNER");
            } else if (item.getItemMeta().getDisplayName().contains("Kit Hunter")) {
                startEditingKit(player, "HUNTER");
            }
        }

        // 2. GESTION DU MENU D'ÉDITION DE KIT (Le bouton valider)
        if (title.startsWith("§8Édition: ")) {
            // Si on clique sur le bouton de validation (Slot 8, soit le 9ème slot de la hotbar)
            if (event.getSlot() == 8 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.SLIME_BALL) {
                event.setCancelled(true);
                saveKit(player);
            }
        }
    }

    // --- LOGIQUE D'ÉDITION DE KIT ---

    private void startEditingKit(Player player, String mode) {
        player.closeInventory();

        // 1. Sauvegarder l'inventaire actuel de l'admin
        savedInventories.put(player.getUniqueId(), player.getInventory().getContents());
        editingMode.put(player.getUniqueId(), mode);

        // 2. Vider l'inventaire pour qu'il puisse mettre le stuff
        player.getInventory().clear();
        player.sendMessage("§eMode Édition: §fPlace les items dans ton inventaire.");

        // 3. Donner le bouton de validation
        ItemStack validate = new ItemStack(Material.SLIME_BALL);
        ItemMeta meta = validate.getItemMeta();
        meta.setDisplayName("§a§lSAUVEGARDER LE KIT");
        validate.setItemMeta(meta);
        player.getInventory().setItem(8, validate); // Slot 9 (index 8)

        // On ouvre un inventaire vide "fictif" ou on le laisse juste utiliser son inventaire
        // Ici on le laisse utiliser son propre inventaire, c'est plus intuitif pour s'équiper.
        // On change juste le titre (impossible sans ouvrir une GUI, donc on simule via message)
    }

    private void saveKit(Player player) {
        String mode = editingMode.get(player.getUniqueId());
        ManhuntGame game = (ManhuntGame) Main.getInstance().getGameManager().getCurrentGame();

        // On récupère tout le contenu SAUF le bouton de validation (slot 8)
        ItemStack[] kitContent = player.getInventory().getContents();
        kitContent[8] = null; // On enlève la slimeball

        if (mode.equals("RUNNER")) {
            game.getConfig().setRunnerKit(kitContent);
            player.sendMessage("§aKit Runner sauvegardé !");
        } else {
            game.getConfig().setHunterKit(kitContent);
            player.sendMessage("§aKit Hunter sauvegardé !");
        }

        // Restauration
        player.getInventory().setContents(savedInventories.get(player.getUniqueId()));
        savedInventories.remove(player.getUniqueId());
        editingMode.remove(player.getUniqueId());

        // Retour au menu
        openConfigGUI(player, game);
    }
}
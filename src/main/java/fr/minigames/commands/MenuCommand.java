package fr.minigames.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class MenuCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;

        // 1. Création de l'inventaire (9 cases, 18, 27...)
        // Le titre "Sélection de Jeux" est important pour le listener après
        Inventory gui = Bukkit.createInventory(null, 27, "§8Sélection de Jeux");

        // 2. Création de l'item Manhunt (Boussole)
        ItemStack manhuntItem = new ItemStack(Material.COMPASS);
        ItemMeta meta = manhuntItem.getItemMeta();
        meta.setDisplayName("§6§lManhunt");
        meta.setLore(Arrays.asList("§7Clique pour rejoindre", "§7le lobby du Manhunt."));
        manhuntItem.setItemMeta(meta);

        // 3. On place l'item au milieu (slot 13)
        gui.setItem(13, manhuntItem);

        // 4. On ouvre
        player.openInventory(gui);

        return true;
    }
}
package fr.minigames.games.manhunt;

import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ManhuntConfig {

    // Qui sont les coureurs ? (On stocke les UUID, c'est plus sûr que les noms)
    private final List<UUID> runners = new ArrayList<>();

    // Les kits (Tableaux d'items)
    private ItemStack[] runnerKit;
    private ItemStack[] hunterKit;

    // Temps de freeze en secondes
    private int freezeTime = 30;

    public List<UUID> getRunners() { return runners; }

    public void addRunner(UUID uuid) { if(!runners.contains(uuid)) runners.add(uuid); }
    public void removeRunner(UUID uuid) { runners.remove(uuid); }
    public boolean isRunner(UUID uuid) { return runners.contains(uuid); }

    public void setRunnerKit(ItemStack[] kit) { this.runnerKit = kit; }
    public ItemStack[] getRunnerKit() { return runnerKit; }

    public void setHunterKit(ItemStack[] kit) { this.hunterKit = kit; }
    public ItemStack[] getHunterKit() { return hunterKit; }

    public int getFreezeTime() { return freezeTime; }
    public void setFreezeTime(int time) { this.freezeTime = time; }

    public void addFreezeTime(int seconds) {
        this.freezeTime += seconds;
        if (this.freezeTime > 300) this.freezeTime = 300; // Max 5 minutes
    }

    public void removeFreezeTime(int seconds) {
        this.freezeTime -= seconds;
        if (this.freezeTime < 0) this.freezeTime = 0; // Pas de temps négatif
    }
}
package fr.filipe.hardcorelives;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gere le compteur de vies de chaque joueur, avec persistance dans lives.yml.
 * Supporte le mode "vies partagees en equipe" (pool commun).
 */
public class LivesManager {

    private final HardcoreLives plugin;
    private final File file;
    private final FileConfiguration store;
    private final ConcurrentHashMap<UUID, Integer> cache = new ConcurrentHashMap<>();

    // Pool commun pour le mode equipe
    private int teamPool;

    public LivesManager(HardcoreLives plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "lives.yml");
        if (!file.exists()) {
            try { plugin.getDataFolder().mkdirs(); file.createNewFile(); }
            catch (IOException e) { plugin.getLogger().warning("Impossible de creer lives.yml"); }
        }
        this.store = YamlConfiguration.loadConfiguration(file);
        this.teamPool = store.getInt("__team_pool__", plugin.getConfig().getInt("team-lives-pool", 10));
    }

    private boolean isShared() {
        return plugin.getConfig().getBoolean("shared-team-lives", false);
    }

    public int getLives(UUID id) {
        if (isShared()) return teamPool;
        if (cache.containsKey(id)) return cache.get(id);
        int start = plugin.getConfig().getInt("starting-lives", 3);
        int val = store.getInt(id.toString(), start);
        cache.put(id, val);
        return val;
    }

    public void setLives(UUID id, int value) {
        if (value < 0) value = 0;
        if (isShared()) {
            teamPool = value;
            store.set("__team_pool__", teamPool);
        } else {
            cache.put(id, value);
            store.set(id.toString(), value);
        }
        save();
    }

    public void addLives(UUID id, int delta) {
        setLives(id, getLives(id) + delta);
    }

    /**
     * Retire une vie au joueur. Retourne le nombre de vies restantes.
     * Si 0, declenche la mise hors-jeu.
     */
    public int loseLife(Player player) {
        UUID id = player.getUniqueId();
        int remaining = getLives(id) - 1;
        if (remaining < 0) remaining = 0;
        setLives(id, remaining);

        if (remaining <= 0) {
            putOut(player);
        } else {
            player.sendMessage(plugin.raw("life-lost").replace("%lives%", String.valueOf(remaining)));
        }
        return remaining;
    }

    /**
     * Met le joueur hors-jeu : spectateur ou ban selon la config.
     */
    public void putOut(Player player) {
        HardcoreLives.OutAction action = plugin.getOutAction();
        if (action == HardcoreLives.OutAction.BAN) {
            String reason = ChatColor.stripColor(plugin.raw("no-lives"));
            Bukkit.getBanList(org.bukkit.BanList.Type.NAME)
                    .addBan(player.getName(), reason, null, "HardcoreLives");
            player.kickPlayer(reason);
        } else {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(plugin.raw("no-lives"));
        }
    }

    /**
     * Reanime un joueur : lui rend des vies et le repasse en survie.
     */
    public void revive(Player player, int lives) {
        setLives(player.getUniqueId(), Math.max(1, lives));
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        // setHealth(20) evite la dependance a l'attribut MAX_HEALTH dont le
        // nom a change entre 1.21.1 (GENERIC_MAX_HEALTH) et 1.21.4 (MAX_HEALTH).
        // 20 = barre de vie pleine par defaut. Fonctionne sur toutes les 1.21.x.
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.sendMessage(plugin.raw("revived"));
    }

    public boolean isOut(Player player) {
        return getLives(player.getUniqueId()) <= 0;
    }

    public void save() {
        try { store.save(file); }
        catch (IOException e) { plugin.getLogger().warning("Echec sauvegarde lives.yml"); }
    }

    public void saveAll() {
        if (isShared()) store.set("__team_pool__", teamPool);
        else cache.forEach((id, v) -> store.set(id.toString(), v));
        save();
    }
}

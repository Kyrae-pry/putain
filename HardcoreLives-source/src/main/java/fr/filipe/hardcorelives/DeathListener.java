package fr.filipe.hardcorelives;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.GameMode;

/**
 * Au moment de la mort, on retire une vie. La logique de mise hors-jeu
 * (spectateur / ban) est dans LivesManager.
 */
public class DeathListener implements Listener {

    private final HardcoreLives plugin;
    private final LivesManager lives;

    public DeathListener(HardcoreLives plugin, LivesManager lives) {
        this.plugin = plugin;
        this.lives = lives;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        // On retire la vie ici ; la bascule en spectateur se fait au respawn
        // pour eviter les conflits avec l'ecran de mort vanilla.
        int remaining = lives.getLives(event.getEntity().getUniqueId()) - 1;
        if (remaining < 0) remaining = 0;
        lives.setLives(event.getEntity().getUniqueId(), remaining);

        if (remaining > 0) {
            event.getEntity().sendMessage(
                    plugin.raw("life-lost").replace("%lives%", String.valueOf(remaining)));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (lives.isOut(event.getPlayer())) {
            // Bascule differee d'un tick pour que le respawn vanilla se termine.
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getOutAction() == HardcoreLives.OutAction.BAN) {
                    lives.putOut(event.getPlayer());
                } else {
                    event.getPlayer().setGameMode(GameMode.SPECTATOR);
                    event.getPlayer().sendMessage(plugin.raw("no-lives"));
                }
            }, 1L);
        }
    }
}

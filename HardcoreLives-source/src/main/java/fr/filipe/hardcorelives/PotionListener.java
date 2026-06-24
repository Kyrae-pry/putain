package fr.filipe.hardcorelives;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Mode POTION : consommer une Potion de Resurrection rend des vies.
 */
public class PotionListener implements Listener {

    private final HardcoreLives plugin;
    private final LivesManager lives;

    public PotionListener(HardcoreLives plugin, LivesManager lives) {
        this.plugin = plugin;
        this.lives = lives;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (!ResurrectionPotion.isPotion(plugin, item)) return;

        int restored = plugin.getConfig().getInt("potion.lives-restored", 1);
        lives.addLives(event.getPlayer().getUniqueId(), restored);
        event.getPlayer().sendMessage(
                plugin.raw("potion-used").replace("%amount%", String.valueOf(restored)));
    }
}

package fr.filipe.hardcorelives;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Mode REVIVE : un joueur vivant utilise un "Totem de Reanimation" en
 * visant un joueur spectateur proche pour le ramener a la vie.
 *
 * Le totem est un item vanilla (TOTEM_OF_UNDYING) marque via PersistentData
 * pour eviter toute confusion avec un totem normal.
 */
public class ReviveManager implements Listener {

    private final HardcoreLives plugin;
    private final LivesManager lives;
    private final NamespacedKey tagKey;

    public ReviveManager(HardcoreLives plugin, LivesManager lives) {
        this.plugin = plugin;
        this.lives = lives;
        this.tagKey = new NamespacedKey(plugin, "revive_totem");

        if (plugin.getActiveMode() == HardcoreLives.GameMode.REVIVE) {
            registerRecipe();
        }
    }

    public ItemStack createTotem() {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Totem de Reanimation");
        meta.setLore(java.util.List.of(
                ChatColor.GRAY + "Clic droit en visant un",
                ChatColor.GRAY + "allie spectateur proche."));
        meta.getPersistentDataContainer().set(tagKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isTotem(ItemStack item) {
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte b = meta.getPersistentDataContainer().get(tagKey, PersistentDataType.BYTE);
        return b != null && b == 1;
    }

    private void registerRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "revive_totem_recipe");
        if (plugin.getServer().getRecipe(key) != null) return;
        ShapedRecipe recipe = new ShapedRecipe(key, createTotem());
        recipe.shape("GDG", "DTD", "GDG");
        recipe.setIngredient('G', Material.GOLD_BLOCK);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
        plugin.getServer().addRecipe(recipe);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (plugin.getActiveMode() != HardcoreLives.GameMode.REVIVE) return;
        Player user = event.getPlayer();
        ItemStack inHand = event.getItem();
        if (!isTotem(inHand)) return;
        if (!event.getAction().toString().contains("RIGHT")) return;

        event.setCancelled(true);

        // Cherche un joueur spectateur dans un rayon de 5 blocs.
        Player target = null;
        double best = Double.MAX_VALUE;
        for (Player p : user.getWorld().getPlayers()) {
            if (!lives.isOut(p)) continue;
            double d = p.getLocation().distanceSquared(user.getLocation());
            if (d <= 25 && d < best) { best = d; target = p; }
        }

        if (target == null) {
            user.sendMessage(ChatColor.RED + "Aucun allie spectateur a proximite (5 blocs).");
            return;
        }

        int castSeconds = plugin.getConfig().getInt("revive.cast-seconds", 5);
        user.sendMessage(plugin.raw("revive-start").replace("%seconds%", String.valueOf(castSeconds)));

        final Player finalTarget = target;
        final org.bukkit.Location startLoc = user.getLocation().clone();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Verifie que le reanimateur n'a pas trop bouge.
            if (user.getLocation().distanceSquared(startLoc) > 4) {
                user.sendMessage(ChatColor.RED + "Reanimation interrompue (tu as bouge).");
                return;
            }
            if (!finalTarget.isOnline() || !lives.isOut(finalTarget)) return;

            // Consomme un totem.
            if (inHand.getAmount() <= 1) user.getInventory().setItemInMainHand(null);
            else inHand.setAmount(inHand.getAmount() - 1);

            lives.revive(finalTarget, 1);
            finalTarget.teleport(user.getLocation());
        }, castSeconds * 20L);
    }
}

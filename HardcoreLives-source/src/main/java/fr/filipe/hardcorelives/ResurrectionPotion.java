package fr.filipe.hardcorelives;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Fabrique l'item "Potion de Resurrection" (base : ENCHANTED_GOLDEN_APPLE
 * renommee + marquee). Consommer cet item rend des vies.
 */
public final class ResurrectionPotion {

    private ResurrectionPotion() {}

    public static NamespacedKey key(HardcoreLives plugin) {
        return new NamespacedKey(plugin, "resurrection_potion");
    }

    public static ItemStack create(HardcoreLives plugin) {
        ItemStack item = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Potion de Resurrection");
        meta.setLore(java.util.List.of(
                ChatColor.GRAY + "Consomme cet objet pour",
                ChatColor.GRAY + "regagner une vie."));
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isPotion(HardcoreLives plugin, ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_GOLDEN_APPLE) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte b = meta.getPersistentDataContainer().get(key(plugin), PersistentDataType.BYTE);
        return b != null && b == 1;
    }

    public static void registerRecipe(HardcoreLives plugin) {
        NamespacedKey rk = new NamespacedKey(plugin, "resurrection_potion_recipe");
        if (plugin.getServer().getRecipe(rk) != null) return;
        ShapedRecipe recipe = new ShapedRecipe(rk, create(plugin));
        // Recette : 4 diamants, 4 blocs d'or, 1 etoile du nether au centre.
        recipe.shape("GDG", "DND", "GDG");
        recipe.setIngredient('G', Material.GOLD_BLOCK);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('N', Material.NETHER_STAR);
        plugin.getServer().addRecipe(recipe);
    }
}

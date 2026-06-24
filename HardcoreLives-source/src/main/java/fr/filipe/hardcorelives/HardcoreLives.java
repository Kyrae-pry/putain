package fr.filipe.hardcorelives;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public class HardcoreLives extends JavaPlugin {

    private LivesManager livesManager;
    private ReviveManager reviveManager;
    private GameMode activeMode;

    public enum GameMode { LIVES, REVIVE, POTION }
    public enum OutAction { SPECTATOR, BAN }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadActiveMode();

        this.livesManager = new LivesManager(this);
        this.reviveManager = new ReviveManager(this, livesManager);

        getServer().getPluginManager().registerEvents(
                new DeathListener(this, livesManager), this);
        getServer().getPluginManager().registerEvents(reviveManager, this);

        if (activeMode == GameMode.POTION) {
            ResurrectionPotion.registerRecipe(this);
            getServer().getPluginManager().registerEvents(
                    new PotionListener(this, livesManager), this);
        }

        getLogger().info("HardcoreLives active. Mode = " + activeMode);
    }

    @Override
    public void onDisable() {
        if (livesManager != null) livesManager.saveAll();
    }

    public void reloadActiveMode() {
        String m = getConfig().getString("mode", "LIVES").toUpperCase();
        try {
            this.activeMode = GameMode.valueOf(m);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Mode inconnu '" + m + "', LIVES par defaut.");
            this.activeMode = GameMode.LIVES;
        }
    }

    public GameMode getActiveMode() { return activeMode; }

    public OutAction getOutAction() {
        String a = getConfig().getString("on-death-out", "SPECTATOR").toUpperCase();
        try { return OutAction.valueOf(a); }
        catch (IllegalArgumentException e) { return OutAction.SPECTATOR; }
    }

    public String msg(String key) {
        String prefix = getConfig().getString("messages.prefix", "");
        String body = getConfig().getString("messages." + key, "");
        return ChatColor.translateAlternateColorCodes('&', prefix + body);
    }

    public String raw(String key) {
        return ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages." + key, ""));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("lives")) {
            return handleLives(sender, args);
        }
        if (command.getName().equalsIgnoreCase("hcl")) {
            return handleAdmin(sender, args);
        }
        return false;
    }

    private boolean handleLives(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Joueur introuvable.");
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("Usage : /lives <joueur>");
            return true;
        }
        int lives = livesManager.getLives(target.getUniqueId());
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&cHardcore&8] &e" + target.getName() + " &7a &e" + lives + " &7vie(s)."));
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hardcorelives.admin")) {
            sender.sendMessage(ChatColor.RED + "Permission refusee.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GRAY + "/hcl reload | set <joueur> <n> | give <joueur> <n> | revive <joueur> | mode");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
                reloadConfig();
                reloadActiveMode();
                sender.sendMessage(ChatColor.GREEN + "Config rechargee. Mode = " + activeMode);
                return true;
            case "mode":
                sender.sendMessage(ChatColor.GREEN + "Mode actif : " + activeMode
                        + " | sortie : " + getOutAction());
                return true;
            case "set":
                if (args.length < 3) { sender.sendMessage(ChatColor.RED + "/hcl set <joueur> <n>"); return true; }
                return adminSet(sender, args[1], args[2], false);
            case "give":
                if (args.length < 3) { sender.sendMessage(ChatColor.RED + "/hcl give <joueur> <n>"); return true; }
                return adminSet(sender, args[1], args[2], true);
            case "revive":
                if (args.length < 2) { sender.sendMessage(ChatColor.RED + "/hcl revive <joueur>"); return true; }
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage(ChatColor.RED + "Joueur introuvable."); return true; }
                livesManager.revive(t, 1);
                sender.sendMessage(ChatColor.GREEN + t.getName() + " reanime.");
                return true;
            case "resetmap":
                return handleResetMap(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Sous-commande inconnue.");
                return true;
        }
    }

    /**
     * Reset complet de la map. Pour eviter tout accident, il faut confirmer
     * avec /hcl resetmap CONFIRM. La sequence :
     *   1. on remet toutes les vies a zero (fichier lives.yml efface)
     *   2. on ecrit un flag RESET_PENDING
     *   3. on arrete le serveur
     * Au demarrage suivant, le script start (fourni) supprime les dossiers
     * de monde si le flag est present, et Paper regenere une map neuve.
     */
    private boolean handleResetMap(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("CONFIRM")) {
            sender.sendMessage(ChatColor.RED + "==============================================");
            sender.sendMessage(ChatColor.RED + " ATTENTION : ceci EFFACE la map et les vies.");
            sender.sendMessage(ChatColor.YELLOW + " Pour confirmer, tape : " + ChatColor.WHITE + "/hcl resetmap CONFIRM");
            sender.sendMessage(ChatColor.RED + "==============================================");
            return true;
        }

        getLogger().warning("RESET MAP demande par " + sender.getName() + ". Arret du serveur...");
        Bukkit.broadcastMessage(ChatColor.RED + "[Hardcore] Reset de la map en cours. Le serveur redemarre avec une nouvelle map.");

        // 1. efface les vies
        File livesFile = new File(getDataFolder(), "lives.yml");
        if (livesFile.exists()) livesFile.delete();

        // 2. ecrit le flag de reset a la racine du serveur
        try {
            File flag = new File(getServer().getWorldContainer(), "RESET_PENDING");
            flag.createNewFile();
        } catch (java.io.IOException e) {
            getLogger().severe("Impossible d'ecrire le flag RESET_PENDING : " + e.getMessage());
            sender.sendMessage(ChatColor.RED + "Echec : flag non ecrit. Reset annule.");
            return true;
        }

        // 3. arrete le serveur dans 3 secondes (laisse le temps au broadcast)
        getServer().getScheduler().runTaskLater(this, () -> Bukkit.shutdown(), 60L);
        return true;
    }

    private boolean adminSet(CommandSender sender, String name, String nStr, boolean add) {
        Player t = Bukkit.getPlayerExact(name);
        UUID id;
        if (t != null) id = t.getUniqueId();
        else { sender.sendMessage(ChatColor.RED + "Joueur introuvable (doit etre connecte)."); return true; }
        int n;
        try { n = Integer.parseInt(nStr); }
        catch (NumberFormatException e) { sender.sendMessage(ChatColor.RED + "Nombre invalide."); return true; }

        if (add) livesManager.addLives(id, n);
        else livesManager.setLives(id, n);
        sender.sendMessage(ChatColor.GREEN + "Vies de " + name + " = " + livesManager.getLives(id));
        return true;
    }

    public LivesManager getLivesManager() { return livesManager; }
}

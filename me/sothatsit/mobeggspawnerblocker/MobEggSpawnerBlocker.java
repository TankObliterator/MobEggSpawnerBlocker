package me.sothatsit.mobeggspawnerblocker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class MobEggSpawnerBlocker extends JavaPlugin implements Listener {

    private String message;
    private boolean blockCreative;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        reloadConfiguration();
    }

    public void reloadConfiguration() {
        this.saveDefaultConfig();
        this.reloadConfig();

        if (!getConfig().isSet("message") || !getConfig().isString("message")) {
            getConfig().set("message", "&cChanging spawners using mob eggs is disabled on this server");
            saveConfig();
        }

        if (!getConfig().isSet("block-creative") || !getConfig().isBoolean("block-creative")) {
            getConfig().set("block-creative", false);
            saveConfig();
        }

        this.message = getConfig().getString("message");
        this.blockCreative = getConfig().getBoolean("block-creative");
    }

    // Using HIGHEST priority and ignoring cancelled to play nice with protection plugins
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (!blockCreative && e.getPlayer().getGameMode() == GameMode.CREATIVE)
            return;

        if (e.getPlayer().isOp() || e.getPlayer().hasPermission("mobeggspawnerblocker.override"))
            return;

        // Ensure the player is right-clicking a block
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block b = e.getClickedBlock();
        if (b == null || b.getType() != Material.SPAWNER) // Modern name is SPAWNER
            return;

        ItemStack i = e.getItem();
        if (i == null)
            return;

        // Check if the item is any type of spawn egg (works for Armadillos, etc.)
        if (!i.getType().name().endsWith("_SPAWN_EGG"))
            return;

        // Cancel the event immediately
        e.setCancelled(true);

        // Notify the player
        if (message != null && !message.isEmpty()) {
            e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }

        // Logic to "reset" the spawner in case the client/server had a race condition
        CreatureSpawner cs = (CreatureSpawner) b.getState();
        final Location loc = cs.getLocation();
        final EntityType type = cs.getSpawnedType();

        Bukkit.getScheduler().runTaskLater(this, () -> {
            Block currentBlock = loc.getBlock();
            if (currentBlock.getType() == Material.SPAWNER) {
                CreatureSpawner currentCs = (CreatureSpawner) currentBlock.getState();
                currentCs.setSpawnedType(type);
                currentCs.update();
            }
        }, 1L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("mobeggspawnerblocker")) {
            if (!sender.hasPermission("mobeggspawnerblocker.reload")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            reloadConfiguration();
            sender.sendMessage(ChatColor.GREEN + "MobEggSpawnerBlocker config reloaded");
            return true;
        }
        return false;
    }
}

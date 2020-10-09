package cx.mia.sleepercentage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.UUID;

public final class Sleepercentage extends JavaPlugin implements Listener {

    public static NumberFormat percentFormat = NumberFormat.getPercentInstance();

    public ArrayList<UUID> sleepingPlayers = new ArrayList<java.util.UUID>();
    public ArrayList<UUID> enforcedPlayers = new ArrayList<UUID>();

    public FileConfiguration config;

    @Override
    public void onEnable() {

        this.saveDefaultConfig();

        config = this.getConfig();

        Bukkit.getPluginManager().registerEvents(this, this);

        Bukkit.getOnlinePlayers().forEach(player -> {
            if (!player.hasPermission("sleepercentage.exempt")) {
                enforcedPlayers.add(player.getUniqueId());
                if (player.isSleeping()) {
                    sleepingPlayers.add(player.getUniqueId());
                }
            }
        });

    }

    @Override
    public void onDisable() {
        sleepingPlayers.clear();
        enforcedPlayers.clear();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (!player.hasPermission("sleepercentage.exempt")) enforcedPlayers.add(player.getUniqueId());

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        enforcedPlayers.remove(player.getUniqueId());

    }

    /**
     * adds a player to {@link #sleepingPlayers} when they enter a bed and are not exempt.
     */
    @EventHandler
    public void onPlayerSleep(PlayerBedEnterEvent event) {

        Player player = event.getPlayer();

        if (player.hasPermission("sleepercentage.exempt")) return;

        World world = player.getWorld();

        sleepingPlayers.add(player.getUniqueId());

        int needed = Math.round(enforcedPlayers.size() * percentageFromString(config.getString("percentage")));

        enforcedPlayers.forEach(uuid -> {

            Player enforcedPlayer = Bukkit.getPlayer(uuid);

            if (sleepingPlayers.contains(enforcedPlayer.getUniqueId())) return;

            String message = ChatColor.translateAlternateColorCodes('&', config.getString("message"));
            message = message.replace("{PLAYER}", player.getDisplayName());
            message = message.replace("{CURRENT}", String.valueOf(sleepingPlayers.size()));
            message = message.replace("{NEEDED}", String.valueOf(needed));
            enforcedPlayer.sendMessage(message);

        });

        if (sleepingPlayers.size() >= needed) {

            world.setTime(0);

        }


    }

    /**
     * removes a player from {@link #sleepingPlayers} when they leave a bed
     */
    @EventHandler
    public void onPlayerWake(PlayerBedLeaveEvent event) { sleepingPlayers.remove(event.getPlayer()); }

    public static Float percentageFromString(String s) {

        try {
            return percentFormat.parse(s).floatValue();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return (float) 0;
    }
}

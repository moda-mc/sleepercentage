package cx.mia.sleepercentage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.UUID;

public final class Sleepercentage extends JavaPlugin implements Listener {

    public static NumberFormat percentFormat = NumberFormat.getPercentInstance();

    public FileConfiguration config;

    @Override
    public void onEnable() {

        this.saveDefaultConfig();

        Bukkit.getPluginManager().registerEvents(this, this);

        this.config = getConfig();

        Bukkit.getWorlds().forEach(this::update);

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) { update(event.getPlayer().getWorld()); }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) { update(event.getPlayer().getWorld()); }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) { update(event.getPlayer().getWorld()); }

    @EventHandler
    public void onPlayerWake(PlayerBedLeaveEvent event) { update(event.getPlayer().getWorld()); }


    // TODO fix the stream thing?
    public void update(World world) {

        ArrayList<UUID> enforcedPlayers = new ArrayList<>();
        ArrayList<UUID> sleepingPlayers = new ArrayList<>();

        for (Player player : world.getPlayers()) {
            UUID uuid = player.getUniqueId();
            if (!player.hasPermission("sleepercentage.exempt")) {

                if (!enforcedPlayers.contains(uuid)) enforcedPlayers.add(uuid);

                int required = Math.round(enforcedPlayers.size() * Sleepercentage.percentageFromString(config.getString("percentage")));

                if (player.isSleeping()) {

                    if (!sleepingPlayers.contains(uuid)) {
                        sleepingPlayers.add(uuid);

                        String sleeping = ChatColor.translateAlternateColorCodes('&', config.getString("messages.sleeping"));

                        sleeping = sleeping.replace("{PLAYER}", player.getDisplayName());
                        sleeping = sleeping.replace("{CURRENT}", String.valueOf(sleepingPlayers.size()));
                        sleeping = sleeping.replace("{NEEDED}", String.valueOf(required));

                        for (UUID enforcedPlayerUUID : enforcedPlayers) {
                            Player enforcedPlayer = Bukkit.getPlayer(enforcedPlayerUUID);
                            enforcedPlayer.sendMessage(sleeping);
                        }
                    }

                } else {
                    sleepingPlayers.remove(uuid);
                }
            } else {
                enforcedPlayers.remove(uuid);
            }
        }

        int required = Math.round(enforcedPlayers.size() * Sleepercentage.percentageFromString(config.getString("percentage")));

        if (sleepingPlayers.size() >= required && required > 0) {

            String skipped = ChatColor.translateAlternateColorCodes('&', config.getString("messages.skipped"));

            world.getPlayers().forEach(player -> {
                player.sendMessage(skipped);
            });

            world.setTime(0);
        }
    }

    public static Float percentageFromString(String s) {

        try {
            return percentFormat.parse(s).floatValue();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return (float) 0;
    }
}

package cx.mia.sleepercentage;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

public final class Sleepercentage extends JavaPlugin implements Listener {

    public FileConfiguration config;

    public static NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance();
    static final HashMap<String, Set<String>> PLAYERS_SLEEPING = new HashMap<>();
    static final HashMap<String, Integer> PLAYERS_NEEDED = new HashMap<>();
    static final Map<String, Integer> TASKS = new HashMap<>();

    @Override
    public void onEnable() {

        this.config = getConfig();

        this.saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);

    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldSwitch(PlayerChangedWorldEvent event) {

        Player player = event.getPlayer();

        if (player.hasPermission("sleepercentage.exempt")) return;

        PLAYERS_NEEDED.compute(event.getFrom().getName(), (k, v) -> v == null ? 0 : --v);
        PLAYERS_NEEDED.compute(event.getPlayer().getWorld().getName(), (k, v) -> v == null ? 0 : ++v);

        skip(event.getPlayer().getWorld().getName());
        // TODO trigger skip logic

    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (player.hasPermission("sleepercentage.exempt")) return;

        PLAYERS_NEEDED.compute(event.getPlayer().getWorld().getName(), (k, v) -> v == null ? 0 : ++v);

    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        if (player.hasPermission("sleepercentage.exempt")) return;

        World world = player.getWorld();

        PLAYERS_NEEDED.compute(world.getName(), (k, v) -> v == null ? 0 : --v);

        // TODO trigger skip logic

    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {

        String playerName = event.getPlayer().getName();
        UUID uuid = event.getPlayer().getUniqueId();
        String worldName = event.getPlayer().getWorld().getName();

        int task = Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {

            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isSleeping()) return;

            Set<String> playersSleeping = PLAYERS_SLEEPING.computeIfAbsent(worldName, k -> new HashSet<>());

            // TODO broadcast sleeping message
            playersSleeping.add(playerName);

        }, (long) (20 * config.getDouble("settings.default.sleep-wait")));

        TASKS.put(playerName, task);

        // TODO trigger skip logic

    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerWake(PlayerBedLeaveEvent event) {

        Player player = event.getPlayer();
        World world = player.getWorld();

        TASKS.computeIfPresent(player.getName(), (k, v) -> {
            Bukkit.getScheduler().cancelTask(v);
            return null;
        });

        Set<String> playersSleeping = PLAYERS_SLEEPING.computeIfAbsent(world.getName(), k -> new HashSet<>());

        playersSleeping.remove(player.getName());

    }

    public void skip(String worldName) {

        float percentage = percentageFromString(getSetting(worldName, "percentage"));
        int playersNeeded = Math.round(PLAYERS_NEEDED.get(worldName) * percentage);

        if (PLAYERS_SLEEPING.get(worldName).size() < playersNeeded) return;

        if (Bukkit.getWorld(worldName) == null) return;

        World world = Bukkit.getWorld(worldName);

        if (world.hasStorm() && (boolean) getSetting(worldName, "skip-storms")) {

            // TODO message



            world.setStorm(false);

        }

        if (world.getTime() > 12000 && (boolean) getSetting(worldName, "skip-nights")) {

            // TODO message

            world.setTime(0);

        }

    }

    public <T> T getSetting(String worldName, String setting) {

        if ( config.contains("settings.worlds." + worldName + "." + setting) ) {

            return (T) config.get("settings.worlds." + worldName + "." + setting);

        }

        return (T) config.get("settings.default." + setting);

    }


    // TODO fix the stream thing?
//    public void update(World world) {
//
//        ArrayList<UUID> enforcedPlayers = new ArrayList<>();
//        ArrayList<UUID> sleepingPlayers = new ArrayList<>();
//
//        for (Player player : world.getPlayers()) {
//
//            UUID uuid = player.getUniqueId();
//            if (!player.hasPermission("sleepercentage.exempt")) {
//
//                if (!enforcedPlayers.contains(uuid)) enforcedPlayers.add(uuid);
//
//                int required = Math.round(enforcedPlayers.size() * Sleepercentage.percentageFromString(config.getString("percentage")));
//
//                if (player.isSleeping()) {
//
//                    if (!sleepingPlayers.contains(uuid)) {
//                        sleepingPlayers.add(uuid);
//
//                        String sleeping = ChatColor.translateAlternateColorCodes('&', config.getString("messages.sleeping"));
//
//                        sleeping = sleeping.replace("{PLAYER}", player.getDisplayName());
//                        sleeping = sleeping.replace("{CURRENT}", String.valueOf(sleepingPlayers.size()));
//                        sleeping = sleeping.replace("{NEEDED}", String.valueOf(required));
//
//                        for (UUID enforcedPlayerUUID : enforcedPlayers) {
//                            Player enforcedPlayer = Bukkit.getPlayer(enforcedPlayerUUID);
//                            enforcedPlayer.sendMessage(sleeping);
//                        }
//                    }
//
//                } else {
//                    sleepingPlayers.remove(uuid);
//                }
//            } else {
//                enforcedPlayers.remove(uuid);
//            }
//        }
//
//        int required = Math.round(enforcedPlayers.size() * Sleepercentage.percentageFromString(config.getString("percentage")));
//
//        if (sleepingPlayers.size() >= required && required > 0) {
//
//            String skipped = ChatColor.translateAlternateColorCodes('&', config.getString("messages.skipped"));
//
//            world.getPlayers().forEach(player -> {
//                player.sendMessage(skipped);
//            });
//
//            world.setTime(0);
//        }
//    }

    public static Float percentageFromString(String s) {

        try { return PERCENT_FORMAT.parse(s).floatValue(); } catch (ParseException e) { e.printStackTrace(); }

        return (float) 0;
    }
}
package cx.mia.moda.sleepercentage;

import moda.plugin.moda.module.IMessage;
import moda.plugin.moda.module.Module;
import moda.plugin.moda.module.storage.NoStorageHandler;
import moda.plugin.moda.placeholder.ModaPlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitTask;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

public final class Sleepercentage extends Module<NoStorageHandler> implements Listener {

    public FileConfiguration config;

    public static NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance();
    static final HashMap<String, Set<String>> CURRENT_SLEEPERS = new HashMap<>();
    static final HashMap<String, Integer> ENFORCED_SLEEPERS = new HashMap<>();
    static final Map<String, Integer> TASKS = new HashMap<>();

    @Override
    public String getName() {
        return "Sleepercentage";
    }

    @Override
    public IMessage[] getMessages() {
        return SleepercentageMessage.values();
    }

    @Override
    public void onEnable() {

        this.config = getConfig();

        update();

        registerListener(this);

    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldSwitch(PlayerChangedWorldEvent event) {

        Player player = event.getPlayer();

        if (player.hasPermission("sleepercentage.exempt")) return;

        ENFORCED_SLEEPERS.compute(event.getFrom().getName(), (k, v) -> v == null ? 0 : --v);
        ENFORCED_SLEEPERS.compute(event.getPlayer().getWorld().getName(), (k, v) -> v == null ? 0 : ++v);

        skip(event.getPlayer().getWorld().getName());

    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (player.hasPermission("sleepercentage.exempt")) return;

        ENFORCED_SLEEPERS.compute(event.getPlayer().getWorld().getName(), (k, v) -> v == null ? 0 : ++v);

    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        if (player.hasPermission("sleepercentage.exempt")) return;

        String worldName = player.getWorld().getName();

        ENFORCED_SLEEPERS.compute(worldName, (k, v) -> v == null ? 0 : --v);

        skip(worldName);

    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {

        getLogger().info("PBEE"); // TODO

        String playerName = event.getPlayer().getName();
        UUID uuid = event.getPlayer().getUniqueId();
        String worldName = event.getPlayer().getWorld().getName();

        getLogger().info(playerName + worldName); // TODO

        BukkitTask task = this.getScheduler().delay((20 * (int) getSetting(worldName, "sleep-wait")), () -> {

            getLogger().info("task exec"); // TODO

            Player player = Bukkit.getPlayer(uuid);

            getLogger().info(String.valueOf(player == null)); // TODO

            if (player == null || !player.isSleeping()) return;

            Set<String> playersSleeping = CURRENT_SLEEPERS.computeIfAbsent(worldName, k -> new HashSet<>());

            float percentage = percentageFromString(getSetting(worldName, "percentage"));
            int playersNeeded = Math.round(ENFORCED_SLEEPERS.get(worldName) * percentage);

            playersSleeping.add(playerName);

            String message = ModaPlaceholderAPI.parsePlaceholders(
                    this.getLang().getMessage(
                            SleepercentageMessage.SLEEPING,
                                "CURRENT_SLEEPERS", String.valueOf(CURRENT_SLEEPERS.get(worldName).size()),
                                "NEEDED_SLEEPERS", playersNeeded), player);

            player.getWorld().getPlayers().forEach(p -> {
                p.sendMessage(message);
            });

            TASKS.remove(playerName);

            skip(worldName);

        });

        getLogger().info("task add"); // TODO

        TASKS.put(playerName, task.getTaskId());

    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerWake(PlayerBedLeaveEvent event) {

        Player player = event.getPlayer();
        World world = player.getWorld();

        TASKS.computeIfPresent(player.getName(), (k, v) -> {
            Bukkit.getScheduler().cancelTask(v);
            return null;
        });

        Set<String> playersSleeping = CURRENT_SLEEPERS.computeIfAbsent(world.getName(), k -> new HashSet<>());

        playersSleeping.remove(player.getName());

    }

    public void skip(String worldName) {

        float percentage = percentageFromString(getSetting(worldName, "percentage"));
        int playersNeeded = Math.round(ENFORCED_SLEEPERS.get(worldName) * percentage);

        if (CURRENT_SLEEPERS.get(worldName).size() < playersNeeded) return;

        if (Bukkit.getWorld(worldName) == null) return;

        World world = Bukkit.getWorld(worldName);

        if (world.getTime() > 12000 && (boolean) getSetting(worldName, "skip-nights")) {

            world.getPlayers().forEach(player -> getLang().getMessage(SleepercentageMessage.SKIP_NIGHT));

            world.setTime(0);

        }

        if (world.hasStorm() && (boolean) getSetting(worldName, "skip-storms")) {

            world.getPlayers().forEach(player -> getLang().getMessage(SleepercentageMessage.SKIP_STORM));

            world.setStorm(false);

        }



    }

    public void update() {

        ENFORCED_SLEEPERS.clear();
        CURRENT_SLEEPERS.clear();

        Bukkit.getWorlds().forEach(world -> {
            world.getPlayers().forEach(player -> {

                if (player.isSleeping()) {
                    Set<String> playersSleeping = CURRENT_SLEEPERS.computeIfAbsent(world.getName(), k -> new HashSet<>());
                }

                if (player.hasPermission("sleepercentage.exempt")) return;

                ENFORCED_SLEEPERS.compute(world.getName(), (k, v) -> v == null ? 0 : ++v);

                skip(world.getName());

            });
        });
    }

    public <T> T getSetting(String worldName, String setting) {

        if ( config.contains("settings.worlds." + worldName + "." + setting) ) {

            return (T) config.get("settings.worlds." + worldName + "." + setting);

        }

        return (T) config.get("settings." + setting);

    }

    public static Float percentageFromString(String s) {

        try { return PERCENT_FORMAT.parse(s).floatValue(); } catch (ParseException e) { e.printStackTrace(); }

        return (float) 0;
    }
}
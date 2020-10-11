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

        ModaPlaceholderAPI.addPlaceholder("CURRENT_SLEEPERS", player -> {
            return CURRENT_SLEEPERS.get(player.getWorld().getName()).size();
        });

        registerListener(this);

    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldSwitch(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        getLogger().debug("Player " + player.getName() + " switched to world " + worldName + ".");

        getLogger().debug("Trying to skip in " + worldName + ".");
        skip(worldName);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        getLogger().debug("Player " + player.getName() + " joined in world " + worldName + ".");

        getLogger().debug("Trying to skip in " + worldName + ".");
        skip(worldName);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {

        String playerName = event.getPlayer().getName();
        String worldName = event.getPlayer().getWorld().getName();
        String bedLocation = event.getBed().getLocation().toString();

        UUID uuid = event.getPlayer().getUniqueId();

        getLogger().debug(playerName + " Entered bed at " + bedLocation + " in world " + worldName + ".");

        BukkitTask task = this.getScheduler().delay((20 * (int) getSetting(worldName, "sleep-wait")), () -> {

            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isSleeping()) {
                getLogger().debug("Player " + playerName + " stopped sleeping or logged out before task was triggered.");
                return;
            }

            getLogger().debug("Adding player " + playerName + " to current sleepers.");
            Set<String> currentSleepers = CURRENT_SLEEPERS.compute(worldName, (k, v) -> {
                if (v == null) {
                    v = new HashSet<>();
                }
                v.add(playerName);
                return v;
            });

            getLogger().debug("Broadcasting " + playerName + "'s PBEE to " + worldName + ".");
            String message = ModaPlaceholderAPI.parsePlaceholders(
                    this.getLang().getMessage(
                            SleepercentageMessage.SLEEPING,
                                "CURRENT_SLEEPERS", String.valueOf(currentSleepers.size()),
                                "NEEDED_SLEEPERS", getNeededSleepers(worldName)), player);

            player.getWorld().getPlayers().forEach(p -> {
                p.sendMessage(message);
            });

            getLogger().debug("Removing finished task.");
            TASKS.remove(playerName);

            getLogger().debug("Trying to skip in " + worldName + ".");
            skip(worldName);

        });

        getLogger().debug("Added delayed task with ID " + task.getTaskId() + ".");
        TASKS.put(playerName, task.getTaskId());

    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerWake(PlayerBedLeaveEvent event) {

        String playerName = event.getPlayer().getName();
        String worldName = event.getPlayer().getWorld().getName();
        String bedLocation = event.getBed().getLocation().toString();

        getLogger().debug(playerName + " left bed at " + bedLocation + " in world " + worldName + ".");

        getLogger().debug(playerName + " left bed at " + bedLocation + " in world " + worldName + ".");
        TASKS.computeIfPresent(playerName, (k, v) -> {
            Bukkit.getScheduler().cancelTask(v);
            return null;
        });

        CURRENT_SLEEPERS.compute(worldName, (k, v) -> {
            if (v == null) {
                v = new HashSet<>();
            }
            v.remove(playerName);
            return v;
        });

    }

    public void skip(String worldName) {

        if (Bukkit.getWorld(worldName) == null) return;

        if (CURRENT_SLEEPERS.get(worldName).size() < getNeededSleepers(worldName)) return;

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

        CURRENT_SLEEPERS.clear();

        Bukkit.getWorlds().forEach(world -> {
            world.getPlayers().forEach(player -> {

                if (player.isSleeping()) {
                    CURRENT_SLEEPERS.compute(world.getName(), (k, v) -> {
                        if (v == null) {
                            v = new HashSet<>();
                        }
                        v.add(player.getName());
                        return v;
                    });
                }
            });
            skip(world.getName());
        });
    }

    @SuppressWarnings("unchecked")
    public <T> T getSetting(String worldName, String setting) {

        if ( config.contains("settings.worlds." + worldName + "." + setting) ) {

            return (T) config.get("settings.worlds." + worldName + "." + setting);

        }

        return (T) config.get("settings." + setting);

    }

    private int getNeededSleepers(String worldName) {

        int enforcedSleepers = (int) Bukkit.getWorld(worldName).getPlayers().stream()
                .filter(p -> !p.hasPermission("sleepercentage.exempt"))
                .count();

        return Math.round(enforcedSleepers * percentageFromString(getSetting(worldName, "percentage")));

    }

    public static Float percentageFromString(String s) {

        try { return PERCENT_FORMAT.parse(s).floatValue(); } catch (ParseException e) { e.printStackTrace(); }

        return (float) 0;
    }
}
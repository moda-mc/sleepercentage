package cx.mia.sleepercentage;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.stream.Collectors;

public final class Sleepercentage extends JavaPlugin implements Listener {

    public static NumberFormat percentFormat = NumberFormat.getPercentInstance();

    public ArrayList<SleeperWorld> sleeperWorlds = new ArrayList<>();

    public FileConfiguration config;

    @Override
    public void onEnable() {

        this.saveDefaultConfig();

        Bukkit.getPluginManager().registerEvents(this, this);

        this.config = getConfig();

        Bukkit.getWorlds().forEach(world -> {
            sleeperWorlds.add(new SleeperWorld(world, config));
        });

        sleeperWorlds.forEach(SleeperWorld::update);

    }

    @Override
    public void onDisable() {
        sleeperWorlds.clear();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) { update(event); }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) { update(event); }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) { update(event); }

    @EventHandler
    public void onPlayerWake(PlayerBedLeaveEvent event) { update(event); }

    public void update(PlayerEvent event) {
        sleeperWorlds.stream().filter(sleeperWorld -> {
            return sleeperWorld.world.equals(event.getPlayer().getWorld());
        }).collect(Collectors.toList()).forEach(sleeperWorld -> {
            System.out.println(sleeperWorld.world.getName());
            sleeperWorld.update();
        });
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

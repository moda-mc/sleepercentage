package cx.mia.sleepercentage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public class SleeperWorld {

    public final World world;
    private final FileConfiguration config;

    SleeperWorld(World world, FileConfiguration config) {
        this.world = world;
        this.config = config;
    }

    public ArrayList<UUID> sleepingPlayers = new ArrayList<java.util.UUID>();
    public ArrayList<UUID> enforcedPlayers = new ArrayList<UUID>();

    /**
     * updates
     */
    public void update() {

        int required = Math.round(enforcedPlayers.size() * Sleepercentage.percentageFromString(config.getString("percentage")));

        for (Player player : this.world.getPlayers()) {
            UUID uuid = player.getUniqueId();
            if (!player.hasPermission("sleepercentage.exempt")) {

                if (!enforcedPlayers.contains(uuid)) enforcedPlayers.add(uuid);

                required = Math.round(enforcedPlayers.size() * Sleepercentage.percentageFromString(config.getString("percentage")));

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

        if (sleepingPlayers.size() >= required && required > 0) {

            String skipped = ChatColor.translateAlternateColorCodes('&', config.getString("messages.skipped"));

            world.getPlayers().forEach(player -> {
                player.sendMessage(skipped);
            });

            this.world.setTime(0);
        }

    }
}

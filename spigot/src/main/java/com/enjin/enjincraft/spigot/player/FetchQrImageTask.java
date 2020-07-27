package com.enjin.enjincraft.spigot.player;

import com.enjin.enjincraft.spigot.SpigotBootstrap;
import lombok.NonNull;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import javax.imageio.ImageIO;
import java.net.URL;

public class FetchQrImageTask extends BukkitRunnable {

    public static final Long TASK_DELAY  = 1L;
    public static final Long TASK_PERIOD = 2L;

    private final SpigotBootstrap bootstrap;
    private final EnjPlayer       player;
    private final String          url;

    private FetchQrImageTask() {
        throw new IllegalStateException();
    }

    protected FetchQrImageTask(SpigotBootstrap bootstrap,
                               @NonNull EnjPlayer player,
                               @NonNull String url) throws NullPointerException {
        this.bootstrap = bootstrap;
        this.player = player;
        this.url = url;
    }

    @Override
    public void run() {
        if (isCancelled())
            return;

        if (player.getBukkitPlayer() == null || !player.getBukkitPlayer().isOnline()) {
            cancel();
        } else {
            try {
                player.setLinkingCodeQr(ImageIO.read(new URL(url)));
            } catch (Exception e) {
                bootstrap.log(e);
            } finally {
                cancel();
            }
        }
    }

    public static BukkitTask fetch(SpigotBootstrap bootstrap, EnjPlayer player, String url) {
        if (player == null || url == null)
            return null;

        FetchQrImageTask task = new FetchQrImageTask(bootstrap, player, url);
        // Note: TASK_PERIOD is measured in server ticks 20 ticks / second.
        return task.runTaskTimerAsynchronously(bootstrap.plugin(), TASK_DELAY, TASK_PERIOD);
    }

}

package us.blockbox.customjukebox.customjukebox;

import org.bukkit.scheduler.*;
import org.bukkit.entity.*;
import org.bukkit.*;
import org.bukkit.plugin.*;
import java.util.*;

class CustomDiscLooper extends BukkitRunnable
{
    private final CustomJukebox customJukebox;
    private String currentLoopSong;
    private final List<Map.Entry<String, Long>> songDurations;
    
    public CustomDiscLooper(final CustomJukebox customJukebox, final List<Map.Entry<String, Long>> songDurations) {
        this.currentLoopSong = null;
        this.customJukebox = customJukebox;
        this.songDurations = new ArrayList<>(songDurations);
    }
    
    public String getCurrentLoopSong() {
        return this.currentLoopSong;
    }
    
    public void run() {
        if (this.songDurations.isEmpty()) {
            this.cancel();
            return;
        }
        Collections.shuffle(this.songDurations);
        for (final Map.Entry<String, Long> song : this.songDurations) {
            if (!this.customJukebox.isLoopScheduler()) {
                break;
            }
            this.currentLoopSong = song.getKey();
            if (this.customJukebox.isDebug()) {
                this.customJukebox.getLogger().info("DEBUG: Starting song for all players: " + song.getKey());
            }
            for (final Player p : this.customJukebox.getServer().getOnlinePlayers()) {
                p.stopSound(Sound.MUSIC_DISC_11);
                p.stopSound(this.currentLoopSong);
                p.playSound(p.getLocation(), (String)song.getKey(), 60.0f, 1.0f);
            }
            try {
                Thread.sleep(song.getValue() * 1000L + 1500L);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (this.customJukebox.isLoopScheduler()) {
            if (this.customJukebox.isDebug()) {
                this.customJukebox.getLogger().info("DEBUG: All tracks in tracks folder have been played, looping through songs again.");
            }
            new CustomDiscLooper(this.customJukebox, this.songDurations).runTaskAsynchronously((Plugin)this.customJukebox);
        }
    }
}

package us.blockbox.customjukebox.customjukebox;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

class JukeboxListener implements Listener
{
    private final CustomJukebox customJukebox;
    private final CustomJukeboxAPI api;
    private final CustomDiscLooper discLooper;
    private static final float DISC_VOLUME_LOCAL = 3.0f;
    private static final float DISC_VOLUME_GLOBAL = 60.0f;
    
    public JukeboxListener(final CustomJukebox customJukebox, final CustomJukeboxAPI api, final CustomDiscLooper discLooper) {
        Validate.notNull(customJukebox);
        Validate.notNull(api);
        this.customJukebox = customJukebox;
        this.api = api;
        this.discLooper = discLooper;
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onJukeboxInteract(final PlayerInteractEvent e) {
        final Block clickedBlock = e.getClickedBlock();
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || clickedBlock == null || clickedBlock.getType() != Material.JUKEBOX) {
            return;
        }
        final BlockState state = clickedBlock.getState();
        if (!(state instanceof Jukebox)) {
            return;
        }
        final Jukebox j = (Jukebox)state;
        if (j.isPlaying()) {
            return;
        }
        final ItemStack hand = e.getItem();
        final Location jukeboxLoc = clickedBlock.getLocation();
        if (this.api.hasCustomDiscInserted(j)) {
            e.setCancelled(true);
            this.api.discEject(j);
        }
        else if (hand != null && hand.getType() == Material.MUSIC_DISC_CAT && this.api.isCustomDisc(hand)) {
            e.setCancelled(true);
            final String lore = hand.getItemMeta().getLore().get(0);
            final String trackName = this.customJukebox.getDiscNames().get(lore);
            for (final Player p : jukeboxLoc.getWorld().getPlayers()) {
                if (jukeboxLoc.distanceSquared(p.getLocation()) <= 4225.0) {
                    p.stopSound(trackName);
                    p.stopSound(Sound.MUSIC_DISC_CAT);
                }
            }
            final Player p2 = e.getPlayer();
            p2.getWorld().playSound(clickedBlock.getLocation(), trackName, 3.0f, 1.0f);
            p2.sendMessage(ChatColor.GREEN + "Now playing: " + lore);
            p2.getInventory().setItem(e.getPlayer().getInventory().getHeldItemSlot(), new ItemStack(Material.AIR));
            if (this.customJukebox.isDebug()) {
                this.customJukebox.getLogger().info("[DEBUG] Player " + p2.getName() + " inserted " + trackName + " disc into jukebox at " + clickedBlock.getLocation().toString());
            }
            j.setMetadata("CJB_DISC", new FixedMetadataValue(this.customJukebox, lore));
            this.customJukebox.getDiscLocations().put(jukeboxLoc, trackName);
        }
    }
    
    @EventHandler
    public void onJukeboxBreak(final BlockBreakEvent e) {
        if (!e.isCancelled() && e.getBlock().getType() == Material.JUKEBOX) {
            final Jukebox j = (Jukebox)e.getBlock().getState();
            if (this.api.hasCustomDiscInserted(j)) {
                this.api.discEject(j);
            }
        }
    }
    
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent e) {
        if (this.customJukebox.isLoopSongs() && this.discLooper != null) {
            final String currentLoopSong = this.discLooper.getCurrentLoopSong();
            if (currentLoopSong != null) {
                final Player p = e.getPlayer();
                p.stopSound(Sound.MUSIC_DISC_CAT);
                p.playSound(p.getLocation(), currentLoopSong, 60.0f, 1.0f);
            }
        }
    }
}

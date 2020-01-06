package us.blockbox.customjukebox.customjukebox;

import com.google.common.collect.BiMap;
import com.google.common.collect.Maps;
import org.bukkit.*;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.Map;

public class CustomJukeboxAPIImpl implements CustomJukeboxAPI
{
    private final CustomJukebox customJukebox;
    private final String metaKey;
    private final BiMap<String, String> discNames;
    private final Map<Location, String> discLocations;
    private final Map<String, Long> songDurations;
    private final Map<Location,ItemStack> discLoc2Stack;
    
    CustomJukeboxAPIImpl(final CustomJukebox customJukebox, final String metaKey, final BiMap<String, String> discNames, final Map<Location, String> discLocations, final Map<String, Long> songDurations, final Map<Location, ItemStack> discLoc2Stack) {
        this.customJukebox = customJukebox;
        this.metaKey = metaKey;
        this.discNames = discNames;
        this.discLocations = discLocations;
        this.songDurations = songDurations;
        this.discLoc2Stack = discLoc2Stack;
    }
    
    @Override
    public BiMap<String, String> getDiscNames() {
        return (BiMap<String, String>)Maps.unmodifiableBiMap((BiMap)this.discNames);
    }
    
    @Override
    public boolean hasCustomDiscInserted(final Jukebox j) {
        return j.hasMetadata(this.metaKey);
    }
    
    @Override
    public String getCustomDiscInserted(final Jukebox j) {
        if (this.hasCustomDiscInserted(j)) {
            return j.getMetadata(this.metaKey).get(0).asString();
        }
        return null;
    }
    
    @Override
    public boolean isCustomDisc(final ItemStack item) {
        return item.hasItemMeta() && this.discNames.containsKey(item.getItemMeta().getLore().get(0));
    }
    
    @Override
    public boolean discEject(final Jukebox j) {
        final String customDiscInserted = this.getCustomDiscInserted(j);
        if (customDiscInserted == null) {
            return false;
        }
        //final ItemStack recStack = this.discCreate(customDiscInserted);
        ItemStack recStack = discLoc2Stack.get(j.getLocation());
        discLoc2Stack.remove(j.getLocation());
        final Location loc = j.getLocation();
        if (this.customJukebox.isDebug()) {
            this.customJukebox.getLogger().info("DEBUG: Ejecting disc " + customDiscInserted + " from jukebox at " + loc.toString());
        }
        j.removeMetadata(this.metaKey, this.customJukebox);
        if(recStack.getType() != Material.AIR){
            loc.getWorld().dropItem(loc.clone().add(0.5, 1.0, 0.5), recStack);
        }
        this.discLocations.remove(loc);
        for (final Player p : loc.getWorld().getPlayers()) {
            if (loc.distanceSquared(p.getLocation()) <= 4225.0) {
                p.stopSound(Sound.MUSIC_DISC_11);
                p.stopSound(this.getDiscNames().get(customDiscInserted),SoundCategory.RECORDS);
            }
        }
        return true;
    }
    
    @Override
    public ItemStack discCreate(final String lore) {
        final ItemStack recStack = new ItemStack(Material.MUSIC_DISC_CAT, 1);
        final ItemMeta recMeta = recStack.getItemMeta();
        recMeta.setLore(Collections.singletonList(lore));
        recStack.setItemMeta(recMeta);
        return recStack;
    }

    @Override
    public void discInsert(Location p0, ItemStack p1) {
        discLoc2Stack.put(p0, p1);
    }

    @Override
    public long getDuration(final String song) {
        final Long duration = this.songDurations.get(song);
        if (duration == null) {
            return -1L;
        }
        return duration;
    }
    
    @Override
    public void playDisc(final Location l, final String name, final SoundCategory category, final float volume, final float pitch) {
        l.getWorld().playSound(l, name, category, volume, pitch);
    }
}

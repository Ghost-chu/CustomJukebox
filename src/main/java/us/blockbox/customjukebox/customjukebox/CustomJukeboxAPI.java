package us.blockbox.customjukebox.customjukebox;

import com.google.common.collect.*;
import org.bukkit.block.*;
import org.bukkit.inventory.*;
import org.bukkit.entity.*;
import org.bukkit.*;

public interface CustomJukeboxAPI
{
    BiMap<String, String> getDiscNames();
    
    boolean hasCustomDiscInserted(final Jukebox p0);
    
    String getCustomDiscInserted(final Jukebox p0);
    
    boolean isCustomDisc(final ItemStack p0);
    
    ItemStack discCreate(final String p0);
    
    void playDisc(final Player p0, final Location p1, final String p2, final float p3, final float p4);
    
    void playDisc(final Player p0, final Location p1, final String p2, final SoundCategory p3, final float p4, final float p5);
    
    void playDisc(final Location p0, final String p1, final float p2, final float p3);
    
    void playDisc(final Location p0, final String p1, final SoundCategory p2, final float p3, final float p4);
    
    boolean discEject(final Jukebox p0);
    
    long getDuration(final String p0);
}

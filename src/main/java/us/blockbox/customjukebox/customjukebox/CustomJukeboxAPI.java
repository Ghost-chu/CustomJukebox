package us.blockbox.customjukebox.customjukebox;

import com.google.common.collect.BiMap;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.block.Jukebox;
import org.bukkit.inventory.ItemStack;

public interface CustomJukeboxAPI
{
    BiMap<String, String> getDiscNames();
    
    boolean hasCustomDiscInserted(final Jukebox p0);
    
    String getCustomDiscInserted(final Jukebox p0);
    
    boolean isCustomDisc(final ItemStack p0);
    
    ItemStack discCreate(final String p0);
    void discInsert(final Location p0, final ItemStack p1);

    void playDisc(final Location p0, final String p1, final SoundCategory p2, final float p3, final float p4);
    
    boolean discEject(final Jukebox p0);
    
    long getDuration(final String p0);
}

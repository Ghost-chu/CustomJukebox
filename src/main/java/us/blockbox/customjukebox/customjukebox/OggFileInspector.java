package us.blockbox.customjukebox.customjukebox;

import java.io.*;
import java.util.logging.*;
import java.util.*;

class OggFileInspector
{
    private final CustomJukebox customJukebox;
    private static final FilenameFilter filter;
    
    public OggFileInspector(final CustomJukebox customJukebox) {
        this.customJukebox = customJukebox;
    }
    
    public Map<String, Long> getTrackDurations() {
        final File trackDir = new File(this.customJukebox.getDataFolder(), "tracks");
        final Logger log = this.customJukebox.getLogger();
        if (!trackDir.isDirectory()) {
            if (trackDir.mkdir()) {
                log.info("Successfully created track directory. Place songs in it and restart CustomJukebox.");
            }
            else {
                log.severe("Failed to create track directory.");
            }
            return Collections.emptyMap();
        }
        final File[] files = trackDir.listFiles(OggFileInspector.filter);
        if (files == null) {
            throw new IllegalStateException("Failed to list files in tracks directory!");
        }
        log.info("Attempting to load song lengths from " + files.length + " files in " + trackDir.getName() + " directory");
        final Set<String> soundNames = this.customJukebox.getSoundNames();
        return createMap(log, files, soundNames);
    }
    
    private static Map<String, Long> createMap(final Logger log, final File[] files, final Set<String> soundNames) {
        final Map<String, Long> songDurations = new HashMap<>(Math.min(files.length, soundNames.size()));
        for (final File f : files) {
            final String fileName = f.getName();
            final String songName = fileName.substring(0, fileName.lastIndexOf(46));
            Label_0190: {
                if (soundNames.contains(songName)) {
                    Ogg ogg;
                    try {
                        ogg = new Ogg(f);
                    }
                    catch (Exception e) {
                        log.warning("Failed to load song at \"" + fileName + "\", it will not play properly.");
                        e.printStackTrace();
                        break Label_0190;
                    }
                    final long seconds = ogg.getSeconds();
                    log.info("Loaded \"" + songName + "\" (" + seconds + " sec)");
                    songDurations.put(songName, seconds);
                }
            }
        }
        return songDurations;
    }
    
    static {
        filter = (file, s) -> s.endsWith(".ogg");
    }
}

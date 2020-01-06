package us.blockbox.customjukebox.customjukebox;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.primitives.Ints;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class CustomJukebox extends JavaPlugin implements Listener
{
    public static final String DISC_META = "CJB_DISC";
    private final BiMap<String, String> discNames;
    private final Set<String> soundNames;
    private final Map<Location, String> discLocations;
    private final Map<Location, ItemStack> disc2Stack;
    private File jukeboxLocFile;
    private FileConfiguration jukeboxLocConfig;
    private File jukeboxDiscsFile;
    private FileConfiguration jukeboxDiscConfig;
    private static CustomJukebox plugin;
    private boolean loopSongs;
    private boolean loopScheduler;
    private boolean debug;
    private Logger log;
    private boolean enabledCleanly;
    private CustomJukeboxAPI api;
    
    public CustomJukebox() {
        this.discNames = HashBiMap.create();
        this.soundNames = new HashSet<>();
        this.discLocations = new HashMap<>();
        this.disc2Stack = new HashMap<>();
        this.loopSongs = false;
        this.loopScheduler = false;
        this.debug = false;
    }
    
    public static CustomJukebox getPlugin() {
        return CustomJukebox.plugin;
    }
    
    public CustomJukeboxAPI getAPI() {
        return this.api;
    }
    
    public void onEnable() {
        final long start = System.currentTimeMillis();
        try {
            this.init();
        }
        catch (Exception e) {
            e.printStackTrace();
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (!this.enabledCleanly) {
            this.getServer().getConsoleSender().sendMessage(ChatColor.RED + "Plugin did not enable cleanly, disabling.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        final long end = System.currentTimeMillis();
        this.getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "[CustomJukebox] Enabled successfully in " + (end - start) + " ms!");
    }
    
    private void init() {
        final Server server = this.getServer();
        if (this.isBukkitTooOld(server)) {
            return;
        }
        CustomJukebox.plugin = this;
        this.log = this.getLogger();
        this.jukeboxLocFile = new File(this.getDataFolder(), "locations.yml");
        this.jukeboxLocConfig = YamlConfiguration.loadConfiguration(this.jukeboxLocFile);
        this.saveDefaultConfig(this.jukeboxDiscsFile = new File(this.getDataFolder(), "discs.yml"));
        this.jukeboxDiscConfig = YamlConfiguration.loadConfiguration(this.jukeboxDiscsFile);
        this.loadConfig();
        CustomDiscLooper discLooper = null;
        final Map<String, Long> songDurations = this.getSongDurations();
        if (this.loopSongs) {
            this.loopScheduler = true;
            discLooper = new CustomDiscLooper(this, new ArrayList<>(songDurations.entrySet()));
            discLooper.runTaskLaterAsynchronously(this, 40L);
        }
        this.api = new CustomJukeboxAPIImpl(this, "CJB_DISC", this.discNames, this.discLocations, songDurations, this.disc2Stack);
        server.getPluginManager().registerEvents(new JukeboxListener(this, this.api, discLooper), this);
        this.getCommand("disc").setTabCompleter(new DiscTabCompleter(this));
        this.enabledCleanly = true;
    }
    
    private boolean isBukkitTooOld(final Server server) {
        final String[] split = server.getBukkitVersion().split("-");
        final String[] version = split[0].split("\\.");
        Integer major;
        Integer minor;
        if (version.length >= 2) {
            major = Ints.tryParse(version[0]);
            minor = Ints.tryParse(version[1]);
        }
        else {
            major = null;
            minor = null;
        }
        final int majorMin = 1;
        final int minorMin = 10;
        if (major == null || minor == null || (major <= 1 && (major != 1 || minor < 10))) {
            server.getConsoleSender().sendMessage(ChatColor.RED + "You must be running " + 1 + "." + 10 + "or higher to use CustomJukebox.");
            server.getPluginManager().disablePlugin(this);
            return true;
        }
        return false;
    }
    
    private Map<String, Long> getSongDurations() {
        final OggFileInspector oggFileInspector = new OggFileInspector(this);
        return oggFileInspector.getTrackDurations();
    }
    
    public void onDisable() {
        if (!this.enabledCleanly) {
            return;
        }
        this.loopScheduler = false;
        this.getServer().getScheduler().cancelTasks(this);
        for (final World w : this.getServer().getWorlds()) {
            final List<String> worldJukeboxes = new ArrayList<>();
            for (final Map.Entry<Location, String> e : this.discLocations.entrySet()) {
                final Location l = e.getKey();
                if (l.getWorld() != w) {
                    continue;
                }
                if (l.getBlock().getType() == Material.JUKEBOX) {
                    worldJukeboxes.add(l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ() + "," + e.getValue());
                }
                else {
                    this.log.warning("Block at location is not a jukebox, not saving info for it.");
                }
            }
            this.jukeboxLocConfig.set(w.getName(), worldJukeboxes);
        }
        try {
            this.jukeboxLocConfig.save(this.jukeboxLocFile);
        }
        catch (IOException e2) {
            e2.printStackTrace();
        }
        if (this.getConfig().getBoolean("debug") != this.debug) {
            this.getConfig().set("debug", this.debug);
            this.saveConfig();
        }
    }
    
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        if (cmd.getName().equalsIgnoreCase("disc")) {
            if (!(sender instanceof Player) || !sender.hasPermission("customjukebox.command.disc")) {
                sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to use this command.");
                return true;
            }
            if (args.length <= 0) {
                sender.sendMessage(ChatColor.DARK_AQUA + "Available discs:");
                for (final String soundName : this.soundNames) {
                    sender.sendMessage("  - " + soundName);
                }
                return true;
            }
            String discLore = null;
            String discName = null;
            for (final Map.Entry<String, String> disc : this.discNames.entrySet()) {
                if (disc.getValue().equals(args[0]) || disc.getValue().toLowerCase().startsWith(args[0].toLowerCase())) {
                    discLore = disc.getKey();
                    discName = disc.getValue();
                    break;
                }
            }
            if (discLore == null) {
                sender.sendMessage(ChatColor.DARK_RED + "There is no disc by that name.");
            }
            else {
                if (((Player)sender).getInventory().firstEmpty() == -1) {
                    sender.sendMessage(ChatColor.DARK_RED + "Not enough room in your inventory.");
                    return true;
                }
                ((Player)sender).getInventory().addItem(this.getAPI().discCreate(discLore));
                sender.sendMessage(ChatColor.DARK_AQUA + "Got disc: " + ChatColor.RESET + discName);
            }
        }
        else if (cmd.getName().equalsIgnoreCase("cjbdebug")) {
            if (!sender.hasPermission("customjukebox.command.debug")) {
                sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to use this command.");
                return true;
            }
            this.debug = !this.debug;
            if (this.debug) {
                sender.sendMessage("[CustomJukebox] Debugging enabled.");
            }
            else {
                sender.sendMessage("[CustomJukebox] Debugging disabled.");
            }
        }
        return true;
    }
    
    private void loadConfig() {
        this.saveDefaultConfig();
        this.getConfig();
        this.discNames.clear();
        this.soundNames.clear();
        this.discLocations.clear();
        this.debug = this.getConfig().getBoolean("debug", false);
        this.loopSongs = this.getConfig().getBoolean("loopsongs", false);
        this.loadDiscNamesAndLore(this.jukeboxDiscConfig.getValues(false).entrySet());
        this.getServer().getConsoleSender().sendMessage(ChatColor.GOLD + "[CustomJukebox] Songs loaded:");
        for (final Map.Entry<String, String> test : this.discNames.entrySet()) {
            this.log.info(test.getKey());
        }
        this.soundNames.addAll(this.discNames.values());
        final List<String> worldsLoaded = new ArrayList<>();
        for (final String w : this.jukeboxLocConfig.getKeys(false)) {
            if (this.getServer().getWorld(w) != null) {
                worldsLoaded.add(w);
            }
            else {
                this.log.warning("World " + w + " is not loaded, ignoring its jukebox locations.");
            }
        }
        for (final String worldSection : worldsLoaded) {
            for (final String s : this.jukeboxLocConfig.getStringList(worldSection)) {
                int i = 0;
                ++i;
                final String[] c = s.split(",");
                if (c.length == 4) {
                    if (this.debug) {
                        this.log.info("LOADING LOCATION: " + s);
                    }
                    this.discLocations.put(new Location(this.getServer().getWorld(worldSection), Double.parseDouble(c[0]), Double.parseDouble(c[1]), Double.parseDouble(c[2])), c[3]);
                }
                else {
                    this.log.warning("Invalid entry at position " + i + " in locations.yml list.");
                }
            }
        }
    }
    
    private void loadDiscNamesAndLore(final Set<Map.Entry<String, Object>> entries) {
        for (final Map.Entry<String, Object> song : entries) {
            final Object value = song.getValue();
            if (value instanceof String) {
                final String key = song.getKey();
                final String keyDotted = key.replace(',', '.');
                this.discNames.put((String)value, keyDotted);
            }
        }
    }
    
    private void saveDefaultConfig(final File f) {
        if (f == null) {
            return;
        }
        if (!f.exists()) {
            CustomJukebox.plugin.saveResource(f.getName(), false);
        }
    }
    
    public Map<String, String> getDiscNames() {
        return this.discNames;
    }
    
    public Set<String> getSoundNames() {
        return this.soundNames;
    }
    
    public Map<Location, String> getDiscLocations() {
        return this.discLocations;
    }
    
    public File getJukeboxLocFile() {
        return this.jukeboxLocFile;
    }
    
    public FileConfiguration getJukeboxLocConfig() {
        return this.jukeboxLocConfig;
    }
    
    public File getJukeboxDiscsFile() {
        return this.jukeboxDiscsFile;
    }
    
    public FileConfiguration getJukeboxDiscConfig() {
        return this.jukeboxDiscConfig;
    }
    
    public boolean isLoopSongs() {
        return this.loopSongs;
    }
    
    public boolean isLoopScheduler() {
        return this.loopScheduler;
    }
    
    public boolean isDebug() {
        return this.debug;
    }
}

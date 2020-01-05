package us.blockbox.customjukebox.customjukebox;

import org.bukkit.command.*;
import java.util.*;

class DiscTabCompleter implements TabCompleter
{
    private final CustomJukebox customJukebox;
    
    public DiscTabCompleter(final CustomJukebox customJukebox) {
        this.customJukebox = customJukebox;
    }
    
    public List<String> onTabComplete(final CommandSender commandSender, final Command command, final String alias, final String[] args) {
        if (args.length > 0) {
            final Set<String> soundNames = this.customJukebox.getSoundNames();
            final List<String> partialMatch = new ArrayList<>(Math.min(soundNames.size() / 2, 16));
            for (final String disc : soundNames) {
                if (disc.toLowerCase().startsWith(args[0].toLowerCase())) {
                    partialMatch.add(disc);
                }
            }
            if (partialMatch.size() > 0) {
                return partialMatch;
            }
        }
        return null;
    }
}

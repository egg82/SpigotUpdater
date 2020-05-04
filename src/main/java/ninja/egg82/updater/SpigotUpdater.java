package ninja.egg82.updater;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import ninja.egg82.json.JSONWebUtil;
import org.bukkit.plugin.Plugin;

public class SpigotUpdater extends Updater {
    private final Plugin plugin;
    private final int resourceId;

    public SpigotUpdater(Plugin plugin, int resourceId) { this(plugin, resourceId, 1L, TimeUnit.HOURS); }
    public SpigotUpdater(Plugin plugin, int resourceId, long updateCheckDelayTime, TimeUnit timeUnit) {
        super(plugin.getDescription().getVersion(), updateCheckDelayTime, timeUnit);

        if (resourceId <= 0) {
            throw new IllegalArgumentException("resourceId cannot be <= 0.");
        }

        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public String getDownloadLink() { return "https://api.spiget.org/v2/resources/" + resourceId + "/versions/latest/download"; }

    protected void checkUpdate() throws IOException {
        latestVersion = JSONWebUtil.getString(new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId), "GET", 5000, "egg82/SpigotUpdater");

        int[] latest = parseVersion(latestVersion, '.');
        int[] current = parseVersion(plugin.getDescription().getVersion(), '.');

        for (int i = 0; i < Math.min(latest.length, current.length); i++) {
            if (latest[i] > current[i]) {
                updateAvailable = true;
                return;
            }
        }

        updateAvailable = false;
    }
}

package ninja.egg82.updater;

import com.google.common.primitives.Ints;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.md_5.bungee.api.plugin.Plugin;
import ninja.egg82.json.JSONWebUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BungeeUpdater {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private volatile boolean updateAvailable = false;
    private volatile String latestVersion;
    private AtomicLong lastUpdateTime = new AtomicLong(0L);

    private final Plugin plugin;
    private final int resourceId;
    private final long updateCheckDelayTime;

    public BungeeUpdater(Plugin plugin, int resourceId) { this(plugin, resourceId, 1L, TimeUnit.HOURS); }
    public BungeeUpdater(Plugin plugin, int resourceId, long updateCheckDelayTime, TimeUnit timeUnit) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin cannot be null");
        }
        if (resourceId <= 0) {
            throw new IllegalArgumentException("resourceId cannot be <= 0");
        }

        this.plugin = plugin;
        this.resourceId = resourceId;
        this.updateCheckDelayTime = timeUnit.toMillis(updateCheckDelayTime);
        this.latestVersion = plugin.getDescription().getVersion();
    }

    public CompletableFuture<Boolean> isUpdateAvailable() {
        return CompletableFuture.supplyAsync(() -> {
            long current = System.currentTimeMillis();
            if (lastUpdateTime.updateAndGet(v -> current - v >= updateCheckDelayTime ? current : v) == current) {
                try {
                    checkUpdate();
                } catch (IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
            return updateAvailable;
        });
    }

    public CompletableFuture<String> getLatestVersion() {
        return CompletableFuture.supplyAsync(() -> {
            long current = System.currentTimeMillis();
            if (lastUpdateTime.updateAndGet(v -> current - v >= updateCheckDelayTime ? current : v) == current) {
                try {
                    checkUpdate();
                } catch (IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
            return latestVersion;
        });
    }

    public String getDownloadLink() { return "https://api.spiget.org/v2/resources/" + resourceId + "/versions/latest/download"; }

    private void checkUpdate() throws IOException {
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

    private int[] parseVersion(String version, char separator) {
        if (version == null) {
            throw new IllegalArgumentException("version cannot be null.");
        }

        List<Integer> ints = new ArrayList<>();

        int lastIndex = 0;
        int currentIndex = version.indexOf(separator);

        while (currentIndex > -1) {
            int current = tryParseInt(version.substring(lastIndex, currentIndex));
            if (current > -1) {
                ints.add(current);
            }

            lastIndex = currentIndex + 1;
            currentIndex = version.indexOf(separator, currentIndex + 1);
        }
        int current = tryParseInt(version.substring(lastIndex));
        if (current > -1) {
            ints.add(current);
        }

        return Ints.toArray(ints);
    }

    private int tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return -1;
        }
    }
}

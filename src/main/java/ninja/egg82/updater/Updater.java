package ninja.egg82.updater;

import com.google.common.primitives.Ints;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Updater {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected volatile String latestVersion;
    protected volatile boolean updateAvailable = false;

    private final long updateCheckDelayTime;
    private AtomicLong lastUpdateTime = new AtomicLong(0L);

    public Updater(String latestVersion, long updateCheckDelayTime, TimeUnit timeUnit) {
        if (latestVersion == null) {
            throw new IllegalArgumentException("latestVersion cannot be null.");
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("timeUnit cannot be null.");
        }

        this.latestVersion = latestVersion;
        this.updateCheckDelayTime = timeUnit.toMillis(updateCheckDelayTime);
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

    public abstract String getDownloadLink();

    protected abstract void checkUpdate() throws IOException;

    protected final int[] parseVersion(String version, char separator) {
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

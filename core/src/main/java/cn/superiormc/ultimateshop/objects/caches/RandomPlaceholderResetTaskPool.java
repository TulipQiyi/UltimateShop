package cn.superiormc.ultimateshop.objects.caches;

import java.time.LocalDateTime;

public final class RandomPlaceholderResetTaskPool {

    private static final int NEVER_REFRESH_YEAR = 2999;

    private RandomPlaceholderResetTaskPool() {}

    public static void register(
            ObjectRandomPlaceholderCache cache,
            LocalDateTime refreshTime
    ) {
        if (cache == null || refreshTime == null
                || refreshTime.getYear() == NEVER_REFRESH_YEAR) {
            return;
        }
        DeadlineTaskPool.schedule(
                cache,
                DeadlineTaskPool.TaskType.RANDOM_PLACEHOLDER,
                refreshTime,
                cache::setRefreshTime
        );
    }

    public static void unregister(ObjectRandomPlaceholderCache cache) {
        DeadlineTaskPool.cancel(
                cache,
                DeadlineTaskPool.TaskType.RANDOM_PLACEHOLDER
        );
    }

    public static void shutdown() {
        DeadlineTaskPool.cancelAll(
                DeadlineTaskPool.TaskType.RANDOM_PLACEHOLDER
        );
    }
}

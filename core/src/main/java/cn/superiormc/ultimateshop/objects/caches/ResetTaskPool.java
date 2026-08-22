package cn.superiormc.ultimateshop.objects.caches;

import java.time.LocalDateTime;

public final class ResetTaskPool {

    private static final int NEVER_REFRESH_YEAR = 2999;

    private ResetTaskPool() {}

    public static void registerBuy(
            ObjectUseTimesCache cache,
            LocalDateTime refreshTime
    ) {
        register(
                cache,
                refreshTime,
                DeadlineTaskPool.TaskType.USE_TIMES_BUY,
                cache::refreshBuyTimes
        );
    }

    public static void registerSell(
            ObjectUseTimesCache cache,
            LocalDateTime refreshTime
    ) {
        register(
                cache,
                refreshTime,
                DeadlineTaskPool.TaskType.USE_TIMES_SELL,
                cache::refreshSellTimes
        );
    }

    private static void register(
            ObjectUseTimesCache cache,
            LocalDateTime refreshTime,
            DeadlineTaskPool.TaskType type,
            Runnable action
    ) {
        if (cache == null || refreshTime == null
                || refreshTime.getYear() == NEVER_REFRESH_YEAR) {
            return;
        }
        DeadlineTaskPool.schedule(cache, type, refreshTime, action);
    }

    public static void unregisterBuy(ObjectUseTimesCache cache) {
        DeadlineTaskPool.cancel(cache, DeadlineTaskPool.TaskType.USE_TIMES_BUY);
    }

    public static void unregisterSell(ObjectUseTimesCache cache) {
        DeadlineTaskPool.cancel(cache, DeadlineTaskPool.TaskType.USE_TIMES_SELL);
    }

    public static void shutdown() {
        DeadlineTaskPool.cancelAll(
                DeadlineTaskPool.TaskType.USE_TIMES_BUY,
                DeadlineTaskPool.TaskType.USE_TIMES_SELL
        );
    }
}

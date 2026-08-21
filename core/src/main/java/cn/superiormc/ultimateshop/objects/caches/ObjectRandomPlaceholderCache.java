package cn.superiormc.ultimateshop.objects.caches;

import cn.superiormc.ultimateshop.managers.BungeeCordManager;
import cn.superiormc.ultimateshop.database.PlayerDataSnapshot.RandomPlaceholderSnapshot;
import cn.superiormc.ultimateshop.managers.ConfigManager;
import cn.superiormc.ultimateshop.managers.ErrorManager;
import cn.superiormc.ultimateshop.objects.items.subobjects.ObjectRandomPlaceholder;
import cn.superiormc.ultimateshop.utils.CommandUtil;
import cn.superiormc.ultimateshop.utils.CommonUtil;
import cn.superiormc.ultimateshop.utils.TextUtil;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ObjectRandomPlaceholderCache {

    private static final int NEVER_REFRESH_YEAR = 2999;

    private List<String> nowValue = null;

    private LocalDateTime refreshDoneTime = null;

    private LocalDateTime lastResetTime = null;

    private boolean initialized;

    private final ObjectRandomPlaceholder placeholder;

    private final ObjectCache cache;

    public ObjectRandomPlaceholderCache(ObjectCache cache,
                                        ObjectRandomPlaceholder placeholder) {
        this.cache = cache;
        this.placeholder = placeholder;
    }

    public synchronized void initialize() {
        if (initialized || cache.canNotModify()) {
            return;
        }
        initialized = true;
        setRefreshTime();
    }

    public synchronized void loadState(List<String> value,
                                       LocalDateTime refreshTime,
                                       LocalDateTime loadedLastResetTime) {
        if (value == null) {
            return;
        }
        cancelResetTask();
        this.nowValue = new ArrayList<>(value);
        this.refreshDoneTime = refreshTime;
        this.lastResetTime = loadedLastResetTime;
        migrateLegacyCustomState();
        this.initialized = true;
        activateResetTask();
    }

    public synchronized void loadState(List<String> value, LocalDateTime refreshTime) {
        loadState(value, refreshTime, null);
    }

    public ObjectRandomPlaceholder getPlaceholder() {
        return placeholder;
    }

    public synchronized LocalDateTime getRefreshDoneTime() {
        if ("CUSTOM".equals(placeholder.getMode())
                || refreshDoneTime != null && !refreshDoneTime.isAfter(CommonUtil.getNowTime())) {
            setRefreshTime();
        }
        return refreshDoneTime;
    }

    public synchronized LocalDateTime getLastResetTime() {
        return lastResetTime;
    }

    public synchronized void removeRefreshDoneTime() {
        refreshDoneTime = null;
        lastResetTime = null;
    }

    public synchronized void cancelResetTask() {
        RandomPlaceholderResetTaskPool.unregister(this);
    }

    public synchronized void activateResetTask() {
        if (!initialized || cache.canNotModify()) {
            return;
        }
        if ("CUSTOM".equals(placeholder.getMode())) {
            setRefreshTime();
            scheduleResetTask();
        } else {
            scheduleResetTask();
        }
    }

    public List<String> getNowValue() {
        return getNowValue(true, false);
    }

    public List<String> getNowValue(boolean disable) {
        return getNowValue(true, disable);
    }

    public synchronized List<String> getNowValue(boolean needRefresh, boolean disable) {
        if (needRefresh) {
            setRefreshTime(disable);
        }
        return nowValue == null ? null : new ArrayList<>(nowValue);
    }

    public synchronized void setRefreshTime() {
        setRefreshTime(false);
    }

    public synchronized void setRefreshTime(boolean notUseBungee) {
        if (cache.canNotModify()) {
            cancelResetTask();
            return;
        }
        String mode = placeholder.getMode();
        String time = TextUtil.withPAPI(placeholder.getConfig().getString("reset-time"), null);
        if (mode == null || time.isEmpty()) {
            if (nowValue == null) {
                setPlaceholder(notUseBungee);
            }
            return;
        }
        if (mode.equals("ONCE")) {
            lastResetTime = CommonUtil.getNowTime();
            setPlaceholder(notUseBungee);
            return;
        }
        LocalDateTime now = CommonUtil.getNowTime();
        LocalDateTime customRefreshTime = null;
        if (mode.equals("CUSTOM")) {
            customRefreshTime = getCustomRefreshTime(time);
            if (customRefreshTime == null) {
                refreshDoneTime = neverRefresh();
                cancelResetTask();
                if (nowValue == null) {
                    setPlaceholder(notUseBungee);
                }
                return;
            }
            if (lastResetTime != null && !lastResetTime.isBefore(customRefreshTime)) {
                refreshDoneTime = neverRefresh();
                cancelResetTask();
                if (nowValue == null) {
                    setPlaceholder(notUseBungee);
                }
                return;
            }
            if (customRefreshTime.isAfter(now)
                    && nowValue != null
                    && refreshDoneTime != null
                    && (isNeverRefresh(refreshDoneTime) || refreshDoneTime.isAfter(now))
                    && !customRefreshTime.equals(refreshDoneTime)) {
                refreshDoneTime = customRefreshTime;
                cancelResetTask();
                scheduleResetTask();
                return;
            }
        }
        boolean hadValue = nowValue != null;
        boolean needRefresh = nowValue == null || refreshDoneTime == null
                || !refreshDoneTime.isAfter(now)
                || mode.equals("CUSTOM") && !customRefreshTime.isAfter(now);
        for (ObjectRandomPlaceholder tempVal1 : placeholder.getNotSameAs()) {
            if (tempVal1.equals(getPlaceholder())) {
                continue;
            }
            if (tempVal1.getNowValue(cache).equals(nowValue)) {
                needRefresh = true;
            }
        }
        if (needRefresh) {
            switch (mode) {
                case "TIMED":
                    refreshDoneTime = getTimedRefreshTime(time);
                    break;
                case "TIMER":
                    refreshDoneTime = getTimerRefreshTime(time);
                    break;
                case "CUSTOM":
                    refreshDoneTime = customRefreshTime;
                    if (!customRefreshTime.isAfter(now)) {
                        refreshDoneTime = neverRefresh();
                    }
                    break;
                case "RANDOM_PLACEHOLDER":
                    if (time.equals(placeholder.getID())) {
                        refreshDoneTime = neverRefresh();
                    } else {
                        refreshDoneTime = ObjectRandomPlaceholder.getRefreshDoneTimeObject(cache.getPlayer(), time);
                    }
                    break;
                default:
                    refreshDoneTime = neverRefresh();
                    break;
            }

            cancelResetTask();
            scheduleResetTask();
            if (hadValue || mode.equals("CUSTOM") && !customRefreshTime.isAfter(now)) {
                lastResetTime = now;
            }
            setPlaceholder(notUseBungee);
            CommandUtil.updateGUI(cache.getPlayer());
        }
    }

    private void scheduleResetTask() {
        String mode = placeholder.getMode();
        if (!isSchedulableMode(mode)
                || !ConfigManager.configManager.getBoolean("use-times.auto-reset-mode")
                || refreshDoneTime == null
                || isNeverRefresh(refreshDoneTime)) {
            return;
        }

        RandomPlaceholderResetTaskPool.register(this, refreshDoneTime);
    }

    private boolean isSchedulableMode(String mode) {
        return "TIMED".equals(mode)
                || "TIMER".equals(mode)
                || "CUSTOM".equals(mode)
                || "RANDOM_PLACEHOLDER".equals(mode);
    }

    private LocalDateTime getCustomRefreshTime(String time) {
        try {
            return CommonUtil.stringToTime(
                    time,
                    placeholder.getConfig().getString(
                            "time-format",
                            "yyyy-MM-dd HH:mm:ss"
                    )
            );
        } catch (DateTimeException exception) {
            ErrorManager.errorManager.sendErrorMessage(
                    "§cError: Your reset time " + time + " is invalid."
            );
            return null;
        }
    }

    private void migrateLegacyCustomState() {
        if (!"CUSTOM".equals(placeholder.getMode())
                || lastResetTime != null
                || !isNeverRefresh(refreshDoneTime)) {
            return;
        }
        lastResetTime = CommonUtil.getNowTime();
    }

    private LocalDateTime neverRefresh() {
        return CommonUtil.getNowTime().withYear(NEVER_REFRESH_YEAR);
    }

    private boolean isNeverRefresh(LocalDateTime time) {
        return time != null && time.getYear() == NEVER_REFRESH_YEAR;
    }

    public void setPlaceholder(boolean notUseBungee) {
        setPlaceholder(placeholder.getNewValue(cache), notUseBungee);
    }

    public synchronized void setPlaceholder(List<String> element, boolean notUseBungee) {
        if (element == null || cache.canNotModify()) {
            return;
        }
        nowValue = new ArrayList<>(element);
        if (!notUseBungee && BungeeCordManager.bungeeCordManager != null) {
            BungeeCordManager.bungeeCordManager.sendRandomPlaceholderToOtherServer(
                    placeholder.getID(),
                    CommonUtil.translateStringList(nowValue),
                    CommonUtil.timeToString(refreshDoneTime),
                    CommonUtil.timeToString(lastResetTime));
        }
    }

    public synchronized RandomPlaceholderSnapshot snapshot() {
        if (nowValue == null || "ONCE".equals(placeholder.getMode())) {
            return null;
        }
        return new RandomPlaceholderSnapshot(
                placeholder.getID(),
                CommonUtil.translateStringList(nowValue),
                CommonUtil.timeToString(refreshDoneTime),
                CommonUtil.timeToString(lastResetTime)
        );
    }

    private LocalDateTime getTimedRefreshTime(String time) {
        LocalDate nowTime = LocalDate.now();
        LocalDateTime refreshResult = null;
        String[] tempVal3 = time.split(";;");
        for (String tempVal4 : tempVal3) {
            LocalDateTime thisResult;
            String[] tempVal2 = tempVal4.split(":");
            if (tempVal2.length < 3) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Your reset time " + tempVal4 + " is invalid.");
                return CommonUtil.getNowTime();
            }
            int month = 0;
            int day = 0;
            if (tempVal2.length == 5) {
                month = Integer.parseInt(tempVal2[0]);
            }
            if (tempVal2.length >= 4) {
                day = Integer.parseInt(tempVal2[tempVal2.length - 4]);
            }
            thisResult = nowTime.atTime(Integer.parseInt(tempVal2[tempVal2.length - 3]),
                    Integer.parseInt(tempVal2[tempVal2.length - 2]),
                    Integer.parseInt(tempVal2[tempVal2.length - 1]));
            thisResult= thisResult.plusDays(day).plusMonths(month);
            if (CommonUtil.getNowTime().isAfter(thisResult)) {
                thisResult = thisResult.plusDays(1L);
            }
            if (refreshResult == null || thisResult.isBefore(refreshResult)) {
                refreshResult = thisResult;
            }
        }
        return refreshResult;
    }

    private LocalDateTime getTimerRefreshTime(String time) {
        LocalDateTime refreshResult = CommonUtil.getNowTime();
        String[] tempVal2 = time.split(":");
        if (tempVal2.length < 3) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Your reset time " + time + " is invalid.");
            return CommonUtil.getNowTime();
        }
        int month = 0;
        int day = 0;
        if (tempVal2.length == 5) {
            month = Integer.parseInt(tempVal2[0]);
        }
        if (tempVal2.length >= 4) {
            day = Integer.parseInt(tempVal2[tempVal2.length - 4]);
        }
        refreshResult = refreshResult.plusMonths(month).plusDays(day)
                .plusHours(Integer.parseInt(tempVal2[tempVal2.length - 3]))
                .plusMinutes(Integer.parseInt(tempVal2[tempVal2.length - 2]))
                .plusSeconds(Integer.parseInt(tempVal2[tempVal2.length - 1]));
        return refreshResult;
    }

}

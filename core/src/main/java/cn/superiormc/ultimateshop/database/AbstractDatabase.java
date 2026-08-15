package cn.superiormc.ultimateshop.database;

import cn.superiormc.ultimateshop.objects.caches.ObjectCache;

public abstract class AbstractDatabase {

    public void onInit() {
        // Empty...
    }

    public void onClose() {
        // Empty...
    }

    public abstract void checkData(ObjectCache cache);

    public abstract void updateData(PlayerDataSnapshot snapshot);

    public void updateDataOnDisable(PlayerDataSnapshot snapshot, boolean disable) {
        updateData(snapshot);
    }
}

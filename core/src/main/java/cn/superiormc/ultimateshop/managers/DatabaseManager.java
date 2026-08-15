package cn.superiormc.ultimateshop.managers;

import cn.superiormc.ultimateshop.database.AbstractDatabase;
import cn.superiormc.ultimateshop.database.SQLDatabase;
import cn.superiormc.ultimateshop.database.YamlDatabase;

public class DatabaseManager extends AbstractManager {

    public static DatabaseManager databaseManager;

    public AbstractDatabase database;

    public DatabaseManager() {
        databaseManager = this;
        if (ConfigManager.configManager.getBoolean("database.enabled")) {
            database = new SQLDatabase();
        } else {
            database = new YamlDatabase();
        }
        database.onInit();
    }
}


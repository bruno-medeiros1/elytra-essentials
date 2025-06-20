package org.bruno.elytraEssentials;

/* Licensed under the MIT License
 * @author CodingMaestro © 2025
 * @link https://github.com/bruno-medeiros1/elytra-essentials
*/

import com.github.jewishbanana.playerarmorchangeevent.PlayerArmorListener;
import net.milkbowl.vault.economy.Economy;
import org.bruno.elytraEssentials.commands.ElytraEssentialsCommand;
import org.bruno.elytraEssentials.handlers.*;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.FileHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.VersionHelper;
import org.bruno.elytraEssentials.listeners.*;
import org.bruno.elytraEssentials.placeholders.FlightTimePlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

//  TODO: [X] Add UpdateHandler to check for newer versions of the plugin
//  TODO: [X] Disabled elytra globally
//  TODO: [X] Allow to disable elytra only in specific worlds
//  TODO: [X] Add multiple speed limits per world.
//  TODO: [X] Easily disable the ability for players to equip an Elytra (add new config + permission to bypass that).
//  TODO: [X] Restrict or completely disable Elytra flight on your server.

//  TODO: [X] Add/set/remove flight time limits for players to ensure balanced gameplay.
//  TODO: [] Enable automatic recovery of flight time or customize how players regain flight.
//  TODO: [] Choose between a unique flight time display or show the exact remaining time for precision.
//  TODO: [X] Prevent fall damage when players run out of flight time, keeping them safe.

//  TODO: [] Fully customize Elytra flight, firework boosting, and riptide boosting across different worlds.
//  TODO: [] Disable firework boosting or set a customizable cooldown to balance Elytra flight.
//  TODO: [] Disable riptide boosting to prevent abuse.
//  TODO: [X] Review the plugin commands
//  TODO: [] Reward players with awesome Elytra flight effects, perfect for in-game purchases or special achievements.

//  TODO: [X] Placeholders API support
//  TODO: [X] Add BStats support
//  TODO: [] Add support for multiple versions (1.20 >)
//  TODO: [] Update for 1.21.5


public final class ElytraEssentials extends JavaPlugin {
    private final PluginDescriptionFile pluginDescriptionFile = this.getDescription();
    private final String pluginVersion = this.pluginDescriptionFile.getVersion();

    private ElytraFlightListener elytraFlightListener;
    private ElytraBoostListener elytraBoostListener;
    private ElytraEquipListener elytraEquipListener;
    private ElytraUpdaterListener elytraUpdaterListener;
    private ElytraEffectsListener elytraEffectsListener;

    private MessagesHandler messagesHandler;
    private MessagesHelper messagesHelper;
    private FileHelper fileHelper;
    private RecoveryHandler recoveryHandler;

    private ConfigHandler configHandler;
    private DatabaseHandler databaseHandler;
    private EffectsHandler effectsHandler;
    private TpsHandler tpsHandler;

    private boolean databaseConnectionSuccessful = false;
    public boolean newerVersion = false;
    private static Economy economy  = null;

    public void onLoad() {
        this.getConfig().options().copyDefaults();
        this.saveDefaultConfig();

        Object obj = new ConfigHandler(this.getConfig());
        this.configHandler = (ConfigHandler) obj;

        obj = new DatabaseHandler(this);
        this.databaseHandler = (DatabaseHandler) obj;
        try {
            databaseHandler.Initialize();
            databaseConnectionSuccessful = true;
        } catch (SQLException e) {
            getLogger().severe("Failed to connect to the database during plugin loading. Plugin will not be enabled.");
            getLogger().severe("Error: " + e.getMessage());
            getLogger().severe("Stack Trace:");
            for (StackTraceElement element : e.getStackTrace()) {
                getLogger().severe("  at " + element.toString());
            }
            databaseConnectionSuccessful = false;
        }

        obj = new FileHelper(this);
        this.fileHelper = (FileHelper) obj;

        obj = new EffectsHandler(this, this.fileHelper.GetShopFileConfiguration());
        this.effectsHandler = (EffectsHandler) obj;

        obj = new RecoveryHandler(this);
        this.recoveryHandler = (RecoveryHandler) obj;

        obj = new TpsHandler(this);
        this.tpsHandler = (TpsHandler) obj;

        obj = new MessagesHandler(this.fileHelper.GetMessagesFileConfiguration());
        this.messagesHandler = (MessagesHandler) obj;

        obj = new MessagesHelper(this);
        this.messagesHelper = (MessagesHelper) obj;

        this.messagesHelper.setDebugMode(this.configHandler.getIsDebugModeEnabled());
    }

    @Override
    public void onEnable() {
        if (!databaseConnectionSuccessful) {
            getLogger().severe(String.format("[%s] - Database connection was not established. Disabling plugin...", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.messagesHelper.sendConsoleMessage("&a-------------------------------------------");
        this.messagesHelper.sendConsoleMessage("&aDetected Version &d" + Bukkit.getVersion());
        this.messagesHelper.sendConsoleMessage("&aLoading settings for Version &d" + Bukkit.getVersion());

        this.messagesHelper.sendConsoleMessage("&aRegistering commands");
        this.getCommand("ee").setExecutor(new ElytraEssentialsCommand(this));

        this.messagesHelper.sendConsoleMessage("&aRegistering event listeners");
        this.elytraFlightListener = new ElytraFlightListener(this);
        this.elytraBoostListener = new ElytraBoostListener(this);
        this.elytraEquipListener = new ElytraEquipListener(this);
        this.elytraUpdaterListener = new ElytraUpdaterListener(this);
        this.elytraEffectsListener = new ElytraEffectsListener(this);
        Bukkit.getPluginManager().registerEvents(this.elytraFlightListener, this);
        Bukkit.getPluginManager().registerEvents(this.elytraBoostListener, this);
        Bukkit.getPluginManager().registerEvents(this.elytraEquipListener, this);
        Bukkit.getPluginManager().registerEvents(this.elytraUpdaterListener, this);
        Bukkit.getPluginManager().registerEvents(this.elytraEffectsListener, this);

        new PlayerArmorListener(this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new FlightTimePlaceholder(this).register();
            this.messagesHelper.sendConsoleMessage("PlaceholderAPI support enabled!");
        } else {
            getLogger().warning("PlaceholderAPI not found. Placeholder support is disabled.");
        }

        boolean checkForUpdatesEnabled = this.configHandler.getIsCheckForUpdatesEnabled();
        if (checkForUpdatesEnabled) {
            new UpdaterHandler(this, 126002).getVersion(latestVersion -> {
                String currentVersion = this.getDescription().getVersion();
                if (VersionHelper.isNewerVersion(currentVersion, latestVersion)) {
                    this.messagesHelper.sendConsoleMessage("&eA new version is available! &6https://www.spigotmc.org/resources/126002/");
                    this.newerVersion = true;
                } else {
                    this.messagesHelper.sendConsoleMessage("&aThe plugin is up-to-date.");
                }
            });
        }

        //  bStats
        new Metrics(this, 26164);

        this.messagesHelper.sendConsoleMessage("###########################################");
        this.messagesHelper.sendConsoleMessage("&ePlugin by: &6&lCodingMaestro");
        this.messagesHelper.sendConsoleMessage("&eVersion: &6&l" + this.pluginVersion);
        this.messagesHelper.sendConsoleMessage("&ahas been loaded successfully");
        this.messagesHelper.sendConsoleMessage("###########################################");
        this.messagesHelper.sendDebugMessage("&eDeveloper debug mode enabled!");
        this.messagesHelper.sendDebugMessage("&eThis WILL fill the console");
        this.messagesHelper.sendDebugMessage("&ewith additional ElytraEssentials information!");
        this.messagesHelper.sendDebugMessage("&eThis setting is not intended for ");
        this.messagesHelper.sendDebugMessage("&econtinous use!");

        this.getTpsHandler().start();
    }

    @Override
    public void onDisable() {

        //  Disable tasks
        this.getTpsHandler().cancel();

        StackTraceElement[] stackTraceElementArray = Thread.currentThread().getStackTrace();
        boolean isReloading;
        block7: {
            for (int i = 0; i < stackTraceElementArray.length; ++i) {
                StackTraceElement stackTraceElement = stackTraceElementArray[i];
                String className = stackTraceElement.getClassName();
                if (!className.startsWith("org.bukkit.craftbukkit.") ||
                        !className.endsWith(".CraftServer") ||
                        !stackTraceElement.getMethodName().equals("reload"))
                {
                    continue;
                }
                isReloading = true;
                break block7;
            }
            isReloading = false;
        }
        if (isReloading) {
            this.messagesHelper.sendConsoleLog("error", "&4\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
            this.messagesHelper.sendConsoleLog("error", "&4\u2551                             WARNING                                    \u2551");
            this.messagesHelper.sendConsoleLog("error", "&4\u2551      RELOADING THE SERVER WHILE ELYTRAESSENTIALS IS ENABLED MIGHT      \u2551");
            this.messagesHelper.sendConsoleLog("error", "&4\u2551                    LEAD TO UNEXPECTED ERRORS!                          \u2551");
            this.messagesHelper.sendConsoleLog("error", "&4\u2551                                                                        \u2551");
            this.messagesHelper.sendConsoleLog("error", "&4\u2551   Please to fully restart your server if you encounter issues!         \u2551");
            this.messagesHelper.sendConsoleLog("error", "&4\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
        }
        this.messagesHelper.sendConsoleMessage("-------------------------------------------");
        this.messagesHelper.sendConsoleMessage("&aPlugin by CodingMaestro");

        if (databaseConnectionSuccessful){
            this.messagesHelper.sendConsoleMessage("&aClosing database connection...");

            if (elytraFlightListener != null){
                Map<UUID, Integer> flightTimes = elytraFlightListener.GetAllActiveFlights();
                if (flightTimes != null ) {
                    for (Map.Entry<UUID,Integer> entry : flightTimes.entrySet()) {
                        try {
                            databaseHandler.SetPlayerFlightTime(entry.getKey(), entry.getValue());
                        } catch (SQLException e) {
                            this.messagesHelper.sendDebugMessage("Something went wrong while trying to set flight time");
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            databaseHandler.Disconnect();
        }

        HandlerList.unregisterAll(this);
        this.messagesHelper.sendConsoleMessage("&aAll event listeners unregistered successfully!");
        this.messagesHelper.sendConsoleMessage("&aPlugin Version &d&l" + pluginVersion);
        this.messagesHelper.sendConsoleMessage("&aPlugin shutdown successfully!");
        this.messagesHelper.sendConsoleMessage("&aGoodbye");
        this.messagesHelper.sendConsoleMessage("-------------------------------------------");
        this.elytraFlightListener = null;
        this.elytraBoostListener = null;
        this.elytraEquipListener = null;
        this.elytraUpdaterListener = null;
        this.elytraEffectsListener = null;
        this.messagesHandler = null;
        this.messagesHelper = null;
        this.configHandler = null;
        this.effectsHandler = null;
        this.recoveryHandler = null;
        this.tpsHandler = null;
    }

    public MessagesHelper getMessagesHelper() { return this.messagesHelper; }

    public MessagesHandler getMessagesHandlerInstance() { return this.messagesHandler; }

    public ConfigHandler getConfigHandlerInstance() { return this.configHandler; }

    public ElytraFlightListener getElytraFlightListener() { return this.elytraFlightListener; }

    public DatabaseHandler getDatabaseHandler() {
        return this.databaseHandler;
    }

    public EffectsHandler getEffectsHandler() { return this.effectsHandler; }

    public TpsHandler getTpsHandler() { return this.tpsHandler; }

    public Economy getEconomy() {
        return this.economy;
    }

    public void setConfigHandler(ConfigHandler configHandler) {
        this.configHandler = configHandler;
    }

    public void setMessagesHandler(MessagesHandler messagesHandler) {
        this.messagesHandler = messagesHandler;
    }

    public void setEffectsHandler(EffectsHandler effectsHandler) { this.effectsHandler = effectsHandler; }

    public void setRecoveryHandler(RecoveryHandler recoveryHandler) { this.recoveryHandler = recoveryHandler; }

    public void SetMessagesHelper(MessagesHelper messagesHelper) { this.messagesHelper = messagesHelper; }

    public void setFileHelper (FileHelper fileHelper) { this.fileHelper = fileHelper; }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }
}

package org.bruno.elytraEssentials;

/* Licensed under the MIT License
 * @author CodingMaestro © 2025
 * @link https://github.com/bruno-medeiros1/elytra-essentials
*/

import net.milkbowl.vault.economy.Economy;
import org.bruno.elytraEssentials.commands.EffectsCommand;
import org.bruno.elytraEssentials.commands.ElytraEssentialsCommand;
import org.bruno.elytraEssentials.commands.ForgeCommand;
import org.bruno.elytraEssentials.commands.ShopCommand;
import org.bruno.elytraEssentials.handlers.*;
import org.bruno.elytraEssentials.helpers.FileHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.VersionHelper;
import org.bruno.elytraEssentials.listeners.*;
import org.bruno.elytraEssentials.placeholders.ElytraEssentialsPlaceholders;
import org.bruno.elytraEssentials.utils.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;


public final class ElytraEssentials extends JavaPlugin {
    private final PluginDescriptionFile pluginDescriptionFile = this.getDescription();
    private final String pluginVersion = this.pluginDescriptionFile.getVersion();

    private ElytraFlightListener elytraFlightListener;
    private ElytraBoostListener elytraBoostListener;
    private ElytraEquipListener elytraEquipListener;
    private ElytraUpdaterListener elytraUpdaterListener;
    private ArmoredElytraListener armoredElytraListener;
    private ArmoredElytraDamageListener armoredElytraDamageListener;
    private EffectsGuiListener effectsGuiListener;
    private ShopGuiListener shopGuiListener;
    private ForgeGuiListener forgeGuiListener;
    private CombatTagListener combatTagListener;
    private EmergencyDeployListener emergencyDeployListener;

    private MessagesHandler messagesHandler;
    private MessagesHelper messagesHelper;
    private FileHelper fileHelper;
    private RecoveryHandler recoveryHandler;
    private StatsHandler statsHandler;

    private ConfigHandler configHandler;
    private DatabaseHandler databaseHandler;
    private EffectsHandler effectsHandler;
    private TpsHandler tpsHandler;

    private boolean databaseConnectionSuccessful = false;

    public boolean isNewerVersionAvailable = false;
    public String latestVersion;

    private static Economy economy  = null;
    private ElytraEssentialsPlaceholders elytraStatsExpansion;

    private ServerVersion serverVersion;

    public void onLoad() {
        this.serverVersion = ServerVersion.getCurrent();

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

        obj = new StatsHandler(this);
        this.statsHandler = (StatsHandler) obj;

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

        this.messagesHelper.sendConsoleMessage("&e###########################################");
        this.messagesHelper.sendConsoleMessage("&aDetected Version: &d" + Bukkit.getVersion());
        this.messagesHelper.sendConsoleMessage("&aLoading settings for Version: &d" + Bukkit.getVersion());

        this.messagesHelper.sendConsoleMessage("&aRegistering commands...");
        ElytraEssentialsCommand mainCommand = new ElytraEssentialsCommand(this);
        getCommand("ee").setExecutor(mainCommand);
        getCommand("ee").setTabCompleter(mainCommand);

        this.messagesHelper.sendConsoleMessage("&aRegistering event listeners...");
        this.elytraFlightListener = new ElytraFlightListener(this);
        this.elytraBoostListener = new ElytraBoostListener(this);
        this.elytraEquipListener = new ElytraEquipListener(this);
        this.elytraUpdaterListener = new ElytraUpdaterListener(this);
        this.armoredElytraListener = new ArmoredElytraListener(this);
        this.armoredElytraDamageListener = new ArmoredElytraDamageListener(this);
        this.combatTagListener = new CombatTagListener(this);
        this.emergencyDeployListener = new EmergencyDeployListener(this);

        this.effectsGuiListener = new EffectsGuiListener(this, new EffectsCommand(this), new ShopCommand(this));
        this.shopGuiListener = new ShopGuiListener(this, new EffectsCommand(this), new ShopCommand(this));
        this.forgeGuiListener = new ForgeGuiListener(this, new ForgeCommand(this));

        Bukkit.getPluginManager().registerEvents(this.elytraFlightListener, this);
        Bukkit.getPluginManager().registerEvents(this.elytraBoostListener, this);
        Bukkit.getPluginManager().registerEvents(this.elytraEquipListener, this);
        Bukkit.getPluginManager().registerEvents(this.elytraUpdaterListener, this);
        Bukkit.getPluginManager().registerEvents(this.armoredElytraListener, this);
        Bukkit.getPluginManager().registerEvents(this.armoredElytraDamageListener, this);
        Bukkit.getPluginManager().registerEvents(this.combatTagListener, this);
        Bukkit.getPluginManager().registerEvents(this.emergencyDeployListener, this);

        Bukkit.getPluginManager().registerEvents(this.effectsGuiListener, this);
        Bukkit.getPluginManager().registerEvents(this.shopGuiListener, this);
        Bukkit.getPluginManager().registerEvents(this.forgeGuiListener, this);

        //  Placeholder API Expansion classes
        registerPlaceholders();

        boolean checkForUpdatesEnabled = this.configHandler.getIsCheckForUpdatesEnabled();
        if (checkForUpdatesEnabled) {
            new UpdaterHandler(this, 126002).getVersion(latestVersion -> {
                String currentVersion = this.getDescription().getVersion();
                if (VersionHelper.isNewerVersion(currentVersion, latestVersion)) {
                    this.messagesHelper.sendConsoleMessage("§av" + latestVersion + " §eis available at &ahttps://www.spigotmc.org/resources/126002/");
                    this.messagesHelper.sendConsoleMessage("&ePlease update as you are currently using &cv" + currentVersion);
                    this.isNewerVersionAvailable = true;
                    this.latestVersion = latestVersion;
                }
            });
        }

        //  bStats
        new Metrics(this, 26164);

        this.messagesHelper.sendConsoleMessage("&aPlugin by: &bCodingMaestro");
        this.messagesHelper.sendConsoleMessage("&aVersion: &b" + this.pluginVersion);
        this.messagesHelper.sendConsoleMessage("&aHas been loaded successfully!");
        this.messagesHelper.sendConsoleMessage("&e###########################################");
        this.messagesHelper.sendDebugMessage("&eDeveloper debug mode enabled!");
        this.messagesHelper.sendDebugMessage("&eThis will fill the console");
        this.messagesHelper.sendDebugMessage("&eWith additional ElytraEssentials information!");
        this.messagesHelper.sendDebugMessage("&eThis setting is not intended for ");
        this.messagesHelper.sendDebugMessage("&econtinous use!");

        startAllPluginTasks();
    }

    @Override
    public void onDisable() {

        //  Disable tasks
        cancelAllPluginTasks();

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

        unregisterPlaceholders();

        this.messagesHelper.sendConsoleMessage("&aAll event listeners unregistered successfully!");
        this.messagesHelper.sendConsoleMessage("&aPlugin Version &d&l" + pluginVersion);
        this.messagesHelper.sendConsoleMessage("&aPlugin shutdown successfully!");
        this.messagesHelper.sendConsoleMessage("&aGoodbye");
        this.messagesHelper.sendConsoleMessage("-------------------------------------------");

        this.elytraFlightListener = null;
        this.elytraBoostListener = null;
        this.elytraEquipListener = null;
        this.elytraUpdaterListener = null;
        this.armoredElytraListener = null;
        this.armoredElytraDamageListener = null;
        this.effectsGuiListener = null;
        this.shopGuiListener = null;
        this.forgeGuiListener = null;
        this.combatTagListener = null;

        this.messagesHandler = null;
        this.messagesHelper = null;
        this.configHandler = null;
        this.effectsHandler = null;
        this.recoveryHandler = null;
        this.tpsHandler = null;
        this.statsHandler = null;
    }


    public MessagesHelper getMessagesHelper() { return this.messagesHelper; }

    public MessagesHandler getMessagesHandlerInstance() { return this.messagesHandler; }

    public ConfigHandler getConfigHandlerInstance() { return this.configHandler; }

    public ElytraFlightListener getElytraFlightListener() { return this.elytraFlightListener; }

    public ElytraBoostListener getElytraBoostListener() { return this.elytraBoostListener; }

    public ShopGuiListener getShopGuiListener() { return this.shopGuiListener; }

    public CombatTagListener getCombatTagListener() { return this.combatTagListener; }


    public DatabaseHandler getDatabaseHandler() {
        return this.databaseHandler;
    }

    public EffectsHandler getEffectsHandler() { return this.effectsHandler; }

    public RecoveryHandler getRecoveryHandler() { return this.recoveryHandler; }

    public TpsHandler getTpsHandler() { return this.tpsHandler; }

    public StatsHandler getStatsHandler() { return this.statsHandler; }

    public Economy getEconomy() {
        return this.economy;
    }

    public ServerVersion getServerVersion() { return this.serverVersion; }



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

    public void setStatsHandler (StatsHandler statsHandler) { this.statsHandler = statsHandler; }



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

    public void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.elytraStatsExpansion = new ElytraEssentialsPlaceholders(this);
            this.elytraStatsExpansion.register();
            this.messagesHelper.sendConsoleMessage("&aSuccessfully hooked into PlaceholderAPI");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }
    }

    public void unregisterPlaceholders() {
        if (this.elytraStatsExpansion != null) {
            this.elytraStatsExpansion.unregister();
            this.messagesHelper.sendConsoleMessage("&aSuccessfully unhooked from PlaceholderAPI.");
        }
    }


    //  HELPERS

    public void cancelAllPluginTasks(){
        this.getRecoveryHandler().cancel();
        this.getTpsHandler().cancel();
        this.getStatsHandler().cancel();
        this.getDatabaseHandler().cancelBackupTask();
        this.getCombatTagListener().cancel();
    }

    public void startAllPluginTasks(){
        this.getRecoveryHandler().start();
        this.getTpsHandler().start();
        this.getStatsHandler().start();
        this.getDatabaseHandler().startAutoBackupTask();
        this.getCombatTagListener().start();
    }
}

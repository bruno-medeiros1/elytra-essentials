package org.bruno.elytraEssentials;

/* Licensed under the MIT License
 * @author CodingMaestro © 2025
 * @link https://github.com/bruno-medeiros1/elytra-essentials
*/

import net.milkbowl.vault.economy.Economy;
import org.bruno.elytraEssentials.commands.*;
import org.bruno.elytraEssentials.handlers.*;
import org.bruno.elytraEssentials.helpers.ArmoredElytraHelper;
import org.bruno.elytraEssentials.helpers.FileHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.VersionHelper;
import org.bruno.elytraEssentials.listeners.*;
import org.bruno.elytraEssentials.placeholders.ElytraEssentialsPlaceholders;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.utils.ServerVersion;
import org.bruno.elytraEssentials.utils.StatType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

import java.sql.SQLException;
import java.util.logging.Level;

public final class ElytraEssentials extends JavaPlugin {
    // Handlers
    private ConfigHandler configHandler;
    private DatabaseHandler databaseHandler;
    private EffectsHandler effectsHandler;
    private TpsHandler tpsHandler;
    private RecoveryHandler recoveryHandler;
    private StatsHandler statsHandler;
    private AchievementsHandler achievementsHandler;

    // Helpers
    private MessagesHandler messagesHandler;
    private MessagesHelper messagesHelper;
    private FileHelper fileHelper;
    private ArmoredElytraHelper armoredElytraHelper;

    // Listeners
    private ElytraFlightListener elytraFlightListener;
    private ElytraBoostListener elytraBoostListener;
    private ElytraEquipListener elytraEquipListener;
    private ElytraUpdaterListener elytraUpdaterListener;
    private ArmoredElytraListener armoredElytraListener;
    private ArmoredElytraDamageListener armoredElytraDamageListener;
    private CombatTagListener combatTagListener;
    private ShopGuiListener shopGuiListener;
    private EffectsGuiListener effectsGuiListener;
    private ForgeGuiListener forgeGuiListener;
    private AchievementsGuiListener achievementsGuiListener;

    // Commands
    private ElytraEssentialsCommand mainCommand;
    private HelpCommand helpCommand;
    private ReloadCommand reloadCommand;
    private FlightTimeCommand flightTimeCommand;
    private ShopCommand shopCommand;
    private EffectsCommand effectsCommand;
    private StatsCommand statsCommand;
    private TopCommand topCommand;
    private ForgeCommand forgeCommand;
    private AchievementsCommand achievementsCommand;
    private ArmorCommand armorCommand;
    private ImportDbCommand importDbCommand;

    // Integrations & Info
    private static Economy economy = null;
    private ElytraEssentialsPlaceholders elytraStatsExpansion;
    private ServerVersion serverVersion;
    public boolean isNewerVersionAvailable = false;
    public String latestVersion;


    @Override
    public void onLoad() {
        this.serverVersion = ServerVersion.getCurrent();
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        if (!setupHandlers()) return;
        if (!setupDatabase()) return;
        if (!setupEconomy()) return;

        setupCommands();
        setupListeners();
        registerPlaceholders();
        startAllPluginTasks();

        new Metrics(this, Constants.Integrations.BSTATS_ID);
        checkForUpdates();

        messagesHelper.sendConsoleMessage("&aPlugin v" + getDescription().getVersion() + " has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        cancelAllPluginTasks();
        if (databaseHandler != null) {
            databaseHandler.saveAllData();
            databaseHandler.Disconnect();
        }
        unregisterPlaceholders();
        HandlerList.unregisterAll(this);
        getLogger().info("ElytraEssentials has been disabled.");
    }


    //<editor-fold desc="Setup Methods">
    public boolean setupHandlers() {
        try {
            this.configHandler = new ConfigHandler(this.getConfig());
            this.fileHelper = new FileHelper(this);
            this.fileHelper.initialize(); // Creates and loads custom YMLs

            this.messagesHandler = new MessagesHandler(this.fileHelper.getMessagesConfig());
            this.messagesHelper = new MessagesHelper(this);
            this.messagesHelper.setDebugMode(this.configHandler.getIsDebugModeEnabled());
            this.armoredElytraHelper = new ArmoredElytraHelper(this);

            this.effectsHandler = new EffectsHandler(this, this.fileHelper.getShopConfig());
            this.tpsHandler = new TpsHandler(this);
            this.recoveryHandler = new RecoveryHandler(this);
            this.statsHandler = new StatsHandler(this);
            this.achievementsHandler = new AchievementsHandler(this);
            this.combatTagListener = new CombatTagListener(this);

            this.shopCommand = new ShopCommand(this);
            this.effectsCommand = new EffectsCommand(this);
            this.forgeCommand = new ForgeCommand(this);
            this.achievementsCommand = new AchievementsCommand(this);

            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize core handlers. Disabling plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
    }

    public boolean setupDatabase() {
        this.databaseHandler = new DatabaseHandler(this);
        try {
            databaseHandler.Initialize();
            return true;
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to connect to the database. Disabling plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
    }

    private void setupListeners() {
        Bukkit.getLogger().info("Registering event listeners...");

        // Initialize all listeners and store their instances
        this.elytraFlightListener = new ElytraFlightListener(this);
        this.elytraBoostListener = new ElytraBoostListener(this);
        this.elytraEquipListener = new ElytraEquipListener(this);
        this.elytraUpdaterListener = new ElytraUpdaterListener(this);
        this.armoredElytraListener = new ArmoredElytraListener(this);
        this.armoredElytraDamageListener = new ArmoredElytraDamageListener(this);
        this.combatTagListener = new CombatTagListener(this);
        this.shopGuiListener = new ShopGuiListener(this);
        this.effectsGuiListener = new EffectsGuiListener(this);
        this.forgeGuiListener = new ForgeGuiListener(this);
        this.achievementsGuiListener = new AchievementsGuiListener(this, achievementsCommand);

        // Register all listeners instances
        Bukkit.getPluginManager().registerEvents(elytraFlightListener, this);
        Bukkit.getPluginManager().registerEvents(elytraBoostListener, this);
        Bukkit.getPluginManager().registerEvents(elytraEquipListener, this);
        Bukkit.getPluginManager().registerEvents(elytraUpdaterListener, this);
        Bukkit.getPluginManager().registerEvents(armoredElytraListener, this);
        Bukkit.getPluginManager().registerEvents(armoredElytraDamageListener, this);
        Bukkit.getPluginManager().registerEvents(combatTagListener, this);
        Bukkit.getPluginManager().registerEvents(shopGuiListener, this);
        Bukkit.getPluginManager().registerEvents(effectsGuiListener, this);
        Bukkit.getPluginManager().registerEvents(forgeGuiListener, this);
        Bukkit.getPluginManager().registerEvents(achievementsGuiListener, this);
    }

    private void setupCommands() {
        Bukkit.getLogger().info("Registering commands...");

        this.helpCommand = new HelpCommand(this);
        this.reloadCommand = new ReloadCommand(this);
        this.flightTimeCommand = new FlightTimeCommand(this);
        this.shopCommand = new ShopCommand(this);
        this.effectsCommand = new EffectsCommand(this);
        this.statsCommand = new StatsCommand(this);
        this.topCommand = new TopCommand(this);
        this.forgeCommand = new ForgeCommand(this);
        this.armorCommand = new ArmorCommand(this);
        this.importDbCommand = new ImportDbCommand(this);
        this.achievementsCommand = new AchievementsCommand(this);

        this.mainCommand = new ElytraEssentialsCommand(this);
        mainCommand.registerSubCommand("help", helpCommand);
        mainCommand.registerSubCommand("reload", reloadCommand);
        mainCommand.registerSubCommand("ft", flightTimeCommand);
        mainCommand.registerSubCommand("shop", shopCommand);
        mainCommand.registerSubCommand("effects", effectsCommand);
        mainCommand.registerSubCommand("stats", statsCommand);
        mainCommand.registerSubCommand("top", topCommand);
        mainCommand.registerSubCommand("forge", forgeCommand);
        mainCommand.registerSubCommand("armor", armorCommand);
        mainCommand.registerSubCommand("importdb", importDbCommand);
        mainCommand.registerSubCommand("achievements", achievementsCommand);

        getCommand("ee").setExecutor(mainCommand);
        getCommand("ee").setTabCompleter(mainCommand);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found! Economy features will be disabled.");
            return true; // Don't disable the whole plugin, just the feature
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("No Vault economy provider found. Economy features will be disabled.");
            return true;
        }
        economy = rsp.getProvider();
        Bukkit.getLogger().info("Successfully hooked into Vault.");
        return true;
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.elytraStatsExpansion = new ElytraEssentialsPlaceholders(this);
            this.elytraStatsExpansion.register();
            Bukkit.getLogger().info("Successfully hooked into PlaceholderAPI.");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }
    }

    private void unregisterPlaceholders() {
        if (this.elytraStatsExpansion != null) {
            this.elytraStatsExpansion.unregister();
        }
    }

    private void checkForUpdates() {
        if (configHandler.getIsCheckForUpdatesEnabled()) {
            new UpdaterHandler(this, Constants.Integrations.SPIGOT_RESOURCE_ID).getVersion(latestVersion -> {
                if (VersionHelper.isNewerVersion(getDescription().getVersion(), latestVersion)) {
                    Bukkit.getLogger().warning("A new version (" + latestVersion + ") is available!");
                    this.isNewerVersionAvailable = true;
                    this.latestVersion = latestVersion;
                }
            });
        }
    }
    //</editor-fold>

    //<editor-fold desc="Task Management">
    public void cancelAllPluginTasks(){
        if (recoveryHandler != null) recoveryHandler.cancel();
        if (tpsHandler != null) tpsHandler.cancel();
        if (statsHandler != null) statsHandler.cancel();
        if (achievementsHandler != null) achievementsHandler.cancel();
        if (combatTagListener != null) combatTagListener.cancel();
        if (elytraFlightListener != null) elytraFlightListener.cancel();

        if (databaseHandler != null) databaseHandler.cancelBackupTask();
    }

    public void startAllPluginTasks(){
        if (databaseHandler != null) databaseHandler.startAutoBackupTask();

        if (recoveryHandler != null) recoveryHandler.start();
        if (tpsHandler != null) tpsHandler.start();
        if (statsHandler != null) statsHandler.start();
        if (achievementsHandler != null) achievementsHandler.start();
        if (combatTagListener != null) combatTagListener.start();
        if (elytraFlightListener != null) elytraFlightListener.start();
    }
    //</editor-fold>

    //<editor-fold desc="GUI Openers">
    public void openShopGUI(Player player, int page) {
        if (shopCommand != null) {
            shopCommand.openShop(player, page);
        }
    }

    public void openEffectsGUI(Player player) {
        if (effectsCommand != null) {
            effectsCommand.openOwnedEffects(player);
        }
    }

    public void openAchievementsGUI(Player player, int page, StatType filter){
        if (achievementsCommand != null) {
            achievementsCommand.openAchievementsGUI(player, page, filter);
        }
    }
    //</editor-fold>

    //<editor-fold desc="Getters">
    public MessagesHelper getMessagesHelper() { return this.messagesHelper; }
    public MessagesHandler getMessagesHandlerInstance() { return this.messagesHandler; }
    public ConfigHandler getConfigHandlerInstance() { return this.configHandler; }
    public ShopGuiListener getShopGuiListener() { return this.shopGuiListener; }
    public AchievementsGuiListener getAchievementsGuiListener() { return this.achievementsGuiListener; }
    public DatabaseHandler getDatabaseHandler() { return this.databaseHandler; }
    public EffectsHandler getEffectsHandler() { return this.effectsHandler; }
    public TpsHandler getTpsHandler() { return this.tpsHandler; }
    public StatsHandler getStatsHandler() { return this.statsHandler; }
    public AchievementsHandler getAchievementsHandler() { return this.achievementsHandler; }
    public Economy getEconomy() { return economy; }
    public ServerVersion getServerVersion() { return this.serverVersion; }
    public FileConfiguration getAchievementsFileConfiguration() { return this.fileHelper.getAchievementsConfig(); }
    public ArmoredElytraHelper getArmoredElytraHelper() { return this.armoredElytraHelper; }
    public ElytraBoostListener getElytraBoostListener() { return this.elytraBoostListener; }
    public ElytraFlightListener getElytraFlightListener() { return this.elytraFlightListener; }
    public String getLatestVersion() { return this.latestVersion; }
    //</editor-fold>
}

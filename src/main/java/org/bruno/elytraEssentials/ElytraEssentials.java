package org.bruno.elytraEssentials;

/*
 * This file is part of ElytraEssentials.
 *
 * ElytraEssentials is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * ElytraEssentials is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

import net.milkbowl.vault.economy.Economy;
import org.bruno.elytraEssentials.commands.*;
import org.bruno.elytraEssentials.gui.GuiListener;
import org.bruno.elytraEssentials.handlers.*;
import org.bruno.elytraEssentials.gui.achievements.AchievementsGuiHandler;
import org.bruno.elytraEssentials.gui.effects.EffectsGuiHandler;
import org.bruno.elytraEssentials.gui.forge.ForgeGuiHandler;
import org.bruno.elytraEssentials.gui.shop.ShopGuiHandler;
import org.bruno.elytraEssentials.helpers.*;
import org.bruno.elytraEssentials.listeners.*;
import org.bruno.elytraEssentials.placeholders.ElytraEssentialsPlaceholders;
import org.bruno.elytraEssentials.utils.Constants;
import org.bruno.elytraEssentials.utils.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

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
    private FlightHandler flightHandler;
    private ArmoredElytraHandler armoredElytraHandler;
    private ShopGuiHandler shopGuiHandler;
    private ForgeGuiHandler forgeGuiHandler;
    private EffectsGuiHandler effectsGuiHandler;
    private AchievementsGuiHandler achievementsGuiHandler;
    private ElytraEquipHandler elytraEquipHandler;
    private BoostHandler boostHandler;
    private CombatTagHandler combatTagHandler;

    // Helpers
    private MessagesHandler messagesHandler;
    private MessagesHelper messagesHelper;
    private FileHelper fileHelper;
    private ArmoredElytraHelper armoredElytraHelper;
    private FoliaHelper foliaHelper;

    // Listeners
    private ElytraFlightListener elytraFlightListener;
    private BoostListener elytraBoostListener;
    private ElytraEquipListener elytraEquipListener;
    private ElytraUpdaterListener elytraUpdaterListener;
    private ArmoredElytraListener armoredElytraListener;
    private CombatTagListener combatTagListener;
    private DamageListener damageListener;
    private GuiListener guiListener;

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
        if (!setupHelpers()) return;
        if (!setupHandlers()) return;
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
        shutdown();

        if (databaseHandler != null) {
            databaseHandler.Disconnect();
        }
        unregisterPlaceholders();
        HandlerList.unregisterAll(this);
        getLogger().info("ElytraEssentials has been disabled.");
    }


    //<editor-fold desc="Setup Methods">

    public boolean setupHelpers(){
        try {
            this.foliaHelper = new FoliaHelper(this);
            this.fileHelper = new FileHelper(this);
            this.messagesHelper = new MessagesHelper(this);
            this.armoredElytraHelper = new ArmoredElytraHelper(this);

            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize helpers. Disabling plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
    }

    public boolean setupHandlers() {
        try {
            this.configHandler = new ConfigHandler(this.getConfig());
            this.messagesHelper.setDebugMode(this.configHandler.getIsDebugModeEnabled());

            this.databaseHandler = new DatabaseHandler(this, this.configHandler, this.foliaHelper, this.messagesHelper);
            databaseHandler.Initialize();

            this.messagesHandler = new MessagesHandler(this.fileHelper.getMessagesConfig());
            this.messagesHelper.setPrefix(this.messagesHandler.getPrefixMessage());

            this.effectsHandler = new EffectsHandler(this, this.fileHelper.getShopConfig(), this.foliaHelper, this.databaseHandler, this.messagesHelper);
            this.tpsHandler = new TpsHandler(this, this.foliaHelper, this.messagesHelper);
            this.recoveryHandler = new RecoveryHandler(this, this.flightHandler, this.messagesHelper);
            this.statsHandler = new StatsHandler(this, this.databaseHandler, this.foliaHelper, messagesHelper, effectsHandler);
            this.achievementsHandler = new AchievementsHandler(this, this.databaseHandler, this.statsHandler, this.foliaHelper, this.messagesHelper);

            this.boostHandler = new BoostHandler(this, this.foliaHelper, this.messagesHelper);
            this.flightHandler = new FlightHandler(this, this.configHandler, this.effectsHandler, this.boostHandler, this.foliaHelper, this.messagesHelper, this.databaseHandler);
            this.boostHandler.setFlightHandler(this.flightHandler);

            this.armoredElytraHandler = new ArmoredElytraHandler(this, this.configHandler, this.foliaHelper, this.armoredElytraHelper, this.messagesHelper);
            this.forgeGuiHandler = new ForgeGuiHandler(this.configHandler, this.armoredElytraHelper, this.foliaHelper);
            this.achievementsGuiHandler = new AchievementsGuiHandler(this, this.databaseHandler, this.foliaHelper, this.messagesHelper);
            this.elytraEquipHandler = new ElytraEquipHandler(this.configHandler, this.messagesHelper, this.foliaHelper);

            this.effectsGuiHandler = new EffectsGuiHandler(this, this.effectsHandler, this.databaseHandler, this.foliaHelper, this.messagesHelper);
            this.shopGuiHandler = new ShopGuiHandler(this, this.effectsHandler, this.effectsGuiHandler);
            this.effectsGuiHandler.setShopGuiHandler(this.shopGuiHandler);

            this.combatTagHandler = new CombatTagHandler(this, this.configHandler, this.messagesHelper, this.foliaHelper);

            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize core handlers. Disabling plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
    }

    private void setupListeners() {
        Bukkit.getLogger().info("Registering event listeners...");

        // Initialize all listeners and store their instances
        this.elytraFlightListener = new ElytraFlightListener(this.flightHandler, this.statsHandler, this.effectsHandler);
        this.elytraBoostListener = new BoostListener(this.boostHandler);
        this.elytraEquipListener = new ElytraEquipListener(this.elytraEquipHandler);
        this.elytraUpdaterListener = new ElytraUpdaterListener(this, this.messagesHelper);
        this.armoredElytraListener = new ArmoredElytraListener(this.armoredElytraHandler, this.configHandler);
        this.combatTagListener = new CombatTagListener(this.combatTagHandler);
        this.damageListener = new DamageListener(this.flightHandler, this.statsHandler, this.armoredElytraHandler);
        this.guiListener = new GuiListener(this.shopGuiHandler, this.forgeGuiHandler, this.effectsGuiHandler, this.achievementsGuiHandler);

        // Register all listeners instances
        Bukkit.getPluginManager().registerEvents(elytraFlightListener, this);
        Bukkit.getPluginManager().registerEvents(elytraBoostListener, this);
        Bukkit.getPluginManager().registerEvents(elytraEquipListener, this);
        Bukkit.getPluginManager().registerEvents(elytraUpdaterListener, this);
        Bukkit.getPluginManager().registerEvents(armoredElytraListener, this);
        Bukkit.getPluginManager().registerEvents(combatTagListener, this);
        Bukkit.getPluginManager().registerEvents(damageListener, this);
        Bukkit.getPluginManager().registerEvents(guiListener, this);
    }

    private void setupCommands() {
        Bukkit.getLogger().info("Registering commands...");

        var helpCommand = new HelpCommand(this);
        var reloadCommand = new ReloadCommand(this, this.messagesHelper);
        var flightTimeCommand = new FlightTimeCommand(this, this.flightHandler, this.configHandler, this.messagesHelper, this.foliaHelper);
        var shopCommand = new ShopCommand(this.shopGuiHandler, this.messagesHelper);
        var effectsCommand = new EffectsCommand(this, this.effectsGuiHandler, this.effectsHandler, this.databaseHandler, this.foliaHelper, this.messagesHelper);
        var statsCommand = new StatsCommand(this.statsHandler, this.messagesHelper);
        var topCommand = new TopCommand(this.statsHandler, this.messagesHelper);
        var forgeCommand = new ForgeCommand(this.forgeGuiHandler, this.configHandler, this.messagesHelper);
        var armorCommand = new ArmorCommand(this, this.messagesHelper);
        var importDbCommand = new ImportDbCommand(this, this.messagesHelper);
        var achievementsCommand = new AchievementsCommand(this.achievementsGuiHandler, this.messagesHelper);

        // Commands
        ElytraEssentialsCommand mainCommand = new ElytraEssentialsCommand(this, this.messagesHelper);
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
            this.elytraStatsExpansion = new ElytraEssentialsPlaceholders(this, this.flightHandler);
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
            new UpdaterHandler(this, this.foliaHelper, Constants.Integrations.SPIGOT_RESOURCE_ID).getVersion(latestVersion -> {
                if (VersionHelper.isNewerVersion(getDescription().getVersion(), latestVersion)) {
                    Bukkit.getLogger().warning("A new version (" + latestVersion + ") is available!");
                    this.isNewerVersionAvailable = true;
                    this.latestVersion = latestVersion;
                }
            });
        }
    }
    //</editor-fold>

    public void shutdown(){
        if (recoveryHandler != null)
            recoveryHandler.cancel();

        if (tpsHandler != null)
            tpsHandler.cancel();

        if (statsHandler != null)
            statsHandler.shutdown();

        if (achievementsHandler != null)
            achievementsHandler.shutdown();

        if (combatTagHandler != null)
            combatTagHandler.shutdown();

        if (flightHandler != null)
            flightHandler.shutdown();

        if (databaseHandler != null)
            databaseHandler.shutdown();
    }


    /**
     * This is responsible for starting all plugin tasks that need to run periodically.
     */
    public void startAllPluginTasks(){
        if (databaseHandler != null) databaseHandler.start();

        if (recoveryHandler != null) recoveryHandler.start();
        if (tpsHandler != null) tpsHandler.start();
        if (statsHandler != null) statsHandler.start();
        if (achievementsHandler != null) achievementsHandler.start();
        if (flightHandler != null) flightHandler.start();
        if (combatTagHandler != null) combatTagHandler.start();
    }

    //<editor-fold desc="Getters">
    public MessagesHandler getMessagesHandlerInstance() { return this.messagesHandler; }
    public ConfigHandler getConfigHandlerInstance() { return this.configHandler; }
    public DatabaseHandler getDatabaseHandler() { return this.databaseHandler; }
    public EffectsHandler getEffectsHandler() { return this.effectsHandler; }
    public TpsHandler getTpsHandler() { return this.tpsHandler; }
    public StatsHandler getStatsHandler() { return this.statsHandler; }
    public AchievementsHandler getAchievementsHandler() { return this.achievementsHandler; }

    public Economy getEconomy() { return economy; }
    public ServerVersion getServerVersion() { return this.serverVersion; }
    public FileConfiguration getAchievementsFileConfiguration() { return this.fileHelper.getAchievementsConfig(); }
    public String getLatestVersion() { return this.latestVersion; }
    //</editor-fold>
}

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
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

import java.util.Objects;
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
    private PluginInfoHandler pluginInfoHandler;
    private UpdaterHandler updaterHandler;

    // Helpers
    private MessagesHandler messagesHandler;
    private MessagesHelper messagesHelper;
    private FileHelper fileHelper;
    private ArmoredElytraHelper armoredElytraHelper;
    private FoliaHelper foliaHelper;

    // Integrations
    private Economy economy = null;
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
        updaterHandler.performCheck();

        messagesHelper.sendConsoleMessage("&aPlugin v" + pluginInfoHandler.getCurrentVersion() + " has been enabled successfully!");
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

    public boolean setupHelpers(){
        try {
            this.foliaHelper = new FoliaHelper(this);
            this.fileHelper = new FileHelper(this, getLogger());
            this.messagesHelper = new MessagesHelper(this, this.serverVersion);
            this.armoredElytraHelper = new ArmoredElytraHelper(this, getLogger());

            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize helpers. Disabling plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
    }

    public boolean setupHandlers() {
        try {
            this.pluginInfoHandler = new PluginInfoHandler(this.getPluginMeta());

            this.configHandler = new ConfigHandler(this.getConfig(), getLogger());
            this.messagesHelper.setDebugMode(this.configHandler.getIsDebugModeEnabled());

            this.databaseHandler = new DatabaseHandler(this, this.configHandler, this.foliaHelper, this.messagesHelper, getLogger());
            databaseHandler.Initialize();

            this.messagesHandler = new MessagesHandler(this.fileHelper.getMessagesConfig());
            this.messagesHelper.setPrefix(this.messagesHandler.getPrefixMessage());

            this.effectsHandler = new EffectsHandler(this, this.fileHelper.getShopConfig(), this.foliaHelper, this.databaseHandler, this.messagesHelper, this.serverVersion, this.economy, this.tpsHandler, this.messagesHandler, getLogger());
            this.tpsHandler = new TpsHandler(this, this.foliaHelper, this.messagesHelper);
            this.recoveryHandler = new RecoveryHandler(this.flightHandler, this.messagesHelper, this.messagesHandler, this.configHandler, this.foliaHelper);
            this.statsHandler = new StatsHandler(getLogger(), this.databaseHandler, this.foliaHelper, messagesHelper, effectsHandler);
            this.achievementsHandler = new AchievementsHandler(this, this.databaseHandler, this.statsHandler, this.foliaHelper, this.messagesHelper, this.fileHelper.getAchievementsConfig(), getLogger());

            this.boostHandler = new BoostHandler(this, this.foliaHelper, this.messagesHelper, this.serverVersion, this.statsHandler, this.configHandler, this.messagesHandler);
            this.flightHandler = new FlightHandler(getLogger(), this.configHandler, this.effectsHandler, this.boostHandler, this.foliaHelper, this.messagesHelper, this.databaseHandler, this.statsHandler, this.messagesHandler);
            this.boostHandler.setFlightHandler(this.flightHandler);

            this.armoredElytraHandler = new ArmoredElytraHandler(this, this.configHandler, this.foliaHelper, this.armoredElytraHelper, this.messagesHelper);
            this.forgeGuiHandler = new ForgeGuiHandler(this.configHandler, this.armoredElytraHelper, this.foliaHelper);
            this.achievementsGuiHandler = new AchievementsGuiHandler(getLogger(), this.databaseHandler, this.foliaHelper, this.messagesHelper, this.achievementsHandler, this.statsHandler);
            this.elytraEquipHandler = new ElytraEquipHandler(this.configHandler, this.messagesHelper, this.foliaHelper);

            this.effectsGuiHandler = new EffectsGuiHandler(this, this.effectsHandler, this.databaseHandler, this.foliaHelper, this.messagesHelper, getLogger());
            this.shopGuiHandler = new ShopGuiHandler(this, this.effectsHandler, this.effectsGuiHandler, getLogger());
            this.effectsGuiHandler.setShopGuiHandler(this.shopGuiHandler);

            this.combatTagHandler = new CombatTagHandler(this, this.configHandler, this.messagesHelper, this.foliaHelper);

            this.updaterHandler = new UpdaterHandler(getLogger(), this.foliaHelper, this.configHandler, this.pluginInfoHandler);

            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize core handlers. Disabling plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
    }

    private void setupListeners() {
        getLogger().info("Registering event listeners...");

        // Initialize all listeners and store their instances
        var elytraFlightListener = new ElytraFlightListener(this.flightHandler, this.statsHandler, this.effectsHandler);
        var elytraBoostListener = new BoostListener(this.boostHandler);
        var elytraEquipListener = new ElytraEquipListener(this.elytraEquipHandler);
        var elytraUpdaterListener = new ElytraUpdaterListener(this, this.messagesHelper, this.latestVersion, this.configHandler);
        var armoredElytraListener = new ArmoredElytraListener(this.armoredElytraHandler, this.configHandler);
        var combatTagListener = new CombatTagListener(this.combatTagHandler);
        var damageListener = new DamageListener(this.flightHandler, this.statsHandler, this.armoredElytraHandler);
        var guiListener = new GuiListener(this.shopGuiHandler, this.forgeGuiHandler, this.effectsGuiHandler, this.achievementsGuiHandler);

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
        getLogger().info("Registering commands...");

        var helpCommand = new HelpCommand(this);
        var reloadCommand = new ReloadCommand(this, this.messagesHelper, this.statsHandler, this.databaseHandler, this.messagesHandler, getLogger());
        var flightTimeCommand = new FlightTimeCommand(this.flightHandler, this.configHandler, this.messagesHelper, this.foliaHelper, this.messagesHandler);
        var shopCommand = new ShopCommand(this.shopGuiHandler, this.messagesHelper);
        var effectsCommand = new EffectsCommand(getLogger(), this.effectsGuiHandler, this.effectsHandler, this.databaseHandler, this.foliaHelper, this.messagesHelper);
        var statsCommand = new StatsCommand(this.statsHandler, this.messagesHelper);
        var topCommand = new TopCommand(this.statsHandler, this.messagesHelper);
        var forgeCommand = new ForgeCommand(this.forgeGuiHandler, this.configHandler, this.messagesHelper);
        var armorCommand = new ArmorCommand(this, this.messagesHelper, this.economy, this.configHandler, this.messagesHandler);
        var importDbCommand = new ImportDbCommand(this, getLogger(), this.messagesHelper, this.databaseHandler, this.messagesHandler);
        var achievementsCommand = new AchievementsCommand(this.achievementsGuiHandler, this.messagesHelper);

        // Commands
        ElytraEssentialsCommand mainCommand = new ElytraEssentialsCommand(getLogger(), this.messagesHelper);
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

        Objects.requireNonNull(getCommand("ee")).setExecutor(mainCommand);
        Objects.requireNonNull(getCommand("ee")).setTabCompleter(mainCommand);
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
        getLogger().info("Successfully hooked into Vault.");
        return true;
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.elytraStatsExpansion = new ElytraEssentialsPlaceholders(this.flightHandler, this.statsHandler, this.effectsHandler, this.databaseHandler, this.configHandler, this.pluginInfoHandler);
            this.elytraStatsExpansion.register();
            getLogger().info("Successfully hooked into PlaceholderAPI.");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }
    }

    private void unregisterPlaceholders() {
        if (this.elytraStatsExpansion != null) {
            this.elytraStatsExpansion.unregister();
        }
    }

    public void shutdown(){
        if (recoveryHandler != null)
            recoveryHandler.shutdown();

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

    public void startAllPluginTasks(){
        if (databaseHandler != null) databaseHandler.start();

        if (recoveryHandler != null) recoveryHandler.start();
        if (tpsHandler != null) tpsHandler.start();
        if (statsHandler != null) statsHandler.start();
        if (achievementsHandler != null) achievementsHandler.start();
        if (flightHandler != null) flightHandler.start();
        if (combatTagHandler != null) combatTagHandler.start();
    }
}

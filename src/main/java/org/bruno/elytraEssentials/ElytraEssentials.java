package org.bruno.elytraEssentials;

import com.github.jewishbanana.playerarmorchangeevent.PlayerArmorListener;
import org.bruno.elytraEssentials.commands.ElytraEssentialsCommand;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.handlers.DatabaseHandler;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.listeners.ElytraBoostListener;
import org.bruno.elytraEssentials.listeners.ElytraEquipListener;
import org.bruno.elytraEssentials.listeners.ElytraFlightListener;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

//  TODO: [] Add UpdateHandler to check for newer versions of the plugin
//  TODO: [X] Add MaxSpeed in the configuration
//  TODO: [X] Reload not working
//  TODO: [X] Disabled elytra globally
//  TODO: [X] Allow to disable elytra only in specific worlds
//  TODO: [X] Add multiple speed limits per world.
//  TODO: [X] Easily disable the ability for players to equip an Elytra (add new config + permission to bypass that).
//  TODO: [X] Restrict or completely disable Elytra flight on your server.

//  TODO: [] Add/set/remove flight time limits for players to ensure balanced gameplay.
//  TODO: [] Enable automatic recovery of flight time or customize how players regain flight.
//  TODO: [] Choose between a unique flight time display or show the exact remaining time for precision.
//  TODO: [] Prevent fall damage when players run out of flight time, keeping them safe.

//  TODO: [] Fully customize Elytra flight, firework boosting, and riptide boosting across different worlds.
//  TODO: [] Disable firework boosting or set a customizable cooldown to balance Elytra flight.
//  TODO: [] Disable riptide boosting to prevent abuse.
//  TODO: [] Review the plugin commands
//  TODO: [] Reward players with awesome Elytra flight effects, perfect for in-game purchases or special achievements.

//  TODO: Add placeholders (Placeholders API)
//  TODO: Add support for multiple versions (1.20 >)

public final class ElytraEssentials extends JavaPlugin {
    private final PluginDescriptionFile pluginDescriptionFile = this.getDescription();
    private final String pluginVersion = this.pluginDescriptionFile.getVersion();

    private ElytraFlightListener elytraFlightListener;
    private ElytraBoostListener elytraBoostListener;
    private ElytraEquipListener elytraEquipListener;

    private MessagesHandler messagesHandler;
    private ColorHelper colorHelper;
    private MessagesHelper messagesHelper;
    private ConfigHandler configHandler;

    private DatabaseHandler databaseHandler;

    private boolean databaseConnectionSuccessful = false;


    public final void onLoad() {
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

        obj = new ColorHelper(this);
        this.colorHelper= (ColorHelper) obj;

        obj = new MessagesHandler(this.colorHelper.GetFileConfiguration());
        this.messagesHandler = (MessagesHandler) obj;

        obj = new MessagesHelper(this);
        this.messagesHelper = (MessagesHelper) obj;

        this.messagesHelper.SetDebugMode(this.configHandler.getIsDebugModeEnabled());
    }

    @Override
    public void onEnable() {
        if (!databaseConnectionSuccessful) {
            getLogger().severe("Database connection was not established. Disabling plugin...");
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
        Bukkit.getPluginManager().registerEvents(this.elytraFlightListener, this);
        Bukkit.getPluginManager().registerEvents(this.elytraBoostListener, this);
        Bukkit.getPluginManager().registerEvents(this.elytraEquipListener, this);

        new PlayerArmorListener(this);

        this.messagesHelper.sendConsoleMessage("###########################################");
        this.messagesHelper.sendConsoleMessage("&ePlugin by: &6&lCodingMaestro");
        this.messagesHelper.sendConsoleMessage("&eVersion: &6&l" + this.pluginVersion);
        this.messagesHelper.sendConsoleMessage("&ahas been loaded successfully");
        this.messagesHelper.sendConsoleMessage("###########################################");
        this.messagesHelper.SendDebugMessage("&eDeveloper debug mode enabled!");
        this.messagesHelper.SendDebugMessage("&eThis WILL fill the console");
        this.messagesHelper.SendDebugMessage("&ewith additional ElytraEssentials information!");
        this.messagesHelper.SendDebugMessage("&eThis setting is not intended for ");
        this.messagesHelper.SendDebugMessage("&econtinous use!");
    }

    @Override
    public final void onDisable() {
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
            for (Map.Entry<UUID,Integer> entry : this.elytraFlightListener.GetAllActiveFlights().entrySet()) {
                try {
                    databaseHandler.SetPlayerFlightTime(entry.getKey(), entry.getValue());
                } catch (SQLException e) {
                    this.messagesHelper.SendDebugMessage("Something went wrong while trying to set flight time");
                    throw new RuntimeException(e);
                }
            }

            databaseHandler.Disconnect();
        }

        HandlerList.unregisterAll(this);
        this.messagesHelper.sendConsoleMessage("&aAll event listeners unregistered successfully!");

        try {
            this.messagesHelper.sendConsoleMessage("&aAll background tasks disabled successfully!");
        }
        catch (Exception exception) {
            this.messagesHelper.sendConsoleMessage("&aAll background tasks disabled successfully!");
        }
        this.messagesHelper.sendConsoleMessage("&aPlugin Version &d&l" + pluginVersion);
        this.messagesHelper.sendConsoleMessage("&aPlugin shutdown successfully!");
        this.messagesHelper.sendConsoleMessage("&aGoodbye");
        this.messagesHelper.sendConsoleMessage("-------------------------------------------");
        this.elytraFlightListener = null;
        this.elytraBoostListener = null;
        this.elytraEquipListener = null;
        this.messagesHandler = null;
        this.colorHelper = null;
        this.messagesHelper = null;
        this.configHandler = null;
    }

    public final MessagesHelper getMessagesHelper() { return this.messagesHelper; }

    public final MessagesHandler getMessagesHandlerInstance() { return this.messagesHandler; }

    public final ConfigHandler getConfigHandlerInstance() { return this.configHandler; }

    public final ElytraFlightListener getElytraFlightListener() {
        return this.elytraFlightListener;
    }

    public final DatabaseHandler getDatabaseHandler() {
        return this.databaseHandler;
    }

    public final void setColorHelper(ColorHelper colorHelper) {
        this.colorHelper = colorHelper;
    }

    public final void setConfigHandler(ConfigHandler configHandler) {
        this.configHandler = configHandler;
    }

    public final void setMessagesHandler(MessagesHandler messagesHandler) {
        this.messagesHandler = messagesHandler;
    }

    public final void SetMessagesHelper(MessagesHelper messagesHelper) { this.messagesHelper = messagesHelper; }
}

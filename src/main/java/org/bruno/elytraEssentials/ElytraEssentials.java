package org.bruno.elytraEssentials;

import com.github.jewishbanana.playerarmorchangeevent.PlayerArmorListener;
import org.bruno.elytraEssentials.commands.ReloadCommand;
import org.bruno.elytraEssentials.handlers.ConfigHandler;
import org.bruno.elytraEssentials.handlers.DatabaseHandler;
import org.bruno.elytraEssentials.handlers.MessagesHandler;
import org.bruno.elytraEssentials.helpers.ColorHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.listeners.ElytraBoostListener;
import org.bruno.elytraEssentials.listeners.ElytraEquipListener;
import org.bruno.elytraEssentials.listeners.ElytraFlightListener;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
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
    private ConfigHandler configHandler;

    private DatabaseHandler databaseHandler;

    public final void onLoad() {
        this.getConfig().options().copyDefaults();
        this.saveDefaultConfig();

        Object obj = new ConfigHandler(this.getConfig());
        this.configHandler = (ConfigHandler) obj;

        obj = new DatabaseHandler(this);
        this.databaseHandler = (DatabaseHandler) obj;
        try {
            databaseHandler.Initialize();
        } catch (SQLException e) {
            //TODO: If database was not able to connect, disable plugin
            throw new RuntimeException(e);
        }

        obj = new ColorHelper(this);
        this.colorHelper= (ColorHelper) obj;

        obj = new MessagesHandler(this.colorHelper.GetFileConfiguration());
        this.messagesHandler = (MessagesHandler) obj;

        MessagesHelper.SetDebugMode(this.configHandler.getIsDebugModeEnabled());
    }

    @Override
    public void onEnable() {
        MessagesHelper.sendConsoleMessage("&a-------------------------------------------");
        MessagesHelper.sendConsoleMessage("&aDetected Version &d" + Bukkit.getVersion());
        MessagesHelper.sendConsoleMessage("&aLoading settings for Version &d" + Bukkit.getVersion());

        MessagesHelper.sendConsoleMessage("&aRegistering commands");
        this.getCommand("eereload").setExecutor((CommandExecutor)new ReloadCommand(this));

        MessagesHelper.sendConsoleMessage("&aRegistering event listeners");
        this.elytraFlightListener = new ElytraFlightListener(this);
        this.elytraBoostListener = new ElytraBoostListener(this);
        this.elytraEquipListener = new ElytraEquipListener(this);
        Bukkit.getPluginManager().registerEvents(this.elytraFlightListener, this);
        Bukkit.getPluginManager().registerEvents(this.elytraBoostListener, this);
        Bukkit.getPluginManager().registerEvents(this.elytraEquipListener, this);

        new PlayerArmorListener(this);

        MessagesHelper.sendConsoleMessage("###########################################");
        MessagesHelper.sendConsoleMessage("&ePlugin by: &6&lCodingMaestro");
        MessagesHelper.sendConsoleMessage("&eVersion: &6&l" + this.pluginVersion);
        MessagesHelper.sendConsoleMessage("&ahas been loaded successfully");
        MessagesHelper.sendConsoleMessage("###########################################");
        MessagesHelper.SendDebugMessage("&eDeveloper debug mode enabled!");
        MessagesHelper.SendDebugMessage("&eThis WILL fill the console");
        MessagesHelper.SendDebugMessage("&ewith additional ElytraEssentials information!");
        MessagesHelper.SendDebugMessage("&eThis setting is not intended for ");
        MessagesHelper.SendDebugMessage("&econtinous use!");
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
            MessagesHelper.sendConsoleLog("error", "&4\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
            MessagesHelper.sendConsoleLog("error", "&4\u2551                             WARNING                                    \u2551");
            MessagesHelper.sendConsoleLog("error", "&4\u2551      RELOADING THE SERVER WHILE ELYTRAESSENTIALS IS ENABLED MIGHT      \u2551");
            MessagesHelper.sendConsoleLog("error", "&4\u2551                    LEAD TO UNEXPECTED ERRORS!                          \u2551");
            MessagesHelper.sendConsoleLog("error", "&4\u2551                                                                        \u2551");
            MessagesHelper.sendConsoleLog("error", "&4\u2551   Please to fully restart your server if you encounter issues!         \u2551");
            MessagesHelper.sendConsoleLog("error", "&4\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
        }
        MessagesHelper.sendConsoleMessage("-------------------------------------------");
        MessagesHelper.sendConsoleMessage("&aPlugin by CodingMaestro");

        MessagesHelper.sendConsoleMessage("&aClosing database connection...");
        for (Map.Entry<UUID,Integer> entry : this.elytraFlightListener.GetAllActiveFlights().entrySet()) {
            try {
                databaseHandler.SetPlayerFlightTime(entry.getKey(), entry.getValue());
            } catch (SQLException e) {
                MessagesHelper.SendDebugMessage("Something went wrong while trying to set flight time");
                throw new RuntimeException(e);
            }
        }

        databaseHandler.Disconnect();

        HandlerList.unregisterAll(this);
        MessagesHelper.sendConsoleMessage("&aAll event listeners unregistered successfully!");

        try {
            MessagesHelper.sendConsoleMessage("&aAll background tasks disabled successfully!");
        }
        catch (Exception exception) {
            MessagesHelper.sendConsoleMessage("&aAll background tasks disabled successfully!");
        }
        MessagesHelper.sendConsoleMessage("&aPlugin Version &d&l" + pluginVersion);
        MessagesHelper.sendConsoleMessage("&aPlugin shutdown successfully!");
        MessagesHelper.sendConsoleMessage("&aGoodbye");
        MessagesHelper.sendConsoleMessage("-------------------------------------------");
        this.elytraFlightListener = null;
        this.elytraBoostListener = null;
        this.elytraEquipListener = null;
        this.messagesHandler = null;
        this.colorHelper = null;
        this.configHandler = null;
    }

    public final MessagesHandler getMessagesHandlerInstance() {
        return this.messagesHandler;
    }

    public final ConfigHandler getConfigHandlerInstance() { return this.configHandler;}

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
}



## PLEASE READ THE RESOURCE OVERVIEW BEFORE DOWNLOADING THE PLUGIN TO SEE IF THIS PLUGIN FITS YOUR NEEDS. IF YOU HAVE ANY QUESTIONS, JOIN MY [DISCORD](https://discord.com/invite/Y99qmZRVe6) SERVER AND CREATE A TICKET.

[Spigot](https://www.spigotmc.org/resources/126002/) | [Modrinth](https://modrinth.com/plugin/elytraessentials) | [Discord](https://discord.com/invite/Y99qmZRVe6) | [Wiki](https://github.com/bruno-medeiros1/elytra-essentials/wiki)

## ElytraEssentials
It's a plugin compatible with **Spigot**, **Paper**, **Purpur**, and **Folia** servers designed to enhance the Elytra flying experience in Minecraft. It transforms the vanilla elytra experience into a fully customizable and engaging system. From flight mechanics and cosmetic effects to player stats and leaderboards, this plugin gives you complete control over how players traverse your world.

## Features
- Set, manage, and recover player flight time with admin commands.
- Configure global or per-world flight speed limits.
- Disable elytra usage globally, in specific worlds, or prevent equipping them.
- Prevent player death from fall damage (broken elytra, out of time) and high-speed wall collisions.
- Boost flight speed with a configurable item, including a high-speed Super Boost.
- In-game GUI shop for players to buy cosmetic particle effects.
- Track detailed player statistics (/ee stats) with competitive leaderboards (/ee top).
- Performance saver automatically disables effects when server TPS is <18 for 10 consecutive seconds.
- Full support for both SQLite and MySQL.
- Extensive permission nodes for all commands and features.
- Placeholder API and Vault integration for stats and economy.
- Automatic hourly backups for SQLite databases, storing up to 24 copies (one full day).
- Command to restore SQLite database backups with /ee importdb command.
- Fuse any chest plate with elytra in the new /ee forge GUI, transferring all enchantments and durability.
- Armored Elytras can be fully reverted in the forge GUI, restoring the original items and their enchantments.
- Ability to check info about a worn Armored Elytra with the /ee armor command.
- Players can launch into the air from the ground by holding sneak and right-clicking with the boost item.
- Configurable combat tag system to disable elytra on damage.
- Emergency deploy will automatically equip an elytra in the player's inventory if it falls, preventing them from dying.
- Fully configurable achievements and rewards system with integrated GUI (/ee achievements)
- It supports Minecraft version 1.18.x through 1.21.x.
- Folia Support

## Showcase
Check this section of the wiki for a showcase of the plugin: [Showcase](https://github.com/bruno-medeiros1/elytra-essentials/wiki/Showcase)

## Installation Guide
Setting up ElytraEssentials is designed to be as simple as possible.
1. Download the latest release of ElytraEssentials, based on your server version, from the release section.
2. Place the downloaded .jar file into your server’s `/plugins` folder.
3. For full functionality, ensure you have these plugins installed:
   - Vault: Required for all economy features in the /ee shop.
   - PlaceholderAPI: Required for using any of the %elytraessentials% placeholders.
4. Restart your server. This will generate the default configuration files in the `plugins/ElytraEssentials/` folder.


## Update Guide
For most updates that include bug fixes and small feature additions, you do not need to reset your configuration files.
1. Stop your server.
2. Delete the old `ElytraEssentials-x.x.x.jar` file from your `/plugins` folder.
3. Place the new, downloaded .jar file into your `/plugins` folder and remove the old one.
4. Start your server. Your existing `config.yml`, `messages.yml`, `shop.yml`, and `achievements.yml` will be used automatically.

> [!WARNING]
> Always check the changelog before updating. It will state if a configuration reset is necessary. If a reset is recommended, back up your existing `plugins/ElytraEssentials` folder and delete the necessary configuration files. The database folder is not recommended to be deleted since it will erase your player's data

## Configuration
By default, the plugin is configured to work "out of the box" using a local SQLite database file, requiring no extra setup.
1. Database (Optional): If you want to use a dedicated MySQL database, stop your server and edit the config.yml file:
   - Change storage type from SQLITE to MYSQL.
   - Fill in your MySQL server details (host, port, database, username, password).
2. Customization: Open the config.yml, messages.yml, and shop.yml files to customize all features, messages, and shop items to your liking.
3. Reload: After making changes, you can use the command /ee reload to apply them without needing a full server restart.


## API for Developers
ElytraEssentials provides a simple and powerful API that allows other plugins to interact with its features. 
To use the API, you need to add ElytraEssentials as a dependency to your project. This is easily done using JitPack.

### Maven Setup
Add the JitPack repository and the ElytraEssentials dependency to your pom.xml:

```
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
```
<dependencies>
    <dependency>
        <groupId>com.github.bruno-medeiros1</groupId>
        <artifactId>ElytraEssentials</artifactId>
        <version>v1.9.1</version> <!-- Replace with the desired release tag -->
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Gradle Setup
Add the JitPack repository and the ElytraEssentials dependency to your `build.gradle`:
```
repositories {
maven { url 'https://jitpack.io' }
}

dependencies {
compileOnly 'com.github.bruno-medeiros1:ElytraEssentials:v1.9.1' // Replace with the desired release tag
}
```
> [!IMPORTANT]
> You must use <scope>provided</scope> (for Maven) or compileOnly (for Gradle). This ensures that ElytraEssentials is not bundled inside your plugin's JAR file.


### Getting the API Instance
After adding the dependency, you need to get the API instance from Bukkit's ServicesManager. It's best to do this in your plugin's onEnable() method.

```
import org.bruno.elytraEssentials.api.ElytraEssentialsAPI;
import org.bukkit.plugin.RegisteredServiceProvider;

public class YourPlugin extends JavaPlugin {

    private ElytraEssentialsAPI elytraApi;

    @Override
    public void onEnable() {
        if (!setupElytraEssentialsAPI()) {
            getLogger().severe("ElytraEssentials API not found! This plugin requires ElytraEssentials to function.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // You can now use the API!
        getLogger().info("Successfully hooked into the ElytraEssentials API.");
    }

    private boolean setupElytraEssentialsAPI() {
        RegisteredServiceProvider<ElytraEssentialsAPI> provider = getServer().getServicesManager().getRegistration(ElytraEssentialsAPI.class);
        if (provider != null) {
            this.elytraApi = provider.getProvider();
        }
        return this.elytraApi != null;
    }

    // Example of using the API
    public void checkPlayerFlightTime(Player player) {
        if (this.elytraApi == null) return;

        int flightTime = elytraApi.getFlightTime(player.getUniqueId());
        String activeEffect = elytraApi.getActiveEffectKey(player.getUniqueId());

        player.sendMessage("Your flight time is: " + flightTime + " seconds.");
        if (activeEffect != null) {
            player.sendMessage("Your active effect is: " + activeEffect);
        }
    }
}
```

## Statistics
![STATS](https://bstats.org/signatures/bukkit/elytraessentials.svg)

To guide future development and improvements, this plugin utilises bStats to gather anonymous statistics on its usage. This helps me track the total server count, assess version compatibility needs, and see which features are most valued by the community.

You can view all of this information openly on the public dashboard: https://bstats.org/plugin/bukkit/ElytraEssentials

If you prefer to disable bStats, you can manage this setting for your entire server within the plugins/bStats/config.yml configuration file.

## License
Distributed under the GNU GPL v3 License. See [LICENSE](https://github.com/bruno-medeiros1/elytra-essentials/blob/master/.github/LICENSE) for more information.

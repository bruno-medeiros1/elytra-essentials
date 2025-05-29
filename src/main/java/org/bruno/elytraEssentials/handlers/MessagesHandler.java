package org.bruno.elytraEssentials.handlers;

import org.bukkit.configuration.file.FileConfiguration;

public final class MessagesHandler {
    private final FileConfiguration fileConfiguration;
    private String prefix;
    private String reloadStart;
    private String reloadSuccess;
    private String noPermission;

    private String elytraUsageDisabled;
    private String elytraUsageWorldDisabled;
    private String elytraEquipDisabled;
    private String elytraEquipReturned;
    private String elytraEquipDropped;
    private String elytraTimeLimit;
    private String elytraBypassTimeLimit;
    private String elytraFlightTimeExpired;

    public MessagesHandler(FileConfiguration fileConfiguration) {
        this.fileConfiguration = fileConfiguration;
        SetMessages();
    }

    public final void SetMessages(){
        this.prefix = this.fileConfiguration.getString("prefix", "&6[&eElytraEssentials&6]");
        this.reloadStart = this.fileConfiguration.getString("reload-start", "&aReloading ElytraEssentials... Please wait.");
        this.reloadSuccess = this.fileConfiguration.getString("reload-success", "&aPlugin successfully reloaded! All configuration files are up to date.");
        this.noPermission = this.fileConfiguration.getString("no-permission", "&cYou lack the required permission to perform this action.");

        this.elytraUsageDisabled = this.fileConfiguration.getString("elytra-usage-disabled", "&cElytra usage is disabled on this server.");
        this.elytraUsageWorldDisabled = this.fileConfiguration.getString("elytra-usage-world-disabled", "&cElytra usage is not allowed in this world.");
        this.elytraEquipDisabled = this.fileConfiguration.getString("elytra-equip-disabled", "&cYou are not allowed to equip an elytra.");
        this.elytraEquipReturned = this.fileConfiguration.getString("elytra-equip-returned", "&&6Elytra equipping is disabled. The equipped elytra has been safely returned to your inventory.");
        this.elytraEquipDropped = this.fileConfiguration.getString("elytra-equip-dropped", "&6Elytra equipping is disabled. Your inventory is full, so the equipped elytra has been dropped on the ground.");
        this.elytraTimeLimit = this.fileConfiguration.getString("elytra-time-limit", "&eRemaining Flight Time: &6{0}s");
        this.elytraBypassTimeLimit = this.fileConfiguration.getString("elytra-bypass-time-limit", "&eYou have infinite time!");
        this.elytraFlightTimeExpired = this.fileConfiguration.getString("elytra-flight-time-expired", "&cYour elytra flight time has expired...");
    }

    public final String getPrefixMessage() { return this.prefix; }
    public final String getReloadStartMessage() { return this.reloadStart; }
    public final String getReloadSuccessMessage() { return this.reloadSuccess; }
    public final String getNoPermissionMessage() { return this.noPermission; }

    public final String getElytraUsageDisabledMessage() { return this.elytraUsageDisabled; }
    public final String getElytraUsageWorldDisabledMessage() { return this.elytraUsageWorldDisabled; }
    public final String getElytraEquipDisabledMessage() { return this.elytraEquipDisabled; }
    public final String getElytraEquipReturnedMessage() { return this.elytraEquipReturned; }
    public final String getElytraEquipDroppedMessage() { return this.elytraEquipDropped; }
    public final String getElytraTimeLimitMessage() { return this.elytraTimeLimit; }
    public final String getElytraBypassTimeLimitMessage() { return this.elytraBypassTimeLimit; }
    public final String getElytraFlightTimeExpired() { return this.elytraFlightTimeExpired; }
}

package org.bruno.elytraEssentials.handlers;

import org.bukkit.configuration.file.FileConfiguration;

public final class MessagesHandler {
    private final FileConfiguration fileConfiguration;

    private String prefix;
    private String noPermission;
    private String playerNotFound;
    private String featureNotEnabled;
    private String notEnoughXP;
    private String notEnoughMoney;

    private String reloadStart;
    private String reloadSuccess;

    private String elytraUsageDisabled;
    private String elytraUsageWorldDisabled;
    private String elytraEquipDisabled;
    private String elytraEquipDropped;

    private String flightTimeRecovery;
    private String flightTimeExpired;
    private String flightTimeAdded;
    private String flightTimeRemoved;
    private String flightTimeSet;
    private String flightTimeCleared;
    private String flightTimeBypass;
    private String flightTimeLimit;

    private String boostCooldown;

    private String speedoMeterNormal;
    private String speedoMeterBoost;
    private String speedoMeterSuperBoost;

    private String newPRLongestFlight;

    private String purchaseSuccessful;
    private String effectSelected;
    private String effectDeselected;
    private String effectGuiOwned;
    private String effectGuiPurchase;
    private String effectGuiSelect;
    private String effectGuiDeselect;

    private String forgeSuccessful;
    private String revertSuccessful;

    public MessagesHandler(FileConfiguration fileConfiguration) {
        this.fileConfiguration = fileConfiguration;
        loadMessages();
    }


    public void loadMessages(){
        // General Messages
        this.prefix = fileConfiguration.getString("prefix", "&6&lElytraEssentials &e»");
        this.noPermission = fileConfiguration.getString("no-permission", "&cYou do not have permission to perform this action.");
        this.playerNotFound = fileConfiguration.getString("player-not-found", "&cPlayer '{0}' could not be found.");
        this.featureNotEnabled = fileConfiguration.getString("feature-not-enabled", "&cThis feature is not enabled.");
        this.notEnoughXP = fileConfiguration.getString("not-enough-xp", "&cYou do not have enough experience levels.");
        this.notEnoughMoney = fileConfiguration.getString("not-enough-money", "&cYou don't have enough money.");

        // Reload Command Messages
        this.reloadStart = fileConfiguration.getString("reload-start", "&eReloading ElytraEssentials... Please wait.");
        this.reloadSuccess = fileConfiguration.getString("reload-success", "&aPlugin successfully reloaded! All configuration files are up to date.");

        // Restriction Messages
        this.elytraUsageDisabled = fileConfiguration.getString("elytra-usage-disabled", "&cElytra flight is currently disabled on this server.");
        this.elytraUsageWorldDisabled = fileConfiguration.getString("elytra-usage-world-disabled", "&cYou are not permitted to fly in this world.");
        this.elytraEquipDisabled = fileConfiguration.getString("elytra-equip-disabled", "&cEquipping elytra is disabled in this server");
        this.elytraEquipDropped = fileConfiguration.getString("elytra-equip-dropped", "&6Your inventory is full! The elytra was dropped on the ground instead.");

        // Flight Time Messages
        this.flightTimeRecovery = fileConfiguration.getString("flight-time-recovery", "&7You have recovered &e{0} &7of flight time!");
        this.flightTimeExpired = fileConfiguration.getString("flight-time-expired", "&cYour elytra flight time has expired...");
        this.flightTimeAdded = fileConfiguration.getString("flight-time-added", "&e{0} &7have been added to your flight time.");
        this.flightTimeRemoved = fileConfiguration.getString("flight-time-removed", "&e{0} &7 have been removed from your flight time.");
        this.flightTimeSet = fileConfiguration.getString("flight-time-set", "&7Your flight time has been set to &e{0}&7.");
        this.flightTimeCleared = fileConfiguration.getString("flight-time-cleared", "&cYour flight time has been cleared.");
        this.flightTimeBypass = fileConfiguration.getString("flight-time-bypass", "&eYou have unlimited time!");
        this.flightTimeLimit = fileConfiguration.getString("flight-time-limit", "&eFlight Time Left: &6{0}");

        // Boost Messages
        this.boostCooldown = fileConfiguration.getString("boost-cooldown", "&7You must wait &e{0} before boosting again.");

        //  SpeedoMeter
        this.speedoMeterNormal = fileConfiguration.getString("speedometer-normal", "&eSpeed: {0}{1} &ekm/h");
        this.speedoMeterBoost = fileConfiguration.getString("speedometer-boost", "&a&l+ &eSpeed: {0}{1} &ekm/h &a&l+");
        this.speedoMeterSuperBoost = fileConfiguration.getString("speedometer-super-boost", "&c&l++ &eSpeed: {0}{1} &ekm/h &c&l++");

        // Stats & Records
        this.newPRLongestFlight = fileConfiguration.getString("longest-flight-pr", "&6&lNew Record! &fYour new longest flight: &e{0} blocks!");

        // Shop & Effects Messages
        this.purchaseSuccessful = fileConfiguration.getString("purchase-successful", "&aYou have successfully purchased the {0} effect!");
        this.effectSelected = fileConfiguration.getString("effect-selected", "&7You have equipped the {0} &7effect.");
        this.effectDeselected = fileConfiguration.getString("effect-deselected", "&7You have cleared the {0} &7effect.");
        this.effectGuiOwned = fileConfiguration.getString("effect-gui-owned", "&cYou already own this effect!");
        this.effectGuiPurchase = fileConfiguration.getString("effect-gui-purchase", "§aLeft Click: Select Effect");
        this.effectGuiSelect = fileConfiguration.getString("effect-gui-select", "&aLeft Click: Select Effect");
        this.effectGuiDeselect = fileConfiguration.getString("effect-gui-deselect", "&cRight Click: Clear Effect");

        //  Forge
        this.forgeSuccessful = fileConfiguration.getString("forge-successful", "&aYou have successfully forged an Armored Elytra!");
        this.revertSuccessful = fileConfiguration.getString("revert-successful", "&aYou have successfully reverted your Armored Elytra.");
    }

    // General Messages
    public String getPrefixMessage() { return this.prefix; }
    public String getNoPermissionMessage() { return this.noPermission; }
    public String getPlayerNotFound() { return playerNotFound; }
    public String getFeatureNotEnabled() { return this.featureNotEnabled; }
    public String getNotEnoughXP() { return this.notEnoughXP; }
    public String getNotEnoughMoney() { return notEnoughMoney; }

    // Reload Command Messages
    public String getReloadStartMessage() { return this.reloadStart; }
    public String getReloadSuccessMessage() { return this.reloadSuccess; }

    // Restriction Messages
    public String getElytraUsageDisabledMessage() { return this.elytraUsageDisabled; }
    public String getElytraUsageWorldDisabledMessage() { return this.elytraUsageWorldDisabled; }
    public String getElytraEquipDisabled() { return this.elytraEquipDisabled; }
    public String getElytraEquipDropped() { return this.elytraEquipDropped; }

    // Flight Time Messages
    public String getElytraFlightTimeAdded() { return this.flightTimeAdded; }
    public String getElytraFlightTimeBypass() { return this.flightTimeBypass; }
    public String getElytraFlightTimeCleared() { return this.flightTimeCleared; }
    public String getElytraFlightTimeExpired() { return this.flightTimeExpired; }
    public String getElytraFlightTimeRecovery() { return this.flightTimeRecovery; }
    public String getElytraFlightTimeRemoved() { return this.flightTimeRemoved; }
    public String getElytraFlightTimeSet() { return this.flightTimeSet; }
    public String getElytraTimeLimitMessage() { return this.flightTimeLimit; }

    // Boost Messages
    public String getBoostCooldown() { return this.boostCooldown; }

    //  SpeedoMeter
    public String getSpeedoMeterNormal() { return this.speedoMeterNormal; }
    public String getSpeedoMeterBoost() { return this.speedoMeterBoost; }
    public String getSpeedoMeterSuperBoost() { return this.speedoMeterSuperBoost; }

    // Stats & Records
    public String getNewPRLongestFlightMessage() { return this.newPRLongestFlight; }

    // Shop & Effects Messages
    public String getEffectGuiOwned() { return effectGuiOwned; }
    public String getEffectGuiPurchase() { return effectGuiPurchase; }
    public String getEffectDeselected() { return effectDeselected; }
    public String getEffectSelected() { return effectSelected; }
    public String getEffectGuiDeselect() { return effectGuiDeselect; }
    public String getEffectGuiSelect() { return effectGuiSelect; }
    public String getPurchaseSuccessful() { return purchaseSuccessful; }

    //  Forge
    public String getForgeSuccessful() { return forgeSuccessful; }
    public String getRevertSuccessful() { return revertSuccessful; }

}

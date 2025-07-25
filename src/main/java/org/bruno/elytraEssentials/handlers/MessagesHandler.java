package org.bruno.elytraEssentials.handlers;

import org.bukkit.configuration.file.FileConfiguration;

public final class MessagesHandler {
    private FileConfiguration fileConfiguration;

    private String prefix;
    private String noPermission;
    private String playerNotFound;
    private String featureNotEnabled;
    private String notEnoughXP;
    private String notEnoughMoney;
    private String fallProtectionEnabled;
    private String fireworkBoostDisabled;
    private String riptideLaunchDisabled;

    private String reloadStart;
    private String reloadSuccess;

    private String elytraUsageDisabled;
    private String elytraUsageWorldDisabled;
    private String elytraEquipDisabled;
    private String elytraEquipDropped;

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

    // Shop & Effects Messages
    private String purchaseSuccessful;
    private String effectSelected;
    private String effectDeselected;
    private String effectGuiOwned;
    private String effectGuiPurchase;
    private String effectGuiSelect;
    private String effectGuiDeselect;
    private String noActiveEffectToClear;
    public String giveEffectSuccess;

    // Armored Elytra Messages
    private String notWearingArmoredElytra;
    private String notWearingArmoredElytraRepair;
    private String armoredElytraRepairSuccess;
    private String armoredElytraAlreadyRepaired;
    private String armoredElytraBroken;

    // Forge Messages
    private String forgeSuccessful;
    private String revertSuccessful;

    // Emergency Deploy Messages
    private String emergencyDeploySuccess;

    // Achievements Messages
    private String achievementUnlocked;

    // Combat Tag Messages
    private String cannotGlideCombatTagged;
    private String combatTagged;
    private String combatTaggedExpired;

    // Tandem Flight Messages
    private String driverTandemFlightFailed;
    private String driverTandemFlightCountdown;
    private String driverMountedSuccess;
    private String driverVoluntaryDismount;
    private String driverTandemInvitationAccepted;
    private String driverAlreadyHasPassenger;
    private String driverInvitationExpired;
    private String driverInvitationSent;
    private String passengerTandemFlightFailed;
    private String passengerMountedSuccess;
    private String passengerVoluntaryDismount;
    private String passengerTandemInvitationAccepted;
    private String passengerInvitationExpired;
    private String dismountCountdown;
    private String mountingCountdown;
    private String driverNotAvailable;
    private String noPendingInvitation;

    public MessagesHandler(FileConfiguration fileConfiguration) {
        this.fileConfiguration = fileConfiguration;
        loadMessages();
    }

    /**
     * Reloads all message strings from a new FileConfiguration object.
     * This is called from the main plugin's reload sequence.
     * @param newFileConfiguration The newly reloaded messages.yml config object.
     */
    public void reload(FileConfiguration newFileConfiguration) {
        this.fileConfiguration = newFileConfiguration;
        loadMessages();
    }

    public void loadMessages(){
        // General Messages
        this.prefix = fileConfiguration.getString("prefix", "&#FFD700ElytraEssentials &e» ");
        this.noPermission = fileConfiguration.getString("no-permission", "&cYou do not have permission to perform this action.");
        this.playerNotFound = fileConfiguration.getString("player-not-found", "&cPlayer '{0}' could not be found.");
        this.featureNotEnabled = fileConfiguration.getString("feature-not-enabled", "&cThis feature is not enabled.");
        this.notEnoughXP = fileConfiguration.getString("not-enough-xp", "&cYou do not have enough experience levels.");
        this.notEnoughMoney = fileConfiguration.getString("not-enough-money", "&cYou don't have enough money.");
        this.fallProtectionEnabled = fileConfiguration.getString("fall-protection-enabled", "&fFall Protection: &a&lEnabled");
        this.fireworkBoostDisabled = fileConfiguration.getString("firework-boost-disabled", "&cFirework boosting is disabled.");
        this.riptideLaunchDisabled = fileConfiguration.getString("riptide-launch-disabled", "&cRiptide launching is disabled.");

        // Reload Command Messages
        this.reloadStart = fileConfiguration.getString("reload-start", "&eReloading ElytraEssentials... Please wait.");
        this.reloadSuccess = fileConfiguration.getString("reload-success", "&aPlugin successfully reloaded! All configuration files are up to date.");

        // Restriction Messages
        this.elytraUsageDisabled = fileConfiguration.getString("elytra-usage-disabled", "&cElytra flight is currently disabled on this server.");
        this.elytraUsageWorldDisabled = fileConfiguration.getString("elytra-usage-world-disabled", "&cYou are not permitted to fly in this world.");
        this.elytraEquipDisabled = fileConfiguration.getString("elytra-equip-disabled", "&cEquipping elytra is disabled in this server");
        this.elytraEquipDropped = fileConfiguration.getString("elytra-equip-dropped", "&6Your inventory is full! The elytra was dropped on the ground instead.");

        // Flight Time Messages
        this.flightTimeExpired = fileConfiguration.getString("flight-time-expired", "&cYour elytra flight time has expired...");
        this.flightTimeAdded = fileConfiguration.getString("flight-time-added", "&e{0} &7have been added to your flight time.");
        this.flightTimeRemoved = fileConfiguration.getString("flight-time-removed", "&e{0} &7 have been removed from your flight time.");
        this.flightTimeSet = fileConfiguration.getString("flight-time-set", "&7Your flight time has been set to &e{0}&7.");
        this.flightTimeCleared = fileConfiguration.getString("flight-time-cleared", "&cYour flight time has been cleared.");
        this.flightTimeBypass = fileConfiguration.getString("flight-time-bypass", "&eYou have unlimited time!");
        this.flightTimeLimit = fileConfiguration.getString("flight-time-limit", "&eFlight Time Left: &6{0}");

        // Boost Messages
        this.boostCooldown = fileConfiguration.getString("boost-cooldown", "&7You must wait &e{0}s &7before boosting again.");

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
        this.noActiveEffectToClear = fileConfiguration.getString("no-active-effect-to-clear", "&cYou do not have an active effect to clear.");
        this.giveEffectSuccess = fileConfiguration.getString("give-effect-success", "&aYou have successfully given the {0} effect to {1}.");

        // Forge
        this.forgeSuccessful = fileConfiguration.getString("forge-successful", "&aYou have successfully forged an Armored Elytra!");
        this.revertSuccessful = fileConfiguration.getString("revert-successful", "&aYou have successfully reverted your Armored Elytra.");

        // Armored Elytra
        this.notWearingArmoredElytra = fileConfiguration.getString("not-wearing-armored-elytra", "&cYou are not currently wearing an Armored Elytra.");
        this.notWearingArmoredElytraRepair = fileConfiguration.getString("not-wearing-armored-elytra-repair", "&eYou must be wearing an Armored Elytra to repair it.");
        this.armoredElytraRepairSuccess = fileConfiguration.getString("armored-elytra-repair-success", "&aYour Armored Elytra's has been fully repaired!");
        this.armoredElytraAlreadyRepaired = fileConfiguration.getString("armored-elytra-already-repaired", "&cYour Armored Elytra's is already fully repaired!");
        this.armoredElytraBroken = fileConfiguration.getString("armored-elytra-broken", "&cYour Armored Elytra's plating has shattered!");

        //  Emergency Deploy
        this.emergencyDeploySuccess = fileConfiguration.getString("emergency-deploy-success", "&eElytra Auto-Deployed!");

        // Achievements
        this.achievementUnlocked = fileConfiguration.getString("achievement-unlocked", "&eYou have completed the &6{0} &eachievement!");

        // Combat Tag
        this.cannotGlideCombatTagged = fileConfiguration.getString("cannot-glide-combat-tagged", "&cYou cannot glide while in combat!");
        this.combatTagged = fileConfiguration.getString("combat-tagged", "&cCombat Tagged! Time Left: &6{0}");
        this.combatTaggedExpired = fileConfiguration.getString("combat-tagged-expired", "&cYour combat tag has expired.");

        // Tandem Flight Messages
        this.driverTandemFlightFailed = fileConfiguration.getString("driver-tandem-flight-failed", "&cYou failed to take off in time. Your passenger has been dismounted!");
        this.driverTandemFlightCountdown = fileConfiguration.getString("driver-tandem-flight-countdown", "&eYou have {0} seconds to take off!");
        this.driverMountedSuccess = fileConfiguration.getString("driver-mounted-success", "&a{0} has mounted onto you!");
        this.driverVoluntaryDismount = fileConfiguration.getString("driver-voluntary-dismount", "&e{0} has dismounted.");
        this.driverTandemInvitationAccepted = fileConfiguration.getString("driver-tandem-invitation-accepted", "&a{0} accepted! Mounting in {1} seconds...");
        this.driverAlreadyHasPassenger = fileConfiguration.getString("driver-already-has-passenger", "&cYou already have a passenger.");
        this.driverInvitationExpired = fileConfiguration.getString("driver-invitation-expired", "&cYour tandem flight invitation to {0} has expired.");
        this.driverInvitationSent = fileConfiguration.getString("driver-invitation-sent", "&aYou have invited {0} to a tandem flight!");
        this.driverNotAvailable = fileConfiguration.getString("driver-not-available", "&cThe player who invited you is no longer available.");
        this.passengerTandemFlightFailed = fileConfiguration.getString("passenger-tandem-flight-failed", "&cYour driver failed to take off in time.");
        this.passengerMountedSuccess = fileConfiguration.getString("passenger-mounted-success", "&aYou have mounted onto {0}!");
        this.passengerVoluntaryDismount = fileConfiguration.getString("passenger-voluntary-dismount", "&eYou have dismounted.");
        this.passengerTandemInvitationAccepted = fileConfiguration.getString("passenger-tandem-invitation-accepted", "&aAccepted! Mounting in {0} seconds...");
        this.passengerInvitationExpired = fileConfiguration.getString("passenger-invitation-expired", "&cYour tandem flight invitation from {0} has expired.");
        this.dismountCountdown = fileConfiguration.getString("dismount-countdown", "&cDismounting in &6{0}&c...");
        this.mountingCountdown = fileConfiguration.getString("mounting-countdown", "&eMounting in &6{0}&e...");
        this.noPendingInvitation = fileConfiguration.getString("no-pending-invitation", "&cYou don't have a pending tandem flight invitation.");
    }

    // General Messages
    public String getPrefixMessage() { return this.prefix; }
    public String getNoPermissionMessage() { return this.noPermission; }
    public String getPlayerNotFound() { return playerNotFound; }
    public String getFeatureNotEnabled() { return this.featureNotEnabled; }
    public String getNotEnoughXP() { return this.notEnoughXP; }
    public String getNotEnoughMoney() { return notEnoughMoney; }
    public String getFallProtectionEnabled() { return this.fallProtectionEnabled; }
    public String getFireworkBoostDisabled() { return this.fireworkBoostDisabled; }
    public String getRiptideLaunchDisabled() { return this.riptideLaunchDisabled; }

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
    public String getNoActiveEffectToClear() { return noActiveEffectToClear; }
    public String getGiveEffectSuccess() { return giveEffectSuccess; }

    //  Forge
    public String getForgeSuccessful() { return forgeSuccessful; }
    public String getRevertSuccessful() { return revertSuccessful; }

    // Armored Elytra
    public String getNotWearingArmoredElytra() { return notWearingArmoredElytra; }
    public String getNotWearingArmoredElytraRepair() { return notWearingArmoredElytraRepair; }
    public String getArmoredElytraRepairSuccess() { return armoredElytraRepairSuccess; }
    public String getArmoredElytraAlreadyRepaired() { return armoredElytraAlreadyRepaired; }
    public String getArmoredElytraBroken() { return armoredElytraBroken; }

    //  Emergency Deploy
    public String getEmergencyDeploySuccess() { return emergencyDeploySuccess; }

    //  Achievements
    public String getAchievementUnlockedMessage() { return achievementUnlocked; }

    // Combat Tag
    public String getCannotGlideCombatTagged() { return cannotGlideCombatTagged; }
    public String getCombatTagged() { return combatTagged; }
    public String getCombatTaggedExpired() { return combatTaggedExpired; }

    // Tandem Flight
    public String getDriverTandemFlightFailed() { return driverTandemFlightFailed; }
    public String getDriverTandemFlightCountdown() { return driverTandemFlightCountdown; }
    public String getDriverMountedSuccess() { return driverMountedSuccess; }
    public String getDriverVoluntaryDismount() { return driverVoluntaryDismount; }
    public String getDriverTandemInvitationAccepted() { return driverTandemInvitationAccepted; }
    public String getDriverAlreadyHasPassenger() { return driverAlreadyHasPassenger; }
    public String getDriverInvitationExpired() { return driverInvitationExpired; }
    public String getDriverInvitationSent() { return driverInvitationSent; }
    public String getDriverNotAvailable() { return driverNotAvailable; }
    public String getPassengerTandemFlightFailed() { return passengerTandemFlightFailed; }
    public String getPassengerMountedSuccess() { return passengerMountedSuccess; }
    public String getPassengerVoluntaryDismount() { return passengerVoluntaryDismount; }
    public String getPassengerTandemInvitationAccepted() { return passengerTandemInvitationAccepted; }
    public String getPassengerInvitationExpired() { return passengerInvitationExpired; }
    public String getDismountCountdown() { return dismountCountdown; }
    public String getMountingCountdown() { return mountingCountdown; }
    public String getNoPendingInvitation() { return noPendingInvitation; }
}

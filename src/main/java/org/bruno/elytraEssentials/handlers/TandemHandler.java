package org.bruno.elytraEssentials.handlers;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bruno.elytraEssentials.helpers.FoliaHelper;
import org.bruno.elytraEssentials.helpers.MessagesHelper;
import org.bruno.elytraEssentials.helpers.PermissionsHelper;
import org.bruno.elytraEssentials.utils.CancellableTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TandemHandler {
    public static final int DISMOUNT_COUNTDOWN_SECONDS = 5;

    private final ConfigHandler configHandler;
    private final MessagesHelper messagesHelper;
    private final FoliaHelper foliaHelper;
    private final FlightHandler flightHandler;
    private final MessagesHandler messagesHandler;

    // These maps track the state of the tandem system
    private final Map<UUID, UUID> activeTandems = new ConcurrentHashMap<>(); // Passenger -> Driver
    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>(); // Invitee -> Inviter
    private final Map<UUID, CancellableTask> inviteExpirations = new ConcurrentHashMap<>();
    private final Map<UUID, CancellableTask> takeoffTimers = new ConcurrentHashMap<>();
    private final Map<UUID, CancellableTask> dismountTimers = new ConcurrentHashMap<>(); // Passenger -> Dismount Timer
    private final Map<UUID, CancellableTask> mountTimers = new ConcurrentHashMap<>(); // Passenger -> Mount Timer

    private CancellableTask passengerUpdateTask;

    public TandemHandler(ConfigHandler configHandler, MessagesHelper messagesHelper, FoliaHelper foliaHelper, FlightHandler flightHandler, MessagesHandler messagesHandler) {
        this.configHandler = configHandler;
        this.messagesHelper = messagesHelper;
        this.foliaHelper = foliaHelper;
        this.flightHandler = flightHandler;
        this.messagesHandler = messagesHandler;
    }

    public void start() {
        if (!configHandler.getIsTandemFlightEnabled()) return;

        // This task runs every tick to ensure passengers stay mounted correctly.
        this.passengerUpdateTask = foliaHelper.runTaskTimerGlobal(this::updatePassengers, 1L, 1L);
    }

    public void shutdown() {
        if (passengerUpdateTask != null) {
            passengerUpdateTask.cancel();
            passengerUpdateTask = null;
        }
    }

    /**
     * The core repeating task that keeps passengers mounted and handles dismounting conditions.
     */
    private void updatePassengers() {
        for (Map.Entry<UUID, UUID> entry : activeTandems.entrySet()) {
            Player passenger = Bukkit.getPlayer(entry.getKey());
            Player driver = Bukkit.getPlayer(entry.getValue());

            if (passenger == null || driver == null) {
                dismountPassenger(passenger, driver, false);
                continue;
            }

            if (driver.isGliding()) {
                // If driver is flying, cancel any pending takeoff or dismount timers.
                cancelAndRemoveTask(takeoffTimers, passenger.getUniqueId());
                cancelAndRemoveTask(dismountTimers, passenger.getUniqueId());
            } else if (!takeoffTimers.containsKey(passenger.getUniqueId())) {
                // If driver is not gliding and not in the takeoff window, start the dismount process.
                startDismountCountdown(passenger, driver);
                continue;
            }

            if (driver.getPassengers().isEmpty() || !driver.getPassengers().contains(passenger)) {
                driver.addPassenger(passenger);
            }

            // Consume extra flight time from the driver only once per second and if flying
            if (driver.isGliding() && Bukkit.getCurrentTick() % 20 == 0 && configHandler.getIsTimeLimitEnabled() && !PermissionsHelper.playerBypassTimeLimit(driver)) {
                double costMultiplier = configHandler.getTandemFlightTimeCostMultiplier();
                if (costMultiplier > 1.0) {
                    int extraSecondsToConsume = (int) Math.round(costMultiplier - 1.0);
                    if (extraSecondsToConsume > 0) {
                        flightHandler.removeFlightTime(driver.getUniqueId(), extraSecondsToConsume, null);
                    }
                }
            }
        }
    }

    /**
     * Initiates a tandem flight invitation from a driver to a potential passenger.
     */
    public void invitePlayer(Player driver, Player invitee) {
        // Validation checks
        if (activeTandems.containsValue(driver.getUniqueId())) {
            messagesHelper.sendPlayerMessage(driver, messagesHandler.getDriverAlreadyHasPassenger());
            return;
        }

        if (activeTandems.containsKey(invitee.getUniqueId()) || pendingInvites.containsKey(invitee.getUniqueId())) {
            messagesHelper.sendPlayerMessage(driver, "&c" + invitee.getName() + " is already busy or has a pending invite.");
            return;
        }

        // TODO (b.med): Review if this check is necessary
        ItemStack inviteeChestplate = invitee.getInventory().getChestplate();
        if (inviteeChestplate != null && inviteeChestplate.getType() == Material.ELYTRA) {
            messagesHelper.sendPlayerMessage(driver, "&c" + invitee.getName() + " cannot be invited as they are wearing an elytra.");
            return;
        }

        pendingInvites.put(invitee.getUniqueId(), driver.getUniqueId());

        // Schedule the invitation to expire
        int timeout = configHandler.getTandemInviteTimeout();
        CancellableTask expirationTask = foliaHelper.runTaskLater(invitee, () -> {
            if (pendingInvites.remove(invitee.getUniqueId()) != null) {
                messagesHelper.sendPlayerMessage(invitee,  messagesHandler.getPassengerInvitationExpired().replace("{0}", driver.getName()));
                messagesHelper.sendPlayerMessage(driver, messagesHandler.getDriverInvitationExpired().replace("{0}", invitee.getName()));
            }
        }, (long) timeout * 20L);
        inviteExpirations.put(invitee.getUniqueId(), expirationTask);

        // Send invitation messages
        messagesHelper.sendPlayerMessage(driver, messagesHandler.getDriverInvitationSent().replace("{0}", invitee.getName()));

        TextComponent message = new TextComponent(("§e" + driver.getName() + " §7has invited you to a tandem flight! "));
        TextComponent commandComponent = new TextComponent("§a§l[ACCEPT]");
        commandComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ee tandem accept"));
        commandComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click here to accept the invitation.")));
        message.addExtra(commandComponent);
        invitee.spigot().sendMessage(message);
    }

    /**
     * Allows a player to accept a pending tandem flight invitation.
     */
    public void acceptInvite(Player passenger) {
        UUID driverUUID = pendingInvites.remove(passenger.getUniqueId());
        if (driverUUID == null) {
            messagesHelper.sendPlayerMessage(passenger,  messagesHandler.getNoPendingInvitation());
            return;
        }

        cancelAndRemoveTask(inviteExpirations, passenger.getUniqueId());

        Player driver = Bukkit.getPlayer(driverUUID);
        if (driver == null || !driver.isOnline()) {
            messagesHelper.sendPlayerMessage(passenger,  messagesHandler.getDriverNotAvailable());
            return;
        }

        int countdownSeconds = configHandler.getTandemMountCountdown();
        if (countdownSeconds <= 0) {
            // If countdown is disabled, mount instantly.
            mountPlayer(passenger, driver);
        } else {
            // Start the countdown.
            messagesHelper.sendPlayerMessage(passenger, messagesHandler.getPassengerTandemInvitationAccepted().replace("{0}", String.valueOf(countdownSeconds)));
            messagesHelper.sendPlayerMessage(driver, messagesHandler.getDriverTandemInvitationAccepted().replace("{0}", passenger.getName()).replace("{1}",String.valueOf(countdownSeconds)));

            final AtomicInteger countdown = new AtomicInteger(countdownSeconds);
            CancellableTask mountTask = foliaHelper.runTaskTimerGlobal(() -> {
                Player currentPassenger = Bukkit.getPlayer(passenger.getUniqueId());
                Player currentDriver = Bukkit.getPlayer(driver.getUniqueId());

                if (currentPassenger == null || currentDriver == null) {
                    cancelAndRemoveTask(mountTimers, passenger.getUniqueId());
                    return;
                }

                int remaining = countdown.getAndDecrement();
                if (remaining > 0) {
                    messagesHelper.sendPlayerMessage(currentPassenger, messagesHandler.getMountingCountdown().replace("{0}", String.valueOf(remaining)));
                    messagesHelper.sendPlayerMessage(currentDriver, messagesHandler.getMountingCountdown().replace("{0}", String.valueOf(remaining)));
                } else {
                    mountPlayer(currentPassenger, currentDriver);
                    cancelAndRemoveTask(mountTimers, passenger.getUniqueId());
                }
            }, 0L, 20L);
            mountTimers.put(passenger.getUniqueId(), mountTask);
        }
    }

    /**
     * Dismounts a passenger from their driver.
     */
    public void dismountPassenger(Player passenger, Player driver, boolean voluntary) {
        if (passenger == null) return;

        // Cancel any pending takeoff timer
        CancellableTask takeoffTask = takeoffTimers.remove(passenger.getUniqueId());
        if (takeoffTask != null) {
            takeoffTask.cancel();
        }

        UUID driverUUID = activeTandems.remove(passenger.getUniqueId());
        if (driverUUID == null) return;

        if (driver == null) {
            driver = Bukkit.getPlayer(driverUUID);
        }

        if (driver != null && driver.isOnline()) {
            driver.removePassenger(passenger);
            if (voluntary) {
                messagesHelper.sendPlayerMessage(driver, messagesHandler.getDriverVoluntaryDismount().replace("{0}", passenger.getName()));
            }
        }

        if (voluntary) {
            messagesHelper.sendPlayerMessage(passenger, messagesHandler.getPassengerVoluntaryDismount());
        }

        // Protect from fall damage if dismounted mid-air
        if (configHandler.getTandemFallDamageProtection() && !passenger.isOnGround()) {
            flightHandler.protectPlayerFromFall(passenger);
        }
    }

    public boolean isPassenger(Player player) {
        return activeTandems.containsKey(player.getUniqueId());
    }

    /**
     * Cleans up player data when they quit the server to prevent memory leaks.
     */
    public void clearPlayerData(Player player) {
        // If the player was a passenger, dismount them
        dismountPassenger(player, null, false);

        // If the player was a driver, dismount their passenger
        if (activeTandems.containsValue(player.getUniqueId())) {
            activeTandems.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(player.getUniqueId()))
                    .findFirst()
                    .ifPresent(entry -> dismountPassenger(Bukkit.getPlayer(entry.getKey()), player, false));
        }

        // Clean up any pending invites
        pendingInvites.remove(player.getUniqueId());
        CancellableTask task = inviteExpirations.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    private void startDismountCountdown(Player passenger, Player driver) {
        // If a countdown is already running, do nothing.
        if (dismountTimers.containsKey(passenger.getUniqueId())) return;

        final AtomicInteger countdown = new AtomicInteger(DISMOUNT_COUNTDOWN_SECONDS);
        CancellableTask task = foliaHelper.runTaskTimerGlobal(() -> {
            int remaining = countdown.getAndDecrement();
            if (remaining > 0) {
                String message = messagesHandler.getDismountCountdown().replace("{0}", String.valueOf(remaining));
                messagesHelper.sendPlayerMessage(passenger, message);
                messagesHelper.sendPlayerMessage(driver, message);
            } else {
                // Countdown finished, perform the dismount.
                dismountPassenger(passenger, driver, false);
                cancelAndRemoveTask(dismountTimers, passenger.getUniqueId());
            }
        }, 0L, 20L);

        dismountTimers.put(passenger.getUniqueId(), task);
    }

    private void cancelAndRemoveTask(Map<UUID, CancellableTask> map, UUID key) {
        CancellableTask task = map.remove(key);
        if (task != null) {
            task.cancel();
        }
    }

    private void mountPlayer(Player passenger, Player driver) {
        activeTandems.put(passenger.getUniqueId(), driver.getUniqueId());
        driver.addPassenger(passenger);
        messagesHelper.sendPlayerMessage(passenger, messagesHandler.getPassengerMountedSuccess().replace("{0}", driver.getName()));
        messagesHelper.sendPlayerMessage(driver, messagesHandler.getDriverMountedSuccess().replace("{0}", passenger.getName()));

        // Start the 10-second takeoff timer if the driver is on the ground
        if (!driver.isGliding()) {
            messagesHelper.sendPlayerMessage(driver, messagesHandler.getDriverTandemFlightCountdown().replace("{0}", "10"));

            //  if after 10 seconds the driver is still not gliding, dismount the passenger
            CancellableTask takeoffTask = foliaHelper.runTaskLater(driver, () -> {
                if (activeTandems.containsKey(passenger.getUniqueId()) && !driver.isGliding()) {
                    messagesHelper.sendPlayerMessage(driver, messagesHandler.getDriverTandemFlightFailed());
                    messagesHelper.sendPlayerMessage(passenger, messagesHandler.getPassengerTandemFlightFailed());
                    dismountPassenger(passenger, driver, false);
                }
                takeoffTimers.remove(passenger.getUniqueId());
            }, 200L);
            takeoffTimers.put(passenger.getUniqueId(), takeoffTask);
        }
    }
}

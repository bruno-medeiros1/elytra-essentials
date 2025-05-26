package org.bruno.elytraEssentials.handlers;

import org.bukkit.configuration.file.FileConfiguration;

public final class MessagesHandler {
    private final FileConfiguration fileConfiguration;
    private String prefixMessage;
    private String noPermissionMessage;
    private String reloadBeginMessage;
    private String reloadSuccessMessage;
    private String elytraDisabledMessage;
    private String elytraWorldDisabledMessage;
    private String equipElytraDisabledMessage;
    private String elytraRemovedToInventoryMessage;
    private String elytraRemovedToFloorMessage;

    public MessagesHandler(FileConfiguration fileConfiguration) {
        this.fileConfiguration = fileConfiguration;
        SetMessages();
    }

    public final void SetMessages(){
        this.prefixMessage = this.fileConfiguration.getString("prefix", "&6[&e&lElytraEssentials&6]");
        this.noPermissionMessage = this.fileConfiguration.getString("no-permission", "&4You do not have permission to execute this command!");
        this.reloadBeginMessage = this.fileConfiguration.getString("reload-begin", "&aBeginning plugin reload...");
        this.reloadSuccessMessage = this.fileConfiguration.getString("reload-successful", "&aConfiguration files have been successfully reloaded!");
        this.elytraDisabledMessage = this.fileConfiguration.getString("elytra-disabled-warning", "&cYou are not allowed to use elytra in this server");
        this.elytraWorldDisabledMessage = this.fileConfiguration.getString("elytra-world-disabled-warning", "&cYou are not allowed to use elytra in this world");
        this.equipElytraDisabledMessage = this.fileConfiguration.getString("equip-elytra-disabled", "&cYou are not allowed to equip an elytra");
        this.elytraRemovedToInventoryMessage = this.fileConfiguration.getString("elytra-returned-to-inventory", "&6The server disabled equipping elytra. Returned equipped one to your inventory...");
        this.elytraRemovedToFloorMessage = this.fileConfiguration.getString("elytra-dropped", "&6The server disabled equipping elytra. Since your inventory is full, dropping equipped one...");
    }

    public final String getPrefixMessage() {
        return prefixMessage;
    }

    public final String getNoPermissionMessage() {
        return noPermissionMessage;
    }

    public final String getReloadBeginMessage() {
        return reloadBeginMessage;
    }

    public final String getReloadSuccessMessage() {
        return reloadSuccessMessage;
    }

    public final String getElytraDisabledMessage() {
        return elytraDisabledMessage;
    }

    public final String getElytraWorldDisabledMessage() {
        return elytraWorldDisabledMessage;
    }

    public final String getEquipElytraDisabledMessage() {
        return equipElytraDisabledMessage;
    }

    public final String getElytraRemovedToInventoryMessage(){
        return elytraRemovedToInventoryMessage;
    }

    public final String getElytraRemovedToFloorMessage(){
        return elytraRemovedToFloorMessage;
    }
}

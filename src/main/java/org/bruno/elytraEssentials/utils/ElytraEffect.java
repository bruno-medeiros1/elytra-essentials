package org.bruno.elytraEssentials.utils;

import org.bukkit.Material;
import org.bukkit.Particle;

import java.util.List;

public class ElytraEffect {
    private String key;
    private String name;
    private final Material displayMaterial;
    private final Particle particle;
    private List<String> lore;
    private double price;
    private String permission;
    private boolean isActive;

    public ElytraEffect(String Key, String name, Material displayMaterial, Particle particle,
                        List<String> lore, double price, String permission) {
        this.key = Key;
        this.name = name;
        this.displayMaterial = displayMaterial;
        this.particle = particle;
        this.lore = lore;
        this.price = price;
        this.permission = permission;
        this.isActive = false;
    }

    public ElytraEffect(ElytraEffect templateEffect) {
        this.key = templateEffect.key;
        this.name = templateEffect.name;
        this.displayMaterial = templateEffect.displayMaterial;
        this.particle = templateEffect.particle;
        this.lore = templateEffect.lore;
        this.price = templateEffect.price;
        this.permission = templateEffect.permission;
        this.isActive = templateEffect.isActive;
    }

    public String getKey() { return key; }

    public String getName() {
        return name;
    }

    public void setName(String name) { this.name = name; }

    public Material getDisplayMaterial() {
        return displayMaterial;
    }

    public List<String> getLore() {
        return lore;
    }

    public void setLore(List<String> lore) { this.lore = lore; }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) { this.price = price; }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) { this.permission = permission; }

    public Boolean getIsActive(){
        return isActive;
    }

    public Particle getParticle(){
        return particle;
    }

    public void setIsActive(Boolean isActive){
        this.isActive = isActive;
    }
}
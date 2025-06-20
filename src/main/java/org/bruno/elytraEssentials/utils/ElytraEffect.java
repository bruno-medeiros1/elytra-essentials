package org.bruno.elytraEssentials.utils;

import org.bukkit.Material;
import org.bukkit.Particle;

import java.util.List;

public class ElytraEffect {
    private String name;
    private final Material displayMaterial;
    private final Particle particle;
    private List<String> lore;
    private double price;
    private String permission;
    private boolean isActive;

    public ElytraEffect(String name, Material displayMaterial, Particle particle,
                        List<String> lore, double price, String permission) {
        this.name = name;
        this.displayMaterial = displayMaterial;
        this.particle = particle;
        this.lore = lore;
        this.price = price;
        this.permission = permission;
        this.isActive = false;
    }

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
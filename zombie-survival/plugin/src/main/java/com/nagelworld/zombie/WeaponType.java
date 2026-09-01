package com.nagelworld.zombie;

import org.bukkit.Material;

enum WeaponType {
    PISTOL("pistol", "Pistola 9mm", Material.CARROT_ON_A_STICK, "light_ammo", 12, 7.0, 38.0, 260, 1500, 1, 0.0),
    SHOTGUN("shotgun", "Escopeta", Material.CARROT_ON_A_STICK, "shell", 6, 4.5, 24.0, 850, 2400, 8, 0.10),
    RIFLE("rifle", "Rifle de Assalto", Material.CARROT_ON_A_STICK, "rifle_ammo", 30, 6.0, 65.0, 125, 2100, 1, 0.012),
    SNIPER("sniper", "Rifle de Precisao", Material.SPYGLASS, "sniper_ammo", 5, 22.0, 130.0, 1300, 3000, 1, 0.0);

    final String id;
    final String displayName;
    final Material material;
    final String ammoId;
    final int magazineSize;
    final double damage;
    final double range;
    final long cooldownMillis;
    final long reloadMillis;
    final int pellets;
    final double spread;

    WeaponType(String id, String displayName, Material material, String ammoId,
               int magazineSize, double damage, double range, long cooldownMillis,
               long reloadMillis, int pellets, double spread) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.ammoId = ammoId;
        this.magazineSize = magazineSize;
        this.damage = damage;
        this.range = range;
        this.cooldownMillis = cooldownMillis;
        this.reloadMillis = reloadMillis;
        this.pellets = pellets;
        this.spread = spread;
    }

    static WeaponType fromId(String id) {
        for (WeaponType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}


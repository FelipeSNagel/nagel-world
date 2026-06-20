package com.nagelworld.gestures;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class NagelGesturesPlugin extends JavaPlugin implements Listener {
    private static final int REQUIRED_CROUCHES = 3;
    private static final long SEQUENCE_WINDOW_MS = 2_000;
    private static final long ACTIVATION_COOLDOWN_MS = 5_000;
    private static final double MAX_MOVEMENT_SQUARED = 0.25;

    private final Map<UUID, SneakSequence> sequences = new HashMap<>();
    private final Map<UUID, Long> lastActivations = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Atalho /home ativo: agache 3 vezes em ate 2 segundos.");
    }

    @Override
    public void onDisable() {
        sequences.clear();
        lastActivations.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("nagelgestures.home") || !player.isOnGround()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastActivation = lastActivations.getOrDefault(playerId, 0L);
        if (now - lastActivation < ACTIVATION_COOLDOWN_MS) {
            sequences.remove(playerId);
            return;
        }

        Location current = player.getLocation();
        SneakSequence sequence = sequences.get(playerId);
        if (sequence == null
                || now - sequence.startedAt() > SEQUENCE_WINDOW_MS
                || movedTooFar(sequence.origin(), current)) {
            sequences.put(playerId, new SneakSequence(1, now, current.clone()));
            return;
        }

        int count = sequence.count() + 1;
        if (count < REQUIRED_CROUCHES) {
            sequences.put(playerId, new SneakSequence(count, sequence.startedAt(), sequence.origin()));
            return;
        }

        sequences.remove(playerId);
        lastActivations.put(playerId, now);
        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline()) {
                player.performCommand("home");
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        sequences.remove(playerId);
        lastActivations.remove(playerId);
    }

    private boolean movedTooFar(Location origin, Location current) {
        return origin.getWorld() != current.getWorld()
                || origin.distanceSquared(current) > MAX_MOVEMENT_SQUARED;
    }

    private record SneakSequence(int count, long startedAt, Location origin) {
    }
}

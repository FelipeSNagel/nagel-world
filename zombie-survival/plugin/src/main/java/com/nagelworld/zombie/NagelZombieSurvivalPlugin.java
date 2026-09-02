package com.nagelworld.zombie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class NagelZombieSurvivalPlugin extends JavaPlugin implements Listener {
    private enum Nutrient {
        CARBS("carboidratos"), PROTEIN("proteinas"), FAT("gorduras"), VITAMINS("vitaminas"), WATER("hidratacao");

        final String label;

        Nutrient(String label) {
            this.label = label;
        }
    }

    private enum Season {
        SPRING("Primavera", 2.0), SUMMER("Verao", 9.0), AUTUMN("Outono", -1.0), WINTER("Inverno", -10.0);

        final String label;
        final double modifier;

        Season(String label, double modifier) {
            this.label = label;
            this.modifier = modifier;
        }
    }

    private static final class BlockProgress {
        double damage;
        long lastHit;
    }

    private static final class ShotImpact {
        double damage;
        int pellets;
        boolean headshot;
    }

    private record ShotHit(LivingEntity target, boolean headshot) {}

    private final Map<UUID, Long> lastShot = new HashMap<>();
    private final Map<UUID, Long> automaticFireUntil = new HashMap<>();
    private final Map<UUID, Long> lastMeleeDamage = new HashMap<>();
    private final Set<UUID> reloading = new HashSet<>();
    private final Set<UUID> applyingWeaponDamage = new HashSet<>();
    private final Map<String, BlockProgress> blockDamage = new HashMap<>();
    private final Map<UUID, Double> lastTemperature = new HashMap<>();
    private final Map<UUID, Long> lastHordeDay = new HashMap<>();
    private final Map<UUID, Integer> hordeCapacityBonus = new HashMap<>();
    private final Map<UUID, Long> hordeScheduledUntil = new HashMap<>();
    private final Map<UUID, Long> lastHordeMusic = new HashMap<>();
    private final Map<UUID, Long> lastBerserkerRoar = new HashMap<>();
    private final Map<UUID, Long> lastZombieBuild = new HashMap<>();
    private final Map<String, Block> temporaryZombieBlocks = new HashMap<>();
    private final Map<Material, double[]> foodValues = new EnumMap<>(Material.class);

    private NamespacedKey weaponKey;
    private NamespacedKey ammoKey;
    private NamespacedKey magazineKey;
    private NamespacedKey zombieTypeKey;
    private NamespacedKey zombieLookKey;
    private NamespacedKey zombieBreakerKey;
    private NamespacedKey zombieBuilderKey;
    private NamespacedKey hordeZombieKey;
    private NamespacedKey homeWorldKey;
    private NamespacedKey homeXKey;
    private NamespacedKey homeYKey;
    private NamespacedKey homeZKey;
    private NamespacedKey homeYawKey;
    private NamespacedKey homePitchKey;
    private final Map<Nutrient, NamespacedKey> nutrientKeys = new EnumMap<>(Nutrient.class);
    private int seasonLength;
    private int hordeInterval;
    private int hordeMinimumSize;
    private int hordeMaximumSize;
    private int hordeWaveSize;
    private int hordeMaximumPhases;
    private int hordePhaseBreakSeconds;
    private boolean zombiesBreakBlocks;
    private boolean zombiesBuildStairs;
    private boolean zombiesSpawnInDaylight;
    private boolean friendlyFire;
    private int protectedSpawnRadius;
    private double daylightSpawnChance;
    private int daylightZombieCap;
    private int nightZombieCapPerPlayer;
    private double minimumZombieSpawnDistance;
    private int maximumZombieSpawnBlockLight;
    private boolean zombiesSpawnOutdoorsOnly;
    private double zombieBreakerChance;
    private double zombieBuilderChance;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        seasonLength = Math.max(1, getConfig().getInt("season-length-days", 28));
        hordeInterval = Math.max(1, getConfig().getInt("horde-interval-days", 10));
        hordeMinimumSize = Math.max(1, getConfig().getInt("horde-minimum-size", 30));
        hordeMaximumSize = Math.max(hordeMinimumSize, getConfig().getInt("horde-maximum-size", 100));
        hordeWaveSize = Math.max(1, Math.min(10, getConfig().getInt("horde-wave-size", 5)));
        hordeMaximumPhases = Math.max(1, Math.min(5, getConfig().getInt("horde-maximum-phases", 5)));
        hordePhaseBreakSeconds = Math.max(5, getConfig().getInt("horde-phase-break-seconds", 12));
        zombiesBreakBlocks = getConfig().getBoolean("zombies-break-blocks", true);
        zombiesBuildStairs = getConfig().getBoolean("zombies-build-stairs", true);
        zombiesSpawnInDaylight = getConfig().getBoolean("zombies-spawn-in-daylight", true);
        friendlyFire = getConfig().getBoolean("friendly-fire", false);
        protectedSpawnRadius = Math.max(0, getConfig().getInt("protected-spawn-radius", 12));
        daylightSpawnChance = Math.max(0.0, Math.min(1.0, getConfig().getDouble("daylight-spawn-chance", 0.12)));
        daylightZombieCap = Math.max(0, getConfig().getInt("daylight-zombie-cap", 3));
        nightZombieCapPerPlayer = Math.max(1, getConfig().getInt("night-zombie-cap-per-player", 12));
        if (nightZombieCapPerPlayer == 8) nightZombieCapPerPlayer = 12;
        minimumZombieSpawnDistance = Math.max(24.0, getConfig().getDouble("minimum-zombie-spawn-distance", 36.0));
        maximumZombieSpawnBlockLight = Math.max(0, Math.min(15,
            getConfig().getInt("maximum-zombie-spawn-block-light", 0)));
        zombiesSpawnOutdoorsOnly = getConfig().getBoolean("zombies-spawn-outdoors-only", true);
        zombieBreakerChance = clampedChance("zombie-breaker-chance", 0.16);
        zombieBuilderChance = clampedChance("zombie-builder-chance", 0.12);

        weaponKey = new NamespacedKey(this, "weapon");
        ammoKey = new NamespacedKey(this, "ammo");
        magazineKey = new NamespacedKey(this, "magazine");
        zombieTypeKey = new NamespacedKey(this, "zombie_type");
        zombieLookKey = new NamespacedKey(this, "zombie_look");
        zombieBreakerKey = new NamespacedKey(this, "zombie_breaker");
        zombieBuilderKey = new NamespacedKey(this, "zombie_builder");
        hordeZombieKey = new NamespacedKey(this, "horde_zombie");
        homeWorldKey = new NamespacedKey(this, "home_world");
        homeXKey = new NamespacedKey(this, "home_x");
        homeYKey = new NamespacedKey(this, "home_y");
        homeZKey = new NamespacedKey(this, "home_z");
        homeYawKey = new NamespacedKey(this, "home_yaw");
        homePitchKey = new NamespacedKey(this, "home_pitch");
        for (Nutrient nutrient : Nutrient.values()) {
            nutrientKeys.put(nutrient, new NamespacedKey(this, nutrient.name().toLowerCase(Locale.ROOT)));
        }

        configureFoodValues();
        registerRecipes();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, this::tickPlayers, 40L, 40L);
        getServer().getScheduler().runTaskTimer(this, this::tickAutomaticWeapons, 2L, 2L);
        getServer().getScheduler().runTaskTimer(this, this::tickZombieBreaking, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, this::tickHordeTargets, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, this::tickHordeMusic, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, this::tickHordes, 200L, 200L);
        getServer().getScheduler().runTaskTimer(this, this::tickZombieAtmosphere, 80L, 80L);
        for (World world : Bukkit.getWorlds()) {
            world.getEntitiesByClass(Monster.class).stream()
                .filter(monster -> !(monster instanceof Zombie))
                .forEach(Entity::remove);
            world.getEntitiesByClass(Zombie.class).forEach(this::configureZombie);
        }
        getLogger().info("Nagel Zombie Survival carregado: armas, nutricao, estacoes e hordas ativas.");
        getLogger().info("Controle de spawn: " + nightZombieCapPerPlayer + " por jogador, distancia minima "
            + Math.round(minimumZombieSpawnDistance) + ", luz maxima " + maximumZombieSpawnBlockLight
            + ", somente ao ar livre: " + zombiesSpawnOutdoorsOnly + ".");
        getLogger().info("Horda especial: " + hordeMinimumSize + " a " + hordeMaximumSize
            + " infectados, ate " + hordeMaximumPhases + " fases, ondas de " + hordeWaveSize + ".");
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(player ->
            player.stopSound("nagelzombie:horde.tension", SoundCategory.MUSIC));
        temporaryZombieBlocks.values().forEach(block -> {
            if (block.getType() == Material.COBBLESTONE) block.setType(Material.AIR, false);
        });
        temporaryZombieBlocks.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        initializeNutrition(player);
        player.discoverRecipes(Arrays.asList(
            key("pistol"), key("smg"), key("shotgun"), key("rifle"), key("sniper"),
            key("light_ammo"), key("shell"), key("rifle_ammo"), key("sniper_ammo")
        ));
        player.sendMessage(ChatColor.DARK_RED + "Apocalipse ativo. " + ChatColor.GRAY + "Use /armas, /nutricao e /estacao.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getItem() == null) {
            return;
        }
        WeaponType weapon = weaponFrom(event.getItem());
        if (weapon == null) {
            return;
        }

        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            triggerFire(event.getPlayer(), event.getItem(), weapon);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (weapon != WeaponType.SNIPER || event.getPlayer().isSneaking()) {
                reload(event.getPlayer(), event.getItem(), weapon);
            }
            if (weapon != WeaponType.SNIPER || event.getPlayer().isSneaking()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onArmSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        WeaponType weapon = weaponFrom(item);
        if (weapon != null) {
            triggerFire(event.getPlayer(), item, weapon);
        } else {
            scheduleMeleeFallback(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWeaponMelee(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (applyingWeaponDamage.contains(player.getUniqueId())) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        WeaponType weapon = weaponFrom(item);
        if (weapon == null) {
            if (event.getEntity() instanceof Zombie) {
                lastMeleeDamage.put(player.getUniqueId(), System.currentTimeMillis());
            }
            return;
        }
        event.setCancelled(true);
        triggerFire(player, item, weapon);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onReloadDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        WeaponType weapon = weaponFrom(item);
        if (weapon == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        getServer().getScheduler().runTask(this, () -> {
            ItemStack held = player.getInventory().getItemInMainHand();
            WeaponType heldWeapon = weaponFrom(held);
            if (heldWeapon == weapon) reload(player, held, weapon);
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onStopUsingSniper(PlayerStopUsingItemEvent event) {
        if (event.getTicksHeldFor() < 3 || weaponFrom(event.getItem()) != WeaponType.SNIPER) {
            return;
        }
        Player player = event.getPlayer();
        getServer().getScheduler().runTask(this, () -> {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (weaponFrom(held) == WeaponType.SNIPER && !player.isSneaking()) {
                fire(player, held, WeaponType.SNIPER);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        double[] values = foodValues.get(event.getItem().getType());
        if (values == null) {
            return;
        }
        Player player = event.getPlayer();
        for (int i = 0; i < Nutrient.values().length; i++) {
            addNutrient(player, Nutrient.values()[i], values[i]);
        }
        getServer().getScheduler().runTaskLater(this, () -> sendSurvivalHud(player), 1L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onZombieSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Monster && isNaturalMonsterSpawn(event.getSpawnReason())
            && isHordeActive(event.getLocation().getWorld())) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Monster && !(event.getEntity() instanceof Zombie)) {
            if (isNaturalMonsterSpawn(event.getSpawnReason())) {
                Location location = event.getLocation().clone();
                event.setCancelled(true);
                if (!isDaytime(location.getWorld()) && canSpawnZombieAt(location)
                    && canSpawnMoreZombies(location.getWorld())) {
                    getServer().getScheduler().runTask(this, () -> {
                        if (location.isWorldLoaded() && canSpawnZombieAt(location)
                            && canSpawnMoreZombies(location.getWorld())) spawnZombieVariant(location);
                    });
                }
            }
            return;
        }
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (isNaturalMonsterSpawn(event.getSpawnReason())) {
            if (isDaytime(zombie.getWorld()) || !canSpawnZombieAt(event.getLocation())
                || !canSpawnMoreZombies(zombie.getWorld())) {
                event.setCancelled(true);
                return;
            }
        }
        if (zombie.getType() != EntityType.ZOMBIE && isNaturalMonsterSpawn(event.getSpawnReason())) {
            Location location = event.getLocation().clone();
            event.setCancelled(true);
            getServer().getScheduler().runTask(this, () -> {
                if (location.isWorldLoaded() && canSpawnZombieAt(location)
                    && canSpawnMoreZombies(location.getWorld())) spawnZombieVariant(location);
            });
            return;
        }
        if (!zombiesSpawnInDaylight && zombie.getWorld().getTime() < 12000) {
            return;
        }
        configureZombie(zombie);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onZombieCombust(EntityCombustEvent event) {
        if (event.getEntity() instanceof Zombie) {
            event.setCancelled(true);
            event.getEntity().setFireTicks(0);
        }
    }

    private boolean isNaturalMonsterSpawn(CreatureSpawnEvent.SpawnReason reason) {
        return Set.of("NATURAL", "CHUNK_GEN", "PATROL", "REINFORCEMENTS", "JOCKEY")
            .contains(reason.name());
    }

    private boolean isDaytime(World world) {
        return world.getEnvironment() == World.Environment.NORMAL && world.getTime() < 12000;
    }

    private boolean canSpawnZombieAt(Location location) {
        World world = location.getWorld();
        Block feet = location.getBlock();
        Block floor = feet.getRelative(BlockFace.DOWN);
        if (!feet.isPassable() || feet.isLiquid() || !floor.getType().isSolid()
            || feet.getLightFromBlocks() > maximumZombieSpawnBlockLight) {
            return false;
        }
        if (world.getEnvironment() == World.Environment.NORMAL && zombiesSpawnOutdoorsOnly) {
            int surfaceY = world.getHighestBlockYAt(feet.getX(), feet.getZ(), HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (feet.getY() <= surfaceY) return false;
        }
        double minimumDistanceSquared = minimumZombieSpawnDistance * minimumZombieSpawnDistance;
        for (Player player : world.getPlayers()) {
            if (player.getGameMode() == GameMode.SURVIVAL
                && player.getLocation().distanceSquared(location) < minimumDistanceSquared) {
                return false;
            }
        }
        return true;
    }

    private boolean canSpawnMoreZombies(World world) {
        return world.getEntitiesByClass(Zombie.class).size() < nightZombieCap(world);
    }

    private int nightZombieCap(World world) {
        long survivalPlayers = world.getPlayers().stream()
            .filter(player -> player.getGameMode() == GameMode.SURVIVAL)
            .count();
        if (survivalPlayers == 0) return 0;
        return (int) survivalPlayers * nightZombieCapPerPlayer
            + hordeCapacityBonus.getOrDefault(world.getUID(), 0);
    }

    private boolean isHordeActive(World world) {
        return hordeCapacityBonus.getOrDefault(world.getUID(), 0) > 0;
    }

    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) {
            return;
        }
        lastBerserkerRoar.remove(zombie.getUniqueId());
        lastZombieBuild.remove(zombie.getUniqueId());
        String type = zombie.getPersistentDataContainer().get(zombieTypeKey, PersistentDataType.STRING);
        double chance = "brute".equals(type) ? 0.35 : 0.10;
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            String ammo = ThreadLocalRandom.current().nextBoolean() ? "light_ammo" : "shell";
            event.getDrops().add(createAmmo(ammo, ThreadLocalRandom.current().nextInt(2, 7)));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("kitarmas") && !(sender instanceof Player)) {
            if (args.length != 1) {
                sender.sendMessage("Uso: kitarmas <jogador>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("Jogador nao encontrado.");
                return true;
            }
            giveTestKit(target);
            sender.sendMessage("Kit entregue para " + target.getName() + ".");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Comando exclusivo para jogadores.");
            return true;
        }
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "armas" -> showWeaponHelp(player);
            case "kitarmas" -> giveTestKit(player);
            case "nutricao" -> showNutrition(player);
            case "estacao" -> showSeason(player);
            case "recarregar" -> reloadHeldWeapon(player);
            case "horda" -> startManualHorde(player);
            case "sethome" -> setHome(player);
            case "home" -> teleportHome(player);
            default -> { return false; }
        }
        return true;
    }

    private void startManualHorde(Player player) {
        World world = player.getWorld();
        if (isHordeActive(world)) {
            player.sendMessage(ChatColor.RED + "Ja existe uma horda ativa neste mundo.");
            return;
        }
        List<Player> players = world.getPlayers().stream()
            .filter(online -> online.getGameMode() == GameMode.SURVIVAL)
            .toList();
        if (players.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Entre no modo sobrevivencia para iniciar a horda.");
            return;
        }
        lastHordeDay.put(world.getUID(), world.getFullTime() / 24000L);
        spawnHorde(world, players);
        player.sendMessage(ChatColor.DARK_RED + "Horda iniciada manualmente.");
    }

    private void fire(Player player, ItemStack item, WeaponType weapon) {
        long now = System.currentTimeMillis();
        if (reloading.contains(player.getUniqueId())) {
            player.sendActionBar(ChatColor.YELLOW + "Recarregando...");
            return;
        }
        if (now - lastShot.getOrDefault(player.getUniqueId(), 0L) < weapon.cooldownMillis) {
            return;
        }

        int magazine = magazine(item, weapon);
        if (magazine <= 0) {
            reload(player, item, weapon);
            return;
        }

        setMagazine(item, magazine - 1, weapon);
        player.getInventory().setItemInMainHand(item);
        lastShot.put(player.getUniqueId(), now);
        float shotVolume = weapon == WeaponType.SNIPER ? 1.5f : weapon == WeaponType.SMG ? 0.55f : 0.8f;
        float shotPitch = weapon == WeaponType.SHOTGUN ? 0.65f : weapon == WeaponType.SMG ? 1.65f : 1.35f;
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, shotVolume, shotPitch);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.7)), 5, 0.04, 0.04, 0.04, 0.01);

        Map<LivingEntity, ShotImpact> impacts = new HashMap<>();
        for (int pellet = 0; pellet < weapon.pellets; pellet++) {
            Vector direction = spread(player.getEyeLocation().getDirection(), weapon.spread);
            ShotHit hit = traceShot(player, direction, weapon);
            if (hit == null) continue;
            ShotImpact impact = impacts.computeIfAbsent(hit.target(), ignored -> new ShotImpact());
            impact.damage += weapon.damage * (hit.headshot() ? 1.75 : 1.0);
            impact.pellets++;
            impact.headshot |= hit.headshot();
        }
        impacts.forEach((target, impact) -> applyWeaponImpact(player, target, weapon, impact));
        attractZombies(player.getLocation(), weapon == WeaponType.SNIPER ? 64.0 : 42.0);

        int remaining = magazine - 1;
        player.sendActionBar(ChatColor.GOLD + weapon.displayName + ChatColor.GRAY + " [" + remaining + "/" + weapon.magazineSize + "]");
        if (remaining == 0) {
            getServer().getScheduler().runTaskLater(this, () -> reload(player, item, weapon), 4L);
        }
    }

    private void triggerFire(Player player, ItemStack item, WeaponType weapon) {
        if (weapon == WeaponType.SMG) {
            automaticFireUntil.put(player.getUniqueId(), System.currentTimeMillis() + 380L);
        }
        fire(player, item, weapon);
    }

    private void tickAutomaticWeapons() {
        long now = System.currentTimeMillis();
        automaticFireUntil.entrySet().removeIf(entry -> {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline() || entry.getValue() < now) return true;
            ItemStack held = player.getInventory().getItemInMainHand();
            if (weaponFrom(held) != WeaponType.SMG) return true;
            fire(player, held, WeaponType.SMG);
            return false;
        });
    }

    private void scheduleMeleeFallback(Player player) {
        long swingAt = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        getServer().getScheduler().runTask(this, () -> {
            if (!player.isOnline() || lastMeleeDamage.getOrDefault(playerId, 0L) >= swingAt
                || weaponFrom(player.getInventory().getItemInMainHand()) != null) {
                return;
            }
            Location origin = player.getEyeLocation();
            Vector direction = origin.getDirection();
            RayTraceResult result = player.getWorld().rayTrace(
                origin, direction, 3.4, FluidCollisionMode.NEVER, true, 0.25,
                entity -> entity instanceof Zombie
            );
            if (result == null || !(result.getHitEntity() instanceof Zombie zombie)) return;
            double attackDamage = 1.0;
            AttributeInstance attribute = player.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attribute != null) attackDamage = Math.max(1.0, attribute.getValue());
            lastMeleeDamage.put(playerId, System.currentTimeMillis());
            zombie.damage(attackDamage, player);
        });
    }

    private ShotHit traceShot(Player player, Vector direction, WeaponType weapon) {
        Location origin = player.getEyeLocation();
        RayTraceResult result = player.getWorld().rayTrace(
            origin, direction, weapon.range, FluidCollisionMode.NEVER, true, 0.35,
            entity -> entity instanceof LivingEntity && entity != player && (friendlyFire || !(entity instanceof Player))
        );

        double travel = weapon.range;
        if (result != null && result.getHitPosition() != null) {
            travel = result.getHitPosition().distance(origin.toVector());
        }
        drawTracer(origin, direction, travel);

        if (result == null || !(result.getHitEntity() instanceof LivingEntity target)) {
            return null;
        }
        double headY = target.getBoundingBox().getMaxY() - 0.35;
        return new ShotHit(target, result.getHitPosition().getY() >= headY);
    }

    private void applyWeaponImpact(Player player, LivingEntity target, WeaponType weapon, ShotImpact impact) {
        if (impact.headshot) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.8f);
            player.sendActionBar(ChatColor.RED + "HEADSHOT" + ChatColor.GRAY + " - " + String.format(Locale.US, "%.1f", impact.damage) + " dano");
        }
        UUID shooterId = player.getUniqueId();
        applyingWeaponDamage.add(shooterId);
        try {
            target.damage(impact.damage, player);
        } finally {
            applyingWeaponDamage.remove(shooterId);
        }
        applyWeaponKnockback(player, target, weapon, impact.pellets);
        if (target instanceof Zombie zombie
            && "infected".equals(zombie.getPersistentDataContainer().get(zombieTypeKey, PersistentDataType.STRING))) {
            playBerserkerRoar(zombie);
        }
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 4, 0.2, 0.3, 0.2, 0.05);
    }

    private void applyWeaponKnockback(Player player, LivingEntity target, WeaponType weapon, int pelletsHit) {
        double strength = weapon.knockback * pelletsHit / weapon.pellets;
        Vector direction = target.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0);
        if (direction.lengthSquared() < 0.001) direction = player.getLocation().getDirection().setY(0);
        direction.normalize().multiply(strength);
        Vector velocity = target.getVelocity().add(direction);
        velocity.setY(Math.min(0.55, Math.max(velocity.getY(), 0.08) + strength * 0.18));
        target.setVelocity(velocity);
    }

    private void drawTracer(Location origin, Vector direction, double distance) {
        World world = origin.getWorld();
        int points = Math.min(24, Math.max(2, (int) (distance / 3.0)));
        for (int i = 1; i <= points; i++) {
            Location point = origin.clone().add(direction.clone().multiply(distance * i / points));
            world.spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, 0);
        }
    }

    private Vector spread(Vector source, double amount) {
        if (amount <= 0) {
            return source.clone().normalize();
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return source.clone().add(new Vector(
            random.nextGaussian() * amount,
            random.nextGaussian() * amount,
            random.nextGaussian() * amount
        )).normalize();
    }

    private void reload(Player player, ItemStack item, WeaponType weapon) {
        UUID id = player.getUniqueId();
        if (!reloading.add(id)) {
            return;
        }
        int current = magazine(item, weapon);
        int needed = weapon.magazineSize - current;
        if (needed <= 0) {
            reloading.remove(id);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.7f, 1.5f);
            player.sendActionBar(ChatColor.GRAY + "Pente cheio" + ChatColor.DARK_GRAY + " [" + current + "/" + weapon.magazineSize + "]");
            return;
        }
        int available = countAmmo(player, weapon.ammoId);
        int toLoad = Math.min(needed, available);
        if (toLoad <= 0) {
            reloading.remove(id);
            player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_FAIL, 0.8f, 0.8f);
            player.sendActionBar(ChatColor.RED + "Sem municao");
            return;
        }
        player.sendActionBar(ChatColor.YELLOW + "Recarregando " + weapon.displayName + "...");
        player.playSound(player.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 0.7f, 1.4f);
        long delay = Math.max(1L, weapon.reloadMillis / 50L);
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) {
                reloading.remove(id);
                return;
            }
            ItemStack held = player.getInventory().getItemInMainHand();
            if (weaponFrom(held) != weapon) {
                reloading.remove(id);
                return;
            }
            int loaded = removeAmmo(player, weapon.ammoId, toLoad);
            setMagazine(held, magazine(held, weapon) + loaded, weapon);
            player.getInventory().setItemInMainHand(held);
            reloading.remove(id);
            player.playSound(player.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 0.7f, 1.6f);
            player.sendActionBar(ChatColor.GREEN + "Pronto" + ChatColor.GRAY + " [" + magazine(held, weapon) + "/" + weapon.magazineSize + "]");
        }, delay);
    }

    private void reloadHeldWeapon(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        WeaponType weapon = weaponFrom(item);
        if (weapon == null) {
            player.sendActionBar(ChatColor.RED + "Segure uma arma para recarregar");
            return;
        }
        reload(player, item, weapon);
    }

    private void tickPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            initializeNutrition(player);
            drainNutrition(player);
            double temperature = calculateTemperature(player);
            lastTemperature.put(player.getUniqueId(), temperature);
            applySurvivalEffects(player, temperature);
            sendSurvivalHud(player);
        }
    }

    private void sendSurvivalHud(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        WeaponType weapon = weaponFrom(held);
        if (weapon != null) {
            player.sendActionBar(ChatColor.GOLD + weapon.displayName + ChatColor.GRAY + " [" + magazine(held, weapon) + "/" + weapon.magazineSize + "]  " + temperatureText(player));
            return;
        }
        int average = 0;
        for (Nutrient nutrient : Nutrient.values()) {
            average += (int) nutrient(player, nutrient);
        }
        average /= Nutrient.values().length;
        player.sendActionBar(ChatColor.GREEN + "Nutricao " + average + "%  " + temperatureText(player));
    }

    private String temperatureText(Player player) {
        double value = lastTemperature.getOrDefault(player.getUniqueId(), calculateTemperature(player));
        ChatColor color = value < 5 ? ChatColor.AQUA : value > 34 ? ChatColor.RED : ChatColor.YELLOW;
        return color + String.format(Locale.US, "%.0f C", value);
    }

    private void applySurvivalEffects(Player player, double temperature) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        double nutritionAverage = 0;
        for (Nutrient nutrient : Nutrient.values()) {
            nutritionAverage += nutrient(player, nutrient);
        }
        nutritionAverage /= Nutrient.values().length;
        if (nutritionAverage < 15 || temperature < -5 || temperature > 44) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, true, false, true));
        }
        if (temperature < 0 || temperature > 40) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, false, true));
        }
        if (nutritionAverage > 85 && temperature >= 8 && temperature <= 30) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 45, 0, true, false, true));
        }
    }

    private double calculateTemperature(Player player) {
        Location location = player.getLocation();
        double value = 18.0 + season(player.getWorld()).modifier;
        double biome = player.getWorld().getTemperature(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        value += (biome - 0.8) * 18.0;
        long time = player.getWorld().getTime();
        if (time > 12500 && time < 23000) value -= 6.0;
        if (player.getWorld().hasStorm()) value -= 4.0;
        if (location.getY() > 100) value -= (location.getY() - 100) * 0.08;
        if (player.isInWater()) value -= 8.0;
        value += heatNearby(location);
        return value;
    }

    private double heatNearby(Location location) {
        double heat = 0.0;
        World world = location.getWorld();
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    Material type = world.getBlockAt(location.clone().add(x, y, z)).getType();
                    if (type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE || type == Material.FIRE) heat += 0.7;
                    if (type == Material.LAVA) heat += 1.2;
                }
            }
        }
        return Math.min(12.0, heat);
    }

    private Season season(World world) {
        long day = world.getFullTime() / 24000L;
        return Season.values()[(int) ((day / seasonLength) % Season.values().length)];
    }

    private void tickZombieBreaking() {
        if (!zombiesBreakBlocks && !zombiesBuildStairs) {
            return;
        }
        long now = System.currentTimeMillis();
        blockDamage.entrySet().removeIf(entry -> now - entry.getValue().lastHit > 8000L);
        for (World world : Bukkit.getWorlds()) {
            for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
                if (!(zombie.getTarget() instanceof Player target)) {
                    continue;
                }
                double distanceSquared = zombie.getLocation().distanceSquared(target.getLocation());
                if (zombiesBuildStairs && distanceSquared <= 100.0 && hasZombieAbility(zombie, zombieBuilderKey)) {
                    tryBuildZombieStep(zombie, target, now);
                }
                if (!zombiesBreakBlocks || distanceSquared > 16.0 || !hasZombieAbility(zombie, zombieBreakerKey)) {
                    continue;
                }
                Block block = obstruction(zombie, target);
                if (block == null || !canZombieBreak(block)) {
                    continue;
                }
                double resistance = blockResistance(block.getType());
                String key = world.getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
                BlockProgress progress = blockDamage.computeIfAbsent(key, ignored -> new BlockProgress());
                String type = zombie.getPersistentDataContainer().get(zombieTypeKey, PersistentDataType.STRING);
                progress.damage += "infected".equals(type) ? 3.25 : "brute".equals(type) ? 2.5 : 1.0;
                progress.lastHit = now;
                float ratio = (float) Math.min(1.0, progress.damage / resistance);
                for (Player player : world.getPlayers()) {
                    if (player.getLocation().distanceSquared(block.getLocation()) < 1024) {
                        player.sendBlockDamage(block.getLocation(), ratio, zombie);
                    }
                }
                world.playSound(block.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.35f, 0.8f);
                if (progress.damage >= resistance) {
                    world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 18, 0.25, 0.25, 0.25, block.getBlockData());
                    block.breakNaturally(false);
                    blockDamage.remove(key);
                }
            }
        }
    }

    private void tryBuildZombieStep(Zombie zombie, Player target, long now) {
        UUID id = zombie.getUniqueId();
        if (target.getLocation().getY() - zombie.getLocation().getY() < 1.75
            || now - lastZombieBuild.getOrDefault(id, 0L) < 1800L) {
            return;
        }
        Vector direction = target.getLocation().toVector().subtract(zombie.getLocation().toVector()).setY(0);
        if (direction.lengthSquared() < 0.04) return;
        direction.normalize();

        Block step = zombie.getLocation().clone().add(direction.multiply(1.15)).getBlock();
        Block support = step.getRelative(BlockFace.DOWN);
        Block headroom = step.getRelative(BlockFace.UP);
        if (!step.isPassable() || step.isLiquid() || !headroom.isPassable() || !support.getType().isSolid()
            || isProtectedSpawn(step)) {
            return;
        }
        for (Player player : step.getWorld().getPlayers()) {
            if (player.getLocation().getBlock().equals(step)
                || player.getEyeLocation().getBlock().equals(step)) return;
        }

        step.setType(Material.COBBLESTONE, false);
        lastZombieBuild.put(id, now);
        String key = blockKey(step);
        temporaryZombieBlocks.put(key, step);
        step.getWorld().playSound(step.getLocation(), Sound.BLOCK_STONE_PLACE, 0.65f, 0.72f);
        step.getWorld().spawnParticle(Particle.BLOCK, step.getLocation().add(0.5, 0.75, 0.5),
            8, 0.2, 0.15, 0.2, step.getBlockData());
        getServer().getScheduler().runTaskLater(this, () -> {
            Block temporaryBlock = temporaryZombieBlocks.remove(key);
            if (temporaryBlock != null && temporaryBlock.getType() == Material.COBBLESTONE) {
                temporaryBlock.setType(Material.AIR, false);
            }
        }, 20L * 90L);
    }

    private Block obstruction(Zombie zombie, Player target) {
        Location eye = zombie.getEyeLocation();
        Vector direction = target.getEyeLocation().toVector().subtract(eye.toVector()).normalize();
        for (double distance = 0.6; distance <= 1.6; distance += 0.35) {
            Block block = eye.clone().add(direction.clone().multiply(distance)).getBlock();
            if (!block.isPassable() && block.getType().isSolid()) {
                return block;
            }
        }
        return null;
    }

    private boolean canZombieBreak(Block block) {
        Material material = block.getType();
        String name = material.name();
        return !isProtectedSpawn(block)
            && material != Material.BEDROCK
            && material != Material.OBSIDIAN
            && material != Material.CRYING_OBSIDIAN
            && material != Material.REINFORCED_DEEPSLATE
            && !name.contains("CHEST")
            && !name.contains("SHULKER")
            && !name.contains("SPAWNER")
            && !name.contains("COMMAND_BLOCK")
            && !name.contains("PORTAL")
            && material.getHardness() >= 0;
    }

    private boolean isProtectedSpawn(Block block) {
        Location spawn = block.getWorld().getSpawnLocation();
        return protectedSpawnRadius > 0
            && Math.abs(block.getX() - spawn.getBlockX()) <= protectedSpawnRadius
            && Math.abs(block.getZ() - spawn.getBlockZ()) <= protectedSpawnRadius;
    }

    private String blockKey(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private double blockResistance(Material material) {
        String name = material.name();
        if (name.contains("GLASS") || name.contains("DOOR") || name.contains("TRAPDOOR")) return 3.0;
        if (name.contains("WOOD") || name.contains("PLANK") || name.contains("LOG") || material == Material.DIRT) return 8.0;
        if (name.contains("STONE") || name.contains("BRICK") || name.contains("COBBLE")) return 20.0;
        if (name.contains("IRON") || name.contains("COPPER")) return 45.0;
        return Math.max(6.0, material.getHardness() * 8.0);
    }

    private void tickHordes() {
        tickDaylightZombies();
        for (World world : Bukkit.getWorlds()) {
            long day = world.getFullTime() / 24000L;
            long time = world.getTime();
            if (day == 0 || day % hordeInterval != 0 || time < 13000 || time > 13400
                || lastHordeDay.getOrDefault(world.getUID(), -1L) == day || isHordeActive(world)) {
                continue;
            }
            List<Player> players = world.getPlayers().stream()
                .filter(player -> player.getGameMode() == GameMode.SURVIVAL)
                .toList();
            if (players.isEmpty()) continue;
            lastHordeDay.put(world.getUID(), day);
            spawnHorde(world, players);
        }
    }

    private void tickDaylightZombies() {
        if (!zombiesSpawnInDaylight) return;
        for (World world : Bukkit.getWorlds()) {
            long time = world.getTime();
            if (time >= 12000 || world.getEnvironment() != World.Environment.NORMAL || isHordeActive(world)) continue;
            for (Player player : world.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL) continue;
                List<Zombie> nearbyZombies = new ArrayList<>();
                for (Entity entity : world.getNearbyEntities(player.getLocation(), 48, 20, 48)) {
                    if (entity instanceof Zombie zombie) nearbyZombies.add(zombie);
                }
                nearbyZombies.sort((first, second) -> Double.compare(
                    second.getLocation().distanceSquared(player.getLocation()),
                    first.getLocation().distanceSquared(player.getLocation())
                ));
                while (nearbyZombies.size() > daylightZombieCap) {
                    nearbyZombies.remove(0).remove();
                }
                if (nearbyZombies.size() >= daylightZombieCap
                    || ThreadLocalRandom.current().nextDouble() > daylightSpawnChance) continue;
                double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
                double distance = ThreadLocalRandom.current().nextDouble(38.0, 54.0);
                int x = player.getLocation().getBlockX() + (int) (Math.cos(angle) * distance);
                int z = player.getLocation().getBlockZ() + (int) (Math.sin(angle) * distance);
                int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
                Location spawn = new Location(world, x + 0.5, y, z + 0.5);
                if (!canSpawnZombieAt(spawn)) continue;
                Zombie zombie = spawnZombieVariant(spawn);
                zombie.setTarget(player);
            }
        }
    }

    private void spawnHorde(World world, List<Player> players) {
        int total = ThreadLocalRandom.current().nextInt(hordeMinimumSize, hordeMaximumSize + 1);
        int phases = ThreadLocalRandom.current().nextInt(1, hordeMaximumPhases + 1);
        List<Integer> phaseSizes = splitHordeIntoPhases(total, phases);
        hordeCapacityBonus.put(world.getUID(), total);
        List<UUID> playerIds = players.stream().map(Player::getUniqueId).toList();
        players.forEach(player -> playHordeWarning(player, total, phases));
        getLogger().info("Horda sorteada em " + world.getName() + ": " + total
            + " infectados em " + phases + " fases " + phaseSizes + ".");

        long phaseStart = 60L;
        int distributed = 0;
        for (int phase = 0; phase < phases; phase++) {
            int phaseNumber = phase + 1;
            int phaseAmount = phaseSizes.get(phase);
            int startingIndex = distributed;
            long currentPhaseStart = phaseStart;
            getServer().getScheduler().runTaskLater(this,
                () -> announceHordePhase(world, playerIds, phaseNumber, phases, phaseAmount), currentPhaseStart);

            int waves = (phaseAmount + hordeWaveSize - 1) / hordeWaveSize;
            for (int wave = 0; wave < waves; wave++) {
                int waveIndex = wave;
                int amount = Math.min(hordeWaveSize, phaseAmount - wave * hordeWaveSize);
                getServer().getScheduler().runTaskLater(this, () -> {
                    for (int index = 0; index < amount; index++) {
                        UUID targetId = playerIds.get((startingIndex + waveIndex * hordeWaveSize + index) % playerIds.size());
                        Player target = Bukkit.getPlayer(targetId);
                        if (target != null && target.isOnline() && target.getWorld().equals(world)) {
                            trySpawnHordeZombie(target);
                        }
                    }
                }, currentPhaseStart + wave * 20L);
            }
            distributed += phaseAmount;
            phaseStart += waves * 20L + hordePhaseBreakSeconds * 20L;
        }
        long scheduledTicks = phaseStart - hordePhaseBreakSeconds * 20L;
        hordeScheduledUntil.put(world.getUID(), System.currentTimeMillis() + scheduledTicks * 50L);
    }

    private List<Integer> splitHordeIntoPhases(int total, int phases) {
        List<Integer> sizes = new ArrayList<>();
        int remaining = total;
        int minimumPerPhase = Math.min(hordeWaveSize, Math.max(1, total / phases / 2));
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int phase = 0; phase < phases; phase++) {
            int remainingPhases = phases - phase - 1;
            if (remainingPhases == 0) {
                sizes.add(remaining);
                break;
            }
            int average = remaining / (remainingPhases + 1);
            int lower = Math.max(minimumPerPhase, average / 2);
            int upper = Math.min(remaining - remainingPhases * minimumPerPhase,
                Math.max(lower, average + average / 2));
            int amount = random.nextInt(lower, upper + 1);
            sizes.add(amount);
            remaining -= amount;
        }
        return sizes;
    }

    private void announceHordePhase(World world, List<UUID> playerIds, int phase, int phases, int amount) {
        for (UUID playerId : playerIds) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline() || !player.getWorld().equals(world)) continue;
            player.sendTitle(ChatColor.DARK_RED + "FASE " + phase + "/" + phases,
                ChatColor.GRAY + String.valueOf(amount) + " infectados", 5, 45, 10);
            player.playSound(player.getLocation(), Sound.EVENT_RAID_HORN, 1.0f, 0.72f + phase * 0.04f);
        }
    }

    private void trySpawnHordeZombie(Player target) {
        World world = target.getWorld();
        for (int attempt = 0; attempt < 12 && canSpawnMoreZombies(world); attempt++) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
            double distance = ThreadLocalRandom.current().nextDouble(40.0, 64.0);
            int x = target.getLocation().getBlockX() + (int) (Math.cos(angle) * distance);
            int z = target.getLocation().getBlockZ() + (int) (Math.sin(angle) * distance);
            int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
            Location spawn = new Location(world, x + 0.5, y, z + 0.5);
            if (!canSpawnZombieAt(spawn)) continue;
            Zombie zombie = spawnZombieVariant(spawn);
            markHordeZombie(zombie);
            zombie.setTarget(nearestSurvivalPlayer(zombie.getWorld(), zombie.getLocation()));
            return;
        }
    }

    private void playHordeWarning(Player player, int total, int phases) {
        Location location = player.getLocation();
        player.playSound(location, "nagelzombie:horde.warning", 2.4f, 1.0f);
        player.playSound(location, Sound.BLOCK_BELL_RESONATE, 1.15f, 0.52f);
        getServer().getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.32f, 0.58f);
                player.sendTitle(ChatColor.DARK_RED + "A HORDA CHEGOU",
                    ChatColor.GRAY + String.valueOf(total) + " infectados em " + phases + " fases", 10, 70, 20);
            }
        }, 34L);
    }

    private Zombie spawnZombieVariant(Location location) {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < 0.68) return location.getWorld().spawn(location, Zombie.class);
        if (roll < 0.88) return location.getWorld().spawn(location, Husk.class);
        return location.getWorld().spawn(location, Drowned.class);
    }

    private void configureZombie(Zombie zombie) {
        PersistentDataContainer data = zombie.getPersistentDataContainer();
        String type = data.get(zombieTypeKey, PersistentDataType.STRING);
        if (type == null) {
            double roll = ThreadLocalRandom.current().nextDouble();
            type = roll < 0.30 ? "shambler"
                : roll < 0.78 ? "walker"
                : roll < 0.90 ? "stalker"
                : roll < 0.95 ? "runner"
                : roll < 0.99 ? "brute"
                : "infected";
            data.set(zombieTypeKey, PersistentDataType.STRING, type);
        }
        assignZombieAbilities(zombie, type);
        applyZombieProfile(zombie, type);
        applyZombieLook(zombie, type);
        if (isHordeZombie(zombie)) applyHordeProfile(zombie);
        zombie.setCustomNameVisible(false);
        zombie.setCanBreakDoors(true);
        zombie.setShouldBurnInDay(false);
        zombie.setFireTicks(0);
        zombie.setRemoveWhenFarAway(!isHordeZombie(zombie));
    }

    private void markHordeZombie(Zombie zombie) {
        zombie.getPersistentDataContainer().set(hordeZombieKey, PersistentDataType.BYTE, (byte) 1);
        applyHordeProfile(zombie);
        zombie.setRemoveWhenFarAway(false);
    }

    private boolean isHordeZombie(Zombie zombie) {
        return Byte.valueOf((byte) 1).equals(
            zombie.getPersistentDataContainer().get(hordeZombieKey, PersistentDataType.BYTE));
    }

    private void applyHordeProfile(Zombie zombie) {
        AttributeInstance speed = zombie.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(Math.min(0.38, speed.getBaseValue() * 1.12));
        AttributeInstance range = zombie.getAttribute(Attribute.FOLLOW_RANGE);
        if (range != null) range.setBaseValue(Math.max(128.0, range.getBaseValue()));
        zombie.setAware(true);
    }

    private void tickHordeTargets() {
        for (World world : Bukkit.getWorlds()) {
            for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
                if (!isHordeZombie(zombie)) continue;
                Player nearest = nearestSurvivalPlayer(world, zombie.getLocation());
                if (nearest != null) zombie.setTarget(nearest);
            }
        }
    }

    private void tickHordeMusic() {
        long now = System.currentTimeMillis();
        Set<UUID> listeningPlayers = new HashSet<>();
        for (World world : Bukkit.getWorlds()) {
            UUID worldId = world.getUID();
            boolean scheduled = now < hordeScheduledUntil.getOrDefault(worldId, 0L);
            boolean hasHordeZombies = world.getEntitiesByClass(Zombie.class).stream().anyMatch(this::isHordeZombie);
            if (!scheduled && !hasHordeZombies) {
                hordeCapacityBonus.remove(worldId);
                hordeScheduledUntil.remove(worldId);
                continue;
            }
            for (Player player : world.getPlayers()) {
                if (player.getGameMode() == GameMode.SPECTATOR) continue;
                UUID playerId = player.getUniqueId();
                listeningPlayers.add(playerId);
                if (now - lastHordeMusic.getOrDefault(playerId, 0L) < 22000L) continue;
                player.playSound(player, "nagelzombie:horde.tension", SoundCategory.MUSIC, 0.82f, 1.0f);
                player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.AMBIENT, 0.18f, 0.62f);
                lastHordeMusic.put(playerId, now);
            }
        }
        for (UUID playerId : new HashSet<>(lastHordeMusic.keySet())) {
            if (listeningPlayers.contains(playerId)) continue;
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) player.stopSound("nagelzombie:horde.tension", SoundCategory.MUSIC);
            lastHordeMusic.remove(playerId);
        }
    }

    private Player nearestSurvivalPlayer(World world, Location location) {
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Player player : world.getPlayers()) {
            if (player.getGameMode() != GameMode.SURVIVAL) continue;
            double distance = player.getLocation().distanceSquared(location);
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private void assignZombieAbilities(Zombie zombie, String type) {
        PersistentDataContainer data = zombie.getPersistentDataContainer();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (!data.has(zombieBreakerKey, PersistentDataType.BYTE)) {
            double chance = switch (type) {
                case "infected" -> 1.0;
                case "brute" -> Math.max(0.65, zombieBreakerChance);
                case "stalker" -> Math.min(1.0, zombieBreakerChance * 1.5);
                default -> zombieBreakerChance;
            };
            data.set(zombieBreakerKey, PersistentDataType.BYTE, (byte) (random.nextDouble() < chance ? 1 : 0));
        }
        if (!data.has(zombieBuilderKey, PersistentDataType.BYTE)) {
            double chance = switch (type) {
                case "infected" -> 1.0;
                case "brute" -> Math.max(0.30, zombieBuilderChance);
                case "stalker", "runner" -> Math.min(1.0, zombieBuilderChance * 1.5);
                default -> zombieBuilderChance;
            };
            data.set(zombieBuilderKey, PersistentDataType.BYTE, (byte) (random.nextDouble() < chance ? 1 : 0));
        }
    }

    private boolean hasZombieAbility(Zombie zombie, NamespacedKey key) {
        return Byte.valueOf((byte) 1).equals(
            zombie.getPersistentDataContainer().get(key, PersistentDataType.BYTE));
    }

    private double clampedChance(String path, double fallback) {
        return Math.max(0.0, Math.min(1.0, getConfig().getDouble(path, fallback)));
    }

    private void applyZombieProfile(Zombie zombie, String type) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        setAttribute(zombie, Attribute.FOLLOW_RANGE, 40.0);
        setAttribute(zombie, Attribute.ATTACK_DAMAGE, 4.0);
        setAttribute(zombie, Attribute.KNOCKBACK_RESISTANCE, 0.0);
        setAttribute(zombie, Attribute.MAX_HEALTH, 20.0);
        setAttribute(zombie, Attribute.SCALE, 1.0);
        zombie.setHealth(Math.min(zombie.getHealth(), 20.0));
        switch (type) {
            case "shambler" -> {
                setAttribute(zombie, Attribute.MOVEMENT_SPEED, random.nextDouble(0.10, 0.14));
                setAttribute(zombie, Attribute.MAX_HEALTH, 18.0);
                setAttribute(zombie, Attribute.ATTACK_DAMAGE, 3.0);
                zombie.setHealth(Math.min(zombie.getHealth(), 18.0));
                zombie.setCustomName(ChatColor.DARK_GRAY + "Cambaleante");
            }
            case "stalker" -> {
                setAttribute(zombie, Attribute.MOVEMENT_SPEED, random.nextDouble(0.24, 0.28));
                setAttribute(zombie, Attribute.MAX_HEALTH, 22.0);
                setAttribute(zombie, Attribute.ATTACK_DAMAGE, 5.0);
                zombie.setHealth(Math.max(zombie.getHealth(), 22.0));
                setAttribute(zombie, Attribute.FOLLOW_RANGE, 52.0);
                zombie.setCustomName(ChatColor.GOLD + "Cacador");
            }
            case "runner" -> {
                setAttribute(zombie, Attribute.MOVEMENT_SPEED, random.nextDouble(0.31, 0.36));
                setAttribute(zombie, Attribute.MAX_HEALTH, 14.0);
                zombie.setHealth(Math.min(zombie.getHealth(), 14.0));
                zombie.setCustomName(ChatColor.YELLOW + "Corredor");
            }
            case "brute" -> {
                setAttribute(zombie, Attribute.MOVEMENT_SPEED, random.nextDouble(0.15, 0.19));
                setAttribute(zombie, Attribute.MAX_HEALTH, 60.0);
                setAttribute(zombie, Attribute.ATTACK_DAMAGE, 10.0);
                setAttribute(zombie, Attribute.KNOCKBACK_RESISTANCE, 0.55);
                zombie.setHealth(Math.max(zombie.getHealth(), 60.0));
                zombie.setCustomName(ChatColor.DARK_RED + "Brutamontes");
            }
            case "infected" -> {
                setAttribute(zombie, Attribute.MOVEMENT_SPEED, random.nextDouble(0.11, 0.15));
                setAttribute(zombie, Attribute.MAX_HEALTH, 80.0);
                setAttribute(zombie, Attribute.ATTACK_DAMAGE, 12.0);
                setAttribute(zombie, Attribute.FOLLOW_RANGE, 64.0);
                setAttribute(zombie, Attribute.KNOCKBACK_RESISTANCE, 0.72);
                setAttribute(zombie, Attribute.SCALE, 1.45);
                zombie.setHealth(Math.max(zombie.getHealth(), 80.0));
                zombie.setCustomName(ChatColor.DARK_RED + "Berserker");
            }
            default -> {
                setAttribute(zombie, Attribute.MOVEMENT_SPEED, random.nextDouble(0.17, 0.22));
                zombie.setCustomName(ChatColor.GRAY + "Errante");
            }
        }
    }

    private void applyZombieLook(Zombie zombie, String type) {
        PersistentDataContainer data = zombie.getPersistentDataContainer();
        String look = data.get(zombieLookKey, PersistentDataType.STRING);
        if (look == null) {
            String[] looks = {"civilian", "worker", "medic", "soldier", "prisoner", "scavenger"};
            look = looks[ThreadLocalRandom.current().nextInt(looks.length)];
            data.set(zombieLookKey, PersistentDataType.STRING, look);
        }
        EntityEquipment equipment = zombie.getEquipment();
        equipment.clear();
        if ("infected".equals(type)) {
            equipment.setChestplate(dyedArmor(Material.LEATHER_CHESTPLATE, Color.fromRGB(72, 20, 18)));
            equipment.setLeggings(dyedArmor(Material.LEATHER_LEGGINGS, Color.fromRGB(35, 31, 29)));
        } else {
            switch (look) {
                case "worker" -> {
                    equipment.setHelmet(dyedArmor(Material.LEATHER_HELMET, Color.fromRGB(196, 113, 24)));
                    equipment.setChestplate(dyedArmor(Material.LEATHER_CHESTPLATE, Color.fromRGB(184, 91, 23)));
                    equipment.setLeggings(dyedArmor(Material.LEATHER_LEGGINGS, Color.fromRGB(48, 54, 57)));
                }
                case "medic" -> {
                    equipment.setChestplate(dyedArmor(Material.LEATHER_CHESTPLATE, Color.fromRGB(190, 188, 177)));
                    equipment.setLeggings(dyedArmor(Material.LEATHER_LEGGINGS, Color.fromRGB(69, 76, 79)));
                    equipment.setBoots(dyedArmor(Material.LEATHER_BOOTS, Color.fromRGB(126, 31, 28)));
                }
                case "soldier" -> {
                    equipment.setHelmet(dyedArmor(Material.LEATHER_HELMET, Color.fromRGB(55, 65, 43)));
                    equipment.setChestplate(dyedArmor(Material.LEATHER_CHESTPLATE, Color.fromRGB(64, 74, 48)));
                    equipment.setLeggings(dyedArmor(Material.LEATHER_LEGGINGS, Color.fromRGB(46, 51, 39)));
                }
                case "prisoner" -> {
                    equipment.setChestplate(dyedArmor(Material.LEATHER_CHESTPLATE, Color.fromRGB(202, 94, 23)));
                    equipment.setLeggings(dyedArmor(Material.LEATHER_LEGGINGS, Color.fromRGB(176, 75, 18)));
                }
                case "scavenger" -> {
                    equipment.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
                    equipment.setLeggings(dyedArmor(Material.LEATHER_LEGGINGS, Color.fromRGB(73, 54, 40)));
                }
                default -> {
                    equipment.setChestplate(dyedArmor(Material.LEATHER_CHESTPLATE, Color.fromRGB(51, 65, 75)));
                    equipment.setLeggings(dyedArmor(Material.LEATHER_LEGGINGS, Color.fromRGB(53, 45, 40)));
                }
            }
        }
        equipment.setHelmetDropChance(0.0f);
        equipment.setChestplateDropChance(0.0f);
        equipment.setLeggingsDropChance(0.0f);
        equipment.setBootsDropChance(0.0f);
    }

    private ItemStack dyedArmor(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private void tickZombieAtmosphere() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (World world : Bukkit.getWorlds()) {
            enforceNightZombieCap(world);
            for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
                zombie.setShouldBurnInDay(false);
                if (zombie.getFireTicks() > 0) zombie.setFireTicks(0);
                Player nearby = nearestPlayer(zombie.getLocation(), 28.0);
                if (nearby == null) continue;
                if (zombie.getTarget() == null && random.nextDouble() < 0.35) zombie.setTarget(nearby);
                String type = zombie.getPersistentDataContainer().get(zombieTypeKey, PersistentDataType.STRING);
                if ("infected".equals(type)) {
                    if (random.nextDouble() < 0.28) playBerserkerRoar(zombie);
                } else if (random.nextDouble() < 0.12) {
                    float pitch = "runner".equals(type) ? 1.12f : "brute".equals(type) ? 0.72f : random.nextFloat(0.82f, 1.02f);
                    world.playSound(zombie.getLocation(), "nagelzombie:zombie.ambient", 1.15f, pitch);
                    world.playSound(zombie.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 0.28f, pitch * 0.9f);
                }
            }
        }
    }

    private void enforceNightZombieCap(World world) {
        if (isDaytime(world) && !isHordeActive(world)) return;
        if (world.getPlayers().stream().noneMatch(player -> player.getGameMode() == GameMode.SURVIVAL)) {
            hordeCapacityBonus.remove(world.getUID());
            hordeScheduledUntil.remove(world.getUID());
        }
        int cap = nightZombieCap(world);
        List<Zombie> zombies = new ArrayList<>(world.getEntitiesByClass(Zombie.class));
        if (zombies.size() <= cap) return;
        zombies.sort((first, second) -> Double.compare(
            distanceToNearestPlayerSquared(second), distanceToNearestPlayerSquared(first)));
        while (zombies.size() > cap) zombies.remove(0).remove();
    }

    private double distanceToNearestPlayerSquared(Zombie zombie) {
        double nearest = Double.MAX_VALUE;
        for (Player player : zombie.getWorld().getPlayers()) {
            if (player.getGameMode() == GameMode.SURVIVAL) {
                nearest = Math.min(nearest, player.getLocation().distanceSquared(zombie.getLocation()));
            }
        }
        return nearest;
    }

    private void playBerserkerRoar(Zombie zombie) {
        long now = System.currentTimeMillis();
        UUID id = zombie.getUniqueId();
        if (now - lastBerserkerRoar.getOrDefault(id, 0L) < 12000L) return;
        lastBerserkerRoar.put(id, now);
        World world = zombie.getWorld();
        world.playSound(zombie.getLocation(), "nagelzombie:zombie.berserker", 2.2f, 0.78f);
        world.playSound(zombie.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.75f, 0.58f);
        world.playSound(zombie.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.22f, 0.68f);
    }

    private void setAttribute(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    private void attractZombies(Location location, double radius) {
        for (Entity entity : location.getWorld().getNearbyEntities(location, radius, radius / 2.0, radius)) {
            if (entity instanceof Zombie zombie) {
                Player nearest = nearestPlayer(zombie.getLocation(), radius);
                if (nearest != null) zombie.setTarget(nearest);
            }
        }
    }

    private Player nearestPlayer(Location location, double radius) {
        Player nearest = null;
        double best = radius * radius;
        for (Player player : location.getWorld().getPlayers()) {
            double distance = location.distanceSquared(player.getLocation());
            if (distance < best && player.getGameMode() == GameMode.SURVIVAL) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private ItemStack createWeapon(WeaponType weapon) {
        ItemStack item = new ItemStack(weapon.material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + weapon.displayName);
        meta.setItemModel(new NamespacedKey("nagelzombie", weapon.id));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        meta.setLore(List.of(
            ChatColor.GRAY + "Municao: " + ammoName(weapon.ammoId),
            ChatColor.GRAY + "Pente: " + weapon.magazineSize,
            ChatColor.DARK_GRAY + "Ataque: disparar",
            ChatColor.DARK_GRAY + "R: recarregar (associe a Soltar item)"
        ));
        meta.getPersistentDataContainer().set(weaponKey, PersistentDataType.STRING, weapon.id);
        meta.getPersistentDataContainer().set(magazineKey, PersistentDataType.INTEGER, 0);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAmmo(String id, int amount) {
        ItemStack item = new ItemStack(Material.IRON_NUGGET, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + ammoName(id));
        meta.setItemModel(new NamespacedKey("nagelzombie", id));
        meta.getPersistentDataContainer().set(ammoKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    private WeaponType weaponFrom(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(weaponKey, PersistentDataType.STRING);
        return id == null ? null : WeaponType.fromId(id);
    }

    private int magazine(ItemStack item, WeaponType weapon) {
        Integer value = item.getItemMeta().getPersistentDataContainer().get(magazineKey, PersistentDataType.INTEGER);
        return value == null ? 0 : Math.min(weapon.magazineSize, Math.max(0, value));
    }

    private void setMagazine(ItemStack item, int value, WeaponType weapon) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(magazineKey, PersistentDataType.INTEGER, Math.min(weapon.magazineSize, Math.max(0, value)));
        item.setItemMeta(meta);
    }

    private int countAmmo(Player player, String ammoId) {
        int total = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (ammoId.equals(ammoId(item))) total += item.getAmount();
        }
        return total;
    }

    private int removeAmmo(Player player, String ammoId, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (!ammoId.equals(ammoId(item))) continue;
            int removed = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - removed);
            remaining -= removed;
            contents[i] = item.getAmount() == 0 ? null : item;
        }
        player.getInventory().setStorageContents(contents);
        return amount - remaining;
    }

    private String ammoId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ammoKey, PersistentDataType.STRING);
    }

    private void initializeNutrition(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        for (Nutrient nutrient : Nutrient.values()) {
            if (!data.has(nutrientKeys.get(nutrient), PersistentDataType.DOUBLE)) {
                data.set(nutrientKeys.get(nutrient), PersistentDataType.DOUBLE, 70.0);
            }
        }
    }

    private void drainNutrition(Player player) {
        double drain = player.isSprinting() ? 0.09 : 0.035;
        for (Nutrient nutrient : Nutrient.values()) {
            addNutrient(player, nutrient, nutrient == Nutrient.WATER ? -drain * 1.7 : -drain);
        }
    }

    private double nutrient(Player player, Nutrient nutrient) {
        return player.getPersistentDataContainer().getOrDefault(nutrientKeys.get(nutrient), PersistentDataType.DOUBLE, 70.0);
    }

    private void addNutrient(Player player, Nutrient nutrient, double amount) {
        double value = Math.max(0.0, Math.min(100.0, nutrient(player, nutrient) + amount));
        player.getPersistentDataContainer().set(nutrientKeys.get(nutrient), PersistentDataType.DOUBLE, value);
    }

    private void showNutrition(Player player) {
        player.sendMessage(ChatColor.GOLD + "--- Nutricao ---");
        for (Nutrient nutrient : Nutrient.values()) {
            player.sendMessage(ChatColor.GRAY + nutrient.label + ": " + nutritionColor(nutrient(player, nutrient)) + String.format(Locale.US, "%.0f%%", nutrient(player, nutrient)));
        }
    }

    private ChatColor nutritionColor(double value) {
        if (value < 25) return ChatColor.RED;
        if (value < 60) return ChatColor.YELLOW;
        return ChatColor.GREEN;
    }

    private void showSeason(Player player) {
        long day = player.getWorld().getFullTime() / 24000L;
        player.sendMessage(ChatColor.GOLD + "Estacao: " + ChatColor.WHITE + season(player.getWorld()).label);
        player.sendMessage(ChatColor.GOLD + "Dia: " + ChatColor.WHITE + day + ChatColor.GRAY + " | " + temperatureText(player));
    }

    private void showWeaponHelp(Player player) {
        player.sendMessage(ChatColor.DARK_RED + "--- Arsenal ---");
        player.sendMessage(ChatColor.GOLD + "Pistola: " + ChatColor.GRAY + "ferro, redstone e pederneira.");
        player.sendMessage(ChatColor.GOLD + "Submetralhadora: " + ChatColor.GRAY + "ferro, cobre, redstone e pederneira.");
        player.sendMessage(ChatColor.GOLD + "Escopeta: " + ChatColor.GRAY + "ferro, madeira e redstone.");
        player.sendMessage(ChatColor.GOLD + "Rifle: " + ChatColor.GRAY + "ferro, cobre, redstone e diamante.");
        player.sendMessage(ChatColor.GOLD + "Sniper: " + ChatColor.GRAY + "luneta, ferro, redstone e diamante.");
        player.sendMessage(ChatColor.GRAY + "Ataque dispara. Secundario ou soltar item recarrega.");
        player.sendMessage(ChatColor.DARK_GRAY + "Na sniper: agache e use o secundario para recarregar.");
        player.sendMessage(ChatColor.DARK_GRAY + "No Java, associe 'Soltar item' a tecla R se preferir.");
        player.sendMessage(ChatColor.GRAY + "As receitas aparecem no livro da bancada.");
    }

    private void setHome(Player player) {
        Location location = player.getLocation();
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(homeWorldKey, PersistentDataType.STRING, location.getWorld().getUID().toString());
        data.set(homeXKey, PersistentDataType.DOUBLE, location.getX());
        data.set(homeYKey, PersistentDataType.DOUBLE, location.getY());
        data.set(homeZKey, PersistentDataType.DOUBLE, location.getZ());
        data.set(homeYawKey, PersistentDataType.FLOAT, location.getYaw());
        data.set(homePitchKey, PersistentDataType.FLOAT, location.getPitch());
        player.sendMessage(ChatColor.GREEN + "Casa salva.");
    }

    private void teleportHome(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        String worldId = data.get(homeWorldKey, PersistentDataType.STRING);
        Double x = data.get(homeXKey, PersistentDataType.DOUBLE);
        Double y = data.get(homeYKey, PersistentDataType.DOUBLE);
        Double z = data.get(homeZKey, PersistentDataType.DOUBLE);
        if (worldId == null || x == null || y == null || z == null) {
            player.sendMessage(ChatColor.RED + "Voce ainda nao usou /sethome.");
            return;
        }
        World world;
        try {
            world = Bukkit.getWorld(UUID.fromString(worldId));
        } catch (IllegalArgumentException exception) {
            world = null;
        }
        if (world == null) {
            player.sendMessage(ChatColor.RED + "O mundo da sua casa nao esta carregado.");
            return;
        }
        float yaw = data.getOrDefault(homeYawKey, PersistentDataType.FLOAT, 0.0f);
        float pitch = data.getOrDefault(homePitchKey, PersistentDataType.FLOAT, 0.0f);
        player.teleport(new Location(world, x, y, z, yaw, pitch));
        player.sendMessage(ChatColor.GREEN + "Voce voltou para casa.");
    }

    private void giveTestKit(Player player) {
        for (WeaponType weapon : WeaponType.values()) {
            player.getInventory().addItem(createWeapon(weapon));
            player.getInventory().addItem(createAmmo(weapon.ammoId, weapon.magazineSize * 3));
        }
        player.sendMessage(ChatColor.GREEN + "Kit de teste entregue.");
    }

    private void configureFoodValues() {
        food(Material.BREAD, 18, 2, 1, 1, 2);
        food(Material.BAKED_POTATO, 14, 2, 1, 4, 3);
        food(Material.COOKED_BEEF, 2, 22, 12, 1, 1);
        food(Material.COOKED_CHICKEN, 1, 18, 6, 1, 1);
        food(Material.COOKED_COD, 1, 16, 5, 3, 2);
        food(Material.APPLE, 8, 1, 0, 15, 8);
        food(Material.CARROT, 7, 1, 0, 13, 7);
        food(Material.GOLDEN_CARROT, 10, 2, 3, 24, 8);
        food(Material.MELON_SLICE, 6, 0, 0, 8, 14);
        food(Material.PUMPKIN_PIE, 20, 3, 8, 6, 2);
        food(Material.HONEY_BOTTLE, 18, 0, 0, 2, 10);
        food(Material.MUSHROOM_STEW, 10, 5, 4, 10, 15);
        food(Material.RABBIT_STEW, 12, 20, 8, 12, 18);
    }

    private void food(Material material, double carbs, double protein, double fat, double vitamins, double water) {
        foodValues.put(material, new double[] {carbs, protein, fat, vitamins, water});
    }

    private void registerRecipes() {
        registerWeaponRecipe("pistol", WeaponType.PISTOL, new String[] {" II", "RFI", " I "}, Map.of('I', Material.IRON_INGOT, 'R', Material.REDSTONE, 'F', Material.FLINT));
        registerWeaponRecipe("smg", WeaponType.SMG, new String[] {"III", "RFC", " II"}, Map.of('I', Material.IRON_INGOT, 'R', Material.REDSTONE, 'F', Material.FLINT, 'C', Material.COPPER_INGOT));
        registerWeaponRecipe("shotgun", WeaponType.SHOTGUN, new String[] {"III", "RFW", " WW"}, Map.of('I', Material.IRON_INGOT, 'R', Material.REDSTONE, 'F', Material.FLINT, 'W', Material.OAK_PLANKS));
        registerWeaponRecipe("rifle", WeaponType.RIFLE, new String[] {"IID", "RCI", " II"}, Map.of('I', Material.IRON_INGOT, 'R', Material.REDSTONE, 'C', Material.COPPER_INGOT, 'D', Material.DIAMOND));
        registerWeaponRecipe("sniper", WeaponType.SNIPER, new String[] {"SID", "RII", "  I"}, Map.of('S', Material.SPYGLASS, 'I', Material.IRON_INGOT, 'R', Material.REDSTONE, 'D', Material.DIAMOND));

        registerAmmoRecipe("light_ammo", "light_ammo", 12, new String[] {" C ", " I ", " G "}, Map.of('C', Material.COPPER_INGOT, 'I', Material.IRON_NUGGET, 'G', Material.GUNPOWDER));
        registerAmmoRecipe("shell", "shell", 6, new String[] {" P ", " C ", " G "}, Map.of('P', Material.PAPER, 'C', Material.COPPER_INGOT, 'G', Material.GUNPOWDER));
        registerAmmoRecipe("rifle_ammo", "rifle_ammo", 10, new String[] {" C ", " I ", "GGG"}, Map.of('C', Material.COPPER_INGOT, 'I', Material.IRON_INGOT, 'G', Material.GUNPOWDER));
        registerAmmoRecipe("sniper_ammo", "sniper_ammo", 5, new String[] {" I ", " C ", "GDG"}, Map.of('I', Material.IRON_INGOT, 'C', Material.COPPER_INGOT, 'G', Material.GUNPOWDER, 'D', Material.DIAMOND));
    }

    private void registerWeaponRecipe(String id, WeaponType weapon, String[] shape, Map<Character, Material> ingredients) {
        ShapedRecipe recipe = new ShapedRecipe(key(id), createWeapon(weapon)).shape(shape);
        ingredients.forEach(recipe::setIngredient);
        Bukkit.addRecipe(recipe);
    }

    private void registerAmmoRecipe(String recipeId, String ammoId, int amount, String[] shape, Map<Character, Material> ingredients) {
        ShapedRecipe recipe = new ShapedRecipe(key(recipeId), createAmmo(ammoId, amount)).shape(shape);
        ingredients.forEach(recipe::setIngredient);
        Bukkit.addRecipe(recipe);
    }

    private NamespacedKey key(String id) {
        return new NamespacedKey(this, id);
    }

    private String ammoName(String id) {
        return switch (id) {
            case "light_ammo" -> "Municao 9mm";
            case "shell" -> "Cartucho calibre 12";
            case "rifle_ammo" -> "Municao de rifle";
            case "sniper_ammo" -> "Municao de precisao";
            default -> "Municao";
        };
    }
}

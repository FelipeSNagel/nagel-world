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
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
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

    private final Map<UUID, Long> lastShot = new HashMap<>();
    private final Set<UUID> reloading = new HashSet<>();
    private final Set<UUID> applyingWeaponDamage = new HashSet<>();
    private final Map<String, BlockProgress> blockDamage = new HashMap<>();
    private final Map<UUID, Double> lastTemperature = new HashMap<>();
    private final Map<UUID, Long> lastHordeDay = new HashMap<>();
    private final Map<Material, double[]> foodValues = new EnumMap<>(Material.class);

    private NamespacedKey weaponKey;
    private NamespacedKey ammoKey;
    private NamespacedKey magazineKey;
    private NamespacedKey zombieTypeKey;
    private NamespacedKey homeWorldKey;
    private NamespacedKey homeXKey;
    private NamespacedKey homeYKey;
    private NamespacedKey homeZKey;
    private NamespacedKey homeYawKey;
    private NamespacedKey homePitchKey;
    private final Map<Nutrient, NamespacedKey> nutrientKeys = new EnumMap<>(Nutrient.class);
    private int seasonLength;
    private int hordeInterval;
    private int hordeSize;
    private boolean zombiesBreakBlocks;
    private boolean zombiesSpawnInDaylight;
    private boolean friendlyFire;
    private int protectedSpawnRadius;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        seasonLength = Math.max(1, getConfig().getInt("season-length-days", 28));
        hordeInterval = Math.max(1, getConfig().getInt("horde-interval-days", 10));
        hordeSize = Math.max(1, getConfig().getInt("horde-size-per-player", 8));
        zombiesBreakBlocks = getConfig().getBoolean("zombies-break-blocks", true);
        zombiesSpawnInDaylight = getConfig().getBoolean("zombies-spawn-in-daylight", true);
        friendlyFire = getConfig().getBoolean("friendly-fire", false);
        protectedSpawnRadius = Math.max(0, getConfig().getInt("protected-spawn-radius", 12));

        weaponKey = new NamespacedKey(this, "weapon");
        ammoKey = new NamespacedKey(this, "ammo");
        magazineKey = new NamespacedKey(this, "magazine");
        zombieTypeKey = new NamespacedKey(this, "zombie_type");
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
        getServer().getScheduler().runTaskTimer(this, this::tickZombieBreaking, 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, this::tickHordes, 200L, 200L);
        getServer().getScheduler().runTaskTimer(this, this::tickZombieAtmosphere, 80L, 80L);
        for (World world : Bukkit.getWorlds()) {
            world.getEntitiesByClass(Monster.class).stream()
                .filter(monster -> !(monster instanceof Zombie))
                .forEach(Entity::remove);
            world.getEntitiesByClass(Zombie.class).forEach(this::configureZombie);
        }
        getLogger().info("Nagel Zombie Survival carregado: armas, nutricao, estacoes e hordas ativas.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        initializeNutrition(player);
        player.discoverRecipes(Arrays.asList(
            key("pistol"), key("shotgun"), key("rifle"), key("sniper"),
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
            fire(event.getPlayer(), event.getItem(), weapon);
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
            fire(event.getPlayer(), item, weapon);
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
            return;
        }
        event.setCancelled(true);
        fire(player, item, weapon);
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
        if (event.getEntity() instanceof Monster && !(event.getEntity() instanceof Zombie)) {
            if (isNaturalMonsterSpawn(event.getSpawnReason())) {
                Location location = event.getLocation().clone();
                event.setCancelled(true);
                getServer().getScheduler().runTask(this, () -> {
                    if (location.isWorldLoaded()) {
                        location.getWorld().spawn(location, Zombie.class);
                    }
                });
            }
            return;
        }
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (zombie.getType() != EntityType.ZOMBIE && isNaturalMonsterSpawn(event.getSpawnReason())) {
            Location location = event.getLocation().clone();
            event.setCancelled(true);
            getServer().getScheduler().runTask(this, () -> {
                if (location.isWorldLoaded()) location.getWorld().spawn(location, Zombie.class);
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

    @EventHandler
    public void onZombieDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) {
            return;
        }
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
            case "sethome" -> setHome(player);
            case "home" -> teleportHome(player);
            default -> { return false; }
        }
        return true;
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
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE,
            weapon == WeaponType.SNIPER ? 1.5f : 0.8f,
            weapon == WeaponType.SHOTGUN ? 0.65f : 1.35f);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.7)), 5, 0.04, 0.04, 0.04, 0.01);

        for (int pellet = 0; pellet < weapon.pellets; pellet++) {
            Vector direction = spread(player.getEyeLocation().getDirection(), weapon.spread);
            shootRay(player, direction, weapon);
        }
        attractZombies(player.getLocation(), weapon == WeaponType.SNIPER ? 64.0 : 42.0);

        int remaining = magazine - 1;
        player.sendActionBar(ChatColor.GOLD + weapon.displayName + ChatColor.GRAY + " [" + remaining + "/" + weapon.magazineSize + "]");
        if (remaining == 0) {
            getServer().getScheduler().runTaskLater(this, () -> reload(player, item, weapon), 4L);
        }
    }

    private void shootRay(Player player, Vector direction, WeaponType weapon) {
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
            return;
        }
        double damage = weapon.damage;
        double headY = target.getBoundingBox().getMaxY() - 0.35;
        if (result.getHitPosition().getY() >= headY) {
            damage *= 1.75;
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.8f);
            player.sendActionBar(ChatColor.RED + "HEADSHOT" + ChatColor.GRAY + " - " + String.format(Locale.US, "%.1f", damage) + " dano");
        }
        UUID shooterId = player.getUniqueId();
        applyingWeaponDamage.add(shooterId);
        try {
            target.damage(damage, player);
        } finally {
            applyingWeaponDamage.remove(shooterId);
        }
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 4, 0.2, 0.3, 0.2, 0.05);
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
        if (!zombiesBreakBlocks) {
            return;
        }
        long now = System.currentTimeMillis();
        blockDamage.entrySet().removeIf(entry -> now - entry.getValue().lastHit > 8000L);
        for (World world : Bukkit.getWorlds()) {
            for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
                if (!(zombie.getTarget() instanceof Player target) || zombie.getLocation().distanceSquared(target.getLocation()) > 16.0) {
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
                progress.damage += "brute".equals(type) ? 2.5 : 1.0;
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
        Location spawn = block.getWorld().getSpawnLocation();
        boolean protectedSpawn = protectedSpawnRadius > 0
            && Math.abs(block.getX() - spawn.getBlockX()) <= protectedSpawnRadius
            && Math.abs(block.getZ() - spawn.getBlockZ()) <= protectedSpawnRadius;
        return !protectedSpawn
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
                || lastHordeDay.getOrDefault(world.getUID(), -1L) == day) {
                continue;
            }
            lastHordeDay.put(world.getUID(), day);
            for (Player player : world.getPlayers()) {
                spawnHorde(player);
            }
        }
    }

    private void tickDaylightZombies() {
        if (!zombiesSpawnInDaylight) return;
        for (World world : Bukkit.getWorlds()) {
            long time = world.getTime();
            if (time >= 12000 || world.getEnvironment() != World.Environment.NORMAL) continue;
            for (Player player : world.getPlayers()) {
                if (player.getGameMode() != GameMode.SURVIVAL) continue;
                int nearby = 0;
                for (Entity entity : world.getNearbyEntities(player.getLocation(), 32, 16, 32)) {
                    if (entity instanceof Zombie) nearby++;
                }
                if (nearby >= 5 || ThreadLocalRandom.current().nextDouble() > 0.45) continue;
                double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
                double distance = ThreadLocalRandom.current().nextDouble(20.0, 30.0);
                int x = player.getLocation().getBlockX() + (int) (Math.cos(angle) * distance);
                int z = player.getLocation().getBlockZ() + (int) (Math.sin(angle) * distance);
                int y = world.getHighestBlockYAt(x, z) + 1;
                Zombie zombie = world.spawn(new Location(world, x + 0.5, y, z + 0.5), Zombie.class);
                zombie.setTarget(player);
            }
        }
    }

    private void spawnHorde(Player player) {
        World world = player.getWorld();
        player.sendTitle(ChatColor.DARK_RED + "A HORDA CHEGOU", ChatColor.GRAY + "Defenda seu abrigo", 10, 70, 20);
        for (int i = 0; i < hordeSize; i++) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
            double distance = ThreadLocalRandom.current().nextDouble(18.0, 28.0);
            int x = player.getLocation().getBlockX() + (int) (Math.cos(angle) * distance);
            int z = player.getLocation().getBlockZ() + (int) (Math.sin(angle) * distance);
            int y = world.getHighestBlockYAt(x, z) + 1;
            Zombie zombie = world.spawn(new Location(world, x + 0.5, y, z + 0.5), Zombie.class);
            zombie.setTarget(player);
        }
    }

    private void configureZombie(Zombie zombie) {
        PersistentDataContainer data = zombie.getPersistentDataContainer();
        String type = data.get(zombieTypeKey, PersistentDataType.STRING);
        if (type == null) {
            double roll = ThreadLocalRandom.current().nextDouble();
            type = roll < 0.18 ? "shambler"
                : roll < 0.53 ? "walker"
                : roll < 0.75 ? "stalker"
                : roll < 0.91 ? "runner"
                : roll < 0.98 ? "brute"
                : "infected";
            data.set(zombieTypeKey, PersistentDataType.STRING, type);
        }
        applyZombieProfile(zombie, type);
        zombie.setCustomNameVisible(false);
        zombie.setCanBreakDoors(true);
        zombie.setShouldBurnInDay(false);
        zombie.setFireTicks(0);
        zombie.setRemoveWhenFarAway(true);
    }

    private void applyZombieProfile(Zombie zombie, String type) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        setAttribute(zombie, Attribute.FOLLOW_RANGE, 40.0);
        setAttribute(zombie, Attribute.ATTACK_DAMAGE, 4.0);
        setAttribute(zombie, Attribute.KNOCKBACK_RESISTANCE, 0.0);
        setAttribute(zombie, Attribute.MAX_HEALTH, 20.0);
        zombie.setHealth(Math.min(zombie.getHealth(), 20.0));
        switch (type) {
            case "shambler" -> {
                setAttribute(zombie, Attribute.MOVEMENT_SPEED, random.nextDouble(0.13, 0.18));
                zombie.setCustomName(ChatColor.DARK_GRAY + "Cambaleante");
            }
            case "stalker" -> {
                setAttribute(zombie, Attribute.MOVEMENT_SPEED, random.nextDouble(0.26, 0.31));
                setAttribute(zombie, Attribute.FOLLOW_RANGE, 52.0);
                zombie.setCustomName(ChatColor.GOLD + "Cacador");
            }
            case "runner" -> {
                setAttribute(zombie, Attribute.MOVEMENT_SPEED, random.nextDouble(0.34, 0.40));
                setAttribute(zombie, Attribute.MAX_HEALTH, 16.0);
                zombie.setHealth(Math.min(zombie.getHealth(), 16.0));
                zombie.setCustomName(ChatColor.YELLOW + "Corredor");
            }
            case "brute" -> {
                setAttribute(zombie, Attribute.MOVEMENT_SPEED, random.nextDouble(0.18, 0.22));
                setAttribute(zombie, Attribute.MAX_HEALTH, 52.0);
                setAttribute(zombie, Attribute.ATTACK_DAMAGE, 9.0);
                setAttribute(zombie, Attribute.KNOCKBACK_RESISTANCE, 0.65);
                zombie.setHealth(Math.max(zombie.getHealth(), 52.0));
                zombie.setCustomName(ChatColor.DARK_RED + "Brutamontes");
            }
            case "infected" -> {
                setAttribute(zombie, Attribute.MOVEMENT_SPEED, random.nextDouble(0.29, 0.34));
                setAttribute(zombie, Attribute.MAX_HEALTH, 32.0);
                setAttribute(zombie, Attribute.FOLLOW_RANGE, 64.0);
                zombie.setHealth(Math.max(zombie.getHealth(), 32.0));
                zombie.setCustomName(ChatColor.RED + "Infectado Alfa");
            }
            default -> {
                setAttribute(zombie, Attribute.MOVEMENT_SPEED, random.nextDouble(0.19, 0.25));
                zombie.setCustomName(ChatColor.GRAY + "Errante");
            }
        }
    }

    private void tickZombieAtmosphere() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (World world : Bukkit.getWorlds()) {
            for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
                zombie.setShouldBurnInDay(false);
                if (zombie.getFireTicks() > 0) zombie.setFireTicks(0);
                Player nearby = nearestPlayer(zombie.getLocation(), 28.0);
                if (nearby == null) continue;
                if (zombie.getTarget() == null && random.nextDouble() < 0.35) zombie.setTarget(nearby);
                if (random.nextDouble() < 0.12) {
                    String type = zombie.getPersistentDataContainer().get(zombieTypeKey, PersistentDataType.STRING);
                    float pitch = "runner".equals(type) ? 1.25f : "brute".equals(type) ? 0.62f : random.nextFloat(0.78f, 1.08f);
                    world.playSound(zombie.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1.1f, pitch);
                }
            }
        }
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
            ChatColor.DARK_GRAY + "Secundario ou soltar item: recarregar"
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

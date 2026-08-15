package com.nagelworld;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class NagelAliasesPlugin extends JavaPlugin implements CommandExecutor, Listener {
  private static final double MARKET_X = 0.5;
  private static final double MARKET_Y = 90.0;
  private static final double MARKET_Z = 0.5;
  private final Set<UUID> coordinatesEnabled = new HashSet<>();

  @Override
  public void onEnable() {
    for (String command : Arrays.asList(
        "mercado",
        "loja",
        "vender",
        "vendermao",
        "vendertudo",
        "venderinventario",
        "preco",
        "saldo",
        "dinheiro",
        "pagar",
        "coords",
        "coordenadas")) {
      if (getCommand(command) != null) {
        getCommand(command).setExecutor(this);
      }
    }

    getServer().getPluginManager().registerEvents(this, this);
    Bukkit.getScheduler().runTaskTimer(this, this::updateCoordinates, 0L, 10L);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    String name = command.getName().toLowerCase(Locale.ROOT);

    switch (name) {
      case "mercado":
      case "loja":
        return teleportToMarket(sender);
      case "vender":
        return dispatch(sender, sellTarget(args));
      case "vendermao":
        return dispatch(sender, "sell hand");
      case "vendertudo":
        return dispatch(sender, "sell all");
      case "venderinventario":
        return dispatch(sender, "sell inventory");
      case "preco":
        return dispatch(sender, "worth" + suffix(args));
      case "saldo":
      case "dinheiro":
        return dispatch(sender, "balance" + suffix(args));
      case "pagar":
        return dispatch(sender, "pay" + suffix(args));
      case "coords":
      case "coordenadas":
        return toggleCoordinates(sender);
      default:
        return false;
    }
  }

  private boolean toggleCoordinates(CommandSender sender) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("Use este comando dentro do jogo.");
      return true;
    }

    Player player = (Player) sender;
    if (coordinatesEnabled.remove(player.getUniqueId())) {
      player.sendActionBar("");
      player.sendMessage(ChatColor.YELLOW + "Coordenadas desativadas.");
      return true;
    }

    coordinatesEnabled.add(player.getUniqueId());
    showCoordinates(player);
    player.sendMessage(ChatColor.GREEN + "Coordenadas ativadas. Use /coords para ocultar.");
    return true;
  }

  private void updateCoordinates() {
    for (UUID playerId : coordinatesEnabled) {
      Player player = Bukkit.getPlayer(playerId);
      if (player != null && player.isOnline()) {
        showCoordinates(player);
      }
    }
  }

  private void showCoordinates(Player player) {
    Location location = player.getLocation();
    String coordinates = String.format(
        Locale.ROOT,
        "%sX: %d  Y: %d  Z: %d",
        ChatColor.GOLD,
        location.getBlockX(),
        location.getBlockY(),
        location.getBlockZ());
    player.sendActionBar(coordinates);
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    coordinatesEnabled.remove(event.getPlayer().getUniqueId());
  }

  private boolean teleportToMarket(CommandSender sender) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("Use este comando dentro do jogo.");
      return true;
    }

    Player player = (Player) sender;
    World world = marketWorld();
    Location location = new Location(world, MARKET_X, MARKET_Y, MARKET_Z, 180.0f, 0.0f);
    player.teleport(location);
    player.sendMessage("Teleportado para o mercado.");
    return true;
  }

  private World marketWorld() {
    String configured = System.getenv("MARKET_WORLD");
    if (configured != null && !configured.isBlank() && Bukkit.getWorld(configured) != null) {
      return Bukkit.getWorld(configured);
    }
    return Bukkit.getWorlds().get(0);
  }

  private String sellTarget(String[] args) {
    if (args.length == 0) {
      return "sell hand";
    }

    String first = args[0].toLowerCase(Locale.ROOT);
    if (first.equals("tudo") || first.equals("all")) {
      return "sell all";
    }
    if (first.equals("inventario") || first.equals("inv") || first.equals("inventory")) {
      return "sell inventory";
    }
    if (first.equals("mao") || first.equals("hand")) {
      return "sell hand";
    }
    return "sell " + String.join(" ", args);
  }

  private boolean dispatch(CommandSender sender, String command) {
    Bukkit.dispatchCommand(sender, command);
    return true;
  }

  private String suffix(String[] args) {
    if (args.length == 0) {
      return "";
    }
    return " " + Arrays.stream(args).collect(Collectors.joining(" "));
  }
}

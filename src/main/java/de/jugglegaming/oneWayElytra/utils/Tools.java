package de.jugglegaming.oneWayElytra.utils;

import de.jugglegaming.oneWayElytra.OneWayElytra;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Tools {

  private final OneWayElytra oneWayElytra;

  public Tools(OneWayElytra oneWayElytra) {
    this.oneWayElytra = oneWayElytra;
  }

  public String replaceVariables(String text) {
    return translate(text);
  }

  public String replaceVariables(String text, String location) {
    return translate(text)
            .replace("%location%", String.valueOf(location));
  }

  public String replaceVariables(String text, Integer integer) {
    return translate(text)
            .replace("%radius%", integer + "")
            .replace("%multiplier%", integer + "")
            .replace("%boostMultiplier%", integer + "")
            .replace("%boostmultiplier%", integer + "");
  }

  public String replaceVariables(String text, Integer integer, String location) {
    return translate(text)
            .replace("%radius%", integer + "")
            .replace("%multiplier%", integer + "")
            .replace("%boostMultiplier%", integer + "")
            .replace("%boostmultiplier%", integer + "")
            .replace("%location%", String.valueOf(location));
  }

  public String replaceVariables(Player player, String text) {
    return translate(text)
            .replace("%player%", player.getName());
  }

  public String replaceVariables(String text, Double inputDouble) {
    return translate(text)
            .replace("%money%", inputDouble.toString())
            .replace("%multiplier%", inputDouble + "")
            .replace("%boostMultiplier%", inputDouble + "")
            .replace("%boostmultiplier%", inputDouble + "");
  }

  public String replaceVariables(Player player, String text, Double money) {
    return translate(text)
            .replace("%money%", money.toString())
            .replace("%player%", player.getName());
  }

  public String replaceVariables(String text, int time) {
    return translate(text)
            .replace("%time%", time + "");
  }

  private String translate(String text) {
    return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text)
            .replace("%>>%", "»")
            .replace("%<<%", "«")
            .replace("%ue%", "ü")
            .replace("%ae%", "ä")
            .replace("%oe%", "ö")
            .replace("%ss%", "ß")
            .replace("%coin%", "⛂")
            .replace("%coins%", "⛃");
  }

  public boolean isInt(String s) {
    try {
      Integer.parseInt(s);
    } catch (NumberFormatException nfe) {
      return false;
    }
    return true;
  }

  public boolean isDouble(String string) {
    try {
      Double.parseDouble(string);
    } catch (NumberFormatException nfe) {
      return false;
    }
    return true;
  }

}

package com.github.intangir.RadioChat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class Radio implements Comparable<Radio>
{
	public static Map<Location, Radio> allRadios = new HashMap<Location, Radio>();
	private static boolean dirty = false;
	public static RadioChat plugin;
	public static Location fromLoc = null;
	
	// statics/helpers/utilities
	
	public static void SaveRadios()
	{
		if(!dirty)
			return;
		dirty = false;
		
		YamlConfiguration config = new YamlConfiguration();
		int counter = 0;
		for (Radio radio : allRadios.values()) {
			ConfigurationSection section = config.createSection(counter + "");
			radio.save(section);
			counter++;
		}
		
		File file = new File(plugin.getDataFolder(), "save.yml");
		if (file.exists()) {
			file.delete();
		}
		try {
			config.save(file);
			plugin.log.info("Saved radio data");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void LoadRadios()
	{
		File file = new File(plugin.getDataFolder(), "save.yml");
		if (!file.exists()) return;
		
		YamlConfiguration config = new YamlConfiguration();
		try {
			config.load(file);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		Set<String> keys = config.getKeys(false);
		for (String key : keys) {
			ConfigurationSection section = config.getConfigurationSection(key);
			Radio radio = new Radio(section);
			if(radio != null)
			{
				allRadios.put(radio.getLocation(), radio);
			}
		}
	}
	
	public static void UpdateRadio(Location loc, int range, String name, String message)
	{
		allRadios.put(loc, new Radio(loc, range, name));
		dirty = true;
	}
	
	public static void DeleteRadio(Location loc)
	{
		allRadios.remove(loc);
		dirty = true;
	}
	
	public static List<Radio> FindNearbyRadios(Location loc)
	{
		List<Radio> nearbyRadios = new ArrayList<Radio>();
		
		for (Radio radio : allRadios.values()) {
			if(radio.inRange(loc))
			{
				nearbyRadios.add(radio);
			}
		}
		
		return nearbyRadios;
	}
	
	public static Radio ScanTuneRadio(Location playerLoc, int change, Location tuneLoc)
	{
		List<Radio> nearby = FindNearbyRadios(playerLoc);
		fromLoc = playerLoc;
		Collections.sort(nearby);
		
		if(nearby.isEmpty())
		{
			return null;
		}
		else
		{
			int size = nearby.size();
			//plugin.log.info("nearby radios: " + size);
			int index = nearby.indexOf(GetRadioAt(tuneLoc)) + change;
			index = ((index % size) + size) % size;
			return nearby.get(index);
		}
	}
	
	public static Radio GetRadioAt(Location loc)
	{
		return allRadios.get(loc);
	}

	//----------------------------------------------------//
	// members
	protected Location location;
	protected String name;
	protected String message;
	protected int range;
	protected double rangeSquared;
	protected boolean powered;
	
	
	public Radio(Location _location, int _range, String _name) {
		location = _location;
		range = _range;
		name = _name;
		rangeSquared = (double)_range * _range;
	}
	
	public Radio(ConfigurationSection config) {
		load(config);
	}

	public boolean inRange(Location loc)
	{
		if(!location.getWorld().getName().equals(loc.getWorld().getName()))
		{
			return false;
		}
		return location.distanceSquared(loc) < rangeSquared;
	}
	
	public Location getLocation()
	{
		return location;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setPowered(boolean _powered)
	{
		powered = _powered;
	}
	
	public void save(ConfigurationSection config) {
		config.set("world", location.getWorld().getName());
		config.set("x", location.getBlockX());
		config.set("y", location.getBlockY());
		config.set("z", location.getBlockZ());
		config.set("name", name);
		config.set("message", message);
		config.set("range", range);
	}
	
	public void load(ConfigurationSection config) {
		location = new Location(
				plugin.getServer().getWorld(config.getString("world")),
				config.getInt("x"),
				config.getInt("y"),
				config.getInt("z"));
		name = config.getString("name");
		message = config.getString("message");
		range = config.getInt("range");
		rangeSquared = (double) range * range;
	}
	
	public int compareTo(Radio anotherRadio)
	{
	    int anotherDistance = (int)(anotherRadio).getLocation().distanceSquared(fromLoc);
	    int thisDistance = (int)getLocation().distanceSquared(fromLoc);
	    return thisDistance - anotherDistance;    
	  }
}

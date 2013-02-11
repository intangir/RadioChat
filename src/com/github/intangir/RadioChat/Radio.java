package com.github.intangir.RadioChat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class Radio implements Comparable<Radio>
{
	// statics/helpers/utilities

	final static int maxHeight = 250;
	final static int minHeight = 4;
	final static int seaLevel = 65;
	final static int rangePerHeight = 50;
	
	public static Map<Location, Radio> allRadios = new HashMap<Location, Radio>();
	private static boolean dirty = false;
	public static RadioChat plugin;
	public static Location fromLoc = null;

	// key allRadios without Y
	public static Location KeyXZ(Location loc)
	{
		Location newLoc = loc.clone();
		return newLoc.subtract(0, newLoc.getY(), 0);
	}

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
				allRadios.put(KeyXZ(radio.getLocation()), radio);
			}
		}
	}
	
	public static int MeasureHeight(Location _loc)
	{
		Material m = _loc.getBlock().getType();
		if(m != Material.GOLD_BLOCK && m != Material.IRON_BLOCK)
		{
			return 0;
		}

		Location loc = _loc.clone();
		int height = 0;
		while(loc.getBlockY() < maxHeight)
		{
			if(loc.add(0, 1, 0).getBlock().getType() == Material.IRON_FENCE)
			{
				height++;
			}
			else
			{
				break;
			}
		}
		plugin.log.info("height = " + height);
		return height;
	}

	public static Location FindRadioBase(Location _loc)
	{
		Location loc = _loc.clone();
		plugin.log.info("looking for radio base");
		Material m;
		
		plugin.log.info("location " + loc.toString());
		
		while(loc.getBlockY() >= seaLevel)
		{
			m = loc.getBlock().getType();
			
			if(m == Material.IRON_FENCE)
			{
				plugin.log.info("found iron, looking lower");
				loc.subtract(0, 1, 0);
			}
			else if(m == Material.GOLD_BLOCK || m == Material.IRON_BLOCK)
			{
				plugin.log.info("found the base");
				return loc;
			}
			else
			{
				plugin.log.info("found non radio part");
				break;
			}
		}
		return null;
	}
	
	public static void UpdateRadio(Location loc, Player p)
	{
		plugin.log.info("updating radio");
		
		plugin.log.info("location check 1 " + loc.toString());
		
		Radio existing = GetRadioAt(loc);
		
		plugin.log.info("location check 2 " + loc.toString());
		
		int height = 0;
		if(existing != null)
		{
			plugin.log.info("checking existing radio");
			height = MeasureHeight(existing.getLocation());
			if(height >= minHeight)
			{
				plugin.log.info("updating radio height");
				if(existing.setHeight(height))
				{
					p.sendMessage(ChatColor.YELLOW + "Radio range changed to " + existing.getRange() + " meters.");
				}
			}
			else
			{
				plugin.log.info("removing broken radio");
				p.sendMessage(ChatColor.YELLOW + "Radio ruined.");
				DeleteRadio(existing.getLocation());
			}
			
		}
		else // no existing radio
		{
			// find radio base
			Radio newRadio = CreateRadio(FindRadioBase(loc)); 
			if(newRadio != null)
			{
				p.sendMessage(ChatColor.YELLOW + "Created new radio [" + newRadio.getName() + "] with a range of " + newRadio.getRange() + " meters.");
			}
		}
	}
	
	public static Radio CreateRadio(Location loc)
	{
		plugin.log.info("creating radio");
		if(loc == null)
		{
			plugin.log.info("bad location");
			return null;
		}

		Block base = loc.getBlock();
		Material m = base.getType();
		
		if(m != Material.GOLD_BLOCK && m != Material.IRON_BLOCK)
		{
			plugin.log.info("not radio base type");
			return null;
		}
		
		int height = MeasureHeight(loc);
		
		// must be at least minHeight
		if(height < minHeight)
		{
			plugin.log.info("not tall enough");
			return null;
		}

		Radio newRadio = new Radio(loc, 
								   height, 
								   base.isBlockPowered(), 
								   m == Material.GOLD_BLOCK, 
								   "unnamed", 
								   null);
		
		plugin.log.info("created new radio!!");
		
		allRadios.put(KeyXZ(loc), newRadio);
		dirty = true;
		
		return newRadio;
	}
	
	public static void DeleteRadio(Location loc)
	{
		allRadios.remove(KeyXZ(loc));
		dirty = true;
	}
	
	public static List<Radio> FindNearbyRadios(Location loc)
	{
		List<Radio> nearbyRadios = new ArrayList<Radio>();
		
		for (Radio radio : allRadios.values()) {
			if(radio.enabled() && radio.inRange(loc))
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
			int index = nearby.indexOf(GetRadioAt(tuneLoc)) + change;
			index = ((index % size) + size) % size;
			return nearby.get(index);
		}
	}
	
	public static Radio GetRadioAt(Location loc)
	{
		return allRadios.get(KeyXZ(loc));
	}

	//----------------------------------------------------//
	// members
	protected Location location;
	protected String name;
	protected String message;
	protected int height;
	protected int range;
	protected double rangeSquared;
	protected boolean powered;
	protected boolean twoway;
	
	
	public Radio(Location _location, int _height, boolean _powered, boolean _twoway, String _name, String _message) {
		location = _location;
		height = _height;
		range = height * rangePerHeight;
		rangeSquared = (double)range * range;
		twoway = _twoway;
		powered = _powered;
		name = _name;
		message = _message;
		powered = true;
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
	
	public boolean enabled()
	{
		return powered;
	}
	
	public Location getLocation()
	{
		return location;
	}
	
	public String getName()
	{
		return name;
	}
	
	public int getRange()
	{
		return range;
	}
	
	public void setPowered(boolean _powered)
	{
		powered = _powered;
		dirty = true;
	}
	
	public boolean setHeight(int _height)
	{
		if(height == _height)
			return false;
		
		height = _height;
		range = height * rangePerHeight;
		rangeSquared = (double)range * range;
		dirty = true;
		
		return true;
	}
	
	public void save(ConfigurationSection config) {
		config.set("world", location.getWorld().getName());
		config.set("x", location.getBlockX());
		config.set("y", location.getBlockY());
		config.set("z", location.getBlockZ());
		config.set("name", name);
		config.set("message", message);
		config.set("height", height);
		config.set("powered", powered);
		config.set("twoway", twoway);
	}
	
	public void load(ConfigurationSection config) {
		location = new Location(
				plugin.getServer().getWorld(config.getString("world")),
				config.getInt("x"),
				config.getInt("y"),
				config.getInt("z"));
		name = config.getString("name");
		message = config.getString("message");
		height = config.getInt("height");
		powered = config.getBoolean("powered");
		twoway = config.getBoolean("twoway");
		range = height * rangePerHeight;
		rangeSquared = (double) range * range;
	}
	
	public int compareTo(Radio anotherRadio)
	{
	    int anotherDistance = (int)(anotherRadio).getLocation().distanceSquared(fromLoc);
	    int thisDistance = (int)getLocation().distanceSquared(fromLoc);
	    return thisDistance - anotherDistance;    
	  }
}

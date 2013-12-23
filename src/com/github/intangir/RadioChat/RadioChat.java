package com.github.intangir.RadioChat;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Sign;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import static com.github.intangir.RadioChat.Radio.UpdateRadio;
import static com.github.intangir.RadioChat.Radio.UpdateSign;
import static com.github.intangir.RadioChat.Radio.ScanTuneRadio;
import static com.github.intangir.RadioChat.Radio.SaveRadios;
import static com.github.intangir.RadioChat.Radio.LoadRadios;
import static com.github.intangir.RadioChat.Radio.GetRadioAdjacent;


public class RadioChat extends JavaPlugin implements Listener
{
    public Logger log;
    public PluginDescriptionFile pdfFile;
    
	public void onEnable()
	{
		log = this.getLogger();
		pdfFile = this.getDescription();
		Radio.plugin = this;

		Bukkit.getPluginManager().registerEvents(this, this);
		
		log.info("v" + pdfFile.getVersion() + " enabled!");
		
		// wait for server and its worlds to be fully loaded before loading radios
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				LoadRadios();
			}
		}, 0);

		// start saver
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				SaveRadios();
			}
		}, 6000, 6000);
	}
	
	public void onDisable()
	{
		SaveRadios();
		log.info("v" + pdfFile.getVersion() + " disabled.");
	}

	public void ScheduleUpdate(final Location loc, final Player p)
	{
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				UpdateRadio(loc, p);
			}
		}, 1);
	}
	

	public void ScheduleSignUpdate(final Location loc, final Player p)
	{
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				UpdateSign(loc, p);
			}
		}, 1);
	}

	public void SchedulePowerUpdate(final Radio radio)
	{
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				radio.updatePowered();
			}
		}, 1);
	}

	@EventHandler(ignoreCancelled=true, priority = EventPriority.MONITOR)
	public void onBlockPlace(BlockPlaceEvent e)
	{
		Material m = e.getBlock().getType();
		if(m == Material.IRON_BLOCK || 
		   m == Material.GOLD_BLOCK ||
		   m == Material.IRON_FENCE)
		{
			ScheduleUpdate(e.getBlock().getLocation(), e.getPlayer());
		}
    }

	@EventHandler(ignoreCancelled=true, priority = EventPriority.MONITOR)
	public void onBlockBreak(BlockBreakEvent e)
	{
		Material m = e.getBlock().getType();
		
		switch(m)
		{
		case IRON_BLOCK:
		case GOLD_BLOCK:
		case IRON_FENCE:
			ScheduleUpdate(e.getBlock().getLocation(), e.getPlayer());
			break;
			
		case WALL_SIGN:
			ScheduleSignUpdate(e.getBlock().getRelative(((Sign)e.getBlock().getState().getData()).getAttachedFace()).getLocation(), e.getPlayer());
			break;
			
		case REDSTONE_WIRE:
		case WOOD_PLATE:
		case WOOD_BUTTON:
		case STONE_PLATE:
		case STONE_BUTTON:
		case LEVER:
		case DIODE:
		case REDSTONE_TORCH_ON:
			Radio radio = GetRadioAdjacent(e.getBlock().getLocation());
			if(radio != null)
			{
				SchedulePowerUpdate(radio);
			}
			break;
		default:
		}
	}

	@EventHandler(ignoreCancelled=true, priority = EventPriority.MONITOR)
	public void onSignChange(SignChangeEvent e)
	{
		ScheduleSignUpdate(e.getBlock().getRelative(((Sign)e.getBlock().getState().getData()).getAttachedFace()).getLocation(), e.getPlayer());
	}

	@EventHandler(ignoreCancelled=true, priority = EventPriority.MONITOR)
	public void onRedstoneEvent(BlockRedstoneEvent e)
	{
		Radio radio = GetRadioAdjacent(e.getBlock().getLocation());
		if(radio != null)
		{
			SchedulePowerUpdate(radio);
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e)
	{
		if(e.getMaterial() == Material.COMPASS)
		{
			int dir = (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) ? 1 : -1;
			Radio station = ScanTuneRadio(e.getPlayer().getLocation(), dir, e.getPlayer().getCompassTarget());
			
			if(station == null)
			{
				e.getPlayer().sendMessage(ChatColor.YELLOW + "No Radios in range.");
			}
			else
			{
				e.getPlayer().sendMessage(ChatColor.YELLOW + "[" + station.getName() + "] " + (int)station.getLocation().distance(e.getPlayer().getLocation()) + " meters away." );
				if(station.getMessage() != null)
				{
					e.getPlayer().sendMessage(ChatColor.YELLOW + "[" + station.getName() + "] \"" + station.getMessage() + "\"");
				}
				e.getPlayer().setCompassTarget(station.getLocation());
			}
		}
	}
}


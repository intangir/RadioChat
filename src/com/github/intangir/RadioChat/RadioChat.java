package com.github.intangir.RadioChat;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import static com.github.intangir.RadioChat.Radio.UpdateRadio;
import static com.github.intangir.RadioChat.Radio.DeleteRadio;
import static com.github.intangir.RadioChat.Radio.ScanTuneRadio;
import static com.github.intangir.RadioChat.Radio.SaveRadios;
import static com.github.intangir.RadioChat.Radio.LoadRadios;
import static com.github.intangir.RadioChat.Radio.GetRadioAt;


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
		
		LoadRadios();
		
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
	
	@EventHandler(ignoreCancelled=true)
	public void onBlockPlace(BlockPlaceEvent e)
	{
		if(e.getBlock().getType() == Material.IRON_BLOCK)
		{
			UpdateRadio(e.getBlock().getLocation(), 100, "new", "message");
			log.info("updated radio");
		}
    }

	@EventHandler(ignoreCancelled=true)
	public void onBlockBreak(BlockBreakEvent e)
	{
		if(e.getBlock().getType() == Material.IRON_BLOCK)
		{
			DeleteRadio(e.getBlock().getLocation());
			log.info("deleted radio");
		}
    }

	@EventHandler(ignoreCancelled=true)
	public void onPlayerInteract(PlayerInteractEvent e)
	{
		if(e.getMaterial() == Material.COMPASS)
		{
			Radio station = ScanTuneRadio(e.getPlayer().getLocation(), 1, e.getPlayer().getCompassTarget());
			
			if(station == null)
			{
				e.getPlayer().sendMessage(ChatColor.YELLOW + "No Radios in range.");
			}
			else
			{
				e.getPlayer().sendMessage(ChatColor.YELLOW + "[" + station.getName() + "] " + (int)station.getLocation().distance(e.getPlayer().getLocation()) + " meters away." );
				e.getPlayer().setCompassTarget(station.getLocation());
			}
			e.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled=true)
	public void onRedstoneEvent(BlockRedstoneEvent e)
	{
		if(e.getBlock().getType() == Material.IRON_BLOCK)
		{
			Radio radio = GetRadioAt(e.getBlock().getLocation());
			if(radio != null)
			{
				radio.setPowered(e.getBlock().isBlockPowered());
			}
		}
	}
}


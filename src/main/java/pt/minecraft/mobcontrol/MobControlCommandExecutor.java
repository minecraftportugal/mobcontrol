package pt.minecraft.mobcontrol;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pt.minecraft.mobcontrol.MobSpawnProfiler.MobSpawnProfilerException;

public class MobControlCommandExecutor implements CommandExecutor {
	
	public static interface ReportEndCallback
	{
		public void reportEnded();
	}
	
	private class ReportThread extends Thread implements ReportEndCallback {
	
		CommandSender sender;
		World world;
		MobSpawnProfiler profiler;
		
		public ReportThread(MobSpawnProfiler profiler, CommandSender sender, World world)
		{
			this.profiler = profiler;
			this.sender = sender;
			this.world = world;
		}
		
		@Override
		public void run()
		{
			profiler.buildReport(world, sender, MobControlCommandExecutor.this.plugin, this);
		}

		@Override
		public void reportEnded()
		{
			synchronized(MobControlCommandExecutor.this.rtMutex)
			{
				MobControlCommandExecutor.this.activeReportThread = null;
			}
		}
	}
	

	
	private MobControlPlugin plugin = null;
	
	
	public static final String COMMAND_FAMILY = "mobprofiler";
	private ReportThread activeReportThread = null;
	private final Object rtMutex = new Object();
	
	
	
	public MobControlCommandExecutor(MobControlPlugin plugin)
	{
		this.plugin = plugin;
	}
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		boolean wasValid = false;
		
		if(    plugin != null
			&& cmd.getName().equals(COMMAND_FAMILY) )
		{
			String doWhat = args[0].trim().toLowerCase();
			MobSpawnProfiler profiler = plugin.getProfilerInstance();
			
			try {
				
				if( args.length == 1 )
				{
					if( doWhat.equals("start") )
					{
						wasValid = true;
						profiler.start();
						
						Utils.sendMessage("{DARKGREEN}Profiler started.", sender);
						
						if( profiler.getElapsedTime() > 0 )
							Utils.sendMessage("{GRAY}Profiler was running before. You may want to reset it first and run this command again.", sender);
					}
					
					else
					if( doWhat.equals("stop") )
					{
						wasValid = true;
						profiler.stop();
						
						Utils.sendMessage("{DARKGREEN}Profiler stopped.", sender);
					}
					
					else
					if( doWhat.equals("clean") )
					{
						wasValid = true;
						profiler.clean();
						profiler = plugin.getProfilerInstance(true);
						
						Utils.sendMessage("{DARKGREEN}Profiler cleaned.", sender);
					}
					
					else
					if( doWhat.equals("reset") )
					{
						wasValid = true;
						profiler.clean();
						profiler = plugin.getProfilerInstance(true);
						profiler.start();
						
						Utils.sendMessage("{DARKGREEN}Profiler restarted.", sender);
					}
					
					else
					if( doWhat.equals("chunk") )
					{
						wasValid = true;
						
						if( sender instanceof Player )
						{
							World world = ((Player)sender).getWorld();
							Location loc = ((Player)sender).getLocation();
							
							if( world != null && loc != null )
								Utils.sendMessage(String.format("{BRIGHTGREEN}You are in chunk:{GOLD} %d{BRIGHTGREEN},{GOLD} %d{BRIGHTGREEN}, on world:{GOLD} %s",
										loc.getChunk().getX(), loc.getChunk().getZ(), world.getName()), sender);
						}
						else
							Utils.sendMessage("{RED}You cannot use this command from the console.", sender);
					}
					
					else
					if( doWhat.equals("report") )
					{
						_processReport(null, profiler, sender);
						wasValid = true;
					}
				}
				else
				if( args.length <= 2 )
				{
					if( doWhat.equals("report") )
					{
						_processReport(args[1], profiler, sender);
						wasValid = true;
					}
				}
				
				if( !wasValid )
					Utils.sendMessage("{RED}Invalid command.", sender);
				
			} catch(MobSpawnProfilerException e) {
				Utils.sendMessage(String.format("{RED}Error: %s", e.getMessage()), sender);
				
			}
		}
		return wasValid;
	}

	private void _processReport(String worldName, MobSpawnProfiler profiler, CommandSender sender)
	{
		
		World world = null;
		
		if( worldName != null )
			world = plugin.getServer().getWorld(worldName);
		
		else
		{
			if( sender instanceof Player )
			{
				world = ((Player)sender).getWorld();
				
				if( Utils.isDebug() )
				{
					Location loc = ((Player)sender).getLocation();
					if( loc != null )
						Utils.debug("User '%s' asked for a report on world: '%s', C.Pos: %d, %d", sender.getName(), world.getName(), loc.getChunk().getX(), loc.getChunk().getZ());
				}
			}
		}
		
		if( world == null )
		{
			Utils.sendMessage("{RED}Please provider world name.", sender);
			return;
		}
			
		synchronized(rtMutex)
		{
			if( activeReportThread != null )
			{
				Utils.sendMessage("{RED}There is a report already being built. Please wait.", sender);
				return;
			}
			
			activeReportThread = new ReportThread(profiler, sender, world);
			activeReportThread.start();
		}

	}

}

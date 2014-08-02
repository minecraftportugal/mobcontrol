package pt.minecraft.mobcontrol;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;


public class MobControlPlugin extends JavaPlugin {

	
	public static final String PLUGIN_NAME = "MobControl";
	
	
	private GroupDescriptorProvider provider = null;
	private MobSpawnListener listener = null;
	private MobSpawnProfiler profiler = null;
	
	@Override
	public void onEnable()
	{
		this.saveDefaultConfig();
		this.reloadConfig();
		//this.saveConfig();
		
		Utils.setDebug(this.getConfig().getBoolean("debug", false));
		
		
		provider = GroupDescriptorProvider.buildFromConfig(getConfig(), this);
		
		if( provider != null )
		{
			listener = new MobSpawnListener(this);
		
			this.getServer().getPluginManager().registerEvents(listener, this);
			
			this.getCommand(MobControlCommandExecutor.COMMAND_FAMILY).setExecutor(new MobControlCommandExecutor(this));
		}
		else
		{
			Utils.severe("Could not load configs correctly. aborting");
			
			this.getServer().getPluginManager().disablePlugin(this);
		}
		
	}
	
	@Override
	public void onDisable()
	{
		
		provider = null;
		listener = null;
		
		HandlerList.unregisterAll(this);
	}
	

	
	public MobSpawnProfiler getProfilerInstance()
	{
		return this.getProfilerInstance(false);
	}
	public MobSpawnProfiler getProfilerInstance(boolean forceNew)
	{
		if( this.profiler == null || forceNew )
			this.profiler = new MobSpawnProfiler(this);
		
		return this.profiler;
	}
	
	public GroupDescriptorProvider getGroupDescriptorProvider()
	{
		return provider;
	}
	
}

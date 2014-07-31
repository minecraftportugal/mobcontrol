package pt.minecraft.mobcontrol;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;


public class MobControlPlugin extends JavaPlugin {

	
	public static final String PLUGIN_NAME = "MobControl";
	
	
	private GroupDescriptorProvider provider = null;
	private MobSpawnListener listener = null;
	
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
	
	
	
	
	
	public GroupDescriptorProvider getGroupDescriptorProvider()
	{
		return provider;
	}
}

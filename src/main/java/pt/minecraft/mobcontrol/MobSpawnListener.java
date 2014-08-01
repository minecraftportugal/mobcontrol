package pt.minecraft.mobcontrol;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;


public class MobSpawnListener implements Listener {
	
	
	private MobControlPlugin plugin = null;
	private GroupDescriptorProvider provider = null;
	private Random randomGen = null;
	
	public MobSpawnListener(MobControlPlugin plugin)
	{
		this.plugin = plugin;
		
		if( this.plugin != null )
			this.provider = plugin.getGroupDescriptorProvider();
		
		if( this.provider == null )
			Utils.severe("Group Descriptor provider not found");
		
		randomGen = new Random(System.currentTimeMillis());
	}
	

	@EventHandler(priority=EventPriority.LOW )
	public void onEntitySpawn(CreatureSpawnEvent event)
	{
		if(    event == null
			|| provider == null )
			return;
		
		if( event.isCancelled() )
			return;
		
		// Only process natural spawns
		switch(event.getSpawnReason())
		{
		case NATURAL:
		case SPAWNER:
			
			Location loc = event.getLocation();
			Chunk chunk;
			double rate;
			boolean cancel = false;
			EntityType type = event.getEntityType();
			MobSpawnProfiler profiler = plugin.getProfilerInstance();
			
			if(     loc != null
				&& (chunk = loc.getChunk()) != null )
			{
				rate = provider.getRate(chunk.getX(), chunk.getZ(), chunk.getWorld(), type);
				
				if( rate <= 0.0 )
					cancel = true;
				
				else
				if( rate < 1.0 )
					cancel = ( randomGen.nextDouble() > rate );
				
				if( cancel )
					event.setCancelled( true );
				
				if( profiler.isValid() )
				{
					if( cancel )
						profiler.incPreventedMobSpawn(chunk.getWorld());
					else
						profiler.addMobSpawn(chunk.getX(), chunk.getZ(), chunk.getWorld(), type);
				}

				if( Utils.isDebug() )
					Utils.debug("Entity: '%s' on chunk[%s, %d, %d] found on group: '%s' with rate: %f, cancelled: %s",
								type.toString(), chunk.getWorld().getName(),
								chunk.getX(), chunk.getZ(), provider.getLastIdent(), rate, ( cancel ? "yes" : "no" ) );
			}
			
			break;
		default:
			break;

		}
	}

}

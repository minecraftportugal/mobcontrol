package pt.minecraft.mobcontrol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import pt.minecraft.mobcontrol.GroupDescriptorProvider.ChunkLocation;
import pt.minecraft.mobcontrol.MobControlCommandExecutor.ReportEndCallback;

public class MobSpawnProfiler {
	
	public class MobSpawnProfilerException extends Exception
	{
		private static final long serialVersionUID = -1321308266169251285L;

		public MobSpawnProfilerException(String msg)
		{
			super(msg);
		}
	}
	
	private static class Pair<K,V> {
		K k;
		V v;
		public Pair(K k, V v)
		{
			this.k = k;
			this.v = v;
		}
	}
	
	private static class ChunkInfo implements Comparable<ChunkInfo>
	{
		ChunkLocation cLoc = null;
		EntityMap eMap = null;
		long totalSpawns = 0;
		LinkedList<Pair<EntityType, Long>> mostCommon = new LinkedList<Pair<EntityType, Long>>();
		
		
		@Override
		public int compareTo(ChunkInfo o)
		{
			if( o == null )
				return -Integer.MIN_VALUE;
			
			return (int) (o.totalSpawns - this.totalSpawns);
			
//			if( this.totalSpawns > o.totalSpawns )
//				return -1;
//			
//			if( this.totalSpawns < o.totalSpawns )
//				return -1;
//			
//			return 0;
		}
	}
	
	private static class EntityMap extends HashMap<EntityType, Long>
	{
		private static final long serialVersionUID = 8080580692746837630L;

		public long incrementEntity(EntityType type)
		{
			Long val = this.get(type);
			
			if( val == null )
				val = 1L;
			else
				val = val + 1;
			
			synchronized(this)
			{
				this.put(type, val);
			}
			
			return val;
		}
		
//		public long getEntityValue(EntityType type)
//		{
//			Long l = this.get(type);
//			
//			return ( l == null ) ? 0L : l ;
//		}
	}
	
	
	private boolean running = false;

	private HashMap<String, Long> preventedMap = new HashMap<String, Long>();
	private long timeStart = 0;
	private long elapsedTime = 0;
	private HashMap<String, HashMap<ChunkLocation, EntityMap>>
						worldMap = new HashMap<String, HashMap<ChunkLocation, EntityMap>>();
	
	private static final int MAXIMUM_ENTITY_MOST_LIST = 3;
	private static final int REPORT_CHUNK_PAGE_SIZE = 10;
	
	
	public boolean isValid()
	{
		return running;
	}
	
	
	public void start() throws MobSpawnProfilerException
	{
		if ( running )
			throw new MobSpawnProfilerException("Profiler already started");
		
//		if ( ended )
//			throw new MobSpawnProfilerException("Profiler has ended");
		
		running = true;
		timeStart = System.currentTimeMillis();
	}
	
	public void stop() throws MobSpawnProfilerException
	{		
		if ( !running )
			throw new MobSpawnProfilerException("Profiler is not running");
		
		running = false;
		elapsedTime = System.currentTimeMillis() - timeStart;
	}
	
	
	public void incPreventedMobSpawn(World world)
	{
		if(   !running
			|| world == null )
			return;
		
		Long val = preventedMap.get(world.getName());
		
		preventedMap.put(world.getName(), val == null ? 1L : ( val + 1L)  );
	}
	
	
	public void addMobSpawn(long chunkX, long chunkZ, World world, EntityType type)
	{
		if(    !running
			|| world == null )
			return;

		EntityMap chunkEntities = null;
		ChunkLocation cLoc = new ChunkLocation(chunkX, chunkZ);
		HashMap<ChunkLocation, EntityMap> chunkList = worldMap.get(world.getName());
		
		if( chunkList == null )
		{
			chunkList = new HashMap<ChunkLocation, EntityMap>();
			worldMap.put(world.getName(), chunkList);
		}
		
		chunkEntities = chunkList.get(cLoc);
		
		if( chunkEntities == null )
		{
			chunkEntities = new EntityMap();
			
			synchronized( chunkList )
			{
				chunkList.put(cLoc, chunkEntities);
			}
		}
		
		chunkEntities.incrementEntity(type);
	}
	
	public long getElapsedTime()
	{
		return this.elapsedTime;
	}
	
	
	
	public void buildReport(World world, final CommandSender sender, JavaPlugin plugin, ReportEndCallback endCallback)
	{
		long totalPrevented = 0L;
		long totalElapsed = 0L;
		boolean success = false;
		final ArrayList<ChunkInfo> cacheList = new ArrayList<ChunkInfo>();
		String worldName = null;
		long totalSpawns = 0L;
		
		try {
			
			if( world == null || sender == null )
				return;
			
			worldName = world.getName();
			HashMap<ChunkLocation, EntityMap> chunkList = worldMap.get(worldName);

			Pair<EntityType, Long> pair = null;
			int addToPos;
			
			if( chunkList != null )
			{
				synchronized( chunkList )
				{
					for(Entry<ChunkLocation, EntityMap> entry: chunkList.entrySet())
					{
						ChunkInfo cInfo = new ChunkInfo();
						cInfo.cLoc = entry.getKey();
						cInfo.eMap = entry.getValue();
						
						cacheList.add(cInfo);
					}
				}
				
				for( ChunkInfo cInfo : cacheList )
				{
					cInfo.totalSpawns = 0;
					
					if( cInfo.eMap == null )
						continue;
					
					synchronized(cInfo.eMap)
					{
						for(Entry<EntityType, Long> entry : cInfo.eMap.entrySet())
						{
							cInfo.totalSpawns += entry.getValue();
							totalSpawns += entry.getValue();
							
							addToPos = -1;
							
							if( cInfo.mostCommon.size() == 0 )
								addToPos = 0;
								
							else
							{
								for(int i = 0; i < cInfo.mostCommon.size() && i < MAXIMUM_ENTITY_MOST_LIST; i++)
								{
									pair = cInfo.mostCommon.get(i);
									
									if(    pair == null
										|| entry.getValue() > pair.v )
									{
										addToPos = i;
										break;
									}
								}
							}
							
							if( addToPos >= 0 )
								cInfo.mostCommon.add(addToPos, new Pair<EntityType, Long>(entry.getKey(), new Long(entry.getValue())));
							
							if( cInfo.mostCommon.size() > MAXIMUM_ENTITY_MOST_LIST )
								cInfo.mostCommon.removeLast();
						}
					}
				}
				
				Collections.sort(cacheList);
		
				
				totalPrevented = preventedMap.containsKey(preventedMap) ? preventedMap.get(worldName) : 0L;
				totalElapsed = ( running ) ? ( System.currentTimeMillis() - timeStart ) : elapsedTime ;
				success = true;
				
			}
		} catch( Exception e) {
			e.printStackTrace();
			
		} finally {
			if( endCallback != null )
				endCallback.reportEnded();
		}
		
		final long _totalPrevented = totalPrevented;
		final long _totalElapsed = totalElapsed;
		final boolean _success = success; 
		final String _worldName = worldName;
		final long _totalSpawns = totalSpawns;
			
		(new BukkitRunnable() {	
			
			@Override
			public void run() {
				
				if( !_success )
					Utils.sendMessage(String.format("{RED}Report Error: world '%s' was not found", _worldName), sender);
				
				else
				{
					ChunkInfo cInfo;
					
					Utils.sendMessage(String.format("{YELLOW}[MP-Report] {BRIGHTGREEN}Elapsed time:{GOLD} %.2f{BRIGHTGREEN}, Spawned Mobs: {GOLD}%d{BRIGHTGREEN}, prevented: {GOLD}%d", _totalElapsed/1000.0, _totalSpawns, _totalPrevented), sender);
					
					for(int i = 0; i < cacheList.size() && i < REPORT_CHUNK_PAGE_SIZE; i++)
					{
						cInfo = cacheList.get(i);
						
						if( cInfo == null )
							continue;
								
						Utils.sendMessage(String.format("{YELLOW}[MP-Report] {GOLD}%d{BRIGHTGREEN}. T. Spawns: {GOLD}%d {BRIGHTGREEN}Chunk Pos: {GOLD}%d{BRIGHTGREEN}, {GOLD}%d", i+1, cInfo.totalSpawns, cInfo.cLoc.x, cInfo.cLoc.z), sender);
					}
				}
			}
		}).runTask(plugin);

	}

}

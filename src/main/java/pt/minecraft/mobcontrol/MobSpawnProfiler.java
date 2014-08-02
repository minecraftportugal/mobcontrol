package pt.minecraft.mobcontrol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import pt.minecraft.mobcontrol.GroupDescriptorProvider.ChunkLocation;
import pt.minecraft.mobcontrol.MobControlCommandExecutor.ReportEndCallback;

public class MobSpawnProfiler {
	
	
	/**
	 * 
	 *
	 */
	
	public class MobSpawnProfilerException extends Exception
	{
		private static final long serialVersionUID = -1321308266169251285L;

		public MobSpawnProfilerException(String msg)
		{
			super(msg);
		}
	}
	
	
	/**
	 * 
	 *
	 */
	
	private static class Pair<K,V> {
		K k;
		V v;
		public Pair(K k, V v)
		{
			this.k = k;
			this.v = v;
		}
	}
	
	/**
	 * 
	 *
	 */
	
	private static class ChunkInfo implements Comparable<ChunkInfo>
	{
		ChunkLocation cLoc = null;
		EntityMap eMap = null;
		long totalSpawns = 0;
		LinkedList<Pair<EntityType, Long>> mostCommon = new LinkedList<Pair<EntityType, Long>>();
		
		double neighbours = 0;
		long meanDiv = 0;
		
		
		@Override
		public int compareTo(ChunkInfo o)
		{
			if( o == null )
				return -Integer.MIN_VALUE;
			
			//return (int) (o.totalSpawns - this.totalSpawns);
			//return (int) (o.neighborMean - this.neighborMean);
			return
				( o.neighbours == this.neighbours ) ? 0
						: ( ( o.neighbours > this.neighbours ) ? 1 : - 1 );
						
			
//			if( this.totalSpawns > o.totalSpawns )
//				return -1;
//			
//			if( this.totalSpawns < o.totalSpawns )
//				return -1;
//			
//			return 0;
		}
	}
	
	/**
	 * 
	 *
	 */
	
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
	
	
	
	
	
	/**
	 * 
	 *
	 */
	
	
	private boolean running = false;

	//private MobControlPlugin plugin = null;
	
	private HashMap<String, Long> preventedMap = new HashMap<String, Long>();
	private long timeStart = 0;
	private long elapsedTime = 0;
	private HashMap<String, HashMap<ChunkLocation, EntityMap>>
						worldMap = new HashMap<String, HashMap<ChunkLocation, EntityMap>>();
	
	private final int MAXIMUM_ENTITY_MOST_LIST;
	private final int REPORT_CHUNK_PAGE_SIZE;
	//private static final int REPORT_CHUNK_MOB_DESCRIPTION_SIZE = 2;
	
	
	public MobSpawnProfiler(MobControlPlugin plugin)
	{
		//this.plugin = plugin;
		
		MAXIMUM_ENTITY_MOST_LIST = plugin.getConfig().getInt("reportSize", 10);
		REPORT_CHUNK_PAGE_SIZE = plugin.getConfig().getInt("mostCommonMobsSize", 2);
	}
	
	
	
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
	
	public void clean()
	{
		running = false;
		timeStart = 0;
		elapsedTime = 0;
		preventedMap.clear();
		worldMap.clear();
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
				
				ChunkInfo c1,c2;
				double dist;
				
				for(int i = 0; i < cacheList.size(); i++)
				{
					c1 = cacheList.get(i);
					
					if(    c1 == null
						|| c1.cLoc == null )
						continue;
					
					for(int y = i+1; y < cacheList.size(); y++)
					{
						c2 = cacheList.get(y);

						if(     c2 == null
							 || c2.cLoc == null )
							continue;
						
						dist = c1.cLoc.distance(c2.cLoc);
						
						if( (int)dist == 1 )
						{
							//System.out.println(String.format("c1[%d,%d], c1[%d,%d], dist: %f", c1.cLoc.x, c1.cLoc.z, c2.cLoc.x, c2.cLoc.z, dist));
							c1.neighbours += c2.totalSpawns;
							c2.neighbours += c1.totalSpawns;
							
							c1.meanDiv++;
							c2.meanDiv++;
						}
					}
					
					if( c1.meanDiv > 0 )
						c1.neighbours = (c1.neighbours / (double)c1.meanDiv)/2.0;
					
					
					c1.neighbours += c1.totalSpawns;
					//cInfo.meanDiv = 1;
					//c1.neighbours /= 9.0;
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
		
		
		Bukkit.getScheduler().callSyncMethod(plugin, new Callable<Void>() {

			@Override
			public Void call() throws Exception
			{
				if( !_success )
					Utils.sendMessage(String.format("{RED}Report Error: world '%s' was not found", _worldName), sender);
				
				else
				{
					ChunkInfo cInfo;
					
					Utils.sendMessage(String.format("{YELLOW}[MP-Report] {BRIGHTGREEN}Elapsed time:{GOLD} %.2f{BRIGHTGREEN}, Spawned Mobs: {GOLD}%d{BRIGHTGREEN},"
													+ " Prevented: {GOLD}%d", _totalElapsed/1000.0, _totalSpawns, _totalPrevented), sender);
					
					for(int i = 0; i < cacheList.size() && i < REPORT_CHUNK_PAGE_SIZE; i++)
					{
						cInfo = cacheList.get(i);
						
						if( cInfo == null )
							continue;
						
						StringBuilder sb = new StringBuilder("");
						int maxIter = Math.min(cInfo.mostCommon.size(), MAXIMUM_ENTITY_MOST_LIST);
						
						for(int y = 0; y < maxIter; y++)
						{
							sb.append(cInfo.mostCommon.get(y).k.toString());
							sb.append(": ");
							sb.append(cInfo.mostCommon.get(y).v);
							
							if( (y+1) < maxIter )
								sb.append(", ");
						}
								
						Utils.sendMessage(String.format("{YELLOW}[MP-Rep] {GOLD}%d{BRIGHTGREEN}. Neighb: {GOLD}%.1f{BRIGHTGREEN}, "
								+ "Spawns: {GOLD}%d {BRIGHTGREEN}, Pos: {GOLD}%d{BRIGHTGREEN}, {GOLD}%d {BRIGHTGREEN}[{GOLD}%s{BRIGHTGREEN}]",
								i+1, cInfo.neighbours, cInfo.totalSpawns, cInfo.cLoc.x, cInfo.cLoc.z, sb.toString()), sender);
					}
				}
				
				return null;
			}
		});

	}

}

package pt.minecraft.mobcontrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;


public class GroupDescriptorProvider {
	
	
	
	private static class ChunkLocation {
		int x,z;

//		@Override
//		public boolean equals(Object o)
//		{
//			if(     o == null
//				|| !(o instanceof ChunkLocation ) )
//				return false;
//			
//			return (    this.x == ((ChunkLocation)o).x
//					 && this.z == ((ChunkLocation)o).z );
//		}
	}
	
	
	
	public static class GroupDescriptor {
		
		private String ident = null;
		
		private double rate = 1.0;
		
		private boolean incAll = false;
		
		private List<EntityType> incList = null; // = new ArrayList<EntityType>();
		private List<EntityType> excList = null; //= new ArrayList<EntityType>();
		
		private List<ChunkLocation> chunks = null; //= new ArrayList<ChunkLocation>();
		
		
		public String getIdent()
		{
			return this.ident;
		}
		public void setIdent(String ident)
		{
			this.ident = ident;
		}
		
		
		public boolean insideGroup(int chunkX, int chunkZ, EntityType type)
		{
			
			// If there is a chunk list, this chunk must be listed
			if( chunks != null )
			{
				boolean found = false;
				
				for( ChunkLocation c: chunks)
				{
					if( c == null )
						continue;
					
					if( c.x == chunkX && c.z == chunkZ )
					{
						found = true;
						break;
					}
				}
				
				if( !found )
					return false;
			}
			
			// if there is an exclusion list, this entity must not be listed in it
			if( excList != null )
			{
				if( excList.contains(type) )
					return false;
			}
			
			// Triggers all entities
			if( incAll )
				return true;
			
			// its not an 'include all mobs', so this entity MUST be listed
			if( incList != null )
			{
				if( incList.contains(type) )
					return true;
			}
			
			return false;
		}
	}
	
	
	private GroupDescriptor defaultDescr = null;
	private HashMap<String, ArrayList<GroupDescriptor>> descriptorList = new HashMap<String, ArrayList<GroupDescriptor>>();
	
	
	
	
	private GroupDescriptorProvider() { }

	
	
	
	
	public static GroupDescriptorProvider buildFromConfig(FileConfiguration config, JavaPlugin plugin)
	{
		if( config == null )
			return null;
		
		ConfigurationSection worlds = config.getConfigurationSection("worlds"),
				             world;
		
		if ( worlds == null )
			return null;
		
		GroupDescriptorProvider prov = new GroupDescriptorProvider();
		GroupDescriptor defaultDescr,gDescr = null;
		World bWorld = null;
		ArrayList<GroupDescriptor> dwList = null;

		// Process default world
		defaultDescr = processSection( worlds.getConfigurationSection("default") );
		if( defaultDescr == null )
		{
			if( Utils.isDebug() )
				Utils.debug("Default world group not found, creating from default template");
			
			defaultDescr = new GroupDescriptor();
			defaultDescr.incAll = true;
		}
		prov.defaultDescr = defaultDescr;
			

		// Now iterate over all other worlds
		for( String w: worlds.getKeys(false) )
		{
			if( w == null )
				continue;
			
			w = w.trim();
			
			if( w.equals("default") ) // default already processed
				continue;
			
			bWorld = plugin.getServer().getWorld(w);
			
			if( bWorld == null )
			{
				Utils.severe("World: %s not found on server, ignoring...", w);
				
				continue;
			}
			
			Utils.info("Processing configuration from world: %s", w);
			
			world = worlds.getConfigurationSection(w);
			
			if( world == null )
			{
				if( Utils.isDebug() )
					Utils.debug("Error accessing world configuration: %s", w);
				
				continue;
			}
			
			dwList = prov.descriptorList.get(w);
			
			if( dwList == null )
			{
				dwList = new ArrayList<GroupDescriptor>();
				prov.descriptorList.put(w, dwList);
			}

//			gDescr = processSection( world.getConfigurationSection("default") );
//			
//			if( gDescr == null )
//				gDescr = defaultDescr;
			
			// Global default is always the first element on the list
			dwList.add( defaultDescr );
			
			for(String g : world.getKeys(false) )
			{
				if( g == null )
					continue;
				
				g = g.trim();
				
//				if( g.equals("default") ) // default already processed
//					continue;

				gDescr = processSection( world.getConfigurationSection(g) );

				if( gDescr != null )
				{
					gDescr.setIdent(w + "/" + g);
					dwList.add( gDescr );
					
					Utils.debug("Adding group: %s/%s", w, g);
				}
			}
		}
		
		return prov;
	}
	
	
	
	
	
	private static GroupDescriptor processSection(ConfigurationSection section)
	{
		if(    section == null
			|| !section.getBoolean("active", true) )
			return null;
		
		GroupDescriptor descr = new GroupDescriptor();
		descr.incAll = false;
		
		descr.rate = section.getDouble("rate", 1.0);
		
		if( descr.rate < 0 )
			descr.rate = 0.0;
		else
		if( descr.rate > 1.0 )
			descr.rate = 1.0;
		
		descr.incList = parseMobList( section.getStringList("include"), false);
		
		if(    descr.incList == null
			|| descr.incList.size() == 0 )
		{
			descr.incList = null;
			descr.incAll = true;
		}
		
		descr.incList = parseMobList( section.getStringList("exclude"), true);
		
		
		//TODO:
		//ConfigurationSection chunks = section.getConfigurationSection("chunks");
		
		
		return descr;
	}
	
	private static List<EntityType> parseMobList(List<String> moblist, boolean ignoreAllKeyword)
	{
		if( moblist == null )
			return null;
		
		ArrayList<EntityType> list = new ArrayList<EntityType>();
		EntityType type;
		
		for(String mob : moblist)
		{
			if( mob == null )
				continue;
			
			mob = mob.trim().toUpperCase();
			
			if(    !ignoreAllKeyword
				&& mob.equals("ALL") )
				return null;
			
			else
			{
				try {
					type = EntityType.valueOf(mob);
				} catch(Exception e) {
					type = null;
				}
				
				if( type == null )
				{
					Utils.severe("Unknown entity: %s", mob);
					
					continue;
				}
				
				if( !type.isAlive() )
				{
					Utils.severe("Not living entity: %s", mob);
					
					continue;
				}
				
				list.add(type);
			}
		}
		
		return list;
	}
	
	
	public double getRate(int chunkX, int chunkZ, World world, EntityType type)
	{
		if( type != null )
		{
			ArrayList<GroupDescriptor> descList = null;
			GroupDescriptor foundGroup = null,group;
			
			if( world != null )
				descList = descriptorList.get(world.getName());
			
			if( descList == null )
			{
				if (    defaultDescr != null
					 && defaultDescr.insideGroup(chunkX, chunkZ, type) )
					foundGroup = defaultDescr;
			}
			else
			{
				// Iterate backwards, from most significant to least significant.
				// World's default is always on first position
				for(int i = descList.size()-1; i >= 0; i-- )
				{
					group = descList.get(i);
					
					if( group == null )
						continue;
					
					if ( group.insideGroup(chunkX, chunkZ, type) )
					{
						foundGroup = group;
						break;
					}
				}
			}
			
			if( foundGroup != null )
			{
				if( Utils.isDebug() )
					Utils.debug("Entity: '%s' on chunk[%s, %d, %d] found on group: '%s' with rate: %f",
						   type.toString(), world.getName(), chunkX, chunkZ, foundGroup.ident, foundGroup.rate );
				
				return foundGroup.rate;
			}
			
			if( Utils.isDebug() )
				Utils.debug("Entity: '%s' on chunk[%s, %d, %d] not found on any group",
					   type.toString(), world.getName(), chunkX, chunkZ );
		}
		
		return 1.0;
	}

}

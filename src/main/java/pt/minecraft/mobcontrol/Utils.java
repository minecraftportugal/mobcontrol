package pt.minecraft.mobcontrol;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;



public class Utils {

	private static Logger logger = Logger.getLogger("Minecraft");
	private static boolean debug = false;
	
	
    public static void info(String msg, Object ... args)
    {
        if (args.length > 0)
            msg = String.format(msg, args);
        
        msg = ChatColor.stripColor(msg);
        
        if (msg.isEmpty())
        	return;
        
        logger.log(Level.INFO, String.format("[%s] %s", MobControlPlugin.PLUGIN_NAME, msg));
    }

    public static void warning(String msg, Object ... args)
    {
        if (args.length > 0)
            msg = String.format(msg, args);
        
        msg = ChatColor.stripColor(msg);
        
        if (msg.isEmpty())
        	return;
        
        logger.log(Level.WARNING, String.format("[%s] %s", MobControlPlugin.PLUGIN_NAME, msg));
    }

    public static void severe(String msg, Object ... args)
    {
        if (args.length > 0)
            msg = String.format(msg, args);
        
        msg = ChatColor.stripColor(msg);
        
        if (msg.isEmpty())
        	return;
        
        logger.log(Level.SEVERE, String.format("[%s] %s", MobControlPlugin.PLUGIN_NAME, msg));
    }

    public static void severe(Throwable t, String msg, Object ... args)
    {
        if (args.length > 0)
            msg = String.format(msg, args);
        
        msg = ChatColor.stripColor(msg);
        
        if (msg.isEmpty())
        	return;
        
        logger.log(Level.SEVERE, String.format("[%s] %s", MobControlPlugin.PLUGIN_NAME, msg), t);
    }
	
	
//	public static void info(String msg)
//	{
//		logger.log(Level.INFO, "[ItemDropper] " + msg);
//	}
//	
//	public static void error(String msg)
//	{
//		logger.log(Level.SEVERE, "[ItemDropper] Error: " + msg);
//	}
//	
	
	
    public static String nameTreat(String s) {
        if (s.length() == 0)
            return s;

        s = s.replaceAll("_", " ");
        return s.toLowerCase();
    }
    
    public static void debug(String msg, Object ... args)
    {
    	info("[DEBUG] " + msg, args);
    }
    
    public static void debug(Throwable t, String msg, Object ... args)
    {
    	severe(t, "[DEBUG] " + msg, args);
    }
    
    
    public static void sendMessage(String message, CommandSender sender)
    {
    	if(sender == null )
    		return;
    	
    	message = replaceColors(message);
    		
    	for (String line : message.split("\n"))
    	{
    		Utils.info(line);
    	  sender.sendMessage(line);
    	}
    }
    
    public static String replaceColors(String s) {
        s = s.replace("{BLACK}", "&0");
        s = s.replace("{DARKBLUE}", "&1");
        s = s.replace("{DARKGREEN}", "&2");
        s = s.replace("{DARKTEAL}", "&3");
        s = s.replace("{DARKRED}", "&4");
        s = s.replace("{PURPLE}", "&5");
        s = s.replace("{GOLD}", "&6");
        s = s.replace("{GRAY}", "&7");
        s = s.replace("{DARKGRAY}", "&8");
        s = s.replace("{BLUE}", "&9");
        s = s.replace("{BRIGHTGREEN}", "&a");
        s = s.replace("{TEAL}", "&b");
        s = s.replace("{RED}", "&c");
        s = s.replace("{PINK}", "&d");
        s = s.replace("{YELLOW}", "&e");
        s = s.replace("{WHITE}", "&f");

        return switchToColorChar(s);
    }

    public static String switchToColorChar(String s) {
        return s.replace('&', ChatColor.COLOR_CHAR);
    }

    
    public static boolean isDebug()
    {
    	return debug;
    }
    public static void setDebug(boolean debug)
    {
    	Utils.debug = debug;
    }
    

//    public static void sendMessage(String node, CommandSender sender, String targetName)
//    {
//        if (sender != null) {
//            String message = getNode(node, sender.getName(), targetName);
//
//            for (String line : message.split("\n"))
//                sender.sendMessage(line);
//        }
//    }

}
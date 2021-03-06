package dev.array21.harotorch.commands.torchSubCmds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import dev.array21.bukkitreflectionlib.ReflectionUtil;
import net.md_5.bungee.api.ChatColor;
import dev.array21.harotorch.HaroTorch;
import dev.array21.harotorch.commands.SubCommand;
import dev.array21.harotorch.lang.LangHandler;
import dev.array21.harotorch.torch.TorchHandler;

@SuppressWarnings("deprecation")
public class HighlightExecutor implements SubCommand {
	
	private static HashMap<UUID, Long> lastCommandTimestamps = new HashMap<>();
	
	private static Class<?> craftPlayerClass;
	private static Class<?> craftWorldClass;
	private static Class<?> worldClass;
	private static Class<?> entityTypesClass;
	private static Class<?> entityMagmaCubeClass;
	private static Class<?> entitySlimeClass;
	private static Class<?> entityClass;
	private static Class<?> entityLivingClass;
	private static Class<?> packetPlayOutEntityLivingClass;
	private static Class<?> packetPlayOutEntityLivingInterfaceClass;
	private static Class<?> packetPlayOutEntityMetadataClass;
	private static Class<?> packetPlayOutEntityMetadataInterfaceClass;
	private static Class<?> packetPlayOutEntityDestroyClass;
	private static Class<?> packetPlayOutEntityDestroyInterfaceClass;
	private static Class<?> dataWatcherClass;
	private static Object entityMagmaCubeField;

	static {
		try {
			craftPlayerClass = ReflectionUtil.getBukkitClass("entity.CraftPlayer");
			craftWorldClass = ReflectionUtil.getBukkitClass("CraftWorld");
			
			if(ReflectionUtil.isUseNewSpigotPackaging()) {
				worldClass = ReflectionUtil.getMinecraftClass("world.level.World");
				entityTypesClass = ReflectionUtil.getMinecraftClass("world.entity.EntityTypes");
				entityMagmaCubeClass = ReflectionUtil.getMinecraftClass("world.entity.monster.EntityMagmaCube");
				entitySlimeClass = ReflectionUtil.getMinecraftClass("world.entity.monster.EntitySlime");
				entityClass = ReflectionUtil.getMinecraftClass("world.entity.Entity");
				entityLivingClass = ReflectionUtil.getMinecraftClass("world.entity.EntityLiving");
				packetPlayOutEntityLivingClass = ReflectionUtil.getMinecraftClass("network.protocol.game.PacketPlayOutSpawnEntityLiving");
				packetPlayOutEntityMetadataClass = ReflectionUtil.getMinecraftClass("network.protocol.game.PacketPlayOutEntityMetadata");
				packetPlayOutEntityDestroyClass = ReflectionUtil.getMinecraftClass("network.protocol.game.PacketPlayOutEntityDestroy");
				dataWatcherClass = ReflectionUtil.getMinecraftClass("network.syncher.DataWatcher");

				entityMagmaCubeField = ReflectionUtil.getObject(null, entityTypesClass, "X");
			} else {
				worldClass = ReflectionUtil.getNmsClass("World");
				entityTypesClass = ReflectionUtil.getNmsClass("EntityTypes");
				entityMagmaCubeClass = ReflectionUtil.getNmsClass("EntityMagmaCube");
				entitySlimeClass = ReflectionUtil.getNmsClass("EntitySlime");
				entityClass = ReflectionUtil.getNmsClass("Entity");
				entityLivingClass = ReflectionUtil.getNmsClass("EntityLiving");
				packetPlayOutEntityLivingClass = ReflectionUtil.getNmsClass("PacketPlayOutSpawnEntityLiving");
				packetPlayOutEntityMetadataClass = ReflectionUtil.getNmsClass("PacketPlayOutEntityMetadata");			
				packetPlayOutEntityDestroyClass = ReflectionUtil.getNmsClass("PacketPlayOutEntityDestroy");
				dataWatcherClass = ReflectionUtil.getNmsClass("DataWatcher");
				
				entityMagmaCubeField = ReflectionUtil.getObject(null, entityTypesClass, "MAGMA_CUBE");
			}
			
			packetPlayOutEntityLivingInterfaceClass = packetPlayOutEntityLivingClass.getInterfaces()[0];
			packetPlayOutEntityMetadataInterfaceClass = packetPlayOutEntityMetadataClass.getInterfaces()[0];
			packetPlayOutEntityDestroyInterfaceClass = packetPlayOutEntityDestroyClass.getInterfaces()[0];
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public boolean run(HaroTorch plugin, CommandSender sender, String[] args) {
		
		Integer commandCooldown = plugin.getConfigManifest().commandCooldown;
		if(commandCooldown != null && commandCooldown > 0) {
			Long lastCommandUseTimestamp = lastCommandTimestamps.get(((Player) sender).getUniqueId());
			if(lastCommandUseTimestamp != null) {
				if(lastCommandUseTimestamp >= System.currentTimeMillis()) {
					sender.sendMessage(HaroTorch.getMessagePrefix() + ChatColor.GOLD + LangHandler.activeLang.getLangMessages().get("commandCooldown"));
					return true;
				}
			}
			
			lastCommandTimestamps.put(((Player) sender).getUniqueId(), System.currentTimeMillis() + (commandCooldown * 1000));
		}
		
		List<Location> nearbyTorches = TorchHandler.getTorchLocationsNearPlayer((Player) sender, plugin.getConfigManifest().torchHighlightRange);
		Player p = (Player) sender;
		
		List<Integer> returnedIds = spawnHighlight(p,  nearbyTorches);
		
		String msg = LangHandler.activeLang.getLangMessages().get("startingHiglight").replaceAll("%SECONDS%", ChatColor.RED + String.valueOf(plugin.getConfigManifest().torchHighlightTime) + ChatColor.GOLD);
		p.sendMessage(HaroTorch.getMessagePrefix() + ChatColor.GOLD + msg);
		
		new BukkitRunnable() {
			
			@Override
			public void run() {
				killHighlighted(returnedIds, p);
				p.sendMessage(HaroTorch.getMessagePrefix() + ChatColor.GOLD + LangHandler.activeLang.getLangMessages().get("endingHighlight"));
			}
		}.runTaskLater(plugin, plugin.getConfigManifest().torchHighlightTime * 20);
		
		
		return true;
	}
	
	public List<Integer> spawnHighlight(Player player, List<Location> locations) {
		
		List<Integer> result = new ArrayList<>();
		
		try {
			Object entityPlayerObject = ReflectionUtil.invokeMethod(craftPlayerClass, player, "getHandle");
			Object playerConnectionObject;
			if(ReflectionUtil.isUseNewSpigotPackaging()) {
				playerConnectionObject = ReflectionUtil.getObject(entityPlayerObject, "b");
			} else {
				playerConnectionObject = ReflectionUtil.getObject(entityPlayerObject, "playerConnection");
			}
			
			for(Location loc : locations) {
				Object nmsWorld = ReflectionUtil.invokeMethod(craftWorldClass, loc.getWorld(), "getHandle");
				Object entityMagmaCube = ReflectionUtil.invokeConstructor(entityMagmaCubeClass, new Class<?>[] { entityTypesClass, worldClass }, new Object[] { entityMagmaCubeField, nmsWorld });
				
				ReflectionUtil.invokeMethod(entityClass, entityMagmaCube, "setInvisible", new Class<?>[] { boolean.class }, new Object[] { false });
				entityLivingClass.getField("collides").set(entityMagmaCube, false);
				
				//Glowing
				ReflectionUtil.invokeMethod(entityClass, entityMagmaCube, "setFlag", new Class<?>[] { int.class, boolean.class }, new Object[] { 6, true });
				
				//Invisibility
				ReflectionUtil.invokeMethod(entityClass, entityMagmaCube, "setFlag", new Class<?>[] { int.class, boolean.class }, new Object[] { 5, true });
				
				//Size
				ReflectionUtil.invokeMethod(entitySlimeClass, entityMagmaCube, "setSize", new Class<?>[] { int.class, boolean.class }, new Object[] { 2, true });
				
				ReflectionUtil.invokeMethod(entityClass, entityMagmaCube, "setLocation", 
						new Class<?>[] { double.class, double.class, double.class, float.class, float.class}, 
						new Object[] { loc.getBlockX() + 0.5D, loc.getBlockY(), loc.getBlockZ() + 0.5D, 0f, 0f});
				
				Object spawnEntityLivingPacket = ReflectionUtil.invokeConstructor(packetPlayOutEntityLivingClass, new Class<?>[] { entityLivingClass }, new Object[] { entityMagmaCube });
				Object entityMetadataPacket = ReflectionUtil.invokeConstructor(packetPlayOutEntityMetadataClass, 
						new Class<?>[] { int.class, dataWatcherClass,  boolean.class}, 
						new Object[] { 
								ReflectionUtil.invokeMethod(entityClass, entityMagmaCube, "getId"),
								ReflectionUtil.invokeMethod(entityClass, entityMagmaCube, "getDataWatcher"),
								false
						});
				
				ReflectionUtil.invokeMethod(playerConnectionObject, "sendPacket", 
						new Class<?>[] { packetPlayOutEntityLivingInterfaceClass }, 
						new Object[] { spawnEntityLivingPacket });
				
				ReflectionUtil.invokeMethod(playerConnectionObject, "sendPacket",
						new Class<?>[] { packetPlayOutEntityMetadataInterfaceClass },
						new Object[] { entityMetadataPacket });
				
				result.add((Integer) ReflectionUtil.invokeMethod(entityClass, entityMagmaCube, "getId"));
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public void killHighlighted(List<Integer> ids, Player player) {
		try {
			Object entityPlayerObject = ReflectionUtil.invokeMethod(craftPlayerClass, player, "getHandle");
			Object playerConnectionObject;
			if(ReflectionUtil.isUseNewSpigotPackaging()) {
				playerConnectionObject = ReflectionUtil.getObject(entityPlayerObject, "b");
			} else {
				playerConnectionObject = ReflectionUtil.getObject(entityPlayerObject, "playerConnection");
			}
			
			if(ReflectionUtil.isUseNewSpigotPackaging()) {
				for(int id : ids) {
					Object destroyEntityPacket = ReflectionUtil.invokeConstructor(packetPlayOutEntityDestroyClass, new Class<?>[] { int.class }, new Object[] { id });
					ReflectionUtil.invokeMethod(playerConnectionObject, "sendPacket",
							new Class<?>[] { packetPlayOutEntityDestroyInterfaceClass }, 
							new Object[] { destroyEntityPacket });
				}
			} else {
				Object destroyEntityPacket = ReflectionUtil.invokeConstructor(packetPlayOutEntityDestroyClass, new Class<?>[] { int[].class }, new Object[] { toIntArray(ids) });
				ReflectionUtil.invokeMethod(playerConnectionObject, "sendPacket",
						new Class<?>[] { packetPlayOutEntityDestroyInterfaceClass }, 
						new Object[] { destroyEntityPacket });
			}

			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private int[] toIntArray(List<Integer> a) {
		int[] b = new int[a.size()];
		for(int i = 0; i < a.size(); i++) {
			b[i] = a.get(i);
		}
		
		return b;
	}
}

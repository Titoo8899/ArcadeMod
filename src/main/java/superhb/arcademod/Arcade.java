package superhb.arcademod;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.versioning.ComparableVersion;
import org.apache.logging.log4j.*;
import superhb.arcademod.client.ArcadeItems;
import superhb.arcademod.proxy.CommonProxy;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod.*;
import net.minecraftforge.fml.common.event.*;
import superhb.arcademod.client.tileentity.*;
import superhb.arcademod.util.PrizeList;

import java.io.File;
import java.util.*;

/* Game List
    - Mrs. Pac-Man
    - Space Invaders
    - Donkey Kong (ReddyRedStoneOre) [CF]
    - Super Mario Bros (thatguyEnder) [CF]
    - Asteroids (WilchHabos) [CF]
    - Galaga (TheFroggyFrog) [CF]
    - DDR (GamerGuy941Ytube) [MCF]
    - Pinball
    - Centipede (Kaylagoodie) [PMC]
    - Bubble Bobble (pumpkin0022) [CF]
 */

/* Special Machines
    - Skeeball
    - Claw Machine
    - Coin Pusher
    - Stacker (The game with the square that has to line up)
 */

/* ChangeLog
 */
@Mod(modid = Reference.MODID, name = Reference.NAME, version = Reference.VERSION, updateJSON = Reference.UPDATE_URL)
public class Arcade {
	@SidedProxy(clientSide = Reference.CLIENT_PROXY, serverSide = Reference.SERVER_PROXY)
	public static CommonProxy proxy;
	
	@Instance(Reference.MODID)
	public static Arcade instance;
	
	// Logger
	public static final Logger logger = LogManager.getLogger(Reference.MODID);
	
	// Creative Tab
	@MethodsReturnNonnullByDefault
	public static final CreativeTabs tab = new CreativeTabs(Reference.MODID) {
		@Override
		public ItemStack getTabIconItem () {
			return new ItemStack(ArcadeItems.COIN);
		}
		
		@Override
		public String getTranslatedTabLabel () {
			return I18n.format("mod.arcademod:name.locale");
		}
	};
	
	// ChangeLog Variables
	public static Set<Map.Entry<ComparableVersion, String>> changelog;
	public static ForgeVersion.Status status;
	
	// Configuration Variables
	public static boolean disableCoins;
	private static boolean requireRedstone;
	public static boolean disableUpdateNotification;
	
	// Prize List Variables
	private static int prizeTotal = 0;
	public static PrizeList[] prizeList;
	private String[] s_prizeList;
	
	// TODO: default add all plushies
	private final String[] defaultList = {
			"arcademod:plushie:5:Mob=0",
			"arcademod:plushie:3:Mob=1",
			"minecraft:diamond:128",
			"minecraft:iron_ingot:64",
			"minecraft:gold_ingot:96",
			"minecraft:ender_pearl:16",
			"minecraft:ender_eye:32"
	};
	
	// Game Addons
	private static File gameDir;
	
	@EventHandler
	public void preInit (FMLPreInitializationEvent event) {
		// Mod info
		event.getModMetadata().autogenerated = false;
		event.getModMetadata().credits = Reference.CREDIT;
		event.getModMetadata().authorList.add(Reference.AUTHOR);
		event.getModMetadata().description = Reference.DESCRIPTION;
		event.getModMetadata().url = Reference.URL;
		event.getModMetadata().logoFile = Reference.LOGO;
		event.getModMetadata().updateJSON = Reference.UPDATE_URL;
		
		// Configuration File
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		config.load();
		disableCoins = config.getBoolean("disableCoins", Configuration.CATEGORY_GENERAL, false, "Disable the need to use coins to play the arcade machines");
		requireRedstone = config.getBoolean("requireRedstone", Configuration.CATEGORY_GENERAL, false, "Require the machines to be powered by redstone to play");
		disableUpdateNotification = config.getBoolean("disableUpdateNotification", Configuration.CATEGORY_GENERAL, false, "Disable message in chat when update is available");
		// Prize List
		// TODO: Get prizeTotal some other way
		prizeTotal = config.getInt("total", "prize_list", defaultList.length, 0, 100, "Amount of prizes in list. This has to be changed manually.");
		prizeList = new PrizeList[prizeTotal];
		s_prizeList = new String[prizeTotal];
		for (int i = 0; i < prizeTotal; i++)
			s_prizeList[i] = config.getString(String.format("%d", i), "prize_list", defaultList[i], "Format: name:cost");
		config.save();
		
		// Game Addons
		gameDir = new File(event.getModConfigurationDirectory().getParent(), "/Arcade_Games/");
		if (!gameDir.exists()) {
			logger.info("Games Addon directory doesn't exist. Creating empty folder...");
			gameDir.mkdir();
			gameDir.mkdirs();
		}
		
		// Register TileEntity
		GameRegistry.registerTileEntity(TileEntityArcade.class, Reference.MODID + ":tile_arcade");
		GameRegistry.registerTileEntity(TileEntityPlushie.class, Reference.MODID + ":tile_plushie");
		GameRegistry.registerTileEntity(TileEntityPrize.class, Reference.MODID + ":tile_prize");
		GameRegistry.registerTileEntity(TileEntityPusher.class, Reference.MODID + ":tile_pusher");
		
		proxy.preInit(event);
	}
	
	@EventHandler
	public void init (FMLInitializationEvent event) {
		proxy.init(event);
	}
	
	@EventHandler
	public void postInit (FMLPostInitializationEvent event) {
		// Change Log
		for (ModContainer mod : Loader.instance().getModList()) {
			if (mod.getModId().equals(Reference.MODID)) {
				status = ForgeVersion.getResult(mod).status;
				if (status == ForgeVersion.Status.OUTDATED || status == ForgeVersion.Status.BETA_OUTDATED)
					changelog = ForgeVersion.getResult(mod).changes.entrySet();
			}
		}
		
		// Check for other mods here
		
		// Prize List
		loadPrizeList();
		
		proxy.postInit(event);
	}
	
	private void loadPrizeList () {
		for (int i = 0; i < s_prizeList.length; i++) {
			String[] s = s_prizeList[i].split(":");
			if (s.length == 3) {
				Item item = Item.getByNameOrId(s[0] + ":" + s[1]);
				int cost = new Integer(s[2]);
				prizeList[i] = new PrizeList(new ItemStack(item), cost);
			} else if (s.length == 4) {
				int cost = new Integer(s[2]);
				Item item = Item.getByNameOrId(s[0] + ":" + s[1]);
				String[] nbt = s[3].split("=");
				
				NBTTagCompound compound = new NBTTagCompound();
				// TODO: phraser for other types
				try {
					compound.setInteger(nbt[0], new Integer(nbt[1]));
				} catch (NumberFormatException e) {
					compound.setString(nbt[0], nbt[1]);
				}
				ItemStack stack = new ItemStack(item, 1);
				stack.setTagCompound(compound);
				prizeList[i] = new PrizeList(stack, cost);
			}
		}
	}
	
	// TODO: Game Addons
	// Gets files from /Arcade_Games/ directory
	private void getAddons (List<File> games) {
		for (File game : games) {
			if (game.isDirectory()) {
			}
		}
	}
}

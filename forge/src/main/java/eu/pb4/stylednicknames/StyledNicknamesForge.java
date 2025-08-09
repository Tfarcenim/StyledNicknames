package eu.pb4.stylednicknames;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.stylednicknames.command.StyledNicknameCommands;
import eu.pb4.stylednicknames.config.Config;
import eu.pb4.stylednicknames.config.ConfigManager;
import eu.pb4.stylednicknames.network.PacketHandler;
import eu.pb4.stylednicknames.platform.Services;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod(StyledNicknames.MOD_ID)
public class StyledNicknamesForge {

	public static String VERSION = "0";//FabricLoader.getInstance().getModContainer(StyledNicknames.MOD_ID).get().getMetadata().getVersion().getFriendlyString();

	public StyledNicknamesForge() {
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		bus.addListener(this::setup);
		MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
		MinecraftForge.EVENT_BUS.addListener(this::serverStarting);
		MinecraftForge.EVENT_BUS.addListener(this::commands);
		MinecraftForge.EVENT_BUS.addListener(this::nameFormat);
		MinecraftForge.EVENT_BUS.addListener(this::getTabName);
		MinecraftForge.EVENT_BUS.addListener(this::playerLogin);

		Placeholders.register(StyledNicknames.id("display_name"), (ctx, arg) -> {
			if (ctx.hasPlayer()) {
				if (ctx.player().connection != null) {
					return PlaceholderResult.value(NickUtils.getOutputOrVanilla(ctx.player(),true));
				} else {
					return PlaceholderResult.value(ctx.player().getName());
				}
			} else {
				return PlaceholderResult.invalid("Not a player!");
			}
		});

		if (FMLEnvironment.dist.isClient() && Services.PLATFORM.isDevelopmentEnvironment()) {
			MinecraftForge.EVENT_BUS.addListener(Client::forceNameTag);
		}
	}

	void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		ServerPlayer player = (ServerPlayer) event.getEntity();
		NickUtils.loadNicknameData(player);
	}

	void setup(FMLCommonSetupEvent event) {
		ConfigManager.loadConfig();
		Config config = ConfigManager.getConfig();
		if (!config.configData.serverSideOnly) {
			PacketHandler.registerPackets();
		}
	}

	void nameFormat(PlayerEvent.NameFormat event) {
		event.setDisplayname(NickUtils.getDisplayNickName(event.getEntity(),event.getDisplayname()));
	}

	void getTabName(PlayerEvent.TabListNameFormat event) {
		event.setDisplayName(NickUtils.getDisplayNickName(event.getEntity(),null));
	}

	public static class Client {

		public static Map<UUID, CompoundTag> dataMap = new HashMap<>();

		static void forceNameTag(RenderNameTagEvent event) {
			event.setResult(Event.Result.ALLOW);
		}
	}

	void commands(RegisterCommandsEvent event) {
		StyledNicknameCommands.register(event.getDispatcher());
	}

	void serverStarted(ServerStartedEvent event) {
		//CardboardWarning.checkAndAnnounce();
	}

	void serverStarting(ServerStartingEvent event) {
		ConfigManager.loadConfig();
	}

}

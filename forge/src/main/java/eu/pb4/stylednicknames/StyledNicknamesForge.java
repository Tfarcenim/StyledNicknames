package eu.pb4.stylednicknames;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.stylednicknames.command.StyledNicknameCommands;
import eu.pb4.stylednicknames.config.ConfigManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static eu.pb4.stylednicknames.StyledNicknames.id;

@Mod(StyledNicknames.MOD_ID)
public class StyledNicknamesForge {

	public static String VERSION = "0";//FabricLoader.getInstance().getModContainer(StyledNicknames.MOD_ID).get().getMetadata().getVersion().getFriendlyString();

	public StyledNicknamesForge() {
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
		MinecraftForge.EVENT_BUS.addListener(this::serverStarting);
		MinecraftForge.EVENT_BUS.addListener(this::commands);

		Placeholders.register(StyledNicknames.id("display_name"), (ctx, arg) -> {
			if (ctx.hasPlayer()) {
				if (ctx.player().connection != null) {
					return PlaceholderResult.value(NicknameHolder.of(ctx.player().connection).styledNicknames$getOutputOrVanilla());
				} else {
					return PlaceholderResult.value(ctx.player().getName());
				}
			} else {
				return PlaceholderResult.invalid("Not a player!");
			}
		});
	}

	void commands(RegisterCommandsEvent event) {
		StyledNicknameCommands.register(event.getDispatcher());
	}

	void serverStarted(ServerStartedEvent event) {
		CardboardWarning.checkAndAnnounce();
	}

	void serverStarting(ServerStartingEvent event) {
		ConfigManager.loadConfig();
	}

	public static void setPermission(ServerPlayer player,byte permissions) {
		player.getPersistentData().getCompound(ServerPlayer.PERSISTED_NBT_TAG).putByte("permission",permissions);
	}

	public static void setNickname(ServerPlayer player,String nickname) {
		if (nickname == null) {
			player.getPersistentData().getCompound(ServerPlayer.PERSISTED_NBT_TAG).remove("nickname");
		} else {
			player.getPersistentData().getCompound(ServerPlayer.PERSISTED_NBT_TAG).putString("nickname",nickname);
		}
	}

	public static String getNickname(ServerPlayer player) {
		return player.getPersistentData().getCompound(ServerPlayer.PERSISTED_NBT_TAG).getString(id("nickname").toString());

	}

	public static byte getPermission(ServerPlayer player) {
		return player.getPersistentData().getCompound(ServerPlayer.PERSISTED_NBT_TAG).getByte(id("permission").toString());
	}

}

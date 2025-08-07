package eu.pb4.stylednicknames;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.stylednicknames.command.Commands;
import eu.pb4.stylednicknames.config.ConfigManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;


public class StyledNicknamesMod implements ModInitializer {

	public static String VERSION = FabricLoader.getInstance().getModContainer(StyledNicknames.MOD_ID).get().getMetadata().getVersion().getFriendlyString();

	@Override
	public void onInitialize() {
		Commands.register();
		ServerLifecycleEvents.SERVER_STARTED.register((s) -> {
			CardboardWarning.checkAndAnnounce();
		});

		ServerLifecycleEvents.SERVER_STARTING.register((s) -> {
			ConfigManager.loadConfig();
		});

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

}

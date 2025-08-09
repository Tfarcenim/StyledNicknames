package eu.pb4.stylednicknames;

import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.placeholders.api.parsers.TextParserV1;
import eu.pb4.stylednicknames.config.Config;
import eu.pb4.stylednicknames.config.ConfigManager;
import eu.pb4.stylednicknames.network.client.S2CSyncPersistantDataPacket;
import eu.pb4.stylednicknames.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Objects;

public class MixinHooks {

    public static void styledNicknames$set(ServerPlayer player, String nickname, boolean requirePermission) {
        Config config = ConfigManager.getConfig();
        CommandSourceStack source = player.createCommandSourceStack();
        if (nickname == null || nickname.isEmpty() || (requirePermission && !source.hasPermission(ConfigManager.getConfig().configData.allowByDefault ? 0 : 2))) {
            NickUtils.setNickname(player,null);
            NickUtils.setStyledNickname(player,null);
            NickUtils.setPermission(player, (byte) 0);
        } else {
            NickUtils.setNickname(player, nickname);
            NickUtils.setPermission(player, (byte) (requirePermission ? 1 : 0));

            var handlers = new HashMap<String, TextParserV1.TagNodeBuilder>();


            for (var entry : TextParserV1.SAFE.getTags()) {
                if (config.defaultFormattingCodes.getBoolean(entry.name()) || player.hasPermissions( 2)) {

                    handlers.put(entry.name(), entry.parser());

                    if (entry.aliases() != null) {
                        for (var a : entry.aliases()) {
                            handlers.put(a, entry.parser());
                        }
                    }
                }
            }

            if (config.configData.allowLegacyFormatting) {
                for (ChatFormatting formatting : ChatFormatting.values()) {
                    if (handlers.get(formatting.getName()) != null) {
                        nickname = nickname.replace(String.copyValueOf(new char[]{'&', formatting.getChar()}), "<" + formatting.getName() + ">");
                    }
                }
            }

            NickUtils.setStyledNickname(player,TextParserUtils.formatText(nickname, handlers::get));
        }

        player.refreshTabListName();
        player.refreshDisplayName();
        if (config.configData.changePlayerListName) {
              Objects.requireNonNull(player.getServer()).getPlayerList().broadcastAll(
                    new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player));
        }
        if (!config.configData.serverSideOnly) {
            CompoundTag persistentData = player.getPersistentData();
            Services.PLATFORM.sendToClients(new S2CSyncPersistantDataPacket(player.getUUID(),persistentData), player.server.getPlayerList().getPlayers());
        }
    }
}

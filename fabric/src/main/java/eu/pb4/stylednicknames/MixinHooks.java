package eu.pb4.stylednicknames;

import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.placeholders.api.parsers.TextParserV1;
import eu.pb4.playerdata.api.PlayerDataApi;
import eu.pb4.stylednicknames.config.Config;
import eu.pb4.stylednicknames.config.ConfigManager;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.util.HashMap;
import java.util.Objects;

public class MixinHooks {

    public static void styledNicknames$set(ServerGamePacketListenerImpl impl, String nickname, boolean requirePermission) {
        Config config = ConfigManager.getConfig();
        ServerPlayer player = impl.player;
        NicknameHolder nicknameHolder = (NicknameHolder)impl;
        CommandSourceStack source = impl.player.createCommandSourceStack();
        if (nickname == null || nickname.isEmpty() || (requirePermission && !Permissions.check(source, "stylednicknames.use", ConfigManager.getConfig().configData.allowByDefault ? 0 : 2))) {
            nicknameHolder.directlySetNickname(null);
            nicknameHolder.setRequirePermission(false);
            nicknameHolder.directlySetParsedNickname(null);
            PlayerDataApi.setGlobalDataFor(player, StyledNicknames.id("nickname"), null);
            PlayerDataApi.setGlobalDataFor(player, StyledNicknames.id("permission"), ByteTag.valueOf(false));
        } else {
            nicknameHolder.directlySetNickname(nickname);
            nicknameHolder.setRequirePermission(requirePermission);
            PlayerDataApi.setGlobalDataFor(player, StyledNicknames.id("nickname"), StringTag.valueOf(nickname));
            PlayerDataApi.setGlobalDataFor(player, StyledNicknames.id("permission"), ByteTag.valueOf(requirePermission));

            var handlers = new HashMap<String, TextParserV1.TagNodeBuilder>();


            for (var entry : TextParserV1.SAFE.getTags()) {
                if ((config.defaultFormattingCodes.getBoolean(entry.name())
                        || Permissions.check(player, "stylednicknames.format." + entry.name(), 2))) {

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

            nicknameHolder.directlySetParsedNickname(TextParserUtils.formatText(nickname, handlers::get));
        }

        if (config.configData.changePlayerListName) {
            Objects.requireNonNull(player.getServer()).getPlayerList().broadcastAll(
                    new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player));
        }
    }
}

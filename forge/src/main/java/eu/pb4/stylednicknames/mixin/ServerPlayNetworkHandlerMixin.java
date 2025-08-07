package eu.pb4.stylednicknames.mixin;

import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.placeholders.api.parsers.TextParserV1;
import eu.pb4.stylednicknames.NicknameHolder;
import eu.pb4.stylednicknames.StyledNicknamesForge;
import eu.pb4.stylednicknames.config.Config;
import eu.pb4.stylednicknames.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPlayNetworkHandlerMixin implements NicknameHolder {
    @Shadow
    public ServerPlayer player;
    @Unique
    private String styledNicknames$nickname = null;
    @Unique
    private Component styledNicknames$parsedNicknameRaw = null;
    @Unique
    private boolean styledNicknames$requirePermission = true;

    @Override
    public void styledNicknames$loadData() {
        try {
            String nickname = StyledNicknamesForge.getNickname(player);
            byte permissions =  StyledNicknamesForge.getPermission(player);

            if (!nickname.isBlank()) {
                this.styledNicknames$set(nickname, permissions > 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void styledNicknames$set(String nickname, boolean requirePermission) {
        Config config = ConfigManager.getConfig();
        CommandSourceStack source = player.createCommandSourceStack();
        if (nickname == null || nickname.isEmpty() || (requirePermission && source.hasPermission(ConfigManager.getConfig().configData.allowByDefault ? 0 : 2))) {
            this.styledNicknames$nickname = null;
            this.styledNicknames$requirePermission = false;
            this.styledNicknames$parsedNicknameRaw = null;
            StyledNicknamesForge.setNickname(this.player, null);
            StyledNicknamesForge.setPermission(this.player, (byte) 0);
        } else {
            this.styledNicknames$nickname = nickname;
            this.styledNicknames$requirePermission = requirePermission;
            StyledNicknamesForge.setNickname(this.player,nickname);
            StyledNicknamesForge.setPermission(this.player, (byte) (requirePermission ? 1 : 0));

            var handlers = new HashMap<String, TextParserV1.TagNodeBuilder>();


            for (var entry : TextParserV1.SAFE.getTags()) {
                if ((config.defaultFormattingCodes.getBoolean(entry.name())
                        || player.hasPermissions(2))) {

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

            this.styledNicknames$parsedNicknameRaw = TextParserUtils.formatText(nickname, handlers::get);
        }

        if (config.configData.changePlayerListName) {
            Objects.requireNonNull(this.player.getServer()).getPlayerList().broadcastAll(
                    new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, this.player));
        }
    }

    @Override
    public @Nullable String styledNicknames$get() {
        return this.styledNicknames$nickname;
    }

    @Override
    public @Nullable Component styledNicknames$getParsed() {
        return this.styledNicknames$parsedNicknameRaw;
    }

    @Override
    public @Nullable MutableComponent styledNicknames$getOutput() {
        return this.styledNicknames$parsedNicknameRaw != null ? (MutableComponent) Placeholders.parseText(ConfigManager.getConfig().nicknameFormat, Placeholders.PREDEFINED_PLACEHOLDER_PATTERN, Map.of("nickname", this.styledNicknames$parsedNicknameRaw, "name", this.styledNicknames$parsedNicknameRaw)) : null;
    }

    @Override
    public MutableComponent styledNicknames$getOutputOrVanilla() {
        return this.styledNicknames$parsedNicknameRaw != null ? (MutableComponent) Placeholders.parseText(ConfigManager.getConfig().nicknameFormat, Placeholders.PREDEFINED_PLACEHOLDER_PATTERN, Map.of("nickname", this.styledNicknames$parsedNicknameRaw, "name", this.styledNicknames$parsedNicknameRaw)) : this.player.getName().copy();
    }

    @Override
    public boolean styledNicknames$requiresPermission() {
        return this.styledNicknames$requirePermission;
    }

    @Override
    public boolean styledNicknames$shouldDisplay() {
        return this.styledNicknames$parsedNicknameRaw != null && (!this.styledNicknames$requirePermission || player.hasPermissions(ConfigManager.getConfig().configData.allowByDefault ? 0 : 3));
    }

    @Override
    public Map<String, Component> styledNicknames$placeholdersCommand() {
        var name = this.styledNicknames$getOutputOrVanilla();
        return Map.of("nickname", name, "name", name);
    }
}

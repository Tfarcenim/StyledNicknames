package eu.pb4.stylednicknames.mixin;

import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.placeholders.api.parsers.TextParserV1;
import eu.pb4.playerdata.api.PlayerDataApi;
import eu.pb4.stylednicknames.NicknameHolder;
import eu.pb4.stylednicknames.config.Config;
import eu.pb4.stylednicknames.config.ConfigManager;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.StringTag;
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

import static eu.pb4.stylednicknames.StyledNicknames.id;


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
            StringTag nickname = PlayerDataApi.getGlobalDataFor(player, id("nickname"), StringTag.TYPE);
            ByteTag permissions = PlayerDataApi.getGlobalDataFor(player, id("permission"),ByteTag.TYPE);

            if (nickname != null) {
                this.styledNicknames$set(nickname.getAsString(), permissions.getAsByte() > 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void styledNicknames$set(String nickname, boolean requirePermission) {
        Config config = ConfigManager.getConfig();
        CommandSourceStack source = player.createCommandSourceStack();
        if (nickname == null || nickname.isEmpty() || (requirePermission && !Permissions.check(source, "stylednicknames.use", ConfigManager.getConfig().configData.allowByDefault ? 0 : 2))) {
            this.styledNicknames$nickname = null;
            this.styledNicknames$requirePermission = false;
            this.styledNicknames$parsedNicknameRaw = null;
            PlayerDataApi.setGlobalDataFor(this.player, id("nickname"), null);
            PlayerDataApi.setGlobalDataFor(this.player, id("permission"), ByteTag.valueOf(false));
        } else {
            this.styledNicknames$nickname = nickname;
            this.styledNicknames$requirePermission = requirePermission;
            PlayerDataApi.setGlobalDataFor(this.player, id("nickname"), StringTag.valueOf(nickname));
            PlayerDataApi.setGlobalDataFor(this.player, id("permission"), ByteTag.valueOf(requirePermission));

            var handlers = new HashMap<String, TextParserV1.TagNodeBuilder>();


            for (var entry : TextParserV1.SAFE.getTags()) {
                if ((config.defaultFormattingCodes.getBoolean(entry.name())
                        || Permissions.check(this.player, "stylednicknames.format." + entry.name(), 2))) {

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
        return this.styledNicknames$parsedNicknameRaw != null && (!this.styledNicknames$requirePermission || Permissions.check(this.player, "stylednicknames.use", ConfigManager.getConfig().configData.allowByDefault ? 0 : 3));
    }

    @Override
    public Map<String, Component> styledNicknames$placeholdersCommand() {
        var name = this.styledNicknames$getOutputOrVanilla();
        return Map.of("nickname", name, "name", name);
    }
}

package eu.pb4.stylednicknames.mixin;

import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.placeholders.api.parsers.TextParserV1;
import eu.pb4.playerdata.api.PlayerDataApi;
import eu.pb4.stylednicknames.MixinHooks;
import eu.pb4.stylednicknames.NicknameHolder;
import eu.pb4.stylednicknames.StyledNicknames;
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
            StringTag nickname = PlayerDataApi.getGlobalDataFor(player, StyledNicknames.id("nickname"), StringTag.TYPE);
            ByteTag permissions = PlayerDataApi.getGlobalDataFor(player, StyledNicknames.id("permission"),ByteTag.TYPE);

            if (nickname != null) {
                this.styledNicknames$set(nickname.getAsString(), permissions.getAsByte() > 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void styledNicknames$set(String nickname, boolean requirePermission) {
        MixinHooks.styledNicknames$set((ServerGamePacketListenerImpl)(Object) this,nickname,requirePermission);
    }

    @Override
    public void directlySetNickname(String nickname) {
        styledNicknames$nickname = nickname;
    }

    @Override
    public void directlySetParsedNickname(Component nickname) {
        styledNicknames$parsedNicknameRaw = nickname;
    }

    @Override
    public void setRequirePermission(boolean permission) {
        this.styledNicknames$requirePermission = permission;
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

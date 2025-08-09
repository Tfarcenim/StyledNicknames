package eu.pb4.stylednicknames;

import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.stylednicknames.config.ConfigManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class NickUtils {


    public static Component getDisplayNickName(Player player,Component fallback) {
        Component component = getStyledNickname(player);
        if (shouldDisplay(player)) {
            return component;
        }
        return fallback;
    }

    public static CompoundTag getSideSafeData(Player player) {
        if (player instanceof ServerPlayer) {
            return player.getPersistentData();
        } else {
            return StyledNicknamesForge.Client.dataMap.getOrDefault(player.getUUID(),new CompoundTag());
        }
    }

    public static void setPermission(ServerPlayer player, byte permissions) {
        CompoundTag persistantData = getSideSafeData(player).getCompound(ServerPlayer.PERSISTED_NBT_TAG);
        persistantData.putByte(PERM_KEY,permissions);
        ensureSaved(player,persistantData);
    }

    static void ensureSaved(ServerPlayer player,CompoundTag persistentData) {
        if (!player.getPersistentData().contains(Player.PERSISTED_NBT_TAG)) {
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG,persistentData);
        }
    }

    public static void setNickname(ServerPlayer player, String nickname) {
        CompoundTag persistantData = getSideSafeData(player).getCompound(ServerPlayer.PERSISTED_NBT_TAG);
        if (nickname == null) {
            persistantData.remove(NICK_KEY);
            System.out.println(persistantData);
        } else {
            persistantData.putString(NICK_KEY,nickname);
            ensureSaved(player,persistantData);
        }
    }

    public static String getNickname(Player player) {
        return getSideSafeData(player).getCompound(ServerPlayer.PERSISTED_NBT_TAG).getString(NICK_KEY);
    }

    static final String STYLED_NICK_KEY = StyledNicknames.id("styled_nickname").toString();
    static final String PERM_KEY = StyledNicknames.id("permission").toString();
    static final String NICK_KEY = StyledNicknames.id("nickname").toString();

    public static Component getStyledNickname(Player player) {
        CompoundTag persistantData = getSideSafeData(player).getCompound(ServerPlayer.PERSISTED_NBT_TAG);

        if (persistantData.contains(STYLED_NICK_KEY)) {
            return Component.Serializer.fromJson(persistantData.getString(STYLED_NICK_KEY));
        } else {
            return null;
        }
    }

    public static void setStyledNickname(ServerPlayer player,Component styledNickname) {
        CompoundTag persistantData = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (styledNickname != null) {
            persistantData.putString(STYLED_NICK_KEY, Component.Serializer.toJson(styledNickname));
            ensureSaved(player,persistantData);
        } else {
            persistantData.remove(STYLED_NICK_KEY);
        }
    }

    public static byte getPermission(Player player) {
        return getSideSafeData(player).getCompound(ServerPlayer.PERSISTED_NBT_TAG).getByte(PERM_KEY);
    }

    @Nullable
    public static MutableComponent getOutputOrVanilla(Player player,boolean fallback) {

        Component parsedNick = getStyledNickname(player);

        return parsedNick != null ?
                (MutableComponent) Placeholders.parseText(ConfigManager.getConfig().nicknameFormat, Placeholders.PREDEFINED_PLACEHOLDER_PATTERN,
                        Map.of("nickname", parsedNick, "name", parsedNick)) : (fallback ? player.getName().copy() : null);
    }

    public static boolean shouldDisplay(Player player) {
        Component parsedNick = getStyledNickname(player);

        return parsedNick != null && (!(NickUtils.getPermission(player)>0) || player.hasPermissions(ConfigManager.getConfig().configData.allowByDefault ? 0 : 3));
    }

    public static Map<String, Component> placeholdersCommand(ServerPlayer player) {
        var name = getOutputOrVanilla(player,true);
        return Map.of("nickname", name, "name", name);
    }

    public static void loadNicknameData(ServerPlayer player) {
        try {
            String nickname = NickUtils.getNickname(player);
            byte permissions =  NickUtils.getPermission(player);

            if (!nickname.isBlank()) {
                MixinHooks.styledNicknames$set(player,nickname, permissions > 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
        @Override
    public void styledNicknames$loadData() {
        try {
            String nickname = NickUtils.getNickname(player);
            byte permissions =  NickUtils.getPermission(player);

            if (!nickname.isBlank()) {
                this.styledNicknames$set(nickname, permissions > 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
     */

}

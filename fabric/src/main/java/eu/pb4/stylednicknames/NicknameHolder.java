package eu.pb4.stylednicknames;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface NicknameHolder {
    NicknameHolder EMPTY = new NicknameHolder() {
        @Override
        public void styledNicknames$set(String nickname, boolean requirePermission) {
        }

        @Override
        public @Nullable String styledNicknames$get() {
            return null;
        }

        @Override
        public void directlySetNickname(String nickname) {

        }

        @Override
        public void directlySetParsedNickname(Component nickname) {

        }

        @Override
        public void setRequirePermission(boolean permission) {

        }

        @Override
        public @Nullable Component styledNicknames$getParsed() {
            return null;
        }

        @Override
        public @Nullable MutableComponent styledNicknames$getOutput() {
            return null;
        }

        @Override
        public MutableComponent styledNicknames$getOutputOrVanilla() {
            return Component.empty();
        }

        @Override
        public boolean styledNicknames$requiresPermission() {
            return false;
        }

        @Override
        public void styledNicknames$loadData() {}

        @Override
        public boolean styledNicknames$shouldDisplay() {
            return false;
        }

        @Override
        public Map<String, Component> styledNicknames$placeholdersCommand() {
            return Map.of("nickname", Component.empty(), "name", Component.empty());
        }
    };

    static NicknameHolder of(ServerPlayer player) {
        return (NicknameHolder) player.connection;
    }

    static NicknameHolder of(ServerGamePacketListenerImpl handler) {
        return (NicknameHolder) handler;
    }

    static NicknameHolder of(Object possiblePlayer) {
        if (possiblePlayer instanceof ServerPlayer player) {
            return (NicknameHolder) player.connection;
        }
        return EMPTY;
    }

    void styledNicknames$set(String nickname, boolean requirePermission);

    @Nullable
    String styledNicknames$get();

    void directlySetNickname(String nickname);
    void directlySetParsedNickname(Component nickname);
    void setRequirePermission(boolean permission);

    @Nullable
    Component styledNicknames$getParsed();

    @Nullable
    MutableComponent styledNicknames$getOutput();

    MutableComponent styledNicknames$getOutputOrVanilla();

    boolean styledNicknames$requiresPermission();

    void styledNicknames$loadData();

    boolean styledNicknames$shouldDisplay();

    Map<String, Component> styledNicknames$placeholdersCommand();


    // Kept for switchy so it won't break
    @Deprecated
    default String sn_get() {
        return this.styledNicknames$get();
    }

    // Kept for switchy so it won't break
    @Deprecated
    default void sn_set(String input, boolean permission) {
        this.styledNicknames$set(input, permission);
    }
}

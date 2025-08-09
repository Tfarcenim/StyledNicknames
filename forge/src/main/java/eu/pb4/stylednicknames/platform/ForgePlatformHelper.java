package eu.pb4.stylednicknames.platform;

import eu.pb4.stylednicknames.PacketHandlerForge;
import eu.pb4.stylednicknames.StyledNicknamesForge;
import eu.pb4.stylednicknames.network.client.S2CModPacket;
import eu.pb4.stylednicknames.network.client.S2CSyncPersistantDataPacket;
import eu.pb4.stylednicknames.platform.services.IPlatformHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

import java.nio.file.Path;
import java.util.function.Function;

public class ForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {

        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return !FMLLoader.isProduction();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void sendToClient(S2CModPacket msg, ServerPlayer player) {
        PacketHandlerForge.sendToClient(msg,player);
    }

    @Override
    public void sendToTrackingClients(S2CModPacket msg, Entity entity) {
        PacketHandlerForge.INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity),msg);
    }

    int i;

    @Override
    public <MSG extends S2CModPacket> void registerClientPacket(Class<MSG> packetLocation, Function<FriendlyByteBuf, MSG> reader) {
        PacketHandlerForge.INSTANCE.registerMessage(i++, packetLocation, MSG::write, reader, PacketHandlerForge.wrapS2C());
    }

    @Override
    public void handle(S2CSyncPersistantDataPacket s2CSyncPersistantDataPacket) {
        StyledNicknamesForge.Client.dataMap.put(s2CSyncPersistantDataPacket.playerUUID(),s2CSyncPersistantDataPacket.data());
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            Player player = level.getPlayerByUUID(s2CSyncPersistantDataPacket.playerUUID());
            if (player != null) {
                player.refreshDisplayName();
            }
        }
    }
}
package eu.pb4.stylednicknames.network;

import eu.pb4.stylednicknames.StyledNicknames;
import eu.pb4.stylednicknames.network.client.S2CSyncPersistantDataPacket;
import eu.pb4.stylednicknames.platform.Services;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public class PacketHandler {

    public static void registerPackets() {
        Services.PLATFORM.registerClientPacket(S2CSyncPersistantDataPacket.class, S2CSyncPersistantDataPacket::new);
    }

    public static ResourceLocation packet(Class<?> clazz) {
        return StyledNicknames.id(clazz.getName().toLowerCase(Locale.ROOT));
    }

}

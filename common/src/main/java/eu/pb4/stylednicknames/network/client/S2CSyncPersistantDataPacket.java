package eu.pb4.stylednicknames.network.client;

import eu.pb4.stylednicknames.platform.Services;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record S2CSyncPersistantDataPacket(UUID playerUUID, CompoundTag data) implements S2CModPacket {
    public S2CSyncPersistantDataPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(),buf.readNbt());
    }

    @Override
    public void handleClient() {
        Services.PLATFORM.handle(this);
    }

    @Override
    public void write(FriendlyByteBuf to) {
        to.writeUUID(playerUUID);
        to.writeNbt(data);
    }
}

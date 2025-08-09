package eu.pb4.stylednicknames.network.client;


import eu.pb4.stylednicknames.network.ModPacket;

public interface S2CModPacket extends ModPacket {
    void handleClient();
}

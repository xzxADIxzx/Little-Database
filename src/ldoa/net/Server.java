package ldoa.net;

import mindustry.net.ArcNetProvider;

public class Server extends arc.net.Server {

    public Server() {
        super(32768, 8192, new ArcNetProvider.PacketSerializer());
    }
}
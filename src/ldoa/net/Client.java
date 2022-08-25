package ldoa.net;

import arc.func.Cons;

public class Client extends arc.net.Client {

    public Client() {
        super(8192, 8192, new PacketSerializer());
    }

    public void send(String request, Cons<Object> response) {
        sendTCP(request); // TODO response
    }
}

package ldoa.net;

import arc.func.Cons;
import arc.util.Log;
import ldoa.net.ResponseMessage.*;

import static ldoa.Main.*;

public class Client extends arc.net.Client {

    public Client() {
        super(8192, 8192, new PacketSerializer());
    }

    public void send(String request, Cons<Object> response) {
        sendTCP(request); // TODO response
    }

    public void authorize() {
        send(login + " " + password, res -> {
            if (res instanceof RequestSuccess req) Log.info("Successfully logged with login: @ and password: @", login, password);
            else if (res instanceof RequestException req) Log.err("Could not to login: @", req.response);
            else throw new RuntimeException("Unknown response!");
        });
    }
}

package ldoa.net;

import arc.func.Cons;
import arc.util.Log;
import ldoa.net.ResponseMessage.*;

import static ldoa.Main.*;

public class Client extends arc.net.Client {

    /** Represents folder and exists to work with database files. */
    public final JsonShell root = new JsonShell("root");

    public Client() {
        super(8192, 8192, new PacketSerializer());
    }

    public void send(String request, Cons<Object> response) {
        sendTCP(request); // TODO response
    }

    public void authorize() {
        if (login == null || password == null) return;
        send(login + " " + password, res -> {
            if (res instanceof RequestSuccess req) Log.info("Successfully logged with login: @ and password: @", login, password);
            else if (res instanceof RequestException req) Log.err("Could not to login: @", req.response);
            else throw new RuntimeException("Unknown response!");
        });
    }

    // region file managment

    public void create(String name, Cons<Object> response) {
        root.putAsync(name, "{}", response);
    }

    public JsonShell get(String name) {
        return new JsonShell(name);
    }

    public void remove(String name, Cons<Object> response) {
        root.removeAsync(name, response);
    }

    // endregion
}

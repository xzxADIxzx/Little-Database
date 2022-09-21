package ldoa.net;

import arc.func.Cons;
import arc.net.Connection;
import arc.net.NetListener;
import arc.struct.IntMap;
import arc.util.Log;
import ldoa.Control;
import ldoa.net.ResponseMessage.*;

import static ldoa.Main.*;

public class Client extends arc.net.Client implements NetListener {

    /** Represents folder and exists to work with database files. */
    public final JsonShell root = new JsonShell("root");
    public final IntMap<Cons<Object>> responses = new IntMap<>();

    public Client() {
        super(8192, 8192, new PacketSerializer());
        addListener(this);
    }

    public void send(String request, Cons<Object> response) {
        sendTCP(request);
        responses.put(ResponseMessage.id++, response);
    }

    /** For internal use in {@link Control}. */
    public void authorize() {
        if (login == null || password == null) return;
        send(login + " " + password, res -> {
            if (res instanceof RequestSuccess req) Log.info("Successfully logged with login: @ and password: @", login, password);
            else if (res instanceof RequestException req) Log.err("Could not to login.");
            else throw new RuntimeException("Unknown response!");
        });
    }

    /** For external use in plugins or libraries. */
    public void authorize(String login, String password, Cons<Object> response) {
        send(login + " " + password, response);
    }

    // region file managment

    public JsonShell get(String name) {
        return new JsonShell(name);
    }

    public void create(String name, Cons<Object> response) {
        root.putAsync(name, "{}", response);
    }

    public void delete(String name, Cons<Object> response) {
        root.removeAsync(name, response);
    }

    public void exists(String name, Cons<Object> response) {
        root.containsAsync(name, response);
    }

    // endregion

    public void connected(Connection connection) {
        ResponseMessage.id = 0;
    }

    public void received(Connection connection, Object object) {
        if (object instanceof ResponseMessage res) responses.remove(res.requestID).get(res);
    }
}

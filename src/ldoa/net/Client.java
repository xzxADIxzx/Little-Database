package ldoa.net;

import arc.net.Connection;
import arc.net.NetListener;
import arc.struct.IntMap;
import arc.util.Log;
import ldoa.Control;
import ldoa.net.ResponseMessage.*;
import useful.Json;

import static ldoa.Main.*;

public class Client extends arc.net.Client implements NetListener {

    /** Represents folder and exists to work with database files. */
    public final JsonShell root = new JsonShell("root");

    /** Contains all requests callbacks by their id. */
    public final IntMap<ResponseCons> responses = new IntMap<>();

    public Client() {
        super(8192, 8192, new PacketSerializer());
        addListener(this);
    }

    /** Sends LDR to server and returns a {@link ResponseMessage}. */
    public void send(String request, ResponseCons response) {
        sendTCP(request);
        responses.put(ResponseMessage.id++, response);
    }

    /** For internal use in {@link Control}. */
    public void authorize() {
        if (login == null || password == null) return;
        send(login + " " + password, res -> {
            if (res instanceof RequestSuccess) Log.info("Successfully logged with login: @ and password: @", login, password);
            else if (res instanceof RequestException) Log.err("Could not to login.");
            else throw new RuntimeException("Unknown response!");
        });
    }

    /** For external use in plugins or libraries. */
    public void authorize(String login, String password, ResponseCons response) {
        send(login + " " + password, response);
    }

    // region file managment

    public JsonShell get(String name) {
        return new JsonShell(name);
    }

    public void create(String name, ResponseCons response) {
        root.put(name, new Json()).cons(response);
    }

    public void delete(String name, ResponseCons response) {
        root.remove(name).cons(response);
    }

    public void exists(String name, ResponseCons response) {
        root.contains(name).cons(response);
    }

    // endregion

    public void connected(Connection connection) {
        ResponseMessage.id = 0;
    }

    public void received(Connection connection, Object object) {
        if (object instanceof ResponseMessage message) responses.remove(message.requestID).get(message);
    }
}

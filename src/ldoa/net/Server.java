package ldoa.net;

import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Timer;

import static arc.Core.*;
import static ldoa.Main.*;

public class Server extends arc.net.Server implements NetListener {

    public Database database = new Database();

    public Seq<Connection> authorized = new Seq<>(); // TODO ConnectionMap instead of this ObjectMaps and Seq
    public ObjectMap<Connection, Seq<String>> tasks = new ObjectMap<>();
    public ObjectMap<Connection, String> reasons = new ObjectMap<>();

    public Server() {
        super(32768, 8192, new PacketSerializer());
        addListener(this);
    }

    @Override
    public void run() {
        database.load("database");
        super.run(); // called after the database as it stops the thread
    }

    @Override
    public void stop() {
        super.stop();
        database.save("database");

        authorized.clear();
        tasks.clear();
    }

    public void connected(Connection connection) {
        Log.info("Received connection @.", connection.getID());

        if (login == null || password == null) return; // allow all connections unless the database is locked
        Timer.schedule(() -> {
            if (authorized.contains(connection)) return; // connection was authorized, so there is no sense to disconnecting it
            reasons.put(connection, "login and password were not provided.");
            connection.close(DcReason.closed);
        }, 5f);
    }

    public void disconnected(Connection connection, DcReason reason) {
        authorized.remove(connection);
        tasks.remove(connection);
        Log.info("Connection @ was closed@", connection.getID(), reasons.containsKey(connection) ? ": " + reasons.get(connection) : ".");
    }

    public void received(Connection connection, Object object) {
        if (object instanceof String message) {
            String[] args = message.split(" ");
            if (args.length != 2) {
                if (authorized.contains(connection)) database.execute(connection, message);
            } else {
                if (login == null || password == null) return; // it makes no sense to check the login and password if the database is not locked
                if (args[0].equals(login) && args[1].equals(password)) {
                    Log.info("Received correct login and password from connection @.", connection.getID());
                    authorized.add(connection);
                } else {
                    reasons.put(connection, "login or password is incorrect.");
                    app.post(() -> connection.close(DcReason.closed));
                }
            }
        }
    }

    public void idle(Connection connection) {} // who needs this?
}

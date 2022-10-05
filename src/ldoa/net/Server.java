package ldoa.net;

import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Timer;
import ldoa.net.ResponseMessage.*;

import static arc.Core.*;
import static ldoa.Main.*;

public class Server extends arc.net.Server implements NetListener {

    public Database database = new Database();
    public ConnectionMap connections = new ConnectionMap();

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

        database.jsons.clear();
        connections.clear();
    }

    public void connected(Connection connection) {
        Log.info("Received connection @.", connection.getID());

        if (login == null || password == null) return; // allow all connections unless the database is locked
        Timer.schedule(() -> {
            if (connections.authorized(connection)) return; // connection was authorized, so there is no sense to disconnecting it
            connections.disconnect(connection, "login and password were not provided");
        }, 5f); // exception packet not sent as client is not waiting for
    }

    public void disconnected(Connection connection, DcReason reason) {
        ResponseMessage.ids.remove(connection.getID()); // not essential, but saves memory

        connections.disconnected(connection);
        Log.info("Connection @ was closed: @.", connection.getID(), connections.reason(connection));
    }

    public void received(Connection connection, Object object) {
        if (object instanceof String message) {
            if (connections.authorized(connection)) connection.sendTCP(database.execute(connection, message));
            else {
                String[] args = message.split(" ");
                if (args[0].equals(login) && args[1].equals(password)) {
                    connections.authorize(connection);
                    Log.info("Received correct login and password from connection @.", connection.getID());
                } else {
                    connections.disconnect(connection, "login or password is incorrect");
                    new RequestException("Goodbye :3").send(connection);
                }
            }
        }
    }

    public void idle(Connection connection) {} // who needs this?

    public class ConnectionMap {

        public Seq<Connection> authorized = new Seq<>();
        public IntMap<Seq<String>> tasks = new IntMap<>();
        public IntMap<String> reasons = new IntMap<>();

        public void authorize(Connection connection) {
            authorized.add(connection);
            new RequestSuccess("Hi :3").send(connection);
        }

        public boolean authorized(Connection connection) { // it makes no sense to check authorization if the database is not locked
            return authorized.contains(connection) || login == null || password == null;
        }

        public void disconnect(Connection connection, String reason) {
            reasons.put(connection.getID(), reason);
            app.post(() -> connection.close(DcReason.closed));
        }

        public void disconnected(Connection connection) {
            authorized.remove(connection);
            tasks.remove(connection.getID());
        }

        public String reason(Connection connection) {
            String reason = reasons.remove(connection.getID());
            return reason == null ? "disconnected by client" : reason;
        }

        public void clear() {
            authorized.clear();
            tasks.clear();
        }
    }
}

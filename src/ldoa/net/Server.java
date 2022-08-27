package ldoa.net;

import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Timer;

public class Server extends arc.net.Server implements NetListener {

    public Seq<Connection> authorized = new Seq<>();
    public ObjectMap<Connection, Seq<String>> tasks = new ObjectMap<>();

    public Server() {
        super(32768, 8192, new PacketSerializer());
        addListener(this);
    }

    @Override
    public void stop() {
        super.stop();
        authorized.clear();
        tasks.clear();
    }

    public void execute(String request) {} // TODO maaany things

    public void connected(Connection connection) {
        Timer.schedule(() -> {
            if (authorized.contains(connection)) connection.close(DcReason.closed);
        }, 5f);
    }

    public void disconnected(Connection connection, DcReason reason) {
        authorized.remove(connection);
        tasks.remove(connection);
    }

    public void received(Connection connection, Object object) {} // TODO handle ldrs

    public void idle(Connection connection) {} // who needs this?
}

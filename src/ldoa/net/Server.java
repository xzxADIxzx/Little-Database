package ldoa.net;

import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;

public class Server extends arc.net.Server implements NetListener {

    public Server() {
        super(32768, 8192, new PacketSerializer());
        addListener(this);
    }

    public void connected(Connection connection) {} // TODO check login and password or is it a local connection

    public void disconnected(Connection connection, DcReason reason) {} // TODO stop all tasks created by this connection

    public void received(Connection connection, Object object) {} // TODO handle ldrs

    public void idle(Connection connection) {} // who needs this?
}

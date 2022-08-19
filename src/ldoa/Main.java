package ldoa;

import java.io.IOException;

import arc.util.Log;
import arc.util.Threads;

public class Main {

    public static final Server server = new Server();
    public static final Client client = new Client();

    public static Thread serverThread;
    public static Thread clientThread;

    public static void main(String[] args) {
        try {
            server.bind(6567, 6567);

            serverThread = Threads.daemon("Net Server", server::run);
            clientThread = Threads.daemon("Net Client", client::run);

            client.connect(5000, "127.0.0.1", 6567, 6567);
        } catch (IOException error) { Log.err("Could not to create server/client", error); }
    }
}

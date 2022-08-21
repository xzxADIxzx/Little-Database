package ldoa;

import arc.backend.headless.HeadlessApplication;
import arc.util.Log;
import arc.util.Threads;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {

    public static final Server server = new Server();
    public static final Client client = new Client();

    public static final String[] tags = { "&lc&fb[D]&fr", "&lb&fb[I]&fr", "&ly&fb[W]&fr", "&lr&fb[E]", "" };
    public static final DateTimeFormatter dateTime = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public static Thread serverThread;
    public static Thread clientThread;

    public static void main(String[] args) {
        Log.logger = (level, text) -> {
            String result = Log.format("&lk&fb[" + dateTime.format(LocalDateTime.now()) + "]&fr " + tags[level.ordinal()] + " " + text + "&fr");
            System.out.println(result);
        };

        try {
            server.bind(6567, 6567);

            serverThread = Threads.daemon("Net Server", server::run);
            clientThread = Threads.daemon("Net Client", client::run);

            client.connect(5000, "127.0.0.1", 6567, 6567);

            new HeadlessApplication(new Control(), Log::err);
            Log.info("Little Database application loaded. Type @ for help.", "help");
        } catch (IOException error) {
            Log.err("Could not to load Little Database application", error);
        }
    }
}

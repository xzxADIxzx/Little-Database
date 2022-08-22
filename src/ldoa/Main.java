package ldoa;

import arc.backend.headless.HeadlessApplication;
import arc.util.Log;
import ldoa.net.Client;
import ldoa.net.Server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {

    public static final Server server = new Server();
    public static final Client client = new Client();

    public static final String[] tags = { "&lc&fb[D]&fr", "&lb&fb[I]&fr", "&ly&fb[W]&fr", "&lr&fb[E]", "" };
    public static final DateTimeFormatter dateTime = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public static void main(String[] args) {
        Log.logger = (level, text) -> {
            String result = Log.format("&lk&fb[" + dateTime.format(LocalDateTime.now()) + "]&fr " + tags[level.ordinal()] + " " + text + "&fr");
            System.out.println(result);
        };

        try {
            Log.info("Little Database application loaded. Type @ for help.", "help");
            new HeadlessApplication(new Control(args), Log::err);
        } catch (Throwable error) {
            Log.err("Could not to load Little Database application", error);
        }
    }
}

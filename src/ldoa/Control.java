package ldoa;

import arc.ApplicationListener;
import arc.util.CommandHandler;
import arc.util.CommandHandler.CommandResponse;
import arc.util.CommandHandler.ResponseType;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Threads;

import static ldoa.Main.*;

import java.io.IOException;

public class Control implements ApplicationListener {

    public final CommandHandler handler = new CommandHandler("");
    public final String[] startCommands;

    public Thread thread;

    public Control(String[] args) {
        this.startCommands = args;
    }

    @Override
    public void init() {
        registerCommands();
        for (String command : startCommands)
            handleCommand(command);
    }

    private void registerCommands() {
        handler.register("host", "<port> <public/private>", "Host a new little database.", args -> {
            try {
                server.bind(6567, 6567);
                thread = Threads.daemon("Net Server", server::run);
            } catch (IOException error) {
                Log.err("Could not to host a little database", error);
            }
        });

        handler.register("join", "<ip> <port>", "Join to a little database.", args -> {
            try {
                thread = Threads.daemon("Net Client", client::run);
                client.connect(5000, "127.0.0.1", 6567, 6567);
            } catch (IOException error) {
                Log.err("Could not to join to a little database", error);
            }
        });
    }

    private void handleCommand(String command) {
        CommandResponse response = handler.handleMessage(command);

        if (response.type == ResponseType.unknownCommand) {
            String closest = handler.getCommandList().map(cmd -> cmd.text).min(cmd -> Strings.levenshtein(cmd, command));
            Log.err("Command not found. Did you mean @?", closest);
        } else if (response.type != ResponseType.noCommand && response.type != ResponseType.valid)
            Log.err("Too @ command arguments.", response.type == ResponseType.fewArguments ? "few" : "many");
    }
}

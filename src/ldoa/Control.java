package ldoa;

import arc.ApplicationListener;
import arc.util.CommandHandler;
import arc.util.CommandHandler.CommandResponse;
import arc.util.CommandHandler.ResponseType;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Threads;

import static arc.Core.*;
import static ldoa.Main.*;

import java.io.IOException;
import java.util.Scanner;

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

        Threads.daemon("Application Control", () -> {
            try (Scanner scanner = new Scanner(System.in)) {
                while (scanner.hasNext()) handleCommand(scanner.nextLine());
            }
        });
    }

    private void registerCommands() {
        handler.register("help", "Display the command list.", args -> {
            Log.info("Commands:");
            handler.getCommandList().each(command -> Log.info("  &b&lb @&lc&fi@&fr - &lw@",
                    command.text, command.paramText.isEmpty() ? "" : " " + command.paramText, command.description));
        });

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

        handler.register("stop", "Stop hosting the server or disconnect the client.", arg -> {
            if (thread == null) Log.err("No server/client launched yet.");
            else {
                server.stop();
                client.stop();
                thread = null;
                Log.info("Stopped server.");
            }
        });

        handler.register("exit", "Exit the Little Database application.", arg -> {
            Log.info("Shutting down Little Database application.");
            try {
                server.dispose();
                client.dispose();
            } catch (IOException ignored) {}
            app.exit();
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

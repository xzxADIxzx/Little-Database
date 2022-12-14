package ldoa;

import arc.ApplicationListener;
import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;
import arc.util.CommandHandler;
import arc.util.CommandHandler.CommandResponse;
import arc.util.CommandHandler.ResponseType;
import ldoa.net.ResponseMessage;
import ldoa.net.ResponseMessage.*;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Threads;

import static arc.Core.*;
import static ldoa.Main.*;

import java.io.IOException;
import java.time.LocalDateTime;
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

        client.addListener(new NetListener() { // located here because Client can be used in third party plugins or libraries with own logging
            public void connected(Connection connection) {
                Log.info("Successfully connected to the server.");
            }
        
            public void disconnected(Connection connection, DcReason reason) {
                Log.info("Connection was closed.");
                client.stop();
                thread = null;
            }
        });

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

        handler.register("host", "<port> <public/private> [login] [password]", "Host a new little database.", args -> {
            if (thread != null) Log.err("The server/client is already launched.");
            else try {
                boolean isLAN = args[1].equals("private");
                handleLoginPassword(args);
                    
                server.bind(6567, 6567);
                thread = Threads.daemon("Net Server", server::run);
                Log.info("Server launched.");

                if (args.length > 2 && isLAN) Log.warn("Login and password are ignored as the server is private.");
            } catch (IOException error) {
                Log.err("Could not to host a little database", error);
            }
        });

        handler.register("join", "<ip> <port> [login] [password]", "Join to a little database.", args -> {
            if (thread != null) Log.err("The server/client is already launched.");
            else try {
                // join also has 4 arguments so this will work
                handleLoginPassword(args);

                thread = Threads.daemon("Net Client", client::run);
                client.connect(5000, "127.0.0.1", 6567, 6567);
                Log.info("Client launched.");

                client.authorize();
            } catch (IOException error) {
                Log.err("Could not to join to a little database", error);
            }
        });

        handler.register("stop", "Stop hosting the server or disconnect the client.", args -> {
            if (thread == null) Log.err("No server/client launched yet.");
            else {
                server.stop();
                client.stop();
                thread = null;
                Log.info("Stopped server/client.");
            }
        });

        handler.register("exit", "Exit the Little Database application.", args -> {
            Log.info("Shutting down Little Database application.");
            try {
                server.stop();
                server.dispose();
                client.stop();
                client.dispose();
            } catch (IOException ignored) {}
            app.exit();
        });

        handler.register("send", "<request...>", "Send a LDR or execute it locally.", args -> {
            if (thread == null) Log.err("No server/client launched yet.");
            else {
                if (thread.getName().equals("Net Client")) client.send(args[0], this::handleResponse);
                else handleResponse(server.database.executeResponse(null, args[0]));
            }
        });

        handler.register("backup", "Take a little backup of a little database.", args -> {
            if (thread == null || !thread.getName().equals("Net Server")) Log.err("No server launched.");
            else server.database.save("backup " + dateTime.format(LocalDateTime.now()));
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

    private void handleLoginPassword(String[] args) {
        switch (args.length) {
            case 2 -> login = password = null;
            case 3 -> {
                login = args[2];
                password = "DoYouEvenRealizeHowStupidThisIs?";
                Log.warn("Password not found! @ will be used by default.", password);
            }
            case 4 -> {
                login = args[2];
                password = args[3];
            }
        }
    }

    private void handleResponse(ResponseMessage response) {
        if (response instanceof RequestSuccess req) Log.info(req.response);
        if (response instanceof RequestException req) Log.err(req.response);
    }
}

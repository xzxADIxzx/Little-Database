package ldoa;

import arc.ApplicationListener;
import arc.util.CommandHandler;
import arc.util.CommandHandler.CommandResponse;
import arc.util.CommandHandler.ResponseType;
import arc.util.Log;
import arc.util.Strings;

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

        });

        handler.register("join", "<ip> <port>", "Join to a little database.", args -> {

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

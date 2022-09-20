package ldoa.net;

import arc.files.Fi;
import arc.net.Connection;
import arc.struct.ObjectMap;
import arc.util.Log;
import ldoa.net.ResponseMessage.*;
import useful.Json;
import useful.Json.JsonStyle;

import static arc.Core.*;

public class Database {

    public static final Object requestComplete = new Object();

    public final ObjectMap<String, Json> jsons = new ObjectMap<>();
    public Object context = requestComplete;

    public void load(String from) {
        jsons.clear();
        Fi root = files.local(from);

        if (root.exists()) {
            if (root.isDirectory()) root.walk(this::read);
            else throw new RuntimeException("Database subdirectory cannot be created as a file with the same name already exists!");
        } else root.mkdirs();

        Log.info("Loaded @ database files from @.", jsons.size, root.absolutePath());
    }

    public void save(String to) {
        if (jsons.isEmpty()) return;
        Fi root = files.local(to);

        if (root.exists()) {
            if (!root.isDirectory()) throw new RuntimeException("Cannot save to /" + to + " because the root folder contains a file with that name!");
        } else root.mkdirs();

        jsons.each((key, value) -> write(root.child(key), value));
        Log.info("Saved @ database files to @.", jsons.size, root.absolutePath());
    }

    public Object execute(Connection connection, String request) {
        if (context == requestComplete) { // method called from outside
            String[] file = request.split(" ", 2);
            context = jsons.get(file[0]); // ldr start with filename

            try {
                if (context == null) return new RequestException(connection, "File not found!");
                if (file.length == 1) return new RequestException(connection, "Invalid LDR!");
                return execute(connection, file[1]);
            } finally { context = requestComplete; }
        } else {
            String[] command = request.split(" ", 2);
            if (command.length == 1) return new RequestException(connection, "Too few arguments!");

            switch (command[0]) { // Java gods will punish me for this
                case "get" -> {
                    if (context instanceof Json json) {
                        String[] args = command[1].split(" ", 2);
                        context = json.get(args[0]);

                        if (args.length == 1) return new RequestSuccess(connection, context == null ? null : Json.write(context, JsonStyle.standard));
                        else return execute(connection, args[1]);
                    } else return new RequestException(connection, "Can not get value from non-json object!");
                }
                case "put" -> {
                    if (context instanceof Json json) {
                        String[] args = command[1].split(" ", 2);

                        if (args.length == 1) return new RequestException(connection, "Too few arguments!");
                        else try {
                            json.put(args[0], Json.readAs(args[1]));
                            return new RequestSuccess(connection, args[1]);
                        } catch (RuntimeException error) { // unknown field type
                            return new RequestException(connection, error.getMessage());
                        }
                    } else return new RequestException(connection, "Can not put value to non-json object!");
                }
                default -> {
                    return new RequestException(connection, "Invalid LDR!");
                }
            }
        }
    }

    private void read(Fi file) {
        jsons.put(file.nameWithoutExtension(), Json.read(file.readString()));
    }

    private void write(Fi file, Json json) {
        file.writeString(json.write(JsonStyle.compact));
    }
}

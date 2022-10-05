package ldoa.net;

import arc.files.Fi;
import arc.func.Func2;
import arc.net.Connection;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import ldoa.net.ResponseMessage.*;
import useful.Json;
import useful.Json.JsonStyle;

import static arc.Core.*;

public class Database {

    public static final Object requestComplete = new Object();
    public static final Seq<Action> actions = Seq.with(
            new Action("get", 1, true, (context, args) -> {
                if (context instanceof Json json) {
                    return json.get(args[0]);
                } else return new RequestException("Can not get value from non-json object!");
            }),
            new Action("put", 2, false, (context, args) -> {
                if (context instanceof Json json) {
                    try {
                        Object object = Json.readAs(args[1]);
                        json.put(args[0], object);
                        return object;
                    } catch (RuntimeException error) { // unknown field type
                        return new RequestException(error.getMessage());
                    }
                } else return new RequestException("Can not put value to non-json object!");
            }),
            new Action("remove", 1, false, (context, args) -> {
                if (context instanceof Json json) {
                    json.remove(args[0]);
                    return null; // TODO return old value
                } else return new RequestException("Can not remove value from non-json object!");
            }),
            new Action("contains", 1, false, (context, args) -> {
                if (context instanceof Json json) return json.contains(args[0]);
                return new RequestException("Can not get value from non-json object!");
            })
    );

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

    public ResponseMessage execute(Connection connection, String request) {
        if (context == requestComplete) { // method called from outside
            String[] file = request.split(" ", 2);
            context = jsons.get(file[0]); // ldr start with filename

            try {
                if (context == null) return new RequestException("File not found!");
                if (file.length == 1) return new RequestException("Invalid LDR!");
                return execute(connection, file[1]);
            } finally { context = requestComplete; }
        } else {
            int index = request.indexOf(" ");
            if (index == -1) return new RequestException("Too few arguments!");

            String name = request.substring(0, index);
            String args = request.substring(index + 1);

            Action action = actions.find(cmd -> cmd.name.equals(name));
            if (action == null) return new RequestException("Action not found!");

            context = action.execute(context, args);
            if (context instanceof ResponseMessage message) return message;

            if (action.continuable) { // request may continue: get a get b or add a div b
                String[] splitted = args.split(" ", action.argsAmount + 1);
                if (splitted.length > action.argsAmount) return execute(connection, splitted[splitted.length - 1]);
            }

            // pretty print for response
            return new RequestSuccess(context == null ? null : Json.write(context, JsonStyle.standard));
        }
    }

    private void read(Fi file) {
        jsons.put(file.nameWithoutExtension(), Json.read(file.readString()));
    }

    private void write(Fi file, Json json) {
        file.writeString(json.write(JsonStyle.compact));
    }

    public record Action(String name, int argsAmount, boolean continuable, Func2<Object, String[], Object> runner) {

        public Object execute(Object context, String args) {
            String[] splitted = args.split(" ", continuable ? argsAmount + 1 : argsAmount);
            if (splitted.length < argsAmount) return new RequestException("Too few arguments!");
            return runner.get(context, splitted);
        }
    }
}

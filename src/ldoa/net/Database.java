package ldoa.net;

import arc.files.Fi;
import arc.func.*;
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
            new Action("get", 1, true, (json, args) -> {
                return json.get(args[0]);
            }, "Can not get value from non-json object!"),

            new Action("put", 2, false, (json, args) -> {
                try {
                    Object object = Json.readAs(args[1]);
                    json.put(args[0], object);
                    return object;
                } catch (RuntimeException error) { // unknown field type
                    return new RequestException(error.getMessage());
                }
            }, "Can not put value to non-json object!"),

            new Action("remove", 1, false, (json, args) -> {
                json.remove(args[0]);
                return null; // TODO return old value
            }, "Can not remove value from non-json object!"),

            new Action("contains", 1, false, (json, args) -> {
                return json.contains(args[0]);
            }, "Can not check value existence in non-json object!"),

            new Action("each", 0, true, (json, args) -> {
                return new EachAction(json);
            }, "Can not iterate over values in non-json object!")
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

    public ResponseMessage executeResponse(Connection connection, String request) {
        Object response = execute(connection, request);
        if (response instanceof ResponseMessage message) return message; // exception

        // pretty print for response
        return new RequestSuccess(response == null ? null : Json.write(response, JsonStyle.standard));
    }

    public Object execute(Connection connection, String request) {
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
            if (context instanceof ResponseMessage || !action.continuable) return context; // exception or final result

            String[] splitted = args.split(" ", action.argsAmount + 1);
            if (splitted.length <= action.argsAmount) return context;

            // request continuation: get a get b or add a div b
            String next = splitted[splitted.length - 1];

            if (context instanceof EachAction each) // special case for each action
                return each.execute(item -> {
                    context = item;
                    return execute(connection, next);
                });
            else return execute(connection, next); // any other action
        }
    }

    private void read(Fi file) {
        jsons.put(file.nameWithoutExtension(), Json.read(file.readString()));
    }

    private void write(Fi file, Json json) {
        file.writeString(json.write(JsonStyle.compact));
    }

    public record Action(String name, int argsAmount, boolean continuable, Func2<Json, String[], Object> runner, String exception) {

        public Object execute(Object context, String args) {
            if (context instanceof Json json) {
                String[] splitted = args.split(" ", continuable ? argsAmount + 1 : argsAmount);
                if (splitted.length < argsAmount) return new RequestException("Too few arguments!");
                return runner.get(json, splitted);
            } else return new RequestException(exception);
        }
    }

    public record EachAction(Json json) {

        public Json execute(Func<Object, Object> executor) {
            Json result = new Json();
            json.each((key, value) -> { // mapping values through an executor
                Object mapped = executor.get(value);
                result.put(key, mapped instanceof ResponseMessage message ? message.response : mapped);
            });
            return result;
        }
    }
}

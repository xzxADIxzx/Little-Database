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
            new JsonAction("get", 1, true, (database, json, args) -> {
                database.cache = new GetCache(json, args[0]);
                return json.get(args[0]);
            }, "Can not get value from non-json object!"),

            new JsonAction("put", 2, false, (json, args) -> {
                try {
                    Object object = Json.readAs(args[1]);
                    json.put(args[0], object);
                    return object;
                } catch (RuntimeException error) { // unknown field type
                    return new RequestException(error.getMessage());
                }
            }, "Can not put value to non-json object!"),

            new JsonAction("remove", 1, false, (json, args) -> {
                json.remove(args[0]);
                return null; // TODO return old value
            }, "Can not remove value from non-json object!"),

            new JsonAction("contains", 1, false, (json, args) -> {
                return json.contains(args[0]);
            }, "Can not check value existence in non-json object!"),

            new JsonAction("each", 1, false, (database, json, args) -> {
                Json result = new Json();
                json.each((key, value) -> { // mapping values through an execute
                    database.context = value;
                    Object mapped = database.execute(null, args[0]);
                    result.put(key, mapped instanceof ResponseMessage message ? message.response : mapped);
                });
                return result;
            }, "Can not iterate over values in non-json object!"),

            new MathAction("add", 1, true, (number, args) -> number + Float.valueOf(args[0])),
            new MathAction("sub", 1, true, (number, args) -> number - Float.valueOf(args[0])),
            new MathAction("mul", 1, true, (number, args) -> number * Float.valueOf(args[0])),
            new MathAction("div", 1, true, (number, args) -> number / Float.valueOf(args[0])));

    public final ObjectMap<String, Json> jsons = new ObjectMap<>();
    public Object context = requestComplete;

    /** Specially for math actions as they auto-save the value after the operation is done. */
    protected GetCache cache;

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

            context = action.run(this, args);
            if (context instanceof ResponseMessage || !action.continuable) return context; // exception or final result

            String[] split = args.split(" ", action.argsAmount + 1);
            if (split.length <= action.argsAmount) return context;

            String next = split[split.length - 1]; // request continuation: get a get b or add a div b
            return execute(connection, next);
        }
    }

    private void read(Fi file) {
        jsons.put(file.nameWithoutExtension(), Json.read(file.readString()));
    }

    private void write(Fi file, Json json) {
        file.writeString(json.write(JsonStyle.compact));
    }

    public static abstract class Action {

        public String name;
        public int argsAmount;
        public boolean continuable;

        public Action(String name, int argsAmount, boolean continuable) {
            this.name = name;
            this.argsAmount = argsAmount;
            this.continuable = continuable;
        }

        public Object run(Database database, String args) {
            String[] split = args.split(" ", continuable ? argsAmount + 1 : argsAmount);
            if (split.length < argsAmount) return new RequestException("Too few arguments!");
            return run(database, split);
        }

        protected abstract Object run(Database database, String[] args);
    }

    public static class JsonAction extends Action {

        private Func3<Database, Json, String[], Object> runner;
        private String exception;

        public JsonAction(String name, int argsAmount, boolean continuable, Func3<Database, Json, String[], Object> runner, String exception) {
            super(name, argsAmount, continuable);
            this.runner = runner;
            this.exception = exception;
        }

        public JsonAction(String name, int argsAmount, boolean continuable, Func2<Json, String[], Object> runner, String exception) {
            this(name, argsAmount, continuable, (database, json, args) -> runner.get(json, args), exception);
        }

        @Override
        protected Object run(Database database, String[] args) {
            return database.context instanceof Json json ? runner.get(database, json, args) : new RequestException(exception);
        }
    }

    public static class MathAction extends Action {

        private Func2<Float, String[], Float> runner;
        private String exception;

        public MathAction(String name, int argsAmount, boolean continuable, Func2<Float, String[], Float> runner) {
            super(name, argsAmount, continuable);
            this.runner = runner;
            this.exception = "Can not perform a mathematical operation on a non-numeric value!";
        }

        @Override
        protected Object run(Database database, String[] args) {
            if (database.context instanceof Number number) {
                Float performed = runner.get(number.floatValue(), args);

                Object result; // parse the value back to its original type
                if (database.context instanceof Integer) result = performed.intValue();
                else result = performed; // if needed because java sucks

                database.cache.put(result); // change value to new
                return result;
            } else return new RequestException(exception);
        }
    }

    public static record GetCache(Json from, String key) {

        public void put(Object value) {
            from.put(key, value);
        }
    }
}

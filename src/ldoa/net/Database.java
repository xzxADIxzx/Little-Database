package ldoa.net;

import arc.files.Fi;
import arc.net.Connection;
import arc.struct.ObjectMap;
import arc.util.Log;
import useful.Json;
import useful.Json.JsonStyle;

import static arc.Core.*;

public class Database {

    public ObjectMap<String, Json> jsons = new ObjectMap<>();
    public Fi root;

    public void load(String from) {
        jsons.clear();
        root = files.local(from);

        if (root.exists()) {
            if (root.isDirectory()) root.walk(this::read);
            else throw new RuntimeException("Database subdirectory cannot be created as a file with the same name already exists!");
        } else root.mkdirs();

        Log.info("Loaded @ database files.", jsons.size);
    }

    public Object execute(Connection connection, String request) {
        return null;
    }

    private void read(Fi file) {
        jsons.put(file.nameWithoutExtension(), Json.read(file.readString()));
    }

    private void write(String name) {
        root.child(name + ".json").writeString(jsons.get(name).write(JsonStyle.standard));
    }
}

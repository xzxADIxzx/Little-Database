package ldoa.net;

/** Represents json but does not contain any data, everything is obtained through requests to the database server. */
public class JsonShell {

    public String path;

    public JsonShell(String path) {
        this.path = path;
    }

    /** Returns value by key in json which is represented by this {@link JsonShell}. */
    public Object get(String key) {
        return null; // TODO get data through Client
    }

    /** Returns value representation located by key in json, which is represented by this {@link JsonShell}. */
    public JsonShell getShell(String key) {
        return new JsonShell(path + " get " + key);
    }

    /** Puts a value to json which is represented by this {@link JsonShell}. */
    public void put(String key, Object value) {
        // String request = path + " put " + TODO create ldr and... idk, parse a values to String? i think
    }

    public boolean contains(String key) {
        // String request = path + " contains " + key
        return false; // TODO create ldr and return response == null
    }
}

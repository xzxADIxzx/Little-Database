package ldoa.net;

import arc.func.Cons;

import static ldoa.Main.*;

/** Represents json but does not contain any data, everything is obtained through requests to the database server. */
public class JsonShell { // TODO handler error response

    public String path;
    public Object response;

    public JsonShell(String path) {
        this.path = path;
    }

    /** Returns a value representation located by a key in json which is represented by this {@link JsonShell}. */
    public JsonShell getShell(String key) {
        return new JsonShell(path + " get " + key);
    }

    /** Returns a value by a key in json which is represented by this {@link JsonShell}. */
    public Object get(String key) {
        getAsync(key, res -> response = res);
        waitUntilResponse();
        return response;
    }

    /** Works like {@link #get(String)} but doesn't stop the thread. */
    public void getAsync(String key, Cons<Object> response) {
        client.send(path + " get " + key, response);
    }

    /** Puts a value to json, which is represented by this {@link JsonShell}, by a key. */
    public Object put(String key, Object value) {
        putAsync(key, value, res -> response = res);
        waitUntilResponse();
        return response;
    }

    /** Works like {@link #put(String, Object)} but doesn't stop the thread. */
    public void putAsync(String key, Object value, Cons<Object> response) {
        client.send(path + " put " + key, response);
    }

    /** Remove a value from json, which is represented by this {@link JsonShell}, by a key. */
    public Object remove(String key) {
        removeAsync(key, res -> response = res);
        waitUntilResponse();
        return response;
    }

    /** Works like {@link #remove(String)} but doesn't stop the thread. */
    public void removeAsync(String key, Cons<Object> response) {
        client.send(path + " remove " + key, response);
    }

    /**Returns whether the json, which is represented by this {@link JsonShell}, contains a key. */
    public boolean contains(String key) {
        client.send(path + " contains " + key, res -> response = res);
        waitUntilResponse();
        return (boolean) response;
    }

    private void waitUntilResponse() {
        response = null;
        while (response == null) break; // TODO wait until response and break if out of time
    }
}

package ldoa.net;

import arc.util.Time;
import ldoa.net.ResponseMessage.*;
import useful.Json;

import static ldoa.Main.*;

/** Represents json but does not contain any data, everything is obtained through requests to the database server. */
public class JsonShell { // TODO handler success/error response

    /** Maximum time to wait for server response. */
    public static final long maxWaitDuration = 5000l; // 5 sec

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
        return Json.readAs(getResponse());
    }

    /** Works like {@link #get(String)} but doesn't stop the thread and returns a {@link ResponseMessage}. */
    public void getAsync(String key, ResponseCons response) {
        client.send(path + " get " + key, response);
    }

    /** Puts a value to json, which is represented by this {@link JsonShell}, by a key. */
    public Object put(String key, Object value) {
        putAsync(key, value, res -> response = res);
        waitUntilResponse();
        return getResponse();
    }

    /** Works like {@link #put(String, Object)} but doesn't stop the thread and returns a {@link ResponseMessage}. */
    public void putAsync(String key, Object value, ResponseCons response) {
        client.send(path + " put " + key + " " + value, response);
    }

    /** Remove a value from json, which is represented by this {@link JsonShell}, by a key. */
    public Object remove(String key) {
        removeAsync(key, res -> response = res);
        waitUntilResponse();
        return getResponse();
    }

    /** Works like {@link #remove(String)} but doesn't stop the thread and returns a {@link ResponseMessage}. */
    public void removeAsync(String key, ResponseCons response) {
        client.send(path + " remove " + key, response);
    }

    /** Returns whether the json, which is represented by this {@link JsonShell}, contains a key. */
    public boolean contains(String key) {
        containsAsync(key, res -> response = res);
        waitUntilResponse();
        return Boolean.valueOf(getResponse());
    }

    /** Works like {@link #contains(String key)} but doesn't stop the thread and returns a {@link ResponseMessage}. */
    public void containsAsync(String key, ResponseCons response) {
        client.send(path + " contains " + key, response);
    }

    private void waitUntilResponse() {
        response = null;

        long mark = Time.millis();
        while (response == null) {
            try {
                Thread.sleep(1); // idk why but it doesn't work without sleep
            } catch (Throwable ignored) {}

            if (Time.timeSinceMillis(mark) > maxWaitDuration)
                throw new RuntimeException("Timeout waiting for server response.");
        }
    }

    private String getResponse() {
        if (response instanceof RequestSuccess req) return req.response;
        if (response instanceof RequestException) throw new RuntimeException("Exception occurred while processing your request: " + response);
        throw new RuntimeException("Unknown response!"); // now it's rly impossible as unknown messages are handled by packet serializer
    }
}

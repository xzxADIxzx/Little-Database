package ldoa.net;

import arc.func.Cons;
import arc.func.Func;
import arc.util.Time;
import ldoa.net.ResponseMessage.*;
import useful.Json;
import useful.Json.JsonStyle;

import static ldoa.Main.*;

/** Represents json but does not contain any data, everything is obtained through requests to the database server. */
public class JsonShell {

    /** Maximum time to wait for server response. */
    public static final long maxWaitDuration = 5000l; // 5 sec

    public String path;

    public JsonShell(String path) {
        this.path = path;
    }

    /** Returns a value representation located by a key in json which is represented by this {@link JsonShell}. */
    public JsonShell getShell(String key) {
        return new JsonShell(path + " get " + key);
    }

    /** Returns a value by a key in json which is represented by this {@link JsonShell}. */
    public Response<Object> get(String key) {
        return new Response<>(path + " get " + key, Json::readAs);
    }

    /** Puts a value to json, which is represented by this {@link JsonShell}, by a key. */
    public Response<Object> put(String key, Object value) {
        return new Response<>(path + " put " + key + " " + Json.write(value, JsonStyle.compact), Json::readAs);
    }

    /** Removes a value from json, which is represented by this {@link JsonShell}, by a key. */
    public Response<Object> remove(String key) {
        return new Response<>(path + " remove " + key, Json::readAs);
    }

    /** Returns whether the json, which is represented by this {@link JsonShell}, contains a key. */
    public Response<Boolean> contains(String key) {
        return new Response<>(path + " contains " + key, Boolean::valueOf);
    }

    /** Removes all values from json which is represented by this {@link JsonShell}. */
    public Response<Object> clear(String key) {
        return new Response<>(path + " clear", Json::readAs);
    }

    /** Request wrapper used to process an asynchronous request or block a thread until a response is received. */
    public static class Response<T> {

        public String request;
        public Object response;

        public Func<String, T> parser;

        public Response(String request, Func<String, T> parser) {
            this.request = request;
            this.parser = parser;
        }

        /** Works like {@link #send(Cons)} but returns {@link ResponseMessage}. */
        public void cons(ResponseCons response) {
            client.send(request, response);
        }

        /** Works like {@link #block()} but doesn't stop the thread. */
        public void send(Cons<T> cons) throws ShellException {
            client.send(request, res -> {
                response = res;
                cons.get(parser.get(getResponse()));
            });
        }

        /** Returns the result of the request, or throws an exception if the result is instance of {@link RequestException}. */
        public T block() throws ShellException {
            client.send(request, res -> response = res);
            waitUntilResponse();
            return parser.get(getResponse());
        }

        private void waitUntilResponse() throws ShellException {
            response = null;

            long mark = Time.millis();
            while (response == null) {
                try {
                    Thread.sleep(1); // idk why but it doesn't work without sleep
                } catch (Throwable ignored) {}

                if (Time.timeSinceMillis(mark) > maxWaitDuration)
                    throw new ShellException("Timeout waiting for server response.");
            }
        }

        private String getResponse() throws ShellException {
            if (response instanceof RequestSuccess req) return req.response;
            if (response instanceof RequestException req) throw new ShellException("Exception occurred while processing your request: " + req.response);
            return null; // now it's rly impossible as unknown messages are handled by packet serializer
        }
    }

    /** Database request exception wrapper. */
    public static class ShellException extends RuntimeException {

        public ShellException(String message) {
            super(message);
        }
    }
}

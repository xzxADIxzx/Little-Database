package ldoa.net;

import arc.net.Connection;
import arc.struct.IntIntMap;
import arc.util.Strings;

public abstract class ResponseMessage {

    public static int id;
    public static IntIntMap ids = new IntIntMap();

    public int requestID;
    public String response;

    public ResponseMessage() {} // for packet serializer

    public ResponseMessage(String response) {
        this.response = response;
    }

    public void send(Connection connection) {
        this.requestID = ids.increment(connection.getID());
        connection.sendTCP(this);
    }

    @Override
    public String toString() {
        return Strings.format("[@] @", requestID, response);
    }

    /** Internal message about the successful completion of the request by the server. */
    public static class RequestSuccess extends ResponseMessage {
        public RequestSuccess() {}

        public RequestSuccess(String response) {
            super(response);
        }
    }

    /** Internal message about a request exception on the server. */
    public static class RequestException extends ResponseMessage {
        public RequestException() {}

        public RequestException(String response) {
            super(response);
        }
    }

    public static interface ResponseCons {
        void get(ResponseMessage message);
    }
}

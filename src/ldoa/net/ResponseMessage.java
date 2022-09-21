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

    public ResponseMessage(Connection connection, String response) {
        if (connection != null) // request can be created on the server side
            this.requestID = ids.increment(connection.getID());
        this.response = response;
    }

    @Override
    public String toString() {
        return Strings.format("[@] @", requestID, response);
    }

    /** Internal message about the successful completion of the request by the server. */
    public static class RequestSuccess extends ResponseMessage {
        public RequestSuccess() {}

        public RequestSuccess(Connection connection, String response) {
            super(connection, response);
        }
    }

    /** Internal message about a request exception on the server. */
    public static class RequestException extends ResponseMessage {
        public RequestException() {}

        public RequestException(Connection connection, String response) {
            super(connection, response);
        }
    }
}

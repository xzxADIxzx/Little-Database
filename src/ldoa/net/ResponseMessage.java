package ldoa.net;

import arc.net.Connection;
import arc.struct.IntIntMap;

public abstract class ResponseMessage {

    public static int id;
    public static IntIntMap ids = new IntIntMap();

    public int requestID;
    public String response;

    public ResponseMessage() {}

    public ResponseMessage(Connection connection, String response) {
        if (connection != null) // request can be created on the server side
            this.requestID = ids.increment(connection.getID());
        this.response = response;
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

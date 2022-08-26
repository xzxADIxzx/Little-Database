package ldoa.net;

public interface ResponseMessage {

    /** Internal message about the successful completion of the request by the server. */
    public class RequestSuccess implements ResponseMessage {
        public int requestID;
        public String response;
    }

    /** Internal message about a request exception on the server. */
    public class RequestException implements ResponseMessage {
        public int requestID;
        public String response;
    }
}

package ldoa.net;

import arc.net.FrameworkMessage;
import arc.net.FrameworkMessage.*;
import arc.net.NetSerializer;
import ldoa.net.ResponseMessage.*;

import java.nio.ByteBuffer;

public class PacketSerializer implements NetSerializer {

    @Override
    public void write(ByteBuffer buffer, Object object) {
        if (object instanceof FrameworkMessage message) {
            buffer.put((byte) 1);
            writeFramework(buffer, message);
        } else if (object instanceof ResponseMessage message) {
            buffer.put((byte) 2);
            writeResponse(buffer, message);
        } else {
            buffer.put((byte) 3);
            writeString(buffer, (String) object);
        }
    }

    @Override
    public Object read(ByteBuffer buffer) {
        byte id = buffer.get();
        if (id == 1) return readFramework(buffer);
        if (id == 2) return readResponse(buffer);
        if (id == 3) return readString(buffer);
        return null; // unknown
    }

    public void writeFramework(ByteBuffer buffer, FrameworkMessage message) {
        if (message instanceof Ping || message instanceof DiscoverHost) buffer.put((byte) 0); // unused
        else if (message instanceof RegisterTCP reg) buffer.put((byte) 1).putInt(reg.connectionID);
        else if (message instanceof RegisterUDP reg) buffer.put((byte) 2).putInt(reg.connectionID);
        else if (message instanceof KeepAlive) buffer.put((byte) 3);
    }

    public FrameworkMessage readFramework(ByteBuffer buffer) {
        byte id = buffer.get();
        if (id == 1)
            return new RegisterTCP() {{
                connectionID = buffer.getInt();
            }};
        else if (id == 2)
            return new RegisterUDP() {{
                connectionID = buffer.getInt();
            }};
        else if (id == 3) return FrameworkMessage.keepAlive;
        else throw new RuntimeException("Unknown framework message!"); // how is that even possible?
    }

    public void writeResponse(ByteBuffer buffer, ResponseMessage message) {
        if (message instanceof RequestSuccess res) {
            buffer.put((byte) 1).putInt(res.requestID);
            writeString(buffer, res.response);
        } else if (message instanceof RequestException res) {
            buffer.put((byte) 2).putInt(res.requestID);
            writeString(buffer, res.response);
        }
    }

    public ResponseMessage readResponse(ByteBuffer buffer) {
        byte id = buffer.get();
        if (id == 1)
            return new RequestSuccess() {{
                requestID = buffer.getInt();
                response = readString(buffer);
            }};
        else if (id == 2)
            return new RequestException() {{
                requestID = buffer.getInt();
                response = readString(buffer);
            }};
        else throw new RuntimeException("Unknown response message!"); // impossible?
    }

    public static void writeString(ByteBuffer buffer, String message) {
        buffer.putInt(message.length());
        for (char chara : message.toCharArray())
            buffer.putChar(chara);
    }

    public static String readString(ByteBuffer buffer) {
        int length = buffer.getInt();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++)
            builder.append(buffer.getChar());
        return builder.toString();
    }
}

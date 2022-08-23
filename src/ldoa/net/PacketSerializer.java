package ldoa.net;

import arc.net.FrameworkMessage;
import arc.net.FrameworkMessage.*;
import arc.net.NetSerializer;

import java.nio.ByteBuffer;

public class PacketSerializer implements NetSerializer {

    @Override
    public void write(ByteBuffer buffer, Object object) {
        if(object instanceof FrameworkMessage message){
            buffer.put((byte) 1);
            writeFramework(buffer, message);
        } else {
            buffer.put((byte) 2);
            writeString(buffer, (String) object);
        }
    }

    @Override
    public Object read(ByteBuffer buffer) {
        byte id = buffer.get();
        if (id == 1) return readFramework(buffer);
        if (id == 2) return readString(buffer);
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
        if (id == 1) {
            RegisterTCP reg = new RegisterTCP();
            reg.connectionID = buffer.getInt();
            return reg;
        } else if (id == 2) {
            RegisterUDP reg = new RegisterUDP();
            reg.connectionID = buffer.getInt();
            return reg;
        } else if (id == 3) return FrameworkMessage.keepAlive;
        else throw new RuntimeException("Unknown framework message!"); // how is that even possible?
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

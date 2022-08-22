package ldoa.net;

import arc.net.FrameworkMessage;
import arc.net.FrameworkMessage.*;
import arc.net.NetSerializer;

import java.nio.ByteBuffer;

public class PacketSerializer implements NetSerializer {

    @Override
    public void write(ByteBuffer buffer, Object object) {
        if(object instanceof FrameworkMessage message){
            buffer.put((byte)1);
            writeFramework(buffer, message);
        }
    }

    @Override
    public Object read(ByteBuffer buffer) {
        byte id = buffer.get();
        if (id == 1) return readFramework(buffer);
        else return null; // temp
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
}

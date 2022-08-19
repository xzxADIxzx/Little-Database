package ldoa;

public class Client extends arc.net.Client {

    public Client(){
        super(8192, 8192, new PacketSerializer());
    }
}

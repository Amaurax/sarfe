package event.test;

import event.given.Broker.ConnectListener;
import event.given.Channel;
import event.given.Channel.ReadListener;
import event.given.Channel.WriteListener;
import event.given.Executor;
import event.queue.CBroker;

public class EchoClient {
    private CBroker broker;
    private Channel channel;

    public EchoClient(String name, Executor executor) {
        broker = new CBroker(name, executor);
    }

    public void connect(String serverName, int port, Runnable onConnected) {
        broker.connect(serverName, port, new ConnectListener() {
            @Override
            public void connected(Channel channel) {
                EchoClient.this.channel = channel;
                onConnected.run();
            }
        });
    }

    public void sendMessage(byte[] message, ReadListener onResponse) {
        channel.write(message, 0, message.length, new WriteListener() {
            @Override
            public void written(byte[] bytes, int offset, int length, int written) {
                channel.read(new byte[message.length], 0, message.length, onResponse);
            }
        });
    }
}

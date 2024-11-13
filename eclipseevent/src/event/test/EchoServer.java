package event.test;

import event.given.Broker.AcceptListener;
import event.given.Channel;
import event.given.Channel.ReadListener;
import event.given.Channel.WriteListener;
import event.given.Executor;
import event.queue.CBroker;

public class EchoServer {
    private CBroker broker;

    public EchoServer(String name, Executor executor) {
        broker = new CBroker(name, executor);
    }

    public void start(int port) {
        broker.accept(port, new AcceptListener() {
            @Override
            public void accepted(Channel channel) {
                handleClient(channel);
                start(port); // Continue accepting new clients
            }
        });
    }

    private void handleClient(Channel channel) {
        byte[] buffer = new byte[1024];
        channel.read(buffer, 0, buffer.length, new ReadListener() {
            @Override
            public void read(byte[] bytes) {
                if (bytes.length > 0) {
                    channel.write(bytes, 0, bytes.length, new WriteListener() {
                        @Override
                        public void written(byte[] bytes, int offset, int length, int written) {
                            handleClient(channel); // Continue reading from the client
                        }
                    });
                }
            }
        });
    }
}

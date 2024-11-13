package event.test;

import event.given.Channel.ReadListener;
import event.given.Executor;

public class EchoServerTest {
    public static void main(String[] args) {
        Executor executor = new Executor("Executor");
        executor.start();

        EchoServer server = new EchoServer("EchoServer", executor);
        server.start(8080);

        int numberOfClients = 5;
        int messagesPerClient = 3;

        for (int i = 0; i < numberOfClients; i++) {
            final int clientId = i;
            EchoClient client = new EchoClient("EchoClient" + clientId, executor);
            client.connect("EchoServer", 8080, new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < messagesPerClient; j++) {
                        final int messageIndex = j;
                        String message = "Message " + messageIndex + " from client " + clientId;
                        client.sendMessage(message.getBytes(), new ReadListener() {
                            @Override
                            public void read(byte[] bytes) {
                                String response = new String(bytes);
                                System.out.println("Received from server: " + response);
                                if (response.equals(message)) {
                                    System.out
                                            .println("Test passed for client " + clientId + " message " + messageIndex);
                                } else {
                                    System.out
                                            .println("Test failed for client " + clientId + " message " + messageIndex);
                                }
                            }
                        });
                    }
                }
            });
        }

        // Wait for the test to complete
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

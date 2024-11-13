package event.queue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import event.given.Broker;
import event.given.Channel;
import event.given.Executor;
import event.given.QueueBroker;

public class CQueueBroker extends QueueBroker {

	// map des ports bindés avec leur objet pour synchronisation
	private Map<Integer, Object> binds = new HashMap<>();
	private List<Integer> unbinds = new ArrayList<>();

	public CQueueBroker(Executor pump, Broker broker) {
		super(pump, broker);
	}

	@Override
	public synchronized boolean bind(int port, AcceptListener listener) {
		if (binds.containsKey(port))
			return false;
		binds.put(port, new Object());

		Broker.AcceptListener acceptListener = new Broker.AcceptListener() {
			@Override
			public void accepted(Channel channel) {
				CMessageQueue messageQueue = new CMessageQueue(channel, self(), getEventPump());
				Channel.ReadListener readListener = new Channel.ReadListener() {
					@Override
					public void read(byte[] bytes) {
						if (bytes.equals(CMessageQueue.UNBINDING_QUEUE_PAYLOAD)) {
							messageQueue.close();
							unbinds.add(port);
						} else {
							Runnable r = new Runnable() {
								@Override
								public void run() {
									listener.accepted(messageQueue);
								}
							};
							getEventPump().post(r);
							if (binds.containsKey(port) || !unbinds.contains(port))
								getBroker().accept(port, acceptListener());
						}
					}
				};
				messageQueue.startReadPayload(readListener);
			}

			private Broker.AcceptListener acceptListener() {
				return this;
			}
		};
		getBroker().accept(port, acceptListener);
		unbinds.remove(Integer.valueOf(port));
		return true;
	}

	@Override
	public boolean unbind(int port) {
		synchronized (binds.get(Integer.valueOf(port)) == null ? new Object() : binds.get(Integer.valueOf(port))) {
			if (binds.remove(Integer.valueOf(port)) == null)
				return false;
			Broker.ConnectListener connectListener = new Broker.ConnectListener() {
				@Override
				public void connected(Channel channel) {
					CMessageQueue queue = new CMessageQueue(channel, self(), self().getEventPump());
					queue.send(CMessageQueue.UNBINDING_QUEUE_PAYLOAD);
				}
			};

			if (!getBroker().connect(this.getName(), port, connectListener)) {
				return false;
			}
			return true;
		}
	}

	@Override
	public boolean connect(String name, int port, ConnectListener listener) {
		Broker.ConnectListener connectListener = new Broker.ConnectListener() {
			@Override
			public void connected(Channel channel) {
				CMessageQueue queue = new CMessageQueue(channel, self(), self().getEventPump());
				queue.send(CMessageQueue.REGULAR_QUEUE_PAYLOAD);
				Runnable r = new Runnable() {
					@Override
					public void run() {
						listener.connected(queue);
					}
				};
				getEventPump().post(r);
			}
		};

		if (!getBroker().connect(name, port, connectListener)) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					listener.refused();
				}
			};
			this.getEventPump().post(r);
			return false;
		}
		return true;
	}

	private CQueueBroker self() {
		return this;
	}

}

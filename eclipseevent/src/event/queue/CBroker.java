package event.queue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import event.given.Broker;
import event.given.Executor;

public class CBroker extends Broker {

	// map stockant les acceptlisteners sur les ports ouverts
	private Map<Integer, AcceptListener> acceptingPorts = new HashMap<>();
	// map stockant les connectlisteners en attente d'une acceptation sur les ports
	private Map<Integer, List<ConnectListener>> connectingPorts = new HashMap<>();
	private Executor executor;

	public CBroker(String name, Executor executor) {
		super(name);
		BrokerManager.addBroker(this);
		this.executor = executor;
	}

	/*
	 * Si la map acceptingPorts possède un AcceptListener sur le port donné :
	 * IllegalArgumentException
	 * Si la map connectingPorts a un/des écouteurs sur le port donné :
	 * - récupère le premier ConnectListener de la liste et l'en retire
	 * - crée 2 nouveaux CChannels liés
	 * - crée un nouveau Runnable appelant la méthode accepted() du listener donné
	 * sur l'un des channels depuis sa méthode run() (du runnable)
	 * - crée un autre nouveau Runnable appelant la méthode connected() du
	 * ConnectListener récupéré avec l'autre channel depuis sa méthode run().
	 * - poste ces 2 Runnable dans la pompe
	 * Sinon :
	 * - Ajoute le listener donné à la map acceptingPorts sur le port donné
	 */
	@Override
	public synchronized void accept(int port, AcceptListener listener) {
		if (acceptingPorts.get(port) != null)
			throw new IllegalArgumentException(this.toString() + " accept : port invalide");
		if (connectingPorts.get(port) != null && connectingPorts.get(port).size() > 0) {
			ConnectListener connectListener = connectingPorts.get(port).get(0);
			connectingPorts.get(port).remove(connectListener);
			if (connectingPorts.get(port).size() == 0)
				connectingPorts.remove(port);
			CChannel acceptChannel = new CChannel(this, port, executor);
			CChannel connectChannel = new CChannel(this, port, acceptChannel, executor);
			Runnable runnableAccept = new Runnable() {
				@Override
				public void run() {
					listener.accepted(acceptChannel);
				}
			};
			Runnable runnableConnect = new Runnable() {
				@Override
				public void run() {
					connectListener.connected(connectChannel);
				}
			};
			executor.post(runnableAccept);
			executor.post(runnableConnect);
		} else {
			acceptingPorts.put(port, listener);
		}
	}

	/*
	 * Récupère le broker distant et retourne false s'il est null.
	 * Dans un bloc synchronisé sur l'objet broker récupéré :
	 * | Si la map acceptingPorts possède un AcceptListener sur le port donné :
	 * | - récupère l'AcceptListener de la map distante et le retire ensuite
	 * | - crée 2 nouveaux CChannels liés
	 * | - crée un nouveau Runnable appelant la méthode accepted() de
	 * l'AcceptListener récupéré sur l'un des channels depuis sa méthode run() (du
	 * runnable)
	 * | - crée un autre nouveau Runnable appelant la méthode connected() du
	 * listener donné avec l'autre channel depuis sa méthode run().
	 * | - poste ces 2 Runnable dans la pompe
	 * | Sinon :
	 * | - Initialise liste des ConnectListener distants dans la map si ellle ne
	 * l'est pas déjà
	 * | - Ajoute l'écouteur donné à la liste de la map distante sur le port donné
	 * Retourne true
	 */
	@Override
	public boolean connect(String name, int port, ConnectListener listener) {
		CBroker broker = BrokerManager.getBroker(name);
		if (broker == null)
			return false;
		synchronized (broker) {
			if (broker.acceptingPorts.get(port) != null) {
				AcceptListener acceptListener = broker.acceptingPorts.get(port);
				broker.acceptingPorts.remove(port);
				CChannel connectChannel = new CChannel(this, port, executor);
				CChannel acceptChannel = new CChannel(this, port, connectChannel, executor);
				Runnable runnableConnect = new Runnable() {
					@Override
					public void run() {
						listener.connected(connectChannel);
					}
				};
				Runnable runnableAccept = new Runnable() {
					@Override
					public void run() {
						acceptListener.accepted(acceptChannel);
					}
				};
				executor.post(runnableConnect);
				executor.post(runnableAccept);
			} else {
				if (broker.connectingPorts.get(port) == null) {
					ArrayList<ConnectListener> list = new ArrayList<>();
					broker.connectingPorts.put(port, list);
				}
				broker.connectingPorts.get(port).add(listener);
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return "[CBroker " + this.getName() + "]";
	}

}

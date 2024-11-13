package event.queue;

import java.util.HashMap;
import java.util.Map;

/**
 * Permet de gérer l'ensemble des Brokers (leur permet de se trouver par leur
 * nom)
 * Doit être unique pour garder l'unicité des noms de Broker
 */
public class BrokerManager {

	private static Map<String, CBroker> brokers = new HashMap<>();

	/**
	 * @param broker : Broker à ajouter
	 * @throws IllegalArgumentException si le nom du Broker est déjà utilisé
	 */
	public synchronized static void addBroker(CBroker broker) throws IllegalArgumentException {
		if (isNameUsed(broker.getName()))
			throw new IllegalArgumentException("Broker name not unique (" + broker.getName() + ")");
		brokers.put(broker.getName(), broker);
	}

	/**
	 * @param name : nom du Broker
	 * @return vrai si le nom est déjà utilisé, faux sinon
	 */
	public static boolean isNameUsed(String name) {
		return brokers.containsKey(name);
	}

	/**
	 * @param name : nom du Broker
	 * @return le Broker correspondant au nom donné, null sinon
	 */
	public static CBroker getBroker(String name) {
		return brokers.get(name);
	}

}

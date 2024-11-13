package event.given;

/**
 * QueueBroker permettent de créer les MessageQueues
 * Les QueueBrokers encapsulent les Brokers}.
 */
public abstract class QueueBroker {

	private Broker broker;
	private Executor pump;

	public QueueBroker(Executor pump, Broker broker) {
		this.broker = broker;
		this.pump = pump;
	}

	public Executor getEventPump() {
		return pump;
	}

	public String getName() {
		return broker.getName();
	}

	public Broker getBroker() {
		return broker;
	}

	/**
	 * Listener déstiné à la méthode bind() de QueueBroker.
	 * Permet de définir le comportement lorsqu'une connexion est acceptée et
	 * complète.
	 */
	public interface AcceptListener {
		/**
		 * Définit le comportement lorsqu'une connexion est acceptée et complète.
		 * 
		 * @param queue : la file de messages résultante
		 */
		void accepted(MessageQueue queue);
	}

	/**
	 * Permet d'accepter toute connexion sur le port donné jusqu'à ce que unbind()
	 * soit appelée.
	 * Définit le listener à utiliser sur ce port lorsque une connexion est acceptée
	 * et complète
	 * Ne fait rien si le port donné est déjà lié.
	 * Méthode est thread-safe et non-bloquante.
	 * 
	 * @param port     : le port de communication pour accepter
	 * @param listener : le listener à utiliser
	 * @return True si le port donné n'est pas déjà lié, false sinon
	 */
	public abstract boolean bind(int port, AcceptListener listener);

	/**
	 * Permet d'arrêter d'unbind un port
	 * Ne ferme pas les connexions précédentes sur ce port.
	 * Méthode est thread-safe et non-bloquante.
	 * 
	 * @param port : le port de communication
	 * @return True si le port est correctement délié, false sinon
	 */
	public abstract boolean unbind(int port);

	/**
	 * Listener pour la méthode connect() de QueueBroker.
	 * Permet de définir le comportement lorsqu'une connexion est complète ou
	 * refusée.
	 */
	public interface ConnectListener {
		/**
		 * Définit le comportement lorsqu'une connexion est complète.
		 * 
		 * @param queue : la file de messages résultante de la connexion
		 */
		void connected(MessageQueue queue);

		/**
		 * Définit le comportement lorsque la connexion est refusée.
		 */
		void refused();
	}

	/**
	 * Permet de se connecter à un autre QueueBroker sur le port donné.
	 * Définit le listener à utiliser lorsque la connexion est complète ou refusée.
	 * La connexion est refusée si le nom donné n'est celui d'aucun QueueBroker.
	 * Méthode thread-safe et non-bloquante.
	 * 
	 * @param name     : nom du QueueBroker distant
	 * @param port     : le port de communication
	 * @param listener : le listener à utiliser
	 * @return True
	 */
	public abstract boolean connect(String name, int port, ConnectListener listener);

}

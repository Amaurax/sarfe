package event.given;

public abstract class MessageQueue {

	/**
	 * Renvoie le Broker associé à cette MessageQueue
	 * 
	 * @return
	 */
	abstract public QueueBroker broker();

	/**
	 * Aide au débug
	 * 
	 * @return Le nom du Broker distant
	 */
	public abstract String getRemoteName();

	/**
	 * Listener destiné à MessageQueue
	 * Permet de définir le comportement lorsqu'un message est reçu,
	 * et lorsqu'une connexion est fermée.
	 */
	public interface Listener {
		/**
		 * Definit le comportement lorsqu'un message est reçu.
		 * 
		 * @param msg : le message reçu
		 */
		void received(byte[] msg);

		/**
		 * Definit le comportement lorsqu'une connexion est fermée.
		 */
		void closed();
	}

	/**
	 * Permet de définir le listener à utiliser à la réception de messages et à la
	 * fermeture de connexion.
	 * Démarre la réception automatique des messages si appelé pour la première
	 * fois.
	 * Méthode FIFO non bloquante et thread-safe.
	 * 
	 * @param l : le nouveau listener à utiliser
	 */
	public abstract void setListener(Listener l);

	/**
	 * Permet d'éviter tout problème de propriété sur le tableau afin qu'on puisse
	 * le
	 * modifier au retour de cette méthode.
	 * Méthode FIFO non bloquante et thread-safe.
	 * 
	 * @param bytes : le message à envoyer
	 * @return True si le message est correctement mis en file d'attente pour
	 *         l'envoi
	 */
	public abstract boolean send(byte[] bytes);

	/**
	 * Ferme cette MessageQueue de manière thread-safe, et déploque tout thread
	 * bloqué dans un send() ou receive().
	 */
	public abstract void close();

	/**
	 * @returns vrai si cette MessageQueue est fermée, faux sinon
	 */
	public abstract boolean closed();
}

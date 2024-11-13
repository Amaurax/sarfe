package event.given;

/**
 * Les brokers permettent d'établir des canaux.
 * Chaque broker doit avoir un nom unique.
 * Chaque broker peut être utilisé pour accepter des connexions sur différents
 * ports simultanément,
 * créant un nouveau canal pour chaque nouvelle connexion acceptée.
 */
public abstract class Broker {
  String name;

  /**
   * Chaque broker doit avoir un nom unique.
   * 
   * @param name     : nom du broker
   * @param executor : la pompe à événements de l'exécuteur
   * @throws IllegalArgumentException si le nom n'est pas unique.
   */
  protected Broker(String name) {
    this.name = name;
  }

  /**
   * @returns le nom de ce broker.
   */
  public String getName() {
    return name;
  }

  /**
   * Listener pour le broker
   * Permet de définir le comportement lorsqu'une connexion est acceptée et
   * entièrement connectée.
   */
  public interface AcceptListener {
    /**
     * Définit le comportement lorsqu'une connexion est acceptée complète.
     * 
     * @param channel : le canal résultant
     */
    public void accepted(Channel channel);
  }

  /**
   * Indique que ce broker acceptera une connexion sur le port donné.
   * Cela créera un canal entièrement connecté renvoyé via le listener.
   * Il s'agit d'un rendez-vous non bloquant et thread-safe.
   * 
   * @param port     : le port de connexion
   * @param listener : le listener à appeler lors de la connexion
   * @throws IllegalArgumentException s'il y a déjà une acceptation en attente sur
   *                                  le port donné.
   */
  public abstract void accept(int port, AcceptListener listener);

  /**
   * Listener pour le broker
   * Permet de définir le comportement lorsqu'une connexion est complète.
   */
  public interface ConnectListener {
    /**
     * Définit le comportement lorsqu'une connexion est entièrement connectée.
     * 
     * @param channel : le canal résultant
     */
    public void connected(Channel channel);
  }

  /**
   * Tente une connexion au port donné, via le broker avec le nom donné.
   * Cela créera un canal entièrement connecté renvoyé via le listener.
   * Il s'agit d'un rendez-vous non bloquant et thread-safe.
   * Remarque : plusieurs acceptations de différentes tâches avec
   * le même nom et le même port sont légales
   * 
   * @param name     : nom du broker à connecter.
   * @param port     : le port de connexion
   * @param listener : le listener à appeler lors de la connexion
   * @return True si le broker distant a été trouvé, false sinon.
   */
  public abstract boolean connect(String name, int port, ConnectListener listener);

}

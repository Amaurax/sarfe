package event.given;

/**
 * Channel est un flux de bytes point-à-point (ie sans intermédiaire)
 * Full-duplex, chaque point d'extrémité peut être utilisé pour lire ou écrire.
 * Un canal connecté est FIFO et sans perte.
 * Un canal peut être déconnecté à tout moment, de chaque côté.
 */
public abstract class Channel {
  Broker broker;

  /**
   * @param broker   : le Broker parent
   * @param executor : la pompe à événements de l'exécuteur
   */
  protected Channel(Broker broker) {
    this.broker = broker;
  }

  /**
   * Aide au débug
   * 
   * @return Le nom du Broker distant
   */
  public abstract String getRemoteName();

  /**
   * @return Le Broker parent
   */
  public Broker getBroker() {
    return broker;
  }

  /**
   * @return Le port de connexion
   */
  public abstract int getPort();

  /**
   * Listener detsiné à Channel
   * Permet de définir le comportement lorsqu'un tableau de bytes a été lu.
   */
  public interface ReadListener {
    /**
     * Définit le comportement lorsqu'un tableau de bytes a été lu.
     * 
     * @param bytes : les bytes lus
     */
    public void read(byte[] bytes);
  }

  /**
   * Lit les bytes dans le tableau donné, en commençant à l'offset donné.
   * Au moins 1 byte sera lu, au plus "lenght".
   * Méthode FIFO non bloquante et thread-safe.
   * 
   * @param bytes    : le tableau sur lequel écrire
   * @param offset   : l'index de départ dans le tableau
   * @param length   : nombre de bytes à lire
   * @param listener : le listener à appeler une fois terminé
   */
  public abstract void read(byte[] bytes, int offset, int length, ReadListener listener);

  /**
   * Listener destiné à Channel
   * Permet de définir le comportement lorsqu'un tableau de bytes a été écrit.
   */
  public interface WriteListener {
    /**
     * Définit le comportement lorsqu'un tableau de bytes a été écrit.
     * 
     * @param bytes  : les bytes écrits
     * @param offset : l'index de départ
     * @param length : nombre de bytes écrits
     */
    public void written(byte[] bytes, int offset, int length, int written);
  }

  /**
   * Écrit les bytes du tableau donné, en commençant à l'offset donné.
   * Au moins 1 byte sera écrit, au plus "length".
   * Méthode FIFO non bloquante et thread-safe.
   * 
   * @param bytes    : le tableau à lire
   * @param offset   : l'index de départ dans le tableau
   * @param length   : nombre de bytes à écrire
   * @param listener : le listener à appeler une fois terminé
   */
  public abstract void write(byte[] bytes, int offset, int length, WriteListener listener);

  /**
   * Déconnecte ce Channel de manière thread-safe, débloquant tout thread
   * bloqué sur une opération de lecture ou d'écriture.
   */
  public abstract void disconnect();

  /**
   * @returns true si ce Channel est déconnecté (thread-safe)
   */
  public abstract boolean disconnected();

}

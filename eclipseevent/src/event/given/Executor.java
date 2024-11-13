package event.given;

import java.util.LinkedList;
import java.util.List;

/**
 * Executor est une pompe à événements, permettant d'exécuter des tâches en
 * parallèle.
 * Les tâches sont exécutées dans l'ordre où elles ont été postées, dans un
 * unique thread dédié et commun
 */
public class Executor extends Thread {
  List<Runnable> queue;

  /**
   * @param name
   */
  public Executor(String name) {
    super(name);
    queue = new LinkedList<Runnable>();
  }

  /**
   * Boucle principale de l'exécuteur,
   * boucle *infinie* qui récupère les tâches postées et les exécute
   */
  public void run() {
    Runnable r;
    while (true) {
      synchronized (queue) {
        while (queue.size() == 0) {
          sleep();
          // System.out.println("reveille");
        }

        r = queue.remove(0);

      }
      // System.out.println("va run" + r);
      r.run();
    }
  }

  /**
   * Poste une tâche à exécuter
   * 
   * @param r
   */
  public void post(Runnable r) {
    // System.out.println("posting " + r);
    synchronized (queue) {
      queue.add(r); // at the end…
      // System.out.println("posted" + r);

      queue.notify();
    }
  }

  /**
   * Met en pause l'exécuteur
   */
  private void sleep() {
    try {
      queue.wait();
    } catch (InterruptedException ex) {
      // nothing to do here.
    }
  }

}

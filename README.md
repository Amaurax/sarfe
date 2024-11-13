JOUVE Axel - GOGUILLOT Amaury

# Module de Communication Événementiel

Ce projet implémente un module de communication événementiel basé sur une architecture classique utilisant des brokers, des channels et des message queues. Le système est conçu pour permettre une communication asynchrone et parallèle entre différents composants.

## Structure du Projet

Le projet est organisé en plusieurs classes principales réparties en deux couches : la couche Channel et la couche Message.

### Couche Channel

- **Broker** : Un broker permet d'établir des canaux de communication. Chaque broker doit avoir un nom unique et peut accepter des connexions sur différents ports simultanément.

  - `Broker.java` : Classe abstraite définissant les méthodes `accept` et `connect`.
  - `CBroker.java` : Implémentation concrète de `Broker` utilisant un `Executor` pour gérer les tâches asynchrones.

- **Channel** : Un channel est un flux de bytes point-à-point, full-duplex, permettant la lecture et l'écriture de données. Un channel peut être déconnecté à tout moment.

  - `Channel.java` : Classe abstraite définissant les méthodes `read`, `write`, `disconnect` et `disconnected`.
  - `CChannel.java` : Implémentation concrète de `Channel` utilisant des `CircularBufferEvent` pour gérer les buffers de lecture et d'écriture.

- **CircularBufferEvent** : Utilisé pour gérer les buffers circulaires pour les opérations de lecture et d'écriture dans les channels.
  - `CircularBufferEvent.java` : Implémentation des buffers circulaires avec des listeners pour les événements de lecture et d'écriture.

### Couche Message

- **MessageQueue** : Gère la file d'attente des messages et leur traitement asynchrone. Permet de définir des listeners pour la réception des messages et la fermeture des connexions.

  - `MessageQueue.java` : Classe abstraite définissant les méthodes `setListener`, `send`, `close` et `closed`.
  - `CMessageQueue.java` : Implémentation concrète de `MessageQueue` utilisant un `Channel` pour la communication.

- **QueueBroker** : Encapsule les brokers et permet de créer des message queues. Gère les connexions et les déconnexions des ports.
  - `QueueBroker.java` : Classe abstraite définissant les méthodes `bind`, `unbind` et `connect`.
  - `CQueueBroker.java` : Implémentation concrète de `QueueBroker` utilisant un `Executor` pour gérer les tâches asynchrones.

### Gestion des Brokers

- **BrokerManager** : Permet de gérer plusieurs brokers et de les retrouver par leur nom.

## Utilisation du Serveur Echo

La classe event.test.EchoServerTest.java permet de tester la communication asynchrone entre plusieurs clients et un serveur. <br>
Pour utiliser le serveur Echo, il suffit de lancer la méthode `main` de la classe `EchoServerTest`. Cela créera et lancera un serveur EchoServer.java ainsi que plusieurs clients EchoClient.java, qui échangeront des messages (échange ckassique d'une application echo serveur)

# Spécification du Module de Communication Événementiel

## Introduction

Ce module de communication événementiel est conçu pour permettre une communication asynchrone et parallèle entre différents composants d'un système. Il est basé sur une architecture classique utilisant des brokers, des channels et des message queues. Le module est divisé en deux couches principales : la couche Channel et la couche Message.

## Objectifs

- Permettre une communication asynchrone entre différents composants.
- Gérer les connexions et les messages de manière efficace et flexible.
- Assurer la fiabilité et la sécurité des communications.

## Architecture

Le module est organisé en plusieurs classes principales réparties en deux couches : la couche Channel et la couche Message.

### Couche Channel

#### Broker

Un broker permet d'établir des canaux de communication. Chaque broker doit avoir un nom unique et peut accepter des connexions sur différents ports simultanément.

- **Broker.java** : Classe abstraite définissant les méthodes `accept` et `connect`.
- **CBroker.java** : Implémentation concrète de Broker utilisant un Executor pour gérer les tâches asynchrones.

**Méthodes :**

- `accept(int port, AcceptListener listener)` : Indique que ce broker acceptera une connexion sur le port donné.
- `connect(String name, int port, ConnectListener listener)` : Tente une connexion au port donné, via le broker avec le nom donné.

**Interfaces :**

- **AcceptListener** : Définit le comportement lorsqu'une connexion est acceptée et entièrement connectée.
- **ConnectListener** : Définit le comportement lorsqu'une connexion est entièrement connectée.

#### Channel

Un channel est un flux de bytes point-à-point, full-duplex, permettant la lecture et l'écriture de données. Un channel peut être déconnecté à tout moment.

- **Channel.java** : Classe abstraite définissant les méthodes `read`, `write`, `disconnect` et `disconnected`.
- **CChannel.java** : Implémentation concrète de Channel utilisant des CircularBufferEvent pour gérer les buffers de lecture et d'écriture.

**Méthodes :**

- `read(byte[] bytes, int offset, int length, ReadListener listener)` : Lit les bytes dans le tableau donné, en commençant à l'offset donné.
- `write(byte[] bytes, int offset, int length, WriteListener listener)` : Écrit les bytes du tableau donné, en commençant à l'offset donné.
- `disconnect()` : Déconnecte ce Channel de manière thread-safe.
- `disconnected()` : Retourne true si ce Channel est déconnecté.

**Interfaces :**

- **ReadListener** : Définit le comportement lorsqu'un tableau de bytes a été lu.
- **WriteListener** : Définit le comportement lorsqu'un tableau de bytes a été écrit.

#### CircularBufferEvent

Utilisé pour gérer les buffers circulaires pour les opérations de lecture et d'écriture dans les channels.

- **CircularBufferEvent.java** : Implémentation des buffers circulaires avec des listeners pour les événements de lecture et d'écriture.

**Méthodes :**

- `full()` : Retourne true si ce buffer est plein.
- `empty()` : Retourne true si ce buffer est vide.
- `push(byte b)` : Pousse un byte dans le buffer.
- `pull()` : Tire le prochain byte disponible du buffer.

**Interfaces :**

- **InListener** : Listener pour savoir s'il y a quelque chose à lire.
- **OutListener** : Listener pour savoir s'il y a de la place pour écrire.

### Couche Message

#### MessageQueue

Gère la file d'attente des messages et leur traitement asynchrone. Permet de définir des listeners pour la réception des messages et la fermeture des connexions.

- **MessageQueue.java** : Classe abstraite définissant les méthodes `setListener`, `send`, `close` et `closed`.
- **CMessageQueue.java** : Implémentation concrète de MessageQueue utilisant un Channel pour la communication.

**Méthodes :**

- `setListener(Listener l)` : Permet de définir le listener à utiliser à la réception de messages et à la fermeture de connexion.
- `send(byte[] bytes)` : Permet d'envoyer un message sous forme de tableau d'octets.
- `close()` : Ferme cette MessageQueue de manière thread-safe.
- `closed()` : Retourne true si cette MessageQueue est fermée.

**Interfaces :**

- **Listener** : Permet de définir le comportement lorsqu'un message est reçu et lorsqu'une connexion est fermée.

#### QueueBroker

Encapsule les brokers et permet de créer des message queues. Gère les connexions et les déconnexions des ports.

- **QueueBroker.java** : Classe abstraite définissant les méthodes `bind`, `unbind` et `connect`.
- **CQueueBroker.java** : Implémentation concrète de QueueBroker utilisant un Executor pour gérer les tâches asynchrones.

**Méthodes :**

- `bind(int port, AcceptListener listener)` : Permet d'accepter toute connexion sur le port donné jusqu'à ce que `unbind()` soit appelée.
- `unbind(int port)` : Permet d'arrêter d'unbind un port.
- `connect(String name, int port, ConnectListener listener)` : Permet de se connecter à un autre QueueBroker sur le port donné.

**Interfaces :**

- **AcceptListener** : Définit le comportement lorsqu'une connexion est acceptée et entièrement connectée.
- **ConnectListener** : Définit le comportement lorsqu'une connexion est entièrement connectée.

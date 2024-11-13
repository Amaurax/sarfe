package event.queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import event.given.Broker;
import event.given.Channel;
import event.given.CircularBufferEvent;
import event.given.Executor;

public class CChannel extends Channel {

	// Buffers d'entrée et de sortie
	private CircularBufferEvent in, out;
	// Indicateur d'état de déconnexion
	private boolean disconnected = false;
	// Le canal avec lequel vous communiquez ('canal distant')
	private CChannel linkedChannel;
	// Port de communication
	private int port;
	// la pompe de l'exécuteur d'événements
	private Executor executor;

	// Liste des WriteRequest et ReadRequest
	private List<WriteRequest> writeRequests = new ArrayList<>();
	private List<ReadRequest> readRequests = new ArrayList<>();

	// Utilisé pour savoir si on est en train d'écrire ou de lire
	private boolean writing = false, reading = false;

	/*
	 * Appelle le constructeur super() avec le broker donné.
	 * Stocke le port donné
	 * Initialise les buffers in et out avec deux CircularBuffer différents :
	 * 
	 * Synchronise readRequests et vérifie s'il y a une nouvelle demande et si on
	 * n'est pas déjà en train de lire :
	 * --- si vrai : executor travaille sur la première demande
	 * Synchronise writeRequests et vérifie s'il y a une nouvelle demande et si on
	 * n'est pas déjà en train d'écrire :
	 * --- si vrai : executor travaille sur la première demande
	 */
	/**
	 * Crée un canal partiellementconnecté
	 * 
	 * @param broker : Broker parent
	 * @param port   : port de communication
	 * @see {@link Channel#Channel(Broker) Channel(Broker)}
	 */
	protected CChannel(Broker broker, int port, Executor executor) {
		super(broker);
		this.port = port;
		this.in = new CircularBufferEvent(256, executor, new CircularBufferEvent.InListener() {
			@Override
			public void bytesAvailable() {
				synchronized (readRequests) {
					if (readRequests.size() > 0 && !reading) {
						executor.post(readRequests.get(0));
						reading = true;
					}
				}

			}
		});
		this.out = new CircularBufferEvent(256, executor, new CircularBufferEvent.OutListener() {
			@Override
			public void spaceFreed() {
				synchronized (writeRequests) {
					if (writeRequests.size() > 0 && !writing) {
						executor.post(writeRequests.get(0));
						writing = true;
					}

				}
			}
		});
		this.executor = executor;
	}

	/*
	 * Appelle le constructeur super().
	 * Stocke le port donné
	 * Stocke le canal donné
	 * Appelle la méthode setLinkedChannel() du canal donné avec this
	 * Stocke le buffer in du canal donné dans notre champ out.
	 * Stocke le buffer out du canal donné dans notre champ in.
	 * Stocke l'exécuteur donné
	 */
	/**
	 * Crée un canal entièrement connecté.
	 * Complète la connexion du le canal distant.
	 * 
	 * @param broker  : Broker parent
	 * @param port    : port de communication
	 * @param channel : channel 'distant'
	 */
	protected CChannel(Broker broker, int port, CChannel channel, Executor executor) {
		super(broker);
		this.port = port;
		this.linkedChannel = channel;
		channel.setLinkedChannel(this);
		this.in = channel.getOutBuffer();
		this.out = channel.getInBuffer();
		this.executor = executor;
	}

	@Override
	public String getRemoteName() {
		return this.linkedChannel.getBroker().getName();
	}

	@Override
	public int getPort() {
		return this.port;
	}

	/*
	 * Poste une readRequest
	 * Vérifie si le channel est lié
	 * Vérifie si les arguments sont corrects
	 * Vérifie si le canal n'est pas déconnecté
	 * Ajoute une nouvelle ReadRequest avec les arguments donnés
	 * Synchronisation sur la liste des readRequests et si on n'est pas déjà en
	 * train de lire :
	 * --- La pompe se charge de la première requête
	 * --- Le flag read passe à true
	 */
	@Override
	public void read(byte[] bytes, int offset, int length, ReadListener listener) {
		if (!this.isLinked())
			throw new IllegalStateException(
					"CChannel[" + this.getBroker().getName() + ":" + port + "] read : not linked");
		if (bytes == null || offset < 0 || length < 0 || offset + length > bytes.length)
			throw new IllegalArgumentException(this.toString() + " read : Illegal arguments");
		if (disconnected())
			throw new IllegalStateException(this.toString() + " read : disconnected");
		readRequests.add(new ReadRequest(bytes, offset, length, listener));
		synchronized (readRequests) {
			if (!reading) {
				executor.post(readRequests.get(0));
				reading = true;
			}
		}
	}

	/*
	 * Poste une writeRequest
	 * Vérifie si le channel est lié
	 * Vérifie si les arguments sont corrects
	 * Vérifie si le canal n'est pas déconnecté
	 * Ajoute une nouvelle WriteRequest avec les arguments donnés
	 * Synchronisation sur la liste des writeRequests et si on n'est pas déjà en
	 * train d'écrire :
	 * --- La pompe se charge de la première requête
	 * --- Le flag write passe à true
	 */
	@Override
	public void write(byte[] bytes, int offset, int length, WriteListener listener) {
		if (!this.isLinked())
			throw new IllegalStateException(
					"CChannel[" + this.getBroker().getName() + ":" + port + "] read : not linked");
		if (bytes == null || offset < 0 || length < 0 || offset + length > bytes.length)
			throw new IllegalArgumentException(this.toString() + " write : Illegal arguments");
		if (disconnected())
			throw new IllegalStateException(this.toString() + " write : disconnected");
		writeRequests.add(new WriteRequest(bytes, offset, length, listener));
		synchronized (writeRequests) {
			if (!writing) {
				executor.post(writeRequests.get(0));
				writing = true;
			}
		}
	}

	@Override
	public void disconnect() {
		this.disconnected = true;
	}

	@Override
	public boolean disconnected() {
		return this.disconnected;
	}

	public CircularBufferEvent getInBuffer() {
		return this.in;
	}

	public CircularBufferEvent getOutBuffer() {
		return this.out;
	}

	public void setLinkedChannel(CChannel channel) {
		this.linkedChannel = channel;
	}

	public boolean isLinked() {
		return this.linkedChannel != null;
	}

	/*
	 * Chaque WriteRequest est un Runnable
	 * Chaque WriteRequest a :
	 * -- le tableau d'octets à écrire
	 * -- Un offset
	 * -- la longueur
	 * -- Un WriteListener
	 * 
	 * Si le canal est déconnecté, la liste des writeRequests est purgée
	 * Si le outCircularBuffer n'est pas plein :
	 * -- si le canal distant est déconnecté et qu'il n'y a rien à lire dans le
	 * InCircularBuffer :
	 * -------- Le channel est déconnecté et la liste des writeRequests est purgée
	 * -- Tant que possible, on écrit dans le outCircularBuffer
	 * -- Un nouveau runnable est créé et le listener sait qu'un message a été écrit
	 * et connaît le nombre d'octets écrits
	 * -- Le runnable est posté
	 * -- La requête venant d'être exécutée est supprimée de la liste des
	 * WriteRequest
	 * -- Si le outCircularBuffer n'est pas plein et s'il y a une autre writeRequest
	 * dans la liste
	 * -------- On poste la première de la liste des writeRequests
	 * -- sinon le flag write passe à false
	 * sinon le flag wriite passe à false
	 */

	private class WriteRequest implements Runnable {
		byte[] bytes;
		int offset;
		int length;
		WriteListener listener;

		public WriteRequest(byte[] bytes, int offset, int length, WriteListener listener) {
			this.bytes = bytes;
			this.offset = offset;
			this.length = length;
			this.listener = listener;
		}

		@Override
		public void run() {
			if (disconnected()) {
				writeRequests.clear();
				return;
			}
			if (!out.full()) {
				if (linkedChannel.disconnected() && in.empty()) {
					disconnect();
					writeRequests.clear();
					return;
				}
				int writtenBytes = 0;
				while (!out.full() && writtenBytes < length && !disconnected()) {
					try {
						out.push(bytes[writtenBytes + offset]);
						writtenBytes++;
					} catch (IllegalStateException e) {
						break;
					}
				}
				final int b = writtenBytes;
				Runnable r = new Runnable() {
					@Override
					public void run() {
						listener.written(bytes, offset, length, b);
					}
				};
				executor.post(r);
				writeRequests.remove(this);
				if (!out.full() && writeRequests.size() > 0)
					executor.post(writeRequests.get(0));
				else
					synchronized (writeRequests) {
						writing = false;
					}
			} else {
				synchronized (writeRequests) {
					writing = false;
				}
			}
		}
	}

	/*
	 * Chaque ReadRequest est un Runnable
	 * Chaque ReadRequest a :
	 * -- le tableau d'octets à lire
	 * -- Un offset
	 * -- la longueur
	 * -- Un ReadListener
	 * 
	 * Si le canal est déconnecté, la liste des readRequests est purgée
	 * Si le inCircularBuffer n'est pas vide :
	 * -- Tant que possible, on lit dans le inCircularBuffer
	 * -- Un nouveau runnable est créé et le listener sait qu'un message a été lu et
	 * connaît le nombre d'octets lus
	 * -- Le runnable est posté
	 * -- La requête venant d'être exécutée est supprimée de la liste des
	 * ReadRequest
	 * -- Si le inCircularBuffer est vide et que le canal distant est déconnecté :
	 * * -------- Le canal est déconnecté et la liste des readRequests est purgée
	 * -- Si le inCircularBuffer n'est pas vide et s'il y a une autre ReadRequest
	 * dans la liste
	 * -------- On poste la première de la liste des readRequests
	 * -- sinon le flag red passe à false
	 * sinon le flag read passe à false
	 */

	private class ReadRequest implements Runnable {
		byte[] bytes;
		int offset;
		int length;
		ReadListener listener;

		public ReadRequest(byte[] bytes, int offset, int length, ReadListener listener) {
			this.bytes = bytes;
			this.offset = offset;
			this.length = length;
			this.listener = listener;
		}

		@Override
		public void run() {
			if (disconnected()) {
				readRequests.clear();
				return;
			}
			if (!in.empty()) {
				int readBytes = 0;
				while (readBytes < length && !disconnected()) {
					try {
						bytes[offset + readBytes] = in.pull();
						readBytes++;
					} catch (IllegalStateException e) {
						break;
					}
				}
				final int b = readBytes;
				Runnable r = new Runnable() {
					@Override
					public void run() {
						listener.read(Arrays.copyOfRange(bytes, offset, offset + b));
					}
				};
				executor.post(r);
				readRequests.remove(this);

				if (in.empty() && linkedChannel.disconnected()) {
					disconnect();
					readRequests.clear();
					return;
				}
				if (!in.empty() && readRequests.size() > 0)
					executor.post(readRequests.get(0));
				else
					synchronized (readRequests) {
						reading = false;
					}
			} else {
				synchronized (readRequests) {
					reading = false;
				}
			}
		}
	}

	@Override
	public String toString() {
		return "CChannel[" + this.getBroker().getName() + ":" + port + "]-[" + linkedChannel.getBroker().getName() + ":"
				+ linkedChannel.port + "]";
	}

}

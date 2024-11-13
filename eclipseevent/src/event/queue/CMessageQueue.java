package event.queue;

import java.nio.ByteBuffer;
import java.util.Arrays;

import event.given.Channel;
import event.given.Channel.ReadListener;
import event.given.Channel.WriteListener;
import event.given.Executor;
import event.given.MessageQueue;
import event.given.QueueBroker;

public class CMessageQueue extends MessageQueue {

	public static final byte[] UNBINDING_QUEUE_PAYLOAD = { 0 }, REGULAR_QUEUE_PAYLOAD = { 1 };
	private Channel channel;
	private Executor executor;
	private QueueBroker broker;
	private Listener listener;

	public CMessageQueue(Channel channel, QueueBroker broker, Executor executor) {
		this.channel = channel;
		this.executor = executor;
		this.broker = broker;
	}

	@Override
	public QueueBroker broker() {
		return this.broker;
	}

	/*
	 * Définit le listener de manière synchronisée et vérifie que c'est la première
	 * fois
	 * si c'est la première fois :
	 * -- startReadMessage() est lancée
	 */
	@Override
	public synchronized void setListener(Listener l) {
		boolean firstTime = this.listener == null;
		this.listener = l;
		if (firstTime) {
			startReadMessage();
		}
	}

	/**
	 * Démarre la lecture d'un message à partir du canal.
	 * - Iinitialise un tableau de bytes pour stocker la taille du message
	 * - Définit un readlistener pour traiter les données lues. Si la taille du
	 * message est complètement lue, elle appelle la méthode
	 * readMessage avec la taille du message. Sinon, elle continue à lire les
	 * données restantes.
	 * 
	 * En cas d'erreur lors de la lecture du channel, la méthode close est appelée
	 * pour fermer la connexion.
	 */
	private void startReadMessage() {
		byte[] messageSize = new byte[Integer.BYTES * 2];
		ReadListener listenerSize = new ReadListener() {
			@Override
			public void read(byte[] bytes) {
				if (byteArrayToInt(Arrays.copyOf(messageSize, Integer.BYTES)) + bytes.length == Integer.BYTES) {
					readMessage(byteArrayToInt(Arrays.copyOfRange(messageSize, Integer.BYTES, messageSize.length)));
				} else {
					int index = byteArrayToInt(Arrays.copyOf(messageSize, Integer.BYTES)) + bytes.length;
					byte[] indexArray = intToByteArray(index);
					for (int y = 0; y < indexArray.length; y++)
						messageSize[y] = indexArray[y];
					try {
						channel.read(messageSize, index + Integer.BYTES, Integer.BYTES - index, this);
					} catch (Exception e) {
						close();
					}
				}
			}
		};
		try {
			channel.read(messageSize, Integer.BYTES, Integer.BYTES, listenerSize);
		} catch (Exception e) {
			close();
		}
	}

	/**
	 * Lit un message de la taille spécifiée à partir du canal et le traite.
	 *
	 * @param size la taille du message à lire en octets
	 *
	 *             Crée un tableau d'octets pour stocker le message,
	 *             Utilise un ReadListener pour lire les octets du canal.
	 *             Si la taille totale des octets lus correspond à la taille
	 *             spécifiée,
	 *             - exécute un Runnable pour traiter le message reçu.
	 *             Sinon, elle met à jour l'indiex de lecture et continue à lire les
	 *             octets restants.
	 *
	 *             En cas d'erreur lors de la lecture du channel, la méthode ferme
	 *             le channel.
	 */
	private void readMessage(int size) {
		byte[] message = new byte[Integer.BYTES + size];
		ReadListener listenerSize = new ReadListener() {
			@Override
			public void read(byte[] bytes) {
				if (byteArrayToInt(Arrays.copyOf(message, Integer.BYTES)) + bytes.length == size) {
					Runnable r = new Runnable() {
						@Override
						public void run() {
							listener.received(Arrays.copyOfRange(message, Integer.BYTES, message.length));
						}
					};
					executor.post(r);
					startReadMessage();
				} else {
					int index = byteArrayToInt(Arrays.copyOf(message, Integer.BYTES)) + bytes.length;
					byte[] indexArray = intToByteArray(index);
					for (int y = 0; y < indexArray.length; y++)
						message[y] = indexArray[y];
					try {
						channel.read(message, index + Integer.BYTES, size - index, this);
					} catch (Exception e) {
						close();
					}
				}
			}
		};
		try {
			channel.read(message, Integer.BYTES, size, listenerSize);
		} catch (Exception e) {
			close();
		}
	}

	/**
	 * Envoie un message sous forme de tableau d'octets.
	 * 
	 * Méthode synchronisée pour garantir que l'envoi du message est thread-safe.
	 * On convertit d'abord la taille du message en tableau d'octets, puis on
	 * concatène cette taille
	 * avec le message original.
	 * On utilise un WriteListener pour gérer l'écriture asynchrone sur le canal.
	 * 
	 * @param bytes Le message à envoyer sous forme de tableau d'octets.
	 * @return true si le message a été envoyé avec succès.
	 */
	@Override
	public synchronized boolean send(byte[] bytes) {
		byte[] size = intToByteArray(bytes.length);
		byte[] message = concatArray(size, 0, size.length, bytes, 0, bytes.length);
		WriteListener writeListener = new WriteListener() {
			@Override
			public void written(byte[] bytes, int offset, int length, int written) {
				if (written != length) {
					try {
						channel.write(bytes, offset + written, length - written, this);
					} catch (Exception e) {
						close();
					}
				}
			}
		};
		try {
			channel.write(message, 0, message.length, writeListener);
		} catch (Exception e) {
			close();
		}
		return true;
	}

	@Override
	public void close() {
		if (!channel.disconnected())
			channel.disconnect();
		if (listener != null) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					listener.closed();
				}
			};
			executor.post(r);
		}
	}

	@Override
	public boolean closed() {
		return channel.disconnected();
	}

	@Override
	public String getRemoteName() {
		return this.channel.getRemoteName();
	}

	/**
	 * Convertis un tableau de bytes en un entier
	 * 
	 * @param array
	 * @return
	 */
	private int byteArrayToInt(byte[] array) {
		ByteBuffer buffer = ByteBuffer.wrap(array);
		return buffer.getInt();
	}

	/**
	 * Convertit un entier en un tableau de bytes
	 * 
	 * @param i : l'entier à convertir
	 * @return : L'interprétation sous forme de tableau de bytes
	 */
	private byte[] intToByteArray(int i) {
		ByteBuffer buffer = ByteBuffer.wrap(new byte[Integer.BYTES]);
		buffer.putInt(i);
		return buffer.array();
	}

	/**
	 * Lit la taille de la charge utile et appelle la méthode
	 * {@link #readPayload(int)}
	 * lorsqu'elle est terminée.
	 * 
	 * @param listener : le listener à appeler lorsque la charge utile est lue
	 */
	public void startReadPayload(ReadListener listener) {
		byte[] messageSize = new byte[Integer.BYTES * 2]; // messageSize[] = [reading index (int)] + [message size
															// (int)]
		ReadListener listenerSize = new ReadListener() {
			@Override
			public void read(byte[] bytes) {
				if (byteArrayToInt(Arrays.copyOf(messageSize, Integer.BYTES)) + bytes.length == Integer.BYTES) {
					readPayload(byteArrayToInt(Arrays.copyOfRange(messageSize, Integer.BYTES, messageSize.length)),
							listener);
				} else {
					int index = byteArrayToInt(Arrays.copyOf(messageSize, Integer.BYTES)) + bytes.length;
					byte[] indexArray = intToByteArray(index);
					for (int y = 0; y < indexArray.length; y++)
						messageSize[y] = indexArray[y];
					try {
						channel.read(messageSize, index + Integer.BYTES, Integer.BYTES - index, this);
					} catch (Exception e) {
						close();
					}
				}
			}
		};
		try {
			channel.read(messageSize, Integer.BYTES, Integer.BYTES, listenerSize);
		} catch (Exception e) {
			close();
		}
	}

	/**
	 * Lit une charge utile et appelle la méthode read() du listener lorsqu'elle est
	 * terminée
	 * 
	 * @param size     : la taille de la charge utile
	 * @param listener : le listener à appeler lorsqu'elle est terminée
	 */
	private void readPayload(int size, ReadListener listener) {
		byte[] message = new byte[Integer.BYTES + size]; // message[] = [reading index (int)] + [message]
		ReadListener listenerSize = new ReadListener() {
			@Override
			public void read(byte[] bytes) {
				if (byteArrayToInt(Arrays.copyOf(message, Integer.BYTES)) + bytes.length == size) {
					Runnable r = new Runnable() {
						@Override
						public void run() {
							listener.read(Arrays.copyOfRange(message, Integer.BYTES, message.length));
						}
					};
					executor.post(r);
				} else {
					int index = byteArrayToInt(Arrays.copyOf(message, Integer.BYTES)) + bytes.length;
					byte[] indexArray = intToByteArray(index);
					for (int y = 0; y < indexArray.length; y++)
						message[y] = indexArray[y];
					try {
						channel.read(message, index + Integer.BYTES, Integer.BYTES - index, this);
					} catch (Exception e) {
						close();
					}
				}
			}
		};
		try {
			channel.read(message, Integer.BYTES, size, listenerSize);
		} catch (Exception e) {
			close();
		}
	}

	/**
	 * <pre>
	 * Exemple : concatArray([0,1,2,3,4], 1, 3, [5,6,7,8,9], 0, 4) <br>	retourne [1,2,3,5,6,7,8]
	 * </pre>
	 * 
	 * @param a        : le premier tableau
	 * @param offset_a : l'index de départ du premier tableau
	 * @param length_a : le nombre d'octets à copier du premier tableau
	 * @param b        : le deuxième tableau
	 * @param offset_b : l'index de départ du deuxième tableau
	 * @param length_b : le nombre d'octets à copier du deuxième tableau
	 * @return Les deux tableaux concaténés
	 */
	private byte[] concatArray(byte[] a, int offset_a, int length_a, byte[] b, int offset_b, int length_b) {
		// exception si les arguments ne sont pas valide
		if (a == null || offset_a < 0 || length_a < 0 || a.length < offset_a + length_a)
			throw new IllegalArgumentException("Illegal arguments for byte[] concatenation");
		if (b == null || offset_b < 0 || length_b < 0 || b.length < offset_b + length_b)
			throw new IllegalArgumentException("Illegal arguments for byte[] concatenation");
		// concaténation du tableau a et b
		byte[] result = new byte[length_a + length_b];
		int index = 0;
		for (int i = offset_a; i < length_a; i++) {
			result[index] = a[i];
			index++;
		}
		for (int i = offset_b; i < length_b; i++) {
			result[index] = b[i];
			index++;
		}
		return result;
	}

}

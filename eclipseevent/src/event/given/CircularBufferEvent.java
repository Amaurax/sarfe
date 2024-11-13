package event.given;

/* CircularBuffer avec désormais 2 listener pour chaque canal :
 * Permettent savoir s'il y a respectivement quelque chose à lire ou à écrire
 */

public class CircularBufferEvent {
	int m_tail, m_head;
	byte m_bytes[];
	private InListener inListener;
	private OutListener outListener;
	private Executor executor;

	public CircularBufferEvent(int capacity, Executor executor) {
		m_bytes = new byte[capacity];
		m_tail = m_head = 0;
		this.executor = executor;
	}

	public CircularBufferEvent(int capacity, Executor executor, InListener inListener) {
		m_bytes = new byte[capacity];
		m_tail = m_head = 0;
		this.executor = executor;
		this.inListener = inListener;
	}

	public CircularBufferEvent(int capacity, Executor executor, OutListener outListener) {
		m_bytes = new byte[capacity];
		m_tail = m_head = 0;
		this.executor = executor;
		this.outListener = outListener;
	}

	/**
	 * @return true si ce buffer est plein, false sinon
	 */
	public boolean full() {
		int next = (m_head + 1) % m_bytes.length;
		return (next == m_tail);
	}

	/**
	 * @return true si ce buffer est vide, false sinon
	 */
	public boolean empty() {
		return (m_tail == m_head);
	}

	/**
	 * @param b: le byte à pousser dans le buffer
	 * @throws IllegalStateException si plein.
	 */
	public void push(byte b) {
		boolean wasEmpty = empty();
		int next = (m_head + 1) % m_bytes.length;
		if (next == m_tail)
			throw new IllegalStateException();
		m_bytes[m_head] = b;
		m_head = next;
		if (wasEmpty && inListener != null) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					inListener.bytesAvailable();
				}
			};
			executor.post(r);
		}
	}

	/**
	 * @return le prochain byte disponible
	 * @throws IllegalStateException si vide.
	 */
	public byte pull() {
		boolean wasFull = full();
		if (m_tail == m_head)
			throw new IllegalStateException();
		int next = (m_tail + 1) % m_bytes.length;
		byte bits = m_bytes[m_tail];
		m_tail = next;
		if (wasFull && outListener != null) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					outListener.spaceFreed();
				}
			};
			executor.post(r);
		}
		return bits;
	}

	/* Listener pour savoir s'il y a quelque chose à lire */
	public interface InListener {
		public void bytesAvailable();
	}

	/* Listener pour savoir s'il y a de la place pour écrire */
	public interface OutListener {
		public void spaceFreed();
	}
}

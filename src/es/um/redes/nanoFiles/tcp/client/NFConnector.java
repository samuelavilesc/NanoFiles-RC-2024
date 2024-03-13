package es.um.redes.nanoFiles.tcp.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileDigest;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor
public class NFConnector {
	private Socket socket;
	private InetSocketAddress serverAddr;
	private DataInputStream dis;
	private DataOutputStream dos;
	/*
	 * 
	 * ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES +
	 * Integer.BYTES + bserverdata.length);
	 * bb.put(DirMessageOps.OPCODE_SERVE_FILES); bb.putInt(bserverdata.length);
	 * bb.put(bserverdata);
	 */

	public NFConnector(InetSocketAddress fserverAddr) throws UnknownHostException, IOException {
		serverAddr = fserverAddr;
		try {
			this.socket = new Socket(fserverAddr.getHostName(), fserverAddr.getPort());
			this.dis = new DataInputStream(socket.getInputStream());
			this.dos = new DataOutputStream(socket.getOutputStream());
		} catch (UnknownHostException e) {
			System.err.println("Fallo al crear la conexión con el socket.");
			e.printStackTrace();
		}
		/*
		 * Se crea el socket a partir de la dirección del servidor (IP, puerto). La
		 * creación exitosa del socket significa que la conexión TCP ha sido
		 * establecida.
		 */

		/*
		 * Se crean los DataInputStream/DataOutputStream a partir de los streams de
		 * entrada/salida del socket creado. Se usarán para enviar (dos) y recibir (dis)
		 * datos del servidor.
		 */

	}
	public void close() {

		try {
			this.dis.close();
			this.dos.close();
			this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Método para descargar un fichero a través del socket mediante el que estamos
	 * conectados con un peer servidor.
	 * 
	 * @param targetFileHashSubstr Subcadena del hash del fichero a descargar
	 * @param file                 El objeto File que referencia el nuevo fichero
	 *                             creado en el cual se escribirán los datos
	 *                             descargados del servidor
	 * @return Verdadero si la descarga se completa con éxito, falso en caso
	 *         contrario.
	 * @throws IOException Si se produce algún error al leer/escribir del socket.
	 */
	public boolean downloadFile(String targetFileHashSubstr, File file) throws IOException {
		boolean downloaded = false;
		byte longitud = (byte) targetFileHashSubstr.length();
		// enviamos la subcadena del hash
		PeerMessage sendToServer = new PeerMessage(PeerMessageOps.OPCODE_DOWNLOAD_FILE, longitud,
				targetFileHashSubstr.getBytes());
		sendToServer.writeMessageToOutputStream(dos);
		/*
		 * Construir objetos PeerMessage que modelen mensajes con los valores adecuados
		 * en sus campos (atributos), según el protocolo diseñado, y enviarlos al
		 * servidor a través del "dos" del socket mediante el método
		 * writeMessageToOutputStream.
		 */
		PeerMessage hashMessageFromServer = PeerMessage.readMessageFromInputStream(dis);
		String hashFromServer = new String(hashMessageFromServer.getParam2());
		PeerMessage receiveDataFromServer = PeerMessage.readMessageFromInputStream(dis);
		byte opcode = receiveDataFromServer.getOpcode();
		byte[] par2 = receiveDataFromServer.getParam2();
		if (opcode == PeerMessageOps.OPCODE_SEND_FILE) {
			try (FileOutputStream fos = new FileOutputStream(file)) {
				fos.write(par2);
				fos.close();
			}
			downloaded = true;
		} else {
			System.err.println("El servidor no pudo completar la descarga.");
		}
		/* TODO: hacer que funcione con chunks de ficheros */
		/*
		 * : Recibir mensajes del servidor a través del "dis" del socket usando
		 * PeerMessage.readMessageFromInputStream, y actuar en función del tipo de
		 * mensaje recibido, extrayendo los valores necesarios de los atributos del
		 * objeto (valores de los campos del mensaje).
		 */
		/*
		 * Para escribir datos de un fichero recibidos en un mensaje, se puede crear un
		 * FileOutputStream a partir del parámetro "file" para escribir cada fragmento
		 * recibido (array de bytes) en el fichero mediante el método "write". Cerrar el
		 * FileOutputStream una vez se han escrito todos los fragmentos.
		 */
		/*
		 * NOTA: Hay que tener en cuenta que puede que la subcadena del hash pasada como
		 * parámetro no identifique unívocamente ningún fichero disponible en el
		 * servidor (porque no concuerde o porque haya más de un fichero coincidente con
		 * dicha subcadena)
		 */

		// recibir el contenido del fichero
		if (downloaded) {

			String hashOfFile = FileDigest.computeFileChecksumString(file.getName());
			if (hashFromServer.equals(hashOfFile)) {
				System.out.println("La descarga del archivo fue exitosa y su integridad ha sido verificada.");
			} else {
				System.out.println("La descarga del archivo fue exitosa pero su integridad no pudo ser verificada.");
			}
		}
		/*
		 * TODO: Finalmente, comprobar la integridad del fichero creado para comprobar
		 * que es idéntico al original, calculando el hash a partir de su contenido con
		 * FileDigest.computeFileChecksumString y comparándolo con el hash completo del
		 * fichero solicitado. Para ello, es necesario obtener del servidor el hash
		 * completo del fichero descargado, ya que quizás únicamente obtuvimos una
		 * subcadena del mismo como parámetro.
		 */

		return downloaded;
	}

	public InetSocketAddress getServerAddr() {
		return serverAddr;
	}

}

package es.um.redes.nanoFiles.tcp.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileDigest;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor
public class NFConnector {
	private Socket socket;
	private InetSocketAddress serverAddr;
	private DataInputStream dis;
	private DataOutputStream dos;

	public NFConnector(InetSocketAddress fserverAddr) throws UnknownHostException, IOException {
		serverAddr = fserverAddr;
		try {
			this.socket = new Socket(fserverAddr.getHostName(), fserverAddr.getPort());
			this.dis = new DataInputStream(socket.getInputStream());
			this.dos = new DataOutputStream(socket.getOutputStream());
		} catch (UnknownHostException e) {
			System.err.println("Fallo al crear la conexión con el socket.");
		}
		
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
		// recibimos el hash del fichero que nos va a devolver (si está bien el hash
		// enviado)
		PeerMessage hashMessageFromServer = PeerMessage.readMessageFromInputStream(dis);
		String hashFromServer = "";
		if (hashMessageFromServer.getOpcode() == PeerMessageOps.OPCODE_GET_HASH) {
			hashFromServer = new String(hashMessageFromServer.getParam2());
			PeerMessage receiveDataFromServer = PeerMessage.readMessageFromInputStream(dis);
			byte[] par2 = receiveDataFromServer.getParam2();
			if (receiveDataFromServer.getOpcode() == PeerMessageOps.OPCODE_SEND_FILE) {
				try (FileOutputStream fos = new FileOutputStream(file)) {
					fos.write(par2);
					fos.close();
				}
				downloaded = true;
			} else {
				System.err.println("El servidor no pudo completar la descarga.");
			}

		} else {
			System.err.println("El hash enviado es ambiguo, no se pudo completar la descarga.");
		}

		// recibir el contenido del fichero
		if (downloaded) {

			String hashOfFile = FileDigest.computeFileChecksumString(file.getName());
			if (hashFromServer.equals(hashOfFile)) {
				System.out.println("La descarga del archivo fue exitosa y su integridad ha sido verificada.");
			} else {
				System.out.println("La descarga del archivo fue exitosa pero su integridad no pudo ser verificada.");
			}
		}

		return downloaded;
	}

	public InetSocketAddress getServerAddr() {
		return serverAddr;
	}

}

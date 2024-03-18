package es.um.redes.nanoFiles.udp.message;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;

import es.um.redes.nanoFiles.util.FileInfo;

/**
 * Clase que modela los mensajes del protocolo de comunicación entre pares para
 * implementar el explorador de ficheros remoto (servidor de ficheros). Estos
 * mensajes son intercambiados entre las clases DirectoryServer y
 * DirectoryConnector, y se codifican como texto en formato "campo:valor".
 * 
 * @author rtitos
 *
 */
public class DirMessage {
	public static final int PACKET_MAX_SIZE = 65507; // 65535 - 8 (UDP header) - 20 (IP header)

	private static final char DELIMITER = ':'; // Define el delimitador
	private static final char END_LINE = '\n'; // Define el carácter de fin de línea
	private static final String IPDELIMITER = ":";
	/**
	 * Nombre del campo que define el tipo de mensaje (primera línea)
	 */
	private static final String FIELDNAME_OPERATION = "operation";
	private static final String FIELDNAME_NICKNAME = "nickname";
	private static final String FIELDNAME_SESSIONKEY = "session_key";
	private static final String FIELDNAME_HOSTNAME = "hostname";
	private static final String FIELDNAME_FILEINFO = "fileinfo";

	/*
	 * TODO: Definir de manera simbólica los nombres de todos los campos que pueden
	 * aparecer en los mensajes de este protocolo (formato campo:valor)
	 */

	/**
	 * Tipo del mensaje, de entre los tipos definidos en PeerMessageOps.
	 */
	private String operation = DirMessageOps.OPERATION_INVALID;
	/*
	 * TODO: Crear un atributo correspondiente a cada uno de los campos de los
	 * diferentes mensajes de este protocolo.
	 */
	private String nickname;
	private int session_key;
	private String hostname;
	private String fileInfo;

	public DirMessage(String op) {
		operation = op;
		nickname = null;
		session_key = -1;
		hostname=null;
		fileInfo=null;
	}

	/*
	 *
	 * Constructor para iniciar sesion
	 */
	public DirMessage(String op, String nickname) {
		this.nickname = nickname;
		operation = op;
		session_key = -1;
		hostname=null;
		fileInfo=null;
	}

	/*
	 * Constructor para cerrar sesion
	 * 
	 */
	public DirMessage(String op, int session_key) {
		operation = op;
		nickname = null;
		this.session_key = session_key;
		hostname=null;
		fileInfo=null;
	}
	/*
	 * Constructor para dar de alta servidor
	 * 
	 */
	public DirMessage(String op, int session_key, String hostname) {
		operation = op;
		nickname = null;
		this.session_key = session_key;
		this.hostname=hostname;
		fileInfo=null;
	}

	public int getSession_key() {
		return session_key;
	}

	public void setSession_key(int session_key) {
		this.session_key = session_key;
	}

	/*
	 * TODO: Crear diferentes constructores adecuados para construir mensajes de
	 * diferentes tipos con sus correspondientes argumentos (campos del mensaje)
	 */
	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public String getOperation() {
		return operation;
	}
	public int getPort() {
		return Integer.parseInt(this.hostname.split(IPDELIMITER)[1]);
	}

	public void setNickname(String nick) {

		nickname = nick;
	}

	public String getNickname() {

		return nickname;
	}

	/**
	 * Método que convierte un mensaje codificado como una cadena de caracteres, a
	 * un objeto de la clase PeerMessage, en el cual los atributos correspondientes
	 * han sido establecidos con el valor de los campos del mensaje.
	 * 
	 * @param message El mensaje recibido por el socket, como cadena de caracteres
	 * @return Un objeto PeerMessage que modela el mensaje recibido (tipo, valores,
	 *         etc.)
	 */
	public static DirMessage fromString(String message) {
		/*
		 * TODO: Usar un bucle para parsear el mensaje línea a línea, extrayendo para
		 * cada línea el nombre del campo y el valor, usando el delimitador DELIMITER, y
		 * guardarlo en variables locales.
		 */

		// System.out.println("DirMessage read from socket:");
		// System.out.println(message);
		String[] lines = message.split(END_LINE + "");
		// Local variables to save data during parsing
		DirMessage m = null;

		for (String line : lines) {
			int idx = line.indexOf(DELIMITER); // Posición del delimitador
			String fieldName = line.substring(0, idx).toLowerCase(); // minúsculas
			String value = line.substring(idx + 1).trim();

			switch (fieldName) {
			case FIELDNAME_OPERATION: {
				assert (m == null);
				m = new DirMessage(value);
				break;

			}
			case FIELDNAME_NICKNAME: {
				if (m != null && value != null) {
					m.setNickname(value);
				}
				break;
			}
			case FIELDNAME_SESSIONKEY: {
				//comprobando que no está definido como el valor por defecto
				if (m != null && Integer.parseInt(value) != -1) {
					m.setSession_key(Integer.parseInt(value));
				}
				break;
			}
			case FIELDNAME_HOSTNAME: {
				if (m != null && value != null) {
					
				}
				m.setHostname(value);
				break;
			}
			case FIELDNAME_FILEINFO: {
				if (m != null && value != null) {
					m.setFileInfo(value);	
				}
				
				break;
			}

			default:
				System.err.println("PANIC: DirMessage.fromString - message with unknown field name " + fieldName);
				System.err.println("Message was:\n" + message);
				System.exit(-1);
			}
		}

		return m;
	}

	/**
	 * Método que devuelve una cadena de caracteres con la codificación del mensaje
	 * según el formato campo:valor, a partir del tipo y los valores almacenados en
	 * los atributos.
	 * 
	 * @return La cadena de caracteres con el mensaje a enviar por el socket.
	 */
	public String toString() {

		StringBuffer sb = new StringBuffer();
		sb.append(FIELDNAME_OPERATION + DELIMITER + operation + END_LINE); // Construimos el campo
		sb.append(FIELDNAME_NICKNAME + DELIMITER + nickname + END_LINE);
		sb.append(FIELDNAME_SESSIONKEY + DELIMITER + session_key + END_LINE);
		sb.append(FIELDNAME_HOSTNAME + DELIMITER + hostname + END_LINE);
		sb.append(FIELDNAME_FILEINFO + DELIMITER + fileInfo + END_LINE);
		/*
		 * TODO: En función del tipo de mensaje, crear una cadena con el tipo y
		 * concatenar el resto de campos necesarios usando los valores de los atributos
		 * del objeto.
		 */

		sb.append(END_LINE); // Marcamos el final del mensaje
		return sb.toString();
	}

	public String getFileInfo() {
		return fileInfo;
	}

	public void setFileInfo(String fileInfo) {
		this.fileInfo = fileInfo;
	}

}

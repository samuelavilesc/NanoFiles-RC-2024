package es.um.redes.nanoFiles.udp.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;

import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */
public class DirectoryConnector {
	/**
	 * Puerto en el que atienden los servidores de directorio
	 */
	private static final int DIRECTORY_PORT = 6868;
	/**
	 * Tiempo máximo en milisegundos que se esperará a recibir una respuesta por el
	 * socket antes de que se deba lanzar una excepción SocketTimeoutException para
	 * recuperar el control
	 */
	private static final int TIMEOUT = 1000;
	/**
	 * Número de intentos máximos para obtener del directorio una respuesta a una
	 * solicitud enviada. Cada vez que expira el timeout sin recibir respuesta se
	 * cuenta como un intento.
	 */
	private static final int MAX_NUMBER_OF_ATTEMPTS = 5;

	/**
	 * Valor inválido de la clave de sesión, antes de ser obtenida del directorio al
	 * loguearse
	 */
	public static final int INVALID_SESSION_KEY = -1;

	/**
	 * Socket UDP usado para la comunicación con el directorio
	 */
	private DatagramSocket socket;
	/**
	 * Dirección de socket del directorio (IP:puertoUDP)
	 */
	private static final String DELIMITER = ",";
	private InetSocketAddress directoryAddress;

	private int sessionKey = INVALID_SESSION_KEY;

	public DirectoryConnector(String address) throws IOException {
		/*
		 * Convertir el nombre de host 'address' a InetAddress y guardar la dirección de
		 * socket (address:DIRECTORY_PORT) del directorio en el atributo
		 * directoryAddress, para poder enviar datagramas a dicho destino.
		 */
		InetAddress serverIp = InetAddress.getByName(address);
		this.directoryAddress = new InetSocketAddress(serverIp, DIRECTORY_PORT);
		/*
		 * Crea el socket UDP en cualquier puerto para enviar datagramas al directorio
		 */
		this.socket = new DatagramSocket();

	}

	private byte[] receiveDatagrams() {
		byte responseData[] = new byte[DirMessage.PACKET_MAX_SIZE];
		byte response[] = null;
		/******** RECEIVE FROM SERVER **********/
		// Creamos un datagrama asociado al búfer de recepción
		DatagramPacket packetFromServer = new DatagramPacket(responseData, responseData.length);
		int attempts = 0;
		boolean received = false;
		while (attempts < MAX_NUMBER_OF_ATTEMPTS && received == false) {
			try {
				// Tratamos de recibir la respuesta
				socket.receive(packetFromServer);
				received = true;
			} catch (IOException e) {
				attempts++;
				if (attempts == MAX_NUMBER_OF_ATTEMPTS) {
					System.err.println("Error al recibir la respuesta del servidor.");
					e.printStackTrace();

				}
			}
		}
		// asignamos a response la longitud rellenada
		String stringFromServer = new String(responseData, 0, packetFromServer.getLength());
		response = stringFromServer.getBytes();
		return response;
	}

	/**
	 * Método para enviar y recibir datagramas al/del directorio
	 * 
	 * @param requestData los datos a enviar al directorio (mensaje de solicitud)
	 * @return los datos recibidos del directorio (mensaje de respuesta)
	 */
	private byte[] sendAndReceiveDatagrams(byte[] requestData) {
		byte responseData[] = new byte[DirMessage.PACKET_MAX_SIZE];
		byte response[] = null;
		if (directoryAddress == null) {
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP server destination address is null!");
			System.err.println(
					"DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"directoryAddress\"");
			System.exit(-1);

		}
		if (socket == null) {
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP socket is null!");
			System.err.println(
					"DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"socket\"");
			System.exit(-1);
		}
		/*
		 * Enviar datos en un datagrama al directorio y recibir una respuesta. El array
		 * devuelto debe contener únicamente los datos recibidos, *NO* el búfer de
		 * recepción al completo.
		 */
		// creamos el paquete a enviar
		DatagramPacket packetToServer = new DatagramPacket(requestData, requestData.length, this.directoryAddress);
		// enviamos al servidor

		/******** RECEIVE FROM SERVER **********/
		// Creamos un datagrama asociado al búfer de recepción
		DatagramPacket packetFromServer = new DatagramPacket(responseData, responseData.length);
		int attempts = 0;
		boolean received = false;
		while (attempts < MAX_NUMBER_OF_ATTEMPTS && received == false) {
			try {
				socket.send(packetToServer);
				// Establecemos un temporizador de 1 segundo para evitar que receive se
				// bloquee indefinidamente (en caso de que el servidor no responda)
				socket.setSoTimeout(TIMEOUT);
				// Tratamos de recibir la respuesta
				socket.receive(packetFromServer);
				received = true;
			} catch (IOException e) {
				attempts++;
				if (attempts == MAX_NUMBER_OF_ATTEMPTS) {
					System.err.println("Error al recibir la respuesta del servidor.");
					e.printStackTrace();

				}
			}
		}
		// asignamos a response la longitud rellenada
		String stringFromServer = new String(responseData, 0, packetFromServer.getLength());
		response = stringFromServer.getBytes();
		/*
		 * /* Una vez el envío y recepción asumiendo un canal confiable (sin pérdidas)
		 * esté terminado y probado, debe implementarse un mecanismo de retransmisión
		 * usando temporizador, en caso de que no se reciba respuesta en el plazo de
		 * TIMEOUT. En caso de salte el timeout, se debe reintentar como máximo en
		 * MAX_NUMBER_OF_ATTEMPTS ocasiones.
		 */
		/*
		 * Las excepciones que puedan lanzarse al leer/escribir en el socket deben ser
		 * capturadas y tratadas en este método. Si se produce una excepción de
		 * entrada/salida (error del que no es posible recuperarse), se debe informar y
		 * terminar el programa.
		 */
		/*
		 * NOTA: Las excepciones deben tratarse de la más concreta a la más genérica.
		 * SocketTimeoutException es más concreta que IOException.
		 */

		if (response != null && response.length == responseData.length) {
			System.err.println("Your response is as large as the datagram reception buffer!!\n"
					+ "You must extract from the buffer only the bytes that belong to the datagram!");
		}
		return response;
	}

	/**
	 * Método para probar la comunicación con el directorio mediante el envío y
	 * recepción de mensajes sin formatear ("en crudo")
	 * 
	 * @return verdadero si se ha enviado un datagrama y recibido una respuesta
	 */
	public boolean testSendAndReceive() {
		/*
		 * Probar el correcto funcionamiento de sendAndReceiveDatagrams. Se debe enviar
		 * un datagrama con la cadena "login" y comprobar que la respuesta recibida es
		 * "loginok". En tal caso, devuelve verdadero, falso si la respuesta no contiene
		 * los datos esperados.
		 */
		byte[] response = sendAndReceiveDatagrams("login".getBytes());
		boolean success = false;
		byte[] responseRequired = "loginok".getBytes();
		if (Arrays.equals(response, responseRequired)) {
			success = true;
		}
		return success;
	}

	public InetSocketAddress getDirectoryAddress() {
		return directoryAddress;
	}

	public int getSessionKey() {
		return sessionKey;
	}

	/**
	 * Método para "iniciar sesión" en el directorio, comprobar que está operativo y
	 * obtener la clave de sesión asociada a este usuario.
	 * 
	 * @param nickname El nickname del usuario a registrar
	 * @return La clave de sesión asignada al usuario que acaba de loguearse, o -1
	 *         en caso de error
	 */
	public boolean logIntoDirectory(String nickname) {
		assert (sessionKey == INVALID_SESSION_KEY);
		boolean success = false;
		DirMessage messageToServer = new DirMessage(DirMessageOps.OPERATION_LOGIN, nickname);
		byte[] dataToServer = new byte[DirMessage.PACKET_MAX_SIZE];
		dataToServer = messageToServer.toString().getBytes();
		byte[] dataFromServer = sendAndReceiveDatagrams(dataToServer);
		String messageFromServer = new String(dataFromServer, 0, dataFromServer.length);
		DirMessage responseFromServer = DirMessage.fromString(messageFromServer);
		if (responseFromServer.getOperation().equals(DirMessageOps.CODE_LOGINOK)) {
			int key = responseFromServer.getSession_key();
			sessionKey = key;
			success = true;
		}

		return success;
	}

	/**
	 * Método para obtener la lista de "nicknames" registrados en el directorio.
	 * Opcionalmente, la respuesta puede indicar para cada nickname si dicho peer
	 * está sirviendo ficheros en este instante.
	 * 
	 * @return La lista de nombres de usuario registrados, o null si el directorio
	 *         no pudo satisfacer nuestra solicitud
	 */
	public String[] getUserList() {
		String[] userlist = null;
		DirMessage messageToServer = new DirMessage(DirMessageOps.OPERATION_USERLIST);
		byte[] dataToServer = new byte[DirMessage.PACKET_MAX_SIZE];
		dataToServer = messageToServer.toString().getBytes();
		byte[] dataFromServer = sendAndReceiveDatagrams(dataToServer);
		String messageFromServer = new String(dataFromServer, 0, dataFromServer.length);
		DirMessage responseFromServer = DirMessage.fromString(messageFromServer);
		if (responseFromServer.getOperation().equals(DirMessageOps.OPERATION_USERLIST)) {
			userlist = responseFromServer.getNickname().split(DELIMITER);
		}

		return userlist;
	}

	/**
	 * Método para "cerrar sesión" en el directorio
	 * 
	 * @return Verdadero si el directorio eliminó a este usuario exitosamente
	 */
	public boolean logoutFromDirectory() {
		boolean success = false;
		DirMessage messageToServer = new DirMessage(DirMessageOps.OPERATION_LOGOUT, this.sessionKey);
		byte[] dataToServer = new byte[DirMessage.PACKET_MAX_SIZE];
		dataToServer = messageToServer.toString().getBytes();
		byte[] dataFromServer = sendAndReceiveDatagrams(dataToServer);
		String messageFromServer = new String(dataFromServer, 0, dataFromServer.length);
		DirMessage responseFromServer = DirMessage.fromString(messageFromServer);
		if (responseFromServer.getOperation().equals(DirMessageOps.CODE_LOGOUTOK)) {
			success = true;
		} else {
			System.err.println("Logout fallido.");
		}

		return success;
	}

	/**
	 * Método para dar de alta como servidor de ficheros en el puerto indicado a
	 * este peer.
	 * 
	 * @param serverPort El puerto TCP en el que este peer sirve ficheros a otros
	 * @return Verdadero si el directorio acepta que este peer se convierta en
	 *         servidor.
	 */
	public boolean registerServerPort(String hostname) {
		boolean success = false;
		DirMessage messageToServer = new DirMessage(DirMessageOps.OPERATION_REGISTER_FILESERVER, this.sessionKey,
				hostname);
		byte[] dataToServer = new byte[DirMessage.PACKET_MAX_SIZE];
		dataToServer = messageToServer.toString().getBytes();
		byte[] dataFromServer = sendAndReceiveDatagrams(dataToServer);
		String messageFromServer = new String(dataFromServer, 0, dataFromServer.length);
		DirMessage responseFromServer = DirMessage.fromString(messageFromServer);
		if (responseFromServer.getOperation().equals(DirMessageOps.CODE_REGSERVER_OK)) {
			success = true;
		}
		return success;
	}

	public boolean unregisterServerPort() {
		boolean success = false;
		DirMessage messageToServer = new DirMessage(DirMessageOps.OPERATION_UNREGISTER_FILESERVER, this.sessionKey);
		byte[] dataToServer = new byte[DirMessage.PACKET_MAX_SIZE];
		dataToServer = messageToServer.toString().getBytes();
		byte[] dataFromServer = sendAndReceiveDatagrams(dataToServer);
		String messageFromServer = new String(dataFromServer, 0, dataFromServer.length);
		DirMessage responseFromServer = DirMessage.fromString(messageFromServer);
		if (responseFromServer.getOperation().equals(DirMessageOps.CODE_UNREGSERVER_OK)) {
			success = true;
		}
		return success;
	}

	/**
	 * Método para obtener del directorio la dirección de socket (IP:puerto)
	 * asociada a un determinado nickname.
	 * 
	 * @param nick El nickname del servidor de ficheros por el que se pregunta
	 * @return La dirección de socket del servidor en caso de que haya algún
	 *         servidor dado de alta en el directorio con ese nick, o null en caso
	 *         contrario.
	 */
	public InetSocketAddress lookupServerAddrByUsername(String nick) {
		InetSocketAddress serverAddr = null;
		DirMessage messageToServer = new DirMessage(DirMessageOps.OPERATION_GETIP, nick);
		byte[] dataToServer = new byte[DirMessage.PACKET_MAX_SIZE];
		dataToServer = messageToServer.toString().getBytes();
		byte[] dataFromServer = sendAndReceiveDatagrams(dataToServer);
		String messageFromServer = new String(dataFromServer, 0, dataFromServer.length);
		DirMessage responseFromServer = DirMessage.fromString(messageFromServer);
		if (responseFromServer.getOperation().equals(DirMessageOps.OPERATION_SERVEIP)) {
			String[] ipHost = responseFromServer.getHostname().split(DELIMITER);
			serverAddr = new InetSocketAddress(ipHost[0], Integer.parseInt(ipHost[1]));

		}
		return serverAddr;
	}

	/**
	 * Método para publicar ficheros que este peer servidor de ficheros están
	 * compartiendo.
	 * 
	 * @param files La lista de ficheros que este peer está sirviendo.
	 * @return Verdadero si el directorio tiene registrado a este peer como servidor
	 *         y acepta la lista de ficheros, falso en caso contrario.
	 */
	public boolean publishLocalFiles(FileInfo[] files) {
		// primero manda un mensaje publish si el servidor responde con publishok
		// mandamos un mensaje por cada fichero con su informacioh
		// cuando termina mandamos otro mensaje para decir que hemos terminado
		boolean success = false;
		String fileInfo = "";
		DirMessage messageToServer = new DirMessage(DirMessageOps.OPERATION_PUBLISH, this.sessionKey);
		byte[] dataToServer = new byte[DirMessage.PACKET_MAX_SIZE];
		dataToServer = messageToServer.toString().getBytes();
		byte[] dataFromServer = sendAndReceiveDatagrams(dataToServer);
		String messageFromServer = new String(dataFromServer, 0, dataFromServer.length);
		DirMessage responseFromServer = DirMessage.fromString(messageFromServer);
		if (responseFromServer.getOperation().equals(DirMessageOps.CODE_PUBLISHOK)) {
			// permiso para mandar los ficheros
			for (FileInfo file : files) {
				fileInfo = file.fileName + DELIMITER + file.fileSize + DELIMITER + file.fileHash;
				DirMessage fileToServer = new DirMessage(DirMessageOps.OPERATION_FILEINFO, this.sessionKey);
				fileToServer.setFileInfo(fileInfo);
				dataToServer = fileToServer.toString().getBytes();

				byte[] confirmFromServer = sendAndReceiveDatagrams(dataToServer);
				String strFromServer = new String(confirmFromServer, 0, confirmFromServer.length);
				responseFromServer = DirMessage.fromString(strFromServer);
				if (!responseFromServer.getOperation().equals(DirMessageOps.CODE_FILEINFOOK)) {
					// si el directorio responde con otra cosa que no sea la confirmacion
					// abortamos la ejecucion
					System.err.println("Problemas enviando informacion de ficheros al directorio");
					System.exit(1);
				}
			}
			messageToServer = new DirMessage(DirMessageOps.OPERATION_PUBLISH_END);
			dataToServer = messageToServer.toString().getBytes();
			dataFromServer = sendAndReceiveDatagrams(dataToServer);
			String confirmFromServer = new String(dataFromServer, 0, dataFromServer.length);
			responseFromServer = DirMessage.fromString(confirmFromServer);
			if (responseFromServer.getOperation().equals(DirMessageOps.CODE_PUBLISHENDOK)) {
				success = true;
			}
		}


		return success;
	}

	/**
	 * Método para obtener la lista de ficheros que los peers servidores han
	 * publicado al directorio. Para cada fichero se debe obtener un objeto FileInfo
	 * con nombre, tamaño y hash. Opcionalmente, puede incluirse para cada fichero,
	 * su lista de peers servidores que lo están compartiendo.
	 * 
	 * @return Los ficheros publicados al directorio, o null si el directorio no
	 *         pudo satisfacer nuestra solicitud
	 */
	public FileInfo[] getFileList() {
		FileInfo[] filelist = null;
		LinkedList<FileInfo> files = new LinkedList<>();
		DirMessage messageToServer = new DirMessage(DirMessageOps.OPERATION_GET_FILELIST);
		byte[] dataToServer = new byte[DirMessage.PACKET_MAX_SIZE];
		dataToServer = messageToServer.toString().getBytes();
		byte[] dataFromServer = sendAndReceiveDatagrams(dataToServer);
		String messageFromServer = new String(dataFromServer, 0, dataFromServer.length);
		DirMessage responseFromServer = DirMessage.fromString(messageFromServer);
		while (responseFromServer.getOperation().equals(DirMessageOps.OPERATION_FILEINFO)) {
			files.add(FileInfo.fromString(responseFromServer.getFileInfo()));
			// añadimos el fichero a lista y pedimos otro
			dataFromServer = receiveDatagrams();
			messageFromServer = new String(dataFromServer, 0, dataFromServer.length);
			responseFromServer = DirMessage.fromString(messageFromServer);
		}
		if (responseFromServer.getOperation().equals(DirMessageOps.CODE_FILELISTOK)) {
			filelist = new FileInfo[files.size()];
			files.toArray(filelist);
		}
		return filelist;
	}

	/**
	 * Método para obtener la lista de nicknames de los peers servidores que tienen
	 * un fichero identificado por su hash. Opcionalmente, puede aceptar también
	 * buscar por una subcadena del hash, en vez de por el hash completo.
	 * 
	 * @return La lista de nicknames de los servidores que han publicado al
	 *         directorio el fichero indicado. Si no hay ningún servidor, devuelve
	 *         una lista vacía.
	 */
	public String[] getServerNicknamesSharingThisFile(String fileHash) {
		String[] nicklist = null;
		DirMessage messageToServer = new DirMessage(DirMessageOps.OPERATION_GET_NICKLIST);
		messageToServer.setFileInfo(fileHash);
		byte[] dataToServer = new byte[DirMessage.PACKET_MAX_SIZE];
		dataToServer = messageToServer.toString().getBytes();
		byte[] dataFromServer = sendAndReceiveDatagrams(dataToServer);
		String messageFromServer = new String(dataFromServer, 0, dataFromServer.length);
		DirMessage responseFromServer = DirMessage.fromString(messageFromServer);
		if (responseFromServer.getOperation().equals(DirMessageOps.OPERATION_SERVE_NICKLIST)) {
			nicklist = responseFromServer.getFileInfo().split(DELIMITER);
		} else {
			System.err.println("Hubo algun problema identificando el fichero con su hash.");
		}

		return nicklist;
	}

}

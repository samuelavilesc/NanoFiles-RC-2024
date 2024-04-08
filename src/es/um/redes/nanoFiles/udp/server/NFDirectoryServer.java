package es.um.redes.nanoFiles.udp.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFDirectoryServer {
	/**
	 * Número de puerto UDP en el que escucha el directorio
	 */
	public static final int DIRECTORY_PORT = 6868;

	/**
	 * Socket de comunicación UDP con el cliente UDP (DirectoryConnector)
	 */
	private DatagramSocket socket = null;
	/**
	 * Estructura para guardar los nicks de usuarios registrados, y clave de sesión
	 * 
	 */
	private HashMap<String, Integer> nicks;
	/**
	 * Estructura para guardar las claves de sesión y sus nicks de usuario asociados
	 * 
	 */
	private HashMap<Integer, String> sessionKeys;
	/*
	 * Estructuras para guardar las claves de inicio de sesion y hostname son
	 * servidor.
	 */
	private HashMap<Integer, String> sessionHostnames;

	/*
	 * Estructura para guardar las session keys y los ficheros asociados a esa
	 * session key
	 */
	private HashMap<Integer, LinkedList<FileInfo>> sessionFiles;

	/*
	 * 
	 * Lista con todos los ficheros que hay.
	 */
	private LinkedList<FileInfo> files;

	private String addressClient;

	private static final String DELIMITER = ",";
	/**
	 * Generador de claves de sesión aleatorias (sessionKeys)
	 */
	Random random = new Random();
	/**
	 * Probabilidad de descartar un mensaje recibido en el directorio (para simular
	 * enlace no confiable y testear el código de retransmisión)
	 */
	private double messageDiscardProbability;

	public NFDirectoryServer(double corruptionProbability) throws SocketException {
		/*
		 * Guardar la probabilidad de pérdida de datagramas (simular enlace no
		 * confiable)
		 */
		messageDiscardProbability = corruptionProbability;

		this.socket = new DatagramSocket(DIRECTORY_PORT);

		this.nicks = new HashMap<>();
		this.sessionKeys = new HashMap<>();
		this.sessionHostnames = new HashMap<>();
		this.sessionFiles = new HashMap<>();
		this.files = new LinkedList<>();

		if (NanoFiles.testMode) {
			if (socket == null || nicks == null || sessionKeys == null) {
				System.err.println("[testMode] NFDirectoryServer: code not yet fully functional.\n"
						+ "Check that all TODOs in its constructor and 'run' methods have been correctly addressed!");
				System.exit(-1);
			}
		}
	}

	public void run() throws IOException {
		byte[] receptionBuffer = new byte[DirMessage.PACKET_MAX_SIZE];
		InetSocketAddress clientAddr = null;
		int dataLength = -1;
		/*
		 * Crear un búfer para recibir datagramas y un datagrama asociado al búfer
		 */

		// creamos paquete para recibir del cliente
		DatagramPacket packetFromClient = new DatagramPacket(receptionBuffer, receptionBuffer.length);

		System.out.println("Directory starting...");

		while (true) { // Bucle principal del servidor de directorio

			// Recibimos a través del socket un datagrama
			this.socket.receive(packetFromClient);
			// Establecemos dataLength con longitud del datagrama
			// recibido
			dataLength = packetFromClient.getLength();
			// Establecemos 'clientAddr' con la dirección del cliente,
			// obtenida del
			// datagrama recibido
			clientAddr = new InetSocketAddress(packetFromClient.getAddress(), packetFromClient.getPort());
			addressClient = packetFromClient.getAddress().getHostAddress();
			if (NanoFiles.testMode) {
				if (receptionBuffer == null || clientAddr == null || dataLength < 0) {
					System.err.println("NFDirectoryServer.run: code not yet fully functional.\n"
							+ "Check that all TODOs have been correctly addressed!");
					System.exit(-1);
				}
			}
			System.out.println("Directory received datagram from " + clientAddr + " of size " + dataLength + " bytes");

			// Analizamos la solicitud y la procesamos
			if (dataLength > 0) {
				String messageFromClient = new String(receptionBuffer, 0, packetFromClient.getLength());
				/*
				 * Construir una cadena a partir de los datos recibidos en el buffer de
				 * recepción
				 */

				if (NanoFiles.testMode) { // En modo de prueba (mensajes en "crudo", boletín UDP)
					System.out.println("[testMode] Contents interpreted as " + dataLength + "-byte String: \""
							+ messageFromClient + "\"");
					// comprobando que la palabra introducida es login
					if (messageFromClient.equals("login")) {
						byte[] responseBufferToClient = "loginok".getBytes();
						DatagramPacket packetToClient = new DatagramPacket(responseBufferToClient,
								responseBufferToClient.length, clientAddr);
						socket.send(packetToClient);
					} else {
						System.err.println("Directory received a wrong datagram, it should contain 'login'");
						System.exit(1);
					}

					/*
					 * Comprobar que se ha recibido un datagrama con la cadena "login" y en ese caso
					 * enviar como respuesta un mensaje al cliente con la cadena "loginok". Si el
					 * mensaje recibido no es "login", se informa del error y no se envía ninguna
					 * respuesta.
					 */

				} else { // Servidor funcionando en modo producción (mensajes bien formados)

					// Vemos si el mensaje debe ser ignorado por la probabilidad de descarte
					double rand = Math.random();
					if (rand < messageDiscardProbability) {
						System.err.println("Directory DISCARDED datagram from " + clientAddr);
						continue;
					}
					System.out.println(messageFromClient);

					DirMessage dirMessageFromClient = DirMessage.fromString(messageFromClient);
					if (dirMessageFromClient.getOperation().equals(DirMessageOps.OPERATION_GET_FILELIST)) {
						DirMessage response = null;
						for (FileInfo file : files) {
							response = new DirMessage(DirMessageOps.OPERATION_FILEINFO);
							response.setFileInfo(file.fileName + DELIMITER + file.fileSize + DELIMITER + file.fileHash
									+ DELIMITER + file.filePath);
							String responseToSend = response.toString();
							DatagramPacket packetToClient = new DatagramPacket(responseToSend.getBytes(),
									responseToSend.getBytes().length, clientAddr);
							try {
								socket.send(packetToClient);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						// por cada fichero envio un paquete al cliente

						// cuando termine envio un ultimo paquete diciendo que he terminado
						response = new DirMessage(DirMessageOps.CODE_FILELISTOK);
						String responseToSend = response.toString();
						DatagramPacket packetToClient = new DatagramPacket(responseToSend.getBytes(),
								responseToSend.getBytes().length, clientAddr);
						try {
							socket.send(packetToClient);
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						DirMessage responseToClient = buildResponseFromRequest(dirMessageFromClient, clientAddr);

						String responseToSend = responseToClient.toString();
						DatagramPacket packetToClient = new DatagramPacket(responseToSend.getBytes(),
								responseToSend.getBytes().length, clientAddr);
						socket.send(packetToClient);

					}
				}

			} else {
				System.err.println("Directory ignores EMPTY datagram from " + clientAddr);
			}

		}
	}

	private DirMessage buildResponseFromRequest(DirMessage msg, InetSocketAddress clientAddr) {

		String operation = msg.getOperation();

		DirMessage response = null;

		switch (operation) {
		case DirMessageOps.OPERATION_LOGIN: {
			String username = msg.getNickname();
			int key = 0;
			// si ya se encuentra en el keySet de nicks devolver loginfailed
			if (this.nicks.keySet().contains(username)) {
				response = new DirMessage(DirMessageOps.CODE_LOGINFAILED);
				System.err.println("Intento de login fallido usuario: " + username);
				break;

			} else {
				key = random.nextInt(10000);
				this.nicks.put(username, key);
				this.sessionKeys.put(key, username);
			}

			response = new DirMessage(DirMessageOps.CODE_LOGINOK, username);
			response.setSession_key(key);
			System.out.println("El usuario " + username + " se ha conectado correctamente.");

			break;
		}
		case DirMessageOps.OPERATION_LOGOUT: {
			String username = null;
			int key = msg.getSession_key();
			// si ya se encuentra en el keySet de nicks devolver loginfailed
			if (this.sessionKeys.containsKey(key)) {
				response = new DirMessage(DirMessageOps.CODE_LOGOUTOK);
				username = this.sessionKeys.get(key);
				this.nicks.remove(username);
				this.sessionKeys.remove(key);
				this.sessionHostnames.remove(key);
				LinkedList<FileInfo> files = this.sessionFiles.get(key);
				if (files != null) {
					for (FileInfo file : files) {
						boolean repeated = false;
						for (LinkedList<FileInfo> checkFiles : this.sessionFiles.values()) {
							if (checkFiles.equals(files)) {
								continue;
							}
							for (FileInfo f : checkFiles) {
								if (f.fileHash.equals(file.fileHash)) {
									repeated = true;
								}
							}

						}
						if (!repeated)
							this.files.remove(file);

					}
					this.sessionFiles.remove(key);
				}
			}
			break;

		}
		case DirMessageOps.OPERATION_USERLIST: {
			String userList = "";
			for (String user : this.nicks.keySet()) {
				int userKey = this.nicks.get(user);
				if (this.sessionHostnames.containsKey(userKey)) {
					user = user + "(server)";
				}
				if (userList.equals("")) {
					userList = user;
				} else {
					userList = userList + DELIMITER + user;
				}
			}

			response = new DirMessage(DirMessageOps.OPERATION_USERLIST, userList);
			break;

		}
		case DirMessageOps.OPERATION_REGISTER_FILESERVER: {
			// no es necesario contemplar el caso de que
			// el cliente que solicita esta operacion no este
			// registrado dado que el automata
			// del cliente protege el envio de este mensaje si no esta
			// iniciado sesion
			String port = msg.getHostname();
			String hostname = addressClient + DELIMITER + port;
			this.sessionHostnames.put(msg.getSession_key(), hostname);
			response = new DirMessage(DirMessageOps.CODE_REGSERVER_OK);
			break;

		}
		case DirMessageOps.OPERATION_UNREGISTER_FILESERVER: {
			// no es necesario contemplar el caso de que
			// el cliente que solicita esta operacion no este
			// registrado como servidor activo dado que el automata
			// del cliente protege el envio de este mensaje si no esta
			// registrado como servidor
			int key = msg.getSession_key();
			this.sessionHostnames.remove(key);
			LinkedList<FileInfo> files = this.sessionFiles.get(key);
			if(files!=null) {
			//solo hace este proceso si el servidor tiene ficheros publicados
			for (FileInfo file : files) {
				boolean repeated = false;
				for (LinkedList<FileInfo> checkFiles : this.sessionFiles.values()) {
					if (checkFiles.equals(files)) {
						//si la lista es la misma que la asignada a esa sesion key continua con 
						//otra iteracion
						continue;
					}
					for (FileInfo f : checkFiles) {
						if (f.fileHash.equals(file.fileHash)) {
							repeated = true;
						}
					}

				}
				if (!repeated)
					this.files.remove(file);
					file.removeNamePath(this.sessionKeys.get(key));

			}
			}
			this.sessionFiles.remove(key);

			response = new DirMessage(DirMessageOps.CODE_UNREGSERVER_OK);
			break;

		}
		case DirMessageOps.OPERATION_GETIP: {
			int key = this.nicks.get(msg.getNickname());
			if (sessionHostnames.containsKey(key)) {
				String hostname = this.sessionHostnames.get(key);
				response = new DirMessage(DirMessageOps.OPERATION_SERVEIP);

				response.setHostname(hostname);
			} else {
				response = new DirMessage(DirMessageOps.CODE_GETIPFAILED);
			}
			break;

		}
		case DirMessageOps.OPERATION_PUBLISH: {
			int key = msg.getSession_key();
			if (sessionHostnames.containsKey(key)) {
				response = new DirMessage(DirMessageOps.CODE_PUBLISHOK);
			} else {
				response = new DirMessage(DirMessageOps.CODE_PUBLISH_FAILED);
			}
			break;

		}
		case DirMessageOps.OPERATION_FILEINFO: {
			int key = msg.getSession_key();
			String username = this.sessionKeys.get(key);
			FileInfo file = FileInfo.fromString(msg.getFileInfo());
			file.filePath = username;
			this.sessionFiles.computeIfAbsent(key, k -> new LinkedList<>()).add(file);
			Iterator<FileInfo> it = files.iterator();
			while (it.hasNext()) {
				FileInfo f = it.next();
				if (f.fileHash.equals(file.fileHash)) {
					file.filePath = f.filePath + DELIMITER + username;
					it.remove();
				}
			}
			files.add(file);
			response = new DirMessage(DirMessageOps.CODE_FILEINFOOK);

			break;

		}
		case DirMessageOps.OPERATION_PUBLISH_END: {
			response = new DirMessage(DirMessageOps.CODE_PUBLISHENDOK);
			break;
		}
		case DirMessageOps.OPERATION_GET_NICKLIST: {
			FileInfo[] filesToSearch = new FileInfo[files.size()];
			files.toArray(filesToSearch);
			FileInfo[] coincidencia = FileInfo.lookupHashSubstring(filesToSearch, msg.getFileInfo());
			if (coincidencia.length > 1 || coincidencia.length == 0) {
				// hash ambiguo hay que decirlo al cliente
				response = new DirMessage(DirMessageOps.CODE_HASH_NOT_FOUND);
				break;
			}
			response = new DirMessage(DirMessageOps.OPERATION_SERVE_NICKLIST);
			response.setFileInfo(coincidencia[0].filePath);
			break;

		}
		default:
			System.out.println("Unexpected message operation: \"" + operation + "\"");
		}
		return response;

	}
}

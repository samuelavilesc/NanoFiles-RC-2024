package es.um.redes.nanoFiles.logic;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.client.DirectoryConnector;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFControllerLogicDir {
	private final static String DELIMITER=":";
	// Conector para enviar y recibir mensajes del directorio
	private DirectoryConnector directoryConnector;

	/**
	 * Método para comprobar que la comunicación con el directorio es exitosa (se
	 * pueden enviar y recibir datagramas) haciendo uso de la clase
	 * DirectoryConnector
	 * 
	 * @param directoryHostname el nombre de host/IP en el que se está ejecutando el
	 *                          directorio
	 * @return true si se ha conseguido contactar con el directorio.
	 * @throws IOException
	 */
	protected void testCommunicationWithDirectory(String directoryHostname) throws IOException {
		assert (NanoFiles.testMode);
		System.out.println("[testMode] Testing communication with directory...");
		/*
		 * Crea un objeto DirectoryConnector a partir del parámetro directoryHostname y
		 * lo utiliza para hacer una prueba de comunicación con el directorio.
		 */
		DirectoryConnector directoryConnector = new DirectoryConnector(directoryHostname);
		if (directoryConnector.testSendAndReceive()) {
			System.out.println("[testMode] Test PASSED!");
		} else {
			System.err.println("[testMode] Test FAILED!");
		}
	}

	/**
	 * Método para conectar con el directorio y obtener la "sessionKey" que se
	 * deberá utilizar en lo sucesivo para identificar a este cliente ante el
	 * directorio
	 * 
	 * @param directoryHostname el nombre de host/IP en el que se está ejecutando el
	 *                          directorio
	 * @return true si se ha conseguido contactar con el directorio.
	 * @throws IOException
	 */
	protected boolean doLogin(String directoryHostname, String nickname) {
		try {
			this.directoryConnector = new DirectoryConnector(directoryHostname);
		} catch (IOException e) {
			System.err.println("Fallo al establecer comunicación con el servidor.");
			e.printStackTrace();
		}
		/*
		 * lo hace en logIntoDirectory Debe crear un objeto DirectoryConnector a partir
		 * del parámetro directoryHostname y guardarlo en el atributo correspondiente
		 * para que pueda ser utilizado por el resto de métodos de esta clase. A
		 * continuación, utilizarlo para comunicarse con el directorio y tratar de
		 * realizar el "login", informar por pantalla del éxito/fracaso e imprimir la
		 * clave de sesión asignada por el directorio. Devolver éxito/fracaso de la
		 * operación.
		 */

		boolean result = this.directoryConnector.logIntoDirectory(nickname);
		if (result == true) {
			System.out.println("Bienvenido, " + nickname);
			System.out.println("Id de sesión: " + this.directoryConnector.getSessionKey());
		} else {
			this.directoryConnector=null;
			System.err.println("Inicio de sesión fallido.");
		}
		return result;
	}

	/**
	 * Método para desconectarse del directorio: cerrar sesión y dar de baja el
	 * nombre de usuario registrado
	 */
	public boolean doLogout() {

		boolean result = false;
		if (this.directoryConnector != null) {
			result = this.directoryConnector.logoutFromDirectory();
			this.directoryConnector=null;
			System.out.println("Sesión cerrada correctamente.");
		} else {
			System.err.println("No puedes cerrar sesión antes de abrirla.");
		}

		return result;
	}

	/**
	 * Método para obtener y mostrar la lista de nicks registrados en el directorio
	 */
	protected boolean getAndPrintUserList() {

		boolean result = false;
		if(this.directoryConnector!=null) {
			String[] userList = this.directoryConnector.getUserList();
			if(userList.length!=0) {
				result=true;
				System.out.println("Lista de usuarios activos: ");
				for(String user: userList) {
					System.out.println(user);
				}
			}else {
				System.err.println("No existen usuarios activos en este momento.");
			}
			
		} else {
			System.err.println("No puedes consultar la lista de usuarios sin iniciar sesion.");
		}

			
		return result;
	}

	/**
	 * Método para obtener y mostrar la lista de ficheros que los peer servidores
	 * han publicado al directorio
	 */
	protected boolean getAndPrintFileList() {

		boolean result = false;
		FileInfo[] files = this.directoryConnector.getFileList();
		if(files.length!=0) {
			FileInfo.printToSysoutWithPath(files);
			result=true;
		}
		
		return result;
	}

	/**
	 * Método para registrarse en el directorio como servidor de ficheros en un
	 * puerto determinado
	 * 
	 * @param serverPort el puerto en el que está escuchando nuestro servidor de
	 *                   ficheros
	 */

	public boolean registerFileServer(String serverHostname) {

		boolean result = false;
		this.directoryConnector.registerServerPort(serverHostname);
		return result;
	}

	/**
	 * Método para enviar al directorio la lista de ficheros que este peer servidor
	 * comparte con el resto (ver método filelist).
	 * 
	 */
	protected boolean publishLocalFiles() {

		boolean result = false;
		result=this.directoryConnector.publishLocalFiles(NanoFiles.db.getFiles());
		if(result==true) {
			System.out.println("Ficheros publicados con éxito.");
		}else {
			System.err.println("Hubo algun problema publicando los ficheros.");
		}
		return result;
	}
	
	/**
	 * Método para consultar al directorio el nick de un peer servidor y obtener
	 * como respuesta la dirección de socket IP:puerto asociada a dicho servidor
	 * 
	 * @param nickname el nick del servidor por cuya IP:puerto se pregunta
	 * @return La dirección de socket del servidor identificado por dich nick, o
	 *         null si no se encuentra ningún usuario con ese nick que esté
	 *         sirviendo ficheros.
	 */
	private InetSocketAddress lookupServerAddrByUsername(String nickname) {

		InetSocketAddress serverAddr = null;
		serverAddr = this.directoryConnector.lookupServerAddrByUsername(nickname);

		return serverAddr;
	}

	/**
	 * Método para obtener la dirección de socket asociada a un servidor a partir de
	 * una cadena de caracteres que contenga: i) el nick del servidor, o ii)
	 * directamente una IP:puerto.
	 * 
	 * @param serverNicknameOrSocketAddr El nick o IP:puerto del servidor por el que
	 *                                   preguntamos
	 * @return La dirección de socket del peer identificado por dicho nick, o null
	 *         si no se encuentra ningún peer con ese nick.
	 */
	public InetSocketAddress getServerAddress(String serverNicknameOrSocketAddr) {
		InetSocketAddress fserverAddr = null;
		/*
		 *  Averiguar si el nickname es en realidad una cadena "IP:puerto", en cuyo
		 * caso no es necesario comunicarse con el directorio (simplemente se devuelve
		 * un InetSocketAddress); en otro caso, utilizar el método
		 * lookupServerAddrByUsername de esta clase para comunicarse con el directorio y
		 * obtener la IP:puerto del servidor con dicho nickname. Devolver null si la
		 * operación fracasa.
		 */

		if (serverNicknameOrSocketAddr.contains(":")) { // Then it has to be a socket address (IP:port)
			String[] str = serverNicknameOrSocketAddr.split(DELIMITER);
			fserverAddr = new InetSocketAddress(str[0], Integer.parseInt(str[1]));
			/* 
			 * : Extraer la dirección IP y el puerto de la cadena y devolver un
			 * InetSocketAddress. Para convertir un string con la IP a un objeto InetAddress
			 * se debe usar InetAddress.getByName()
			 */

		} else {
			/*
			 * : Si es un nickname, preguntar al directorio la IP:puerto asociada a
			 * dicho peer servidor.
			 */
			fserverAddr = lookupServerAddrByUsername(serverNicknameOrSocketAddr);
		}
		return fserverAddr;
	}

	/**
	 * Método para consultar al directorio los nicknames de los servidores que
	 * tienen un determinado fichero identificado por su hash.
	 * 
	 * @param fileHashSubstring una subcadena del hash del fichero por el que se
	 *                          pregunta
	 */
	public boolean getAndPrintServersNicknamesSharingThisFile(String fileHashSubstring) {

		boolean result = false;
		String[] nicks =this.directoryConnector.getServerNicknamesSharingThisFile(fileHashSubstring);
		if(nicks!=null) {
			System.out.println("Servidores encontrados que tienen disponible el fichero:");
			for(String nick : nicks) {
				System.out.println(nick);
			}
		}
		return result;
	}

	/**
	 * Método para consultar al directorio las direcciones de socket de los
	 * servidores que tienen un determinado fichero identificado por su hash.
	 * 
	 * @param fileHashSubstring una subcadena del hash del fichero por el que se
	 *                          pregunta
	 * @return Una lista de direcciones de socket de los servidores que comparten
	 *         dicho fichero, o null si dicha subcadena del hash no identifica
	 *         ningún fichero concreto (no existe o es una subcadena ambigua)
	 * 
	 */
	public LinkedList<InetSocketAddress> getServerAddressesSharingThisFile(String downloadTargetFileHash) {
		LinkedList<InetSocketAddress> serverAddressList = null;


		return serverAddressList;
	}

	/**
	 * Método para dar de baja a nuestro servidor de ficheros en el directorio.
	 * 
	 * @return Éxito o fracaso de la operación
	 */
	public boolean unregisterFileServer() {

		boolean result = false;
		this.directoryConnector.unregisterServerPort();

		return result;
	}

	protected InetSocketAddress getDirectoryAddress() {
		return directoryConnector.getDirectoryAddress();
	}

}

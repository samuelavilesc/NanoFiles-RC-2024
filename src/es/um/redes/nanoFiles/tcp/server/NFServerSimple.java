package es.um.redes.nanoFiles.tcp.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class NFServerSimple {

	private static final int SERVERSOCKET_ACCEPT_TIMEOUT_MILISECS = 1000;
	private static final String STOP_SERVER_COMMAND = "fgstop";
	private static final int PORT = 10000;
	private ServerSocket serverSocket = null;
	private int port;
	public NFServerSimple() {
		/*
		 *  Crear una direción de socket a partir del puerto especificado
		 */
		//probar a crear la conexión si lo consigue usara el puerto por defecto
		//si no va a probar con el siguiente numero de puerto
		port=PORT;
		boolean on = false;
		while(!on) {
			InetSocketAddress dir = new InetSocketAddress(port);
		try {
			this.serverSocket = new ServerSocket();
			this.serverSocket.bind(dir);
			on=true;
		} catch (IOException e) {
			System.err.println("Puerto de escucha del servidor 	ya en uso, probando con otro puerto.");
			port+=1; //sumo 1 al puerto y vuelvo a probar
		}
		}
		
		/*
		 * Crear un socket servidor y ligarlo a la dirección de socket anterior
		 */



	}

	/**
	 * Método para ejecutar el servidor de ficheros en primer plano. Sólo es capaz
	 * de atender una conexión de un cliente. Una vez se lanza, ya no es posible
	 * interactuar con la aplicación a menos que se implemente la funcionalidad de
	 * detectar el comando STOP_SERVER_COMMAND (opcional)
	 * 
	 */
	public void run() {
		while(true) {
		/*
		 * TODO: Comprobar que el socket servidor está creado y ligado
		 */
		if(serverSocket!=null&&!serverSocket.isClosed()) {
			try {
				Socket conn = this.serverSocket.accept();
				NFServerComm.serveFilesToClient(conn);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		/*
		 *  Usar el socket servidor para esperar conexiones de otros peers que
		 * soliciten descargar ficheros
		 */
		/*
		 *  Al establecerse la conexión con un peer, la comunicación con dicho
		 * cliente se hace en el método NFServerComm.serveFilesToClient(socket), al cual
		 * hay que pasarle el socket devuelto por accept
		 */
		}


		//System.out.println("NFServerSimple stopped. Returning to the nanoFiles shell...");
	}

	public InetAddress getHostname() {
		return this.serverSocket.getInetAddress();
	}
	public int getPort() {
		return port;
	}
}

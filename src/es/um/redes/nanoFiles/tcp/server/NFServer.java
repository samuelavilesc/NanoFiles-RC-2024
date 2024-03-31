package es.um.redes.nanoFiles.tcp.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor que se ejecuta en un hilo propio. Creará objetos
 * {@link NFServerThread} cada vez que se conecte un cliente.
 */
public class NFServer extends Thread {

	private ServerSocket serverSocket = null;
	private boolean stopServer = false;

	public NFServer() throws IOException {
		/*
		 * Crear un socket servidor y ligarlo a cualquier puerto disponible
		 */
		this.serverSocket = new ServerSocket(0); // 0 para cualqueir puerto disponible
		//this.serverSocket.setSoTimeout(SERVERSOCKET_ACCEPT_TIMEOUT_MILISECS);

	}
	public int getServerPort() {
		return this.serverSocket.getLocalPort();
	}
	public InetAddress getServerAddress() {
		return this.serverSocket.getInetAddress();
	}
	public void stopServer() {
		this.stopServer=true;
	}

	/**
	 * Método que crea un socket servidor y ejecuta el hilo principal del servidor,
	 * esperando conexiones de clientes.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		while (!stopServer) {

			if (serverSocket != null && !serverSocket.isClosed()) {
				try {
					Socket conn = this.serverSocket.accept();
					//con esta conexion hay que crear un hilo nuevo
					NFServerThread thread = new NFServerThread(conn);
					thread.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}


	}
}

package es.um.redes.boletinTCP;

import java.io.*;
import java.net.*;

public class TCPServer {

	private final static int PORT = 6969;

	public static void main(String[] args) {
		// Get the address of the server socket
		InetSocketAddress serverSocketAddress = new InetSocketAddress(PORT);

		// Server state (data structures shared by threads)
		TCPServerState state = new TCPServerState();
		
		// Start server
		try (ServerSocket serverSocket = new ServerSocket()) {
			// After creating the server socket, bind to the listening port
			serverSocket.bind(serverSocketAddress);
			System.out.println("\nServer is listening on port " + PORT);
			while (true) {
				// Waiting for new connection requests
				Socket socket = serverSocket.accept();
				// Created the socket for the connection with the client
				System.out.println(
						"New client connected: " + socket.getInetAddress().toString() + ":" + socket.getPort());
				// Inicia el hilo de servicio al cliente recién conectado,
				// enviándole el socket de este cliente
				TCPServerThread st = new TCPServerThread(socket, state);
				st.start();
			}
		} catch (IOException ex) {
			System.out.println("Server exception: " + ex.getMessage());
			ex.printStackTrace();
		}

	}
}
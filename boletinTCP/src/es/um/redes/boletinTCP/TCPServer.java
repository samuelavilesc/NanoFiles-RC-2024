package es.um.redes.boletinTCP;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.Scanner;

public class TCPServer {

	private final static int PORT = 6969;

	public static void main(String[] args) {
		// Get the "server identifier"
		System.out.print("Enter identifier: ");
		Scanner reader = new Scanner(System.in);
		String serverIdentifier = reader.nextLine();
		reader.close();
		if (serverIdentifier.equals("")) {
			serverIdentifier = "unknown";
		}
		
		// Get the address of the server socket
		InetSocketAddress serverSocketAddress = new InetSocketAddress(PORT);
		
		// Start server
		try (ServerSocket serverSocket = new ServerSocket()) {
			// After creating the server socket, bind to the listening port
			serverSocket.bind(serverSocketAddress);
			System.out.println("\nServer is listening on port " + PORT);
			while (true) {
				// Waiting for new connection requests
				Socket socket = serverSocket.accept();
				// Created the socket for the connection with the client
				System.out.println("\nNew client connected: " +
					socket.getInetAddress().toString() + ":" + socket.getPort());
				// Get the streams of the created socket
				DataInputStream dis = new DataInputStream(socket.getInputStream());
				DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
				// Send data to client
				String dataToClient = "Hello, client. I am " + serverIdentifier + ",\n" +
					"and the time on my watch is... " + new Date().toString();
				dos.writeUTF(dataToClient);
				// Wait response from client
				String dataFromClient = dis.readUTF();
				System.out.println("Response from client...\n" + dataFromClient);
			}
		} catch (IOException ex) {
			System.out.println("Server exception: " + ex.getMessage());
			ex.printStackTrace();
		}
		
	}
}
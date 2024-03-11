package es.um.redes.boletinTCP;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TCPClient {

	private final static int PORT = 6969;

	public static void main(String[] args) throws IOException {
		String serverName;
		// Get server name via command-line argument (optional)
		if (args.length == 1) {
			serverName = args[0];
		} else {
			System.err.print("You did not specify the server name nor IP. ");
			System.err.println("Assuming server and client are both running on this host");
			serverName = "localhost";
		}
		System.out.println("Attempting to reach server located in " + serverName);
		
		try {
			// Try connection
			Socket socket = new Socket(serverName, PORT);
			// Connection ok. Get the streams of the new socket
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			// Wait message from server
			System.out.println("\nConnection OK. Message from server...");
			String dataFromServer = dis.readUTF();
			System.out.println(dataFromServer);
			// Send response
			System.out.print("\nEnter response: ");
			Scanner reader = new Scanner(System.in);
			String dataToServer = reader.nextLine();
			reader.close();
			if (dataToServer.equals("")) {
				dataToServer = "Hello, server";
			}
			dos.writeUTF(dataToServer);
			socket.close();
			System.out.println("(client finished)");
		} catch (UnknownHostException ex) {

			System.out.println("Server not found: " + ex.getMessage());

		} catch (IOException ex) {

			System.out.println("I/O error: " + ex.getMessage());
		}
	}
}
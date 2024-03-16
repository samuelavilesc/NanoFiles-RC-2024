package es.um.redes.boletinTCP;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Date;

public class TCPServerThread extends Thread {

	private Socket socket;
	private TCPServerState serverState;

	public TCPServerThread(Socket _sock, TCPServerState state) {
		socket = _sock;
		serverState = state;
	}

	public void run() {
		boolean terminate = false;
		SocketAddress clientAddr = socket.getRemoteSocketAddress();
		try {
			while (!terminate) {
				DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
				DataInputStream dis = new DataInputStream(socket.getInputStream());

				// Read message from client
				String dataFromClient = dis.readUTF();
				System.out.println("[Message from client] " + dataFromClient);
				
				// Update server *GLOBAL* state (shared by all threads)
				int reqNum = serverState.fetchAndIncrementNumRequests();
				// Print total number of time requests received by all server threads
				System.out.println("This server has received "+reqNum + " requests so far.");

				// Send message to client
				String dataToClient = "Hello, client. Time on my watch is: " + new Date().toString();
				dos.writeUTF(dataToClient);
			}
		} catch (EOFException e) {
			System.out.println("Connection closed by client at addr " + clientAddr);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Cannot communicate with client at addr " + clientAddr);
		}
	}
}

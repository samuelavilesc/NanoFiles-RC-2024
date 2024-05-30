package es.um.redes.nanoFiles.tcp.server;

import java.net.Socket;

public class NFServerThread extends Thread {

	private Socket socket;
	public NFServerThread(Socket socket) {
		this.socket=socket;
	}
	@Override
	public void run() {
		NFServerComm.serveFilesToClient(socket);
	}

}

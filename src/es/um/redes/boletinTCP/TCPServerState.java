package es.um.redes.boletinTCP;

public class TCPServerState {
	private int nextRequestNumber = 0;
	
	public synchronized int fetchAndIncrementNumRequests() {
		return ++nextRequestNumber;
	}
}

package es.um.redes.nanoFiles.tcp.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import es.um.redes.nanoFiles.udp.message.DirMessageOps;

public class PeerMessageTest {

	public static void main(String[] args) throws IOException {
		String nombreArchivo = "peermsg.bin";
		DataOutputStream fos = new DataOutputStream(new FileOutputStream(nombreArchivo));
		/*
		 * TODO: Probar a crear diferentes tipos de mensajes (con los opcodes válidos
		 * definidos en PeerMessageOps), estableciendo los atributos adecuados a cada
		 * tipo de mensaje. Luego, escribir el mensaje a un fichero con
		 * writeMessageToOutputStream para comprobar que readMessageFromInputStream
		 * construye un mensaje idéntico al original.
		 */
		byte b = 1;
		PeerMessage msgOut = new PeerMessage(PeerMessageOps.OPCODE_SEND_FILE_CHUNK,b ,"2".getBytes());
		msgOut.writeMessageToOutputStream(fos);

		DataInputStream fis = new DataInputStream(new FileInputStream(nombreArchivo));
		PeerMessage msgIn = PeerMessage.readMessageFromInputStream((DataInputStream) fis);
		if (msgOut.getOpcode() == msgIn.getOpcode()) {
			System.out.println("El opcode coincide");
		}
		if (Arrays.equals(msgOut.getParam1(), msgIn.getParam1())) {
			System.out.println("El param1 coincide");
		}
		if (Arrays.equals(msgOut.getParam2(), msgIn.getParam2())) {
			System.out.println("El param2 coincide");
		}

		/*
		 * TODO: Comprobar que coinciden los valores de los atributos relevantes al tipo
		 * de mensaje en ambos mensajes (msgOut y msgIn), empezando por el opcode.
		 */
		if (msgOut.getOpcode() != msgIn.getOpcode()) {
			System.err.println("Opcode does not match!");
		}
	}

}

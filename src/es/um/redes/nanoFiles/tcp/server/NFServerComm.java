package es.um.redes.nanoFiles.tcp.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFServerComm {
	private final static byte HASH_MAX_LENGTH = 20;

	public static void serveFilesToClient(Socket socket) {
		try {
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			DataInputStream dis = new DataInputStream(is);
			DataOutputStream dos = new DataOutputStream(os);
				/*
				 * Primero recibimos un mensaje con el hash Despues buscamos el fichero y
				 * enviamos su hash, cuando el cliente nos confirme haber recibido el hash
				 * empezamo a mandarle el fichero ya sea por trozos o completo
				 */
				PeerMessage receiveFromClient = PeerMessage.readMessageFromInputStream(dis);
				if (receiveFromClient.getOpcode() == PeerMessageOps.OPCODE_DOWNLOAD_FILE) {
					byte[] param1Size = receiveFromClient.getParam1();
					String hashSubstr = new String(receiveFromClient.getParam2());
					String filePath = "";
					String completeHash = "";
					if (param1Size[0] == HASH_MAX_LENGTH) {
						filePath = NanoFiles.db.lookupFilePath(hashSubstr);
						completeHash = hashSubstr;
					} else {
						FileInfo[] coincidencia = FileInfo.lookupHashSubstring(NanoFiles.db.getFiles(), hashSubstr);
						if (coincidencia.length > 1 || coincidencia.length == 0) {
							PeerMessage sendError = new PeerMessage(PeerMessageOps.OPCODE_NOT_FOUND);
							sendError.writeMessageToOutputStream(dos);
							return; //finalizo la ejecucion.
						} else {
							filePath = coincidencia[0].filePath;
							completeHash = coincidencia[0].fileHash;
						}
					}

					/* Envio el mensaje */
					PeerMessage sendHashToClient = new PeerMessage(PeerMessageOps.OPCODE_SERVE_HASH,
							(byte) completeHash.getBytes().length, completeHash.getBytes());
					sendHashToClient.writeMessageToOutputStream(dos);
					File f = new File(filePath); // abro el fichero
					DataInputStream disF = new DataInputStream(new FileInputStream(f));
					long filelength = f.length(); // miro su tamaño
					byte data[] = new byte[(int) filelength]; // hago un array con el fichero completo
					disF.readFully(data);
					disF.close();
					// genero mensaje con el fichero
					PeerMessage dataToClient = new PeerMessage(PeerMessageOps.OPCODE_SEND_FILE, filelength, data);
					dataToClient.writeMessageToOutputStream(dos);
					socket.close(); // cierro el socket tras acabar la conexion
				}


		} catch (IOException e) {
			System.err.println("Hubo un error sirviendo ficheros al cliente.");
		}

	}

}

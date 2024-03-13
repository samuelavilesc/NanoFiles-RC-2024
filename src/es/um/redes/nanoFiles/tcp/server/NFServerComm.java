package es.um.redes.nanoFiles.tcp.server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.ByteBuffer;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFServerComm {
	private final static byte HASH_MAX_LENGTH = 20;

	public static void serveFilesToClient(Socket socket) {
		/*
		 * TODO: Crear dis/dos a partir del socket
		 */
		try {
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			DataInputStream dis = new DataInputStream(is);
			DataOutputStream dos = new DataOutputStream(os);
			while (!socket.isClosed()) {
				/*
				 * Primero recibimos un mensaje con el hash Despues buscamos el fichero y
				 * enviamos su hash, cuando el CLiente nos confirme haber recibido el hash
				 * empezamo a mandarle el fichero ya sea por trozos o completo
				 */
				PeerMessage receiveFromClient = PeerMessage.readMessageFromInputStream(dis);
				if (receiveFromClient.getOpcode() == PeerMessageOps.OPCODE_DOWNLOAD_FILE) {
					byte[] param1Size = receiveFromClient.getParam1();
					String hashSubstr = new String(receiveFromClient.getParam2());
					String filePath = "";
					String completeHash="";
					if (param1Size[0] == HASH_MAX_LENGTH) {
						filePath= NanoFiles.db.lookupFilePath(hashSubstr);
						completeHash=hashSubstr;
					} else {
						FileInfo[] coincidencia = FileInfo.lookupHashSubstring(NanoFiles.db.getFiles(), hashSubstr);
						if(coincidencia.length>1 || coincidencia.length ==0) {
							//hay un error hay que decirle al cliente
							//que su hash es ambiguo
						}else {
							filePath=coincidencia[0].filePath;
							completeHash=coincidencia[0].fileHash;
						}
					}
					
				/*Envio el mensaje */
					PeerMessage sendHashToClient = new PeerMessage(PeerMessageOps.OPCODE_GET_HASH,(byte)completeHash.getBytes().length,completeHash.getBytes());
					sendHashToClient.writeMessageToOutputStream(dos);
					File f = new File(filePath); // abro el fichero
					DataInputStream disF = new DataInputStream(new FileInputStream(f));
					long filelength = f.length(); //miro su tamaño
					byte data[] = new byte[(int) filelength]; //hago un array con el fichero completo
					disF.readFully(data);
					disF.close();
					//genero mensaje con el fichero
					PeerMessage dataToClient = new PeerMessage(PeerMessageOps.OPCODE_SEND_FILE,filelength,data);
						dataToClient.writeMessageToOutputStream(dos);
						socket.close(); //cierro el socket tras acabar la conexion
				}

						
				}

		}catch(IOException e){
		e.printStackTrace();
	}

	/*
	 * Mientras el cliente esté conectado, leer mensajes de socket, convertirlo a un
	 * objeto PeerMessage y luego actuar en función del tipo de mensaje recibido,
	 * enviando los correspondientes mensajes de respuesta.
	 */
	/*
	 * Para servir un fichero, hay que localizarlo a partir de su hash (o subcadena)
	 * en nuestra base de datos de ficheros compartidos. Los ficheros compartidos se
	 * pueden obtener con NanoFiles.db.getFiles(). El método
	 * FileInfo.lookupHashSubstring es útil para buscar coincidencias de una
	 * subcadena del hash. El método NanoFiles.db.lookupFilePath(targethash)
	 * devuelve la ruta al fichero a partir de su hash completo.
	 */

}

}

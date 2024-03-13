package es.um.redes.nanoFiles.tcp.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import es.um.redes.nanoFiles.util.FileInfo;

public class PeerMessage {



	private byte opcode;
	private byte[] param1;
	private byte[] param2;
	private static int bytesLongitud;

	/*
	 *  Añadir atributos y crear otros constructores específicos para crear
	 * mensajes con otros campos (tipos de datos)
	 * 
	 */

	public PeerMessage() {
		opcode = PeerMessageOps.OPCODE_INVALID_CODE;
	}

//formato control
	public PeerMessage(byte op) {
		opcode = op;
	}

//formato operacion
	public PeerMessage(byte op, byte[] par1) {
		opcode = op;
		param1 = par1;
	}

	public PeerMessage(byte op, byte[] par1, byte[] par2) {
		opcode = op;
		param1 = par1;
		param2 = par2;
	}

//formato tlv
	public PeerMessage(byte op, byte longitud, byte[] valor) {
		if (valor.length == longitud) {
			opcode = op;
			param1 = new byte[1];
			param1[0]=longitud;
			param2 = valor;
			bytesLongitud = 1;
		}

	}

	public PeerMessage(byte op, short longitud, byte[] valor) {
		if (valor.length == longitud) {
			opcode = op;
			ByteBuffer aux= ByteBuffer.allocate(Short.BYTES);
			aux.putShort(longitud);
			param1 = aux.array();
			param2 = valor;
			bytesLongitud = Short.BYTES;
		}
	}

	public PeerMessage(byte op, int longitud, byte[] valor) {
		if (valor.length == longitud) {
			opcode = op;
			ByteBuffer aux= ByteBuffer.allocate(Integer.BYTES);
			aux.putInt(longitud);
			param1 = aux.array();
			param2 = valor;
			bytesLongitud = Integer.BYTES;
		}
	}

	public PeerMessage(byte op, long longitud, byte[] valor) {
		if (valor.length == longitud) {
			opcode = op;
			ByteBuffer aux= ByteBuffer.allocate(Long.BYTES);
			aux.putLong(longitud);
			param1 = aux.array();
			param2 = valor;
			bytesLongitud = Long.BYTES;
		}

	}

	/*
	 * TODO: Crear métodos getter y setter para obtener valores de nuevos atributos,
	 * comprobando previamente que dichos atributos han sido establecidos por el
	 * constructor (sanity checks)
	 */
	public byte[] getParam1() {
		if (param1 == null) {
			throw new IllegalArgumentException();
		}
		return param1;
	}

	public void setParam1(byte[] param1) {
		this.param1 = param1;
	}

	public byte[] getParam2() {
		if (param2 == null) {
			throw new IllegalArgumentException();
		}
		return param2;
	}

	public void setParam2(byte[] param2) {
		this.param2 = param2;
	}

	public byte getOpcode() {
		if (Integer.parseInt(Byte.toString(opcode)) < 0) {
			throw new IllegalArgumentException();
		}
		return opcode;
	}

	/**
	 * Método de clase para parsear los campos de un mensaje y construir el objeto
	 * DirMessage que contiene los datos del mensaje recibido
	 * 
	 * @param data El array de bytes recibido
	 * @return Un objeto de esta clase cuyos atributos contienen los datos del
	 *         mensaje recibido.
	 * @throws IOException
	 */
	public static PeerMessage readMessageFromInputStream(DataInputStream dis) throws IOException {
		/*
		 * En función del tipo de mensaje, leer del socket a través del "dis" el
		 * resto de campos para ir extrayendo con los valores y establecer los atributos
		 * del un objeto DirMessage que contendrá toda la información del mensaje, y que
		 * será devuelto como resultado. NOTA: Usar dis.readFully para leer un array de
		 * bytes, dis.readInt para leer un entero, etc.
		 */

		byte opcode = dis.readByte();
		PeerMessage message = new PeerMessage(opcode);
		switch (opcode) {
		case (PeerMessageOps.OPCODE_INVALID_CODE): {
			System.err.println("Invalid operation code");
			break;
		}
		case (PeerMessageOps.OPCODE_DOWNLOAD_FILE): {
			bytesLongitud=1;
			byte[] hashLength =  new byte[bytesLongitud];// suponemos que el campo valor del hash ocupa 1byte
			hashLength[0]=dis.readByte();
			message.setParam1(hashLength);
			byte[] hash = new byte[(int)hashLength[0]];
			dis.readFully(hash);
			message.setParam2(hash);
			
			break;
		}
		case (PeerMessageOps.OPCODE_NOT_FOUND):
			break;
		case (PeerMessageOps.OPCODE_SEND_FILE): {
			/*
			 * Como el tamaño del fichero es un long
			 * mandando el fichero completo el tamaño del campo longitud
			 * del tlv va a ser un long*/
			bytesLongitud=Long.BYTES;
			byte[] longitud = new byte[bytesLongitud];
			dis.read(longitud);
			message.setParam1(longitud);
			ByteBuffer byteBuffer = ByteBuffer.wrap(longitud);
			long longit = byteBuffer.getLong();
			byte[] valor = new byte[(int) longit];
			dis.readFully(valor);
			message.setParam2(valor);
			break;
		}
		/* TODO: cambiar los parse cuando sepa como va a ser
		case (PeerMessageOps.OPCODE_SEND_FILE_CHUNK): {
			byte[] longitud = new byte[bytesLongitud]; // supongo que el campo longitud tiene 8 bytes
			dis.read(longitud);
			message.setParam1(longitud);
			switch (bytesLongitud) {
			case (1): {
				byte[] valor = new byte[1];
				dis.readFully(valor);
				message.setParam2(valor);
				break;
			}
			case (Short.BYTES): {
				short longit = Short.parseShort(longitud.toString());
				byte[] valor = new byte[longit];
				dis.readFully(valor);
				message.setParam2(valor);
				break;
			}
			case (Integer.BYTES): {
				int longit = Integer.parseInt(longitud.toString());
				byte[] valor = new byte[longit];
				dis.readFully(valor);
				message.setParam2(valor);
				break;
			}
			case(Long.BYTES):{
				long longit = Long.parseLong(longitud.toString());
				byte[] valor = new byte[(int) longit];
				dis.readFully(valor);
				message.setParam2(valor);
				break;
			}
			}
			
			break;
		}
		*/
		case (PeerMessageOps.OPCODE_GET_HASH): {
			bytesLongitud=1;
			byte[] hashLength =  new byte[bytesLongitud];
			hashLength[0]=dis.readByte();
			message.setParam1(hashLength);
			byte[] hash = new byte[(int)hashLength[0]];
			dis.readFully(hash);
			message.setParam2(hash);
			
			break;
		}
		default:
			System.err.println("PeerMessage.readMessageFromInputStream doesn't know how to parse this message opcode: "
					+ PeerMessageOps.opcodeToOperation(opcode));
			System.exit(-1);
		}
		return message;
	}

	public void writeMessageToOutputStream(DataOutputStream dos) throws IOException {
		/*
		 * TODO: Escribir los bytes en los que se codifica el mensaje en el socket a
		 * través del "dos", teniendo en cuenta opcode del mensaje del que se trata y
		 * los campos relevantes en cada caso. NOTA: Usar dos.write para leer un array
		 * de bytes, dos.writeInt para escribir un entero, etc.
		 */

		dos.writeByte(opcode);
		switch (opcode) {
		case (PeerMessageOps.OPCODE_INVALID_CODE): {
			System.err.println("Invalid operation code");
			break;
		}
		case (PeerMessageOps.OPCODE_DOWNLOAD_FILE): {
			dos.write(param1);
			dos.write(param2);
			break;
		}
		case (PeerMessageOps.OPCODE_NOT_FOUND):
			break;
		case (PeerMessageOps.OPCODE_SEND_FILE): {
			dos.write(param1);
			dos.write(param2);
			break;
		}
		case (PeerMessageOps.OPCODE_SEND_FILE_CHUNK): {
			dos.write(param1);
			dos.write(param2);
			break;
		}
		case(PeerMessageOps.OPCODE_GET_HASH):{
			dos.write(param1);
			dos.write(param2);
			break;
		}

		default:
			System.err.println("PeerMessage.writeMessageToOutputStream found unexpected message opcode " + opcode + "("
					+ PeerMessageOps.opcodeToOperation(opcode) + ")");
		}
	}

}

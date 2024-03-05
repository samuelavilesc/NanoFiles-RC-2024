package es.um.redes.javaDataStreams;

import java.io.*;
import java.util.Arrays;

public class EjemploDataStreams {

	// Método para escribir datos en un output stream. El stream puede haber
	// sido creado a partir de un fichero, de un socket, etc.
	public static void escribirDatosBinarios(OutputStream os) {
		try {
			DataOutputStream dos = new DataOutputStream(os);
			dos.writeInt(123); // Escribe un entero
			dos.writeLong(123456789L); // Escribe un long
			dos.writeFloat(3.14f); // Escribe un float
			dos.writeDouble(1.23456789); // Escribe un double
			byte[] datos = {1,2,3,4,5};
			dos.writeInt(datos.length);
			dos.write(datos);
			System.out.println("Datos escritos en el archivo.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Método para leer datos de un input stream. El stream puede haber
	// sido creado a partir de un fichero, de un socket, etc.
	public static void leerDatosBinarios(InputStream is) {
		try {
			DataInputStream dis = new DataInputStream(is);
			int entero = dis.readInt(); // Lee un entero
			long largo = dis.readLong(); // Lee un long
			float flotante = dis.readFloat(); // Lee un float
			double doble = dis.readDouble(); // Lee un double
			int longitudDatos = dis.readInt(); // Lee la longitud del array que viene a continuación
			byte[] datos = new byte[longitudDatos];
			dis.readFully(datos);
			System.out.println("Entero: " + entero);
			System.out.println("Long: " + largo);
			System.out.println("Float: " + flotante);
			System.out.println("Double: " + doble);
			System.out.println("Datos: " + Arrays.toString(datos));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws FileNotFoundException {
		String nombreArchivo = "datos.bin";
		FileOutputStream fos = new FileOutputStream(nombreArchivo);
		escribirDatosBinarios(fos);

		FileInputStream fis = new FileInputStream(nombreArchivo);
		leerDatosBinarios(fis);
	}
}

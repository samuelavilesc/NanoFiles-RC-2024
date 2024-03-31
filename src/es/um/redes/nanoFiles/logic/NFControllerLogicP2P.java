package es.um.redes.nanoFiles.logic;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedList;

import es.um.redes.nanoFiles.tcp.client.NFConnector;
import es.um.redes.nanoFiles.tcp.server.NFServer;
import es.um.redes.nanoFiles.tcp.server.NFServerSimple;

public class NFControllerLogicP2P {

	private NFServer bgserver;
	
	protected NFControllerLogicP2P() {
		this.bgserver=null;
	}

	/**
	 * Método para arrancar un servidor de ficheros en primer plano.
	 * 
	 */
	protected void foregroundServeFiles(NFControllerLogicDir controllerDir) {

			NFServerSimple simple = new NFServerSimple();
			controllerDir.registerFileServer(""+simple.getPort()); //aviso al directorio del puerto de escucha
			simple.run();
			


	}

	/**
	 * Método para ejecutar un servidor de ficheros en segundo plano. Debe arrancar
	 * el servidor en un nuevo hilo creado a tal efecto.
	 * 
	 * @return Verdadero si se ha arrancado en un nuevo hilo con el servidor de
	 *         ficheros, y está a la escucha en un puerto, falso en caso contrario.
	 * 
	 */
	protected boolean backgroundServeFiles() {

		boolean success = false;
		if(this.bgserver==null) {
			try {
				this.bgserver= new NFServer();
				bgserver.start();
				if(bgserver.getServerPort()>0) {
					System.out.println("Servidor iniciado correctamente, puerto: "+bgserver.getServerPort());
					success=true;
				}
			} catch (IOException e) {
				System.err.println("Hubo un problema con la creacion del NFServer");
			}
		}else {
			System.err.println("Ya existe un objeto NFServer creado previamente.");
		}

		return success;
	}

	/**
	 * Método para descargar un fichero del peer servidor de ficheros
	 * 
	 * @param fserverAddr    La dirección del servidor al que se conectará
	 * @param targetFileHash El hash del fichero a descargar
	 * @param localFileName  El nombre con el que se guardará el fichero descargado
	 */
	protected boolean downloadFileFromSingleServer(InetSocketAddress fserverAddr, String targetFileHash,
			String localFileName) {
		boolean result = false;
		if (fserverAddr == null) {
			System.err.println("* Cannot start download - No server address provided");
			return false;
		}

		try {
			NFConnector conn = new NFConnector(fserverAddr);
			File file = new File(localFileName);
			if (!file.exists()) {
				file.createNewFile();
				boolean downloaded = conn.downloadFile(targetFileHash, file);
				if (!downloaded) {
					file.delete();
				}

			} else {
				System.err.println("No se ha podido completar la descarga del archivo, ya existe un archivo"
						+ " con el mismo nombre");
			}
			conn.close();
		} catch (IOException e) {
			System.err.println("Fallo al crear la conexión con el socket.");
			try { // duermo el programa 10ms para que no se solape el print anterior
					// con el print del shell de nanofiles
				Thread.sleep(10);
			} catch (InterruptedException e1) {
			}
		}

		return result;
	}

	/**
	 * Método para descargar un fichero del peer servidor de ficheros
	 * 
	 * @param serverAddressList La lista de direcciones de los servidores a los que
	 *                          se conectará
	 * @param targetFileHash    Hash completo del fichero a descargar
	 * @param localFileName     Nombre con el que se guardará el fichero descargado
	 */
	public boolean downloadFileFromMultipleServers(LinkedList<InetSocketAddress> serverAddressList,
			String targetFileHash, String localFileName) {
		boolean downloaded = false;

		if (serverAddressList == null) {
			System.err.println("* Cannot start download - No list of server addresses provided");
			return false;
		}
		/*
		 * TODO: Crear un objeto NFConnector para establecer la conexión con cada
		 * servidor de ficheros, y usarlo para descargar un trozo (chunk) del fichero
		 * mediante su método "downloadFileChunk". Se debe comprobar previamente si ya
		 * existe un fichero con el mismo nombre en esta máquina, en cuyo caso se
		 * informa y no se realiza la descarga. Si todo va bien, imprimir mensaje
		 * informando de que se ha completado la descarga.
		 */
		/*
		 * TODO: Las excepciones que puedan lanzarse deben ser capturadas y tratadas en
		 * este método. Si se produce una excepción de entrada/salida (error del que no
		 * es posible recuperarse), se debe informar sin abortar el programa
		 */

		return downloaded;
	}

	/**
	 * Método para obtener el puerto de escucha de nuestro servidor de ficheros en
	 * segundo plano
	 * 
	 * @return El puerto en el que escucha el servidor, o 0 en caso de error.
	 */
	public int getServerPort() {
		int port = 0;
		/*
		 * TODO: Devolver el puerto de escucha de nuestro servidor de ficheros en
		 * segundo plano
		 */
		if(this.bgserver!=null) {
			port=bgserver.getServerPort();
		}

		return port;
	}
	public InetAddress getServerAddress() {
		return this.bgserver.getServerAddress();
	}
	/**
	 * Método para detener nuestro servidor de ficheros en segundo plano
	 * 
	 */
	public void stopBackgroundFileServer() {
		/*
		 * TODO: Enviar señal para detener nuestro servidor de ficheros en segundo plano
		 */
		this.bgserver.stopServer();
		this.bgserver=null;
		System.out.println("Servidor detenido correctamente.");

	}

}

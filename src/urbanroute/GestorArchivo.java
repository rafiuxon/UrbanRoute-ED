package urbanroute;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Se encarga de todo lo relacionado con el archivo CSV de la red:
 * leerlo para construir el grafo y guardar en el los cambios hechos en memoria.
 * <p>
 * Formato de cada linea: {@code Estacion_Origen,Estacion_Destino,Tiempo_Minutos}.
 * La primera linea puede ser un encabezado; si lo es, se ignora.
 *
 * @author Gabriel Gonzalez, Francisco Landaeta, Rafael Blanco
 */
public class GestorArchivo {

    /** Ruta del archivo maestro que se carga al iniciar el programa. */
    public static final String RUTA_ARCHIVO_MAESTRO = "datos" + File.separator + "metro_caracas.csv";

    /** Encabezado que se escribe al guardar. */
    private static final String ENCABEZADO = "Estacion_Origen,Estacion_Destino,Tiempo_Minutos";

    /** Maximo de errores que se describen (para no mostrar mensajes enormes). */
    private static final int MAX_ERRORES_MOSTRADOS = 15;

    /** Archivo que se esta usando actualmente (null si no se ha cargado ninguno). */
    private File archivoActual;

    /** Descripcion de los errores encontrados en la ultima carga. */
    private StringBuilder errores;

    /** Cantidad de lineas con error en la ultima carga. */
    private int cantidadErrores;

    /** Cantidad de conexiones validas leidas en la ultima carga. */
    private int conexionesLeidas;

    /**
     * Crea el gestor sin ningun archivo asociado.
     */
    public GestorArchivo() {
        archivoActual = null;
        errores = new StringBuilder();
        cantidadErrores = 0;
        conexionesLeidas = 0;
    }

    /**
     * Carga el archivo maestro de la red.
     *
     * @return el grafo construido
     * @throws IOException si el archivo no existe, no se puede leer o no tiene conexiones validas
     */
    public Grafo cargarArchivoMaestro() throws IOException {
        return cargar(new File(RUTA_ARCHIVO_MAESTRO));
    }

    /**
     * Lee un archivo CSV y construye un grafo nuevo con su contenido.
     * Cada estacion se crea la primera vez que aparece en el archivo.
     * Las lineas con errores se ignoran y se anotan (ver {@link #getErrores()}).
     * <p>
     * Se construye un grafo <b>nuevo</b>: si la carga falla, los datos que
     * ya estaban en memoria no se pierden.
     *
     * @param archivo archivo a leer
     * @return el grafo construido
     * @throws IOException si el archivo no existe, no se puede leer o no tiene conexiones validas
     */
    public Grafo cargar(File archivo) throws IOException {
        if (archivo == null || !archivo.isFile()) {
            throw new IOException("El archivo no existe.");
        }

        Grafo nuevoGrafo = new Grafo();
        errores = new StringBuilder();
        cantidadErrores = 0;
        conexionesLeidas = 0;

        // try-with-resources: el archivo se cierra solo al terminar (como "with open" en Python).
        // Se lee en UTF-8 para que nombres como "CAÑ" se lean bien.
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {

            String linea = lector.readLine();
            int numeroLinea = 1;
            while (linea != null) {
                procesarLinea(linea, numeroLinea, nuevoGrafo);
                linea = lector.readLine();
                numeroLinea++;
            }
        }

        if (conexionesLeidas == 0) {
            String mensaje = "El archivo no contiene ninguna conexion valida.\n"
                    + "Formato esperado en cada linea: Estacion_Origen,Estacion_Destino,Tiempo_Minutos";
            if (cantidadErrores > 0) {
                mensaje += "\n\n" + getErrores();
            }
            throw new IOException(mensaje);
        }

        archivoActual = archivo;
        return nuevoGrafo;
    }

    /**
     * Valida una linea del archivo y, si es correcta, agrega la conexion al grafo.
     *
     * @param linea       texto de la linea
     * @param numeroLinea numero de la linea (empieza en 1), para los mensajes de error
     * @param grafo       grafo que se esta construyendo
     */
    private void procesarLinea(String linea, int numeroLinea, Grafo grafo) {
        // Algunos programas (como Excel) agregan un caracter invisible al inicio del archivo
        linea = linea.replace("﻿", "").trim();

        if (linea.isEmpty()) {
            return;
        }

        String[] partes = linea.split(",");
        if (partes.length != 3) {
            agregarError(numeroLinea, "se esperaban 3 valores separados por coma");
            return;
        }

        String idOrigen = partes[0].trim();
        String idDestino = partes[1].trim();
        String textoTiempo = partes[2].trim();

        if (!esNumeroEntero(textoTiempo)) {
            if (numeroLinea == 1) {
                return; // Es el encabezado (ej. "Estacion_Origen,Estacion_Destino,Tiempo_Minutos")
            }
            agregarError(numeroLinea, "el tiempo \"" + textoTiempo + "\" no es un numero entero");
            return;
        }

        int tiempo = Integer.parseInt(textoTiempo);

        if (idOrigen.isEmpty() || idDestino.isEmpty()) {
            agregarError(numeroLinea, "falta el ID de alguna estacion");
            return;
        }
        if (tiempo <= 0) {
            agregarError(numeroLinea, "el tiempo debe ser mayor que cero");
            return;
        }
        if (idOrigen.equals(idDestino)) {
            agregarError(numeroLinea, "una estacion no puede conectarse consigo misma");
            return;
        }

        // Cada estacion se crea la primera vez que aparece
        if (!grafo.existeEstacion(idOrigen)) {
            grafo.agregarEstacion(idOrigen);
        }
        if (!grafo.existeEstacion(idDestino)) {
            grafo.agregarEstacion(idDestino);
        }

        if (grafo.conectar(idOrigen, idDestino, tiempo)) {
            conexionesLeidas++;
        } else {
            agregarError(numeroLinea, "la conexion " + idOrigen + "-" + idDestino + " esta repetida");
        }
    }

    /**
     * Revisa si un texto es un numero entero (puede tener signo negativo).
     *
     * @param texto texto a revisar
     * @return true si es un entero valido
     */
    private boolean esNumeroEntero(String texto) {
        try {
            Integer.parseInt(texto);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Anota un error de una linea del archivo.
     *
     * @param numeroLinea numero de la linea
     * @param descripcion que tiene de malo la linea
     */
    private void agregarError(int numeroLinea, String descripcion) {
        cantidadErrores++;
        if (cantidadErrores <= MAX_ERRORES_MOSTRADOS) {
            errores.append("Linea ").append(numeroLinea).append(": ").append(descripcion).append("\n");
        }
    }

    /**
     * Guarda la red en un archivo CSV con el mismo formato con el que se lee.
     * Como el grafo es no dirigido, cada conexion esta guardada en las dos estaciones;
     * para no escribirla dos veces, solo se escribe cuando el ID de origen va
     * antes que el de destino en orden alfabetico.
     * <p>
     * Nota: una estacion que se quede sin conexiones no aparece en el archivo,
     * porque el formato solo tiene lineas de conexiones.
     *
     * @param grafo   red a guardar
     * @param archivo archivo donde se guarda (se sobrescribe)
     * @throws IOException si no se puede escribir el archivo
     */
    public void guardar(Grafo grafo, File archivo) throws IOException {
        try (BufferedWriter escritor = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {

            escritor.write(ENCABEZADO);
            escritor.newLine();

            Estacion[] estaciones = grafo.obtenerEstaciones();
            for (int i = 0; i < estaciones.length; i++) {
                Estacion origen = estaciones[i];
                Conexion actual = origen.getPrimeraConexion();

                while (actual != null) {
                    Estacion destino = actual.getDestino();
                    if (origen.getId().compareTo(destino.getId()) < 0) {
                        escritor.write(origen.getId() + "," + destino.getId() + "," + actual.getTiempo());
                        escritor.newLine();
                    }
                    actual = actual.getSiguiente();
                }
            }
        }
        archivoActual = archivo;
    }

    /**
     * @return el archivo en uso, o null si no se ha cargado ninguno
     */
    public File getArchivoActual() {
        return archivoActual;
    }

    /**
     * @return true si la ultima carga tuvo lineas con errores
     */
    public boolean hayErrores() {
        return cantidadErrores > 0;
    }

    /**
     * Devuelve la descripcion de los errores de la ultima carga.
     *
     * @return texto con un error por linea
     */
    public String getErrores() {
        String texto = errores.toString();
        if (cantidadErrores > MAX_ERRORES_MOSTRADOS) {
            texto += "... y " + (cantidadErrores - MAX_ERRORES_MOSTRADOS) + " errores mas.\n";
        }
        return texto;
    }

    /**
     * @return la cantidad de lineas con error de la ultima carga
     */
    public int getCantidadErrores() {
        return cantidadErrores;
    }

    /**
     * @return la cantidad de conexiones validas leidas en la ultima carga
     */
    public int getConexionesLeidas() {
        return conexionesLeidas;
    }
}

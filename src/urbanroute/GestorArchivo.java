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
 * Se encarga de todo lo relacionado con los archivos CSV de la red.
 * La red se guarda en <b>dos archivos</b>:
 * <ul>
 *   <li>Archivo de estaciones: {@code ID_Estacion,Nombre,Linea,Zona,Capacidad_Pasajeros_Hora}.
 *       Define los nodos del grafo y lo que se guarda en la tabla hash.</li>
 *   <li>Archivo de conexiones: {@code Estacion_Origen,Estacion_Destino,Tiempo_Minutos}.
 *       Define las aristas del grafo.</li>
 * </ul>
 * Primero se lee el de estaciones y despues el de conexiones, porque una
 * conexion solo puede unir estaciones que ya existen.
 * La primera linea de cada archivo puede ser un encabezado; si lo es, se ignora.
 *
 * @author Gabriel Gonzalez, Francisco Landaeta, Rafael Blanco
 */
public class GestorArchivo {

    /** Ruta del archivo maestro de estaciones que se carga al iniciar el programa. */
    public static final String RUTA_ESTACIONES_MAESTRO = "datos" + File.separator + "estaciones.csv";

    /** Ruta del archivo maestro de conexiones que se carga al iniciar el programa. */
    public static final String RUTA_CONEXIONES_MAESTRO = "datos" + File.separator + "conexiones.csv";

    /** Encabezado que se escribe al guardar el archivo de estaciones. */
    private static final String ENCABEZADO_ESTACIONES = "ID_Estacion,Nombre,Linea,Zona,Capacidad_Pasajeros_Hora";

    /** Encabezado que se escribe al guardar el archivo de conexiones. */
    private static final String ENCABEZADO_CONEXIONES = "Estacion_Origen,Estacion_Destino,Tiempo_Minutos";

    /** Maximo de errores que se describen (para no mostrar mensajes enormes). */
    private static final int MAX_ERRORES_MOSTRADOS = 15;

    /** Archivo de estaciones en uso (null si no se ha cargado ninguno). */
    private File archivoEstaciones;

    /** Archivo de conexiones en uso (null si no se ha cargado ninguno). */
    private File archivoConexiones;

    /** Descripcion de los errores encontrados en la ultima carga. */
    private StringBuilder errores;

    /** Cantidad de lineas con error en la ultima carga (sumando los dos archivos). */
    private int cantidadErrores;

    /** Cantidad de estaciones validas leidas en la ultima carga. */
    private int estacionesLeidas;

    /** Cantidad de conexiones validas leidas en la ultima carga. */
    private int conexionesLeidas;

    /** Nombre del archivo que se esta leyendo, para los mensajes de error. */
    private String nombreArchivoLeido;

    /**
     * Crea el gestor sin ningun archivo asociado.
     */
    public GestorArchivo() {
        archivoEstaciones = null;
        archivoConexiones = null;
        errores = new StringBuilder();
        cantidadErrores = 0;
        estacionesLeidas = 0;
        conexionesLeidas = 0;
        nombreArchivoLeido = "";
    }

    // ===================================================================
    // Carga
    // ===================================================================

    /**
     * Carga los dos archivos maestros de la red.
     *
     * @return el grafo construido
     * @throws IOException si algun archivo no existe, no se puede leer o no tiene datos validos
     */
    public Grafo cargarArchivosMaestros() throws IOException {
        return cargar(new File(RUTA_ESTACIONES_MAESTRO), new File(RUTA_CONEXIONES_MAESTRO));
    }

    /**
     * Lee los dos archivos y construye un grafo nuevo con su contenido.
     * Las lineas con errores se ignoran y se anotan (ver {@link #getErrores()}).
     * <p>
     * Se construye un grafo <b>nuevo</b>: si la carga falla, los datos que
     * ya estaban en memoria no se pierden.
     *
     * @param estaciones archivo de estaciones
     * @param conexiones archivo de conexiones
     * @return el grafo construido
     * @throws IOException si algun archivo no existe, no se puede leer,
     *                     no tiene ninguna estacion valida o ambos son el mismo archivo
     */
    public Grafo cargar(File estaciones, File conexiones) throws IOException {
        if (estaciones == null || !estaciones.isFile()) {
            throw new IOException("El archivo de estaciones no existe.");
        }
        if (conexiones == null || !conexiones.isFile()) {
            throw new IOException("El archivo de conexiones no existe.");
        }
        if (estaciones.getAbsoluteFile().equals(conexiones.getAbsoluteFile())) {
            throw new IOException("Seleccionó el mismo archivo dos veces.\n"
                    + "Debe elegir un archivo de estaciones y otro de conexiones.");
        }

        Grafo nuevoGrafo = new Grafo();
        errores = new StringBuilder();
        cantidadErrores = 0;
        estacionesLeidas = 0;
        conexionesLeidas = 0;

        // 1) Estaciones: crean los nodos del grafo y se guardan en la tabla hash
        leerArchivo(estaciones, nuevoGrafo, true);

        if (estacionesLeidas == 0) {
            String mensaje = "El archivo " + estaciones.getName() + " no contiene ninguna estación válida.\n"
                    + "Formato esperado en cada línea: " + ENCABEZADO_ESTACIONES;
            if (cantidadErrores > 0) {
                mensaje += "\n\n" + getErrores();
            }
            throw new IOException(mensaje);
        }

        // 2) Conexiones: unen estaciones que ya existen
        int erroresAntes = cantidadErrores;
        leerArchivo(conexiones, nuevoGrafo, false);

        // Si el archivo tenia lineas pero ninguna sirvio, probablemente no es un archivo de conexiones.
        // (Un archivo de conexiones vacio si es valido: todas las estaciones quedan aisladas.)
        if (conexionesLeidas == 0 && cantidadErrores > erroresAntes) {
            throw new IOException("El archivo " + conexiones.getName() + " no contiene ninguna conexión válida.\n"
                    + "Formato esperado en cada línea: " + ENCABEZADO_CONEXIONES + "\n\n" + getErrores());
        }

        archivoEstaciones = estaciones;
        archivoConexiones = conexiones;
        return nuevoGrafo;
    }

    /**
     * Lee un archivo linea por linea y manda cada linea a validar.
     *
     * @param archivo       archivo a leer
     * @param grafo         grafo que se esta construyendo
     * @param esEstaciones  true si es el archivo de estaciones, false si es el de conexiones
     * @throws IOException si el archivo no se puede leer
     */
    private void leerArchivo(File archivo, Grafo grafo, boolean esEstaciones) throws IOException {
        nombreArchivoLeido = archivo.getName();

        // try-with-resources: el archivo se cierra solo al terminar (como "with open" en Python).
        // Se lee en UTF-8 para que nombres como "CAÑ" o "Chacaíto" se lean bien.
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {

            String linea = lector.readLine();
            int numeroLinea = 1;
            while (linea != null) {
                if (esEstaciones) {
                    procesarLineaEstacion(linea, numeroLinea, grafo);
                } else {
                    procesarLineaConexion(linea, numeroLinea, grafo);
                }
                linea = lector.readLine();
                numeroLinea++;
            }
        }
    }

    /**
     * Valida una linea del archivo de estaciones y, si es correcta,
     * crea la estacion y la agrega al grafo (y a la tabla hash).
     *
     * @param linea       texto de la linea
     * @param numeroLinea numero de la linea (empieza en 1)
     * @param grafo       grafo que se esta construyendo
     */
    private void procesarLineaEstacion(String linea, int numeroLinea, Grafo grafo) {
        // Algunos programas (como Excel) agregan un caracter invisible al inicio del archivo
        linea = linea.replace("﻿", "").trim();

        if (linea.isEmpty()) {
            return;
        }

        String[] partes = linea.split(",");
        if (partes.length != 5) {
            agregarError(numeroLinea, "se esperaban 5 valores separados por coma (ID, nombre, línea, zona, capacidad)");
            return;
        }

        String id = partes[0].trim();
        String nombre = partes[1].trim();
        String lineaTransporte = partes[2].trim();
        String zona = partes[3].trim();
        String textoCapacidad = partes[4].trim();

        if (!esNumeroEntero(textoCapacidad)) {
            if (numeroLinea == 1) {
                return; // Es el encabezado (ej. "ID_Estacion,Nombre,Linea,Zona,Capacidad_Pasajeros_Hora")
            }
            agregarError(numeroLinea, "la capacidad \"" + textoCapacidad + "\" no es un número entero");
            return;
        }

        int capacidad = Integer.parseInt(textoCapacidad);

        if (id.isEmpty() || nombre.isEmpty() || lineaTransporte.isEmpty() || zona.isEmpty()) {
            agregarError(numeroLinea, "falta algún dato de la estación");
            return;
        }
        if (capacidad <= 0) {
            agregarError(numeroLinea, "la capacidad debe ser mayor que cero");
            return;
        }

        Estacion nueva = new Estacion(id, nombre, lineaTransporte, zona, capacidad);
        if (grafo.agregarEstacion(nueva)) {
            estacionesLeidas++;
        } else {
            agregarError(numeroLinea, "la estación " + id + " está repetida");
        }
    }

    /**
     * Valida una linea del archivo de conexiones y, si es correcta,
     * conecta las dos estaciones en el grafo.
     *
     * @param linea       texto de la linea
     * @param numeroLinea numero de la linea (empieza en 1)
     * @param grafo       grafo que se esta construyendo
     */
    private void procesarLineaConexion(String linea, int numeroLinea, Grafo grafo) {
        linea = linea.replace("﻿", "").trim();

        if (linea.isEmpty()) {
            return;
        }

        String[] partes = linea.split(",");
        if (partes.length != 3) {
            agregarError(numeroLinea, "se esperaban 3 valores separados por coma (origen, destino, tiempo)");
            return;
        }

        String idOrigen = partes[0].trim();
        String idDestino = partes[1].trim();
        String textoTiempo = partes[2].trim();

        if (!esNumeroEntero(textoTiempo)) {
            if (numeroLinea == 1) {
                return; // Es el encabezado (ej. "Estacion_Origen,Estacion_Destino,Tiempo_Minutos")
            }
            agregarError(numeroLinea, "el tiempo \"" + textoTiempo + "\" no es un número entero");
            return;
        }

        int tiempo = Integer.parseInt(textoTiempo);

        if (idOrigen.isEmpty() || idDestino.isEmpty()) {
            agregarError(numeroLinea, "falta el ID de alguna estación");
            return;
        }
        if (tiempo <= 0) {
            agregarError(numeroLinea, "el tiempo debe ser mayor que cero");
            return;
        }
        if (idOrigen.equals(idDestino)) {
            agregarError(numeroLinea, "una estación no puede conectarse consigo misma");
            return;
        }

        // La conexion solo puede unir estaciones que vienen en el archivo de estaciones
        if (!grafo.existeEstacion(idOrigen)) {
            agregarError(numeroLinea, "la estación " + idOrigen + " no está en el archivo de estaciones");
            return;
        }
        if (!grafo.existeEstacion(idDestino)) {
            agregarError(numeroLinea, "la estación " + idDestino + " no está en el archivo de estaciones");
            return;
        }

        if (grafo.conectar(idOrigen, idDestino, tiempo)) {
            conexionesLeidas++;
        } else {
            agregarError(numeroLinea, "la conexión " + idOrigen + "-" + idDestino + " está repetida");
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
     * Anota un error de una linea del archivo que se esta leyendo.
     *
     * @param numeroLinea numero de la linea
     * @param descripcion que tiene de malo la linea
     */
    private void agregarError(int numeroLinea, String descripcion) {
        cantidadErrores++;
        if (cantidadErrores <= MAX_ERRORES_MOSTRADOS) {
            errores.append(nombreArchivoLeido).append(", línea ").append(numeroLinea)
                    .append(": ").append(descripcion).append("\n");
        }
    }

    // ===================================================================
    // Guardado
    // ===================================================================

    /**
     * Guarda la red en los mismos dos archivos que se cargaron.
     *
     * @param grafo red a guardar
     * @throws IOException si todavia no hay archivos asociados o no se pueden escribir
     */
    public void guardar(Grafo grafo) throws IOException {
        if (archivoEstaciones == null || archivoConexiones == null) {
            throw new IOException("No hay archivos asociados. Primero cargue los archivos de la red.");
        }
        guardar(grafo, archivoEstaciones, archivoConexiones);
    }

    /**
     * Guarda la red en dos archivos CSV con el mismo formato con el que se leen.
     * Se guardan todas las estaciones, incluso las que no tienen conexiones.
     *
     * @param grafo      red a guardar
     * @param estaciones archivo de estaciones (se sobrescribe)
     * @param conexiones archivo de conexiones (se sobrescribe)
     * @throws IOException si no se pueden escribir los archivos
     */
    public void guardar(Grafo grafo, File estaciones, File conexiones) throws IOException {
        Estacion[] lista = grafo.obtenerEstaciones();
        guardarEstaciones(lista, estaciones);
        guardarConexiones(lista, conexiones);
        archivoEstaciones = estaciones;
        archivoConexiones = conexiones;
    }

    /**
     * Escribe el archivo de estaciones: una linea por estacion.
     *
     * @param lista   estaciones de la red
     * @param archivo archivo donde se escribe
     * @throws IOException si no se puede escribir
     */
    private void guardarEstaciones(Estacion[] lista, File archivo) throws IOException {
        try (BufferedWriter escritor = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {

            escritor.write(ENCABEZADO_ESTACIONES);
            escritor.newLine();

            for (int i = 0; i < lista.length; i++) {
                Estacion e = lista[i];
                escritor.write(e.getId() + "," + e.getNombre() + "," + e.getLinea() + ","
                        + e.getZona() + "," + e.getCapacidad());
                escritor.newLine();
            }
        }
    }

    /**
     * Escribe el archivo de conexiones. Como el grafo es no dirigido, cada conexion
     * esta guardada en las dos estaciones; para no escribirla dos veces, solo se
     * escribe cuando el ID de origen va antes que el de destino en orden alfabetico.
     *
     * @param lista   estaciones de la red
     * @param archivo archivo donde se escribe
     * @throws IOException si no se puede escribir
     */
    private void guardarConexiones(Estacion[] lista, File archivo) throws IOException {
        try (BufferedWriter escritor = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {

            escritor.write(ENCABEZADO_CONEXIONES);
            escritor.newLine();

            for (int i = 0; i < lista.length; i++) {
                Estacion origen = lista[i];
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
    }

    // ===================================================================
    // Consultas
    // ===================================================================

    /**
     * @return el archivo de estaciones en uso, o null si no se ha cargado ninguno
     */
    public File getArchivoEstaciones() {
        return archivoEstaciones;
    }

    /**
     * @return el archivo de conexiones en uso, o null si no se ha cargado ninguno
     */
    public File getArchivoConexiones() {
        return archivoConexiones;
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
     * @return texto con un error por linea, indicando el archivo
     */
    public String getErrores() {
        String texto = errores.toString();
        if (cantidadErrores > MAX_ERRORES_MOSTRADOS) {
            texto += "... y " + (cantidadErrores - MAX_ERRORES_MOSTRADOS) + " errores más.\n";
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
     * @return la cantidad de estaciones validas leidas en la ultima carga
     */
    public int getEstacionesLeidas() {
        return estacionesLeidas;
    }

    /**
     * @return la cantidad de conexiones validas leidas en la ultima carga
     */
    public int getConexionesLeidas() {
        return conexionesLeidas;
    }
}

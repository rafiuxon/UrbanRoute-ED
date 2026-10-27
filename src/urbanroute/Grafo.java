package urbanroute;

/**
 * Representa la red de transporte completa como un grafo ponderado no dirigido.
 * <ul>
 *   <li>Nodos: objetos {@link Estacion}.</li>
 *   <li>Aristas: objetos {@link Conexion}, guardados en la lista de adyacencia de cada estacion.</li>
 *   <li>Peso: el tiempo de traslado en minutos.</li>
 * </ul>
 * Para encontrar una estacion por su ID, el grafo delega la busqueda
 * a la {@link TablaHash}, asi no tiene que recorrer todas las estaciones.
 *
 * @author Gabriel Gonzalez, Francisco Landaeta, Rafael Blanco
 */
public class Grafo {

    /** Indice de todas las estaciones de la red (ID -> Estacion). */
    private TablaHash tablaEstaciones;

    /**
     * Crea un grafo vacio.
     */
    public Grafo() {
        tablaEstaciones = new TablaHash();
    }

    /**
     * Busca una estacion por su ID usando la tabla hash.
     *
     * @param id ID de la estacion
     * @return la estacion, o null si no existe
     */
    public Estacion buscarEstacion(String id) {
        if (id == null) {
            return null;
        }
        return tablaEstaciones.buscar(id.trim());
    }

    /**
     * @param id ID de la estacion
     * @return true si la estacion existe en la red
     */
    public boolean existeEstacion(String id) {
        return buscarEstacion(id) != null;
    }

    /**
     * Agrega una estacion nueva (sin conexiones) a la red.
     *
     * @param id ID de la nueva estacion
     * @return la estacion creada, o null si el ID es invalido o ya existia
     */
    public Estacion agregarEstacion(String id) {
        if (id == null || id.trim().isEmpty() || existeEstacion(id)) {
            return null;
        }
        Estacion nueva = new Estacion(id.trim());
        tablaEstaciones.insertar(nueva.getId(), nueva);
        return nueva;
    }

    /**
     * Elimina una estacion de la red (simula su cierre).
     * Tambien borra las conexiones que la tenian como destino,
     * para que ninguna estacion vecina quede apuntando a ella.
     *
     * @param id ID de la estacion
     * @return true si se elimino, false si no existia
     */
    public boolean eliminarEstacion(String id) {
        Estacion estacion = buscarEstacion(id);
        if (estacion == null) {
            return false;
        }

        // Cada vecina tiene una conexion hacia esta estacion: se quita
        Conexion actual = estacion.getPrimeraConexion();
        while (actual != null) {
            actual.getDestino().eliminarConexion(estacion.getId());
            actual = actual.getSiguiente();
        }

        tablaEstaciones.eliminar(estacion.getId());
        return true;
    }

    /**
     * Conecta dos estaciones en ambos sentidos (grafo no dirigido).
     *
     * @param idOrigen  ID de una estacion
     * @param idDestino ID de la otra estacion
     * @param tiempo    tiempo de traslado en minutos (mayor que cero)
     * @return true si se conectaron; false si alguna no existe, son la misma,
     *         el tiempo no es valido o ya estaban conectadas
     */
    public boolean conectar(String idOrigen, String idDestino, int tiempo) {
        Estacion origen = buscarEstacion(idOrigen);
        Estacion destino = buscarEstacion(idDestino);

        if (origen == null || destino == null || origen == destino || tiempo <= 0) {
            return false;
        }
        if (origen.buscarConexion(destino.getId()) != null) {
            return false;
        }

        origen.agregarConexion(destino, tiempo);
        destino.agregarConexion(origen, tiempo);
        return true;
    }

    /**
     * Elimina la conexion entre dos estaciones en ambos sentidos.
     *
     * @param idOrigen  ID de una estacion
     * @param idDestino ID de la otra estacion
     * @return true si se desconectaron, false si no estaban conectadas
     */
    public boolean desconectar(String idOrigen, String idDestino) {
        Estacion origen = buscarEstacion(idOrigen);
        Estacion destino = buscarEstacion(idDestino);

        if (origen == null || destino == null) {
            return false;
        }

        boolean seElimino = origen.eliminarConexion(destino.getId());
        destino.eliminarConexion(origen.getId());
        return seElimino;
    }

    /**
     * Devuelve todas las estaciones ordenadas alfabeticamente por ID
     * (ordenamiento por insercion).
     *
     * @return arreglo con las estaciones de la red
     */
    public Estacion[] obtenerEstaciones() {
        Estacion[] estaciones = tablaEstaciones.obtenerEstaciones();

        for (int i = 1; i < estaciones.length; i++) {
            Estacion actual = estaciones[i];
            int j = i - 1;
            while (j >= 0 && estaciones[j].getId().compareTo(actual.getId()) > 0) {
                estaciones[j + 1] = estaciones[j];
                j--;
            }
            estaciones[j + 1] = actual;
        }
        return estaciones;
    }

    /**
     * @return la cantidad de estaciones de la red
     */
    public int getNumEstaciones() {
        return tablaEstaciones.getCantidad();
    }

    /**
     * Cuenta las conexiones de la red. Como el grafo es no dirigido,
     * cada conexion aparece en las dos estaciones, por eso se divide entre 2.
     *
     * @return la cantidad de conexiones
     */
    public int getNumConexiones() {
        int sumaGrados = 0;
        Estacion[] estaciones = tablaEstaciones.obtenerEstaciones();
        for (int i = 0; i < estaciones.length; i++) {
            sumaGrados += estaciones[i].getGrado();
        }
        return sumaGrados / 2;
    }

    /**
     * @return true si la red no tiene estaciones
     */
    public boolean estaVacio() {
        return tablaEstaciones.getCantidad() == 0;
    }
}

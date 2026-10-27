package urbanroute;

/**
 * Representa una parada o estacion de la red de transporte
 * (por ejemplo Propatria o Plaza Venezuela). Es un nodo del grafo.
 * <p>
 * Cada estacion guarda su lista de adyacencia: una lista enlazada
 * de objetos {@link Conexion} que salen de ella.
 *
 * @author Gabriel Gonzalez, Francisco Landaeta, Rafael Blanco
 */
public class Estacion {

    /** Identificador de la estacion (ej. "PRO"). Funciona como su nombre. */
    private String id;

    /** Primera conexion de la lista de adyacencia (null si no tiene). */
    private Conexion primeraConexion;

    /** Cantidad de conexiones directas que tiene la estacion. */
    private int grado;

    /**
     * Crea una estacion sin conexiones.
     *
     * @param id identificador de la estacion
     */
    public Estacion(String id) {
        this.id = id;
        this.primeraConexion = null;
        this.grado = 0;
    }

    /**
     * @return el identificador de la estacion
     */
    public String getId() {
        return id;
    }

    /**
     * @return la primera conexion de la lista, o null si no tiene conexiones
     */
    public Conexion getPrimeraConexion() {
        return primeraConexion;
    }

    /**
     * Devuelve el grado de la estacion, es decir, cuantas conexiones
     * directas tiene. Se usara para identificar los hubs.
     *
     * @return el numero de conexiones directas
     */
    public int getGrado() {
        return grado;
    }

    /**
     * Busca la conexion que va desde esta estacion hacia otra.
     *
     * @param idDestino ID de la estacion vecina
     * @return la conexion encontrada, o null si no estan conectadas
     */
    public Conexion buscarConexion(String idDestino) {
        Conexion actual = primeraConexion;
        while (actual != null) {
            if (actual.getDestino().getId().equals(idDestino)) {
                return actual;
            }
            actual = actual.getSiguiente();
        }
        return null;
    }

    /**
     * Agrega una conexion al final de la lista de esta estacion.
     * No permite conexiones repetidas hacia la misma estacion.
     *
     * @param destino estacion vecina
     * @param tiempo  tiempo de traslado en minutos
     * @return true si se agrego, false si ya existia una conexion hacia ese destino
     */
    public boolean agregarConexion(Estacion destino, int tiempo) {
        if (buscarConexion(destino.getId()) != null) {
            return false;
        }

        Conexion nueva = new Conexion(destino, tiempo);

        if (primeraConexion == null) {
            primeraConexion = nueva;
        } else {
            // Se recorre hasta la ultima conexion para agregar la nueva al final
            Conexion actual = primeraConexion;
            while (actual.getSiguiente() != null) {
                actual = actual.getSiguiente();
            }
            actual.setSiguiente(nueva);
        }

        grado++;
        return true;
    }

    /**
     * Quita de la lista la conexion que va hacia una estacion vecina.
     *
     * @param idDestino ID de la estacion vecina
     * @return true si se elimino, false si no existia esa conexion
     */
    public boolean eliminarConexion(String idDestino) {
        Conexion actual = primeraConexion;
        Conexion anterior = null;

        while (actual != null) {
            if (actual.getDestino().getId().equals(idDestino)) {
                if (anterior == null) {
                    // Era la primera de la lista
                    primeraConexion = actual.getSiguiente();
                } else {
                    // Se "salta" la conexion que se quiere quitar
                    anterior.setSiguiente(actual.getSiguiente());
                }
                grado--;
                return true;
            }
            anterior = actual;
            actual = actual.getSiguiente();
        }
        return false;
    }

    /**
     * @return el ID de la estacion
     */
    @Override
    public String toString() {
        return id;
    }
}

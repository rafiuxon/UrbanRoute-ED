package urbanroute;

/**
 * Representa un tramo de la red de transporte: la union entre una estacion
 * y una estacion vecina (destino), junto con el tiempo de traslado.
 * <p>
 * Cada estacion guarda sus conexiones en una lista enlazada hecha a mano.
 * Por eso cada conexion conoce cual es la siguiente conexion de la lista.
 *
 * @author Gabriel Gonzalez, Francisco Landaeta, Rafael Blanco
 */
public class Conexion {

    /** Estacion a la que se llega por esta conexion. */
    private Estacion destino;

    /** Tiempo de traslado en minutos (siempre mayor que cero). */
    private int tiempo;

    /** Siguiente conexion de la misma estacion (null si es la ultima). */
    private Conexion siguiente;

    /**
     * Crea una conexion hacia una estacion destino.
     *
     * @param destino estacion a la que se llega
     * @param tiempo  tiempo de traslado en minutos
     */
    public Conexion(Estacion destino, int tiempo) {
        this.destino = destino;
        this.tiempo = tiempo;
        this.siguiente = null;
    }

    /**
     * @return la estacion a la que se llega por esta conexion
     */
    public Estacion getDestino() {
        return destino;
    }

    /**
     * @return el tiempo de traslado en minutos
     */
    public int getTiempo() {
        return tiempo;
    }

    /**
     * Cambia el tiempo de traslado de la conexion.
     *
     * @param tiempo nuevo tiempo en minutos
     */
    public void setTiempo(int tiempo) {
        this.tiempo = tiempo;
    }

    /**
     * @return la siguiente conexion de la lista, o null si es la ultima
     */
    public Conexion getSiguiente() {
        return siguiente;
    }

    /**
     * Enlaza esta conexion con la siguiente de la lista.
     *
     * @param siguiente conexion que va despues de esta
     */
    public void setSiguiente(Conexion siguiente) {
        this.siguiente = siguiente;
    }
}

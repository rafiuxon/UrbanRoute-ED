package urbanroute;

/**
 * Elemento guardado dentro de la {@link TablaHash}. Une una clave (el ID
 * de la estacion) con su objeto {@link Estacion}.
 * <p>
 * Cuando dos claves caen en la misma posicion de la tabla (colision),
 * los nodos se enlazan formando una cadena (encadenamiento separado).
 *
 * @author Gabriel Gonzalez, Francisco Landaeta, Rafael Blanco
 */
public class NodoHash {

    /** ID de la estacion. */
    private String clave;

    /** Estacion asociada a la clave. */
    private Estacion estacion;

    /** Siguiente nodo de la misma posicion de la tabla (null si es el ultimo). */
    private NodoHash siguiente;

    /**
     * Crea un nodo con una clave y su estacion.
     *
     * @param clave    ID de la estacion
     * @param estacion estacion asociada
     */
    public NodoHash(String clave, Estacion estacion) {
        this.clave = clave;
        this.estacion = estacion;
        this.siguiente = null;
    }

    /**
     * @return la clave (ID de la estacion)
     */
    public String getClave() {
        return clave;
    }

    /**
     * @return la estacion guardada en este nodo
     */
    public Estacion getEstacion() {
        return estacion;
    }

    /**
     * @return el siguiente nodo de la cadena, o null si es el ultimo
     */
    public NodoHash getSiguiente() {
        return siguiente;
    }

    /**
     * Enlaza este nodo con el siguiente de la cadena.
     *
     * @param siguiente nodo que va despues de este
     */
    public void setSiguiente(NodoHash siguiente) {
        this.siguiente = siguiente;
    }
}

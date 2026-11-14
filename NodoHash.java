package urbanroute;

/**
 * Elemento guardado dentro de la {@link TablaHash}. Une una clave (el ID
 * de la estacion) con su objeto {@link Estacion} y guarda el
 * <b>valor hash</b> de esa clave.
 * <p>
 * El valor hash se calcula una sola vez, cuando se crea el nodo, y no depende
 * del tamano de la tabla. Asi la tabla puede:
 * <ul>
 *   <li>Calcular la posicion del nodo cuando crece, sin volver a recorrer
 *       los caracteres del ID.</li>
 *   <li>Descartar rapido los nodos de otra clave al buscar: primero compara
 *       dos numeros y solo si son iguales compara los textos.</li>
 * </ul>
 * Cuando dos claves caen en la misma posicion de la tabla (colision),
 * los nodos se enlazan formando una cadena (encadenamiento separado).
 * <p>
 * Fuente: la idea de guardar el valor hash en cada nodo es la misma de la
 * clase java.util.Hashtable de Java (codigo fuente de OpenJDK), donde cada
 * entrada guarda su hash para no recalcularlo al crecer la tabla.
 *
 * @author Gabriel Gonzalez, Francisco Landaeta, Rafael Blanco
 */
public class NodoHash {

    /** ID de la estacion. */
    private String clave;

    /** Valor hash de la clave (numero entre 0 y 2^31 - 2, no depende del tamano de la tabla). */
    private int valorHash;

    /** Estacion asociada a la clave. */
    private Estacion estacion;

    /** Siguiente nodo de la misma posicion de la tabla (null si es el ultimo). */
    private NodoHash siguiente;

    /**
     * Crea un nodo con una clave, su valor hash y su estacion.
     *
     * @param clave     ID de la estacion
     * @param valorHash valor hash de la clave, calculado por la {@link TablaHash}
     * @param estacion  estacion asociada
     */
    public NodoHash(String clave, int valorHash, Estacion estacion) {
        this.clave = clave;
        this.valorHash = valorHash;
        this.estacion = estacion;
        this.siguiente = null;
    }

    /**
     * Indica si este nodo es el de la clave buscada. Primero compara los
     * valores hash (dos numeros, muy rapido); si son distintos, la clave no
     * puede ser la misma y no hace falta comparar los textos.
     *
     * @param claveBuscada ID que se esta buscando
     * @param valorBuscado valor hash de ese ID
     * @return true si el nodo guarda esa clave
     */
    public boolean tieneClave(String claveBuscada, int valorBuscado) {
        return valorHash == valorBuscado && clave.equals(claveBuscada);
    }

    /**
     * @return el valor hash de la clave
     */
    public int getValorHash() {
        return valorHash;
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

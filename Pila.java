package urbanroute;

/**
 * Pila propia de estaciones, implementada con un arreglo. Es una estructura
 * LIFO: el ultimo elemento que entra es el primero que sale.
 * <p>
 * La clase controla sus propias reglas:
 * <ul>
 *   <li>Solo se puede ver o sacar el elemento del tope.</li>
 *   <li>Sacar o consultar el tope de una pila vacia es un error
 *       (subdesbordamiento) y lanza una excepcion.</li>
 *   <li>Si el arreglo se llena, se agranda al doble, asi la pila nunca
 *       se desborda.</li>
 * </ul>
 * Apilar, desapilar y ver el tope cuestan O(1) (apilar es O(1) en promedio,
 * porque de vez en cuando hay que agrandar el arreglo).
 * <p>
 * La usa {@link AnalizadorRed} en el recorrido en profundidad (DFS).
 * <p>
 * Fuente: Cormen, T. H., Leiserson, C. E., Rivest, R. L. y Stein, C. (2009).
 * <i>Introduction to Algorithms</i> (3.a ed.), seccion 10.1 "Stacks and queues".
 *
 * @author Gabriel Gonzalez, Francisco Landaeta, Rafael Blanco
 */
public class Pila {

    /** Tamano del arreglo cuando no se indica otro. */
    private static final int CAPACIDAD_INICIAL = 16;

    /** Arreglo donde se guardan los elementos, desde el fondo (posicion 0) hasta el tope. */
    private Estacion[] elementos;

    /** Cantidad de elementos en la pila. El del tope esta en elementos[tope - 1]. */
    private int tope;

    /**
     * Crea una pila vacia con la capacidad inicial por defecto.
     */
    public Pila() {
        this(CAPACIDAD_INICIAL);
    }

    /**
     * Crea una pila vacia con espacio para cierta cantidad de elementos.
     * Si se llena, igual puede crecer.
     *
     * @param capacidadInicial cuantos elementos caben al principio (minimo 1)
     */
    public Pila(int capacidadInicial) {
        if (capacidadInicial < 1) {
            capacidadInicial = 1;
        }
        elementos = new Estacion[capacidadInicial];
        tope = 0;
    }

    /**
     * Mete una estacion en el tope de la pila.
     *
     * @param estacion estacion a apilar
     * @throws IllegalArgumentException si la estacion es null
     */
    public void apilar(Estacion estacion) {
        if (estacion == null) {
            throw new IllegalArgumentException("No se puede apilar un elemento vacio (null).");
        }
        if (tope == elementos.length) {
            agrandar();
        }
        elementos[tope] = estacion;
        tope++;
    }

    /**
     * Saca la estacion del tope de la pila (la ultima que entro).
     *
     * @return la estacion que estaba en el tope
     * @throws IllegalStateException si la pila esta vacia
     */
    public Estacion desapilar() {
        if (estaVacia()) {
            throw new IllegalStateException("No se puede desapilar: la pila esta vacia.");
        }
        tope--;
        Estacion estacion = elementos[tope];
        elementos[tope] = null; // la posicion queda libre
        return estacion;
    }

    /**
     * Devuelve la estacion del tope sin sacarla.
     *
     * @return la estacion del tope
     * @throws IllegalStateException si la pila esta vacia
     */
    public Estacion verTope() {
        if (estaVacia()) {
            throw new IllegalStateException("No hay tope: la pila esta vacia.");
        }
        return elementos[tope - 1];
    }

    /**
     * @return true si la pila no tiene elementos
     */
    public boolean estaVacia() {
        return tope == 0;
    }

    /**
     * @return cuantos elementos hay en la pila
     */
    public int getTamano() {
        return tope;
    }

    /**
     * Duplica el tamano del arreglo y copia los elementos en el mismo orden.
     */
    private void agrandar() {
        Estacion[] nuevo = new Estacion[elementos.length * 2];
        for (int i = 0; i < tope; i++) {
            nuevo[i] = elementos[i];
        }
        elementos = nuevo;
    }
}

package urbanroute;

/**
 * Tabla de dispersion propia que guarda las estaciones usando su ID como clave
 * (ID -> Estacion). Permite buscar una estacion en tiempo O(1) en promedio,
 * sin recorrer toda la red.
 * <p>
 * Las colisiones se resuelven con <b>encadenamiento separado</b>: cada
 * posicion del arreglo es el inicio de una lista enlazada de {@link NodoHash}.
 * Cuando la tabla se llena demasiado (factor de carga mayor a 0,75), se
 * agranda para que las cadenas sigan siendo cortas.
 *
 * @author Gabriel Gonzalez, Francisco Landaeta, Rafael Blanco
 */
public class TablaHash {

    /** Tamano inicial del arreglo. Se usa un numero primo para repartir mejor las claves. */
    private static final int CAPACIDAD_INICIAL = 53;

    /** Si cantidad / capacidad supera este valor, la tabla se agranda. */
    private static final double FACTOR_CARGA_MAXIMO = 0.75;

    /** Arreglo de posiciones; cada una es el inicio de una cadena. */
    private NodoHash[] tabla;

    /** Cantidad de estaciones guardadas. */
    private int cantidad;

    /**
     * Crea una tabla vacia.
     */
    public TablaHash() {
        tabla = new NodoHash[CAPACIDAD_INICIAL];
        cantidad = 0;
    }

    /**
     * Funcion de dispersion: convierte una clave en una posicion del arreglo.
     * <p>
     * Recorre cada caracter y acumula: hash = hash * 31 + caracter.
     * Se aplica el modulo en cada paso para que el numero nunca se desborde.
     * Asi, claves parecidas como "CAP" y "CAP_L4" caen en posiciones distintas.
     *
     * @param clave     ID de la estacion
     * @param capacidad tamano del arreglo
     * @return una posicion entre 0 y capacidad - 1
     */
    private int funcionHash(String clave, int capacidad) {
        int hash = 0;
        for (int i = 0; i < clave.length(); i++) {
            hash = (hash * 31 + clave.charAt(i)) % capacidad;
        }
        return hash;
    }

    /**
     * Guarda una estacion en la tabla. Si hay colision, el nodo nuevo
     * se coloca al inicio de la cadena de esa posicion.
     *
     * @param clave    ID de la estacion
     * @param estacion estacion a guardar
     * @return true si se guardo, false si ya existia una estacion con esa clave
     */
    public boolean insertar(String clave, Estacion estacion) {
        if (buscar(clave) != null) {
            return false;
        }

        int posicion = funcionHash(clave, tabla.length);
        NodoHash nuevo = new NodoHash(clave, estacion);
        nuevo.setSiguiente(tabla[posicion]);
        tabla[posicion] = nuevo;
        cantidad++;

        if (cantidad > tabla.length * FACTOR_CARGA_MAXIMO) {
            redimensionar();
        }
        return true;
    }

    /**
     * Busca una estacion por su ID.
     *
     * @param clave ID de la estacion
     * @return la estacion, o null si no existe
     */
    public Estacion buscar(String clave) {
        int posicion = funcionHash(clave, tabla.length);
        NodoHash actual = tabla[posicion];

        while (actual != null) {
            if (actual.getClave().equals(clave)) {
                return actual.getEstacion();
            }
            actual = actual.getSiguiente();
        }
        return null;
    }

    /**
     * Elimina una estacion de la tabla.
     *
     * @param clave ID de la estacion
     * @return true si se elimino, false si no existia
     */
    public boolean eliminar(String clave) {
        int posicion = funcionHash(clave, tabla.length);
        NodoHash actual = tabla[posicion];
        NodoHash anterior = null;

        while (actual != null) {
            if (actual.getClave().equals(clave)) {
                if (anterior == null) {
                    tabla[posicion] = actual.getSiguiente();
                } else {
                    anterior.setSiguiente(actual.getSiguiente());
                }
                cantidad--;
                return true;
            }
            anterior = actual;
            actual = actual.getSiguiente();
        }
        return false;
    }

    /**
     * Agranda el arreglo (al doble mas uno) y vuelve a colocar cada nodo
     * en su nueva posicion, porque la posicion depende del tamano del arreglo.
     */
    private void redimensionar() {
        NodoHash[] tablaVieja = tabla;
        tabla = new NodoHash[tablaVieja.length * 2 + 1];

        for (int i = 0; i < tablaVieja.length; i++) {
            NodoHash actual = tablaVieja[i];
            while (actual != null) {
                NodoHash siguiente = actual.getSiguiente();
                int posicion = funcionHash(actual.getClave(), tabla.length);
                actual.setSiguiente(tabla[posicion]);
                tabla[posicion] = actual;
                actual = siguiente;
            }
        }
    }

    /**
     * Devuelve todas las estaciones guardadas en un arreglo (sin un orden particular).
     *
     * @return arreglo con todas las estaciones
     */
    public Estacion[] obtenerEstaciones() {
        Estacion[] estaciones = new Estacion[cantidad];
        int indice = 0;

        for (int i = 0; i < tabla.length; i++) {
            NodoHash actual = tabla[i];
            while (actual != null) {
                estaciones[indice] = actual.getEstacion();
                indice++;
                actual = actual.getSiguiente();
            }
        }
        return estaciones;
    }

    /**
     * @return la cantidad de estaciones guardadas
     */
    public int getCantidad() {
        return cantidad;
    }

    /**
     * @return el tamano actual del arreglo interno
     */
    public int getCapacidad() {
        return tabla.length;
    }
}

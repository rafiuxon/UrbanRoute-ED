package urbanroute;

/**
 * Tabla de dispersion propia que guarda las estaciones usando su ID como clave
 * (ID -> Estacion). Permite buscar una estacion en tiempo O(1) en promedio,
 * sin recorrer toda la red.
 * <p>
 * <b>Funcion hash, en dos pasos:</b>
 * <ol>
 *   <li>El ID se convierte en un numero, su <b>valor hash</b>, con el metodo
 *       polinomial: para cada caracter, valor = valor * 31 + caracter.
 *       Este valor no depende del tamano de la tabla y queda guardado en el
 *       {@link NodoHash}.</li>
 *   <li>La posicion se obtiene con el metodo de la division:
 *       posicion = valor hash % capacidad.</li>
 * </ol>
 * <b>Colisiones:</b> se resuelven con <b>encadenamiento separado</b>. Cada
 * posicion del arreglo es el inicio de una lista enlazada de {@link NodoHash}.
 * <p>
 * <b>Crecimiento:</b> cuando el factor de carga (cantidad / capacidad) pasa
 * de 0,75, el arreglo crece al primer numero primo mayor o igual a
 * 2 * capacidad + 1. Cada nodo se recoloca usando el valor hash que ya tiene
 * guardado. La capacidad siempre es un numero primo: 53, 107, 223, 449...
 * <p>
 * <b>Fuentes:</b>
 * <ul>
 *   <li>Cormen, T. H., Leiserson, C. E., Rivest, R. L. y Stein, C. (2009).
 *       <i>Introduction to Algorithms</i> (3.a ed.), cap. 11 "Hash Tables":
 *       encadenamiento separado y factor de carga (11.2), y metodo de la
 *       division con un numero primo como tamano (11.3.1).</li>
 *   <li>Weiss, M. A. (2012). <i>Data Structures and Algorithm Analysis in Java</i>
 *       (3.a ed.), cap. 5 "Hashing": funcion hash polinomial para textos,
 *       encadenamiento separado y redimensionamiento (rehashing) a un tamano primo.</li>
 *   <li>Bloch, J. (2018). <i>Effective Java</i> (3.a ed.), item 11:
 *       por que se usa 31 como multiplicador.</li>
 *   <li>Oracle. Documentacion de Java SE 17: String.hashCode(), que usa la
 *       misma formula con 31, y java.util.Hashtable, que usa 0,75 como factor
 *       de carga por defecto. Se usaron solo como referencia: esta tabla no usa
 *       ninguna clase de Java Collections.</li>
 * </ul>
 *
 * @author Gabriel Gonzalez, Francisco Landaeta, Rafael Blanco
 */
public class TablaHash {

    /** Tamano inicial del arreglo. Es un numero primo para repartir mejor las claves. */
    private static final int CAPACIDAD_INICIAL = 53;

    /** Si cantidad / capacidad supera este valor, la tabla se agranda. */
    private static final double FACTOR_CARGA_MAXIMO = 0.75;

    /** Multiplicador de la funcion hash: un primo mayor que la diferencia entre dos letras (25). */
    private static final int MULTIPLICADOR = 31;

    /**
     * Modulo que se aplica al calcular el valor hash para que el numero nunca
     * se desborde ni quede negativo. Es 2^31 - 1, el int mas grande, que ademas es primo.
     */
    private static final long MODULO_VALOR_HASH = Integer.MAX_VALUE;

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

    // ===================================================================
    // Funcion hash
    // ===================================================================

    /**
     * Paso 1 de la funcion hash: convierte una clave en su valor hash.
     * <p>
     * Recorre cada caracter y acumula: valor = valor * 31 + caracter.
     * Se aplica el modulo 2^31 - 1 en cada paso para que el numero nunca se
     * desborde. Por eso se usa un long para la cuenta: valor * 31 no cabe en un int.
     * Asi, claves parecidas como "CAP" y "CAP_L4" dan valores distintos.
     *
     * @param clave ID de la estacion
     * @return el valor hash, entre 0 y 2^31 - 2 (no depende del tamano de la tabla)
     */
    private int calcularValorHash(String clave) {
        long valor = 0;
        for (int i = 0; i < clave.length(); i++) {
            valor = (valor * MULTIPLICADOR + clave.charAt(i)) % MODULO_VALOR_HASH;
        }
        return (int) valor;
    }

    /**
     * Paso 2 de la funcion hash (metodo de la division): convierte un valor
     * hash en una posicion del arreglo.
     *
     * @param valorHash valor hash de la clave
     * @param capacidad tamano del arreglo
     * @return una posicion entre 0 y capacidad - 1
     */
    private int calcularPosicion(int valorHash, int capacidad) {
        return valorHash % capacidad;
    }

    // ===================================================================
    // Operaciones
    // ===================================================================

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

        int valorHash = calcularValorHash(clave);
        int posicion = calcularPosicion(valorHash, tabla.length);
        NodoHash nuevo = new NodoHash(clave, valorHash, estacion);
        nuevo.setSiguiente(tabla[posicion]);
        tabla[posicion] = nuevo;
        cantidad++;

        if (getFactorCarga() > FACTOR_CARGA_MAXIMO) {
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
        int valorHash = calcularValorHash(clave);
        int posicion = calcularPosicion(valorHash, tabla.length);
        NodoHash actual = tabla[posicion];

        while (actual != null) {
            if (actual.tieneClave(clave, valorHash)) {
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
        int valorHash = calcularValorHash(clave);
        int posicion = calcularPosicion(valorHash, tabla.length);
        NodoHash actual = tabla[posicion];
        NodoHash anterior = null;

        while (actual != null) {
            if (actual.tieneClave(clave, valorHash)) {
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
     * Agranda el arreglo al primer primo mayor o igual a 2 * capacidad + 1
     * y vuelve a colocar cada nodo en su nueva posicion, porque la posicion
     * depende del tamano del arreglo. No hace falta recalcular el valor hash:
     * cada nodo ya lo tiene guardado.
     */
    private void redimensionar() {
        NodoHash[] tablaVieja = tabla;
        tabla = new NodoHash[siguientePrimo(tablaVieja.length * 2 + 1)];

        for (int i = 0; i < tablaVieja.length; i++) {
            NodoHash actual = tablaVieja[i];
            while (actual != null) {
                NodoHash siguiente = actual.getSiguiente();
                int posicion = calcularPosicion(actual.getValorHash(), tabla.length);
                actual.setSiguiente(tabla[posicion]);
                tabla[posicion] = actual;
                actual = siguiente;
            }
        }
    }

    /**
     * @param numero numero desde el que se empieza a buscar
     * @return el primer numero primo mayor o igual a numero
     */
    private int siguientePrimo(int numero) {
        while (!esPrimo(numero)) {
            numero++;
        }
        return numero;
    }

    /**
     * Revisa si un numero es primo probando divisores hasta su raiz cuadrada.
     *
     * @param numero numero a revisar
     * @return true si es primo
     */
    private boolean esPrimo(int numero) {
        if (numero < 2) {
            return false;
        }
        for (int divisor = 2; divisor * divisor <= numero; divisor++) {
            if (numero % divisor == 0) {
                return false;
            }
        }
        return true;
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

    // ===================================================================
    // Estado de la tabla (para mostrarla en la ventana)
    // ===================================================================

    /**
     * @return la cantidad de estaciones guardadas
     */
    public int getCantidad() {
        return cantidad;
    }

    /**
     * @return el tamano actual del arreglo interno (cuantas casillas tiene)
     */
    public int getCapacidad() {
        return tabla.length;
    }

    /**
     * @return el factor de carga: estaciones guardadas / casillas del arreglo
     */
    public double getFactorCarga() {
        return (double) cantidad / tabla.length;
    }

    /**
     * Devuelve el primer nodo de la cadena de una casilla, para poder
     * recorrerla y mostrarla.
     *
     * @param posicion casilla, entre 0 y capacidad - 1
     * @return el primer nodo de esa casilla, o null si esta vacia
     */
    public NodoHash getPrimeroDeCasilla(int posicion) {
        return tabla[posicion];
    }

    /**
     * @return cuantas casillas tienen al menos una estacion
     */
    public int contarCasillasOcupadas() {
        int ocupadas = 0;
        for (int i = 0; i < tabla.length; i++) {
            if (tabla[i] != null) {
                ocupadas++;
            }
        }
        return ocupadas;
    }

    /**
     * Cuenta las colisiones: las estaciones que cayeron en una casilla que
     * ya estaba ocupada. Es igual a la cantidad de estaciones menos la
     * cantidad de casillas ocupadas.
     *
     * @return cuantas estaciones chocaron con otra
     */
    public int contarColisiones() {
        return cantidad - contarCasillasOcupadas();
    }

    /**
     * @return el largo de la cadena mas larga (0 si la tabla esta vacia)
     */
    public int getLargoCadenaMasLarga() {
        int maximo = 0;
        for (int i = 0; i < tabla.length; i++) {
            int largo = 0;
            NodoHash actual = tabla[i];
            while (actual != null) {
                largo++;
                actual = actual.getSiguiente();
            }
            if (largo > maximo) {
                maximo = largo;
            }
        }
        return maximo;
    }
}

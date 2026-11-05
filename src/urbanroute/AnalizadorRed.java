package urbanroute;

/**
 * Contiene los algoritmos que estudian la red para responder tres preguntas:
 * <ul>
 *   <li>Que zonas quedan aisladas (subredes / componentes conexos): busqueda en profundidad (DFS).</li>
 *   <li>Cual es la ruta mas rapida entre dos estaciones: algoritmo de Dijkstra.</li>
 *   <li>Cuales son las estaciones mas importantes (hubs): centralidad de grado.</li>
 * </ul>
 * Los algoritmos no guardan resultados entre llamadas: cada vez se calculan
 * sobre la red como esta en ese momento, asi que despues de cerrar una
 * estacion o una conexion el resultado ya refleja el cambio.
 *
 * @author Gabriel Gonzalez, Francisco Landaeta, Rafael Blanco
 */
public class AnalizadorRed {

    /** Red sobre la que se hacen los calculos. */
    private Grafo grafo;

    /** Tiempo total (en minutos) de la ultima ruta calculada. */
    private int tiempoUltimaRuta;

    /**
     * Crea un analizador para una red.
     *
     * @param grafo red a analizar
     */
    public AnalizadorRed(Grafo grafo) {
        this.grafo = grafo;
        this.tiempoUltimaRuta = 0;
    }

    /**
     * Cambia la red a analizar (se usa cuando se cargan archivos nuevos).
     *
     * @param grafo nueva red
     */
    public void setGrafo(Grafo grafo) {
        this.grafo = grafo;
    }

    // ===================================================================
    // Subredes aisladas (DFS)
    // ===================================================================

    /**
     * Identifica las subredes aisladas de la red usando busqueda en profundidad (DFS).
     * Cada subred es un grupo de estaciones conectadas entre si, pero
     * sin ningun camino hacia las estaciones de las otras subredes.
     * <p>
     * Idea: se recorren todas las estaciones; cada vez que se encuentra una que
     * todavia no fue visitada, empieza una subred nueva y se lanza un DFS desde ella,
     * que marca todas las estaciones a las que se puede llegar.
     * <p>
     * Eficiencia: O(V + E), porque cada estacion se visita una vez y cada
     * conexion se revisa una vez desde cada extremo. Las visitadas se guardan
     * en una {@link TablaHash}, asi preguntar "¿ya la visite?" cuesta O(1).
     *
     * @return arreglo de subredes, de la mas grande a la mas pequena;
     *         cada subred es un arreglo de estaciones
     */
    public Estacion[][] obtenerSubredes() {
        Estacion[] estaciones = grafo.obtenerEstaciones();
        TablaHash visitadas = new TablaHash();

        // Como maximo hay una subred por estacion (si ninguna estuviera conectada)
        Estacion[][] subredes = new Estacion[estaciones.length][];
        int cantidadSubredes = 0;

        // Arreglos de trabajo: la pila del DFS y la subred que se esta armando
        Estacion[] pila = new Estacion[estaciones.length];
        Estacion[] temporal = new Estacion[estaciones.length];

        for (int i = 0; i < estaciones.length; i++) {
            if (visitadas.buscar(estaciones[i].getId()) == null) {
                // Estacion sin visitar: empieza una subred nueva
                int tamano = recorrerDFS(estaciones[i], visitadas, pila, temporal);

                // Se copia la subred a un arreglo de su tamano exacto
                Estacion[] subred = new Estacion[tamano];
                for (int j = 0; j < tamano; j++) {
                    subred[j] = temporal[j];
                }
                subredes[cantidadSubredes] = subred;
                cantidadSubredes++;
            }
        }

        // Se deja el arreglo del tamano justo
        Estacion[][] resultado = new Estacion[cantidadSubredes][];
        for (int i = 0; i < cantidadSubredes; i++) {
            resultado[i] = subredes[i];
        }

        ordenarPorTamano(resultado);
        return resultado;
    }

    /**
     * Recorrido DFS desde una estacion, usando una <b>pila</b> propia
     * (el ultimo en entrar es el primero en salir). Como siempre se sigue
     * por la ultima estacion que se agrego, el recorrido va "lo mas profundo
     * posible" antes de volver, que es la idea del DFS.
     * <p>
     * Se usa una pila en vez de recursion para que una red muy grande no
     * desborde la memoria de llamadas de Java (StackOverflowError).
     * Cada estacion se marca como visitada al meterla en la pila, asi
     * nunca entra dos veces y la pila no necesita mas de V posiciones.
     *
     * @param inicio    estacion desde donde empieza el recorrido
     * @param visitadas tabla con las estaciones ya visitadas
     * @param pila      arreglo que se usa como pila
     * @param subred    arreglo donde se van guardando las estaciones de la subred
     * @return la cantidad de estaciones de la subred
     */
    private int recorrerDFS(Estacion inicio, TablaHash visitadas, Estacion[] pila, Estacion[] subred) {
        int tope = 0;      // cuantos elementos hay en la pila
        int cantidad = 0;  // cuantas estaciones lleva la subred

        pila[tope] = inicio;
        tope++;
        visitadas.insertar(inicio.getId(), inicio);

        while (tope > 0) {
            // Sacar de la pila la ultima estacion que entro
            tope--;
            Estacion actual = pila[tope];
            subred[cantidad] = actual;
            cantidad++;

            // Meter en la pila a las vecinas que todavia no fueron visitadas
            Conexion conexion = actual.getPrimeraConexion();
            while (conexion != null) {
                Estacion vecina = conexion.getDestino();
                if (visitadas.buscar(vecina.getId()) == null) {
                    visitadas.insertar(vecina.getId(), vecina);
                    pila[tope] = vecina;
                    tope++;
                }
                conexion = conexion.getSiguiente();
            }
        }
        return cantidad;
    }

    /**
     * Ordena las subredes de la mas grande a la mas pequena (ordenamiento por insercion).
     *
     * @param subredes subredes a ordenar
     */
    private void ordenarPorTamano(Estacion[][] subredes) {
        for (int i = 1; i < subredes.length; i++) {
            Estacion[] actual = subredes[i];
            int j = i - 1;
            while (j >= 0 && subredes[j].length < actual.length) {
                subredes[j + 1] = subredes[j];
                j--;
            }
            subredes[j + 1] = actual;
        }
    }

    /**
     * @return cuantas subredes aisladas tiene la red (1 si todo esta conectado)
     */
    public int contarSubredes() {
        return obtenerSubredes().length;
    }

    // ===================================================================
    // Ruta mas rapida (Dijkstra)
    // ===================================================================

    /**
     * Calcula la ruta de menor tiempo entre dos estaciones con el algoritmo de Dijkstra.
     * Despues de llamarlo, el tiempo total se consulta con {@link #getTiempoUltimaRuta()}.
     *
     * @param idOrigen  ID de la estacion de salida
     * @param idDestino ID de la estacion de llegada
     * @return las estaciones de la ruta en orden (origen ... destino),
     *         o null si alguna estacion no existe o no hay ninguna ruta posible
     */
    public Estacion[] calcularRutaMasCorta(String idOrigen, String idDestino) {
        // PENDIENTE: Dijkstra
        tiempoUltimaRuta = 0;
        return null;
    }

    /**
     * @return el tiempo total en minutos de la ultima ruta calculada
     */
    public int getTiempoUltimaRuta() {
        return tiempoUltimaRuta;
    }

    // ===================================================================
    // Hubs (centralidad de grado)
    // ===================================================================

    /**
     * Identifica los hubs de transferencia: las estaciones con el mayor
     * numero de conexiones directas (mayor grado).
     *
     * @return las estaciones con el grado maximo (puede haber empate);
     *         vacio si la red no tiene conexiones
     */
    public Estacion[] identificarHubs() {
        Estacion[] estaciones = grafo.obtenerEstaciones();

        // 1) Buscar el grado maximo
        int gradoMaximo = 0;
        for (int i = 0; i < estaciones.length; i++) {
            if (estaciones[i].getGrado() > gradoMaximo) {
                gradoMaximo = estaciones[i].getGrado();
            }
        }
        if (gradoMaximo == 0) {
            return new Estacion[0];
        }

        // 2) Contar cuantas lo tienen y guardarlas
        int cantidad = 0;
        for (int i = 0; i < estaciones.length; i++) {
            if (estaciones[i].getGrado() == gradoMaximo) {
                cantidad++;
            }
        }

        Estacion[] hubs = new Estacion[cantidad];
        int indice = 0;
        for (int i = 0; i < estaciones.length; i++) {
            if (estaciones[i].getGrado() == gradoMaximo) {
                hubs[indice] = estaciones[i];
                indice++;
            }
        }
        return hubs;
    }

    /**
     * Devuelve todas las estaciones ordenadas de mayor a menor grado
     * (ordenamiento por insercion). Sirve para mostrar un ranking de importancia.
     *
     * @return estaciones de la red, de mayor a menor grado
     */
    public Estacion[] obtenerRankingPorGrado() {
        Estacion[] estaciones = grafo.obtenerEstaciones();
        for (int i = 1; i < estaciones.length; i++) {
            Estacion actual = estaciones[i];
            int j = i - 1;
            while (j >= 0 && estaciones[j].getGrado() < actual.getGrado()) {
                estaciones[j + 1] = estaciones[j];
                j--;
            }
            estaciones[j + 1] = actual;
        }
        return estaciones;
    }
}

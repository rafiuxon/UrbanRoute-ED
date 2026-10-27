package urbanroute;

/**
 * Contiene los algoritmos que estudian la red para responder tres preguntas:
 * <ul>
 *   <li>Que zonas quedan aisladas (subredes / componentes conexos) - DFS.</li>
 *   <li>Cual es la ruta mas rapida entre dos estaciones - Dijkstra.</li>
 *   <li>Cuales son las estaciones mas importantes (hubs) - grado.</li>
 * </ul>
 * <b>Estado (semana 6):</b> solo esta definida la estructura de los metodos.
 * Los algoritmos se implementan en las semanas 8 y siguientes.
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
     * Cambia la red a analizar (se usa cuando se carga un archivo nuevo).
     *
     * @param grafo nueva red
     */
    public void setGrafo(Grafo grafo) {
        this.grafo = grafo;
    }

    /**
     * Identifica las subredes aisladas de la red usando busqueda en profundidad (DFS).
     * Cada subred es un grupo de estaciones conectadas entre si, pero
     * sin camino hacia las estaciones de las otras subredes.
     *
     * @return arreglo de subredes; cada subred es un arreglo de estaciones
     */
    public Estacion[][] obtenerSubredes() {
        // PENDIENTE (semana 8): recorrer todas las estaciones; por cada una
        // que no haya sido visitada, lanzar recorrerDFS y guardar la subred encontrada.
        return new Estacion[0][];
    }

    /**
     * Recorrido DFS recursivo desde una estacion. Marca como visitadas
     * (guardandolas en la tabla "visitadas") todas las estaciones alcanzables.
     *
     * @param actual    estacion donde va el recorrido
     * @param visitadas tabla con las estaciones ya visitadas
     * @param subred    arreglo donde se van guardando las estaciones de la subred
     * @param cantidad  cuantas estaciones lleva guardadas la subred
     * @return la nueva cantidad de estaciones en la subred
     */
    private int recorrerDFS(Estacion actual, TablaHash visitadas, Estacion[] subred, int cantidad) {
        // PENDIENTE (semana 8)
        return cantidad;
    }

    /**
     * @return cuantas subredes aisladas tiene la red
     */
    public int contarSubredes() {
        return obtenerSubredes().length;
    }

    /**
     * Calcula la ruta de menor tiempo entre dos estaciones con el algoritmo de Dijkstra.
     * Despues de llamarlo, el tiempo total se consulta con {@link #getTiempoUltimaRuta()}.
     *
     * @param idOrigen  ID de la estacion de salida
     * @param idDestino ID de la estacion de llegada
     * @return las estaciones de la ruta en orden (origen ... destino),
     *         o null si no existe ninguna ruta posible
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

    /**
     * Identifica los hubs de transferencia: las estaciones con el mayor
     * numero de conexiones directas (mayor grado).
     *
     * @return las estaciones con el grado maximo (puede haber empate)
     */
    public Estacion[] identificarHubs() {
        // PENDIENTE: buscar el grado maximo y devolver las estaciones que lo tienen
        return new Estacion[0];
    }
}

package urbanroute;

/**
 * Dibuja la red en pantalla usando la libreria GraphStream.
 * Solo sabe mostrar; no calcula nada por si misma
 * (los calculos los hace {@link AnalizadorRed}).
 * <p>
 * <b>Estado (semana 6):</b> solo esta definida la estructura de los metodos.
 * Para implementarla hay que agregar los .jar de GraphStream al proyecto
 * (gs-core y gs-ui-swing) y copiarlos dentro de la carpeta del proyecto.
 *
 * @author Gabriel Gonzalez, Francisco Landaeta, Rafael Blanco
 */
public class VisualizadorGrafo {

    // PENDIENTE: cuando se agregue GraphStream, aqui ira el grafo visual:
    // private org.graphstream.graph.Graph grafoVisual;

    /**
     * Crea el visualizador.
     */
    public VisualizadorGrafo() {
        // PENDIENTE: crear el grafo de GraphStream y su hoja de estilos
    }

    /**
     * Dibuja todas las estaciones y conexiones de la red, con sus tiempos.
     *
     * @param grafo red a dibujar
     */
    public void mostrarRed(Grafo grafo) {
        // PENDIENTE: agregar un nodo por cada Estacion y una arista por cada Conexion
    }

    /**
     * Pinta cada subred aislada con un color distinto.
     *
     * @param subredes subredes calculadas por {@link AnalizadorRed#obtenerSubredes()}
     */
    public void colorearSubredes(Estacion[][] subredes) {
        // PENDIENTE
    }

    /**
     * Resalta en el dibujo la ruta mas rapida encontrada.
     *
     * @param ruta estaciones de la ruta en orden
     */
    public void resaltarRuta(Estacion[] ruta) {
        // PENDIENTE
    }

    /**
     * Vuelve a dibujar la red despues de un cambio (agregar o eliminar).
     *
     * @param grafo red actualizada
     */
    public void actualizar(Grafo grafo) {
        // PENDIENTE: borrar el dibujo anterior y llamar a mostrarRed(grafo)
    }
}

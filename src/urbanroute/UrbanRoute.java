package urbanroute;

/**
 * Clase principal del programa UrbanRoute.
 * Abre la ventana y carga los dos archivos maestros de la red
 * (estaciones y conexiones).
 *
 * @author Gabriel Gonzalez, Francisco Landaeta, Rafael Blanco
 */
public class UrbanRoute {

    /**
     * Punto de entrada del programa.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        // Swing pide que las ventanas se creen en su propio hilo (EventQueue)
        java.awt.EventQueue.invokeLater(() -> {
            VentanaPrincipal ventana = new VentanaPrincipal();
            ventana.setVisible(true);
            ventana.cargarArchivosMaestros();
        });
    }
}

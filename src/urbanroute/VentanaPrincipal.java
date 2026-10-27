package urbanroute;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Ventana principal con la que interactua el usuario.
 * Recibe sus solicitudes, las delega a la clase que corresponde
 * ({@link GestorArchivo}, {@link Grafo}, {@link AnalizadorRed}, {@link VisualizadorGrafo})
 * y muestra los resultados. No hace calculos ni guarda datos de la red.
 *
 * @author Gabriel Gonzalez, Francisco Landaeta, Rafael Blanco
 */
public class VentanaPrincipal extends JFrame {

    /** Color principal de la interfaz. */
    private static final Color COLOR_PRINCIPAL = new Color(0, 82, 147);

    /** Red que esta en memoria. */
    private Grafo grafo;

    /** Lee y guarda el archivo CSV. */
    private GestorArchivo gestorArchivo;

    /** Algoritmos de analisis de la red. */
    private AnalizadorRed analizador;

    /** Dibujo de la red con GraphStream. */
    private VisualizadorGrafo visualizador;

    /** true si la red cambio desde la ultima vez que se guardo. */
    private boolean hayCambiosSinGuardar;

    /** Area donde se muestra la lista de estaciones y conexiones. */
    private JTextArea areaRed;

    /** Barra inferior con el archivo actual y el resumen de la red. */
    private JLabel etiquetaEstado;

    /**
     * Crea la ventana con una red vacia.
     */
    public VentanaPrincipal() {
        grafo = new Grafo();
        gestorArchivo = new GestorArchivo();
        analizador = new AnalizadorRed(grafo);
        visualizador = new VisualizadorGrafo();
        hayCambiosSinGuardar = false;

        construirInterfaz();
        mostrarRed();
    }

    // ===================================================================
    // Construccion de la interfaz
    // ===================================================================

    /**
     * Crea y acomoda todos los componentes de la ventana.
     */
    private void construirInterfaz() {
        setTitle("UrbanRoute - Red de Transporte Multimodal");
        setSize(950, 600);
        setMinimumSize(new Dimension(750, 450));
        setLocationRelativeTo(null); // centrar en pantalla

        // Al cerrar, primero se pregunta si hay cambios sin guardar
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salir();
            }
        });

        setLayout(new BorderLayout(10, 10));
        add(crearPanelSuperior(), BorderLayout.NORTH);
        add(crearPanelBotones(), BorderLayout.WEST);
        add(crearPanelCentral(), BorderLayout.CENTER);

        etiquetaEstado = new JLabel();
        etiquetaEstado.setBorder(BorderFactory.createEmptyBorder(5, 10, 8, 10));
        add(etiquetaEstado, BorderLayout.SOUTH);
    }

    /**
     * @return panel con el titulo y las instrucciones de uso
     */
    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBackground(COLOR_PRINCIPAL);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel titulo = new JLabel("UrbanRoute");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 22));
        titulo.setForeground(Color.WHITE);

        JLabel instrucciones = new JLabel("Cargue un archivo .csv con el formato "
                + "Estacion_Origen,Estacion_Destino,Tiempo_Minutos y use los botones de la izquierda.");
        instrucciones.setForeground(Color.WHITE);

        panel.add(titulo);
        panel.add(instrucciones);
        return panel;
    }

    /**
     * @return panel con los botones de todas las funciones
     */
    private JPanel crearPanelBotones() {
        JPanel botones = new JPanel(new GridLayout(0, 1, 6, 6)); // 0 filas = las que hagan falta

        JButton botonCargar = crearBoton("Cargar archivo", "Elegir un archivo .csv con la red");
        JButton botonGuardar = crearBoton("Guardar cambios", "Escribir la red en memoria en el archivo");
        JButton botonAgregar = crearBoton("Agregar conexión", "Conectar dos estaciones (o crear una nueva)");
        JButton botonEliminar = crearBoton("Eliminar estación", "Simular el cierre de una estación");
        JButton botonVer = crearBoton("Ver grafo", "Dibujo de la red");
        JButton botonSubredes = crearBoton("Subredes aisladas", "Zonas que quedan incomunicadas (DFS)");
        JButton botonRuta = crearBoton("Ruta más rápida", "Ruta de menor tiempo (Dijkstra)");
        JButton botonHubs = crearBoton("Hubs", "Estaciones con más conexiones");

        botonCargar.addActionListener(e -> cargarArchivo());
        botonGuardar.addActionListener(e -> guardarCambios());
        botonAgregar.addActionListener(e -> agregarConexion());
        botonEliminar.addActionListener(e -> eliminarEstacion());
        botonVer.addActionListener(e -> funcionPendiente("Ver grafo"));
        botonSubredes.addActionListener(e -> funcionPendiente("Subredes aisladas"));
        botonRuta.addActionListener(e -> funcionPendiente("Ruta más rápida"));
        botonHubs.addActionListener(e -> funcionPendiente("Hubs"));

        botones.add(botonCargar);
        botones.add(botonGuardar);
        botones.add(botonAgregar);
        botones.add(botonEliminar);
        botones.add(botonVer);
        botones.add(botonSubredes);
        botones.add(botonRuta);
        botones.add(botonHubs);

        // Se pone dentro de otro panel para que los botones no se estiren hasta abajo
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        contenedor.add(botones, BorderLayout.NORTH);
        return contenedor;
    }

    /**
     * Crea un boton con el estilo de la aplicacion.
     *
     * @param texto texto del boton
     * @param ayuda mensaje que aparece al pasar el mouse por encima
     * @return el boton creado
     */
    private JButton crearBoton(String texto, String ayuda) {
        JButton boton = new JButton(texto);
        boton.setToolTipText(ayuda);
        boton.setFocusPainted(false);
        boton.setPreferredSize(new Dimension(170, 34));
        return boton;
    }

    /**
     * @return panel central con la lista de estaciones y conexiones
     */
    private JScrollPane crearPanelCentral() {
        areaRed = new JTextArea();
        areaRed.setEditable(false);
        areaRed.setFont(new Font("Monospaced", Font.PLAIN, 13));
        areaRed.setMargin(new java.awt.Insets(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(areaRed);
        scroll.setBorder(BorderFactory.createTitledBorder("Red en memoria (estación, grado y conexiones)"));
        return scroll;
    }

    // ===================================================================
    // Carga y guardado del archivo
    // ===================================================================

    /**
     * Carga el archivo maestro. Se llama al iniciar el programa.
     */
    public void cargarArchivoMaestro() {
        File maestro = new File(GestorArchivo.RUTA_ARCHIVO_MAESTRO);
        if (!maestro.isFile()) {
            JOptionPane.showMessageDialog(this,
                    "No se encontró el archivo maestro en:\n" + maestro.getAbsolutePath()
                    + "\n\nUse el botón \"Cargar archivo\" para elegir uno.",
                    "Archivo maestro", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cargarDesde(maestro);
    }

    /**
     * Pide al usuario un archivo con JFileChooser y lo carga.
     * Antes avisa que los datos en memoria se van a reemplazar.
     */
    private void cargarArchivo() {
        if (!grafo.estaVacio()) {
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "Al cargar un archivo nuevo se reemplazarán los datos que están en memoria.\n"
                    + "¿Desea guardarlos antes de continuar?",
                    "Guardar datos actuales", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

            if (respuesta == JOptionPane.YES_OPTION) {
                if (!guardarCambios()) {
                    return; // no se pudo guardar: no se carga nada
                }
            } else if (respuesta != JOptionPane.NO_OPTION) {
                return; // Cancelar o cerro el dialogo
            }
        }

        JFileChooser selector = new JFileChooser(new File("datos"));
        selector.setDialogTitle("Seleccione el archivo de la red");
        selector.setFileFilter(new FileNameExtensionFilter("Archivos CSV o de texto (*.csv, *.txt)", "csv", "txt"));

        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            cargarDesde(selector.getSelectedFile());
        }
    }

    /**
     * Carga un archivo y, si todo sale bien, reemplaza la red en memoria.
     *
     * @param archivo archivo a cargar
     */
    private void cargarDesde(File archivo) {
        try {
            grafo = gestorArchivo.cargar(archivo);
            analizador.setGrafo(grafo);
            hayCambiosSinGuardar = false;
            mostrarRed();

            if (gestorArchivo.hayErrores()) {
                mostrarTextoLargo("Se cargó el archivo, pero " + gestorArchivo.getCantidadErrores()
                        + " línea(s) tenían errores y se ignoraron:\n\n" + gestorArchivo.getErrores(),
                        "Advertencias de carga", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException ex) {
            mostrarTextoLargo("No se pudo cargar el archivo " + archivo.getName() + ".\n\n" + ex.getMessage()
                    + "\n\nSe conservaron los datos que estaban en memoria.",
                    "Error al cargar", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Guarda la red en memoria en el archivo actual. Si todavia no hay
     * archivo, le pide al usuario donde guardarlo.
     *
     * @return true si se guardo correctamente
     */
    private boolean guardarCambios() {
        if (grafo.estaVacio()) {
            JOptionPane.showMessageDialog(this, "No hay datos en memoria para guardar.",
                    "Guardar", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        File destino = gestorArchivo.getArchivoActual();
        if (destino == null) {
            JFileChooser selector = new JFileChooser(new File("datos"));
            selector.setDialogTitle("Guardar la red como");
            if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return false;
            }
            destino = selector.getSelectedFile();
            if (!destino.getName().toLowerCase().endsWith(".csv")) {
                destino = new File(destino.getPath() + ".csv");
            }
        }

        try {
            gestorArchivo.guardar(grafo, destino);
            hayCambiosSinGuardar = false;
            actualizarEstado();
            JOptionPane.showMessageDialog(this, "Los cambios se guardaron en " + destino.getName() + ".",
                    "Guardar", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el archivo:\n" + ex.getMessage(),
                    "Error al guardar", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // ===================================================================
    // Modificacion de la red
    // ===================================================================

    /**
     * Pide dos estaciones y un tiempo, y las conecta.
     * Si alguna estacion no existe, se ofrece crearla (asi se agrega una estacion nueva).
     */
    private void agregarConexion() {
        JTextField campoOrigen = new JTextField(10);
        JTextField campoDestino = new JTextField(10);
        JTextField campoTiempo = new JTextField(10);

        JPanel formulario = new JPanel(new GridLayout(3, 2, 5, 5));
        formulario.add(new JLabel("Estación origen (ID):"));
        formulario.add(campoOrigen);
        formulario.add(new JLabel("Estación destino (ID):"));
        formulario.add(campoDestino);
        formulario.add(new JLabel("Tiempo (minutos):"));
        formulario.add(campoTiempo);

        int respuesta = JOptionPane.showConfirmDialog(this, formulario, "Agregar conexión",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (respuesta != JOptionPane.OK_OPTION) {
            return;
        }

        String idOrigen = campoOrigen.getText().trim();
        String idDestino = campoDestino.getText().trim();
        String textoTiempo = campoTiempo.getText().trim();

        // ---- Validaciones ----
        if (idOrigen.isEmpty() || idDestino.isEmpty() || textoTiempo.isEmpty()) {
            mostrarError("Debe llenar los tres campos.");
            return;
        }
        if (idOrigen.contains(",") || idDestino.contains(",")) {
            mostrarError("Los ID de las estaciones no pueden tener comas.");
            return;
        }
        if (idOrigen.equals(idDestino)) {
            mostrarError("Una estación no puede conectarse consigo misma.");
            return;
        }

        int tiempo;
        try {
            tiempo = Integer.parseInt(textoTiempo);
        } catch (NumberFormatException ex) {
            mostrarError("El tiempo debe ser un número entero de minutos (por ejemplo: 3).");
            return;
        }
        if (tiempo <= 0) {
            mostrarError("El tiempo debe ser mayor que cero.");
            return;
        }

        // ---- Estaciones nuevas ----
        String estacionesNuevas = "";
        if (!grafo.existeEstacion(idOrigen)) {
            estacionesNuevas += "   " + idOrigen + "\n";
        }
        if (!grafo.existeEstacion(idDestino)) {
            estacionesNuevas += "   " + idDestino + "\n";
        }
        if (!estacionesNuevas.isEmpty()) {
            int confirmar = JOptionPane.showConfirmDialog(this,
                    "Estas estaciones no existen y se crearán como estaciones nuevas:\n"
                    + estacionesNuevas + "\n¿Desea continuar?",
                    "Estación nueva", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirmar != JOptionPane.YES_OPTION) {
                return;
            }
            grafo.agregarEstacion(idOrigen);   // si ya existe, no hace nada
            grafo.agregarEstacion(idDestino);
        }

        // ---- Conectar ----
        if (grafo.conectar(idOrigen, idDestino, tiempo)) {
            hayCambiosSinGuardar = true;
            mostrarRed();
            JOptionPane.showMessageDialog(this,
                    "Conexión agregada: " + idOrigen + " <-> " + idDestino + " (" + tiempo + " min).",
                    "Agregar conexión", JOptionPane.INFORMATION_MESSAGE);
        } else {
            mostrarError("Las estaciones " + idOrigen + " y " + idDestino + " ya están conectadas.");
        }
    }

    /**
     * Pide una estacion de la lista y la elimina junto con sus conexiones
     * (simula el cierre de la estacion).
     */
    private void eliminarEstacion() {
        if (grafo.estaVacio()) {
            mostrarError("No hay estaciones cargadas.");
            return;
        }

        Estacion[] estaciones = grafo.obtenerEstaciones();
        String[] ids = new String[estaciones.length];
        for (int i = 0; i < estaciones.length; i++) {
            ids[i] = estaciones[i].getId();
        }

        String elegida = (String) JOptionPane.showInputDialog(this,
                "Seleccione la estación que desea cerrar:", "Eliminar estación",
                JOptionPane.QUESTION_MESSAGE, null, ids, ids[0]);
        if (elegida == null) {
            return; // el usuario cancelo
        }

        int grado = grafo.buscarEstacion(elegida).getGrado();
        int confirmar = JOptionPane.showConfirmDialog(this,
                "¿Seguro que desea eliminar la estación " + elegida + "?\n"
                + "También se eliminarán sus " + grado + " conexión(es).",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirmar == JOptionPane.YES_OPTION) {
            grafo.eliminarEstacion(elegida);
            hayCambiosSinGuardar = true;
            mostrarRed();
        }
    }

    // ===================================================================
    // Funciones pendientes y mensajes
    // ===================================================================

    /**
     * Avisa que una funcion todavia no esta disponible.
     *
     * @param nombre nombre de la funcion
     */
    private void funcionPendiente(String nombre) {
        JOptionPane.showMessageDialog(this,
                "La función \"" + nombre + "\" estará disponible en una próxima versión.",
                "En construcción", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Muestra la red en memoria en el area central y actualiza la barra de estado.
     */
    private void mostrarRed() {
        if (grafo.estaVacio()) {
            areaRed.setText("No hay ninguna red cargada.\n\n"
                    + "Use el botón \"Cargar archivo\" para seleccionar un archivo .csv.");
            actualizarEstado();
            return;
        }

        StringBuilder texto = new StringBuilder();
        Estacion[] estaciones = grafo.obtenerEstaciones();

        for (int i = 0; i < estaciones.length; i++) {
            Estacion estacion = estaciones[i];
            texto.append(String.format("%-10s grado %d:  ", estacion.getId(), estacion.getGrado()));

            Conexion actual = estacion.getPrimeraConexion();
            if (actual == null) {
                texto.append("(sin conexiones)");
            }
            while (actual != null) {
                texto.append(actual.getDestino().getId()).append(" (").append(actual.getTiempo()).append(" min)");
                if (actual.getSiguiente() != null) {
                    texto.append(", ");
                }
                actual = actual.getSiguiente();
            }
            texto.append("\n");
        }

        areaRed.setText(texto.toString());
        areaRed.setCaretPosition(0); // volver al inicio del texto
        actualizarEstado();
    }

    /**
     * Actualiza la barra inferior con el archivo actual y el resumen de la red.
     */
    private void actualizarEstado() {
        String nombreArchivo = "ninguno";
        if (gestorArchivo.getArchivoActual() != null) {
            nombreArchivo = gestorArchivo.getArchivoActual().getName();
        }

        String texto = "Archivo: " + nombreArchivo
                + "     |     Estaciones: " + grafo.getNumEstaciones()
                + "     |     Conexiones: " + grafo.getNumConexiones();
        if (hayCambiosSinGuardar) {
            texto += "     |     Hay cambios sin guardar";
        }
        etiquetaEstado.setText(texto);
    }

    /**
     * Muestra un mensaje de error sencillo.
     *
     * @param mensaje texto del error
     */
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Dato no válido", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Muestra un mensaje largo dentro de un area con barra de desplazamiento.
     *
     * @param mensaje texto a mostrar
     * @param titulo  titulo de la ventana
     * @param tipo    tipo de mensaje de JOptionPane (error, advertencia...)
     */
    private void mostrarTextoLargo(String mensaje, String titulo, int tipo) {
        JTextArea area = new JTextArea(mensaje);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(480, 220));
        JOptionPane.showMessageDialog(this, scroll, titulo, tipo);
    }

    /**
     * Cierra el programa. Si hay cambios sin guardar, pregunta antes.
     */
    private void salir() {
        if (hayCambiosSinGuardar) {
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "Hay cambios sin guardar. ¿Desea guardarlos antes de salir?",
                    "Salir", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

            if (respuesta == JOptionPane.YES_OPTION) {
                if (!guardarCambios()) {
                    return;
                }
            } else if (respuesta != JOptionPane.NO_OPTION) {
                return;
            }
        }
        dispose();
        System.exit(0);
    }
}

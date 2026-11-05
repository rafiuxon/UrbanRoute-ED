package urbanroute;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;

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

    /** Titulos de las columnas de la tabla de estaciones. */
    private static final String[] COLUMNAS = {
        "ID", "Nombre", "Línea", "Zona", "Capacidad (pas/h)", "Grado", "Conexiones (minutos)"
    };

    /** Red que esta en memoria. */
    private Grafo grafo;

    /** Lee y guarda los archivos CSV. */
    private GestorArchivo gestorArchivo;

    /** Algoritmos de analisis de la red. */
    private AnalizadorRed analizador;

    /** Dibujo de la red con GraphStream. */
    private VisualizadorGrafo visualizador;

    /** true si la red cambio desde la ultima vez que se guardo. */
    private boolean hayCambiosSinGuardar;

    /** Estaciones que se muestran en la tabla, ordenadas por ID. */
    private Estacion[] estacionesTabla;

    /** Modelo de la tabla: le dice a la JTable que mostrar en cada celda. */
    private AbstractTableModel modeloTabla;

    /** Tabla con los datos de cada estacion. */
    private JTable tablaEstaciones;

    /** Campo para buscar una estacion por su ID. */
    private JTextField campoBuscar;

    /** Barra inferior con los archivos actuales y el resumen de la red. */
    private JLabel etiquetaEstado;

    /** IDs de las conexiones que se van armando en el formulario "Agregar estación". */
    private String[] idsPendientes;

    /** Tiempos de las conexiones que se van armando en el formulario "Agregar estación". */
    private int[] tiemposPendientes;

    /** Cuantas conexiones lleva el formulario "Agregar estación". */
    private int cantidadPendientes;

    /**
     * Crea la ventana con una red vacia.
     */
    public VentanaPrincipal() {
        grafo = new Grafo();
        gestorArchivo = new GestorArchivo();
        analizador = new AnalizadorRed(grafo);
        visualizador = new VisualizadorGrafo();
        hayCambiosSinGuardar = false;
        estacionesTabla = new Estacion[0];

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
        setSize(1150, 650);
        setMinimumSize(new Dimension(900, 500));
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

        JLabel instrucciones = new JLabel("La red se carga desde dos archivos .csv: uno de estaciones y otro de "
                + "conexiones. Use los botones de la izquierda para modificarla y guardarla.");
        instrucciones.setForeground(Color.WHITE);

        panel.add(titulo);
        panel.add(instrucciones);
        return panel;
    }

    /**
     * @return panel con los botones de todas las funciones, agrupados por tema
     */
    private JPanel crearPanelBotones() {
        JPanel botones = new JPanel(new GridLayout(0, 1, 6, 6)); // 0 filas = las que hagan falta

        JButton botonCargar = crearBoton("Cargar archivos", "Elegir el archivo de estaciones y el de conexiones");
        JButton botonGuardar = crearBoton("Guardar cambios", "Escribir la red en memoria en los dos archivos");
        JButton botonAgregarEst = crearBoton("Agregar estación", "Crear una estación nueva con sus conexiones");
        JButton botonAgregarCon = crearBoton("Agregar conexión", "Conectar dos estaciones que ya existen");
        JButton botonEliminarEst = crearBoton("Eliminar estación", "Simular el cierre de una estación");
        JButton botonEliminarCon = crearBoton("Eliminar conexión", "Quitar el tramo entre dos estaciones");
        JButton botonVer = crearBoton("Ver grafo", "Dibujo de la red");
        JButton botonSubredes = crearBoton("Subredes aisladas", "Zonas que quedan incomunicadas (DFS)");
        JButton botonRuta = crearBoton("Ruta más rápida", "Ruta de menor tiempo (Dijkstra)");
        JButton botonHubs = crearBoton("Hubs", "Estaciones con más conexiones");

        botonCargar.addActionListener(e -> cargarArchivos());
        botonGuardar.addActionListener(e -> guardarCambios());
        botonAgregarEst.addActionListener(e -> agregarEstacion());
        botonAgregarCon.addActionListener(e -> agregarConexion());
        botonEliminarEst.addActionListener(e -> eliminarEstacion());
        botonEliminarCon.addActionListener(e -> eliminarConexion());
        botonVer.addActionListener(e -> funcionPendiente("Ver grafo"));
        botonSubredes.addActionListener(e -> funcionPendiente("Subredes aisladas"));
        botonRuta.addActionListener(e -> funcionPendiente("Ruta más rápida"));
        botonHubs.addActionListener(e -> funcionPendiente("Hubs"));

        botones.add(crearTituloGrupo("Archivos"));
        botones.add(botonCargar);
        botones.add(botonGuardar);
        botones.add(crearTituloGrupo("Modificar la red"));
        botones.add(botonAgregarEst);
        botones.add(botonAgregarCon);
        botones.add(botonEliminarEst);
        botones.add(botonEliminarCon);
        botones.add(crearTituloGrupo("Análisis"));
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
        boton.setPreferredSize(new Dimension(175, 32));
        return boton;
    }

    /**
     * Crea el titulo de un grupo de botones.
     *
     * @param texto titulo del grupo
     * @return la etiqueta creada
     */
    private JLabel crearTituloGrupo(String texto) {
        JLabel etiqueta = new JLabel(texto.toUpperCase());
        etiqueta.setFont(new Font("SansSerif", Font.BOLD, 11));
        etiqueta.setForeground(COLOR_PRINCIPAL);
        return etiqueta;
    }

    /**
     * @return panel central con el buscador y la tabla de estaciones
     */
    private JPanel crearPanelCentral() {
        // ---- Buscador por ID (usa la tabla hash) ----
        JPanel buscador = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        campoBuscar = new JTextField(12);
        JButton botonBuscar = new JButton("Buscar");
        botonBuscar.addActionListener(e -> buscarEstacion());
        campoBuscar.addActionListener(e -> buscarEstacion()); // tambien con la tecla Enter
        buscador.add(new JLabel("Buscar estación por ID:"));
        buscador.add(campoBuscar);
        buscador.add(botonBuscar);

        // ---- Tabla de estaciones ----
        // El modelo lee los datos directamente del arreglo estacionesTabla
        modeloTabla = new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return estacionesTabla.length;
            }

            @Override
            public int getColumnCount() {
                return COLUMNAS.length;
            }

            @Override
            public String getColumnName(int columna) {
                return COLUMNAS[columna];
            }

            @Override
            public Object getValueAt(int fila, int columna) {
                Estacion e = estacionesTabla[fila];
                switch (columna) {
                    case 0:
                        return e.getId();
                    case 1:
                        return e.getNombre();
                    case 2:
                        return e.getLinea();
                    case 3:
                        return e.getZona();
                    case 4:
                        return e.getCapacidad();
                    case 5:
                        return e.getGrado();
                    default:
                        return textoConexiones(e);
                }
            }
        };

        tablaEstaciones = new JTable(modeloTabla);
        tablaEstaciones.setRowHeight(22);
        tablaEstaciones.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaEstaciones.getTableHeader().setReorderingAllowed(false);
        tablaEstaciones.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        int[] anchos = {80, 160, 70, 120, 110, 50, 300};
        for (int i = 0; i < anchos.length; i++) {
            tablaEstaciones.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        JScrollPane scroll = new JScrollPane(tablaEstaciones);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 0, 10),
                BorderFactory.createTitledBorder("Estaciones en memoria")));
        panel.add(buscador, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ===================================================================
    // Carga y guardado de los archivos
    // ===================================================================

    /**
     * Carga los dos archivos maestros. Se llama al iniciar el programa.
     */
    public void cargarArchivosMaestros() {
        File estaciones = new File(GestorArchivo.RUTA_ESTACIONES_MAESTRO);
        File conexiones = new File(GestorArchivo.RUTA_CONEXIONES_MAESTRO);

        if (!estaciones.isFile() || !conexiones.isFile()) {
            String faltan = "";
            if (!estaciones.isFile()) {
                faltan += "   " + estaciones.getAbsolutePath() + "\n";
            }
            if (!conexiones.isFile()) {
                faltan += "   " + conexiones.getAbsolutePath() + "\n";
            }
            JOptionPane.showMessageDialog(this,
                    "No se encontraron los archivos maestros:\n" + faltan
                    + "\nUse el botón \"Cargar archivos\" para elegirlos.",
                    "Archivos maestros", JOptionPane.WARNING_MESSAGE);
            return;
        }
        cargarDesde(estaciones, conexiones);
    }

    /**
     * Pide al usuario los dos archivos con JFileChooser (primero estaciones,
     * despues conexiones) y los carga. Antes avisa que los datos en memoria
     * se van a reemplazar.
     */
    private void cargarArchivos() {
        if (!grafo.estaVacio()) {
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "Al cargar archivos nuevos se reemplazarán los datos que están en memoria.\n"
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

        File estaciones = elegirArchivoParaAbrir(new File("datos"),
                "Paso 1 de 2: seleccione el archivo de ESTACIONES");
        if (estaciones == null) {
            return;
        }

        File conexiones = elegirArchivoParaAbrir(estaciones.getParentFile(),
                "Paso 2 de 2: seleccione el archivo de CONEXIONES");
        if (conexiones == null) {
            return;
        }

        cargarDesde(estaciones, conexiones);
    }

    /**
     * Muestra un JFileChooser para abrir un archivo .csv o .txt.
     *
     * @param carpeta carpeta en la que se abre el selector
     * @param titulo  titulo del selector
     * @return el archivo elegido, o null si el usuario cancelo
     */
    private File elegirArchivoParaAbrir(File carpeta, String titulo) {
        JFileChooser selector = new JFileChooser(carpeta);
        selector.setDialogTitle(titulo);
        selector.setFileFilter(new FileNameExtensionFilter("Archivos CSV o de texto (*.csv, *.txt)", "csv", "txt"));

        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return selector.getSelectedFile();
        }
        return null;
    }

    /**
     * Carga los dos archivos y, si todo sale bien, reemplaza la red en memoria.
     *
     * @param estaciones archivo de estaciones
     * @param conexiones archivo de conexiones
     */
    private void cargarDesde(File estaciones, File conexiones) {
        try {
            grafo = gestorArchivo.cargar(estaciones, conexiones);
            analizador.setGrafo(grafo);
            hayCambiosSinGuardar = false;
            mostrarRed();

            if (gestorArchivo.hayErrores()) {
                mostrarTextoLargo("Se cargaron " + gestorArchivo.getEstacionesLeidas() + " estaciones y "
                        + gestorArchivo.getConexionesLeidas() + " conexiones, pero "
                        + gestorArchivo.getCantidadErrores() + " línea(s) tenían errores y se ignoraron:\n\n"
                        + gestorArchivo.getErrores(),
                        "Advertencias de carga", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException ex) {
            mostrarTextoLargo("No se pudieron cargar los archivos.\n\n" + ex.getMessage()
                    + "\n\nSe conservaron los datos que estaban en memoria.",
                    "Error al cargar", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Guarda la red en memoria en los dos archivos actuales. Si todavia no hay
     * archivos, le pide al usuario donde guardar cada uno.
     *
     * @return true si se guardo correctamente
     */
    private boolean guardarCambios() {
        if (grafo.estaVacio()) {
            JOptionPane.showMessageDialog(this, "No hay datos en memoria para guardar.",
                    "Guardar", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        try {
            if (gestorArchivo.getArchivoEstaciones() == null || gestorArchivo.getArchivoConexiones() == null) {
                File estaciones = elegirArchivoParaGuardar("Guardar el archivo de ESTACIONES como");
                if (estaciones == null) {
                    return false;
                }
                File conexiones = elegirArchivoParaGuardar("Guardar el archivo de CONEXIONES como");
                if (conexiones == null) {
                    return false;
                }
                if (estaciones.getAbsoluteFile().equals(conexiones.getAbsoluteFile())) {
                    mostrarError("Los dos archivos deben tener nombres distintos.");
                    return false;
                }
                gestorArchivo.guardar(grafo, estaciones, conexiones);
            } else {
                gestorArchivo.guardar(grafo);
            }

            hayCambiosSinGuardar = false;
            actualizarEstado();
            JOptionPane.showMessageDialog(this, "Los cambios se guardaron en:\n   "
                    + gestorArchivo.getArchivoEstaciones().getName() + "\n   "
                    + gestorArchivo.getArchivoConexiones().getName(),
                    "Guardar", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (IOException ex) {
            mostrarError("No se pudieron guardar los archivos:\n" + ex.getMessage());
            return false;
        }
    }

    /**
     * Muestra un JFileChooser para elegir donde guardar un archivo .csv.
     *
     * @param titulo titulo del selector
     * @return el archivo elegido (terminado en .csv), o null si el usuario cancelo
     */
    private File elegirArchivoParaGuardar(String titulo) {
        JFileChooser selector = new JFileChooser(new File("datos"));
        selector.setDialogTitle(titulo);
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File archivo = selector.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".csv")) {
            archivo = new File(archivo.getPath() + ".csv");
        }
        return archivo;
    }

    // ===================================================================
    // Agregar estacion
    // ===================================================================

    /**
     * Muestra el formulario para crear una estacion nueva con sus datos
     * y sus conexiones (con los tiempos) hacia estaciones que ya existen.
     * Si algun dato esta mal, avisa y vuelve a mostrar el formulario sin perder lo escrito.
     */
    private void agregarEstacion() {
        Estacion[] existentes = grafo.obtenerEstaciones();

        // ---- Datos de la estacion ----
        JTextField campoId = new JTextField(12);
        JTextField campoNombre = new JTextField(12);
        JComboBox<String> comboLinea = new JComboBox<>(obtenerLineas(existentes));
        comboLinea.setEditable(true); // se puede elegir una linea existente o escribir una nueva
        JTextField campoZona = new JTextField(12);
        JTextField campoCapacidad = new JTextField(12);

        JPanel datos = new JPanel(new GridLayout(5, 2, 5, 5));
        datos.setBorder(BorderFactory.createTitledBorder("1. Datos de la estación"));
        datos.add(new JLabel("ID (ej. LCH):"));
        datos.add(campoId);
        datos.add(new JLabel("Nombre:"));
        datos.add(campoNombre);
        datos.add(new JLabel("Línea:"));
        datos.add(comboLinea);
        datos.add(new JLabel("Zona:"));
        datos.add(campoZona);
        datos.add(new JLabel("Capacidad (pasajeros/hora):"));
        datos.add(campoCapacidad);

        // ---- Conexiones con estaciones existentes ----
        idsPendientes = new String[existentes.length];
        tiemposPendientes = new int[existentes.length];
        cantidadPendientes = 0;

        JComboBox<String> comboVecina = new JComboBox<>(textosEstaciones(existentes));
        JTextField campoTiempo = new JTextField(5);
        JButton botonAnadir = new JButton("Añadir");
        JButton botonQuitar = new JButton("Quitar la última");
        JTextArea areaPendientes = new JTextArea(5, 30);
        areaPendientes.setEditable(false);
        areaPendientes.setText("(sin conexiones todavía)");

        botonAnadir.addActionListener(e ->
                anadirConexionPendiente(existentes, comboVecina, campoTiempo, areaPendientes));
        botonQuitar.addActionListener(e -> {
            if (cantidadPendientes > 0) {
                cantidadPendientes--;
                areaPendientes.setText(textoPendientes());
            }
        });

        JPanel filaConexion = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        filaConexion.add(new JLabel("Estación:"));
        filaConexion.add(comboVecina);
        filaConexion.add(new JLabel("Minutos:"));
        filaConexion.add(campoTiempo);
        filaConexion.add(botonAnadir);
        filaConexion.add(botonQuitar);

        JPanel conexiones = new JPanel(new BorderLayout(0, 5));
        conexiones.setBorder(BorderFactory.createTitledBorder("2. Conexiones con estaciones existentes"));
        conexiones.add(filaConexion, BorderLayout.NORTH);
        conexiones.add(new JScrollPane(areaPendientes), BorderLayout.CENTER);

        JPanel formulario = new JPanel(new BorderLayout(0, 10));
        formulario.add(datos, BorderLayout.NORTH);
        formulario.add(conexiones, BorderLayout.CENTER);

        // Se repite hasta que los datos sean validos o el usuario cancele
        while (true) {
            int respuesta = JOptionPane.showConfirmDialog(this, formulario, "Agregar estación",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (respuesta != JOptionPane.OK_OPTION) {
                return;
            }

            String id = campoId.getText().trim();
            String nombre = campoNombre.getText().trim();
            String linea = comboLinea.getEditor().getItem().toString().trim();
            String zona = campoZona.getText().trim();
            String textoCapacidad = campoCapacidad.getText().trim();

            // ---- Validaciones ----
            if (id.isEmpty() || nombre.isEmpty() || linea.isEmpty() || zona.isEmpty() || textoCapacidad.isEmpty()) {
                mostrarError("Debe llenar todos los datos de la estación.");
                continue;
            }
            if (tieneComa(id) || tieneComa(nombre) || tieneComa(linea) || tieneComa(zona)) {
                mostrarError("Los datos no pueden tener comas, porque se guardan en un archivo CSV.");
                continue;
            }
            if (grafo.existeEstacion(id)) {
                mostrarError("Ya existe una estación con el ID " + id + ".");
                continue;
            }
            int capacidad = leerEnteroPositivo(textoCapacidad, "La capacidad");
            if (capacidad == -1) {
                continue;
            }
            if (cantidadPendientes == 0) {
                int confirmar = JOptionPane.showConfirmDialog(this,
                        "La estación no tiene conexiones, así que quedará aislada del resto de la red.\n"
                        + "¿Desea agregarla de todas formas?",
                        "Estación sin conexiones", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (confirmar != JOptionPane.YES_OPTION) {
                    continue;
                }
            }

            // ---- Agregar al grafo (y a la tabla hash) ----
            Estacion nueva = new Estacion(id, nombre, linea, zona, capacidad);
            grafo.agregarEstacion(nueva);
            for (int i = 0; i < cantidadPendientes; i++) {
                grafo.conectar(id, idsPendientes[i], tiemposPendientes[i]);
            }

            hayCambiosSinGuardar = true;
            mostrarRed();
            seleccionarEnTabla(id);
            JOptionPane.showMessageDialog(this,
                    "Estación agregada: " + id + " (" + nombre + ") con " + cantidadPendientes + " conexión(es).",
                    "Agregar estación", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
    }

    /**
     * Agrega una conexion a la lista del formulario "Agregar estación",
     * despues de validar el tiempo y que no este repetida.
     *
     * @param existentes     estaciones del combo, en el mismo orden
     * @param comboVecina    combo con la estacion elegida
     * @param campoTiempo    campo con los minutos
     * @param areaPendientes area donde se muestran las conexiones agregadas
     */
    private void anadirConexionPendiente(Estacion[] existentes, JComboBox<String> comboVecina,
            JTextField campoTiempo, JTextArea areaPendientes) {
        if (existentes.length == 0) {
            mostrarError("No hay estaciones en la red con las cuales conectar.");
            return;
        }

        int tiempo = leerEnteroPositivo(campoTiempo.getText().trim(), "El tiempo");
        if (tiempo == -1) {
            return;
        }

        String idVecina = existentes[comboVecina.getSelectedIndex()].getId();
        for (int i = 0; i < cantidadPendientes; i++) {
            if (idsPendientes[i].equals(idVecina)) {
                mostrarError("Ya agregó una conexión con " + idVecina + ".");
                return;
            }
        }

        idsPendientes[cantidadPendientes] = idVecina;
        tiemposPendientes[cantidadPendientes] = tiempo;
        cantidadPendientes++;
        areaPendientes.setText(textoPendientes());
        campoTiempo.setText("");
    }

    /**
     * @return las conexiones del formulario "Agregar estación", una por linea
     */
    private String textoPendientes() {
        if (cantidadPendientes == 0) {
            return "(sin conexiones todavía)";
        }
        String texto = "";
        for (int i = 0; i < cantidadPendientes; i++) {
            Estacion vecina = grafo.buscarEstacion(idsPendientes[i]);
            texto += (i + 1) + ". " + vecina.getId() + " - " + vecina.getNombre()
                    + "  (" + tiemposPendientes[i] + " min)\n";
        }
        return texto;
    }

    // ===================================================================
    // Agregar conexion
    // ===================================================================

    /**
     * Muestra el formulario para conectar dos estaciones que ya existen.
     * Si algun dato esta mal, avisa y vuelve a mostrar el formulario sin perder lo elegido.
     */
    private void agregarConexion() {
        Estacion[] existentes = grafo.obtenerEstaciones();
        if (existentes.length < 2) {
            mostrarError("Se necesitan al menos dos estaciones para crear una conexión.\n"
                    + "Use \"Agregar estación\" para crear estaciones nuevas.");
            return;
        }

        JComboBox<String> comboOrigen = new JComboBox<>(textosEstaciones(existentes));
        JComboBox<String> comboDestino = new JComboBox<>(textosEstaciones(existentes));
        comboDestino.setSelectedIndex(1);
        JTextField campoTiempo = new JTextField(10);

        JPanel formulario = new JPanel(new GridLayout(3, 2, 5, 5));
        formulario.add(new JLabel("Estación origen:"));
        formulario.add(comboOrigen);
        formulario.add(new JLabel("Estación destino:"));
        formulario.add(comboDestino);
        formulario.add(new JLabel("Tiempo (minutos):"));
        formulario.add(campoTiempo);

        // Se repite hasta que los datos sean validos o el usuario cancele
        while (true) {
            int respuesta = JOptionPane.showConfirmDialog(this, formulario, "Agregar conexión",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (respuesta != JOptionPane.OK_OPTION) {
                return;
            }

            Estacion origen = existentes[comboOrigen.getSelectedIndex()];
            Estacion destino = existentes[comboDestino.getSelectedIndex()];

            // ---- Validaciones ----
            if (origen == destino) {
                mostrarError("Una estación no puede conectarse consigo misma.");
                continue;
            }
            if (origen.buscarConexion(destino.getId()) != null) {
                mostrarError("Las estaciones " + origen.getId() + " y " + destino.getId() + " ya están conectadas.");
                continue;
            }
            int tiempo = leerEnteroPositivo(campoTiempo.getText().trim(), "El tiempo");
            if (tiempo == -1) {
                continue;
            }

            // ---- Conectar ----
            grafo.conectar(origen.getId(), destino.getId(), tiempo);
            hayCambiosSinGuardar = true;
            mostrarRed();
            seleccionarEnTabla(origen.getId());
            JOptionPane.showMessageDialog(this,
                    "Conexión agregada: " + origen.getId() + " <-> " + destino.getId() + " (" + tiempo + " min).",
                    "Agregar conexión", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
    }

    // ===================================================================
    // Eliminar estacion y conexion
    // ===================================================================

    /**
     * Pide una estacion de la lista y la elimina junto con sus conexiones
     * (simula su cierre).
     */
    private void eliminarEstacion() {
        Estacion[] existentes = grafo.obtenerEstaciones();
        if (existentes.length == 0) {
            mostrarError("No hay estaciones cargadas.");
            return;
        }

        JComboBox<String> comboEstacion = new JComboBox<>(textosEstaciones(existentes));
        JPanel formulario = new JPanel(new GridLayout(2, 1, 5, 5));
        formulario.add(new JLabel("Seleccione la estación que desea cerrar:"));
        formulario.add(comboEstacion);

        int respuesta = JOptionPane.showConfirmDialog(this, formulario, "Eliminar estación",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (respuesta != JOptionPane.OK_OPTION) {
            return;
        }

        Estacion elegida = existentes[comboEstacion.getSelectedIndex()];
        int confirmar = JOptionPane.showConfirmDialog(this,
                "¿Seguro que desea eliminar la estación " + elegida.getId() + " (" + elegida.getNombre() + ")?\n"
                + "También se eliminarán sus " + elegida.getGrado() + " conexión(es).",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirmar == JOptionPane.YES_OPTION) {
            grafo.eliminarEstacion(elegida.getId());
            hayCambiosSinGuardar = true;
            mostrarRed();
        }
    }

    /**
     * Pide una estacion y una de sus vecinas, y elimina el tramo entre ellas.
     */
    private void eliminarConexion() {
        // Solo se ofrecen las estaciones que tienen al menos una conexion
        Estacion[] todas = grafo.obtenerEstaciones();
        int conConexiones = 0;
        for (int i = 0; i < todas.length; i++) {
            if (todas[i].getGrado() > 0) {
                conConexiones++;
            }
        }
        if (conConexiones == 0) {
            mostrarError("No hay conexiones para eliminar.");
            return;
        }

        Estacion[] estaciones = new Estacion[conConexiones];
        int indice = 0;
        for (int i = 0; i < todas.length; i++) {
            if (todas[i].getGrado() > 0) {
                estaciones[indice] = todas[i];
                indice++;
            }
        }

        JComboBox<String> comboEstacion = new JComboBox<>(textosEstaciones(estaciones));
        JComboBox<String> comboVecina = new JComboBox<>();
        llenarVecinas(estaciones[0], comboVecina);
        // Cuando cambia la estacion, se actualiza la lista de sus vecinas
        comboEstacion.addActionListener(e ->
                llenarVecinas(estaciones[comboEstacion.getSelectedIndex()], comboVecina));

        JPanel formulario = new JPanel(new GridLayout(2, 2, 5, 5));
        formulario.add(new JLabel("Estación:"));
        formulario.add(comboEstacion);
        formulario.add(new JLabel("Conexión a eliminar:"));
        formulario.add(comboVecina);

        int respuesta = JOptionPane.showConfirmDialog(this, formulario, "Eliminar conexión",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (respuesta != JOptionPane.OK_OPTION) {
            return;
        }

        Estacion origen = estaciones[comboEstacion.getSelectedIndex()];
        // Las vecinas estan en el combo en el mismo orden que en la lista de conexiones
        Conexion elegida = origen.getPrimeraConexion();
        for (int i = 0; i < comboVecina.getSelectedIndex(); i++) {
            elegida = elegida.getSiguiente();
        }
        String idDestino = elegida.getDestino().getId();

        int confirmar = JOptionPane.showConfirmDialog(this,
                "¿Seguro que desea eliminar la conexión " + origen.getId() + " <-> " + idDestino + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirmar == JOptionPane.YES_OPTION) {
            grafo.desconectar(origen.getId(), idDestino);
            hayCambiosSinGuardar = true;
            mostrarRed();
            seleccionarEnTabla(origen.getId());
        }
    }

    /**
     * Llena un combo con las conexiones de una estacion, en el orden de su lista.
     *
     * @param estacion estacion cuyas conexiones se muestran
     * @param combo    combo que se llena
     */
    private void llenarVecinas(Estacion estacion, JComboBox<String> combo) {
        combo.removeAllItems();
        Conexion actual = estacion.getPrimeraConexion();
        while (actual != null) {
            Estacion vecina = actual.getDestino();
            combo.addItem(vecina.getId() + " - " + vecina.getNombre() + " (" + actual.getTiempo() + " min)");
            actual = actual.getSiguiente();
        }
    }

    // ===================================================================
    // Buscar estacion
    // ===================================================================

    /**
     * Busca una estacion por su ID (en la tabla hash, O(1)) y muestra sus datos.
     */
    private void buscarEstacion() {
        String id = campoBuscar.getText().trim();
        if (id.isEmpty()) {
            mostrarError("Escriba el ID de la estación que desea buscar.");
            return;
        }

        Estacion e = grafo.buscarEstacion(id);
        if (e == null) {
            mostrarError("No existe ninguna estación con el ID " + id + ".");
            return;
        }

        seleccionarEnTabla(e.getId());
        JOptionPane.showMessageDialog(this,
                "ID: " + e.getId()
                + "\nNombre: " + e.getNombre()
                + "\nLínea: " + e.getLinea()
                + "\nZona: " + e.getZona()
                + "\nCapacidad: " + e.getCapacidad() + " pasajeros/hora"
                + "\nGrado: " + e.getGrado()
                + "\nConexiones: " + textoConexiones(e),
                "Estación " + e.getId(), JOptionPane.INFORMATION_MESSAGE);
    }

    // ===================================================================
    // Ayudantes
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
     * Actualiza la tabla con la red en memoria y la barra de estado.
     */
    private void mostrarRed() {
        estacionesTabla = grafo.obtenerEstaciones();
        modeloTabla.fireTableDataChanged(); // le avisa a la tabla que los datos cambiaron
        actualizarEstado();
    }

    /**
     * Selecciona en la tabla la fila de una estacion y la hace visible.
     *
     * @param id ID de la estacion
     */
    private void seleccionarEnTabla(String id) {
        for (int i = 0; i < estacionesTabla.length; i++) {
            if (estacionesTabla[i].getId().equals(id)) {
                tablaEstaciones.setRowSelectionInterval(i, i);
                tablaEstaciones.scrollRectToVisible(tablaEstaciones.getCellRect(i, 0, true));
                return;
            }
        }
    }

    /**
     * @param e estacion
     * @return sus conexiones en una linea, ej. "PER (2), PZA (3)"
     */
    private String textoConexiones(Estacion e) {
        Conexion actual = e.getPrimeraConexion();
        if (actual == null) {
            return "(sin conexiones)";
        }
        String texto = "";
        while (actual != null) {
            texto += actual.getDestino().getId() + " (" + actual.getTiempo() + ")";
            if (actual.getSiguiente() != null) {
                texto += ", ";
            }
            actual = actual.getSiguiente();
        }
        return texto;
    }

    /**
     * @param lista estaciones
     * @return un texto "ID - Nombre" por estacion, en el mismo orden, para los combos
     */
    private String[] textosEstaciones(Estacion[] lista) {
        String[] textos = new String[lista.length];
        for (int i = 0; i < lista.length; i++) {
            textos[i] = lista[i].getId() + " - " + lista[i].getNombre();
        }
        return textos;
    }

    /**
     * @param lista estaciones
     * @return las lineas de transporte que existen, sin repetir
     */
    private String[] obtenerLineas(Estacion[] lista) {
        String[] lineas = new String[lista.length];
        int cantidad = 0;
        for (int i = 0; i < lista.length; i++) {
            boolean repetida = false;
            for (int j = 0; j < cantidad; j++) {
                if (lineas[j].equals(lista[i].getLinea())) {
                    repetida = true;
                }
            }
            if (!repetida) {
                lineas[cantidad] = lista[i].getLinea();
                cantidad++;
            }
        }

        // Se copia a un arreglo del tamano justo para que el combo no tenga espacios vacios
        String[] resultado = new String[cantidad];
        for (int i = 0; i < cantidad; i++) {
            resultado[i] = lineas[i];
        }
        return resultado;
    }

    /**
     * Convierte un texto en un entero mayor que cero. Si no se puede, muestra el error.
     *
     * @param texto texto escrito por el usuario
     * @param campo nombre del dato, para el mensaje (ej. "El tiempo")
     * @return el numero, o -1 si no es valido
     */
    private int leerEnteroPositivo(String texto, String campo) {
        int numero;
        try {
            numero = Integer.parseInt(texto);
        } catch (NumberFormatException ex) {
            mostrarError(campo + " debe ser un número entero (por ejemplo: 3).");
            return -1;
        }
        if (numero <= 0) {
            mostrarError(campo + " debe ser mayor que cero.");
            return -1;
        }
        return numero;
    }

    /**
     * @param texto texto a revisar
     * @return true si el texto tiene alguna coma
     */
    private boolean tieneComa(String texto) {
        return texto.contains(",");
    }

    /**
     * Actualiza la barra inferior con los archivos actuales y el resumen de la red.
     */
    private void actualizarEstado() {
        String archivos = "ninguno";
        if (gestorArchivo.getArchivoEstaciones() != null && gestorArchivo.getArchivoConexiones() != null) {
            archivos = gestorArchivo.getArchivoEstaciones().getName() + " y "
                    + gestorArchivo.getArchivoConexiones().getName();
        }

        String texto = "Archivos: " + archivos
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
        scroll.setPreferredSize(new Dimension(520, 240));
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

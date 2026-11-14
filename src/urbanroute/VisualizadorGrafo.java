package urbanroute;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.EnumSet;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.graphicGraph.GraphPosLengthUtils;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.swing_viewer.ViewPanel;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.util.InteractiveElement;
import org.graphstream.ui.view.util.MouseManager;
import org.graphstream.ui.view.util.ShortcutManager;

/**
 * Dibuja la red en pantalla usando la libreria GraphStream.
 * Solo sabe mostrar; no calcula nada por si misma
 * (las subredes y las rutas las calcula {@link AnalizadorRed}).
 * <p>
 * El dibujo se abre en una ventana aparte y tiene dos formas de colorear las estaciones:
 * <ul>
 *   <li><b>Por linea de transporte:</b> cada linea (Linea 1, Linea 2...) tiene un color.</li>
 *   <li><b>Por subred aislada:</b> cada grupo de estaciones conectadas entre si
 *       tiene un color, asi se ve que zonas quedaron incomunicadas.</li>
 * </ul>
 * Ademas puede resaltar en rojo la ruta mas rapida y agrandar los hubs
 * (las estaciones con mas conexiones).
 * <p>
 * GraphStream se usa solo para dibujar. La red de verdad (estaciones,
 * conexiones y tabla hash) sigue estando en {@link Grafo}; aqui solo se copia
 * lo necesario para el dibujo.
 *
 * @author Gabriel Gonzalez, Francisco Landaeta
 */
public class VisualizadorGrafo {

    /** Modo de color: un color por linea de transporte. */
    public static final int COLOR_POR_LINEA = 0;

    /** Modo de color: un color por subred aislada. */
    public static final int COLOR_POR_SUBRED = 1;

    /** Colores que se reparten entre las lineas o las subredes (formato #RRGGBB). */
    private static final String[] COLORES = {
        "#D62828", "#2A9D8F", "#1D4ED8", "#F4A100", "#7B2CBF",
        "#8D5524", "#E76F51", "#0EA5E9", "#65A30D", "#DB2777"
    };

    /**
     * Hoja de estilos de GraphStream (se parece a CSS).
     * Las clases "ruta" y "atenuado" se usan para resaltar las conexiones de la
     * ruta mas rapida. El color y el tamano de cada estacion se ponen aparte,
     * en {@link #estiloNodo(String, Estacion)}.
     */
    private static final String ESTILOS =
            "graph { fill-color: white; padding: 50px; }"
            + "node { size: 16px; fill-color: #888888; stroke-mode: plain; stroke-color: #333333;"
            + "       text-size: 12; text-style: bold; text-alignment: under; text-offset: 0px, 3px;"
            + "       text-background-mode: rounded-box; text-background-color: white; text-padding: 2px; }"
            + "edge { fill-color: #9A9A9A; size: 2px; text-size: 10; text-color: #555555; }"
            + "edge.ruta { fill-color: #E63946; size: 6px; text-size: 13; text-color: black; text-style: bold; }"
            + "edge.atenuado { fill-color: #DDDDDD; text-color: #BBBBBB; }";

    /** Copia de la red que GraphStream sabe dibujar. */
    private Graph grafoVisual;

    /** Red que se esta dibujando (de aqui se leen la linea de cada estacion). */
    private Grafo grafo;

    /** Se usa para pedir las subredes cuando el usuario cambia el modo de color. */
    private AnalizadorRed analizador;

    /** Ventana donde se muestra el dibujo (se crea la primera vez que se abre). */
    private JFrame ventana;

    /** Lista para elegir como colorear las estaciones. */
    private JComboBox<String> comboColor;

    /** Texto de abajo que explica que significa cada color. */
    private JLabel leyenda;

    /** Modo de color actual: {@link #COLOR_POR_LINEA} o {@link #COLOR_POR_SUBRED}. */
    private int modoColor;

    /** Subredes que se estan pintando (solo en el modo por subred). */
    private Estacion[][] subredes;

    /** Ruta que esta resaltada, o null si no hay ninguna. */
    private Estacion[] ruta;

    /** Tiempo total de la ruta resaltada, para mostrarlo en la leyenda. */
    private int tiempoRuta;

    /** Hubs que estan resaltados, o null si no hay ninguno. */
    private Estacion[] hubs;

    /** Cuantos acomodos distintos se prueban para elegir el mejor. */
    private static final int INTENTOS = 10;

    /** Limite de pasos de cada acomodo (casi siempre termina en unos 300). */
    private static final int MAX_PASOS = 3000;

    /** Semilla de los numeros al azar del acomodo. Cambia con "Reordenar dibujo". */
    private int semilla;

    /**
     * Crea el visualizador con un dibujo vacio.
     *
     * @param analizador analizador al que se le piden las subredes
     */
    public VisualizadorGrafo(AnalizadorRed analizador) {
        // Le dice a GraphStream que dibuje con Swing (la misma libreria de la ventana)
        System.setProperty("org.graphstream.ui", "swing");

        this.analizador = analizador;
        grafo = new Grafo();
        modoColor = COLOR_POR_LINEA;
        subredes = null;
        ruta = null;
        tiempoRuta = 0;
        hubs = null;
        semilla = 1;

        grafoVisual = new SingleGraph("Red de transporte");
        grafoVisual.setAttribute("ui.stylesheet", ESTILOS);
        grafoVisual.setAttribute("ui.quality");     // dibujo mas suave
        grafoVisual.setAttribute("ui.antialias");
    }

    // ===================================================================
    // Ventana
    // ===================================================================

    /**
     * Muestra la ventana del dibujo. Si ya estaba abierta, la trae al frente.
     */
    public void abrir() {
        if (ventana == null) {
            crearVentana();
        }
        ventana.setVisible(true);
        ventana.toFront();
    }

    /**
     * @return true si la ventana del dibujo esta abierta en pantalla
     */
    public boolean estaAbierto() {
        return ventana != null && ventana.isVisible();
    }

    /**
     * Crea la ventana con el dibujo de GraphStream, los controles de arriba
     * y la leyenda de abajo.
     */
    private void crearVentana() {
        // El visor no acomoda las estaciones solo: usa las posiciones que
        // calcula acomodarEstaciones(), asi el dibujo queda quieto
        SwingViewer visor = new SwingViewer(grafoVisual, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        ViewPanel panelDibujo = (ViewPanel) visor.addDefaultView(false);

        // El dibujo siempre se muestra completo y centrado: se quitan el mouse
        // y el teclado que GraphStream trae por defecto (zoom y mover)
        SinControles sinControles = new SinControles();
        panelDibujo.setMouseManager(sinControles);
        panelDibujo.setShortcutManager(sinControles);

        // ---- Controles de arriba ----
        comboColor = new JComboBox<>(new String[]{"Línea de transporte", "Subred aislada"});
        comboColor.setSelectedIndex(modoColor);
        comboColor.addActionListener(e -> {
            if (comboColor.getSelectedIndex() == modoColor) {
                return; // no cambio nada
            }
            if (comboColor.getSelectedIndex() == COLOR_POR_SUBRED) {
                colorearSubredes(analizador.obtenerSubredes());
            } else {
                colorearPorLinea();
            }
        });

        JButton botonQuitarRuta = new JButton("Quitar ruta y hubs");
        botonQuitarRuta.setToolTipText("Deja de resaltar la ruta más rápida y los hubs");
        botonQuitarRuta.addActionListener(e -> quitarResaltados());

        JButton botonReordenar = new JButton("Reordenar dibujo");
        botonReordenar.setToolTipText("Busca otra forma de acomodar las estaciones");
        botonReordenar.addActionListener(e -> reordenar());

        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        controles.add(new JLabel("Colorear por:"));
        controles.add(comboColor);
        controles.add(botonQuitarRuta);
        controles.add(botonReordenar);

        // ---- Leyenda de abajo ----
        leyenda = new JLabel();
        leyenda.setFont(new Font("SansSerif", Font.PLAIN, 12));
        leyenda.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(6, 10, 8, 10)));

        ventana = new JFrame("UrbanRoute - Dibujo de la red");
        ventana.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); // al cerrar solo se oculta
        ventana.setLayout(new BorderLayout());
        ventana.add(controles, BorderLayout.NORTH);
        ventana.add(panelDibujo, BorderLayout.CENTER);
        ventana.add(leyenda, BorderLayout.SOUTH);
        ventana.setSize(1000, 720);
        ventana.setLocationRelativeTo(null);

        pintar();
    }

    // ===================================================================
    // Dibujar la red
    // ===================================================================

    /**
     * Dibuja todas las estaciones y las conexiones de la red, con sus tiempos.
     * Si ya habia un dibujo, solo agrega y quita lo que cambio, y despues
     * vuelve a acomodar las estaciones.
     *
     * @param grafo red a dibujar
     */
    public void mostrarRed(Grafo grafo) {
        this.grafo = grafo;
        Estacion[] estaciones = grafo.obtenerEstaciones();

        // 1) Quitar del dibujo las estaciones que ya no estan en la red
        //    (se recorre de atras hacia adelante porque al borrar se corren las posiciones)
        for (int i = grafoVisual.getNodeCount() - 1; i >= 0; i--) {
            Node nodo = grafoVisual.getNode(i);
            if (!grafo.existeEstacion(nodo.getId())) {
                grafoVisual.removeNode(nodo);
            }
        }

        // 2) Agregar las estaciones nuevas
        for (int i = 0; i < estaciones.length; i++) {
            if (grafoVisual.getNode(estaciones[i].getId()) == null) {
                Node nodo = grafoVisual.addNode(estaciones[i].getId());
                nodo.setAttribute("ui.label", estaciones[i].getId());
            }
        }

        // 3) Las conexiones se borran todas y se vuelven a agregar
        while (grafoVisual.getEdgeCount() > 0) {
            grafoVisual.removeEdge(0);
        }
        for (int i = 0; i < estaciones.length; i++) {
            Estacion origen = estaciones[i];
            Conexion actual = origen.getPrimeraConexion();
            while (actual != null) {
                Estacion destino = actual.getDestino();
                // Cada conexion esta guardada en las dos estaciones: se dibuja una sola vez
                if (origen.getId().compareTo(destino.getId()) < 0) {
                    Edge arista = grafoVisual.addEdge(idArista(origen, destino), origen.getId(), destino.getId());
                    arista.setAttribute("ui.label", actual.getTiempo() + "");
                }
                actual = actual.getSiguiente();
            }
        }

        acomodarEstaciones();
        pintar();
    }

    /**
     * Busca otra forma de acomodar las estaciones (boton "Reordenar dibujo").
     * Cambia la semilla, asi los intentos son distintos a los anteriores.
     */
    public void reordenar() {
        semilla = semilla + INTENTOS;
        acomodarEstaciones();
    }

    // ===================================================================
    // Acomodar las estaciones
    // ===================================================================

    /**
     * Calcula donde va cada estacion en el dibujo.
     * <p>
     * GraphStream trae un acomodador (SpringBox) que trata las conexiones como
     * resortes que acercan las estaciones, y hace que las estaciones se empujen
     * entre si. Pero empieza con las estaciones en lugares al azar, y a veces
     * algunas terminan una encima de otra.
     * <p>
     * Por eso se prueban {@link #INTENTOS} acomodos distintos y se elige el que
     * deja las dos estaciones mas cercanas lo mas separadas posible. Como la
     * semilla de los numeros al azar es fija, la misma red sale siempre igual.
     */
    private void acomodarEstaciones() {
        int cantidad = grafoVisual.getNodeCount();
        if (cantidad == 0) {
            return;
        }

        double[] mejorX = null;
        double[] mejorY = null;
        double mejorSeparacion = -1;

        for (int intento = 0; intento < INTENTOS; intento++) {
            // Copia de la red solo para que el acomodador trabaje en ella
            SpringBox acomodador = new SpringBox(false, new Random(semilla + intento));
            Graph borrador = new SingleGraph("borrador");
            borrador.addSink(acomodador);           // el acomodador ve lo que se agrega
            acomodador.addAttributeSink(borrador);  // y escribe las posiciones en "xyz"
            for (int i = 0; i < cantidad; i++) {
                borrador.addNode(grafoVisual.getNode(i).getId());
            }
            for (int i = 0; i < grafoVisual.getEdgeCount(); i++) {
                Edge arista = grafoVisual.getEdge(i);
                borrador.addEdge(arista.getId(), arista.getSourceNode().getId(), arista.getTargetNode().getId());
            }

            // Se mueve paso a paso hasta que casi no se mueva (95% estable)
            int pasos = 0;
            while (pasos < MAX_PASOS && acomodador.getStabilization() < 0.95) {
                acomodador.compute();
                pasos++;
            }

            // Se leen las posiciones (en el mismo orden que grafoVisual)
            double[] x = new double[cantidad];
            double[] y = new double[cantidad];
            for (int i = 0; i < cantidad; i++) {
                double[] posicion = GraphPosLengthUtils.nodePosition(borrador.getNode(i));
                x[i] = posicion[0];
                y[i] = posicion[1];
            }

            double separacion = calcularSeparacionMinima(x, y);
            if (separacion > mejorSeparacion) {
                mejorSeparacion = separacion;
                mejorX = x;
                mejorY = y;
            }
        }

        // Se ponen las mejores posiciones en el dibujo
        for (int i = 0; i < cantidad; i++) {
            grafoVisual.getNode(i).setAttribute("xyz", mejorX[i], mejorY[i], 0.0);
        }
    }

    /**
     * Busca las dos estaciones mas cercanas del dibujo y devuelve su distancia,
     * dividida entre el tamano del dibujo (asi no importa la escala).
     *
     * @param x posiciones en x de las estaciones
     * @param y posiciones en y de las estaciones
     * @return distancia minima entre dos estaciones (0 = una encima de otra)
     */
    private double calcularSeparacionMinima(double[] x, double[] y) {
        if (x.length < 2) {
            return 1; // con una sola estacion no hay nada encimado
        }

        // Tamano del dibujo: lo mas ancho o lo mas alto
        double minX = x[0];
        double maxX = x[0];
        double minY = y[0];
        double maxY = y[0];
        for (int i = 1; i < x.length; i++) {
            minX = Math.min(minX, x[i]);
            maxX = Math.max(maxX, x[i]);
            minY = Math.min(minY, y[i]);
            maxY = Math.max(maxY, y[i]);
        }
        double tamano = Math.max(maxX - minX, maxY - minY);
        if (tamano == 0) {
            return 0;
        }

        // Se compara cada par de estaciones una sola vez
        double minima = Double.MAX_VALUE;
        for (int i = 0; i < x.length; i++) {
            for (int j = i + 1; j < x.length; j++) {
                double distancia = Math.sqrt((x[i] - x[j]) * (x[i] - x[j]) + (y[i] - y[j]) * (y[i] - y[j]));
                if (distancia < minima) {
                    minima = distancia;
                }
            }
        }
        return minima / tamano;
    }

    /**
     * Vuelve a dibujar la red despues de un cambio (agregar o eliminar).
     * La ruta resaltada se quita, porque puede que ya no exista.
     * Si se estaba coloreando por subred, las subredes se vuelven a pedir
     * al {@link AnalizadorRed}, porque el cambio pudo dividir la red.
     *
     * @param grafo red actualizada
     */
    public void actualizar(Grafo grafo) {
        ruta = null;
        hubs = null; // al cambiar la red los hubs pueden ser otros
        if (modoColor == COLOR_POR_SUBRED) {
            subredes = analizador.obtenerSubredes();
        }
        mostrarRed(grafo);
    }

    // ===================================================================
    // Colores, ruta y leyenda
    // ===================================================================

    /**
     * Pinta cada estacion con el color de su linea de transporte.
     */
    public void colorearPorLinea() {
        modoColor = COLOR_POR_LINEA;
        subredes = null;
        cambiarCombo();
        pintar();
    }

    /**
     * Pinta cada subred aislada con un color distinto.
     *
     * @param subredes subredes calculadas por {@link AnalizadorRed#obtenerSubredes()}
     */
    public void colorearSubredes(Estacion[][] subredes) {
        modoColor = COLOR_POR_SUBRED;
        this.subredes = subredes;
        cambiarCombo();
        pintar();
    }

    /**
     * Resalta en el dibujo la ruta mas rapida encontrada: sus conexiones
     * se ponen rojas y gruesas, y el resto de conexiones se ponen grises.
     *
     * @param ruta       estaciones de la ruta en orden (calculada por {@link AnalizadorRed})
     * @param tiempoTotal tiempo total de la ruta en minutos, para la leyenda
     */
    public void resaltarRuta(Estacion[] ruta, int tiempoTotal) {
        this.ruta = ruta;
        this.tiempoRuta = tiempoTotal;
        pintar();
    }

    /**
     * Agranda en el dibujo los hubs de transferencia, con un borde negro.
     *
     * @param hubs estaciones con mas conexiones (calculadas por {@link AnalizadorRed#identificarHubs()})
     */
    public void resaltarHubs(Estacion[] hubs) {
        this.hubs = hubs;
        pintar();
    }

    /**
     * Deja de resaltar la ruta y los hubs.
     */
    public void quitarResaltados() {
        ruta = null;
        hubs = null;
        pintar();
    }

    /**
     * Aplica los colores del modo actual y la ruta resaltada (si hay),
     * y actualiza la leyenda.
     */
    private void pintar() {
        String textoLeyenda;
        if (modoColor == COLOR_POR_SUBRED && subredes != null) {
            textoLeyenda = pintarPorSubred();
        } else {
            textoLeyenda = pintarPorLinea();
        }
        textoLeyenda += pintarRuta();
        textoLeyenda += textoHubs();

        if (leyenda != null) {
            leyenda.setText("<html>" + textoLeyenda
                    + "<br><font color='gray'>El número sobre cada conexión es el tiempo de traslado en minutos.</font></html>");
        }
    }

    /**
     * Pinta cada estacion con el color de su linea.
     *
     * @return texto de la leyenda en HTML
     */
    private String pintarPorLinea() {
        String[] lineas = obtenerLineasOrdenadas();
        String texto = "<b>Líneas:</b> ";

        for (int i = 0; i < grafoVisual.getNodeCount(); i++) {
            Node nodo = grafoVisual.getNode(i);
            Estacion estacion = grafo.buscarEstacion(nodo.getId());
            if (estacion != null) {
                int posicion = buscarTexto(lineas, estacion.getLinea());
                nodo.setAttribute("ui.style", estiloNodo(color(posicion), estacion));
            }
        }

        for (int i = 0; i < lineas.length; i++) {
            texto += cuadrito(color(i)) + " " + lineas[i] + "&nbsp;&nbsp;&nbsp;";
        }
        return texto;
    }

    /**
     * Pinta cada estacion con el color de su subred.
     *
     * @return texto de la leyenda en HTML
     */
    private String pintarPorSubred() {
        String texto = "<b>" + subredes.length + " subred(es):</b> ";
        for (int i = 0; i < subredes.length; i++) {
            for (int j = 0; j < subredes[i].length; j++) {
                Node nodo = grafoVisual.getNode(subredes[i][j].getId());
                if (nodo != null) {
                    nodo.setAttribute("ui.style", estiloNodo(color(i), subredes[i][j]));
                }
            }
            // En la leyenda se describen solo las primeras, para que no quede muy larga
            if (i < COLORES.length) {
                texto += cuadrito(color(i)) + " Subred " + (i + 1)
                        + " (" + subredes[i].length + " est.)&nbsp;&nbsp;&nbsp;";
            }
        }
        if (subredes.length > COLORES.length) {
            texto += "... (los colores se repiten)";
        }
        return texto;
    }

    /**
     * Marca con clases de estilo las conexiones de la ruta (las estaciones de
     * la ruta ya se agrandaron en {@link #estiloNodo(String, Estacion)}).
     *
     * @return texto de la leyenda en HTML (vacio si no hay ruta)
     */
    private String pintarRuta() {
        // Primero se limpian las marcas anteriores
        for (int i = 0; i < grafoVisual.getEdgeCount(); i++) {
            if (ruta == null) {
                grafoVisual.getEdge(i).removeAttribute("ui.class");
            } else {
                grafoVisual.getEdge(i).setAttribute("ui.class", "atenuado");
            }
        }

        if (ruta == null) {
            return "";
        }

        String texto = "<br><b><font color='#E63946'>Ruta más rápida:</font></b> ";
        for (int i = 0; i < ruta.length; i++) {
            if (i > 0) {
                Edge arista = grafoVisual.getEdge(idArista(ruta[i - 1], ruta[i]));
                if (arista != null) {
                    arista.setAttribute("ui.class", "ruta");
                }
                texto += " &rarr; ";
            }
            texto += ruta[i].getId();
        }
        texto += "  (" + tiempoRuta + " min)";
        return texto;
    }

    /**
     * @return texto de la leyenda con los hubs en HTML (vacio si no hay hubs resaltados)
     */
    private String textoHubs() {
        if (hubs == null || hubs.length == 0) {
            return "";
        }
        String texto = "<br><b>Hubs (" + hubs[0].getGrado() + " conexiones), los más grandes:</b> ";
        for (int i = 0; i < hubs.length; i++) {
            if (i > 0) {
                texto += ", ";
            }
            texto += hubs[i].getId() + " - " + hubs[i].getNombre();
        }
        return texto;
    }

    // ===================================================================
    // Ayudantes
    // ===================================================================

    /**
     * Arma el estilo de una estacion: su color y su tamano (los hubs son mas
     * grandes). Siempre se escriben el tamano y el borde, porque GraphStream
     * suma el estilo nuevo al anterior: si no se escriben, un hub que deja de
     * serlo se quedaria grande.
     *
     * @param color    color de relleno (#RRGGBB)
     * @param estacion estacion que se pinta
     * @return el estilo para el atributo "ui.style" de GraphStream
     */
    private String estiloNodo(String color, Estacion estacion) {
        String estilo = "fill-color: " + color + ";";
        if (esHub(estacion)) {
            estilo += " size: 30px; stroke-color: black; stroke-width: 3px;";
        } else if (estaEnRuta(estacion)) {
            estilo += " size: 24px; stroke-color: black; stroke-width: 3px;";
        } else {
            estilo += " size: 16px; stroke-color: #333333; stroke-width: 1px;";
        }
        return estilo;
    }

    /**
     * @param estacion estacion a revisar
     * @return true si la estacion es parte de la ruta resaltada
     */
    private boolean estaEnRuta(Estacion estacion) {
        if (ruta == null) {
            return false;
        }
        for (int i = 0; i < ruta.length; i++) {
            if (ruta[i] == estacion) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param estacion estacion a revisar
     * @return true si la estacion esta entre los hubs resaltados
     */
    private boolean esHub(Estacion estacion) {
        if (hubs == null) {
            return false;
        }
        for (int i = 0; i < hubs.length; i++) {
            if (hubs[i] == estacion) {
                return true;
            }
        }
        return false;
    }

    /**
     * Arma el ID de la arista entre dos estaciones. Siempre pone primero el ID
     * menor, asi A-B y B-A dan el mismo ID.
     *
     * @param a una estacion
     * @param b la otra estacion
     * @return el ID de la arista, ej. "CAP|LAO"
     */
    private String idArista(Estacion a, Estacion b) {
        if (a.getId().compareTo(b.getId()) < 0) {
            return a.getId() + "|" + b.getId();
        }
        return b.getId() + "|" + a.getId();
    }

    /**
     * Busca las lineas de transporte de la red, sin repetir y en orden alfabetico,
     * para que cada linea tenga siempre el mismo color.
     *
     * @return nombres de las lineas
     */
    private String[] obtenerLineasOrdenadas() {
        Estacion[] estaciones = grafo.obtenerEstaciones();
        String[] lineas = new String[estaciones.length];
        int cantidad = 0;

        for (int i = 0; i < estaciones.length; i++) {
            if (buscarTexto(lineas, estaciones[i].getLinea()) == -1) {
                // Insercion ordenada: se corren las mayores una posicion a la derecha
                int j = cantidad - 1;
                while (j >= 0 && lineas[j].compareTo(estaciones[i].getLinea()) > 0) {
                    lineas[j + 1] = lineas[j];
                    j--;
                }
                lineas[j + 1] = estaciones[i].getLinea();
                cantidad++;
            }
        }

        String[] resultado = new String[cantidad];
        for (int i = 0; i < cantidad; i++) {
            resultado[i] = lineas[i];
        }
        return resultado;
    }

    /**
     * @param lista arreglo de textos (puede tener null al final)
     * @param texto texto buscado
     * @return la posicion del texto, o -1 si no esta
     */
    private int buscarTexto(String[] lista, String texto) {
        for (int i = 0; i < lista.length; i++) {
            if (lista[i] != null && lista[i].equals(texto)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param numero numero de linea o de subred
     * @return un color de la paleta; si hay mas grupos que colores, se repiten
     */
    private String color(int numero) {
        if (numero < 0) {
            return "#888888";
        }
        return COLORES[numero % COLORES.length];
    }

    /**
     * @param color color en formato #RRGGBB
     * @return un cuadrado de ese color para la leyenda (HTML)
     */
    private String cuadrito(String color) {
        return "<font color='" + color + "'>&#9632;</font>";
    }

    /**
     * Pone el combo en el modo actual (por si el modo se cambio desde la ventana principal).
     */
    private void cambiarCombo() {
        if (comboColor != null) {
            comboColor.setSelectedIndex(modoColor);
        }
    }

    // ===================================================================
    // Dibujo fijo
    // ===================================================================

    /**
     * Control vacio para el mouse y el teclado del dibujo. GraphStream trae
     * por defecto zoom, mover el dibujo y mover estaciones; con esta clase no
     * se hace nada de eso y la red siempre se ve completa y centrada.
     * GraphStream pide que sea un {@link MouseManager} y un {@link ShortcutManager}.
     */
    private class SinControles implements MouseManager, ShortcutManager {

        /** No registra nada, asi el mouse y el teclado no cambian el dibujo. */
        @Override
        public void init(GraphicGraph grafico, View vista) {
        }

        /** No hay nada que quitar. */
        @Override
        public void release() {
        }

        /**
         * @return conjunto vacio: con el mouse no se toma ninguna estacion
         */
        @Override
        public EnumSet<InteractiveElement> getManagedTypes() {
            return EnumSet.noneOf(InteractiveElement.class);
        }
    }
}

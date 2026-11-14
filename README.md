# Urban Route - Optimización y Análisis de Redes de Transporte Multimodal

Herramienta orientada al modelado, análisis y optimización de redes de transporte urbano multimodal mediante grafos ponderados y tablas hash propias, permitiendo simular contingencias y calcular rutas óptimas en la red.

## Integrantes del Equipo
* Francisco Landaeta - 32336964
* Gabriel González - 32122172

Ex-integrante: Rafael Blanco - 32347798 (participó en las primeras entregas del proyecto).

## Enlace del Repositorio
* https://github.com/rafiuxon/UrbanRoute-ED

## Cómo ejecutarlo
1. Abrir la carpeta del proyecto en NetBeans (Java 17 o superior).
2. Las librerías de GraphStream ya vienen en la carpeta `lib/` y están agregadas al proyecto.
3. Ejecutar la clase `urbanroute.UrbanRoute`. Al iniciar se cargan los archivos maestros `datos/estaciones.csv` y `datos/conexiones.csv`.

## Qué hace el programa
* **Cargar archivos** con JFileChooser (uno de estaciones y otro de conexiones) y **guardar cambios** en esos mismos archivos.
* **Modificar la red:** agregar o eliminar estaciones y conexiones. Cada cambio actualiza el grafo y la tabla hash.
* **Ver grafo:** dibujo de la red con GraphStream, coloreado por línea de transporte o por subred aislada. Se actualiza cada vez que la red cambia, se muestra completo y centrado, y las estaciones se acomodan para que no queden encimadas (botón "Reordenar dibujo" para ver otro acomodo).
* **Subredes aisladas (DFS), Ruta más rápida (Dijkstra) y Hubs (grado):** los resultados se pueden ver resaltados en el dibujo.
* **Ver tabla hash:** casillas, colisiones y factor de carga de la tabla hash de estaciones.

## Documentación
* `docs/Diagrama_de_clases_UrbanRoute.png` (y .pdf): diagrama de clases del proyecto final.
* `docs/Tarjetas_CRC_UrbanRoute_Final.docx`: tarjetas CRC actualizadas.
* `docs/diseno-interfaz/`: bocetos de la interfaz.

## Fuentes
Referencias que usamos para diseñar las estructuras y los algoritmos. Todas las estructuras están implementadas por nosotros, sin Java Collections.

**Tabla hash (`TablaHash`, `NodoHash`)**
* Cormen, T. H., Leiserson, C. E., Rivest, R. L. y Stein, C. (2009). *Introduction to Algorithms* (3.ª ed.). MIT Press. Cap. 11: encadenamiento separado, factor de carga y método de la división con un tamaño primo.
* Weiss, M. A. (2012). *Data Structures and Algorithm Analysis in Java* (3.ª ed.). Pearson. Cap. 5: función hash polinomial para textos y redimensionamiento a un tamaño primo.
* Bloch, J. (2018). *Effective Java* (3.ª ed.). Addison-Wesley. Ítem 11: uso de 31 como multiplicador.
* Oracle. *Java SE 17 API*: [`String.hashCode()`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/String.html#hashCode()) (misma fórmula con 31) y [`Hashtable`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Hashtable.html) (factor de carga 0,75 por defecto). Código fuente de OpenJDK de `Hashtable`: cada nodo guarda su valor hash.

**Grafo y algoritmos (`Grafo`, `Pila`, `AnalizadorRed`)**
* Cormen et al. (2009). Sec. 10.1: pilas. Sec. 22.1: lista de adyacencia. Sec. 22.3: búsqueda en profundidad (DFS). Sec. 24.3: algoritmo de Dijkstra.
* Weiss, M. A. (2012). Cap. 9: representación de grafos, caminos más cortos con pesos (Dijkstra).

**Dibujo de la red (`VisualizadorGrafo`)**
* GraphStream 2.0 (gs-core y gs-ui-swing): https://graphstream-project.org/ . Se usa solo para dibujar; la red está guardada en nuestras propias clases.

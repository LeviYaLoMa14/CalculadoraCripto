/* Calculadora Criptografica - Interfaz grafica (Swing)
 * Compilar:  javac *.java
 * Ejecutar:  java CalculadoraCripto
 */
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.math.BigInteger;
import java.util.List;

public class CalculadoraCripto extends JFrame {

    static final Font FUENTE      = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    static final Font FUENTE_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 14);
    static final Color COLOR_ERROR = new Color(255, 220, 220);

    // Matrices
    private final JSpinner spK = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
    private final JSpinner spN = new JSpinner(new SpinnerNumberModel(27, 2, 1_000_000, 1));
    private final PanelMatriz matrizA = new PanelMatriz("Matriz A");
    private final PanelMatriz matrizB = new PanelMatriz("Matriz B");
    private final JTextArea salidaMatrices = areaSalida();
    private BigInteger[][] ultimoResultado = null;

    public CalculadoraCripto() {
        super("Calculadora Criptografica");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.setFont(FUENTE.deriveFont(Font.BOLD));
        pestanas.addTab("Matrices", crearPestanaMatrices());
        pestanas.addTab("Binario / Decimal", crearPestanaBinario());
        pestanas.addTab("XOR", crearPestanaXor());

        // Atajos Alt+1, Alt+2, Alt+3 para cambiar de pestana
        for (int i = 0; i < 3; i++) pestanas.setMnemonicAt(i, KeyEvent.VK_1 + i);

        setContentPane(pestanas);
        setMinimumSize(new Dimension(380, 480));
        setSize(980, 720);
        setLocationRelativeTo(null);
    }

    // ===================================================================
    //  PESTANA MATRICES
    // ===================================================================

    private JComponent crearPestanaMatrices() {
        JPanel raiz = new JPanel(new BorderLayout(8, 8));
        raiz.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Barra superior: k y n
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barra.add(etiqueta("Tamano k:"));
        barra.add(spK);
        barra.add(Box.createHorizontalStrut(12));
        barra.add(etiqueta("Modulo n:"));
        barra.add(spN);
        spK.setFont(FUENTE);
        spN.setFont(FUENTE);
        ((JSpinner.DefaultEditor) spN.getEditor()).getTextField().setColumns(6);
        spK.addChangeListener(e -> {
            int k = (Integer) spK.getValue();
            matrizA.redimensionar(k);
            matrizB.redimensionar(k);
        });
        raiz.add(barra, BorderLayout.NORTH);

        // Matrices A y B: lado a lado si hay espacio, una sobre otra si no
        JPanel panelAB = new JPanel(new GridLayout(1, 2, 10, 10));
        panelAB.add(matrizA);
        panelAB.add(matrizB);

        // Botones de operaciones
        JPanel ops = new JPanel(new GridLayout(0, 4, 6, 6));
        ops.add(boton("A x B", e -> multiplicar(false)));
        ops.add(boton("A x B mod n", e -> multiplicar(true)));
        ops.add(boton("det(A) y gcd", e -> determinante(matrizA, "A")));
        ops.add(boton("det(B) y gcd", e -> determinante(matrizB, "B")));
        ops.add(boton("A^-1 mod n", e -> inversa(matrizA, "A")));
        ops.add(boton("B^-1 mod n", e -> inversa(matrizB, "B")));
        ops.add(boton("Resultado a A", e -> resultadoA()));
        ops.add(boton("Intercambiar A y B", e -> intercambiar()));

        JPanel arriba = new PanelAnchoVentana(new BorderLayout(8, 8));
        arriba.add(panelAB, BorderLayout.CENTER);
        arriba.add(ops, BorderLayout.SOUTH);

        // Diseno responsivo: en ventanas angostas las matrices se apilan y los botones usan 2 columnas
        arriba.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                boolean angosto = arriba.getWidth() < 640;
                GridLayout gab = (GridLayout) panelAB.getLayout();
                GridLayout gops = (GridLayout) ops.getLayout();
                if (angosto == (gab.getColumns() == 1)) return;
                gab.setRows(angosto ? 2 : 1);
                gab.setColumns(angosto ? 1 : 2);
                gops.setColumns(angosto ? 2 : 4);
                arriba.revalidate();
            }
        });

        JScrollPane scrollArriba = new JScrollPane(arriba);
        scrollArriba.setBorder(null);
        scrollArriba.getVerticalScrollBar().setUnitIncrement(16);

        JPanel abajo = new JPanel(new BorderLayout(4, 4));
        abajo.add(etiqueta("Resultado"), BorderLayout.NORTH);
        abajo.add(new JScrollPane(salidaMatrices), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollArriba, abajo);
        split.setResizeWeight(0.6);
        split.setBorder(null);
        raiz.add(split, BorderLayout.CENTER);
        return raiz;
    }

    private long modN() {
        return ((Integer) spN.getValue()).longValue();
    }

    private void multiplicar(boolean conModulo) {
        BigInteger[][] a = matrizA.leer(), b = matrizB.leer();
        if (a == null || b == null) { error("Hay celdas con valores no enteros (marcadas en rojo)."); return; }
        long n = modN();
        BigInteger[][] p = OperacionesCripto.producto(a, b);
        StringBuilder sb = new StringBuilder("A x B =\n").append(formatoMatriz(p));
        ultimoResultado = p;
        if (conModulo) {
            BigInteger[][] pm = OperacionesCripto.modulo(p, n);
            sb.append("\nA x B mod ").append(n).append(" =\n").append(formatoMatriz(pm));
            ultimoResultado = pm;
        }
        salidaMatrices.setText(sb.toString());
        salidaMatrices.setCaretPosition(0);
    }

    private void determinante(PanelMatriz pm, String nombre) {
        BigInteger[][] m = pm.leer();
        if (m == null) { error("Hay celdas con valores no enteros (marcadas en rojo)."); return; }
        long n = modN();
        BigInteger det = OperacionesCripto.determinante(m);
        long detMod = det.mod(BigInteger.valueOf(n)).longValue();
        long g = OperacionesCripto.mcd(detMod, n);
        StringBuilder sb = new StringBuilder();
        sb.append(nombre).append(" =\n").append(formatoMatriz(m)).append('\n');
        sb.append("det(").append(nombre).append(") = ").append(det).append('\n');
        sb.append("det(").append(nombre).append(") mod ").append(n).append(" = ").append(detMod).append('\n');
        sb.append("gcd(").append(detMod).append(", ").append(n).append(") = ").append(g).append("\n\n");
        if (g == 1) {
            long inv = OperacionesCripto.inversoMultiplicativo(detMod, n);
            sb.append("gcd = 1  ->  ").append(nombre).append(" SI tiene inversa mod ").append(n).append('\n');
            sb.append("det^-1 mod ").append(n).append(" = ").append(inv)
              .append("   (comprobacion: ").append(detMod).append(" * ").append(inv)
              .append(" mod ").append(n).append(" = ").append(Math.floorMod(detMod * inv, n)).append(")");
        } else {
            sb.append("gcd != 1  ->  ").append(nombre).append(" NO tiene inversa mod ").append(n);
        }
        salidaMatrices.setText(sb.toString());
        salidaMatrices.setCaretPosition(0);
    }

    private void inversa(PanelMatriz pm, String nombre) {
        BigInteger[][] m = pm.leer();
        if (m == null) { error("Hay celdas con valores no enteros (marcadas en rojo)."); return; }
        long n = modN();
        BigInteger[][] inv = OperacionesCripto.inversaModular(m, n);
        if (inv == null) {
            determinante(pm, nombre);
            salidaMatrices.append("\n\nNo se puede calcular la inversa.");
            return;
        }
        BigInteger[][] check = OperacionesCripto.modulo(OperacionesCripto.producto(m, inv), n);
        ultimoResultado = inv;
        salidaMatrices.setText(nombre + "^-1 mod " + n + " =\n" + formatoMatriz(inv)
                + "\nComprobacion: " + nombre + " x " + nombre + "^-1 mod " + n + " =\n" + formatoMatriz(check));
        salidaMatrices.setCaretPosition(0);
    }

    private void resultadoA() {
        if (ultimoResultado == null) { error("Aun no hay un resultado de matriz."); return; }
        matrizA.escribir(ultimoResultado);
    }

    private void intercambiar() {
        BigInteger[][] a = matrizA.leer(), b = matrizB.leer();
        if (a == null || b == null) { error("Hay celdas con valores no enteros (marcadas en rojo)."); return; }
        matrizA.escribir(b);
        matrizB.escribir(a);
    }

    static String formatoMatriz(BigInteger[][] m) {
        int ancho = 1;
        for (BigInteger[] fila : m)
            for (BigInteger v : fila) ancho = Math.max(ancho, v.toString().length());
        StringBuilder sb = new StringBuilder();
        for (BigInteger[] fila : m) {
            sb.append("| ");
            for (int j = 0; j < fila.length; j++) {
                if (j > 0) sb.append("  ");
                sb.append(String.format("%" + ancho + "s", fila[j]));
            }
            sb.append(" |\n");
        }
        return sb.toString();
    }

    // Panel que dentro de un JScrollPane toma el ancho de la ventana (solo hay scroll vertical)
    static class PanelAnchoVentana extends JPanel implements Scrollable {
        PanelAnchoVentana(LayoutManager lm) { super(lm); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // Panel con la cuadricula de una matriz kxk y su entrada rapida
    class PanelMatriz extends JPanel {
        private JTextField[][] celdas = new JTextField[0][0];
        private final JPanel cuadricula = new JPanel();
        private final JTextField rapida = new JTextField();

        PanelMatriz(String titulo) {
            super(new BorderLayout(6, 6));
            TitledBorder tb = BorderFactory.createTitledBorder(titulo);
            tb.setTitleFont(FUENTE.deriveFont(Font.BOLD));
            setBorder(BorderFactory.createCompoundBorder(tb, new EmptyBorder(4, 6, 6, 6)));

            rapida.setFont(FUENTE_MONO);
            rapida.setToolTipText("Escribe o pega los valores por filas (ej. 1 2 3; 4 5 6; 7 8 9) y presiona Enter");
            rapida.addActionListener(e -> cargarTexto(rapida.getText()));
            JPanel norte = new JPanel(new BorderLayout(6, 0));
            norte.add(etiqueta("Entrada rapida:"), BorderLayout.WEST);
            norte.add(rapida, BorderLayout.CENTER);
            norte.add(boton("Cargar", e -> cargarTexto(rapida.getText())), BorderLayout.EAST);
            add(norte, BorderLayout.NORTH);

            add(cuadricula, BorderLayout.CENTER);

            JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            sur.add(boton("Identidad", e -> identidad()));
            sur.add(boton("Limpiar", e -> limpiar()));
            add(sur, BorderLayout.SOUTH);

            redimensionar(3);
        }

        void redimensionar(int k) {
            JTextField[][] viejas = celdas;
            celdas = new JTextField[k][k];
            cuadricula.removeAll();
            cuadricula.setLayout(new GridLayout(k, k, 4, 4));
            for (int i = 0; i < k; i++) {
                for (int j = 0; j < k; j++) {
                    JTextField c = new JTextField(3);
                    c.setFont(FUENTE_MONO.deriveFont(16f));
                    c.setHorizontalAlignment(JTextField.CENTER);
                    if (i < viejas.length && j < viejas.length) c.setText(viejas[i][j].getText());
                    configurarNavegacion(c, i, j);
                    celdas[i][j] = c;
                    cuadricula.add(c);
                }
            }
            cuadricula.revalidate();
            cuadricula.repaint();
        }

        // Enter / flechas para moverse entre celdas; al entrar se selecciona el contenido
        private void configurarNavegacion(JTextField c, int i, int j) {
            c.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { c.selectAll(); }
            });
            c.addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    int k = celdas.length, fi = i, co = j;
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_ENTER:
                            co++;
                            if (co == k) { co = 0; fi = (fi + 1) % k; }
                            break;
                        case KeyEvent.VK_UP:    fi = Math.max(0, fi - 1); break;
                        case KeyEvent.VK_DOWN:  fi = Math.min(k - 1, fi + 1); break;
                        case KeyEvent.VK_LEFT:  if (c.getCaretPosition() > 0 && c.getSelectedText() == null) return; co = Math.max(0, co - 1); break;
                        case KeyEvent.VK_RIGHT: if (c.getCaretPosition() < c.getText().length() && c.getSelectedText() == null) return; co = Math.min(k - 1, co + 1); break;
                        default: return;
                    }
                    e.consume();
                    celdas[fi][co].requestFocusInWindow();
                }
            });
        }

        // Lee los numeros de un texto. Si la cantidad es un cuadrado perfecto ajusta k.
        void cargarTexto(String texto) {
            String[] partes = texto.trim().split("[\\s,;]+");
            if (texto.trim().isEmpty()) return;
            int total = partes.length;
            int k = (int) Math.round(Math.sqrt(total));
            if (k * k != total || k > 10) {
                error("Se capturaron " + total + " valores; deben ser k*k (4, 9, 16, ...).");
                return;
            }
            for (String p : partes) {
                if (!p.matches("-?\\d+")) { error("Valor no valido: " + p); return; }
            }
            spK.setValue(k); // dispara redimensionar en A y B
            for (int t = 0; t < total; t++) celdas[t / k][t % k].setText(partes[t]);
            celdas[0][0].requestFocusInWindow();
        }

        BigInteger[][] leer() {
            int k = celdas.length;
            BigInteger[][] m = new BigInteger[k][k];
            boolean ok = true;
            for (int i = 0; i < k; i++) {
                for (int j = 0; j < k; j++) {
                    String s = celdas[i][j].getText().trim();
                    if (s.isEmpty()) s = "0";
                    if (s.matches("-?\\d+")) {
                        m[i][j] = new BigInteger(s);
                        celdas[i][j].setBackground(UIManager.getColor("TextField.background"));
                    } else {
                        celdas[i][j].setBackground(COLOR_ERROR);
                        ok = false;
                    }
                }
            }
            return ok ? m : null;
        }

        void escribir(BigInteger[][] m) {
            if (m.length != celdas.length) spK.setValue(m.length);
            for (int i = 0; i < m.length; i++)
                for (int j = 0; j < m.length; j++)
                    celdas[i][j].setText(m[i][j].toString());
        }

        void identidad() {
            for (int i = 0; i < celdas.length; i++)
                for (int j = 0; j < celdas.length; j++)
                    celdas[i][j].setText(i == j ? "1" : "0");
        }

        void limpiar() {
            for (JTextField[] fila : celdas)
                for (JTextField c : fila) {
                    c.setText("");
                    c.setBackground(UIManager.getColor("TextField.background"));
                }
            rapida.setText("");
            celdas[0][0].requestFocusInWindow();
        }
    }

    // ===================================================================
    //  PESTANA BINARIO / DECIMAL
    // ===================================================================

    private JComponent crearPestanaBinario() {
        JPanel raiz = new PanelAnchoVentana(null);
        raiz.setLayout(new BoxLayout(raiz, BoxLayout.Y_AXIS));
        raiz.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Decimal a binario ---
        JTextField decimal = campo();
        JSpinner bits = new JSpinner(new SpinnerNumberModel(0, 0, 4096, 1));
        bits.setFont(FUENTE);
        bits.setToolTipText("0 = sin relleno. Ej. 8 rellena con ceros hasta 8 bits");
        JTextField salidaBin = campoSalida();
        JTextArea pasosBin = areaSalida();
        pasosBin.setRows(6);

        Runnable convDecBin = () -> {
            String s = decimal.getText().trim();
            if (s.isEmpty()) { salidaBin.setText(""); pasosBin.setText(""); decimal.setBackground(UIManager.getColor("TextField.background")); return; }
            if (!OperacionesCripto.esDecimal(s)) {
                decimal.setBackground(COLOR_ERROR);
                salidaBin.setText("Solo digitos 0-9 (entero no negativo)");
                pasosBin.setText("");
                return;
            }
            decimal.setBackground(UIManager.getColor("TextField.background"));
            String b = OperacionesCripto.decimalABinario(s, (Integer) bits.getValue());
            salidaBin.setText(b);
            String txt = "Agrupado: " + OperacionesCripto.agrupar(b) + "   (" + b.length() + " bits)\n\n";
            if (s.length() <= 20) txt += String.join("\n", OperacionesCripto.pasosDecimalABinario(s));
            pasosBin.setText(txt);
            pasosBin.setCaretPosition(0);
        };
        alCambiar(decimal, convDecBin);
        bits.addChangeListener(e -> convDecBin.run());

        JPanel filaDec = new JPanel(new BorderLayout(6, 0));
        filaDec.add(decimal, BorderLayout.CENTER);
        JPanel bitsP = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bitsP.add(etiqueta("Bits:"));
        bitsP.add(bits);
        filaDec.add(bitsP, BorderLayout.EAST);

        raiz.add(seccion("Decimal a binario",
                etiqueta("Decimal"), filaDec,
                etiqueta("Binario"), filaConCopiar(salidaBin),
                etiqueta("Procedimiento"), new JScrollPane(pasosBin)));
        raiz.add(Box.createVerticalStrut(10));

        // --- Binario a decimal ---
        JTextField binario = campo();
        binario.setToolTipText("Se permiten espacios para agrupar, ej. 1010 1100");
        JTextField salidaDec = campoSalida();
        JTextArea pasosDec = areaSalida();
        pasosDec.setRows(4);

        alCambiar(binario, () -> {
            String s = OperacionesCripto.limpiarBinario(binario.getText());
            if (s.isEmpty()) { salidaDec.setText(""); pasosDec.setText(""); binario.setBackground(UIManager.getColor("TextField.background")); return; }
            if (!OperacionesCripto.esBinario(s)) {
                binario.setBackground(COLOR_ERROR);
                salidaDec.setText("Solo se permiten 0 y 1");
                pasosDec.setText("");
                return;
            }
            binario.setBackground(UIManager.getColor("TextField.background"));
            salidaDec.setText(OperacionesCripto.binarioADecimal(s));
            pasosDec.setText(s.length() <= 64 ? OperacionesCripto.pasosBinarioADecimal(s) : "(procedimiento omitido: mas de 64 bits)");
            pasosDec.setCaretPosition(0);
        });

        raiz.add(seccion("Binario a decimal",
                etiqueta("Binario"), binario,
                etiqueta("Decimal"), filaConCopiar(salidaDec),
                etiqueta("Procedimiento"), new JScrollPane(pasosDec)));

        JScrollPane sp = new JScrollPane(raiz);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    // ===================================================================
    //  PESTANA XOR
    // ===================================================================

    private JComponent crearPestanaXor() {
        JPanel raiz = new PanelAnchoVentana(null);
        raiz.setLayout(new BoxLayout(raiz, BoxLayout.Y_AXIS));
        raiz.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextField a = campo();
        JTextField b = campo();
        a.setToolTipText("Binario; se permiten espacios para agrupar");
        b.setToolTipText("Binario; se permiten espacios para agrupar");
        JTextField resultado = campoSalida();
        JTextArea detalle = areaSalida();
        detalle.setRows(5);

        Runnable calcular = () -> {
            String sa = OperacionesCripto.limpiarBinario(a.getText());
            String sb = OperacionesCripto.limpiarBinario(b.getText());
            boolean okA = sa.isEmpty() || OperacionesCripto.esBinario(sa);
            boolean okB = sb.isEmpty() || OperacionesCripto.esBinario(sb);
            a.setBackground(okA ? UIManager.getColor("TextField.background") : COLOR_ERROR);
            b.setBackground(okB ? UIManager.getColor("TextField.background") : COLOR_ERROR);
            if (!okA || !okB) { resultado.setText("Solo se permiten 0 y 1"); detalle.setText(""); return; }
            if (sa.isEmpty() || sb.isEmpty()) { resultado.setText(""); detalle.setText(""); return; }

            String r = OperacionesCripto.xor(sa, sb);
            int L = r.length();
            String pa = OperacionesCripto.rellenar(sa, L), pb = OperacionesCripto.rellenar(sb, L);
            resultado.setText(r);
            String ga = OperacionesCripto.agrupar(pa), gb = OperacionesCripto.agrupar(pb), gr = OperacionesCripto.agrupar(r);
            String linea = "-".repeat(gr.length());
            detalle.setText(
                    "    A = " + ga + "   (" + OperacionesCripto.binarioADecimal(pa) + ")\n" +
                    "    B = " + gb + "   (" + OperacionesCripto.binarioADecimal(pb) + ")\n" +
                    "        " + linea + "\n" +
                    "A ^ B = " + gr + "   (" + OperacionesCripto.binarioADecimal(r) + ")\n\n" +
                    "Regla: bits iguales -> 0, bits distintos -> 1. Se rellena con ceros a la izquierda.");
        };
        alCambiar(a, calcular);
        alCambiar(b, calcular);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        acciones.add(boton("Resultado a A", e -> { a.setText(resultado.getText()); b.setText(""); b.requestFocusInWindow(); }));
        acciones.add(boton("Intercambiar", e -> { String t = a.getText(); a.setText(b.getText()); b.setText(t); }));
        acciones.add(boton("Limpiar", e -> { a.setText(""); b.setText(""); a.requestFocusInWindow(); }));
        a.addActionListener(e -> b.requestFocusInWindow());

        raiz.add(seccion("XOR de binarios",
                etiqueta("A"), a,
                etiqueta("B"), b,
                etiqueta("A XOR B"), filaConCopiar(resultado),
                etiqueta("Detalle"), new JScrollPane(detalle),
                new JLabel(" "), acciones));

        JScrollPane sp = new JScrollPane(raiz);
        sp.setBorder(null);
        return sp;
    }

    // ===================================================================
    //  UTILIDADES DE INTERFAZ
    // ===================================================================

    private static JLabel etiqueta(String t) {
        JLabel l = new JLabel(t);
        l.setFont(FUENTE);
        return l;
    }

    private static JButton boton(String t, ActionListener al) {
        JButton b = new JButton(t);
        b.setFont(FUENTE);
        b.setFocusable(true);
        b.addActionListener(al);
        return b;
    }

    private static JTextField campo() {
        JTextField c = new JTextField();
        c.setFont(FUENTE_MONO.deriveFont(16f));
        return c;
    }

    private static JTextField campoSalida() {
        JTextField c = campo();
        c.setEditable(false);
        return c;
    }

    private static JTextArea areaSalida() {
        JTextArea a = new JTextArea(8, 30);
        a.setFont(FUENTE_MONO);
        a.setEditable(false);
        a.setMargin(new Insets(6, 8, 6, 8));
        return a;
    }

    private static JPanel filaConCopiar(JTextField salida) {
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.add(salida, BorderLayout.CENTER);
        p.add(boton("Copiar", e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(salida.getText()), null)), BorderLayout.EAST);
        return p;
    }

    // Seccion con titulo: recibe pares (etiqueta, componente) que se apilan verticalmente
    private static JPanel seccion(String titulo, JComponent... comps) {
        JPanel p = new JPanel(new GridBagLayout());
        TitledBorder tb = BorderFactory.createTitledBorder(titulo);
        tb.setTitleFont(FUENTE.deriveFont(Font.BOLD));
        p.setBorder(BorderFactory.createCompoundBorder(tb, new EmptyBorder(4, 8, 8, 8)));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        for (int i = 0; i < comps.length; i++) {
            g.gridy = i;
            g.insets = new Insets(i % 2 == 0 ? 6 : 2, 0, 0, 0);
            if (comps[i] instanceof JScrollPane) { g.fill = GridBagConstraints.BOTH; g.weighty = 1; }
            else { g.fill = GridBagConstraints.HORIZONTAL; g.weighty = 0; }
            p.add(comps[i], g);
        }
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private static void alCambiar(JTextField c, Runnable r) {
        c.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { r.run(); }
            public void removeUpdate(DocumentEvent e)  { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalculadoraCripto().setVisible(true));
    }
}

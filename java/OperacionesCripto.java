/* Calculadora Criptografica - Logica (sin interfaz) */
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class OperacionesCripto {

    // ===================== MATRICES =====================

    // Producto de matrices kxk (sin modulo)
    public static BigInteger[][] producto(BigInteger[][] a, BigInteger[][] b) {
        int k = a.length;
        BigInteger[][] c = new BigInteger[k][k];
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                BigInteger suma = BigInteger.ZERO;
                for (int t = 0; t < k; t++)
                    suma = suma.add(a[i][t].multiply(b[t][j]));
                c[i][j] = suma;
            }
        }
        return c;
    }

    // Reduce cada entrada mod n (siempre en el rango 0..n-1)
    public static BigInteger[][] modulo(BigInteger[][] m, long n) {
        BigInteger bn = BigInteger.valueOf(n);
        int k = m.length;
        BigInteger[][] r = new BigInteger[k][k];
        for (int i = 0; i < k; i++)
            for (int j = 0; j < k; j++)
                r[i][j] = m[i][j].mod(bn);
        return r;
    }

    // Determinante exacto con el algoritmo de Bareiss (solo divisiones exactas)
    public static BigInteger determinante(BigInteger[][] original) {
        int k = original.length;
        BigInteger[][] m = new BigInteger[k][];
        for (int i = 0; i < k; i++) m[i] = original[i].clone();

        int signo = 1;
        BigInteger previo = BigInteger.ONE;
        for (int p = 0; p < k - 1; p++) {
            if (m[p][p].signum() == 0) {
                int fila = -1;
                for (int i = p + 1; i < k; i++)
                    if (m[i][p].signum() != 0) { fila = i; break; }
                if (fila == -1) return BigInteger.ZERO;
                BigInteger[] tmp = m[p]; m[p] = m[fila]; m[fila] = tmp;
                signo = -signo;
            }
            for (int i = p + 1; i < k; i++) {
                for (int j = p + 1; j < k; j++) {
                    m[i][j] = m[i][j].multiply(m[p][p])
                            .subtract(m[i][p].multiply(m[p][j]))
                            .divide(previo);
                }
            }
            previo = m[p][p];
        }
        BigInteger det = m[k - 1][k - 1];
        return signo < 0 ? det.negate() : det;
    }

    // Matriz inversa mod n: A^-1 = det^-1 * adj(A) mod n. Regresa null si no existe.
    public static BigInteger[][] inversaModular(BigInteger[][] a, long n) {
        int k = a.length;
        long det = determinante(a).mod(BigInteger.valueOf(n)).longValue();
        long detInv = inversoMultiplicativo(det, n);
        if (detInv == -1) return null;

        BigInteger bInv = BigInteger.valueOf(detInv);
        BigInteger[][] inv = new BigInteger[k][k];
        if (k == 1) {
            inv[0][0] = bInv.mod(BigInteger.valueOf(n));
            return inv;
        }
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                // adj(A)[i][j] = cofactor C[j][i] = (-1)^(i+j) * det(menor sin fila j y columna i)
                BigInteger cof = determinante(menor(a, j, i));
                if ((i + j) % 2 == 1) cof = cof.negate();
                inv[i][j] = cof.multiply(bInv);
            }
        }
        return modulo(inv, n);
    }

    // Submatriz quitando la fila f y la columna c
    public static BigInteger[][] menor(BigInteger[][] a, int f, int c) {
        int k = a.length;
        BigInteger[][] r = new BigInteger[k - 1][k - 1];
        for (int i = 0, ri = 0; i < k; i++) {
            if (i == f) continue;
            for (int j = 0, rj = 0; j < k; j++) {
                if (j == c) continue;
                r[ri][rj++] = a[i][j];
            }
            ri++;
        }
        return r;
    }

    // ===================== EUCLIDES =====================

    // Algoritmo de Euclides (AE)
    public static long mcd(long a, long n) {
        a = Math.abs(a);
        n = Math.abs(n);
        while (n != 0) {
            long res = a % n;
            a = n;
            n = res;
        }
        return a;
    }

    // Algoritmo de Euclides Extendido (AEE), regresa {gcd, x, y} tal que a*x + b*y = gcd(a, b)
    public static long[] AEE(long a, long b) {
        long x0 = 1, y0 = 0, x1 = 0, y1 = 1;
        while (b != 0) {
            long q = a / b;
            long t = a - q * b; a = b; b = t;
            t = x0 - q * x1; x0 = x1; x1 = t;
            t = y0 - q * y1; y0 = y1; y1 = t;
        }
        return new long[]{a, x0, y0};
    }

    public static long inversoMultiplicativo(long a, long m) {
        long[] r = AEE(Math.floorMod(a, m), m);
        if (r[0] != 1) return -1;
        return Math.floorMod(r[1], m);
    }

    // ===================== BINARIO / DECIMAL =====================

    public static String limpiarBinario(String s) {
        return s.replaceAll("[\\s_]", "");
    }

    public static boolean esBinario(String s) {
        return s.matches("[01]+");
    }

    public static boolean esDecimal(String s) {
        return s.matches("[0-9]+");
    }

    // bits = 0 significa sin relleno; si es mayor rellena con ceros a la izquierda
    public static String decimalABinario(String decimal, int bits) {
        String b = new BigInteger(decimal).toString(2);
        return rellenar(b, bits);
    }

    public static String binarioADecimal(String binario) {
        return new BigInteger(binario, 2).toString();
    }

    public static String rellenar(String b, int bits) {
        StringBuilder sb = new StringBuilder();
        for (int i = b.length(); i < bits; i++) sb.append('0');
        return sb.append(b).toString();
    }

    // Agrupa de 4 en 4 desde la derecha: 101101 -> 10 1101
    public static String agrupar(String b) {
        StringBuilder sb = new StringBuilder();
        int primer = b.length() % 4;
        for (int i = 0; i < b.length(); i++) {
            if (i > 0 && (i - primer) % 4 == 0) sb.append(' ');
            sb.append(b.charAt(i));
        }
        return sb.toString();
    }

    // Divisiones sucesivas entre 2
    public static List<String> pasosDecimalABinario(String decimal) {
        List<String> pasos = new ArrayList<>();
        BigInteger n = new BigInteger(decimal);
        BigInteger dos = BigInteger.valueOf(2);
        if (n.signum() == 0) { pasos.add("0 / 2 = 0   residuo 0"); return pasos; }
        while (n.signum() > 0) {
            BigInteger[] qr = n.divideAndRemainder(dos);
            pasos.add(n + " / 2 = " + qr[0] + "   residuo " + qr[1]);
            n = qr[0];
        }
        pasos.add("Se leen los residuos de abajo hacia arriba.");
        return pasos;
    }

    // Suma de potencias de 2
    public static String pasosBinarioADecimal(String binario) {
        StringBuilder terminos = new StringBuilder();
        StringBuilder valores = new StringBuilder();
        int L = binario.length();
        for (int i = 0; i < L; i++) {
            int exp = L - 1 - i;
            if (terminos.length() > 0) terminos.append(" + ");
            terminos.append(binario.charAt(i)).append("*2^").append(exp);
            if (binario.charAt(i) == '1') {
                if (valores.length() > 0) valores.append(" + ");
                valores.append(BigInteger.ONE.shiftLeft(exp));
            }
        }
        if (valores.length() == 0) valores.append("0");
        return terminos + "\n= " + valores + "\n= " + binarioADecimal(binario);
    }

    // ===================== XOR =====================

    // XOR bit a bit; las cadenas se rellenan a la misma longitud
    public static String xor(String a, String b) {
        int L = Math.max(a.length(), b.length());
        a = rellenar(a, L);
        b = rellenar(b, L);
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < L; i++)
            r.append(a.charAt(i) == b.charAt(i) ? '0' : '1');
        return r.toString();
    }
}

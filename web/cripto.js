/* Calculadora Criptografica - Logica (sin interfaz)
 * Usa BigInt para que los determinantes grandes no pierdan precision.
 */
(function (global) {
  'use strict';

  // ===================== MATRICES =====================

  // Modulo que siempre regresa un valor en 0..n-1
  function mod(a, n) {
    const r = a % n;
    return r < 0n ? r + n : r;
  }

  // Producto de matrices kxk (sin modulo)
  function producto(a, b) {
    const k = a.length;
    const c = [];
    for (let i = 0; i < k; i++) {
      c.push([]);
      for (let j = 0; j < k; j++) {
        let suma = 0n;
        for (let t = 0; t < k; t++) suma += a[i][t] * b[t][j];
        c[i].push(suma);
      }
    }
    return c;
  }

  // Reduce cada entrada mod n
  function moduloMatriz(m, n) {
    return m.map(fila => fila.map(v => mod(v, n)));
  }

  // Determinante exacto con el algoritmo de Bareiss (solo divisiones exactas)
  function determinante(original) {
    const k = original.length;
    const m = original.map(fila => fila.slice());
    let signo = 1n;
    let previo = 1n;
    for (let p = 0; p < k - 1; p++) {
      if (m[p][p] === 0n) {
        let fila = -1;
        for (let i = p + 1; i < k; i++) if (m[i][p] !== 0n) { fila = i; break; }
        if (fila === -1) return 0n;
        [m[p], m[fila]] = [m[fila], m[p]];
        signo = -signo;
      }
      for (let i = p + 1; i < k; i++) {
        for (let j = p + 1; j < k; j++) {
          m[i][j] = (m[i][j] * m[p][p] - m[i][p] * m[p][j]) / previo;
        }
      }
      previo = m[p][p];
    }
    return signo * m[k - 1][k - 1];
  }

  // Submatriz quitando la fila f y la columna c
  function menor(a, f, c) {
    return a.filter((_, i) => i !== f).map(fila => fila.filter((_, j) => j !== c));
  }

  // Matriz inversa mod n: A^-1 = det^-1 * adj(A) mod n. Regresa null si no existe.
  function inversaModular(a, n) {
    const k = a.length;
    const det = mod(determinante(a), n);
    const detInv = inversoMultiplicativo(det, n);
    if (detInv === -1n) return null;
    if (k === 1) return [[mod(detInv, n)]];
    const inv = [];
    for (let i = 0; i < k; i++) {
      inv.push([]);
      for (let j = 0; j < k; j++) {
        // adj(A)[i][j] = cofactor C[j][i]
        let cof = determinante(menor(a, j, i));
        if ((i + j) % 2 === 1) cof = -cof;
        inv[i].push(mod(cof * detInv, n));
      }
    }
    return inv;
  }

  // ===================== EUCLIDES =====================

  // Algoritmo de Euclides (AE)
  function mcd(a, n) {
    a = a < 0n ? -a : a;
    n = n < 0n ? -n : n;
    while (n !== 0n) {
      const res = a % n;
      a = n;
      n = res;
    }
    return a;
  }

  // Algoritmo de Euclides Extendido (AEE): [gcd, x, y] tal que a*x + b*y = gcd(a, b)
  function AEE(a, b) {
    let x0 = 1n, y0 = 0n, x1 = 0n, y1 = 1n;
    while (b !== 0n) {
      const q = a / b;
      [a, b] = [b, a - q * b];
      [x0, x1] = [x1, x0 - q * x1];
      [y0, y1] = [y1, y0 - q * y1];
    }
    return [a, x0, y0];
  }

  function inversoMultiplicativo(a, m) {
    const r = AEE(mod(a, m), m);
    if (r[0] !== 1n) return -1n;
    return mod(r[1], m);
  }

  // ===================== BINARIO / DECIMAL =====================

  const limpiarBinario = s => s.replace(/[\s_]/g, '');
  const esBinario = s => /^[01]+$/.test(s);
  const esDecimal = s => /^[0-9]+$/.test(s);
  const rellenar = (b, bits) => b.padStart(bits, '0');

  // bits = 0 significa sin relleno
  function decimalABinario(decimal, bits) {
    return rellenar(BigInt(decimal).toString(2), bits || 0);
  }

  function binarioADecimal(binario) {
    return BigInt('0b' + binario).toString();
  }

  // Agrupa de 4 en 4 desde la derecha: 101101 -> 10 1101
  function agrupar(b) {
    let s = '';
    const primer = b.length % 4;
    for (let i = 0; i < b.length; i++) {
      if (i > 0 && (i - primer) % 4 === 0) s += ' ';
      s += b[i];
    }
    return s;
  }

  // Divisiones sucesivas entre 2
  function pasosDecimalABinario(decimal) {
    let n = BigInt(decimal);
    if (n === 0n) return ['0 / 2 = 0   residuo 0'];
    const pasos = [];
    while (n > 0n) {
      pasos.push(`${n} / 2 = ${n / 2n}   residuo ${n % 2n}`);
      n /= 2n;
    }
    pasos.push('Se leen los residuos de abajo hacia arriba.');
    return pasos;
  }

  // Suma de potencias de 2
  function pasosBinarioADecimal(binario) {
    const L = binario.length;
    const terminos = [], valores = [];
    for (let i = 0; i < L; i++) {
      const exp = L - 1 - i;
      terminos.push(`${binario[i]}*2^${exp}`);
      if (binario[i] === '1') valores.push((1n << BigInt(exp)).toString());
    }
    return `${terminos.join(' + ')}\n= ${valores.join(' + ') || '0'}\n= ${binarioADecimal(binario)}`;
  }

  // ===================== XOR =====================

  // XOR bit a bit; las cadenas se rellenan a la misma longitud
  function xor(a, b) {
    const L = Math.max(a.length, b.length);
    a = rellenar(a, L);
    b = rellenar(b, L);
    let r = '';
    for (let i = 0; i < L; i++) r += a[i] === b[i] ? '0' : '1';
    return r;
  }

  const Cripto = {
    mod, producto, moduloMatriz, determinante, menor, inversaModular,
    mcd, AEE, inversoMultiplicativo,
    limpiarBinario, esBinario, esDecimal, rellenar, decimalABinario, binarioADecimal,
    agrupar, pasosDecimalABinario, pasosBinarioADecimal, xor
  };

  if (typeof module !== 'undefined' && module.exports) module.exports = Cripto;
  else global.Cripto = Cripto;
})(this);

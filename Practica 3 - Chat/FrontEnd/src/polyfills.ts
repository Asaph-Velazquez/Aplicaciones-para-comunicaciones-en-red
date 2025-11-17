/**
 * Polyfills necesarios para SockJS en navegadores modernos
 * SockJS espera variables globales de Node.js que no existen en navegadores
 */

// Definir 'global' como referencia a globalThis
(globalThis as any).global = globalThis;

// Definir 'process' con las propiedades mínimas que SockJS necesita
if (typeof (globalThis as any).process === 'undefined') {
  (globalThis as any).process = {
    env: { DEBUG: undefined },
    version: '',
    platform: 'browser',
    browser: true,
  };
}

// Definir 'Buffer' si no existe (algunas versiones de SockJS lo necesitan)
if (typeof (globalThis as any).Buffer === 'undefined') {
  (globalThis as any).Buffer = {
    isBuffer: () => false,
  };
}

console.log('✅ Polyfills de SockJS cargados correctamente');

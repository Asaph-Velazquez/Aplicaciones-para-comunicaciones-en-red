// ===== VARIABLES GLOBALES =====
let ticketData = null;
let ticketId = null;

// ===== FUNCIONES DE INICIALIZACIÓN =====
function inicializarTicket() {
  generarIdTicket();
  establecerFecha();
  cargarDatosTicket();
}

function generarIdTicket() {
  // Generar un ID único para el ticket
  const timestamp = Date.now();
  const random = Math.floor(Math.random() * 1000);
  ticketId = `TKT-${timestamp}-${random}`;
  document.getElementById('ticket-id').textContent = ticketId;
}

function establecerFecha() {
  const fecha = new Date();
  const opciones = {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  };
  document.getElementById('fecha-compra').textContent = 
    fecha.toLocaleDateString('es-ES', opciones);
}

// ===== FUNCIONES DE CARGA DE DATOS =====
function cargarDatosTicket() {
  // Intentar obtener datos desde localStorage
  const carritoLocalStorage = localStorage.getItem('carritoTemporal');
  
  if (carritoLocalStorage) {
    try {
      ticketData = JSON.parse(carritoLocalStorage);
      // Vaciar el carrito después de procesar el pago
      localStorage.removeItem('carritoTemporal');
      generarTicket();
      return;
    } catch (error) {
      console.error("Error al parsear carrito desde localStorage:", error);
    }
  }
  
  // Si no hay datos, mostrar mensaje
  mostrarMensajeVacio();
}

function generarTicket() {
  if (!ticketData || ticketData.length === 0) {
    mostrarMensajeVacio();
    return;
  }
  
  renderizarTicket();
}

// ===== FUNCIONES DE RENDERIZADO =====
function mostrarMensajeVacio() {
  const content = document.getElementById('ticket-content');
  content.innerHTML = `
    <div class="ticket-items">
      <div style="text-align: center; padding: 40px; color: #718096;">
        <span style="font-size: 3em; display: block; margin-bottom: 20px;">🛒</span>
        <h3>No hay datos de compra</h3>
        <p>No se encontraron productos en el carrito para generar el ticket.</p>
        <button class="btn btn-primary" onclick="volverInicio()" style="margin-top: 20px;">
          🏠 Volver al Inicio
        </button>
      </div>
    </div>
  `;
}

function renderizarTicket() {
  const content = document.getElementById('ticket-content');
  const totalElement = document.getElementById('total-compra');
  
  let total = 0;
  let html = '';
  
  // Crear sección de items
  html += '<div class="ticket-items">';
  html += '<h3 style="margin: 0 0 20px 0; color: #2d3748; font-size: 1.3em;">📦 Productos Comprados</h3>';
  
  ticketData.forEach((item, index) => {
    total += Number(item.precio);
    
    html += `
      <div class="ticket-item">
        <div class="ticket-item-info">
          <div class="ticket-item-nombre">${item.nombre}</div>
          <div class="ticket-item-details">
            <div class="ticket-item-detail">
              <span>🏷️</span>
              <span>${item.marca}</span>
            </div>
            <div class="ticket-item-detail">
              <span>📂</span>
              <span>${item.categoria}</span>
            </div>
            <div class="ticket-item-detail">
              <span>📝</span>
              <span>${item.descripcion}</span>
            </div>
          </div>
        </div>
        <div class="ticket-item-precio">$${Number(item.precio).toFixed(2)}</div>
      </div>
    `;
  });
  
  html += '</div>';
  
  // Crear resumen de compra
  html += '<div class="ticket-summary">';
  html += '<h3 style="margin: 0 0 15px 0; color: #2d3748; font-size: 1.3em;">💰 Resumen de Compra</h3>';
  html += `<div class="ticket-summary-row">
    <span class="ticket-summary-label">Cantidad de productos:</span>
    <span class="ticket-summary-value">${ticketData.length}</span>
  </div>`;
  html += `<div class="ticket-summary-row">
    <span class="ticket-summary-label">Total:</span>
    <span class="ticket-summary-value">$${total.toFixed(2)}</span>
  </div>`;
  html += '</div>';
  
  content.innerHTML = html;
  totalElement.textContent = `$${total.toFixed(2)}`;
}

// ===== FUNCIONES DE ACCIONES =====
function volverInicio() {
  window.location.href = 'index.html';
}

// ===== INICIALIZACIÓN AL CARGAR LA PÁGINA =====
document.addEventListener('DOMContentLoaded', inicializarTicket);

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', inicializarTicket);
} else {
  inicializarTicket();
}

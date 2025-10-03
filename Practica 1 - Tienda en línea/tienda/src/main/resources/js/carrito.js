// ===== VARIABLES GLOBALES =====
let carritoData = [];

// ===== FUNCIONES DE CARGA DE DATOS =====
function cargarCarritoDesdeStorage() {
  try {
    const carritoTemporal = localStorage.getItem('carritoTemporal');
    if (carritoTemporal) {
      carritoData = JSON.parse(carritoTemporal);
      return true;
    }
    return false;
  } catch (error) {
    console.error("Error al cargar carrito desde localStorage:", error);
    return false;
  }
}

function mostrarCarritoCompleto(carritoArrayJson, totalCarrito) {
  try {
    carritoData = JSON.parse(carritoArrayJson);
    renderizarCarrito();
  } catch (error) {
    console.error("Error al parsear carrito desde Java:", error);
    mostrarError("Error al cargar el carrito");
  }
}

function mostrarCarrito(carritoString) {
  parseCarritoData(carritoString);
  renderizarCarrito();
}

function parseCarritoData(carritoString) {
  try {
    carritoData = JSON.parse(carritoString);
  } catch (error) {
    console.error("Error al parsear carrito:", error);
    carritoData = [];
  }
}

// ===== FUNCIONES DE RENDERIZADO =====
function mostrarError(mensaje) {
  const itemsContainer = document.getElementById('carrito-items');
  itemsContainer.innerHTML = `
    <div class="error-message" style="text-align: center; padding: 40px; color: #e53e3e; background: #fed7d7; border-radius: 8px; margin: 20px 0;">
      <h3>⚠️ Error</h3>
      <p>${mensaje}</p>
    </div>
  `;
}

function renderizarCarrito() {
  const itemsContainer = document.getElementById('carrito-items');
  const contadorElement = document.getElementById('contador');
  const totalElement = document.getElementById('carrito-total');
  const vacioElement = document.getElementById('carrito-vacio');
  const accionesElement = document.getElementById('acciones');
  const totalAmountElement = document.getElementById('total-amount');
  
  itemsContainer.innerHTML = '';
  
  if (!carritoData || carritoData.length === 0) {
    vacioElement.style.display = 'block';
    totalElement.style.display = 'none';
    accionesElement.style.display = 'none';
    contadorElement.textContent = '0 artículos';
  } else {
    vacioElement.style.display = 'none';
    totalElement.style.display = 'block';
    accionesElement.style.display = 'flex';
    
    let total = 0;
    
    carritoData.forEach((item, index) => {
      const itemElement = document.createElement('div');
      itemElement.className = 'carrito-item';
      itemElement.innerHTML = `
        <div class="item-info">
          <div class="item-nombre">${item.nombre}</div>
          <div class="item-marca">🏷️ ${item.marca}</div>
          <div class="item-categoria">📂 ${item.categoria}</div>
          <div class="item-descripcion">${item.descripcion}</div>
        </div>
        <div class="item-actions">
          <div class="item-precio">$${Number(item.precio).toFixed(2)}</div>
          <button class="btn-eliminar" onclick="eliminarProducto(${index})" title="Eliminar producto">
            🗑️
          </button>
        </div>
      `;
      itemsContainer.appendChild(itemElement);
      total += Number(item.precio);
    });
    
    contadorElement.textContent = `${carritoData.length} artículo${carritoData.length !== 1 ? 's' : ''}`;
    totalAmountElement.textContent = `$${total.toFixed(2)}`;
  }
}

// ===== FUNCIONES DE ACCIONES DEL CARRITO =====
function procederCompra() {
    if (carritoData.length==0){
        alert("El carrito está vacío. Agrega productos antes de proceder a la compra.");
    }else{
        const carritoJson = JSON.stringify(carritoData);
        // Llamar al método Java del cliente usando el nombre correcto
        if (window.clienteJava && window.clienteJava.procesarCompra) {
            window.clienteJava.procesarCompra(carritoJson);
            vaciarCarrito();
        } else {
            console.error("Método clienteJava.procesarCompra no disponible");
            alert("Error: No se puede procesar la compra en este momento");
        }
    }
}

function vaciarCarrito() {
  carritoData = [];
  localStorage.removeItem('carritoTemporal');
  renderizarCarrito();
}

function eliminarProducto(index) {
  carritoData.splice(index, 1);
  if (carritoData.length > 0) {
    localStorage.setItem('carritoTemporal', JSON.stringify(carritoData));
  } else {
    localStorage.removeItem('carritoTemporal');
  }
  renderizarCarrito();
}

// ===== INICIALIZACIÓN =====
function inicializarCarrito() {
  if (cargarCarritoDesdeStorage()) {
    renderizarCarrito();
  } else {
    renderizarCarrito();
  }
}

document.addEventListener('DOMContentLoaded', inicializarCarrito);

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', inicializarCarrito);
} else {
  inicializarCarrito();
}
// ===== VARIABLES GLOBALES =====
let todosLosProductos = [];

// ===== FUNCIONES DE GESTIÓN DE PRODUCTOS =====
function limpiarProductos() {
  document.getElementById('producto').innerHTML = '';
  actualizarContadorProductos(0);
}

function agregarProducto(jsonStr) {
  const data = JSON.parse(jsonStr);
  todosLosProductos.push(data);
  
  const productoDiv = document.createElement('div');
  productoDiv.className = 'producto-item';
  productoDiv.innerHTML = `
    <div class="producto-header">
      <h3 class="producto-nombre">${data.nombre}</h3>
      <span class="producto-categoria">${data.categoria}</span>
    </div>
    
    <div class="producto-details">
      <div class="detail-item">
        <span class="detail-label">🏷️ Marca</span>
        <span class="detail-value">${data.marca}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">📊 Stock</span>
        <span class="detail-value">${data.cantidad} unidades</span>
      </div>
    </div>
    
    <div class="producto-descripcion">
      <div class="detail-label">📝 Descripción</div>
      <div style="margin-top: 8px; color: #555; line-height: 1.5;">
        ${data.descripcion}
      </div>
    </div>
    
    <div class="producto-footer">
      <div class="precio-section">
        <div class="precio-label">Precio</div>
        <div class="precio-valor">$${data.precio}</div>
        <button class="btn btn-cart" onclick="agregarAlCarrito('${encodeURIComponent(JSON.stringify(data))}')">🛒 Añadir al Carrito</button>
      </div>
      <div class="stock-section">
        <div class="stock-label">Disponibilidad</div>
        <div class="stock-valor">${data.cantidad > 0 ? 'En stock' : 'Agotado'}</div>
      </div>
    </div>
  `;
  
  document.getElementById('producto').appendChild(productoDiv);
  actualizarContadorProductos(todosLosProductos.length);
}

function mostrarProducto(jsonStr) {
  limpiarProductos();
  agregarProducto(jsonStr);
}

function mostrarEstadoVacio() {
  document.getElementById('producto').innerHTML = `
    <div class="empty-state">
      <span class="emoji">📦</span>
      <h3>No hay productos disponibles</h3>
      <p>El catálogo está vacío o no se pudo conectar al servidor</p>
    </div>
  `;
}

// ===== FUNCIONES DE INTERFAZ =====
function actualizarContadorProductos(cantidad) {
  document.getElementById('total-productos').textContent = cantidad;
}

function filtrarProductos() {
  const filtro = document.getElementById('buscar').value.toLowerCase();
  const productos = document.querySelectorAll('.producto-item');
  
  productos.forEach(producto => {
    const texto = producto.textContent.toLowerCase();
    if (texto.includes(filtro)) {
      producto.style.display = 'block';
    } else {
      producto.style.display = 'none';
    }
  });
}

function recargarProductos() {
  location.reload();
}

// ===== FUNCIONES DE CARRITO =====
function agregarAlCarrito(productoJson) {
  try {
    const producto = JSON.parse(decodeURIComponent(productoJson));
    producto.id = Date.now() + Math.random();
    
    let carrito = JSON.parse(localStorage.getItem('carritoTemporal') || '[]');
    carrito.push(producto);
    localStorage.setItem('carritoTemporal', JSON.stringify(carrito));
    
    window.open('carrito.html', '_blank');
  } catch (error) {
    console.error('Error al agregar al carrito:', error);
  }
}

// ===== INICIALIZACIÓN =====
document.getElementById('buscar').addEventListener('input', filtrarProductos);

setTimeout(() => {
  if (todosLosProductos.length === 0) {
    mostrarEstadoVacio();
  }
}, 5000);
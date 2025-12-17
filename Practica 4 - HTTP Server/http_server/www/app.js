/**
 * JavaScript para la interfaz web del servidor HTTP
 * Permite navegar archivos y probar métodos HTTP
 */

// Detectar puerto actual y cargar archivos
window.addEventListener('DOMContentLoaded', function() {
    const port = window.location.port || '8080';
    document.getElementById('serverPort').textContent = port;
    
    // Cargar lista de archivos del directorio uploads/
    loadUploadsFiles();
});

/**
 * Abre un archivo en una nueva ventana
 */
function openFile(filename) {
    const url = '/' + filename;
    window.open(url, '_blank');
    
    // Mostrar feedback
    showNotification('Abriendo ' + filename + '...', 'info');
}

/**
 * Prueba un método HTTP
 */
async function testMethod(method) {
    let resource = document.getElementById('testResource').value.trim();
    const body = document.getElementById('testBody').value;
    const output = document.getElementById('responseOutput');
    
    // Asegurar que el recurso empiece con /
    if (!resource.startsWith('/')) {
        resource = '/' + resource;
    }

    // Mostrar que está cargando
    output.textContent = 'Enviando ' + method + ' request...\n';

    try {
        const options = {
            method: method,
            headers: {}
        };

        // Agregar body si es POST o PUT
        if ((method === 'POST' || method === 'PUT') && body.trim()) {
            options.body = body;
            options.headers['Content-Type'] = 'application/json';
        }

        const response = await fetch(resource, options);
        
        // Construir información de la respuesta
        let result = '========== RESPONSE ==========\n';
        result += 'Status: ' + response.status + ' ' + response.statusText + '\n';
        result += 'Headers:\n';
        
        response.headers.forEach((value, key) => {
            result += '  ' + key + ': ' + value + '\n';
        });

        result += '\n========== BODY ==========\n';

        // Para HEAD no hay body
        if (method === 'HEAD') {
            result += '(HEAD request - sin body)';
        } else {
            const text = await response.text();
            result += text || '(vacío)';
        }

        output.textContent = result;
        showNotification(method + ' request completado', 'success');
        
        // Actualizar lista de archivos si fue POST, PUT o DELETE exitoso
        if ((method === 'POST' || method === 'PUT' || method === 'DELETE') && response.ok) {
            setTimeout(() => {
                loadUploadsFiles();
                showNotification('📋 Lista de archivos actualizada', 'info');
            }, 500);
        }

    } catch (error) {
        output.textContent = 'ERROR: ' + error.message;
        showNotification('Error en ' + method + ' request', 'error');
    }
}



/**
 * Muestra una notificación temporal
 */
function showNotification(message, type) {
    // Crear elemento de notificación
    const notification = document.createElement('div');
    notification.className = 'notification ' + type;
    notification.textContent = message;
    
    // Estilos inline
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 25px;
        background: ${type === 'success' ? '#28a745' : type === 'error' ? '#dc3545' : '#17a2b8'};
        color: white;
        border-radius: 6px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.3);
        z-index: 1000;
        animation: slideIn 0.3s ease;
        font-weight: 600;
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(400px);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(400px);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

// Log de inicio
console.log('Servidor HTTP - Interfaz Web cargada');

/**
 * Carga dinámicamente los archivos del directorio uploads/
 */
async function loadUploadsFiles() {
    const container = document.getElementById('uploadsList');
    
    try {
        // Mostrar mensaje de carga
        container.innerHTML = '<p style="color: #d4a574; text-align: center; padding: 20px;">Cargando archivos...</p>';
        
        const response = await fetch('/api/files');
        
        if (!response.ok) {
            throw new Error('Error al cargar archivos');
        }
        
        const files = await response.json();
        
        if (files.length === 0) {
            container.innerHTML = '<p style="color: #d4a574; text-align: center; padding: 20px;">No hay archivos en el directorio uploads/</p>';
            return;
        }
        
        // Limpiar contenedor
        container.innerHTML = '';
        
        // Crear elementos para cada archivo
        files.forEach(file => {
            const fileItem = document.createElement('div');
            fileItem.className = 'file-item';
            fileItem.onclick = () => openFile('uploads/' + file.name);
            
            fileItem.innerHTML = `
                <span class="file-icon">${file.icon}</span>
                <span class="file-name">${file.name}</span>
                <span class="file-type">${file.mimeType}</span>
            `;
            
            container.appendChild(fileItem);
        });
        
        showNotification(`${files.length} archivo(s) cargado(s)`, 'success');
        
    } catch (error) {
        console.error('Error cargando archivos:', error);
        container.innerHTML = '<p style="color: #8b4513; text-align: center; padding: 20px;">⚠️ Error al cargar archivos. Verifica que el servidor esté funcionando.</p>';
    }
}

/**
 * Recarga la lista de archivos
 */
function refreshFiles() {
    loadUploadsFiles();
}
console.log('Puerto:', window.location.port || '8080');
console.log('Host:', window.location.hostname);

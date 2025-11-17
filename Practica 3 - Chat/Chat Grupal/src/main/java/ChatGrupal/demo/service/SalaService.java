package ChatGrupal.demo.service;

import ChatGrupal.demo.model.Sala;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SalaService {
    
    private static final Logger logger = LoggerFactory.getLogger(SalaService.class);
    private final Map<String, Sala> salas = new ConcurrentHashMap<>();

    /**
     * Obtiene o crea una sala
     */
    public Sala obtenerOCrearSala(String nombreSala) {
        return salas.computeIfAbsent(nombreSala, Sala::new);
    }

    /**
     * Agrega un usuario a una sala
     */
    public Set<String> agregarUsuarioASala(String nombreSala, String usuario) {
        Sala sala = obtenerOCrearSala(nombreSala);
        sala.agregarUsuario(usuario);
        logger.debug("[SERVICIO] Nueva sala creada/accedida: '{}'", nombreSala);
        logger.info("  Usuario '{}' se unio a la sala '{}'", usuario, nombreSala);
        logger.info("  Usuarios actuales en '{}': {}", nombreSala, sala.getUsuarios());
        return new HashSet<>(sala.getUsuarios());
    }

    /**
     * Remueve un usuario de una sala
     */
    public Set<String> removerUsuarioDeSala(String nombreSala, String usuario) {
        Sala sala = salas.get(nombreSala);
        if (sala != null) {
            sala.removerUsuario(usuario);
            logger.warn("  Usuario '{}' abandono la sala '{}'", usuario, nombreSala);
            
            // Si la sala queda vacia, la eliminamos
            if (sala.getUsuarios().isEmpty()) {
                salas.remove(nombreSala);
                logger.warn("  Sala '{}' eliminada (vacia)", nombreSala);
            } else {
                logger.info("  Usuarios restantes en '{}': {}", nombreSala, sala.getUsuarios());
            }
            
            return new HashSet<>(sala.getUsuarios());
        }
        return new HashSet<>();
    }

    /**
     * Obtiene los usuarios de una sala
     */
    public Set<String> obtenerUsuariosDeSala(String nombreSala) {
        Sala sala = salas.get(nombreSala);
        return sala != null ? new HashSet<>(sala.getUsuarios()) : new HashSet<>();
    }

    /**
     * Obtiene todas las salas
     */
    public Collection<Sala> obtenerTodasLasSalas() {
        return salas.values();
    }

    /**
     * Verifica si una sala existe
     */
    public boolean salaExiste(String nombreSala) {
        return salas.containsKey(nombreSala);
    }
}

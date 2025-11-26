package ChatGrupal.demo.controller;

import ChatGrupal.demo.model.ChatMessage;
import ChatGrupal.demo.service.SalaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Controller
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SalaService salaService;

    /**
     * Usuario se une a una sala
     */
    @MessageMapping("/chat/{sala}/join")
    public void unirseASala(@DestinationVariable String sala, ChatMessage mensaje) {
        logger.info("==================================================================");
        logger.info("[CLIENTE -> SERVIDOR] Operacion: JOIN");
        logger.info("  Usuario: {}", mensaje.getUsuario());
        logger.info("  Sala: {}", sala);
        logger.info("  Timestamp: {}", LocalDateTime.now());
        logger.info("==================================================================");
        
        // Agregar usuario a la sala
        Set<String> usuarios = salaService.agregarUsuarioASala(sala, mensaje.getUsuario());
        
        // Crear mensaje de bienvenida
        ChatMessage bienvenida = new ChatMessage();
        bienvenida.setId(UUID.randomUUID().toString());
        bienvenida.setUsuario("Sistema");
        bienvenida.setSala(sala);
        bienvenida.setContenido(mensaje.getUsuario() + " se ha unido a la sala");
        bienvenida.setTipo(ChatMessage.TipoMensaje.JOIN);
        bienvenida.setTimestamp(LocalDateTime.now());
        
        // Enviar mensaje de bienvenida a todos en la sala
        messagingTemplate.convertAndSend("/topic/sala/" + sala, bienvenida);
        logger.info("[SERVIDOR -> CLIENTES] Mensaje de bienvenida enviado a sala: {}", sala);
        
        // Enviar lista actualizada de usuarios
        ChatMessage usuariosMsg = new ChatMessage();
        usuariosMsg.setId(UUID.randomUUID().toString());
        usuariosMsg.setTipo(ChatMessage.TipoMensaje.USUARIOS_ACTUALIZADOS);
        usuariosMsg.setSala(sala);
        usuariosMsg.setContenido(String.join(",", usuarios));
        usuariosMsg.setTimestamp(LocalDateTime.now());
        
        messagingTemplate.convertAndSend("/topic/sala/" + sala + "/usuarios", usuariosMsg);
        logger.info("[SERVIDOR -> CLIENTES] Lista de usuarios actualizada: {}", usuarios);
    }

    /**
     * Usuario abandona una sala
     */
    @MessageMapping("/chat/{sala}/leave")
    public void abandonarSala(@DestinationVariable String sala, ChatMessage mensaje) {
        logger.warn("==================================================================");
        logger.warn("[CLIENTE -> SERVIDOR] Operacion: LEAVE");
        logger.warn("  Usuario: {}", mensaje.getUsuario());
        logger.warn("  Sala: {}", sala);
        logger.warn("  Timestamp: {}", LocalDateTime.now());
        logger.warn("==================================================================");
        
        // Remover usuario de la sala
        Set<String> usuarios = salaService.removerUsuarioDeSala(sala, mensaje.getUsuario());
        
        // Crear mensaje de despedida
        ChatMessage despedida = new ChatMessage();
        despedida.setId(UUID.randomUUID().toString());
        despedida.setUsuario("Sistema");
        despedida.setSala(sala);
        despedida.setContenido(mensaje.getUsuario() + " ha abandonado la sala");
        despedida.setTipo(ChatMessage.TipoMensaje.LEAVE);
        despedida.setTimestamp(LocalDateTime.now());
        
        // Enviar mensaje de despedida
        messagingTemplate.convertAndSend("/topic/sala/" + sala, despedida);
        logger.info("[SERVIDOR -> CLIENTES] Mensaje de despedida enviado a sala: {}", sala);
        
        // Enviar lista actualizada de usuarios
        ChatMessage usuariosMsg = new ChatMessage();
        usuariosMsg.setId(UUID.randomUUID().toString());
        usuariosMsg.setTipo(ChatMessage.TipoMensaje.USUARIOS_ACTUALIZADOS);
        usuariosMsg.setSala(sala);
        usuariosMsg.setContenido(String.join(",", usuarios));
        usuariosMsg.setTimestamp(LocalDateTime.now());
        
        messagingTemplate.convertAndSend("/topic/sala/" + sala + "/usuarios", usuariosMsg);
        logger.info("[SERVIDOR -> CLIENTES] Lista de usuarios actualizada: {}", usuarios);
    }

    /**
     * Enviar mensaje a la sala
     */
    @MessageMapping("/chat/{sala}/send")
    @SendTo("/topic/sala/{sala}")
    public ChatMessage enviarMensaje(@DestinationVariable String sala, ChatMessage mensaje) {
        logger.info("==================================================================");
        logger.info("[CLIENTE -> SERVIDOR] Operacion: SEND MESSAGE");
        logger.info("  De: {}", mensaje.getUsuario());
        logger.info("  Sala: {}", sala);
        logger.info("  Mensaje: {}", mensaje.getContenido());
        logger.info("  Timestamp: {}", LocalDateTime.now());
        logger.info("==================================================================");
        
        mensaje.setId(UUID.randomUUID().toString());
        mensaje.setSala(sala);
        mensaje.setTimestamp(LocalDateTime.now());
        
        logger.info("[SERVIDOR -> CLIENTES] Mensaje retransmitido a sala: {}", sala);
        logger.info("  ID Mensaje: {}", mensaje.getId());
        
        return mensaje;
    }

    /**
     * Enviar mensaje privado (incluye soporte para emojis)
     */
    @MessageMapping("/chat/private")
    public void enviarMensajePrivado(ChatMessage mensaje) {
        logger.info("==================================================================");
        logger.info("[CLIENTE -> SERVIDOR] Operacion: PRIVATE MESSAGE");
        logger.info("  De: {}", mensaje.getUsuario());
        logger.info("  Para: {}", mensaje.getDestinatario());
        logger.info("  Mensaje: {}", mensaje.getContenido());
        logger.info("  Tipo: {}", mensaje.getTipo());
        logger.info("  Timestamp: {}", LocalDateTime.now());
        logger.info("==================================================================");
        
        mensaje.setId(UUID.randomUUID().toString());
        
        // Si el mensaje es solo emojis pero viene como TEXTO, detectarlo y cambiar el tipo
        if (mensaje.getTipo() == ChatMessage.TipoMensaje.TEXTO && esEmoji(mensaje.getContenido())) {
            mensaje.setTipo(ChatMessage.TipoMensaje.EMOJI);
            logger.info("  Detectado emoji en mensaje privado, tipo cambiado a EMOJI");
        } else if (mensaje.getTipo() != ChatMessage.TipoMensaje.EMOJI) {
            mensaje.setTipo(ChatMessage.TipoMensaje.PRIVADO);
        }
        
        mensaje.setTimestamp(LocalDateTime.now());
        
        // Enviar al destinatario en su cola privada especifica
        messagingTemplate.convertAndSend(
            "/queue/private/" + mensaje.getDestinatario(), 
            mensaje
        );
        logger.info("[SERVIDOR -> CLIENTE] Mensaje privado enviado a: {} (Tipo: {})", 
                    mensaje.getDestinatario(), mensaje.getTipo());
        
        // Tambien enviar al remitente para que vea su mensaje
        messagingTemplate.convertAndSend(
            "/queue/private/" + mensaje.getUsuario(), 
            mensaje
        );
        logger.info("[SERVIDOR -> CLIENTE] Copia del mensaje privado enviada al remitente: {}", mensaje.getUsuario());
    }
    
    /**
     * Detecta si un texto contiene solo emojis
     */
    private boolean esEmoji(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return false;
        }
        String emojiPattern = "[\\p{So}\\p{Cn}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+";
        return texto.trim().matches(emojiPattern);
    }
}

package ChatGrupal.demo.model;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatMessage {
    private String id;
    private String usuario;
    private String sala;
    private String contenido;
    private TipoMensaje tipo;
    private LocalDateTime timestamp;
    @JsonProperty("destinatario")
    private String destinatario; // Para mensajes privados

    public enum TipoMensaje {
        TEXTO,
        EMOJI,
        STICKER,
        AUDIO,
        PRIVADO,
        JOIN,
        LEAVE,
        USUARIOS_ACTUALIZADOS
    }

    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(String usuario, String sala, String contenido, TipoMensaje tipo) {
        this.usuario = usuario;
        this.sala = sala;
        this.contenido = contenido;
        this.tipo = tipo;
        this.timestamp = LocalDateTime.now();
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getSala() {
        return sala;
    }

    public void setSala(String sala) {
        this.sala = sala;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public TipoMensaje getTipo() {
        return tipo;
    }

    public void setTipo(TipoMensaje tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id='" + id + '\'' +
                ", usuario='" + usuario + '\'' +
                ", sala='" + sala + '\'' +
                ", contenido='" + contenido + '\'' +
                ", tipo=" + tipo +
                ", timestamp=" + timestamp +
                ", destinatario='" + destinatario + '\'' +
                '}';
    }
}

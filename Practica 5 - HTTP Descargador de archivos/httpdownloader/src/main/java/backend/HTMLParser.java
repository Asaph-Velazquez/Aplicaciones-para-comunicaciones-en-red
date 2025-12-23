package backend;

import java.util.*;
import java.util.regex.*;

/**
 * Parser simple de HTML para extraer enlaces y recursos.
 * Usado para implementar descarga recursiva estilo wget -r
 */
public class HTMLParser {
    
    /**
     * Extrae todos los enlaces href de un documento HTML
     * @param html Contenido HTML a parsear
     * @param baseUrl URL base para resolver enlaces relativos
     * @return Lista de URLs absolutas encontradas
     */
    public static List<String> extractLinks(String html, String baseUrl) {
        List<String> links = new ArrayList<>();
        
        // Patrón para encontrar etiquetas <a href="...">
        Pattern pattern = Pattern.compile(
            "<a\\s+[^>]*href\\s*=\\s*[\"']([^\"']*)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern.matcher(html);
        
        while (matcher.find()) {
            String link = matcher.group(1);
            
            // Ignorar enlaces vacíos, anclas y javascript
            if (link.isEmpty() || link.startsWith("#") || 
                link.startsWith("javascript:") || link.startsWith("mailto:")) {
                continue;
            }
            
            // Resolver URL relativa a absoluta
            String absoluteUrl = resolveUrl(baseUrl, link);
            if (absoluteUrl != null && !links.contains(absoluteUrl)) {
                links.add(absoluteUrl);
            }
        }
        
        return links;
    }
    
    /**
     * Extrae recursos adicionales (imágenes, CSS, JS, etc.)
     * @param html Contenido HTML a parsear
     * @param baseUrl URL base para resolver rutas relativas
     * @return Lista de URLs de recursos
     */
    public static List<String> extractResources(String html, String baseUrl) {
        List<String> resources = new ArrayList<>();
        
        // Patrones para diferentes tipos de recursos
        String[] patterns = {
            "<img\\s+[^>]*src\\s*=\\s*[\"']([^\"']*)[\"']",           // Imágenes
            "<link\\s+[^>]*href\\s*=\\s*[\"']([^\"']*)[\"']",         // CSS
            "<script\\s+[^>]*src\\s*=\\s*[\"']([^\"']*)[\"']"         // JavaScript
        };
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(html);
            
            while (matcher.find()) {
                String resource = matcher.group(1);
                
                // Ignorar recursos vacíos o data URIs
                if (resource.isEmpty() || resource.startsWith("data:")) {
                    continue;
                }
                
                String absoluteUrl = resolveUrl(baseUrl, resource);
                if (absoluteUrl != null && !resources.contains(absoluteUrl)) {
                    resources.add(absoluteUrl);
                }
            }
        }
        
        return resources;
    }
    
    /**
     * Resuelve una URL relativa a absoluta
     * @param baseUrl URL base
     * @param relativeUrl URL que puede ser relativa o absoluta
     * @return URL absoluta o null si hay error
     */
    private static String resolveUrl(String baseUrl, String relativeUrl) {
        try {
            java.net.URL base = new java.net.URL(baseUrl);
            java.net.URL resolved = new java.net.URL(base, relativeUrl);
            return resolved.toString();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Verifica si una URL pertenece al mismo dominio
     * @param baseUrl URL base
     * @param targetUrl URL a verificar
     * @return true si es del mismo dominio
     */
    public static boolean isSameDomain(String baseUrl, String targetUrl) {
        try {
            java.net.URL base = new java.net.URL(baseUrl);
            java.net.URL target = new java.net.URL(targetUrl);
            return base.getHost().equalsIgnoreCase(target.getHost());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Normaliza una URL para evitar duplicados
     */
    public static String normalizeUrl(String url) {
        // Eliminar fragmentos (#)
        int hashIndex = url.indexOf('#');
        if (hashIndex != -1) {
            url = url.substring(0, hashIndex);
        }
        
        // Eliminar slash final si es un directorio
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        
        return url;
    }
}

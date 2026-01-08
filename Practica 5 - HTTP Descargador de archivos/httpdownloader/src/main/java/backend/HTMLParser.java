package backend;

import java.util.*;
import java.util.regex.*;

public class HTMLParser {
    
    public static List<String> extractLinks(String html, String baseUrl) {
        List<String> links = new ArrayList<>();
        
        Pattern pattern = Pattern.compile(
            "<a\\s+[^>]*href\\s*=\\s*[\"']([^\"']*)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern.matcher(html);
        
        while (matcher.find()) {
            String link = matcher.group(1);
            
            if (link.isEmpty() || link.startsWith("#") || 
                link.startsWith("javascript:") || link.startsWith("mailto:")) {
                continue;
            }
            
            String absoluteUrl = resolveUrl(baseUrl, link);
            if (absoluteUrl != null && !links.contains(absoluteUrl)) {
                links.add(absoluteUrl);
            }
        }
        
        return links;
    }
    
    public static List<String> extractResources(String html, String baseUrl) {
        List<String> resources = new ArrayList<>();
        
        String[] patterns = {
            "<img\\s+[^>]*src\\s*=\\s*[\"']([^\"']*)[\"']",
            "<link\\s+[^>]*href\\s*=\\s*[\"']([^\"']*)[\"']",
            "<script\\s+[^>]*src\\s*=\\s*[\"']([^\"']*)[\"']"
        };
        
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(html);
            
            while (matcher.find()) {
                String resource = matcher.group(1);
                
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
    
    public static List<String> extractDirectoryListing(String html, String baseUrl) {
        List<String> files = new ArrayList<>();
        
        boolean isDirectoryListing = 
            html.contains("Index of") || 
            html.contains("Directory listing") ||
            html.contains("Parent Directory") ||
            html.contains("<title>Index of") ||
            html.matches("(?s).*<h1>Index of.*</h1>.*") ||
            (html.contains("<table") && html.contains("<a href=") && 
             (html.contains("Last modified") || html.contains("Size") || html.contains("Description"))) ||
            (html.contains("<pre>") && html.split("<a href=").length > 3);
        
        if (!isDirectoryListing) {
            return files;
        }
        
        System.out.println("📂 Detectado listado de directorio en: " + baseUrl);
        
        if (html.length() < 5000) {
            System.out.println("📋 HTML del listado (primeros caracteres):");
            System.out.println(html.substring(0, Math.min(500, html.length())));
        }
        
        Pattern pattern = Pattern.compile(
            "<a\\s+href\\s*=\\s*[\"']([^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern.matcher(html);
        
        while (matcher.find()) {
            String href = matcher.group(1);
            
            if (href.isEmpty() || 
                href.equals("/") ||
                href.equals("../") ||
                href.equals("?") ||
                href.startsWith("?") ||
                href.equals(".") ||
                href.startsWith("#") ||
                href.startsWith("javascript:") ||
                href.startsWith("mailto:") ||
                href.startsWith("http://") ||
                href.startsWith("https://")) {
                continue;
            }
            
            String absoluteUrl = resolveUrl(baseUrl, href);
            
            if (absoluteUrl != null && !files.contains(absoluteUrl)) {
                if (href.endsWith("/") && !absoluteUrl.endsWith("/")) {
                    absoluteUrl += "/";
                }
                
                files.add(absoluteUrl);
                
                if (absoluteUrl.endsWith("/")) {
                    System.out.println("  📁 Subdirectorio: " + href);
                } else {
                    System.out.println("  📄 Archivo: " + href);
                }
            }
        }
        
        System.out.println("✅ Total de archivos/directorios encontrados: " + files.size());
        
        return files;
    }
    
    public static boolean isLikelyFile(String url) {
        String[] fileExtensions = {
            ".html", ".htm", ".txt", ".pdf", ".doc", ".docx",
            ".xls", ".xlsx", ".ppt", ".pptx", ".odt", ".ods", ".odp",
            ".zip", ".rar", ".tar", ".gz", ".7z", ".tgz", ".bz2",
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".svg", ".ico",
            ".webp", ".tiff", ".tif",
            ".css", ".js", ".json", ".xml", ".jsp", ".php",
            ".java", ".c", ".cpp", ".h", ".hpp", ".py", ".rb",
            ".go", ".rs", ".ts", ".jsx", ".tsx", ".vue", ".md",
            ".cs", ".vb", ".swift", ".kt", ".scala",
            ".csv", ".sql", ".db", ".sqlite",
            ".sh", ".bat", ".cmd", ".ps1",
            ".exe", ".dll", ".so", ".jar", ".war", ".ear",
            ".mp3", ".mp4", ".avi", ".mov", ".wav", ".flac",
            ".mkv", ".webm", ".ogg", ".m4a", ".aac",
            ".ttf", ".otf", ".woff", ".woff2", ".eot",
            ".class", ".properties", ".ini", ".cfg", ".conf",
            ".log", ".dat", ".bin"
        };
        
        String lowerUrl = url.toLowerCase();
        
        int queryIndex = lowerUrl.indexOf('?');
        if (queryIndex > 0) {
            lowerUrl = lowerUrl.substring(0, queryIndex);
        }
        
        for (String ext : fileExtensions) {
            if (lowerUrl.endsWith(ext)) {
                return true;
            }
        }
        
        return false;
    }
    
    private static String resolveUrl(String baseUrl, String relativeUrl) {
        try {
            if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
                return relativeUrl;
            }
            
            String normalizedBase = baseUrl;
            if (!normalizedBase.endsWith("/")) {
                normalizedBase = normalizedBase + "/";
            }
            
            java.net.URL base = new java.net.URL(normalizedBase);
            java.net.URL resolved = new java.net.URL(base, relativeUrl);
            String result = resolved.toString();
            
            if (!relativeUrl.equals("../") && !relativeUrl.equals("?")) {
                System.out.println("      🔗 Resolviendo: '" + relativeUrl + "' + base:'" + normalizedBase + "' = '" + result + "'");
            }
            
            return result;
        } catch (Exception e) {
            System.err.println("      ❌ Error resolviendo URL: base='" + baseUrl + "', relativa='" + relativeUrl + "': " + e.getMessage());
            return null;
        }
    }
    
    public static boolean isSameDomain(String baseUrl, String targetUrl) {
        try {
            java.net.URL base = new java.net.URL(baseUrl);
            java.net.URL target = new java.net.URL(targetUrl);
            return base.getHost().equalsIgnoreCase(target.getHost());
        } catch (Exception e) {
            return false;
        }
    }
    
    public static String normalizeUrl(String url) {
        int hashIndex = url.indexOf('#');
        if (hashIndex != -1) {
            url = url.substring(0, hashIndex);
        }
        
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        
        return url;
    }
}

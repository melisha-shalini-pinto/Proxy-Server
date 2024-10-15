import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ProxyServer {
    private static final int PORT = 8081;
    private static final String MAIN_SERVER_URL = "http://localhost:8080/";
    private static final String CACHE_DIR = "proxy_cache/";

    private Map<String, File> cache = new HashMap<>();

    public static void main(String[] args) throws IOException {
        new ProxyServer().start();
    }

    public void start() throws IOException {
        // Create the cache directory if it doesn't exist
        File cacheDir = new File(CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }

        //HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

        // Set a context for the proxy
        server.createContext("/", new ProxyHandler());

        // Start the server
        server.setExecutor(null);
        server.start();

        System.out.println("Proxy server is listening on port " + PORT);
    }

    private class ProxyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
         
            String clientAddress = exchange.getRemoteAddress().getAddress().getHostAddress();

            String requestURI = exchange.getRequestURI().toString();
            String fileName = requestURI.equals("/") ? "index.html" : requestURI.substring(1);
            File cachedFile = cache.get(fileName);

            // Log the client's IP address and requested HTML page
            System.out.println("Client IP: " + clientAddress + " Requested Page: " + fileName);

            if (cachedFile != null && cachedFile.exists()) {
                // Serve the file from cache
                System.out.println("Serving " + fileName + " from cache");
                byte[] response = readFile(cachedFile);
                exchange.sendResponseHeaders(200, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            } else {
                // Fetch the file from the main server
                System.out.println("Fetching " + fileName + " from main server");
                try {
                    URI uri = new URI(MAIN_SERVER_URL + fileName);
                    URL url = uri.toURL();
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("X-Forwarded-For", "anonymous");

                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        InputStream inputStream = connection.getInputStream();
                        byte[] response = inputStream.readAllBytes();
                        inputStream.close();

                        // Cache the file
                        File cacheFile = new File(CACHE_DIR + fileName);
                        cacheFile.getParentFile().mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                            fos.write(response);
                        }
                        cache.put(fileName, cacheFile);

                        // Send the response to the client
                        exchange.sendResponseHeaders(200, response.length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response);
                        os.close();
                    } else {
                        String response = "404 (Not Found)\n";
                        exchange.sendResponseHeaders(404, response.length());
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    String response = "500 (Internal Server Error)\n";
                    exchange.sendResponseHeaders(500, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            }
        }
    }

    // Helper method to read the file content into a byte array
    private byte[] readFile(File file) throws IOException {
        InputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return data;
    }
}

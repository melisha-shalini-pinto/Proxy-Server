import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class MainServer {
    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Set a context for the root directory
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String clientAddress = exchange.getRemoteAddress().getAddress().getHostAddress();
                
                // Parse the request URI to get the file name
                String requestURI = exchange.getRequestURI().toString();
                String fileName = requestURI.equals("/") ? "index.html" : requestURI.substring(1);
                
                System.out.println("Client IP: " + clientAddress + " Requested Page: " + fileName);

                // Construct the file path
                File file = new File("web", fileName);

                // Check if the file exists and is a file
                if (file.exists() && file.isFile()) {
                    
                    byte[] response = readFile(file);

                    exchange.sendResponseHeaders(200, response.length);

                    // Send the response
                    OutputStream os = exchange.getResponseBody();
                    os.write(response);
                    os.close();
                } else {
                    // Send 404 Not Found response
                    String response = "404 (Not Found)\n";
                    exchange.sendResponseHeaders(404, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            }
        });

        // Start the server
        server.setExecutor(null);
        server.start();

        System.out.println("Server is listening on port 8080");
    }

    // Helper method to read the file content into a byte array
    private static byte[] readFile(File file) throws IOException {
        InputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return data;
    }
}

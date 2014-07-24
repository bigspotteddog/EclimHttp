package com.nobodyelses.httpserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Paths;
import java.nio.file.Path;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class TinyHttpServer {
    private final HttpServer server;
    private final int port;
    private final String rootPath;

    public TinyHttpServer() throws Exception {
        this(8000, ".");
    }

    public TinyHttpServer(int port) throws Exception {
        this(port, ".");
    }

    public TinyHttpServer(int port, String rootPath) throws Exception {
        this.port = port;
        this.rootPath = rootPath;

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new PathHandler());
        server.setExecutor(null);
    }

    public void start() {
        server.start();

        System.out.println(String.format("Server listening on port: %d", port));
    }

    public void route(String path, HttpHandler handler) {
        server.createContext(path, handler);
    }

    private class PathHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            URI uri = t.getRequestURI();
            String path = uri.getPath();

            int status = 200;
            String response = null;

            String relativePath = path.substring(1);
            if (relativePath.isEmpty()) {
                relativePath = rootPath;
            }

            System.out.println(relativePath);

            try {
                Path pathReq = Paths.get(relativePath);

                checkPath(pathReq);

                File file = pathReq.toFile();

                if (file.exists() && file.isDirectory()) {
                    relativePath = relativePath + "/index.html";
                }

                response = getFileAsString(relativePath);
            } catch (FileNotFoundException e) {
                status = 404;
                response = "Not found.";
            } catch (Exception e) {
                status = 500;
                response = e.getMessage();
            }

            t.sendResponseHeaders(status, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private static void checkPath(final Path pathReq) throws Exception {
        Path root = Paths.get(".");
        Path realRoot = root.toRealPath();
        Path realPath = pathReq.toRealPath();

        System.out.println(realRoot);
        System.out.println(realPath);

        if (!realPath.startsWith(realRoot)) {
            throw new FileNotFoundException("Not found.");
        }
    }

    public static String getFileAsString(final String path) throws FileNotFoundException, Exception {
        java.util.Scanner scanner = new java.util.Scanner(new File(path));
        try {
            String next = scanner.useDelimiter("\\A").next();
            return next;
        } finally {
            scanner.close();
        }
    }

    public static void redirect(HttpExchange exchange, String uri) throws Exception {
        exchange.getResponseHeaders().add("Location", uri);
        exchange.sendResponseHeaders(302, 0);
    }
}

package com.nobodyelses.httpserver;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());
    private static String eclim;

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
    private static Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();

    public static void main(String[] args) throws Exception {
        Map<String, String> map = new HashMap<String, String>();

        String name = null;
        for (int i = 0; i < args.length; i++) {
            if (i % 2 == 0) {
                name = args[i];
            } else {
                map.put(name, args[i]);
                name = null;
            }
        }

        if (name != null) {
            map.put(name, name);
        }

        eclim = map.get("-e");
        if (eclim == null) {
            eclim = System.getenv("ECLIM_COMMAND");
        }

        if (eclim == null) {
            System.out.println("ECLIM_COMMAND must be defined.");
            return;
        }

        int port = 8000;
        String portString = map.get("-p");
        if (portString != null) {
            port = Integer.parseInt(portString);
        }

        String root = map.get("-r");
        if (root == null) {
            root = ".";
        }

        new Main().start(port, root);
    }

    private WatchDir watcher;
    private String projectPath;
    private String project;

    public void start(int port, String root) throws Exception {
        TinyHttpServer tiny = new TinyHttpServer(port, root);
        tiny.route("/data", new EclimHandler());
        tiny.route("/data/problems", new ProblemsHandler());
        tiny.start();
    }

    private void watchProject(final String projectPath) {
        this.projectPath = projectPath;

        if (watcher != null) {
            try {
                watcher.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    String response = processEclimCommand(new String[] {"-command", "java_src_dirs", "-p", project});
                    response = response.replace("\"", "");
                    response = response.trim();
                    String[] split = response.split("\\\\n");
                    List<Path> dirs = new ArrayList<Path>();
                    for (String dir : split) {
                        dirs.add(Paths.get(dir));
                    }
                    log.info("Watch: " + dirs);
                    watcher = new WatchDir(dirs, true, Main.this);
                    watcher.processEvents();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    private long lastTime = -1;
    private long lastTimeHold = 0;

    private class ProblemsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            log.info("processing problems");

            URI uri = t.getRequestURI();
            String path = uri.getPath();
            String[] split = path.substring(1).split("/");
            project = split[3];

            Map<String, String> queryMap = new HashMap<String, String>();
            String queryString = uri.getQuery();
            if (queryString != null) {
                String[] parameterPairs = queryString.split("&");
                for (String p : parameterPairs) {
                    String[] s = p.split("=");
                    String name = s[0];
                    String value = null;
                    if (s.length > 1) {
                        value = s[1];
                    }
                    queryMap.put(name, value);
                }

                String lastTimeHoldString = queryMap.get("k");
                if (lastTimeHoldString != null) {
                    lastTimeHold = Long.parseLong(lastTimeHoldString);
                } else {
                    lastTimeHold = 0;
                }
            } else {
                lastTimeHold = 0;
            }

            for (int i = 0; i < 10000 / 200; i++) {
                if (lastTime != lastTimeHold) {
                    break;
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {}
            }

            if (lastTimeHold == 0) {
                processEclimCommand(new String[] {"-command", "project_refresh", "-p", project});
            }

            lastTimeHold = lastTime;

            String response = null;

            response = processEclimCommand(new String[] {"-command", "project_info", "-p", project});
            Map<String, Object> map = gson.fromJson(response, mapType);
            String projectPath = (String) map.get("path");

            response = processEclimCommand(new String[] {"-command", "problems", "-p", project});
            List<Map<String, Object>> problems = gson.fromJson(response, listType);
            log.info(response);

            if (problems.isEmpty()) {
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("message", "No problems");
                problems.add(item);
            } else {
                for (Map<String, Object> item : problems) {
                    String filename = (String) item.get("filename");
                    filename = filename.substring(projectPath.length() + 1);
                    item.put("filename", filename);

                    item.put("line", ((Double) item.get("line")).intValue());
                    item.put("column", ((Double) item.get("column")).intValue());
                }

                Collections.sort(problems, new Comparator<Map<String, Object>>() {
                    @Override
                    public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                        Boolean b1 = (Boolean) o1.get("warning");
                        Boolean b2 = (Boolean) o2.get("warning");

                        int compare = b1.compareTo(b2);

                        if (compare != 0) {
                            return compare;
                        }

                        String p1 = (String) o1.get("filename");
                        String p2 = (String) o2.get("filename");

                        compare = p1.compareTo(p2);
                        if (compare != 0) {
                            return compare;
                        }

                        int l1 = (int) o1.get("line");
                        int l2 = (int) o2.get("line");

                        if (l1 != l2) {
                            return l1 - l2;
                        }

                        int c1 = (int) o1.get("column");
                        int c2 = (int) o2.get("column");

                        return c1 - c2;
                    }
                });
            }

            FileReader reader = new FileReader("templates/problems.html");

            Map<String, Object> data = new HashMap<String, Object>();
            data.put("project", project);
            data.put("problems", problems);
            data.put("key", lastTime);
            response = template(reader, data);

            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();

            if (!projectPath.equals(Main.this.projectPath)) {
                watchProject(projectPath);
            }
        }
    }

    private String template(Reader reader, Object obj) {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache compile = mf.compile(reader, null);
        StringWriter writer = new StringWriter();
        compile.execute(writer, obj);
        String result = writer.toString();
        return result;
    }

    private class EclimHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            URI uri = t.getRequestURI();
            String path = uri.getPath();
            String[] split = path.substring(1).split("/");

            String response = processEclimCommand(split);

            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private String processEclimCommand(String[] split) throws IOException {
        List<String> list = new ArrayList<String>();
        list.add(eclim);
        list.add("-command");

        for (int i = 1; i < split.length; i++) {
            String p = split[i];
            list.add(p);
        }

        Process process = new ProcessBuilder(list).start();
        InputStream is = process.getInputStream();
        java.util.Scanner scanner = new java.util.Scanner(is);
        try {
            if (scanner.hasNext()) {
                String response = scanner.useDelimiter("\\A").next();
                return response;
            }
        } finally {
            scanner.close();
        }
        return "";
    }

    public void dirChanged(WatchEvent<?> event, Path child) throws Exception {
        String filename = child.toString();
        filename = filename.substring(projectPath.length() + 1);
        log.info("Refresh " + filename);
        processEclimCommand(new String[] {"-command", "project_refresh_file", "-p", project, "-f", filename});
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {}
        lastTime = System.currentTimeMillis();
    }
}

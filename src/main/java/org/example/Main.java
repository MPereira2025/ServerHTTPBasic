package org.example;
import org.w3c.dom.ls.LSOutput;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.util.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        int port = 8080;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor escuchando en puerto " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                      new Thread(() -> {
                          System.out.println("Nueva conexion desde " + clientSocket.getInetAddress());

                          try {
                              handleRequest(clientSocket);
                          } catch (IOException e) {
                              System.err.println("Error al manejar cliente" + e.getMessage());
                              e.printStackTrace();
                          }finally {
                              try {
                                  clientSocket.close();
                              } catch (IOException e) {
                                  System.err.println("Error al cerrar socket: " + e.getMessage());
                              }
                          }

                      }).start();
                }
            }catch (IOException e) {
            System.err.println("No se pudo iniciar el servidor: " + e.getMessage());
        }
    }

    private static void handleRequest(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
        BufferedOutputStream dataOut = new BufferedOutputStream(clientSocket.getOutputStream());

        // Lee la primera línea de la request (ej. "GET / HTTP/1.1")
        String requestLine = in.readLine();
        if (requestLine == null) return;

        // Lee headers hasta línea vacía
        String headerLine;
        int contentLength = 0;
        String contentType = null;

        String[] parts = requestLine.split(" ");
        if(parts.length != 3) {
            System.err.println( "400 Bad Request" + "Peticion mal formulada");
            return;
        }

        String method = parts[0];
        String path = parts[1];
        System.out.println("EL PATH es: " + path);
        String version = parts[2];

        //LEYENDO EL HEADER
        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
            // Puedes parsear headers aquí si necesitas
            System.out.println("HeaderLiner: " + headerLine);
            if (headerLine.startsWith("Content-Length")){
                String cacheLength = headerLine.substring("Content-Length:".length()).trim();
                contentLength = Integer.parseInt(cacheLength);
                System.out.println("Content-Length encontrado: " + contentLength);
            }else if (headerLine.startsWith("Content-Type:")){
                String cacheType = headerLine.substring("Content-Type:".length()).trim();
                contentType = cacheType;
                System.out.println("Content-Type encontrado: " + contentType);
            }
        }

        //LEYENDO EL BODY
        String body = "";
        if ("POST".equals(method) && contentLength > 0){
            char[] bodyChars = new char[contentLength];
            in.read(bodyChars, 0, contentLength);
            body = new String(bodyChars);
            System.out.println("Body leido: " + body);
        }




        //DESGLOSANDO EL PATH
        //Despues de obtener el path = parts[1]
        String purePath = path;
        String query = null;
        HashMap<String, String>queryValores = new HashMap<>();
        if(path.contains("?")){
            //divide en dos partes
            String[] pathAndQuery = path.split("\\?", 2);
            purePath = pathAndQuery[0];
            if(pathAndQuery.length > 1 && !pathAndQuery[1].isEmpty()){
                query = pathAndQuery[1];
               // System.out.println("Query String completa : " + query);
                //para soportar varios parametros
                String[] pairs = query.split("&");
                for (String p : pairs){
                    String[] claveValor = p.split("=", 2);
                    String clave = URLDecoder.decode(claveValor[0], StandardCharsets.UTF_8);
                    String valor = claveValor.length > 1 ? URLDecoder.decode(claveValor[1], StandardCharsets.UTF_8) : "";
                    queryValores.put(clave, valor);
                   // System.out.println("parametro: " + clave + " = " + valor);
                }

            }
            path = pathAndQuery[0];
            query = pathAndQuery[1];
            System.out.println(query);
        }



        switch(purePath){
            case"/api/post"-> {
                try {
                    if (!"POST".equals(method)) {
                        sendResponse(out, dataOut, "405 Method Not Allowed", "<h1>405 Method Not Allowed</h1><p>Solo POST aqui</p>", "text/html");
                        return;
                    }
                    //Se verifica posibles exceptiones por parte del cliente
                    if (contentType == null || !contentType.contains("application/json")) {
                        sendResponse(out, dataOut, "415 Unsupported Media Type", "{\"error\": \"Solo se acepta application/json. Recibido: "
                                + (contentType != null ? contentType : "ninguno") + "\"}", "application/json");
                        return;
                    }
                    if (contentLength <= 0) {
                        sendResponse(out, dataOut, "411 Length Required", "{\"error\": \"Se requiere Content-Length para POST con body\"}",
                                "application/json");
                        return;
                    }

                    //Arriba por fuera del switch ya se leyo el body
                    System.out.println("JSON recibido: " + body);

                    //Parsear JSON manualmente
                    Map<String, String> jsonParams = parseSimpleJson(body);

                    if (jsonParams.isEmpty()) {
                        sendResponse(out, dataOut, "400 Bad Request", "{\"error\": \"JSON mal formulado o vacio\"}",
                                "application/json");
                        return;
                    }


                    if (!jsonParams.containsKey("nombre")) {
                        sendResponse(out, dataOut, "400 Bad Request",
                                "{\"error\": \"Falta el campo requerido 'nombre'\"}",
                                "application/json");
                        return;
                    }
                    if (!jsonParams.containsKey("edad")) {
                        sendResponse(out, dataOut, "400 Bad Request",
                                "{\"error\": \"Falta el campo requerido 'edad'\"}",
                                "application/json");
                        return;
                    }
                    if (!jsonParams.containsKey("email")) {
                        sendResponse(out, dataOut, "400 Bad Request",
                                "{\"error\": \"Falta el campo requerido 'email'\"}",
                                "application/json");
                        return;
                    }

                    //Construir respuesta JSON basada en los datos recibidos
                    String nombre = jsonParams.getOrDefault("nombre", "Visitante");
                    String edad = jsonParams.getOrDefault("edad", "desconocida");

                    String edadStr = jsonParams.get("edad");
                    int edadNum;
                    try {
                        edadNum = Integer.parseInt(edadStr);
                    } catch (NumberFormatException e) {
                        sendResponse(out, dataOut, "400 Bad Request",
                                "{\"error\": \"El campo 'edad' debe ser un número entero\"}",
                                "application/json");
                        return;
                    }
                    String respuestaJson = String.format("{\"saludo\": \"Hola %s!\", \"mensaje\": \"Tienes %s years\", \"recibido\": %s}",
                            nombre, edad, jsonParams.toString().replace("=", "\": \"").replace(", ", "\", \""));

                    sendResponse(out, dataOut, "200 OK", respuestaJson, "application/json");
                }catch(Exception e){
                    System.err.println("Error inesperado en /api/post: " + e.getMessage());
                    e.printStackTrace();

                    String errorJson = "{\"error\": \"Error interno del servidor\", \"detalle\": \"" + e.getMessage() + "\"}";
                    sendResponse(out, dataOut, "500 Internal Server Error", errorJson, "application/json");
                }
                }

            case"/api/hello"-> {
                if (!"GET".equals(method)) {
                    sendResponse(out, dataOut, "405 Method Not Allowed",
                            "<h1>405 Method Not Allowed</h1><p>Solo GET aquí</p>", "text/html");
                    return;
                }
                String mensaje = "Hola desde JSON!";
                String fecha = new java.util.Date().toString();
                String json = String.format("{\"mensaje\": \"%s\", \"fecha\": \"%s\", \"servidor\": \"Hecho en Java puro\"}", mensaje, fecha);

                System.out.println("Enviando JSON: " + json);  // Para que veas en consola

                sendResponse(out, dataOut, "200 OK", json, "application/json");

            }
            case"/form"->{
                if("POST".equals(method)){
                    //Parsea body como form data (igual que query)
                    Map<String, String> postParams = new HashMap<>();

                    if (contentLength <= 0) {
                        sendResponse(out, dataOut, "411 Length Required",
                                "<html><body><h1>411 Length Required</h1>" +
                                        "<p>Se requiere Content-Length para peticiones POST con body</p></body></html>", "text/html");
                        return;
                    }
                    if (contentType == null || !contentType.contains("application/x-www-form-urlencoded")) {
                        sendResponse(out, dataOut, "415 Unsupported Media Type",
                                "<html><body><h1>415 Unsupported Media Type</h1>" +
                                        "<p>Solo se acepta application/x-www-form-urlencoded. " +
                                        "Content-Type recibido: " + (contentType != null ? contentType : "ninguno") + "</p></body></html>", "text/html");
                        return;  // ¡Importante! Salir para no procesar más
                    }

                    if(contentType != null && contentType.contains("application/x-www-form-urlencoded")){
                        String[] pairs = body.split("&");
                        for(String p: pairs){
                            String[] claveValor = p.split("=", 2);
                            String clave = URLDecoder.decode(claveValor[0], StandardCharsets.UTF_8);
                            String valor = claveValor.length > 1 ? URLDecoder.decode(claveValor[1], StandardCharsets.UTF_8) : "";
                            postParams.put(clave, valor);
                        }
                    }


                    //Se construye la respuesta con los param del POST
                    StringBuilder html = new StringBuilder();
                    html.append("<html><body><h1>Datos recibidos via POST</h1>");
                    if (postParams.isEmpty()){
                        html.append("<p>No se recibieron datos</p>");
                    }else{
                        html.append("<ul>");
                        for (Map.Entry<String, String> pP : postParams.entrySet()){
                            html.append("<li>").append(pP.getKey()).append(" = ").append(pP.getValue()).append("</li>");
                        }
                        html.append("</ul>");
                    }
                    html.append("</body></html>");

                    sendResponse(out, dataOut, "200 OK", html.toString(), "text/html");
                    System.out.println("POST recibido en /form");
                    System.out.println("  - Content-Type: " + contentType);
                    System.out.println("  - Content-Length: " + contentLength);
                    System.out.println("  - Body crudo: " + body);
                    System.out.println("  - Parámetros parseados: " + postParams);

                }else {
                    sendResponse(out, dataOut, "405 Method Not Allowed", "<h1>405 Method Not Allowed</h1><p>Solo POST aqui</p>", "text/html");
                }


            }
            case"/"->{
            String nombre = queryValores.getOrDefault("nombre", "Visitante");
            String responseBody = "<html><body><h1>Hello, your name is " + nombre + "</h1></body></html>";
            sendResponse(out, dataOut, "200 OK", responseBody, "text/html");
            }
            case"/hello"->{
                String nombre = queryValores.getOrDefault("nombre", "Visitante");
                String responseBody = "<html><body><h1>¡Hola mundo desde /hello!</h1></body></html>";
                sendResponse(out, dataOut, "200 OK", responseBody, "text/html");
            }
            case"/echo"->{
                StringBuilder html = new StringBuilder();
                html.append("<html><body><h1>Echo de parámetros</h1>");
                if (queryValores.isEmpty()){
                    html.append("<p>No se recibieron parámetros en la URL</p>");
                }else{
                    html.append("<ul>");
                    for (Map.Entry<String, String> m : queryValores.entrySet()){
                        String claveM = m.getKey();
                        String valorM = m.getValue();
                        html.append("<li>").append(claveM).append(" = ").append(valorM).append("</li>");
                    }
                    html.append("</ul>");
                }
                html.append("<a href=\"http://localhost:8080/\">Volver al home</a>");
                html.append("</body></html>");

                String responseBody = html.toString();
                sendResponse(out, dataOut, "200 OK", responseBody, "text/html");
            }
            case"/about"->{
                String nombre = queryValores.getOrDefault("nombre", "Visitante");
                String responseBody = "<html><body><h1>Sobre este servidor</h1><br><br><p>Hecho desde cero con java.net" +
                        "</p><br><br><p>Aprendiendo backend desde cero</p></body></html>";
                sendResponse(out, dataOut, "200 OK", responseBody, "text/html");
            }
            default->{
                String responseBody = "<html><body><h1>404 Not Found</h1></body></html>";
                sendResponse(out, dataOut, "404 Not Found", responseBody, "text/html");
            }
        }


        // Prepara respuesta simple
       /* String responseBody = "<html><body><h1>Hello from Java HTTP Server!</h1><p>La pagina " + path + " no existe</p></body></html>";
        out.println("HTTP/1.1 404 Not Found");
        out.println("Content-Type: text/html;charset=utf-8");
        out.println("Content-Length: " + responseBody.length());
        out.println();
        out.flush();

        dataOut.write(responseBody.getBytes("UTF-8"));
        dataOut.flush();
*/
        in.close();
        out.close();
        dataOut.close();
    }

    private static void sendResponse(PrintWriter out, BufferedOutputStream dataOut, String status, String body, String contentType) throws IOException {
        byte[] bodyBytes = body.getBytes("UTF-8");
        out.println("HTTP/1.1 " + status);
        out.println("Content-Type: " + contentType + "; charset=utf-8");
        out.println("Content-Length: " + bodyBytes.length);  // Siempre correcto
        out.println();
        out.flush();
        dataOut.write(bodyBytes);
        dataOut.flush();
    }

    private static Map<String, String> parseSimpleJson(String jsonBody) {
        Map<String, String> map = new HashMap<>();

        if (jsonBody == null || jsonBody.trim().isEmpty()) {
            return map;
        }

        String trimmed = jsonBody.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            System.out.println("No es un objeto JSON válido: " + trimmed);
            return map;
        }

        // Quitamos { y } y espacios extras
        String content = trimmed.substring(1, trimmed.length() - 1).trim();

        // Dividimos por comas, pero solo las que están fuera de comillas
        String[] pairs = content.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            pair = pair.trim();
            if (pair.isEmpty()) continue;

            // Split por : (solo el primero)
            int colonIndex = pair.indexOf(':');
            if (colonIndex == -1) continue;

            String keyPart = pair.substring(0, colonIndex).trim();
            String valuePart = pair.substring(colonIndex + 1).trim();

            // Quitamos comillas si existen
            if (keyPart.startsWith("\"") && keyPart.endsWith("\"")) {
                keyPart = keyPart.substring(1, keyPart.length() - 1);
            }
            if (valuePart.startsWith("\"") && valuePart.endsWith("\"")) {
                valuePart = valuePart.substring(1, valuePart.length() - 1);
            }

            map.put(keyPart, valuePart);
        }

        System.out.println("JSON parseado: " + map);
        return map;
    }

    }

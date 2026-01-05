package org.example;
import java.io.*;
import java.net.*;
import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.Map;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        int port = 8080;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor escuchando en puerto " + port);
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleRequest(clientSocket);
                } catch (IOException e) {
                    System.err.println("Error al manejar la conexión: " + e.getMessage());
                }
            }
        } catch (IOException e) {
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
        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
            // Puedes parsear headers aquí si necesitas
            System.out.println("HeaderLiner: " + headerLine);
        }

        String[] parts = requestLine.split(" ");
        if(parts.length != 3) {
            System.err.println( "400 Bad Request" + "Peticion mal formulada");
            return;
        }

        String method = parts[0];
        String path = parts[1];
        //System.out.println(path);
        String version = parts[2];


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
                System.out.println("Query String completa : " + query);
                //para soportar varios parametros
                String[] pairs = query.split("&");
                for (String p : pairs){
                    String[] claveValor = p.split("=", 2);
                    String clave = claveValor[0];
                    String valor = claveValor.length > 1 ? claveValor[1] : "";
                    queryValores.put(clave, valor);
                    System.out.println("parametro: " + clave + " = " + valor);
                }

            }
            path = pathAndQuery[0];
            query = pathAndQuery[1];
            System.out.println(query);
        }



        if("/".equals(purePath)){
            String nombre = queryValores.getOrDefault("nombre", "Visitante");
            String responseBody = "<html><body><h1>Hello, your name is " + nombre + "</h1></body></html>";
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html;charset=utf-8");
            out.println("Content-Length: " + responseBody.length());
            out.println();
            out.flush();
            dataOut.write(responseBody.getBytes("UTF-8"));
            dataOut.flush();
        }else {
            String responseBody = "<html><body><h1>404 Not Found</h1></body></html>";
            out.println("HTTP/1.1 404 Not Found");
            out.println("Content-Type: text/html;charset=utf-8");
            out.println("Content-Length: " + responseBody.length());
            out.println();
            out.flush();
            dataOut.write(responseBody.getBytes("UTF-8"));
            dataOut.flush();
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
    }

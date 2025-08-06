import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 2222;

        try (
            Socket socket = new Socket(serverAddress, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("Conectado ao servidor.");
            System.out.println("Bem-vindo ao shell remoto. Digite um comando ('exit' para sair):");

            // Thread para ler e exibir a saída do servidor
            new Thread(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                        System.out.print("> "); // Coloca o prompt de volta após a saída
                    }
                } catch (IOException e) {
                    System.err.println("Conexão com o servidor encerrada.");
                }
            }).start();
            
            // Loop principal para ler a entrada do usuário e enviar ao servidor
            String userInput;
            while (true) {
                System.out.print("> ");
                userInput = scanner.nextLine();
                out.println(userInput);
                
                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Host desconhecido: " + serverAddress);
        } catch (IOException e) {
            System.err.println("Não foi possível conectar ao servidor: " + e.getMessage());
        }
    }
}
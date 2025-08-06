import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static void main(String[] args) {
        int port = 2222;
        ExecutorService pool = Executors.newFixedThreadPool(10); // Pool de threads para lidar com múltiplos clientes

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor iniciado. Aguardando conexões na porta " + port + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Process shellProcess;
    private final BufferedReader shellInput;
    private final BufferedWriter shellOutput;

    public ClientHandler(Socket socket) throws IOException {
        this.clientSocket = socket;
        
        // Inicia um shell (bash no Linux/macOS, cmd.exe no Windows) e o mantém aberto
        ProcessBuilder builder;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            builder = new ProcessBuilder("cmd.exe");
        } else {
            builder = new ProcessBuilder("bash");
        }
        
        // Redireciona a saída de erro para a saída padrão para facilitar a leitura
        builder.redirectErrorStream(true);
        
        this.shellProcess = builder.start();
        
        // Streams de comunicação do processo do shell
        this.shellInput = new BufferedReader(new InputStreamReader(shellProcess.getInputStream()));
        this.shellOutput = new BufferedWriter(new OutputStreamWriter(shellProcess.getOutputStream()));
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            out.println("Bem-vindo ao shell remoto. Digite um comando ('exit' para sair):");

            // Inicia um thread para enviar a saída do shell para o cliente
            new Thread(this::readShellOutput).start();

            String commandLine;
            while ((commandLine = in.readLine()) != null) {
                System.out.println("Comando recebido de " + clientSocket.getInetAddress() + ": " + commandLine);

                if ("exit".equalsIgnoreCase(commandLine.trim())) {
                    out.println("Saindo...");
                    break;
                }
                
                try {
                    // Envia o comando para o shell em execução
                    shellOutput.write(commandLine);
                    shellOutput.newLine();
                    shellOutput.flush();
                } catch (IOException e) {
                    out.println("ERRO ao enviar comando para o shell: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Erro na comunicação com o cliente: " + e.getMessage());
        } finally {
            try {
                // Encerra o processo do shell quando a conexão com o cliente termina
                shellProcess.destroyForcibly(); 
                clientSocket.close();
                System.out.println("Cliente desconectado: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Método para ler a saída do shell em tempo real e enviar para o cliente
    private void readShellOutput() {
        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            char[] buffer = new char[1024];
            int bytesRead;
            while ((bytesRead = shellInput.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler a saída do shell: " + e.getMessage());
        }
    }
}
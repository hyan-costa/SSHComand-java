import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClienteSSH{
    public static void main(String[] args) {
        String servidor = "localhost"; // IP ou host do servidor
        int porta = 5000;

        try (Socket socket = new Socket(servidor, porta);
             DataInputStream entrada = new DataInputStream(socket.getInputStream());
             DataOutputStream saida = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Conectado ao servidor SSH.");
            System.out.println("Comando sem '*' no início → executa localmente.");
            System.out.println("Comando com '*' no início → executa remotamente.");
            System.out.println("Comandos especiais: get <arquivo>, put <arquivo>");

            while (true) {
                System.out.print("> ");
                String comando = scanner.nextLine().trim();

                if (comando.equalsIgnoreCase("exit")) {
                    saida.writeUTF("exit");
                    break;
                }

                if (comando.startsWith("get ")) {
                    saida.writeUTF(comando);
                    receberArquivo(entrada, comando.split(" ", 2)[1]);
                } 
                else if (comando.startsWith("put ")) {
                    String nomeArquivo = comando.split(" ", 2)[1];
                    saida.writeUTF(comando);
                    enviarArquivo(saida, nomeArquivo);
                }
                else if (comando.startsWith("*") && comando.length() > 1) {
                    // Execução remota (removendo o "l" inicial)
                    String comandoRemoto = comando.substring(1); 
                    saida.writeUTF(comandoRemoto.trim());
                    String resposta = entrada.readUTF();
                    System.out.print(resposta);
                }
                else {
                    // Execução local (padrão)
                    executarLocal(comando);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void executarLocal(String comando) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", comando);
            } else {
                pb = new ProcessBuilder("bash", "-c", comando);
            }
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String linha;
                while ((linha = reader.readLine()) != null) {
                    System.out.println(linha);
                }
            }
            proc.waitFor();
        } catch (Exception e) {
            System.out.println("Erro ao executar comando local: " + e.getMessage());
        }
    }

    private static void receberArquivo(DataInputStream entrada, String nomeArquivo) throws IOException {
        long tamanho = entrada.readLong();
        if (tamanho < 0) {
            System.out.println("Arquivo não encontrado no servidor.");
            return;
        }
        try (FileOutputStream fos = new FileOutputStream(nomeArquivo)) {
            byte[] buffer = new byte[4096];
            long totalLido = 0;
            while (totalLido < tamanho) {
                int bytesLidos = entrada.read(buffer);
                fos.write(buffer, 0, bytesLidos);
                totalLido += bytesLidos;
            }
            System.out.println("Arquivo '" + nomeArquivo + "' recebido com sucesso.");
        }
    }

    private static void enviarArquivo(DataOutputStream saida, String nomeArquivo) throws IOException {
        File arquivo = new File(nomeArquivo);
        if (!arquivo.exists()) {
            saida.writeLong(-1);
            System.out.println("Arquivo não encontrado.");
            return;
        }
        saida.writeLong(arquivo.length());
        try (FileInputStream fis = new FileInputStream(arquivo)) {
            byte[] buffer = new byte[4096];
            int bytesLidos;
            while ((bytesLidos = fis.read(buffer)) != -1) {
                saida.write(buffer, 0, bytesLidos);
            }
            System.out.println("Arquivo '" + nomeArquivo + "' enviado com sucesso.");
        }
    }
}

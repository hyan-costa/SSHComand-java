import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorSSH {
    public static void main(String[] args) {
        int porta = 5000;
        File diretorioAtual = new File(System.getProperty("user.dir")); // diretório inicial do servidor

        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("Servidor SSH aguardando conexões na porta " + porta);

            while (true) {
                try (Socket socket = serverSocket.accept();
                     DataInputStream entrada = new DataInputStream(socket.getInputStream());
                     DataOutputStream saida = new DataOutputStream(socket.getOutputStream())) {

                    System.out.println("Cliente conectado: " + socket.getInetAddress());

                    while (true) {
                        String comando = entrada.readUTF();
                        if (comando.equalsIgnoreCase("exit")) {
                            break;
                        }

                        if (comando.startsWith("get ")) {
                            enviarArquivo(saida, comando.split(" ", 2)[1], diretorioAtual);
                        } 
                        else if (comando.startsWith("put ")) {
                            receberArquivo(entrada, comando.split(" ", 2)[1], diretorioAtual);
                        }
                        else if (comando.startsWith("cd ")) {
                            String novoDir = comando.substring(3).trim();
                            File novoDiretorio = new File(diretorioAtual, novoDir);
                            if (novoDiretorio.exists() && novoDiretorio.isDirectory()) {
                                diretorioAtual = novoDiretorio;
                                saida.writeUTF("Diretório alterado para: " + diretorioAtual.getAbsolutePath() + "\n");
                            } else {
                                saida.writeUTF("Diretório não encontrado.\n");
                            }
                        }
                        else {
                            saida.writeUTF(executarComando(diretorioAtual, comando));
                        }
                    }

                } catch (IOException e) {
                    System.out.println("Erro na conexão com cliente: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String executarComando(File diretorio, String comando) {
        StringBuilder saidaComando = new StringBuilder();
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", comando);
            } else {
                pb = new ProcessBuilder("bash", "-c", comando);
            }
            pb.directory(diretorio);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String linha;
                while ((linha = reader.readLine()) != null) {
                    saidaComando.append(linha).append("\n");
                }
            }
            proc.waitFor();
        } catch (Exception e) {
            saidaComando.append("Erro ao executar comando: ").append(e.getMessage()).append("\n");
        }
        return saidaComando.toString();
    }

    private static void enviarArquivo(DataOutputStream saida, String nomeArquivo, File dir) throws IOException {
        File arquivo = new File(dir, nomeArquivo);
        if (!arquivo.exists()) {
            saida.writeLong(-1);
            return;
        }
        saida.writeLong(arquivo.length());
        try (FileInputStream fis = new FileInputStream(arquivo)) {
            byte[] buffer = new byte[4096];
            int bytesLidos;
            while ((bytesLidos = fis.read(buffer)) != -1) {
                saida.write(buffer, 0, bytesLidos);
            }
        }
    }

    private static void receberArquivo(DataInputStream entrada, String nomeArquivo, File dir) throws IOException {
        long tamanho = entrada.readLong();
        if (tamanho < 0) {
            return;
        }
        try (FileOutputStream fos = new FileOutputStream(new File(dir, nomeArquivo))) {
            byte[] buffer = new byte[4096];
            long totalLido = 0;
            while (totalLido < tamanho) {
                int bytesLidos = entrada.read(buffer);
                fos.write(buffer, 0, bytesLidos);
                totalLido += bytesLidos;
            }
        }
    }
}

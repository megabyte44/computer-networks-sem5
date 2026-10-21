import java.io.*;
import java.net.*;

public class ServerTCP {

    private static final int PORT = 5001;
    private static final int SHIFT = 3;   // Caesar key

    // Caesar Decryption
    public static String caesarDecrypt(String text, int shift) {
        StringBuilder decrypted = new StringBuilder();

        for (char ch : text.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                decrypted.append((char) ((ch - 'A' - shift + 26) % 26 + 'A'));
            } else if (Character.isLowerCase(ch)) {
                decrypted.append((char) ((ch - 'a' - shift + 26) % 26 + 'a'));
            } else {
                decrypted.append(ch);
            }
        }
        return decrypted.toString();
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("Server waiting on port " + PORT + "...");
            Socket socket = serverSocket.accept();
            System.out.println("Client connected");

            DataInputStream dis = new DataInputStream(socket.getInputStream());

            String encryptedData = dis.readUTF();  // readUTF matches client's writeUTF

            System.out.println("\n Encrypted Data Received:");
            System.out.println("--------------------------------");
            System.out.println(encryptedData);

            // Decrypt
            String decryptedData = caesarDecrypt(encryptedData, SHIFT);

            System.out.println("\n Decrypted (Original) Content:");
            System.out.println("--------------------------------");
            System.out.println(decryptedData);

            // Save file
            FileWriter writer = new FileWriter("received_file.txt");
            writer.write(decryptedData);
            writer.close();

            System.out.println("\n File saved as received_file.txt");

            dis.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

import java.io.*;
import java.net.*;

public class ClientTCP {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5001;
    private static final int SHIFT = 3;

    // Caesar Encryption
    public static String caesarEncrypt(String text, int shift) {
        StringBuilder encrypted = new StringBuilder();

        for (char ch : text.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                encrypted.append((char) ((ch - 'A' + shift) % 26 + 'A'));
            } else if (Character.isLowerCase(ch)) {
                encrypted.append((char) ((ch - 'a' + shift) % 26 + 'a'));
            } else {
                encrypted.append(ch);
            }
        }
        return encrypted.toString();
    }

    public static void main(String[] args) {
        try {
            // Read file
            FileReader fr = new FileReader("hello.txt");
            StringBuilder fileData = new StringBuilder();
            int ch;
            while ((ch = fr.read()) != -1) {
                fileData.append((char) ch);
            }
            fr.close();

            // Encrypt
            String encryptedData = caesarEncrypt(fileData.toString(), SHIFT);

            System.out.println("\n Encrypted Data:");
            System.out.println("--------------------------------");
            System.out.println(encryptedData);

            // Send to server
            Socket socket = new Socket(HOST, PORT);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            dos.writeUTF(encryptedData);  // writeUTF sends length + data
            dos.flush();

            System.out.println("\n File sent successfully!");

            dos.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

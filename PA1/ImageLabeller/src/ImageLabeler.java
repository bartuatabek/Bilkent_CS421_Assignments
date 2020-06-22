import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

/*
 * Image labeller class connects to server after successful authentication
 * requests sets of image after saving the image and requests manual input
 * for labelling and sends the responses back to the server.
 * Utku Görkem Ertürk - 21502497
 * Bartu Atabek - 21602229
*/
public class ImageLabeler {

	public static void main(String[] args) {
		if (args.length != 2) {
			return;
		}

		// Scanner
		Scanner scanner = new Scanner(System.in);

		String addr = args[0];
		int port = Integer.parseInt(args[1]);

		// Variables
		Socket socket = null;
		DataInputStream dataInputStream = null;
		DataOutputStream outputStream = null;

		// Open socket

		try {
			String currentRequest;
			socket = new Socket(addr,port);
			System.out.println("Connected to the server.");

			// Input and Output Streams
			dataInputStream = new DataInputStream(socket.getInputStream());
			outputStream = new DataOutputStream(socket.getOutputStream());

			// Authentication
			currentRequest = "USER bilkentstu\r\n";
			outputStream.write(currentRequest.getBytes(StandardCharsets.US_ASCII));

			checkResponseIsOk(readResponse(dataInputStream), true);

			currentRequest = "PASS cs421f2019\r\n";
			outputStream.write(currentRequest.getBytes(StandardCharsets.US_ASCII));

			checkResponseIsOk(readResponse(dataInputStream), true);

			// Get image

			currentRequest = "IGET\r\n";

			for (int i = 0; i < 4; i++) {
				outputStream.write(currentRequest.getBytes(StandardCharsets.US_ASCII));

				for (int k = 0; k < 3; k++) {
					readImageResponse(dataInputStream,i*3+k);
				}

				String[] labels = new String[3];
				String currentLabelRequest;

				do {
					for (int k = 0; k < 3; k++) {
						System.out.print("Enter the label for "+ (i*3+k) + ".jpg: ");
						labels[k] = scanner.next();
					}

					currentLabelRequest = "ILBL "+labels[0]+","+labels[1]+","+labels[2]+"\r\n";
					outputStream.write(currentLabelRequest.getBytes(StandardCharsets.US_ASCII));
				} while (!checkResponseIsOk(readResponse(dataInputStream), false));
			}

			currentRequest = "EXIT\r\n";
			outputStream.write(currentRequest.getBytes(StandardCharsets.US_ASCII));
			System.out.println(readResponse(dataInputStream));

			// Close the socket and streams
			try {
				dataInputStream.close();
				outputStream.close();
				scanner.close();
				socket.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean checkResponseIsOk(String response, boolean terminate) {
		System.out.println("----------------------------");
		System.out.println(response);
		System.out.println("----------------------------");

		if (response.contains("INVALID")) {
			if (terminate) {
				System.exit(0);
			} else {
				return false;
			}
		}
		return true;
	}

	public static String readResponse(DataInputStream dataInputStream) {
		String currentResponse = "";
		boolean rIsFound = false;
		int currentReadInt;

		try {
			while (true) {
				currentReadInt = dataInputStream.readByte();
				if (currentReadInt == '\r') {
					rIsFound = true;
				}

				if ( rIsFound && currentReadInt == '\n') {
					currentResponse = currentResponse.substring(0,currentResponse.length()-1);
					return currentResponse;
				}
				currentResponse = currentResponse + (char) currentReadInt;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void readImageResponse(DataInputStream dataInputStream, int imgNo) {
		byte[] headerBytes = new byte[7];
		try {
			for (int i = 0; i< 7; i++) {
				headerBytes[i] = dataInputStream.readByte();
			}

			if (!new String(Arrays.copyOfRange(headerBytes,0,4)).equals("ISND")) {
				return;
			}

			byte[] sizeInBytes = new byte[4];
			sizeInBytes[0] = 0;
			System.arraycopy(Arrays.copyOfRange(headerBytes,4,7), 0, sizeInBytes, 1, 3);
			int size = ByteBuffer.wrap(sizeInBytes).getInt();
			byte[] imageBytes = new byte[size];

			for (int i = 0; i< size; i++) {
				imageBytes[i] = dataInputStream.readByte();
			}

			ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
			BufferedImage bImage = ImageIO.read(bis);
			ImageIO.write(bImage, "jpg", new File(imgNo + ".jpg") );
			System.out.println("image created");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

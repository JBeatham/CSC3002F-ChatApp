import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;

/**
Given a file name and ClientConnection, copies the bytes of that file into a byte array, adds header information
and calls the method in ClientConnection to send that message.
**/

public class FileSend extends Thread{
	private ClientConnection client;
	private String fileName;
   private byte[] file;
	
	public FileSend(ClientConnection client, String fileName){
		this.client = client;
		this.fileName = fileName;
	}
	
	public void run(){
		try {
			file = Files.readAllBytes(new File(fileName).toPath());
         
         byte[] fileNameBytes = fileName.getBytes();
    		int fileNameBytesLength = fileNameBytes.length;
    		
    		ByteBuffer intBuffer = ByteBuffer.allocate(4);
    		intBuffer.putInt(fileNameBytesLength);
    		byte[] fileNameBytesLengthBytes = intBuffer.array();
    		
    		ByteArrayOutputStream messageOutputStream = new ByteArrayOutputStream();
         byte messagetype = 2;
    		messageOutputStream.write(messagetype); // 2 = type of message
    		messageOutputStream.write(fileNameBytesLengthBytes); // length of the filename in bytes
    		messageOutputStream.write(fileNameBytes); // String filename in bytes
    		messageOutputStream.write(file);
    		byte[] finalMessage = messageOutputStream.toByteArray();
         messageOutputStream.close();

			client.addMessageToSend(finalMessage);
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}

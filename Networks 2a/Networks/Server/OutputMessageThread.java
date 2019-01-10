import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
   Class for handling messages to be sent to the client. See in-text comments for more detail
**/
public class OutputMessageThread extends Thread{
   private LinkedBlockingQueue<byte[]> messagesToSend; // (thread-safe) queue of messages that need to be sent to this client
   private Socket connection;
   private DataOutputStream output;
   private AtomicBoolean outputSending;
   private AtomicBoolean inputSending;

   
   public OutputMessageThread(Socket connection, LinkedBlockingQueue<byte[]> messagesToSend, AtomicBoolean outputSending, AtomicBoolean inputSending){
      this.connection = connection;
      this.messagesToSend = messagesToSend;
      this.outputSending = outputSending;
      this.inputSending = inputSending;
   }
   
   public void run(){
      try{
         output = new DataOutputStream(connection.getOutputStream());
         
         while(true){
            byte[] message = messagesToSend.take();
            byte messageType = message[0];
            
            
            
            if(messageType == 0){ // server has called for a shutdown. .
               output.write(messageType); // send to the client that the server has called for a shutdown.
               output.flush();
               synchronized(connection){
                  while(inputSending.get()){
                     connection.wait(); // need to wait until the InputMessageThread confirms that the client has acknowledged that the server wants to shutdown
                                        // and wont use the socket anymore. This is as closing the output stream will end up closing the socket,
                                        // because closing a DataOutPutStream closes all wrapped streams (i.e. the socket OutputStream) and closing the socket
                                        // OutPutStream closes the socket (javaDocs)
                  }
               }
               break;
            }
            
            
            if(messageType == 6){ // message from InputMessageThread that client has called for a shutdown
               output.write(messageType); // tell client that the Server acknowledges its call for a shutdown, and wont use the socket anymore
               output.flush();
               synchronized(connection){
                  outputSending.set(false); 
                  connection.notifyAll();  // notify the waiting InputMessageThread that the OutPutMessageThread won't use the socket anymore and that it can close it
               }
               break;
            }
   
            switch(messageType){
            case 1: // text
                  output.write(messageType);
            		String messageString = new String(Arrays.copyOfRange(message, 1, message.length));
            		output.writeUTF(messageString);
            		output.flush();
            		break;
            case 2: //file
            		ByteBuffer wrapped = ByteBuffer.wrap(Arrays.copyOfRange(message, 1, 5)); 
            		int fileNameLength = wrapped.getInt(); // length of the String of the filename in bytes, stored in bytes 1 - 4 as an integer
            		String fileName = new String(Arrays.copyOfRange(message, 5, 5 + fileNameLength));
                  output.write(messageType); // tells client that this is a file
                  output.writeInt(message.length - 5 - fileNameLength); //writes the length of the file in bytes (is simply the remaining bytes in the message)
                  output.writeUTF(fileName);  // tell recieiving client the file's name
            		output.write(Arrays.copyOfRange(message, 5 + fileNameLength, message.length)); //write rest of the message (i.e. the file)
            		output.flush();
            		break;
            case 4: //file has been sent to the server by client message
                  output.write(messageType); // tell clients that this is a file request message
            		String stringMessage = new String(Arrays.copyOfRange(message, 1, message.length)); // send a string containing the username of the person sending the file
                                                                                                     // and the name of the file he/she wants to send.
            		output.writeUTF(stringMessage);
            		output.flush(); 
                  break;             
            }

            
         }
         output.close();
      }catch(IOException e){
         System.out.println(e);
      }catch(InterruptedException e){
         System.out.println(e);
      }
   }

}

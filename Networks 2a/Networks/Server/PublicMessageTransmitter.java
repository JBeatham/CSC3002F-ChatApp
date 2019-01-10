import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;

/**
   `Takes messages from the (thread-safe) LinkedBlockingQueue of messages that need to be sent and transmits them to all clients.
    Their are two special cases. If the first byte in a message is 4 it means that a client has sent a file to the server. In that case
    transmits to all clients but the client that sent that message the username and name of that file with the first byte kept at 4.
    Transmits to the client that sent the file that the file has been recieved by the server. In the case that the first (and only) byte of a 
    message is 0 this means that the ServerHub has called for a shutdown. Delivers this message to all clients and stops transmitting.
**/
public class PublicMessageTransmitter extends Thread{

   private ConcurrentHashMap<String, ClientConnection> clients;
   private LinkedBlockingQueue<byte[]> messagePool;
   
   public PublicMessageTransmitter(ConcurrentHashMap<String, ClientConnection> clients, LinkedBlockingQueue<byte[]> messagePool){
      this.clients = clients;
      this.messagePool = messagePool;
   }
   
   public void run(){
      try{
      
         while(true){
               byte[] message = messagePool.take();
               if(message[0] == 4){ 
                  StringTokenizer tokens = new StringTokenizer(new String(Arrays.copyOfRange(message, 1, message.length)));
                  ClientConnection clientSending = clients.get(tokens.nextToken()); 
                  
                  for(ClientConnection client: clients.values()){ // is not synchronized so a new user (added after call to clients.values()) may miss a message. clients is thread-safe so its no big deal
                     if(client == clientSending){
                        byte header = 1;
                        client.addMessageToSend(getHeaderAndStringBytes("File sent to server (may not have been accepted)", header)); 
                     }else{
                        client.addMessageToSend(message);
                     }
                  }
               }
               else{
                  for(ClientConnection client: clients.values()){
                     client.addMessageToSend(message);
                  }
               }
               if(message[0] == 0){
                  break;
               }
            
         }
      }catch(InterruptedException e){
         System.out.println(e);
      }catch(IOException e){
         System.out.println(e);
      }
   }
   
   private byte[] getHeaderAndStringBytes(String s, byte header) throws IOException{
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
         outputStream.write(header);
         outputStream.write(s.getBytes());
         byte[] messageTypeAndBytes = outputStream.toByteArray();
         outputStream.close();
         return messageTypeAndBytes;
   }
}
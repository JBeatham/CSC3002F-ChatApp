
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
  Class representing a client's connection. Starts the connection by starting a thread
  that will handle input of client messages. Puts messages for the client into a LinkedBlockingQueue
  that will be taken by a thread that handles output of messages with the client. LinkedBlockingQueue
  is thread-safe (see java docs), so there are no concurrency issues with this.
**/
public class ClientConnection{
   private ConcurrentHashMap<String, ClientConnection> clients;
   private Socket connection;
   private LinkedBlockingQueue<byte[]> messagePool; // messages that must be sent to all clients
   private LinkedBlockingQueue<byte[]> messagesToSend; // messages sent to this client
   
   public ClientConnection(Socket connection, ConcurrentHashMap<String, ClientConnection> clients, LinkedBlockingQueue<byte[]> messagePool){
      this.messagePool = messagePool;
      this.connection = connection;
      this.messagesToSend = new LinkedBlockingQueue<byte[]>();
      this.clients = clients;
   }
   
   public void connect(){
      InputMessageThread input = new InputMessageThread(connection, this, clients, messagePool, messagesToSend);
      input.start();
   }
   
   public boolean addMessageToSend(byte[] message){
      try{
    	   messagesToSend.put(message);
         return true;
      }catch(InterruptedException e){
         System.out.println(e);
         return false;
      }
   }  
      
   
}
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
   Has a list of Clients stored in a (thread-safe) ConcurrentHashMap where the keys
   are the clients usernames and the values are their corresponding ClientConnections.
   On startServer() creates a ServerThread that listens for connections, and a Public
   MessageTransmitter that handles messages that need be sent to all clients. On
   closeServer() tells the ServerThread to stop listening and sends a 'poison-pill'
   message to the (thread-safe) LinkedBlockingQueue that the PublicMessageTransmitter
   takes messages from. Upon recieiving this message the PublicMessageTransmitter will
   deliver the poison pill to all listening clients and stop transmitting messages.
**/
public class ServerHub{
   public static final int DEFAULT_PORT = 5500;
   private final int serverPort;
   private AtomicBoolean listen;

   private ConcurrentHashMap<String, ClientConnection> clients; 
   private LinkedBlockingQueue<byte[]> messagePool; // messages that need to be sent to all clients
   
   public ServerHub(){
      serverPort = new Integer(DEFAULT_PORT);
      messagePool = new LinkedBlockingQueue<byte[]>();
      clients = new ConcurrentHashMap<String, ClientConnection>();
      listen = new AtomicBoolean(true);
   }
   
   public ServerHub(int port){
      serverPort = port;
      messagePool = new LinkedBlockingQueue<byte[]>();
      clients = new ConcurrentHashMap<String, ClientConnection>();
      
   }
   
   public void startServer(){
      
      PublicMessageTransmitter transmitToAll = new PublicMessageTransmitter(clients, messagePool);
      transmitToAll.start();
   
      ServerThread server = new ServerThread(serverPort, clients, messagePool, listen);
      server.start();
      
    }
    
    public void closeServer(){
      try{
         byte closeMessage = 0; // poison pill
         byte[] closeMessageBytes = {closeMessage};
         messagePool.put(closeMessageBytes);
         listen.set(false);
      }catch(Exception e){
         System.out.println(e);
      }
      
    }   
   
}

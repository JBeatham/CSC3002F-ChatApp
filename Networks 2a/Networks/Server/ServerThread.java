import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.SocketTimeoutException;

/**
 Listens for client connection on the serverSocket, until told not to. On acceptance of a client creates
 a ClientConnection for that client and calls it's connect() method.
**/
public class ServerThread extends Thread{
   private ServerSocket serverSocket;
   private int port;
   private LinkedBlockingQueue<byte[]> messagePool;
   private ConcurrentHashMap<String, ClientConnection> clients;
   private AtomicBoolean listen;
   
   public ServerThread(int port, ConcurrentHashMap<String, ClientConnection> clients, LinkedBlockingQueue<byte[]> messagePool, AtomicBoolean listen){
      this.port = port;
      this.messagePool = messagePool;
      this.clients = clients;
      this.listen = listen;
   }
   
   public void run(){
      try{
         serverSocket = new ServerSocket(port);
         serverSocket.setSoTimeout(1000); // if there is no connection within a 1000 milliseconds, 
         while(listen.get()){             // accept() throws a SocketTimeOutException.
            try{                          // This is needed as otherwise would only be able to stop listening once a new client connected.
               Socket connection = serverSocket.accept();
               ClientConnection newClient = new ClientConnection(connection, clients, messagePool);
               newClient.connect();
            }catch(SocketTimeoutException e){
               continue;
            }
         }
         serverSocket.close();
      }catch(IOException e){
         System.out.println(e);
      }
   }
}
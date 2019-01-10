import java.net.Socket;
import java.nio.file.Files;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.ByteArrayOutputStream;



/**
   Class for handling messages from client. On start() reads the username of the client, and stores the username and ClientConnection
   in the (thread-safe) ConcurrentHashTable that the PublicMessageTransmitter transmits messages to. Then creates an OutputMessageThread
   for the client that handles messages that needs to be sent to the client. Then handles messages from the client until it recieves
   a message from the client that either it has recieved a message from the server that the server has shut-down, or that the client
   wants to close its connection.
**/
public class InputMessageThread extends Thread{
   private Socket connection;
   private DataInputStream input;
   private LinkedBlockingQueue<byte[]> messagePool; // messages to be transmitted to all clients
   private LinkedBlockingQueue<byte[]> messagesToSend; // messages to be sent to this client
   private String username;
   private ConcurrentHashMap<String, ClientConnection> clients;
   private ClientConnection client; // the client this InputMessageThread corresponds to.
   private AtomicBoolean outputSending;
   private AtomicBoolean inputSending;
   
   public InputMessageThread(Socket connection, ClientConnection client, ConcurrentHashMap<String, ClientConnection> clients, LinkedBlockingQueue<byte[]> messagePool, LinkedBlockingQueue<byte[]> messagesToSend){
      this.connection = connection;
      this.messagePool = messagePool;
      this.messagesToSend = messagesToSend;
      this.client = client;
      this.clients = clients;
      outputSending = new AtomicBoolean(true);
      inputSending = new AtomicBoolean(true);
   }  
   
   public void run(){
      try{              
         input = new DataInputStream(connection.getInputStream());
         username = input.readUTF();
         boolean changedName = false;
         
         synchronized(clients){ // need to synchronize as clients.keySet() will not always reflect changes to clients. Not a big deal for
                                // PublicMessageTransmitter, but important here.
            for(String name: clients.keySet()){
               if(username.equals(name)){
                  username = username + "1";
                  changedName = true;
               }
            }
            if(changedName){
            byte headerr = 1;
            messagesToSend.put(getStringHeaderAndStringBytes("Username in use. You have been assigned " + username + " as a username.", headerr));
            }
             clients.put(username, client);
         }
         

        
         
         OutputMessageThread output = new OutputMessageThread(connection, messagesToSend, outputSending, inputSending);
         output.start();
         
         while(true){
            byte messageType = input.readByte(); //tells us what type of message is going to be sent by client next
            if(messageType == 0){ // client acknowledges that the server is shutting down and it won't try to use the socket.
               System.out.println("we got a zero");
               inputSending.set(false);
               synchronized(connection){
                  connection.notifyAll();  // this and the two lines above are to tell the OutPutMessageThread to stop waiting for
                                           // a confirmation that this thread won't try to use the socket again
               }
            	break;  // stop looping
            }
            if(messageType == 6){  // client has called for a shutdown
               byte[] message = {messageType};
               messagesToSend.put(message);  // tell the OutPutMessageThread that the client has called for a shutdown.
               synchronized(connection){
                  while(outputSending.get()){
                     connection.wait();  // wait until the OutPutMessageThread confirms that it won't try to to use the socket anymore. 
                  }

               }
               break; // stop looping
            }
            
            
            byte header;
            switch(messageType){
				   case 1:  //String 
                        String messageAndUsername = (username + ": " + input.readUTF());
                        header = 1;
                        byte[] messageAndUsernameWithHeaderBytes = getStringHeaderAndStringBytes(messageAndUsername, header);
                        messagePool.put(messageAndUsernameWithHeaderBytes);
                        break;

                case 2: //File
                        int fileLength = input.readInt();
                        String fileName = input.readUTF();
                        byte[] file = new byte[fileLength];
                        input.readFully(file);
                        FileOutputStream fos = new FileOutputStream(fileName); 
                        fos.write(file); // write the file to disk
                        fos.close();
                        header = 4; // file sent by a user header
                        byte[] fileSentMessageBytes = getStringHeaderAndStringBytes(username + " " + fileName, header);
                        messagePool.put(fileSentMessageBytes); // will tell rest of the clients that this username sent this file
                                                               // PublicMessageTransmitter won't send this message to this client
                                                               // will instead send a message confirming that the file was recieved
                                                               // by the server.
                        break;
                        
                case 3: //Request for a file stored on disk.
                		  fileName = input.readUTF();
                		  FileSend fileSender = new FileSend(client, fileName); // let the FileSend thread handle it
                		  fileSender.start();
                		  break;
            }

         }
         
         input.close();
         connection.close();
         synchronized(clients){clients.remove(username, client);} // need to synchronize so we don't remove if another InputMessageThread
                                                                  // is checking whether the username is in use. Once again, its not a big
                                                                  // deal for PublicMessageTransmitter as ConcurrentHashMap is thread-safe,
                                                                  // and its not a big deal if one more message gets added to the messagesToSend
                                                                  // of a client which has closed.
      }catch(IOException e){
         System.out.println(e);
         e.printStackTrace();
         System.out.println(this);
      }catch(InterruptedException e){
         System.out.println(e);
      }
   }
   
   /**
      Given a string and a byte, returns an array with array[0] = byte, and the rest 
      of the array is the String in byte form.
   **/
   private byte[] getStringHeaderAndStringBytes(String s, byte header) throws IOException{
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
         outputStream.write(header);
         outputStream.write(s.getBytes());
         byte[] messageTypeAndBytes = outputStream.toByteArray();
         outputStream.close();
         return messageTypeAndBytes;
   }
}

import java.io.*;
import java.net.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.LinkedBlockingQueue;

/**
   Thread for handling messages sent from the server. See in-text comments for more details.
**/
public class ClientThread extends Thread{
	Socket clientSoc;
	DataInputStream input;
	boolean isRunning;
   AtomicBoolean fileToBeSent;
   AtomicBoolean serverShutdown;
   AtomicBoolean gettingInput;
   LinkedBlockingQueue<String> fileToBeSentName; // will store name of the file that another client wants to send

	ClientThread(Socket clientSoc, AtomicBoolean fileToBeSent, LinkedBlockingQueue<String> fileName, AtomicBoolean serverShutdown, AtomicBoolean gettingInput){
		try{
			this.clientSoc = clientSoc;
			input = new DataInputStream(clientSoc.getInputStream());
			isRunning = true;
         this.fileToBeSent = fileToBeSent;
         this.fileToBeSentName = fileName;
         this.serverShutdown = serverShutdown;
         this.gettingInput = gettingInput;
		}
		catch(IOException e){
				System.out.println(e);
		}
	}

	public void run(){
      try{
		   while(isRunning){
			
            byte messageType = input.readByte(); 
				if(messageType ==1){ // string being sent
					System.out.println(input.readUTF());
				}
				else if(messageType ==2){ // file being sent
					int len = input.readInt(); // get length of the file in bytes
					String name = input.readUTF(); // get name of the file
               					
					byte[] data = new byte[len];
					input.readFully(data);

					Path path = Paths.get(name);
					Files.write(path, data); // write file to disk
               System.out.println("Succesfully recieved " + name);
				}else if(messageType == 4){ // another user has sent a file to the server
               StringTokenizer tokens = new StringTokenizer(input.readUTF());
               String userNameOfSender = tokens.nextToken();
               String fileName = tokens.nextToken();
               fileToBeSentName.put(fileName); // tell client that this is the name of the file the other user wants to send
               System.out.println(userNameOfSender + " wants to send " + fileName + ". (y/n)");
               fileToBeSent.set(true);
               synchronized(clientSoc){
                  while(fileToBeSent.get()){
                     try{
                        clientSoc.wait(); // wait until client class confirms that the user has either accepted or
                                          // declined the file
                     }catch(InterruptedException e){
                        System.out.println(e);
                     }
                  }
               }
            }else if(messageType == 6){ // server has acknowledged that client wants to shutdown
               synchronized(clientSoc){
                  gettingInput.set(false);
                  clientSoc.notifyAll(); // this and the above are to tell the client thread to stop waiting 
                                         // for confirmation that the server won't use the socket anymore.

               }
               isRunning = false;
            }else if(messageType == 0){ // server wants to shutdown
               System.out.println(clientSoc.isConnected());
               System.out.println("Server shutdown. Press ENTER to quit.");
               serverShutdown.set(true);
               synchronized(clientSoc){
                  while(serverShutdown.get()){ // wait until client class sends a message to the server
                                               // telling the server that it wont use the socket anymore
                                               // (this requires the user to press ENTER as the client thread
                                               // both handles user input and sending of user messages, hence 
                                               // hence the server will only shutdown once all clients have
                                               // pressed ENTER).
                                                
                        clientSoc.wait();
                   }

               }
               isRunning = false;
            }

         }
         input.close();

		}catch(Exception e){
				System.out.println(e);
	   }
	}
	



}

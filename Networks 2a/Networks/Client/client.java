import java.io.*;
import java.net.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.LinkedBlockingQueue;

/**
   Driver class for the client. Handles messages that the client wants to send. On calling of main() 
   asks the client if he wants to connect and if so attempts to connect. Then creates a seperate thread,
   ClientThread, that handles incoming messages from the server. See in-text comments for more detail.
**/
public class client {
	public static void main(String[] argv){
	Socket clientSoc;
	DataOutputStream output;
	BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
	String inputString;
	boolean isRunning = true;
	String uName;
	Thread inThread;
   AtomicBoolean fileToBeSent = new AtomicBoolean(false); // nobody is wanting to send a file yet
   AtomicBoolean serverShutdown = new AtomicBoolean(false); // server hasn't called for a shutdown yet
   AtomicBoolean gettingInput = new AtomicBoolean(true); // we still want to recieve messages from the server
   
   LinkedBlockingQueue<String> fileToBeSentName = new LinkedBlockingQueue<String>(); // when another client has
                                                                                     // sent a file to the server
                                                                                     // the name of this file will
                                                                                     // be (temporarily) stored in
                                                                                     // this (threa-safe) queue, until
                                                                                     // the user either accepts or declines
                                                                                     // the file.
                                                                                    

	System.out.println("Connect to server?(y/n)");
   try{
		inputString = userInput.readLine();
		if("y".equals(inputString)){
		 	try{
				clientSoc = new Socket("localhost",5500);
				System.out.println("Enter name. /end to disconnect");
				
				uName = userInput.readLine();
				
				inThread = new ClientThread(clientSoc, fileToBeSent, fileToBeSentName, serverShutdown, gettingInput);
				output = new DataOutputStream(clientSoc.getOutputStream());
				inThread.start();
				output.writeUTF(uName); // tell server the client's username

				while(isRunning){
					inputString =  userInput.readLine();
               
               if(serverShutdown.get()){ // server has called for a shutdown
                  System.out.println("Tried to send a zero");
                  output.writeByte(0); // tell server that this call for a shutdown has been acknowledged, and that
                                       // the client won't attempt to use the socket anymore.
                  serverShutdown.set(false); 
                  synchronized(clientSoc){
                     clientSoc.notifyAll(); // this and setting serverShutdown to false are to notify the clientThread
                                            // that it acknowledges that this thread acknowledges that there should
                                            // be no more use of the socket, and to tell it to stop waiting for this
                                            // confirmation.
                  }
                  System.out.println("Sent a zero");
                  isRunning = false;
                  continue;
               }
               
               synchronized(clientSoc){
                  if(fileToBeSent.get() && !(inputString.equals("y") || inputString.equals("n"))){
                     // fileToBeSent tells us whether another using is wanting to send a file. Upon this variable
                     // being set true the only permissible inputs are 'y' to accept the file and 'n' to decline it.
                     System.out.println("Please accept or recieve the file");
                     continue;
                  }else if(fileToBeSent.get() && (inputString.equals("y") || inputString.equals("n"))){
                     fileToBeSent.set(false);
                     if(inputString.equals("y")){ // user accepted the file
                        output.writeByte(3); // tell the server that the user accepted the file
                        output.writeUTF(fileToBeSentName.take()); // tell the server the name of the file
                     }
                        clientSoc.notifyAll(); //notify the waiting ClientThread that it can start processing
                                               // messages again
                        continue;
                     }
                  }
                  
					if(inputString.charAt(0) == '/'){
					 	if (inputString.equals("/end")){ // user wants to shutdown
							output.writeByte(6); // tell the server that user wants to shutdown
                     synchronized(clientSoc){
                        while(gettingInput.get()){ // wait until the ClientThread confirms that the server
                                                   // acknowledges the shutdown and wont use the client anymore
                           clientSoc.wait();
                        }
                     }
                     System.out.println("DONE");
							isRunning = false; // break out of loop
						}
						else if(inputString.equals("/s")){
							System.out.println("ENTER FILE PATH"); // must be stored in same folder as client.class
							inputString = userInput.readLine();

							try{
								Path path = Paths.get(inputString);
								byte[] data = Files.readAllBytes(path); //get the files bytes
					
								output.writeByte(2); // tell the server a file is being sent
								output.writeInt(data.length); //tell the server the length of the file
								output.writeUTF(inputString); // tell the server the name of the file
								output.write(data);
							}catch(Exception e){
								System.out.println("File not Found");
							}				
						}
						else if(inputString.equals("/r")){ // request a file previously sent to the server (perhaps the user
                                                     // declined the file earlier) but wants it now
							System.out.println("ENTER FILE PATH");
							inputString = userInput.readLine();

							output.writeByte(3);
							output.writeUTF(inputString);
						}
					}
					else{ // send input string as a message
						output.writeByte(1);	// tell server its a string					
						output.writeUTF(inputString);
					}					
				}
            output.close();
            clientSoc.close();
        }catch (Exception e){
				   System.out.println(e);
		  }		
	     }else{System.out.println("Why bother?");}

		}catch (Exception e){
				System.out.println(e);
      }
	
   }
}

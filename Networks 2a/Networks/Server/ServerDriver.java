import java.io.InputStreamReader;
import java.io.BufferedReader;

public class ServerDriver{
   
   /**
      Creates a ServerHub and starts it. On "/end" being input by user will shutdown the server.
      Programme will only finish once each client connected at time of /end command presses enter
      on their keyboard. This is as user input and message sending are handled by the same thread
      in the client, and in order to shutdown without throwing an exception it is necessary that
      the client confirms to the server that it will not attempt to use the socket anymore, and it needs
      to wait until the call to read a line from user input ends.
   **/
   public static void main(String[] args){
      ServerHub serverHub = new ServerHub();
      serverHub.startServer();
      
    try{
           BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
           System.out.println("Server listening. type /end to quit (will have to wait for clients to confirm that server is shutdown).");
           while(true){
              if(userInput.readLine().equals("/end")){
                  serverHub.closeServer();
                  break;
             }
          }
          userInput.close();
       }catch(Exception e){
         System.out.println(e);
       }
   }
}
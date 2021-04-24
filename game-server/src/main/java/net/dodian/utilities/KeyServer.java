package net.dodian.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;

public class KeyServer extends Thread
{
 public static String modulus;
 public static String exponent;
 public static BigInteger Modulus;
 public static BigInteger Exponent;
 private static Socket socket;
 public static String CSM;
 public static String CSE;
    
    public static Thread thread = new Thread(){
        public void run(){
            try
            {
     
                int port = 43591;
                @SuppressWarnings("resource")
    			ServerSocket serverSocket = new ServerSocket(port);
                System.out.println("Keyserver started and listening to on port 43591");
     
                //Server is running always. This is done using this while(true) loop
                while(true)
                {
                    //Reading the initializer string from client (garbage string)
                    socket = serverSocket.accept();
                    
                    modulus = String.valueOf(Modulus)+"\n";
                    exponent = String.valueOf(Exponent)+"\n";
     
                    // Output/Input
                    OutputStream os = socket.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os);
                    BufferedWriter bw = new BufferedWriter(osw);
                    
                    InputStream in = socket.getInputStream();
                    InputStreamReader isr = new InputStreamReader(in);
                    BufferedReader br = new BufferedReader(isr);
                    
                    //Sends key pair to client.
                    bw.write(modulus);
                    System.out.println("Modulus sent to the client is : "+modulus);
                    bw.write(exponent);
                    System.out.println("Exponent sent to the client is : "+exponent);
                    bw.flush();
                    
                    //Receives keys from client.
                    br.readLine();
                    CSM = br.readLine();
                    CSE = br.readLine();
                    
                    if (CSM!=null&&CSE!=null){
                    	System.out.println("Handshake received.");
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                try {
    				socket.close();
    			} catch (IOException e1) {
    				// TODO Auto-generated catch block
    				e1.printStackTrace();
    			}
            }
        }
      };
    
    public static void startKeyServer()
    {
    	thread.start();
    }
    
    public static boolean verifiedKeys(){
    	BigInteger mod = new BigInteger(CSM);
    	BigInteger exp = new BigInteger(CSE);
    	
    	if ((mod.compareTo(Modulus)==0)&&
    			(exp.compareTo(Exponent)==0)){
    		return true;
    	}else return false;
    }
}

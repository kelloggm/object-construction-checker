import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.io.IOException;

public class ACSocketTest
{

    Socket makeSocket(String address, int port){

        try
        {
            Socket socket = new Socket(address, port);
            return socket;
        }
        catch(IOException i)
        {
            return null;
        }
    }

    void basicTest(String address, int port){
        try
        {
            // :: error: missing.alwayscall
            Socket socket2 = new Socket(address, port);
            Socket socket = new Socket(address, port);
            socket.close();
        }
        catch(IOException i)
        {

        }
    }


    void callMakeSocket(String address, int port){
        Socket socket = makeSocket(address, port);
        try
        {
            socket.close();
        }
        catch (IOException i)
        {
        }

    }


    void callMakeSocketWrong(String address, int port){
        // :: error: missing.alwayscall
        Socket socket = makeSocket(address, port);
    }

    void ifElseWithDeclaration(String address, int port, boolean b){
        Socket s1;
        Socket s2;
        try
        {
            if (b) {
                s1 = new Socket(address, port);
                s1.close();
            } else {
                // :: error: missing.alwayscall
                s2 = new Socket(address, port+1);
            }
        }
        catch (IOException i)
        {

        }
    }

    void testLoop(String address, int port) {
        Socket s = null;
        while (true) {
            // :: error: missing.alwayscall
            s = makeSocket(address, port);
        }
    }

    void overWrittingVarInLoop(String address, int port) {
        // :: error: missing.alwayscall
        Socket s = makeSocket(address, port);
        while (true) {
            // :: error: missing.alwayscall
            s = makeSocket(address, port);
        }
    }


    void loopWithNestedBranches(String address, int port, boolean b) {
        Socket s = null;
        while (true) {
            if (b) {
                // :: error: missing.alwayscall
                s = makeSocket(address, port);
            } else {
                // :: error: missing.alwayscall
                s = makeSocket(address, port);
            }
        }
    }


    void replaceVarWithNull(String address, int port, boolean b, boolean c) {
        // :: error: missing.alwayscall
        Socket s = makeSocket(address, port);
        if (b) {
            s = null;
        } else if (c) {
            s = null;
        } else {

        }
    }


    void ownershipTransfer(String address, int port) {
        Socket s1 = makeSocket(address, port);
        // :: error: missing.alwayscall
        Socket s2 = s1;
        if(true){

            try {
                s2.close();
            }
            catch (IOException i)
            {

            }
        }

    }

    void test(String address, int port)
    {
        try
        {
            // :: error: missing.alwayscall
            Socket socket = new Socket( address, 80 );

            // Create input and output streams to read from and write to the server
            PrintStream out = new PrintStream( socket.getOutputStream() );
            BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
//            socket.close();
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }

    @EnsuresCalledMethods(value = "#1", methods = "close")
    void closeSocket(Socket sock) {
        try {
            sock.close();
        } catch (IOException e) {

        }
    }

    void useCloseSocket(String address, int port) throws IOException {
        Socket sock = new Socket(address, port);
        closeSocket(sock);
    }
}


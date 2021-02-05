import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.objectconstruction.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Optional;
import javax.net.ssl.*;
import java.nio.channels.*;

public class ACSocketTest
{

    @Owning Socket makeSocket(String address, int port){

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
            // :: error: required.method.not.called
            Socket socket2 = new Socket(address, port);
            Socket specialSocket = new Socket(address, port);
            specialSocket.close();
        }
        catch(IOException i)
        {

        }
    }

    void tryWithResourcesTest(String address, int port) throws IOException {
        try (Socket s = new Socket(address, port)) {

        }
    }

    void callMakeSocketAndClose(String address, int port){
        Socket socket = makeSocket(address, port);
        try
        {
            socket.close();
        }
        catch (IOException i)
        {
        }

    }


    void callMakeSocket(String address, int port){
        // :: error: required.method.not.called
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
                // :: error: required.method.not.called
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
            try {
                s = new Socket(address, port);
                s.close();
            } catch (IOException e) {

            }
        }
    }

    void overWrittingVarInLoop(String address, int port) {
        // :: error: required.method.not.called
        Socket s = makeSocket(address, port);
        while (true) {
            try {
                // :: error: required.method.not.called
                s = new Socket(address, port);
            } catch (IOException e) {

            }
        }
    }


    void loopWithNestedBranches(String address, int port, boolean b) {
        Socket s = null;
        while (true) {
            if (b) {
                // :: error: required.method.not.called
                s = makeSocket(address, port);
            } else {
                // :: error: required.method.not.called
                s = makeSocket(address, port);
            }
        }
    }


    void replaceVarWithNull(String address, int port, boolean b, boolean c) {
        Socket s;
        try {
            // :: error: required.method.not.called
            s = new Socket(address, port);
        } catch (IOException e) {

        }
        if (b) {
            s = null;
        } else if (c) {
            s = null;
        } else {

        }
    }


    void ownershipTransfer(String address, int port) {
        Socket s1 = null;
        try {
            // :: error: required.method.not.called
            s1 = new Socket(address, port);
        } catch (IOException e) {

        }
        Socket s2 = s1;
        if(true){
            closeSocket(s2);
        }

    }

    void test(String address, int port)
    {
        try
        {
            // :: error: required.method.not.called
            Socket socket = new Socket( address, 80 );
            PrintStream out = new PrintStream( socket.getOutputStream() );
            BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
            in.close();
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }

    protected Socket sock;
    void connectToLeader(AtomicReference<Socket> socket) throws IOException {
        // :: error: required.method.not.called
        if (socket.get() == null) {
            throw new IOException("Failed connect to " );
        } else {
            // :: error: required.method.not.called
            sock = socket.get();
        }

    }


    Socket createSocket(boolean b, String address, int port) throws IOException {
        Socket sock;
        if (b) {
            // :: error: required.method.not.called
            sock = new Socket(address, port);
        } else {
            // :: error: required.method.not.called
            sock = new Socket(address, port);
        }

        sock.setSoTimeout(10000);
        closeSocket(sock);
        return sock;
    }

    void replaceVarWithNull(String address, int port) {
        try {
            // :: error: required.method.not.called
            Socket s = new Socket(address, port);
            s = null;
        } catch (IOException e) {

        }

    }

//    @EnsuresCalledMethodsIf(expression = "#1", methods = {"close"}, result = true)
//    void closeSocket(Socket sock) {
////        if (sock == null) {
////            return;
////        }
//
//        try {
//            sock.close();
//        } catch (IOException ie) {
//
//        }
//    }


    public static void ruok(String host, int port) {
        Socket s = null;
        try {
            s = new Socket(host, port);
        } catch (IOException e) {

        } finally {

            try {
                s.close();
            } catch (IOException e) {

            }

        }
    }

    @EnsuresCalledMethods(value = "#1", methods = "close")
    void closeSocket(Socket sock) {
        try {
            if(sock!=null){
                sock.close();
            }
        } catch (IOException e) {

        }
    }

    @EnsuresCalledMethods(value = "#1", methods = "close")
    void closeServerSocket(ServerSocket sock) {
        try {
            if(sock!=null){
                sock.close();
            }
        } catch (IOException e) {

        }
    }

    void useCloseSocket(String address, int port) throws IOException {
        Socket sock = new Socket(address, port);
        Socket s = getSocket(sock);
        closeSocket(sock);
    }



    void setSockOpts(Socket sock) throws SocketException {
        sock.setTcpNoDelay(true);
        sock.setKeepAlive(true);
        sock.setSoTimeout(1000);
    }

    void initiateConnection(SocketAddress endpoint, int timeout, SSLContext context, final Long sid) {
        Socket sock = null;
        try {
            sock = context.getSocketFactory().createSocket();
            setSockOpts(sock);
            sock.connect(endpoint, timeout);
            if (sock instanceof SSLSocket) {
                SSLSocket sslSock = (SSLSocket) sock;
                sslSock.startHandshake();

            }

        } catch (ClassCastException e){
            closeSocket(sock);
            return;
        } catch (IOException e) {
            closeSocket(sock);
            return;
        }

        try{
            startConnection(sock);
        }catch (IOException e) {
            closeSocket(sock);
        }
    }

    private boolean startConnection(@Owning Socket s) throws IOException {
        closeSocket(s);
        return true;
    }

    private boolean startConnection(@Owning SSLSocket s) throws IOException {
        closeSocket(s);
        return true;
    }



    @MustCall({"close"})
    class PrependableSocket extends Socket {

        public PrependableSocket(SocketImpl base) throws IOException {
            super(base);
        }
    }

    void makePrependableSocket() throws IOException {
        // :: error: required.method.not.called
        final PrependableSocket prependableSocket = new PrependableSocket(null);
    }

    /**
     * The errors in this method are caused by the lack of support for unconnected sockets.
     */
    void createNewServerSocket(InetSocketAddress address, boolean b, boolean c) throws IOException {
        ServerSocket socket;

        if (b) {
            // :: error: required.method.not.called
            socket = new ServerSocket();
        } else if (c) {
            // :: error: required.method.not.called
            socket = new ServerSocket();
        } else {
            // :: error: required.method.not.called
            socket = new ServerSocket();
        }

        socket.setReuseAddress(true);
        socket.bind(address);
        closeServerSocket(socket);
    }


    @Owning public SSLServerSocket createSSLServerSocket(SSLContext sslContext) throws IOException {
        SSLServerSocket sslServerSocket = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket();
        return configureSSLServerSocket(sslServerSocket);
    }

    private SSLServerSocket nonOwningSSField;

    void assignToNonOwningViaCast(SSLContext sslContext) throws IOException {
        // :: error: required.method.not.called
        nonOwningSSField = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket();
    }

    private SSLServerSocket configureSSLServerSocket(@Owning SSLServerSocket socket) {
        return socket;
    }

    public SSLSocket createSSLSocket(@Owning Socket socket, byte[] pushbackBytes, SSLContext sslContext) throws IOException {
        SSLSocket sslSocket;
        if (pushbackBytes != null && pushbackBytes.length > 0) {
            sslSocket = (SSLSocket) sslContext.getSocketFactory()
                    .createSocket(socket, null, socket.getPort(), true);
        } else {
            sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(socket, null, socket.getPort(), true);
        }
        return configureSSLSocket(sslSocket, false);
    }

    private SSLSocket configureSSLSocket(@Owning SSLSocket socket, boolean isClientSocket) {
        SSLParameters sslParameters = socket.getSSLParameters();
//        configureSslParameters(sslParameters, isClientSocket);
        socket.setSSLParameters(sslParameters);
        socket.setUseClientMode(isClientSocket);
        return socket;
    }


    private void updateSocketAddresses( SelectionKey sockKey ) {
        // :: error: required.method.not.called
        Socket socket = ((SocketChannel) sockKey.channel()).socket();
        SocketAddress localSocketAddress = socket.getLocalSocketAddress();
        SocketAddress remoteSocketAddress = socket.getRemoteSocketAddress();
    }


    private void recieverParameterWithCasting(@Owning SelectableChannel channel1) throws IOException {
        try {
            ((SocketChannel) channel1).socket();
        } finally {
            channel1.close();
        }
    }

    // SelectionKey doesn't implement Closeable
    private void updateSocketAddressesWithOwning(@Owning SelectionKey sockKey ) {
        // :: error: required.method.not.called
        Socket socket = ((SocketChannel) sockKey.channel()).socket();
        SocketAddress localSocketAddress = socket.getLocalSocketAddress();
        SocketAddress remoteSocketAddress = socket.getRemoteSocketAddress();
    }

    @NotOwning Socket getSocket(Socket s) {
        return s;
    }

    private ServerSocket testMCCParamInReturn() throws IOException{
        ServerSocketChannel chan = ServerSocketChannel.open();
        return chan.socket();
    }

}

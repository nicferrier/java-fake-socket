package fakesocket;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import java.net.SocketImpl;
import java.net.SocketImplFactory;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetSocketAddress;


/** Obvious tests for the fake socket.
 */
class TestFakeSocket {

    /** Tests for the underyling blocking queue socket implementation.
     */
    void testCommunicatingSocket() throws IOException {
        CommunicatingSocket s1 = new CommunicatingSocket();
        CommunicatingSocket s2 = new CommunicatingSocket();
        s1.connectComms(s2);
        OutputStream out = s1.getOutputStream();
        out.write(1);
        InputStream in = s2.getInputStream();
        int read = in.read();
        assert(read == 1);
    }

    /** Socket based acceptance test.
     *
     * Changes the local socket implementation and then starts a
     * server and a client.
     */
    void testFakeSocks() throws IOException {
        final Backplane backplane = new Backplane();
        boolean doTest = true; // turn this off and it's a test of tcp sockets
        if (doTest) {
            ServerSocket.setSocketFactory(new SocketImplFactory() {
                    public SocketImpl createSocketImpl() {
                        return new FakeSocketImpl(backplane);
                    }
                });
            Socket.setSocketImplFactory(new SocketImplFactory() {
                    public SocketImpl createSocketImpl() {
                        return new FakeSocketImpl(backplane);
                    }
                });
        }

        final ServerSocket serverSock = new ServerSocket();
        serverSock.bind(new InetSocketAddress(8000));
        Thread listener = new Thread(new Runnable() {
                public void run () {
                    try {
                        System.out.println("serversocket accepting thread");
                        Socket sock = serverSock.accept();
                        System.out.println("serversocket accepted");
                        sock.getOutputStream().write("hello".getBytes());
                        System.out.println("serversocket written");
                        sock.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("some error");
                    }

                }
            });
        listener.start();

        Thread sender = new Thread(new Runnable() {
                public void run () {
                    try {
                        System.out.println("socket connecting thread");
                        Thread.sleep(2000);
                        Socket sock = new Socket("localhost", 8000);
                        System.out.println("socket connected");
                        InputStream in = sock.getInputStream();
                        System.out.println("socket got inputstream");
                        byte[] buf = new byte[1000];
                        int red = in.read(buf, 0, 1000);
                        int read = 0;
                        while (red > 0) {
                            read += red;
                            System.out.printf("read %s red %s data %s\n",
                                              read,
                                              red,
                                              new String(buf, 0, read, "UTF-8"));
                            red = in.read(buf, read, 1000 - read);
                        }
                        System.out.println("socket read from inputstream");
                        System.out.printf("socket read %s red %s\n", read, red);
                        System.out.printf("socket read %s\n", new String(buf, 0, read, "UTF-8"));
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                        System.out.println("some error");
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("some error");
                    }
                }
            });
        sender.start();
    }
    
    public static void main (String[] argv) throws IOException {
        TestFakeSocket t = new TestFakeSocket();
        t.testCommunicatingSocket();
        t.testFakeSocks();
    }
}

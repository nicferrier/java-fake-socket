package fakesocket;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import java.net.SocketImpl;
import java.net.SocketImplFactory;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetSocketAddress;


class TestFakeSocket {
    void doit() throws IOException {
        CommunicatingSocket s1 = new CommunicatingSocket();
        CommunicatingSocket s2 = new CommunicatingSocket();
        s1.connectComms(s2);
        OutputStream out = s1.getOutputStream();
        out.write(1);
        InputStream in = s2.getInputStream();
    }

    void testSocks() throws IOException {
        final Backplane backplane = new Backplane();
        boolean doTest = true;
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
                        int read = in.read(buf, 0, 1000);
                        System.out.println("socket read from inputstream");
                        System.out.println("socket read " + read);
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
        //new FakeSocket().doit();
        new TestFakeSocket().testSocks();
    }
}

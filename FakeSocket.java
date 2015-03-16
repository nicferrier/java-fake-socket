import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.net.SocketOption;
import java.net.SocketException;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.HashMap;

// Test
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetSocketAddress;


public class FakeSocket {

    class CommunicatingSocket {
        final BlockingQueue<Byte> in = new ArrayBlockingQueue<Byte>(5);
        final BlockingQueue<Byte> out = new ArrayBlockingQueue<Byte>(5);
        Thread[] communicators;
        
        void connectComms (final CommunicatingSocket sock) {
            Thread[] comms = {           
                new Thread(new Runnable () {
                        public void run () {
                            try {
                                while (true) {
                                    byte b = in.take();
                                    sock.out.put(b);
                                }
                            }
                            catch (InterruptedException e) {
                            }           
                        }
                    }),
                new Thread(new Runnable () {
                        public void run () {
                            try {
                                while (true) {
                                    byte b = out.take();
                                    sock.in.put(b);
                                }
                            }
                            catch (InterruptedException e) {
                            }           
                        }
                    })
            };
            this.communicators = comms;
            for (Thread t: comms) {
                t.setDaemon(true);
                t.start();
            }
        }

        public OutputStream getOutputStream() {
            return new OutputStream () {
                public void write (int a) throws IOException {
                    try {
                        out.put((byte)a);
                    }
                    catch (InterruptedException e) {
                        throw new IOException("interrupted");
                    }
                }
                
                public void write (byte[] b, int offset, int length) throws IOException {
                    for (int i=offset; i < offset + length; i++) {
                        try {
                            out.put (b[i]);
                        }
                        catch (InterruptedException e) {
                            throw new IOException("interrupted");
                        }
                    }
                }
            };
        }
        
        public InputStream getInputStream() {
            return new InputStream() {
                public int read() throws IOException {
                    try {
                        return in.take();
                    }
                    catch (InterruptedException e) {
                        throw new IOException("interrupted");
                    }
                }
                
                public int read(byte[] b, int offset, int length) throws IOException {
                    for (int i = offset; i < offset + length; i++) {
                        try {
                            b [i] = in.take ();
                        }
                        catch (InterruptedException e) {
                            throw new IOException("interrupted");
                        }
                    }
                    return length;
                }
            };
        }
    }

    class Address {
        String ip;
        int port;
        
        Address(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        public boolean equals(Object a) {
            Address aa;
            try {
                aa = (Address) a;
            }
            catch (ClassCastException e) {
                return false;
            }
            return aa.hashCode() == this.hashCode();
        }

        public int hashCode () {
            String x = String.format("%s::%s", this.ip, this.port);
            return x.hashCode();
        }
    }

    class Backplane {
        HashMap<Address,FakeSocketImpl>
            listeners = new HashMap<Address,FakeSocketImpl>();

        FakeSocketImpl get(Address a) {
            FakeSocketImpl f = listeners.get(a);
            if (f == null) {
                Address v = new Address("0.0.0.0", a.port);
                f = listeners.get(v);
            }
            return f;
        }
    }

    class FakeSocketImpl extends SocketImpl {
        
        boolean isStream = false;
        int listenBacklog = 1;
        BlockingQueue<CommunicatingSocket> accepts;
        CommunicatingSocket thisSock;
        Backplane backplane;

        FakeSocketImpl(Backplane backplane) {
            this.backplane = backplane;
            this.thisSock = new CommunicatingSocket();
        }

        protected void create(boolean stream) throws IOException {
            this.isStream = stream;
        }

        FakeSocketImpl getFakeSocket(String host, int port) {
            Address a = new Address(host, port);
            FakeSocketImpl f = backplane.get(a);
            return f;
        }

        protected void connect(String host, int port) throws IOException {
            FakeSocketImpl f = getFakeSocket(host, port);
            if (f == null) {
                throw new IOException("connect has unknown address");
            }
            try {
                CommunicatingSocket remoteSock = new CommunicatingSocket();
                remoteSock.connectComms(thisSock);
                System.out.println("connected remote sock");
                f.accepts.put(remoteSock);
                System.out.println("put onto remote sock");
            }
            catch (InterruptedException e) {
                throw new IOException("no idea");
            }
        }

        protected void connect(InetAddress address, int port) throws IOException {
            connect(address.getHostAddress(), port);
        }

        protected void connect(SocketAddress address, int timeout) throws IOException {
            connect(((InetSocketAddress)address).getAddress().getHostAddress(),
                    ((InetSocketAddress)address).getPort());
        }

        protected void bind(InetAddress host, int port) throws IOException {
            this.accepts = new ArrayBlockingQueue<CommunicatingSocket>(this.listenBacklog);
            Address listening = new Address(host.getHostAddress(), port);
            backplane.listeners.put(listening, this);
        }

        protected void listen(int backlog) throws IOException {
            this.listenBacklog = backlog;
        }

        protected void accept(SocketImpl s) throws IOException {
            FakeSocketImpl sock = (FakeSocketImpl)s;
            try {
                sock.thisSock = accepts.take();
            }
            catch (InterruptedException e) {
                throw new IOException("no idea");
            }
        }

        protected InputStream getInputStream() throws IOException {
            return this.thisSock.getInputStream();
        }

        protected OutputStream getOutputStream() throws IOException {
            return this.thisSock.getOutputStream();
        }

        protected int available() throws IOException {
            return 0; // number of bytes in the buffers
        }

        protected void close() throws IOException {
        }

        protected void sendUrgentData (int data) throws IOException {
        }

        public void setOption(int optID, Object value) throws SocketException {
        }

        public Object getOption(int optID) throws SocketException {
            return null;
        }
    }


    void doit() throws IOException {
        CommunicatingSocket s1 = new CommunicatingSocket();
        CommunicatingSocket s2 = new CommunicatingSocket();
        s1.connectComms(s2);
        OutputStream out = s1.getOutputStream();
        out.write(1);
        System.out.println("after write");
        InputStream in = s2.getInputStream();
        System.out.println("value from in: " + in.read());
        System.out.println("done");
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
                        int read = in.read(buf, 0, 4);
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
        new FakeSocket().testSocks();
    }
}

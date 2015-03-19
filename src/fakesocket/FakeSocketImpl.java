package fakesocket;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.net.SocketOption;
import java.net.SocketException;
import java.net.InetSocketAddress;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.HashMap;


public class FakeSocketImpl extends SocketImpl {
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
            f.accepts.put(remoteSock);
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
        this.thisSock.close();
    }

    protected void sendUrgentData (int data) throws IOException {
    }

    public void setOption(int optID, Object value) throws SocketException {
    }

    public Object getOption(int optID) throws SocketException {
        return null;
    }
}

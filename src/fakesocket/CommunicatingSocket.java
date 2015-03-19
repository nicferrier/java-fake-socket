package fakesocket;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class CommunicatingSocket {

    class ByteOrEOF {
        byte b;
        boolean isEOF = false;

        ByteOrEOF() {
            this.isEOF = true;
        }

        ByteOrEOF(byte b) {
            this.b = b;
        }

        boolean isEOF() {
            return this.isEOF;
        }

        byte getByte() {
            return b;
        }

        public String toString() {
            return String.format("isEOF? %s b: %s", isEOF, b);
        }
    }

    class ClosedState {
        private boolean isClosed = false;
        CommunicatingSocket sock;
        ClosedState(CommunicatingSocket sock) {
            this.sock = sock;
        }
        void setClosed() {
            System.out.printf("cs[%s]> closed\n", this.sock);
            isClosed = true;
        }
        boolean get() {
            return isClosed;
        }
    }

    final ClosedState isClosed = new ClosedState(this);
    final BlockingQueue<ByteOrEOF> in = new ArrayBlockingQueue<ByteOrEOF>(5);
    final BlockingQueue<ByteOrEOF> out = new ArrayBlockingQueue<ByteOrEOF>(5);
    Thread[] communicators;
    
    /// FIXME - don't let the threads carry on if we close
    void connectComms (final CommunicatingSocket sock) {
        Thread[] comms = {           
            new Thread(new Runnable () {
                    public void run () {
                        try {
                            ByteOrEOF b = new ByteOrEOF((byte)1);
                            while (!b.isEOF()) {
                                b = in.take();
                                System.out.printf("sock thread in> %s\n", b);
                                sock.out.put(b);
                            }
                            sock.isClosed.setClosed();
                        }
                        catch (InterruptedException e) {
                        }           
                    }
                }),
            new Thread(new Runnable () {
                    public void run () {
                        try {
                            ByteOrEOF b = new ByteOrEOF((byte)1);
                            while (!b.isEOF()) {
                                b = out.take();
                                System.out.printf("sock thread out> %s\n", b);
                                sock.in.put(b);
                            }
                            sock.isClosed.setClosed();
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

    public void close() {
        try {
            out.put(new ByteOrEOF());
        }
        catch (InterruptedException e) {
            // FIXME
        }
        this.isClosed.setClosed();
    }

    public OutputStream getOutputStream() {
        final ClosedState isClosed = this.isClosed;
        return new OutputStream () {
            public void write (int a) throws IOException {
                if (isClosed.get()) {
                    throw new IOException("closed");
                }
                try {
                    out.put(new ByteOrEOF((byte)a));
                }
                catch (InterruptedException e) {
                    throw new IOException("interrupted");
                }
            }
                
            public void write (byte[] b, int offset, int length) throws IOException {
                if (isClosed.get()) {
                    throw new IOException("closed");
                }
                // System.out.println("CS write put " + new String(b, offset, length, "UTF-8"));
                for (int i=offset; i < offset + length; i++) {
                    try {
                        out.put(new ByteOrEOF(b[i]));
                    }
                    catch (InterruptedException e) {
                        throw new IOException("interrupted");
                    }
                }
            }
        };
    }
        
    public InputStream getInputStream() {
        final ClosedState isClosed = this.isClosed;
        return new InputStream() {
            public int read() throws IOException {
                int avail = in.size();
                if (avail < 1 && isClosed.get()) {
                    return -1;
                }
                else {
                    try {
                        ByteOrEOF b = in.take();
                        if (b.isEOF()) {
                            return -1;
                        }
                        else {
                            return b.getByte();
                        }
                    }
                    catch (InterruptedException e) {
                        throw new IOException("interrupted");
                    }
                }
            }
                
            public int read(byte[] buf, int offset, int length) throws IOException {
                int avail = in.size();
                System.out.printf("cs> read %s %s %s\n", offset, length, avail);
                if (avail < 1 && isClosed.get()) {
                    return -1;
                }
                else {
                    try {
                        if (avail > 0) {
                            int len = (length > avail) ? avail:length;
                            for (int i = offset; i < offset + len; i++) {
                                ByteOrEOF b = in.take ();
                                if (b.isEOF()) {
                                    return i - 1;
                                }
                                else {
                                    buf[i] = b.getByte();
                                }
                            }
                            return avail;
                        }
                        else {  // block for 1 read
                            ByteOrEOF b = in.take();
                            if (b.isEOF()) {
                                return -1;
                            }
                            else {
                                buf [offset] = b.getByte ();
                            }
                            return 1;
                        }
                    }
                    catch (InterruptedException e) {
                        throw new IOException("interrupted");
                    }
                }
            }
        };
    }
}

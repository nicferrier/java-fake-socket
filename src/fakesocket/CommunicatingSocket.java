package fakesocket;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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
                // System.out.println("CS write put " + new String(b, offset, length, "UTF-8"));
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
                try {
                    // System.out.printf("CS read has %s\n", in.size());
                    b[offset] = in.take();
                    int len = (length > in.size()) ? 1+in.size() : length;
                    for (int i = ++offset; i < offset + len-1; i++) {
                        b [i] = in.take ();
                    }
                    // System.out.printf("CS read returning %s\n", len);
                    return len;
                }
                catch (InterruptedException e) {
                    throw new IOException("interrupted");
                }
            }
        };
    }
}

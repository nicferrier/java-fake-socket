package fakesocket;

import java.util.HashMap;

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

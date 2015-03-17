package fakesocket;

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


        

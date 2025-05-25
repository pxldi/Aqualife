package common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public final class NeighborUpdate implements Serializable{
    private String id;
    private InetSocketAddress left;
    private InetSocketAddress right;

    public NeighborUpdate(String id, InetSocketAddress left, InetSocketAddress right) {
        this.id = id;
        this.left = left;
        this.right = right;
    }

    public String getId() {
        return id;
    }

    public InetSocketAddress getLeft() {
        return left;
    }

    public InetSocketAddress getRight() {
        return right;
    }
}

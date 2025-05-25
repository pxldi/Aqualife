package aqua.src.common.msgtypes;

import java.io.Serializable;

public final class DeregisterRequest implements Serializable {
	private final String id;

	public DeregisterRequest(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
}

package client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import common.Direction;
import common.FishModel;

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected volatile InetSocketAddress leftNeighbor;
	protected volatile InetSocketAddress rightNeighbor;
	protected final Set<FishModel> fishes;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;

	public boolean hasToken = false;
	public Timer timer = new Timer();

	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishes = Collections.newSetFromMap(new ConcurrentHashMap<>());
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	synchronized void onNeighborUpdate(InetSocketAddress leftNeighbor, InetSocketAddress rightNeighbor) {
		if (leftNeighbor != null)
            this.leftNeighbor = leftNeighbor;
        if (rightNeighbor != null)
            this.rightNeighbor = rightNeighbor;
	}

	public synchronized void newFish(int x, int y) {
		if (fishes.size() < MAX_FISHIES) {
			x = Math.min(x, WIDTH - FishModel.getXSize() - 1);
			y = Math.min(y, HEIGHT - FishModel.getYSize());

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishes.add(fish);
		}
	}

	synchronized void receiveFish(FishModel fish) {
		fish.setToStart();
		fishes.add(fish);
	}

	public void receiveToken() {
		hasToken = true;
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				InetSocketAddress address = leftNeighbor;
				forwarder.handoffToken(address);
				hasToken = false;
			}
		}, 2000);
	}

	public boolean hasToken() {
		return hasToken;
	}

	public String getId() {
		return id;
	}

	public InetSocketAddress getLeftNeighbor() {
		return leftNeighbor;
	}

	public InetSocketAddress getRightNeighbor() {
		return rightNeighbor;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishes.iterator();
	}

	private synchronized void updateFishes() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge()) {
                if (this.hasToken()) {
                    forwarder.handOff(fish, this);
                } else {
                    fish.reverse();
                }
            }

			if (fish.disappears())
				it.remove();
		}
	}

	private synchronized void update() {
		updateFishes();
		setChanged();
		notifyObservers();
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public synchronized void finish() {
		forwarder.deregister(id);
	}

}
package aqua.blatt1.broker;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;
import java.awt.Window;

public class Broker {
    private int clientCounter = 0;
    private ClientCollection<InetSocketAddress> clients = new ClientCollection<>();
    private Endpoint endpoint = new Endpoint(4711);
    private ExecutorService executor = Executors.newFixedThreadPool(8);
    private volatile boolean stopRequested = false;
    private JFrame frame;

    private class BrokerTask implements Runnable{
        private ReentrantReadWriteLock lock;
        private Message message;

        public BrokerTask(Message message) {
            this.lock = new ReentrantReadWriteLock();
            this.message = message;
        }

        private void register(Message message) {
            InetSocketAddress sender = message.getSender();
            String clientId = "client" + ++clientCounter;
            lock.writeLock().lock();
            clients.add(clientId, sender);
            lock.writeLock().unlock();
            endpoint.send(sender, new RegisterResponse(clientId));
            System.out.println("Client " + clientId + " registered");
        }

        private void deregister(Message message) {
            DeregisterRequest deregisterRequest = (DeregisterRequest) message.getPayload();
            String clientId = deregisterRequest.getId();
            lock.readLock().lock();
            int clientIndex = clients.indexOf(clientId);
            lock.readLock().unlock();

            lock.writeLock().lock();
            clients.remove(clientIndex);
            lock.writeLock().unlock();
            System.out.println("Client " + clientId + " deregistered");
        }

        private void handOff(Message message) {
            HandoffRequest handoffRequest = (HandoffRequest) message.getPayload();
            InetSocketAddress receiver;
            FishModel fish = handoffRequest.getFish();
            lock.readLock().lock();
            int clientIndex = clients.indexOf(message.getSender());
            lock.readLock().unlock();
            if (fish.getDirection() == Direction.LEFT)
                receiver = clients.getLeftNeighborOf(clientIndex);
            else
                receiver = clients.getRightNeighborOf(clientIndex);
            endpoint.send(receiver, handoffRequest);
        }

        @Override
        public void run() {
            var payload = message.getPayload();
            if (payload instanceof RegisterRequest)
                register(message);
            if (payload instanceof DeregisterRequest)
                deregister(message);
            if (payload instanceof HandoffRequest)
                handOff(message);
            if (payload instanceof PoisonPill)
                stopRequested = true;
        }
    }

    public void broker() {
        frame = new JFrame("Broker");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setType(Window.Type.UTILITY);
        Thread stopRequestedDialogThread = new Thread(() -> {
            JOptionPane.showOptionDialog(
                frame,
                "Press Ok button to stop server",
                "",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null, new Object[]{"OK"}, null);
            stopRequested = true;
        });
        stopRequestedDialogThread.start();

        while (!stopRequested) {
            Message msg = endpoint.blockingReceive();
            var brokerTask = new BrokerTask(msg);
            executor.execute(brokerTask);
        }
        executor.shutdownNow();
        frame.dispose();
        System.out.println("Broker stopped.");
        System.exit(0);
    }

    public static void main(String[] args) {
        Broker broker = new Broker();
        broker.broker();
    }
}

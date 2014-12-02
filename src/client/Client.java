import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;
import java.util.Vector;

public class Client extends UnicastRemoteObject implements IGameEngine, IGui{

    /**
     * Instance of the client GUI
     */
    private Gui gui;

    /**
     * Server's stub
     */
    private IServer server;

    /**
     * Server's IP address
     */
    private String serverAddress;

    /**
     * UUID attributed by the server
     */
    private UUID uuid;

    public Client(String serverAddress) throws java.rmi.RemoteException {
        super();
        this.serverAddress = serverAddress;
    }

    /**
     *
     */
    public void connectToServer() {
        //look up the server and store a reference to the returned object in a class variable
        try {
            server = (IServer) java.rmi.Naming.lookup(this.serverAddress);
            //give the server a remote reference to myself with which it can call me back
            this.uuid = server.registerClient((IGui) java.rmi.server.RemoteObject.toStub(this));

        } catch (Exception e) {
            //System.out.println("Help! " + e);
            e.printStackTrace();
        }
    }

    @Override
    public void setGUI(Gui gui) {
        this.gui = gui;
    }

    @Override
    public int getScore(){
        try {
            return server.getScore(uuid);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public boolean isGameInProgress() {
        try {
            return server.isGameInProgress(uuid);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void createGame() {
        try {
            server.createGame(uuid);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            server.close(uuid);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setLeftPressed(boolean pressed) {
        try {
            server.setLeftPressed(uuid, pressed);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setRightPressed(boolean pressed) {
        try {
            server.setRightPressed(uuid, pressed);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUpPressed(boolean pressed) {
        try {
            server.setUpPressed(uuid, pressed);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setDownPressed(boolean pressed) {
        try {
            server.setDownPressed(uuid, pressed);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(final Vector<Rectangle> vDisplayRoad, final Vector<Rectangle> vDisplayObstacles, final Vector<Rectangle> vDisplayCars, final Car myCar, final int pos, final int nbParticipants, final boolean bGameOver, final String sPosition) throws RemoteException {
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    gui.update(vDisplayRoad, vDisplayObstacles, vDisplayCars, myCar, pos, nbParticipants, bGameOver, sPosition);
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setPlayButtonEnabled(boolean enabled) throws RemoteException {
        gui.jButton1.setEnabled(enabled);
    }

    public static void main(String[] args) {

        String url = "localhost";

        if (args.length == 1)
            if (args[0].length() > 15)
                url = args[0];
            else
                url = args[0];

        IGameEngine client = null;
        try {
            client = new Client("//"+ url +"/SRGameServer");
        } catch (Exception e) {
            System.out.println("Fatal exception on startup: " + e);
            System.exit(1);
        }


        //client.setPlayer(new PlayerInfo(playerName));

        Gui gui = new Gui(client);
        gui.setVisible(true);
        client.setGUI(gui);

        ((Client)client).connectToServer();


    }
}

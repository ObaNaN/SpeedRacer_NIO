import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.UUID;


public class Server extends UnicastRemoteObject implements IServer{

    private Core currentCore;
    private int playersCountPerGame;
    private HashMap<UUID, Player> playersByUuid;

    public Server(int playersCountPerGame) throws Exception{
        super();
        this.playersCountPerGame = playersCountPerGame;
        //register with RMIReg
        startRegistry(1099);
        playersByUuid = new HashMap<>();
        java.rmi.Naming.rebind("//localhost/SRGameServer", this);

        System.out.println("Server ready");
    }

    private void startRegistry(int RMIPortNum) throws RemoteException {
        try {
            Registry registry =
                    LocateRegistry.getRegistry(RMIPortNum);
            registry.list();

            // This call will throw an exception
            // if the registry does not already exist
        } catch (RemoteException e) {
            // No valid registry at that port.
            Registry registry =
                    LocateRegistry.createRegistry(RMIPortNum);
        }
    }

	public static void main(String[] args) {
        int playersCount = 4;

        if (args.length == 1){
            try{
                playersCount = Integer.parseInt(args[0]);
            } catch(Exception e){
                System.out.println("Invalid number");
            }
        }

        try {
            new Server(playersCount);
        } catch (Exception e) {
            System.out.println("Fatal: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public UUID registerClient(IGui clientGui) throws RemoteException {

        if(currentCore == null)
            currentCore = new Core();
        Player newPlayer =  new Player(clientGui, currentCore);
        UUID newUuid = UUID.randomUUID();
        currentCore.addPlayer(newPlayer);
        playersByUuid.put(newUuid, newPlayer);

        if(currentCore.getPlayersCount() == playersCountPerGame){
            Thread t =  new Thread(currentCore);
            t.start();
            currentCore = null;
        }

        return newUuid;
    }

    @Override
    public int getScore(UUID uuid) throws RemoteException {
        return playersByUuid.get(uuid).getScore();
    }

    @Override
    public boolean isGameInProgress(UUID uuid) throws RemoteException {
        return playersByUuid.get(uuid).isGameInProgress();
    }

    @Override
    public void createGame(UUID uuid) throws RemoteException {
        playersByUuid.get(uuid).setReadyToStart(true);
    }

    @Override
    public void close(UUID uuid) throws RemoteException {
        playersByUuid.get(uuid).getCore().removePlayer(playersByUuid.get(uuid));
    }

    @Override
    public void setLeftPressed(UUID uuid, boolean pressed) throws RemoteException {
        playersByUuid.get(uuid).setLeftPressed(pressed);
    }

    @Override
    public void setRightPressed(UUID uuid, boolean pressed) throws RemoteException {
        playersByUuid.get(uuid).setRightPressed(pressed);
    }

    @Override
    public void setUpPressed(UUID uuid, boolean pressed) throws RemoteException {
        playersByUuid.get(uuid).setUpPressed(pressed);
    }

    @Override
    public void setDownPressed(UUID uuid, boolean pressed) throws RemoteException {
        playersByUuid.get(uuid).setDownPressed(pressed);
    }
}

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;


public interface IServer extends Remote{

    //public void setGUI(UUID uuid, IGui gui) throws RemoteException;
    public UUID registerClient(IGui clientGUI) throws RemoteException;
    public int getScore(UUID uuid) throws RemoteException;
    public boolean isGameInProgress(UUID uuid) throws RemoteException;

    public void createGame(UUID uuid) throws RemoteException;

    public void close(UUID uuid) throws RemoteException;

    public void setLeftPressed(UUID uuid, boolean pressed) throws RemoteException;
    public void setRightPressed(UUID uuid, boolean pressed) throws RemoteException;
    public void setUpPressed(UUID uuid, boolean pressed) throws RemoteException;
    public void setDownPressed(UUID uuid, boolean pressed) throws RemoteException;
}

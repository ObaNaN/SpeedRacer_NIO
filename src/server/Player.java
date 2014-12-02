import java.rmi.RemoteException;
import java.util.Vector;

public class Player extends Car implements IGameClient{

    private Core core;
    private IGui gui;
    private boolean connected;

    /**
     * True if the player is pressing the up arrow key
     */
    private boolean upKeyPressed;

    /**
     * True if the player is pressing the down arrow key
     */
    private boolean downKeyPressed;

    /**
     * True if the player is pressing the right arrow key
     */
    private boolean rightKeyPressed;

    /**
     * True if the player is pressing the left arrow key
     */
    private boolean leftKeyPressed;

    /**
     * Integer representation of the final position (rank) of the player after passing the finish line
     */
    private int position;

    /**
     * The player's score
     */
    private int score;

    private boolean finishLineCrossed;

    private Vector<Rectangle> displayedRoad;
    private Vector<Rectangle> displayedObstacles;
    private Vector<Rectangle> displayedCars;

    private boolean readyToStart;

    public Player(IGui stub, Core core){
        super(158,43100,32,64,6,1,0,0.5,0,0,true);
        this.core =core;
        this.gui = stub;
        this.connected = true;
        this.score = 0;
        this.upKeyPressed = false;
        this.downKeyPressed = false;
        this.rightKeyPressed = false;
        this.leftKeyPressed = false;
        this.readyToStart = false;
    }

    public Core getCore() {
        return core;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setLeftPressed(boolean pressed){
        this.leftKeyPressed = pressed;
    }

    public void setRightPressed(boolean pressed){
        this.rightKeyPressed = pressed;
    }

    public void setUpPressed(boolean pressed){
        this.upKeyPressed = pressed;
    }

    public void setDownPressed(boolean pressed){
        this.downKeyPressed = pressed;
    }

    public boolean isUpKeyPressed() {
        return upKeyPressed;
    }

    public boolean isDownKeyPressed() {
        return downKeyPressed;
    }

    public boolean isRightKeyPressed() {
        return rightKeyPressed;
    }

    public boolean isLeftKeyPressed() {
        return leftKeyPressed;
    }

    public boolean isReadyToStart() {
        return readyToStart;
    }

    public void setReadyToStart(boolean readyToStart) {
        this.readyToStart = readyToStart;
    }

    public void setDisplayedRoad(Vector<Rectangle> displayedRoad) {
        this.displayedRoad = displayedRoad;
    }

    public void setDisplayedObstacles(Vector<Rectangle> displayedObstacles) {
        this.displayedObstacles = displayedObstacles;
    }

    public void setDisplayedCars(Vector<Rectangle> displayedCars) {
        this.displayedCars = displayedCars;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isGameInProgress(){
        return core.isGameInProgress();
    }

    public boolean isFinishLineCrossed() {
        return finishLineCrossed;
    }

    public void setFinishLineCrossed(boolean finishLineCrossed) {
        this.finishLineCrossed = finishLineCrossed;
    }

    public String positionToString(){
        switch(position){
            case 1:
                return "1st";
            case 2:
                return "2nd";
            case 3:
                return "3rd";
            default:
                return position +"th";
        }
    }

    public Car toCar(){
        Car c = new Car(this.x, this.y, this.width, this.height, this.id, this.effect, this.xSpeed, this.ySpeed, this. xAcc, this.yAcc, this.Racer);
        c.bustedSpeed = this.bustedSpeed;
        c.bustedTime = this.bustedTime;
        return c;
    }

    @Override
    public void update(int nbParticipants){
        if(this.connected)
            try {
                this.gui.update(displayedRoad, displayedObstacles, displayedCars, this.toCar(), position, nbParticipants, this.finishLineCrossed, this.positionToString());
            } catch (RemoteException e) {
                e.printStackTrace();
                this.connected = false;
                this.readyToStart = false;
            }
    }

    @Override
    public void setPlayButtonEnabled(boolean enabled) {
        try {
            gui.setPlayButtonEnabled(enabled);
        } catch (RemoteException e) {
            e.printStackTrace();
            this.connected = false;
        }
    }
}

public interface IGameEngine {
    public void setGUI(Gui gui);
    public int getScore();
    public boolean isGameInProgress();
    public void createGame();
    public void close();
    public void setLeftPressed(boolean pressed);
    public void setRightPressed(boolean pressed);
    public void setUpPressed(boolean pressed);
    public void setDownPressed(boolean pressed);
}

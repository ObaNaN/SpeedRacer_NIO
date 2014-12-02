/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.Vector;
import java.util.Iterator;

/**
 * Performs most of the computations
 * @author Sam
 * @version 1.0
 */
public class Core implements Runnable{

    /**
     * The delay in ms between two loops
     */
    private int tickDelay;

    /**
     * True if the finish line has been passed. False otherwise
     */
    private boolean gameFinishing;

    /**
     * True if the player is currently playing. False otherwise
     */
    private boolean gameInProgress;

    /**
     * The number of competitors (not civilians)
     */
    private int nbParticipants;

    /**
     * Vector containing the road elements (including the obstacles)
     */
    private Vector<Rectangle>[] tabRoad;

    /**
     * Vector containing the obstacles that must be taken into account for collision detection (thus also includes cars)
     */
    private Vector<CollidableRectangle>[] tabObstacles;

    /**
     * Vector containing the cars
     */
    private Vector<Car> cars;

    /**
     * The finite state array representing the finite state machine
     */
    private FiniteState[] states;

    /**
     * The absolute position of the police car on the y-axis
     */
    private int policePos;

    /**
     * Used to count the time ticks (1 tick = 50ms, 20 ticks = 1 second) for score update
     */
    private int runTime;

    /**
     * Same as above, but for game updates
     */
    private int gameRunTime;

    /**
     * Tells the frequency of updates (gameMaxRunTime = X means one update every X ticks)
     */
    private int gameMaxRunTime;

    private int playersCount;

    private int connectedPlayersCount;

    public Core(){

        tickDelay = 30;
        gameFinishing = false;
        gameInProgress = false;
        tabRoad = new Vector[108];
        tabObstacles = new Vector[109];
        cars = new Vector<>();
        runTime = 0;
        gameRunTime = 0;
        gameMaxRunTime = 1;
        playersCount = 0;
    }

    public boolean isGameInProgress() {
        return gameInProgress;
    }

    /**
     * Initializes the finite state machine and stores it in states[].
     * The probability to reach one state is given in such a way that the straight lines are preferred over the curves and appearing/disappeaing road segments.
     * The sum of probabilities must be (at least (for computational reasons); exactly equal to (for logical reasons)) 1 for each state
     */
    void initFiniteStateMachine()
    {
        //Memory allocation
        states = new FiniteState[14];

        //The machine has 14 states.
        //For each character in the String representation of a state,
        //  - 0 means grass
        //  - 1 means road
        //  - u means the road becomes grass at the middle
        //  - ^ means the grass becoms road at the middle
        //  - \ means the road turns left (doubled due to character escaping)
        //  - / means the road turns right
        states[0] = new FiniteState(0,"011110");
        states[1] = new FiniteState(1,"0u1110");
        states[2] = new FiniteState(2,"0111u0");
        states[3] = new FiniteState(3,"0^1110");
        states[4] = new FiniteState(4,"00\\\\\\0");
        states[5] = new FiniteState(5,"001110");
        states[6] = new FiniteState(6,"0011u0");
        states[7] = new FiniteState(7,"0111^0");
        states[8] = new FiniteState(8,"0///00");
        states[9] = new FiniteState(9,"011100");
        states[10] = new FiniteState(10,"0u1100");
        states[11] = new FiniteState(11,"0^1100");
        states[12] = new FiniteState(12,"0011^0");
        states[13] = new FiniteState(13,"001100");

        states[0].nextState = new FiniteState[3];
        states[0].nextState[0] = states[0];
        states[0].nextState[1] = states[1];
        states[0].nextState[2] = states[2];
        states[0].dStateProb = new double[3];
        states[0].dStateProb[0] = 0.5;
        states[0].dStateProb[1] = 0.25;
        states[0].dStateProb[2] = 0.25;
        states[0].pathToRoot = states[0];

        states[1].nextState = new FiniteState[4];
        states[1].nextState[0] = states[3];
        states[1].nextState[1] = states[4];
        states[1].nextState[2] = states[5];
        states[1].nextState[3] = states[6];
        states[1].dStateProb = new double[4];
        states[1].dStateProb[0] = 0.1;
        states[1].dStateProb[1] = 0.2;
        states[1].dStateProb[2] = 0.5;
        states[1].dStateProb[3] = 0.2;
        states[1].pathToRoot = states[3];

        states[2].nextState = new FiniteState[4];
        states[2].nextState[0] = states[7];
        states[2].nextState[1] = states[8];
        states[2].nextState[2] = states[9];
        states[2].nextState[3] = states[10];
        states[2].dStateProb = new double[4];
        states[2].dStateProb[0] = 0.1;
        states[2].dStateProb[1] = 0.2;
        states[2].dStateProb[2] = 0.5;
        states[2].dStateProb[3] = 0.2;
        states[2].pathToRoot = states[7];

        states[3].nextState = new FiniteState[3];
        states[3].nextState[0] = states[0];
        states[3].nextState[1] = states[1];
        states[3].nextState[2] = states[2];
        states[3].dStateProb = new double[3];
        states[3].dStateProb[0] = 0.5;
        states[3].dStateProb[1] = 0.2;
        states[3].dStateProb[2] = 0.3;
        states[3].pathToRoot = states[0];

        states[4].nextState = new FiniteState[4];
        states[4].nextState[0] = states[7];
        states[4].nextState[1] = states[8];
        states[4].nextState[2] = states[9];
        states[4].nextState[3] = states[10];
        states[4].dStateProb = new double[4];
        states[4].dStateProb[0] = 0.1;
        states[4].dStateProb[1] = 0.2;
        states[4].dStateProb[2] = 0.5;
        states[4].dStateProb[3] = 0.2;
        states[4].pathToRoot = states[7];

        states[5].nextState = new FiniteState[4];
        states[5].nextState[0] = states[3];
        states[5].nextState[1] = states[4];
        states[5].nextState[2] = states[5];
        states[5].nextState[3] = states[6];
        states[5].dStateProb = new double[4];
        states[5].dStateProb[0] = 0.17;
        states[5].dStateProb[1] = 0.17;
        states[5].dStateProb[2] = 0.5;
        states[5].dStateProb[3] = 0.16;
        states[5].pathToRoot = states[3];

        states[6].nextState = new FiniteState[3];
        states[6].nextState[0] = states[11];
        states[6].nextState[1] = states[12];
        states[6].nextState[2] = states[13];
        states[6].dStateProb = new double[3];
        states[6].dStateProb[0] = 0.3;
        states[6].dStateProb[1] = 0.2;
        states[6].dStateProb[2] = 0.5;
        states[6].pathToRoot = states[11];

        states[7].nextState = new FiniteState[3];
        states[7].nextState[0] = states[0];
        states[7].nextState[1] = states[1];
        states[7].nextState[2] = states[2];
        states[7].dStateProb = new double[3];
        states[7].dStateProb[0] = 0.5;
        states[7].dStateProb[1] = 0.3;
        states[7].dStateProb[2] = 0.2;
        states[7].pathToRoot = states[0];

        states[8].nextState = new FiniteState[4];
        states[8].nextState[0] = states[3];
        states[8].nextState[1] = states[4];
        states[8].nextState[2] = states[5];
        states[8].nextState[3] = states[6];
        states[8].dStateProb = new double[4];
        states[8].dStateProb[0] = 0.2;
        states[8].dStateProb[1] = 0.1;
        states[8].dStateProb[2] = 0.5;
        states[8].dStateProb[3] = 0.2;
        states[8].pathToRoot = states[3];

        states[9].nextState = new FiniteState[4];
        states[9].nextState[0] = states[7];
        states[9].nextState[1] = states[8];
        states[9].nextState[2] = states[9];
        states[9].nextState[3] = states[10];
        states[9].dStateProb = new double[4];
        states[9].dStateProb[0] = 0.17;
        states[9].dStateProb[1] = 0.17;
        states[9].dStateProb[2] = 0.5;
        states[9].dStateProb[3] = 0.16;
        states[9].pathToRoot = states[7];

        states[10].nextState = new FiniteState[3];
        states[10].nextState[0] = states[11];
        states[10].nextState[1] = states[12];
        states[10].nextState[2] = states[13];
        states[10].dStateProb = new double[3];
        states[10].dStateProb[0] = 0.2;
        states[10].dStateProb[1] = 0.3;
        states[10].dStateProb[2] = 0.5;
        states[10].pathToRoot = states[11];

        states[11].nextState = new FiniteState[4];
        states[11].nextState[0] = states[7];
        states[11].nextState[1] = states[8];
        states[11].nextState[2] = states[9];
        states[11].nextState[3] = states[10];
        states[11].dStateProb = new double[4];
        states[11].dStateProb[0] = 0.2;
        states[11].dStateProb[1] = 0.2;
        states[11].dStateProb[2] = 0.5;
        states[11].dStateProb[3] = 0.1;
        states[11].pathToRoot = states[7];

        states[12].nextState = new FiniteState[4];
        states[12].nextState[0] = states[3];
        states[12].nextState[1] = states[4];
        states[12].nextState[2] = states[5];
        states[12].nextState[3] = states[6];
        states[12].dStateProb = new double[4];
        states[12].dStateProb[0] = 0.2;
        states[12].dStateProb[1] = 0.2;
        states[12].dStateProb[2] = 0.5;
        states[12].dStateProb[3] = 0.1;
        states[12].pathToRoot = states[3];

        states[13].nextState = new FiniteState[3];
        states[13].nextState[0] = states[11];
        states[13].nextState[1] = states[12];
        states[13].nextState[2] = states[13];
        states[13].dStateProb = new double[3];
        states[13].dStateProb[0] = 0.25;
        states[13].dStateProb[1] = 0.25;
        states[13].dStateProb[2] = 0.5;
        states[13].pathToRoot = states[11];


    }

    /**
     * Performs some calibration measure such that the game looks smooth
     * @return The delay in ms that we must wait between two loops
     */
    int computeTickValueForCurrentSystem()
    {
        //Minimum and maximum value for tick
        int min = 1;
        int max = 100;

        //Init mean value
        int rc = 50;

        //Loop until convergence
        while(max-min > 1)
        {
            rc = (max+min)/2;

            //System.out.println("Starting calibration");
            long lNanoTime = System.nanoTime();

            //Perform 20 loops
            //Initializes the game status booleans
            //bGameQuit = false;
            gameInProgress = false;

            for(int i = 0; i < 20; i++)
            {
                try
                {
                    //Wait for a tick
                    Thread.sleep(rc);

                    //Count this new tick
                    runTime++;
                    gameRunTime++;

                    //If we must update the game status
                    if(gameRunTime%gameMaxRunTime == 0 && gameInProgress)
                    {

                        //Move the cars according to their speed, acceleration and to the pressed keys (for player car only)
                        moveCars();

                        //Manage the collisions (the finish line is a CollidableRectangle, so it also tells whether the game must end soon)
                        manageCollisions();

                        //Find the rectangles to display according to the car speed and position
                        findDisplayRectangles();

                        //Get position (rank)
                        if(!gameFinishing)
                        {
                            for(int j = 0; j < playersCount; j++) {
                                Player myCar = (Player) cars.elementAt(j);
                                int pos = 1;
                                int ypos = (int) myCar.y;

                                for (Car currentCar : cars) {
                                    if ((currentCar.Racer || currentCar instanceof Player) && currentCar != myCar && currentCar.y < ypos)
                                        pos++;
                                    if (currentCar.bustedTime > 0)
                                        currentCar.bustedTime--;
                                }
                                myCar.setPosition(pos);
                            }
                        }

                    }

                    //The score updates every second if the game is running by adding the square of the current player car speed
                    if(runTime == 20)
                    {
                        runTime = 0;
                        if(gameInProgress)
                            for(int j = 0; j < playersCount; j++){
                                Player myCar = (Player) cars.elementAt(j);
                                myCar.setScore(myCar.getScore() + (int)Math.pow(myCar.ySpeed, 2));
                            }
                    }
                }
                catch(Exception e)
                {
                    //In case of problem, we only display the exception, but we keep going
                    e.printStackTrace();
                }
            }

            long lNanoTime2 = System.nanoTime();
            long diff = lNanoTime2 - lNanoTime;

            //System.out.println("Calibration : " + diff + ", rc = " + rc);

            if(diff > 500000000)
                max = rc;
            else
                min = rc;
        }

        return rc;
    }

    /**
     * "Main" method of the class. It loops until the GUI asked for closure. After each loop, the thread sleeps for 50 ms. In each loop:
     * - The cars are moved
     * - The collisions are processed
     * - The rectangles to display are computed
     * - The player position (rank) is computed
     * - The GUI is asked to perform its update
     */
    public void runGame()
    {
        connectedPlayersCount = playersCount;
        //Initialize the finite state machine
        initFiniteStateMachine();

        //Generates the road, obstacles and cars
        newGrid();

        tickDelay = computeTickValueForCurrentSystem();

        for(int i = 0; i < playersCount; i++)
            ((Player) cars.elementAt(i)).setPlayButtonEnabled(true);


        //Initializes the game status booleans
        //bGameQuit = false;
        gameInProgress = false;


        //While the GUI did not ask for closure
        while(connectedPlayersCount > 0)
        {
            connectedPlayersCount = 0;
            for (int i = 0; i < playersCount; i++)
                if(((Player) cars.elementAt(i)).isConnected())
                    connectedPlayersCount++;
            try
            {
                //Wait for a tick
                Thread.sleep(tickDelay);

                //Count this new tick
                runTime++;
                gameRunTime++;

                //Sets game in progress if all players are ready to start
                if(!gameInProgress) {
                    gameInProgress = true;
                    for (int i = 0; i < playersCount && gameInProgress; i++)
                        gameInProgress = ((Player) cars.elementAt(i)).isReadyToStart() && !((Player) cars.elementAt(i)).isFinishLineCrossed();
                }

                //If we must update the game status
                if(gameRunTime%gameMaxRunTime == 0 && gameInProgress)
                {

                    //Move the cars according to their speed, acceleration and to the pressed keys (for player car only)
                    moveCars();

                    //Manage the collisions (the finish line is a CollidableRectangle, so it also tells whether the game must end soon)
                    manageCollisions();

                    gameFinishing = true;
                    for(int i = 0; i < playersCount && gameFinishing; i++)
                        gameFinishing = ((Player) cars.elementAt(i)).isFinishLineCrossed();

                    //Find the rectangles to display according to the car speed and position
                    findDisplayRectangles();

                    //Get position (rank)
                    if(!gameFinishing)
                    {
                        for(int j = 0; j < playersCount; j++) {
                            Player myCar = (Player) cars.elementAt(j);
                            int pos = 1;
                            int ypos = (int) myCar.y;

                            for (Car currentCar : cars) {
                                if ((currentCar.Racer || currentCar instanceof Player) && currentCar != myCar && currentCar.y < ypos)
                                    pos++;
                                if (currentCar.bustedTime > 0)
                                    currentCar.bustedTime--;
                            }
                            myCar.setPosition(pos);
                        }
                    }

                    for(int i = 0; i < playersCount; i++)
                        ((Player) cars.elementAt(i)).update(nbParticipants);

                }

                //The score updates every second if the game is running by adding the square of the current player car speed
                if(runTime == 20)
                {
                    runTime = 0;
                    if(gameInProgress)
                        for(int j = 0; j < playersCount; j++) {
                            Player myCar = (Player) cars.elementAt(j);
                            myCar.setScore(myCar.getScore() + (int)Math.pow(myCar.ySpeed, 2));
                        }
                }
            }
            catch(Exception e)
            {
                //In case of problem, we only display the exception, but we keep going
                e.printStackTrace();
            }
        }
    }

    /**
     * Manages the collisions
     * @return True if the finish line has just been passed. False otherwise
     */
    public void manageCollisions()
    {
        //For each Car
        for (Car myCar : cars) {
            //In which section am I?
            int iSector = (int) myCar.y / 400;
            if (iSector < 1)
                iSector = 1;
            else if (iSector >= 107)
                iSector = 106;

            //Generate the vector of obstacles which can be hit by the current car
            Vector<CollidableRectangle> vCloseObstacles = new Vector<>(tabObstacles[iSector - 1]);
            vCloseObstacles.addAll(tabObstacles[iSector]);
            vCloseObstacles.addAll(tabObstacles[iSector + 1]);

            //Also add the cars
            vCloseObstacles.addAll(tabObstacles[108]);

            //For each obstacle
            for (CollidableRectangle currentObstacle : vCloseObstacles) {
                //Current obstacle
                //Find the intersection between the current car and the current obstacle
                Rectangle rInter = findIntersection(myCar, currentObstacle);

                //If there is an intersection
                if (rInter != null) {
                    //Switch from relative position to absolute position (on the y-axis)
                    rInter.y += myCar.y;

                    if (currentObstacle.effect == 3 && myCar instanceof Player) {
                        //The player just passed the finish line
                        ((Player)myCar).setFinishLineCrossed(true);
                        ((Player)myCar).setReadyToStart(false);
                    } else if (currentObstacle.effect == 4) {
                        //One car hit the flash zone. The flash is set to be active at 3.6 (thus 180 Km/h)
                        // in such a way that only the player car can be busted (or another car if pushed by the player)
                        if (myCar.ySpeed >= 3.6) {
                            //This car is busted for 5 seconds
                            myCar.bustedTime = 100;
                            myCar.bustedSpeed = (int) (myCar.ySpeed * 50);

                            //We put this car behing the police car and prevent it for moving during the 5 second penalty
                            myCar.y = policePos + 64;
                            myCar.x = 292;
                            myCar.xSpeed = 0;
                            myCar.xAcc = 0;
                            myCar.ySpeed = 0;
                            if (myCar instanceof Player) {
                                ((Player) myCar).setLeftPressed(false);
                                ((Player) myCar).setRightPressed(false);
                            }
                        }
                    } else if (currentObstacle.effect == 2) {
                        //The car is slowed down (by the grass)
                        myCar.ySpeed = myCar.ySpeed * 0.97;
                    } else if (currentObstacle.effect == 1) {
                        //The car hit a movable obstacle (another car)
                        //Check that the two cars are different
                        if (!myCar.equals(currentObstacle)) {
                            //Find the places where the car hit the obstacle
                            boolean[] bHitPlace = findHitPlace(myCar, rInter);

                            //We hit on the front part. We transfer the speed on the y axis to the obstacle and reversely then we move the car a bit backwards
                            if (bHitPlace[1]) {
                                if (myCar.ySpeed > ((Car) currentObstacle).ySpeed) {
                                    double temp = myCar.ySpeed;
                                    myCar.ySpeed = ((Car) currentObstacle).ySpeed;
                                    ((Car) currentObstacle).ySpeed = temp;
                                }
                                myCar.y += rInter.height;
                            }

                            //We hit on the plain left or plain right. We transfer the speed on the x axis to the obstacle and reversely then we move the car a bit to the opposite direction
                            else if (bHitPlace[3] || bHitPlace[5]) {
                                double temp = myCar.xSpeed;
                                myCar.xSpeed = ((Car) currentObstacle).xSpeed;
                                ((Car) currentObstacle).xSpeed = temp;
                                if (bHitPlace[3])
                                    myCar.x += rInter.width;
                                else
                                    myCar.x -= rInter.width;
                            }

                            //We only hit on a top corner. We do both the above
                            else if (bHitPlace[0] || bHitPlace[2]) {
                                if (myCar.ySpeed > ((Car) currentObstacle).ySpeed) {
                                    double temp = myCar.ySpeed;
                                    myCar.ySpeed = ((Car) currentObstacle).ySpeed;
                                    ((Car) currentObstacle).ySpeed = temp;
                                }
                                double temp = myCar.xSpeed;
                                myCar.xSpeed = ((Car) currentObstacle).xSpeed;
                                ((Car) currentObstacle).xSpeed = temp;
                                myCar.y += rInter.height;
                                if (bHitPlace[0])
                                    myCar.x += rInter.width;
                                else
                                    myCar.x -= rInter.width;
                            }
                        }
                    } else if (currentObstacle.effect == 0) {
                        //The car hit an obstacle that cannot move
                        //Find the places where the car hit the obstacle
                        boolean[] bHitPlace = findHitPlace(myCar, rInter);

                        //We hit on the front part. The car will stop immediately.
                        if (bHitPlace[1]) {
                            myCar.ySpeed = 0;
                            myCar.y += rInter.height;
                        }

                        //We hit on the sides
                        else if (bHitPlace[0] || bHitPlace[2] || bHitPlace[3] || bHitPlace[5]) {
                            //If we hit on the top corners, we slow down a bit more than if we hit on the plain sides
                            if (bHitPlace[0] || bHitPlace[2])
                                myCar.ySpeed = myCar.ySpeed * 0.95;
                            else
                                myCar.ySpeed = myCar.ySpeed * 0.97;

                            //We move the car to the opposite direction
                            if (bHitPlace[0] || bHitPlace[3])
                                myCar.x += rInter.width;
                            else
                                myCar.x -= rInter.width;

                        }
                    }
                }
            }
        }
    }

    /**
     * Find the area of the car that is hit by the obstacle
     * @param myCar The car that made the collision
     * @param rInter The intersection of the car and the obstacle
     * @return The array of collided zones:
     * +---+---+---+
     * | 0 | 1 | 2 |
     * +---+---+---+
     * | 3 | 4 | 5 |
     * +---+---+---+
     * | 6 | 7 | 8 |
     * +---+---+---+
     * Note : Only the regions 0, 1, 2, 3 and 5 are computed
     */
    public boolean[] findHitPlace(Rectangle myCar, Rectangle rInter)
    {
        boolean[] rb = new boolean[6];

        rb[0] = findIntersection(new Rectangle(myCar.x,myCar.y,10,21,6),rInter) != null;
        rb[1] = findIntersection(new Rectangle(myCar.x+10,myCar.y,12,21,6),rInter) != null;
        rb[2] = findIntersection(new Rectangle(myCar.x+22,myCar.y,10,21,6),rInter) != null;
        rb[3] = findIntersection(new Rectangle(myCar.x,myCar.y+21,10,22,6),rInter) != null;
        //rb[4] = findIntersection(new Rectangle(myCar.x+10,myCar.y+21,12,22,6),rInter) != null;
        rb[5] = findIntersection(new Rectangle(myCar.x+22,myCar.y+21,10,22,6),rInter) != null;
        //rb[6] = findIntersection(new Rectangle(myCar.x,myCar.y+43,10,21,6),rInter) != null;
        //rb[7] = findIntersection(new Rectangle(myCar.x+10,myCar.y+43,12,21,6),rInter) != null;
        //rb[8] = findIntersection(new Rectangle(myCar.x+22,myCar.y+43,10,21,6),rInter) != null;

        return rb;
    }

    /**
     * Moves the cars according to their speed, acceleration and key pressed for the players cars
     */
    public void moveCars()
    {

        for(int i = 0; i < playersCount; i++){
            Player myCar = (Player)cars.elementAt(i);
            //If we did not pass the finish line, we can still act on the acceleration on the y axis
            if(!gameFinishing && !myCar.isFinishLineCrossed())
            {
                if(myCar.isUpKeyPressed())
                {
                    //Accelerates
                    if(myCar.yAcc < 4)
                        myCar.yAcc++;
                }
                else if(myCar.isDownKeyPressed())
                {
                    //Decelerates
                    if(myCar.yAcc > -8)
                        myCar.yAcc--;
                }
                else
                {
                    //Iteratively reachs a constant deceleration of -1 if no key is pressed
                    if(myCar.yAcc > -1)
                        myCar.yAcc--;
                    else if(myCar.yAcc < -1)
                        myCar.yAcc++;
                }
            }
            else
            {
                //If we passed the finish line, we must decelerate
                myCar.yAcc = -8;
            }

            //Impacts the acceleration of the x axis
            if(myCar.isRightKeyPressed())
            {
                //Going to the right
                if(myCar.xAcc < 4)
                    myCar.xAcc++;
            }
            else if(myCar.isLeftKeyPressed())
            {
                //Going to the left
                if(myCar.xAcc > -4)
                    myCar.xAcc--;
            }
            else
            {
                //If we don't press anything, the x acceleration is calculated to iteratively counter the x speed and make it reach 0
                if(myCar.xSpeed > 1)
                {
                    myCar.xAcc = -(int)(myCar.xSpeed+1);
                    if(myCar.xAcc < -4)
                        myCar.xAcc = -4;
                }
                else if(myCar.xSpeed < -1)
                {
                    myCar.xAcc = -(int)(myCar.xSpeed-1);
                    if(myCar.xAcc > 4)
                        myCar.xAcc = 4;
                }
                else
                {
                    myCar.xAcc = 0;
                    myCar.xSpeed = 0;
                }
            }
        }

        //We then scan the other cars
        for (Car currentCar : cars) {
            //If this is not the player's car
            if (!(currentCar instanceof Player)) {
                //We try to maintain the x speed to 0 using the same formula
                if (currentCar.xSpeed > 1) {
                    currentCar.xAcc = -(int) (currentCar.xSpeed + 1);
                } else if (currentCar.xSpeed < -1) {
                    currentCar.xAcc = -(int) (currentCar.xSpeed - 1);
                } else {
                    currentCar.xAcc = 0;
                    currentCar.xSpeed = 0;
                }

            }

            //The speed on the y axis is updated according to the acceleration
            currentCar.ySpeed += (double) currentCar.yAcc / 100;

            //We try to maintain the car speed in its acceptable range of functionning
            if (currentCar.ySpeed < 0.5) {
                //The car must have at least a speed of 0.5
                if (!gameFinishing) {
                    currentCar.ySpeed = 0.5;
                } else {
                    //Unless the player passes the finish line which, at this point, stops the game
                    if (currentCar.id == 6) {
                        currentCar.ySpeed = 0;
                        gameInProgress = false;
                    } else {
                        currentCar.ySpeed = 0.5;
                    }
                }
            }
            //The opponents will try to stay at 3.58
            else if (currentCar.ySpeed > 3.5 && currentCar.Racer && currentCar.id == 7) {
                currentCar.ySpeed = (currentCar.ySpeed - 3.5) * 0.8 + 3.5;
            }
            //The player car cannot exceed a speed of 4.64
            else if (currentCar.ySpeed > 4.5 && currentCar.Racer && currentCar.id == 6)
                currentCar.ySpeed = (currentCar.ySpeed - 4.5) * 0.8 + 4.5;
                //The civilians will try to stay at 2.04
            else if (currentCar.ySpeed > 2 && !currentCar.Racer)
                currentCar.ySpeed = (currentCar.ySpeed - 2) * 0.8 + 2;

            //The speed on the x axis is updated with the acceleration
            currentCar.xSpeed += (double) currentCar.xAcc / 5;

            //The speed will stay in a certain range [-8,8]
            if (currentCar.xSpeed > 8)
                currentCar.xSpeed = 8;
            if (currentCar.xSpeed < -8)
                currentCar.xSpeed = -8;

            //We update the car position according to its speeds
            currentCar.y -= currentCar.ySpeed * 4;
            currentCar.x += currentCar.xSpeed;

            //But the position of the car must be in the range [0,368]
            if (currentCar.x < 0) {
                currentCar.x = 0;
                currentCar.xAcc = 0;
                currentCar.xSpeed = 0;
            }
            if (currentCar.x > 368) {
                currentCar.x = 368;
                currentCar.xAcc = 0;
                currentCar.xSpeed = 0;
            }

            //If, for some reason, the player car did not decrease enough after the finish line, we ensure that the game will end
            //before we get out of the visible road (+ a safety margin)
            if (gameFinishing && (currentCar instanceof Player) && currentCar.y < 500) {
                gameInProgress = false;
            }

        }
    }

    /**
     * Extracts the visible rectangles to display according to the player's car position and speed
     */
    public void findDisplayRectangles()
    {

        for(int i = 0; i < playersCount; i++) {
            Player currentCar = (Player) cars.elementAt(i);

            int myCarY = (int)currentCar.y;           //Position on the y axis of the car on the road

            //Where is the car w.r.t the display according to its speed?
            int displayCarY = (int)((4.5-currentCar.ySpeed)*50)+100;

            //Where is the display window with respect to the road?
            int displayBoxY = myCarY-displayCarY;

            //We construct the rectangle that represents the current view
            Rectangle currentView = new Rectangle(0,displayBoxY,400,400,9);

            //In which section am I?
            int iSector = displayBoxY/400;
            if(iSector == 0)
                iSector = 1;
            else if(iSector == 107)
                iSector = 106;

            //Generate the vector of road elements which can be seen by the current car
            Vector<Rectangle> vCloseRoad = new Vector<>(tabRoad[iSector-1]);
            vCloseRoad.addAll(tabRoad[iSector]);
            vCloseRoad.addAll(tabRoad[iSector+1]);

            Vector<Rectangle> displayedRoad = new Vector<>();
            //For each road item
            for (Rectangle aVCloseRoad : vCloseRoad) {
                //If this item intersects with the display window
                Rectangle rInter = findIntersection(currentView, aVCloseRoad);
                if (rInter != null) {
                    //We display it
                    displayedRoad.add(rInter);
                }
            }

            currentCar.setDisplayedRoad(displayedRoad);

            Vector<Rectangle> displayedCars = new Vector<>();
            //For each car
            for (Car car : cars) {
                //If this car intersects with the display window
                Rectangle rInter = findIntersection(currentView, car);
                if (rInter != null) {
                    //We display it
                    displayedCars.add(rInter);
                }
            }

            currentCar.setDisplayedCars(displayedCars);

            //We construct four rectangles that are on top of the display window and have a size of 100 pixels each
            Rectangle[] upperView = new Rectangle[4];
            upperView[0] = new Rectangle(0,displayBoxY-400,400,100,9);
            upperView[1] = new Rectangle(0,displayBoxY-300,400,100,9);
            upperView[2] = new Rectangle(0,displayBoxY-200,400,100,9);
            upperView[3] = new Rectangle(0,displayBoxY-100,400,100,9);

            //Which means that the sector is upper
            iSector--;
            if(iSector == 0)
                iSector = 1;

            //Generate the vector of obstacles which can be hit by the current car
            Vector<CollidableRectangle> vCloseObstacles = new Vector<>(tabObstacles[iSector-1]);
            vCloseObstacles.addAll(tabObstacles[iSector]);
            vCloseObstacles.addAll(tabObstacles[iSector+1]);

            //Including the cars
            vCloseObstacles.addAll(tabObstacles[108]);

            Vector<Rectangle> displayedObstacles = new Vector<>();

            //For each obstacle
            for (CollidableRectangle crCurrent : vCloseObstacles) {
                for (int j = 0; j < 4; j++) {
                    //If there is an intersection of one of the rectangles above the display window
                    Rectangle rInter = findIntersection(upperView[j], crCurrent);
                    if (rInter != null && rInter.id >= 5 && rInter.id != 13) {
                        //We display a red rectangle representing the danger to collide with oncoming obstable (except grass, trees and flash zones)
                        //The closer the object is, the wider the red rectangle will be
                        displayedObstacles.add(new Rectangle(rInter.x, 0.0, rInter.width, j + 1, 10));
                    }
                }
            }

            currentCar.setDisplayedObstacles(displayedObstacles);

        }
    }

    /**
     * Finds the intersection, if any, between two rectangles
     * @param reference The reference rectangle (which originates the collision)
     * @param candidate The candidate for collision (passive)
     * @return The intersection between the two rectangles in relative coordinates, or null of the two rectangles don't intersect
     */
    public Rectangle findIntersection(Rectangle reference, Rectangle candidate)
    {
        //Originally, the intersection is null
        Rectangle rr = null;

        //We use java.awt.Rectangle to construct two rectangles of the same dimensions as the parameters
        java.awt.Rectangle r1 = new java.awt.Rectangle((int)reference.x,(int)reference.y,reference.width, reference.height);
        java.awt.Rectangle r2 = new java.awt.Rectangle((int)candidate.x,(int)candidate.y,candidate.width, candidate.height);
        java.awt.Rectangle inter = null;

        //If they intersect, use the built-in method to find their intersection
        if(r1.intersects(r2))
            inter = r1.intersection(r2);

        //If they intersect
        if(inter != null && inter.y-(int)reference.y >= 0)
        {
            //Constructs a new Rectangle with this intersection (the y coordinate is relative the the reference)
            rr = new Rectangle(inter.x, inter.y-(int)reference.y,inter.width, inter.height,candidate.id);
        }

        return rr;
    }

    /**
     * Uses the randomly selected state to generate a new road segment of size 400x400
     * @param vTabRoad The vector of road elements (updated)
     * @param vTabObstacles The vector of obstacles (updated)
     * @param iSegmentId The id of the state that were randomly selected by the finite state machine
     * @param offset The offset to apply to the 400x400 segment on the y axis
     */
    public void generateNextRoadSegment(Vector<Rectangle>[] vTabRoad, Vector<CollidableRectangle>[] vTabObstacles, int iSegmentId, int offset)
    {
        //The machine has 14 states.
        //For each character in the String representation of a state,
        //  - 0 means grass
        //  - 1 means road
        //  - u means the road becomes grass at the middle
        //  - ^ means the grass becomes road at the middle
        //  - \ means the road turns left (doubled due to character escaping)
        //  - / means the road turns right

        //What segment location do I need to generate
        int iSegmentLocation = offset/400;

        //Create the new vectors for this segment
        vTabRoad[iSegmentLocation] = new Vector<>();
        Vector<Rectangle> vRoad = vTabRoad[iSegmentLocation];
        vTabObstacles[iSegmentLocation] = new Vector<>();
        Vector<CollidableRectangle> vObstacles = vTabObstacles[iSegmentLocation];

        //Generate the road segment according to the selected ID
        if(iSegmentId == 0)
        {
            //011110
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche

            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(286,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(114,offset,172,400,1)); //Route noire
            vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 10; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),92,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

            //Le beton
            if(Math.random() >= 0.8)
            {
                crTemp = new CollidableRectangle(114,offset+200,24,32,5,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
            }
        }
        else if(iSegmentId == 1)
        {
            //0u1110
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(110,offset,44,200,0,2));  //Herbe à gauche bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset+200,4,200,3)); //Bord à gauche
            vRoad.add(new Rectangle(154,offset,4,200,3)); //Bord à gauche bis
            vRoad.add(new Rectangle(286,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(114,offset+200,172,200,1)); //Route noire
            vRoad.add(new Rectangle(158,offset,128,200,1)); //Route noire bis
            vRoad.add(new Rectangle(154,offset+200,4,200,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 5; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }
            for(int i = 5; i < 10; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),92,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

            crTemp = new CollidableRectangle(110,offset+200,24,32,5,0);
            vRoad.add(crTemp);
            vObstacles.add(crTemp);
            crTemp = new CollidableRectangle(134,offset+200,24,32,5,0);
            vRoad.add(crTemp);
            vObstacles.add(crTemp);
        }
        else if(iSegmentId == 2)
        {
            //0111u0
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset,44,200,0,2));  //Herbe à droite bis
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(242,offset,4,200,3)); //Bord à droite bis
            vRoad.add(new Rectangle(286,offset+200,4,200,3)); //Bord à droite
            vRoad.add(new Rectangle(114,offset+200,172,200,1)); //Route noire
            vRoad.add(new Rectangle(114,offset,128,200,1)); //Route noire bis
            vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset+200,4,200,2)); //Séparateur témoin 3

            for(int i = 0; i < 5; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }
            for(int i = 5; i < 10; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),92,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

            crTemp = new CollidableRectangle(242,offset+200,24,32,5,0);
            vRoad.add(crTemp);
            vObstacles.add(crTemp);
            crTemp = new CollidableRectangle(266,offset+200,24,32,5,0);
            vRoad.add(crTemp);
            vObstacles.add(crTemp);
        }
        else if(iSegmentId == 3)
        {
            //0^1110
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(110,offset+100,11,300,0,2));  //Herbe à gauche bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(121,offset+150,11,250,0,2));  //Herbe à gauche ter
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(132,offset+200,11,200,0,2));  //Herbe à gauche quad
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(143,offset+250,11,150,0,2));  //Herbe à gauche pent
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset,4,100,3)); //Bord à gauche
            vRoad.add(new Rectangle(121,offset+100,4,50,3)); //Bord à gauche bis
            vRoad.add(new Rectangle(132,offset+150,4,50,3)); //Bord à gauche ter
            vRoad.add(new Rectangle(143,offset+200,4,50,3)); //Bord à gauche quad
            vRoad.add(new Rectangle(154,offset+250,4,150,3)); //Bord à gauche pent
            vRoad.add(new Rectangle(286,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(158,offset,128,400,1)); //Route noire
            vRoad.add(new Rectangle(147,offset,11,250,1)); //Route noire bis
            vRoad.add(new Rectangle(136,offset,11,200,1)); //Route noire ter
            vRoad.add(new Rectangle(125,offset,11,150,1)); //Route noire quad
            vRoad.add(new Rectangle(114,offset,11,100,1)); //Route noire pent
            vRoad.add(new Rectangle(154,offset,4,240,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 6; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),92,12,1)); //Repasser de la route dessus
            }
            for(int i = 6; i < 10; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

        }
        else if(iSegmentId == 4)
        {
            //00\\\0
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(110,offset+100,11,300,0,2));  //Herbe à gauche bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(121,offset+150,11,250,0,2));  //Herbe à gauche ter
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(132,offset+200,11,200,0,2));  //Herbe à gauche quad
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(143,offset+250,11,150,0,2));  //Herbe à gauche pent
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(279,offset,11,250,0,2)); //Herbe à droite bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(268,offset,11,200,0,2)); //Herbe à droite ter
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(257,offset,11,150,0,2)); //Herbe à droite quad
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset,11,100,0,2)); //Herbe à droite pent
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset,4,100,3)); //Bord à gauche
            vRoad.add(new Rectangle(121,offset+100,4,50,3)); //Bord à gauche bis
            vRoad.add(new Rectangle(132,offset+150,4,50,3)); //Bord à gauche ter
            vRoad.add(new Rectangle(143,offset+200,4,50,3)); //Bord à gauche quad
            vRoad.add(new Rectangle(154,offset+250,4,150,3)); //Bord à gauche pent
            vRoad.add(new Rectangle(242,offset,4,100,3)); //Bord à droite
            vRoad.add(new Rectangle(253,offset+100,4,50,3)); //Bord à droite bis
            vRoad.add(new Rectangle(264,offset+150,4,50,3)); //Bord à droite ter
            vRoad.add(new Rectangle(275,offset+200,4,50,3)); //Bord à droite quad
            vRoad.add(new Rectangle(286,offset+250,4,150,3)); //Bord à droite pent
            vRoad.add(new Rectangle(114,offset,128,100,1)); //Route noire
            vRoad.add(new Rectangle(125,offset+100,128,50,1)); //Route noire bis
            vRoad.add(new Rectangle(136,offset+150,128,50,1)); //Route noire ter
            vRoad.add(new Rectangle(147,offset+200,128,50,1)); //Route noire quad
            vRoad.add(new Rectangle(158,offset+250,128,150,1)); //Route noire pent

            for(int i = 0; i < 3; i++)
            {
                vRoad.add(new Rectangle(154,offset+(i*40),4,28,2)); //Séparateur témoin 1
                vRoad.add(new Rectangle(198,offset+(i*40),4,28,2)); //Séparateur témoin 2
            }
            for(int i = 3; i < 7; i++)
            {
                vRoad.add(new Rectangle(154+11*(i-2),offset+(i*40),4,28,2)); //Séparateur témoin 1
                vRoad.add(new Rectangle(198+11*(i-2),offset+(i*40),4,28,2)); //Séparateur témoin 2
            }
            for(int i = 7; i < 10; i++)
            {
                vRoad.add(new Rectangle(198,offset+(i*40),4,28,2)); //Séparateur témoin 1
                vRoad.add(new Rectangle(242,offset+(i*40),4,28,2)); //Séparateur témoin 2
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

        }
        else if(iSegmentId == 5)
        {
            //001110
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,154,400,0,2));  //Herbe à gauche

            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(154,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(286,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(158,offset,128,400,1)); //Route noire
            //vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 10; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

            //Le beton
            if(Math.random() >= 0.8)
            {
                crTemp = new CollidableRectangle(262,offset+200,24,32,5,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
            }
        }
        else if(iSegmentId == 6)
        {
            //0011u0
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,154,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset+200,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset,154,200,0,2)); //Herbe à droite bis
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(154,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(286,offset+200,4,200,3)); //Bord à droite
            vRoad.add(new Rectangle(242,offset,4,200,3)); //Bord à droite bis
            vRoad.add(new Rectangle(158,offset,84,400,1)); //Route noire
            vRoad.add(new Rectangle(246,offset+200,40,400,1)); //Route noire bis
            //vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset+200,4,200,2)); //Séparateur témoin 3

            for(int i = 0; i < 6; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),4,12,1)); //Repasser de la route dessus
            }

            for(int i = 6; i < 10; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

            //Le beton
            crTemp = new CollidableRectangle(242,offset+200,24,32,5,0);
            vRoad.add(crTemp);
            vObstacles.add(crTemp);
            crTemp = new CollidableRectangle(266,offset+200,24,32,5,0);
            vRoad.add(crTemp);
            vObstacles.add(crTemp);
        }
        else if(iSegmentId == 7)
        {
            //0111^0
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(279,offset+100,11,300,0,2));  //Herbe à droite bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(268,offset+150,11,250,0,2));  //Herbe à droite ter
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(257,offset+200,11,200,0,2));  //Herbe à droite quad
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset+250,11,150,0,2));  //Herbe à droite pent
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(275,offset+100,4,50,3)); //Bord à droite bis
            vRoad.add(new Rectangle(264,offset+150,4,50,3)); //Bord à droite ter
            vRoad.add(new Rectangle(253,offset+200,4,50,3)); //Bord à droite quad
            vRoad.add(new Rectangle(242,offset+250,4,150,3)); //Bord à droite pent
            vRoad.add(new Rectangle(286,offset,4,100,3)); //Bord à droite
            vRoad.add(new Rectangle(114,offset,128,400,1)); //Route noire
            vRoad.add(new Rectangle(242,offset,11,250,1)); //Route noire bis
            vRoad.add(new Rectangle(253,offset,11,200,1)); //Route noire ter
            vRoad.add(new Rectangle(264,offset,11,150,1)); //Route noire quad
            vRoad.add(new Rectangle(275,offset,11,100,1)); //Route noire pent
            vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset,4,240,2)); //Séparateur témoin 3

            for(int i = 0; i < 6; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),92,12,1)); //Repasser de la route dessus
            }
            for(int i = 6; i < 10; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

        }
        else if(iSegmentId == 8)
        {
            //0///00
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(279,offset+100,11,300,0,2));  //Herbe à droite bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(268,offset+150,11,250,0,2));  //Herbe à droite ter
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(257,offset+200,11,200,0,2));  //Herbe à droite quad
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset+250,11,150,0,2));  //Herbe à droite pent
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(110,offset,11,250,0,2)); //Herbe à gauche bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(121,offset,11,200,0,2)); //Herbe à gauche ter
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(132,offset,11,150,0,2)); //Herbe à gauche quad
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(143,offset,11,100,0,2)); //Herbe à gauche pent
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(154,offset,4,100,3)); //Bord à gauche
            vRoad.add(new Rectangle(143,offset+100,4,50,3)); //Bord à gauche bis
            vRoad.add(new Rectangle(132,offset+150,4,50,3)); //Bord à gauche ter
            vRoad.add(new Rectangle(121,offset+200,4,50,3)); //Bord à gauche quad
            vRoad.add(new Rectangle(110,offset+250,4,150,3)); //Bord à gauche pent
            vRoad.add(new Rectangle(286,offset,4,100,3)); //Bord à droite
            vRoad.add(new Rectangle(275,offset+100,4,50,3)); //Bord à droite bis
            vRoad.add(new Rectangle(264,offset+150,4,50,3)); //Bord à droite ter
            vRoad.add(new Rectangle(253,offset+200,4,50,3)); //Bord à droite quad
            vRoad.add(new Rectangle(242,offset+250,4,150,3)); //Bord à droite pent
            vRoad.add(new Rectangle(158,offset,128,100,1)); //Route noire
            vRoad.add(new Rectangle(147,offset+100,128,50,1)); //Route noire bis
            vRoad.add(new Rectangle(136,offset+150,128,50,1)); //Route noire ter
            vRoad.add(new Rectangle(125,offset+200,128,50,1)); //Route noire quad
            vRoad.add(new Rectangle(114,offset+250,128,150,1)); //Route noire pent

            for(int i = 0; i < 3; i++)
            {
                vRoad.add(new Rectangle(198,offset+(i*40),4,28,2)); //Séparateur témoin 1
                vRoad.add(new Rectangle(242,offset+(i*40),4,28,2)); //Séparateur témoin 2
            }
            for(int i = 3; i < 7; i++)
            {
                vRoad.add(new Rectangle(198-11*(i-2),offset+(i*40),4,28,2)); //Séparateur témoin 1
                vRoad.add(new Rectangle(242-11*(i-2),offset+(i*40),4,28,2)); //Séparateur témoin 2
            }
            for(int i = 7; i < 10; i++)
            {
                vRoad.add(new Rectangle(154,offset+(i*40),4,28,2)); //Séparateur témoin 1
                vRoad.add(new Rectangle(198,offset+(i*40),4,28,2)); //Séparateur témoin 2
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

        }
        else if(iSegmentId == 9)
        {
            //011100
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche

            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset,154,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(242,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(114,offset,128,400,1)); //Route noire
            vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            //vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 10; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

            //Le beton
            if(Math.random() >= 0.8)
            {
                crTemp = new CollidableRectangle(114,offset+200,24,32,5,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
            }
        }
        else if(iSegmentId == 10)
        {
            //0u1100
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(110,offset,44,200,0,2));  //Herbe à gauche bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset,154,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset+200,4,200,3)); //Bord à gauche
            vRoad.add(new Rectangle(154,offset,4,200,3)); //Bord à gauche bis
            vRoad.add(new Rectangle(242,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(114,offset+200,128,200,1)); //Route noire
            vRoad.add(new Rectangle(158,offset,84,200,1)); //Route noire bis
            vRoad.add(new Rectangle(154,offset+200,4,200,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            //vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 5; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),4,12,1)); //Repasser de la route dessus
            }
            for(int i = 5; i < 10; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

            crTemp = new CollidableRectangle(110,offset+200,24,32,5,0);
            vRoad.add(crTemp);
            vObstacles.add(crTemp);
            crTemp = new CollidableRectangle(134,offset+200,24,32,5,0);
            vRoad.add(crTemp);
            vObstacles.add(crTemp);
        }
        else if(iSegmentId == 11)
        {
            //0^1100
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,110,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(110,offset+100,11,300,0,2));  //Herbe à gauche bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(121,offset+150,11,250,0,2));  //Herbe à gauche ter
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(132,offset+200,11,200,0,2));  //Herbe à gauche quad
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(143,offset+250,11,150,0,2));  //Herbe à gauche pent
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset,154,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(110,offset,4,100,3)); //Bord à gauche
            vRoad.add(new Rectangle(121,offset+100,4,50,3)); //Bord à gauche bis
            vRoad.add(new Rectangle(132,offset+150,4,50,3)); //Bord à gauche ter
            vRoad.add(new Rectangle(143,offset+200,4,50,3)); //Bord à gauche quad
            vRoad.add(new Rectangle(154,offset+250,4,150,3)); //Bord à gauche pent
            vRoad.add(new Rectangle(242,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(158,offset,84,400,1)); //Route noire
            vRoad.add(new Rectangle(147,offset,11,250,1)); //Route noire bis
            vRoad.add(new Rectangle(136,offset,11,200,1)); //Route noire ter
            vRoad.add(new Rectangle(125,offset,11,150,1)); //Route noire quad
            vRoad.add(new Rectangle(114,offset,11,100,1)); //Route noire pent
            vRoad.add(new Rectangle(154,offset,4,240,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            // vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 6; i++)
            {
                vRoad.add(new Rectangle(154,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }
            for(int i = 6; i < 10; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),4,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

        }
        else if(iSegmentId == 12)
        {
            //0011^0
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,154,400,0,2));  //Herbe à gauche
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(279,offset+100,11,300,0,2));  //Herbe à droite bis
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(268,offset+150,11,250,0,2));  //Herbe à droite ter
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(257,offset+200,11,200,0,2));  //Herbe à droite quad
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset+250,11,150,0,2));  //Herbe à droite pent
            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(290,offset,110,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(154,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(275,offset+100,4,50,3)); //Bord à droite bis
            vRoad.add(new Rectangle(264,offset+150,4,50,3)); //Bord à droite ter
            vRoad.add(new Rectangle(253,offset+200,4,50,3)); //Bord à droite quad
            vRoad.add(new Rectangle(242,offset+250,4,150,3)); //Bord à droite pent
            vRoad.add(new Rectangle(286,offset,4,100,3)); //Bord à droite
            vRoad.add(new Rectangle(158,offset,84,400,1)); //Route noire
            vRoad.add(new Rectangle(242,offset,11,250,1)); //Route noire bis
            vRoad.add(new Rectangle(253,offset,11,200,1)); //Route noire ter
            vRoad.add(new Rectangle(264,offset,11,150,1)); //Route noire quad
            vRoad.add(new Rectangle(275,offset,11,100,1)); //Route noire pent
            //vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            vRoad.add(new Rectangle(242,offset,4,240,2)); //Séparateur témoin 3

            for(int i = 0; i < 6; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),48,12,1)); //Repasser de la route dessus
            }
            for(int i = 6; i < 10; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),4,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

        }
        else if(iSegmentId == 13)
        {
            //001100
            CollidableRectangle crTemp;
            vRoad.add(crTemp = new CollidableRectangle(0,offset,154,400,0,2));  //Herbe à gauche

            vObstacles.add(crTemp);
            vRoad.add(crTemp = new CollidableRectangle(246,offset,154,400,0,2)); //Herbe à droite
            vObstacles.add(crTemp);
            vRoad.add(new Rectangle(154,offset,4,400,3)); //Bord à gauche
            vRoad.add(new Rectangle(242,offset,4,400,3)); //Bord à droite
            vRoad.add(new Rectangle(158,offset,84,400,1)); //Route noire
            //vRoad.add(new Rectangle(154,offset,4,400,2)); //Séparateur témoin 1
            vRoad.add(new Rectangle(198,offset,4,400,2)); //Séparateur témoin 2
            //vRoad.add(new Rectangle(242,offset,4,400,2)); //Séparateur témoin 3

            for(int i = 0; i < 10; i++)
            {
                vRoad.add(new Rectangle(198,offset+28+(i*40),4,12,1)); //Repasser de la route dessus
            }


            //Les arbres
            for(int i = 0; i < 2; i++)
            {
                crTemp = new CollidableRectangle(25,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);
                crTemp = new CollidableRectangle(325,offset+100+(i*200),59,64,4,0);
                vRoad.add(crTemp);
                vObstacles.add(crTemp);

            }

        }
        else
        {
            //Les autres, juste pour débugger à son aise
            System.out.println("Noting planned for ID : " + iSegmentId);
            System.exit(1);
        }
    }

    /**
     * Initializes the game state (generates the road, the obstacles, the cars)
     */
    public void newGrid()
    {
        //init tick counter
        runTime = 0;
        gameRunTime = 0;
        gameMaxRunTime = 1;

        //Initializes finite state machine
        initFiniteStateMachine();
        FiniteState currentState = states[0];

        //Re-initializes the vector of road elements and obstacles
        tabRoad = new Vector[108];
        tabObstacles = new Vector[109];

        //Generates 100 segments of 400x400 using the finite state machine
        for(int i = 0; i < 100; i++)
        {
            generateNextRoadSegment(tabRoad, tabObstacles, currentState.iId, (99-i)*400+3200);
            currentState = currentState.nextState();
        }

        //The last 8 400x400 segments are generated by taking the shortest distance to the root
        for(int i = 0; i < 8; i++)
        {
            generateNextRoadSegment(tabRoad, tabObstacles, currentState.iId, 3200-(i+1)*400);
            currentState = currentState.pathToRoot;
        }

        //The finish line
        CollidableRectangle crTemp = null;
        tabRoad[3].add(crTemp = new CollidableRectangle(0, 1200, 400, 20, 2, 3));
        tabObstacles[3].add(crTemp);

        //The cars
        //Starting with the player's car
        cars.removeIf(c -> !(c instanceof Player));
        tabObstacles[108] = new Vector<>();
        for(int i = 0; i < playersCount; i++)
            tabObstacles[108].add(cars.elementAt(i));
        //cars.add((Car) crTemp);

        //Civilians
        for(int i = 0; i < 30; i++)
        {
            crTemp = new Car(204,42270-i*1000,32,64,8,1,0,0,0,1,false);
            tabObstacles[108].add(crTemp);
            cars.add((Car)crTemp);
        }

        //Competitors
        for(int i = 0; i < 10-playersCount; i++)
        {
            crTemp = new Car(160,42200-i*900,32,64,7,1,0,0,0,2,true);
            tabObstacles[108].add(crTemp);
            cars.add((Car)crTemp);
        }

        //Count the number of participants (although, we should know it, but still)
        Iterator<Car> iCars = cars.iterator();
        iCars.next(); //Skip first car
        nbParticipants = 1;
        while(iCars.hasNext())
        {
            Car currentCar = iCars.next();
            if(currentCar.Racer)
            {
                nbParticipants++;

            }
        }

        //Cops and speed indicator
        if(Math.random() >= 0.5)
        {
            //Cops will be placed before the speed indicator
            int pos = (int)(Math.random()*10000)+10000;
            tabRoad[pos/400].add(crTemp = new CollidableRectangle(292,pos,30,64,11,0)); //Panneau 130
            tabObstacles[pos/400].add(crTemp);
            tabRoad[(pos-1000)/400].add(crTemp = new CollidableRectangle(292, pos - 1000, 30, 64, 14, 0)); //Speedometer
            tabObstacles[(pos-1000)/400].add(crTemp);

            pos = (int)(Math.random()*10000)+30000;
            tabRoad[pos/400].add(crTemp = new CollidableRectangle(292, pos, 30, 64, 11, 0)); //Panneau 130
            tabObstacles[pos/400].add(crTemp);
            tabRoad[(pos-1000)/400].add(crTemp = new CollidableRectangle(292, pos - 1000, 32, 64, 12, 0)); //Voiture de flics
            tabObstacles[(pos-1000)/400].add(crTemp);
            policePos = pos-1000;
            tabObstacles[(pos-1200)/400].add(new CollidableRectangle(0, pos - 1200, 400, 200, 13, 4)); //Zone Flash

        }
        else
        {
            //Speed indicator will be placed before the cops
            int pos = (int)(Math.random()*10000)+10000;
            tabRoad[pos/400].add(crTemp = new CollidableRectangle(292,pos,30,64,11,0)); //Panneau 130
            tabObstacles[pos/400].add(crTemp);
            tabRoad[(pos-1000)/400].add(crTemp = new CollidableRectangle(292, pos - 1000, 32, 64, 12, 0)); //Voiture de flics
            tabObstacles[(pos-1000)/400].add(crTemp);
            policePos = pos-1000;
            tabObstacles[(pos-1200)/400].add(new CollidableRectangle(0, pos - 1200, 400, 200, 13, 4)); //Zone Flash


            pos = (int)(Math.random()*10000)+30000;
            tabRoad[pos/400].add(crTemp = new CollidableRectangle(292,pos,30,64,11,0)); //Panneau 130
            tabObstacles[pos/400].add(crTemp);
            tabRoad[(pos-1000)/400].add(crTemp = new CollidableRectangle(292, pos - 1000, 30, 64, 14, 0)); //Speedometer
            tabObstacles[(pos-1000)/400].add(crTemp);
        }

    }

    @Override
    public void run(){
        System.out.println("Starting core");
        this.runGame();
        System.out.println("Ending core");
    }

    public void addPlayer(Player p){
        this.cars.add(0,p);
        playersCount++;

        for(int i = 0; i < playersCount; i++){
            ((Player) cars.elementAt(i)).x = 118 + i*42;
        }
    }

    public void removePlayer(Player p){
        this.cars.remove(p);
        playersCount--;
    }

    public int getPlayersCount(){
        return playersCount;
    }
}

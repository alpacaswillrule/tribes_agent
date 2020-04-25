package core.game;

import core.TribesConfig;
import core.Types;
import core.actions.Action;
import core.actions.tribeactions.EndTurn;
import core.actions.unitactions.Recover;
import core.actions.unitactions.factory.RecoverFactory;
import core.actors.Building;
import core.actors.City;
import core.actors.Temple;
import core.actors.Tribe;
import core.actors.units.Battleship;
import core.actors.units.Boat;
import core.actors.units.Ship;
import core.actors.units.Unit;
import org.json.JSONArray;
import org.json.JSONObject;
import players.Agent;
import players.HumanAgent;
import utils.ElapsedCpuTimer;
import utils.GUI;
import utils.Vector2d;
import utils.WindowInput;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import static core.Constants.*;

public class Game {

    private boolean FORCE_FULL_OBSERVABILITY = true;

    // State of the game (objects, ticks, etc).
    private GameState gs;

    // GameState objects for players to make decisions
    private GameState[] gameStateObservations;

    // Seed for the game state.
    private long seed;

    //Random number generator for the game.
    private Random rnd;

    // List of players of the game
    private Agent[] players;

    //Number of players of the game.
    private int numPlayers;

    /**
     * Constructor of the game
     */
    public Game()
    {}

    /**
     * Initializes the game. This method does the following:
     *   Sets the players of the game, the number of players and their IDs
     *   Initializes the array to hold the player game states.
     *   Assigns the tribes that will play the game.
     *   Creates the board according to the above information and resets the game so it's ready to start.
     *   Turn order: by default, turns run following the order in the tribes array.
     * @param players Players of the game.
     * @param tribes Tribes to play the game with. Players and tribes related by position in array lists.
     * @param filename Name of the file with the level information.
     * @param seed Seed for the game (used only for board generation)
     * @param gameMode Game Mode for this game.
     */
    public void init(ArrayList<Agent> players, ArrayList<Tribe> tribes, String filename, long seed, Types.GAME_MODE gameMode) {

        //Initiate the bare bones of the main game classes
        this.seed = seed;
        this.rnd = new Random(seed);
        this.gs = new GameState(rnd, gameMode);

        if(players.size() != tribes.size())
        {
            System.out.println("ERROR: Number of tribes must equal the number of players.");
        }

        Tribe[] tribesArray = new Tribe[tribes.size()];
        for (int i = 0; i < tribesArray.length; ++i)
        {
            tribesArray[i] = tribes.get(i);
        }

        //Create the players and agents to control them
        numPlayers = players.size();
        this.players = new Agent[numPlayers];
        for(int i = 0; i < numPlayers; ++i)
        {
            this.players[i] = players.get(i);
            this.players[i].setPlayerID(i);
        }
        this.gameStateObservations = new GameState[numPlayers];

        //Assign the tribes to the players
//        this.gs.assignTribes(tribes);

        this.gs.init(filename, tribesArray);

        updateAssignedGameStates();
    }


//    /**
//     * Resets the game, providing a seed.
//     * @param repeatLevel true if the same level should be played.
//     * @param filename Name of the file with the level information.
//     */
//    public void reset(boolean repeatLevel, String filename)
//    {
//        this.seed = repeatLevel ? seed : System.currentTimeMillis();
//        resetGame(filename, numPlayers);
//    }
//
//    /**
//     * Resets the game, providing a seed.
//     * @param seed new seed for the game.
//     * @param filename Name of the file with the level information.
//     */
//    public void reset(int seed, String filename)
//    {
//        this.seed = seed;
//        resetGame(filename, numPlayers);
//    }

//    /**
//     * Resets the game, creating the original game state (and level) and assigning the initial
//     * game state views that each player will have.
//     * @param filename Name of the file with the level information.
//     */
//    private void resetGame(String filename)
//    {
//        this.gs.init(filename);
//        updateAssignedGameStates();
//    }




    /**
     * Runs a game once. Receives frame and window input. If any is null, forces a run with no visuals.
     * @param frame window to draw the game
     * @param wi input for the window.
     */
    public void run(GUI frame, WindowInput wi)
    {
        if (frame == null || wi == null)
            VISUALS = false;
        boolean firstEnd = true;

        while(frame == null || !frame.isClosed()) {
            // Loop while window is still open, even if the game ended.
            // If not playing with visuals, loop is broken when game's ended.
            tick(frame);

            // Check end of game
            if (firstEnd && gameOver()) {
                terminate();
                firstEnd = false;

                if (!VISUALS || frame == null) {
                    // The game has ended, end the loop if we're running without visuals.
                    break;
                } else {
                    frame.update(getGameState(-1)); // One last update with full observation
                }
            }
        }
    }

    /**
     * Ticks the game forward. Asks agents for actions and applies returned actions to obtain the next game state.
     */
    private void tick (GUI frame) {
        if (VERBOSE) {
            //System.out.println("tick: " + gs.getTick());
        }

        Tribe[] tribes = gs.getTribes();
        for (int i = 0; i < numPlayers; i++) {
            Tribe tribe = tribes[i];

            if(tribe.getWinner() == Types.RESULT.LOSS)
                continue; //We don't do anything for tribes that have already lost.


            //play the full turn for this player
            processTurn(i, tribe, frame);

            // Save Game
            saveGame();


            GameLoader gl = new GameLoader("save/" + this.seed + "/"+ gs.getTick() + "_" + gs.getActiveTribeID() +"/game.json");

            //it may be that this player won the game, no more playing.
            if(gameOver())
            {
                return;
            }
        }

        //All turns passed, time to increase the tick.
        gs.incTick();
    }

    public void saveGame(){
        try{
            File rootFileLoc = new File("save/" + this.seed);
            File turnFile = new File(rootFileLoc, gs.getTick() + "_" + gs.getBoard().getActiveTribeID());

            // Only create root file for first time
            if(gs.getTick() == 0 && gs.getActiveTribeID() == 0){
                //Create dictionary
                rootFileLoc.mkdirs();
            }


            turnFile.mkdir();

            // JSON
            JSONObject game = new JSONObject();

            // Board INFO (2D array) - Terrain, Resource, UnitID, CityID, NetWorks
            JSONObject board = new JSONObject();
            JSONArray terrain2D = new JSONArray();
            JSONArray terrain;

            JSONArray resource2D = new JSONArray();
            JSONArray resource;

            JSONArray unit2D = new JSONArray();
            JSONArray units;

            JSONArray city2D = new JSONArray();
            JSONArray cities;

            JSONArray building2D = new JSONArray();
            JSONArray JBuildings;

            JSONArray network2D = new JSONArray();
            JSONArray networks;

            // Unit INFO: id:{type, x, y, kills, isVeteran, cityId, tribeId, HP}
            JSONObject unit = new JSONObject();

            // City INFO: id:{x, y, tribeId, population_need, bound, level, isCapital, population,
            //                production, hasWalls, pointsWorth, building(array)}
            JSONObject city = new JSONObject();

            // Building INFO: {x, y, type, level(optional), turnsToScore(optional), bonus}
            JSONObject building = new JSONObject();

            for (int i=0; i<getBoard().getSize(); i++){

                // Initial JSON Object for each row
                terrain = new JSONArray();
                resource = new JSONArray();
                units = new JSONArray();
                cities = new JSONArray();
                networks = new JSONArray();
                JBuildings = new JSONArray();

                for(int j=0; j<getBoard().getSize(); j++){
                    // Save Terrain INFO
                    terrain.put(gs.getBoard().getTerrainAt(i, j).getKey());
                    // Save Resource INFO
                    resource.put(gs.getBoard().getResourceAt(i, j) != null? gs.getBoard().getResourceAt(i, j).getKey():-1);

                    // Save unit INFO
                    int unitINFO = gs.getBoard().getUnitIDAt(i, j);
                    units.put(unitINFO);
                    if (unitINFO != 0){
                        Unit u = (Unit)gs.getActor(unitINFO);
                        JSONObject uInfo = new JSONObject();
                        uInfo.put("type", u.getType().getKey());
                        if (u.getType() == Types.UNIT.BOAT){
                            uInfo.put("baseLandType", ((Boat)u).getBaseLandUnit().getKey());
                        }else if (u.getType() == Types.UNIT.SHIP){
                            uInfo.put("baseLandType", ((Ship)u).getBaseLandUnit().getKey());
                        }else if (u.getType() == Types.UNIT.BATTLESHIP){
                            uInfo.put("baseLandType", ((Battleship)u).getBaseLandUnit().getKey());
                        }
                        uInfo.put("x", i);
                        uInfo.put("y", j);
                        uInfo.put("kill", u.getKills());
                        uInfo.put("isVeteran", u.isVeteran());
                        uInfo.put("cityID", u.getCityId());
                        uInfo.put("tribeId", u.getTribeId());
                        uInfo.put("currentHP", u.getCurrentHP());
                        unit.put(String.valueOf(unitINFO), uInfo);
                    }
                    // Save city INFO
                    int cityINFO = gs.getBoard().getCityIdAt(i, j);
                    cities.put(cityINFO);
                    if (cityINFO != -1){
                        City c = (City)gs.getActor(cityINFO);
                        // City INFO: id:{x, y, tribeId, population_need, bound, level, isCapital, population,
                        //                production, hasWalls, pointsWorth, building(array)}
                        JSONObject cInfo = new JSONObject();
                        cInfo.put("x", i);
                        cInfo.put("y", j);
                        cInfo.put("tribeID", c.getTribeId());
                        cInfo.put("population_need", c.getPopulation_need());
                        cInfo.put("bound", c.getBound());
                        cInfo.put("level", c.getLevel());
                        cInfo.put("isCapital", c.isCapital());
                        cInfo.put("population", c.getPopulation());
                        cInfo.put("production", c.getProduction());
                        cInfo.put("hasWalls", c.hasWalls());
                        cInfo.put("pointsWorth", c.getPointsWorth());
                        // Save Buildings INFO
                        JSONArray buildingList = new JSONArray();
                        LinkedList<Building> buildings = c.getBuildings();
                        if (buildings != null) {
                            for (Building b : buildings) {
                                JSONObject bInfo = new JSONObject();
                                bInfo.put("x", b.position.x);
                                bInfo.put("y", b.position.y);
                                bInfo.put("type", b.type.getKey());
                                if (b.type.isTemple()) {
                                    Temple t = (Temple) b;
                                    bInfo.put("level", t.getLevel());
                                    bInfo.put("turnsToScore", t.getTurnsToScore());
                                }
                                buildingList.put(bInfo);
                            }
                        }
                        cInfo.put("buildings", buildingList);
                        cInfo.put("units", c.getUnitsID());
                        city.put(String.valueOf(cityINFO), cInfo);
                    }

                    // Save Building INFO
                    JBuildings.put(gs.getBoard().getBuildingAt(i, j)!= null? gs.getBoard().getBuildingAt(i, j).getKey():-1);

                    // Save network INFO
                    networks.put(gs.getBoard().getNetworkTilesAt(i, j));

                }
                // Update row value
                terrain2D.put(terrain);
                resource2D.put(resource);
                unit2D.put(units);
                city2D.put(cities);
                network2D.put(networks);
                building2D.put(JBuildings);
            }

            board.put("terrain", terrain2D);
            board.put("resource", resource2D);
            board.put("unitID", unit2D);
            board.put("cityID", city2D);
            board.put("network", network2D);
            board.put("building", building2D);

            game.put("board", board);
            game.put("unit", unit);
            game.put("city", city);

            // Save Tribes Information () - id: {citiesID, capitalID, type, techTree, stars, winner, score, obsGrid,
            //                                   connectedCities, monuments:{type: status}, tribesMet, extraUnits,
            //                                   nKills, nPacifistCount}
            JSONObject tribesINFO = new JSONObject();
            Tribe[] tribes = gs.getTribes();
            for(Tribe t: tribes){
                JSONObject tribeInfo = new JSONObject();
                tribeInfo.put("citiesID", t.getCitiesID());
                tribeInfo.put("capitalID", t.getCapitalID());
                tribeInfo.put("type", t.getType().getKey());
                JSONObject techINFO = new JSONObject();
                techINFO.put("researched", t.getTechTree().getResearched());
                techINFO.put("everythingResearched", t.getTechTree().isEverythingResearched());
                tribeInfo.put("technology", techINFO);
                tribeInfo.put("star", t.getStars());
                tribeInfo.put("winner", t.getWinner().getKey());
                tribeInfo.put("score", t.getScore());
                tribeInfo.put("obsGrid", t.getObsGrid());
                tribeInfo.put("connectedCities", t.getConnectedCities());
                HashMap<Types.BUILDING, Types.BUILDING.MONUMENT_STATUS> m = t.getMonuments();
                JSONObject monumentInfo = new JSONObject();
                for (Types.BUILDING key : m.keySet()) {
                    monumentInfo.put(String.valueOf(key.getKey()), m.get(key).getKey());
                }
                tribeInfo.put("monuments", monumentInfo);
                JSONArray tribesMetInfo = new JSONArray();
                ArrayList<Integer> tribesMet= t.getTribesMet();
                for (Integer tribeId : tribesMet){
                    tribesMetInfo.put(tribeId);
                }
                tribeInfo.put("tribesMet", tribesMetInfo);
                tribeInfo.put("extraUnits", t.getExtraUnits());
                tribeInfo.put("nKills", t.getnKills());
                tribeInfo.put("nPacifistCount", t.getnPacifistCount());
                tribesINFO.put(String.valueOf(t.getActorId()), tribeInfo);
            }

            game.put("tribes", tribesINFO);
            game.put("seed", seed);
            game.put("tick", gs.getTick());
            game.put("activeTribeID", gs.getActiveTribeID());

            FileWriter fw_game = new FileWriter(turnFile.getPath() + "/game.json");
            fw_game.write(game.toString(4));
            fw_game.close();

        } catch (IOException e){
            e.printStackTrace();
        }
    }



    /**
     * Process a turn for a given player. It queries the player for an action until no more
     * actions are available or the player returns a EndTurnAction action.
     * @param playerID ID of the player whose turn is being processed.
     * @param tribe tribe that corresponds to this player.
     */
    private void processTurn(int playerID, Tribe tribe, GUI frame)
    {
        //Init the turn for this tribe (stars, unit reset, etc).
        gs.initTurn(tribe);

        //Compute the initial player actions and assign the game states.
        gs.computePlayerActions(tribe);
        updateAssignedGameStates();

        //Take the player for this turn
        Agent ag = players[playerID];

        //start the timer to the max duration
        ElapsedCpuTimer ect = new ElapsedCpuTimer();
        ect.setMaxTimeMillis(TURN_TIME_MILLIS);

        // Keep track of time remaining for turn thinking
        long remainingECT = TURN_TIME_MILLIS;

        boolean continueTurn = true;
        int curActionCounter = 0;

        // Timer for action execution, delay introduced from GUI. Another delay is added at the end of the turn to
        // make sure all updates are executed and displayed to humans.
        ElapsedCpuTimer actionDelayTimer = null;
        ElapsedCpuTimer endTurnDelay = null;
        if (VISUALS && frame != null) {
            actionDelayTimer = new ElapsedCpuTimer();
            actionDelayTimer.setMaxTimeMillis(FRAME_DELAY);
        }

        while (true) {
            // Keep track of action played in this loop, null if no action.
            Action action = null;

            // Check GUI end of turn timer
            if (endTurnDelay != null && endTurnDelay.remainingTimeMillis() <= 0) break;

            // Action request and execution if turn should be continued
            if (continueTurn) {
                //noinspection ConstantConditions
                if ((!VISUALS || frame == null) || actionDelayTimer.remainingTimeMillis() <= 0) {
                    // Get one action from the player
                    ect.setMaxTimeMillis(remainingECT);  // Reset timer ignoring all other timers or updates
                    action = ag.act(gameStateObservations[playerID], ect);
                    remainingECT = ect.remainingTimeMillis(); // Note down the remaining time to use it for the next iteration

//            System.out.println(gs.getTick() + " " + curActionCounter + " " + action + "; stars: " + gs.getBoard().getTribe(playerID).getStars());
                    curActionCounter++;

                    // Play the action in the game and update the available actions list and observations
                    gs.next(action);
                    gs.computePlayerActions(tribe);
                    updateAssignedGameStates();

                    if (actionDelayTimer != null) {  // Reset action delay timer for next action request
                        actionDelayTimer = new ElapsedCpuTimer();
                        actionDelayTimer.setMaxTimeMillis(FRAME_DELAY);
                    }

                    // Continue this turn if there are still available actions and end turn was not requested.
                    // If the agent is human, let him play for now.
                    continueTurn = !gs.isTurnEnding();
                    if (!(ag instanceof HumanAgent)) {
                        ect.setMaxTimeMillis(remainingECT);
                        continueTurn &= gs.existAvailableActions(tribe) && !ect.exceededMaxTime();
                    }
                }
            } else if (endTurnDelay == null) {
                // If turn should be ending (and we've not already triggered end turn), the action is automatically EndTurn
                action = new EndTurn(gs.getActiveTribeID());
            }

            // Update GUI after every iteration
            if (VISUALS && frame != null) {
                if (FORCE_FULL_OBSERVABILITY) frame.update(getGameState(-1));  // Full Obs
                else frame.update(gameStateObservations[gs.getActiveTribeID()]);        // Partial Obs

                // Turn should be ending, start timer for delay of next action and show all updates
                if (action instanceof EndTurn) {
                    endTurnDelay = new ElapsedCpuTimer();
                    endTurnDelay.setMaxTimeMillis(FRAME_DELAY);
                }
            } else if (action instanceof EndTurn) { // If no visuals and we should end the turn, just break out of loop here
                break;
            }
        }

        // Ends the turn for this tribe (units that didn't move heal).
        gs.endTurn(tribe);
    }




    /**
     * This method call all agents' end-of-game method for post-processing.
     * Agents receive their final game state and reward
     */
    @SuppressWarnings("UnusedReturnValue")
    private void terminate() {

        Tribe[] tribes = gs.getTribes();
        for (int i = 0; i < numPlayers; i++) {
            Agent ag = players[i];
            ag.result(gs.copy(), tribes[i].getScore());
        }
    }

    /**
     * Returns the winning status of all players.
     * @return the winning status of all players.
     */
    public Types.RESULT[] getWinnerStatus()
    {
        //Build the results array
        Tribe[] tribes = gs.getTribes();
        Types.RESULT[] results = new Types.RESULT[numPlayers];
        for (int i = 0; i < numPlayers; i++) {
            Tribe tribe = tribes[i];
            results[i] = tribe.getWinner();
        }
        return results;
    }

    /**
     * Returns the current scores of all players.
     * @return the current scores of all players.
     */
    public int[] getScores()
    {
        //Build the results array
        Tribe[] tribes = gs.getTribes();
        int[] scores = new int[numPlayers];
        for (int i = 0; i < numPlayers; i++) {
            scores[i] = tribes[i].getScore();
        }
        return scores;
    }

    /**
     * Updates the state observations for all players with copies of the
     * current game state, adapted for PO.
     */
    private void updateAssignedGameStates() {

        //TODO: Probably we don't need to do this for all players, just the active one.
        for (int i = 0; i < numPlayers; i++) {
            gameStateObservations[i] = getGameState(i);
        }
    }

    /**
     * Returns the game state as seen for the player with the index playerIdx. This game state
     * includes only the observations that are visible if partial observability is enabled.
     * @param playerIdx index of the player for which the game state is generated.
     * @return the game state.
     */
    private GameState getGameState(int playerIdx) {
        return gs.copy(playerIdx);
    }

    /**
     * Returns the game board.
     * @return the game board.
     */
    public Board getBoard()
    {
        return gs.getBoard();
    }

    public Agent[] getPlayers() {
        return players;
    }

    /**
     * Method to identify the end of the game. If the game is over, the winner is decided.
     * The winner of a game is determined by TribesConfig.GAME_MODE and TribesConfig.MAX_TURNS
     * @return true if the game has ended, false otherwise.
     */
    boolean gameOver() {
        return gs.gameOver();
    }

}

package utils;

import core.Types;
import core.actions.cityactions.*;
import core.actors.City;
import core.actors.units.Unit;
import core.game.Board;
import core.game.GameState;
import core.actions.Action;
import players.ActionController;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import static utils.GameView.gridSize;

@SuppressWarnings({"SuspiciousNameCombination", "StringConcatenationInsideStringBufferAppend"})
public class InfoView extends JComponent {

    // Dimensions of the window.
    private Dimension size;
    private JEditorPane textArea;

    int actionPanelHeight = 100;
    int sidePanelWidth = 400;
    int sidePanelHeight = 400;
    private JButton actionBF, actionCF, actionD, actionGF, actionRG;
    private JButton[] actionB, actionS;
    private CityActionListener listenerBF, listenerCF, listenerD, listenerGF, listenerRG;
    private CityActionListener[] listenerB, listenerS;
    private ActionController ac;

    private int highlightX, highlightY;
    private int highlightXprev, highlightYprev;

    private GameState gs;

    InfoView(ActionController ac)
    {
        this.size = new Dimension(sidePanelWidth, sidePanelHeight);
        this.ac = ac;

        highlightX = -1;
        highlightY = -1;
        highlightXprev = -1;
        highlightYprev = -1;

        textArea = new JEditorPane("text/html", "");
        textArea.setPreferredSize(new Dimension(sidePanelWidth, sidePanelHeight - actionPanelHeight));
        Font textFont = new Font(textArea.getFont().getName(), Font.PLAIN, 12);
        textArea.setFont(textFont);
        textArea.setEditable(false);
        textArea.setBackground(Color.lightGray);
        DefaultCaret caret = (DefaultCaret)textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        JPanel actionPanel = new JPanel();
        actionPanel.setPreferredSize(new Dimension(sidePanelWidth, 220));

        // Simple actions: BurnForest, ClearForest, Destroy, GrowForest, GatherResource
        actionBF = new JButton("Burn");  // If forest
        listenerBF = new CityActionListener();
        actionBF.addActionListener(listenerBF);
        actionBF.setVisible(false);
        actionCF = new JButton("Clear");  // If forest
        listenerCF = new CityActionListener();
        actionCF.addActionListener(listenerCF);
        actionCF.setVisible(false);
        actionD = new JButton("Destroy");  // If building
        listenerD = new CityActionListener();
        actionD.addActionListener(listenerD);
        actionD.setVisible(false);
        actionGF = new JButton("Grow");  // If plain
        listenerGF = new CityActionListener();
        actionGF.addActionListener(listenerGF);
        actionGF.setVisible(false);
        actionRG = new JButton("Gather");  // If resource
        listenerRG = new CityActionListener();
        actionRG.addActionListener(listenerRG);
        actionRG.setVisible(false);
        actionPanel.add(actionRG);
        actionPanel.add(actionBF);
        actionPanel.add(actionCF);
        actionPanel.add(actionD);
        actionPanel.add(actionGF);

        // Complex actions: Build X, Spawn X
        int nBuildings = Types.BUILDING.values().length;
        actionB = new JButton[nBuildings];
        listenerB = new CityActionListener[nBuildings];
        for (int i = 0; i < nBuildings; i++) {
            actionB[i] = new JButton("Build " + Types.BUILDING.values()[i]);
            listenerB[i] = new CityActionListener();
            actionB[i].addActionListener(listenerB[i]);
            actionB[i].setVisible(false);
            actionPanel.add(actionB[i]);
        }
        int nUnits = Types.UNIT.values().length;
        actionS = new JButton[nUnits];
        listenerS = new CityActionListener[nUnits];
        for (int i = 0; i < nUnits; i++) {
            actionS[i] = new JButton("Spawn " + Types.UNIT.values()[i]);
            listenerS[i] = new CityActionListener();
            actionS[i].addActionListener(listenerS[i]);
            actionS[i].setVisible(false);
            actionPanel.add(actionS[i]);
        }

        this.setLayout(new FlowLayout());
        JScrollPane scrollPane1 = new JScrollPane(textArea);
        JScrollPane scrollPane2 = new JScrollPane(actionPanel);
        scrollPane2.setPreferredSize(new Dimension(sidePanelWidth, actionPanelHeight));
        this.add(scrollPane1);
        this.add(scrollPane2);
    }


    public void paintComponent(Graphics gx)
    {
        Graphics2D g = (Graphics2D) gx;
        paintWithGraphics(g);
    }

    private void paintWithGraphics(Graphics2D g)
    {
        if (gs == null) return;

        //For a better graphics, enable this: (be aware this could bring performance issues depending on your HW & OS).
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Board board = gs.getBoard();

        if (highlightInGridBounds()) {

            Types.TERRAIN t = board.getTerrainAt(highlightY, highlightX);
            Types.RESOURCE r = board.getResourceAt(highlightY, highlightX);
            Types.BUILDING b = board.getBuildingAt(highlightY, highlightX);
            Unit u = board.getUnitAt(highlightY, highlightX);

            // t < r < b < u

            String s;

            if (u != null && !clickedTwice()) {
                // Unit is always on top, show this, unless clicked twice
                s = getUnitInfo(u);
            } else {
                s = "<h1>";
                if (t != null) {
                    if (t == Types.TERRAIN.CITY) { // It's a city, show just this
                        s = getCityInfo();
                    } else {
                        // Show everything else
                        s += t.toString();
                        if (r != null) {
                            // Resource next
                            s += ", " + r.toString();
                        }
                        if (b != null) {
                            // Buildings
                            s += ", " + b.toString();
                        }
                    }
                } else {
                    // Show buildings and resources
                    if (r != null) {
                        // Resource next
                        s += r.toString();
                        if (b != null) {
                            // Buildings
                            s += ", " + b.toString();
                        }
                    } else if (b != null) {
                        // Buildings
                        s += b.toString();
                    }
                }
                s += "</h1>";

                updateButtons();
            }

            if (!textArea.getText().equals(s)) {
                textArea.setText(s);
            }
        }
    }

    private String getUnitInfo(Unit u) {
//        String img = u.getType().getImageStr(u.getTribeId());

        StringBuilder sb = new StringBuilder();
        sb.append("<h1>" + Types.TRIBE.values()[u.getTribeId()] + " " + u.getType() + "</h1>");
//        sb.append("<table border=\"0\"><tr><td><img src=\"file:" + img + "\"/></p></td><td>");
        sb.append("From city " + u.getCityID() + "<br/>");
        if (u.isVeteran()) {
            sb.append("<b>Veteran unit.</b>");
        } else {
            int kills = Math.min(u.getKills(), 3);
            sb.append("" + kills + "/3 kills to become a veteran.");
        }
//        sb.append("</td></tr></table>");
        sb.append("<ul>");
        sb.append("<li><b>Health:</b> " + u.getCurrentHP() + "/" + u.getMaxHP() + "</li>");
        sb.append("<li><b>Attack:</b> " + u.ATK + "</li>");
        sb.append("<li><b>Defence:</b> " + u.DEF + "</li>");
        sb.append("<li><b>Movement:</b> " + u.MOV + "</li>");
        sb.append("<li><b>Range:</b> " + u.RANGE + "</li>");
        sb.append("<li><b>Status:</b> " + u.getStatus() + "</li>");
        sb.append("</ul>");
        return sb.toString();
    }

    private String getCityInfo() {
        Board board = gs.getBoard();
        int cityID = board.getCityIdAt(highlightY, highlightX);
        City c = (City) board.getActor(cityID);

        StringBuilder sb = new StringBuilder();
        if(c != null) {
            sb.append("<h1>" + Types.TRIBE.values()[c.getTribeId()] + " city " + cityID + "</h1>");
//            sb.append("<table border=\"0\"><tr><td><img width=\"" + CELL_SIZE + "\" src=\"file:" + Types.TERRAIN.CITY.getImageStr() + "\"/></p></td><td>");
            sb.append("<ul>");
            sb.append("<li><b>Is Capital:</b> " + c.isCapital() + "</li>");
            sb.append("<li><b>Points:</b> " + c.getPointsPerTurn() + "</li>");
            sb.append("<li><b>Production:</b> " + c.getProduction() + "</li>");
            sb.append("</ul>");
//            sb.append("</td></tr></table>");
        }
        return sb.toString();
    }

    private void updateButtons() {
        Board board = gs.getBoard();
        int cityID = board.getCityIdAt(highlightY, highlightX);
        Types.TERRAIN t = board.getTerrainAt(highlightY, highlightX);
        Types.RESOURCE r = board.getResourceAt(highlightY, highlightX);
        Types.BUILDING b = board.getBuildingAt(highlightY, highlightX);
        Vector2d position = new Vector2d(highlightY, highlightX);
        resetButtonVisibility();

        if (cityID != -1) {
            City c = (City) gs.getBoard().getActor(cityID);
            if (c != null) {  // TODO: this should not be the case. Fix partial observability.
                ArrayList<Action> acts = gs.getCityActions(c);

                if (r != null) {
                    boolean found = false;
                    if (acts != null && acts.size() > 0) {
                        for (Action a : acts) {
                            if (a instanceof ResourceGathering) {
                                if (((ResourceGathering) a).getResource().equals(r)
                                        && ((ResourceGathering) a).getTargetPos().equals(position)) {
                                    // Could try to collect this resource, set button value
                                    listenerRG.update(cityID, position, ac, gs, "ResourceGathering");
                                    listenerRG.setResource(r);
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                    actionRG.setVisible(true);
                    actionRG.setEnabled(found);
                }
                if (t != null) {
                    // if forest: Burn & Clear
                    // if plain: Grow forest
                    if (t == Types.TERRAIN.FOREST) {
                        boolean foundBF = false;
                        boolean foundCF = false;
                        if (acts != null && acts.size() > 0) {
                            for (Action a : acts) {
                                if (a instanceof BurnForest) {
                                    if (((BurnForest) a).getTargetPos().equals(position)) {
                                        listenerBF.update(cityID, position, ac, gs, "BurnForest");
                                        foundBF = true;
                                    }
                                } else if (a instanceof ClearForest) {
                                    if (((ClearForest) a).getTargetPos().equals(position)) {
                                        listenerCF.update(cityID, position, ac, gs, "ClearForest");
                                        foundCF = true;
                                    }
                                }
                            }
                        }
                        actionBF.setVisible(true);
                        actionCF.setVisible(true);
                        actionBF.setEnabled(foundBF);
                        actionCF.setEnabled(foundCF);

                    } else if (t == Types.TERRAIN.PLAIN) {
                        boolean foundGF = false;
                        if (acts != null && acts.size() > 0) {
                            for (Action a : acts) {
                                if (a instanceof GrowForest) {
                                    if (((GrowForest) a).getTargetPos().equals(position)) {
                                        listenerGF.update(cityID, position, ac, gs, "GrowForest");
                                        foundGF = true;
                                    }
                                }
                            }
                        }
                        actionGF.setVisible(true);
                        actionGF.setEnabled(foundGF);
                    }
                }
                if (b != null) {
                    boolean found = false;
                    if (acts != null && acts.size() > 0) {
                        for (Action a : acts) {
                            if (a instanceof Destroy) {
                                if (((Destroy) a).getTargetPos().equals(position)) {
                                    listenerD.update(cityID, position, ac, gs, "Destroy");
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                    actionD.setVisible(true);
                    actionD.setEnabled(found);
                }
                if (c.getPosition().equals(position)) {
                    // We've highlighted the city, all spawn actions show up
                    for (JButton jb: actionS) {
                        jb.setVisible(true);
                        jb.setEnabled(false);
                    }
                    if (acts != null && acts.size() > 0) {
                        for (Action a : acts) {
                            if (a instanceof Spawn) {
                                Types.UNIT unitType = ((Spawn) a).getUnitType();
                                int idx = unitType.getKey();
                                listenerS[idx].update(cityID, position, ac, gs, "Spawn");
                                listenerS[idx].setUnitType(unitType);
                                actionS[idx].setEnabled(true);
                                break;
                            }
                        }
                    }
                } else if (t != null && b == null) {
                    // We might be able to build here
                    for (int i = 0; i < Types.BUILDING.values().length; i++) {
                        if (Types.BUILDING.values()[i].getTerrainRequirements().contains(t)) {
                            actionB[i].setVisible(true);
                        }
                        actionB[i].setEnabled(false);
                    }
                    if (acts != null && acts.size() > 0) {
                        for (Action a : acts) {
                            if (a instanceof Build) {
                                Types.BUILDING buildingType = ((Build) a).getBuildingType();
                                if (buildingType.getTerrainRequirements().contains(t)) {
                                    int idx = buildingType.getKey();
                                    listenerB[idx].update(cityID, position, ac, gs, "Build");
                                    listenerB[idx].setBuildingType(buildingType);
                                    actionB[idx].setEnabled(true);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void resetButtonVisibility(){
        actionBF.setVisible(false);
        actionCF.setVisible(false);
        actionD.setVisible(false);
        actionGF.setVisible(false);
        actionRG.setVisible(false);
        for (JButton jb: actionB) {
            jb.setVisible(false);
        }
        for (JButton jb: actionS) {
            jb.setVisible(false);
        }
    }

    void paint(GameState gs)
    {
        this.gs = gs;
        this.repaint();
    }

    /**
     * Gets the dimensions of the window.
     * @return the dimensions of the window.
     */
    public Dimension getPreferredSize() {
        return size;
    }

    public void setHighlight(int x, int y)
    {
        highlightXprev = highlightX;
        highlightYprev = highlightY;
        highlightX = x;
        highlightY = y;
    }

    public void resetHighlight() {
        highlightX = -1;
        highlightY = -1;
        highlightXprev = -1;
        highlightYprev = -1;

        // Reset highlight info text
        textArea.setText("");

        // Reset actions, none available yet for this player
        resetButtonVisibility();

        repaint();
    }

    public int getHighlightX() {return highlightX;}
    public int getHighlightY() {return highlightY;}
    public boolean clickedTwice() {
        return highlightX == highlightXprev && highlightY == highlightYprev;
    }
    public boolean highlightInGridBounds() {
        return highlightX > -1 && highlightY > -1 && highlightX < gridSize && highlightY < gridSize;
    }

    class CityActionListener implements ActionListener {
        int cityID;
        Vector2d position;
        ActionController ac;
        GameState gs;
        String actionType = "";

        Types.RESOURCE resource;
        Types.UNIT unitType;
        Types.BUILDING buildingType;

        CityActionListener() {}

        public void update(int cityID, Vector2d position, ActionController ac, GameState gs, String type) {
            this.cityID = cityID;
            this.position = position;
            this.ac = ac;
            this.gs = gs;
            this.actionType = type;
        }

        public void setResource(Types.RESOURCE resource) {
            this.resource = resource;
        }

        public void setUnitType(Types.UNIT unitType) {
            this.unitType = unitType;
        }

        public void setBuildingType(Types.BUILDING buildingType) {
            this.buildingType = buildingType;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Action a = null;
            switch (actionType) {
                case "BurnForest":
                    a = new BurnForest(cityID);
                    ((BurnForest) a).setTargetPos(position);
                    break;
                case "ClearForest":
                    a = new ClearForest(cityID);
                    ((ClearForest) a).setTargetPos(position);
                    break;
                case "Destroy":
                    a = new Destroy(cityID);
                    ((Destroy) a).setTargetPos(position);
                    break;
                case "GrowForest":
                    a = new GrowForest(cityID);
                    ((GrowForest) a).setTargetPos(position);
                    break;
                case "ResourceGathering":
                    a = new ResourceGathering(cityID);
                    ((ResourceGathering) a).setTargetPos(position);
                    ((ResourceGathering) a).setResource(resource);
                    break;
                case "Spawn":
                    a = new Spawn(cityID);
                    ((Spawn) a).setTargetPos(position);
                    ((Spawn) a).setUnitType(unitType);
                    break;
                case "Build":
                    a = new Build(cityID);
                    ((Build) a).setTargetPos(position);
                    ((Build) a).setBuildingType(buildingType);
            }
            if (a != null) {
                ac.addAction(a, gs);
                resetHighlight();
            }
        }
    }
}

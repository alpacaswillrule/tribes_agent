package core.actions.unitactions;

import core.TechnologyTree;
import core.Types;
import core.actions.Action;
import core.actors.City;
import core.actors.Tribe;
import core.game.Board;
import core.game.GameState;
import core.actors.units.Unit;
import utils.Vector2d;

import java.util.Random;

import static core.Types.EXAMINE_BONUS.*;

public class Examine extends UnitAction
{
    Types.EXAMINE_BONUS bonus;

    public Examine(int unitId)
    {
        super.unitId = unitId;
    }


    @Override
    public boolean isFeasible(final GameState gs) {
        Unit unit = (Unit) gs.getActor(this.unitId);
        Vector2d unitPos = unit.getPosition();
        return unit.isFresh() && gs.getBoard().getResourceAt(unitPos.x, unitPos.y) == Types.RESOURCE.RUINS;
    }

    @Override
    public boolean execute(GameState gs) {
        if(isFeasible(gs)) {
            Unit unit = (Unit) gs.getActor(this.unitId);
            Tribe t = gs.getTribe(unit.getTribeId());
            Random rnd = gs.getRandomGenerator();
            TechnologyTree technologyTree = t.getTechTree();
            int capital = t.getCapitalID();

            boolean allTech = technologyTree.isEverythingResearched();
            bonus = Types.EXAMINE_BONUS.random(rnd);
            while (allTech && bonus == RESEARCH) {
                bonus = Types.EXAMINE_BONUS.random(rnd);
            }

            switch (bonus) {
                case SUPERUNIT:
                    Board board = gs.getBoard();
                    Vector2d cityPos = gs.getActor(capital).getPosition();
                    Unit unitInCity = board.getUnitAt(cityPos.x, cityPos.y);
                    if(unitInCity != null)
                        gs.pushUnit(unitInCity, cityPos.x, cityPos.y);

                    Unit superUnit = Types.UNIT.createUnit(cityPos, 0, false, capital, unit.getTribeId(), Types.UNIT.SUPERUNIT);
                    board.addUnit((City)gs.getActor(capital), superUnit);
                    break;

                case RESEARCH:
                    technologyTree.researchAtRandom(rnd);
                    break;

                case POP_GROWTH:
                    City c = (City) gs.getActor(capital);
                    c.addPopulation(t, bonus.getBonus());
                    break;

                case EXPLORER:
                    cityPos = gs.getActor(capital).getPosition();
                    gs.getBoard().launchExplorer(cityPos.x, cityPos.y, unit.getTribeId(), rnd);
                    break;

                case RESOURCES:
                    gs.getTribe(unit.getTribeId()).addStars(bonus.getBonus());
                    break;
            }
            Vector2d unitPos = unit.getPosition();
            gs.getBoard().setResourceAt(unitPos.x, unitPos.y, null);
            unit.setStatus(Types.TURN_STATUS.FINISHED);
            return true;
        }
        return false;
    }

    public Types.EXAMINE_BONUS getBonus() {
        return bonus;
    }

    @Override
    public Action copy() {
        Examine copy = new Examine(this.unitId);
        copy.bonus = bonus;
        return copy;
    }

    public String toString() {
        return "EXAMINE by unit " + this.unitId + " of target " + this.bonus.toString();
    }
}

package core.actions.unitactions.command;

import core.Diplomacy;
import core.Types;
import core.actions.Action;
import core.actions.ActionCommand;
import core.actions.unitactions.Convert;
import core.actors.City;
import core.actors.Tribe;
import core.actors.units.Unit;
import core.game.GameState;

import static core.TribesConfig.CONVERT_REPERCUSSION;

public class ConvertCommand implements ActionCommand {

    @Override
    public boolean execute(Action a, GameState gs) {
        Convert action = (Convert) a;

        //Getting current diplomacy
        Diplomacy d = gs.getBoard().getDiplomacy();

        //Check if action is feasible before execution
        if (action.isFeasible(gs)) {
            int unitId = action.getUnitId();
            int targetId = action.getTargetId();
            Unit target = (Unit) gs.getActor(targetId);
            Unit unit = (Unit) gs.getActor(unitId);
            Tribe targetTribe = gs.getTribe(target.getTribeId());

            //remove the unit from its original city.
            int cityId = target.getCityId();
            City c = (City) gs.getActor(cityId);
            gs.getBoard().removeUnitFromCity(target, c, targetTribe);

            //add tribe to converted unit
            target.setTribeId(unit.getTribeId());
            gs.getActiveTribe().addExtraUnit(target);

            // Updating relationship between tribes, deducting 5
            d.updateAllegiance(CONVERT_REPERCUSSION, unit.getTribeId(), target.getTribeId());
            // Checks consequences of the update
            d.checkConsequences(CONVERT_REPERCUSSION, unit.getTribeId(), target.getTribeId());

            //manage status of the units after the action is executed
            unit.transitionToStatus(Types.TURN_STATUS.ATTACKED);
            target.setStatus(Types.TURN_STATUS.FINISHED);
            return true;
        }
        return false;
    }
}

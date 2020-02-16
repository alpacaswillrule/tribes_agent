package core.actions.unitactions;

import core.actions.Action;
import core.game.GameState;
import core.actors.units.Unit;

import java.util.ArrayList;
import java.util.LinkedList;

public class Recover extends UnitAction
{
    public Recover(Unit target)
    {
        super.unit = target;
    }


    @Override
    public LinkedList<Action> computeActionVariants(final GameState gs) {

        LinkedList<Action> actions = new LinkedList<>();
        Recover newAction = new Recover(unit);
        float currentHP = unit.getCurrentHP();
        if (currentHP < unit.MAX_HP && currentHP > 0){
            actions.add(newAction);
        }
        return actions;
    }

    @Override
    public boolean isFeasible(final GameState gs) {
        float currentHP = unit.getCurrentHP();
        return currentHP < unit.MAX_HP && currentHP > 0;
    }

    @Override
    public boolean execute(GameState gs) {
        float currentHP = unit.getCurrentHP();
        int addHP = 2;
        if (currentHP < unit.MAX_HP && currentHP > 0) {
            if (gs != null){
               int cityID = gs.getBoard().getCityIDAt(unit.getCurrentPosition().x, unit.getCurrentPosition().y);
               if (cityID != -1){
                   ArrayList<Integer> citesID = gs.getTribe(unit.getTribeID()).getCitiesID();
                   if (citesID.contains(cityID)){
                       addHP += 2;
                   }
            }
            }
            unit.setCurrentHP(Math.min(currentHP + addHP, unit.MAX_HP));
            return true;
        }
        return false;
    }
}

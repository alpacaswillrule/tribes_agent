package core.actions.cityactions;

import core.Types;
import core.game.GameState;
import core.actors.City;

public class Spawn extends CityAction
{
    private Types.UNIT unit_type;

    public Spawn(City c, Types.UNIT unit_type)
    {
        super.city = c;
        this.unit_type = unit_type;
    }


    public Types.UNIT getUnitType() {
        return unit_type;
    }

    @Override
    public boolean isFeasible(GameState gs) {
        return false;
    }

    @Override
    public void execute(GameState gs) {

    }
}

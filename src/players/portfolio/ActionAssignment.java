package players.portfolio;

import core.actions.Action;
import core.actors.Actor;
import core.game.GameState;
import players.portfolio.scripts.BaseScript;

public class ActionAssignment {
    private final BaseScript script;
    private final Actor actor;
    private Action action;

    public ActionAssignment(Actor a, BaseScript s)
    {
        actor = a;
        script = s;
    }

    boolean process(GameState gs)
    {
        action = script.process(gs, actor);
        return action != null;
    }

    public BaseScript getScript() {
        return script;
    }
    public Actor getActor() {
        return actor;
    }
    public Action getAction() {return action;}

    @Override
    public boolean equals(Object o)
    {
        if(!(o instanceof ActionAssignment))
        {
            return false;
        }
        ActionAssignment aas = (ActionAssignment)o;

        return actor.getActorId() == aas.getActor().getActorId() && action == aas.getAction();
    }

    public String toString()
    {
        return "Actor " + actor.getActorId() + ", Action " + action.toString() + "; " + script;
    }
}

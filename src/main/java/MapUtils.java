import com.github.ocraft.s2client.bot.S2Agent;
import com.github.ocraft.s2client.protocol.spatial.Point;
import com.github.ocraft.s2client.protocol.spatial.Point2d;

import java.util.Comparator;
import java.util.List;

class MapUtils {

    private final S2Agent agent;
    private Point2d STARTING_BASE_LOCATION;
   // private List<Point> base_locations;

    MapUtils(S2Agent agent)
    {
        this.agent = agent;
    }

    void init() {
      //  base_locations = agent.query().calculateExpansionLocations(agent.observation());
        STARTING_BASE_LOCATION = agent.observation().getStartLocation().toPoint2d();

    }

    Point2d getStartingBaseLocation() {
        return STARTING_BASE_LOCATION;
    }

    Point2d getNearestExpansionLocationTo(Point2d source){
        List<Point> base_locations = agent.query().calculateExpansionLocations(agent.observation());
        base_locations.sort(getDistanceComparator(source));
        return base_locations.get(0).toPoint2d();
    }

    private Comparator<Point> getDistanceComparator(Point2d source) {
        return (p1, p2) -> {
            Double d1 = p1.toPoint2d().distance(source);
            Double d2 = p2.toPoint2d().distance(source);
            return d1.compareTo(d2);
        };
    }

}

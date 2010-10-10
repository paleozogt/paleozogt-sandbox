import java.util.*;
import java.io.*;

class RelativePlanetInfo {
    int planetFrom;
    int planetTo;
    int cost;

    public RelativePlanetInfo(int from, int to, int cost) {
        this.planetFrom = from;
        this.planetTo = to;
        this.cost = cost;
    }
}

class SortByCosts implements Comparator<RelativePlanetInfo> {
    public int compare(RelativePlanetInfo p1, RelativePlanetInfo p2) {
        if (p1.cost < p2.cost)
            return -1;
        else if (p1.cost > p2.cost)
            return 1;
        else
            return 0;
    }
}

class SortByGrowthRate implements Comparator<Planet> {

    public int compare(Planet o1, Planet o2) {
        if (o1.GrowthRate() > o2.GrowthRate())
            return -1;
        else if (o1.GrowthRate() < o2.GrowthRate())
            return 1;
        else
            return 0;
    }
}

public class MyBot {

    int turnCount = -1;
    int lastAttackPlanet = -1;
    PrintStream log;
    PlanetWars pw;

    public MyBot() throws IOException {
        log = new PrintStream(new FileOutputStream("botlog.txt"));
    }

    public void DoTurn(PlanetWars pw) {
        turnCount++;
        log.println("\nturn " + turnCount);
        this.pw = pw;

        List<Planet> myPlanets = pw.MyPlanets();
        List<Planet> enemyPlanets = pw.EnemyPlanets();
        List<Planet> otherPlanets = pw.NotMyPlanets();
        List<Planet> dests;
        int myProduction = pw.Production(1);
        int enemyProduction = pw.Production(2);

        for (Planet p : myPlanets) {
            log.println("source " + p.PlanetID() + " (" + p.NumShips() + ")");
            attackBest(p, otherPlanets);
        }
    }

    void attackBest(Planet source, List<Planet> dests) {
        List<RelativePlanetInfo> costs = calcTimeTillBreakEven(source, dests);
        int shipsremaining = source.NumShips();

        for (RelativePlanetInfo info : costs) {
            Planet p = pw.GetPlanet(info.planetTo);
            log.println("p " + info.planetTo + " gr " + p.GrowthRate() + " ns " +
                        p.NumShips() + " dist " +
                        pw.Distance(info.planetFrom, info.planetTo) + " cost " +
                        info.cost);
            
            int numships = p.NumShips() + 1;
            if (numships > source.NumShips())
                numships= source.NumShips();

            issueOrder(source, p, numships);
            
            if (source.NumShips() <= 0) break;
        }
    }

    void issueOrder(Planet s, Planet d, int numShips) {
        s.decrementShips(numShips);
        pw.IssueOrder(s, d, numShips);
    }
    
    List<RelativePlanetInfo> calcTimeTillBreakEven(Planet from, List<Planet> planets) {
        List<RelativePlanetInfo> costs = new ArrayList<RelativePlanetInfo>();
        for (Planet p : planets) {
        	// growthrate zero planets aren't worth considering
        	// (plus, they screw up the formulas)
        	if (p.GrowthRate() <= 0) continue;
        
            int conquerturns= Math.round((float)(p.NumShips() - from.NumShips()) / from.GrowthRate());
            if (conquerturns < 0) conquerturns= 0;
            
            int cost = pw.Distance(from, p) + conquerturns + Math.round((float) p.NumShips() / (float) p.GrowthRate());
            RelativePlanetInfo info = new RelativePlanetInfo(from.PlanetID(), p.PlanetID(), cost);
            costs.add(info);
        }
        Collections.sort(costs, new SortByCosts());
        return costs;
    }

    public static void main(String[] args) throws IOException {
        MyBot bot = new MyBot();
        String line = "";
        String message = "";
        int c;
        try {
            while ((c = System.in.read()) >= 0) {
                switch (c) {
                case '\n':
                    if (line.equals("go")) {
                        PlanetWars pw = new PlanetWars(message);
                        bot.DoTurn(pw);
                        pw.FinishTurn();
                        message = "";
                    } else {
                        message += line + "\n";
                    }
                    line = "";
                    break;
                default:
                    line += (char) c;
                    break;
                }
            }
        } catch (Exception e) {
            // Owned.
        }
    }
}

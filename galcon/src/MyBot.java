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

class SortByDistance implements Comparator<Planet> {
	PlanetWars pw;
	Planet from;
	public SortByDistance(PlanetWars pw, Planet from) {
		this.pw= pw;
		this.from= from;
	}
	
    public int compare(Planet p1, Planet p2) {
    	int dist1= pw.Distance(from, p1);
    	int dist2= pw.Distance(from, p2);
    	
        if (dist1 < dist2)
            return -1;
        else if (dist1 > dist2)
            return 1;
        else
            return 0;
    }
}

public class MyBot {

    int turnCount = -1;
    long turnStart, turnEnd;
    long turnTimeMax;
    PrintStream log;
    PlanetWars pw;

    List<Planet> myPlanets;
    List<Planet> enemyPlanets;
    List<Planet> otherPlanets;    
    
    Map<Integer, Boolean> amAttacking;
    
    public MyBot() throws IOException {
        log = new PrintStream(new FileOutputStream("botlog.txt"));
    }

    public void DoTurn(PlanetWars pw) {
    	turnStart= System.currentTimeMillis();
        turnCount++;
        log.println("\nturn " + turnCount);
        this.pw = pw;
        
		boolean easyConquest= (turnCount < 1);

        myPlanets = pw.MyPlanets();
        enemyPlanets = pw.EnemyPlanets();
        otherPlanets = pw.NotMyPlanets();
        List<Planet> dests;
        List<Fleet> myFleets= pw.MyFleets();
        int myProduction = pw.Production(1);
        int enemyProduction = pw.Production(2);

        // make a map of attack planets for easy access
        amAttacking= new HashMap<Integer, Boolean>();
        for (Fleet f : myFleets) {
        	amAttacking.put(f.DestinationPlanet(), true);
        }
        
        for (Planet p : myPlanets) {
            log.println("source " + p.PlanetID() + " (" + p.NumShips() + ")");
            attackBest(p, otherPlanets, easyConquest);
        }
        
        turnEnd= System.currentTimeMillis();
        long turnTime= turnEnd-turnStart;
        if (turnTime > turnTimeMax) turnTimeMax= turnTime;        	
        log.println("turn took " + turnTime + "ms \n");
    }

    void attackBest(Planet source, List<Planet> dests, boolean easyConquest) {
        List<RelativePlanetInfo> costs = calcTimeTillBreakEven(source, dests);
        int origNumShips = source.NumShips();

        for (RelativePlanetInfo info : costs) {
            Planet p = pw.GetPlanet(info.planetTo);
            log.println("p " + info.planetTo + "(" + p.Owner() + ")" + " gr " + p.GrowthRate() + " ns " +
                        p.NumShips() + " dist " +
                        pw.Distance(info.planetFrom, info.planetTo) + " cost " +
                        info.cost);

            // if one of my own is already attacking a neutral, move on
            if (p.isNeutral() && amAttacking.containsKey(p.PlanetID())) {
            	continue;
            }
            
            if (easyConquest && (!canBeat(source, p) || p.NumShips() > origNumShips/2)) {
            	continue;
            }
            
            if (p.isNeutral() && !canBeat(source, p))
            	break;
            
            int numships = p.NumShips() + 1;
            if (p.isEnemy())
            	numships+= pw.Distance(source, p) * p.GrowthRate();

            if (numships > source.NumShips())
                numships= source.NumShips();
            
            Planet wayPoint= getWaypoint(source, p);
            if (wayPoint.PlanetID() == source.PlanetID())            
            	issueOrder(source, p, numships);
            else
            	issueOrder(source, wayPoint, numships);
            
            if (source.NumShips() <= 0) break;
        }
    }

	boolean canBeat(Planet p1, Planet p2) {
		return p1.NumShips() > p2.NumShips();
	}

    void issueOrder(Planet s, Planet d, int numShips) {
        s.decrementShips(numShips);
        pw.IssueOrder(s, d, numShips);
    }
    
    Planet getWaypoint(Planet source, Planet target) {
    	log.println("calculating waypoint for " + source.PlanetID() + "=>" + target.PlanetID());
    	
    	// get a sorted list of my planets nearest to source
    	List<Planet> nearestPlanets= new ArrayList<Planet>(myPlanets.size());
    	nearestPlanets.addAll(myPlanets);
    	Collections.sort(nearestPlanets, new SortByDistance(pw, source));
    	
    	for (Planet p : nearestPlanets) {
    		log.println("dist " + source.PlanetID() + "=>" + p.PlanetID() + "=" + pw.Distance(source, p));
    	}
    	log.println("");
    	
    	Planet nearest= source;
    	int bestDistance= pw.Distance(source, target);
    	
    	for (Planet p : nearestPlanets) {
    		int distance= pw.Distance(p, target);
    		log.println("dist " + p.PlanetID() + "=>" + target.PlanetID() + "=" + distance);
    		if (distance < bestDistance) {
    			bestDistance= distance;
    			nearest= p;
    			break;
    		}
    	}

    	log.println("waypoint is " + nearest.PlanetID());
    	return nearest;
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
            
            bot.log.println("max turn time " + bot.turnTimeMax + "ms");
        } catch (Exception e) {
            bot.log.println(e.toString());
        }
    }
}

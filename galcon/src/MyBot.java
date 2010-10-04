import java.util.*;
import java.io.*;

class RelativePlanetInfo {
	int planetFrom;
	int planetTo;
	int cost;
	public RelativePlanetInfo(int from, int to, int cost) {
		this.planetFrom= from;
		this.planetTo= to;
		this.cost= cost;
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

class SortByGrowthRate implements Comparator<Planet>{

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

	int turnCount= -1;
	int lastAttackPlanet= -1;
	PrintStream log;
	PlanetWars pw;

	public MyBot() throws IOException {
		log= new PrintStream(new FileOutputStream("botlog.txt"));
	}

    public void DoTurn(PlanetWars pw) {
    	turnCount++;
    	this.pw= pw;
    	
    	List<Planet> enemyPlanets= pw.EnemyPlanets();
    	List<Planet> myPlanets= pw.MyPlanets();
    	List<Planet> otherPlanets= pw.NotMyPlanets();    	
    	List<Planet> dests;
		int myProduction= pw.Production(1);
		int enemyProduction= pw.Production(2);

		if (turnCount == 0) {
			doOpening();
			return;
		}

		// get aggressive when cornered
		if (myProduction < enemyProduction)
			dests= otherPlanets;
		// include enemyplanets when decisively winning
		else if (myPlanets.size() > enemyPlanets.size()*2 && myProduction > enemyProduction)
			dests= otherPlanets;
		// otherwise just grow
		else
			dests= pw.NeutralPlanets();

		if (dests.size() <= 0)
			dests= otherPlanets;

		Collections.sort(dests, new SortByGrowthRate());

		List<Planet> sources= new ArrayList<Planet>();
		for (Planet p : myPlanets) {
			boolean amAttacked= false;
			for (Fleet f : pw.EnemyFleets()) {
				if (f.DestinationPlanet() == p.PlanetID()) {
					amAttacked= true;
				}
			}
			
			if (!amAttacked)
				sources.add(p);
		}

		boolean pickNewTarget= true;
		if (lastAttackPlanet > -1) {
			for (Fleet f : pw.EnemyFleets()) {
				if (f.DestinationPlanet() == lastAttackPlanet) {
					pickNewTarget= false;
				}
			}
		}

		double ratio= (2.0/3.0);

		Planet dest= null;		
		if (pickNewTarget) {
			// find the best growth rate planet
			int destScore= -1;
			for (Planet p : dests) {
				int score = Math.round((float)p.GrowthRate() / (float)p.NumShips());				
				if (score > destScore) {
					destScore = score;
					dest = p;
				}
			}
		} else {
			dest= pw.GetPlanet(lastAttackPlanet);
		}

		if (dest == null) return;
		lastAttackPlanet= dest.PlanetID();
		
		for (Planet p : sources) {
			if (p.NumShips() < 10 * p.GrowthRate()) {
				continue;
			}

			int numShips= Math.max((int)(p.NumShips()*ratio), dest.NumShips() + dest.GrowthRate());
			if (numShips > p.NumShips()) numShips= Math.min(p.GrowthRate(), p.NumShips());
			
			pw.IssueOrder(p, dest, numShips);
		}
    }

	void doOpening() {
		Planet source= pw.MyPlanets().get(0);
    	List<Planet> dests= pw.NeutralPlanets();  		
		List<RelativePlanetInfo> costs= calcTimeTillBreakEven(source, dests);
		int shipsremaining= source.NumShips();

		for (RelativePlanetInfo info : costs) {
			Planet p= pw.GetPlanet(info.planetTo);
			log.println("p " + info.planetTo + " gr " + p.GrowthRate() + " ns " + p.NumShips() + 
						" dist " + pw.Distance(info.planetFrom, info.planetTo) + " cost " + info.cost);

			int numships= p.NumShips() + 1;
			shipsremaining-= numships;
			if (shipsremaining <= 0) break;
			
			pw.IssueOrder(source, p, numships);
		}
	}
	
	List<RelativePlanetInfo> calcTimeTillBreakEven(Planet from, List<Planet> planets) {
		List<RelativePlanetInfo> costs= new ArrayList<RelativePlanetInfo>();
		for (Planet p : planets) {
			int cost= pw.Distance(from, p) + Math.round((float)p.NumShips() / (float)p.GrowthRate());		
			RelativePlanetInfo info= new RelativePlanetInfo(from.PlanetID(), p.PlanetID(), cost);
			costs.add(info);
		}
		Collections.sort(costs, new SortByCosts());
		return costs;
	}
	
    public static void main(String[] args) throws IOException {
    	MyBot bot= new MyBot();
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
					line += (char)c;
					break;
				}
			}
		} catch (Exception e) {
			// Owned.
		}
    }
}


package net.inaka.pong.rank;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.collections15.Transformer;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

import com.google.gdata.client.spreadsheet.SpreadsheetQuery;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;

import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;

class Edge {

	private int	edgeNum;
	private int	weight	= 0;

	public Edge(int edgeNum) {
		this.edgeNum = edgeNum;
	}

	public int num() {
		return this.edgeNum;
	}

	public int weight() {
		return this.weight;
	}

	public void incr() {
		this.weight++;
	}

	@Override
	public String toString() {
		return "" + this.num() + ": " + this.weight();
	}

	public void decr() {
		this.weight--;
	}
}

class TournamentHistory {

	private DirectedSparseGraph<String, Integer>	graph;
	private Transformer<Integer, Double>			edgeWeights;
	private List<Pair<String>>						matches;

	public TournamentHistory(DirectedSparseGraph<String, Integer> g,
			Transformer<Integer, Double> ew, List<Pair<String>> matches) {
		this.graph = g;
		this.edgeWeights = ew;
		this.matches = matches;
	}

	/**
	 * @return the matches
	 */
	public List<Pair<String>> matches() {
		return matches;
	}

	/**
	 * @return the graph
	 */
	public DirectedSparseGraph<String, Integer> graph() {
		return graph;
	}

	/**
	 * @return the edgeWeights
	 */
	public Transformer<Integer, Double> edgeWeights() {
		return edgeWeights;
	}

	@Override
	public String toString() {
		return this.graph.toString() + "\nEW:\t" + this.edgeWeights;
	}
}

/**
 * @author Fernando Benavides <elbrujohalcon@inaka.net>
 * 
 */
public class Ranker {

	/**
	 * Command-line point of entry
	 * 
	 * @param args
	 *            arguments
	 */
	public static void main(String args[]) {
		TournamentHistory th = readHistory();

		System.out.println("Pong Rank: ");
		for (Entry<Integer, List<String>> entry : pongRank(th).entrySet()) {
			System.out.print(entry.getKey() + " " + entry.getValue() + "; ");
		}
		System.out.println();
		System.out.println();
		System.out.println("Averages: ");
		for (Entry<Integer, List<String>> entry : averages(th).entrySet()) {
			System.out.print(entry.getKey() + " " + entry.getValue() + "; ");
		}
		System.out.println();
		System.out.println();
		System.out.println("Averages (with iterations): ");
		for (Entry<Integer, List<String>> entry : averages2(th).entrySet()) {
			System.out.print(entry.getKey() + " " + entry.getValue() + "; ");
		}
		System.out.println();
	}

	/**
	 * @param th
	 * @return
	 */
	private static SortedMap<Integer, List<String>> pongRank(
			TournamentHistory th) {
		PageRank<String, Integer> pr = new PageRank<String, Integer>(
				th.graph(), th.edgeWeights(), 0.15);
		pr.evaluate();

		Set<String> sortedVerticesSet = new HashSet<String>(th.graph()
				.getVertices());

		SortedMap<Integer, List<String>> table = new TreeMap<Integer, List<String>>();

		for (String v : sortedVerticesSet) {
			int score = new Double(pr.getVertexScore(v) * 100000).intValue();
			List<String> current = table.get(score);
			if (current == null)
				table.put(score, new Vector<String>());
			table.get(score).add(v);
		}
		return table;
	}

	/**
	 * @param th
	 * @return
	 */
	private static SortedMap<Integer, List<String>> averages(
			TournamentHistory th) {
		TreeMap<String, Double> points = new TreeMap<String, Double>();
		TreeMap<String, Integer> games = new TreeMap<String, Integer>();

		for (Pair<String> match : th.matches()) {
			points.put(match.getFirst(), 0.0);
			points.put(match.getSecond(), 0.0);
			games.put(match.getFirst(), 0);
			games.put(match.getSecond(), 0);
		}
		for (Pair<String> match : th.matches()) {
			points.put(match.getSecond(), points.get(match.getSecond()) + 1);
			games.put(match.getFirst(), games.get(match.getFirst()) + 1);
			games.put(match.getSecond(), games.get(match.getSecond()) + 1);
		}

		SortedMap<Integer, List<String>> table = new TreeMap<Integer, List<String>>();

		for (String v : games.keySet()) {
			System.out.println("DEBUG - " + v + ": " + points.get(v) + "/"
					+ games.get(v));
			int score = new Double(points.get(v) / games.get(v) * 1000)
					.intValue();
			List<String> current = table.get(score);
			if (current == null)
				table.put(score, new Vector<String>());
			table.get(score).add(v);
		}
		return table;
	}

	/**
	 * @param th
	 * @return
	 */
	private static SortedMap<Integer, List<String>> averages2(
			TournamentHistory th) {
		TreeMap<String, Double> points = new TreeMap<String, Double>();
		TreeMap<String, Integer> games = new TreeMap<String, Integer>();

		for (Pair<String> match : th.matches()) {
			points.put(
					match.getSecond(),
					points.get(match.getSecond()) == null ? 1 : points
							.get(match.getSecond()) + 1);
			games.put(match.getFirst(), games.get(match.getFirst()) == null ? 1
					: games.get(match.getFirst()) + 1);
			games.put(
					match.getSecond(),
					games.get(match.getSecond()) == null ? 1 : games.get(match
							.getSecond()) + 1);
		}

		TreeMap<String, Double> avgs = new TreeMap<String, Double>();
		for (String v : games.keySet()) {
			avgs.put(v,
					points.get(v) == null ? 0.0 : points.get(v) / games.get(v));
		}

		DoubleArrayList avgsList = new DoubleArrayList(avgs.size());
		avgsList.addAllOf(avgs.values());
		avgsList.sort();
		Double avgavg = Descriptive.mean(avgsList);

		System.out.println("Mean factor: " + (1 / avgavg));

		points.clear();

		for (Pair<String> match : th.matches()) {
			double matchPoints = avgs.get(match.getFirst()) / avgavg;
			points.put(match.getSecond(),
					points.get(match.getSecond()) == null ? matchPoints
							: points.get(match.getSecond()) + matchPoints);
		}

		SortedMap<Integer, List<String>> table = new TreeMap<Integer, List<String>>();

		for (String v : games.keySet()) {
			int score = points.get(v) == null ? 0 : new Double(new Double(
					points.get(v)) / new Double(games.get(v)) * 1000)
					.intValue();
			List<String> current = table.get(score);
			if (current == null)
				table.put(score, new Vector<String>());
			table.get(score).add(v);
		}
		return table;
	}

	private static TournamentHistory readHistory() {
		Set<String> players = new HashSet<String>();
		final List<Pair<String>> matches = new Vector<Pair<String>>();

		String username = "fernando.benavides@inakanetworks.com";
		String password = "nohaymonedas";

		try {
			SpreadsheetService service = new SpreadsheetService(
					"MySpreadsheetIntegration-v1");
			service.setUserCredentials(username, password);

			URL mySheets = new URL(
					"https://spreadsheets.google.com/feeds/spreadsheets/private/full");

			// Make a request to the API and get all spreadsheets.
			// Instantiate a SpreadsheetQuery object to retrieve spreadsheets.
			SpreadsheetQuery query = new SpreadsheetQuery(mySheets);

			// Make a request to the API and get all spreadsheets.
			SpreadsheetFeed feed = service.query(query, SpreadsheetFeed.class);
			List<SpreadsheetEntry> spreadsheets = feed.getEntries();

			SpreadsheetEntry inakaPongSheet = null;
			// Iterate through all of the spreadsheets returned
			for (SpreadsheetEntry spreadsheet : spreadsheets)
				if (spreadsheet.getKey().equals("tAgvnojXZmFTN28_5ar9W3A")) {
					inakaPongSheet = spreadsheet;
					System.out.println("DEBUG: " + inakaPongSheet.getId() + "/"
							+ inakaPongSheet.getKey());
					break;
				}

			List<WorksheetEntry> worksheets = inakaPongSheet.getWorksheets();
			for (WorksheetEntry worksheet : worksheets) {
				String[] dates = worksheet.getTitle().getPlainText()
						.split(" - ");
				if (dates.length == 2) {
					System.out.println("DEBUG: Reading tournament " + dates[0]
							+ " -> " + dates[1] + "...");

					URL cellFeedUrl = worksheet.getCellFeedUrl();
					CellFeed cellFeed = service.getFeed(cellFeedUrl,
							CellFeed.class);

					String[] tPlayers = new String[worksheet.getRowCount()];
					for (CellEntry cell : cellFeed.getEntries()) {
						int col = cell.getCell().getCol();
						int row = cell.getCell().getRow();
						if (col == 1) {
							String origPlayerName = cell.getTextContent()
									.getContent().getPlainText().toLowerCase();
							String playerName = origPlayerName;
							if (origPlayerName.equals("gato?"))
								playerName = "gato";
							else if (origPlayerName.equals("steph"))
								playerName = "stef";
							else if (origPlayerName.equals("i–aki"))
								playerName = "inaki";

							if (playerName != null && playerName.length() > 0) {
								System.out.println("DEBUG: Player #" + row
										+ ": " + playerName);
								tPlayers[row] = playerName;
								if (!playerName.equals("freers"))
									players.add(playerName);
							}
						}
					}

					for (CellEntry cell : cellFeed.getEntries()) {
						int col = cell.getCell().getCol();
						int row = cell.getCell().getRow();
						if (row > 1) {
							if (tPlayers[row] == null) {
								System.out.println("DEBUG: No more players");
								break;
							} else if (col == 1)
								System.out.println("DEBUG: Reading data for "
										+ tPlayers[row] + "...");

							if (row > 1 && col > 1 && col < 44
									&& (col - 1) % 3 == 0) {
								if (cell.getCell().getNumericValue() != null
										&& cell.getCell().getNumericValue()
												.intValue() == 1) {
									System.out.println("DEBUG: "
											+ tPlayers[row] + " < "
											+ tPlayers[(col - 1) / 3 + 1]);
									matches.add(new Pair<String>(tPlayers[row],
											tPlayers[(col - 1) / 3 + 1]));
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		players.add("mitad-de-tabla");

		System.out.println("Players: " + players + " (" + matches.size()
				+ " matches)");

		DirectedSparseGraph<String, Integer> g = new DirectedSparseGraph<String, Integer>();

		for (String player : players)
			g.addVertex(player);

		int edgeCount = players.size() * (players.size() - 1);
		Map<Pair<String>, Edge> edgesByMatch = new HashMap<Pair<String>, Edge>(
				edgeCount);
		final Map<Integer, Edge> edgesByNum = new HashMap<Integer, Edge>(
				edgeCount);

		int edgeNum = 1;
		for (String p1 : players)
			for (String p2 : players)
				if (!p1.equals(p2)) {
					Edge edge = new Edge(edgeNum);
					Pair<String> edgeName = new Pair<String>(p1, p2);
					edgesByMatch.put(edgeName, edge);
					edgesByNum.put(edgeNum, edge);
					g.addEdge(edgeNum, edgeName);
					edgeNum++;
				}

		for (Pair<String> match : matches)
			if (!players.contains(match.getFirst())
					|| !players.contains(match.getSecond()))
				matches.remove(match);

		for (Pair<String> match : matches) {
			edgesByMatch.get(match).incr();
			edgesByMatch.get(match).incr();
			edgesByMatch.get(
					new Pair<String>(match.getSecond(), match.getFirst()))
					.decr();
		}

		Transformer<Integer, Double> ew = new Transformer<Integer, Double>() {

			@Override
			public Double transform(Integer edgeNum) {
				return (double) (edgesByNum.get(edgeNum).weight())
						/ matches.size();
			}
		};

		return new TournamentHistory(g, ew, matches);
	}
}

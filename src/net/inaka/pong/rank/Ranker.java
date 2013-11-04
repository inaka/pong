package net.inaka.pong.rank;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import net.inaka.pong.rank.Match.Round;

import org.apache.commons.collections15.Transformer;

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

class Match {
	public enum Round {
		REGULAR, SEMIFINAL, FINAL, THIRD_PLACE
	}

	private String	winner;
	private String	loser;
	private Double	age;
	private Round	round;

	public Match(String loser, String winner, Double age) {
		this(loser, winner, Round.REGULAR, age);
	}

	public Match(String loser, String winner, Round round, Double age) {
		this.winner = winner;
		this.loser = loser;
		this.age = age;
		this.round = round;
	}

	public Match(Object[] data, Round round, Double age) {
		this.round = round;
		this.age = age;
		if (((Number) data[2]).intValue() < ((Number) data[3]).intValue()) {
			this.loser = data[0].toString();
			this.winner = data[1].toString();
		} else {
			this.loser = data[1].toString();
			this.winner = data[0].toString();
		}
		System.out.println("DEBUG: Special match: " + this);
	}

	public Round round() {
		return round;
	}

	public String winner() {
		return winner;
	}

	public String loser() {
		return loser;
	}

	public Double age() {
		return age;
	}

	public Pair<String> name() {
		return new Pair<String>(this.loser, this.winner);
	}

	@Override
	public String toString() {
		return this.winner + " def. " + this.loser + " (" + this.round + ")";
	}

}

class TournamentHistory {

	private DirectedSparseGraph<String, Integer>	graph;
	private Transformer<Integer, Double>			edgeWeights;
	private List<Match>								matches;

	public TournamentHistory(DirectedSparseGraph<String, Integer> g,
			Transformer<Integer, Double> ew, List<Match> matches) {
		this.graph = g;
		this.edgeWeights = ew;
		this.matches = matches;
	}

	/**
	 * @return the matches
	 */
	public List<Match> matches() {
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
		for (Entry<Integer, List<String>> entry : averages(th, true).entrySet()) {
			System.out.print(entry.getKey() + " " + entry.getValue() + "; ");
		}
		System.out.println();
		System.out.println();
		System.out.println("Averages (with iterations): ");
		for (Entry<Integer, List<String>> entry : averages2(th, false)
				.entrySet()) {
			System.out.print(entry.getKey() + " " + entry.getValue() + "; ");
		}
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
	 * @param includeFinalRound
	 * @return
	 */
	private static SortedMap<Integer, List<String>> averages(
			TournamentHistory th, boolean includeFinalRound) {
		TreeMap<String, Double> points = new TreeMap<String, Double>();
		TreeMap<String, Integer> games = new TreeMap<String, Integer>();

		for (Match match : th.matches()) {
			if (includeFinalRound || match.round() == Round.REGULAR) {
				points.put(match.loser(), 0.0);
				points.put(match.winner(), 0.0);
				games.put(match.loser(), 0);
				games.put(match.winner(), 0);
			}
		}
		for (Match match : th.matches()) {
			if (includeFinalRound || match.round() == Round.REGULAR) {
				points.put(match.winner(), points.get(match.winner()) + 1);
				games.put(match.loser(), games.get(match.loser()) + 1);
				games.put(match.winner(), games.get(match.winner()) + 1);
			}
		}

		SortedMap<Integer, List<String>> table = new TreeMap<Integer, List<String>>();

		for (String v : games.keySet()) {
			int score = new Double(points.get(v) / games.get(v) * 1000)
					.intValue();
			if (table.get(score) == null)
				table.put(score, new Vector<String>());
			table.get(score).add(v);
		}

		if (table.get(0) == null)
			table.put(0, new Vector<String>());
		table.get(0).add(">>total-looser<<");

		if (table.get(500) == null)
			table.put(500, new Vector<String>());
		table.get(500).add(">>average-player<<");

		if (table.get(1000) == null)
			table.put(1000, new Vector<String>());
		table.get(1000).add(">>master-of-pong<<");

		return table;
	}

	/**
	 * @param th
	 * @param includeFinalRound
	 * @return
	 */
	private static SortedMap<Integer, List<String>> averages2(
			TournamentHistory th, boolean includeFinalRound) {
		TreeMap<String, Double> points = new TreeMap<String, Double>();
		TreeMap<String, Integer> games = new TreeMap<String, Integer>();

		for (Match match : th.matches()) {
			points.put(match.winner(), points.get(match.winner()) == null ? 1
					: points.get(match.winner()) + 1);
			if (includeFinalRound || match.round() == Round.REGULAR) {
				games.put(match.loser(), games.get(match.loser()) == null ? 1
						: games.get(match.loser()) + 1);
				games.put(match.winner(), games.get(match.winner()) == null ? 1
						: games.get(match.winner()) + 1);
			}
		}

		TreeMap<String, Double> avgs = new TreeMap<String, Double>();
		for (String v : games.keySet()) {
			avgs.put(v,
					points.get(v) == null ? 0.0 : points.get(v) / games.get(v));
		}

		points.clear();

		for (Match match : th.matches()) {
			if (includeFinalRound || match.round() == Round.REGULAR) {
				double matchPoints = .5 + avgs.get(match.loser());
				if (avgs.get(match.loser()) < avgs.get(match.winner())
						&& matchPoints < 1)
					matchPoints = 1.0;
				points.put(match.winner(),
						points.get(match.winner()) == null ? matchPoints
								: points.get(match.winner()) + matchPoints);
			}
		}

		points.put(">>master-of-pong<<", 0.0);
		points.put(">>average-player<<", 0.0);
		for (String v : games.keySet()) {
			double matchPoints = .5 + avgs.get(v);
			double masterPoints = matchPoints < 1 ? 1 : matchPoints;
			points.put(">>master-of-pong<<", points.get(">>master-of-pong<<")
					+ masterPoints);
			points.put(">>average-player<<", points.get(">>average-player<<")
					+ masterPoints);
		}
		games.put(">>master-of-pong<<", games.size());
		games.put(">>average-player<<", games.size() * 2);

		SortedMap<Integer, List<String>> table = new TreeMap<Integer, List<String>>();

		for (String v : games.keySet()) {
			int score = points.get(v) == null ? 0 : new Double(points.get(v)
					/ games.get(v) * 1000).intValue();
			List<String> current = table.get(score);
			if (current == null)
				table.put(score, new Vector<String>());
			table.get(score).add(v);
		}

		if (table.get(0) == null)
			table.put(0, new Vector<String>());
		table.get(0).add(">>total-looser<<");

		return table;
	}

	private static TournamentHistory readHistory() {
		Set<String> players = new HashSet<String>();
		final List<Match> matches = new Vector<Match>();

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
			Double age = 0.0;
			int tournaments = 6;
			for (WorksheetEntry worksheet : worksheets) {
				if (tournaments == 0)
					break;
				tournaments--;

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
							else if (origPlayerName.endsWith("aki"))
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

							if (row > 1 && row < 16 && col > 1 && col < 44
									&& (col - 1) % 3 == 0) {
								if (cell.getCell().getNumericValue() != null
										&& cell.getCell().getNumericValue()
												.intValue() == 1) {
									System.out.println("DEBUG: "
											+ tPlayers[row] + " < "
											+ tPlayers[(col - 1) / 3 + 1]);
									matches.add(new Match(tPlayers[row],
											tPlayers[(col - 1) / 3 + 1], age));
								}
							}
						}
					}

					Object[] semifinal1 = new Object[4];
					Object[] semifinal2 = new Object[4];
					Object[] theFinal = new Object[4];
					Object[] thirdPlace = new Object[4];
					for (CellEntry cell : cellFeed.getEntries()) {
						int col = cell.getCell().getCol();
						int row = cell.getCell().getRow();

						if ((row == 21 || row == 22) && col == 2) {
							String playerName = cell.getTextContent()
									.getContent().getPlainText().toLowerCase();
							semifinal1[row - 21] = playerName;
						} else if ((row == 21 || row == 22) && col == 6) {
							semifinal1[row - 19] = cell.getCell()
									.getNumericValue();
						} else if ((row == 25 || row == 26) && col == 2) {
							String playerName = cell.getTextContent()
									.getContent().getPlainText().toLowerCase();
							semifinal2[row - 25] = playerName;
						} else if ((row == 25 || row == 26) && col == 6) {
							semifinal2[row - 23] = cell.getCell()
									.getNumericValue();
						} else if ((row == 23 || row == 24) && col == 8) {
							String playerName = cell.getTextContent()
									.getContent().getPlainText().toLowerCase();
							theFinal[row - 23] = playerName;
						} else if ((row == 23 || row == 24) && col == 12) {
							theFinal[row - 21] = cell.getCell()
									.getNumericValue();
						} else if ((row == 25 || row == 26) && col == 17) {
							String playerName = cell.getTextContent()
									.getContent().getPlainText().toLowerCase();
							thirdPlace[row - 25] = playerName;
						} else if ((row == 25 || row == 26) && col == 21) {
							thirdPlace[row - 23] = cell.getCell()
									.getNumericValue();
						}
					}

					if (semifinal1[2] != null && semifinal1[3] != null)
						matches.add(new Match(semifinal1, Round.SEMIFINAL, age));
					if (semifinal2[2] != null && semifinal2[3] != null)
						matches.add(new Match(semifinal2, Round.SEMIFINAL, age));
					if (theFinal[2] != null && theFinal[3] != null)
						matches.add(new Match(theFinal, Round.FINAL, age));
					if (thirdPlace[2] != null && thirdPlace[3] != null)
						matches.add(new Match(thirdPlace, Round.THIRD_PLACE,
								age));

					age += (0.5 / worksheets.size());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		players.add(">>average-player<<");

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

		for (Match match : matches)
			if (!players.contains(match.loser())
					|| !players.contains(match.winner()))
				matches.remove(match);

		for (Match match : matches) {
			edgesByMatch.get(match.name()).incr();
			edgesByMatch.get(match.name()).incr();
			switch (match.round()) {
			case REGULAR:
				// Deduce points to the looser
				edgesByMatch.get(
						new Pair<String>(match.winner(), match.loser())).decr();
				break;
			case SEMIFINAL:
				// Add two extra points to the winner
				edgesByMatch.get(match.name()).incr();
				edgesByMatch.get(match.name()).incr();
				break;
			case FINAL:
				// Add two extra points to the winner
				edgesByMatch.get(match.name()).incr();
				edgesByMatch.get(match.name()).incr();
				break;
			case THIRD_PLACE:
				// Add an extra point to the winner
				edgesByMatch.get(match.name()).incr();
				break;
			}
			// So, champion gets 4 extra points, runner up 2 extra points, 3rd 1
			// extra point and 4th remains unaffected
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

package net.inaka.pong.rank;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

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

class TournamentHistory {

	private DirectedSparseGraph<String, Integer>	graph;
	private Transformer<Integer, Double>			edgeWeights;

	public TournamentHistory(DirectedSparseGraph<String, Integer> g,
			Transformer<Integer, Double> ew) {
		this.graph = g;
		this.edgeWeights = ew;
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

		PageRank<String, Integer> pr = new PageRank<String, Integer>(
				th.graph(), th.edgeWeights(), 0.15);
		pr.evaluate();

		int sum = 0;
		Set<String> sortedVerticesSet = new HashSet<String>(th.graph()
				.getVertices());

		SortedMap<Integer, List<String>> table = new TreeMap<Integer, List<String>>();

		for (String v : sortedVerticesSet) {
			int score = new Double(pr.getVertexScore(v) * 100000).intValue();
			sum += score;
			List<String> current = table.get(score);
			if (current == null)
				table.put(score, new Vector<String>());
			table.get(score).add(v);
		}
		System.out.println(table);
		System.out.println("DEBUG: total = " + sum);
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
			boolean firstTournament = true;
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
							if (playerName != null && playerName.length() > 0) {
								System.out.println("DEBUG: Player #" + row
										+ ": " + playerName);
								tPlayers[row] = playerName;
								if (firstTournament)
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
				firstTournament = false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

/*		if(Math.random() > 0.5) players.add("pi–on fijo");
		if(Math.random() > 0.5) players.add("el loco dalla l’bera");
		if(Math.random() > 0.5) players.add("el mu–eco gallardo");
		if(Math.random() > 0.5) players.add("hubot");
		*/
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

		for (Pair<String> match : matches) {
			if (players.contains(match.getFirst())
					&& players.contains(match.getSecond())) {
				edgesByMatch.get(match).incr();
				edgesByMatch.get(match).incr();
				edgesByMatch.get(
						new Pair<String>(match.getSecond(), match.getFirst()))
						.decr();
			}
		}

		Transformer<Integer, Double> ew = new Transformer<Integer, Double>() {

			@Override
			public Double transform(Integer edgeNum) {
				return (double) (edgesByNum.get(edgeNum).weight())
						/ matches.size();
			}
		};

		return new TournamentHistory(g, ew);
	}
}

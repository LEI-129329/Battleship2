package battleship;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.*;

/**
 * Shot
 *
 * @author Nelss
 * Date: 20/02/2026
 * Time: 19:39
 */
public class Move implements IMove {

	//-------------------------------------------------------------------
	private final int number;
	private final List<IPosition> shots;
	private final List<IGame.ShotResult> shotResults;

	private record ShotSummary(
			int validShots,
			int repeatedShots,
			int missedShots,
			Map<String, Integer> sunkBoatsCount,
			Map<String, Integer> hitsPerBoat) {

		private int outsideShots() {
			return Game.NUMBER_SHOTS - validShots - repeatedShots;
		}
	}

	//-------------------------------------------------------------------
	public Move(int moveNumber, List<IPosition> moveShots, List<IGame.ShotResult> moveResults) {
		this.number = moveNumber;
		this.shots = moveShots;
		this.shotResults = moveResults;
	}

	@Override
	public String toString() {
		return "Move{" +
				"number=" + number +
				", shots=" + shots.size() +
				", results=" + shotResults.size() +
				'}';
	}

	@Override
	public int getNumber() {
		return this.number;
	}

	@Override
	public List<IPosition> getShots() {
		return this.shots;
	}

	@Override
	public List<IGame.ShotResult> getShotResults() {
		return this.shotResults;
	}

	/**
	 * Processes the results of enemy fire on the game board, analyzing the outcomes of shots,
	 * such as valid shots, repeated shots, missed shots, hits on ships, and sunk ships. It can
	 * also display a detailed summary of the shot results if verbose mode is activated.
	 *
	 * @param verbose a boolean indicating whether a detailed summary should be printed to the console
	 *                for the processed enemy fire data.
	 * @return a JSON-formatted string that encapsulates the results, including counts of valid shots,
	 *         repeated shots, missed shots, shots outside the game board, and details of hits and
	 *         sunk ships.
	 */
	@Override
	public String processEnemyFire(boolean verbose) {
		ShotSummary summary = summarizeResults();

		if (verbose) {
			System.out.println("Jogada nº" + this.number + " -> " + formatSummary(summary));
		}

		String jsonString = serializeResponse(buildResponse(summary));
		System.out.println(jsonString);
		System.out.println();
		return jsonString;
	}

	private ShotSummary summarizeResults() {
		int validShots = 0;
		int repeatedShots = 0;
		int missedShots = 0;
		Map<String, Integer> sunkBoatsCount = new HashMap<>();
		Map<String, Integer> hitsPerBoat = new HashMap<>();

		for (IGame.ShotResult result : this.shotResults) {
			if (!result.valid()) {
				continue;
			}

			if (result.repeated()) {
				repeatedShots++;
				continue;
			}

			validShots++;
			if (result.ship() == null) {
				missedShots++;
				continue;
			}

			String boatName = result.ship().getCategory();
			hitsPerBoat.put(boatName, hitsPerBoat.getOrDefault(boatName, 0) + 1);
			if (result.sunk()) {
				sunkBoatsCount.put(boatName, sunkBoatsCount.getOrDefault(boatName, 0) + 1);
			}
		}

		return new ShotSummary(validShots, repeatedShots, missedShots, sunkBoatsCount, hitsPerBoat);
	}

	private String formatSummary(ShotSummary summary) {
		StringBuilder output = new StringBuilder();

		if (summary.validShots() == 0 && summary.repeatedShots() > 0) {
			appendRepeatedShots(output, summary.repeatedShots());
		} else {
			if (summary.validShots() > 0) {
				output.append(summary.validShots()).append(" tiro")
						.append(summary.validShots() > 1 ? "s" : "")
						.append(" válido")
						.append(summary.validShots() > 1 ? "s" : "")
						.append(": ");
			}

			appendSunkBoats(output, summary.sunkBoatsCount());
			appendHitsOnBoats(output, summary.hitsPerBoat(), summary.sunkBoatsCount());

			if (summary.missedShots() > 0) {
				output.append(summary.missedShots()).append(" tiro")
						.append(summary.missedShots() > 1 ? "s" : "")
						.append(" na água");
			} else if (!summary.sunkBoatsCount().isEmpty() || !summary.hitsPerBoat().isEmpty()) {
				output.setLength(output.length() - 2);
			}

			if (summary.repeatedShots() > 0) {
				if (summary.validShots() > 0) {
					output.append(", ");
				}
				appendRepeatedShots(output, summary.repeatedShots());
			}
		}

		appendOutsideShots(output, summary.outsideShots());
		return output.toString();
	}

	private void appendSunkBoats(StringBuilder output, Map<String, Integer> sunkBoatsCount) {
		for (Map.Entry<String, Integer> entry : sunkBoatsCount.entrySet()) {
			String boatName = entry.getKey();
			int count = entry.getValue();
			output.append(count).append(" ").append(boatName)
					.append(count > 1 ? "s" : "")
					.append(" ao fundo").append(" + ");
		}
	}

	private void appendHitsOnBoats(
			StringBuilder output,
			Map<String, Integer> hitsPerBoat,
			Map<String, Integer> sunkBoatsCount) {
		for (Map.Entry<String, Integer> entry : hitsPerBoat.entrySet()) {
			String boatName = entry.getKey();
			int hits = entry.getValue();
			if (!sunkBoatsCount.containsKey(boatName)) {
				output.append(hits).append(" tiro").append(hits > 1 ? "s" : "")
						.append(" num(a) ").append(boatName).append(" + ");
			}
		}
	}

	private void appendRepeatedShots(StringBuilder output, int repeatedShots) {
		output.append(repeatedShots).append(" tiro")
				.append(repeatedShots > 1 ? "s" : "")
				.append(" repetido")
				.append(repeatedShots > 1 ? "s" : "");
	}

	private void appendOutsideShots(StringBuilder output, int outsideShots) {
		if (outsideShots > 0) {
			if (!output.isEmpty()) {
				output.append(", ");
			}
			output.append(outsideShots).append(" tiro")
					.append(outsideShots > 1 ? "s" : "")
					.append(" exterior")
					.append(outsideShots > 1 ? "es" : "");
		}
	}

	private Map<String, Object> buildResponse(ShotSummary summary) {
		Map<String, Object> response = new HashMap<>();
		response.put("validShots", summary.validShots());
		response.put("outsideShots", summary.outsideShots());
		response.put("repeatedShots", summary.repeatedShots());
		response.put("missedShots", summary.missedShots());
		List<Map<String, Object>> sunkBoats = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : summary.sunkBoatsCount().entrySet()) {
			Map<String, Object> boat = new HashMap<>();
			boat.put("type", entry.getKey());
			boat.put("count", entry.getValue());
			sunkBoats.add(boat);
		}
		response.put("sunkBoats", sunkBoats);

		List<Map<String, Object>> boatHits = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : summary.hitsPerBoat().entrySet()) {
			if (!summary.sunkBoatsCount().containsKey(entry.getKey())) {
				Map<String, Object> boat = new HashMap<>();
				boat.put("type", entry.getKey());
				boat.put("hits", entry.getValue());
				boatHits.add(boat);
			}
		}
		response.put("hitsOnBoats", boatHits);

		return response;
	}

	private String serializeResponse(Map<String, Object> response) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		try {
			return objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Erro ao serializar o JSON dos resultados da jogada", e);
		}
	}

	protected ObjectMapper createObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		return objectMapper;
	}
}
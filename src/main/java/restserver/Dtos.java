package restserver;

import java.util.List;

/**
 * Data Transfer Objects (DTOs) for the Battleship REST protocol.
 *
 * These are plain Java classes (POJOs). Spring Boot + Jackson automatically
 * serialises them to/from JSON — no extra annotations needed for basic use.
 *
 * Kept in a single file so students have one place to read the full contract.
 */
public class Dtos {

	// =========================================================================
	// m0 — Registration
	// =========================================================================

	/**
	 * m0 REQUEST  →  POST /register
	 *
	 * Example JSON:
	 * {
	 *   "playerName": "Alice",
	 *   "callbackUrl": "http://student-host:9090"
	 * }
	 */
	public static class RegistrationRequest {
		private String playerName;
		private String callbackUrl;

		public String getPlayerName() { return playerName; }
		public void setPlayerName(String playerName) { this.playerName = playerName; }
		public String getCallbackUrl() { return callbackUrl; }
		public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
	}

	/**
	 * m0 RESPONSE  ←  200 OK
	 *
	 * Example JSON:
	 * {
	 *   "gameId":      "550e8400-e29b-41d4-a716-446655440000",
	 *   "status":      "REGISTERED",
	 *   "shotsPerTurn": 3,
	 *   "message":     "Game ready. You shoot first."
	 * }
	 */
	public static class RegistrationResponse {
		private String gameId;
		private String status;
		private int shotsPerTurn;
		private String message;

		public String getGameId() { return gameId; }
		public void setGameId(String gameId) { this.gameId = gameId; }
		public String getStatus() { return status; }
		public void setStatus(String status) { this.status = status; }
		public int getShotsPerTurn() { return shotsPerTurn; }
		public void setShotsPerTurn(int shotsPerTurn) { this.shotsPerTurn = shotsPerTurn; }
		public String getMessage() { return message; }
		public void setMessage(String message) { this.message = message; }
	}

	// =========================================================================
	// m1a / m2a — Shot list  (same structure in both directions)
	// =========================================================================

	/**
	 * m1a REQUEST  →  POST /game/{gameId}/shots      (student → AI)
	 * m2a REQUEST  →  POST {callbackUrl}/game/{gameId}/shots  (AI → student)
	 *
	 * Each position uses the classic notation: row = "A".."J", column = 1..10
	 *
	 * Example JSON:
	 * {
	 *   "shots": [
	 *     { "row": "B", "column": 3 },
	 *     { "row": "G", "column": 7 },
	 *     { "row": "D", "column": 10 }
	 *   ]
	 * }
	 */
	public static class ShotRequest {
		private List<ShotPosition> shots;

		public List<ShotPosition> getShots() { return shots; }
		public void setShots(List<ShotPosition> shots) { this.shots = shots; }
	}

	/** A single position in classic notation. */
	public static class ShotPosition {
		private String row;     // "A" .. "J"
		private int column;     // 1  .. 10

		public String getRow() { return row; }
		public void setRow(String row) { this.row = row; }
		public int getColumn() { return column; }
		public void setColumn(int column) { this.column = column; }
	}

	// =========================================================================
	// m1b / m2b — Cumulative shot results  (same structure in both directions)
	// =========================================================================

	/**
	 * m1b RESPONSE  ←  200 OK  (AI → student, result of student's shots)
	 * m2b RESPONSE  ←  200 OK  (student → AI, result of AI's shots)
	 *
	 * Example JSON:
	 * {
	 *   "results": [
	 *     { "row": "B", "column": 3,  "outcome": "MISS" },
	 *     { "row": "G", "column": 7,  "outcome": "HIT",  "shipType": "caravela" },
	 *     { "row": "D", "column": 10, "outcome": "SUNK", "shipType": "galeao"   }
	 *   ],
	 *   "shipsRemaining": 8,
	 *   "gameStatus": "ONGOING"
	 * }
	 *
	 * gameStatus values: "ONGOING" | "GAME_OVER"
	 * outcome values:    "MISS"    | "HIT" | "SUNK" | "REPEATED" | "INVALID"
	 */
	public static class ShotResponse {
		private List<ShotResult> results;
		private int shipsRemaining;
		private String gameStatus;
		private String winner;          // null when ONGOING; "AI_WINS" or "STUDENT_WINS" when GAME_OVER

		public List<ShotResult> getResults() { return results; }
		public void setResults(List<ShotResult> results) { this.results = results; }
		public int getShipsRemaining() { return shipsRemaining; }
		public void setShipsRemaining(int shipsRemaining) { this.shipsRemaining = shipsRemaining; }
		public String getGameStatus() { return gameStatus; }
		public void setGameStatus(String gameStatus) { this.gameStatus = gameStatus; }
		public String getWinner() { return winner; }
		public void setWinner(String winner) { this.winner = winner; }
	}

	/** Result for one individual shot. */
	public static class ShotResult {
		private String row;
		private int column;
		private String outcome;   // "MISS" | "HIT" | "SUNK" | "REPEATED" | "INVALID"
		private String shipType;  // null unless outcome is HIT or SUNK

		public String getRow() { return row; }
		public void setRow(String row) { this.row = row; }
		public int getColumn() { return column; }
		public void setColumn(int column) { this.column = column; }
		public String getOutcome() { return outcome; }
		public void setOutcome(String outcome) { this.outcome = outcome; }
		public String getShipType() { return shipType; }
		public void setShipType(String shipType) { this.shipType = shipType; }
	}

	// =========================================================================
	// Error response
	// =========================================================================

	/**
	 * Returned with HTTP 4xx when something goes wrong.
	 *
	 * Example JSON:
	 * { "error": "Game not found: abc-123" }
	 */
	public static class ErrorResponse {
		private String error;

		public ErrorResponse() {}
		public ErrorResponse(String error) { this.error = error; }
		public String getError() { return error; }
		public void setError(String error) { this.error = error; }
	}
}
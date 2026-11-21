package battleship;

import java.util.List;

/**
 * Maps fleets and shot moves into the character matrix used by the board views.
 */
public class BoardCellMapper
{
	public static final char EMPTY_MARKER = '.';
	public static final char SHIP_MARKER = '#';
	public static final char SHOT_SHIP_MARKER = '*';
	public static final char SHOT_WATER_MARKER = 'o';
	public static final char SHIP_ADJACENT_MARKER = '-';

	public static char[][] createMap(
			int boardSize,
			IFleet fleet,
			List<IMove> moves,
			boolean showShots)
	{
		char[][] map = new char[boardSize][boardSize];

		for (int row = 0; row < boardSize; row++)
			for (int column = 0; column < boardSize; column++)
				map[row][column] = EMPTY_MARKER;

		mapShips(fleet, map);
		if (showShots)
			mapShots(moves, map);

		return map;
	}

	private static void mapShips(IFleet fleet, char[][] map)
	{
		if (fleet == null)
			return;

		for (IShip ship : fleet.getShips())
		{
			for (IPosition shipPosition : ship.getPositions())
				map[shipPosition.getRow()][shipPosition.getColumn()] = SHIP_MARKER;

			if (!ship.stillFloating())
				for (IPosition adjacentPosition : ship.getAdjacentPositions())
					map[adjacentPosition.getRow()][adjacentPosition.getColumn()] =
							SHIP_ADJACENT_MARKER;
		}
	}

	private static void mapShots(List<IMove> moves, char[][] map)
	{
		if (moves == null)
			return;

		for (IMove move : moves)
		{
			for (IPosition shot : move.getShots())
			{
				if (shot.isInside())
				{
					int row = shot.getRow();
					int column = shot.getColumn();

					if (map[row][column] == SHIP_MARKER)
						map[row][column] = SHOT_SHIP_MARKER;

					if (map[row][column] == EMPTY_MARKER
							|| map[row][column] == SHIP_ADJACENT_MARKER)
						map[row][column] = SHOT_WATER_MARKER;
				}
			}
		}
	}
}

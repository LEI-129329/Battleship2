package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for {@link IMove}.
 * Author: nelss
 * Date: 2026-11-13 15:37
 * Cyclomatic Complexity:
 * - readMove(): 2
 */
class IMoveTest {

	@Test
	@DisplayName("Lê uma jogada sem tiros")
	void readMove1() {
		try (Scanner scanner = new Scanner("0")) {
			Move move = IMove.readMove(4, scanner);

			assertAll("jogada sem tiros",
					() -> assertEquals(4, move.getNumber(),
							"Erro: a jogada deve conservar o número indicado."),
					() -> assertEquals(0, move.getShots().size(),
							"Erro: uma jogada com zero tiros não deve conter posições."),
					() -> assertEquals(0, move.getShotResults().size(),
							"Erro: os resultados devem iniciar vazios."));
		}
	}

	@Test
	@DisplayName("Lê todos os tiros de uma jogada")
	void readMove2() {
		try (Scanner scanner = new Scanner("2 3 5 7 8")) {
			Move move = IMove.readMove(6, scanner);

			assertAll("jogada com tiros",
					() -> assertEquals(6, move.getNumber(),
							"Erro: a jogada deve conservar o número indicado."),
					() -> assertEquals(2, move.getShots().size(),
							"Erro: devem ser lidas as duas posições indicadas."),
					() -> assertEquals(new Position(3, 5), move.getShots().get(0),
							"Erro: a primeira posição deve usar a linha e coluna lidas."),
					() -> assertEquals(new Position(7, 8), move.getShots().get(1),
							"Erro: a segunda posição deve usar a linha e coluna lidas."),
					() -> assertEquals(0, move.getShotResults().size(),
							"Erro: os resultados devem iniciar vazios."));
		}
	}
}

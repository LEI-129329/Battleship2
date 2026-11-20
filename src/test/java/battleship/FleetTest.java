package battleship;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Test class for Fleet.
 * Author: 101283
 */
public class FleetTest {

	private Fleet fleet;
	private final PrintStream originalOut = System.out;
	private ByteArrayOutputStream out;

	@BeforeEach
	void setUp() {
		fleet = new Fleet();
		out = new ByteArrayOutputStream();
		System.setOut(new PrintStream(out));
	}

	@AfterEach
	void tearDown() {
		System.setOut(originalOut);
		fleet = null;
	}

	// ------------------------------------------------------------ constructor

	@Test
	@DisplayName("Construtor cria uma frota vazia")
	void testConstructor() {
		assertNotNull(fleet, "Error: Instance of Fleet should not be null.");
		assertTrue(fleet.getShips().isEmpty(), "Error: Fleet should be initialized with empty ships list.");
	}

	// ----------------------------------------------------------- createRandom

	@Test
	@DisplayName("createRandom devolve uma frota com o número correto de navios")
	void testCreateRandom() {
		IFleet r = Fleet.createRandom();
		assertNotNull(r, "Error: createRandom should return a non-null fleet instance.");
		assertEquals(Fleet.FLEET_SIZE, r.getShips().size(), "Error: createRandom should create a fleet with the correct number of ships.");
	}

	@Test
	@DisplayName("createRandom devolve navios dentro do tabuleiro e sem colisões")
	void testCreateRandomShipsAreValid() {
		List<IShip> ships = Fleet.createRandom().getShips();
		for (int i = 0; i < ships.size(); i++) {
			assertTrue(ships.get(i).isInsideBoard());
			for (int j = i + 1; j < ships.size(); j++)
				assertFalse(ships.get(i).tooCloseTo(ships.get(j)));
		}
	}

	// --------------------------------------------------------------- addShip

	@Test
	@DisplayName("addShip adiciona um navio válido")
	void testAddShip1() {
		IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
		assertTrue(fleet.addShip(ship), "Error: Valid ship should be added successfully.");
		assertEquals(1, fleet.getShips().size(), "Error: Fleet should contain one ship after addition.");
	}

	@Test
	@DisplayName("addShip rejeita navio quando a frota já excede FLEET_SIZE")
	void testAddShip2() {
		// getShips() devolve a lista interna: enche-a para além do limite
		IShip filler = new Barge(Compass.NORTH, new Position(0, 0));
		for (int i = 0; i <= Fleet.FLEET_SIZE; i++)
			fleet.getShips().add(filler);

		IShip anotherShip = new Barge(Compass.NORTH, new Position(8, 8));
		assertFalse(fleet.addShip(anotherShip), "Error: Should not add ship when fleet size limit is reached.");
	}

	@Test
	@DisplayName("addShip rejeita navio fora do tabuleiro")
	void testAddShip3() {
		IShip shipOutside = new Barge(Compass.NORTH, new Position(99, 99));
		assertFalse(fleet.addShip(shipOutside), "Error: Should not add ship outside the board.");
	}

	@Test
	@DisplayName("addShip rejeita navio com risco de colisão")
	void testAddShip4() {
		IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		IShip ship2 = new Barge(Compass.NORTH, new Position(1, 1));  // Overlapping position
		fleet.addShip(ship1);
		assertFalse(fleet.addShip(ship2), "Error: Should not add ship with a collision risk.");
	}

	@Test
	@DisplayName("addShip(null) lança AssertionError")
	void testAddShipNull() {
		assertThrows(AssertionError.class, () -> fleet.addShip(null));
	}

	// --------------------------------------------------------------- getShips

	@Test
	@DisplayName("getShips devolve os navios adicionados")
	void testGetShips() {
		assertTrue(fleet.getShips().isEmpty(), "Error: Fleet's ships list should initially be empty.");
		IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
		fleet.addShip(ship);
		assertEquals(1, fleet.getShips().size(), "Error: Fleet should have size 1 after adding a ship.");
		assertEquals(ship, fleet.getShips().get(0), "Error: Fleet's first ship should match the added ship.");
	}

	// ----------------------------------------------------------- getShipsLike

	@Test
	@DisplayName("getShipsLike devolve só os navios da categoria pedida")
	void testGetShipsLike() {
		IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		IShip ship2 = new Caravel(Compass.NORTH, new Position(4, 4));
		fleet.addShip(ship1);
		fleet.addShip(ship2);

		List<IShip> barges = fleet.getShipsLike("Barca");
		assertEquals(1, barges.size(), "Error: There should be exactly one ship of category 'Barca'.");
		assertEquals(ship1, barges.get(0), "Error: The ship of category 'Barca' does not match.");
	}

	@Test
	@DisplayName("getShipsLike devolve lista vazia se não houver a categoria")
	void testGetShipsLikeNoMatch() {
		fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1)));
		assertTrue(fleet.getShipsLike("Galeao").isEmpty());
	}

	@Test
	@DisplayName("getShipsLike(null) lança AssertionError")
	void testGetShipsLikeNull() {
		assertThrows(AssertionError.class, () -> fleet.getShipsLike(null));
	}

	// ------------------------------------------------ getFloatingShips / Sunk

	@Test
	@DisplayName("getFloatingShips devolve só os navios a flutuar")
	void testGetFloatingShips() {
		IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		IShip ship2 = new Caravel(Compass.NORTH, new Position(4, 4));
		fleet.addShip(ship1);
		fleet.addShip(ship2);

		List<IShip> floatingShips = fleet.getFloatingShips();
		assertEquals(2, floatingShips.size(), "Error: All ships should be floating initially.");

		ship1.getPositions().get(0).shoot();  // Sink ship1
		floatingShips = fleet.getFloatingShips();
		assertEquals(1, floatingShips.size(), "Error: Only one ship should be floating after sinking one.");
		assertEquals(ship2, floatingShips.get(0), "Error: The floating ship should match the expected result.");
	}

	@Test
	@DisplayName("getSunkShips devolve só os navios afundados")
	void testGetSunkShips() {
		IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		IShip ship2 = new Caravel(Compass.NORTH, new Position(4, 4));
		fleet.addShip(ship1);
		fleet.addShip(ship2);

		assertTrue(fleet.getSunkShips().isEmpty(), "Error: No ship should be sunk initially.");

		ship1.getPositions().get(0).shoot();  // Sink ship1
		List<IShip> sunk = fleet.getSunkShips();
		assertEquals(1, sunk.size());
		assertEquals(ship1, sunk.get(0));
	}

	// ----------------------------------------------------------------- shipAt

	@Test
	@DisplayName("shipAt devolve o navio na posição ou null se estiver vazia")
	void testShipAt() {
		IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
		fleet.addShip(ship);

		assertEquals(ship, fleet.shipAt(new Position(1, 1)), "Error: Should return the correct ship at the position.");
		assertNull(fleet.shipAt(new Position(5, 5)), "Error: Should return null for empty positions in the fleet.");
	}

	@Test
	@DisplayName("shipAt(null) lança AssertionError")
	void testShipAtNull() {
		assertThrows(AssertionError.class, () -> fleet.shipAt(null));
	}

	// ------------------------------------------------------------ collisionRisk

	@Test
	@DisplayName("collisionRisk (privado) deteta navios sobrepostos")
	void testcollisionRisk() throws Exception {
		var method = Fleet.class.getDeclaredMethod("collisionRisk", IShip.class);
		method.setAccessible(true);

		IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		IShip ship2 = new Barge(Compass.NORTH, new Position(1, 1));  // Overlapping position
		fleet.addShip(ship1);

		assertTrue((Boolean) method.invoke(fleet, ship2), "Error: Overlapping ships should be at collision risk.");
		assertFalse((Boolean) method.invoke(fleet, new Barge(Compass.NORTH, new Position(5, 5))),
				"Error: Ships at non-overlapping positions should not have a collision risk.");
	}

	@Test
	@DisplayName("collisionRisk (privado) com null lança AssertionError")
	void testcollisionRiskNull() throws Exception {
		var method = Fleet.class.getDeclaredMethod("collisionRisk", IShip.class);
		method.setAccessible(true);

		var ex = assertThrows(InvocationTargetException.class,
				() -> method.invoke(fleet, (Object) null));
		assertInstanceOf(AssertionError.class, ex.getCause());
	}

	// ------------------------------------------------------------- impressões

	@Test
	@DisplayName("printShips imprime cada navio da lista")
	void testPrintShips() {
		IShip a = new Barge(Compass.NORTH, new Position(1, 1));
		IShip b = new Barge(Compass.NORTH, new Position(7, 7));
		Fleet.printShips(List.of(a, b));

		String printed = out.toString();
		assertTrue(printed.contains(a.toString()));
		assertTrue(printed.contains(b.toString()));
	}

	@Test
	@DisplayName("printShips(null) lança AssertionError")
	void testPrintShipsNull() {
		assertThrows(AssertionError.class, () -> Fleet.printShips(null));
	}

	@Test
	@DisplayName("printStatus mostra navios a flutuar e afundados")
	void testPrintStatus() {
		IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		IShip ship2 = new Barge(Compass.NORTH, new Position(7, 7));
		fleet.addShip(ship1);
		fleet.addShip(ship2);
		ship1.getPositions().get(0).shoot();

		fleet.printStatus();

		assertTrue(out.toString().contains("Estado da Frota: 1 a flutuar, 1 afundados!"));
	}

	@Test
	@DisplayName("printShipsByCategory imprime só os navios da categoria")
	void testPrintShipsByCategory() {
		IShip b = new Barge(Compass.NORTH, new Position(1, 1));
		IShip c = new Caravel(Compass.NORTH, new Position(4, 4));
		fleet.addShip(b);
		fleet.addShip(c);

		fleet.printShipsByCategory("Barca");

		String printed = out.toString();
		assertTrue(printed.contains(b.toString()));
		assertFalse(printed.contains(c.toString()));
	}

	@Test
	@DisplayName("printShipsByCategory(null) lança AssertionError")
	void testPrintShipsByCategoryNull() {
		assertThrows(AssertionError.class, () -> fleet.printShipsByCategory(null));
	}

	@Test
	@DisplayName("printFloatingShips imprime só os navios a flutuar")
	void testPrintFloatingShips() {
		IShip sunk = new Barge(Compass.NORTH, new Position(1, 1));
		IShip floating = new Barge(Compass.NORTH, new Position(7, 7));
		fleet.addShip(sunk);
		fleet.addShip(floating);
		sunk.getPositions().get(0).shoot();

		fleet.printFloatingShips();

		String printed = out.toString();
		assertTrue(printed.contains(floating.toString()));
		assertFalse(printed.contains(sunk.toString()));
	}

	@Test
	@DisplayName("printAllShips imprime todos os navios da frota")
	void testPrintAllShips() {
		IShip a = new Barge(Compass.NORTH, new Position(1, 1));
		IShip b = new Barge(Compass.NORTH, new Position(7, 7));
		fleet.addShip(a);
		fleet.addShip(b);

		fleet.printAllShips();

		String printed = out.toString();
		assertTrue(printed.contains(a.toString()));
		assertTrue(printed.contains(b.toString()));
	}

	@Test
	@DisplayName("createRandom deve ignorar navio se addShip retornar false (frota cheia/duplicada)")
	void createRandom_ignoresShipIfAddFails() {
		// 1. Prepara a frota para que addShip retorne false (ex: enche a frota até ao limite)
		// ... (código para encher a fleet ou adicionar um navio duplicado)

		// 2. Chama o método que contém o código amarelo
		// fleet.createRandom(); (ou o nome do método que estás a testar)

		// 3. Asserta que o tamanho não incrementou com o navio rejeitado
	}
}
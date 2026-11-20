package restserver;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PdfReportGenerator}.
 * Author: Copilot
 * Date: 2026-11-19
 * Class under test: PdfReportGenerator
 * Cyclomatic complexity for generateReport: 5
 */
class PdfReportGeneratorTest {

    @TempDir
    Path tempDirectory;

    @Test
    @DisplayName("Constructs a report generator")
    void constructorCreatesInstance() {
        assertNotNull(new PdfReportGenerator(), "The public default constructor should create an instance.");
    }

    @Test
    @DisplayName("Generates a readable report with winner and all recorded moves")
    void generateReportIncludesWinnerAndMoveHistory() throws IOException {
        GameSession session = new GameSession("game-17", "Ada", "http://localhost:8080");
        session.markStudentWins();
        session.addMoveRecord("Shot 1: A1");
        session.addMoveRecord("Shot 2: B2");
        Path destination = tempDirectory.resolve("completed-game.pdf");

        PdfReportGenerator.generateReport(session, destination.toString());

        String report = readPdfText(destination);
        assertTrue(Files.size(destination) > 0, "A non-empty PDF should be created.");
        assertTrue(report.contains("Relatorio de Jogo - Batalha Naval"),
                "The PDF should contain the report title.");
        assertTrue(report.contains("ID do Jogo: game-17"), "The PDF should contain the game ID.");
        assertTrue(report.contains("Jogador: Ada"), "The PDF should contain the player name.");
        assertTrue(report.contains("URL de Callback: http://localhost:8080"),
                "The PDF should contain the callback URL.");
        assertTrue(report.contains("Vencedor: STUDENT_WINS"),
                "The PDF should contain the declared winner.");
        assertTrue(report.contains("--- Histórico de Jogadas ---"),
                "The PDF should contain the move-history heading.");
        assertTrue(report.contains("Shot 1: A1"), "The PDF should contain the first recorded move.");
        assertTrue(report.contains("Shot 2: B2"), "The PDF should contain the second recorded move.");
    }

    @Test
    @DisplayName("Generates an in-progress report when there is no winner or move history")
    void generateReportIncludesInProgressStateAndEmptyHistory() throws IOException {
        GameSession session = new GameSession("game-18", "Grace", "https://player.example/callback");
        Path destination = tempDirectory.resolve("in-progress-game.pdf");

        PdfReportGenerator.generateReport(session, destination.toString());

        String report = readPdfText(destination);
        assertTrue(report.contains("ID do Jogo: game-18"), "The PDF should contain the game ID.");
        assertTrue(report.contains("Vencedor: A decorrer"),
                "A session without a winner should be described as in progress.");
        assertTrue(report.contains("--- Histórico de Jogadas ---"),
                "The PDF should include the history heading when the history is empty.");
        assertFalse(report.contains("Shot"), "An empty history should not produce move entries.");
    }

    @Test
    @DisplayName("Does not create an output file when its parent directory is missing")
    void generateReportHandlesUnavailableDestination() {
        Path missingParent = tempDirectory.resolve("missing-parent");
        Path destination = missingParent.resolve("report.pdf");

        assertDoesNotThrow(() -> PdfReportGenerator.generateReport(
                        new GameSession("game-19", "Lin", "http://localhost"), destination.toString()),
                "The generator catches file creation failures rather than propagating them.");
        assertFalse(Files.exists(missingParent), "The generator should not create missing directories.");
        assertFalse(Files.exists(destination), "No PDF should exist when the destination cannot be opened.");
    }

    @Test
    @DisplayName("Closes and preserves a readable PDF when session access fails after opening")
    void generateReportClosesDocumentAfterSessionFailure() throws IOException {
        GameSession session = new GameSession("game-20", "unused", "http://localhost") {
            @Override
            public String getPlayerName() {
                throw new IllegalStateException("Simulated session failure after document open.");
            }
        };
        Path destination = tempDirectory.resolve("session-failure.pdf");

        assertDoesNotThrow(() -> PdfReportGenerator.generateReport(session, destination.toString()),
                "The generator catches session exceptions and closes the opened document.");

        String report = readPdfText(destination);
        assertTrue(report.contains("Relatorio de Jogo - Batalha Naval"),
                "The partial report should be finalized as a readable PDF.");
        assertTrue(report.contains("ID do Jogo: game-20"),
                "The report should retain content written before the session failure.");
        assertFalse(report.contains("Jogador:"),
                "The failing session value and subsequent fields should not be written.");
    }

    @Test
    @DisplayName("Runs final cleanup if reporting a generation failure also fails")
    @ResourceLock(Resources.SYSTEM_ERR)
    void generateReportRunsCleanupWhenFailureReportingThrows() throws IOException {
        PrintStream originalError = System.err;
        PrintStream failingError = new PrintStream(new ByteArrayOutputStream()) {
            @Override
            public void println(Object value) {
                throw new IllegalStateException("Simulated error stream failure.");
            }
        };
        Path destination = tempDirectory.resolve("error-reporting-failure.pdf");
        Path missingDestination = tempDirectory.resolve("missing-parent").resolve("report.pdf");

        try {
            System.setErr(failingError);
            GameSession session = new GameSession("game-21", "unused", "http://localhost") {
                @Override
                public String getPlayerName() {
                    throw new IllegalArgumentException("Simulated session failure after document open.");
                }
            };

            assertThrows(IllegalStateException.class,
                    () -> PdfReportGenerator.generateReport(session, destination.toString()),
                    "The error-stream failure should propagate after the opened document is closed.");
            assertThrows(IllegalStateException.class,
                    () -> PdfReportGenerator.generateReport(
                            new GameSession("game-22", "Lin", "http://localhost"),
                            missingDestination.toString()),
                    "The error-stream failure should propagate when the document never opened.");
        } finally {
            System.setErr(originalError);
            failingError.close();
        }

        String report = readPdfText(destination);
        assertTrue(report.contains("ID do Jogo: game-21"),
                "The exceptional cleanup path should finalize the partially generated PDF.");
        assertFalse(Files.exists(missingDestination),
                "A failed destination should remain absent when the document never opened.");
    }

    private String readPdfText(Path pdfPath) throws IOException {
        PdfReader reader = new PdfReader(Files.readAllBytes(pdfPath));
        try {
            return new PdfTextExtractor(reader).getTextFromPage(1);
        } finally {
            reader.close();
        }
    }
}

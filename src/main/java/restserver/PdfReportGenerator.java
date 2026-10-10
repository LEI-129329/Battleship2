package restserver;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.FileOutputStream;

public class PdfReportGenerator {

    /**
     * Gera um ficheiro PDF com o resumo da sessão de jogo
     *
     * @param session A sessão de jogo atual
     * @param destPath O caminho onde o ficheiro PDF será gravado
     */
    public static void generateReport(GameSession session, String destPath) {
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, new FileOutputStream(destPath));

            document.open();
            document.add(new Paragraph("Relatorio de Jogo - Batalha Naval"));
            document.add(new Paragraph("ID do Jogo: " + session.getGameId()));
            document.add(new Paragraph("Jogador: " + session.getPlayerName()));
            document.add(new Paragraph("URL de Callback: " + session.getCallbackUrl()));
            document.add(new Paragraph("Vencedor: " + (session.getWinner() != null ? session.getWinner() : "A decorrer")));
            document.add(new Paragraph("--------------------------------------------------"));
            document.add(new Paragraph("--- Histórico de Jogadas ---"));
            for (String move : session.getMoveHistory()) {
                document.add(new Paragraph(move));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }
}
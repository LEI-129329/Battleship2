package battleship;


//Imports necessarios, javaFx foi a dependencia usada apartir do repositorio maven.
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.List;

public class BattleshipGraphicalView {

    // Constantes -> Usadas para representar o tabuleiro, BOARD_SIZE -> indica o tamanho do tabuleiro , os outros argumentos servem para indicar em cada posição o objeto presente(ex: se foi  um tiro na agua)
    public static final int BOARD_SIZE = 10;
    private static final char EMPTY_MARKER = '.';
    private static final char SHIP_MARKER = '#';
    private static final char SHOT_SHIP_MARKER = '*';
    private static final char SHOT_WATER_MARKER = 'o';
    private static final char SHIP_ADJACENT_MARKER = '-';

    // Variável para controlar o arranque único do motor JavaFX
    private static boolean isJavaFXStarted = false;

    //Função que inicia o display
    public static void display(String title, IFleet fleet, List<IMove> moves, boolean showShots) {
        //Inicializa o JavaFX de forma segura e impede que o motor morra ao fechar a janela , vamos ter esta thread a correr em segundo plano, possiblitando a abertura da mesma mais que uma vez durante o programa.
        if (!isJavaFXStarted) {
            try {
                Platform.startup(() -> {
                    Platform.setImplicitExit(false); // Mantém o motor ativo para poderes reabrir a janela , false -> "the application will continue to run normally even after the last window is closed"
                });

                isJavaFXStarted = true;
            } catch (IllegalStateException e) {
                // Ignora se já estiver iniciado
            }
        }

        // Executa a criação da janela na thread correta do JavaFX
        Platform.runLater(() -> {
            Stage window = new Stage(); //novo stage
            window.initModality(Modality.APPLICATION_MODAL);
            window.setTitle(title);

            //Nó onde vamos alocar o tabuleiro
            VBox root = new VBox(15);
            root.setPadding(new Insets(20));
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color: #0f172a;");

            // Usando a variavel de titulo vamos colocar um titulo para a janela
            Label lblTitle = new Label(title.toUpperCase());
            lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
            lblTitle.setTextFill(Color.web("#38bdf8"));

            //Criação do tabuleiro em si , o GridPane()
            GridPane grid = new GridPane();
            grid.setAlignment(Pos.CENTER);
            grid.setHgap(6);
            grid.setVgap(6);

            char[][] map = new char[BOARD_SIZE][BOARD_SIZE]; //Este map serve para representar o tabuleiro, usando o tamanho fixo do tabuleiro
            for (int r = 0; r < BOARD_SIZE; r++) {
                for (int c = 0; c < BOARD_SIZE; c++) {
                    map[r][c] = EMPTY_MARKER;
                }
            }

            // No caso do fleet ser de um objeto não nulo vamos usar funções auxiliares desenvolvidas já na classe Game (copy and past)
            if (fleet != null) {
                printShips(fleet, map); //Colocamos barcos
            }

            if (showShots && moves != null) { //caso queiramos mostrar os shoots e moves não serem null(ou seja já houve tiros) vamos colocar os shots.
                printBoardShots(moves, map); // Colocamos os shoots
            }

            String[] rowLetters = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"}; //Array auxiliar para fazer display das letras por linha (cada linha tem um indice alfabetico)

            for (int col = 1; col <= BOARD_SIZE; col++) { //Aqui vamos percorrer as colunas todas e colocar o indice numerico para cada coluna, começando em 1 indo até 10.
                Label colLabel = new Label(String.valueOf(col)); //label aonde vamos colocar o texto (númerico) relativo ao numero da coluna.
                //Aqui vamos só editar basicamente a aparencia e colocar este no gridPanel.
                colLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                colLabel.setTextFill(Color.web("#94a3b8"));
                colLabel.setMinSize(36, 25);
                colLabel.setAlignment(Pos.CENTER);
                grid.add(colLabel, col, 0); //row index = 0, pois os indices ficam sempre na linha 0 (No topo).
            }

            //Prenchimento dos Indicies das linhas e do tabuleiro em si (percorremos linha a linha, colocamos os indices e depois os barcos/tiros/agua).
            for (int row = 0; row < BOARD_SIZE; row++) {
                Label rowLabel = new Label(rowLetters[row]); //Pegamos a letra associada à linha

                //Edição de aparencia e colocar no gridPanel
                rowLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                rowLabel.setTextFill(Color.web("#94a3b8"));
                rowLabel.setMinSize(25, 36);
                rowLabel.setAlignment(Pos.CENTER);
                grid.add(rowLabel, 0, row + 1);

                for (int col = 0; col < BOARD_SIZE; col++) { //Vamos percorrer agora a restante linha (já foi colocado o indice no incio desta função)
                    char cellChar = map[row][col];
                    Label cellLabel = CreateCelula(cellChar); //Chamamos uma função auxuliar que faz de acordo o cellChar (conteudo na posição do tabuleiro atual) coloca a figura pertinente,usando um switch-case.
                    grid.add(cellLabel, col + 1, row + 1); //adição ao gridp
                }
            }

            VBox legendBox = createLegenda();
            root.getChildren().addAll(lblTitle, grid, legendBox); //Passo importante -> Aqui que camos alocar o titulo, o gridPanel e a legenda.

            Scene scene = new Scene(root, 480, 580); //Criação da cena
            window.setScene(scene); //Alocação da cena no stage.
            window.show(); // show -> "Attempts to show this Window by setting visibility to true"
            window.setAlwaysOnTop(true); //de modo a que a janela abra logo

        });
    }

    private static Label CreateCelula(char marker) {

        // Criação da label e estruta igual para todas (independemente do valor presenta na celula)
        Label cell = new Label();
        cell.setMinSize(36, 36);
        cell.setMaxSize(36, 36);
        cell.setAlignment(Pos.CENTER);
        cell.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        switch (marker) {
            case SHIP_MARKER: // valor associado -> '#'
                cell.setText("🚢"); //emogi que vamos colocar.
                cell.setStyle("-fx-background-color: #334155; -fx-background-radius: 6;"); //Estilo
                break;
            case SHOT_SHIP_MARKER: // '*' (Tiro certeiro)
                cell.setText("💥");
                cell.setStyle("-fx-background-color: #991b1b; -fx-background-radius: 6;");
                break;
            case SHOT_WATER_MARKER: // 'o' (Tiro na água)
                cell.setText("o");
                cell.setTextFill(Color.web("#38bdf8"));
                cell.setStyle("-fx-background-color: #1e3a8a; -fx-background-radius: 6;");
                break;
            default: // EMPTY_MARKER '.' (Água limpa)
                cell.setText(".");
                cell.setTextFill(Color.web("#334155"));
                cell.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 6; -fx-border-color: #273548; -fx-border-radius: 6;");
                break;
        }
        return cell; //Fazemos return da cell
    }

    private static VBox createLegenda() { //Retornamos um VBox

        //Vbox
        VBox box = new VBox(6);
        //Estrutracão
        box.setPadding(new Insets(10, 0, 0, 0));
        box.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Legenda:");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        title.setTextFill(Color.web("#94a3b8"));

        Label l1 = new Label("🚢 Navio   |   💥 Tiro certeiro   |   o: Tiro na água");
        Label l2 = new Label(" ' . ' : Água ");
        for (Label l : new Label[]{l1, l2}) { //Isto vai ser para simplificar, criamos um array de lavel e colocando as labels l1 e l2 ( as legendas de facto) e percorremos cada uma delas dando stilo igual.
            l.setFont(Font.font("Segoe UI", 11));
            l.setTextFill(Color.web("#cbd5e1"));
        }

        box.getChildren().addAll(title, l1, l2); //adicionamos o titulo e as labels á box(container) que vai ter tudo.
        return box;
    }

    //Função auxiliar para preenchimento do tabuleiro (char) , coloca os barcos nos respetivos sitios -> copiado da classe Game
    private static void printShips(IFleet fleet, char[][] map) {
        for (IShip ship : fleet.getShips()) {
            for (IPosition ship_pos : ship.getPositions())
                map[ship_pos.getRow()][ship_pos.getColumn()] = SHIP_MARKER;
            if (!ship.stillFloating())
                for (IPosition adjacent_pos : ship.getAdjacentPositions())
                    map[adjacent_pos.getRow()][adjacent_pos.getColumn()] = SHIP_ADJACENT_MARKER;
        }
    }
    //Função auxiliar para preenchimento do tabuleiro (char) , coloca os tiros  nos respetivos sitios -> copiado da classe Game
    private static void printBoardShots(List<IMove> moves, char[][] map) {
        for (IMove move : moves)
            for (IPosition shot : move.getShots()) {
                if (shot.isInside()){
                    int row = shot.getRow();
                    int col = shot.getColumn();
                    if (map[row][col] == SHIP_MARKER)
                        map[row][col] = SHOT_SHIP_MARKER;
                    if (map[row][col] == EMPTY_MARKER || map[row][col] == SHIP_ADJACENT_MARKER)
                        map[row][col] = SHOT_WATER_MARKER;
                }
            }
    }
}
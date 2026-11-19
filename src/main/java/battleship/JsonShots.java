package battleship;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonShots {
    /**
     * Serializes a list of shot positions into a JSON string. Each shot is represented
     * with its classic row and column values. The method uses the Jackson library for
     * JSON serialization.
     *
     * @param shots a list of shot positions to be serialized. Each position is represented
     *              by an implementation of the {@code IPosition} interface. The list must
     *              not be null.
     * @return a formatted JSON string containing the shot positions. Each shot includes
     *         its classic row and column.
     * @throws RuntimeException if an error occurs during JSON serialization.
     */
    public static String jsonShots(List<IPosition> shots) {

        assert shots != null;

        // Serializar os tiros gerados em JSON usando a biblioteca Jackson
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // 1. Create a simplified list containing only the desired data
        List<Map<String, Object>> simplifiedShots = new ArrayList<>();
        for (IPosition shot : shots) {
            Map<String, Object> simplePos = new LinkedHashMap<>();
            // We use getClassicRow() and getClassicColumn() based on your current JSON output
            simplePos.put("row", String.valueOf(shot.getClassicRow()));
            simplePos.put("column", shot.getClassicColumn());
            simplifiedShots.add(simplePos);
        }

        String jsonString = null;
        try {
            // 2. Serialize the simplified list instead of the raw 'shots' list
            jsonString = objectMapper.writeValueAsString(simplifiedShots);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar o JSON", e);
        }

        System.out.println(jsonString);
        System.out.println();

        // Retornar o JSON
        return jsonString;
    }
}

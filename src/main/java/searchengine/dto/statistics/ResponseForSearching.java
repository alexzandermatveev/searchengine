package searchengine.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Собирательная сущность для {@link PageInfoAfterSearching}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseForSearching {
    private Boolean result;
    private Long count;
    private List<PageInfoAfterSearching> data;
}

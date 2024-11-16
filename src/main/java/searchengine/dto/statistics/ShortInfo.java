package searchengine.dto.statistics;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Короткий формат ответа для клиента
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ShortInfo {
    private boolean result;
    private String error;
}

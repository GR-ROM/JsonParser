package su.grinev;

import java.util.List;
import java.util.Map;

public class ParserContext {

    private int position;
    private Map<String, Object> jsonObject;
    private List<Object> jsonArray;

    public ParserContext(int position, Map<String, Object> jsonObject, List<Object> jsonArray) {
        this.position = position;
        this.jsonObject= jsonObject;
        this.jsonArray = jsonArray;
    }

    public int getPosition() {
        return position;
    }

    public Map<String, Object> getJsonObject() {
        return jsonObject;
    }

    public List<Object> getJsonArray() { return jsonArray; }

}

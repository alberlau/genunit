package genunit;

import java.util.HashMap;
import java.util.Map;

public class MetadataHolder {
    private static Map metadata = new HashMap<>();
    public static Map getMetadata() {
        return metadata;
    }
}

package genunit;

import java.io.File;
import java.nio.file.Paths;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public class GenTest {

    @Test
    public void test() throws Exception {
        String s = IOUtils.toString(GenTest.class.getResourceAsStream("/java/OfficeDepartmentsService.java"));
        Gen.gen(new File(Paths.get("").toAbsolutePath().toString()), s);
    }
}

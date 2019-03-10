package genunit;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "genunit")
public class GenunitMojo extends AbstractMojo {

    @Parameter(property = "files")
    private List<String> filePatterns;

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject project;

    @Override
    public void execute() {
        for (int i = 0; i < filePatterns.size(); i++) {
            try {
                FileInputStream fis = new FileInputStream(
                    new File(project.getBasedir(), filePatterns.get(i)));
                Gen.gen(project.getBasedir(), IOUtils.toString(fis));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public List<String> getFilePatterns() {
        return filePatterns;
    }

    public void setFilePatterns(List<String> filePatterns) {
        this.filePatterns = filePatterns;
    }
}

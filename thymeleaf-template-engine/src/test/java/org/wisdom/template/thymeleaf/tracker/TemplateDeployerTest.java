package org.wisdom.template.thymeleaf.tracker;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ow2.chameleon.core.services.Watcher;
import org.wisdom.api.configuration.ApplicationConfiguration;
import org.wisdom.template.thymeleaf.ThymeleafTemplateCollector;

import java.io.File;
import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Check the template deployer behavior.
 */
public class TemplateDeployerTest {

    File directory = new File("target/base");

    @Before
    public void setUp() {
        FileUtils.deleteQuietly(directory);
        directory.mkdirs();
    }

    @After
    public void tearDown() {
        FileUtils.deleteQuietly(directory);
    }


    @Test
    public void start() {
        TemplateDeployer deployer = new TemplateDeployer();
        deployer.watcher = mock(Watcher.class);
        deployer.configuration = mock(ApplicationConfiguration.class);
        when(deployer.configuration.getBaseDir()).thenReturn(directory);
        when(deployer.configuration.getFileWithDefault("application.template.directory",
                "templates")).thenReturn(new File(directory, "templates"));
        deployer.engine = mock(ThymeleafTemplateCollector.class);

        deployer.start();
        deployer.stop();
    }

    @Test
    public void testAccept() {
        TemplateDeployer deployer = new TemplateDeployer();
        deployer.watcher = mock(Watcher.class);
        deployer.configuration = mock(ApplicationConfiguration.class);
        when(deployer.configuration.getBaseDir()).thenReturn(directory);
        when(deployer.configuration.getFileWithDefault("application.template.directory",
                "templates")).thenReturn(new File(directory, "templates"));
        deployer.engine = mock(ThymeleafTemplateCollector.class);

        assertThat(deployer.accept(new File("src/test/resources/templates/javascript.html"))).isTrue();
        // no th: in this file:
        assertThat(deployer.accept(new File("src/test/resources/templates/raw.html"))).isFalse();
    }

    @Test
    public void testDynamism() throws MalformedURLException {
        TemplateDeployer deployer = new TemplateDeployer();
        deployer.watcher = mock(Watcher.class);
        deployer.configuration = mock(ApplicationConfiguration.class);
        when(deployer.configuration.getBaseDir()).thenReturn(directory);
        when(deployer.configuration.getFileWithDefault("application.template.directory",
                "templates")).thenReturn(new File(directory, "templates"));
        deployer.engine = mock(ThymeleafTemplateCollector.class);

        File file = new File("src/test/resources/templates/javascript.html");
        deployer.onFileCreate(file);
        verify(deployer.engine).addTemplate(file.toURI().toURL());

        deployer.onFileChange(file);
        verify(deployer.engine).updatedTemplate(file);

        deployer.onFileDelete(file);
        verify(deployer.engine).deleteTemplate(file);
    }
}

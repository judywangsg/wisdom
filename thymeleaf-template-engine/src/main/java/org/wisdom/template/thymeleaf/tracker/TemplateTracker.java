package org.wisdom.template.thymeleaf.tracker;

import org.apache.commons.io.IOUtils;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wisdom.template.thymeleaf.ThymeleafTemplateCollector;
import org.wisdom.template.thymeleaf.impl.ThymeLeafTemplateImplementation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * A Chameleon deployer tracking template from bundles.
 */
@Component
@Provides
@Instantiate
public class TemplateTracker implements BundleTrackerCustomizer<List<ThymeLeafTemplateImplementation>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateTracker.class);

    @Requires
    ThymeleafTemplateCollector engine;

    @Context
    BundleContext context;

    private static final String TEMPLATE_DIRECTORY_IN_BUNDLES = "/templates";

    /**
     * The directory containing templates.
     */
    private File directory;

    private BundleTracker<List<ThymeLeafTemplateImplementation>> tracker;


    @Validate
    public void start() {
        LOGGER.info("Starting Thymeleaf template tracker");
        tracker = new BundleTracker<>(context, Bundle.ACTIVE, this);
        tracker.open();
    }

    @Invalidate
    public void stop() {
        tracker.close();
    }

    @Override
    public List<ThymeLeafTemplateImplementation> addingBundle(Bundle bundle, BundleEvent bundleEvent) {
        List<ThymeLeafTemplateImplementation> list = new ArrayList<ThymeLeafTemplateImplementation>();
        Enumeration<URL> urls = bundle.findEntries(TEMPLATE_DIRECTORY_IN_BUNDLES, "*.html", true);
        if (urls == null) {
            return list;
        }
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            try {
                // Check it's the thymeleaf bundle.
                if (IOUtils.toString(url).contains("th:")) {
                    ThymeLeafTemplateImplementation template = engine.addTemplate(url);
                    list.add(template);
                }
            } catch (IOException e) {
                LOGGER.error("Cannot read the content of {} from bundle {}", url, bundle.getSymbolicName(), e);
            }
        }
        return list;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent bundleEvent, List<ThymeLeafTemplateImplementation> o) {
        for (ThymeLeafTemplateImplementation template : o) {
            engine.updatedTemplate(template);
        }
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent bundleEvent, List<ThymeLeafTemplateImplementation> o) {
        for (ThymeLeafTemplateImplementation template : o) {
            LOGGER.info("Thymeleaf template deleted for {} from {}", template.fullName(), bundle.getSymbolicName());
            engine.deleteTemplate(template);

        }
    }
}
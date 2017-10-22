package cs.ut.config;

import cs.ut.config.items.HeaderItem;
import cs.ut.config.items.ModelProperties;
import cs.ut.provider.ModelConfigurationProvider;
import cs.ut.provider.PageConfigurationProvider;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.List;

@XmlRootElement(name = "configuration")
public class MasterConfiguration {
    private static final Logger log = Logger.getLogger(MasterConfiguration.class);

    private static MasterConfiguration master;

    @XmlElement(name = "pageConfig")
    private PageConfigurationProvider pageConfigurationProvider;

    @XmlElement(name = "userLogDirectory")
    private String userLogDirectory;

    @XmlElementWrapper(name = "headerConfiguration")
    @XmlElement(name = "headerItem")
    private List<HeaderItem> headerItems;

    @XmlElement(name = "modelConfig")
    private ModelProperties modelProperties;

    private ModelConfigurationProvider modelConfigurationProvider;

    private MasterConfiguration() {
        configureLogger();
    }

    /**
     * Instantiates master configuration or if it has already been instantiated then returns the object
     *
     * @return master configuration object that contains configurable values defined in configuration.xml
     */
    public static MasterConfiguration getInstance() {
        if (master == null) {
            master = new MasterConfiguration();
            try {
                master.readMasterConfig();
            } catch (JAXBException e) {
                throw new RuntimeException("Failed to read master configuration", e);
            }
        }
        return master;
    }

    /**
     * Reads configuration.xml file and parses it into Java objects.
     *
     * @throws JAXBException if configuration is defined incorrectly
     */
    private void readMasterConfig() throws JAXBException {
        log.debug("Reading master configuration");
        File file = new File(getClass().getClassLoader().getResource("configuration.xml").getFile());

        JAXBContext jaxbContext = JAXBContext.newInstance(MasterConfiguration.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        MasterConfiguration configuration = (MasterConfiguration) unmarshaller.unmarshal(file);
        log.debug("Finished reading configuration");

        pageConfigurationProvider = configuration.getPageConfigurationProvider();
        log.debug(String.format("Successfully retrieved %s page configurations", pageConfigurationProvider.getPages().size()));

        userLogDirectory = configuration.getUserLogDirectory();
        log.debug(String.format("Successfully read user log directory: '%s'", userLogDirectory));

        headerItems = configuration.getHeaderItems();
        log.debug(String.format("Successfully read %s header items", headerItems.size()));

        modelProperties = configuration.getModelProperties();
        log.debug(String.format("Successfully read %s types and %s model parameters",
                modelProperties.getTypes().size(),
                modelProperties.getParameters().size()));

        modelConfigurationProvider = new ModelConfigurationProvider(modelProperties);

        log.debug("Successfully read master configuration");
    }

    public PageConfigurationProvider getPageConfigurationProvider() {
        return pageConfigurationProvider;
    }

    public String getUserLogDirectory() {
        return userLogDirectory;
    }

    public List<HeaderItem> getHeaderItems() {
        return headerItems;
    }

    private ModelProperties getModelProperties() {
        return modelProperties;
    }

    public ModelConfigurationProvider getModelConfigurationProvider() {
        return modelConfigurationProvider;
    }

    /**
     * Configures logger and Enables appenders for Log4j
     */
    private void configureLogger() {
        BasicConfigurator.configure();
    }
}

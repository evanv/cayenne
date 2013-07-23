package org.apache.cayenne.project.upgrade.v7;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.XMLDataChannelDescriptorLoader;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.project.upgrade.BaseUpgradeHandler;
import org.apache.cayenne.project.upgrade.UpgradeHandler;
import org.apache.cayenne.project.upgrade.UpgradeMetaData;
import org.apache.cayenne.project.upgrade.UpgradeType;
import org.apache.cayenne.project.upgrade.v6.ProjectUpgrader_V6;
import org.apache.cayenne.resource.Resource;

class UpgradeHandler_V7 extends BaseUpgradeHandler {

    static final String TO_VERSION = "7";
    static final String MIN_SUPPORTED_VERSION = "3.0.0.1";
    
    @Inject
    protected Injector injector;
    
    @Inject
    private ProjectSaver projectSaver;
    
    public UpgradeHandler_V7(Resource source) {
        super(source);
    }

    @Override
    protected Resource doPerformUpgrade(UpgradeMetaData metaData) throws ConfigurationException {
        if (compareVersions(metaData.getProjectVersion(), MIN_SUPPORTED_VERSION) == 0) {
            ProjectUpgrader_V6 upgraderV6 = new ProjectUpgrader_V6();
            injector.injectMembers(upgraderV6);
            UpgradeHandler handlerV6 = upgraderV6.getUpgradeHandler(projectSource);
            projectSource = handlerV6.performUpgrade();
        }
        XMLDataChannelDescriptorLoader loader = new XMLDataChannelDescriptorLoader();
        injector.injectMembers(loader);
        ConfigurationTree<DataChannelDescriptor> tree = loader.load(projectSource);
        Project project = new Project(tree);
        // because listeners are no longer loaded they are not saved in upgraded project
        projectSaver.save(project); 
        return project.getConfigurationResource();
    }

    @Override
    protected UpgradeMetaData loadMetaData() {
        String version = loadProjectVersion();

        UpgradeMetaData metadata = new UpgradeMetaData();
        metadata.setSupportedVersion(TO_VERSION);
        metadata.setProjectVersion(version);

        int c1 = compareVersions(version, MIN_SUPPORTED_VERSION);
        int c2 = compareVersions(TO_VERSION, version);
        
        if (c1 < 0) {
            metadata.setIntermediateUpgradeVersion(MIN_SUPPORTED_VERSION);
            metadata.setUpgradeType(UpgradeType.INTERMEDIATE_UPGRADE_NEEDED);
        }
        else if (c2 < 0) {
            metadata.setUpgradeType(UpgradeType.DOWNGRADE_NEEDED);
        }
        else if (c2 == 0) {
            metadata.setUpgradeType(UpgradeType.UPGRADE_NOT_NEEDED);
        }
        else {
            metadata.setUpgradeType(UpgradeType.UPGRADE_NEEDED);
        }

        return metadata;
    }

}

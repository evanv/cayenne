package org.objectstyle.cayenne.modeler.control;

import java.awt.Component;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.objectstyle.cayenne.gen.DefaultClassGenerator;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.modeler.ModelerPreferences;
import org.objectstyle.cayenne.modeler.model.ClassGeneratorModel;
import org.objectstyle.cayenne.modeler.util.FileFilters;
import org.objectstyle.cayenne.modeler.view.ClassGeneratorDialog;
import org.objectstyle.cayenne.project.Project;
import org.objectstyle.cayenne.project.validator.Validator;
import org.scopemvc.controller.basic.BasicController;
import org.scopemvc.core.Control;
import org.scopemvc.core.ControlException;

/**
 * @author Andrei Adamchik
 */
public class ClassGeneratorController extends BasicController {
    public static final String CANCEL_CONTROL =
        "cayenne.modeler.classgenerator.cancel.button";

    public static final String GENERATE_CLASSES_CONTROL =
        "cayenne.modeler.classgenerator.generate.button";

    public static final String CHOOSE_LOCATION_CONTROL =
        "cayenne.modeler.classgenerator.choose.button";

    public static final String CHOOSE_TEMPLATE_CONTROL =
        "cayenne.modeler.classgenerator.choosetemplate.button";

    public static final String CHOOSE_SUPERTEMPLATE_CONTROL =
        "cayenne.modeler.classgenerator.choosesupertemplate.button";

    public ClassGeneratorController(
        Project project,
        DataMap map,
        ObjEntity selectedEntity) {
        setModel(prepareModel(project, map, selectedEntity));
    }

    protected Object prepareModel(
        Project project,
        DataMap map,
        ObjEntity selectedEntity) {

        // validate entities
        Validator validator = project.getValidator();
        validator.validate();

        ClassGeneratorModel model =
            new ClassGeneratorModel(map, selectedEntity, validator.validationResults());

        // by default generate pairs of classes
        model.setPairs(true);
        model.updateDefaultSuperClassPackage();

        // figure out default out directory
        ModelerPreferences pref = ModelerPreferences.getPreferences();
        String startDir =
            (String) pref.getProperty(ModelerPreferences.LAST_GENERATED_CLASSES_DIR);

        if (startDir != null) {
            model.setOutputDir(startDir);
        }

        return model;
    }

    /**
     * Creates and runs the class generation dialog.
     */
    public void startup() {
        setView(new ClassGeneratorDialog());
        super.startup();
    }

    protected void doHandleControl(Control control) throws ControlException {
        if (control.matchesID(CANCEL_CONTROL)) {
            shutdown();
        }
        else if (control.matchesID(GENERATE_CLASSES_CONTROL)) {
            generateClasses();
        }
        else if (control.matchesID(CHOOSE_LOCATION_CONTROL)) {
            chooseLocation();
        }
        else if (control.matchesID(CHOOSE_TEMPLATE_CONTROL)) {
            chooseClassTemplate();
        }
        else if (control.matchesID(CHOOSE_SUPERTEMPLATE_CONTROL)) {
            chooseSuperclassTemplate();
        }
    }

    protected void generateClasses() {
        ClassGeneratorModel model = (ClassGeneratorModel) getModel();
        File outputDir = model.getOutputDirectory();

        // no destination folder
        if (outputDir == null) {
            JOptionPane.showMessageDialog(
                (Component) this.getView(),
                "Select directory for source files.");
            return;
        }

        // no such folder
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            JOptionPane.showMessageDialog(
                (Component) this.getView(),
                "Can't create directory " + outputDir + ". Select a different one.");
            return;
        }

        // not a directory
        if (!outputDir.isDirectory()) {
            JOptionPane.showMessageDialog(
                (Component) this.getView(),
                outputDir + " is not a valid directory.");
            return;
        }

        File classTemplate = null;
        if (model.getCustomClassTemplate() != null) {
            classTemplate = new File(model.getCustomClassTemplate());

            if (!classTemplate.canRead()) {
                JOptionPane.showMessageDialog(
                    (Component) this.getView(),
                    model.getCustomClassTemplate() + " is not a valid template file.");
                return;
            }
        }

        File superClassTemplate = null;
        if (model.getCustomSuperclassTemplate() != null) {
            superClassTemplate = new File(model.getCustomSuperclassTemplate());

            if (!superClassTemplate.canRead()) {
                JOptionPane.showMessageDialog(
                    (Component) this.getView(),
                    model.getCustomClassTemplate() + " is not a valid template file.");
                return;
            }
        }

        List selected = model.getSelectedEntities();
        DefaultClassGenerator generator = new DefaultClassGenerator(selected);
        generator.setDestDir(outputDir);
        generator.setMakePairs(model.isPairs());
        generator.setSuperPkg(model.getSuperClassPackage());
        generator.setSuperTemplate(superClassTemplate);
        generator.setTemplate(classTemplate);

        try {
            generator.execute();
            JOptionPane.showMessageDialog(
                (Component) this.getView(),
                "Class generation finished");
            shutdown();
        }
        catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                (Component) this.getView(),
                "Error generating classes - " + e.getMessage());
        }
    }

    protected void chooseLocation() {
        ClassGeneratorModel model = (ClassGeneratorModel) getModel();
        File startDir = model.getOutputDirectory();

        // guess start directory
        if (startDir == null) {
            String lastUsed =
                (String) ModelerPreferences.getPreferences().getProperty(
                    ModelerPreferences.LAST_GENERATED_CLASSES_DIR);
            if (lastUsed != null) {
                startDir = new File(lastUsed);
            }
        }

        // guess again
        if (startDir == null) {
            String lastUsed =
                (String) ModelerPreferences.getPreferences().getProperty(
                    ModelerPreferences.LAST_DIR);
            if (lastUsed != null) {
                startDir = new File(lastUsed);
            }
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);

        if (startDir != null) {
            chooser.setCurrentDirectory(startDir);
        }

        int result = chooser.showOpenDialog((Component) this.getView());
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            // Set preferences
            ModelerPreferences.getPreferences().setProperty(
                ModelerPreferences.LAST_GENERATED_CLASSES_DIR,
                selected.getAbsolutePath());

            // update model
            model.setOutputDir(selected.getAbsolutePath());
        }
    }

    protected void chooseSuperclassTemplate() {
        ClassGeneratorModel model = (ClassGeneratorModel) getModel();
        String template =
            chooseTemplate(
                model.getCustomSuperclassTemplate(),
                "Select Custom Superclass Template");
        model.setCustomSuperclassTemplate(template);
    }

    protected void chooseClassTemplate() {
        ClassGeneratorModel model = (ClassGeneratorModel) getModel();
        String template =
            chooseTemplate(
                model.getCustomClassTemplate(),
                "Select Custom Class Template");
        model.setCustomClassTemplate(template);
    }

    /**
     * Picks and returns class generation velocity template.
     */
    private String chooseTemplate(String oldTemplate, String title) {
        File startDir =
            (oldTemplate != null) ? new File(oldTemplate).getParentFile() : null;

        // guess start directory
        if (startDir == null) {
            String lastUsed =
                (String) ModelerPreferences.getPreferences().getProperty(
                    ModelerPreferences.LAST_CLASS_GENERATION_TEMPLATE);
            if (lastUsed != null) {
                startDir = new File(lastUsed).getParentFile();
            }
        }

        if (startDir == null) {
            String lastUsed =
                (String) ModelerPreferences.getPreferences().getProperty(
                    ModelerPreferences.LAST_DIR);
            if (lastUsed != null) {
                startDir = new File(lastUsed);
            }
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.addChoosableFileFilter(FileFilters.getVelotemplateFilter());

        chooser.setDialogTitle(title);

        if (startDir != null) {
            chooser.setCurrentDirectory(startDir);
        }

        File selected = null;
        int result = chooser.showOpenDialog((Component) this.getView());
        if (result == JFileChooser.APPROVE_OPTION) {
            selected = chooser.getSelectedFile();

            // Set preferences
            ModelerPreferences.getPreferences().setProperty(
                ModelerPreferences.LAST_CLASS_GENERATION_TEMPLATE,
                selected.getAbsolutePath());
            ModelerPreferences.getPreferences().setProperty(
                ModelerPreferences.LAST_DIR,
                selected.getAbsolutePath());
        }

        return (selected != null) ? selected.getAbsolutePath() : null;
    }
}

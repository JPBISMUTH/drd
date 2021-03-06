package cz.stechy.drd;

import com.google.firebase.database.FirebaseDatabase;
import cz.stechy.drd.R.FXML;
import cz.stechy.drd.model.Context;
import cz.stechy.drd.util.UTF8ResourceBundleControl;
import cz.stechy.screens.ScreenManager;
import cz.stechy.screens.ScreenManagerConfiguration;
import cz.stechy.screens.base.IMainScreen;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vstupní bod aplikace
 */
public class App extends Application {

    // region Constants

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static final String BASE_FXML = "root.fxml";
    private static final String FOLDER_FXML = "fxml";
    private static final String FILE_CSS = "css/style.css";
    private static final String FOLDER_LANG = "lang";
    private static final String LANG_FILE_CONVENTION = "lang.translate";
    private static final String FILE_CONFIG = "config.properties";
    private static final String[] SCREENS_BLACKLIST = {
        R.FXML.DEFAULT_STAFF_LEFT, R.FXML.DEFAULT_STAFF_RIGHT};

    // endregion

    // region Variables

    private Context context;

    private ScreenManager manager;

    // endregion

    // region Constructors

    public App() throws FileNotFoundException {
        super();
        initScreenManager();
        context = new Context(getDatabaseName(), manager.getResources());
        manager.setControllerFactory(new ControllerFactory(context));
    }

    // endregion

    // region Private methods

    /**
     * Inicializuje screen manager
     */
    private void initScreenManager() {
        ScreenManagerConfiguration configuration = new ScreenManagerConfiguration.ConfigurationBuilder()
            .baseFxml(App.class.getClassLoader().getResource(BASE_FXML))
            .fxml(App.class.getClassLoader().getResource(FOLDER_FXML))
            .css(App.class.getClassLoader().getResource(FILE_CSS))
            .lang(App.class.getClassLoader().getResource(FOLDER_LANG))
            .config(App.class.getClassLoader().getResource(FILE_CONFIG))
            .build();
        manager = new ScreenManager(configuration);
        ResourceBundle resources = ResourceBundle.getBundle(
            LANG_FILE_CONVENTION, Locale.getDefault(), new UTF8ResourceBundleControl());
        manager.setResources(resources);
        manager.addScreensToBlacklist(SCREENS_BLACKLIST);
    }

    private String getDatabaseName() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(
                manager.getScreenManagerConfiguration().config.toExternalForm()));
            return properties.getProperty("database");
        } catch (IOException e) {
            return "database.sqlite";
        }
    }

    // endregion

    public static void main(String[] args) {
        logger.info("Spouštím aplikaci...");
        launch(args);
    }

    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(manager.getScreenManagerConfiguration().baseFxml);
        loader.setResources(manager.getResources());
        Parent parent = loader.load();
        IMainScreen controlledScreen = loader.getController();
        manager.setMainScreen(controlledScreen);
        manager.loadScreens();
        manager.showNewDialog(parent, primaryStage);
        manager.showScreen(FXML.MAIN, null);
        primaryStage.setOnCloseRequest(event -> {
            logger.info("Ukončuji aplikaci");
            FirebaseDatabase.getInstance().goOffline();
            //ThreadPool.getInstance().shutDown();
        });
    }
}

package cz.stechy.drd.controller.bestiary;

import cz.stechy.drd.Context;
import cz.stechy.drd.R;
import cz.stechy.drd.model.Rule;
import cz.stechy.drd.model.bestiary.MobEntry;
import cz.stechy.drd.model.db.AdvancedDatabaseService;
import cz.stechy.drd.model.db.DatabaseException;
import cz.stechy.drd.model.db.base.Firebase.OnDeleteItem;
import cz.stechy.drd.model.db.base.Firebase.OnDownloadItem;
import cz.stechy.drd.model.db.base.Firebase.OnUploadItem;
import cz.stechy.drd.model.entity.mob.Mob;
import cz.stechy.drd.model.entity.mob.Mob.MobClass;
import cz.stechy.drd.model.user.User;
import cz.stechy.drd.util.CellUtils;
import cz.stechy.drd.util.HashGenerator;
import cz.stechy.drd.util.ObservableMergers;
import cz.stechy.drd.util.StringConvertors;
import cz.stechy.drd.util.Translator;
import cz.stechy.screens.BaseController;
import cz.stechy.screens.Bundle;
import java.net.URL;
import java.util.Comparator;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kontroler pro správu jednotlivých nestvůr ve hře
 */
public class BestiaryController extends BaseController implements Initializable {

    // region Constants

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(BestiaryController.class);

    private static final int NO_SELECTED_INDEX = -1;

    // endregion

    // region Variables

    // region FXML

    @FXML
    private TableView<MobEntry> tableBestiary;
    @FXML
    private TableColumn<MobEntry, Image> columnImage;
    @FXML
    private TableColumn<MobEntry, String> columnName;
    @FXML
    private TableColumn<MobEntry, String> columnAuthor;
    @FXML
    private TableColumn<MobEntry, MobClass> columnMobClass;
    @FXML
    private TableColumn<MobEntry, Rule> columnRulesType;
    @FXML
    private TableColumn<MobEntry, Integer> columnViability;
    @FXML
    private TableColumn<MobEntry, ?> columnAction;

    @FXML
    private Button btnAddItem;
    @FXML
    private Button btnRemoveItem;
    @FXML
    private Button btnEditItem;
    @FXML
    private Button btnSynchronize;
    @FXML
    private ToggleButton btnToggleOnline;

    // endregion

    private final ObservableList<MobEntry> mobs = FXCollections.observableArrayList();
    private final SortedList<MobEntry> sortedList = new SortedList<>(mobs,
        Comparator.comparing(MobEntry::getName));
    private final IntegerProperty selectedRowIndex = new SimpleIntegerProperty(
        this, "selectedRowIndex");
    private final BooleanProperty showOnlineDatabase = new SimpleBooleanProperty(this,
        "showOnlineDatabase, false");
    private final User user;
    private final Translator translator;

    private AdvancedDatabaseService<Mob> service;

    private String title;
    private ResourceBundle resources;

    // endregion

    // region Constructors

    public BestiaryController(Context context) {
        this.service = context.getService(Context.SERVICE_BESTIARY);
        this.user = context.getUserService().getUser().get();
        this.translator = context.getTranslator();
        ObservableMergers.mergeList(MobEntry::new, this.mobs, service.selectAll());
    }

    // endregion

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.resources = resources;
        this.title = resources.getString(R.Translate.BESTIARY_TITLE);

        tableBestiary.setItems(sortedList);

        final BooleanBinding selectedRowBinding = selectedRowIndex.isEqualTo(NO_SELECTED_INDEX);
        selectedRowIndex.bind(tableBestiary.getSelectionModel().selectedIndexProperty());
        showOnlineDatabase.bindBidirectional(btnToggleOnline.selectedProperty());
        btnAddItem.disableProperty().bind(showOnlineDatabase);
        btnRemoveItem.disableProperty().bind(Bindings.or(
            selectedRowBinding,
            showOnlineDatabase));
        btnEditItem.disableProperty().bind(Bindings.or(
            selectedRowBinding,
            showOnlineDatabase));
        btnSynchronize.disableProperty().bind(user.loggedProperty().not());

        showOnlineDatabase.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            service.toggleDatabase(newValue);
        });

        columnImage.setCellValueFactory(new PropertyValueFactory<>("image"));
        columnImage.setCellFactory(param -> CellUtils.forImage());
        columnName.setCellValueFactory(new PropertyValueFactory<>("name"));
        columnAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        columnMobClass.setCellValueFactory(new PropertyValueFactory<>("mobClass"));
        columnMobClass.setCellFactory(
            TextFieldTableCell.forTableColumn(StringConvertors.forMobClass(translator)));
        columnRulesType.setCellValueFactory(new PropertyValueFactory<>("rulesType"));
        columnRulesType.setCellFactory(
            TextFieldTableCell.forTableColumn(StringConvertors.forRulesType(translator)));
        columnViability.setCellValueFactory(new PropertyValueFactory<>("viability"));
        columnAction.setCellFactory(param -> BestiaryHelper
            .forActionButtons(uploadHandler, downloadHandler, deleteHandler, user, resources));
    }

    @Override
    protected void onResume() {
        setScreenSize(1000, 600);
        setTitle(title);
    }

    @Override
    protected void onScreenResult(int statusCode, int actionId, Bundle bundle) {
        Mob mob;
        switch (actionId) {
            case BestiaryHelper.MOB_ACTION_ADD:
                if (statusCode != RESULT_SUCCESS) {
                    return;
                }
                mob = BestiaryHelper.mobFromBundle(bundle);
                try {
                    mob.setAuthor(user.getName());
                    mob.setId(HashGenerator.createHash());
                    service.insert(mob);
                } catch (DatabaseException e) {
                    e.printStackTrace();
                    logger.warn("Nestvůru {} se nepodařilo vložit do databáze", mob.toString());
                }
                break;

            case BestiaryHelper.MOB_ACTION_UPDATE:
                if (statusCode != RESULT_SUCCESS) {
                    return;
                }

                mob = BestiaryHelper.mobFromBundle(bundle);
                try {
                    service.update(mob);
                } catch (DatabaseException e) {
                    logger.warn("Nestvůru {} se napodařilo aktualizovat", mob.toString());
                }
                break;
        }
    }

    // region Button handlers

    public void handleAddItem(ActionEvent actionEvent) {
        Bundle bundle = new Bundle();
        bundle.putInt(BestiaryHelper.MOB_ACTION, BestiaryHelper.MOB_ACTION_ADD);
        startNewDialogForResult(R.FXML.BESTIARY_EDIT, BestiaryHelper.MOB_ACTION_ADD, bundle);
    }

    public void handleRemoveItem(ActionEvent actionEvent) {
        final int rowIndex = selectedRowIndex.get();
        final MobEntry entry = sortedList.get(rowIndex);
        final String name = entry.getName();
        try {
            service.delete(entry.getMobBase().getId());
        } catch (DatabaseException e) {
            logger.warn("Příšeru {} se nepodařilo odebrat z databáze", name);
        }
    }

    public void handleEditItem(ActionEvent actionEvent) {
        final MobEntry entry = sortedList.get(selectedRowIndex.get());
        final Bundle bundle = BestiaryHelper.mobToBundle(entry.getMobBase());
        bundle.putInt(BestiaryHelper.MOB_ACTION, BestiaryHelper.MOB_ACTION_UPDATE);
        startNewDialogForResult(R.FXML.BESTIARY_EDIT, BestiaryHelper.MOB_ACTION_UPDATE, bundle);
    }

    public void handleSynchronize(ActionEvent actionEvent) {
        service.synchronize(user.getName(), total -> {
            logger.info("Bylo synchronizováno celkem: " + total + " nestvůr.");
        });
    }

    // endregion

    private final OnUploadItem<MobEntry> uploadHandler = entry -> service.upload(entry.getMobBase());
    private final OnDownloadItem<MobEntry> downloadHandler = entry -> {
        try {
            service.insert(entry.getMobBase());
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
    };
    private final OnDeleteItem<MobEntry> deleteHandler = (entry, remote) -> service.deleteRemote(entry.getMobBase(), true);
}

package cz.stechy.drd.app.shop;

import static cz.stechy.drd.app.shop.ShopHelper.SHOP_ROW_HEIGHT;

import cz.stechy.drd.R;
import cz.stechy.drd.R.Translate;
import cz.stechy.drd.ThreadPool;
import cz.stechy.drd.app.shop.entry.BackpackEntry;
import cz.stechy.drd.app.shop.entry.ShopEntry;
import cz.stechy.drd.dao.BackpackDao;
import cz.stechy.drd.db.AdvancedDatabaseService;
import cz.stechy.drd.model.MaxActValue;
import cz.stechy.drd.model.Money;
import cz.stechy.drd.model.User;
import cz.stechy.drd.model.item.Backpack;
import cz.stechy.drd.model.item.ItemBase;
import cz.stechy.drd.service.UserService;
import cz.stechy.drd.util.CellUtils;
import cz.stechy.drd.util.ObservableMergers;
import cz.stechy.drd.util.Translator;
import cz.stechy.screens.Bundle;
import cz.stechy.screens.Notification;
import java.net.URL;
import java.util.Comparator;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pomocný kontroler pro obchod s batohy
 */
public class ShopBackpackController implements Initializable, ShopItemController<BackpackEntry> {

    // region Constants

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopGeneralController.class);

    // endregion

    // region Variables

    // region FXML

    @FXML
    private TableView<BackpackEntry> tableBackpacks;
    @FXML
    private TableColumn<BackpackEntry, Image> columnImage;
    @FXML
    private TableColumn<BackpackEntry, String> columnName;
    @FXML
    private TableColumn<BackpackEntry, String> columnAuthor;
    @FXML
    private TableColumn<BackpackEntry, Integer> columnWeight;
    @FXML
    private TableColumn<BackpackEntry, Integer> columnMaxLoad;
    @FXML
    private TableColumn<BackpackEntry, Money> columnPrice;
    @FXML
    private TableColumn<BackpackEntry, MaxActValue> columnAmmount;
    @FXML
    private TableColumn<BackpackEntry, ?> columnAction;

    // endregion

    private final ObservableList<BackpackEntry> backpacks = FXCollections.observableArrayList();
    private final SortedList<BackpackEntry> sortedList = new SortedList<>(backpacks,
        Comparator.comparing(ShopEntry::getName));
    private final BooleanProperty ammountEditable = new SimpleBooleanProperty(true);
    private final BooleanProperty highlightDiffItem = new SimpleBooleanProperty(false);
    private final AdvancedDatabaseService<Backpack> service;
    private final Translator translator;
    private final User user;

    private IntegerProperty selectedRowIndex;
    private ResourceBundle resources;
    private ShopNotificationProvider notifier;
    private ShopOnlineListener shopOnlineListener;

    // endregion

    // region Constrollers

    public ShopBackpackController(UserService userService, BackpackDao backpackDao,
        Translator translator) {
        this.service = backpackDao;
        this.translator = translator;
        this.user = userService.getUser();
    }
    // endregion

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.resources = resources;
        tableBackpacks.setItems(sortedList);
        tableBackpacks.getSelectionModel().selectedIndexProperty()
            .addListener((observable, oldValue, newValue) -> selectedRowIndex.setValue(newValue));
        tableBackpacks.setFixedCellSize(SHOP_ROW_HEIGHT);
        tableBackpacks.setRowFactory(param -> new ShopRow<>(highlightDiffItem));
        sortedList.comparatorProperty().bind(tableBackpacks.comparatorProperty());

        columnMaxLoad.setCellFactory(param -> CellUtils.forWeight());
        columnWeight.setCellFactory(param -> CellUtils.forWeight());
        columnImage.setCellFactory(param -> CellUtils.forImage());
        columnPrice.setCellFactory(param -> CellUtils.forMoney());
        columnAmmount.setCellFactory(param -> CellUtils.forMaxActValue(ammountEditable));
    }

    @Override
    public void setShoppingCart(IShoppingCart shoppingCart) {
        columnAction.setCellFactory(param -> ShopHelper
            .forActionButtons(shoppingCart::addItem, shoppingCart::removeItem,
                resources, ammountEditable));

        final Function<Backpack, BackpackEntry> mapper = backpack -> {
            final BackpackEntry entry;
            final Optional<ShopEntry> cartEntry = shoppingCart.getEntry(backpack.getId());
            entry = cartEntry.map(shopEntry -> (BackpackEntry) shopEntry)
                .orElseGet(() -> new BackpackEntry(backpack));

            return entry;
        };

        service.selectAllAsync()
            .thenAccept(
                backpackList -> ObservableMergers.mergeList(mapper, backpacks, backpackList));
    }

    @Override
    public void setRowSelectedIndexProperty(IntegerProperty rowSelectedIndexProperty) {
        this.selectedRowIndex = rowSelectedIndexProperty;
    }

    @Override
    public void setShowOnlineDatabase(BooleanProperty showOnlineDatabase) {
        showOnlineDatabase.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            service.toggleDatabase(newValue);
        });
    }

    @Override
    public void setHighlightDiffItems(BooleanProperty highlightDiffItems) {
        this.highlightDiffItem.bind(highlightDiffItems);
        highlightDiffItems.addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue) {
                service.getDiff().thenAcceptAsync(diffEntries -> {
                    diffEntries.forEach(diffEntry -> {
                        final String id = diffEntry.getId();
                        backpacks
                            .parallelStream()
                            .filter(entry -> id.equals(entry.getId()))
                            .findFirst()
                            .ifPresent(generalEntry -> {
                                generalEntry.setDiffMap(diffEntry.getDiffMap());
                            });
                    });
                }, ThreadPool.JAVAFX_EXECUTOR);
            } else {
                backpacks.parallelStream().forEach(entry -> entry.clearDiffMap());
            }
        });
    }

    @Override
    public void setAmmountEditableProperty(BooleanProperty ammountEditable) {
        this.ammountEditable.bind(ammountEditable);
    }

    @Override
    public void setNotificationProvider(ShopNotificationProvider notificationProvider) {
        this.notifier = notificationProvider;
    }

    @Override
    public void setOnlineListener(ShopOnlineListener onlineListener) {
        this.shopOnlineListener = onlineListener;
    }

    @Override
    public String getEditScreenName() {
        return R.Fxml.ITEM_BACKPACK;
    }

    @Override
    public void onAddItem(ItemBase item, boolean remote) {
        service.insertAsync((Backpack) item)
            .exceptionally(throwable -> {
                notifier.showNotification(new Notification(String.format(translator.translate(
                    R.Translate.NOTIFY_RECORD_IS_NOT_INSERTED), item.getName())));
                LOGGER.error("Item {} se nepodařilo vložit do databáze", item.getName());
                throw new RuntimeException(throwable);
            })
            .thenAccept(armor -> notifier
                .showNotification(new Notification(String.format(translator.translate(
                    Translate.NOTIFY_RECORD_IS_INSERTED), item.getName()))));
    }

    @Override
    public void onUpdateItem(ItemBase item) {
        service.updateAsync((Backpack) item)
            .exceptionally(throwable -> {
                notifier.showNotification(new Notification(String.format(translator.translate(
                    R.Translate.NOTIFY_RECORD_IS_NOT_UPDATED), item.getName())));
                LOGGER.error("Položku {} se nepodařilo aktualizovat", item.getName());
                throw new RuntimeException(throwable);
            })
            .thenAccept(backpack -> notifier
                .showNotification(new Notification(String.format(translator.translate(
                    Translate.NOTIFY_RECORD_IS_NOT_UPDATED), item.getName()))));
    }

    @Override
    public void insertItemToBundle(Bundle bundle, int index) {
        ItemBackpackController.toBundle(bundle, (Backpack) sortedList.get(index).getItemBase());
    }

    @Override
    public ItemBase fromBundle(Bundle bundle) {
        return ItemBackpackController.fromBundle(bundle);
    }

    @Override
    public void requestRemoveItem(int index) {
        final BackpackEntry entry = sortedList.get(index);
        service.deleteAsync((Backpack) entry.getItemBase())
            .exceptionally(throwable -> {
                notifier.showNotification(new Notification(String.format(translator.translate(
                    R.Translate.NOTIFY_RECORD_IS_NOT_DELETED), entry.getName())));
                LOGGER.error("Položku {} se nepodařilo aktualizovat", entry.getName());
                throw new RuntimeException(throwable);
            })
            .thenAccept(backpack -> notifier
                .showNotification(new Notification(String.format(translator.translate(
                    Translate.NOTIFY_RECORD_IS_DELETED), entry.getName()))));
    }

    @Override
    public void requestRemoveItem(ShopEntry entry, boolean remote) {
        service.deleteRemoteAsync((Backpack) entry.getItemBase())
            .exceptionally(throwable -> {
                shopOnlineListener.handleItemRemove(entry.getName(), remote, false);
                throw new RuntimeException(throwable);
            })
            .thenAccept(aVoid -> shopOnlineListener.handleItemRemove(entry.getName(), remote, true));
    }

    @Override
    public void uploadRequest(ItemBase item) {
        service.uploadAsync((Backpack) item)
            .exceptionally(throwable -> {
                shopOnlineListener.handleItemUpload(item.getName(), false);
                throw new RuntimeException(throwable);
            })
            .thenAccept(aVoid -> shopOnlineListener.handleItemUpload(item.getName(), true));
    }

    @Override
    public void clearSelectedRow() {
        tableBackpacks.getSelectionModel().clearSelection();
    }

    @Override
    public void synchronizeItems() {
        service.synchronize(this.user.getName())
            .thenAccept(total -> LOGGER
                .info("Bylo synchronizováno celkem: " + total + " předmětů typu backpack."));
    }

    @Override
    public Optional<BackpackEntry> getSelectedItem() {
        if (selectedRowIndex.getValue() == null || selectedRowIndex.get() < 0) {
            return Optional.empty();
        }

        return Optional.of(sortedList.get(selectedRowIndex.get()));
    }

    @Override
    public void updateLocalItem(ShopEntry itemBase) {

    }

    @Override
    public void updateOnlineItem(ShopEntry itemBase) {

    }
}

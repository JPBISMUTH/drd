package cz.stechy.drd.model.inventory.container;

import cz.stechy.drd.model.inventory.ItemContainer;
import cz.stechy.drd.model.inventory.ItemSlot;
import cz.stechy.drd.model.inventory.ItemSlotHelper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

/**
 * Inventář obsahující vybavení postavy
 * Vybavení zahrnuje zbroj, zbraň a případně i štít
 */
public class EquipItemContainer extends ItemContainer {

    // region Constants

    public static final int CAPACITY = 3;

    // endregion

    // region Variables

    private final GridPane container = new GridPane();

    private int idCounter = 0;

    // endregion

    // region Constructors

    /**
     * Inicializuje kontainer pro výbavu postavy
     */
    public EquipItemContainer() {
        super(CAPACITY);

        init();
    }

    // endregion

    // region Private methods

    private void init() {
        container.setHgap(1);
        container.setVgap(1);
        BorderPane.setAlignment(container, Pos.CENTER);
        container.prefHeight(100);
        container.setStyle("-fx-background-color: orange");

        final ItemSlot slotSword = ItemSlotHelper.forWeapon(idCounter++, dragDropHandlers);
        final ItemSlot slotShield = ItemSlotHelper.forShield(idCounter++, dragDropHandlers);
        final ItemSlot slotHelm = ItemSlotHelper.forArmorHelm(idCounter++, dragDropHandlers);
        final ItemSlot slotBody = ItemSlotHelper.forArmorBody(idCounter++, dragDropHandlers);
        final ItemSlot slotLegs = ItemSlotHelper.forArmorLegs(idCounter++, dragDropHandlers);
        final ItemSlot slotBots = ItemSlotHelper.forArmorBots(idCounter++, dragDropHandlers);
        final ItemSlot slotGloves = ItemSlotHelper.forArmorGloves(idCounter++, dragDropHandlers);

        itemSlots.setAll(slotSword, slotShield,  slotHelm, slotBody, slotLegs, slotBots, slotGloves);

        container.add(slotHelm.getContainer(), 1, 0);
        container.add(slotBody.getContainer(), 1, 1);
        container.add(slotLegs.getContainer(), 1, 2);
        container.add(slotBots.getContainer(), 1, 3);
        container.add(slotGloves.getContainer(), 0, 2);
        container.add(slotSword.getContainer(), 0, 1);
        container.add(slotShield.getContainer(), 2, 1);
    }

    // endregion

    @Override
    public Node getGraphics() {
        return container;
    }
}

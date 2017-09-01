package cz.stechy.drd.util;

import cz.stechy.drd.model.Money;
import cz.stechy.drd.model.MaxActValue;
import cz.stechy.drd.model.inventory.ItemSlot;
import cz.stechy.drd.widget.MoneyWidget;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Pomocná knihovní třída pro generování různých buněk
 */
public final class CellUtils {

    public static <S> TableCell<S, Image> forImage() {
        final ImageView imageView = new ImageView();
        {
            imageView.setFitWidth(ItemSlot.SLOT_SIZE);
            imageView.setFitHeight(ItemSlot.SLOT_SIZE);
        }
        return new TableCell<S, Image>() {
            @Override
            protected void updateItem(Image item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    imageView.setImage(item);
                    setGraphic(imageView);
                }
            }
        };
    }

    public static <S> TableCell<S, MaxActValue> forMaxActValue() {
        return forMaxActValue(null);
    }

    public static <S> TableCell<S, MaxActValue> forMaxActValue(
        final BooleanProperty editable) {
        return new TableCell<S, MaxActValue>() {
            private final TextField input;
            private boolean initialized = false;

            {
                input = new TextField();
                if (editable != null) {
                    input.disableProperty().bind(editable);
                }
            }

            @Override
            public void updateItem(MaxActValue item, boolean empty) {
                super.updateItem(item, empty);

                // update according to new item
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    if (!initialized) {
                        FormUtils.initTextFormater(input, item);
                        initialized = true;
                    }
                    setGraphic(input);
                    setText(null);
                }
            }
        };
    }

    public static <S> TableCell<S, Money> forMoney() {

        return new TableCell<S, Money>() {
            @Override
            protected void updateItem(Money item, boolean empty) {
                super.updateItem(item, empty);

                final MoneyWidget moneyWidget = new MoneyWidget();

                if(empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(null);
                    moneyWidget.bind(item);
                    setGraphic(moneyWidget);
                }
            }
        };
    }

    public static <S> TableCell<S, Integer> forWeight() {
        return new TableCell<S, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item + " mn");
                }
            }
        };
    }

}

package cz.stechy.drd.controller.spellbook.priceeditor;

import com.jfoenix.controls.JFXComboBox;
import cz.stechy.drd.R;
import cz.stechy.drd.model.spell.parser.SpellParser.SpellVariable;
import cz.stechy.drd.util.Translator;
import cz.stechy.drd.util.Translator.Key;
import java.net.URL;
import java.util.ResourceBundle;

public class VariableDraggableSpellNode extends DraggableSpellNode {

    // region Variables

    private JFXComboBox<SpellVariable> cmbVariable;

    // endregion

    // region Constructors

    public VariableDraggableSpellNode(Translator translator) {
        super(translator);
    }

    // endregion

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbVariable = new JFXComboBox<>();
        cmbVariable.setLabelFloat(true);
        cmbVariable.setPromptText(translator.translate(R.Translate.SPELL_PRICE_TYPE_VARIABLE));

        lblTitle.setText(translator.translate(R.Translate.SPELL_PRICE_TYPE_VARIABLE));

        cmbVariable.getItems().setAll(SpellVariable.values());
        cmbVariable.setConverter(translator.getConvertor(Key.SPELL_VARIABLE_TYPES));

        container.getChildren().setAll(cmbVariable);

    }

}

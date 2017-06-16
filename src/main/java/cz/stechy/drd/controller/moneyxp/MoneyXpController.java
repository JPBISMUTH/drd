package cz.stechy.drd.controller.moneyxp;

import cz.stechy.drd.Money;
import cz.stechy.drd.R;
import cz.stechy.drd.model.Context;
import cz.stechy.drd.model.MaxActValue;
import cz.stechy.drd.model.entity.hero.Hero;
import cz.stechy.drd.model.persistent.HeroManager;
import cz.stechy.drd.util.FormUtils;
import cz.stechy.drd.widget.LabeledProgressBar;
import cz.stechy.drd.widget.MoneyLabel;
import cz.stechy.screens.BaseController;
import cz.stechy.screens.Bundle;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;

/**
 * Kontroler pro správu peněz a zkušeností postavy
 */
public class MoneyXpController extends BaseController implements Initializable {

    // region Constants

    public static final String MONEY = "money";
    public static final String EXPERIENCE = "experience";

    // endregion

    // region Variables

    // region FXML

    @FXML
    private MoneyLabel lblMoney;
    @FXML
    private LabeledProgressBar lblExperience;
    @FXML
    private TextField txtGold;
    @FXML
    private TextField txtSilver;
    @FXML
    private TextField txtCopper;
    @FXML
    private TextField txtExperience;

    // endregion

    private final Money moneyModel = new Money();
    private final MaxActValue goldValue = new MaxActValue(0, Money.MAX_GOLD, moneyModel.getGold());
    private final MaxActValue silverValue = new MaxActValue(0, Money.MAX_SILVER, moneyModel.getSilver());
    private final MaxActValue copperValue = new MaxActValue(0, Money.MAX_COPPER, moneyModel.getCopper());
    private final MaxActValue experienceModel = new MaxActValue(Integer.MAX_VALUE);
    private final Hero hero;

    private String title;

    // endregion

    // region Constructors

    public MoneyXpController(Context context) {
        this.hero = ((HeroManager)context.getManager(Context.MANAGER_HERO)).getHero().get();

        goldValue.actValueProperty().bindBidirectional(moneyModel.gold);
        silverValue.actValueProperty().bindBidirectional(moneyModel.silver);
        copperValue.actValueProperty().bindBidirectional(moneyModel.copper);

        experienceModel.update(hero.getExperiences());
    }

    // endregion

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        title = resources.getString(R.Translate.MONEY_TITLE);

        lblMoney.forMoney(hero.getMoney());
        lblExperience.setMaxActValue(hero.getExperiences());
    }

    @Override
    protected void onCreate(Bundle bundle) {
        FormUtils.initTextFormater(txtGold, goldValue);
        FormUtils.initTextFormater(txtSilver, silverValue);
        FormUtils.initTextFormater(txtCopper, copperValue);
        FormUtils.initTextFormater(txtExperience, experienceModel);
    }

    @Override
    protected void onResume() {
        setTitle(title);
        setScreenSize(400, 300);
    }

    // region Button handlers

    @FXML
    private void handleMoneyAdd(ActionEvent actionEvent) {
        hero.getMoney().add(moneyModel);
        moneyModel.setRaw(0);
    }

    @FXML
    private void handleMoneySubtract(ActionEvent actionEvent) {
        hero.getMoney().subtract(moneyModel);
        moneyModel.setRaw(0);
    }

    @FXML
    private void handleXpAdd(ActionEvent actionEvent) {
        hero.getExperiences().add(experienceModel.getActValue().intValue());
        experienceModel.setActValue(0);
    }

    @FXML
    private void handleCancel(ActionEvent actionEvent) {
        finish();
    }

    @FXML
    private void handleFinish(ActionEvent actionEvent) {
        setResult(RESULT_SUCCESS);
        finish(new Bundle()
            .putInt(MONEY, hero.getMoney().getRaw())
            .putInt(EXPERIENCE, hero.getExperiences().getActValue().intValue()));
    }

    // endregion
}

package cz.stechy.drd.db.table.ranged_weapon;

import static cz.stechy.drd.R.Database.Weaponranged.COLUMN_AUTHOR;
import static cz.stechy.drd.R.Database.Weaponranged.COLUMN_DESCRIPTION;
import static cz.stechy.drd.R.Database.Weaponranged.COLUMN_ID;
import static cz.stechy.drd.R.Database.Weaponranged.COLUMN_IMAGE;
import static cz.stechy.drd.R.Database.Weaponranged.COLUMN_NAME;
import static cz.stechy.drd.R.Database.Weaponranged.COLUMN_PRICE;
import static cz.stechy.drd.R.Database.Weaponranged.COLUMN_RAMPANCY;
import static cz.stechy.drd.R.Database.Weaponranged.COLUMN_RANGE_LONG;
import static cz.stechy.drd.R.Database.Weaponranged.COLUMN_RANGE_LOW;
import static cz.stechy.drd.R.Database.Weaponranged.COLUMN_RANGE_MEDIUM;
import static cz.stechy.drd.R.Database.Weaponranged.COLUMN_RENOWN;
import static cz.stechy.drd.R.Database.Weaponranged.COLUMN_STACK_SIZE;
import static cz.stechy.drd.R.Database.Weaponranged.COLUMN_STRENGTH;
import static cz.stechy.drd.R.Database.Weaponranged.COLUMN_TYPE;
import static cz.stechy.drd.R.Database.Weaponranged.COLUMN_UPLOADED;
import static cz.stechy.drd.R.Database.Weaponranged.COLUMN_WEIGHT;
import static cz.stechy.drd.R.Database.Weaponranged.TABLE_NAME;

import cz.stechy.drd.annotation.Table;
import cz.stechy.drd.annotation.Table.Type;
import cz.stechy.drd.db.base.BaseTableDefinitions;
import cz.stechy.drd.model.item.RangedWeapon;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Table(type = Type.DEFINITION, clazz = RangedWeapon.class)
public class RangedWeaponTableDefinitions extends BaseTableDefinitions<RangedWeapon> {

    // region Constants

    private static final String[] COLUMNS = new String[]{COLUMN_ID, COLUMN_NAME, COLUMN_DESCRIPTION,
        COLUMN_AUTHOR, COLUMN_WEIGHT, COLUMN_PRICE, COLUMN_STRENGTH, COLUMN_RAMPANCY,
        COLUMN_TYPE, COLUMN_RANGE_LOW, COLUMN_RANGE_MEDIUM, COLUMN_RANGE_LONG, COLUMN_RENOWN,
        COLUMN_IMAGE, COLUMN_STACK_SIZE, COLUMN_UPLOADED};
    static final String COLUMNS_KEYS = GENERATE_COLUMN_KEYS(COLUMNS);
    static final String COLUMNS_VALUES = GENERATE_COLUMNS_VALUES(COLUMNS);
    static final String COLUMNS_UPDATE = GENERATE_COLUMNS_UPDATE(COLUMNS);
    static final String QUERY_CREATE = String.format("CREATE TABLE IF NOT EXISTS %s("
            + "%s VARCHAR(255) PRIMARY KEY NOT NULL UNIQUE,"    // id
            + "%s VARCHAR(255) NOT NULL,"                       // name
            + "%s VARCHAR(255),"                                // description
            + "%s VARCHAR(255) NOT NULL,"                       // author
            + "%s INT NOT NULL,"                                // weight
            + "%s INT NOT NULL,"                                // price
            + "%s INT NOT NULL,"                                // strength
            + "%s INT NOT NULL,"                                // rampancy
            + "%s INT NOT NULL,"                                // type
            + "%s INT NOT NULL,"                                // range_low
            + "%s INT NOT NULL,"                                // range_medium
            + "%s INT NOT NULL,"                                // range_high
            + "%s INT NOT NULL,"                                // renown
            + "%s BLOB,"                                        // image
            + "%s INT NOT NULL,"                                // stack size
            + "%s BOOLEAN NOT NULL"                             // je položka nahraná
            + ");", TABLE_NAME, COLUMN_ID, COLUMN_NAME, COLUMN_DESCRIPTION, COLUMN_AUTHOR, COLUMN_WEIGHT,
        COLUMN_PRICE, COLUMN_STRENGTH, COLUMN_RAMPANCY, COLUMN_TYPE, COLUMN_RANGE_LOW,
        COLUMN_RANGE_MEDIUM, COLUMN_RANGE_LONG, COLUMN_RENOWN, COLUMN_IMAGE, COLUMN_STACK_SIZE,
        COLUMN_UPLOADED);

    // endregion

    // region Public methods

    @Override
    protected String getColumnKeys() {
        return COLUMNS_KEYS;
    }

    @Override
    public RangedWeapon parseResultSet(ResultSet resultSet) throws SQLException {
        return new RangedWeapon.Builder()
            .id(resultSet.getString(COLUMN_ID))
            .name(resultSet.getString(COLUMN_NAME))
            .description(resultSet.getString(COLUMN_DESCRIPTION))
            .author(resultSet.getString(COLUMN_AUTHOR))
            .weight(resultSet.getInt(COLUMN_WEIGHT))
            .price(resultSet.getInt(COLUMN_PRICE))
            .strength(resultSet.getInt(COLUMN_STRENGTH))
            .rampancy(resultSet.getInt(COLUMN_RAMPANCY))
            .weaponType(resultSet.getInt(COLUMN_TYPE))
            .rangeLow(resultSet.getInt(COLUMN_RANGE_LOW))
            .rangeMedium(resultSet.getInt(COLUMN_RANGE_MEDIUM))
            .rangeLong(resultSet.getInt(COLUMN_RANGE_LONG))
            .renown(resultSet.getInt(COLUMN_RENOWN))
            .image(readBlob(resultSet, COLUMN_IMAGE))
            .stackSize(resultSet.getInt(COLUMN_STACK_SIZE))
            .downloaded(true)
            .uploaded(resultSet.getBoolean(COLUMN_UPLOADED))
            .build();
    }

    @Override
    public List<Object> toParamList(RangedWeapon weapon) {
        return new ArrayList<>(Arrays.asList(
            weapon.getId(),
            weapon.getName(),
            weapon.getDescription(),
            weapon.getAuthor(),
            weapon.getWeight(),
            weapon.getPrice().getRaw(),
            weapon.getStrength(),
            weapon.getRampancy(),
            weapon.getWeaponType().ordinal(),
            weapon.getRangeLow(),
            weapon.getRangeMedium(),
            weapon.getRangeLong(),
            weapon.getRenown(),
            weapon.getImage(),
            weapon.getStackSize(),
            weapon.isUploaded()
        ));
    }

    @Override
    public RangedWeapon fromStringMap(Map<String, Object> map) {
        return new RangedWeapon.Builder()
            .id((String) map.get(COLUMN_ID))
            .name((String) map.get(COLUMN_NAME))
            .description((String) map.get(COLUMN_DESCRIPTION))
            .author((String) map.get(COLUMN_AUTHOR))
            .weight((Integer) map.get(COLUMN_WEIGHT))
            .price((Integer) map.get(COLUMN_PRICE))
            .strength((Integer) map.get(COLUMN_STRENGTH))
            .rampancy((Integer) map.get(COLUMN_RAMPANCY))
            .weaponType((Integer) map.get(COLUMN_TYPE))
            .rangeLow((Integer) map.get(COLUMN_RANGE_LOW))
            .rangeMedium((Integer) map.get(COLUMN_RANGE_MEDIUM))
            .rangeLong((Integer) map.get(COLUMN_RANGE_LONG))
            .renown((Integer) map.get(COLUMN_RENOWN))
            .image(base64ToBlob((String) map.get(COLUMN_IMAGE)))
            .stackSize((Integer) map.get(COLUMN_STACK_SIZE))
            .build();
    }

    @Override
    public Map<String, Object> toStringItemMap(RangedWeapon item) {
        final Map<String, Object> map = super.toStringItemMap(item);
        map.put(COLUMN_IMAGE, blobToBase64(item.getImage()));
        return map;
    }

    // endregion

}

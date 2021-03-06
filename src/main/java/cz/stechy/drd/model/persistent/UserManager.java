package cz.stechy.drd.model.persistent;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import cz.stechy.drd.model.db.base.Firebase;
import cz.stechy.drd.model.user.User;
import cz.stechy.drd.util.HashGenerator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Správce uivatele
 */
public class UserManager implements Firebase<User> {

    // region Constants

    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);

    private static final String FIREBASE_CHILD_NAME = "users";

    // region Názvy sloupců v databázi

    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_NAME = "name";

    // endregion

    // endregion

    // region Variables

    private final ObservableList<User> onlineDatabase = FXCollections.observableArrayList();
    private final DatabaseReference firebaseReference;
    private final ObjectProperty<User> user = new SimpleObjectProperty<>(new User());

    // endregion

    // region Constants

    /**
     * Vytvoří nového správce uživatelů
     *
     * @param firebase {@link FirebaseDatabase}
     */
    public UserManager(FirebaseDatabase firebase) {
        firebaseReference = firebase.getReference(FIREBASE_CHILD_NAME);
        firebaseReference.addChildEventListener(childEventListener);
    }

    // endregion

    // region Private methods

    private User parseDataSnapshot(DataSnapshot snapshot) {
        return new User.Builder()
            .id(snapshot.getKey())
            .name(snapshot.child(COLUMN_NAME).getValue(String.class))
            .password(snapshot.child(COLUMN_PASSWORD).getValue(String.class))
            .build();
    }

    private Map<String, Object> toMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put(COLUMN_NAME, user.getName());
        map.put(COLUMN_PASSWORD, user.getPassword());
        return map;
    }

    // endregion

    // region Public methods

    public void login(String username, String password) throws UserException {
        final Optional<User> result = onlineDatabase.stream()
            .filter(user -> user.getName().equals(username) && HashGenerator.checkSame(user.getPassword(), password))
            .findFirst();
        if (!result.isPresent()) {
            throw new UserException("User not found");
        }

        this.user.set(result.get());
        this.user.get().setLogged(true);
    }

    public void register(String username, String password) throws UserException {
        final Optional<User> result = onlineDatabase.stream()
            .filter(user -> user.getName().equals(username))
            .findFirst();
        if (result.isPresent()) {
            throw new UserException("User not found");
        }

        User user = new User(username, password);
        upload(user);
    }

    // endregion

    // region Getters & Setters

    public ObjectProperty<User> getUser() {
        return user;
    }

    // endregion

    @Override
    public void upload(User user) {
        final DatabaseReference child = firebaseReference.child(user.getId());
        child.setValue(toMap(user));
    }

    @Override
    public void deleteRemote(User user, boolean remote) {

    }

    private final ChildEventListener childEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            final User user = parseDataSnapshot(dataSnapshot);
            logger.trace("Přidávám uživatele {} z online databáze", user.toString());
            onlineDatabase.add(user);
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            logger.trace("Data uživatele byla změněna v online databázi");
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            final User u = parseDataSnapshot(dataSnapshot);
            logger.trace("Uživatel byl smazán z online databáze", u.toString());
            onlineDatabase.stream()
                .filter(u::equals)
                .findFirst()
                .ifPresent(onlineDatabase::remove);
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    public static class UserException extends Exception {

        public UserException() {
        }

        public UserException(String message) {
            super(message);
        }

        public UserException(String message, Throwable cause) {
            super(message, cause);
        }

        public UserException(Throwable cause) {
            super(cause);
        }
    }
}

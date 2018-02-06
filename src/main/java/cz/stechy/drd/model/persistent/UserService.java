package cz.stechy.drd.model.persistent;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DatabaseReference.CompletionListener;
import com.google.firebase.database.FirebaseDatabase;
import cz.stechy.drd.ThreadPool;
import cz.stechy.drd.di.Singleton;
import cz.stechy.drd.model.db.FirebaseWrapper;
import cz.stechy.drd.model.db.base.Firebase;
import cz.stechy.drd.model.user.User;
import cz.stechy.drd.util.HashGenerator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Služba spravující CRUD operace nad třídou {@link User}
 */
@Singleton
public final class UserService implements Firebase<User> {

    // region Constants

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private static final String FIREBASE_CHILD_NAME = "users";

    // region Názvy sloupců v databázi

    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_NAME = "name";

    // endregion

    // endregion

    // region Variables

    private final ObservableList<User> onlineDatabase = FXCollections.observableArrayList();
    private final ObjectProperty<User> user = new SimpleObjectProperty<>(this, "user", null);
    private DatabaseReference firebaseReference;

    // endregion

    // region Constructors

    /**
     * Vytvoří nového správce uživatelů
     *
     * @param wrapper {@link FirebaseDatabase}
     */
    public UserService(FirebaseWrapper wrapper) {
        wrapper.firebaseProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                onlineDatabase.clear();
                firebaseReference = newValue.getReference(FIREBASE_CHILD_NAME);
                firebaseReference.addChildEventListener(childEventListener);
            }
        });
    }

    // endregion

    // region Private methods

    @Override
    public void uploadAsync(User user, CompletionListener listener) {
        final DatabaseReference child = firebaseReference.child(user.getId());
        child.setValue(toFirebaseMap(user), listener);
    }

    @Override
    public void deleteRemoteAsync(User item, boolean remote,
        CompletionListener listener) {
        throw new NotImplementedException();
    }

    // endregion

    // region Public methods

    @Override
    public User parseDataSnapshot(DataSnapshot snapshot) {
        return new User.Builder()
            .id(snapshot.getKey())
            .name(snapshot.child(COLUMN_NAME).getValue(String.class))
            .password(snapshot.child(COLUMN_PASSWORD).getValue(String.class))
            .build();
    }

    @Override
    public Map<String, Object> toFirebaseMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put(COLUMN_NAME, user.getName());
        map.put(COLUMN_PASSWORD, user.getPassword());
        return map;
    }

    /**
     * Přihlásí uživatele do aplikace
     *
     * @param username Uživatelské jméno
     * @param password Uživatelské heslo
     * @throws UserException Pokud se přihlášení nepodaří
     */
    public CompletableFuture<User> loginAsync(String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            final Optional<User> result = onlineDatabase.stream()
                .filter(user -> user.getName().equals(username) && HashGenerator
                    .checkSame(user.getPassword(), password))
                .findFirst();
            if (!result.isPresent()) {
                throw new UserException();
            }

            return result.get();
        }).thenApplyAsync(user -> {
            this.user.setValue(user);
            getUser().setLogged(true);
            return user;
        }, ThreadPool.JAVAFX_EXECUTOR);
    }

    /**
     * Odhlásí uživatele z aplikace
     */
    public CompletableFuture<Void> logoutAsync() {
        return CompletableFuture.supplyAsync(() -> {
            getUser().setLogged(false);
            user.set(null);
            return null;
        });
    }

    /**
     * Zaregistruje nového uživatele
     *
     * @param username Uživatelské jméno
     * @param password Uživatelské heslo
     * @throws UserException Pokud se registrace nezdaří
     */
    public void registerAsync(String username, String password, CompletionListener listener) {
        final Optional<User> result = onlineDatabase.stream()
            .filter(user -> user.getName().equals(username))
            .findFirst();
        if (result.isPresent()) {
            listener.onComplete(DatabaseError.fromCode(DatabaseError.USER_CODE_EXCEPTION), null);
        }

        final User user = new User(username, password);
        uploadAsync(user, listener);
    }

    // endregion

    // region Getters & Setters

    public final ReadOnlyObjectProperty<User> userProperty() {
        return user;
    }

    public final User getUser() {
        return user.get();
    }

    // endregion

    private final ChildEventListener childEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            final User user = parseDataSnapshot(dataSnapshot);
            LOGGER.trace("Přidávám uživatele {} z online databáze", user.toString());
            onlineDatabase.add(user);
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            LOGGER.trace("Data uživatele byla změněna v online databázi");
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            final User u = parseDataSnapshot(dataSnapshot);
            LOGGER.trace("Uživatel byl smazán z online databáze", u.toString());
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

    public static class UserException extends RuntimeException {

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

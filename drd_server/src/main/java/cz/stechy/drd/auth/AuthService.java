package cz.stechy.drd.auth;

import cz.stechy.drd.Client;
import cz.stechy.drd.ServerDatabase;
import cz.stechy.drd.crypto.CryptoService;
import cz.stechy.drd.firebase.ItemEventListener;
import cz.stechy.drd.net.message.AuthMessage;
import cz.stechy.drd.net.message.AuthMessage.AuthAction;
import cz.stechy.drd.net.message.AuthMessage.AuthMessageData;
import cz.stechy.drd.net.message.DatabaseMessage.DatabaseMessageCRUD.DatabaseAction;
import cz.stechy.drd.net.message.MessageSource;
import cz.stechy.drd.util.HashGenerator;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Třída starající se o správu uživatelů (registrace, přihlášení)
 */
public final class AuthService {

    // region Constants

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    public static final String FIREBASE_CHILD = "users";
    
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PASSWORD = "password";

    // endregion

    // region Variables

    private final List<User> users = new ArrayList<>();
    private final ServerDatabase repository;
    private final CryptoService cryptoService;

    // endregion

    // region Constructors

    public AuthService(ServerDatabase repository, CryptoService cryptoService) {
        this.repository = repository;
        this.cryptoService = cryptoService;
    }

    // endregion

    // region Private methods

    /**
     * Konvertuje {@link Map} na instanci třídy {@link User}
     *
     * @param map
     * @return {@link User}
     */
    private User mapToUser(Map<String, Object> map) {
        return new User(
            (String) map.get(COLUMN_ID),
            (String) map.get(COLUMN_NAME),
            (String) map.get(COLUMN_PASSWORD)
        );
    }

    /**
     * Konvertuje instanci třídy {@link User} na {@link Map}
     *
     * @param user {@link User}
     * @return
     */
    private Map<String, Object> userToMap(User user) {
        final Map<String, Object> map = new HashMap<>();
        map.put(COLUMN_ID, user.id);
        map.put(COLUMN_NAME, user.name);
        map.put(COLUMN_PASSWORD, user.password);
        return map;
    }

    // endregion

    // region Public methods

    public void init() {
        this.repository.registerListener(FIREBASE_CHILD, this.userListener);
    }

    public void register(byte[] usernameRaw, byte[] passwordRaw, Client client) {
        final String username = new String(cryptoService.decrypt(usernameRaw), StandardCharsets.UTF_8);
        final String password = new String(cryptoService.decrypt(passwordRaw), StandardCharsets.UTF_8);
        final Optional<User> result = users.stream().filter(user -> user.name.equals(username)).findFirst();
        if (result.isPresent()) {
            client.sendMessage(new AuthMessage(MessageSource.SERVER, AuthAction.REGISTER,
                false, new AuthMessageData()));
            return;
        }

        final User user = new User(username, password);
        final Map<String, Object> map = userToMap(user);
        this.repository.performInsert(FIREBASE_CHILD, map, user.id);
        client.sendMessage(new AuthMessage(MessageSource.SERVER, AuthAction.REGISTER,
            true, new AuthMessageData(user.id, usernameRaw, passwordRaw)));
    }

    public void login(byte[] usernameRaw, byte[] passwordRaw, Client client) {
        final String username = new String(cryptoService.decrypt(usernameRaw), StandardCharsets.UTF_8);
        final String password = new String(cryptoService.decrypt(passwordRaw), StandardCharsets.UTF_8);
        final Optional<User> result = users.stream().filter(user ->
            user.name.equals(username) && HashGenerator.checkSame(user.password, password))
            .findFirst();

        if (!result.isPresent()) {
            client.sendMessage(new AuthMessage(MessageSource.SERVER, AuthAction.LOGIN,
                false, new AuthMessageData()));
            return;
        }
        client.sendMessage(new AuthMessage(MessageSource.SERVER, AuthAction.LOGIN,
            true, new AuthMessageData(result.get().id, usernameRaw, passwordRaw)));
    }

    // endregion

    private final ItemEventListener userListener = event -> {
        final DatabaseAction action = event.getAction();
        final Map<String, Object> userMap = event.getItem();
        final User user = mapToUser(userMap);
        switch (action) {
            case CREATE:
                LOGGER.info("Přidávám nového uživatele do svého povědomí." + user.name);
                users.add(user);
                break;
            case UPDATE:
                break;
            case DELETE:
                LOGGER.info("Odebírám uživatele z databáze." + user.name);
                users.remove(user);
                break;
            default:
                throw new RuntimeException("Neplatny parametr");
        }
    };

    private static final class User {
        public final String id;
        public final String name;
        public final String password;

        public User(String name, String password) {
            this(HashGenerator.createHash(), name, HashGenerator.createHash(password));
        }

        public User(String id, String name, String password) {
            this.id = id;
            this.name = name;
            this.password = password;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            User user = (User) o;
            return Objects.equals(id, user.id) &&
                Objects.equals(name, user.name) &&
                Objects.equals(password, user.password);
        }

        @Override
        public int hashCode() {

            return Objects.hash(id, name, password);
        }
    }
}

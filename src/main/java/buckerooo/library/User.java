package buckerooo.library;

import com.google.common.base.Objects;

/* test this!! */
public class User {
    public final String username;

    public User(String username) {
        this.username = username;
    }

    public static User user(String username) {
        return new User(username);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equal(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }
}

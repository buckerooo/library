package buckerooo.library;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.time.Clock.fixed;
import static java.time.Instant.now;
import static java.time.ZoneId.systemDefault;

public class LibraryLoadTest {
    @Test
    public void loadUpTheLibrary() {
        List<Item> items = new ArrayList<>();

        for (int i = 0; i < 1_000_000; i++) {
            /* make this much better */
             items.add(Item.book(String.valueOf(i), String.valueOf(i), String.valueOf(i)));
        }


        Library library = new Library(fixed(now(), systemDefault()), items);
    }
}

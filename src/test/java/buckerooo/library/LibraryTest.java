package buckerooo.library;

import com.google.common.collect.Lists;
import com.sun.tools.javac.jvm.Items;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static buckerooo.library.ItemType.Book;
import static buckerooo.library.Item.book;
import static buckerooo.library.Item.dvd;
import static buckerooo.library.Item.vhs;
import static buckerooo.library.ItemType.DVD;
import static buckerooo.library.ItemType.VHS;
import static java.time.Clock.fixed;
import static java.time.Clock.systemUTC;
import static java.time.Instant.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class LibraryTest {

    @Test
    public void canGetTheCurrentInventoryOfAllTheLoanableItems() {
        List<Item> itemsInTheLibrary = asList(
                dvd("1", "7", "Pi"),
                dvd("2", "7", "Pi"),
                book("3", "4", "Introduction to Algorithms"),
                vhs("4", "5", "WarGames"));

        Library library = new Library(systemUTC(), itemsInTheLibrary);

        assertThat(library.currentInventory(), equalTo(itemsInTheLibrary));
    }

    @Test
    public void canBorrowAnItemFromTheLibrary() throws Exception {

        Clock now = fixed(now(), systemDefault());

        Item itemToBeBorrowed = book("3", "4", "Introduction to Algorithms");
        Library library = new Library(now, asList(
                dvd ("1", "7", "Pi"),
                dvd ("2", "7", "Pi"),
                itemToBeBorrowed,
                vhs ("4", "5", "WarGames"))
        );

        Receipt receipt = library.borrowItem("Introduction to Algorithms", Book);

        assertThat(receipt.returnDate, equalTo(LocalDate.now(now)));
        assertThat(receipt.item, equalTo(itemToBeBorrowed));
    }

    @Test
    public void throwsExceptionIfWeCannotFindTheItemToBeBorrowed() throws Exception {
        Library library = new Library(fixed(now(), systemDefault()), emptyList());

        try {
            library.borrowItem("some random item", VHS);
            fail("The item should not have been found");
        } catch (ItemNotFoundException e) {
            assertThat(e.getMessage(), equalTo("Could not find the VHS, some random item, you want to borrow"));
        }
    }

    @Test
    public void throwsExceptionIfTheItemIsCurrentlyOutOfStock() throws Exception {
        Library library = new Library(fixed(now(), systemDefault()),
                singletonList(dvd("1", "7", "Pi"))
        );

        library.borrowItem("Pi", DVD);

        try {
            library.borrowItem("Pi", DVD);
            fail("The item should have been out of stock");
        } catch (ItemOutOfStockException e) {
            assertThat(e.getMessage(), equalTo("The Pi DVD is currently out of stock"));
        }
    }

    @Test
    public void canBorrowAnItemAndThenReturnIt() throws Exception {
        List<Item> libraryItems = singletonList(dvd("1", "7", "Pi"));
        Library library = new Library(fixed(now(), systemDefault()), libraryItems);

        assertThat("should be a single item in the library", library.currentInventory(), equalTo(libraryItems));

        Receipt receiptForBorrowedItem = library.borrowItem("Pi", DVD);

        assertThat("the only item has been taken, so library should be empty", library.currentInventory(), equalTo(emptyList()));

        library.returnItem(receiptForBorrowedItem.item);

        assertThat("item should now be back in the library", library.currentInventory(), equalTo(libraryItems));
    }

    @Test
    public void throwsExceptionIfWeTryAndReturnAItemWeDoNotKnowAbout() {
        List<Item> libraryItems = singletonList(dvd("1", "7", "Pi"));
        Library library = new Library(fixed(now(), systemDefault()), libraryItems);

        try {
            library.returnItem(book("1", "1", "some random item"));
        } catch(ItemNotFoundException e) {
            assertThat(e.getMessage(), equalTo("Could not the item: some random item, Book with id 1"));
        }
    }

    @Test
    public void throwsExceptionIfWeTryAndReturnAItemWithAUniqueIdWeDoNotKnowAbout() {
        List<Item> libraryItems = singletonList(dvd("1", "7", "Pi"));
        Library library = new Library(fixed(now(), systemDefault()), libraryItems);

        try {
            library.returnItem(dvd("2", "7", "Pi"));
        } catch(ItemNotFoundException e) {
            assertThat(e.getMessage(), equalTo("Could not the item: Pi, DVD with id 2"));
        }
    }

    @Test
    public void canGetAListOfAllTheOverdueItems() throws Exception {
        Item item1 = dvd("1", "7", "Pi");
        Item item2 = dvd("2", "7", "Pi 2");
        List<Item> libraryItems = asList(item1, item2);

        MoveableClock clock = new MoveableClock();
        Library library = new Library(clock, libraryItems);

        library.borrowItem("Pi", DVD);

        clock.moveForward(1, DAYS);

        library.borrowItem("Pi 2", DVD);

        clock.moveForward(7, DAYS);
        assertThat(library.overdueItems(), equalTo(singletonList(item1)));

        clock.moveForward(1, DAYS);
        assertThat(library.overdueItems(), equalTo(asList(item1, item2)));
    }

    @Test
    @Ignore
    public void makeSureWeOnlyGiveOutTheItemsWeHaveInTheLibrary() throws InterruptedException, ExecutionException {

        Library library = new Library(fixed(now(), systemDefault()),
                asList(dvd("1", "7", "Pi"), dvd("2", "7", "Pi"))
        );

        /* get 20 people to all try and take the same book out at the same time */
        List<Future<Receipt>> attemptsToBorrowItem = newFixedThreadPool(20).invokeAll(
                nCopies(20, () -> library.borrowItem("Pi", DVD))
        );

        List<Receipt> actualItemsBorrowed = new ArrayList<>();
        int failedAttemptsToBorrowItem = 0;
        for (Future<Receipt> receiptFuture : attemptsToBorrowItem) {
            try {
                actualItemsBorrowed.add(receiptFuture.get());
            } catch (ExecutionException e) {
                if (e.getCause() instanceof ItemOutOfStockException) {
                    failedAttemptsToBorrowItem++;
                } else {
                    throw e;
                }
            }
        }

        assertThat("Only 2 copies of the item should have been borrowed", actualItemsBorrowed.size(), equalTo(2));
        assertThat(failedAttemptsToBorrowItem, equalTo(18));
    }

}

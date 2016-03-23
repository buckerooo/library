package buckerooo.library;

import org.junit.Ignore;
import org.junit.Test;

import java.time.Clock;
import java.time.LocalDate;
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
import static buckerooo.library.User.user;
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

        Receipt receipt = library.borrowItem("Introduction to Algorithms", Book, user("buck"));

        assertThat(receipt.returnDate, equalTo(LocalDate.now(now)));
        assertThat(receipt.item, equalTo(itemToBeBorrowed));
    }

    @Test
    public void canSeeTheItemsAUserHasCurrentlyBorrowed() throws Exception {

        Clock now = fixed(now(), systemDefault());

        Item itemToBeBorrowed1 = book("3", "4", "Introduction to Algorithms");
        Item itemToBeBorrowed2 = vhs("4", "5", "WarGames");
        Library library = new Library(now, asList(
                dvd ("1", "7", "Pi"),
                dvd ("2", "7", "Pi"),
                itemToBeBorrowed1,
                itemToBeBorrowed2)
        );

        User user = user("buck");
        library.borrowItem(itemToBeBorrowed1.title, itemToBeBorrowed1.type, user);
        library.borrowItem(itemToBeBorrowed2.title, itemToBeBorrowed2.type, user);

        assertThat(library.borrowedItems(user), equalTo(asList(itemToBeBorrowed1, itemToBeBorrowed2)));

        library.returnItem(itemToBeBorrowed2);
        assertThat(library.borrowedItems(user), equalTo(singletonList(itemToBeBorrowed1)));

        library.returnItem(itemToBeBorrowed1);
        assertThat(library.borrowedItems(user), equalTo(asList()));
    }

    @Test
    public void throwsExceptionIfWeCannotFindTheItemToBeBorrowed() throws Exception {
        Library library = new Library(fixed(now(), systemDefault()), emptyList());

        try {
            library.borrowItem("some random item", VHS, user("buck"));
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

        library.borrowItem("Pi", DVD, user("buck"));

        try {
            library.borrowItem("Pi", DVD, user("buck"));
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

        Receipt receiptForBorrowedItem = library.borrowItem("Pi", DVD, user("buck"));

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

        library.borrowItem("Pi", DVD, user("buck"));

        clock.moveForward(1, DAYS);

        library.borrowItem("Pi 2", DVD, user("buck"));

        clock.moveForward(7, DAYS);
        assertThat(library.overdueItems(), equalTo(singletonList(item1)));

        clock.moveForward(1, DAYS);
        assertThat(library.overdueItems(), equalTo(asList(item1, item2)));
    }

    @Test
    public void makeSureWeOnlyGiveOutTheItemsWeHaveInTheLibrary() throws InterruptedException, ExecutionException {

        Library library = new Library(fixed(now(), systemDefault()),
                asList(dvd("1", "7", "Pi"), dvd("2", "7", "Pi"))
        );

        /* get 20 people to all try and take the same book out at the same time */
        List<Future<Receipt>> attemptsToBorrowItem = newFixedThreadPool(20).invokeAll(
                nCopies(20, () -> library.borrowItem("Pi", DVD, user("buck")))
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

    @Test
    @Ignore
    public void borrowAndReturnALargeAmountOfItems() throws InterruptedException {
        List<Item> allLibraryItems = new ArrayList<>();

        nCopiesOf("Pi", 10, allLibraryItems);
        nCopiesOf("Pi 2", 10, allLibraryItems);

        Library library = new Library(fixed(now(), systemDefault()), allLibraryItems);

        /* borrow all the items */
        List<Future<Receipt>> attemptsToBorrowPiItem = newFixedThreadPool(5).invokeAll(
                nCopies(50, () -> library.borrowItem("Pi", DVD, user("buck")))
        );
        List<Future<Receipt>> attemptsToBorrowPi2Item = newFixedThreadPool(5).invokeAll(
                 nCopies(50, () -> library.borrowItem("Pi 2", DVD, user("buck")))
        );

        List<Future<Receipt>> allItemsToBorrow = new ArrayList<>();
        allItemsToBorrow.addAll(attemptsToBorrowPiItem);
        allItemsToBorrow.addAll(attemptsToBorrowPi2Item);

        List<Receipt> actualItemsBorrowed = new ArrayList<>();
        for (Future<Receipt> receiptFuture : allItemsToBorrow) {
            try {
                actualItemsBorrowed.add(receiptFuture.get());
            } catch (ExecutionException ignore) {}
        }

//        assertThat("we should have borrowed all the items", actualItemsBorrowed.size(), equalTo(allLibraryItems.size()));
        assertThat(library.currentInventory().size(), equalTo(0));

        /* return all the items */
        List<Callable<Void>> allItemsToReturn = new ArrayList<>();
        for (Receipt receiptFromBorrowedItem : actualItemsBorrowed) {
            allItemsToReturn.add(() -> {
                library.returnItem(receiptFromBorrowedItem.item);
                return null;
            });
        }
        for (Future<Void> itemBeingReturned : newFixedThreadPool(5).invokeAll(allItemsToReturn)) {
            try {
                itemBeingReturned.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        /* check what we have in stock */
        assertThat(library.currentInventory().size(), equalTo(allLibraryItems.size()));
    }

    private void nCopiesOf(String title, int numberOfCopies, List<Item> allLibraryItems) {
        int currentItemsSize = allLibraryItems.size();
        for (int i = allLibraryItems.size(); i <= currentItemsSize + numberOfCopies; i++) {
            allLibraryItems.add(dvd(String.valueOf(i), "7", title));
        }
    }

}

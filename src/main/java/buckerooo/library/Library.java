package buckerooo.library;

import com.google.common.base.Objects;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sun.tools.javac.jvm.Items;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static buckerooo.library.ItemNotFoundException.itemNotFound;
import static buckerooo.library.ItemOutOfStockException.itemOutOfStock;
import static java.util.stream.Collectors.toList;

public class Library {
    private final Clock clock;
    private final Multimap<ItemKey, StockItem> libraryItems;

    public Library(Clock clock, List<Item> libraryItems) {
        this.clock = clock;

        /* i don't want to create the list, can i keep it as a stream */
        List<StockItem> stockItems = libraryItems.stream().map(StockItem::new).collect(toList());

        this.libraryItems = Multimaps.index(stockItems, stockItem -> {
            return new ItemKey(stockItem.item.title, stockItem.item.type);
        });
    }

    public List<Item> currentInventory() {
        return libraryItems.values()
                .stream()
                .filter(StockItem::inStock)
                .map(stockItem -> stockItem.item)
                .collect(toList());
    }

    public Receipt borrowItem(String title, ItemType type) throws ItemNotFoundException, ItemOutOfStockException {
        if (! libraryItems.containsKey(new ItemKey(title, type))) {
           throw itemNotFound(title, type);
        }

        /* look to see if any items are in stock */
        Collection<StockItem> items = libraryItems.get(new ItemKey(title, type));
        StockItem itemToBeBorrowed = items.stream()
                .filter(StockItem::inStock)
                .findFirst()
                .orElseThrow(() -> itemOutOfStock(title, type));

        /* we have an item so lets borrow it */
        itemToBeBorrowed.borrowItem();

        return new Receipt(LocalDate.now(clock), itemToBeBorrowed.item);
    }

    public void returnItem(Item item) throws ItemNotFoundException {
        StockItem foundStockItem = libraryItems.get(new ItemKey(item.title, item.type)).stream()
                .filter(stockItem -> stockItem.item.uniqueId.equals(item.uniqueId))
                .findFirst()
                .orElseThrow(() -> itemNotFound(item));

        foundStockItem.returnItem();
    }

    public List<Item> overdueItems() {
        return libraryItems.values()
                .stream()
                .filter(stockItem -> !stockItem.inStock())
                .filter(stockItem -> stockItem.borrowedDate().isBefore(LocalDate.now(clock).minusDays(7)))
                .map(stockItem -> stockItem.item)
                .collect(toList());
    }

    /* test this!! */
    public class StockItem {
        public final Item item;

        private LocalDateTime borrowedTime;

        public StockItem(Item item) {
            this.item = item;
        }

        public void borrowItem() {
            this.borrowedTime = LocalDateTime.now(clock);
        }

        public void returnItem() {
            this.borrowedTime = null;
        }

        public boolean inStock() {
            return this.borrowedTime == null;
        }

        public LocalDate borrowedDate() {
            return borrowedTime.toLocalDate();
        }
    }

    private static class ItemKey {

        private final String title;
        private final ItemType type;

        public ItemKey(String title, ItemType type) {
            this.title = title;
            this.type = type;
        }

        /* test this!!!! */

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ItemKey itemKey = (ItemKey) o;
            return Objects.equal(title, itemKey.title) &&
                    type == itemKey.type;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(title, type);
        }
    }
}

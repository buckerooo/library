package buckerooo.library;

import static buckerooo.library.ItemType.Book;
import static buckerooo.library.ItemType.DVD;
import static buckerooo.library.ItemType.VHS;

public class Item {

    public final String uniqueId;
    public final String bookId;
    public final ItemType type;
    public final String title;

    public Item(String uniqueId, String bookId, ItemType type, String title) {
        this.uniqueId = uniqueId;
        this.bookId = bookId;
        this.type = type;
        this.title = title;
    }

    public static Item dvd(String uniqueId, String bookId, String title) {
        return new Item(uniqueId, bookId, DVD, title);
    }

    public static Item book(String uniqueId, String bookId, String title) {
        return new Item(uniqueId, bookId, Book, title);
    }

    public static Item vhs(String uniqueId, String bookId, String title) {
        return new Item(uniqueId, bookId, VHS, title);
    }

}

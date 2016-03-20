package buckerooo.library;

public class ItemNotFoundException extends Exception {

    private ItemNotFoundException(String message) {
        super(message);
    }

    public static ItemNotFoundException itemNotFound(String title, ItemType type) {
        return new ItemNotFoundException("Could not find the " + type.name() + ", " + title + ", you want to borrow");
    }

    public static ItemNotFoundException itemNotFound(Item item) {
        return new ItemNotFoundException("Could not the item: " + item.title + ", " + item.type.name() + " with id " + item.uniqueId);
    }
}

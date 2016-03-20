package buckerooo.library;

public class ItemOutOfStockException extends Exception {

    private ItemOutOfStockException(String message) {
        super(message);
    }

    public static ItemOutOfStockException itemOutOfStock(String title, ItemType type) {
        return new ItemOutOfStockException("The " + title + " " + type.name() + " is currently out of stock");
    }
}

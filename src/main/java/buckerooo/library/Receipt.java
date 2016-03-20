package buckerooo.library;

import java.time.LocalDate;

public class Receipt {
    public final LocalDate returnDate;
    public final Item item;

    public Receipt(LocalDate returnDate, Item item) {
        this.returnDate = returnDate;
        this.item = item;
    }
}

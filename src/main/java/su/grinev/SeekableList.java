package su.grinev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SeekableList<T> {

    private final List<T> list;
    private int position;

    public SeekableList(Collection<T> list) {
        position = 0;
        this.list = new ArrayList<>();
        this.list.addAll(list);
    }

    public boolean isEnd() {
        return position == list.size();
    }

    public T next() {
        return list.get(position++);
    }

    public void seek(int n) {
        position += n;
    }

    public T prev() {
        return list.get(position--);
    }

    public T get() {
        return list.get(position);
    }

    public int atPos() {
        return position;
    }
}

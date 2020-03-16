package ru.ifmo.rain.lyubavina.arrayset;

import java.util.*;


public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> list;
    private final Comparator<? super T> comparator;
    private ArraySet<T> descendingSet = null;

    public ArraySet() {
        this.list = Collections.emptyList();
        this.comparator = null;
    }


    public ArraySet(Collection<? extends T> list, Comparator<? super T> comparator) {
        this.comparator = comparator;
        Set<T> set;
        if (comparator != null) {
            set = new TreeSet<>(comparator);
            set.addAll(list);
        } else {
            set = new TreeSet<>(list);
        }

        this.list = List.copyOf(set);
    }

    public ArraySet(Collection<? extends T> list) {
        this(list, null);
    }

    //    list is already sorted
    private ArraySet(List<T> list, Comparator<? super T> comparator) {
        this.list = list;
        this.comparator = comparator;

    }


    @Override
    public Comparator<? super T> comparator() {
        return this.comparator;
    }

    @Override
    public T lower(T t) { //last <
        int pos = binarySearch(t);
        if (pos < 0) pos = getPosition(pos);
        return (pos == 0 ? null : list.get(pos - 1));
    }

    @Override
    public T floor(T t) { // last <=
        int pos = binarySearch(t);
        if (pos < 0) {
            pos = getPosition(pos);
            pos--;
        }
        return (pos >= 0 ? list.get(pos) : null);
    }

    @Override
    public T ceiling(T t) { // first >=
        int pos = binarySearch(t);
        if (pos < 0) pos = getPosition(pos);
        return (pos == size() ? null : list.get(pos));
    }

    @Override
    public T higher(T t) { //first >
        int pos = binarySearch(t);
        if (pos < 0) {
            pos = getPosition(pos);
        } else {
            pos++;
        }
        return (pos < size() ? list.get(pos) : null);
    }


    @Override
    public int size() {
        return list.size();
    }


    @Override
    public T first() {
        if (isEmpty()) throw new NoSuchElementException();
        return list.get(0);
    }

    @Override
    public T last() {
        if (isEmpty()) throw new NoSuchElementException();
        return list.get(size() - 1);
    }

    private int binarySearch(T key) {
        return Collections.binarySearch(this.list, key, this.comparator);
    }

    private int getPosition(int pos) {
        if (pos >= 0) return pos;
        return -pos - 1;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Object[] toArray() {
        if (list == null) {
            return new Object[0];
        }
        return list.toArray();
    }


    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public NavigableSet<T> descendingSet() {

        if (this.descendingSet != null) return this.descendingSet;
        List<T> reversed = new ArrayList<>(this.list);
        Collections.reverse(reversed);
        return this.descendingSet = new ArraySet<>(reversed, Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }


    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }


    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        if (o == null) throw new NullPointerException();
        return (binarySearch((T) o) >= 0);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        int pos = getBorder(toElement, !inclusive);
        return new ArraySet<>(this.list.subList(0, pos), this.comparator);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        int pos = getBorder(fromElement, inclusive);
        return new ArraySet<>(this.list.subList(pos, size()), this.comparator);
    }

    private int getBorder(T element, boolean inclusive) {
        int pos = binarySearch(element);
        if (pos < 0) {
            pos = getPosition(pos);
        } else if (!inclusive) pos++;
        return pos;
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }


    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }


    @SuppressWarnings("unchecked")
    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {

        if (comparator() != null) {
            if (comparator().compare(fromElement, toElement) > 0) {
                throw new IllegalArgumentException();
            }
        } else if (((Comparable<T>) fromElement).compareTo(toElement) > 0) {
            throw new IllegalArgumentException();
        }
        return headSet(toElement, toInclusive).tailSet(fromElement, fromInclusive);
    }


    public static void main(String[] args) {
//        ArrayList<Integer> arrayList = new ArrayList<>();
//        arrayList.add(1);
//        ArraySet<Integer> arraySet = new ArraySet<>(arrayList);
//        arraySet.remove(1);
        TreeSet<Integer> treeSet = new TreeSet<>();
        treeSet.add(1);
        treeSet.contains(null);

    }
}
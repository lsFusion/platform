package lsfusion.base.col.implementations.order;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.ArCol;
import lsfusion.base.col.implementations.abs.AList;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.mutable.MList;

public class ArList<K> extends AList<K> implements MList<K> {

    private final ArCol<K> col;

    public int size() {
        return col.size();
    }

    public K get(int i) {
        return col.get(i);
    }

    public void set(int i, K value) {
        col.set(i, value);
    }

    public ImCol<K> getCol() {
        return col;
    }

    public void add(K key) {
        col.add(key);
    }

    public void addFirst(K key) {
        col.addFirst(key);
    }

    public void addAll(ImList<? extends K> list) {
        for(int i=0,size=list.size();i<size;i++)
            add(list.get(i));
    }

    public ImList<K> immutableList() {
        if(col.size==0)
            return ListFact.EMPTY();
        if(col.size==1)
            return ListFact.singleton(single());

        if(col.array.length > col.size * SetFact.factorNotResize) {
            Object[] newArray = new Object[col.size];
            System.arraycopy(col.array, 0, newArray, 0, col.size);
            col.array = newArray;
        }
        return this;
    }

    public ArList() {
        col = new ArCol<>();
    }

    public ArList(int size) {
        col = new ArCol<>(size);
    }

    public ArList(ArCol<K> col) {
        this.col = col;
    }

    public ArList(ArList<K> list) {
        col = new ArCol<>(list.col);
    }

    public void removeAll() {
        col.removeAll();
    }
    public void removeLast() {
        col.removeLast();
    }
}

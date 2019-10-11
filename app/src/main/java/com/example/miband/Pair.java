package com.example.miband;

public class Pair<E,V> {

    private E e;
    private V v;

    public Pair(E e, V v){
        this.e = e;
        this.v = v;
    }

    public E getFirst(){
        return e;
    }

    public V getSecond(){
        return v;
    }
}

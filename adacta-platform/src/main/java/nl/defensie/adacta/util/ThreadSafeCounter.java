package nl.defensie.adacta.util;

public class ThreadSafeCounter {
    private int value = 0;
    
    public synchronized void increment() {
        value++;
    }
    
    public synchronized void decrement() {
        value--;
    }
    
    public int getValue() {
        return value;
    }
}

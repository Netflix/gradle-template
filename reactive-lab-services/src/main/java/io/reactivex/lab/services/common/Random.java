package io.reactivex.lab.services.common;

public class Random {

    public static int randomIntFrom0to100() {
        // XORShift instead of Math.random http://javamex.com/tutorials/random_numbers/xorshift.shtml
        // benjchristensen => also ... I've personally experienced Math.random() giving bad performance in certain environments when called alot
        long x = System.nanoTime();
        x ^= (x << 21);
        x ^= (x >>> 35);
        x ^= (x << 4);
        return Math.abs((int) x % 100);
    }

}

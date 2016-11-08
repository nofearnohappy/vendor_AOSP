package com.mediatek.rcs.pam.util;

/**
 * this class used for storage three datas.
 *
 * @param <T1> 1st type data
 * @param <T2> 2ed type data
 * @param <T3> 3th type data
 */
public class Triplet<T1, T2, T3> {
    public T1 first;
    public T2 second;
    public T3 third;

    /**
     * @param first the 1st data
     * @param second the 2ed data
     * @param third the 3th data
     */
    public Triplet(T1 first, T2 second, T3 third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }
}

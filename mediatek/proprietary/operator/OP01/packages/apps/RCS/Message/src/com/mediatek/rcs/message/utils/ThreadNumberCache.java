package com.mediatek.rcs.message.utils;

import android.util.Log;
import android.util.LruCache;

public class ThreadNumberCache {
    private static final String TAG = ThreadNumberCache.class.getSimpleName();
    private static final int MAX_CACHE_SIZE = 200;
    private static final LruCache<Long, String[]> THREAD_CACHE = new LruCache<Long, String[]>(
            MAX_CACHE_SIZE);

    /**
     * Save a threadId and relevant tag  into cache
     * @param threadId The threadId to be cached
     * @param tag The tag to be cached
     */
    public static boolean saveThreadandNumbers(long threadId, String[] numbers) {
        Log.d(TAG, "saveThreadandNumbers() entry threadId is " + threadId);
        if (numbers == null || numbers.length == 0) {
            Log.e(TAG, "numbers can not be null");
            return false;
        }
        for (int index = 0; index < numbers.length; index ++) {
            Log.d(TAG, "saveThreadandNumbers() index = " +index + ", number = " + numbers[index]);
        }
        THREAD_CACHE.put(threadId, numbers);
        return true;
    }

    public static String[] getNumbersbyThreadId(long threadId) {
        Log.d(TAG, "getNumbersbyThreadId() entry, threadId is " + threadId);
        String[] numbers = THREAD_CACHE.get(threadId);
        return numbers;
    }
}

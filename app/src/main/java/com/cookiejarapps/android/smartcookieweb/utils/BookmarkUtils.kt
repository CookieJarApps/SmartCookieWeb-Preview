package com.cookiejarapps.android.smartcookieweb.utils;

public final class BookmarkUtils {

    private long lastId = getTime();

    private synchronized long createId() {
        while (true) {
            long currentTime = getTime();

            if (currentTime / 1000 == lastId / 1000) {
                if (lastId % 1000 < 999) {
                    return ++lastId;
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            } else if (currentTime < lastId) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            return lastId = currentTime;
        }
    }

    private static long getTime() {
        long time = System.currentTimeMillis() / 1000;
        return time * 1000;
    }

    private static class InstanceHolder {
        static final BookmarkUtils INSTANCE = new BookmarkUtils();
    }

    public static long getNewId() {
        return InstanceHolder.INSTANCE.createId();
    }
}

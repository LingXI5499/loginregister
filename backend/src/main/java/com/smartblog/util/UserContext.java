package com.smartblog.util;

import com.smartblog.vo.CurrentUser;

public class UserContext {

    private static final ThreadLocal<CurrentUser> THREAD_LOCAL = new ThreadLocal<>();

    public static void set(CurrentUser currentUser) {
        THREAD_LOCAL.set(currentUser);
    }

    public static CurrentUser get() {
        return THREAD_LOCAL.get();
    }

    public static void clear() {
        THREAD_LOCAL.remove();
    }
}
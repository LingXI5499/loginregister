package com.smartblog.util;

public class AuthConstants {

    private AuthConstants() {
    }

    public static final String IDENTITY_USERNAME = "USERNAME";
    public static final String IDENTITY_EMAIL = "EMAIL";

    public static final String CREDENTIAL_PASSWORD = "PASSWORD";

    public static final String SCENE_REGISTER = "REGISTER_EMAIL";
    public static final String SCENE_LOGIN = "LOGIN_EMAIL";
    public static final String SCENE_RESET_PASSWORD = "RESET_PASSWORD";
    public static final String SCENE_DELETE_ACCOUNT = "DELETE_ACCOUNT";
    public static final String SCENE_CANCEL_DELETE_ACCOUNT = "CANCEL_DELETE_ACCOUNT";

    public static final String EVENT_REGISTER = "REGISTER";
    public static final String EVENT_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String EVENT_LOGIN_FAIL = "LOGIN_FAIL";
    public static final String EVENT_LOGOUT = "LOGOUT";
    public static final String EVENT_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String EVENT_PASSWORD_RESET = "PASSWORD_RESET";
    public static final String EVENT_EMAIL_CODE_SEND = "EMAIL_CODE_SEND";
    public static final String EVENT_DELETE_REQUEST = "DELETE_REQUEST";
    public static final String EVENT_DELETE_CANCEL = "DELETE_CANCEL";
    public static final String EVENT_DELETE_FINALIZE = "DELETE_FINALIZE";

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAIL = "FAIL";

    public static final int USER_DISABLED = 0;
    public static final int USER_ACTIVE = 1;
    public static final int USER_PENDING_DELETION = 2;
    public static final int USER_DELETED = 3;

    public static final int SESSION_REVOKED = 0;
    public static final int SESSION_ACTIVE = 1;
    public static final int SESSION_EXPIRED = 2;

    public static final int DELETION_PENDING = 1;
    public static final int DELETION_CANCELLED = 2;
    public static final int DELETION_COMPLETED = 3;
}
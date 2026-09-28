package com.voting;

/** Holds the identity of the administrator who is currently logged in. */
public class AdminSession {

    private static final AdminSession INSTANCE = new AdminSession();

    private String adminId;

    public static AdminSession getInstance() {
        return INSTANCE;
    }

    public boolean isLoggedIn() {
        return adminId != null;
    }

    public String getAdminId() {
        if (!isLoggedIn())
            throw new IllegalStateException("No administrator is currently logged in.");
        return adminId;
    }

    void start(String adminId) {
        this.adminId = adminId;
    }

    public void clear() {
        this.adminId = null;
    }
}

package com.ruthless.less.applications;

/**
 * Row model for the text-only app drawer RecyclerView.
 */
public final class DrawerItem {

    public enum Type { SECTION, APP, EMPTY }

    public final Type type;
    public final String text;
    public final InstalledApp app;

    private DrawerItem(Type type, String text, InstalledApp app) {
        this.type = type;
        this.text = text;
        this.app = app;
    }

    public static DrawerItem section(String title) {
        return new DrawerItem(Type.SECTION, title, null);
    }

    public static DrawerItem app(InstalledApp app) {
        return new DrawerItem(Type.APP, app.getDisplayLabel(), app);
    }

    public static DrawerItem empty(String message) {
        return new DrawerItem(Type.EMPTY, message, null);
    }
}

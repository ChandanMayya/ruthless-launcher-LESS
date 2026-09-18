package com.ruthless.less.applications;

import java.util.Objects;

/**
 * Launchable installed application, text-only representation.
 * Icons are intentionally never loaded.
 */
public final class InstalledApp {

    private final String packageName;
    private final String label;
    private final String labelNormalized;

    public InstalledApp(String packageName, String label) {
        this.packageName = packageName;
        this.label = label == null ? packageName : label;
        this.labelNormalized = this.label.toUpperCase();
    }

    public String getPackageName() {
        return packageName;
    }

    public String getLabel() {
        return label;
    }

    /** Display label always uppercase for the LESS terminal look. */
    public String getDisplayLabel() {
        return labelNormalized;
    }

    public String getLabelNormalized() {
        return labelNormalized;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InstalledApp)) return false;
        InstalledApp that = (InstalledApp) o;
        return Objects.equals(packageName, that.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName);
    }
}

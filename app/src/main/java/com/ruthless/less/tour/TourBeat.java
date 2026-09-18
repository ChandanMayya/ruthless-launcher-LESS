package com.ruthless.less.tour;

import androidx.annotation.Nullable;

/**
 * One narrative beat for first-run tour or Field Manual chapter.
 * Content is data; activities decide how to execute {@link TourAction}.
 */
public final class TourBeat {

    public final String id;
    /** Section label, e.g. "A · THE FIRST TRAP" or "FIELD MANUAL". */
    @Nullable
    public final String section;
    public final String title;
    /** Future-self consequence / scenario. */
    public final String lead;
    /** What LESS changes — short paragraphs. */
    public final String[] body;
    /** Honest warning or trade-off. */
    @Nullable
    public final String warning;
    @Nullable
    public final String primaryLabel;
    @Nullable
    public final TourAction primaryAction;
    @Nullable
    public final String secondaryLabel;
    @Nullable
    public final TourAction secondaryAction;
    /** Show live Usage Access status under body. @deprecated prefer {@link #check} */
    public final boolean showUsageStatus;
    /** Optional live validation for first-run steps. */
    public final TourCheck check;
    /** Interactive boundary picker step (not pure static content). */
    public final boolean boundaryStep;
    /**
     * Reserved id for a future local silent monochrome motion demo.
     * Renderer may leave an empty accessibility-named slot when non-null.
     */
    @Nullable
    public final String demoSlotId;
    /** Field Manual deep-link target chapter id (for index rows). */
    @Nullable
    public final String chapterId;

    private TourBeat(Builder b) {
        this.id = b.id;
        this.section = b.section;
        this.title = b.title;
        this.lead = b.lead;
        this.body = b.body != null ? b.body : new String[0];
        this.warning = b.warning;
        this.primaryLabel = b.primaryLabel;
        this.primaryAction = b.primaryAction;
        this.secondaryLabel = b.secondaryLabel;
        this.secondaryAction = b.secondaryAction;
        this.showUsageStatus = b.showUsageStatus || b.check == TourCheck.USAGE_ACCESS;
        this.check = b.check != null ? b.check : (b.showUsageStatus ? TourCheck.USAGE_ACCESS : TourCheck.NONE);
        this.boundaryStep = b.boundaryStep;
        this.demoSlotId = b.demoSlotId;
        this.chapterId = b.chapterId;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String section;
        private String title = "";
        private String lead = "";
        private String[] body;
        private String warning;
        private String primaryLabel;
        private TourAction primaryAction;
        private String secondaryLabel;
        private TourAction secondaryAction;
        private boolean showUsageStatus;
        private TourCheck check = TourCheck.NONE;
        private boolean boundaryStep;
        private String demoSlotId;
        private String chapterId;

        private Builder(String id) {
            this.id = id;
        }

        public Builder section(String section) {
            this.section = section;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder lead(String lead) {
            this.lead = lead;
            return this;
        }

        public Builder body(String... body) {
            this.body = body;
            return this;
        }

        public Builder warning(String warning) {
            this.warning = warning;
            return this;
        }

        public Builder primary(String label, TourAction action) {
            this.primaryLabel = label;
            this.primaryAction = action;
            return this;
        }

        public Builder secondary(String label, TourAction action) {
            this.secondaryLabel = label;
            this.secondaryAction = action;
            return this;
        }

        public Builder showUsageStatus() {
            this.showUsageStatus = true;
            this.check = TourCheck.USAGE_ACCESS;
            return this;
        }

        public Builder check(TourCheck check) {
            this.check = check != null ? check : TourCheck.NONE;
            if (this.check == TourCheck.USAGE_ACCESS) {
                this.showUsageStatus = true;
            }
            return this;
        }

        public Builder boundaryStep() {
            this.boundaryStep = true;
            return this;
        }

        public Builder demoSlot(String demoSlotId) {
            this.demoSlotId = demoSlotId;
            return this;
        }

        public Builder chapterId(String chapterId) {
            this.chapterId = chapterId;
            return this;
        }

        public TourBeat build() {
            return new TourBeat(this);
        }
    }
}

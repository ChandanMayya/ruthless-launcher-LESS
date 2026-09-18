package com.ruthless.less;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class LessAppInstrumentedTest {

    @Test
    public void useAppContext() {
        assertEquals(
                "com.ruthless.less",
                InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName());
    }

    @Test
    public void applicationWiresRepositories() {
        LessApplication app = (LessApplication) InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext()
                .getApplicationContext();
        assertNotNull(app.getAppRepository());
        assertNotNull(app.getSettingsRepository());
        assertNotNull(app.getPolicyRepository());
        assertNotNull(app.getUsageStatsReader());
        assertNotNull(app.getConfuseManager());
        assertNotNull(app.getQuotaManager());
        assertNotNull(app.getBoredomMessageManager());
    }
}

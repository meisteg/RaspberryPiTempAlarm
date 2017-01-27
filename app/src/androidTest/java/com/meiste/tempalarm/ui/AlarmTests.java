/*
 * Copyright (C) 2017 Gregory S. Meiste  <http://gregmeiste.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meiste.tempalarm.ui;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.meiste.tempalarm.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class AlarmTests {

    @Rule
    public final ActivityTestRule<Alarm> mAlarmTestRule =
            new ActivityTestRule<>(Alarm.class);

    /*
     * Verify activity launches successfully and when dismissed returns to the
     * CurrentTemp activity.
     */
    @Test
    public void dismissButtonPress() {
        onView(withId(R.id.alert_button))
                .check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.temp_list)).check(matches(isDisplayed()));
    }
}

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.monsterbutt.homeview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

/**
 * A collection of utility methods, all static.
 */
public class Utils {

    /*
     * Making sure public utility methods remain static
     */
    private Utils() {
    }

    public static int convertDpToPixel(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }



    private final static long MsInSeconds = 1000;
    private final static long SecondsInMinutes = 60;
    private final static long MinutesInHours = 60;
    @SuppressLint("DefaultLocale")
    public static String timeMStoString(Context context, long timeMs) {

        String div = context.getString(R.string.time_divider);
        long secs = timeMs / MsInSeconds;
        long mins = secs / SecondsInMinutes;
        long hours = mins / MinutesInHours;
        return String.format("%02d%s%02d%s%02d", hours, div, mins % MinutesInHours, div, secs % SecondsInMinutes);
    }

    public static String convertDateToText(Context context, String date) {

        if (TextUtils.isEmpty(date) || context == null)
            return "";
        date = date.replace(" ", "");
        String[] tokens = date.split("-");
        if (tokens.length == 1)
            return tokens[0];
        String month = getMonth(context, Integer.valueOf(tokens[1]));
        if (tokens.length == 2)
            return String.format("%s %s", month, tokens[0]);
        return String.format("%s %s, %s", month, tokens[2], tokens[0]);
    }

    private static String getMonth(Context context, int month) {

        if (context == null)
            return "";

        switch(month) {

            case 1:
                return context.getString(R.string.january);
            case 2:
                return context.getString(R.string.february);
            case 3:
                return context.getString(R.string.march);
            case 4:
                return context.getString(R.string.april);
            case 5:
                return context.getString(R.string.may);
            case 6:
                return context.getString(R.string.june);
            case 7:
                return context.getString(R.string.july);
            case 8:
                return context.getString(R.string.august);
            case 9:
                return context.getString(R.string.september);
            case 10:
                return context.getString(R.string.october);
            case 11:
                return context.getString(R.string.november);
            case 12:
                return context.getString(R.string.december);
            default:
                return "";
        }
    }
}

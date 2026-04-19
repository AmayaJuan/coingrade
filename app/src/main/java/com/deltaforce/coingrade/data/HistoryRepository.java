package com.deltaforce.coingrade.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryRepository {

    private static final String PREFS = "coingrade_native";
    private static final String KEY = "items";
    private static final int MAX = 50;

    public static class Entry {
        public final String coinName;
        public final String gradeCode;
        public final int vp;
        public final long dateMs;

        public Entry(String coinName, String gradeCode, int vp, long dateMs) {
            this.coinName = coinName;
            this.gradeCode = gradeCode;
            this.vp = vp;
            this.dateMs = dateMs;
        }
    }

    public static void add(Context context, String coinName, String gradeCode, int vp) {
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray arr;
        try {
            arr = new JSONArray(sp.getString(KEY, "[]"));
        } catch (JSONException e) {
            arr = new JSONArray();
        }
        JSONObject o = new JSONObject();
        try {
            o.put("name", coinName);
            o.put("grade", gradeCode);
            o.put("vp", vp);
            o.put("t", System.currentTimeMillis());
        } catch (JSONException e) {
            return;
        }
        JSONArray next = new JSONArray();
        try {
            next.put(o);
            for (int i = 0; i < arr.length() && next.length() < MAX; i++)
                next.put(arr.getJSONObject(i));
        } catch (JSONException ignored) {
            return;
        }
        sp.edit().putString(KEY, next.toString()).apply();
    }

    public static List<Entry> getAll(Context context) {
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray arr;
        try {
            arr = new JSONArray(sp.getString(KEY, "[]"));
        } catch (JSONException e) {
            return Collections.emptyList();
        }
        List<Entry> list = new ArrayList<>();
        for (int i = 0;  i < arr.length(); i++) {
            try {
                JSONObject o = arr.getJSONObject(i);
                list.add(new Entry(
                        o.optString("name", ""),
                        o.optString("grade", "?"),
                        o.optInt("vp", 0),
                        o.optLong("t", 0)
                ));
            } catch (JSONException ignored){
            }
        }
        return list;
    }
}

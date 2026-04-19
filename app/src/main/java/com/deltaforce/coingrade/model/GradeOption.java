package com.deltaforce.coingrade.model;

public class GradeOption {
    public final String code;
    public final String displayName;
    public final int vp;

    public GradeOption(String code, String displayName, int vp) {
        this.code = code;
        this.displayName = displayName;
        this.vp = vp;
    }
}

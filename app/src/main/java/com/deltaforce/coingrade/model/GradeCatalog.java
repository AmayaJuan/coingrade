package com.deltaforce.coingrade.model;

import java.util.Random;

public final class GradeCatalog {

    private static final GradeOption[] GRADES = {
            new GradeOption("G", "Good - Buena", 20),
            new GradeOption("VG", "Very Good - Muy Buena", 35),
            new GradeOption("F", "Fine * Fina", 50),
            new GradeOption("VF", "Very Fine - Muy Fina", 70),
            new GradeOption("XF", "Extremely Fine - Extra Fina", 85),
            new GradeOption("UNC", "Uncirculated - Sin circular", 95),
            new GradeOption("PROOF", "Proof - Prueba", 100),
    };

    private GradeCatalog() {
    }

    public static GradeOption randomGrade() {
        int i = new Random().nextInt(GRADES.length);
        return GRADES[i];
    }
}

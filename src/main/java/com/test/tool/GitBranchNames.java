package com.test.tool;

import java.util.regex.Pattern;

// Regole sui nomi di branch: impediscono che un valore arrivato dal modello diventi un'opzione o un path inatteso.
final class GitBranchNames {

    private static final Pattern SAFE_BRANCH = Pattern.compile("[A-Za-z0-9._/-]+");

    private GitBranchNames() {
    }

    static boolean matchesSafePattern(String name) {
        return name != null && SAFE_BRANCH.matcher(name).matches();
    }

    // Il nome non può iniziare con '-' (diventerebbe un'opzione di git) né contenere '..'.
    static boolean isValid(String name) {
        return matchesSafePattern(name) && !name.startsWith("-") && !name.contains("..");
    }
}

package com.tallogre.hanbaobao.Utilities;

import android.content.Context;
import android.view.View;

import java.util.Collection;
import java.util.List;

/**
 * Created by reube on 4/23/2016.
 */
public class CollectionUtil {
    public static <T> int hashCode(Collection<T> collection) {
        int result = 0;
        int i = 1;
        if(collection!=null) for (T c : collection) {
            if (c == null) continue;
            result = 31*result*(i++) + c.hashCode();
        }

        return result;
    }
    public static <T> boolean equals(List<T> a, List<T> b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        for (int i = 0; i<a.size(); i++) {
            if (a.get(i) == b.get(i)) continue;
            if (a.get(i) == null || b.get(i) == null)return false;
            if (!a.get(i).equals(b.get(i))) return false;
        }

        return true;
    }

    private CollectionUtil(){}
}

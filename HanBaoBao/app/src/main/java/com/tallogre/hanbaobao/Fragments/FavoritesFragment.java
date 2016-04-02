package com.tallogre.hanbaobao.Fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tallogre.hanbaobao.R;

import butterknife.ButterKnife;

public class FavoritesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_favorites, container, false);
        ButterKnife.bind(this, result);

        return result;
    }
}

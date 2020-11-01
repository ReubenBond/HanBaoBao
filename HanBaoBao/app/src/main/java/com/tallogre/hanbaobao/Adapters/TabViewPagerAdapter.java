package com.tallogre.hanbaobao.Adapters;

import android.os.Parcelable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.tallogre.hanbaobao.Fragments.QuickDictionaryFragment;
import com.tallogre.hanbaobao.Fragments.SettingsFragment;

import java.util.ArrayList;
import java.util.List;

public class TabViewPagerAdapter extends FragmentStatePagerAdapter {
    private final List<Fragment> tabs = new ArrayList<>();

    public TabViewPagerAdapter(FragmentManager fm) {
        super(fm);
        tabs.add(new QuickDictionaryFragment());
        tabs.add(new SettingsFragment());
        //tabs.add(new HistoryFragment());
        //tabs.add(new FavoritesFragment());
    }

    @Override
    public Parcelable saveState() {
        return super.saveState();
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
        super.restoreState(state, loader);
    }

    @Override
    public Fragment getItem(int position) {
        return tabs.get(position);
    }

    @Override
    public int getCount() {
        return tabs.size();
    }
}

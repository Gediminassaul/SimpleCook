package com.demo.simplecook.ui.listener;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public abstract class EndlessRecyclerViewScrollListener extends RecyclerView.OnScrollListener {
    // Pirmiausia uzraunam minimalu kieki duomenu kiek turi buti lange pries uzkraunant daugiau duomenu
    private int mVisibleThreshold;

    RecyclerView.LayoutManager mLayoutManager;

    public EndlessRecyclerViewScrollListener(LinearLayoutManager layoutManager, int visibleThreshold) {
        this.mLayoutManager = layoutManager;
        this.mVisibleThreshold = visibleThreshold;
    }

    public EndlessRecyclerViewScrollListener(GridLayoutManager layoutManager, int visibleThreshold) {
        this.mLayoutManager = layoutManager;
        mVisibleThreshold = visibleThreshold * layoutManager.getSpanCount();
    }

    public EndlessRecyclerViewScrollListener(StaggeredGridLayoutManager layoutManager, int visibleThreshold) {
        this.mLayoutManager = layoutManager;
        mVisibleThreshold = visibleThreshold * layoutManager.getSpanCount();
    }

    public int getLastVisibleItem(int[] lastVisibleItemPositions) {
        int maxSize = 0;
        for (int i = 0; i < lastVisibleItemPositions.length; i++) {
            if (i == 0) {
                maxSize = lastVisibleItemPositions[i];
            }
            else if (lastVisibleItemPositions[i] > maxSize) {
                maxSize = lastVisibleItemPositions[i];
            }
        }
        return maxSize;
    }

    @Override
    public void onScrolled(RecyclerView view, int dx, int dy) {
        int lastVisibleItemPosition = 0;
        int totalItemCount = mLayoutManager.getItemCount();

        if (mLayoutManager instanceof StaggeredGridLayoutManager) {
            int[] lastVisibleItemPositions = ((StaggeredGridLayoutManager) mLayoutManager).findLastVisibleItemPositions(null);
            lastVisibleItemPosition = getLastVisibleItem(lastVisibleItemPositions);
        } else if (mLayoutManager instanceof GridLayoutManager) {
            lastVisibleItemPosition = ((GridLayoutManager) mLayoutManager).findLastVisibleItemPosition();
        } else if (mLayoutManager instanceof LinearLayoutManager) {
            lastVisibleItemPosition = ((LinearLayoutManager) mLayoutManager).findLastVisibleItemPosition();
        }

        // patikriname ar jau pasiekeme taska kada reikia uzkrauti daugiau duomenu
        // jei reikia uzkrauti daugiau duomenu paleidziame onRequestLoadMore kad requestintu daugiau duomenu.

        if (lastVisibleItemPosition + mVisibleThreshold > totalItemCount) {
            onRequestLoadMore();
        }
    }

    // nustatyti kiek reikes uzkrauti daugiau duomenu priklausomai nuo lango
    // patikrinimas ar vyksta uzkrovimas yra atliekamas ViewModel/Presenter klases.
    public abstract void onRequestLoadMore();

}
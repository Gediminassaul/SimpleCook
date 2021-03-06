package com.demo.simplecook.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.demo.simplecook.R;
import com.demo.simplecook.databinding.FragmentExploreBinding;
import com.demo.simplecook.ui.listener.EndlessRecyclerViewScrollListener;
import com.demo.simplecook.ui.listener.OnRecipeClickListener;
import com.demo.simplecook.viewmodel.ExploreViewModel;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import static com.demo.simplecook.ui.RecipeDetailsActivity.INTENT_KEY_RECIPE;

public class ExploreFragment extends Fragment {
    public static final String TAG = ExploreFragment.class.getName();
    public static final int RECYCLER_VIEW_LOAD_THRESHOLD = 5;

    private FragmentExploreBinding mBinding;
    private RecipeAdapter mRecipeAdapter;

    private ExploreViewModel mViewModel;

    private Snackbar mErrorSnackbar;

    public static ExploreFragment newInstance() {
        ExploreFragment fragment = new ExploreFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_explore, container, false);
        mBinding.setLifecycleOwner(this);
        mBinding.setRetryRefreshCallBack(mOnRetryLoadInitListener);

        mRecipeAdapter = new RecipeAdapter(mOnRecipeClickListener);
        mBinding.recyclerView.setAdapter(mRecipeAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        mBinding.recyclerView.setLayoutManager(linearLayoutManager);
        mBinding.recyclerView.setOnScrollListener(
                new EndlessRecyclerViewScrollListener(linearLayoutManager, RECYCLER_VIEW_LOAD_THRESHOLD) {
            @Override
            public void onRequestLoadMore() {
                loadNextPage();
            }
        });

        ArrayAdapter<CharSequence> foodChoiceAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.food_choice_array, R.layout.spinner_item_simple_cook);
        foodChoiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBinding.foodChoiceSpinner.setAdapter(foodChoiceAdapter);
        mBinding.foodChoiceSpinner.setSelection(0, false);
        mBinding.foodChoiceSpinner.setOnItemSelectedListener(mOnSpinnerSelectedListener);

        ArrayAdapter<CharSequence> prepTimeChoiceAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.prep_time_choice_array, R.layout.spinner_item_simple_cook);
        foodChoiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBinding.prepTimeChoiceSpinner.setAdapter(prepTimeChoiceAdapter);
        mBinding.prepTimeChoiceSpinner.setSelection(0, false);
        mBinding.prepTimeChoiceSpinner.setOnItemSelectedListener(mOnSpinnerSelectedListener);

        ArrayAdapter<CharSequence> dietChoiceAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.diet_choice_array, R.layout.spinner_item_simple_cook);
        foodChoiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBinding.dietChoiceSpinner.setAdapter(dietChoiceAdapter);
        mBinding.dietChoiceSpinner.setSelection(0, false);
        mBinding.dietChoiceSpinner.setOnItemSelectedListener(mOnSpinnerSelectedListener);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ExploreViewModel.Factory factory = new ExploreViewModel.Factory(
                getActivity().getApplication(),
                mBinding.foodChoiceSpinner.getSelectedItem().toString(),
                mBinding.prepTimeChoiceSpinner.getSelectedItem().toString(),
                mBinding.dietChoiceSpinner.getSelectedItem().toString()
        );
        mViewModel = ViewModelProviders.of(this, factory).get(ExploreViewModel.class);
        mBinding.setViewModel(mViewModel);
        subscribeUI();
    }

    private void subscribeUI() {
        mViewModel.getRemoteRecipes()
                .observe(this, recipes -> {
                    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                        this.mRecipeAdapter.setRecipes(recipes);
                    }
                });

        mViewModel.isGetRemoteRecipesErrorLoadingMore.
                observe(this, isErrorLoadingMore -> {
                    if (mErrorSnackbar != null && mErrorSnackbar.isShown()) {
                        mErrorSnackbar.dismiss();
                        mErrorSnackbar = null;
                    }
                    if (isErrorLoadingMore) {
                        mErrorSnackbar = Snackbar.make(mBinding.recyclerViewWrapper,
                                mViewModel.getRemoteRecipesErrorMsg.getValue(), Snackbar.LENGTH_INDEFINITE);
                        mErrorSnackbar.setAction(R.string.retry, mOnRetryLoadMoreListener);
                        mErrorSnackbar.show();
                    }
                });
    }

    private void loadInitialPage() {
        mViewModel.getInitialRemoteRecipes(mBinding.foodChoiceSpinner.getSelectedItem().toString(),
                mBinding.prepTimeChoiceSpinner.getSelectedItem().toString(),
                mBinding.dietChoiceSpinner.getSelectedItem().toString());
    }

    private void loadNextPage() {
        mViewModel.getMoreRemoteRecipes(mBinding.foodChoiceSpinner.getSelectedItem().toString(),
                mBinding.prepTimeChoiceSpinner.getSelectedItem().toString(),
                mBinding.dietChoiceSpinner.getSelectedItem().toString());
    }

    private AdapterView.OnItemSelectedListener mOnSpinnerSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                loadInitialPage();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) { }
    };

    private OnRecipeClickListener mOnRecipeClickListener = recipe -> {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            Intent intent = new Intent(getContext(), RecipeDetailsActivity.class);
            intent.putExtra(INTENT_KEY_RECIPE, recipe);
            startActivity(intent);
        }
    };

    private View.OnClickListener mOnRetryLoadInitListener = (view) -> {
        loadInitialPage();
    };

    private View.OnClickListener mOnRetryLoadMoreListener = (view) -> {
        mViewModel.isGetRemoteRecipesErrorLoadingMore.setValue(false);
        loadNextPage();
    };
}

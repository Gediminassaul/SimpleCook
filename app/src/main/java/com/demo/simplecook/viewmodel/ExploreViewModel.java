package com.demo.simplecook.viewmodel;

import android.app.Application;
import android.content.Context;

import com.demo.simplecook.R;
import com.demo.simplecook.SimpleCookApp;
import com.demo.simplecook.model.Recipe;
import com.demo.simplecook.repository.RecipeRepository;
import com.demo.simplecook.repository.RecipeResultWrapper;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class ExploreViewModel extends AndroidViewModel {
    private Context mAppContext;
    private RecipeRepository mRecipeRepository;

    public MutableLiveData<Boolean> isGetRemoteRecipesLoading = new MutableLiveData<>();
    public MutableLiveData<Boolean> isGetRemoteRecipesError = new MutableLiveData<>();
    public MutableLiveData<Boolean> isGetRemoteRecipesLoadingMore = new MutableLiveData<>();
    public MutableLiveData<Boolean> isGetRemoteRecipesErrorLoadingMore = new MutableLiveData<>();
    public MutableLiveData<String> getRemoteRecipesErrorMsg = new MutableLiveData<>();

    private MediatorLiveData<List<Recipe>> mRemoteRecipesLiveData = new MediatorLiveData<>();
    private LiveData<RecipeResultWrapper> mRemoteRecipesSource;

    private int nextPageStartIndex = 0;

    public ExploreViewModel(@NonNull Application application,
                            @NonNull RecipeRepository recipeRepository,
                            @NonNull String query,
                            @NonNull String time,
                            @NonNull String diet) {
        super(application);
        mAppContext = application.getApplicationContext();
        mRecipeRepository = recipeRepository;

        getInitialRemoteRecipes(query, time, diet);
    }

    public void getInitialRemoteRecipes(String query, String time, String diet) {
        nextPageStartIndex = 0;
        isGetRemoteRecipesLoading.setValue(true);
        isGetRemoteRecipesError.setValue(false);
        isGetRemoteRecipesLoadingMore.setValue(false);
        isGetRemoteRecipesErrorLoadingMore.setValue(false);

        // Istrinamos senos nenaudojimos reiksmes
        if (mRemoteRecipesSource != null) {
            mRemoteRecipesLiveData.removeSource(mRemoteRecipesSource);
        }

        // nustatomas saltinis is repositorijos
        mRemoteRecipesSource = mRecipeRepository.getRemoteRecipes(query, time, diet, nextPageStartIndex);
        // klausyti saltinio ir atnaujinti live data
        mRemoteRecipesLiveData.addSource(mRemoteRecipesSource, (recipeResultWrapper) -> {
            if (recipeResultWrapper.isSucess() && recipeResultWrapper.getRecipes().size() > 0) {
                isGetRemoteRecipesLoading.setValue(false);
                mRemoteRecipesLiveData.setValue(recipeResultWrapper.getRecipes());
                nextPageStartIndex = recipeResultWrapper.getLastIndex();
            } else if (recipeResultWrapper.isSucess() && recipeResultWrapper.getRecipes().size() == 0) {
                getRemoteRecipesErrorMsg.setValue(mAppContext.getString(R.string.load_recipe_no_result));
                isGetRemoteRecipesLoading.setValue(false);
                isGetRemoteRecipesError.setValue(true);
            } else {
                if (recipeResultWrapper.getCode() == 401) { // API Limit exceeded
                    getRemoteRecipesErrorMsg.setValue(mAppContext.getString(R.string.load_recipe_error_exceed_limit));
                } else {
                    getRemoteRecipesErrorMsg.setValue(mAppContext.getString(R.string.load_recipe_error_general));
                }
                isGetRemoteRecipesLoading.setValue(false);
                isGetRemoteRecipesError.setValue(true);
            }
        });
    }

    public void getMoreRemoteRecipes(String query, String time, String diet) {
        // Jei loadina neleisti pereit i kita langa
        if (isGetRemoteRecipesLoading.getValue() != null && isGetRemoteRecipesLoading.getValue() ||
                isGetRemoteRecipesLoadingMore.getValue() != null && isGetRemoteRecipesLoadingMore.getValue() ||
                isGetRemoteRecipesError.getValue() != null && isGetRemoteRecipesError.getValue() ||
                isGetRemoteRecipesErrorLoadingMore.getValue() != null && isGetRemoteRecipesErrorLoadingMore.getValue()) {
            return;
        }

        isGetRemoteRecipesLoadingMore.setValue(true);
        isGetRemoteRecipesErrorLoadingMore.setValue(false);

        // Istrinamos senos nenaudojimos reiksmes
        if (mRemoteRecipesSource != null) {
            mRemoteRecipesLiveData.removeSource(mRemoteRecipesSource);
        }

        // nustatomas saltinis is repositorijos
        mRemoteRecipesSource = mRecipeRepository.getRemoteRecipes(query, time, diet, nextPageStartIndex);
        // klausyti saltinio ir atnaujinti live data
        mRemoteRecipesLiveData.addSource(mRemoteRecipesSource, (recipeResultWrapper) -> {
            if (recipeResultWrapper.isSucess() && recipeResultWrapper.getRecipes().size() > 0) {
                isGetRemoteRecipesLoadingMore.setValue(false);
                if (mRemoteRecipesLiveData.getValue() != null) {
                    recipeResultWrapper.getRecipes().addAll(0, mRemoteRecipesLiveData.getValue());
                }
                mRemoteRecipesLiveData.setValue(recipeResultWrapper.getRecipes());

                nextPageStartIndex = recipeResultWrapper.getLastIndex();
            } else if (recipeResultWrapper.isSucess() && recipeResultWrapper.getRecipes().size() == 0) {
                getRemoteRecipesErrorMsg.setValue(mAppContext.getString(R.string.load_recipe_no_result));
                isGetRemoteRecipesLoadingMore.setValue(false);
                isGetRemoteRecipesErrorLoadingMore.setValue(true);
            } else {
                if (recipeResultWrapper.getCode() == 401) { // API Limit exceeded
                    getRemoteRecipesErrorMsg.setValue(mAppContext.getString(R.string.load_recipe_error_exceed_limit));
                } else {
                    getRemoteRecipesErrorMsg.setValue(mAppContext.getString(R.string.load_recipe_error_general));
                }
                isGetRemoteRecipesLoadingMore.setValue(false);
                isGetRemoteRecipesErrorLoadingMore.setValue(true);
            }
        });
    }

    public LiveData<List<Recipe>> getRemoteRecipes() {
        return mRemoteRecipesLiveData;
    }

    public static class Factory extends ViewModelProvider.NewInstanceFactory {
        @NonNull
        private final Application mApplication;

        private final String mQuery;
        private final String mTime;
        private final String mDiet;

        private final RecipeRepository mRepository;

        public Factory(@NonNull Application application,
                       String query, String time, String diet) {
            mApplication = application;
            mQuery = query;
            mTime = time;
            mDiet = diet;
            mRepository = ((SimpleCookApp) application).getRecipeRepository();
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T) new ExploreViewModel(mApplication, mRepository, mQuery, mTime, mDiet);
        }
    }

}
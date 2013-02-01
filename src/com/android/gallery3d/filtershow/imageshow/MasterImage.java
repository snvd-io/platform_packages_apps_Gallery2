/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.filtershow.imageshow;

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.android.gallery3d.app.Log;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.HistoryAdapter;
import com.android.gallery3d.filtershow.ImageStateAdapter;
import com.android.gallery3d.filtershow.cache.TripleBufferBitmap;
import com.android.gallery3d.filtershow.cache.FilteringPipeline;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.filters.ImageFilter;
import com.android.gallery3d.filtershow.presets.ImagePreset;

import java.util.Vector;

public class MasterImage {

    private static final String LOGTAG = "MasterImage";

    private static MasterImage sMasterImage = new MasterImage();

    private ImageFilter mCurrentFilter = null;
    private ImagePreset mPreset = null;
    private ImagePreset mGeometryOnlyPreset = null;
    private ImagePreset mFiltersOnlyPreset = null;

    private TripleBufferBitmap mFilteredPreview = new TripleBufferBitmap();
    private TripleBufferBitmap mGeometryOnlyPreview = new TripleBufferBitmap();
    private TripleBufferBitmap mFiltersOnlyPreview = new TripleBufferBitmap();

    private ImageLoader mLoader = null;
    private HistoryAdapter mHistory = null;
    private ImageStateAdapter mState = null;

    private FilterShowActivity mActivity = null;

    private Vector<ImageShow> mObservers = new Vector<ImageShow>();

    private MasterImage() { }

    public static MasterImage getImage() {
        return sMasterImage;
    }

    public void addObserver(ImageShow observer) {
        mObservers.add(observer);
    }

    public void setActivity(FilterShowActivity activity) {
        mActivity = activity;
    }

    public synchronized ImagePreset getPreset() {
        return mPreset;
    }

    public synchronized ImagePreset getGeometryPreset() {
        return mGeometryOnlyPreset;
    }

    public synchronized ImagePreset getFiltersOnlyPreset() {
        return mFiltersOnlyPreset;
    }

    public synchronized void setPreset(ImagePreset preset, boolean addToHistory) {
        mPreset = preset;
        mPreset.setImageLoader(mLoader);
        setGeometry();
        mPreset.fillImageStateAdapter(mState);
        if (addToHistory) {
            mHistory.addHistoryItem(mPreset);
        }
        updatePresets(true);
    }

    private void setGeometry() {
        Bitmap image = mLoader.getOriginalBitmapLarge();
        if (image == null) {
            return;
        }
        float w = image.getWidth();
        float h = image.getHeight();
        GeometryMetadata geo = mPreset.mGeoData;
        RectF pb = geo.getPhotoBounds();
        if (w == pb.width() && h == pb.height()) {
            return;
        }
        RectF r = new RectF(0, 0, w, h);
        geo.setPhotoBounds(r);
        geo.setCropBounds(r);
    }

    public void onHistoryItemClick(int position) {
        setPreset(new ImagePreset(mHistory.getItem(position)), false);
        // We need a copy from the history
        mHistory.setCurrentPreset(position);
    }
    public HistoryAdapter getHistory() {
        return mHistory;
    }

    public ImageStateAdapter getState() {
        return mState;
    }

    public void setHistoryAdapter(HistoryAdapter adapter) {
        mHistory = adapter;
    }

    public void setStateAdapter(ImageStateAdapter adapter) {
        mState = adapter;
    }

    public void setImageLoader(ImageLoader loader) {
        mLoader = loader;
    }

    public void setCurrentFilter(ImageFilter filter) {
        mCurrentFilter = filter;
    }

    public ImageFilter getCurrentFilter() {
        return mCurrentFilter;
    }

    public synchronized boolean hasModifications() {
        if (mPreset == null) {
            return false;
        }
        return mPreset.hasModifications();
    }

    public TripleBufferBitmap getDoubleBuffer() {
        return mFilteredPreview;
    }

    public TripleBufferBitmap getGeometryOnlyBuffer() {
        return mGeometryOnlyPreview;
    }

    public TripleBufferBitmap getFiltersOnlyBuffer() {
        return mFiltersOnlyPreview;
    }

    public Bitmap getFilteredImage() {
        return mFilteredPreview.getConsumer();
    }

    public Bitmap getFiltersOnlyImage() {
        return mFiltersOnlyPreview.getConsumer();
    }

    public Bitmap getGeometryOnlyImage() {
        return mGeometryOnlyPreview.getConsumer();
    }

    public void notifyObservers() {
        for (ImageShow observer : mObservers) {
            observer.invalidate();
        }
    }

    public void updatePresets(boolean force) {
        if (force || mGeometryOnlyPreset == null) {
            ImagePreset newPreset = new ImagePreset(mPreset);
            newPreset.setDoApplyFilters(false);
            if (mGeometryOnlyPreset == null
                    || !newPreset.same(mGeometryOnlyPreset)) {
                mGeometryOnlyPreset = newPreset;
            }
        }
        if (force || mFiltersOnlyPreset == null) {
            ImagePreset newPreset = new ImagePreset(mPreset);
            newPreset.setDoApplyGeometry(false);
            if (mFiltersOnlyPreset == null
                    || !newPreset.same(mFiltersOnlyPreset)) {
                mFiltersOnlyPreset = newPreset;
            }
        }
        mActivity.enableSave(hasModifications());
        updateBuffers();
    }

    public void updateBuffers() {
        FilteringPipeline.getPipeline().updatePreviewBuffer();
        FilteringPipeline.getPipeline().updateGeometryOnlyPreviewBuffer();
        FilteringPipeline.getPipeline().updateFiltersOnlyPreviewBuffer();
    }

}
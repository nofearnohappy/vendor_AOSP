/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.quicksearchbox.ui;

import com.android.quicksearchbox.Corpus;
import com.android.quicksearchbox.CorpusSelectionDialog;
import com.android.quicksearchbox.Promoter;
import com.android.quicksearchbox.QsbApplication;
import com.android.quicksearchbox.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

/**
 * Finishes the containing activity on BACK, even if input method is showing.
 */
public class SearchActivityViewSinglePane extends SearchActivityView {

    private CorpusSelectionDialog mCorpusSelectionDialog;

    private ImageButton mCorpusIndicator;

    public SearchActivityViewSinglePane(Context context) {
        super(context);
    }

    public SearchActivityViewSinglePane(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchActivityViewSinglePane(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        /// M:SMB
        if (QsbApplication.getTabletUsedInOneSessionFlag()) {
            Log.d(TAG + ".SMB", "onFinishInflate(), update corpora because from SMB ...");

            getQsbApplication().updateCorpora();
            QsbApplication.setTabletUsedInOneSessionFlag(this);

            Log.d(TAG + ".SMB", "onFinishInflate(), set in phone");
        } else {
            Log.d(TAG + ".SMB", "onFinishInflate(), normal");
        }

        /// M:
        mCorpusIndicator = (ImageButton) mActionBarView.findViewById(R.id.corpus_indicator);
        mCorpusIndicator.setOnKeyListener(mButtonsKeyListener);
        mCorpusIndicator.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showCorpusSelectionDialog();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isCorpusSelectionDialogShowing()) {
            focusQueryTextView();
        }
    }

    @Override
    public void onStop() {
        dismissCorpusSelectionDialog();
    }

    @Override
    protected void setCorpus(Corpus corpus) {
        super.setCorpus(corpus);

        if (mCorpusIndicator != null) {
            Drawable sourceIcon;
            if (corpus == null) {
                sourceIcon = getContext().getResources().getDrawable(R.drawable.search_app_icon_mirrored);
            } else {
                sourceIcon = corpus.getCorpusIcon();
            }
            mCorpusIndicator.setImageDrawable(sourceIcon);
        }
    }

    @Override
    protected Promoter createSuggestionsPromoter() {
        Corpus corpus = getCorpus();
        if (corpus == null) {
            return getQsbApplication().createBlendingPromoter();
        } else {
            return getQsbApplication().createSingleCorpusPromoter(corpus);
        }
    }

    /**
     * Gets the corpus to use for any searches. This is the web corpus in "All" mode,
     * and the selected corpus otherwise.
     */
    @Override
    public Corpus getSearchCorpus() {
        Corpus corpus = getCorpus();
        return corpus == null ? getWebCorpus() : corpus;
    }

    @Override
    public void showCorpusSelectionDialog() {
        if (mCorpusSelectionDialog == null) {
            mCorpusSelectionDialog = getActivity().getCorpusSelectionDialog();
            mCorpusSelectionDialog.setOnCorpusSelectedListener(new CorpusSelectionListener());
        }
        mCorpusSelectionDialog.show(getCorpus());
    }

    protected boolean isCorpusSelectionDialogShowing() {
        return mCorpusSelectionDialog != null && mCorpusSelectionDialog.isShowing();
    }

    protected void dismissCorpusSelectionDialog() {
        if (mCorpusSelectionDialog != null) {
            mCorpusSelectionDialog.dismiss();
        }
    }

    @Override
    public void considerHidingInputMethod() {
        mQueryTextView.hideInputMethod();
    }

    private class CorpusSelectionListener
            implements CorpusSelectionDialog.OnCorpusSelectedListener {
        public void onCorpusSelected(String corpusName) {
            SearchActivityViewSinglePane.this.onCorpusSelected(corpusName);
        }
    }

}

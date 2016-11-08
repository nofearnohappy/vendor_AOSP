/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.ftprecheck;

import java.util.ArrayList;
import java.util.List;

import com.mediatek.ftprecheck.CheckItemBase.CheckResult;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class TabFragment extends Fragment {

    private boolean mIsExecTab;
    private ExpandableListView mExpandListView;
    private ExpandableListAdapter mExpandListAdapter;
    private ConditionManager mConditionManager;

    private List<String> mGroupList;
    private List<List<ChildViewItem>> mChildsList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        mIsExecTab = getArguments().getBoolean("tab_type");
        loadExpandListData();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // TODO Auto-generated method stub

        View fragmentView = inflater.inflate(R.layout.tab_fragment, container, false);
        mExpandListView = (ExpandableListView) fragmentView.findViewById(R.id.expand_list);
        mExpandListAdapter = new ExpandableListAdapter(getActivity(), mIsExecTab);
        mExpandListView.setAdapter(mExpandListAdapter);

        return fragmentView;
    }

    private void loadExpandListData() {
        mConditionManager = ConditionManager.getConditionManager(getActivity());
        mGroupList = new ArrayList<String>();
        mChildsList = new ArrayList<List<ChildViewItem>>();

        List<TestCase> resultCaseList = null;
        if (mIsExecTab) {
            resultCaseList = mConditionManager.getResultExecCases();
        } else {
            resultCaseList = mConditionManager.getResultUnexecCases();
        }

        for (TestCase testCase : resultCaseList) {
            mGroupList.add(testCase.getId() + " " + testCase.getName());

            List<ChildViewItem> childs = new ArrayList<ChildViewItem>();
            for (Condition condition : testCase.getConditionList()) {
                ChildViewItem childViewItem = null;
                if (condition.needCheck()) {
                    childViewItem = new ChildViewItem(condition.getDescription(),
                            condition.getCheckItem().getValue(),
                            condition.getCheckItem().getCheckResult());
                } else {
                    childViewItem = new ChildViewItem(condition.getDescription(),
                            this.getString(R.string.value_needless_check),
                            CheckResult.UNKNOWN);
                }
                childs.add(childViewItem);
            }
            mChildsList.add(childs);
        }
    }

    class ChildViewItem {
        String mCondDesc;
        String mCondValue;
        CheckResult mResult;

        public ChildViewItem(String desc, String value, CheckResult result) {
            mCondDesc = desc;
            mCondValue = value;
            mResult = result;
        }
    }

    class ExpandableListAdapter extends BaseExpandableListAdapter {

        private LayoutInflater mInflater;
        private boolean mIsExec;
        private int mChildDefaultColor;

        class ChildViewHolder {
            private TextView mCondDesc;
            private TextView mCondValue;
        }

        private ExpandableListAdapter(Context c, boolean isExec) {
            mInflater = LayoutInflater.from(c);
            mIsExec = isExec;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            // TODO Auto-generated method stub
            return mChildsList.get(groupPosition).get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition,
                boolean isLastChild, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            ChildViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.expandlist_child_item, null);

                holder = new ChildViewHolder();
                holder.mCondDesc = (TextView) convertView.findViewById(R.id.condition_desc_text);
                holder.mCondValue = (TextView) convertView.findViewById(R.id.condition_value_text);
                mChildDefaultColor = holder.mCondDesc.getCurrentTextColor();
                convertView.setTag(holder); //to associate(store) the holder as a tag in convertView
            } else {
                holder = (ChildViewHolder) convertView.getTag();
            }

            holder.mCondDesc.setText(String.format(getString(R.string.expandlist_cond_desc),
                    mChildsList.get(groupPosition).get(childPosition).mCondDesc));
            holder.mCondValue.setText(mChildsList.get(groupPosition).get(childPosition).mCondValue);

            if (!mIsExec && (mChildsList.get(groupPosition).get(childPosition).mResult)
                    .equals(CheckResult.UNSATISFIED)) {
                holder.mCondDesc.setTextColor(getResources().getColor(R.color.red));
                holder.mCondValue.setTextColor(getResources().getColor(R.color.red));
            } else {
                holder.mCondDesc.setTextColor(mChildDefaultColor);
                holder.mCondValue.setTextColor(mChildDefaultColor);
            }

            return convertView;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            // TODO Auto-generated method stub
            return mChildsList.get(groupPosition).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            // TODO Auto-generated method stub
            return mGroupList.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            // TODO Auto-generated method stub
            return mGroupList.size();
        }

        @Override
        public long getGroupId(int groupPosition) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            TextView caseIdView;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.expandlist_group_item, null);
                caseIdView = (TextView) convertView.findViewById(R.id.case_id_text);
                convertView.setTag(caseIdView); //to associate(store) the holder as a tag in convertView
            } else {
                caseIdView = (TextView) convertView.getTag();
            }

            caseIdView.setText(mGroupList.get(groupPosition));

            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            // TODO Auto-generated method stub
            return false;
        }

    }

}

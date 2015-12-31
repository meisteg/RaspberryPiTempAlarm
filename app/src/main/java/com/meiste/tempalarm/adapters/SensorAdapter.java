/*
 * Copyright (C) 2015 Gregory S. Meiste  <http://gregmeiste.com>
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
package com.meiste.tempalarm.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.jjoe64.graphview.CustomLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;
import com.meiste.tempalarm.AppConstants;
import com.meiste.tempalarm.R;
import com.meiste.tempalarm.items.SensorData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import timber.log.Timber;

public class SensorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int NUM_RECORDS = 120;

    private final LineGraphView mGraph;
    private final Context mContext;
    private final List<SensorData> mListData = new ArrayList<>();
    private final List<GraphView.GraphViewData> mGraphData = new ArrayList<>();
    private final Query mFirebaseQuery;

    private boolean mFirebaseSyncing;
    private boolean mFirebaseAllowChild;

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public HeaderViewHolder(final View v, final LineGraphView lgv) {
            super(v);

            final FrameLayout frameLayout = ButterKnife.findById(v, R.id.graph_placeholder);
            frameLayout.addView(lgv);
        }
    }

    public static class RecordViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.timestamp)
        public TextView timestamp;

        @Bind(R.id.degF)
        public TextView degF;

        @Bind(R.id.humidity)
        public TextView humidity;

        public RecordViewHolder(final View v) {
            super(v);
            ButterKnife.bind(this, v);
        }
    }

    public SensorAdapter(final Context context) {
        mContext = context.getApplicationContext();

        // getColor(id) was replaced in API level 23 with getColor(id, theme)
        //noinspection deprecation
        final int bgColor = context.getResources().getColor(R.color.primary_graph);

        mGraph = new LineGraphView(context, "");
        mGraph.setDrawBackground(true);
        mGraph.setBackgroundColor(bgColor);
        mGraph.setCustomLabelFormatter(new CustomLabelFormatter() {
            @Override
            public String formatLabel(final double value, final boolean isValueX) {
                if (isValueX) {
                    return DateUtils.formatDateTime(mContext,
                            (long) value, AppConstants.DATE_FORMAT_FLAGS_GRAPH);
                }
                return String.format(Locale.getDefault(), "%.1f", value);
            }
        });
        mGraph.getGraphViewStyle().setNumHorizontalLabels(AppConstants.GRAPH_NUM_HORIZONTAL_LABELS);

        final Firebase firebase = new Firebase(AppConstants.FIREBASE_URL_SENSOR);
        mFirebaseQuery = firebase.orderByChild("timestamp").limitToLast(NUM_RECORDS);
        mFirebaseSyncing = false;
    }

    public void startSync() {
        if (!mFirebaseSyncing) {
            Timber.d("Starting Firebase sync");

            mFirebaseSyncing = true;
            mFirebaseAllowChild = false;
            mFirebaseQuery.addChildEventListener(mChildEventListener);
            mFirebaseQuery.addListenerForSingleValueEvent(mValueEventListener);
        }
    }

    public void stopSync() {
        if (mFirebaseSyncing) {
            Timber.d("Stopping Firebase sync");

            mFirebaseSyncing = false;
            mFirebaseQuery.removeEventListener(mChildEventListener);

            /*
             * If requested to stop before the value event has returned, it
             * needs to be removed as well.
             */
            if (!mFirebaseAllowChild) {
                mFirebaseQuery.removeEventListener(mValueEventListener);
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View v = LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false);
        if (viewType == R.layout.record_header) {
            return new HeaderViewHolder(v, mGraph);
        } else if (viewType == R.layout.record) {
            return new RecordViewHolder(v);
        } else {
            throw new IllegalArgumentException("Unrecognized view type");
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof RecordViewHolder) {
            final RecordViewHolder rvh = (RecordViewHolder) holder;
            final SensorData data = mListData.get(position - 1);

            rvh.timestamp.setText(data.getTime(mContext));
            rvh.degF.setText(data.getDegF());
            rvh.humidity.setText(data.getHumidity());
        }
    }

    @Override
    public int getItemCount() {
        /* Only show the graph if data is present to display on it */
        return (mListData.size() > 0) ? (mListData.size() + 1) : 0;
    }

    @Override
    public int getItemViewType(final int position) {
        if (isPositionHeader(position))
            return R.layout.record_header;

        return R.layout.record;
    }

    private boolean isPositionHeader(final int position) {
        return position == 0;
    }

    private void addSensorData(final SensorData sensorData) {
        mListData.add(0, sensorData);
        mGraphData.add(new GraphView.GraphViewData(sensorData.timestamp, sensorData.degF));
    }

    private void updateGraph() {
        final GraphViewSeries temperatureSeries = new GraphViewSeries(
                mGraphData.toArray(new GraphView.GraphViewData[mGraphData.size()]));
        mGraph.removeAllSeries();
        mGraph.addSeries(temperatureSeries);
    }

    private final ChildEventListener mChildEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(final DataSnapshot snapshot, final String prevChild) {
            if (mFirebaseAllowChild) {
                final SensorData sensorData = snapshot.getValue(SensorData.class);
                Timber.v("onChildAdded:   %s", sensorData.toString());

                addSensorData(sensorData);
                notifyItemInserted(1);
                updateGraph();
            }
        }

        @Override
        public void onChildChanged(final DataSnapshot snapshot, final String prevChild) {
            /* Should never happen */
        }

        @Override
        public void onChildRemoved(final DataSnapshot snapshot) {
            if (mFirebaseAllowChild) {
                final SensorData sensorData = snapshot.getValue(SensorData.class);
                Timber.v("onChildRemoved: %s", sensorData.toString());

                final int index = mListData.indexOf(sensorData);
                if (index >= 0) {
                    /*
                     * mListData and mGraphData are always modified at the same
                     * time and therefore are the same size. mListData stores
                     * data newest first while mGraphData is oldest first. Once
                     * we know the index into one list, we invert the index to
                     * get the same value in the other list.
                     */
                    mListData.remove(index);
                    mGraphData.remove(mGraphData.size() - index - 1);

                    notifyItemRemoved(index + 1);
                    updateGraph();
                }
            }
        }

        @Override
        public void onChildMoved(final DataSnapshot snapshot, final String prevChild) {
            /* Should never happen */
        }

        @Override
        public void onCancelled(final FirebaseError error) {
            Timber.e("Firebase ChildEventListener onCancelled");
        }
    };

    /*
     * From the Firebase documentation:
     * "Value events are always triggered last and are guaranteed to contain
     * updates from any other events which occurred before that snapshot was
     * taken."
     *
     * This allows us to perform one big update to get initial state, then
     * use the child updates to modify from there.
     */
    private final ValueEventListener mValueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(final DataSnapshot snapshot) {
            Timber.v("onDataChange: %d records", snapshot.getChildrenCount());

            mFirebaseAllowChild = true;
            mListData.clear();
            mGraphData.clear();

            for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                addSensorData(postSnapshot.getValue(SensorData.class));
            }

            notifyDataSetChanged();
            updateGraph();
        }

        @Override
        public void onCancelled(final FirebaseError error) {
            Timber.e("Firebase ValueEventListener onCancelled");
        }
    };
}

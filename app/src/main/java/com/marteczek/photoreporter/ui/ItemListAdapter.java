package com.marteczek.photoreporter.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TimingLogger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.database.entity.Item;
import com.marteczek.photoreporter.database.entity.Report;
import com.marteczek.photoreporter.ui.misc.ItemTouchHelperAdapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.marteczek.photoreporter.application.Settings.Debug.D;

public class ItemListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
    implements ItemTouchHelperAdapter {

    private static final String TAG = "ItemListAdapter";

    private static final int REPORT = 0;

    private static final int ITEM = 1;

    private Report report;

    private List<Item> items;

    private String newReportName = null;

    private String threadName = null;

    @SuppressLint("UseSparseArrays")
    private final Map<Long, String> itemChanges = new HashMap<>();

    private boolean reordered = false;

    private final Set<Long> removedItems = new HashSet<>();

    private final LayoutInflater inflater;

    private final OnClickListener onClickListener;

    private final OnSelectThreadListener onSelectThreadListener;

    @SuppressLint("UseSparseArrays")
    private final Map<Long, RecyclerView.ViewHolder> thumbnailRequests = new HashMap<>();

    private class ReportViewHolder extends RecyclerView.ViewHolder {
        private final EditText reportNameEditText;
        private final TextView threadNameTextView;
        private ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            reportNameEditText = itemView.findViewById(R.id.report_name);
            threadNameTextView = itemView.findViewById(R.id.thread_name);
            reportNameEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                @Override
                public void afterTextChanged(Editable editable) {
                    newReportName = editable.toString();
                }
            });
            Button button = itemView.findViewById(R.id.select_thread);
            button.setOnClickListener((view) -> {
                if (onSelectThreadListener != null) {
                    onSelectThreadListener.onSelectThread();
                }
            });
        }
    }

    private class ItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView headerView;
        private final ImageView imageView;
        private BackgroundImageLoader imageLoader;
        private ItemHeaderTextWatcher headerTextWatcher;

        private ItemViewHolder(View itemView) {
            super(itemView);
            headerView = itemView.findViewById(R.id.header);
            imageView = itemView.findViewById(R.id.image);
            headerTextWatcher = new ItemHeaderTextWatcher();
            headerView.addTextChangedListener(headerTextWatcher);
        }
    }

    public interface OnClickListener {
        void onClick(Item item);
    }

    public interface OnSelectThreadListener {
        void onSelectThread();
    }

    private static class BackgroundImageLoader extends AsyncTask<String, Void, Bitmap> {

        @SuppressLint("StaticFieldLeak")
        private ImageView imageView;
        private int greaterDimension;

        int rotation;

        BackgroundImageLoader(ImageView imageView, int rotation, int greaterDimension) {
            this.imageView = imageView;
            this.rotation = rotation;
            this.greaterDimension = greaterDimension;
        }

        @Override
        protected Bitmap doInBackground(String... paths) {
            TimingLogger timings = new TimingLogger(TAG, "decodeFile");
            Bitmap bitmap = BitmapFactory.decodeFile(paths[0]);
            timings.dumpToLog();
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if(bitmap != null) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                int width =  bitmap.getWidth();
                int height = bitmap.getHeight();
                float scale = (float) greaterDimension / Math.max(width, height);
                matrix.postScale(scale, scale);
                imageView.setImageBitmap(Bitmap.createBitmap(bitmap, 0, 0, width, height,
                        matrix, true));
            }
        }
    }

    private class ItemHeaderTextWatcher implements TextWatcher {

        private Item item;

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        @Override
        public void afterTextChanged(Editable editable) {
            if(item != null) {
                itemChanges.put(item.getId(), editable.toString());
            }
        }

        void setItem(Item item) {
            this.item = item;
        }
    }

    ItemListAdapter(Context context, OnClickListener onClickListener, OnSelectThreadListener onSelectThreadListener) {
        this.onClickListener = onClickListener;
        this.onSelectThreadListener = onSelectThreadListener;
        inflater = LayoutInflater.from(context);
    }

    void setReport(Report report) {
        this.report = report;
        notifyDataSetChanged();
    }

    void setItems(List<Item>  items){
        if (isEssentialDifference(items)) {
            this.items = items;
            notifyDataSetChanged();
        } else {
            this.items = items;
        }
        for(Item item : items) {
            Long id = item.getId();
            if (thumbnailRequests.containsKey(id)) {
                String thumbnailPath = item.getThumbnailPath();
                if (thumbnailPath != null) {
                    ItemViewHolder itemViewHolder = (ItemViewHolder) thumbnailRequests.get(id);
                    itemViewHolder.imageLoader = new BackgroundImageLoader(itemViewHolder.imageView,
                            item.getPictureRotation(), item.getThumbnailRequiredWidth());
                    itemViewHolder.imageLoader.execute(item.getThumbnailPath());
                    thumbnailRequests.remove(id);
                }
            }
        }
    }

    private boolean isEssentialDifference(List<Item> items) {
        List<Item> oldItems = this.items;
        if (oldItems == null || items == null){
            return true;
        }
        if (oldItems.size() != items.size()) {
            return true;
        }
        int size = items.size();
        for(int i = 0; i < size; i++) {
            String itemHeader = items.get(i).getHeader();
            String oldItemHeader = oldItems.get(i).getHeader();
            if (itemHeader != null) {
                if (!itemHeader.equals(oldItemHeader)) {
                    return true;
                }
            } else {
                if (oldItemHeader != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Item> getItems() {
        return items;
    }

    String getNewReportName() {
        return newReportName;
    }

    void setNewReportName(String name) {
        newReportName = name;
    }

    void setThreadName(String threadName) {
        this.threadName = threadName;
        notifyDataSetChanged();
    }

    Map<Long, String> getItemChanges() {
        return itemChanges;
    }

    Set<Long> getRemovedItems() {return removedItems;}

    boolean isReordered() {
        return reordered;
    }

    void setReordered(boolean reordered) {
        this.reordered = reordered;
    }

    @Override
    public @NonNull RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == REPORT) {
            view = inflater.inflate(R.layout.recyclerview_report_header_item, parent, false);
            return new ReportViewHolder(view);
        } else {
            view = inflater.inflate(R.layout.recyclerview_picture_item, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        if (position == 0 ) {
            if(report != null) {
                ReportViewHolder reportViewHolder = (ReportViewHolder) viewHolder;
                reportViewHolder.reportNameEditText.setText(report.getName());
                reportViewHolder.threadNameTextView.setText(threadName);
            }
        } else {
            if (items != null) {
                ItemViewHolder itemViewHolder = (ItemViewHolder) viewHolder;
                Item item = items.get(position - 1);
                Long id = item.getId();
                String header = (itemChanges.containsKey(id)) ? itemChanges.get(id) : item.getHeader();
                itemViewHolder.headerTextWatcher.setItem(null);
                itemViewHolder.headerView.setText(header);
                itemViewHolder.headerTextWatcher.setItem(item);
                itemViewHolder.itemView.setOnClickListener((view) -> onClickListener.onClick(item));
                itemViewHolder.imageView.setImageBitmap(null);
                if (itemViewHolder.imageLoader != null) {
                    itemViewHolder.imageLoader.cancel(true);
                }
                String imagePath = item.getThumbnailPath();
                if (imagePath != null) {
                    thumbnailRequests.remove(id);
                    itemViewHolder.imageLoader = new BackgroundImageLoader(itemViewHolder.imageView,
                            item.getPictureRotation(), item.getThumbnailRequiredWidth());
                    itemViewHolder.imageLoader.execute(item.getThumbnailPath());
                } else {
                    thumbnailRequests.put(id, viewHolder);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return (report != null && items != null) ? items.size() + 1 : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? REPORT : ITEM;
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        reordered = true;
        Item item = items.get(fromPosition - 1);
        items.remove(fromPosition - 1);
        items.add(toPosition - 1, item);
        notifyItemMoved(fromPosition, toPosition);
        if (D) {
            StringBuilder sb = new StringBuilder("Move: from ");
            sb.append(fromPosition);
            sb.append(" to ");
            sb.append(toPosition);
            sb.append(" -> ");
            for (Item i:  items) {
                sb.append(i.getId());
                sb.append(" ");
            }
            Log.d(TAG, sb.toString());
        }
    }

    @Override
    public void onItemDismiss(int position) {
        Long itemId = items.get(position - 1).getId();
        removedItems.add(itemId);
        items.remove(position - 1);
        itemChanges.remove(itemId);
        notifyItemRemoved(position);
    }
}

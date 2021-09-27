package com.akigon.mydiary;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.akigon.mydiary.db.Diary;
import com.bumptech.glide.Glide;
import com.parse.ParseObject;

import java.util.List;

import static com.akigon.mydiary.BFunctions.DIARY_CATEGORIES;
import static com.akigon.mydiary.BFunctions.DIARY_CONTENT;
import static com.akigon.mydiary.BFunctions.DIARY_CREATED_AT;
import static com.akigon.mydiary.BFunctions.DIARY_TITLE;
import static com.akigon.mydiary.BFunctions.DIARY_UPDATED_AT;
import static com.akigon.mydiary.BFunctions.getStringFromList;
import static com.akigon.mydiary.BFunctions.getTimeStringFromDate;
import static com.akigon.mydiary.BFunctions.getTimeStringTS;
import static com.akigon.mydiary.BFunctions.minToRead;

import org.xml.sax.XMLReader;


public class DiaryRecyclerAdapter extends RecyclerView.Adapter<DiaryRecyclerAdapter.NoticeViewHolder> {

    private Activity activity;
    private List<Diary> list;
    private OnItemClickListener listener;

    public DiaryRecyclerAdapter(Activity activity, List<Diary> list) {
        this.activity = activity;
        this.list = list;
    }

    @NonNull
    @Override
    public NoticeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_journal, parent, false);
        return new NoticeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticeViewHolder holder, int position) {
        Diary d = list.get(position);
        holder.title.setText(d.getTitle());
        String content = d.getContent();
        //preview
        String url = d.getContent();
        int index = url.indexOf("<img width=");
        if (index != -1) {
            String substr = url.substring(index);
            int startindex = substr.indexOf("src=\"");
            int endIndex = substr.indexOf("alt=\"Image\"");
            String imgUrl = substr.substring(startindex + 5, endIndex - 2);
            holder.imageView.setVisibility(View.VISIBLE);
            Glide.with(activity).load(imgUrl).centerCrop()
                    .placeholder(R.mipmap.placeholder).into(holder.imageView);
        } else {
            //no image found
            holder.imageView.setVisibility(View.GONE);
            int index2 = url.indexOf("<iframe width=");
            if (index2 != -1) {
                holder.richEditor.setVisibility(View.VISIBLE);
                String substr = url.substring(index2);
                int startindex = substr.indexOf("src=\"");
                int endIndex = substr.lastIndexOf("</iframe>");
                holder.richEditor.loadData(substr.substring(startindex + 5, endIndex + 9), "text/html; charset=utf-8", "UTF-8");
            } else {
                //yt link not found
                int index3 = url.indexOf("<video width=");
                if (index3 != -1) {
                    String substr = url.substring(index3);
                    int startindex = substr.indexOf("src=\"");
                    int endIndex = substr.lastIndexOf("</video>");
                    holder.richEditor.setVisibility(View.VISIBLE);
                    holder.richEditor.loadData(substr.substring(startindex + 5, endIndex + 8), "text/html; charset=utf-8", "UTF-8");
                } else {
                    //nothing exist
                    holder.richEditor.setVisibility(View.GONE);
                }
            }
        }

        holder.content.setText(Html.fromHtml(String.valueOf(Html.fromHtml(content))));
        holder.created_on.setText(getTimeStringTS(d.getCreatedAt()) + "\t • \t" + minToRead(content) + " min read");
        holder.modified_on.setText("Updated: " + getTimeStringTS(d.getUpdatedAt()));
        holder.tags.setText(d.getCategories());
    }

    private static final String TAG = "DiaryRecyclerAdapter";

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class NoticeViewHolder extends RecyclerView.ViewHolder {
        AppCompatTextView title, content, created_on, modified_on, tags;
        ImageView imageView;
        RichEditor richEditor;

        public NoticeViewHolder(@NonNull View itemView) {
            super(itemView);
            created_on = itemView.findViewById(R.id.journal_row_created_on);
            title = itemView.findViewById(R.id.journal_row_title);
            content = itemView.findViewById(R.id.journal_row_content);
            tags = itemView.findViewById(R.id.journal_row_tags);
            modified_on = itemView.findViewById(R.id.journal_row_modified_on);
            imageView = itemView.findViewById(R.id.imgPreview);
            richEditor = itemView.findViewById(R.id.vidPreview);

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = getAdapterPosition();
                    if (listener != null && position != RecyclerView.NO_POSITION) {
                        listener.OnItemLongClick(list.get(position), position);
                    }
                    return true;
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (listener != null && position != RecyclerView.NO_POSITION) {
                        listener.OnItemClick(list.get(position));
                    }
                }
            });

        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnItemClickListener {
        void OnItemLongClick(Diary journal, int position);

        void OnItemClick(Diary journal);

    }


}

package com.jkxy.notebook.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jkxy.notebook.R;
import com.jkxy.notebook.activity.NoteDetailActivity;
import com.jkxy.notebook.util.TextFormatUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Think on 2016/12/8.
 */

public class AllNoteListAdapter extends RecyclerView.Adapter<AllNoteListAdapter.ViewHolder> {

    private Cursor mCursor;
    private Context mContext;
    private int position;
    private final static int CONTEXT_UPDATE_ORDER = 0;
    private final static int CONTEXT_DELETE_ORDER = 1;

    public AllNoteListAdapter(Cursor cursor, Context context) {
        this.mCursor = cursor;
        this.mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);

        viewHolder.tvNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = viewHolder.getAdapterPosition();
                if (mCursor.moveToPosition(position)) {
                    int itemID = mCursor.getInt(mCursor.getColumnIndex("_id"));
                    Intent intent = new Intent(mContext, NoteDetailActivity.class);
                    intent.putExtra(NoteDetailActivity.SENDED_NOTE_ID, itemID);
                    mContext.startActivity(intent);
                }
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        if (mCursor.moveToPosition(position)) {
            holder.tvTitle.setText(mCursor.getString(mCursor.getColumnIndex("title")));
            holder.tvContent.setText(TextFormatUtil.getNoteSummary(mCursor.getString(mCursor.getColumnIndex("content"))));
            holder.tvUpdateTime.setText(mCursor.getString(mCursor.getColumnIndex("update_time")));
        }

        holder.tvNote.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setPosition(holder.getAdapterPosition());
                return false;
            }
        });

    }


    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.tvNote.setOnLongClickListener(null);
        super.onViewRecycled(holder);

    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void setCursor(Cursor cursor) {
        this.mCursor = cursor;
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

        @BindView(R.id.id_tv_note_summary)
        TextView tvContent;
        @BindView(R.id.id_tv_note_title)
        TextView tvTitle;
        @BindView(R.id.id_tv_note_update_time)
        TextView tvUpdateTime;

        View tvNote;

        public ViewHolder(View itemView) {
            super(itemView);
            tvNote = itemView;
            ButterKnife.bind(this, itemView);
            itemView.setOnCreateContextMenuListener(this);

        }


        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.setHeaderTitle("Enter your choice:");
            menu.add(0, v.getId(), CONTEXT_UPDATE_ORDER, "Update");
            menu.add(0, v.getId(), CONTEXT_DELETE_ORDER, "Delete");
        }
    }
}

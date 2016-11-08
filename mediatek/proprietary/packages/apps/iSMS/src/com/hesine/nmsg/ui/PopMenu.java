package com.hesine.nmsg.ui;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.hesine.nmsg.R;

public class PopMenu {
    private int[] itemListTitle = { R.string.menu_share, R.string.menu_view_contact,
            R.string.menu_add_contact };
    private int[] itemListImages = { R.drawable.popmenu_share, R.drawable.popmenu_view_contact,
            R.drawable.popmenu_add_contact };
    private Context context;
    private PopupWindow popupWindow;
    private ListView listView;
    int width;

    @SuppressWarnings("deprecation")
    public PopMenu(Context context) {
        this.context = context;
        View view = LayoutInflater.from(context).inflate(R.layout.popmenu, null);
        listView = (ListView) view.findViewById(R.id.popmenu_list);
        listView.setAdapter(new PopAdapter());
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        width = wm.getDefaultDisplay().getWidth();
        popupWindow = new PopupWindow(view, width / 2, LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        listView.setOnItemClickListener(listener);
    }

    public void showAsDropDown(View parent) {
        popupWindow.showAsDropDown(parent, -20, 20);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.update();
    }

    public void dismiss() {
        if (popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    public boolean isShowing() {
        return popupWindow.isShowing();
    }

    private final class PopAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return itemListTitle.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.pomenu_item, null);
                holder = new ViewHolder();
                holder.groupItemTitle = (TextView) convertView.findViewById(R.id.popmenu_title);
                holder.groupItemImage = (ImageView) convertView.findViewById(R.id.popmenu_image);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.groupItemTitle.setText(itemListTitle[position]);
            holder.groupItemImage.setImageResource(itemListImages[position]);
            return convertView;
        }

        private final class ViewHolder {
            TextView groupItemTitle;
            ImageView groupItemImage;
        }
    }
}

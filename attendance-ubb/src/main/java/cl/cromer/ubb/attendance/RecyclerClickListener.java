package cl.cromer.ubb.attendance;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class RecyclerClickListener implements RecyclerView.OnItemTouchListener {
    private OnClickListener onClickListener;

    public interface OnClickListener {
        void onClick(View view, int position);
        void onLongClick(View view, int position);
    }

    private GestureDetector gestureDetector;

    public RecyclerClickListener(Context context, final RecyclerView recyclerView, final OnClickListener listener) {
        onClickListener = listener;
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapUp(MotionEvent motionEvent) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent motionEvent) {
                View childView = recyclerView.findChildViewUnder(motionEvent.getX(), motionEvent.getY());

                if(childView != null && listener != null) {
                    listener.onLongClick(childView, recyclerView.getChildAdapterPosition(childView));
                }
            }
        });
    }

    @Override public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
        View childView = view.findChildViewUnder(e.getX(), e.getY());
        if (childView != null && onClickListener != null && gestureDetector.onTouchEvent(e)) {
            onClickListener.onClick(childView, view.getChildAdapterPosition(childView));
            return true;
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) {}

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
}
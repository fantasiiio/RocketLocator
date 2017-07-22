package com.frankdev.rocketlocator;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public  class TouchableWrapper extends FrameLayout {

	private OnMapMoveListener onMapMoveListener;

	public TouchableWrapper(Context context) {
		super(context);
		// Force the host activity to implement the UpdateMapAfterUserInterection Interface
		try {
			onMapMoveListener = (OnMapMoveListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnMapMoveListener");
        }
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		//int historySize = ev.getHistorySize();		
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			//if(historySize > 0){
				onMapMoveListener.onMapMove();
			//}
			break;
		}
		return super.dispatchTouchEvent(ev);
	}

	// Map Activity must implement this interface
    public interface OnMapMoveListener {
        public void onMapMove();
    }
}
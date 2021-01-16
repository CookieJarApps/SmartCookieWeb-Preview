package com.cookiejarapps.android.smartcookieweb.popup

import android.app.AppComponentFactory
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.cookiejarapps.android.smartcookieweb.BrowserActivity
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.showDialog

class PopupMenu {
    private var list: ListView? = null

    //PopupWindow display method
    fun showPopupWindow(view: View, activity: BrowserActivity) {
        //Create a View object yourself through inflater
        val inflater = view.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.toolbar_menu, null)
        val r = view.context.resources

        val px = Math.round(
            TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 228f, r.displayMetrics))

        //Specify the length and width through constants
        val height = LinearLayout.LayoutParams.WRAP_CONTENT

        //Make Inactive Items Outside Of PopupWindow
        val focusable = true

        //Create a window with our parameters
        val popupWindow = PopupWindow(popupView, px, height, focusable)
        val relView = popupView.findViewById<RelativeLayout>(R.id.toolbar_menu)


        popupView.findViewById<ImageButton>(R.id.back_option).setOnClickListener {
            activity.back()
        }
        popupView.findViewById<ImageButton>(R.id.shield).setOnClickListener {
            activity.showAdDialog()
        }

        var container =  popupView.findViewById<ConstraintLayout>(R.id.transparent_container)

        popupWindow.animationStyle = R.style.ToolbarAnim
        popupWindow.showAtLocation(view, Gravity.TOP or Gravity.END, 0, 0)
        relView.gravity = Gravity.TOP

        popupWindow.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        val resources = view.context.resources
        var textString = arrayOf(resources.getString(R.string.new_tab))
        var drawableIds = intArrayOf(R.drawable.ic_round_add)

        val adapter = CustomAdapter(view.context, textString, drawableIds)
        list = popupView.findViewById(R.id.menuList)
        list?.setAdapter(adapter)
        list?.setOnItemClickListener { parent, view, position, id ->
            var positionList = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

            if (positionList[position] == 0) {
                //uiController!!.newTabButtonClicked()
            }
            popupWindow.dismiss()
        }
    }
}
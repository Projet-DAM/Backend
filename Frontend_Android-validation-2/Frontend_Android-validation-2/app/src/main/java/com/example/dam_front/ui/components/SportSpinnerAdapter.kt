package com.example.dam_front.ui.components

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.dam_front.models.SportType

class SportSpinnerAdapter(
    context: Context,
    private val sports: List<SportType>
) : ArrayAdapter<SportType>(context, android.R.layout.simple_spinner_item, sports) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        view.text = SportType.getDisplayName(sports[position].value)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent) as TextView
        view.text = SportType.getDisplayName(sports[position].value)
        return view
    }
}



package com.example.farmforward.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import com.example.farmforward.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalcFragment : Fragment() {

    private lateinit var calendar: Calendar
    private lateinit var tvMonthYear: TextView
    private lateinit var calendarGrid: GridLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calc, container, false)

        calendar = Calendar.getInstance()
        tvMonthYear = view.findViewById(R.id.tvMonthYear)
        calendarGrid = view.findViewById(R.id.calendarGrid)

        val btnPrev = view.findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNext = view.findViewById<ImageButton>(R.id.btnNextMonth)

        updateCalendar()

        btnPrev.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }

        btnNext.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        return view
    }

    private fun updateCalendar() {
        calendarGrid.removeAllViews()

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonthYear.text = monthFormat.format(calendar.time)

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (day in 1..daysInMonth) {
            val btnDay = Button(requireContext())
            btnDay.text = day.toString()
            btnDay.textSize = 14f
            btnDay.setBackgroundResource(R.drawable.day_button_selector)
            btnDay.layoutParams = GridLayout.LayoutParams().apply {
                width = 110
                height = 110
                setMargins(8, 8, 8, 8)
            }

            btnDay.setOnClickListener {
                // Deselect all buttons
                for (i in 0 until calendarGrid.childCount) {
                    calendarGrid.getChildAt(i).isSelected = false
                }
                // Select the clicked day
                btnDay.isSelected = true
            }

            calendarGrid.addView(btnDay)
        }
    }
}
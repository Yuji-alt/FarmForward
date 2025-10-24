package com.example.farmforward.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.farmforward.CropViewModel
import com.example.farmforward.R
import com.example.farmforward.fragmentController.CalcController
import java.util.Calendar
import kotlin.jvm.java

class CalcFragment : Fragment() {

    private lateinit var controller: CalcController
    private lateinit var cropViewModel: CropViewModel
    private lateinit var inputCrop: AutoCompleteTextView
    private lateinit var inputArea: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calc, container, false)
        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]
        controller = CalcController(requireContext(), cropViewModel)

        inputCrop = view.findViewById(R.id.inputCrop)
        inputArea = view.findViewById(R.id.inputArea)
        val btnCalculate = view.findViewById<Button>(R.id.btnCalculate)
        val tvMonthYear = view.findViewById<TextView>(R.id.tvMonthYear)
        val calendarGrid = view.findViewById<GridLayout>(R.id.calendarGrid)
        val btnPrev = view.findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNext = view.findViewById<ImageButton>(R.id.btnNextMonth)

        val cropList = controller.loadCropNames()
        val adapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, cropList)
        inputCrop.setAdapter(adapter)

        val calendar = Calendar.getInstance()
        controller.updateCalendar(calendar, calendarGrid, tvMonthYear, requireContext())

        btnPrev.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            controller.updateCalendar(calendar, calendarGrid, tvMonthYear, requireContext())
        }

        btnNext.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            controller.updateCalendar(calendar, calendarGrid, tvMonthYear, requireContext())
        }

        btnCalculate.setOnClickListener {
            val cropName = inputCrop.text.toString()
            val area = inputArea.text.toString().toDoubleOrNull() ?: 0.0
            val userId = getCurrentUserId()

            if (cropName.isEmpty() || area <= 0.0) {
                Toast.makeText(requireContext(), "Please enter valid inputs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userId != null) {
                controller.saveCropData(userId, cropName, area)
            } else {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun getCurrentUserId(): Int? {
        val session = com.example.farmforward.session.SessionManager(requireContext())
        return session.getUserId()
    }
}

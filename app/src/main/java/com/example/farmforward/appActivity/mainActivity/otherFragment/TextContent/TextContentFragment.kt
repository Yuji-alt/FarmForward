package com.example.farmforward.appActivity.mainActivity.otherFragment

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity

class TextContentFragment : Fragment() {

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_CONTENT = "arg_content"
        private const val ARG_BACK_ID = "arg_back_id" // 1. Add this constant

        fun newInstance(title: String, content: String, backDestination: Int): TextContentFragment {
            val fragment = TextContentFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_CONTENT, content)
            args.putInt(ARG_BACK_ID, backDestination) // Save it
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_text_content, container, false)
        val title = arguments?.getString(ARG_TITLE) ?: ""
        val content = arguments?.getString(ARG_CONTENT) ?: ""
        val backId = arguments?.getInt(ARG_BACK_ID) ?: R.id.nav_home

        val tvTitle = view.findViewById<TextView>(R.id.tv_page_title)
        val tvBody = view.findViewById<TextView>(R.id.tv_content_body)
        val btnBack = view.findViewById<ImageButton>(R.id.btn_back_text)

        tvTitle.text = title
        tvBody.text = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY)
        btnBack.setOnClickListener {
            (activity as? MainActivity)?.controller?.onNavigationItemClicked(backId)
        }

        return view
    }
}
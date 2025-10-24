import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import com.example.farmforward.R
import com.example.farmforward.activityViewmodel.LoginActivity
import com.example.farmforward.session.SessionManager

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val logoutButton = view.findViewById<Button>(R.id.btnLogout)

        logoutButton.setOnClickListener {
            val session = SessionManager(requireContext())
            session.clearSession()

            // Create intent to go back to LoginActivity
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            // Finish the current Activity hosting the fragment
            requireActivity().finish()
        }
    }
}

package yiwoo.prototype.gabobell.ui

import android.content.Intent
import android.os.Bundle
import yiwoo.prototype.gabobell.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.btnEmergencyReport.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            startActivity(intent)
        }

        binding.btnCheckReportDetails.setOnClickListener {
            val intent = Intent(this, ReportDetailActivity::class.java)
            startActivity(intent)
        }

        binding.btnDeviceRegistration.setOnClickListener {
            val intent = Intent(this, RegisterDeviceActivity::class.java)
            startActivity(intent)
        }

        binding.btnDeviceSettings.setOnClickListener {
            val intent = Intent(this, DeviceSettingsActivity::class.java)
            startActivity(intent)
        }
    }
}
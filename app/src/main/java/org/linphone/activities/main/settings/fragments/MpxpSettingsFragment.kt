/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.activities.main.settings.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import org.linphone.R
import org.linphone.activities.main.settings.viewmodels.MpxpSettingsViewModel
import org.linphone.databinding.SettingsMpxpFragmentBinding

class MpxpSettingsFragment : GenericSettingFragment<SettingsMpxpFragmentBinding>() {
    private lateinit var viewModel: MpxpSettingsViewModel

    override fun getLayoutId(): Int = R.layout.settings_mpxp_fragment
    var mpxpMainActivity: MpxpMainActivity? = null
    private val mContext: Context? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.sharedMainViewModel = sharedViewModel
//        val bundle = arguments
//        val mLog = bundle!!.getString("mLog")
        viewModel = ViewModelProvider(this)[MpxpSettingsViewModel::class.java]
//        Log.d("CREATION", "mpxpexcuted$mLog")
//        binding.pktsLog.text = mLog
        binding.viewModel = viewModel
    }
}

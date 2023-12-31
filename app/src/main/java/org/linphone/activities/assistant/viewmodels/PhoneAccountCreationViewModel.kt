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

package org.linphone.activities.assistant.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.linphone.LinphoneApplication.Companion.corePreferences
import org.linphone.R
import org.linphone.core.AccountCreator
import org.linphone.core.AccountCreatorListenerStub
import org.linphone.core.tools.Log
import org.linphone.utils.AppUtils
import org.linphone.utils.Event

class PhoneAccountCreationViewModelFactory(private val accountCreator: AccountCreator) :
    ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PhoneAccountCreationViewModel(accountCreator) as T
    }
}

class PhoneAccountCreationViewModel(accountCreator: AccountCreator) : AbstractPhoneViewModel(
    accountCreator
) {
    val username = MutableLiveData<String>()
    val useUsername = MutableLiveData<Boolean>()
    val usernameError = MutableLiveData<String>()

    val displayName = MutableLiveData<String>()

    val createEnabled: MediatorLiveData<Boolean> = MediatorLiveData()

    val waitForServerAnswer = MutableLiveData<Boolean>()

    val goToSmsValidationEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val onErrorEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }

    private val listener = object : AccountCreatorListenerStub() {
        override fun onIsAccountExist(
            creator: AccountCreator,
            status: AccountCreator.Status,
            response: String?
        ) {
            Log.i("[Assistant] [Phone Account Creation] onIsAccountExist status is $status")
            when (status) {
                AccountCreator.Status.AccountExist, AccountCreator.Status.AccountExistWithAlias -> {
                    waitForServerAnswer.value = false
                    usernameError.value = AppUtils.getString(
                        R.string.assistant_error_username_already_exists
                    )
                }
                AccountCreator.Status.AccountNotExist -> {
                    waitForServerAnswer.value = false
                    checkPhoneNumber()
                }
                else -> {
                    waitForServerAnswer.value = false
                    onErrorEvent.value = Event("Error: ${status.name}")
                }
            }
        }

        override fun onIsAliasUsed(
            creator: AccountCreator,
            status: AccountCreator.Status,
            response: String?
        ) {
            Log.i("[Assistant] [Phone Account Creation] onIsAliasUsed status is $status")
            when (status) {
                AccountCreator.Status.AliasExist -> {
                    waitForServerAnswer.value = false
                    phoneNumberError.value = AppUtils.getString(
                        R.string.assistant_error_phone_number_already_exists
                    )
                }
                AccountCreator.Status.AliasIsAccount -> {
                    waitForServerAnswer.value = false
                    if (useUsername.value == true) {
                        usernameError.value = AppUtils.getString(
                            R.string.assistant_error_username_already_exists
                        )
                    } else {
                        phoneNumberError.value = AppUtils.getString(
                            R.string.assistant_error_phone_number_already_exists
                        )
                    }
                }
                AccountCreator.Status.AliasNotExist -> {
                    val createAccountStatus = creator.createAccount()
                    Log.i(
                        "[Assistant] [Phone Account Creation] createAccount returned $createAccountStatus"
                    )
                    if (createAccountStatus != AccountCreator.Status.RequestOk) {
                        waitForServerAnswer.value = false
                        onErrorEvent.value = Event("Error: ${status.name}")
                    }
                }
                else -> {
                    waitForServerAnswer.value = false
                    onErrorEvent.value = Event("Error: ${status.name}")
                }
            }
        }

        override fun onCreateAccount(
            creator: AccountCreator,
            status: AccountCreator.Status,
            response: String?
        ) {
            Log.i("[Assistant] [Phone Account Creation] onCreateAccount status is $status")
            waitForServerAnswer.value = false
            when (status) {
                AccountCreator.Status.AccountCreated -> {
                    goToSmsValidationEvent.value = Event(true)
                }
                AccountCreator.Status.AccountExistWithAlias -> {
                    phoneNumberError.value = AppUtils.getString(
                        R.string.assistant_error_phone_number_already_exists
                    )
                }
                else -> {
                    onErrorEvent.value = Event("Error: ${status.name}")
                }
            }
        }
    }

    init {
        useUsername.value = false
        accountCreator.addListener(listener)

        createEnabled.value = false
        createEnabled.addSource(prefix) {
            createEnabled.value = isCreateButtonEnabled()
        }
        createEnabled.addSource(phoneNumber) {
            createEnabled.value = isCreateButtonEnabled()
        }
        createEnabled.addSource(useUsername) {
            createEnabled.value = isCreateButtonEnabled()
        }
        createEnabled.addSource(username) {
            createEnabled.value = isCreateButtonEnabled()
        }
        createEnabled.addSource(usernameError) {
            createEnabled.value = isCreateButtonEnabled()
        }
        createEnabled.addSource(phoneNumberError) {
            createEnabled.value = isCreateButtonEnabled()
        }
        createEnabled.addSource(prefixError) {
            createEnabled.value = isCreateButtonEnabled()
        }
    }

    override fun onCleared() {
        accountCreator.removeListener(listener)
        super.onCleared()
    }

    override fun onFlexiApiTokenReceived() {
        Log.i(
            "[Assistant] [Phone Account Creation] Using FlexiAPI auth token [${accountCreator.token}]"
        )
        accountCreator.displayName = displayName.value

        val result = AccountCreator.PhoneNumberStatus.fromInt(
            accountCreator.setPhoneNumber(phoneNumber.value, prefix.value)
        )
        if (result != AccountCreator.PhoneNumberStatus.Ok) {
            Log.e(
                "[Assistant] [Phone Account Creation] Error [$result] setting the phone number: ${phoneNumber.value} with prefix: ${prefix.value}"
            )
            phoneNumberError.value = result.name
            return
        }
        Log.i("[Assistant] [Phone Account Creation] Phone number is ${accountCreator.phoneNumber}")

        if (useUsername.value == true) {
            accountCreator.username = username.value
        } else {
            accountCreator.username = accountCreator.phoneNumber
        }

        if (useUsername.value == true) {
            checkUsername()
        } else {
            checkPhoneNumber()
        }
    }

    override fun onFlexiApiTokenRequestError() {
        Log.e("[Assistant] [Phone Account Creation] Failed to get an auth token from FlexiAPI")
        waitForServerAnswer.value = false
        onErrorEvent.value = Event("Error: Failed to get an auth token from account manager server")
    }

    private fun checkUsername() {
        val status = accountCreator.isAccountExist
        Log.i("[Assistant] [Phone Account Creation] isAccountExist returned $status")
        if (status != AccountCreator.Status.RequestOk) {
            waitForServerAnswer.value = false
            onErrorEvent.value = Event("Error: ${status.name}")
        }
    }

    private fun checkPhoneNumber() {
        val status = accountCreator.isAliasUsed
        Log.i("[Assistant] [Phone Account Creation] isAliasUsed returned $status")
        if (status != AccountCreator.Status.RequestOk) {
            waitForServerAnswer.value = false
            onErrorEvent.value = Event("Error: ${status.name}")
        }
    }

    fun create() {
        val token = accountCreator.token.orEmpty()
        if (token.isNotEmpty()) {
            Log.i(
                "[Assistant] [Phone Account Creation] We already have an auth token from FlexiAPI [$token], continue"
            )
            onFlexiApiTokenReceived()
        } else {
            Log.i("[Assistant] [Phone Account Creation] Requesting an auth token from FlexiAPI")
            waitForServerAnswer.value = true
            requestFlexiApiToken()
        }
    }

    private fun isCreateButtonEnabled(): Boolean {
        val usernameRegexp = corePreferences.config.getString(
            "assistant",
            "username_regex",
            "^[a-z0-9+_.\\-]*\$"
        )
        return isPhoneNumberOk() && usernameRegexp != null &&
            (
                useUsername.value == false ||
                    username.value.orEmpty().matches(Regex(usernameRegexp)) &&
                    username.value.orEmpty().isNotEmpty() &&
                    usernameError.value.orEmpty().isEmpty()
                )
    }
}

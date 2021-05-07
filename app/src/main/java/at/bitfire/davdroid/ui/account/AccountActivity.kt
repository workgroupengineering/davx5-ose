package at.bitfire.davdroid.ui.account

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.*
import at.bitfire.davdroid.DavUtils
import at.bitfire.davdroid.R
import at.bitfire.davdroid.log.Logger
import at.bitfire.davdroid.model.AppDatabase
import at.bitfire.davdroid.model.Collection
import at.bitfire.davdroid.model.Service
import at.bitfire.davdroid.settings.AccountSettings
import at.bitfire.davdroid.ui.PermissionsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_account.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import java.util.logging.Level

class AccountActivity: AppCompatActivity() {

    companion object {
        const val EXTRA_ACCOUNT = "account"
    }

    val model by viewModels<Model> {
        val account = intent.getParcelableExtra(EXTRA_ACCOUNT) as? Account
                ?: throw IllegalArgumentException("AccountActivity requires EXTRA_ACCOUNT")
        Model.Factory(application, account)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = model.account.name
        setContentView(R.layout.activity_account)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        model.accountExists.observe(this, Observer { accountExists ->
            if (!accountExists)
                finish()
        })

        tab_layout.setupWithViewPager(view_pager)
        val tabsAdapter = TabsAdapter(this)
        view_pager.adapter = tabsAdapter
        model.cardDavService.observe(this, Observer {
            tabsAdapter.cardDavSvcId = it
        })
        model.calDavService.observe(this, Observer {
            tabsAdapter.calDavSvcId = it
        })

        sync.setOnClickListener {
            DavUtils.requestSync(this, model.account)
            Snackbar.make(view_pager, R.string.account_synchronizing_now, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_account, menu)
        return true
    }


    // menu actions

    fun openAccountSettings(menuItem: MenuItem) {
        val intent = Intent(this, SettingsActivity::class.java)
        intent.putExtra(SettingsActivity.EXTRA_ACCOUNT, model.account)
        startActivity(intent, null)
    }

    fun renameAccount(menuItem: MenuItem) {
        RenameAccountFragment.newInstance(model.account).show(supportFragmentManager, null)
    }

    fun deleteAccount(menuItem: MenuItem) {
        MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_error)
                .setTitle(R.string.account_delete_confirmation_title)
                .setMessage(R.string.account_delete_confirmation_text)
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    deleteAccount()
                }
                .show()
    }

    private fun deleteAccount() {
        val accountManager = AccountManager.get(this)

        if (Build.VERSION.SDK_INT >= 22)
            accountManager.removeAccount(model.account, this, { future ->
                try {
                    if (future.result.getBoolean(AccountManager.KEY_BOOLEAN_RESULT))
                        Handler(Looper.getMainLooper()).post {
                            finish()
                        }
                } catch(e: Exception) {
                    Logger.log.log(Level.SEVERE, "Couldn't remove account", e)
                }
            }, null)
        else
            accountManager.removeAccount(model.account, { future ->
                try {
                    if (future.result)
                        Handler(Looper.getMainLooper()).post {
                            finish()
                        }
                } catch (e: Exception) {
                    Logger.log.log(Level.SEVERE, "Couldn't remove account", e)
                }
            }, null)
    }


    // other actions

    fun startPermissionsActivity(view: View) {
        startActivity(Intent(this, PermissionsActivity::class.java))
    }



    // adapter

    class TabsAdapter(
            val activity: AppCompatActivity
    ): FragmentStatePagerAdapter(activity.supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        var cardDavSvcId: Long? = null
            set(value) {
                field = value
                recalculate()
            }
        var calDavSvcId: Long? = null
            set(value) {
                field = value
                recalculate()
            }

        private var idxCardDav: Int? = null
        private var idxCalDav: Int? = null
        private var idxWebcal: Int? = null

        private fun recalculate() {
            var currentIndex = 0

            idxCardDav = if (cardDavSvcId != null)
                currentIndex++
            else
                null

            if (calDavSvcId != null) {
                idxCalDav = currentIndex++
                idxWebcal = currentIndex
            } else {
                idxCalDav = null
                idxWebcal = null
            }

            // reflect changes in UI
            notifyDataSetChanged()
        }

        override fun getCount() =
                (if (idxCardDav != null) 1 else 0) +
                (if (idxCalDav != null) 1 else 0) +
                (if (idxWebcal != null) 1 else 0)

        override fun getItem(position: Int): Fragment {
            val args = Bundle(1)
            when (position) {
                idxCardDav -> {
                    val frag = AddressBooksFragment()
                    args.putLong(CollectionsFragment.EXTRA_SERVICE_ID, cardDavSvcId!!)
                    args.putString(CollectionsFragment.EXTRA_COLLECTION_TYPE, Collection.TYPE_ADDRESSBOOK)
                    frag.arguments = args
                    return frag
                }
                idxCalDav -> {
                    val frag = CalendarsFragment()
                    args.putLong(CollectionsFragment.EXTRA_SERVICE_ID, calDavSvcId!!)
                    args.putString(CollectionsFragment.EXTRA_COLLECTION_TYPE, Collection.TYPE_CALENDAR)
                    frag.arguments = args
                    return frag
                }
                idxWebcal -> {
                    val frag = WebcalFragment()
                    args.putLong(CollectionsFragment.EXTRA_SERVICE_ID, calDavSvcId!!)
                    args.putString(CollectionsFragment.EXTRA_COLLECTION_TYPE, Collection.TYPE_WEBCAL)
                    frag.arguments = args
                    return frag
                }
            }
            throw IllegalArgumentException()
        }

        // required to reload all fragments
        override fun getItemPosition(obj: Any) = POSITION_NONE

        override fun getPageTitle(position: Int): String =
                when (position) {
                    idxCardDav -> activity.getString(R.string.account_carddav)
                    idxCalDav -> activity.getString(R.string.account_caldav)
                    idxWebcal -> activity.getString(R.string.account_webcal)
                    else -> throw IllegalArgumentException()
                }

    }


    // model

    class Model(
            application: Application,
            val account: Account
    ): AndroidViewModel(application), OnAccountsUpdateListener {

        class Factory(
                val application: Application,
                val account: Account
        ): ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>) =
                    Model(application, account) as T
        }

        private val db = AppDatabase.getInstance(application)
        val accountManager = AccountManager.get(application)
        val accountSettings by lazy { AccountSettings(getApplication(), account) }

        val accountExists = MutableLiveData<Boolean>()
        val cardDavService = db.serviceDao().getIdByAccountAndType(account.name, Service.TYPE_CARDDAV)
        val calDavService = db.serviceDao().getIdByAccountAndType(account.name, Service.TYPE_CALDAV)

        val showOnlyPersonal = MutableLiveData<Boolean>()
        val showOnlyPersonal_writable = MutableLiveData<Boolean>()


        init {
            accountManager.addOnAccountsUpdatedListener(this, null, true)
            viewModelScope.launch(Dispatchers.IO) {
                accountSettings.getShowOnlyPersonal().let { (value, locked) ->
                    showOnlyPersonal.postValue(value)
                    showOnlyPersonal_writable.postValue(locked)
                }
            }
        }

        override fun onCleared() {
            accountManager.removeOnAccountsUpdatedListener(this)
        }

        override fun onAccountsUpdated(accounts: Array<out Account>) {
            accountExists.postValue(accounts.contains(account))
        }

        fun toggleReadOnly(item: Collection) {
            viewModelScope.launch(Dispatchers.IO + NonCancellable) {
                val newItem = item.copy(forceReadOnly = !item.forceReadOnly)
                db.collectionDao().update(newItem)
            }
        }

        fun toggleShowOnlyPersonal() {
            showOnlyPersonal.value?.let { oldValue ->
                val newValue = !oldValue
                accountSettings.setShowOnlyPersonal(newValue)
                showOnlyPersonal.postValue(newValue)
            }
        }

        fun toggleSync(item: Collection) {
            viewModelScope.launch(Dispatchers.IO + NonCancellable) {
                val newItem = item.copy(sync = !item.sync)
                db.collectionDao().update(newItem)
            }
        }

    }

}
